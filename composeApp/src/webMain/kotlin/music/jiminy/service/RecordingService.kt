package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import music.jiminy.JiminyCommand
import music.jiminy.JiminyLoggerI
import music.jiminy.WS_START_RECORDING
import music.jiminy.WS_STOP_RECORDING

class RecordingService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val logger: JiminyLoggerI,
) {
    suspend fun startRecording(nodes: JiminyCommand.StartRecording) =
        client.post("$baseUrl$WS_START_RECORDING") {
            contentType(ContentType.Application.Json)
            setBody(nodes)
        }

    suspend fun stopRecording() = client.post("$baseUrl$WS_STOP_RECORDING")
}
