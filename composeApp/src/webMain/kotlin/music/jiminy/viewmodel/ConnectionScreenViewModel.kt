package music.jiminy.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyLink
import music.jiminy.JiminyLoggerI
import music.jiminy.NodeConnection
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.service.MainService

class ConnectionScreenViewModel(
    mainService: MainService,
    logger: JiminyLoggerI,
) : BaseConnectionScreenViewModel(mainService, logger) {

    override val deviceType = JiminyDeviceType.Audio

    override fun startCollectingDevices() {
        viewModelScope.launch {
            mainService.audioDevices.collect { devices ->
                _state.update { state ->
                    state.copy(devices = devices.filter { it.name != PW_RECORDER_NAME })
                }
            }
        }
    }

    override fun filterLinks(links: List<NodeConnection>): List<NodeConnection> =
        links.filter { it.speaker.deviceName != PW_RECORDER_NAME }

    override fun convertLinks(links: List<NodeConnection>): List<JiminyLink> =
        links.toJiminyLinks()
}
