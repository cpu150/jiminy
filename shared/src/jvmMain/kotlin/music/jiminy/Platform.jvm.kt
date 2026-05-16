package music.jiminy

class JVMPlatform : Platform {
    override val debug: Boolean = System.getProperty("DEBUG")?.toBoolean()
        ?: System.getenv("DEBUG")?.toBoolean()
        ?: false
}

actual fun getPlatform(): Platform = JVMPlatform()