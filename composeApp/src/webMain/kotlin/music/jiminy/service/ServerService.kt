package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import music.jiminy.LogEntry
import music.jiminy.WS_FLUSH_SERVER_LOGS
import music.jiminy.WS_REBOOT
import music.jiminy.WS_SERVER_LOGS
import music.jiminy.WS_SHUTDOWN
import music.jiminy.WS_UPDATE

class ServerService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getServerLogs(): List<LogEntry> = client.get("$baseUrl$WS_SERVER_LOGS").body()

    suspend fun flushServerLogs() = client.post("$baseUrl$WS_FLUSH_SERVER_LOGS")

    suspend fun shutdown() = client.post("$baseUrl$WS_SHUTDOWN")

    suspend fun reboot() = client.post("$baseUrl$WS_REBOOT")

    suspend fun updateServer() = client.post("$baseUrl$WS_UPDATE")
}
