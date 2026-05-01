package music.jiminy

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import music.jiminy.service.DeviceService
import music.jiminy.service.MainService
import music.jiminy.service.MixerService
import music.jiminy.service.RecordingService
import music.jiminy.viewmodel.ConnectionViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        val jsonInstance = Json
        val client: HttpClient = HttpClient(Js) {
            install(ContentNegotiation) {
                json(jsonInstance)
            }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(jsonInstance)
            }
        }

        val protocol = window.location.protocol
        val hostname: String = window.location.hostname
        val port: Int = window.location.port.ifBlank { "80" }.toInt()
        val baseUrl = "$protocol//$hostname:$port"

        val mixerService = MixerService(hostname, port, client)
        val deviceService = DeviceService(client, baseUrl)
        val recordingService = RecordingService(client, baseUrl)

        val viewModel = viewModel {
            ConnectionViewModel(
                MainService(mixerService, deviceService, recordingService)
            )
        }

        App { viewModel }
    }
}
