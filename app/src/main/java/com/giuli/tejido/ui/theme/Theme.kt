package com.giuli.tejido.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF9C4A5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3F0018),
    secondary = Color(0xFF77565B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF2C1519),
    tertiary = Color(0xFF7A5733),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBC),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
    outline = Color(0xFF847375),
    outlineVariant = Color(0xFFD6C2C4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F1),
    surfaceContainer = Color(0xFFFCEAEC),
    surfaceContainerHigh = Color(0xFFF6E4E6),
    surfaceContainerHighest = Color(0xFFF0DEE0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C0),
    onPrimary = Color(0xFF5E1129),
    primaryContainer = Color(0xFF7D3243),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE6BDC2),
    onSecondary = Color(0xFF43292E),
    secondaryContainer = Color(0xFF5D3F44),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFECBE91),
    onTertiary = Color(0xFF462A09),
    tertiaryContainer = Color(0xFF60401E),
    onTertiaryContainer = Color(0xFFFFDCBC),
    background = Color(0xFF1A1113),
    onBackground = Color(0xFFF0DEE0),
    surface = Color(0xFF1A1113),
    onSurface = Color(0xFFF0DEE0),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C4),
    outline = Color(0xFF9F8C8F),
    outlineVariant = Color(0xFF524345),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainerLow = Color(0xFF22191B),
    surfaceContainer = Color(0xFF271D1F),
    surfaceContainerHigh = Color(0xFF322729),
    surfaceContainerHighest = Color(0xFF3D3234),
)

@Composable
fun GiuliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
