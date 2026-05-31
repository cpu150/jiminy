package music.jiminy.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import music.jiminy.DEVICE_CARD_HEIGHT
import music.jiminy.DEVICE_CARD_INSTRUMENTS_COLOR
import music.jiminy.DEVICE_CARD_SPEAKERS_COLOR
import music.jiminy.DEVICE_LIST_CARD_HEIGHT
import music.jiminy.JiminyAudioDevice
import music.jiminy.JiminyDeviceI
import music.jiminy.JiminyDeviceNodeI
import music.jiminy.JiminyDeviceNodeType
import music.jiminy.JiminyLink
import music.jiminy.NodeConnection
import music.jiminy.disconnectionNodesList
import music.jiminy.screen.ConnectionScreenAction.OnAddRowClick
import music.jiminy.screen.ConnectionScreenAction.OnConfirmUnlinkAll
import music.jiminy.screen.ConnectionScreenAction.OnConnectClick
import music.jiminy.screen.ConnectionScreenAction.OnDeleteDeviceFromRow
import music.jiminy.screen.ConnectionScreenAction.OnDeleteNodeFromRow
import music.jiminy.screen.ConnectionScreenAction.OnDeleteRowClick
import music.jiminy.screen.ConnectionScreenAction.OnDeviceDrag
import music.jiminy.screen.ConnectionScreenAction.OnDeviceDragEnd
import music.jiminy.screen.ConnectionScreenAction.OnDeviceDragStart
import music.jiminy.screen.ConnectionScreenAction.OnDisconnectClick
import music.jiminy.screen.ConnectionScreenAction.OnDismissAddDevicePopup
import music.jiminy.screen.ConnectionScreenAction.OnDismissDeleteAllAlert
import music.jiminy.screen.ConnectionScreenAction.OnDismissError
import music.jiminy.screen.ConnectionScreenAction.OnDismissIncompletePopup
import music.jiminy.screen.ConnectionScreenAction.OnNodesSelected
import music.jiminy.screen.ConnectionScreenAction.OnUnlinkAllClick
import music.jiminy.screen.common.ConnectionScreenDragListener
import music.jiminy.screen.common.ConnectionScreenZoneItem
import music.jiminy.screen.common.DeleteConfirmationAlert
import music.jiminy.screen.common.DeviceCard
import music.jiminy.screen.common.DeviceCardNodeDetails
import music.jiminy.screen.common.DraggableDeviceCard
import music.jiminy.screen.common.DraggableScreen
import music.jiminy.screen.common.ErrorAlert
import music.jiminy.screen.common.GenericMessageAlert
import music.jiminy.screen.common.IncompleteRowsAlert
import music.jiminy.screen.common.JiminyButton
import music.jiminy.screen.common.NodeSelectionAlert
import music.jiminy.screen.common.TextButton
import music.jiminy.screen.common.TextHeadline
import music.jiminy.screen.common.UnlinkConfirmationAlert
import music.jiminy.screen.common.nodes
import music.jiminy.viewmodel.ConnectionScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

enum class ConnectionScreenNodeType {
    Speaker,
    Instrument,
}

@Stable
data class ConnectionScreenState<T : JiminyDeviceI<T>>(
    val devices: List<T> = emptyList(),
    val links: List<JiminyLink<T>> = emptyList(),
    val connectionRows: List<Pair<ConnectionScreenZoneItem<T>, ConnectionScreenZoneItem<T>>> = listOf(
        ConnectionScreenZoneItem<T>(ConnectionScreenNodeType.Instrument) to
                ConnectionScreenZoneItem(ConnectionScreenNodeType.Speaker),
    ),
    val activeDraggingDevice: T? = null,
    val dragOffset: Offset = Offset.Zero,
    val showAddDevicePopup: Boolean = false,
    val showIncompletePopup: Boolean = false,
    val showError: String? = null,
    val lastDropItem: Pair<ConnectionScreenZoneItem<T>, T>? = null,
    val showDeleteAllAlert: Boolean = false,
)

sealed interface ConnectionScreenAction<T : JiminyDeviceI<T>> {
    data class OnDeviceDragStart<T : JiminyDeviceI<T>>(val device: T, val initialOffset: Offset) :
        ConnectionScreenAction<T>

    data class OnDeviceDrag<T : JiminyDeviceI<T>>(val newOffset: Offset) : ConnectionScreenAction<T>
    data class OnDeviceDragEnd<T : JiminyDeviceI<T>>(val finalOffset: Offset) :
        ConnectionScreenAction<T>

    class OnAddRowClick<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    data class OnDeleteRowClick<T : JiminyDeviceI<T>>(val row: Pair<ConnectionScreenZoneItem<T>, ConnectionScreenZoneItem<T>>) :
        ConnectionScreenAction<T>

    class OnConnectClick<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    data class OnDisconnectClick<T : JiminyDeviceI<T>>(val nodes: List<NodeConnection>) :
        ConnectionScreenAction<T>

    class OnUnlinkAllClick<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    class OnConfirmUnlinkAll<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    class OnDismissError<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    class OnDismissIncompletePopup<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    class OnDismissAddDevicePopup<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    class OnDismissDeleteAllAlert<T : JiminyDeviceI<T>> : ConnectionScreenAction<T>
    data class OnNodesSelected<T : JiminyDeviceI<T>>(
        val zone: ConnectionScreenZoneItem<T>,
        val nodes: List<JiminyDeviceNodeI>,
    ) : ConnectionScreenAction<T>

    data class OnDeleteNodeFromRow<T : JiminyDeviceI<T>>(
        val zone: ConnectionScreenZoneItem<T>,
        val node: JiminyDeviceNodeI,
    ) :
        ConnectionScreenAction<T>

    data class OnDeleteDeviceFromRow<T : JiminyDeviceI<T>>(
        val zone: ConnectionScreenZoneItem<T>,
        val device: T,
    ) :
        ConnectionScreenAction<T>
}

fun <T : JiminyDeviceI<T>> Pair<ConnectionScreenZoneItem<T>, ConnectionScreenZoneItem<T>>.speakers() =
    if (first.type == ConnectionScreenNodeType.Speaker) first else second

fun <T : JiminyDeviceI<T>> Pair<ConnectionScreenZoneItem<T>, ConnectionScreenZoneItem<T>>.instruments() =
    if (first.type == ConnectionScreenNodeType.Instrument) first else second

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
) {
    ConnectionRoot(modifier)
}

@Composable
fun ConnectionRoot(
    modifier: Modifier = Modifier,
    viewModel: ConnectionScreenViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadData() }

    DraggableScreen(
        draggableItem = { item: JiminyAudioDevice ->
            val scale = 1.05f
            DeviceCard(
                modifier = Modifier
                    .height((DEVICE_CARD_HEIGHT * scale).dp)
                    .width((DEVICE_CARD_HEIGHT * scale).dp),
                device = { item },
            )
        },
        modifier = modifier,
    ) { activeDraggingItem, offset, containerPosition ->
        MainConnectionScreen(
            state = state,
            onAction = viewModel::onAction,
            activeDraggingItem = activeDraggingItem,
            dragOffset = offset,
            containerPosition = containerPosition,
        )
    }
}

@Composable
fun <T : JiminyDeviceI<T>> MainConnectionScreen(
    state: ConnectionScreenState<T>,
    onAction: (ConnectionScreenAction<T>) -> Unit,
    activeDraggingItem: MutableState<T?>,
    dragOffset: MutableState<Offset>,
    containerPosition: Offset,
) {
    val listener = remember(onAction, containerPosition) {
        object : ConnectionScreenDragListener<T> {
            override fun deviceBeingDragged() = activeDraggingItem.value

            override fun onDeviceDragStart(device: T, initialOffset: Offset) {
                activeDraggingItem.value = device
                dragOffset.value = initialOffset
                onAction(OnDeviceDragStart(device, initialOffset))
            }

            override fun onDeviceDrag(newOffset: Offset) {
                dragOffset.value = newOffset
                onAction(OnDeviceDrag(newOffset))
            }

            override fun onDeviceDragEnd(finalOffset: Offset) {
                onAction(OnDeviceDragEnd(finalOffset))
                activeDraggingItem.value = null
            }

            override fun getContainerPosition() = containerPosition
        }
    }

    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(DEVICE_CARD_HEIGHT.dp + 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            items(state.devices) { device ->
                DraggableDeviceCard(listener) { device }
            }
        }

        if (state.connectionRows.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextHeadline(
                    modifier = Modifier.weight(1f),
                    text = "INSTRUMENTS",
                    textAlign = TextAlign.Center,
                    color = Color(DEVICE_CARD_INSTRUMENTS_COLOR),
                )
                TextHeadline(
                    modifier = Modifier.weight(1f),
                    text = "SPEAKERS",
                    textAlign = TextAlign.Center,
                    color = Color(DEVICE_CARD_SPEAKERS_COLOR),
                )
            }
        }

        state.connectionRows.forEachIndexed { index, row ->
            ConnectionRow(
                index = index,
                row = { row },
                deleteRow = { onAction(OnDeleteRowClick(row)) },
                onDeleteNode = { zone, node -> onAction(OnDeleteNodeFromRow(zone, node)) },
                onDeleteDevice = { zone, device -> onAction(OnDeleteDeviceFromRow(zone, device)) },
            )
            Spacer(Modifier.height(6.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JiminyButton(onClick = { onAction(OnAddRowClick()) }) {
                TextButton("Add Row")
            }
            JiminyButton(onClick = { onAction(OnConnectClick()) }) {
                TextButton("Link")
            }
            JiminyButton(onClick = { onAction(OnUnlinkAllClick()) }) {
                TextButton("Unlink All")
            }
            JiminyButton(onClick = { /* TODO */ }) {
                TextButton("Save Config")
            }
        }
        Spacer(Modifier.height(12.dp))

        state.links.forEach { link ->
            LinkRow(
                link = { link },
                onDisconnectRequest = { onAction(OnDisconnectClick(it)) },
            )
        }

        Spacer(Modifier.height(6.dp))
    }

    // Alerts
    state.showError?.let { errorMessage ->
        ErrorAlert(errorMessage, { onAction(OnDismissError()) })
    }

    state.lastDropItem?.takeIf { state.showAddDevicePopup }?.let { (zone, device) ->
        NodeSelectionAlert(
            onDismiss = { onAction(OnDismissAddDevicePopup()) },
            droppedDevice = { device },
            zoneItem = { zone },
            addNodes = { nodes -> onAction(OnNodesSelected(zone, nodes)) },
        )
    }

    if (state.showIncompletePopup) {
        IncompleteRowsAlert({ onAction(OnDismissIncompletePopup()) })
    }

    if (state.showDeleteAllAlert) {
        GenericMessageAlert(
            title = "Unlink All Links?",
            onDismiss = { onAction(OnDismissDeleteAllAlert()) },
            onConfirm = { onAction(OnConfirmUnlinkAll()) },
            confirmLabel = "Unlink",
        )
    }
}

@Composable
fun <T : JiminyDeviceI<T>> ConnectionRow(
    index: Int,
    row: () -> Pair<ConnectionScreenZoneItem<T>, ConnectionScreenZoneItem<T>>,
    deleteRow: () -> Unit,
    onDeleteNode: (ConnectionScreenZoneItem<T>, JiminyDeviceNodeI) -> Unit,
    onDeleteDevice: (ConnectionScreenZoneItem<T>, T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val instruments = row().instruments()
    val speakers = row().speakers()
    var nodeToDelete by remember { mutableStateOf<JiminyDeviceNodeI?>(null) }
    var showDeleteRowAlert by remember { mutableStateOf(false) }
    var deviceToDelete by remember {
        mutableStateOf<Pair<ConnectionScreenZoneItem<T>, T>?>(null)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colModifier: @Composable (Color) -> Modifier = { color ->
            Modifier
                .defaultMinSize(minHeight = DEVICE_LIST_CARD_HEIGHT.dp)
                .fillMaxHeight()
                .weight(1f)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(8.dp),
                )
                .border(1.dp, color, RoundedCornerShape(8.dp))
                .padding(4.dp)
        }

        Column(
            modifier = colModifier(Color(DEVICE_CARD_INSTRUMENTS_COLOR))
                .onGloballyPositioned { coord ->
                    instruments.zone.value = coord.boundsInWindow()
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            instruments.devices.forEach { instrument ->
                val nodes = remember(instruments.nodes()) {
                    instrument.nodes().toMutableList() // Simplified for now
                }

                DeviceCardNodeDetails(
                    device = { instrument },
                    deviceNodes = { nodes },
                    onDeviceClick = { device -> deviceToDelete = instruments to device },
                    onNodeClick = { node -> nodeToDelete = node },
                )
            }
        }

        Column(
            modifier = colModifier(Color(DEVICE_CARD_SPEAKERS_COLOR))
                .onGloballyPositioned { coord -> speakers.zone.value = coord.boundsInWindow() },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            speakers.devices.forEach { speaker ->
                val nodes = remember(speakers.nodes()) {
                    speaker.nodes().toMutableList()
                }

                DeviceCardNodeDetails(
                    device = { speaker },
                    deviceNodes = { nodes },
                    onDeviceClick = { device -> deviceToDelete = speakers to device },
                    onNodeClick = { node -> nodeToDelete = node },
                )
            }
        }
    }

    if (index > 0) {
        IconButton({ showDeleteRowAlert = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onBackground)
        }
    }

    if (showDeleteRowAlert) {
        if (instruments.nodes().isEmpty() && speakers.nodes().isEmpty()) {
            deleteRow()
        } else {
            DeleteConfirmationAlert(
                "the row",
                onDismiss = { showDeleteRowAlert = false },
                onConfirm = deleteRow,
            )
        }
    }

    nodeToDelete?.let { node ->
        DeleteConfirmationAlert(node.displayPortName, onDismiss = { nodeToDelete = null }) {
            val zone = if (node.type == JiminyDeviceNodeType.Speaker) speakers else instruments
            onDeleteNode(zone, node)
        }
    }

    deviceToDelete?.let { (zone, device) ->
        DeleteConfirmationAlert(device.displayName, onDismiss = { deviceToDelete = null }) {
            onDeleteDevice(zone, device)
        }
    }
}

@Composable
fun <T : JiminyDeviceI<T>> LinkRow(
    link: () -> JiminyLink<T>,
    onDisconnectRequest: (List<NodeConnection>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkValue = link()
    val instrumentDevs = linkValue.instrumentDevices
    val speakerDev = linkValue.speakerDevice
    var showConfirmationAlert by remember {
        mutableStateOf<Pair<T, JiminyDeviceNodeI?>?>(null)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 2.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colModifier = Modifier
            .wrapContentHeight()
            .weight(1f)
            .padding(4.dp)

        Column(
            modifier = colModifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            instrumentDevs.forEach { instrumentDev ->
                DeviceCardNodeDetails(
                    device = { instrumentDev },
                    deviceNodes = { instrumentDev.instruments },
                    onDeviceClick = { showConfirmationAlert = instrumentDev to null },
                    onNodeClick = { showConfirmationAlert = instrumentDev to it },
                )
            }
        }

        Column(
            modifier = colModifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DeviceCardNodeDetails(
                device = { speakerDev },
                deviceNodes = { speakerDev.speakers },
                onDeviceClick = { showConfirmationAlert = speakerDev to null },
                onNodeClick = { showConfirmationAlert = speakerDev to it },
            )
        }

        showConfirmationAlert?.let { (dev, node) ->
            UnlinkConfirmationAlert(
                pair = { dev to node },
                onDismiss = { showConfirmationAlert = null },
                onConfirm = { onDisconnectRequest(linkValue.disconnectionNodesList(dev, node)) },
            )
        }
    }
}
