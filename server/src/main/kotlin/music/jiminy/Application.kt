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
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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
import java.util.Collections
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds

fun main() {
    val controller = if (DEBUG) MockController() else Controller()
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    embeddedServer(
        factory = Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = { module(json, controller) },
    ).start(wait = true)
}

@OptIn(ExperimentalAtomicApi::class)
fun Application.module(json: Json, controller: JiminyServerControllerI) {
    install(ContentNegotiation) {
        json(json)
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
        val isWasmAppPath =
            path == "/" || path.endsWith(".wasm") || path.endsWith(".js") || path.endsWith(".html")
        val isGetWasmAppPath = isWasmAppPath && method == HttpMethod.Get

        if (controller.isRecording) {
            if (!isStopRecordingPath && !isGetWasmAppPath) {
                // Block request
                call.respond(
                    status = HttpStatusCode.Locked,
                    message = mapOf("error" to "Server recording. Only $WS_STOP_RECORDING is available."),
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
            println("Jiminy Server - WebSocket - Adding new session $this")
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
                        } ?: println("Jiminy Server - Busy - Command Ignored $command")
                }
            } catch (e: ClosedReceiveChannelException) {
                println("Jiminy Server - WebSocket closed - ${e.localizedMessage}")
            } catch (e: Exception) {
                println("Jiminy Server - ERROR - WebSocket error: $e - ${e.localizedMessage}")
            } finally {
                println("Cleaning up resources for this session...")
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
                call.respond(HttpStatusCode.OK, "Recording started")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        post(WS_STOP_RECORDING) {
            try {
                controller.stopRecording()
                controller.broadcastAll(sessions.toList(), JiminyCommand.StopRecording())
                call.respond(HttpStatusCode.OK, "Recording saved")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
            }
        }

        staticResources(WS_ROOT, "static") { default(WS_DEFAULT_PATH) }
    }
}
