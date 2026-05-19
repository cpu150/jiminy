package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession

interface JiminyServerControllerI {
    val isRecording: Boolean

    suspend fun executeCommand(command: JiminyCommand): Boolean
    suspend fun getDevicesList(): JiminyDeviceList
    suspend fun linkDevice(link: JiminyCommand.Link): Boolean
    suspend fun getDeviceLinksList(): List<String>
    suspend fun getRecordings(): List<String>
    suspend fun deleteRecordings(filenames: List<String>): Boolean
    suspend fun startRecording(commands: JiminyCommand.StartRecording): Boolean
    suspend fun stopRecording(): Boolean
    suspend fun broadcastAll(
        sessions: List<DefaultWebSocketServerSession>,
        command: JiminyCommand,
        status: Boolean = true,
    )
}