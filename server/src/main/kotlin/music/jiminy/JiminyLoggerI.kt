package music.jiminy

interface JiminyLoggerI {
    fun info(log: String)
    fun warning(log: String)
    fun error(log: String)
}