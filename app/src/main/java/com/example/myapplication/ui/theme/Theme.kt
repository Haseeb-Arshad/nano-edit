package com.example.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ModernLightColors: ColorScheme = lightColorScheme(
    primary = NeonPink,
    secondary = NeonBlue,
    tertiary = NeonPurple,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = GlassWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    primaryContainer = HotPink,
    secondaryContainer = ElectricBlue,
    tertiaryContainer = NeonPurple,
    error = AccentRed,
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    outline = GlassBorder,
    outlineVariant = FrostedGlass,
    scrim = DarkOverlay,
    inversePrimary = NeonYellow
)

private val ModernDarkColors: ColorScheme = darkColorScheme(
    primary = NeonPink,
    secondary = NeonBlue,
    tertiary = NeonPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = GlassBlack,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE5E5E5),
    onSurface = Color(0xFFE5E5E5),
    primaryContainer = HotPink.copy(alpha = 0.3f),
    secondaryContainer = ElectricBlue.copy(alpha = 0.3f),
    tertiaryContainer = NeonPurple.copy(alpha = 0.3f),
    error = AccentRed,
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = GlassBorder,
    outlineVariant = FrostedGlass,
    scrim = DarkOverlay,
    inversePrimary = NeonYellow
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ModernDarkColors else ModernLightColors,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(48.dp),
        ),
        content = content
    )
}

