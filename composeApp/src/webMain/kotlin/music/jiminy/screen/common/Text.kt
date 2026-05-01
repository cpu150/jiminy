package music.jiminy.screen.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.bodySmall,
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
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        style = MaterialTheme.typography.titleMedium,
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
