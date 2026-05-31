package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import music.jiminy.FLUIDSYNTH
import music.jiminy.JiminyAudioDevice
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDeviceList
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType.Instrument
import music.jiminy.JiminyDeviceNodeType.Speaker
import music.jiminy.JiminyDeviceNodeType.Unknown
import music.jiminy.JiminyDevices
import music.jiminy.JiminyLoggerI
import music.jiminy.JiminyMidiDevice
import music.jiminy.JiminyMidiDeviceNode
import music.jiminy.JiminyVolume
import music.jiminy.MIDI_BRIDGE_PREFIX
import music.jiminy.NodeConnection
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.WS_DEVICES
import music.jiminy.WS_LINK_DEVICES

class DeviceService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val logger: JiminyLoggerI,
) {
    private val _devices = mutableListOf<JiminyAudioDevice>()
    private val _midiDevices = mutableListOf<JiminyMidiDevice>()

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

    private fun processDevicesOutput(output: JiminyDeviceList): JiminyDevices {
        _devices.clear()
        _midiDevices.clear()

        val (midiInstruments, audioInstruments) = output.instruments.partition {
            it.startsWith(MIDI_BRIDGE_PREFIX)
        }
        val (midiSpeakers, audioSpeakers) = output.speakers.partition {
            it.startsWith(MIDI_BRIDGE_PREFIX)
        }

        // Audio Devices
        for (list in listOf(audioInstruments, audioSpeakers)) {
            list.forEach { fullName ->
                parseOutputCmd(fullName)?.let { data ->
                    with(data) {
                        val dev = _devices.find { it.name == deviceName }
                            ?: JiminyAudioDevice(deviceName).also { _devices.add(it) }

                        val type = if (list == audioInstruments) Instrument else Speaker

                        dev.addNode(JiminyDeviceNode(fullName, deviceName, portName, type))
                    }
                }
            }
        }

        // MIDI Devices
        for (list in listOf(midiInstruments, midiSpeakers)) {
            list.forEach { fullName ->
                parseMidiOutputCmd(fullName)?.let { data ->
                    with(data) {
                        val dev = _midiDevices.find { it.name == deviceName }
                            ?: JiminyMidiDevice(deviceName).also { _midiDevices.add(it) }

                        val type = if (list == midiInstruments) Instrument else Speaker

                        dev.addNode(JiminyMidiDeviceNode(fullName, deviceName, portName, type))
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

        return JiminyDevices(_devices.toList(), _midiDevices.toList())
    }

    private fun processDeviceLinksOutput(output: List<String>): List<NodeConnection> = buildList {
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
                fullName.contains(":monitor_") ||
                fullName.startsWith(MIDI_BRIDGE_PREFIX)
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

                        add(NodeConnection(it, nodeSpeaker))
                    } ?: add(errorNodeDebug("ERROR parsing: $fullName"))
                } ?: add(errorNodeDebug("ERROR - NO DEV FOR: $fullName"))
            } else {
                add(errorNodeDebug("ERROR - $fullName"))
            }
        }
    }

    private fun errorNodeDebug(errorMsg: String) = NodeConnection(
        JiminyDeviceNode(
            errorMsg,
            "ERROR",
            errorMsg,
            Unknown,
        ),
        JiminyDeviceNode(
            errorMsg,
            "ERROR $errorMsg",
            errorMsg,
            Unknown,
        ),
    )

    private data class OutputParsedData(
        val fullName: String,
        val deviceName: String,
        val portName: String,
    )

    private fun parseMidiOutputCmd(
        fullName: String,
    ): OutputParsedData? {
        // Midi-Bridge:GT-1000 MIDI 1 (capture)
        // Midi-Bridge:FLUID Synth (935)Synth input port (935:0) (playback)
        val arr = fullName.split(":")
        val prefix = arr.getOrNull(0)?.let { "$it:" }
        val fullPortName = arr.getOrNull(1)

        return if (prefix == MIDI_BRIDGE_PREFIX && fullPortName != null) {
            // We want the device name to be part of the port name before (capture)/(playback)
            // But actually, grouped by device would be better.
            // Example: "GT-1000 MIDI 1"
            val deviceName = fullPortName.trim()

            OutputParsedData(fullName, deviceName, fullPortName)
        } else {
            null
        }
    }

    private fun parseOutputCmd(
        fullName: String,
    ) = if (fullName.startsWith(FLUIDSYNTH, true) ||
        fullName.startsWith(PW_RECORDER_NAME, true) ||
        fullName.startsWith(MIDI_BRIDGE_PREFIX)
    ) {
        // FluidSynth:output_FL
        // Midi-Bridge:port_name
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
