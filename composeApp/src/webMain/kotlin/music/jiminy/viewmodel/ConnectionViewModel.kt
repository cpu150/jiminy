package music.jiminy.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
import music.jiminy.JiminyCommand
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.JiminyLoggerI
import music.jiminy.LinkType
import music.jiminy.NodeConnection
import music.jiminy.SaveConfigOptions
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
        Stopping,
    }

    private val _isStoppingRecording = MutableStateFlow(value = false)
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
    val devices: StateFlow<List<JiminyDevice>> = mainService.audioDevices

    fun getDevices() {
        resetError()
        viewModelScope.launch {
            mainService.refreshDevices(::handleError)
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

    //// Configuration Management

    sealed interface LoadConfigState {
        object Idle : LoadConfigState
        object Loading : LoadConfigState
        data class Success(val configurations: List<String>) : LoadConfigState
        data class Error(val message: String) : LoadConfigState
    }

    private val _configurationsState = MutableStateFlow<LoadConfigState>(LoadConfigState.Idle)
    val configurationsState: StateFlow<LoadConfigState> = _configurationsState.asStateFlow()

    private val _showSaveConfigPopup = MutableStateFlow(false)
    val showSaveConfigPopup: StateFlow<Boolean> = _showSaveConfigPopup.asStateFlow()

    private val _showLoadConfigPopup = MutableStateFlow(false)
    val showLoadConfigPopup: StateFlow<Boolean> = _showLoadConfigPopup.asStateFlow()

    private val _showOverwriteConfigPopup = MutableStateFlow<String?>(null)
    val showOverwriteConfigPopup: StateFlow<String?> = _showOverwriteConfigPopup.asStateFlow()

    private var pendingLinks: List<JiminyLink> = emptyList()
    private var pendingOptions: SaveConfigOptions = SaveConfigOptions()

    fun onSaveConfigClick() {
        _configurationsState.update { LoadConfigState.Loading }
        _showSaveConfigPopup.update { true }

        viewModelScope.launch {
            mainService.getConfigurations(
                onSuccess = { response ->
                    _configurationsState.update { LoadConfigState.Success(response.value) }
                },
                onError = { error ->
                    val msg = when (error) {
                        is JiminyResponse.Error -> error.message
                        else -> error.toString()
                    }
                    _configurationsState.update { LoadConfigState.Error(msg) }
                },
            )
        }
    }

    fun onLoadConfigClick() {
        _configurationsState.update { LoadConfigState.Loading }
        _showLoadConfigPopup.update { true }

        viewModelScope.launch {
            mainService.getConfigurations(
                onSuccess = { response ->
                    _configurationsState.update { LoadConfigState.Success(response.value) }
                },
                onError = { error ->
                    val msg = when (error) {
                        is JiminyResponse.Error -> error.message
                        else -> error.toString()
                    }
                    _configurationsState.update { LoadConfigState.Error(msg) }
                },
            )
        }
    }

    fun dismissSaveConfigPopup() {
        _showSaveConfigPopup.update { false }
        _configurationsState.update { LoadConfigState.Idle }
    }

    fun dismissOverwriteConfigPopup() {
        _showOverwriteConfigPopup.update { null }
        pendingLinks = emptyList()
    }

    fun dismissLoadConfigPopup() {
        _showLoadConfigPopup.update { false }
        _configurationsState.update { LoadConfigState.Idle }
    }

    fun saveConfiguration(
        name: String,
        currentLinks: List<JiminyLink>,
        options: SaveConfigOptions,
    ) {
        val existingConfigs =
            (configurationsState.value as? LoadConfigState.Success)?.configurations ?: emptyList()
        if (existingConfigs.contains(name)) {
            pendingLinks = currentLinks
            pendingOptions = options
            _showOverwriteConfigPopup.update { name }
            _showSaveConfigPopup.update { false }
        } else {
            executeSaveConfiguration(name, currentLinks, options)
        }
    }

    fun confirmOverwrite() {
        val name = _showOverwriteConfigPopup.value
        if (name != null) {
            executeSaveConfiguration(name, pendingLinks, pendingOptions)
            _showOverwriteConfigPopup.update { null }
            pendingLinks = emptyList()
            pendingOptions = SaveConfigOptions()
        }
    }

    private fun executeSaveConfiguration(
        name: String,
        currentLinks: List<JiminyLink>,
        options: SaveConfigOptions,
    ) {
        val newLinks = currentLinks.flatMap { link ->
            link.instrumentDevices.flatMap { instrument ->
                instrument.instruments.flatMap { instNode ->
                    link.speakerDevice.speakers.map { spkNode ->
                        JiminyCommand.Link(instNode.fullName, spkNode.fullName, LinkType.Connect)
                    }
                }
            }
        }

        viewModelScope.launch {
            val audioNodes = mainService.audioDevices.value.nodes().map { it.fullName }.toSet()
            val midiNodes = mainService.midiDevices.value.nodes().map { it.fullName }.toSet()

            var existingLinks = emptyList<JiminyCommand.Link>()
            val deferred = CompletableDeferred<List<JiminyCommand.Link>>()

            mainService.getConfiguration(
                name = name,
                onSuccess = { response -> deferred.complete(response.value.links) },
                onError = { deferred.complete(emptyList()) },
            )
            existingLinks = deferred.await()

            val filteredExistingLinks = existingLinks.filter { link ->
                val isAudio = audioNodes.contains(link.instrument)
                val isMidi = midiNodes.contains(link.instrument)

                val shouldRemove = (options.saveAudio && isAudio) || (options.saveMidi && isMidi)
                !shouldRemove
            }

            val finalLinks = filteredExistingLinks + newLinks

            mainService.saveConfiguration(
                config = JiminyConfiguration(name, finalLinks),
                onSuccess = {
                    _showSaveConfigPopup.update { false }
                },
                onError = ::handleError,
            )
        }
    }

    fun loadConfigurations(names: List<String>) {
        viewModelScope.launch {
            _configurationsState.update { LoadConfigState.Loading }

            val allLinks = mutableListOf<JiminyCommand.Link>()
            names.forEach { name ->
                mainService.getConfiguration(
                    name = name,
                    onSuccess = { response -> allLinks.addAll(response.value.links) },
                    onError = ::handleError,
                )
            }

            if (allLinks.isNotEmpty()) {
                mainService.deviceLinks(
                    links = allLinks,
                    onSuccess = { dismissLoadConfigPopup() },
                    onError = ::handleError,
                )
            } else {
                dismissLoadConfigPopup()
            }
        }
    }

    fun deleteConfigurations(names: List<String>) {
        viewModelScope.launch {
            _configurationsState.update { LoadConfigState.Loading }

            names.forEach { name ->
                mainService.deleteConfiguration(
                    name = name,
                    onSuccess = { },
                    onError = ::handleError,
                )
            }
            // Refresh the list
            onLoadConfigClick()
        }
    }
}

fun List<NodeConnection>.toJiminyLinks(): List<JiminyLink> =
    toGenericJiminyLinks(JiminyDeviceType.Audio)

fun List<NodeConnection>.toJiminyMidiLinks(): List<JiminyLink> =
    toGenericJiminyLinks(JiminyDeviceType.Midi)

fun List<NodeConnection>.toGenericJiminyLinks(
    type: JiminyDeviceType,
): List<JiminyLink> =
    sortedBy { it.speaker.fullName }.takeIf { isNotEmpty() }?.run {
        val list = mutableListOf<JiminyLink>()
        val devices = mutableListOf<JiminyDevice>()

        var speakerDev = JiminyDevice(first().speaker.deviceName, type)
            .apply { addNode(first().speaker) }
        val speakers = mutableListOf(speakerDev)

        forEach {
            if (speakerDev.speakers.none { s -> s.fullName == it.speaker.fullName }) {
                list.add(JiminyLink(devices.toList(), speakerDev))
                devices.clear()
                speakerDev = JiminyDevice(it.speaker.deviceName, type)
                    .apply { addNode(it.speaker) }
                    .also { newSpk -> speakers += newSpk }
            }

            val instrumentDev = devices.find { dev -> dev.name == it.instrument.deviceName }
                ?: JiminyDevice(it.instrument.deviceName, type).also { dev -> devices.add(dev) }
            if (instrumentDev.instruments.none { inst -> inst.fullName == it.instrument.fullName }) {
                instrumentDev.addNode(it.instrument)
            }
        }
        speakerDev.let { list.add(JiminyLink(devices.toList(), speakerDev)) }

        val links = mutableListOf<JiminyLink>()
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

fun List<JiminyDevice>.nodes() = flatMap { it.nodes() }
