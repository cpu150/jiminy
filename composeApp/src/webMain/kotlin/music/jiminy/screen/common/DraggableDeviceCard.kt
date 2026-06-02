package music.jiminy.screen.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.IntSize
import music.jiminy.JiminyDeviceI
import music.jiminy.JiminyDeviceNode
import music.jiminy.screen.ConnectionScreenNodeType
import music.jiminy.screen.ConnectionScreenNodeType.Speaker

interface ConnectionScreenDragListener<T : JiminyDeviceI<T>> {
    fun deviceBeingDragged(): T?
    fun onDeviceDragStart(device: T, initialOffset: Offset)
    fun onDeviceDrag(newOffset: Offset)
    fun onDeviceDragEnd(finalOffset: Offset)
    fun getContainerPosition(): Offset
}

@Stable
data class ConnectionScreenZoneItem<T : JiminyDeviceI<T>>(
    val type: ConnectionScreenNodeType,
    val zone: MutableState<Rect> = mutableStateOf(Rect.Zero),
    val devices: SnapshotStateList<T> = mutableStateListOf(),
)

fun <T : JiminyDeviceI<T>> ConnectionScreenZoneItem<T>.removeNode(node: JiminyDeviceNode) =
    devices.find { it.name == node.deviceName }?.also {
        it.removeNode(node)
        if (it.nodes().isEmpty()) devices.remove(it)
    }

fun <T : JiminyDeviceI<T>> ConnectionScreenZoneItem<T>.addNodes(
    nodes: List<JiminyDeviceNode>,
    factory: (String) -> T,
) = nodes.forEach { node ->
    (devices.find { it.name == node.deviceName }?.also { devices.remove(it) }
        ?: factory(node.deviceName))
        .also { devices.add(it) }
        .takeIf { it.nodes().any { n -> n.fullName == node.fullName }.not() }
        ?.also {
            it.addNode(node)
        }
}

fun <T : JiminyDeviceI<T>> ConnectionScreenZoneItem<T>.nodes() = devices.flatMap { device ->
    if (type == Speaker) {
        device.speakers
    } else {
        device.instruments
    }
}

fun <T : JiminyDeviceI<T>> ConnectionScreenZoneItem<T>.isCompleted() = devices.find {
    if (type == Speaker) {
        it.speakers.isEmpty()
    } else {
        it.instruments.isEmpty()
    }
} == null && devices.isNotEmpty()

fun <T : JiminyDeviceI<T>> Pair<ConnectionScreenZoneItem<T>, ConnectionScreenZoneItem<T>>.isCompleted() =
    first.isCompleted() && second.isCompleted()

@Composable
fun <T : JiminyDeviceI<T>> DraggableDeviceCard(
    dragListener: ConnectionScreenDragListener<T>? = null,
    modifier: Modifier = Modifier,
    device: () -> T,
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val draggingDevice = dragListener?.deviceBeingDragged()
    val isDeviceBeingDragged = draggingDevice?.name == device().name
    val containerPosition = dragListener?.getContainerPosition() ?: Offset.Zero

    DeviceCard(
        device = device,
        modifier = modifier
            .onGloballyPositioned { coord ->
                // Only update base position if we're not currently dragging this item
                if (!isDeviceBeingDragged) {
                    itemPosition = coord.positionInWindow().minus(containerPosition)
                    cardSize = coord.size
                }
            }
            .graphicsLayer(alpha = if (isDeviceBeingDragged) 0.2f else 1.0f)
            .pointerInput(device().name) {
                detectDragGestures(
                    onDragStart = {
                        dragListener?.onDeviceDragStart(device(), itemPosition)
                    },
                    onDrag = { pointerChange, dragAmountOffset ->
                        pointerChange.consume()
                        itemPosition += dragAmountOffset
                        dragListener?.onDeviceDrag(itemPosition)
                    },
                    onDragEnd = {
                        val centerOffset = Offset(cardSize.width / 2f, cardSize.height / 2f)
                        dragListener?.onDeviceDragEnd(
                            itemPosition.plus(containerPosition).plus(centerOffset)
                        )
                    },
                    onDragCancel = {
                        val centerOffset = Offset(cardSize.width / 2f, cardSize.height / 2f)
                        dragListener?.onDeviceDragEnd(
                            itemPosition.plus(containerPosition).plus(centerOffset)
                        )
                    },
                )
            },
    )
}
