package music.jiminy

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class JiminyDeviceList(
    val instruments: List<String>,
    val speakers: List<String>,
    val deviceStatus: List<String>,
    val latestVersion: String = "",
)
