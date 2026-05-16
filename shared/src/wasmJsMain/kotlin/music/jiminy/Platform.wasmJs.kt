package music.jiminy

class WasmPlatform: Platform {
    override val debug: Boolean = false
}

actual fun getPlatform(): Platform = WasmPlatform()
