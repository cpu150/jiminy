package music.jiminy.viewmodel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import music.jiminy.JiminyCommand
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDevice
import music.jiminy.JiminyLoggerI
import music.jiminy.LogEntry
import music.jiminy.NodeConnection
import music.jiminy.service.JiminyConnectionStatus
import music.jiminy.service.JiminyResponse
import music.jiminy.service.MainService
import kotlin.time.Duration.Companion.milliseconds

class FakeLogger : JiminyLoggerI {
    val loggedInfo = mutableListOf<String>()
    val loggedWarning = mutableListOf<String>()
    val loggedError = mutableListOf<String>()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    override val logEntries: List<LogEntry>
        get() = _logs.value

    override fun info(log: String) {
        loggedInfo.add(log)
    }

    override fun warning(log: String) {
        loggedWarning.add(log)
    }

    override fun error(log: String) {
        loggedError.add(log)
    }

    override fun clear() {
        loggedInfo.clear()
        loggedWarning.clear()
        loggedError.clear()
        _logs.value = emptyList()
    }
}

open class FakeMainService(
    logger: JiminyLoggerI = FakeLogger(),
) : MainService {
    private val _succeededCommands = MutableSharedFlow<JiminyCommand>(extraBufferCapacity = 64)
    override val succeededCommands = _succeededCommands.asSharedFlow()

    private val _audioDevices = MutableStateFlow<List<JiminyDevice>>(emptyList())
    override val audioDevices: StateFlow<List<JiminyDevice>> = _audioDevices.asStateFlow()

    private val _midiDevices = MutableStateFlow<List<JiminyDevice>>(emptyList())
    override val midiDevices: StateFlow<List<JiminyDevice>> = _midiDevices.asStateFlow()

    private val _connectionStatus =
        MutableStateFlow<JiminyConnectionStatus>(JiminyConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<JiminyConnectionStatus> =
        _connectionStatus.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    val mockDeviceLinks = mutableListOf<NodeConnection>()
    val mockRecordings = mutableListOf<String>()
    val mockServerLogs = mutableListOf<LogEntry>()
    val mockConfigurations = mutableListOf<JiminyConfiguration>()

    fun setAudioDevices(devices: List<JiminyDevice>) {
        _audioDevices.value = devices
    }

    fun setMidiDevices(devices: List<JiminyDevice>) {
        _midiDevices.value = devices
    }

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
    }

    fun setConnectionStatus(status: JiminyConnectionStatus) {
        _connectionStatus.value = status
    }

    override suspend fun mixerSendCommand(command: JiminyCommand) {
        _succeededCommands.emit(command)
    }

    override suspend fun mixerDisconnect() {
        _connectionStatus.value = JiminyConnectionStatus.Disconnected
    }

    override suspend fun mixerConnect(connected: (() -> Unit)?) {
        _connectionStatus.value = JiminyConnectionStatus.Connected
        connected?.invoke()
    }

    override suspend fun refreshDevices(onError: (JiminyResponse) -> Unit) {
        // No-op in fake
    }

    override suspend fun getDeviceLinks(
        onSuccess: (JiminyResponse.Success<List<NodeConnection>>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) {
        onSuccess(JiminyResponse.Success(mockDeviceLinks))
    }

    override suspend fun deviceLinks(
        links: List<JiminyCommand.Link>,
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
        finally: () -> Unit,
    ) {
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
        finally()
    }

    override suspend fun startRecording(
        nodes: JiminyCommand.StartRecording,
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        _isRecording.value = true
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
    }

    override suspend fun stopRecording(
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        _isRecording.value = false
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
    }

    override suspend fun getServerLogs(
        onSuccess: (JiminyResponse.Success<List<LogEntry>>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) {
        onSuccess(JiminyResponse.Success(mockServerLogs))
    }

    override suspend fun flushServerLogs(
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        mockServerLogs.clear()
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
    }

    override suspend fun getRecordings(
        onSuccess: (JiminyResponse.Success<List<String>>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) {
        onSuccess(JiminyResponse.Success(mockRecordings))
    }

    override suspend fun deleteRecordings(
        filenames: List<String>,
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        mockRecordings.removeAll(filenames)
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
    }

    override suspend fun downloadRecordings(
        filenames: List<String>,
        onSuccess: ((JiminyResponse.Success<io.ktor.client.statement.HttpResponse>) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        // No-op in fake
    }

    override suspend fun getConfigurations(
        onSuccess: (JiminyResponse.Success<List<String>>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) {
        delay(10.milliseconds)
        onSuccess(JiminyResponse.Success(mockConfigurations.map { it.name }))
    }

    override suspend fun getConfiguration(
        name: String,
        onSuccess: suspend (JiminyResponse.Success<JiminyConfiguration>) -> Unit,
        onError: (JiminyResponse) -> Unit,
    ) {
        mockConfigurations.find { it.name == name }?.let {
            onSuccess(JiminyResponse.Success(it))
        } ?: onError(JiminyResponse.Error("Not found"))
    }

    override suspend fun saveConfiguration(
        config: JiminyConfiguration,
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        mockConfigurations.removeAll { it.name == config.name }
        mockConfigurations.add(config)
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
    }

    override suspend fun deleteConfiguration(
        name: String,
        onSuccess: ((JiminyResponse.EmptySuccess) -> Unit)?,
        onError: (JiminyResponse) -> Unit,
    ) {
        mockConfigurations.removeAll { it.name == name }
        onSuccess?.invoke(JiminyResponse.EmptySuccess)
    }
}
