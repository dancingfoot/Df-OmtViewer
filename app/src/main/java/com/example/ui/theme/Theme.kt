package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MedicalColorScheme = darkColorScheme(
    primary = MedicalCyan,
    onPrimary = DarkCanvas,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = MedicalCyan,
    secondary = MedicalTeal,
    onSecondary = DarkCanvas,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = MedicalTeal,
    tertiary = MedicalEmerald,
    onTertiary = DarkCanvas,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = MedicalRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MedicalColorScheme,
        typography = Typography,
        content = content
    )
}
