package music.jiminy

class JVMPlatform : Platform {
    override val debug: Boolean = System.getProperty("DEBUG")?.toBoolean()
        ?: System.getenv("DEBUG")?.toBoolean()
        ?: false
    override val version: String = JiminyBuildInfo.GIT_HASH
}

actual fun getPlatform(): Platform = JVMPlatform()
