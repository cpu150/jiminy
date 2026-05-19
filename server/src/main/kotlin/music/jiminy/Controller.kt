package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Controller(
    private val logger: JiminyLoggerI,
) : JiminyServerControllerI {

    private data class RecordProcessInfo(val filename: String, val process: Process)

    override suspend fun executeCommand(command: JiminyCommand) = when (command) {
        is JiminyCommand.VolumeUpdate -> updateVolume(command.deviceVolume, command.volume)
        is JiminyCommand.MuteUpdate -> updateMuteState(command.deviceVolume, command.muteState)
        is JiminyCommand.Link -> linkDevice(command)
        is JiminyCommand.StartRecording -> startRecording(command)
        is JiminyCommand.StopRecording -> stopRecording()
    }.also { logger.info("Jiminy Server - Received: $command") }

    private suspend fun updateVolume(
        deviceVolume: JiminyVolume,
        volume: Float,
    ) = withContext(Dispatchers.IO) {
        try {
            ProcessBuilder("wpctl", "set-volume", deviceVolume.id, "$volume")
                .redirectErrorStream(true)
                .start().waitFor().let {
                    if (it != 0) {
                        logger.error("Jiminy Server - updateVolume - ERROR - code $it - $deviceVolume")
                    }
                    it == 0
                }
        } catch (e: CancellationException) {
            logger.info("Jiminy Server - updateMuteState - Cancelled - $e")
            throw e
        } catch (e: Exception) {
            logger.error("Jiminy Server - updateVolume - ERROR - $deviceVolume[$volume] - ${e.message} - $e")
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
                        logger.error("Jiminy Server - updateMuteState - ERROR - code $it - $deviceVolume")
                    }
                    it == 0
                }
        } catch (e: CancellationException) {
            logger.info("Jiminy Server - updateMuteState - Cancelled - $e")
            throw e
        } catch (e: Exception) {
            logger.error("Jiminy Server - updateMuteState - ERROR - $deviceVolume[$mute] - ${e.message} - $e")
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
                if (it != 0) logger.error("Jiminy Server - linkDevice - ERROR - code $it - $link")
                it == 0
            }
        } catch (e: CancellationException) {
            logger.info("Jiminy Server - linkDevice - Cancelled - $e")
            throw e
        } catch (e: Exception) {
            logger.error("Jiminy Server - linkDevice - ERROR - $link - ${e.message} - $e")
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
                    logger.error("Jiminy Server - WebSocket - status=FAILED while executing $command")
                }
            } catch (e: Exception) {
                // If sending fails, the session might be dead
                logger.error("Jiminy Server - ERROR - WebSocket broadcasting error: $e - ${e.localizedMessage}")
            }
        }
    }

    private var currentRecording: RecordProcessInfo? = null

    @OptIn(ExperimentalAtomicApi::class)
    private val _isRecording = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    override val isRecording: Boolean
        get() = _isRecording.load()

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun startRecording(
        commands: JiminyCommand.StartRecording,
    ) = withContext(Dispatchers.IO) {
        takeIf { commands.nodes.isNotEmpty() }
            ?.takeIf { _isRecording.compareAndSet(expectedValue = false, newValue = true) }
            ?.let {
                try {
                    currentRecording = startRecording(commands.nodes)
                    true
                } catch (e: CancellationException) {
                    logger.info("Jiminy Server - startRecording - Cancelled - $e")
                    stopRecording()
                    throw e
                } catch (e: Exception) {
                    logger.error("Recording failed to start: ${e.message}")
                    stopRecording()
                    false
                }
            } ?: false
    }

    private fun startRecording(nodes: List<JiminyDeviceNode>) = let {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH-mm-ss")
        val date = current.format(formatter)
        val filename = "$date.wav"

        var positionProperty = ""
        nodes.forEachIndexed { index, _ ->
            // Must end with ',' (ex: "AUX0,AUX1,AUX2,")
            positionProperty += "$PW_RECORDER_CHANNEL_PREFIX$index,"
        }

        val task = ProcessBuilder(
            "pw-record",
            "--latency", PW_RECORDER_LATENCY_STR,
            "--target", "0",
            "--rate", PW_RECORDER_RATE,
            "--format", PW_RECORDER_FORMAT,
            "--channels", "${nodes.count()}",
            "--channel-map", positionProperty,
            filename,
        )
        task.directory(File(PW_RECORDER_BUFFER_DIRECTORY))
        val process = task.start()

        // We must programmatically issue the pw-link commands right after the process surfaces!
        CoroutineScope(Dispatchers.IO).launch {
            // Wait a brief moment for PipeWire to initialize the new raw node ports
            delay(800)

            nodes.forEachIndexed { index, node ->
                val link = JiminyCommand.Link(
                    node.fullName,
                    "$PW_RECORDER_NAME:$RECORDER_PLAYBACK_ROOT$index",
                    LinkType.Connect,
                )

                linkDevice(link)
            }
        }

        RecordProcessInfo(filename, process)
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun stopRecording() = let {
        val recording = synchronized(this) {
            val rec = currentRecording
            currentRecording = null
            rec
        }

        try {
            recording?.let { recording ->
                withContext(Dispatchers.IO) {
                    // Wait for the recording buffer to get fully written
                    delay(PW_RECORDER_LATENCY_MILLIS.milliseconds)

                    // Stop the recording
                    recording.process.takeIf { proc -> proc.isAlive }?.destroy()

                    // Move the recorded file from PW_RECORDER_BUFFER_DIRECTORY to PW_RECORDER_STORAGE_DIRECTORY
                    move(
                        recording.filename,
                        PW_RECORDER_BUFFER_DIRECTORY,
                        PW_RECORDER_STORAGE_DIRECTORY,
                    )
                }
            } ?: true
        } catch (e: CancellationException) {
            logger.info("Jiminy Server - stopRecording - Cancelled - $e")
            throw e
        } catch (e: Exception) {
            logger.error("Recording failed to stop: ${e.message}")
            false
        } finally {
            _isRecording.store(false)
        }
    }

    private fun move(filename: String, sourceStr: String, destStr: String) = try {
        val source = File(sourceStr, filename).toPath()
        val destination = File(destStr, filename).toPath()

        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        logger.info("Jiminy Server - File saved to storage: $filename")

        Files.delete(source)
        logger.info("Jiminy Server - Source file deleted: $filename")

        true
    } catch (e: CancellationException) {
        logger.error("Jiminy Server - stopRecording - Cancelled - Failed to save file to storage: $e - ${e.message}")
        throw e
    } catch (e: Exception) {
        logger.error("Jiminy Server - Failed to save file to storage: $e - ${e.message}")
        false
    }

    private fun runCommand(vararg command: String): List<String> {
        val process = ProcessBuilder(*command).redirectError(ProcessBuilder.Redirect.PIPE).start()
        val result = process.inputStream.bufferedReader().readLines()

        // Safety timeout
        process.waitFor(3, TimeUnit.SECONDS)

        return result
    }
}
