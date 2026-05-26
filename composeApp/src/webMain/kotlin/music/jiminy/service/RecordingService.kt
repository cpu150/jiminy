package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import music.jiminy.JiminyCommand
import music.jiminy.JiminyLoggerI
import music.jiminy.WS_DELETE_RECORDINGS
import music.jiminy.WS_DOWNLOAD_RECORDINGS
import music.jiminy.WS_RECORDINGS
import music.jiminy.WS_START_RECORDING
import music.jiminy.WS_STOP_RECORDING

class RecordingService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val logger: JiminyLoggerI,
) {
    suspend fun getRecordings(): List<String> = client.get("$baseUrl$WS_RECORDINGS").body()

    suspend fun deleteRecordings(filenames: List<String>): HttpResponse =
        client.post("$baseUrl$WS_DELETE_RECORDINGS") {
            contentType(ContentType.Application.Json)
            setBody(filenames)
        }

    suspend fun downloadRecordings(filenames: List<String>): HttpResponse =
        client.post("$baseUrl$WS_DOWNLOAD_RECORDINGS") {
            contentType(ContentType.Application.Json)
            setBody(filenames)
        }

    suspend fun startRecording(nodes: JiminyCommand.StartRecording) =
        client.post("$baseUrl$WS_START_RECORDING") {
            contentType(ContentType.Application.Json)
            setBody(nodes)
        }

    suspend fun stopRecording() = client.post("$baseUrl$WS_STOP_RECORDING")
}
