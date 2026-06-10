package music.jiminy.screen.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import music.jiminy.JiminyThemeType

private val OceanColorScheme = darkColorScheme(
    primary = Color(0xFF00ACC1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF006064),
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = Color(0xFF26C6DA),
    onSecondary = Color.Black,
    background = Color(0xFF001F24),
    surface = Color(0xFF001F24),
    onBackground = Color(0xFFE0F7FA),
    onSurface = Color(0xFFE0F7FA),
    surfaceVariant = Color(0xFF00363D),
    onSurfaceVariant = Color(0xFFB2EBF2),
)

@Composable
fun JiminyTheme(
    themeType: JiminyThemeType,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeType) {
        JiminyThemeType.DARK -> darkColorScheme()
        JiminyThemeType.LIGHT -> lightColorScheme()
        JiminyThemeType.OCEAN -> OceanColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
