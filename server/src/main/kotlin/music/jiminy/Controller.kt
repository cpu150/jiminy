package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds

class Controller : JiminyServerControllerI {

    override suspend fun executeCommand(command: JiminyCommand) = when (command) {
        is JiminyCommand.VolumeUpdate -> updateVolume(command.deviceVolume, command.volume)
        is JiminyCommand.MuteUpdate -> updateMuteState(command.deviceVolume, command.muteState)
        is JiminyCommand.Link -> linkDevice(command)
        is JiminyCommand.StartRecording -> startRecording(command)
        is JiminyCommand.StopRecording -> stopRecording()
    }.also { println("Jiminy Server - Received: $command") }

    private suspend fun updateVolume(
        deviceVolume: JiminyVolume,
        volume: Float,
    ) = withContext(Dispatchers.IO) {
        try {
            ProcessBuilder("wpctl", "set-volume", deviceVolume.id, "$volume")
                .redirectErrorStream(true)
                .start().waitFor().let {
                    if (it != 0) {
                        println("Jiminy Server - updateVolume - ERROR - code $it - $deviceVolume")
                    }
                    it == 0
                }
        } catch (e: CancellationException) {
            println("Jiminy Server - updateMuteState - Canceled - $e")
            throw e
        } catch (e: Exception) {
            println("Jiminy Server - updateVolume - ERROR - $deviceVolume[$volume] - ${e.message} - $e")
            false
        }
    }

    private suspend fun updateMuteState(
        deviceVolume: JiminyVolume,
        mute: Boolean,
    ) = withContext(Dispatchers.IO) {
        try {
            ProcessBuilder("wpctl", "set-mute", deviceVolume.id, if (mute) "1" else "0")
                .redirectErrorStream(true)
                .start()
                .waitFor()
                .let {
                    if (it != 0) {
                        println("Jiminy Server - updateMuteState - ERROR - code $it - $deviceVolume")
                    }
                    it == 0
                }
        } catch (e: CancellationException) {
            println("Jiminy Server - updateMuteState - Canceled - $e")
            throw e
        } catch (e: Exception) {
            println("Jiminy Server - updateMuteState - ERROR - $deviceVolume[$mute] - ${e.message} - $e")
            false
        }
    }

    override suspend fun getDevicesList() = withContext(Dispatchers.IO) {
        withTimeout(3.seconds) {
            val instrumentsDevicesDeferred = async(Dispatchers.IO) { runCommand("pw-link", "-o") }
            val speakersDevicesDeferred = async(Dispatchers.IO) { runCommand("pw-link", "-i") }
            val devicesDeferred = async(Dispatchers.IO) { runCommand("wpctl", "status", "-n") }

            JiminyDeviceList(
                instruments = instrumentsDevicesDeferred.await(),
                speakers = speakersDevicesDeferred.await(),
                deviceStatus = devicesDeferred.await(),
            )
        }
    }

    override suspend fun linkDevice(link: JiminyCommand.Link) = withContext(Dispatchers.IO) {
        try {
            if (link.type == LinkType.Connect) {
                ProcessBuilder("pw-link", link.instrument, link.speaker)
            } else {
                ProcessBuilder("pw-link", "-d", link.instrument, link.speaker)
            }.redirectErrorStream(true).start().waitFor().let {
                // Code 255: wiring "speaker" into an "instrument" (wrong) + wiring twice
                if (it != 0) println("Jiminy Server - linkDevice - ERROR - code $it - $link")
                it == 0
            }
        } catch (e: CancellationException) {
            println("Jiminy Server - linkDevice - Canceled - $e")
            throw e
        } catch (e: Exception) {
            println("Jiminy Server - linkDevice - ERROR - $link - ${e.message} - $e")
            false
        }
    }

    override suspend fun getDeviceLinksList() =
        withContext(Dispatchers.IO) { runCommand("pw-link", "-l") }

    override suspend fun broadcastAll(
        sessions: List<DefaultWebSocketServerSession>,
        command: JiminyCommand,
        status: Boolean,
    ) = withContext(Dispatchers.IO) {
        sessions.forEach { session ->
            try {
                if (status) {
                    // Use sendSerialized to send the object back as JSON
                    session.sendSerialized(command)
                } else {
                    println("Jiminy Server - WebSocket - status=FAILED while executing $command")
                }
            } catch (e: Exception) {
                // If sending fails, the session might be dead
                println("Jiminy Server - ERROR - WebSocket broadcasting error: $e - ${e.localizedMessage}")
            }
        }
    }

    private var recordProcess: Process? = null
    private val virtualSinkName = PIPEWIRE_RECORDER

    @OptIn(ExperimentalAtomicApi::class)
    private val _isRecording = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    override val isRecording: Boolean
        get() = _isRecording.load()

    private fun getChannelName(index: Int) = "track$index"

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun startRecording(commands: JiminyCommand.StartRecording) =
        withContext(Dispatchers.IO) {
            commands.recoders
                // IMPORTANT: 1st check if the list is empty and abort before setting 'isRecording = true'
                .takeIf { it.isNotEmpty() }
                ?.takeIf { _isRecording.compareAndSet(expectedValue = false, newValue = true) }
                ?.let { recorders ->
                    try {
                        val channelCount = recorders.size
                        val trackNames = recorders.joinToString(",") { "\"${it.label}\"" }
                        val channelNames =
                            List(recorders.size) { getChannelName(it) }.joinToString(",")
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH-mm-ss")
                        val date = current.format(formatter)
                        val filename = "\"$date.wav\""

                        // Create the Multichannel Virtual Sink (e.g., 6 channels)
                        // We use pactl for this as it's the most reliable way to spawn a null-sink
                        ProcessBuilder(
                            "pactl", "load-module", "module-null-sink",
                            "sink_name=$virtualSinkName",
                            "channels=$channelCount",
                            "channel_map=$channelNames",
                            "metadata=node.name=$virtualSinkName",
                        ).start().waitFor()

                        // Start pw-record targeting the Virtual Sink
                        val recordProcess = ProcessBuilder(
                            "pw-record",
                            "--target", virtualSinkName,
                            "--channels", "$channelCount",
                            "--channel-map", channelNames,
                            "--property", "node.name=\"Jiminy Recorder\"",
                            "--property", "media.title=\"Jiminy Session\"",
                            "--property", "node.description=\"Recorded the $date\"",
                            "--property", "media.title=$trackNames",
                            filename,
                        )
//                recordProcess.environment()["XDG_RUNTIME_DIR"] = "/run/user/1000"
//                recordProcess.directory(File("/home/pi/recordings")) // Ensure this folder exists!
                        recordProcess.start()

                        // Small delay to let the recording node register in the graph
                        delay(1000)

                        // Link each instrument to its dedicated channel pair
                        recorders.forEachIndexed { index, instrument ->
//                    "playback_" or "input_" ??
                            val speaker = "playback_${getChannelName(index)}"
                            linkDevice(
                                JiminyCommand.Link(instrument.nodeName, speaker, LinkType.Connect)
                            )
                        }
                        true
                    } catch (e: CancellationException) {
                        println("Jiminy Server - startRecording - Canceled - $e")
                        stopRecording()
                        throw e
                    } catch (e: Exception) {
                        println("Recording failed to start: ${e.message}")
                        stopRecording()
                        false
                    }
                } ?: false
        }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun stopRecording() = try {
        withContext(Dispatchers.IO) {
            // Stop recording
            recordProcess?.destroy()

            // Clean up: Remove the virtual sink
            ProcessBuilder("pactl", "unload-module", "module-null-sink").start().waitFor()

            recordProcess = null
            true
        }
    } catch (e: CancellationException) {
        println("Jiminy Server - stopRecording - Canceled - $e")
        throw e
    } catch (e: Exception) {
        println("Recording failed to stop: ${e.message}")
        false
    } finally {
        _isRecording.store(false)
    }

    private fun runCommand(vararg command: String): List<String> {
        val process = ProcessBuilder(*command).redirectError(ProcessBuilder.Redirect.PIPE).start()
        val result = process.inputStream.bufferedReader().readLines()

        // Safety timeout
        process.waitFor(3, TimeUnit.SECONDS)

        return result
    }
}
