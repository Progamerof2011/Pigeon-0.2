package fr.pigeon.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette centralisée : changer ces valeurs suffit pour retoucher toute l'app.
val PigeonNavy = Color(0xFF0B1F3A)
val PigeonNavyLight = Color(0xFF13305A)
val PigeonNavyCard = Color(0xFF1B3A66)
val PigeonTeal = Color(0xFF2EC4B6)
val PigeonInk = Color(0xFFE8F0FA)
val PigeonMuted = Color(0xFF8FA3BF)

private val PigeonDarkScheme = darkColorScheme(
    primary = PigeonTeal,
    onPrimary = PigeonNavy,
    secondary = PigeonTeal,
    background = PigeonNavy,
    onBackground = PigeonInk,
    surface = PigeonNavyLight,
    onSurface = PigeonInk,
    surfaceVariant = PigeonNavyCard,
    onSurfaceVariant = PigeonMuted,
)

@Composable
fun PigeonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PigeonDarkScheme, content = content)
}
