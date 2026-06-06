package music.jiminy.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import music.jiminy.LogEntry
import music.jiminy.LogType
import music.jiminy.service.JiminyLogger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogsViewModelTest {

    private lateinit var scope: CoroutineScope
    private lateinit var clientLogger: JiminyLogger
    private lateinit var mainService: FakeMainService
    private lateinit var viewModel: LogsViewModel

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        clientLogger = JiminyLogger()
        mainService = FakeMainService()
        viewModel = LogsViewModel(
            clientLogger = clientLogger,
            mainService = mainService,
        )
    }

    @AfterTest
    fun tearDown() {
        clientLogger.clear()
    }

    @Test
    fun testInitialStateIsEmpty() = runTest {
        val currentLogs = viewModel.logs.first()
        assertEquals(0, currentLogs.size)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun testCombinedLogsSortedByTimestamp() = runTest {
        clientLogger.info("Client Log 1")
        clientLogger.error("Client Log 2")

        mainService.mockServerLogs.add(
            LogEntry(
                type = LogType.INFO,
                timestamp = 1000L,
                message = "Server Log 1",
            ),
        )

        viewModel.loadServerLogs()

        val combinedLogs = viewModel.logs.first { it.size == 3 }
        assertEquals(3, combinedLogs.size)

        assertEquals(LogSource.Server, combinedLogs.last().source)
        assertEquals("Server Log 1", combinedLogs.last().entry.message)
    }

    @Test
    fun testLoadServerLogs() = runTest {
        mainService.mockServerLogs.add(
            LogEntry(
                type = LogType.WARNING,
                timestamp = 2000L,
                message = "Server Warning",
            ),
        )

        viewModel.loadServerLogs()

        val logsList = viewModel.logs.first { it.isNotEmpty() }
        assertEquals(1, logsList.size)
        assertEquals("Server Warning", logsList.first().entry.message)
        assertEquals(LogSource.Server, logsList.first().source)
    }

    @Test
    fun testFlushLogsClearsBothClientAndServer() = runTest {
        clientLogger.info("Client log")
        mainService.mockServerLogs.add(
            LogEntry(
                type = LogType.INFO,
                timestamp = 1500L,
                message = "Server log",
            ),
        )

        viewModel.loadServerLogs()
        viewModel.logs.first { it.size == 2 }

        viewModel.flushLogs()
        viewModel.logs.first { it.isEmpty() }
    }

    @Test
    fun testResetError() {
        viewModel.resetError()
        assertNull(viewModel.errorMessage.value)
    }
}
