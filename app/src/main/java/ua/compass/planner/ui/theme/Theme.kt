package ua.compass.planner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ua.compass.planner.data.Quadrant

private val LightColors = lightColorScheme(
    primary = Color(0xFF3390EC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E9FC),
    onPrimaryContainer = Color(0xFF0B3A66),
    secondary = Color(0xFF5B87B0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFEAF5),
    onSecondaryContainer = Color(0xFF16324A),
    tertiary = Color(0xFF3F9E8F),
    onTertiary = Color.White,
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF1B2430),
    surface = Color(0xFFF4F7FB),
    onSurface = Color(0xFF1B2430),
    surfaceVariant = Color(0xFFE3EAF2),
    onSurfaceVariant = Color(0xFF56636F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEDF2F8),
    surfaceContainerHigh = Color(0xFFE7EEF6),
    surfaceContainerHighest = Color(0xFFE0E8F1),
    outline = Color(0xFF8B99A8),
    outlineVariant = Color(0xFFD0D9E3),
)

// Inspired by Telegram's "Night" theme.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF6AB2F2),
    onPrimary = Color(0xFF0D2A45),
    primaryContainer = Color(0xFF2B5278),
    onPrimaryContainer = Color(0xFFD7E9FC),
    secondary = Color(0xFF8DB4D9),
    onSecondary = Color(0xFF14293D),
    secondaryContainer = Color(0xFF2A3D52),
    onSecondaryContainer = Color(0xFFD5E4F3),
    tertiary = Color(0xFF6CC5B6),
    onTertiary = Color(0xFF0D2E29),
    background = Color(0xFF17212B),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF17212B),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF242F3D),
    onSurfaceVariant = Color(0xFFA0AEBE),
    surfaceContainerLowest = Color(0xFF111A22),
    surfaceContainerLow = Color(0xFF1E2A37),
    surfaceContainer = Color(0xFF202C39),
    surfaceContainerHigh = Color(0xFF263342),
    surfaceContainerHighest = Color(0xFF2C3A4B),
    outline = Color(0xFF6C7A89),
    outlineVariant = Color(0xFF34414F),
)

@Composable
fun CompassTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

fun quadrantColor(q: Quadrant): Color = when (q) {
    Quadrant.I -> Color(0xFFEF5350)
    Quadrant.II -> Color(0xFF3390EC)
    Quadrant.III -> Color(0xFFFFA726)
    Quadrant.IV -> Color(0xFF90A4AE)
}
