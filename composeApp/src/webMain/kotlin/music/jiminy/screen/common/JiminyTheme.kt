package music.jiminy.screen.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import music.jiminy.JiminyThemeType

private val IrisColorScheme = lightColorScheme(
    primary = Color(0xFFFF4081), // Flashy Pink (Accent Pink)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFF80AB), // Lighter Flashy Pink
    onPrimaryContainer = Color(0xFF560027),
    secondary = Color(0xFFF06292), // Medium Pink
    onSecondary = Color.White,
    background = Color(0xFFFFF0F5), // Lavender Blush (Very light pink background)
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFFD81B60), // Deep Pink for text
    onSurface = Color(0xFFD81B60),
    surfaceVariant = Color(0xFFFCE4EC), // Pink 50
    onSurfaceVariant = Color(0xFFAD1457),
)

@Composable
fun JiminyTheme(
    themeType: JiminyThemeType,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeType) {
        JiminyThemeType.DARK -> darkColorScheme()
        JiminyThemeType.LIGHT -> lightColorScheme()
        JiminyThemeType.IRIS -> IrisColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
