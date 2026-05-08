package music.jiminy

import org.slf4j.LoggerFactory

class Logger: JiminyLoggerI {
    private val logger = LoggerFactory.getLogger("JiminyServer")

    override fun info(log: String) {
        logger.info(log)
    }

    override fun warning(log: String) {
        logger.warn(log)
    }

    override fun error(log: String) {
        logger.error(log)
    }
}