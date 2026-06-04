package music.jiminy.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType
import music.jiminy.JiminyDeviceType
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.screen.ConnectionScreenAction
import music.jiminy.screen.common.isEmpty
import music.jiminy.screen.common.nodes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionScreenViewModelTest {

    private lateinit var scope: CoroutineScope
    private lateinit var mainService: FakeMainService
    private lateinit var logger: FakeLogger
    private lateinit var viewModel: ConnectionScreenViewModel

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        logger = FakeLogger()
        mainService = FakeMainService(scope, logger)
        viewModel = ConnectionScreenViewModel(
            mainService = mainService,
            logger = logger,
        )
    }

    @Test
    fun testInitialStateHasOneEmptyRow() = runTest {
        val state = viewModel.state.first()
        assertEquals(0, state.devices.size)
        assertEquals(1, state.connectionRows.size)
        assertTrue(state.connectionRows.first().first.isEmpty())
        assertTrue(state.connectionRows.first().second.isEmpty())
    }

    @Test
    fun testAudioDeviceFiltering() = runTest {
        val speakerNode = JiminyDeviceNode(
            fullName = "alsa_output.usb:playback_0",
            deviceName = "USB Audio",
            portName = "playback_0",
            type = JiminyDeviceNodeType.Speaker,
        )
        val audioDevice = JiminyDevice(
            name = "USB Audio",
            type = JiminyDeviceType.Audio,
        ).apply {
            addNode(speakerNode)
        }

        val pwRecorderDevice = JiminyDevice(
            name = PW_RECORDER_NAME,
            type = JiminyDeviceType.Audio,
        ).apply {
            addNode(speakerNode)
        }

        mainService.setAudioDevices(
            listOf(
                audioDevice,
                pwRecorderDevice,
            ),
        )

        val state = viewModel.state.first { it.devices.isNotEmpty() }
        assertEquals(1, state.devices.size)
        assertEquals("USB Audio", state.devices.first().name)
    }

    @Test
    fun testDragActionStateUpdates() = runTest {
        val device = JiminyDevice("Test Device", JiminyDeviceType.Audio)
        val initialOffset = androidx.compose.ui.geometry.Offset(10f, 20f)

        viewModel.onAction(
            ConnectionScreenAction.OnDeviceDragStart(
                device = device,
                initialOffset = initialOffset,
            ),
        )

        var state = viewModel.state.first()
        assertEquals(device, state.activeDraggingDevice)
        assertEquals(initialOffset, state.dragOffset)

        val dragOffset = androidx.compose.ui.geometry.Offset(30f, 40f)
        viewModel.onAction(ConnectionScreenAction.OnDeviceDrag(dragOffset))

        state = viewModel.state.first()
        assertEquals(dragOffset, state.dragOffset)
    }

    @Test
    fun testEnsureOneEmptyRowEnforcement() = runTest {
        var state = viewModel.state.first()
        assertEquals(1, state.connectionRows.size)

        val node = JiminyDeviceNode(
            fullName = "node_fullName",
            deviceName = "device",
            portName = "port",
            type = JiminyDeviceNodeType.Speaker,
        )

        viewModel.onAction(
            ConnectionScreenAction.OnNodesSelected(
                zone = state.connectionRows.first().second,
                nodes = listOf(node),
            ),
        )

        state = viewModel.state.first { it.connectionRows.size == 2 }
        assertEquals(2, state.connectionRows.size)
        assertEquals(1, state.connectionRows.first().second.nodes().size)
        assertTrue(state.connectionRows.last().first.isEmpty())
        assertTrue(state.connectionRows.last().second.isEmpty())
    }

    @Test
    fun testUnlinkAllAction() = runTest {
        viewModel.onAction(ConnectionScreenAction.OnUnlinkAllClick())
        var state = viewModel.state.first()
        assertTrue(state.showDeleteAllAlert)

        viewModel.onAction(ConnectionScreenAction.OnDismissDeleteAllAlert())
        state = viewModel.state.first()
        assertTrue(!state.showDeleteAllAlert)
    }
}
