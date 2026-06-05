package music.jiminy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsInputSvideo
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.combine
import music.jiminy.screen.ConnectionScreen
import music.jiminy.screen.LogsScreen
import music.jiminy.screen.MIDIScreen
import music.jiminy.screen.MixerScreen
import music.jiminy.screen.RecordingOverlay
import music.jiminy.screen.RecordingScreen
import music.jiminy.screen.common.GenericMessageAlert
import music.jiminy.screen.common.LoadConfigAlert
import music.jiminy.screen.common.MarqueeText
import music.jiminy.screen.common.SaveConfigAlert
import music.jiminy.screen.common.TextError
import music.jiminy.screen.common.TextTitle
import music.jiminy.service.JiminyConnectionStatus
import music.jiminy.viewmodel.ConnectionScreenViewModel
import music.jiminy.viewmodel.ConnectionViewModel
import music.jiminy.viewmodel.LogsViewModel
import music.jiminy.viewmodel.MIDIScreenViewModel
import music.jiminy.viewmodel.RecordingScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme(darkColorScheme()) {
        val viewModel: ConnectionViewModel = koinViewModel()
        var mixerTab: ConnectionViewModel.JiminyTab? = null

        LaunchedEffect(Unit) {
            viewModel.addTab(
                title = { TabTitle(Icons.AutoMirrored.Outlined.AltRoute, "Audio Links") },
                content = ::AudioLinksMainScreen,
            )
            viewModel.addTab(
                title = { TabTitle(Icons.Default.SettingsInputSvideo, "MIDI") },
                content = ::MIDIMainScreen,
            )
            mixerTab = viewModel.addTab(
                title = { TabTitle(Icons.Rounded.Tune, "Mixer") },
                content = ::MixerMainScreen,
            )
            viewModel.addTab(
                title = { TabTitle(Icons.Filled.Voicemail, "Record") },
                content = ::RecordingMainScreen,
            )
            viewModel.addTab(
                title = { TabTitle(Icons.AutoMirrored.Filled.List, "Logs") },
                isScrollable = false, // Logs uses LazyColumn, so it handles its own scrolling
                content = ::LogsMainScreen,
            )
        }

        val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
        val recordingStatus by viewModel.recordingStatus.collectAsStateWithLifecycle()

        LaunchedEffect(selectedTab) { if (selectedTab != mixerTab) viewModel.mixerDisconnect() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            MainScreen(Modifier.fillMaxSize())

            if (recordingStatus != ConnectionViewModel.RecordingStatus.Idle) {
                RecordingOverlay(
                    onStopRequest = viewModel::stopRecording,
                    enabled = recordingStatus == ConnectionViewModel.RecordingStatus.Recording,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
fun TabTitle(icon: ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val connectionViewModel: ConnectionViewModel = koinViewModel()
    val connectionScreenViewModel: ConnectionScreenViewModel = koinViewModel()
    val midiScreenViewModel: MIDIScreenViewModel = koinViewModel()
    val recordingScreenViewModel: RecordingScreenViewModel = koinViewModel()
    val logsViewModel: LogsViewModel = koinViewModel()
    val selectedTab by connectionViewModel.selectedTab.collectAsStateWithLifecycle()
    val connectionStatus by connectionViewModel.connectionStatus.collectAsStateWithLifecycle()
    val isRefreshing by connectionViewModel.isRefreshing.collectAsStateWithLifecycle()

    val audioState by connectionScreenViewModel.state.collectAsStateWithLifecycle()
    val midiState by midiScreenViewModel.state.collectAsStateWithLifecycle()
    val showSaveConfigPopup by connectionViewModel.showSaveConfigPopup.collectAsStateWithLifecycle()
    val showLoadConfigPopup by connectionViewModel.showLoadConfigPopup.collectAsStateWithLifecycle()
    val showOverwriteConfigPopup by connectionViewModel.showOverwriteConfigPopup.collectAsStateWithLifecycle()
    val configurationsState by connectionViewModel.configurationsState.collectAsStateWithLifecycle()

    if (showSaveConfigPopup) {
        SaveConfigAlert(
            state = configurationsState,
            onDismiss = connectionViewModel::dismissSaveConfigPopup,
            onConfirm = { name, saveAudio, saveMidi ->
                val links = buildList {
                    if (saveAudio) addAll(audioState.links)
                    if (saveMidi) addAll(midiState.links)
                }
                connectionViewModel.saveConfiguration(name, links)
            },
        )
    }

    if (showLoadConfigPopup) {
        LoadConfigAlert(
            state = configurationsState,
            onDismiss = connectionViewModel::dismissLoadConfigPopup,
            onSelect = connectionViewModel::loadConfigurations,
            onDelete = connectionViewModel::deleteConfigurations,
        )
    }

    showOverwriteConfigPopup?.let { name ->
        GenericMessageAlert(
            title = "Overwrite configuration?",
            message = "A configuration named \"$name\" already exists. Do you want to overwrite it?",
            onDismiss = connectionViewModel::dismissOverwriteConfigPopup,
            onConfirm = connectionViewModel::confirmOverwrite,
            confirmLabel = "Overwrite",
        )
    }

    val errorFlow = combine(
        connectionViewModel.errorMessage,
        connectionScreenViewModel.errorMessage,
        midiScreenViewModel.errorMessage,
        recordingScreenViewModel.errorMessage,
        logsViewModel.errorMessage,
    ) { err1, err2, err3, err4, err5 ->
        buildString {
            err1?.let { append(it) }
            err2?.let { append(it) }
            err3?.let { append(it) }
            err4?.let { append(it) }
            err5?.let { append(it) }
        }.takeIf { it.isNotEmpty() }
    }

    LaunchedEffect(selectedTab) {
        connectionViewModel.resetError()
        connectionScreenViewModel.resetError()
        midiScreenViewModel.resetError()
        recordingScreenViewModel.resetError()
        logsViewModel.resetError()
    }

    selectedTab?.let { selectedTab ->
        val errorMsg by errorFlow.collectAsStateWithLifecycle(null)
        val tabs by connectionViewModel.tabs.collectAsStateWithLifecycle()
        val scrollState = rememberScrollState()
        val baseModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)

        Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
            StatusBar(
                status = connectionStatus,
                error = errorMsg,
                onSaveClick = connectionViewModel::onSaveConfigClick,
                onLoadClick = connectionViewModel::onLoadConfigClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    connectionViewModel.refresh(
                        connectionScreenViewModel = connectionScreenViewModel,
                        midiScreenViewModel = midiScreenViewModel,
                        recordingScreenViewModel = recordingScreenViewModel,
                        logsViewModel = logsViewModel,
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                // Apply verticalScroll only if the tab is marked as scrollable
                val screenModifier = if (selectedTab.isScrollable) {
                    baseModifier.verticalScroll(scrollState)
                } else {
                    baseModifier
                }
                selectedTab.content(screenModifier)
            }

            HorizontalDivider()

            SecondaryTabRow(
                selectedTabIndex = selectedTab.index,
                containerColor = TabRowDefaults.primaryContainerColor,
                contentColor = TabRowDefaults.primaryContentColor,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(selectedTab.index, matchContentSize = false),
                    )
                },
                divider = { }, // Removed divider here as we have HorizontalDivider() above
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab.index == tab.index,
                        onClick = { connectionViewModel.selectTab(tab.index) },
                        text = tab.title,
                    )
                }
            }
        }
    } ?: TextError("Error while loading the tabs")
}

@Composable
fun StatusBar(
    status: JiminyConnectionStatus,
    error: String?,
    onSaveClick: () -> Unit,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectionStatusIcon(status)

            if (error != null) {
                MarqueeText(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            } else {
                TextTitle(text = "Jiminy")
            }
        }

        Row {
            IconButton(onClick = onLoadClick) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = "Load Config",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save Config",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusIcon(status: JiminyConnectionStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "connection")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    when (status) {
        JiminyConnectionStatus.Connected -> Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = "Connected",
            tint = Color(0xFF4CAF50),
        )

        JiminyConnectionStatus.Connecting -> Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Connecting",
            tint = Color(0xFFFFC107),
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )

        JiminyConnectionStatus.Disconnected -> Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Disconnected",
            tint = Color.Gray,
        )

        is JiminyConnectionStatus.Error -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun AudioLinksMainScreen(
    screenModifier: Modifier = Modifier,
) {
    ConnectionScreen(
        modifier = screenModifier,
    )
}

@Composable
fun MIDIMainScreen(
    screenModifier: Modifier = Modifier,
) {
    MIDIScreen(
        modifier = screenModifier,
    )
}

@Composable
fun MixerMainScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel: ConnectionViewModel = koinViewModel()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val succeededCommands by viewModel.succeededCommands.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getDevices()
        viewModel.mixerConnect()
    }

    MixerScreen(
        devices = { devices.filter { it.volumes.isNotEmpty() && it.name != PW_RECORDER_NAME } },
        succeededCommands = succeededCommands,
        onVolumeChange = { deviceVolume, newVolume ->
            viewModel.mixerSendCommand(JiminyCommand.VolumeUpdate(deviceVolume, newVolume))
        },
        onMuteStateChange = { deviceVolume, newMuteState ->
            viewModel.mixerSendCommand(JiminyCommand.MuteUpdate(deviceVolume, newMuteState))
        },
        modifier = modifier,
    )
}

@Composable
fun RecordingMainScreen(
    modifier: Modifier = Modifier,
) {
    RecordingScreen(
        modifier = modifier,
    )
}

@Composable
fun LogsMainScreen(
    modifier: Modifier = Modifier,
) {
    LogsScreen(
        modifier = modifier,
    )
}
