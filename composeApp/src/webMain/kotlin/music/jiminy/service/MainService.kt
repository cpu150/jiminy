package music.jiminy.service

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.WebsocketDeserializeException
import io.ktor.websocket.FrameType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.LockedForRecordingException
import music.jiminy.service.JiminyConnectionStatus.Connected
import music.jiminy.service.JiminyConnectionStatus.Connecting
import music.jiminy.service.JiminyConnectionStatus.Disconnected
import music.jiminy.service.JiminyResponse.Cancelled
import music.jiminy.service.JiminyResponse.ConnectionClosed
import music.jiminy.service.JiminyResponse.EmptySuccess
import music.jiminy.service.JiminyResponse.Recording
import music.jiminy.service.JiminyResponse.Success

sealed interface JiminyResponse {
    data class Success<T>(val value: T) : JiminyResponse
    object EmptySuccess : JiminyResponse
    data class Error(val message: String) : JiminyResponse
    object ConnectionClosed : JiminyResponse
    object Recording : JiminyResponse
    object Cancelled : JiminyResponse
}

sealed interface JiminyConnectionStatus {
    object Disconnected : JiminyConnectionStatus
    object Connecting : JiminyConnectionStatus
    object Connected : JiminyConnectionStatus
    data class Error(val exception: JiminyResponse? = null) : JiminyConnectionStatus
}

class MainService(
    private val mixerService: MixerService,
    private val deviceService: DeviceService,
    private val recordingService: RecordingService,
) {
    val succeededCommands = mixerService.succeededCommands

    private val _connectionStatus = MutableStateFlow<JiminyConnectionStatus>(Disconnected)
    val connectionStatus: StateFlow<JiminyConnectionStatus>
        get() = _connectionStatus

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean>
        get() = _isRecording

    private suspend fun <T> handleExceptions(
        tryBlock: suspend () -> T?,
        catchBlock: (JiminyResponse) -> T?,
        finallyBlock: (() -> T?)? = null,
        logMsg: String,
    ) = try {
        tryBlock()
    } catch (e: Exception) {
        _isRecording.update { e is LockedForRecordingException }

        val error = when (e) {
            is WebsocketDeserializeException -> when (e.frame.frameType) {
                FrameType.CLOSE -> ConnectionClosed.also { _connectionStatus.update { Disconnected } }
                else -> null
            }

            is CancellationException -> Cancelled
            is LockedForRecordingException -> Recording
            is ClientRequestException -> handleHttpResponse(logMsg, e.response)
            else -> null
        } ?: JiminyResponse.Error("$logMsg - ${e.message} - $e")

        catchBlock(error)
            .takeIf { error is CancellationException }
            ?.let { throw e }
    } finally {
        finallyBlock?.invoke()
    }

    private fun handleHttpResponse(logMsg: String, response: HttpResponse) =
        when (response.status) {
            HttpStatusCode.OK -> null
            HttpStatusCode.Locked -> Recording
            else -> JiminyResponse.Error("$logMsg - ${response.status} - $response")
        }.also { _isRecording.update { response.status.value == HttpStatusCode.Locked.value } }

    suspend fun mixerSendCommand(command: JiminyCommand) = mixerService.sendCommand(command)
    suspend fun mixerDisconnect() = mixerService.disconnect()
    suspend fun mixerConnect(connected: (() -> Unit)? = null) {
        _connectionStatus.update { Connecting }
        handleExceptions(
            logMsg = "mixerConnect",
            tryBlock = {
                mixerService.connect {
                    _connectionStatus.update { Connected }
                    connected?.invoke()
                }

                _connectionStatus.update { Disconnected }
            },
            catchBlock = { e ->
                when (e) {
                    is CancellationException -> _connectionStatus.update { Disconnected }
                    else -> _connectionStatus.update { JiminyConnectionStatus.Error(e) }
                }
            }
        )
    }

    suspend fun getDevices(
        onSuccess: (Success<List<JiminyDevice>>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) = handleExceptions(
        logMsg = "getDevices",
        tryBlock = { onSuccess(Success(deviceService.getDevices())) },
        catchBlock = { error -> onError(error) },
    )

    suspend fun getDeviceLinks(
        onSuccess: (Success<List<Pair<JiminyDeviceNode, JiminyDeviceNode>>>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) = handleExceptions(
        logMsg = "getDeviceLinks",
        tryBlock = { onSuccess(Success(deviceService.getDeviceLinks())) },
        catchBlock = { error -> onError(error) },
    )

    suspend fun deviceLinks(
        links: List<JiminyCommand.Link>,
        onSuccess: ((EmptySuccess) -> Unit)? = null,
        onError: (JiminyResponse) -> Unit,
        finally: () -> Unit,
    ) = handleExceptions(
        logMsg = "deviceLinks",
        tryBlock = {
            handleHttpResponse("deviceLinks", deviceService.linkDevices(links))
                ?.let { onError(it) }
                ?: onSuccess?.invoke(EmptySuccess)
        },
        catchBlock = { error -> onError(error) },
        finallyBlock = finally,
    )

    suspend fun startRecording(
        nodes: JiminyCommand.StartRecording,
        onSuccess: ((EmptySuccess) -> Unit)? = null,
        onError: (JiminyResponse) -> Unit,
    ) = handleExceptions(
        logMsg = "startRecording",
        tryBlock = {
            handleHttpResponse("startRecording", recordingService.startRecording(nodes))
                ?.let { onError(it) }
                ?: let { onSuccess?.invoke(EmptySuccess) }
        },
        catchBlock = { error -> onError(error) },
    )


    suspend fun stopRecording(
        onSuccess: ((EmptySuccess) -> Unit)? = null,
        onError: (JiminyResponse) -> Unit,
    ) = handleExceptions(
        logMsg = "stopRecording",
        tryBlock = {
            handleHttpResponse("stopRecording", recordingService.stopRecording())
                ?.let { onError(it) }
                ?: let { onSuccess?.invoke(EmptySuccess) }
        },
        catchBlock = { error -> onError(error) },
    )
}
