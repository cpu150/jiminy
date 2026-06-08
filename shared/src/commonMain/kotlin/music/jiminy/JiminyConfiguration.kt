package music.jiminy

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class JiminyConfiguration(
    val name: String,
    val audioLinks: List<JiminyCommand.Link>,
    val midiLinks: List<JiminyCommand.Link>,
    val volumes: List<JiminyVolume> = emptyList(),
)
