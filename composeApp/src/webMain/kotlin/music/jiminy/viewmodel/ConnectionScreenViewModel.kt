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
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLoggerI
import music.jiminy.LinkType
import music.jiminy.NodeConnection
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.disconnectionNodesList
import music.jiminy.screen.ConnectionScreenAction
import music.jiminy.screen.ConnectionScreenAction.OnConfirmUnlinkAll
import music.jiminy.screen.ConnectionScreenAction.OnConnectClick
import music.jiminy.screen.ConnectionScreenAction.OnDeleteDeviceFromRow
import music.jiminy.screen.ConnectionScreenAction.OnDeleteNodeFromRow
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
import music.jiminy.screen.common.isEmpty
import music.jiminy.screen.common.nodes
import music.jiminy.screen.common.removeNode
import music.jiminy.screen.instruments
import music.jiminy.screen.speakers
import music.jiminy.service.JiminyResponse
import music.jiminy.service.MainService

class ConnectionScreenViewModel(
    private val mainService: MainService,
    private val logger: JiminyLoggerI,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?>
        get() = _errorMessage

    private val _internalState = MutableStateFlow(ConnectionScreenState())
    val state: StateFlow<ConnectionScreenState> = _internalState.asStateFlow()

    init {
        viewModelScope.launch {
            mainService.audioDevices.collect { devices ->
                _internalState.update { state ->
                    state.copy(
                        devices = devices.filter { it.name != PW_RECORDER_NAME },
                    )
                }
            }
        }
        ensureOneEmptyRow()
    }

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
            mainService.refreshDevices(onError = ::handleError)
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    val filteredLinks =
                        response.value.filter { it.speaker.deviceName != PW_RECORDER_NAME }
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
            is OnDeviceDragEnd -> {
                handleDragEnd(action.finalOffset)
                ensureOneEmptyRow()
            }

            is OnConnectClick -> handleConnect()
            is OnDisconnectClick -> disconnect(action.nodes)
            is OnUnlinkAllClick -> _internalState.update { it.copy(showDeleteAllAlert = true) }

            is OnConfirmUnlinkAll -> {
                val allDisconnections =
                    _internalState.value.links.flatMap { it.disconnectionNodesList(it.speakerDevice) }
                disconnect(allDisconnections)
                _internalState.update { it.copy(showDeleteAllAlert = false) }
            }

            is OnDismissError -> _internalState.update { it.copy(showError = null) }
            is OnDismissIncompletePopup -> _internalState.update { it.copy(showIncompletePopup = false) }
            is OnDismissAddDevicePopup -> _internalState.update { it.copy(showAddDevicePopup = false) }
            is OnDismissDeleteAllAlert -> _internalState.update { it.copy(showDeleteAllAlert = false) }

            is OnNodesSelected -> {
                action.zone.addNodes(
                    nodes = action.nodes,
                    factory = { JiminyDevice(it, JiminyDeviceType.Audio) },
                )
                _internalState.update { it.copy(showAddDevicePopup = false) }
                ensureOneEmptyRow()
            }

            is OnDeleteNodeFromRow -> {
                action.zone.removeNode(action.node)
                _internalState.update { it.copy() }
                ensureOneEmptyRow()
            }

            is OnDeleteDeviceFromRow -> {
                action.zone.devices.remove(action.device)
                _internalState.update { it.copy() }
                ensureOneEmptyRow()
            }
        }
    }

    private fun handleDragEnd(finalOffset: Offset) {
        val bufferOffset = 10f
        val state = _internalState.value
        state.activeDraggingDevice?.let { draggingDevice ->

            state.connectionRows.find {
                val instruments = it.instruments()
                val speakers = it.speakers()
                instruments.zone.value.inflate(bufferOffset).contains(finalOffset) ||
                        speakers.zone.value.inflate(bufferOffset).contains(finalOffset)
            }?.let { pair ->
                val instruments = pair.instruments()
                val droppedInInstrumentsZone =
                    instruments.zone.value.inflate(bufferOffset).contains(finalOffset)
                val hasInstruments = draggingDevice.instruments.isNotEmpty()
                val speakers = pair.speakers()
                val hasSpeakers = draggingDevice.speakers.isNotEmpty()

                when {
                    droppedInInstrumentsZone && hasInstruments -> instruments
                    !droppedInInstrumentsZone && hasSpeakers -> speakers
                    else -> {
                        _internalState.update {
                            it.copy(showError = "No nodes for \"${draggingDevice.displayName}\"")
                        }
                        null
                    }
                }
            }?.also { zone ->
                val availableNodes = if (zone.type == Speaker) {
                    draggingDevice.speakers
                } else {
                    draggingDevice.instruments
                }

                if (availableNodes.size == 1) {
                    zone.addNodes(
                        nodes = availableNodes,
                        factory = { JiminyDevice(it, JiminyDeviceType.Audio) },
                    )
                    _internalState.update { it.copy() }
                } else {
                    _internalState.update {
                        it.copy(
                            lastDropItem = zone to draggingDevice,
                            showAddDevicePopup = true,
                        )
                    }
                }
            }
            _internalState.update { it.copy(activeDraggingDevice = null) }
        }
    }

    private fun handleConnect() {
        val rows = _internalState.value.connectionRows
        val incompletedRow = (rows - rows.lastOrNull()).find { it?.isCompleted() == false }

        resetError()

        if ((incompletedRow == null) && rows.isNotEmpty()) {
            val connections = mutableListOf<NodeConnection>()
            rows.forEach { row ->
                row.speakers().nodes().forEach { speaker ->
                    row.instruments().nodes().forEach { instrument ->
                        connections += NodeConnection(instrument, speaker)
                    }
                }
            }
            if (connections.isNotEmpty()) {
                connect(connections)
                _internalState.update {
                    it.copy(
                        connectionRows = listOf(
                            ConnectionScreenZoneItem(Instrument) to
                                    ConnectionScreenZoneItem(Speaker)
                        )
                    )
                }
                ensureOneEmptyRow()
            }
        } else {
            _internalState.update { it.copy(showIncompletePopup = true) }
        }
    }

    private fun ensureOneEmptyRow() {
        val rows = _internalState.value.connectionRows
        if (rows.count { it.isEmpty() } != 1 || !rows.last().isEmpty()) {
            _internalState.update { state ->
                val rows = state.connectionRows
                val filteredRows = rows.filter { !it.isEmpty() }
                val newRows = filteredRows + (ConnectionScreenZoneItem(Instrument) to
                        ConnectionScreenZoneItem(Speaker))
                state.copy(connectionRows = newRows)
            }
        }
    }

    private fun connect(connections: List<NodeConnection>) {
        resetError()
        deviceLinks(connections, LinkType.Connect)
    }

    private fun disconnect(connections: List<NodeConnection>) {
        resetError()
        deviceLinks(connections, LinkType.Disconnect)
    }

    private fun deviceLinks(
        links: List<NodeConnection>,
        type: LinkType,
    ) = viewModelScope.launch {
        val linksMap = links.map {
            JiminyCommand.Link(it.instrument.fullName, it.speaker.fullName, type)
        }

        mainService.deviceLinks(
            links = linksMap,
            onError = { },
            finally = { loadData() },
        )
    }
}
