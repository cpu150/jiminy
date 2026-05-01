package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession

class MockController : JiminyServerControllerI {
    private var _isRecording = false
    override val isRecording: Boolean
        get() = _isRecording

    override suspend fun executeCommand(command: JiminyCommand) = true

    override suspend fun getDevicesList() = JiminyDeviceList(
        instruments = emptyList(),
        speakers = emptyList(),
        deviceStatus = emptyList(),
    )

    override suspend fun linkDevice(link: JiminyCommand.Link) = true

    override suspend fun getDeviceLinksList() = dummyLinksCmd

    override suspend fun startRecording(commands: JiminyCommand.StartRecording) = true
        .also { _isRecording = true }

    override suspend fun stopRecording() = true
        .also { _isRecording = false }

    override suspend fun broadcastAll(
        sessions: List<DefaultWebSocketServerSession>,
        command: JiminyCommand,
        status: Boolean
    ) {
        // no-op
    }
}