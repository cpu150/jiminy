package music.jiminy

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class SaveConfigOptions(
    val name: String = "",
    val saveAudio: Boolean = true,
    val saveMidi: Boolean = true,
    val saveVolumes: Boolean = true,
    val saveRecordingNodes: Boolean = true,
)

val SaveConfigOptions.isValid
    get() = !name.isBlank() && (saveAudio || saveMidi || saveVolumes || saveRecordingNodes)
