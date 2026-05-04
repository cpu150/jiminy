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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import music.jiminy.screen.ConnectionScreen
import music.jiminy.screen.MixerScreen
import music.jiminy.screen.RecordingOverlay
import music.jiminy.screen.RecordingScreen
import music.jiminy.screen.common.TextError
import music.jiminy.viewmodel.ConnectionViewModel

@Composable
fun App(viewModel: () -> ConnectionViewModel) {
    MaterialTheme(darkColorScheme()) {
        val viewModel = viewModel()
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

        LaunchedEffect(selectedTab) { if (selectedTab != mixerTab) viewModel.mixerDisconnect() }

        val isRecording by viewModel.isRecording.collectAsState()

        Box(Modifier.fillMaxSize()) {
            MainScreen({ viewModel })

            if (isRecording) {
                RecordingOverlay(onStopRequest = viewModel::stopRecording)
            }

            // TODO : connectionStatus
        }
    }
}

@Composable
fun MainScreen(
    viewModel: () -> ConnectionViewModel,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    val screenModifier = Modifier
        .verticalScroll(scrollState)
        .background(MaterialTheme.colorScheme.background)
        .padding(12.dp)

    selectedTab?.let { selectedTab ->
        val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab.index,
                containerColor = TabRowDefaults.primaryContainerColor,
                contentColor = TabRowDefaults.primaryContentColor,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(selectedTab.index, matchContentSize = false)
                    )
                },
                divider = { HorizontalDivider() },
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab.index == tab.index,
                        onClick = { viewModel.selectTab(tab.index) },
                        text = tab.title,
                    )
                }
            }

            errorMsg?.let { TextError(it) }

            selectedTab.content({ viewModel }, screenModifier)
        }
    } ?: TextError("Error while loading the tabs")
}

@Composable
fun TabTitle(icon: ImageVector, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
//        TextTitle(title)
    }
}

@Composable
fun AudioLinksMainScreen(
    viewModel: () -> ConnectionViewModel,
    screenModifier: Modifier = Modifier,
) {
    val viewModel = viewModel()
    val allDevices by viewModel.devices.collectAsStateWithLifecycle()
    val allLinks by viewModel.links.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getDevices()
        viewModel.getDeviceLinks()
    }

    ConnectionScreen(
        devices = { allDevices },
        links = { allLinks },
        connect = viewModel::connect,
        disconnect = viewModel::disconnect,
        modifier = screenModifier,
    )
}

@Composable
fun MixerMainScreen(
    viewModel: () -> ConnectionViewModel,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val succeededCommands by viewModel.succeededCommands.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getDevices()
        viewModel.mixerConnect()
    }

    MixerScreen(
        devices = { devices.filter { it.volumes.isNotEmpty() } },
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
    viewModel: () -> ConnectionViewModel,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.getDevices()
    }

    RecordingScreen(
        devices = { devices.filter { it.instruments.isNotEmpty() } },
        startRecording = viewModel::startRecording,
        modifier = modifier,
    )
}
