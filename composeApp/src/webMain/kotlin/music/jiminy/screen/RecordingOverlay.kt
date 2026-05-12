package music.jiminy.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import music.jiminy.screen.common.TextHeadlineLarge
import music.jiminy.screen.common.TextOnAir

@Composable
fun RecordingOverlay(
    onStopRequest: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {}, // Prevents clicks from reaching the mixer below
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextOnAir()

        Spacer(Modifier.size(40.dp))

        Button(
            onClick = onStopRequest,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB71C1C),
                disabledContainerColor = Color(0xFFB71C1C).copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .size(width = 200.dp, height = 200.dp)
                .clip(RoundedCornerShape(50)),
        ) {
            TextHeadlineLarge(
                text = if (enabled) "STOP RECORDING" else "STOPPING...",
                textAlign = TextAlign.Center,
            )
        }
    }
}
