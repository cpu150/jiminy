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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.jiminy.DEVICE_CARD_HEIGHT
import music.jiminy.DEVICE_CARD_INSTRUMENTS_COLOR
import music.jiminy.DEVICE_CARD_SPEAKERS_COLOR
import music.jiminy.DEVICE_LIST_CARD_HEIGHT
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType
import music.jiminy.JiminyLink
import music.jiminy.disconnectionNodesList
import music.jiminy.screen.ConnectionScreenNodeType.Instrument
import music.jiminy.screen.ConnectionScreenNodeType.Speaker
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
import music.jiminy.screen.common.addNodes
import music.jiminy.screen.common.instruments
import music.jiminy.screen.common.isCompleted
import music.jiminy.screen.common.nodes
import music.jiminy.screen.common.removeNode
import music.jiminy.screen.common.speakers

enum class ConnectionScreenNodeType {
    Speaker,
    Instrument,
}

fun Pair<JiminyDeviceNode, JiminyDeviceNode>.speakers() =
    if (first.type == JiminyDeviceNodeType.Speaker) first else second

fun Pair<JiminyDeviceNode, JiminyDeviceNode>.instruments() =
    if (first.type == JiminyDeviceNodeType.Instrument) first else second

@Composable
fun ConnectionScreen(
    devices: () -> List<JiminyDevice>,
    links: () -> List<JiminyLink>,
    connect: (List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) -> Unit,
    disconnect: (List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    DraggableScreen(
        draggableItem = { item -> DeviceCard { item } },
        modifier = modifier,
    ) { activeDraggingItem, offset ->
        MainScreen(
            getActiveDraggingDevice = { activeDraggingItem },
            offset = offset,
            devices = devices,
            connect = connect,
            links = links,
            disconnect = disconnect,
        )
    }
}

@Composable
fun MainScreen(
    getActiveDraggingDevice: () -> MutableState<JiminyDevice?>,
    offset: MutableState<Offset>,
    devices: () -> List<JiminyDevice>,
    links: () -> List<JiminyLink>,
    connect: (List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) -> Unit,
    disconnect: (List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeDraggingDevice by getActiveDraggingDevice()

    val connectionRows = remember {
        mutableStateListOf(ConnectionScreenZoneItem(Instrument) to ConnectionScreenZoneItem(Speaker))
    }
    var showAddDevicePopup by remember { mutableStateOf(false) }
    var showIncompletePopup by remember { mutableStateOf(false) }
    var showError: String? by remember { mutableStateOf(null) }
    var lastDropItem
            by remember { mutableStateOf<Pair<ConnectionScreenZoneItem, JiminyDevice>?>(null) }

    val listener = object : ConnectionScreenDragListener {
        override fun deviceBeingDragged() = activeDraggingDevice

        override fun onDeviceDragStart(device: JiminyDevice, initialOffset: Offset) {
            getActiveDraggingDevice().value = device
            offset.value = initialOffset
        }

        override fun onDeviceDrag(newOffset: Offset) {
            offset.value = newOffset
        }

        override fun onDeviceDragEnd(finalOffset: Offset) {
            val bufferOffset = 10f

            // Logic to check if drop was into Instruments or Speakers zones
            connectionRows.find {
                val instruments = it.instruments()
                val speakers = it.speakers()
                instruments.zone.value.inflate(bufferOffset).contains(finalOffset) ||
                        speakers.zone.value.inflate(bufferOffset).contains(finalOffset)
            }?.let {
                val instruments = it.instruments()
                val droppedInInstrumentsZone =
                    instruments.zone.value.inflate(bufferOffset).contains(finalOffset)
                val hasInstruments = activeDraggingDevice?.instruments?.isNotEmpty() == true
                val speakers = it.speakers()
                val hasSpeakers = activeDraggingDevice?.speakers?.isNotEmpty() == true
                val typeStr = if (droppedInInstrumentsZone) "Instruments" else "Speakers"

                if (droppedInInstrumentsZone && hasInstruments) {
                    instruments
                } else if (hasSpeakers) {
                    speakers
                } else {
                    showError = "No $typeStr for \"${activeDraggingDevice?.displayName}\""
                    null
                }
            }?.also { items ->
                activeDraggingDevice?.also { device -> lastDropItem = items to device }
                showAddDevicePopup = true
            }

            getActiveDraggingDevice().value = null
        }
    }

    val createNewRow: () -> Unit = {
        val lastLineComplete = connectionRows.lastOrNull()?.isCompleted() ?: true

        if (lastLineComplete) {
            connectionRows
                .add(ConnectionScreenZoneItem(Instrument) to ConnectionScreenZoneItem(Speaker))
        } else {
            showIncompletePopup = true
        }
    }

    val connecting: () -> Unit = {
        val incompletedRow = connectionRows.find { !it.isCompleted() }
        val allLineComplete = incompletedRow == null && connectionRows.isNotEmpty()

        if (allLineComplete) {
            val connections = mutableListOf<Pair<JiminyDeviceNode, JiminyDeviceNode>>()

            connectionRows.forEach { row ->
                row.speakers().nodes().forEach { speaker ->
                    row.instruments().nodes().forEach { instrument ->
                        connections += speaker to instrument
                    }
                }
            }

            if (connections.isNotEmpty()) {
                connect(connections)

                connectionRows.clear()
                connectionRows
                    .add(ConnectionScreenZoneItem(Instrument) to ConnectionScreenZoneItem(Speaker))
            }
        } else {
            showIncompletePopup = true
        }
    }

    MainConnectionScreen(
        deviceList = devices,
        links = links,
        dragListener = listener,
        getConnectionRows = { connectionRows },
        newConnectionRow = createNewRow,
        deleteConnectionRow = { if (connectionRows.count() > 1) connectionRows.remove(it) },
        disconnect = disconnect,
        connect = connecting,
        modifier = modifier,
    )

    showError?.let { errorMessage ->
        ErrorAlert(errorMessage, { showError = null })
    }

    lastDropItem?.takeIf { showAddDevicePopup }?.let { (zone, device) ->
        NodeSelectionAlert(
            onDismiss = { showAddDevicePopup = false },
            droppedDevice = { device },
            zoneItem = { zone },
            addNodes = { nodes -> zone.addNodes(nodes) },
        )
    }

    if (showIncompletePopup) {
        IncompleteRowsAlert({ showIncompletePopup = false })
    }
}

@Composable
fun MainConnectionScreen(
    deviceList: () -> List<JiminyDevice>,
    links: () -> List<JiminyLink>,
    getConnectionRows: () -> List<Pair<ConnectionScreenZoneItem, ConnectionScreenZoneItem>>,
    newConnectionRow: () -> Unit,
    deleteConnectionRow: (Pair<ConnectionScreenZoneItem, ConnectionScreenZoneItem>) -> Unit,
    connect: () -> Unit,
    disconnect: (List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) -> Unit,
    dragListener: ConnectionScreenDragListener,
    modifier: Modifier = Modifier,
) {
    val rows by remember { mutableStateOf(getConnectionRows()) }
    var showDeleteAllAlert by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(DEVICE_CARD_HEIGHT.dp + 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            items(deviceList()) { device ->
                DraggableDeviceCard(dragListener) { device }
            }
        }

        if (rows.count() > 0) {
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

        rows.forEachIndexed { index, row ->
            ConnectionRow(
                index = index,
                row = { row },
                deleteRow = { deleteConnectionRow(row) },
            )
            Spacer(Modifier.height(6.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JiminyButton(onClick = { newConnectionRow() }) {
                TextButton("Add Row")
            }
            JiminyButton(onClick = connect) {
                TextButton("Connect")
            }
        }
        Spacer(Modifier.height(12.dp))

        links().forEach { link ->
            LinkRow(
                link = { link },
                onClick = { (dev, node) -> disconnect(link.disconnectionNodesList(dev, node)) },
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JiminyButton(onClick = { showDeleteAllAlert = true }) {
                TextButton("Unlink All")
            }
            JiminyButton(onClick = { /* TODO */ }) {
                TextButton("Save Config")
            }
        }
    }

    if (showDeleteAllAlert) {
        GenericMessageAlert(
            title = "Unlink All Links?",
            onDismiss = { showDeleteAllAlert = false },
            onConfirm = {
                buildList {
                    links().forEach { link ->
                        addAll(link.disconnectionNodesList(link.speakerDevice))
                    }
                }.also { disconnect(it) }
            },
            confirmLabel = "Unlink",
        )
    }
}

@Composable
fun ConnectionRow(
    index: Int,
    row: () -> Pair<ConnectionScreenZoneItem, ConnectionScreenZoneItem>,
    deleteRow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instruments = row().instruments()
    val speakers = row().speakers()
    var nodeToDelete by remember { mutableStateOf<JiminyDeviceNode?>(null) }
    var showDeleteRowAlert by remember { mutableStateOf(false) }
    var deviceToDelete by remember {
        mutableStateOf<Pair<ConnectionScreenZoneItem, JiminyDevice>?>(null)
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
                val nodes =
                    remember(instruments.nodes()) { instrument.nodes().toMutableStateList() }

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
                val nodes = remember(speakers.nodes()) { speaker.nodes().toMutableStateList() }

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
            if (node.type == JiminyDeviceNodeType.Speaker) {
                speakers
            } else {
                instruments
            }.removeNode(node)
        }
    }

    deviceToDelete?.let { (zone, device) ->
        DeleteConfirmationAlert(device.displayName, onDismiss = { deviceToDelete = null }) {
            zone.devices.remove(device)
        }
    }
}

@Composable
fun LinkRow(
    link: () -> JiminyLink,
    onClick: (Pair<JiminyDevice, JiminyDeviceNode?>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val link = link()
    val instrumentDevs = link.instrumentDevices
    val speakerDev = link.speakerDevice
    var showConfirmationAlert by remember {
        mutableStateOf<Pair<JiminyDevice, JiminyDeviceNode?>?>(null)
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

        showConfirmationAlert?.let {
            UnlinkConfirmationAlert(
                pair = { it },
                onDismiss = { showConfirmationAlert = null },
                onConfirm = { onClick(it) },
            )
        }
    }
}
