package music.jiminy.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import music.jiminy.JiminyCommand
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.LinkType
import music.jiminy.SaveConfigOptions
import music.jiminy.service.JiminyResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {

    private lateinit var scope: CoroutineScope
    private lateinit var mainService: FakeMainService
    private lateinit var logger: FakeLogger
    private lateinit var viewModel: ConnectionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        scope = CoroutineScope(SupervisorJob() + testDispatcher)
        logger = FakeLogger()
        mainService = FakeMainService(logger)
        viewModel = ConnectionViewModel(
            mainService = mainService,
            logger = logger,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSaveConfiguration() = runTest {
        val instrument = JiminyDevice("Instrument", JiminyDeviceType.Audio)
        val speaker = JiminyDevice("Speaker", JiminyDeviceType.Audio)
        val links = listOf(JiminyLink(listOf(instrument), speaker))

        viewModel.saveConfiguration(links, emptyList(), SaveConfigOptions(name = "MyConfig"))
        advanceUntilIdle()

        assertEquals(1, mainService.mockConfigurations.size)
        assertEquals("MyConfig", mainService.mockConfigurations.first().name)
        assertTrue(!viewModel.showSaveConfigPopup.value)
    }

    @Test
    fun testSaveConfigurationWithOverwrite() = runTest {
        // Initial setup with one config
        mainService.mockConfigurations.add(
            JiminyConfiguration(
                "Existing",
                emptyList(),
                emptyList(),
            )
        )

        // Fetch configs to populate state
        viewModel.onSaveConfigClick()
        viewModel.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Success }

        val instrument = JiminyDevice("Instrument", JiminyDeviceType.Audio)
        val speaker = JiminyDevice("Speaker", JiminyDeviceType.Audio)
        val links = listOf(JiminyLink(listOf(instrument), speaker))

        // Try to save with existing name
        viewModel.saveConfiguration(links, emptyList(), SaveConfigOptions(name = "Existing"))
        advanceUntilIdle()

        // Verify overwrite popup shown and save popup dismissed
        val data = viewModel.showOverwriteConfigPopup.value
        assertEquals("Existing", data?.options?.name)
        assertTrue(!viewModel.showSaveConfigPopup.value)

        // Confirm overwrite
        viewModel.confirmOverwrite(data!!)
        advanceUntilIdle()

        // Verify saved and popup dismissed
        assertEquals(1, mainService.mockConfigurations.size)
        assertEquals("Existing", mainService.mockConfigurations.first().name)
        assertTrue(viewModel.showOverwriteConfigPopup.value == null)
    }

    @Test
    fun testOnSaveConfigClickTransitions() = runTest {
        viewModel.onSaveConfigClick()

        // Verify Loading state and popup shown
        assertTrue(viewModel.showSaveConfigPopup.value)
        assertEquals(
            ConnectionViewModel.LoadConfigState.Loading,
            viewModel.configurationsState.value,
        )

        advanceUntilIdle()
        // Wait for coroutine to finish (Success state)
        val state =
            viewModel.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Success }
        assertTrue(state is ConnectionViewModel.LoadConfigState.Success)
    }

    @Test
    fun testOnLoadConfigClickTransitions() = runTest {
        mainService.mockConfigurations.add(JiminyConfiguration("Config1", emptyList(), emptyList()))

        viewModel.onLoadConfigClick()

        // Verify Loading state and popup shown
        assertTrue(viewModel.showLoadConfigPopup.value)
        assertEquals(
            ConnectionViewModel.LoadConfigState.Loading,
            viewModel.configurationsState.value,
        )

        advanceUntilIdle()
        // Wait for coroutine to finish (Success state)
        val state =
            viewModel.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Success }
        assertTrue(state is ConnectionViewModel.LoadConfigState.Success)
        assertEquals(1, state.configurations.size)
        assertEquals("Config1", state.configurations.first())
    }

    @Test
    fun testLoadConfiguration() = runTest {
        val config = JiminyConfiguration(
            "Config1",
            listOf(JiminyCommand.Link("inst", "spk", LinkType.Connect)),
            emptyList(),
        )
        mainService.mockConfigurations.add(config)
        viewModel.onLoadConfigClick() // To show the popup

        viewModel.loadConfigurations(listOf("Config1"))
        advanceUntilIdle()

        // Verify popup dismissed
        assertTrue(!viewModel.showLoadConfigPopup.value)
    }

    @Test
    fun testDeleteConfiguration() = runTest {
        mainService.mockConfigurations.add(
            JiminyConfiguration(
                "ToDelete",
                emptyList(),
                emptyList(),
            )
        )
        viewModel.onLoadConfigClick()
        advanceUntilIdle()

        viewModel.deleteConfigurations(listOf("ToDelete"))
        advanceUntilIdle()

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
                onError: (JiminyResponse) -> Unit,
            ) {
                onError(JiminyResponse.Error("Fetch failed"))
            }
        }
        val vm = ConnectionViewModel(failingService, logger)

        vm.onLoadConfigClick()
        advanceUntilIdle()

        val state = vm.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Error }
        assertTrue(state is ConnectionViewModel.LoadConfigState.Error)
        assertEquals("Fetch failed", state.message)
    }

    @Test
    fun testPartialSaveConfiguration() = runTest {
        // 1. Initial configuration with both Audio and MIDI links
        val audioLink = JiminyCommand.Link("audio_inst", "audio_spk", LinkType.Connect)
        val midiLink = JiminyCommand.Link("midi_inst", "midi_spk", LinkType.Connect)
        mainService.mockConfigurations.add(
            JiminyConfiguration(
                "Config",
                listOf(audioLink),
                listOf(midiLink),
            )
        )

        // 2. Setup audio and midi devices in fake service so we can identify them
        val audioDevice = JiminyDevice("AudioDev", JiminyDeviceType.Audio).apply {
            addNode(
                music.jiminy.JiminyDeviceNode(
                    "audio_inst",
                    "AudioDev",
                    "Port1",
                    music.jiminy.JiminyDeviceNodeType.Instrument,
                )
            )
            addNode(
                music.jiminy.JiminyDeviceNode(
                    "audio_spk",
                    "AudioDev",
                    "Port2",
                    music.jiminy.JiminyDeviceNodeType.Speaker,
                )
            )
        }
        val midiDevice = JiminyDevice("MidiDev", JiminyDeviceType.Midi).apply {
            addNode(
                music.jiminy.JiminyDeviceNode(
                    "midi_inst",
                    "MidiDev",
                    "Port1",
                    music.jiminy.JiminyDeviceNodeType.Instrument,
                )
            )
            addNode(
                music.jiminy.JiminyDeviceNode(
                    "midi_spk",
                    "MidiDev",
                    "Port2",
                    music.jiminy.JiminyDeviceNodeType.Speaker,
                )
            )
        }
        mainService.setAudioDevices(listOf(audioDevice))
        mainService.setMidiDevices(listOf(midiDevice))

        // 3. Prepare new MIDI links to save
        val newMidiDevice = JiminyDevice("NewMidiDev", JiminyDeviceType.Midi).apply {
            addNode(
                music.jiminy.JiminyDeviceNode(
                    "new_midi_inst",
                    "NewMidiDev",
                    "Port1",
                    music.jiminy.JiminyDeviceNodeType.Instrument,
                )
            )
            addNode(
                music.jiminy.JiminyDeviceNode(
                    "new_midi_spk",
                    "NewMidiDev",
                    "Port2",
                    music.jiminy.JiminyDeviceNodeType.Speaker,
                )
            )
        }
        val newMidiLink = JiminyLink(listOf(newMidiDevice), newMidiDevice)

        // Fetch configs to populate state
        viewModel.onSaveConfigClick()
        advanceUntilIdle()
        viewModel.configurationsState.first { it is ConnectionViewModel.LoadConfigState.Success }

        // 4. Save ONLY MIDI section
        viewModel.saveConfiguration(
            emptyList(),
            listOf(newMidiLink),
            SaveConfigOptions(name = "Config", saveAudio = false, saveMidi = true),
        )
        advanceUntilIdle()
        val data = viewModel.showOverwriteConfigPopup.value
        viewModel.confirmOverwrite(data!!)
        advanceUntilIdle()

        // 5. Verify result: Audio link remains, MIDI link is replaced
        val savedConfig = mainService.mockConfigurations.first { it.name == "Config" }

        assertEquals(1, savedConfig.audioLinks.size)
        assertEquals(1, savedConfig.midiLinks.size)
        assertTrue(savedConfig.audioLinks.any { it.instrument == "audio_inst" }) // Kept
        assertTrue(savedConfig.midiLinks.any { it.instrument == "new_midi_inst" }) // New one
        assertTrue(savedConfig.midiLinks.none { it.instrument == "midi_inst" }) // Old MIDI removed
    }

    @Test
    fun testSaveAndLoadVolumes() = runTest {
        // 1. Setup device with volume
        val volume = music.jiminy.JiminyVolume(
            "vol1",
            0.7f,
            music.jiminy.JiminyDeviceNodeType.Instrument,
            false,
        )
        val device = JiminyDevice("Dev", JiminyDeviceType.Audio).apply {
            addVolume(volume)
        }
        mainService.setAudioDevices(listOf(device))

        // 2. Save configuration with volumes
        viewModel.saveConfiguration(
            emptyList(),
            emptyList(),
            SaveConfigOptions(name = "VolConfig", saveVolumes = true),
        )
        advanceUntilIdle()

        // 3. Verify volumes are saved
        val savedConfig = mainService.mockConfigurations.first { it.name == "VolConfig" }
        assertEquals(1, savedConfig.volumes.size)
        assertEquals(0.7f, savedConfig.volumes.first().volume)
        assertEquals("vol1", savedConfig.volumes.first().id)

        // 4. Start collecting before loading to avoid missing emission
        val deferredCommand = kotlinx.coroutines.CompletableDeferred<JiminyCommand>()
        val job = backgroundScope.launch {
            deferredCommand.complete(mainService.succeededCommands.first())
        }

        // 5. Load configuration
        viewModel.loadConfigurations(listOf("VolConfig"))
        advanceUntilIdle()

        // 6. Verify batch command sent with volume update
        val command = deferredCommand.await()
        assertTrue(command is JiminyCommand.Batch)
        assertEquals(2, command.commands.size) // 1 VolumeUpdate + 1 MuteUpdate
        assertTrue(command.commands.any { it is JiminyCommand.VolumeUpdate && it.volume == 0.7f })
        assertTrue(command.commands.any { it is JiminyCommand.MuteUpdate && !it.muteState })
        job.cancel()
    }
}
