package music.jiminy

val DEBUG = getPlatform().debug

const val DEBOUNCING_COMMAND_MILLIS = 120

const val DEBUG_SERVER_PORT = 8081
const val SERVER_PORT = 80
const val DEBUG_SERVER_HOST = "0.0.0.0"
const val SERVER_HOST = "0.0.0.0"
const val WS_ROOT = "/"
const val WS_DEFAULT_PATH = "index.html"
const val WS_MIXER = "/mixer-ws"
const val WS_DEVICES = "/devices"
const val WS_LINK_DEVICES = "/link-devices"
const val WS_START_RECORDING = "/start-recording"
const val WS_STOP_RECORDING = "/stop-recording"
const val WS_RECORDINGS = "/recordings"
const val WS_DELETE_RECORDINGS = "/delete-recordings"
const val WS_DOWNLOAD_RECORDINGS = "/download-recordings"
const val WS_SERVER_LOGS = "/server-logs"
const val WS_FLUSH_SERVER_LOGS = "/flush-server-logs"
const val WS_CONFIGURATIONS = "/configurations"

const val SELECTED_TAB_INDEX_KEY = "selected_tab_index"

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

const val PW_BASE_STORAGE_DIRECTORY = "/mnt/data"
const val PW_RECORDER_STORAGE_DIRECTORY = "$PW_BASE_STORAGE_DIRECTORY/recordings"
const val PW_CONFIGURATION_STORAGE_DIRECTORY = "$PW_BASE_STORAGE_DIRECTORY/configurations"

const val PW_RECORDER_NAME = "pw-record"
const val PW_RECORDER_LATENCY_MILLIS = 10000
const val PW_RECORDER_LATENCY_STR = "${PW_RECORDER_LATENCY_MILLIS}ms"
const val PW_RECORDER_CHANNEL_PREFIX = "AUX"
const val PW_RECORDER_CHANNEL_COUNT = 8
const val PW_RECORDER_RATE = "48000"
const val PW_RECORDER_FORMAT = "s16"

const val GT_1000 = "usb-BOSS_GT-1000-01"
const val GT_1000_MIDI = "GT-1000"
const val RC_500_MIDI = "BOSS_RC-500"
const val QUAD_CORTEX = "usb-Neural_DSP_Quad_Cortex-00"
const val QUAD_CORTEX_MIDI = "Quad Cortex"
const val ROLAND_TD_07 = "usb-Roland_TD-07-01"
const val ROLAND_TD_07_MIDI = "TD-07"
const val FLUIDSYNTH_AUDIO_NAME = "FluidSynth"
const val FLUIDSYNTH_MIDI_NAME = "FLUID Synth"
const val METRONOME_MIDI = "Jiminy_Metronome"
const val MIDI_THROUGH = "Midi Through"
const val MIDI_BRIDGE_PREFIX = "Midi-Bridge:"
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

const val RECORDER_PLAYBACK_ROOT = "input_$PW_RECORDER_CHANNEL_PREFIX"
const val RECORDER_MONITOR_ROOT = "monitor_$PW_RECORDER_CHANNEL_PREFIX"
const val RECORDER_PLAYBACK_1 = "${RECORDER_PLAYBACK_ROOT}0"
const val RECORDER_PLAYBACK_2 = "${RECORDER_PLAYBACK_ROOT}1"
const val RECORDER_PLAYBACK_3 = "${RECORDER_PLAYBACK_ROOT}2"
const val RECORDER_PLAYBACK_4 = "${RECORDER_PLAYBACK_ROOT}3"
const val RECORDER_PLAYBACK_5 = "${RECORDER_PLAYBACK_ROOT}4"
const val RECORDER_PLAYBACK_6 = "${RECORDER_PLAYBACK_ROOT}5"
const val RECORDER_PLAYBACK_7 = "${RECORDER_PLAYBACK_ROOT}6"
const val RECORDER_PLAYBACK_8 = "${RECORDER_PLAYBACK_ROOT}7"
const val RECORDER_PLAYBACK_9 = "${RECORDER_PLAYBACK_ROOT}8"
const val RECORDER_PLAYBACK_10 = "${RECORDER_PLAYBACK_ROOT}9"
const val RECORDER_MONITOR_1 = "${RECORDER_MONITOR_ROOT}0"
const val RECORDER_MONITOR_2 = "${RECORDER_MONITOR_ROOT}1"
const val RECORDER_MONITOR_3 = "${RECORDER_MONITOR_ROOT}2"
const val RECORDER_MONITOR_4 = "${RECORDER_MONITOR_ROOT}3"
const val RECORDER_MONITOR_5 = "${RECORDER_MONITOR_ROOT}4"
const val RECORDER_MONITOR_6 = "${RECORDER_MONITOR_ROOT}5"
const val RECORDER_MONITOR_7 = "${RECORDER_MONITOR_ROOT}6"
const val RECORDER_MONITOR_8 = "${RECORDER_MONITOR_ROOT}7"
const val RECORDER_MONITOR_9 = "${RECORDER_MONITOR_ROOT}8"
const val RECORDER_MONITOR_10 = "${RECORDER_MONITOR_ROOT}9"

enum class AvatarIconsEnum {
    METRONOME,
    GT_1000,
    RC_500,
    QUAD_CORTEX,
    ROLAND_TD_07,
    FLUIDSYNTH,
    SND_CARD_U_GREEN,
    SND_CARD_RED,
    SND_CARD_24,
    MIC_SHURE_MV88,
    RASPBERRY_PI,
    Unknown,
}

val deviceNameToAvatar = mapOf(
    GT_1000 to AvatarIconsEnum.GT_1000,
    QUAD_CORTEX to AvatarIconsEnum.QUAD_CORTEX,
    ROLAND_TD_07 to AvatarIconsEnum.ROLAND_TD_07,
    FLUIDSYNTH_AUDIO_NAME to AvatarIconsEnum.FLUIDSYNTH,
    SND_CARD_U_GREEN to AvatarIconsEnum.SND_CARD_U_GREEN,
    SND_CARD_RED to AvatarIconsEnum.SND_CARD_RED,
    SND_CARD_24 to AvatarIconsEnum.SND_CARD_24,
    MIC_SHURE_MV88 to AvatarIconsEnum.MIC_SHURE_MV88,

    MIDI_THROUGH to AvatarIconsEnum.RASPBERRY_PI,
    METRONOME_MIDI to AvatarIconsEnum.METRONOME,
    GT_1000_MIDI to AvatarIconsEnum.GT_1000,
    RC_500_MIDI to AvatarIconsEnum.RC_500,
    QUAD_CORTEX_MIDI to AvatarIconsEnum.QUAD_CORTEX,
    ROLAND_TD_07_MIDI to AvatarIconsEnum.ROLAND_TD_07,
    FLUIDSYNTH_MIDI_NAME to AvatarIconsEnum.FLUIDSYNTH,
)

val deviceNameToAlias = mapOf(
    METRONOME_MIDI to "Metronome",
    MIDI_THROUGH to "Pi",

    FLUIDSYNTH_MIDI_NAME to "Synth",
    FLUIDSYNTH_AUDIO_NAME to "Synth",
    "$FLUIDSYNTH_AUDIO_NAME:$INPUT_CAPTURE_1" to "Left",
    "$FLUIDSYNTH_AUDIO_NAME:$INPUT_CAPTURE_2" to "Right",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_1" to "Left",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_2" to "Right",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_3" to "3",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_4" to "4",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_5" to "5",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_6" to "6",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_7" to "7",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_8" to "8",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_PLAYBACK_1" to "Playback Left",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_PLAYBACK_2" to "Playback Right",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$FLUIDSYNTH_AUDIO_NAME:$OUTPUT_MONITOR_2" to "Monitor Right",

    QUAD_CORTEX_MIDI to "Umberto",
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

    ROLAND_TD_07_MIDI to "Iris",
    ROLAND_TD_07 to "Iris",
    "$ROLAND_TD_07:$INPUT_CAPTURE_1" to "Left",
    "$ROLAND_TD_07:$INPUT_CAPTURE_2" to "Right",
    "$ROLAND_TD_07:$OUTPUT_PLAYBACK_1" to "Left",
    "$ROLAND_TD_07:$OUTPUT_PLAYBACK_2" to "Right",
    "$ROLAND_TD_07:$OUTPUT_MONITOR_1" to "Monitor Left",
    "$ROLAND_TD_07:$OUTPUT_MONITOR_2" to "Monitor Right",

    RC_500_MIDI to "Looper",
    GT_1000_MIDI to "Nic",
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
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_9" to "Mon Trk 9",
    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_10" to "Mon Trk 10",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_1" to "Rec Mon 1",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_2" to "Rec Mon 2",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_3" to "Rec Mon 3",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_4" to "Rec Mon 4",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_5" to "Rec Mon 5",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_6" to "Rec Mon 6",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_7" to "Rec Mon 7",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_8" to "Rec Mon 8",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_9" to "Rec Mon 9",
    "$PW_RECORDER_NAME:$RECORDER_MONITOR_10" to "Rec Mon 10",
)
