package music.jiminy.screen.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.screen.ConnectionScreenNodeType
import music.jiminy.screen.ConnectionScreenNodeType.Instrument
import music.jiminy.screen.ConnectionScreenNodeType.Speaker

interface ConnectionScreenDragListener {
    fun deviceBeingDragged(): JiminyDevice?
    fun onDeviceDragStart(device: JiminyDevice, initialOffset: Offset)
    fun onDeviceDrag(newOffset: Offset)
    fun onDeviceDragEnd(finalOffset: Offset)
}

data class ConnectionScreenZoneItem(
    val type: ConnectionScreenNodeType,
    val zone: MutableState<Rect> = mutableStateOf(Rect.Zero),
    val devices: SnapshotStateList<JiminyDevice> = mutableStateListOf(),
)

fun ConnectionScreenZoneItem.removeNode(node: JiminyDeviceNode) =
    devices.find { it.name == node.deviceName }?.also {
        it.removeNode(node)
        if (it.nodes().isEmpty()) devices.remove(it)
    }

fun ConnectionScreenZoneItem.addNodes(nodes: List<JiminyDeviceNode>) = nodes.forEach { node ->
    (devices.find { it.name == node.deviceName }?.also { devices.remove(it) }
        ?: JiminyDevice(node.deviceName))
        .also { devices.add(it) }
        .takeIf { it.nodes().contains(node).not() }
        ?.addNode(node)
}

fun ConnectionScreenZoneItem.nodes() = devices.flatMap { device ->
    if (type == Speaker) {
        device.speakers
    } else {
        device.instruments
    }
}

fun ConnectionScreenZoneItem.isCompleted() = devices.find {
    if (type == Speaker) {
        it.speakers.isEmpty()
    } else {
        it.instruments.isEmpty()
    }
} == null && devices.isNotEmpty()

fun Pair<ConnectionScreenZoneItem, ConnectionScreenZoneItem>.speakers() =
    if (first.type == Speaker) first else second

fun Pair<ConnectionScreenZoneItem, ConnectionScreenZoneItem>.instruments() =
    if (first.type == Instrument) first else second

fun Pair<ConnectionScreenZoneItem, ConnectionScreenZoneItem>.isCompleted() =
    first.isCompleted() && second.isCompleted()

@Composable
fun DraggableDeviceCard(
    dragListener: ConnectionScreenDragListener? = null,
    modifier: Modifier = Modifier,
    device: () -> JiminyDevice,
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    val isDeviceBeingDragged by remember {
        mutableStateOf(dragListener?.deviceBeingDragged()?.name == device().name)
    }

    val windowOffset = Offset(12f, 114f)
    DeviceCard(
        device = device,
        modifier = modifier
            // Track where we are in the window to tell the ghost where to start.
            .onGloballyPositioned { coord ->
                itemPosition = coord.positionInWindow().minus(windowOffset)
            }
            // If this card is the one being dragged, make the source semi-transparent
            .graphicsLayer(alpha = if (isDeviceBeingDragged) 0.2f else 1.0f)
            .pointerInput(device().displayName) {
                detectDragGestures(
                    onDragStart = {
                        dragListener?.onDeviceDragStart(device(), itemPosition)
                    },
                    onDrag = { pointerChange, dragAmountOffset ->
                        pointerChange.consume()
                        itemPosition += dragAmountOffset
                        dragListener?.onDeviceDrag(itemPosition)
                    },
                    onDragEnd = { dragListener?.onDeviceDragEnd(itemPosition.plus(windowOffset)) },
                )
            }
    )
}
