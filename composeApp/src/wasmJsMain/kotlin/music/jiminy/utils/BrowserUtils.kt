@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package music.jiminy.utils

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.JsArray
import kotlin.js.toJsString
import kotlin.js.toJsNumber

private fun createBlobOptions(type: String): BlobPropertyBag = js("({ type: type })")

private fun createInt8Array(size: Int): kotlin.js.JsAny = js("new Int8Array(size)")

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

    actual fun triggerBinaryDownload(
        fileName: String,
        content: ByteArray,
    ) {
        val parts = JsArray<kotlin.js.JsAny?>()
        val jsArray = createInt8Array(content.size)
        val jsArrayTyped = jsArray.unsafeCast<JsArray<kotlin.js.JsAny?>>()
        for (i in 0 until content.size) {
            jsArrayTyped[i] = content[i].toInt().toJsNumber()
        }
        parts[0] = jsArray

        val blob = Blob(
            parts,
            createBlobOptions("application/octet-stream"),
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
