package music.jiminy.screen

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import music.jiminy.DEVICE_CARD_HEIGHT
import music.jiminy.JiminyDevice
import music.jiminy.screen.common.DeviceCard
import music.jiminy.screen.common.DraggableScreen
import music.jiminy.viewmodel.MIDIScreenViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MIDIScreen(
    modifier: Modifier = Modifier,
) {
    MIDIRoot(modifier)
}

@Composable
fun MIDIRoot(
    modifier: Modifier = Modifier,
    viewModel: MIDIScreenViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadData() }

    DraggableScreen(
        draggableItem = { item: JiminyDevice ->
            val scale = 1.05f
            DeviceCard(
                modifier = Modifier
                    .height((DEVICE_CARD_HEIGHT * scale).dp)
                    .width((DEVICE_CARD_HEIGHT * scale).dp),
                device = { item },
            )
        },
        modifier = modifier,
    ) { activeDraggingItem, offset, containerPosition ->
        MainConnectionScreen(
            state = state,
            onAction = viewModel::onAction,
            activeDraggingItem = activeDraggingItem,
            dragOffset = offset,
            containerPosition = containerPosition,
        )
    }
}
