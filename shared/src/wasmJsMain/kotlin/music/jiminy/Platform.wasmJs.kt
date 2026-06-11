package music.jiminy

class WasmPlatform : Platform {
    override val debug: Boolean = false
    override val version: String = JiminyBuildInfo.VERSION
    override val gitHash: String = JiminyBuildInfo.GIT_HASH
}

actual fun getPlatform(): Platform = WasmPlatform()
