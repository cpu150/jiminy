package music.jiminy.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import music.jiminy.JiminyLoggerI
import music.jiminy.LogEntry
import music.jiminy.LogType
import kotlin.time.Clock

class JiminyLogger : JiminyLoggerI {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    override fun info(log: String) {
        addLog(LogType.INFO, log)
        println("INFO: $log")
    }

    override fun warning(log: String) {
        addLog(LogType.WARNING, log)
        println("WARNING: $log")
    }

    override fun error(log: String) {
        addLog(LogType.ERROR, log)
        println("ERROR: $log")
    }

    private fun addLog(type: LogType, message: String) {
        val entry = LogEntry(
            type = type,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            message = message,
        )
        _logs.update { it + entry }
    }
}
