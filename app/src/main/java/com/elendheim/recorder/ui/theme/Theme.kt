package com.elendheim.recorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark-mode first, by design. Two schemes: the standard look, and a
// high-contrast variant driven by the accessibility setting.
private val StandardColors = darkColorScheme(
    primary = RecorderAccent,
    onPrimary = RecorderBackground,
    secondary = RecorderAccent,
    onSecondary = RecorderBackground,
    background = RecorderBackground,
    onBackground = RecorderText,
    surface = RecorderSurface,
    onSurface = RecorderText,
    surfaceVariant = RecorderSurface,
    onSurfaceVariant = RecorderTextDim,
    outline = RecorderTextDim,
    error = RecorderAccent,
    onError = RecorderBackground
)

private val HighContrastColors = darkColorScheme(
    primary = HcAccent,
    onPrimary = HcBackground,
    secondary = HcAccent,
    onSecondary = HcBackground,
    background = HcBackground,
    onBackground = HcText,
    surface = HcSurface,
    onSurface = HcText,
    surfaceVariant = HcSurface,
    onSurfaceVariant = HcTextDim,
    outline = HcOutline,
    error = HcAccent,
    onError = HcBackground
)

@Composable
fun ElendheimRecorderTheme(
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (highContrast) HighContrastColors else StandardColors,
        typography = Typography(),
        content = content
    )
}
