package music.jiminy

import kotlinx.serialization.Serializable

@Serializable
enum class LogType {
    INFO,
    WARNING,
    ERROR,
}

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
