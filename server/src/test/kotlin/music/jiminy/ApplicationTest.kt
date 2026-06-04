package music.jiminy

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testGetDevices() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        val response = client.get(WS_DEVICES)
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        val devices = json.decodeFromString<JiminyDeviceList>(body)
        assertEquals(
            mockController.getDevicesList().instruments.size,
            devices.instruments.size,
        )
    }

    @Test
    fun testGetDeviceLinks() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        val response = client.get(WS_LINK_DEVICES)
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        val links = json.decodeFromString<List<String>>(body)
        assertEquals(
            mockController.getDeviceLinksList().size,
            links.size,
        )
    }

    @Test
    fun testGetRecordings() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        val response = client.get(WS_RECORDINGS)
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        val recordings = json.decodeFromString<List<String>>(body)
        assertEquals(
            mockController.getRecordings().size,
            recordings.size,
        )
    }

    @Test
    fun testServerLogsAndFlush() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        // Add some logs
        debugLogger.info("Test log info")
        debugLogger.error("Test log error")

        // Get logs
        val response = client.get(WS_SERVER_LOGS)
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val logs = json.decodeFromString<List<LogEntry>>(body)
        assertEquals(2, logs.size)

        // Flush logs
        val flushResponse = client.post(WS_FLUSH_SERVER_LOGS)
        assertEquals(HttpStatusCode.OK, flushResponse.status)
        assertEquals("Server logs flushed", flushResponse.bodyAsText())

        // Get logs again
        val emptyResponse = client.get(WS_SERVER_LOGS)
        val emptyBody = emptyResponse.bodyAsText()
        val emptyLogs = json.decodeFromString<List<LogEntry>>(emptyBody)
        assertEquals(0, emptyLogs.size)
    }

    @Test
    fun testDeleteRecordings() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        val response = client.post(WS_DELETE_RECORDINGS) {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    listOf(
                        "session1.wav",
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Files deleted", response.bodyAsText())
    }

    @Test
    fun testDownloadRecordingsEmpty() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        val response = client.post(WS_DOWNLOAD_RECORDINGS) {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    emptyList<String>(),
                ),
            )
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("No files specified", response.bodyAsText())
    }

    @Test
    fun testLinkDevices() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        val response = client.post(WS_LINK_DEVICES) {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    emptyList<JiminyCommand.Link>(),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("All links established", response.bodyAsText())
    }

    @Test
    fun testStartStopRecordingLockedEndpoints() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        // Start recording
        val startResponse = client.post(WS_START_RECORDING) {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    JiminyCommand.StartRecording(
                        emptyList(),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.Locked, startResponse.status)
        assertEquals("Started recording...", startResponse.bodyAsText())
        assertTrue(mockController.isRecording)

        // Try to access get devices endpoint, should be locked
        val lockedResponse = client.get(WS_DEVICES)
        assertEquals(HttpStatusCode.Locked, lockedResponse.status)

        // Stop recording
        val stopResponse = client.post(WS_STOP_RECORDING)
        assertEquals(HttpStatusCode.OK, stopResponse.status)
        assertEquals("Stopped recording", stopResponse.bodyAsText())
        assertTrue(!mockController.isRecording)

        // Now devices should be accessible
        val unlockedResponse = client.get(WS_DEVICES)
        assertEquals(HttpStatusCode.OK, unlockedResponse.status)
    }

    @Test
    fun testConfigurations() = testApplication {
        val mockController = MockController()
        val debugLogger = DebugLogger()
        application {
            module(
                json = json,
                controller = mockController,
                logger = debugLogger,
            )
        }

        // List initial configurations
        val listResponse = client.get(WS_CONFIGURATIONS)
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val configs = json.decodeFromString<List<String>>(listResponse.bodyAsText())
        assertEquals(2, configs.size)

        // Save new configuration
        val newConfig = JiminyConfiguration("New Config", emptyList())
        val saveResponse = client.post(WS_CONFIGURATIONS) {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(newConfig))
        }
        assertEquals(HttpStatusCode.OK, saveResponse.status)
        assertEquals("Configuration saved", saveResponse.bodyAsText())

        // Verify it was added
        val listResponse2 = client.get(WS_CONFIGURATIONS)
        val configs2 = json.decodeFromString<List<String>>(listResponse2.bodyAsText())
        assertTrue(configs2.contains("New Config"))

        // Get specific configuration
        val getResponse = client.get("$WS_CONFIGURATIONS/New%20Config")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val fetchedConfig = json.decodeFromString<JiminyConfiguration>(getResponse.bodyAsText())
        assertEquals("New Config", fetchedConfig.name)

        // Delete configuration
        val deleteResponse = client.delete("$WS_CONFIGURATIONS/New%20Config")
        assertEquals(HttpStatusCode.OK, deleteResponse.status)
        assertEquals("Configuration deleted", deleteResponse.bodyAsText())

        // Verify it was removed
        val listResponse3 = client.get(WS_CONFIGURATIONS)
        val configs3 = json.decodeFromString<List<String>>(listResponse3.bodyAsText())
        assertTrue(!configs3.contains("New Config"))
    }
}
