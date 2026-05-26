package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import music.jiminy.JiminyLoggerI
import music.jiminy.LogEntry
import music.jiminy.WS_SERVER_LOGS

class LoggingService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val logger: JiminyLoggerI,
) {
    suspend fun getServerLogs(): List<LogEntry> = client.get("$baseUrl$WS_SERVER_LOGS").body()
}
