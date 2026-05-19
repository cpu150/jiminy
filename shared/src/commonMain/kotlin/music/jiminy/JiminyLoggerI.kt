package music.jiminy

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class LogType {
    INFO,
    WARNING,
    ERROR,
}

@Stable
@Serializable
data class LogEntry(
    val type: LogType,
    val timestamp: Long,
    val message: String,
)

interface JiminyLoggerI {
    fun info(log: String)
    fun warning(log: String)
    fun error(log: String)
}
