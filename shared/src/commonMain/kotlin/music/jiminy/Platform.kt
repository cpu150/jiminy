package music.jiminy

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform