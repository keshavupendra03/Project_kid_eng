package com.samjhoenglish.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A single bright light theme: kids' apps stay consistent regardless of the phone's dark mode.
private val SamjhoColors = lightColorScheme(
    primary = Saffron,
    onPrimary = Color.White,
    primaryContainer = Peach,
    onPrimaryContainer = Ink,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Sunny,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Peach,
    onSurfaceVariant = InkSoft,
    outline = Line,
)

@Composable
fun SamjhoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SamjhoColors,
        typography = SamjhoTypography,
        content = content,
    )
}
