package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FacebookBlue,
    secondary = FacebookLightBlue,
    tertiary = FacebookReactionYellow,
    background = FacebookDarkBg,
    surface = FacebookSurface,
    onPrimary = Color.White,
    onSecondary = FacebookTextPrimary,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FacebookBlue,
    primaryContainer = FacebookLightBlue,
    secondary = FacebookBlue,
    secondaryContainer = FacebookDivider,
    tertiary = FacebookReactionYellow,
    background = FacebookFeedBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = FacebookTextPrimary,
    onSurface = FacebookTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
