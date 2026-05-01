package music.jiminy.screen.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun <T> DraggableScreen(
    draggableItem: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (MutableState<T?>, MutableState<Offset>) -> Unit,
) {
    val activeDraggingItem = remember { mutableStateOf<T?>(null) }
    val offset = remember { mutableStateOf(Offset.Zero) }

    Box(modifier.fillMaxSize()) {
        content(activeDraggingItem, offset)

        activeDraggingItem.value?.let { item ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            offset.value.x.roundToInt(),
                            offset.value.y.roundToInt(),
                        )
                    }.width(250.dp) // Fixed width for the ghost so it doesn't jump
                    .zIndex(100f)
                    .graphicsLayer(alpha = 0.9f, scaleX = 1.05f, scaleY = 1.05f),
                content = { draggableItem(item) },
            )
        }
    }
}
