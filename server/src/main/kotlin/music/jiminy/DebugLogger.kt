package music.jiminy

import org.slf4j.LoggerFactory
import java.util.Collections
import kotlin.time.Clock

class DebugLogger : JiminyLoggerI {

    private val logger = LoggerFactory.getLogger("JiminyServer")
    private val _logEntries = Collections.synchronizedList(mutableListOf<LogEntry>())

    override val logEntries: List<LogEntry>
        get() = synchronized(_logEntries) { _logEntries.toList() }

    override fun info(log: String) {
        addLog(LogType.INFO, log)
        println("[INFO] $log")
        logger.info(log)
    }

    override fun warning(log: String) {
        addLog(LogType.WARNING, log)
        println("[WARNING] $log")
        logger.warn(log)
    }

    override fun error(log: String) {
        addLog(LogType.ERROR, log)
        println("[ERROR] $log")
        logger.error(log)
    }

    override fun clear() {
        _logEntries.clear()
    }

    private fun addLog(type: LogType, message: String) {
        val entry = LogEntry(
            type = type,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            message = message,
        )
        _logEntries.add(entry)
    }
}
