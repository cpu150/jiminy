@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package music.jiminy.utils

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.JsArray
import kotlin.js.toJsString

private fun createBlobOptions(type: String): BlobPropertyBag = js("({ type: type })")

actual object BrowserUtils {
    actual fun triggerFileDownload(
        fileName: String,
        content: String,
    ) {
        val parts = JsArray<kotlin.js.JsAny?>()
        parts[0] = content.toJsString()

        val blob = Blob(
            parts,
            createBlobOptions("text/markdown"),
        )
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = fileName
        document.body?.appendChild(anchor)
        anchor.click()
        document.body?.removeChild(anchor)
        URL.revokeObjectURL(url)
    }
}
