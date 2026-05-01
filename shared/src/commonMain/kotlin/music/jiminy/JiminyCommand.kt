package music.jiminy

import kotlinx.serialization.Serializable

@Serializable
enum class LinkType {
    Connect,
    Disconnect,
}

@Serializable
data class Recorder (
    val label: String,
    val nodeName: String,
)

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
        val type: LinkType = LinkType.Connect,
    ) : JiminyCommand

    @Serializable
    data class StartRecording(val recoders: List<Recorder>) : JiminyCommand

    @Serializable
    data class StopRecording(val isRecording: Boolean = false) : JiminyCommand
}
