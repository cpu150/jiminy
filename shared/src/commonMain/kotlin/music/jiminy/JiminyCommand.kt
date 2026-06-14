package music.jiminy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LinkType {
    Connect,
    Disconnect,
}

@Serializable
sealed interface JiminyCommand {
    @Serializable
    data class VolumeUpdate(
        val deviceVolume: JiminyVolume,
        val volume: Float,
    ) : JiminyCommand

    @Serializable
    data class MuteUpdate(
        val deviceVolume: JiminyVolume,
        val muteState: Boolean,
    ) : JiminyCommand

    @Serializable
    data class Link(
        val instrument: String,
        val speaker: String,
        @SerialName("linkType")
        val type: LinkType = LinkType.Connect,
    ) : JiminyCommand

    @Serializable
    data class StartRecording(val nodes: List<JiminyDeviceNode>) : JiminyCommand

    @Serializable
    data class StopRecording(val isRecording: Boolean = false) : JiminyCommand

    @Serializable
    data class Batch(val commands: List<JiminyCommand>) : JiminyCommand

    @Serializable
    data class SaveConfiguration(val config: JiminyConfiguration) : JiminyCommand

    @Serializable
    data class DeleteConfiguration(val name: String) : JiminyCommand

    @Serializable
    data class DeleteRecordings(val filenames: List<String>) : JiminyCommand

    @Serializable
    data object FlushServerLogs : JiminyCommand

    @Serializable
    data object Shutdown : JiminyCommand
}
