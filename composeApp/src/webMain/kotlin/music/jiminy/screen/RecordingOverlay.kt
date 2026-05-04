package music.jiminy.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecordingOverlay(
    onStopRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Breathing/glowing effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {}, // Prevents clicks from reaching the mixer below
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ON AIR",
            style = MaterialTheme.typography.headlineLarge
                .copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Red.copy(alpha = alpha),
                        blurRadius = 20f,
                    )
                ),
            color = Color.Red.copy(alpha = alpha),
        )

        Spacer(Modifier.size(40.dp))

        Button(
            onClick = onStopRequest,
            colors = ButtonDefaults.buttonColors(contentColor = Color(0xFFB71C1C)),
            modifier = Modifier
                .size(width = 280.dp, height = 80.dp)
                .clip(RoundedCornerShape(50)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        ) {
            Text(
                text = "STOP RECORDING",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            )
        }
    }
}
