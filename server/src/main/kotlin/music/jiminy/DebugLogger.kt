package music.jiminy

import org.slf4j.LoggerFactory

class DebugLogger: JiminyLoggerI {

    private val logger = LoggerFactory.getLogger("JiminyServer")

    override fun info(log: String) {
        println("[INFO] $log")
        logger.info(log)
    }

    override fun warning(log: String) {
        println("[WARNING] $log")
        logger.warn(log)
    }

    override fun error(log: String) {
        println("[ERROR] $log")
        logger.error(log)
    }
}