package music.jiminy

interface Platform {
    val debug: Boolean
}

expect fun getPlatform(): Platform