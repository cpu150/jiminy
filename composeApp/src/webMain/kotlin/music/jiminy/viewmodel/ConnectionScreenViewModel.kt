package music.jiminy.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDeviceNode
import music.jiminy.LinkType
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.disconnectionNodesList
import music.jiminy.screen.ConnectionScreenAction
import music.jiminy.screen.ConnectionScreenAction.OnAddRowClick
import music.jiminy.screen.ConnectionScreenAction.OnConfirmUnlinkAll
import music.jiminy.screen.ConnectionScreenAction.OnConnectClick
import music.jiminy.screen.ConnectionScreenAction.OnDeleteDeviceFromRow
import music.jiminy.screen.ConnectionScreenAction.OnDeleteNodeFromRow
import music.jiminy.screen.ConnectionScreenAction.OnDeleteRowClick
import music.jiminy.screen.ConnectionScreenAction.OnDeviceDrag
import music.jiminy.screen.ConnectionScreenAction.OnDeviceDragEnd
import music.jiminy.screen.ConnectionScreenAction.OnDeviceDragStart
import music.jiminy.screen.ConnectionScreenAction.OnDisconnectClick
import music.jiminy.screen.ConnectionScreenAction.OnDismissAddDevicePopup
import music.jiminy.screen.ConnectionScreenAction.OnDismissDeleteAllAlert
import music.jiminy.screen.ConnectionScreenAction.OnDismissError
import music.jiminy.screen.ConnectionScreenAction.OnDismissIncompletePopup
import music.jiminy.screen.ConnectionScreenAction.OnNodesSelected
import music.jiminy.screen.ConnectionScreenAction.OnUnlinkAllClick
import music.jiminy.screen.ConnectionScreenNodeType.Instrument
import music.jiminy.screen.ConnectionScreenNodeType.Speaker
import music.jiminy.screen.ConnectionScreenState
import music.jiminy.screen.common.ConnectionScreenZoneItem
import music.jiminy.screen.common.addNodes
import music.jiminy.screen.common.isCompleted
import music.jiminy.screen.common.nodes
import music.jiminy.screen.common.removeNode
import music.jiminy.screen.instruments
import music.jiminy.screen.speakers
import music.jiminy.service.JiminyResponse
import music.jiminy.service.MainService

class ConnectionScreenViewModel(
    private val mainService: MainService,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?>
        get() = _errorMessage

    private val _internalState = MutableStateFlow(ConnectionScreenState())
    val state: StateFlow<ConnectionScreenState> = _internalState.asStateFlow()

    fun resetError() {
        _errorMessage.update { null }
    }

    private fun handleError(error: JiminyResponse) = when (error) {
        is JiminyResponse.Error -> _errorMessage.update { error.message }
        is JiminyResponse.Cancelled -> _errorMessage.update { "Operation cancelled" }
        else -> Unit
    }

    fun loadData() {
        resetError()
        viewModelScope.launch {
            mainService.getDevices(
                onSuccess = { response ->
                    _internalState.update { state -> state.copy(devices = response.value.filter { it.name != PW_RECORDER_NAME }) }
                },
                onError = ::handleError,
            )
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    val filteredLinks =
                        response.value.filter { it.speaker().deviceName != PW_RECORDER_NAME }
                    _internalState.update { it.copy(links = filteredLinks.toJiminyLinks()) }
                },
                onError = ::handleError,
            )
        }
    }

    fun onAction(action: ConnectionScreenAction) {
        resetError()
        when (action) {
            is OnDeviceDragStart -> {
                _internalState.update {
                    it.copy(
                        activeDraggingDevice = action.device,
                        dragOffset = action.initialOffset,
                    )
                }
            }

            is OnDeviceDrag -> _internalState.update { it.copy(dragOffset = action.newOffset) }
            is OnDeviceDragEnd -> handleDragEnd(action.finalOffset)

            OnAddRowClick -> {
                if (_internalState.value.connectionRows.lastOrNull()?.isCompleted() != false) {
                    _internalState.update {
                        val new =
                            ConnectionScreenZoneItem(Instrument) to ConnectionScreenZoneItem(Speaker)
                        it.copy(connectionRows = it.connectionRows + new)
                    }
                } else {
                    _internalState.update { it.copy(showIncompletePopup = true) }
                }
            }

            is OnDeleteRowClick -> if (_internalState.value.connectionRows.size > 1) {
                _internalState.update { it.copy(connectionRows = it.connectionRows - action.row) }
            }

            OnConnectClick -> handleConnect()
            is OnDisconnectClick -> disconnect(action.nodes)
            OnUnlinkAllClick -> _internalState.update { it.copy(showDeleteAllAlert = true) }

            OnConfirmUnlinkAll -> {
                val allDisconnections =
                    _internalState.value.links.flatMap { it.disconnectionNodesList(it.speakerDevice) }
                disconnect(allDisconnections)
                _internalState.update { it.copy(showDeleteAllAlert = false) }
            }

            OnDismissError -> _internalState.update { it.copy(showError = null) }
            OnDismissIncompletePopup -> _internalState.update { it.copy(showIncompletePopup = false) }
            OnDismissAddDevicePopup -> _internalState.update { it.copy(showAddDevicePopup = false) }
            OnDismissDeleteAllAlert -> _internalState.update { it.copy(showDeleteAllAlert = false) }

            is OnNodesSelected -> {
                action.zone.addNodes(action.nodes)
                _internalState.update { it.copy(showAddDevicePopup = false) }
            }

            is OnDeleteNodeFromRow -> {
                action.zone.removeNode(action.node)
                // Force update if needed, but SnapshotStateList inside ConnectionScreenZoneItem might handle it
                _internalState.update { it.copy() }
            }

            is OnDeleteDeviceFromRow -> {
                action.zone.devices.remove(action.device)
                _internalState.update { it.copy() }
            }
        }
    }

    private fun handleDragEnd(finalOffset: Offset) {
        val bufferOffset = 10f
        val state = _internalState.value
        val draggingDevice = state.activeDraggingDevice ?: return

        state.connectionRows.find {
            val instruments = it.instruments()
            val speakers = it.speakers()
            instruments.zone.value.inflate(bufferOffset).contains(finalOffset) ||
                    speakers.zone.value.inflate(bufferOffset).contains(finalOffset)
        }?.let {
            val instruments = it.instruments()
            val droppedInInstrumentsZone =
                instruments.zone.value.inflate(bufferOffset).contains(finalOffset)
            val hasInstruments = draggingDevice.instruments.isNotEmpty()
            val speakers = it.speakers()
            val hasSpeakers = draggingDevice.speakers.isNotEmpty()
            val typeStr = if (droppedInInstrumentsZone) "Instruments" else "Speakers"

            if (droppedInInstrumentsZone && hasInstruments) {
                instruments
            } else if (hasSpeakers) {
                speakers
            } else {
                _internalState.update { it.copy(showError = "No $typeStr for \"${draggingDevice.displayName}\"") }
                null
            }
        }?.also { zone ->
            _internalState.update {
                it.copy(
                    lastDropItem = zone to draggingDevice,
                    showAddDevicePopup = true,
                )
            }
        }
        _internalState.update { it.copy(activeDraggingDevice = null) }
    }

    private fun handleConnect() {
        val rows = _internalState.value.connectionRows
        val incompletedRow = rows.find { !it.isCompleted() }

        resetError()

        if ((incompletedRow == null) && rows.isNotEmpty()) {
            val connections = mutableListOf<Pair<JiminyDeviceNode, JiminyDeviceNode>>()
            rows.forEach { row ->
                row.speakers().nodes().forEach { speaker ->
                    row.instruments().nodes().forEach { instrument ->
                        connections += speaker to instrument
                    }
                }
            }
            if (connections.isNotEmpty()) {
                connect(connections)
                _internalState.update {
                    it.copy(
                        connectionRows = listOf(
                            ConnectionScreenZoneItem(Instrument) to ConnectionScreenZoneItem(Speaker)
                        )
                    )
                }
            }
        } else {
            _internalState.update { it.copy(showIncompletePopup = true) }
        }
    }

    private fun connect(connections: List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) {
        resetError()
        deviceLinks(connections, LinkType.Connect)
    }

    private fun disconnect(connections: List<Pair<JiminyDeviceNode, JiminyDeviceNode>>) {
        resetError()
        deviceLinks(connections, LinkType.Disconnect)
    }

    private fun deviceLinks(
        links: List<Pair<JiminyDeviceNode, JiminyDeviceNode>>,
        type: LinkType,
    ) = viewModelScope.launch {
        val linksMap = links.map {
            JiminyCommand.Link(it.instrument().fullName, it.speaker().fullName, type)
        }

        mainService.deviceLinks(
            links = linksMap,
            onError = { },
            finally = { loadData() },
        )
    }
}
