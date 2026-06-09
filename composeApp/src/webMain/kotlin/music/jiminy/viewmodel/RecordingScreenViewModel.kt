package music.jiminy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.JiminyCommand
import music.jiminy.JiminyConfiguration
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyLoggerI
import music.jiminy.PW_RECORDER_CHANNEL_COUNT
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.screen.RecordingScreenAction
import music.jiminy.screen.RecordingScreenAction.OnDeleteRecordings
import music.jiminy.screen.RecordingScreenAction.OnApplyConfiguration
import music.jiminy.screen.RecordingScreenAction.OnDeviceClick
import music.jiminy.screen.RecordingScreenAction.OnDismissDetails
import music.jiminy.screen.RecordingScreenAction.OnDownloadRecordings
import music.jiminy.screen.RecordingScreenAction.OnHideRecordingsClick
import music.jiminy.screen.RecordingScreenAction.OnNodeClick
import music.jiminy.screen.RecordingScreenAction.OnRecordingSelect
import music.jiminy.screen.RecordingScreenAction.OnRecordingsSelect
import music.jiminy.screen.RecordingScreenAction.OnShowRecordingsClick
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

    init {
        viewModelScope.launch {
            mainService.audioDevices.collect { devices ->
                _state.update { state ->
                    state.copy(
                        devices = devices.filter { dev -> (dev.instruments.isNotEmpty() && dev.name != PW_RECORDER_NAME) },
                    )
                }
            }
        }
    }

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
            mainService.refreshDevices(onError = ::handleError)
        }
    }

    fun onAction(action: RecordingScreenAction) {
        when (action) {
            is OnDeviceClick -> _state.update { it.copy(showDetails = action.device) }
            is OnNodeClick -> toggleNode(action.node)
            OnStartRecording -> startRecording()
            OnStopRecording -> stopRecording()
            OnDismissDetails -> _state.update { it.copy(showDetails = null) }
            OnShowRecordingsClick -> showRecordings()
            OnHideRecordingsClick -> _state.update { it.copy(showRecordings = false) }
            is OnRecordingSelect -> toggleRecordingSelection(action.filename)
            is OnRecordingsSelect -> toggleRecordingsSelection(action.filenames)
            OnDownloadRecordings -> downloadRecordings()
            OnDeleteRecordings -> deleteRecordings()
            is OnApplyConfiguration -> applyConfiguration(action.config)
        }
    }

    private fun applyConfiguration(config: JiminyConfiguration) {
        _state.update { state ->
            state.copy(selectedNodes = config.recordingNodes.take(PW_RECORDER_CHANNEL_COUNT))
        }
    }

    private fun showRecordings() {
        viewModelScope.launch {
            mainService.getRecordings(
                onSuccess = { response ->
                    _state.update { state ->
                        state.copy(
                            recordings = response.value,
                            showRecordings = true,
                        )
                    }
                },
                onError = { handleError(it) },
            )
        }
    }

    private fun toggleRecordingSelection(filename: String) {
        _state.update { state ->
            val selected = if (state.selectedRecordings.contains(filename)) {
                state.selectedRecordings - filename
            } else {
                state.selectedRecordings + filename
            }
            state.copy(selectedRecordings = selected)
        }
    }

    private fun toggleRecordingsSelection(filenames: List<String>) {
        _state.update { state ->
            val allSelected = filenames.all { state.selectedRecordings.contains(it) }
            val newSelected = if (allSelected) {
                state.selectedRecordings - filenames.toSet()
            } else {
                state.selectedRecordings + (filenames - state.selectedRecordings.toSet())
            }
            state.copy(selectedRecordings = newSelected)
        }
    }

    private fun deleteRecordings() {
        viewModelScope.launch {
            val toDelete = _state.value.selectedRecordings
            mainService.deleteRecordings(
                filenames = toDelete,
                onSuccess = {
                    _state.update { it.copy(selectedRecordings = emptyList()) }
                    showRecordings()
                },
                onError = { handleError(it) },
            )
        }
    }

    private fun downloadRecordings() {
        viewModelScope.launch {
            val toDownload = _state.value.selectedRecordings
            mainService.downloadRecordings(
                filenames = toDownload,
                onSuccess = { response ->
                    // Handle download in browser: we can't easily handle a POST response file download
                    // in pure KMP without some JS hacks.
                    // For now, logging the intent.
                    logger.info("Downloading ${toDownload.size} files: $response")
                },
                onError = { handleError(it) },
            )
        }
    }

    private fun toggleNode(node: JiminyDeviceNode) {
        _state.update { state ->
            val isSelected = state.selectedNodes.any { it.fullName == node.fullName }
            if (isSelected) {
                state.copy(selectedNodes = state.selectedNodes.filter { it.fullName != node.fullName })
            } else {
                if (state.selectedNodes.size < PW_RECORDER_CHANNEL_COUNT) {
                    state.copy(selectedNodes = state.selectedNodes + node)
                } else {
                    _errorMessage.update { "No available recording slots" }
                    state
                }
            }
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
