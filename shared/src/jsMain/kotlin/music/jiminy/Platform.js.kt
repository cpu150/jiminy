package music.jiminy

class JsPlatform: Platform {
    override val debug: Boolean = false
}

actual fun getPlatform(): Platform = JsPlatform()