package music.jiminy.service

import music.jiminy.JiminyLoggerI

class JiminyLogger : JiminyLoggerI {
    override fun info(log: String) {
        println("INFO: $log")
    }

    override fun warning(log: String) {
        println("WARNING: $log")
    }

    override fun error(log: String) {
        println("ERROR: $log")
    }
}
