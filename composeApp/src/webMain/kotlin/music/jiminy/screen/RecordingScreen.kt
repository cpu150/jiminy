package music.jiminy.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.jiminy.DEVICE_LIST_CARD_HEIGHT
import music.jiminy.DEVICE_LIST_CARD_WIDTH
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.screen.common.DeviceCard
import music.jiminy.screen.common.JiminyButton
import music.jiminy.screen.common.SelectableNodeItem
import music.jiminy.screen.common.TextBody
import music.jiminy.screen.common.TextButton
import music.jiminy.screen.common.TextTitle

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

        if (selectedDevNodePairs.isEmpty()) {
            TextTitle(
                "Add Devices To Record",
                textAlign = TextAlign.Center,
                modifier = recordItemsModifier,
            )
        } else {
            SelectedNodes(
                deviceNodePairs = { selectedDevNodePairs },
                removeNode = removeNode,
                modifier = recordItemsModifier,
            )
        }

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

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items(deviceNodePairs) { (dev, node) ->
            Column(modifier = Modifier.wrapContentSize().clickable { removeNode(dev, node) }) {
                DeviceCard(
                    showLabels = false,
                    modifier = Modifier
                        .height(DEVICE_LIST_CARD_HEIGHT.dp)
                        .width(DEVICE_LIST_CARD_WIDTH.dp),
                ) { dev }
                TextBody(
                    text = node.displayPortName,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .wrapContentHeight()
                        .width(DEVICE_LIST_CARD_WIDTH.dp),
                )
            }
        }
    }
}
