package music.jiminy.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.JiminyLoggerI
import music.jiminy.MIDI_BRIDGE_PREFIX
import music.jiminy.NodeConnection
import music.jiminy.service.MainService

class MIDIScreenViewModel(
    mainService: MainService,
    logger: JiminyLoggerI,
) : BaseConnectionScreenViewModel(mainService, logger) {

    override val deviceType = JiminyDeviceType.Midi

    override fun startCollectingDevices() {
        viewModelScope.launch {
            mainService.midiDevices.collect { devices ->
                _state.update { state ->
                    state.copy(devices = devices)
                }
            }
        }
    }

    override fun filterLinks(links: List<NodeConnection>): List<NodeConnection> =
        links.filter { it.instrument.fullName.startsWith(MIDI_BRIDGE_PREFIX) }

    override fun convertLinks(links: List<NodeConnection>): List<JiminyLink> =
        links.toJiminyMidiLinks()
}
