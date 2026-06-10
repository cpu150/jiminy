package music.jiminy

interface Platform {
    val debug: Boolean
    val version: String
}

expect fun getPlatform(): Platform
