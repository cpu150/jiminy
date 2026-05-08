package music.jiminy.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.jiminy.DEVICE_LIST_CARD_HEIGHT
import music.jiminy.DEVICE_LIST_CARD_WIDTH
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.PW_RECORDER_CHANNEL_COUNT
import music.jiminy.screen.common.DeviceAvatar
import music.jiminy.screen.common.DeviceCard
import music.jiminy.screen.common.JiminyButton
import music.jiminy.screen.common.SelectableNodeItem
import music.jiminy.screen.common.TextBody
import music.jiminy.screen.common.TextButton
import music.jiminy.screen.common.TextLabel
import music.jiminy.screen.common.TextTitle
import music.jiminy.screen.common.toResource
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RecordingScreen(
    preselectedDevNodePairs: () -> List<Pair<JiminyDevice, JiminyDeviceNode>>,
    devices: () -> List<JiminyDevice>,
    startRecording: (List<JiminyDeviceNode>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val devices = devices()
    var showDetails by remember { mutableStateOf<JiminyDevice?>(null) }
    val selectedDevNodePairs = remember { preselectedDevNodePairs().toMutableStateList() }

    Column(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        val recordItemsModifier = Modifier.height((DEVICE_LIST_CARD_HEIGHT + 40).dp).fillMaxWidth()
        val removeNode: (JiminyDevice, JiminyDeviceNode) -> Unit =
            { dev, node -> selectedDevNodePairs.remove(dev to node) }

        SelectedNodes(
            deviceNodePairs = { selectedDevNodePairs },
            removeNode = removeNode,
            modifier = recordItemsModifier,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isRecordingEnable = selectedDevNodePairs.isNotEmpty()
            val textColor = if (isRecordingEnable) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = .3f)
            }

            JiminyButton(
                enabled = isRecordingEnable,
                onClick = { startRecording(selectedDevNodePairs.map { it.second }) },
                modifier = Modifier.padding(4.dp),
            ) { TextButton(text = "Start Recording", color = textColor) }

            JiminyButton(
                enabled = isRecordingEnable,
                onClick = { /* TODO */ },
                modifier = Modifier.padding(4.dp),
            ) { TextButton(text = "Save Config", color = textColor) }
        }

        Spacer(Modifier.size(8.dp))

        TextTitle(
            text = "Available devices",
            modifier = Modifier.padding(4.dp),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(devices) { device ->
                DeviceCard(
                    showLabels = false,
                    modifier = Modifier
                        .clickable(onClick = { showDetails = device })
                        .height(DEVICE_LIST_CARD_HEIGHT.dp)
                        .width(DEVICE_LIST_CARD_WIDTH.dp),
                ) { device }
            }
        }

        Spacer(Modifier.size(4.dp))

        showDetails?.let { device ->
            DeviceDetails(
                device = { device },
                selectedNodes = {
                    selectedDevNodePairs
                        .filter { it.first == device }
                        .map { it.second }
                },
                addNode = { dev, node -> selectedDevNodePairs.add(dev to node) },
                removeNode = removeNode,
                modifier = Modifier.padding(vertical = 8.dp).wrapContentHeight().fillMaxWidth(),
            )
        }
    }
}

@Composable
fun DeviceDetails(
    device: () -> JiminyDevice,
    selectedNodes: () -> List<JiminyDeviceNode>,
    addNode: (JiminyDevice, JiminyDeviceNode) -> Unit,
    removeNode: (JiminyDevice, JiminyDeviceNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val device = device()
    val selectedNodes = selectedNodes()

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        device.instruments.forEach { node ->
            val isSelected = selectedNodes.contains(node)

            SelectableNodeItem(
                node = { node },
                isSelected = isSelected,
                onClick = {
                    if (isSelected) {
                        removeNode(device, node)
                    } else {
                        addNode(device, node)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(.33f),
            )
        }
    }
}

@Composable
fun SelectedNodes(
    deviceNodePairs: () -> List<Pair<JiminyDevice, JiminyDeviceNode>>,
    removeNode: (JiminyDevice, JiminyDeviceNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceNodePairs = deviceNodePairs()
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        for (index in 0..<PW_RECORDER_CHANNEL_COUNT) {
            val pair = deviceNodePairs.getOrNull(index)

            if (pair != null) {
                val (dev, node) = pair
                EmptySelectedNodeItem(
                    linked = true,
                    portName = node.displayPortName,
                    deviceName = node.displayName,
                    avatarImg = dev.avatarIcon.toResource(),
                    modifier = Modifier.clickable(onClick = { removeNode(dev, node) }),
                )
            } else {
                EmptySelectedNodeItem(
                    linked = false,
                    portName = "EMPTY",
                    deviceName = "CH ${index + 1}",
                )
            }
        }
    }
}

@Composable
fun EmptySelectedNodeItem(
    linked: Boolean,
    portName: String,
    deviceName: String,
    avatarImg: DrawableResource? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SineWaveEmptySlot(
            linked = linked,
            label = portName,
            avatarImg = avatarImg,
            modifier = Modifier
                .height(DEVICE_LIST_CARD_HEIGHT.dp)
                .width(DEVICE_LIST_CARD_WIDTH.dp),
        )
        TextBody(
            text = deviceName,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .wrapContentHeight()
                .width(DEVICE_LIST_CARD_WIDTH.dp),
        )
    }
}

@Composable
fun SineWaveEmptySlot(
    linked: Boolean,
    label: String,
    avatarImg: DrawableResource? = null,
    modifier: Modifier = Modifier,
) {
    val sineColor = if (linked) Color(0xFF4CAF50) else Color(0xAAB71C1C)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    // Animation for the "scroll" effect of the sine wave
    val infiniteTransition = rememberInfiniteTransition(label = "sineScroll")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        label = "phase",
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )

    // Animation for the "pulse" of the glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        label = "pulse",
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Column(
        modifier = modifier
            .border(
                1.dp,
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(4.dp),
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The Sine Wave Display
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarImg != null) {
                DeviceAvatar(avatarImg, Modifier.fillMaxSize().padding(8.dp).aspectRatio(1f))
            }

            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 1.dp)) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val amplitude = 16.dp.toPx() // How tall the wave is
                val frequency = 1.25f // How many waves fit in the box

                val path = Path().apply {
                    for (x in 0..width.toInt()) {
                        // Standard Sine Formula: y = A * sin(2π * f * (x/W) + phase)
                        val relativeX = x / width
                        val y =
                            centerY + amplitude * sin(2 * PI * frequency * relativeX + phaseShift).toFloat()

                        if (x == 0) moveTo(x.toFloat(), y) else lineTo(x.toFloat(), y)
                    }
                }

                // Draw the glow layer
                drawPath(
                    path = path,
                    color = sineColor.copy(alpha = glowAlpha),
                    style = Stroke(width = 6.dp.toPx()),
                )

                // Draw the core line
                drawPath(
                    path = path,
                    color = sineColor.copy(alpha = 0.6f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        TextLabel(
            text = label,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
