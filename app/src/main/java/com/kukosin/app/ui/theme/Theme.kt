package com.kukosin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HiFiScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    primaryContainer = GoldDim,
    onPrimaryContainer = Cream,
    secondary = CreamMuted,
    onSecondary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = Ink,
    onSurface = Cream,
    surfaceVariant = InkElevated,
    onSurfaceVariant = CreamMuted,
    outline = Line,
    outlineVariant = Line,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HiFiScheme,
        typography = Typography,
        content = content,
    )
}
