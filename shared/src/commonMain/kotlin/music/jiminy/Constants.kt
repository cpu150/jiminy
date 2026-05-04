package music.jiminy

//val platform = getPlatform()

const val DEBUG = false

const val DEBOUNCING_COMMAND_MILLIS = 100

const val SERVER_PORT = 80
const val WS_ROOT = "/"
const val WS_DEFAULT_PATH = "index.html"
const val WS_MIXER = "/mixer-ws"
const val WS_DEVICES = "/devices"
const val WS_LINK_DEVICES = "/link-devices"
const val WS_START_RECORDING = "/start-recording"
const val WS_STOP_RECORDING = "/stop-recording"

const val DEVICE_CARD_SPEAKERS_LABEL = "SPK"
const val DEVICE_CARD_INSTRUMENTS_LABEL = "INST"
const val DEVICE_CARD_HEIGHT = 100
const val DEVICE_CARD_WIDTH = 100
const val DEVICE_LIST_CARD_HEIGHT = 80
const val DEVICE_LIST_CARD_WIDTH = 80
const val DEVICE_CARD_SPEAKERS_COLOR = 0xFF2196F3
const val DEVICE_CARD_INSTRUMENTS_COLOR = 0xFF4CAF50

const val MIXER_SLIDER_HEIGHT = 300
const val MIXER_SLIDER_WIDTH = 26

const val PW_RECORDER_NAME = "Jiminy-MultiSink"
const val PW_RECORDER_CHANNEL_PREFIX = "AUX"
const val PW_RECORDER_CHANNEL_COUNT = "8"
const val PW_RECORDER_DIRECTORY = "/home/cpu150/"

const val GT_1000 = "usb-BOSS_GT-1000-01"
const val QUAD_CORTEX = "usb-Neural_DSP_Quad_Cortex-00"
const val ROLAND_TD_07 = "usb-Roland_TD-07-01"
const val FLUIDSYNTH = "FluidSynth"
const val SND_CARD_U_GREEN = "usb-Realtek_UGREEN_CM720_USB_Audio_202312130006-00"
const val SND_CARD_RED = "usb-GHW-136D-20231007_USB_Audio_20210726905926-00"
const val SND_CARD_24 = "usb-Synaptics_Hi-Res_Audio_000000000000000000000000-00"
const val MIC_SHURE_MV88 = "usb-Shure_Inc_Shure_MV88_-00"

const val INPUT_CAPTURE_1 = "capture_FL"
const val INPUT_CAPTURE_2 = "capture_FR"
const val INPUT_CAPTURE_3 = "capture_RL"
const val INPUT_CAPTURE_4 = "capture_RR"
const val INPUT_CAPTURE_5 = "capture_FC"
const val INPUT_CAPTURE_6 = "capture_LFE"
const val INPUT_CAPTURE_7 = "capture_SL"
const val INPUT_CAPTURE_8 = "capture_SR"
const val OUTPUT_PLAYBACK_1 = "playback_FL"
const val OUTPUT_PLAYBACK_2 = "playback_FR"
const val OUTPUT_PLAYBACK_3 = "playback_RL"
const val OUTPUT_PLAYBACK_4 = "playback_RR"
const val OUTPUT_PLAYBACK_5 = "playback_FC"
const val OUTPUT_PLAYBACK_6 = "playback_LFE"
const val OUTPUT_PLAYBACK_7 = "playback_SL"
const val OUTPUT_PLAYBACK_8 = "playback_SR"
const val OUTPUT_MONITOR_1 = "monitor_FL"
const val OUTPUT_MONITOR_2 = "monitor_FR"
const val OUTPUT_MONITOR_3 = "monitor_RL"
const val OUTPUT_MONITOR_4 = "monitor_RR"
const val OUTPUT_MONITOR_5 = "monitor_FC"
const val OUTPUT_MONITOR_6 = "monitor_LFE"
const val OUTPUT_MONITOR_7 = "monitor_SL"
const val OUTPUT_MONITOR_8 = "monitor_SR"
const val OUTPUT_1 = "output_FL"
const val OUTPUT_2 = "output_FR"
const val OUTPUT_3 = "output_RL"
const val OUTPUT_4 = "output_RR"
const val OUTPUT_5 = "output_FC"
const val OUTPUT_6 = "output_LFE"
const val OUTPUT_7 = "output_SL"
const val OUTPUT_8 = "output_SR"

const val RECORDER_PLAYBACK_ROOT = "playback_$PW_RECORDER_CHANNEL_PREFIX"
const val RECORDER_MONITOR_ROOT = "monitor_$PW_RECORDER_CHANNEL_PREFIX"
const val RECORDER_PLAYBACK_1 = "${RECORDER_PLAYBACK_ROOT}1"
const val RECORDER_PLAYBACK_2 = "${RECORDER_PLAYBACK_ROOT}2"
const val RECORDER_PLAYBACK_3 = "${RECORDER_PLAYBACK_ROOT}3"
const val RECORDER_PLAYBACK_4 = "${RECORDER_PLAYBACK_ROOT}4"
const val RECORDER_PLAYBACK_5 = "${RECORDER_PLAYBACK_ROOT}5"
const val RECORDER_PLAYBACK_6 = "${RECORDER_PLAYBACK_ROOT}6"
const val RECORDER_PLAYBACK_7 = "${RECORDER_PLAYBACK_ROOT}7"
const val RECORDER_PLAYBACK_8 = "${RECORDER_PLAYBACK_ROOT}8"
const val RECORDER_MONITOR_1 = "${RECORDER_MONITOR_ROOT}1"
const val RECORDER_MONITOR_2 = "${RECORDER_MONITOR_ROOT}2"
const val RECORDER_MONITOR_3 = "${RECORDER_MONITOR_ROOT}3"
const val RECORDER_MONITOR_4 = "${RECORDER_MONITOR_ROOT}4"
const val RECORDER_MONITOR_5 = "${RECORDER_MONITOR_ROOT}5"
const val RECORDER_MONITOR_6 = "${RECORDER_MONITOR_ROOT}6"
const val RECORDER_MONITOR_7 = "${RECORDER_MONITOR_ROOT}7"
const val RECORDER_MONITOR_8 = "${RECORDER_MONITOR_ROOT}8"

enum class AvatarIconsEnum {
    GT_1000,
    QUAD_CORTEX,
    ROLAND_TD_07,
    FLUIDSYNTH,
    SND_CARD_U_GREEN,
    SND_CARD_RED,
    SND_CARD_24,
    MIC_SHURE_MV88,
    Unknown,
}

val deviceNameToAvatar = mapOf(
    GT_1000 to AvatarIconsEnum.GT_1000,
    QUAD_CORTEX to AvatarIconsEnum.QUAD_CORTEX,
    ROLAND_TD_07 to AvatarIconsEnum.ROLAND_TD_07,
    FLUIDSYNTH to AvatarIconsEnum.FLUIDSYNTH,
    SND_CARD_U_GREEN to AvatarIconsEnum.SND_CARD_U_GREEN,
    SND_CARD_RED to AvatarIconsEnum.SND_CARD_RED,
    SND_CARD_24 to AvatarIconsEnum.SND_CARD_24,
    MIC_SHURE_MV88 to AvatarIconsEnum.MIC_SHURE_MV88,
)

val deviceNameToAlias = mapOf(
    FLUIDSYNTH to "Synth",
    "$FLUIDSYNTH:$INPUT_CAPTURE_1" to "Left",
    "$FLUIDSYNTH:$INPUT_CAPTURE_2" to "Right",
    "$FLUIDSYNTH:$OUTPUT_1" to "Left",
    "$FLUIDSYNTH:$OUTPUT_2" to "Right",
    "$FLUIDSYNTH:$OUTPUT_3" to "3",
    "$FLUIDSYNTH:$OUTPUT_4" to "4",
    "$FLUIDSYNTH:$OUTPUT_5" to "5",
    "$FLUIDSYNTH:$OUTPUT_6" to "6",
    "$FLUIDSYNTH:$OUTPUT_7" to "7",
    "$FLUIDSYNTH:$OUTPUT_8" to "8",
    "$FLUIDSYNTH:$OUTPUT_PLAYBACK_1" to "Playback Left",
    "$FLUIDSYNTH:$OUTPUT_PLAYBACK_2" to "Playback Right",
    "$FLUIDSYNTH:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$FLUIDSYNTH:$OUTPUT_MONITOR_2" to "Monitor Right",

    QUAD_CORTEX to "Umberto",
    "$QUAD_CORTEX:$INPUT_CAPTURE_1" to "Left",
    "$QUAD_CORTEX:$INPUT_CAPTURE_2" to "Right / Mono",
    "$QUAD_CORTEX:$INPUT_CAPTURE_3" to "3",
    "$QUAD_CORTEX:$INPUT_CAPTURE_4" to "4",
    "$QUAD_CORTEX:$INPUT_CAPTURE_5" to "5",
    "$QUAD_CORTEX:$INPUT_CAPTURE_6" to "6",
    "$QUAD_CORTEX:$INPUT_CAPTURE_7" to "7",
    "$QUAD_CORTEX:$INPUT_CAPTURE_8" to "8",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_1" to "Left",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_2" to "Right / Mono",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_3" to "3",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_4" to "4",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_5" to "5",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_6" to "6",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_7" to "7",
    "$QUAD_CORTEX:$OUTPUT_PLAYBACK_8" to "8",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_2" to "Monitor Right / Mono",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_3" to "Monitor 3",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_4" to "Monitor 4",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_5" to "Monitor 5",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_6" to "Monitor 6",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_7" to "Monitor 7",
    "$QUAD_CORTEX:$OUTPUT_MONITOR_8" to "Monitor 8",

    ROLAND_TD_07 to "Iris",
    "$ROLAND_TD_07:$INPUT_CAPTURE_1" to "Left",
    "$ROLAND_TD_07:$INPUT_CAPTURE_2" to "Right",
    "$ROLAND_TD_07:$OUTPUT_PLAYBACK_1" to "Left",
    "$ROLAND_TD_07:$OUTPUT_PLAYBACK_2" to "Right",
    "$ROLAND_TD_07:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$ROLAND_TD_07:$OUTPUT_MONITOR_2" to "Monitor Right",

    GT_1000 to "Nic",
    "$GT_1000:$INPUT_CAPTURE_1" to "Left",
    "$GT_1000:$INPUT_CAPTURE_2" to "Right / Mono",
    "$GT_1000:$INPUT_CAPTURE_3" to "3",
    "$GT_1000:$INPUT_CAPTURE_4" to "4",
    "$GT_1000:$INPUT_CAPTURE_5" to "5",
    "$GT_1000:$INPUT_CAPTURE_6" to "6",
    "$GT_1000:$OUTPUT_PLAYBACK_1" to "Left",
    "$GT_1000:$OUTPUT_PLAYBACK_2" to "Right / Mono",
    "$GT_1000:$OUTPUT_PLAYBACK_3" to "3",
    "$GT_1000:$OUTPUT_PLAYBACK_4" to "4",
    "$GT_1000:$OUTPUT_PLAYBACK_5" to "5",
    "$GT_1000:$OUTPUT_PLAYBACK_6" to "6",
    "$GT_1000:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$GT_1000:$OUTPUT_MONITOR_2" to "Monitor Right / Mono",
    "$GT_1000:$OUTPUT_MONITOR_3" to "Monitor 3",
    "$GT_1000:$OUTPUT_MONITOR_4" to "Monitor 4",
    "$GT_1000:$OUTPUT_MONITOR_5" to "Monitor 5",
    "$GT_1000:$OUTPUT_MONITOR_6" to "Monitor 6",

    SND_CARD_U_GREEN to "Pi",
    "$SND_CARD_U_GREEN:$INPUT_CAPTURE_1" to "Left",
    "$SND_CARD_U_GREEN:$INPUT_CAPTURE_2" to "Right",
    "$SND_CARD_U_GREEN:$OUTPUT_PLAYBACK_1" to "Left / Mono",
    "$SND_CARD_U_GREEN:$OUTPUT_PLAYBACK_2" to "Right",
    "$SND_CARD_U_GREEN:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$SND_CARD_U_GREEN:$OUTPUT_MONITOR_2" to "Monitor Right",

    SND_CARD_RED to "RED",
    "$SND_CARD_RED:$INPUT_CAPTURE_1" to "Left",
    "$SND_CARD_RED:$INPUT_CAPTURE_2" to "Right",
    "$SND_CARD_RED:$OUTPUT_PLAYBACK_1" to "Left",
    "$SND_CARD_RED:$OUTPUT_PLAYBACK_2" to "Right",
    "$SND_CARD_RED:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$SND_CARD_RED:$OUTPUT_MONITOR_2" to "Monitor Right",

    SND_CARD_24 to "Snd 24",
    "$SND_CARD_24:$INPUT_CAPTURE_1" to "Left",
    "$SND_CARD_24:$INPUT_CAPTURE_2" to "Right",
    "$SND_CARD_24:$OUTPUT_PLAYBACK_1" to "Left",
    "$SND_CARD_24:$OUTPUT_PLAYBACK_2" to "Right",
    "$SND_CARD_24:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$SND_CARD_24:$OUTPUT_MONITOR_2" to "Monitor Right",

    MIC_SHURE_MV88 to "Shure",
    "$MIC_SHURE_MV88:$INPUT_CAPTURE_1" to "Left",
    "$MIC_SHURE_MV88:$INPUT_CAPTURE_2" to "Right",
    "$MIC_SHURE_MV88:$OUTPUT_PLAYBACK_1" to "Left",
    "$MIC_SHURE_MV88:$OUTPUT_PLAYBACK_2" to "Right",
    "$MIC_SHURE_MV88:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$MIC_SHURE_MV88:$OUTPUT_MONITOR_2" to "Monitor Right",

    PW_RECORDER_NAME to "Recorder",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_1" to "Mon Trk 1",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_2" to "Mon Trk 2",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_3" to "Mon Trk 3",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_4" to "Mon Trk 4",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_5" to "Mon Trk 5",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_6" to "Mon Trk 6",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_7" to "Mon Trk 7",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_8" to "Mon Trk 8",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_1" to "Rec Mon 1",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_2" to "Rec Mon 2",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_3" to "Rec Mon 3",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_4" to "Rec Mon 4",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_5" to "Rec Mon 5",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_6" to "Rec Mon 6",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_7" to "Rec Mon 7",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_8" to "Rec Mon 8",
)

// -------------------------------------------------------------------------------------------------
///// VVV     JUNK     VVV


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
    "TODO"
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
    JiminyDevice("usb-BOSS_GT-1000-01").apply {
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
    JiminyDevice("FluidSynth").apply {
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
    JiminyDevice("usb-Roland_TD-07-01").apply {
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
    JiminyDevice("FluidSynth2").apply {
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
    JiminyDevice("FluidSynth3").apply {
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
    JiminyDevice("FluidSynth4").apply {
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
