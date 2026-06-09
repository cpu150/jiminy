package music.jiminy.screen.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsInputSvideo
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.SaveConfigOptions
import music.jiminy.isValid
import music.jiminy.screen.ConnectionScreenNodeType.Speaker
import music.jiminy.viewmodel.ConnectionViewModel
import music.jiminy.viewmodel.ConnectionViewModel.LoadConfigState.Error
import music.jiminy.viewmodel.ConnectionViewModel.LoadConfigState.Idle
import music.jiminy.viewmodel.ConnectionViewModel.LoadConfigState.Loading
import music.jiminy.viewmodel.ConnectionViewModel.LoadConfigState.Success

@Composable
fun ErrorAlert(
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            JiminyButton(onClick = onDismissRequest) {
                TextButton("OK")
            }
        },
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
        confirmButton = {
            JiminyButton(onClick = onDismissRequest) {
                TextButton("OK")
            }
        },
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
        confirmButton = {
            JiminyButton(onClick = confirmAndDismiss) {
                TextButton(confirmLabel)
            }
        },
        dismissButton = {
            JiminyButton(onClick = onDismiss) {
                TextButton(cancelLabel)
            }
        },
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
        confirmButton = {
            JiminyButton(onClick = confirmAndDismiss) {
                TextButton("Unlink")
            }
        },
        dismissButton = {
            JiminyButton(onClick = onDismiss) {
                TextButton("Cancel")
            }
        },
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
        confirmButton = {
            JiminyButton(onClick = confirmAndDismiss) {
                TextButton("Delete")
            }
        },
        dismissButton = {
            JiminyButton(onClick = onDismiss) {
                TextButton("Cancel")
            }
        },
    )
}

@Composable
fun NodeSelectionAlert(
    onDismiss: () -> Unit,
    droppedDevice: () -> JiminyDevice,
    addNodes: (List<JiminyDeviceNode>) -> Unit,
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
                    modifier = Modifier.heightIn(max = 400.dp), // Keep popup from taking full screen
                ) {
                    items(availableNodes) { node ->
                        val isSelected = selectedNodes.any { it.fullName == node.fullName }
                        SelectableNodeItem(
                            node = { node },
                            isSelected = isSelected,
                            onClick = {
                                // Clear previous error message
                                errorMsg.value = null

                                if (isSelected) {
                                    selectedNodes.removeAll { it.fullName == node.fullName }
                                } else {
                                    selectedNodes.add(node)
                                }
                            },
                        )
                    }
                }
                errorMsg.value?.let { msg ->
                    TextError(msg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        },
        confirmButton = {
            JiminyButton(
                onClick = {
                    if (selectedNodes.isNotEmpty()) {
                        selectedNodes.sortBy { it.displayPortName }
                        addNodes(selectedNodes)
                        onDismiss()
                    } else {
                        errorMsg.value = "Select at least one node"
                    }
                },
            ) { TextButton("Add") }
        },
        dismissButton = {
            JiminyButton(onClick = onDismiss) {
                TextButton("Cancel")
            }
        },
    )
}

@Composable
fun RecordingsSelectionAlert(
    recordings: List<String>,
    selectedRecordings: List<String>,
    onDismiss: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleRecordings: (List<String>) -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupedRecordings = remember(recordings) {
        recordings.groupBy { it.substringBefore(" - ") }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextTitle(text = "Recording Files")
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 600.dp),
                ) {
                    groupedRecordings.forEach { (date, files) ->
                        // Header for the day
                        item(span = { GridItemSpan(3) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = { onToggleRecordings(files) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Select All",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                TextBody(text = date)
                                Spacer(Modifier.width(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }

                        // Files for that day
                        items(files) { recording ->
                            val isSelected = selectedRecordings.contains(recording)
                            RecordingFileItem(
                                name = recording.substringAfter(" - "),
                                isSelected = isSelected,
                                onClick = { onToggleSelection(recording) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val hasSelection = selectedRecordings.isNotEmpty()
                IconButton(
                    enabled = hasSelection,
                    onClick = onDownload,
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = if (hasSelection) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                    )
                }
                IconButton(
                    enabled = hasSelection,
                    onClick = onDelete,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (hasSelection) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun RecordingFileItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text = name,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SaveConfigAlert(
    state: ConnectionViewModel.LoadConfigState,
    onDismiss: () -> Unit,
    onConfirm: (SaveConfigOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorMsg = remember { mutableStateOf<String?>(null) }
    val options = remember { mutableStateOf(SaveConfigOptions()) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextTitle("Save Configuration")
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                    )
                }
            }
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        Loading -> CircularProgressIndicator()
                        is Error -> TextError(state.message, modifier = Modifier.padding(16.dp))
                        else -> SaveConfigScreen(
                            errorMsg = errorMsg,
                            options = options,
                            configurations = (state as? Success)?.configurations ?: emptyList(),
                        )
                    }
                }
                errorMsg.value?.let { TextError(it) }
            }
        },
        confirmButton = {
            val isEnabled = (state is Success) || (state is Idle)
            val name = options.value.name
            IconButton(
                enabled = isEnabled && name.isNotBlank(),
                onClick = {
                    if (name.isBlank()) {
                        errorMsg.value = "Name cannot be empty"
                    } else if (!options.value.isValid) {
                        errorMsg.value = "At least 1 tab must be selected"
                    } else {
                        onConfirm(options.value)
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = if (isEnabled && name.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveConfigScreen(
    errorMsg: MutableState<String?>,
    options: MutableState<SaveConfigOptions>,
    configurations: List<String>,
    modifier: Modifier = Modifier,
) {
    val createNewText = "New"
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember {
        mutableStateOf(if (options.value.name.isEmpty() || options.value.name !in configurations) createNewText else options.value.name)
    }

    Column(modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { TextBody("Select Configuration") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                ).fillMaxWidth(),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { TextBody(createNewText) },
                    onClick = {
                        selectedOption = createNewText
                        options.value = options.value.copy(name = "")
                        errorMsg.value = null
                        expanded = false
                    },
                )
                configurations.forEach { config ->
                    DropdownMenuItem(
                        text = { TextBody(config) },
                        onClick = {
                            selectedOption = config
                            options.value = options.value.copy(name = config)
                            errorMsg.value = null
                            expanded = false
                        },
                    )
                }
            }
        }

        if (selectedOption == createNewText) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = options.value.name,
                onValueChange = {
                    options.value = options.value.copy(name = it)
                    errorMsg.value = null
                },
                label = { TextBody("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val onClick: (SaveConfigOptions.() -> SaveConfigOptions) -> Unit = { newOptions ->
                options.value = options.value.newOptions()
                errorMsg.value = null
            }

            IconButton(onClick = { onClick { copy(saveAudio = !options.value.saveAudio) } }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.AltRoute,
                    contentDescription = "Audio Links",
                    tint = if (options.value.saveAudio) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
            IconButton(onClick = { onClick { copy(saveMidi = !options.value.saveMidi) } }) {
                Icon(
                    imageVector = Icons.Default.SettingsInputSvideo,
                    contentDescription = "MIDI Links",
                    tint = if (options.value.saveMidi) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
            IconButton(onClick = { onClick { copy(saveVolumes = !options.value.saveVolumes) } }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volumes",
                    tint = if (options.value.saveVolumes) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
            IconButton(onClick = { onClick { copy(saveRecordingNodes = !options.value.saveRecordingNodes) } }) {
                Icon(
                    imageVector = Icons.Default.Voicemail,
                    contentDescription = "Recording Slots",
                    tint = if (options.value.saveRecordingNodes) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }
    }
}

@Composable
fun LoadConfigAlert(
    state: ConnectionViewModel.LoadConfigState,
    onDismiss: () -> Unit,
    onSelect: (List<String>) -> Unit,
    onDelete: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedConfigs by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        val name = if (selectedConfigs.size == 1) {
            selectedConfigs.first()
        } else {
            "${selectedConfigs.size} configurations"
        }
        DeleteConfirmationAlert(
            name = name,
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                onDelete(selectedConfigs.toList())
                selectedConfigs = emptySet()
            },
        )
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextTitle("Load Configuration")
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    Idle -> Unit
                    Loading -> CircularProgressIndicator()
                    is Error -> TextError(state.message, modifier = Modifier.padding(16.dp))
                    is Success -> LoadConfigView(
                        configurations = state.configurations,
                        selectedConfigs = selectedConfigs,
                        onToggleSelection = { config ->
                            selectedConfigs = if (selectedConfigs.contains(config)) {
                                selectedConfigs - config
                            } else {
                                selectedConfigs + config
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    enabled = selectedConfigs.isNotEmpty(),
                    onClick = { showDeleteConfirmation = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (selectedConfigs.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
                IconButton(
                    enabled = selectedConfigs.isNotEmpty(),
                    onClick = { onSelect(selectedConfigs.toList()) },
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Load",
                        tint = if (selectedConfigs.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun LoadConfigView(
    configurations: List<String>,
    selectedConfigs: Set<String>,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (configurations.isEmpty()) {
        TextBody("No configurations found", modifier = Modifier.padding(16.dp))
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.heightIn(max = 400.dp),
        ) {
            items(configurations) { config ->
                SelectableConfigItem(
                    name = config,
                    isSelected = selectedConfigs.contains(config),
                    onClick = { onToggleSelection(config) },
                )
            }
        }
    }
}

@Composable
fun SelectableConfigItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            TextBody(
                text = name,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
