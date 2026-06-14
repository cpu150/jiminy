package music.jiminy.utils

expect object BrowserUtils {
    fun triggerFileDownload(
        fileName: String,
        content: String,
    )

    fun triggerBinaryDownload(
        fileName: String,
        content: ByteArray,
    )
}
