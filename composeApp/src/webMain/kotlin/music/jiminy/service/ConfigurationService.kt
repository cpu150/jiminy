package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import music.jiminy.JiminyConfiguration
import music.jiminy.WS_CONFIGURATIONS

class ConfigurationService(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getConfigurations(): List<String> = client
        .get("$baseUrl$WS_CONFIGURATIONS")
        .body()

    suspend fun getConfiguration(name: String): JiminyConfiguration = client
        .get("$baseUrl$WS_CONFIGURATIONS/$name")
        .body()

    suspend fun saveConfiguration(config: JiminyConfiguration) = client
        .post("$baseUrl$WS_CONFIGURATIONS") {
            contentType(ContentType.Application.Json)
            setBody(config)
        }

    suspend fun deleteConfiguration(name: String) = client
        .delete("$baseUrl$WS_CONFIGURATIONS/$name")
}
