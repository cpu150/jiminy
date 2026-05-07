package music.jiminy

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import music.jiminy.service.DeviceService
import music.jiminy.service.MainService
import music.jiminy.service.MixerService
import music.jiminy.service.RecordingService
import music.jiminy.viewmodel.ConnectionViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val appCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    ComposeViewport {
        val jsonInstance = Json
        val client = HttpClient(Js) {
            // This allows the validator to run for non-2xx codes
            expectSuccess = true

            HttpResponseValidator {
                validateResponse { response ->
                    val statusCode = response.status.value

                    if (statusCode == HttpStatusCode.Locked.value) {
                        throw LockedForRecordingException()
                    }
                }
            }

            install(ContentNegotiation) {
                json(jsonInstance)
            }

            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(jsonInstance)
            }
        }

        val protocol = window.location.protocol
        val hostname = window.location.hostname
        val defaultPort = if (DEBUG) DEBUG_SERVER_PORT else SERVER_PORT
        val port = window.location.port.ifBlank { defaultPort.toString() }.toInt()
        val baseUrl = "$protocol//$hostname:$port"

        val mixerService = MixerService(hostname, port, client)
        val deviceService = DeviceService(client, baseUrl)
        val recordingService = RecordingService(client, baseUrl)

        val viewModel = viewModel {
            ConnectionViewModel(
                MainService(appCoroutineScope, mixerService, deviceService, recordingService)
            )
        }

        App { viewModel }
    }
}
