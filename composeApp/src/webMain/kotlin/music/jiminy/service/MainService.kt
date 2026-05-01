package music.jiminy.service

import music.jiminy.JiminyCommand

class MainService(
    private val mixerService: MixerService,
    private val deviceService: DeviceService,
    private val recordingService: RecordingService,
) {
    val succeededCommands = mixerService.succeededCommands
    suspend fun mixerSendCommand(command: JiminyCommand) = mixerService.sendCommand(command)
    suspend fun mixerDisconnect() = mixerService.disconnect()
    suspend fun mixerConnect(connected: (() -> Unit)? = null) = mixerService.connect(connected)

    suspend fun getDevices() = deviceService.getDevices()
    suspend fun getDeviceLinks() = deviceService.getDeviceLinks()
    suspend fun deviceLinks(links: List<JiminyCommand.Link>) = deviceService.linkDevices(links)

    suspend fun startRecording(nodes: JiminyCommand.StartRecording) = recordingService.startRecording(nodes)
}
