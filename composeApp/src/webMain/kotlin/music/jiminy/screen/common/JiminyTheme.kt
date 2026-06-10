package music.jiminy.screen.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import music.jiminy.JiminyThemeType

private val IrisColorScheme = darkColorScheme(
    primary = Color(0xFFEC407A), // Pink 400
    onPrimary = Color.White,
    primaryContainer = Color(0xFF880E4F), // Pink 900
    onPrimaryContainer = Color(0xFFF8BBD0), // Pink 100
    secondary = Color(0xFFF06292), // Pink 300
    onSecondary = Color.Black,
    background = Color(0xFF1A0A10), // Very dark pink/black
    surface = Color(0xFF1A0A10),
    onBackground = Color(0xFFFCE4EC), // Pink 50
    onSurface = Color(0xFFFCE4EC),
    surfaceVariant = Color(0xFF3F1626), // Dark pinkish grey
    onSurfaceVariant = Color(0xFFF8BBD0),
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
