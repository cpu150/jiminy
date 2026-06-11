package music.jiminy

class JsPlatform : Platform {
    override val debug: Boolean = false
    override val version: String = JiminyBuildInfo.VERSION
    override val gitHash: String = JiminyBuildInfo.GIT_HASH
}

actual fun getPlatform(): Platform = JsPlatform()
