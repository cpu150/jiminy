package music.jiminy

interface Platform {
    val debug: Boolean
    val version: String
    val gitHash: String
}

expect fun getPlatform(): Platform
