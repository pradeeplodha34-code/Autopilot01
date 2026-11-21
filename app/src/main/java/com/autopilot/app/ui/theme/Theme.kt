package com.autopilot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = MidnightNavy,
    secondary = ElectricCyan,
    onSecondary = MidnightNavy,
    secondaryContainer = Color(0xFFD6F4FA),
    onSecondaryContainer = Color(0xFF003544),
    tertiary = CobaltDark,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = StatusError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyTint,
    onPrimary = MidnightNavy,
    primaryContainer = MidnightNavySurface,
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = ElectricCyan,
    onSecondary = MidnightNavy,
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFB3EEFA),
    tertiary = ElectricCyan,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val AutoPilotShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AutoPilotTheme(
    themePreference: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AutoPilotTypography,
        shapes = AutoPilotShapes,
        content = content
    )
}
