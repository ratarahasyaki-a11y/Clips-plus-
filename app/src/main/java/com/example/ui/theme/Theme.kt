package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ClipPulseColorScheme = darkColorScheme(
    primary = CoralPulse,
    onPrimary = OnCoral,
    primaryContainer = CoralContainer,
    onPrimaryContainer = InkPrimary,
    secondary = AcidLime,
    onSecondary = OnAcidLime,
    secondaryContainer = AcidLimeContainer,
    onSecondaryContainer = AcidLime,
    tertiary = CyanWave,
    onTertiary = OnCyan,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = CyanWave,
    background = ObsidianBg,
    onBackground = InkPrimary,
    surface = SurfaceDark,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceBright,
    outline = OutlineHairline,
    outlineVariant = SurfaceElevated,
    error = ErrorCrimson,
    onError = OnCoral
)

@Composable
fun ClipPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipPulseColorScheme,
        typography = Typography,
        content = content
    )
}
