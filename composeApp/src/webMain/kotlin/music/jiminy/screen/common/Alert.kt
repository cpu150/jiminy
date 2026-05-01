package music.jiminy.screen.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.screen.ConnectionScreenNodeType.Speaker

@Composable
fun ErrorAlert(
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { JiminyButton(onDismissRequest) { TextButton("OK") } },
        title = { TextTitle(message) },
        modifier = modifier,
    )
}

@Composable
fun IncompleteRowsAlert(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        title = { TextTitle("Complete all rows") },
        text = { TextBody("All rows must be completed before!") },
        onDismissRequest = onDismissRequest,
        confirmButton = { JiminyButton(onClick = onDismissRequest) { TextButton("OK") } },
        modifier = modifier,
    )
}

@Composable
fun GenericMessageAlert(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "Ok",
    cancelLabel: String = "Cancel",
) {
    val confirmAndDismiss = {
        onConfirm()
        onDismiss()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { TextTitle(title) },
        text = { message?.let { TextTitle(message) } },
        confirmButton = { JiminyButton(onClick = confirmAndDismiss) { TextButton(confirmLabel) } },
        dismissButton = { JiminyButton(onClick = onDismiss) { TextButton(cancelLabel) } },
    )
}

@Composable
fun UnlinkConfirmationAlert(
    pair: () -> Pair<JiminyDevice, JiminyDeviceNode?>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val (device, node) = pair()
    val str = if (node == null) {
        "all ports from \"${device.displayName}\" device"
    } else {
        "\"${node.displayPortName}\" port from \"${device.displayName}\" device"
    }
    val detailStr: (@Composable () -> Unit)? = if (node == null) {
        { TextBody("Ports: ${device.nodes().joinToString { "${it.displayPortName}, " }}") }
    } else {
        null
    }

    val confirmAndDismiss = {
        onConfirm()
        onDismiss()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { TextTitle("Unlink $str?") },
        text = detailStr,
        confirmButton = { JiminyButton(onClick = confirmAndDismiss) { TextButton("Unlink") } },
        dismissButton = { JiminyButton(onClick = onDismiss) { TextButton("Cancel") } },
    )
}

@Composable
fun DeleteConfirmationAlert(
    name: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val confirmAndDismiss = {
        onConfirm()
        onDismiss()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { TextTitle("Delete") },
        text = { TextBody("Do you want to delete \"$name\"?") },
        confirmButton = { JiminyButton(onClick = confirmAndDismiss) { TextButton("Delete") } },
        dismissButton = { JiminyButton(onClick = onDismiss) { TextButton("Cancel") } },
    )
}

@Composable
fun NodeSelectionAlert(
    onDismiss: () -> Unit,
    droppedDevice: () -> JiminyDevice,
    addNodes: (List<JiminyDeviceNode>)-> Unit,
    zoneItem: () -> ConnectionScreenZoneItem,
    modifier: Modifier = Modifier,
) {
    // Track which items the user has clicked inside the popup
    val selectedNodes = remember { mutableStateListOf<JiminyDeviceNode>() }
    val device = remember { droppedDevice() }
    val (label, availableNodes) = remember {
        if (zoneItem().type == Speaker) {
            "speaker(s)" to device.speakers
        } else {
            "instrument(s)" to device.instruments
        }
    }
    val errorMsg = remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { TextTitle("Select \"${device.displayName}\" $label") },
        text = {
            Column {
                // A grid with exactly 3 columns
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp) // Keep popup from taking full screen
                ) {
                    items(availableNodes) { node ->
                        val isSelected = selectedNodes.contains(node)
                        SelectableNodeItem(
                            node = { node },
                            isSelected = isSelected,
                            onClick = {
                                // Clear previous error message
                                errorMsg.value = null

                                if (isSelected) {
                                    selectedNodes.remove(node)
                                } else {
                                    selectedNodes.add(node)
                                }
                            }
                        )
                    }
                }
                errorMsg.value?.let { msg ->
                    TextError(msg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        },
        confirmButton = {
            JiminyButton(onClick = {
                if (selectedNodes.isNotEmpty()) {
                    selectedNodes.sortBy { it.displayPortName }
                    addNodes(selectedNodes)
                    onDismiss()
                } else {
                    errorMsg.value = "Select at least one node"
                }
            }) { TextButton("Add") }
        },
        dismissButton = { JiminyButton(onClick = onDismiss) { TextButton("Cancel") } }
    )
}

@Composable
fun SelectableNodeItem(
    node: () -> JiminyDeviceNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val node = node()
    val color =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color,
        border = border,
        modifier = modifier.height(60.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            TextBody(
                text = node.displayPortName,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
