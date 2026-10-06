package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarStreamColorScheme = darkColorScheme(
    primary = CrimsonStream,
    onPrimary = Color.White,
    primaryContainer = CrimsonSoft,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = ElectricCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = CyanSoft,
    onSecondaryContainer = Color(0xFFB8F8FF),
    tertiary = EmeraldTelemetry,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CockpitCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CarStreamColorScheme,
        typography = Typography,
        content = content
    )
}
