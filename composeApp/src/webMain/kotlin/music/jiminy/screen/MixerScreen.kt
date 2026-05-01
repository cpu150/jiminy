package music.jiminy.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.jiminy.DEVICE_CARD_INSTRUMENTS_COLOR
import music.jiminy.DEVICE_CARD_SPEAKERS_COLOR
import music.jiminy.DEVICE_CARD_WIDTH
import music.jiminy.JiminyCommand
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNodeType.Instrument
import music.jiminy.JiminyVolume
import music.jiminy.MIXER_SLIDER_HEIGHT
import music.jiminy.MIXER_SLIDER_WIDTH
import music.jiminy.screen.common.DeviceCard
import music.jiminy.screen.common.TextLabel

@Composable
fun MixerScreen(
    devices: () -> List<JiminyDevice>,
    succeededCommands: JiminyCommand?,
    onVolumeChange: (JiminyVolume, Float) -> Unit,
    onMuteStateChange: (JiminyVolume, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val devices by remember(devices()) { mutableStateOf(devices()) }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        devices.forEach { device ->
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp),
                    ),
            ) {
                DeviceCard { device }

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .width(DEVICE_CARD_WIDTH.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    device.volumes.forEach { volume ->
                        VerticalFader(
                            volume = { volume },
                            onVolumeChange = onVolumeChange,
                            onMuteStateChange = onMuteStateChange,
                            succeededCommands = succeededCommands,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalFader(
    volume: () -> JiminyVolume,
    onVolumeChange: (JiminyVolume, Float) -> Unit,
    onMuteStateChange: (JiminyVolume, Boolean) -> Unit,
    succeededCommands: JiminyCommand?,
    modifier: Modifier = Modifier,
) {
    val volume = volume()
    var volumeValue by remember(volume.volume) { mutableStateOf(volume.volume) }
    var muteValue by remember(volume.mute) { mutableStateOf(volume.mute) }
    var isDragging by remember { mutableStateOf(false) }
    var latestSucceededVolumeCommand: JiminyCommand.VolumeUpdate? by remember { mutableStateOf(null) }

    succeededCommands?.let { command ->
        when (command) {
            is JiminyCommand.VolumeUpdate -> if (command.deviceVolume == volume && !isDragging) {
                volumeValue = command.volume
                latestSucceededVolumeCommand = command
            }

            is JiminyCommand.MuteUpdate -> if (command.deviceVolume == volume) {
                muteValue = command.muteState
            }

            else -> Unit
        }
    }

    Column(
        modifier = modifier.width(32.dp).wrapContentHeight().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MuteButton(
            isMuted = muteValue,
            onClick = {
                muteValue = !muteValue
                onMuteStateChange(volume, muteValue)
            },
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Slider(
            enabled = !muteValue,
            value = volumeValue,
            onValueChange = { newVolume ->
                volumeValue = newVolume
                onVolumeChange(volume, newVolume)
            },
            modifier = Modifier
                .width(MIXER_SLIDER_WIDTH.dp)
                .height(MIXER_SLIDER_HEIGHT.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isDragging = true
                            tryAwaitRelease()
                            latestSucceededVolumeCommand?.volume?.let { volumeValue = it }
                            isDragging = false
                        }
                    )
                }.graphicsLayer {
                    rotationZ = -90f // Rotate to vertical
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }.layout { measurable, constraints ->
                    // Swap width and height constraints because of rotation
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxWidth,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(
                            -((placeable.width - placeable.height) / 2),
                            (placeable.width - placeable.height) / 2,
                        )
                    }
                },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                inactiveTrackColor = Color.Gray,
                activeTrackColor = if (volume.type == Instrument) {
                    DEVICE_CARD_INSTRUMENTS_COLOR
                } else {
                    DEVICE_CARD_SPEAKERS_COLOR
                }.let { Color(it) },
            ),
        )

        TextLabel(
            text = "${(volumeValue * 100).toInt()}%",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 8.dp),
        )
    }
}

@Composable
fun MuteButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clickable { onClick() }
            .background(
                if (isMuted) Color(0xFFD32F2F) else Color.Gray,
                RoundedCornerShape(4.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "Mute",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
