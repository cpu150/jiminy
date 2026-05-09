package music.jiminy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.LinkType
import music.jiminy.PW_RECORDER_CHANNEL_COUNT
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.recorders
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
        loadDevices()
        loadLinks()
    }

    private fun loadDevices() {
        viewModelScope.launch {
            mainService.getDevices(
                onSuccess = { response ->
                    val devices = response.value
                    val recorder = devices.find { it.name == PW_RECORDER_NAME }
                    _state.update {
                        it.copy(
                            devices = devices.filter { dev -> (dev.instruments.isNotEmpty() && dev.name != PW_RECORDER_NAME) },
                            recorderDevice = recorder,
                        )
                    }
                },
                onError = ::handleError,
            )
        }
    }

    private fun loadLinks() {
        viewModelScope.launch {
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    val allLinks = response.value
                    val recorderLinks = allLinks.filter { it.second.deviceName == PW_RECORDER_NAME }

                    val pairs = mutableListOf<Pair<JiminyDevice, JiminyDeviceNode>>()
                    repeat(PW_RECORDER_CHANNEL_COUNT) { index ->
                        val portName = recorders.getOrNull(index)
                        val link = recorderLinks
                            .takeIf { !portName.isNullOrBlank() }
                            ?.find { it.second.portName == portName }

                        if (link != null) {
                            val instrumentNode = link.first
                            val device =
                                _state.value.devices.find { it.name == instrumentNode.deviceName }
                                    ?: JiminyDevice(instrumentNode.deviceName)
                            pairs.add(device to instrumentNode)
                        }
                    }

                    _state.update { it.copy(selectedDevNodePairs = pairs) }
                },
                onError = ::handleError,
            )
        }
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
        val isSelected = _state.value.selectedDevNodePairs.any { it.second == node }
        if (isSelected) {
            removeNode(node)
        } else {
            addNode(node)
        }
    }

    private fun addNode(node: JiminyDeviceNode) {
        val recorder = _state.value.recorderDevice ?: return

        viewModelScope.launch {
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    val recorderLinks =
                        response.value.filter { it.second.deviceName == PW_RECORDER_NAME }
                    val usedSlots = recorderLinks.map { it.second.portName }

                    var targetPortName: String? = null
                    for (i in 0 until PW_RECORDER_CHANNEL_COUNT) {
                        val portName = recorders.getOrNull(i)
                        if (portName !in usedSlots) {
                            targetPortName = portName
                            break
                        }
                    }

                    if (targetPortName != null) {
                        val recorderNode = recorder.speakers.find { it.portName == targetPortName }
                        if (recorderNode != null) {
                            val links = listOf(
                                JiminyCommand.Link(
                                    node.fullName,
                                    recorderNode.fullName,
                                    LinkType.Connect,
                                )
                            )
                            viewModelScope.launch {
                                mainService.deviceLinks(
                                    links = links,
                                    onError = ::handleError,
                                ) { loadLinks() }
                            }
                        }
                    }
                },
                onError = ::handleError,
            )
        }
    }

    private fun removeNode(node: JiminyDeviceNode) {
        viewModelScope.launch {
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    val link =
                        response.value.find { it.first == node && it.second.deviceName == PW_RECORDER_NAME }
                    if (link != null) {
                        val links = listOf(
                            JiminyCommand.Link(
                                node.fullName,
                                link.second.fullName,
                                LinkType.Disconnect,
                            )
                        )
                        viewModelScope.launch {
                            mainService.deviceLinks(
                                links = links,
                                onError = ::handleError,
                                finally = { loadLinks() },
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
            mainService.startRecording(nodes = JiminyCommand.StartRecording, onError = ::handleError)
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            mainService.stopRecording(onError = ::handleError)
        }
    }
}
