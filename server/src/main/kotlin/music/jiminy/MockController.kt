package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession

class MockController : JiminyServerControllerI {
    private var _isRecording = false
    override val isRecording: Boolean
        get() = _isRecording

    override suspend fun executeCommand(command: JiminyCommand): Boolean = when (command) {
        is JiminyCommand.Batch -> command.commands.all { executeCommand(it) }
        else -> true
    }

    override suspend fun getDevicesList() = JiminyDeviceList(
        instruments = dummyInstrumentsCmd,
        speakers = dummySpeakersCmd,
        deviceStatus = dummyStatusCmd,
    )

    override suspend fun linkDevice(link: JiminyCommand.Link) = true

    override suspend fun getDeviceLinksList() = dummyLinksCmd

    override suspend fun getRecordings() = listOf(
        "2024-05-16 - 12-00-00.wav",
        "2024-05-16 - 11-00-00.wav",
        "2026-05-19 - 23-37-55.wav",
        "2026-05-19 - 23-22-42.wav",
    )

    override suspend fun deleteRecordings(filenames: List<String>) = true

    override fun getRecordingFile(filename: String): java.io.File? = null

    override suspend fun startRecording(commands: JiminyCommand.StartRecording) = true
        .also { _isRecording = true }

    override suspend fun stopRecording() = true
        .also { _isRecording = false }

    private val mockConfigurations = mutableListOf(
        JiminyConfiguration("Standard Setup", emptyList(), emptyList()),
        JiminyConfiguration("Live Gig", emptyList(), emptyList()),
    )

    override suspend fun getConfigurations() = mockConfigurations.map { it.name }

    override suspend fun saveConfiguration(config: JiminyConfiguration): Boolean {
        mockConfigurations.removeAll { it.name == config.name }
        mockConfigurations.add(config)
        return true
    }

    override suspend fun getConfiguration(name: String) =
        mockConfigurations.find { it.name == name }

    override suspend fun deleteConfiguration(name: String) =
        mockConfigurations.removeAll { it.name == name }

    override suspend fun broadcastAll(
        sessions: List<DefaultWebSocketServerSession>,
        command: JiminyCommand,
        status: Boolean,
    ) {
        // no-op
    }
}

//alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FL
//alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FR
//alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL
//alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR
//alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FL
//alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FR

//pw-link alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FL                              alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL
//pw-link alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FR                              alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR
//pw-link alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FL                                   alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL
//pw-link alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FR                                   alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR
//pw-link alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FL    alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL
//pw-link alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FR    alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR

//pw-link -d alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FL                           alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL
//pw-link -d alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FR                           alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR
//pw-link -d alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FL alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL
//pw-link -d alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FR alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR

//    JiminyDeviceNode("alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FL", "usb-BOSS_GT-1000-01", "monitor_FL", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FR", "usb-BOSS_GT-1000-01", "monitor_FR", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FL", "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00", "capture_FL", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FR", "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00", "capture_FR", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL", "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00", "playback_FL", JiminyDeviceNodeType.Speakers),
//    JiminyDeviceNode("alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR", "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00", "playback_FR", JiminyDeviceNodeType.Speakers),

//    JiminyDeviceNode("alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FL", "usb-BOSS_GT-1000-01", "monitor_FL", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR", "usb-BOSS_GT-1000-01", "monitor_FR", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FL", "usb-BOSS_GT-1000-01", "capture_FL", JiminyDeviceNodeType.Speakers),
//    JiminyDeviceNode("alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FR", "usb-BOSS_GT-1000-01", "capture_FR", JiminyDeviceNodeType.Speakers),
//    JiminyDeviceNode("alsa_playback.fluidsynth:output_FL", "fluidsynth", "output_FL", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_playback.fluidsynth:output_FR", "fluidsynth", "output_FR", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL", "usb-Roland_TD-07-01", "playback_FL", JiminyDeviceNodeType.Speakers),
//    JiminyDeviceNode("alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR", "usb-Roland_TD-07-01", "playback_FR", JiminyDeviceNodeType.Speakers),
//    JiminyDeviceNode("alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FL", "usb-Roland_TD-07-01", "capture_FL", JiminyDeviceNodeType.Instruments),
//    JiminyDeviceNode("alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FR", "usb-Roland_TD-07-01", "capture_FR", JiminyDeviceNodeType.Instruments),

val dummyLinksCmd = listOf(
    "alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:playback_FL",
    "  |<- alsa_input.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:capture_FR",
    "alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:playback_FR",
    "  |<- alsa_input.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:capture_FR",
    "alsa_input.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:capture_FR",
    "  |-> alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:playback_FL",
    "  |-> alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:playback_FR",
)

val dummyInstrumentsCmd = listOf(
    "FluidSynth:output_1",
    "FluidSynth:output_2",
    "Midi-Bridge:Midi Through Port-0 (capture)",
    "Midi-Bridge:Jiminy_Metronome MIDI Click Out (capture)",
    "Midi-Bridge:BOSS_RC-500 MIDI 1 (capture)",
    "Midi-Bridge:Quad Cortex MIDI 1 (capture)",
    "Midi-Bridge:TD-07 MIDI 1 (capture)",
    "Midi-Bridge:GT-1000 MIDI 1 (capture)",
    "Midi-Bridge:GT-1000 MIDI 2 (capture)",
    "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FL",
    "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:monitor_FR",
    "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL",
    "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR",
    "alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FL",
    "alsa_input.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:capture_FR",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FL",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_RL",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_RR",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FC",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_LFE",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_FL",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_FR",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_RL",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_RR",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_FC",
    "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_LFE",
    "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FL",
    "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FR",
    "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_RL",
    "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_RR",
    "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FC",
    "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_LFE",
    "alsa_output.usb-Shure_Inc_Shure_MV88_-00.analog-stereo:monitor_FL",
    "alsa_output.usb-Shure_Inc_Shure_MV88_-00.analog-stereo:monitor_FR",
    "alsa_output.usb-Shure_Inc_Shure_MV88_-00.analog-stereo:playback_FL",
    "alsa_output.usb-Shure_Inc_Shure_MV88_-00.analog-stereo:playback_FR",
    "alsa_input.usb-Shure_Inc_Shure_MV88_-00.analog-stereo:capture_FL",
    "alsa_input.usb-Shure_Inc_Shure_MV88_-00.analog-stereo:capture_FR",
    "alsa_output.usb-Roland_TD-07-01.analog-stereo:monitor_FL",
    "alsa_output.usb-Roland_TD-07-01.analog-stereo:monitor_FR",
    "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL",
    "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR",
    "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FL",
    "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FR",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_FL",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_FR",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_RL",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_RR",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_FC",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_LFE",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_SL",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:monitor_SR",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_FL",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_FR",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_RL",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_RR",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_FC",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_LFE",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_SL",
    "alsa_output.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:playback_SR",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_FL",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_FR",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_RL",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_RR",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_FC",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_LFE",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_SL",
    "alsa_input.usb-Neural_DSP_Quad_Cortex-00.analog-surround-71:capture_SR",
)

val dummySpeakersCmd = listOf(
    "Midi-Bridge:Midi Through Port-0 (playback)",
    "Midi-Bridge:FLUID Synth (928)Synth input port (928:0) (playback)",
    "Midi-Bridge:Jiminy_Metronome MIDI Clock In (playback)",
    "Midi-Bridge:GT-1000 MIDI 1 (playback)",
    "Midi-Bridge:GT-1000 MIDI 2 (playback)",
    "Midi-Bridge:BOSS_RC-500 MIDI 1 (playback)",
    "Midi-Bridge:Quad Cortex MIDI 1 (playback)",
    "Midi-Bridge:TD-07 MIDI 1 (playback)",
    "alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:playback_FL",
    "alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo:playback_FR",
)

val dummyStatusCmd = listOf(
    "PipeWire 'pipewire-0' [1.4.2, cpu150@jiminy, cookie:115901764]",
    " └─ Clients:",
    "        35. WirePlumber                         [1.4.2, cpu150@jiminy, pid:923]",
    "        59. WirePlumber [export]                [1.4.2, cpu150@jiminy, pid:923]",
    "        60. pipewire                            [1.4.2, cpu150@jiminy, pid:925]",
    "        61. fluidsynth                          [1.4.2, cpu150@jiminy, pid:928]",
    "       107. wpctl                               [1.4.2, cpu150@jiminy, pid:2349]",
    "",
    "Audio",
    " ├─ Devices:",
    " │      63. alsa_card.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00 [alsa]",
    " │      64. alsa_card.platform-107c701400.hdmi  [alsa]",
    " │      65. alsa_card.platform-107c706400.hdmi  [alsa]",
    " │  ",
    " ├─ Sinks:",
    " │  *   91. alsa_output.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo [vol: 0.49]",
    " │  ",
    " ├─ Sources:",
    " │  *   92. alsa_input.usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00.analog-stereo [vol: 1.00]",
    " │  ",
    " ├─ Filters:",
    " │  ",
    " └─ Streams:",
    "        62. FluidSynth                                                  ",
    "             87. output_FL      ",
    "             88. output_FR      ",
    "",
    "Video",
    " ├─ Devices:",
    " │      70. v4l2_device.platform-1000800000.codec [v4l2]",
    " │      71. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      72. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      73. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      74. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      75. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      76. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      77. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      78. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      79. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      80. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      81. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      82. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      83. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      84. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      85. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │      86. v4l2_device.platform-1000880000.pisp_be [v4l2]",
    " │  ",
    " ├─ Sinks:",
    " │  ",
    " ├─ Sources:",
    " │  ",
    " ├─ Filters:",
    " │  ",
    " └─ Streams:",
    "",
    "Settings",
    " └─ Default Configured Devices:",
)

val dummyLinks = listOf(
    JiminyDeviceNode(
        "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FL",
        "usb-BOSS_GT-1000-01",
        "monitor_FL",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL",
        "usb-Roland_TD-07-01",
        "playback_FL",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR",
        "usb-BOSS_GT-1000-01",
        "monitor_FR",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL",
        "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00",
        "playback_FL",
        JiminyDeviceNodeType.Speaker
    ),
//    JiminyDeviceNode("alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR", "usb-BOSS_GT-1000-01", "monitor_FR", JiminyDeviceNodeType.Instrument)
//            to JiminyDeviceNode("alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR", "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00", "playback_FR", JiminyDeviceNodeType.Speaker)
//    ,
    JiminyDeviceNode(
        "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FL",
        "usb-Roland_TD-07-01",
        "capture_FL",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FL",
        "usb-BOSS_GT-1000-01",
        "capture_FL",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FL",
        "usb-BOSS_GT-1000-01",
        "monitor_FL",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL",
        "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00",
        "playback_FL",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR",
        "usb-BOSS_GT-1000-01",
        "monitor_FR",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR",
        "usb-Roland_TD-07-01",
        "playback_FR",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FL",
        "usb-Roland_TD-07-01",
        "capture_FL",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FL",
        "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00",
        "playback_FL",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FR",
        "usb-Roland_TD-07-01",
        "capture_FR",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FR",
        "usb-BOSS_GT-1000-01",
        "capture_FR",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FL",
        "usb-BOSS_GT-1000-01",
        "monitor_FL",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR",
        "usb-Roland_TD-07-01",
        "playback_FR",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FR",
        "usb-Roland_TD-07-01",
        "capture_FR",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00.analog-stereo:playback_FR",
        "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00",
        "playback_FR",
        JiminyDeviceNodeType.Speaker
    ),
    JiminyDeviceNode(
        "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR",
        "usb-BOSS_GT-1000-01",
        "monitor_FR",
        JiminyDeviceNodeType.Instrument
    )
            to JiminyDeviceNode(
        "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL",
        "usb-Roland_TD-07-01",
        "playback_FL",
        JiminyDeviceNodeType.Speaker
    ),
)

val dummyDevices = listOf(
    JiminyDevice("usb-BOSS_GT-1000-01", JiminyDeviceType.Audio).apply {
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FL",
                "usb-BOSS_GT-1000-01",
                "monitor_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FR",
                "usb-BOSS_GT-1000-01",
                "monitor_FR",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_RL",
                "usb-BOSS_GT-1000-01",
                "monitor_RL",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_RR",
                "usb-BOSS_GT-1000-01",
                "monitor_RR",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_FC",
                "usb-BOSS_GT-1000-01",
                "monitor_FC",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:monitor_LFE",
                "usb-BOSS_GT-1000-01",
                "monitor_LFE",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_FL",
                "usb-BOSS_GT-1000-01",
                "playback_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_FR",
                "usb-BOSS_GT-1000-01",
                "playback_FR",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_RL",
                "usb-BOSS_GT-1000-01",
                "playback_RL",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_RR",
                "usb-BOSS_GT-1000-01",
                "playback_RR",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_FC",
                "usb-BOSS_GT-1000-01",
                "playback_FC",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-BOSS_GT-1000-01.multichannel-output:playback_LFE",
                "usb-BOSS_GT-1000-01",
                "playback_LFE",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FL",
                "usb-BOSS_GT-1000-01",
                "capture_FL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FR",
                "usb-BOSS_GT-1000-01",
                "capture_FR",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_RL",
                "usb-BOSS_GT-1000-01",
                "capture_RL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_RR",
                "usb-BOSS_GT-1000-01",
                "capture_RR",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_FC",
                "usb-BOSS_GT-1000-01",
                "capture_FC",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-BOSS_GT-1000-01.multichannel-input:capture_LFE",
                "usb-BOSS_GT-1000-01",
                "capture_LFE",
                JiminyDeviceNodeType.Speaker
            )
        )
        addVolume(
            JiminyVolume(
                id = "38",
                volume = .6f,
                type = JiminyDeviceNodeType.Instrument,
                mute = false,
            )
        )
        addVolume(
            JiminyVolume(
                id = "40",
                volume = 1f,
                type = JiminyDeviceNodeType.Speaker,
                mute = false,
            )
        )
    },
    JiminyDevice("FluidSynth", JiminyDeviceType.Audio).apply {
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:input_FL",
                "fluidsynth",
                "input_FL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:output_FL",
                "FluidSynth",
                "output_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
    },
    JiminyDevice("usb-Roland_TD-07-01", JiminyDeviceType.Audio).apply {
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FL",
                "usb-Roland_TD-07-01",
                "playback_FL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_output.usb-Roland_TD-07-01.analog-stereo:playback_FR",
                "usb-Roland_TD-07-01",
                "playback_FR",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FL",
                "usb-Roland_TD-07-01",
                "capture_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_input.usb-Roland_TD-07-01.analog-stereo:capture_FR",
                "usb-Roland_TD-07-01",
                "capture_FR",
                JiminyDeviceNodeType.Instrument
            )
        )
        addVolume(
            JiminyVolume(
                id = "34",
                volume = .4f,
                type = JiminyDeviceNodeType.Instrument,
                mute = false,
            )
        )
        addVolume(
            JiminyVolume(
                id = "36",
                volume = .8f,
                type = JiminyDeviceNodeType.Speaker,
                mute = false,
            )
        )
    },
    JiminyDevice("FluidSynth2", JiminyDeviceType.Audio).apply {
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:input_FL",
                "fluidsynth",
                "input_FL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:output_FL",
                "FluidSynth",
                "output_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
    },
    JiminyDevice("FluidSynth3", JiminyDeviceType.Audio).apply {
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:input_FL",
                "fluidsynth",
                "input_FL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:output_FL",
                "FluidSynth",
                "output_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
    },
    JiminyDevice("FluidSynth4", JiminyDeviceType.Audio).apply {
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:input_FL",
                "fluidsynth",
                "input_FL",
                JiminyDeviceNodeType.Speaker
            )
        )
        addNode(
            JiminyDeviceNode(
                "alsa_playback.FluidSynth:output_FL",
                "FluidSynth",
                "output_FL",
                JiminyDeviceNodeType.Instrument
            )
        )
    },
)
