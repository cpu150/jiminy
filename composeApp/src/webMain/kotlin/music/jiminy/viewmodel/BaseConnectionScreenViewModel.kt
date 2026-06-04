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
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.JiminyLoggerI
import music.jiminy.LinkType
import music.jiminy.NodeConnection
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

abstract class BaseConnectionScreenViewModel(
    protected val mainService: MainService,
    protected val logger: JiminyLoggerI,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    protected val _state = MutableStateFlow(ConnectionScreenState())
    val state: StateFlow<ConnectionScreenState> = _state.asStateFlow()

    protected abstract val deviceType: JiminyDeviceType

    init {
        startCollectingDevices()
        ensureOneEmptyRow()
    }

    protected abstract fun startCollectingDevices()

    fun resetError() {
        _errorMessage.update { null }
    }

    protected fun handleError(error: JiminyResponse) {
        when (error) {
            is JiminyResponse.Error -> _errorMessage.update { error.message }
            is JiminyResponse.Cancelled -> _errorMessage.update { "Operation cancelled" }
            else -> Unit
        }
    }

    fun loadData() {
        resetError()
        viewModelScope.launch {
            mainService.refreshDevices(onError = ::handleError)
            mainService.getDeviceLinks(
                onSuccess = { response ->
                    val filteredLinks = filterLinks(response.value)
                    _state.update { it.copy(links = convertLinks(filteredLinks)) }
                },
                onError = ::handleError,
            )
        }
    }

    protected abstract fun filterLinks(links: List<NodeConnection>): List<NodeConnection>
    protected abstract fun convertLinks(links: List<NodeConnection>): List<JiminyLink>

    fun onAction(action: ConnectionScreenAction) {
        resetError()
        when (action) {
            is OnDeviceDragStart -> {
                _state.update {
                    it.copy(
                        activeDraggingDevice = action.device,
                        dragOffset = action.initialOffset,
                    )
                }
            }

            is OnDeviceDrag -> _state.update { it.copy(dragOffset = action.newOffset) }
            is OnDeviceDragEnd -> {
                handleDragEnd(action.finalOffset)
                ensureOneEmptyRow()
            }

            is OnConnectClick -> handleConnect()
            is OnDisconnectClick -> disconnect(action.nodes)

            is OnUnlinkAllClick -> _state.update { it.copy(showDeleteAllAlert = true) }

            is OnConfirmUnlinkAll -> {
                val allDisconnections =
                    _state.value.links.flatMap { it.disconnectionNodesList(it.speakerDevice) }
                disconnect(allDisconnections)
                _state.update { it.copy(showDeleteAllAlert = false) }
            }

            is OnDismissError -> _state.update { it.copy(showError = null) }
            is OnDismissIncompletePopup -> _state.update { it.copy(showIncompletePopup = false) }
            is OnDismissAddDevicePopup -> _state.update { it.copy(showAddDevicePopup = false) }
            is OnDismissDeleteAllAlert -> _state.update { it.copy(showDeleteAllAlert = false) }

            is OnNodesSelected -> {
                addNodesInZone(action.zone, action.nodes)
                _state.update { it.copy(showAddDevicePopup = false) }
            }

            is OnDeleteNodeFromRow -> {
                removeNodeInZone(action.zone, action.node)
                _state.update { it.copy() }
            }

            is OnDeleteDeviceFromRow -> {
                removeDeviceInZone(action.zone, action.device)
                _state.update { it.copy() }
            }
        }
    }

    private fun handleDragEnd(finalOffset: Offset) {
        val bufferOffset = 10f
        val state = _state.value
        val draggingDevice = state.activeDraggingDevice ?: return

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
                    _state.update {
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
                addNodesInZone(zone, availableNodes)
                _state.update { it.copy() }
            } else {
                _state.update {
                    it.copy(
                        lastDropItem = zone to draggingDevice,
                        showAddDevicePopup = true,
                    )
                }
            }
        }
        _state.update { it.copy(activeDraggingDevice = null) }
    }

    private fun handleConnect() {
        val rows = _state.value.connectionRows
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
                _state.update {
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
            _state.update { it.copy(showIncompletePopup = true) }
        }
    }

    protected fun ensureOneEmptyRow() {
        val rows = _state.value.connectionRows
        if (rows.count { it.isEmpty() } != 1 || !rows.last().isEmpty()) {
            _state.update { state ->
                val rows = state.connectionRows
                val filteredRows = rows.filter { !it.isEmpty() }
                val newRows = filteredRows + (ConnectionScreenZoneItem(Instrument) to
                        ConnectionScreenZoneItem(Speaker))
                state.copy(connectionRows = newRows)
            }
        }
    }

    protected fun connect(connections: List<NodeConnection>) {
        resetError()
        deviceLinks(connections, LinkType.Connect)
    }

    protected fun disconnect(connections: List<NodeConnection>) {
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

    private fun addNodesInZone(zone: ConnectionScreenZoneItem, nodes: List<JiminyDeviceNode>) {
        zone.addNodes(nodes, deviceType)
        ensureOneEmptyRow()
    }

    private fun removeNodeInZone(zone: ConnectionScreenZoneItem, node: JiminyDeviceNode) {
        zone.removeNode(node)
        ensureOneEmptyRow()
    }

    private fun removeDeviceInZone(zone: ConnectionScreenZoneItem, device: JiminyDevice) {
        zone.devices.remove(device)
        ensureOneEmptyRow()
    }
}
