package com.samjhoenglish.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light themes only: kids' apps stay consistent regardless of the phone's dark mode.

/** Class 1–4: bright and warm. */
private val JuniorColors = lightColorScheme(
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

/** Class 5–8: calmer and cleaner, so an older child beginning at Level 1 doesn't feel it's "for babies". */
private val SeniorColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoPale,
    onPrimaryContainer = Ink,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Sunny,
    onTertiary = Ink,
    background = CoolWhite,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = IndigoPale,
    onSurfaceVariant = InkSoft,
    outline = CoolLine,
)

/** True for the Class 5–8 look: a smaller Tinku and more compact buttons. */
val LocalSeniorLook = staticCompositionLocalOf { false }

@Composable
fun SamjhoTheme(senior: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSeniorLook provides senior) {
        MaterialTheme(
            colorScheme = if (senior) SeniorColors else JuniorColors,
            typography = SamjhoTypography,
            content = content,
        )
    }
}
