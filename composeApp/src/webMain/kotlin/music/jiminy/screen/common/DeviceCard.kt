package music.jiminy.screen.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import music.jiminy.DEVICE_CARD_HEIGHT
import music.jiminy.DEVICE_CARD_INSTRUMENTS_COLOR
import music.jiminy.DEVICE_CARD_INSTRUMENTS_LABEL
import music.jiminy.DEVICE_CARD_SPEAKERS_COLOR
import music.jiminy.DEVICE_CARD_SPEAKERS_LABEL
import music.jiminy.DEVICE_CARD_WIDTH
import music.jiminy.JiminyDevice
import music.jiminy.JiminyDeviceNode
import music.jiminy.JiminyDeviceNodeType

@Composable
fun DeviceCard(
    showLabels: Boolean = true,
    modifier: Modifier = Modifier,
    device: () -> JiminyDevice,
) {
    Card(
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .height(DEVICE_CARD_HEIGHT.dp)
            .width(DEVICE_CARD_WIDTH.dp),
    ) {
        Column(
            modifier = Modifier.padding(4.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            DeviceHeader(device, Modifier.wrapContentHeight().fillMaxWidth())
            DeviceAvatar(device().avatarIcon.toResource(), Modifier.weight(1f).aspectRatio(1f))

            if (showLabels) {
                // Instruments/Speakers info
                Row(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    InfoLabel(
                        label = DEVICE_CARD_INSTRUMENTS_LABEL,
                        count = device().instruments.count(),
                        color = Color(DEVICE_CARD_INSTRUMENTS_COLOR),
                        modifier = Modifier.weight(1f),
                    )
                    InfoLabel(
                        label = DEVICE_CARD_SPEAKERS_LABEL,
                        count = device().speakers.count(),
                        color = Color(DEVICE_CARD_SPEAKERS_COLOR),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DeviceCardPreview() {
    MaterialTheme {
        DeviceCard(
            device = {
                JiminyDevice("Test Device").apply {
                    addNode(
                        JiminyDeviceNode(
                            fullName = "dev:instrument",
                            deviceName = "Test Device",
                            portName = "In 1",
                            type = JiminyDeviceNodeType.Instrument
                        )
                    )
                    addNode(
                        JiminyDeviceNode(
                            fullName = "dev:speaker",
                            deviceName = "Test Device",
                            portName = "Out 1",
                            type = JiminyDeviceNodeType.Speaker
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun DeviceHeader(
    device: () -> JiminyDevice,
    modifier: Modifier = Modifier,
) {
    TextTitle(
        text = device().displayName,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun InfoLabel(
    label: String,
    count: Int,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    TextLabel(
        text = if (count > 0) "$count $label" else "N/A",
        color = if (count > 0) color else Color.Gray,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
