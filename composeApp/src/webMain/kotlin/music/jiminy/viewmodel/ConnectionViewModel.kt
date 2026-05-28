package music.jiminy.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.DEBOUNCING_COMMAND_MILLIS
import music.jiminy.JiminyAudioDevice
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDeviceI
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeI
import music.jiminy.JiminyDeviceNodeType
import music.jiminy.JiminyLink
import music.jiminy.JiminyLoggerI
import music.jiminy.JiminyMidiDevice
import music.jiminy.JiminyMidiDeviceNode
import music.jiminy.SELECTED_TAB_INDEX_KEY
import music.jiminy.service.JiminyConnectionStatus
import music.jiminy.service.JiminyResponse
import music.jiminy.service.MainService
import kotlin.time.Duration.Companion.milliseconds

class ConnectionViewModel(
    private val mainService: MainService,
    private val logger: JiminyLoggerI,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?>
        get() = _errorMessage

    private var preferredTabIndex =
        kotlinx.browser.window.localStorage.getItem(SELECTED_TAB_INDEX_KEY)?.toIntOrNull()

    enum class RecordingStatus {
        Idle,
        Recording,
        Stopping
    }

    private val _isStoppingRecording = MutableStateFlow(false)
    val recordingStatus = combine(
        mainService.isRecording,
        _isStoppingRecording,
    ) { isRecording, isStopping ->
        when {
            isStopping -> RecordingStatus.Stopping
            isRecording -> RecordingStatus.Recording
            else -> RecordingStatus.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RecordingStatus.Idle)

    fun resetError() {
        _errorMessage.update { null }
    }

    private fun handleError(error: JiminyResponse) {
        when (error) {
            is JiminyResponse.Error -> _errorMessage.update { error.message }
            is JiminyResponse.Cancelled -> _errorMessage.update { "Operation cancelled" }
            else -> Unit
        }
    }

    //// TAB
    class JiminyTab(
        val index: Int,
        val title: @Composable () -> Unit,
        val content: @Composable (modifier: Modifier) -> Unit,
        val isScrollable: Boolean,
    )

    private val _tabs = MutableStateFlow(emptyList<JiminyTab>())
    val tabs: StateFlow<List<JiminyTab>>
        get() = _tabs

    private val _selectedTab = MutableStateFlow<JiminyTab?>(null)
    val selectedTab: StateFlow<JiminyTab?>
        get() = _selectedTab

    fun addTab(
        title: @Composable () -> Unit,
        isScrollable: Boolean = true,
        content: @Composable (modifier: Modifier) -> Unit,
    ): JiminyTab {
        val tab = JiminyTab(_tabs.value.count(), title, content, isScrollable)
        _tabs.update { tabs -> tabs + tab }

        when (preferredTabIndex) {
            null -> if (_selectedTab.value == null) {
                _selectedTab.update { tab }
            }

            else -> if (tab.index == preferredTabIndex) {
                _selectedTab.update { tab }
            }
        }

        return tab
    }

    fun selectTab(index: Int) {
        val tab = tabs.value.find { tab -> tab.index == index }
        _selectedTab.update { tab }
        kotlinx.browser.window.localStorage.setItem(SELECTED_TAB_INDEX_KEY, index.toString())
    }

    //// Mixer View

    private var collectMixerUpdatesJob: Job? = null
    val connectionStatus: StateFlow<JiminyConnectionStatus>
        get() = mainService.connectionStatus

    fun mixerConnect() {
        resetError()

        viewModelScope.launch { mainService.mixerConnect() }

        viewModelScope.launch {
            // Receiving from the server
            collectMixerUpdatesJob = launch {
                mainService.succeededCommands.collect { jiminyCommand ->
                    _succeededCommands.update { jiminyCommand }
                }
            }
        }
    }

    private var job: Job? = null
    fun mixerSendCommand(command: JiminyCommand) {
        resetError()

        if (job == null) {
            job = viewModelScope.launch {
                mainService.mixerSendCommand(command)
                delay(DEBOUNCING_COMMAND_MILLIS.milliseconds)
                job = null
            }
            job?.start()
        }
    }

    fun mixerDisconnect() {
        resetError()
        collectMixerUpdatesJob?.cancel(CancellationException("Mixer disconnected"))
        viewModelScope.launch {
            mainService.mixerDisconnect()
        }
    }

    // This is coming from the Server
    private val _succeededCommands = MutableStateFlow<JiminyCommand?>(null)
    val succeededCommands = _succeededCommands.asStateFlow()

    //// Connection View
    private val _devices = MutableStateFlow(emptyList<JiminyAudioDevice>())
    val devices: StateFlow<List<JiminyAudioDevice>>
        get() = _devices

    fun getDevices() {
        resetError()
        viewModelScope.launch {
            _devices.update { emptyList() }
            mainService.getDevices(
                { response ->
                    _devices.update { response.value.audioDevices }
                },
                ::handleError,
            )
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh(
        connectionScreenViewModel: ConnectionScreenViewModel,
        midiScreenViewModel: MIDIScreenViewModel,
        recordingScreenViewModel: RecordingScreenViewModel,
        logsViewModel: LogsViewModel,
    ) {
        viewModelScope.launch {
            _isRefreshing.update { true }

            // Refresh data in all relevant ViewModels
            getDevices()
            connectionScreenViewModel.loadData()
            midiScreenViewModel.loadData()
            recordingScreenViewModel.loadData()
            logsViewModel.loadServerLogs()

            // Artificial delay to show the refresh indicator
            delay(500.milliseconds)
            _isRefreshing.update { false }
        }
    }

    fun stopRecording() = viewModelScope.launch {
        _isStoppingRecording.update { true }
        mainService.stopRecording(onError = ::handleError)
        _isStoppingRecording.update { false }
    }
}

fun <N : JiminyDeviceNodeI> Pair<N, N>.speaker() =
    if (first.type == JiminyDeviceNodeType.Speaker) first else second

fun <N : JiminyDeviceNodeI> Pair<N, N>.instrument() =
    if (first.type == JiminyDeviceNodeType.Instrument) first else second

fun List<Pair<JiminyDeviceNode, JiminyDeviceNode>>.toJiminyLinks(): List<JiminyLink<JiminyAudioDevice>> =
    toGenericJiminyLinks(
        deviceFactory = { JiminyAudioDevice(it) },
        nodeAdder = { dev, node -> dev.addNode(node) }
    )

fun List<Pair<JiminyMidiDeviceNode, JiminyMidiDeviceNode>>.toJiminyMidiLinks(): List<JiminyLink<JiminyMidiDevice>> =
    toGenericJiminyLinks(
        deviceFactory = { JiminyMidiDevice(it) },
        nodeAdder = { dev, node -> dev.addNode(node) }
    )

fun <T : JiminyDeviceI<T>, N : JiminyDeviceNodeI> List<Pair<N, N>>.toGenericJiminyLinks(
    deviceFactory: (String) -> T,
    nodeAdder: (T, N) -> Unit
): List<JiminyLink<T>> =
    sortedBy { it.speaker().fullName }.takeIf { isNotEmpty() }?.run {
        val list = mutableListOf<JiminyLink<T>>()
        val devices = mutableListOf<T>()

        var speakerDev = deviceFactory(first().speaker().deviceName)
            .apply { nodeAdder(this, first().speaker()) }
        val speakers = mutableListOf(speakerDev)

        forEach {
            if (speakerDev.speakers.none { s -> s.fullName == it.speaker().fullName }) {
                list.add(JiminyLink(devices.toList(), speakerDev))
                devices.clear()
                speakerDev = deviceFactory(it.speaker().deviceName)
                    .apply { nodeAdder(this, it.speaker()) }
                    .also { newSpk -> speakers += newSpk }
            }

            val instrumentDev = devices.find { dev -> dev.name == it.instrument().deviceName }
                ?: deviceFactory(it.instrument().deviceName).also { dev -> devices.add(dev) }
            if (instrumentDev.instruments.none { inst -> inst.fullName == it.instrument().fullName }) {
                nodeAdder(instrumentDev, it.instrument())
            }
        }
        speakerDev.let { list.add(JiminyLink(devices.toList(), speakerDev)) }

        val links = mutableListOf<JiminyLink<T>>()
        list.firstOrNull()?.let { first ->
            var cur = first
            list.forEachIndexed { index, _ ->
                list.elementAtOrNull(index + 1)?.let { next ->
                    if (cur.speakerDevice.name == next.speakerDevice.name &&
                        cur.instrumentDevices.nodes().map { it.fullName }
                            .containsAll(next.instrumentDevices.nodes().map { it.fullName })
                    ) {
                        cur = JiminyLink(
                            instrumentDevices = cur.instrumentDevices,
                            speakerDevice = cur.speakerDevice + next.speakerDevice,
                        )
                    } else {
                        links.add(cur)
                        cur = next
                    }
                } ?: links.add(cur)
            }
        }

        links
    } ?: emptyList()

fun List<JiminyDeviceI<*>>.nodes() = flatMap { it.nodes() }
