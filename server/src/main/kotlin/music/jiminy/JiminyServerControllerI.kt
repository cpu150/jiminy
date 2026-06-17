package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession
import java.io.File

interface JiminyServerControllerI {
    val isRecording: Boolean

    suspend fun executeCommand(command: JiminyCommand): Boolean
    suspend fun getDevicesList(): JiminyDeviceList
    suspend fun linkDevice(link: JiminyCommand.Link): Boolean
    suspend fun getDeviceLinksList(): List<String>
    suspend fun getRecordings(): List<String>
    suspend fun deleteRecordings(filenames: List<String>): Boolean
    fun getRecordingFile(filename: String): File?
    suspend fun startRecording(commands: JiminyCommand.StartRecording): Boolean
    suspend fun stopRecording(): Boolean

    suspend fun getConfigurations(): List<String>
    suspend fun saveConfiguration(config: JiminyConfiguration): Boolean
    suspend fun getConfiguration(name: String): JiminyConfiguration?
    suspend fun deleteConfiguration(name: String): Boolean

    suspend fun shutdown(): Boolean
    suspend fun updateServer(): Boolean

    suspend fun fetchLatestVersion()

    suspend fun broadcastAll(
        sessions: List<DefaultWebSocketServerSession>,
        command: JiminyCommand,
        status: Boolean = true,
    )
}
