package music.jiminy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyLoggerI
import music.jiminy.LinkType
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.screen.RecordingScreenAction
import music.jiminy.screen.RecordingScreenAction.OnDeviceClick
import music.jiminy.screen.RecordingScreenAction.OnDismissDetails
import music.jiminy.screen.RecordingScreenAction.OnNodeClick
import music.jiminy.screen.RecordingScreenAction.OnStartRecording
import music.jiminy.screen.RecordingScreenAction.OnStopRecording
import music.jiminy.screen.RecordingScreenState
import music.jiminy.service.JiminyResponse
import music.jiminy.service.MainService

class RecordingScreenViewModel(
    private val mainService: MainService,
    private val logger: JiminyLoggerI,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordingScreenState())
    val state: StateFlow<RecordingScreenState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun resetError() {
        _errorMessage.update { null }
    }

    private fun handleError(error: JiminyResponse) {
        if (error is JiminyResponse.Error) {
            _errorMessage.update { error.message }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            loadDevices()
            // Wait for devices (and recorderDevice) to be loaded before fetching links
            loadLinks()
        }
    }

    private suspend fun loadDevices() {
        mainService.getDevices(
            onSuccess = { response ->
                var recorder = response.value.find { it.name == PW_RECORDER_NAME }
                val devices = response.value
                    .filter { dev -> (dev.instruments.isNotEmpty() && dev.name != PW_RECORDER_NAME) }

                if (recorder == null) {
                    _errorMessage.update { "No device named \"$PW_RECORDER_NAME\" found" }
                } else if (recorder.speakers.isEmpty()) {
                    recorder = null
                    _errorMessage.update { "Recorder \"$PW_RECORDER_NAME\" does not contain Speakers" }
                }

                _state.update { state ->
                    state.copy(
                        devices = devices,
                        recorderDevice = recorder,
                    )
                }
            },
            onError = ::handleError,
        )
    }

    private suspend fun loadLinks() {
        mainService.getDeviceLinks(
            onSuccess = { response ->
                val recorderName = _state.value.recorderDevice?.name
                val recorderNodes = _state.value.recorderDevice?.speakers?.map { it.fullName }

                val selectedNodes = response.value
                    .filter { recorderNodes?.contains(it.second.fullName) ?: false }
                    .filter { it.second.deviceName == recorderName }
                    .map { it.first }

                _state.update { state ->
                    state.copy(selectedNodes = selectedNodes)
                }
            },
            onError = ::handleError,
        )
    }

    fun onAction(action: RecordingScreenAction) {
        when (action) {
            is OnDeviceClick -> _state.update { it.copy(showDetails = action.device) }
            is OnNodeClick -> toggleNode(action.node)
            OnStartRecording -> startRecording()
            OnStopRecording -> stopRecording()
            OnDismissDetails -> _state.update { it.copy(showDetails = null) }
        }
    }

    private fun toggleNode(node: JiminyDeviceNode) {
        val isSelected = _state.value.selectedNodes.any { it == node }
        if (isSelected) {
            removeNode(node)
        } else {
            addNode(node)
        }
    }

    private fun addNode(node: JiminyDeviceNode) = viewModelScope.launch {
        val recorder = _state.value.recorderDevice ?: return@launch
        val recorderName = recorder.name

        mainService.getDeviceLinks(
            onSuccess = { response ->
                val usedPortFullNames = response.value
                    .filter { it.second.deviceName == recorderName }
                    .map { it.second.fullName }

                recorder.speakers
                    .find { it.fullName !in usedPortFullNames }
                    ?.let { recorderNode ->
                        val link = JiminyCommand.Link(
                            node.fullName,
                            recorderNode.fullName,
                            LinkType.Connect,
                        )

                        viewModelScope.launch {
                            mainService.deviceLinks(
                                links = listOf(link),
                                onError = ::handleError,
                            ) { viewModelScope.launch { loadLinks() } }
                        }
                    } ?: _errorMessage.update { "No available recording slots" }
            },
            onError = ::handleError,
        )
    }

    private fun removeNode(node: JiminyDeviceNode) {
        val recorderName = _state.value.recorderDevice?.name ?: return
        viewModelScope.launch {
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    response.value
                        .find { it.first == node && it.second.deviceName == recorderName }
                        ?.let { link ->
                            val link = JiminyCommand.Link(
                                node.fullName,
                                link.second.fullName,
                                LinkType.Disconnect,
                            )

                            viewModelScope.launch {
                                mainService.deviceLinks(
                                    links = listOf(link),
                                    onError = ::handleError,
                                    finally = { viewModelScope.launch { loadLinks() } },
                                )
                            }
                        }
                },
                onError = ::handleError,
            )
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            mainService.startRecording(
                nodes = JiminyCommand.StartRecording(_state.value.selectedNodes),
                onError = ::handleError,
            )
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            mainService.stopRecording(onError = ::handleError)
        }
    }
}
