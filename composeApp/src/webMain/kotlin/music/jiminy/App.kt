package music.jiminy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.combine
import music.jiminy.screen.ConnectionScreen
import music.jiminy.screen.MixerScreen
import music.jiminy.screen.RecordingOverlay
import music.jiminy.screen.RecordingScreen
import music.jiminy.screen.common.TextError
import music.jiminy.viewmodel.ConnectionScreenViewModel
import music.jiminy.viewmodel.ConnectionViewModel
import music.jiminy.viewmodel.RecordingScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme(darkColorScheme()) {
        val viewModel: ConnectionViewModel = koinViewModel()
        var mixerTab: ConnectionViewModel.JiminyTab? = null

        LaunchedEffect(Unit) {
            viewModel.addTab(
                { TabTitle(Icons.AutoMirrored.Outlined.AltRoute, "Audio Links") },
                ::AudioLinksMainScreen,
            )
            mixerTab = viewModel.addTab(
                { TabTitle(Icons.Rounded.Tune, "Mixer") },
                content = ::MixerMainScreen,
            )
            viewModel.addTab(
                { TabTitle(Icons.Filled.Voicemail, "Record") },
                content = ::RecordingMainScreen,
            )
        }

        val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
        val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
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

            // TODO : connectionStatus
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
    val recordingScreenViewModel: RecordingScreenViewModel = koinViewModel()
    val selectedTab by connectionViewModel.selectedTab.collectAsStateWithLifecycle()
    val errorFlow = combine(
        connectionViewModel.errorMessage,
        connectionScreenViewModel.errorMessage,
        recordingScreenViewModel.errorMessage
    ) { err1, err2, err3 ->
        buildString {
            err1?.let { append(it) }
            err2?.let { append(it) }
            err3?.let { append(it) }
        }.takeIf { it.isNotEmpty() }
    }

    LaunchedEffect(selectedTab) {
        connectionViewModel.resetError()
        connectionScreenViewModel.resetError()
        recordingScreenViewModel.resetError()
    }

    selectedTab?.let { selectedTab ->
        val errorMsg by errorFlow.collectAsStateWithLifecycle(null)
        val tabs by connectionViewModel.tabs.collectAsStateWithLifecycle()
        val scrollState = rememberScrollState()
        val screenModifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)

        Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab.index,
                containerColor = TabRowDefaults.primaryContainerColor,
                contentColor = TabRowDefaults.primaryContentColor,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(selectedTab.index, matchContentSize = false),
                    )
                },
                divider = { HorizontalDivider() },
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab.index == tab.index,
                        onClick = { connectionViewModel.selectTab(tab.index) },
                        text = tab.title,
                    )
                }
            }

            errorMsg?.let { TextError(it) }

            selectedTab.content(screenModifier)
        }
    } ?: TextError("Error while loading the tabs")
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
