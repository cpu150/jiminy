package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import music.jiminy.JiminyCommand
import music.jiminy.JiminyLoggerI
import music.jiminy.WS_MIXER

class MixerService(
    private val hostname: String,
    private val port: Int,
    private val client: HttpClient,
    private val logger: JiminyLoggerI,
) {
    private var session: DefaultClientWebSocketSession? = null
    private val _succeededCommands = MutableSharedFlow<JiminyCommand>(
        extraBufferCapacity = 64, // Handle bursts of messages
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val succeededCommands = _succeededCommands.asSharedFlow()

    suspend fun connect(connected: (() -> Unit)? = null) = try {
        client.webSocket(
            method = HttpMethod.Get,
            host = hostname,
            port = port,
            path = WS_MIXER,
        ) {
            session = this

            // This loop runs until the socket closes
            try {
                connected?.invoke()
                while (true) {
                    _succeededCommands.emit(receiveDeserialized<JiminyCommand>())
                }
            } catch (e: ClosedReceiveChannelException) {
                logger.info("Jiminy Client - WebSocket closed - ${e.message}")
            } catch (e: CancellationException) {
                logger.info("Jiminy Client - WebSocket stopped - ${e.message}")
                throw e
            } catch (e: Exception) {
                logger.error("Jiminy Client - ERROR - WebSocket error: $e - ${e.message}")
                throw e
            }
        }
    } catch (e: Exception) {
        throw e
    }

    suspend fun sendCommand(command: JiminyCommand) = try {
        if (session?.isActive == true) {
            session?.sendSerialized(command)
        } else {
            throw IllegalStateException("ERROR - session not active - sendCommand($command)")
        }
    } catch (e: Exception) {
        throw e
    }

    suspend fun disconnect() {
        session?.close(CloseReason(CloseReason.Codes.NORMAL, "User exited mixer"))
        session = null
    }
}
