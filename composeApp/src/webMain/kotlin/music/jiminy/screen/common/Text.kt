package music.jiminy.screen.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp

@Composable
fun TextBody(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        modifier = modifier,
    )
}

@Composable
fun TextButton(
    text: String,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier,
    )
}

@Composable
fun TextLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun TextError(
    text: String,
    color: Color = MaterialTheme.colorScheme.error,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        modifier = modifier,
    )
}

@Composable
fun TextTitle(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        style = style,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

@Composable
fun TextHeadline(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign? = null,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    fontWeight: FontWeight? = FontWeight.Bold,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight,
        style = style,
        modifier = modifier,
    )
}

@Composable
fun TextOnAir(
    text: String = "ON AIR",
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Text(
        text = text,
        color = Color.Red.copy(alpha = alpha),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            shadow = Shadow(color = Color.Red.copy(alpha = alpha), blurRadius = 30f)
        ),
    )
}

@Composable
fun TextHeadlineLarge(
    text: String,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
        style = MaterialTheme.typography.headlineMedium
            .copy(fontWeight = FontWeight.Black, fontSize = 26.sp),
    )
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
) {
    var textWidth by remember(text) { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }

    val shouldScroll = containerWidth in 1..<textWidth

    val scrollOffset = if (shouldScroll) {
        val maxOffset = (textWidth - containerWidth).toFloat()
        val duration = (maxOffset * 30).toInt().coerceIn(2000, 10000)

        val infiniteTransition = rememberInfiniteTransition(label = "marquee")
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -maxOffset,
            animationSpec = infiniteRepeatable(
                animation = tween(duration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "offset",
        )
        offset
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { containerWidth = it.size.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = color,
            style = style,
            maxLines = 1,
            modifier = Modifier
                .offset { IntOffset(scrollOffset.toInt(), 0) }
                .onGloballyPositioned { textWidth = it.size.width },
        )
    }
}
