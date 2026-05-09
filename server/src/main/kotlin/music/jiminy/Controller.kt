package music.jiminy

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.seconds

class Controller(
    private val logger: JiminyLoggerI,
) : JiminyServerControllerI {

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

    private var recordProcess: Process? = null

    @OptIn(ExperimentalAtomicApi::class)
    private val _isRecording = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    override val isRecording: Boolean
        get() = _isRecording.load()

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun startRecording(
        commands: JiminyCommand.StartRecording,
    ) = withContext(Dispatchers.IO) {
        // At least one node is connected to the recorder
        runCommand("pw-link", "-l")
            .firstOrNull { it.contains("${PW_RECORDER_NAME}:$RECORDER_PLAYBACK_ROOT") }
            ?.takeIf {
                // Not already recording then set it as recording now
                _isRecording.compareAndSet(expectedValue = false, newValue = true)
            }?.let {
                try {
                    startRecording()
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

    private fun startRecording() {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd - HH-mm-ss")
        val date = current.format(formatter)
        val filename = "$date.wav"

        val task = ProcessBuilder(
            "pw-record",
            "--target", PW_RECORDER_NAME,
            "--channels", PW_RECORDER_CHANNEL_COUNT_STR,
            "--rate", PW_RECORDER_RATE,
            "--format", PW_RECORDER_FORMAT,
            filename,
        )
        task.directory(File(PW_RECORDER_DIRECTORY))
        recordProcess = task.start()
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun stopRecording() = try {
        withContext(Dispatchers.IO) {
            recordProcess?.takeIf { it.isAlive }?.destroy()
            recordProcess = null
            true
        }
    } catch (e: CancellationException) {
        logger.info("Jiminy Server - stopRecording - Cancelled - $e")
        throw e
    } catch (e: Exception) {
        logger.error("Recording failed to stop: ${e.message}")
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
