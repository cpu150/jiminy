package music.jiminy.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import music.jiminy.DEVICE_LIST_CARD_HEIGHT
import music.jiminy.DEVICE_LIST_CARD_WIDTH
import music.jiminy.JiminyDeviceI
import music.jiminy.JiminyDeviceNodeI

@Composable
fun <T : JiminyDeviceI<T>> DeviceCardNodeDetails(
    device: () -> T,
    deviceNodes: () -> List<JiminyDeviceNodeI>,
    onDeviceClick: (T) -> Unit,
    onNodeClick: (JiminyDeviceNodeI) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(2.dp)
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        DeviceCard(
            device = device,
            showLabels = false,
            modifier = Modifier
                .clickable(onClick = { onDeviceClick(device()) })
                .height(DEVICE_LIST_CARD_HEIGHT.dp)
                .width(DEVICE_LIST_CARD_WIDTH.dp),
        )

        Column(Modifier.wrapContentHeight().fillMaxWidth()) {
            deviceNodes().forEach { node ->
                NodeDetails(
                    nodeName = node.displayPortName,
                    onClick = { onNodeClick(node) },
                )
            }
        }
    }
}

@Composable
fun NodeDetails(
    nodeName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextBody(
        text = nodeName,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .wrapContentHeight()
            .fillMaxWidth(),
    )
}
