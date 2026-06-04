package music.jiminy.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import music.jiminy.JiminyCommand
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.LinkType
import music.jiminy.service.JiminyResponse
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionViewModelTest {

    private lateinit var scope: CoroutineScope
    private lateinit var mainService: FakeMainService
    private lateinit var logger: FakeLogger
    private lateinit var viewModel: ConnectionViewModel

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        logger = FakeLogger()
        mainService = FakeMainService(scope, logger)
        viewModel = ConnectionViewModel(
            mainService = mainService,
            logger = logger,
        )
    }

    @Test
    fun testSaveConfiguration() = runTest {
        val instrument = JiminyDevice("Instrument", JiminyDeviceType.Audio)
        val speaker = JiminyDevice("Speaker", JiminyDeviceType.Audio)
        val links = listOf(JiminyLink(listOf(instrument), speaker))

        viewModel.saveConfiguration("MyConfig", links)

        assertEquals(1, mainService.mockConfigurations.size)
        assertEquals("MyConfig", mainService.mockConfigurations.first().name)
        assertTrue(!viewModel.showSaveConfigPopup.value)
    }

    @Test
    fun testOnLoadConfigClickTransitions() = runTest {
        mainService.mockConfigurations.add(JiminyConfiguration("Config1", emptyList()))

        viewModel.onLoadConfigClick()

        // Verify Loading state and popup shown
        assertTrue(viewModel.showLoadConfigPopup.value)
        assertEquals(ConnectionViewModel.LoadConfigState.Loading, viewModel.configurationsState.value)

        // Wait for coroutine to finish (Success state)
        val state = viewModel.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Success }
        assertTrue(state is ConnectionViewModel.LoadConfigState.Success)
        assertEquals(1, state.configurations.size)
        assertEquals("Config1", state.configurations.first())
    }

    @Test
    fun testLoadConfiguration() = runTest {
        val config = JiminyConfiguration(
            "Config1",
            listOf(JiminyCommand.Link("inst", "spk", LinkType.Connect))
        )
        mainService.mockConfigurations.add(config)
        viewModel.onLoadConfigClick() // To show the popup

        viewModel.loadConfiguration("Config1")

        // Verify popup dismissed
        assertTrue(!viewModel.showLoadConfigPopup.value)
    }

    @Test
    fun testDeleteConfiguration() = runTest {
        mainService.mockConfigurations.add(JiminyConfiguration("ToDelete", emptyList()))
        viewModel.onLoadConfigClick()

        viewModel.deleteConfiguration("ToDelete")

        // Wait for list to be refreshed (should be empty now)
        val state = viewModel.configurationsState.first { 
            it is ConnectionViewModel.LoadConfigState.Success && it.configurations.isEmpty() 
        }
        assertTrue(state is ConnectionViewModel.LoadConfigState.Success)
        assertTrue(state.configurations.isEmpty())
        assertTrue(mainService.mockConfigurations.isEmpty())
    }

    @Test
    fun testLoadConfigError() = runTest {
        // We need a way to make the fake service fail
        val failingService = object : FakeMainService() {
            override suspend fun getConfigurations(
                onSuccess: (JiminyResponse.Success<List<String>>) -> Unit,
                onError: (JiminyResponse) -> Unit
            ) {
                onError(JiminyResponse.Error("Fetch failed"))
            }
        }
        val vm = ConnectionViewModel(failingService, logger)

        vm.onLoadConfigClick()

        val state = vm.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Error }
        assertTrue(state is ConnectionViewModel.LoadConfigState.Error)
        assertEquals("Fetch failed", state.message)
    }
}
