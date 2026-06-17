package music.jiminy.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.browser.window
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.DEBOUNCING_COMMAND_MILLIS
import music.jiminy.FLUIDSYNTH_MIDI_NAME
import music.jiminy.JiminyCommand
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.JiminyLoggerI
import music.jiminy.JiminyVolume
import music.jiminy.LinkType
import music.jiminy.MIDI_BRIDGE_PREFIX
import music.jiminy.NodeConnection
import music.jiminy.SELECTED_TAB_INDEX_KEY
import music.jiminy.SaveConfigData
import music.jiminy.SaveConfigOptions
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
        window.localStorage.getItem(SELECTED_TAB_INDEX_KEY)?.toIntOrNull()

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
        window.localStorage.setItem(SELECTED_TAB_INDEX_KEY, index.toString())
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

    val isUpdateAvailable: StateFlow<Boolean> = combine(
        mainService.latestVersion,
        MutableStateFlow(music.jiminy.getPlatform().version)
    ) { latest, current ->
        latest.isNotBlank() && latest != current
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    private val _loadedConfiguration = MutableSharedFlow<JiminyConfiguration>()
    val loadedConfiguration: SharedFlow<JiminyConfiguration> = _loadedConfiguration.asSharedFlow()

    private val _showSaveConfigPopup = MutableStateFlow(false)
    val showSaveConfigPopup: StateFlow<Boolean> = _showSaveConfigPopup.asStateFlow()

    private val _showLoadConfigPopup = MutableStateFlow(false)
    val showLoadConfigPopup: StateFlow<Boolean> = _showLoadConfigPopup.asStateFlow()

    private val _showShutdownPopup = MutableStateFlow(false)
    val showShutdownPopup: StateFlow<Boolean> = _showShutdownPopup.asStateFlow()

    private val _showUpdatePopup = MutableStateFlow(false)
    val showUpdatePopup: StateFlow<Boolean> = _showUpdatePopup.asStateFlow()

    private val _showOverwriteConfigPopup = MutableStateFlow<SaveConfigData?>(null)
    val showOverwriteConfigPopup: StateFlow<SaveConfigData?> =
        _showOverwriteConfigPopup.asStateFlow()

    fun onSaveConfigClick() {
        _configurationsState.update { LoadConfigState.Loading }
        _showSaveConfigPopup.update { true }

        viewModelScope.launch {
            mainService.getConfigurations(
                onSuccess = { response ->
                    _configurationsState.update { LoadConfigState.Success(response.value.sortedBy { it.lowercase() }) }
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
                    _configurationsState.update { LoadConfigState.Success(response.value.sortedBy { it.lowercase() }) }
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
    }

    fun dismissLoadConfigPopup() {
        _showLoadConfigPopup.update { false }
        _configurationsState.update { LoadConfigState.Idle }
    }

    fun onShutdownClick() {
        _showShutdownPopup.update { true }
    }

    fun onShutdownConfirm() {
        _showShutdownPopup.update { false }
        viewModelScope.launch {
            mainService.shutdown(::handleError)
        }
    }

    fun dismissShutdownPopup() {
        _showShutdownPopup.update { false }
    }

    fun onUpdateClick() {
        _showUpdatePopup.update { true }
    }

    fun onUpdateConfirm() {
        _showUpdatePopup.update { false }
        viewModelScope.launch {
            mainService.updateServer(::handleError)
        }
    }

    fun dismissUpdatePopup() {
        _showUpdatePopup.update { false }
    }

    fun saveConfiguration(
        audioLinks: List<JiminyLink>,
        midiLinks: List<JiminyLink>,
        recordingNodes: List<JiminyDeviceNode>,
        options: SaveConfigOptions,
    ) {
        val data = SaveConfigData(
            options = options,
            audioLinks = audioLinks.toImmutableList(),
            midiLinks = midiLinks.toImmutableList(),
            recordingNodes = recordingNodes.toImmutableList(),
        )
        val existingConfigs = when (val configState = configurationsState.value) {
            is LoadConfigState.Success -> configState.configurations
            else -> emptyList()
        }

        if (existingConfigs.contains(options.name)) {
            _showOverwriteConfigPopup.update { data }
            _showSaveConfigPopup.update { false }
        } else {
            viewModelScope.launch {
                executeSaveConfiguration(data, false)
            }
        }
    }

    fun confirmOverwrite(data: SaveConfigData) {
        val name = _showOverwriteConfigPopup.value
        if (name != null) {
            viewModelScope.launch {
                executeSaveConfiguration(data, true)
                _showOverwriteConfigPopup.update { null }
            }
        }
    }

    private suspend fun executeSaveConfiguration(
        data: SaveConfigData,
        remoteConfigExists: Boolean,
    ) = with(data) {
        val name = options.name

        val save: suspend (JiminyConfiguration?) -> Unit = { remoteConfig ->
            val audioLinks = when (options.saveAudio) {
                true -> audioLinks.flatMap { it.toLink() }
                false -> remoteConfig?.audioLinks ?: emptyList()
            }
            val midiLinks = when (options.saveMidi) {
                true -> midiLinks.flatMap { it.toLink() }
                false -> remoteConfig?.midiLinks ?: emptyList()
            }
            val volumes = when (options.saveVolumes) {
                false -> remoteConfig?.volumes ?: emptyList()
                true -> devices.value.flatMap { device -> device.volumes }
            }
            val recordingNodes = when (options.saveRecordingNodes) {
                true -> recordingNodes
                false -> remoteConfig?.recordingNodes ?: emptyList()
            }

            mainService.saveConfiguration(
                config = JiminyConfiguration(name, audioLinks, midiLinks, volumes, recordingNodes),
                onSuccess = {
                    _showSaveConfigPopup.update { false }
                    _showOverwriteConfigPopup.update { null }
                    logger.info("Configuration \"$name\" successfully saved")
                },
                onError = ::handleError,
            )
        }

        if (remoteConfigExists) {
            mainService.getConfiguration(
                name = name,
                onSuccess = { response -> save(response.value) },
                onError = ::handleError,
            )
        } else {
            save(null)
        }
    }

    private fun JiminyLink.toLink() = instrumentDevices.flatMap { instrument ->
        instrument.instruments.flatMap { instNode ->
            speakerDevice.speakers.map { spkNode ->
                JiminyCommand.Link(instNode.fullName, spkNode.fullName, LinkType.Connect)
            }
        }
    }

    fun loadConfigurations(names: List<String>) {
        viewModelScope.launch {
            _configurationsState.update { LoadConfigState.Loading }

            val configurations = mutableListOf<JiminyConfiguration>()
            var hasError = false
            names.forEach { name ->
                mainService.getConfiguration(
                    name = name,
                    onSuccess = { response ->
                        configurations.add(response.value)
                        _loadedConfiguration.emit(response.value)
                    },
                    onError = { error ->
                        hasError = true
                        handleError(error)
                        val msg = when (error) {
                            is JiminyResponse.Error -> error.message
                            else -> error.toString()
                        }
                        _configurationsState.update { LoadConfigState.Error(msg) }
                    },
                )
            }

            if (!hasError) {
                val links = mutableListOf<JiminyCommand.Link>()
                val volumes = mutableListOf<JiminyVolume>()

                configurations.forEach { config ->
                    links += config.audioLinks + config.midiLinks.mapMidiDevices()
                    volumes += config.volumes
                }

                val batchCommands = mutableListOf<JiminyCommand>()
                if (volumes.isNotEmpty()) {
                    batchCommands += volumes.map { it.toVolumeCommand() }
                    batchCommands += volumes.map { it.toMuteCommand() }
                }
                if (links.isNotEmpty()) {
                    batchCommands += links
                }

                if (batchCommands.isNotEmpty()) {
                    mainService.executeBatch(
                        batch = JiminyCommand.Batch(batchCommands),
                        onSuccess = { dismissLoadConfigPopup() },
                        onError = { error ->
                            handleError(error)
                            val msg = when (error) {
                                is JiminyResponse.Error -> error.message
                                else -> error.toString()
                            }
                            _configurationsState.update { LoadConfigState.Error(msg) }
                        },
                    )
                } else {
                    dismissLoadConfigPopup()
                }
            }
        }
    }

    // HACK - FluidSynth device name change at every boot
    private suspend fun List<JiminyCommand.Link>.mapMidiDevices(): List<JiminyCommand.Link> = let {
        mainService.refreshDevices { logger.error("mapMidiDevices - error while refreshing - $it") }
        mainService.midiDevices.value
    }
        .firstOrNull { it.name.startsWith(FLUIDSYNTH_MIDI_NAME, true) }
        ?.let { it.instruments.firstOrNull()?.fullName to it.speakers.firstOrNull()?.fullName }
        ?.let { (inst, spk) ->
            val prefix = MIDI_BRIDGE_PREFIX + FLUIDSYNTH_MIDI_NAME
            map { link ->
                if (!inst.isNullOrBlank() &&
                    link.instrument.startsWith(prefix, true)
                ) {
                    logger.info("Mapping instrument: ${link.instrument} -> $inst")
                    JiminyCommand.Link(inst, link.speaker)
                } else if (!spk.isNullOrBlank() &&
                    link.speaker.startsWith(prefix, true)
                ) {
                    logger.info("Mapping speaker: ${link.speaker} -> $spk")
                    JiminyCommand.Link(link.instrument, spk)
                } else {
                    link
                }
            }
        } ?: this

    private fun JiminyVolume.toVolumeCommand() = JiminyCommand.VolumeUpdate(this, volume)
    private fun JiminyVolume.toMuteCommand() = JiminyCommand.MuteUpdate(this, mute)

    fun deleteConfigurations(names: List<String>) {
        viewModelScope.launch {
            _configurationsState.update { LoadConfigState.Loading }

            var hasError = false
            names.forEach { name ->
                mainService.deleteConfiguration(
                    name = name,
                    onSuccess = { },
                    onError = { error ->
                        hasError = true
                        handleError(error)
                        val msg = when (error) {
                            is JiminyResponse.Error -> error.message
                            else -> error.toString()
                        }
                        _configurationsState.update { LoadConfigState.Error(msg) }
                    },
                )
            }
            // Refresh the list if no errors
            if (!hasError) {
                onLoadConfigClick()
            }
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
