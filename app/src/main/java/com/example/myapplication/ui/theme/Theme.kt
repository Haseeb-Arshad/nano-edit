package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF12C2A9)
private val DarkBackground = Color(0xFF0E0E0F)
private val LightBackground = Color(0xFFFFFFFF)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Teal,
    secondary = Teal,
    background = LightBackground,
    surface = LightBackground,
    onPrimary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Teal,
    secondary = Teal,
    background = DarkBackground,
    surface = Color(0xFF161617),
    onPrimary = Color.White,
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

