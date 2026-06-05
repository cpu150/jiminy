package music.jiminy

import kotlinx.serialization.Serializable

@Serializable
data class SaveConfigOptions(
    val saveAudio: Boolean = true,
    val saveMidi: Boolean = true,
)

val SaveConfigOptions.isValid get() = saveAudio || saveMidi
