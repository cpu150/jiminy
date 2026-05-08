package music.jiminy.di

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
import music.jiminy.DEBUG
import music.jiminy.DEBUG_SERVER_PORT
import music.jiminy.LockedForRecordingException
import music.jiminy.SERVER_PORT
import music.jiminy.service.DeviceService
import music.jiminy.service.MainService
import music.jiminy.service.MixerService
import music.jiminy.service.RecordingService
import music.jiminy.viewmodel.ConnectionScreenViewModel
import music.jiminy.viewmodel.ConnectionViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    single {
        val jsonInstance = Json
        HttpClient(Js) {
            expectSuccess = true
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status.value == HttpStatusCode.Locked.value) {
                        throw LockedForRecordingException()
                    }
                }
            }
            install(ContentNegotiation) { json(jsonInstance) }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(jsonInstance)
            }
        }
    }

    single {
        val defaultPort = if (DEBUG) DEBUG_SERVER_PORT else SERVER_PORT
        val hostname = window.location.hostname
        val protocol = window.location.protocol

        // During local development, the frontend is often served by a dev server (e.g. port 8080)
        // while the Ktor backend runs on its own port. We ensure the client points to the backend.
        val port = if (hostname == "localhost" && DEBUG) {
            DEBUG_SERVER_PORT
        } else {
            window.location.port.ifBlank { defaultPort.toString() }.toInt()
        }

        val baseUrl = "$protocol//$hostname:$port"

        Triple(hostname, port, baseUrl)
    }

    single {
        MixerService(
            get<Triple<String, Int, String>>().first,
            get<Triple<String, Int, String>>().second,
            get(),
        )
    }
    single { DeviceService(get(), get<Triple<String, Int, String>>().third) }
    single { RecordingService(get(), get<Triple<String, Int, String>>().third) }

    singleOf(::MainService)

    viewModelOf(::ConnectionViewModel)
    viewModelOf(::ConnectionScreenViewModel)
}
