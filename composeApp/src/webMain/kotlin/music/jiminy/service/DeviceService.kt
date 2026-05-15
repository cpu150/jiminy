package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import music.jiminy.FLUIDSYNTH
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceList
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType.Instrument
import music.jiminy.JiminyDeviceNodeType.Speaker
import music.jiminy.JiminyDeviceNodeType.Unknown
import music.jiminy.JiminyLoggerI
import music.jiminy.JiminyVolume
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.WS_DEVICES
import music.jiminy.WS_LINK_DEVICES

class DeviceService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val logger: JiminyLoggerI,
) {
    private val _devices = mutableListOf<JiminyDevice>()

    suspend fun getDevices() = client
        .get("$baseUrl$WS_DEVICES")
        .body<JiminyDeviceList>()
        .let { processDevicesOutput(it) }

    suspend fun getDeviceLinks() = client
        .get("$baseUrl$WS_LINK_DEVICES")
        .body<List<String>>()
        .let { processDeviceLinksOutput(it) }

    suspend fun linkDevices(links: List<JiminyCommand.Link>) = client
        .post("$baseUrl$WS_LINK_DEVICES") {
            contentType(ContentType.Application.Json)
            setBody(links)
        }

    private fun processDevicesOutput(output: JiminyDeviceList): List<JiminyDevice> {
        _devices.clear()

        for (list in listOf(output.instruments, output.speakers)) {
            list.forEach { fullName ->
                parseOutputCmd(fullName)?.let { data ->
                    with(data) {
                        val dev = _devices.find { it.name == deviceName }
                            ?: JiminyDevice(deviceName).also { _devices.add(it) }

                        val type = if (list == output.instruments) Instrument else Speaker

                        dev.addNode(JiminyDeviceNode(fullName, deviceName, portName, type))
                    }
                }
            }
        }

        var type = Unknown
        output.deviceStatus.forEach { statusLine ->
            if (statusLine.contains("─ Sinks:")) {
                type = Speaker
            } else if (statusLine.contains("─ Sources:")) {
                type = Instrument
            } else if (statusLine.contains("[vol: ")) {
                _devices.find { device -> statusLine.contains(device.name) }?.also { device ->
                    // │ |  *   78. alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo [vol: 0.40]
                    // │ │      75. alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo [vol: 0.75 MUTED]
                    val arr = statusLine.split("\\.${device.name}\\..* \\[vol: ".toRegex())

                    // │  *   78. alsa_output
                    val str = arr.getOrNull(0)

                    val id = "[0-9].*".toRegex()
                        .find(str.orEmpty())?.value             // 78. alsa_output
                        ?.split(".")?.getOrNull(0) // 78
                        .orEmpty()

                    // .analog-stereo [vol: 0.40]
                    // .analog-stereo [vol: 0.75 MUTED]
                    val strEnd = arr.getOrNull(1)
                    val muted = strEnd?.contains(" MUTED]") ?: false
                    val volume = strEnd
                        ?.removeSuffix("]")
                        ?.removeSuffix(" MUTED")
                        ?.toFloatOrNull() ?: -1.0f

                    device.addVolume(JiminyVolume(id, volume, type, muted))
                }
            }
        }

        return _devices
    }

    private fun processDeviceLinksOutput(output: List<String>) = buildList {
        var nodeInstrument: JiminyDeviceNode? = null
        output.forEach { fullName ->
            if ((fullName.startsWith("alsa_output") && !fullName.contains(":monitor_")) ||
                fullName.startsWith("  |<-")
            ) {
                // Ignore
                nodeInstrument = null
            } else if (fullName.startsWith("alsa_input") ||
                fullName.startsWith("alsa_playback") ||
                fullName.startsWith(FLUIDSYNTH, true) ||
                fullName.startsWith(PW_RECORDER_NAME, true) ||
                fullName.contains(":monitor_")
            ) {
                parseOutputCmd(fullName)?.let { data ->
                    nodeInstrument = JiminyDeviceNode(
                        data.fullName,
                        data.deviceName,
                        data.portName,
                        Instrument,
                    )
                } ?: add(errorNodeDebug("ERROR parsing: $fullName"))
            } else if (fullName.startsWith("  |-> ")) {
                nodeInstrument?.let {
                    parseOutputCmd(fullName.removePrefix("  |-> "))?.let { data ->
                        val nodeSpeaker = JiminyDeviceNode(
                            data.fullName,
                            data.deviceName,
                            data.portName,
                            Speaker,
                        )

                        add(it to nodeSpeaker)
                    } ?: add(errorNodeDebug("ERROR parsing: $fullName"))
                } ?: add(errorNodeDebug("ERROR - NO DEV FOR: $fullName"))
            } else {
                add(errorNodeDebug("ERROR - $fullName"))
            }
        }
    }

    private fun errorNodeDebug(errorMsg: String) = JiminyDeviceNode(
        errorMsg,
        "ERROR",
        errorMsg,
        Unknown,
    ) to JiminyDeviceNode(
        errorMsg,
        "ERROR $errorMsg",
        errorMsg,
        Unknown,
    )

    private data class OutputParsedData(
        val fullName: String,
        val deviceName: String,
        val portName: String,
    )

    // TODO - Process midi devices:
    // Midi-Bridge:Midi Through Port-0 (capture)
    // Midi-Bridge:Midi Through Port-0 (playback)
    // Midi-Bridge:FLUID Synth (935)Synth input port (935:0) (playback)
    private fun parseOutputCmd(
        fullName: String,
    ) = if (fullName.startsWith(FLUIDSYNTH, true) ||
        fullName.startsWith(PW_RECORDER_NAME, true)
    ) {
        // FluidSynth:output_FL
        // Jiminy-MultiSink:playback_AUX0 || Jiminy-MultiSink:monitor_AUX0
        val arr = fullName.split(":")
        val deviceName = arr.getOrNull(0)
        val portName = arr.getOrNull(1)
        if (deviceName != null && portName != null) {
            OutputParsedData(fullName, deviceName, portName)
        } else {
            null
        }
    } else {
        // alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FL
        var arr = fullName.split(":")
        val portName = arr.getOrNull(1).orEmpty()

        arr = arr.getOrNull(0).orEmpty().split(".")
        val deviceName = arr.getOrNull(1).orEmpty()

        if (deviceName.isNotBlank()) {
            OutputParsedData(fullName, deviceName, portName)
        } else {
            null
        }
    }
}
