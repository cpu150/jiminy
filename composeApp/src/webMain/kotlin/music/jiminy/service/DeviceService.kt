package music.jiminy.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import music.jiminy.FLUIDSYNTH_AUDIO_NAME
import music.jiminy.FLUIDSYNTH_MIDI_NAME
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceType
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDeviceList
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType.Instrument
import music.jiminy.JiminyDeviceNodeType.Speaker
import music.jiminy.JiminyDeviceNodeType.Unknown
import music.jiminy.JiminyLoggerI
import music.jiminy.JiminyVolume
import music.jiminy.MIDI_BRIDGE_PREFIX
import music.jiminy.MIDI_THROUGH
import music.jiminy.NodeConnection
import music.jiminy.PW_RECORDER_NAME
import music.jiminy.WS_DEVICES
import music.jiminy.WS_LINK_DEVICES

class DeviceService(
    private val client: HttpClient,
    private val baseUrl: String,
    private val logger: JiminyLoggerI,
) {
    private val _audioDevices = MutableStateFlow<List<JiminyDevice>>(emptyList())
    val audioDevices: StateFlow<List<JiminyDevice>> = _audioDevices.asStateFlow()

    private val _midiDevices = MutableStateFlow<List<JiminyDevice>>(emptyList())
    val midiDevices: StateFlow<List<JiminyDevice>> = _midiDevices.asStateFlow()

    suspend fun refreshDevices() = client
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

    private fun processDevicesOutput(output: JiminyDeviceList) {
        val audioDevices = mutableListOf<JiminyDevice>()
        val midiDevices = mutableListOf<JiminyDevice>()

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
                        val dev = audioDevices.find { it.name == deviceName }
                            ?: JiminyDevice(deviceName, JiminyDeviceType.Audio).also { audioDevices.add(it) }

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
                        val dev = midiDevices.find { it.name == deviceName }
                            ?: JiminyDevice(deviceName, JiminyDeviceType.Midi).also { midiDevices.add(it) }

                        val type = if (list == midiInstruments) Instrument else Speaker

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
                audioDevices.find { device -> statusLine.contains(device.name) }?.also { device ->
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

        _audioDevices.update { audioDevices }
        _midiDevices.update { midiDevices }
        logger.info("processDevicesOutput - audio: ${audioDevices.size}, midi: ${midiDevices.size}")
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
                fullName.startsWith(FLUIDSYNTH_AUDIO_NAME, true) ||
                fullName.startsWith(PW_RECORDER_NAME, true) ||
                fullName.contains(":monitor_") ||
                fullName.startsWith(MIDI_BRIDGE_PREFIX)
            ) {
                nodeInstrument = createNode(fullName, Instrument)
                if (nodeInstrument == null) {
                    logger.error("ERROR - processDeviceLinksOutput - parsing device: $fullName")
                }
            } else if (fullName.startsWith("  |-> ")) {
                nodeInstrument?.let {
                    val speakerName = fullName.removePrefix("  |-> ")
                    createNode(speakerName, Speaker)?.let { nodeSpeaker ->
                        add(NodeConnection(it, nodeSpeaker))
                    } ?: logger.error("ERROR - processDeviceLinksOutput - parsing: $fullName")
                } ?: logger.error("ERROR - processDeviceLinksOutput - NO DEV FOR: $fullName")
            } else {
                logger.error("ERROR - processDeviceLinksOutput - $fullName")
            }
        }
    }

    private fun createNode(fullName: String, type: music.jiminy.JiminyDeviceNodeType): JiminyDeviceNode? {
        return if (fullName.startsWith(MIDI_BRIDGE_PREFIX)) {
            parseMidiOutputCmd(fullName)?.let { data ->
                JiminyDeviceNode(data.fullName, data.deviceName, data.portName, type)
            }
        } else {
            parseOutputCmd(fullName)?.let { data ->
                JiminyDeviceNode(data.fullName, data.deviceName, data.portName, type)
            }
        }
    }

    private data class OutputParsedData(
        val fullName: String,
        val deviceName: String,
        val portName: String,
    )

    private fun parseMidiOutputCmd(
        fullName: String,
    ): OutputParsedData? = if (fullName.startsWith(MIDI_BRIDGE_PREFIX)) {
        val nameWithoutType = fullName
            .removePrefix(MIDI_BRIDGE_PREFIX)
            .removeSuffix("(capture)")
            .removeSuffix("(playback)")
            .trim()

        var deviceName: String?
        var portName: String?
        when {
            nameWithoutType.startsWith(MIDI_THROUGH, ignoreCase = true) -> {
                deviceName = MIDI_THROUGH
                portName = nameWithoutType.substring(MIDI_THROUGH.length).trim()
            }

            nameWithoutType.startsWith(FLUIDSYNTH_MIDI_NAME, ignoreCase = true) -> {
                deviceName = FLUIDSYNTH_MIDI_NAME
                portName = Regex("""port \(([^)]+)\)""", RegexOption.IGNORE_CASE)
                    .findAll(nameWithoutType)
                    .lastOrNull()
                    ?.groupValues
                    ?.getOrNull(1)
            }

            else -> {
                val parts = nameWithoutType.split("MIDI", ignoreCase = true)
                deviceName = parts.getOrNull(0)?.trim()
                portName = parts.getOrNull(1)?.trim()
            }
        }

        if (portName != null && deviceName != null) {
            OutputParsedData(
                fullName = fullName,
                deviceName = deviceName,
                portName = portName,
            )
        } else {
            logger.error("parseMidiOutputCmd - Impossible to parse \"$fullName\"")
            null
        }
    } else {
        null
    }

    private fun parseOutputCmd(
        fullName: String,
    ) = if (fullName.startsWith(FLUIDSYNTH_AUDIO_NAME, true) ||
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
