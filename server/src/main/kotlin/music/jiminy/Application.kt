package music.jiminy

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.util.Collections
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds

fun main() {
    val port = if (DEBUG) DEBUG_SERVER_PORT else SERVER_PORT
    val host = if (DEBUG) DEBUG_SERVER_HOST else SERVER_HOST
    val logger = if (DEBUG) DebugLogger() else Logger()

    val controller = if (DEBUG) MockController() else Controller(logger)
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    embeddedServer(
        factory = Netty,
        port = port,
        host = host,
        module = { module(json, controller, logger) },
    ).start(wait = true)
}

@OptIn(ExperimentalAtomicApi::class)
fun Application.module(json: Json, controller: JiminyServerControllerI, logger: JiminyLoggerI) {
    install(CallLogging) {
        level = Level.INFO // This ensures requests show up in the logs
        // This filter ignores the frequent polling/websocket noise if you want
        filter { call -> call.request.path().startsWith("/") }
        // This ensures you see the actual URL being requested
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Status: $status, Method: $httpMethod, UserAgent: $userAgent"
        }
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        anyHost() // BE CAREFUL: Only for local debugging
    }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(json)
        pingPeriod = 14.seconds
        timeout = 16.seconds
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()
        val method = call.request.httpMethod

        val isStopRecordingPath = path.endsWith(WS_STOP_RECORDING)
        // favicon.ico, styles.css, composeApp.js, 73cbe24d7cf5a54d37ad.wasm, 836cba7d6d2ab50e6ca5.wasm
        val isWasmAppPath = path == "/" ||
                path.endsWith(".wasm", ignoreCase = true) ||
                path.endsWith(".js", ignoreCase = true) ||
                path.endsWith(".html", ignoreCase = true) ||
                path.endsWith(".css", ignoreCase = true) ||
                path.contains(".ico", ignoreCase = true)
        val gettingWasmAppPath = isWasmAppPath && method == HttpMethod.Get

        if (controller.isRecording) {
            if (!isStopRecordingPath && !gettingWasmAppPath) {
                call.respond(
                    status = HttpStatusCode.Locked,
                    message = mapOf("error" to "Server recording, $WS_STOP_RECORDING available only"),
                )
                finish()
            }
        }
    }

    // A thread-safe set to store all active WebSocket sessions
    val sessions = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketServerSession>())
    val jobInProgress = AtomicBoolean(false)
    routing {
        webSocket(WS_MIXER) {
            logger.info("Jiminy Server - WebSocket - Adding new session $this")
            try {
                sessions.add(this)

                if (controller.isRecording) {
                    sendSerialized(JiminyCommand.StopRecording())
                }

                while (true) {
                    // Waiting for a command to be received
                    val command = receiveDeserialized<JiminyCommand>()

                    // Processing command
                    jobInProgress
                        // IMPORTANT: If recording then abort before setting 'isRecording = true'
                        .takeIf { !controller.isRecording }
                        ?.compareAndSet(expectedValue = false, newValue = true)
                        ?.takeIf { it }
                        ?.let {
                            withContext(Dispatchers.IO) {
                                val status = controller.executeCommand(command)

                                controller.broadcastAll(sessions.toList(), command, status)

                                jobInProgress.store(false)
                            }
                        } ?: logger.warning("Jiminy Server - Busy - Command Ignored $command")
                }
            } catch (e: ClosedReceiveChannelException) {
                logger.info("Jiminy Server - WebSocket closed - ${e.localizedMessage}")
            } catch (e: Exception) {
                logger.error("Jiminy Server - ERROR - WebSocket error: $e - ${e.localizedMessage}")
            } finally {
                sessions.remove(this)
            }
        }

        get(WS_DEVICES) { call.respond(controller.getDevicesList()) }

        get(WS_LINK_DEVICES) { call.respond(controller.getDeviceLinksList()) }

        post(WS_LINK_DEVICES) {
            try {
                val links = call.receive<List<JiminyCommand.Link>>()
                // Run all connections in parallel for speed
                val failed = links
                    .map { async { controller.linkDevice(it) } }
                    .awaitAll()
                    .count { !it }

                if (failed == 0) {
                    call.respond(HttpStatusCode.OK, "All links established")
                } else {
                    call.respond(HttpStatusCode.MultiStatus, "Failed to connect $failed links")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        post(WS_START_RECORDING) {
            try {
                val nodes = call.receive<JiminyCommand.StartRecording>()
                controller.startRecording(nodes)
                controller.broadcastAll(sessions.toList(), nodes)
                call.respond(HttpStatusCode.Locked, "Started recording...")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        post(WS_STOP_RECORDING) {
            try {
                controller.stopRecording()
                controller.broadcastAll(sessions.toList(), JiminyCommand.StopRecording())
                call.respond(HttpStatusCode.OK, "Stopped recording")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        staticResources(WS_ROOT, "static") { default(WS_DEFAULT_PATH) }
    }
}
