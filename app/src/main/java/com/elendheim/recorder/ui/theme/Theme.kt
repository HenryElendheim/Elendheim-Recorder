package com.elendheim.recorder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark-mode first, by design. There is no light scheme: the app is meant to
// live in the dark-gray-and-soft-red world of the Elendheim suite.
private val ElendheimColors = darkColorScheme(
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
    error = RecorderAccent,
    onError = RecorderBackground
)

@Composable
fun ElendheimRecorderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ElendheimColors,
        typography = Typography(),
        content = content
    )
}
