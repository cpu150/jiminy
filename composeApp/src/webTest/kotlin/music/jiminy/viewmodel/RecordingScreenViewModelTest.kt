package music.jiminy.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType
import music.jiminy.JiminyDeviceType
import music.jiminy.PW_RECORDER_CHANNEL_COUNT
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.screen.RecordingScreenAction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordingScreenViewModelTest {

    private lateinit var scope: CoroutineScope
    private lateinit var mainService: FakeMainService
    private lateinit var logger: FakeLogger
    private lateinit var viewModel: RecordingScreenViewModel

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        logger = FakeLogger()
        mainService = FakeMainService(logger)
        viewModel = RecordingScreenViewModel(
            mainService = mainService,
            logger = logger,
        )
    }

    @Test
    fun testInitialState() = runTest {
        val state = viewModel.state.first()
        assertEquals(0, state.devices.size)
        assertNull(state.showDetails)
        assertEquals(0, state.selectedNodes.size)
        assertFalse(state.showRecordings)
        assertEquals(0, state.selectedRecordings.size)
    }

    @Test
    fun testDeviceCollectionFiltersCorrectly() = runTest {
        val instrumentNode = JiminyDeviceNode(
            fullName = "alsa_input.usb:capture_0",
            deviceName = "USB Audio",
            portName = "capture_0",
            type = JiminyDeviceNodeType.Instrument,
        )
        val audioDevice = JiminyDevice(
            name = "USB Audio",
            type = JiminyDeviceType.Audio,
        ).apply {
            addNode(instrumentNode)
        }

        val pwRecorderDevice = JiminyDevice(
            name = PW_RECORDER_NAME,
            type = JiminyDeviceType.Audio,
        ).apply {
            addNode(instrumentNode)
        }

        mainService.setAudioDevices(listOf(audioDevice, pwRecorderDevice))

        val state = viewModel.state.first { it.devices.isNotEmpty() }
        assertEquals(1, state.devices.size)
        assertEquals("USB Audio", state.devices.first().name)
    }

    @Test
    fun testOnDeviceClickAndDismissDetails() = runTest {
        val device = JiminyDevice("Test Device", JiminyDeviceType.Audio)
        viewModel.onAction(RecordingScreenAction.OnDeviceClick(device))

        var state = viewModel.state.first()
        assertEquals(device, state.showDetails)

        viewModel.onAction(RecordingScreenAction.OnDismissDetails)
        state = viewModel.state.first()
        assertNull(state.showDetails)
    }

    @Test
    fun testNodeSelectionLimit() = runTest {
        val nodes = (1..PW_RECORDER_CHANNEL_COUNT + 1).map { index ->
            JiminyDeviceNode(
                fullName = "node_$index",
                deviceName = "dev",
                portName = "port_$index",
                type = JiminyDeviceNodeType.Instrument,
            )
        }

        nodes.take(PW_RECORDER_CHANNEL_COUNT).forEach { node ->
            viewModel.onAction(RecordingScreenAction.OnNodeClick(node))
        }

        var state = viewModel.state.first()
        assertEquals(PW_RECORDER_CHANNEL_COUNT, state.selectedNodes.size)

        viewModel.onAction(RecordingScreenAction.OnNodeClick(nodes.last()))
        state = viewModel.state.first()
        assertEquals(PW_RECORDER_CHANNEL_COUNT, state.selectedNodes.size)
        assertEquals("No available recording slots", viewModel.errorMessage.value)
    }

    @Test
    fun testStartAndStopRecordingFlow() = runTest {
        val node = JiminyDeviceNode("node", "dev", "port", JiminyDeviceNodeType.Instrument)
        viewModel.onAction(RecordingScreenAction.OnNodeClick(node))

        viewModel.onAction(RecordingScreenAction.OnStartRecording)
        val isRecording = mainService.isRecording.first { it }
        assertTrue(isRecording)

        viewModel.onAction(RecordingScreenAction.OnStopRecording)
        val isNotRecording = mainService.isRecording.first { !it }
        assertFalse(isNotRecording)
    }

    @Test
    fun testRecordingSelectionAndDeletion() = runTest {
        mainService.mockRecordings.addAll(listOf("rec1.wav", "rec2.wav", "rec3.wav"))

        viewModel.onAction(RecordingScreenAction.OnShowRecordingsClick)
        var state = viewModel.state.first { it.showRecordings }
        assertTrue(state.showRecordings)
        assertEquals(3, state.recordings.size)

        viewModel.onAction(RecordingScreenAction.OnRecordingSelect("rec1.wav"))
        viewModel.onAction(RecordingScreenAction.OnRecordingSelect("rec3.wav"))
        state = viewModel.state.first()
        assertEquals(2, state.selectedRecordings.size)
        assertTrue(state.selectedRecordings.containsAll(listOf("rec1.wav", "rec3.wav")))

        viewModel.onAction(RecordingScreenAction.OnDeleteRecordings)
        state = viewModel.state.first { it.recordings.size == 1 }
        assertEquals(0, state.selectedRecordings.size)
        assertEquals(1, state.recordings.size)
        assertEquals("rec2.wav", state.recordings.first())
    }

    @Test
    fun testOnApplyConfiguration() = runTest {
        val node1 = JiminyDeviceNode("node1", "dev1", "port1", JiminyDeviceNodeType.Instrument)
        val node2 = JiminyDeviceNode("node2", "dev2", "port2", JiminyDeviceNodeType.Instrument)
        val config = JiminyConfiguration(
            name = "TestConfig",
            audioLinks = emptyList(),
            midiLinks = emptyList(),
            recordingNodes = listOf(node1, node2)
        )

        viewModel.onAction(RecordingScreenAction.OnApplyConfiguration(config))
        val state = viewModel.state.first()

        assertEquals(2, state.selectedNodes.size)
        assertTrue(state.selectedNodes.containsAll(listOf(node1, node2)))
    }

    @Test
    fun testOnApplyConfigurationRespectsLimit() = runTest {
        val extraNodes = (1..PW_RECORDER_CHANNEL_COUNT + 5).map { index ->
            JiminyDeviceNode("node_$index", "dev", "port", JiminyDeviceNodeType.Instrument)
        }
        val config = JiminyConfiguration(
            name = "OverLimitConfig",
            audioLinks = emptyList(),
            midiLinks = emptyList(),
            recordingNodes = extraNodes
        )

        viewModel.onAction(RecordingScreenAction.OnApplyConfiguration(config))
        val state = viewModel.state.first()

        assertEquals(PW_RECORDER_CHANNEL_COUNT, state.selectedNodes.size)
    }
}
