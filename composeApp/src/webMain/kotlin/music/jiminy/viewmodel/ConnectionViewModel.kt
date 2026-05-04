package music.jiminy.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.DEBOUNCING_COMMAND_MILLIS
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType
import music.jiminy.JiminyLink
import music.jiminy.LinkType
import music.jiminy.Recorder
import music.jiminy.screen.instruments
import music.jiminy.screen.speakers
import music.jiminy.service.JiminyConnectionStatus
import music.jiminy.service.JiminyResponse
import music.jiminy.service.MainService
import kotlin.time.Duration.Companion.milliseconds

class ConnectionViewModel(
    private val mainService: MainService,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?>
        get() = _errorMessage

    val isRecording: StateFlow<Boolean>
        get() = mainService.isRecording

    fun resetError() {
        _errorMessage.update { null }
    }

    private fun handleError(error: JiminyResponse) = when (error) {
        is JiminyResponse.Error -> _errorMessage.update { error.message }
        is JiminyResponse.Cancelled -> _errorMessage.update { "Operation cancelled" }
        else -> Unit
    }

    //// TAB
    class JiminyTab(
        val index: Int,
        val title: @Composable () -> Unit,
        val content: @Composable (viewModel: () -> ConnectionViewModel, modifier: Modifier) -> Unit,
    )

    private val _tabs = MutableStateFlow(emptyList<JiminyTab>())
    val tabs: StateFlow<List<JiminyTab>>
        get() = _tabs

    private val _selectedTab = MutableStateFlow<JiminyTab?>(null)
    val selectedTab: StateFlow<JiminyTab?>
        get() = _selectedTab

    fun addTab(
        title: @Composable () -> Unit,
        content: @Composable (viewModel: () -> ConnectionViewModel, modifier: Modifier) -> Unit,
    ) = JiminyTab(_tabs.value.count(), title, content)
        .also { tab -> _tabs.update { tabs -> tabs + tab } }
        .takeIf { _selectedTab.value == null }
        ?.also { tab -> _selectedTab.update { tab } }

    fun selectTab(index: Int) =
        _selectedTab.update { tabs.value.find { tab -> tab.index == index } }

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
    private val _devices = MutableStateFlow(emptyList<JiminyDevice>())
    val devices: StateFlow<List<JiminyDevice>>
        get() = _devices

    private val _links = MutableStateFlow(emptyList<JiminyLink>())
    val links: StateFlow<List<JiminyLink>>
        get() = _links

    fun getDevices() {
        resetError()
        viewModelScope.launch {
            _devices.update { emptyList() }
            mainService.getDevices(
                { response -> _devices.update { response.value } },
                ::handleError,
            )
        }
    }

    fun getDeviceLinks() {
        resetError()
        viewModelScope.launch {
            mainService.getDeviceLinks(
                { response -> _links.update { response.value.toJiminyLinks() } },
                ::handleError,
            )
        }
    }

    fun connect(connections: List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) =
        deviceLinks(connections, LinkType.Connect)

    fun disconnect(connections: List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) =
        deviceLinks(connections, LinkType.Disconnect)

    private fun deviceLinks(
        links: List<Pair<JiminyDeviceNode, JiminyDeviceNode>>,
        type: LinkType,
    ) = viewModelScope.launch {
        resetError()

        val linksMap = links.map {
            JiminyCommand.Link(it.instruments().fullName, it.speakers().fullName, type)
        }

        mainService.deviceLinks(
            links = linksMap,
            onError = ::handleError,
            finally = ::getDeviceLinks,
        )
    }

    /// Recording
    fun startRecording(nodes: List<JiminyDeviceNode>) = viewModelScope.launch {
        val recordings = JiminyCommand.StartRecording(
            recoders = nodes.map {
                val label = "${it.displayName} ${it.displayPortName}"
                Recorder(label, it.fullName)
            }
        )
        mainService.startRecording(
            nodes = recordings,
            onError = ::handleError,
        )
    }

    fun stopRecording() = viewModelScope.launch {
        mainService.stopRecording(onError = ::handleError)
    }
}

fun Pair<JiminyDeviceNode, JiminyDeviceNode>.speaker() =
    if (first.type == JiminyDeviceNodeType.Speaker) first else second

fun Pair<JiminyDeviceNode, JiminyDeviceNode>.instrument() =
    if (first.type == JiminyDeviceNodeType.Instrument) first else second

fun List<Pair<JiminyDeviceNode, JiminyDeviceNode>>.toJiminyLinks() =
    sortedBy { it.speaker().fullName }.takeIf { isNotEmpty() }?.run {
        val list = mutableListOf<JiminyLink>()
        val devices = mutableListOf<JiminyDevice>()

        var speakerDev = JiminyDevice(first().speaker().deviceName)
            .apply { addNode(first().speaker()) }
        val speakers = mutableListOf(speakerDev)

        forEach {
            if (!speakerDev.speakers.contains(it.speaker())) {
                list.add(JiminyLink(devices.toList(), speakerDev))
                devices.clear()
                speakerDev = JiminyDevice(it.speaker().deviceName)
                    .apply { addNode(it.speaker()) }
                    .also { newSpk -> speakers += newSpk }
            }

            val instrumentDev = devices.find { dev -> dev.name == it.instrument().deviceName }
                ?: JiminyDevice(it.instrument().deviceName).also { dev -> devices.add(dev) }
            if (!instrumentDev.instruments.contains(it.instrument())) {
                instrumentDev.addNode(it.instrument())
            }
        }
        speakerDev.let { list.add(JiminyLink(devices.toList(), speakerDev)) }

        val links = mutableListOf<JiminyLink>()
        list.firstOrNull()?.let { first ->
            var cur = first
            list.forEachIndexed { index, _ ->
                list.elementAtOrNull(index + 1)?.let { next ->
                    if (cur.speakerDevice.name == next.speakerDevice.name &&
                        cur.instrumentDevices.nodes().containsAll(next.instrumentDevices.nodes())
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

fun List<JiminyDevice>.nodes() = flatMap { it.nodes() }
