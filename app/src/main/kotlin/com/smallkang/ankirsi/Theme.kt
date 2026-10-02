package com.smallkang.ankirsi

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Warm paper, ink-black type and a single terracotta accent (the tertiary role).
private val Paper = Color(0xFFFAF9F5)
private val Ink = Color(0xFF1F1E1B)
private val Night = Color(0xFF1C1B19)
private val Chalk = Color(0xFFECEAE4)

private val Light = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    tertiary = Color(0xFFB4552F),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF75736C),
    surfaceTint = Paper,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFD5D2C8),
    outlineVariant = Color(0xFFE7E4DB),
    error = Color(0xFFB3412E),
)

private val Dark = darkColorScheme(
    primary = Chalk,
    onPrimary = Night,
    tertiary = Color(0xFFE08B67),
    background = Night,
    onBackground = Chalk,
    surface = Night,
    onSurface = Chalk,
    onSurfaceVariant = Color(0xFF9F9C94),
    surfaceTint = Night,
    surfaceContainerLowest = Color(0xFF282725),
    surfaceContainerLow = Color(0xFF282725),
    surfaceContainer = Color(0xFF282725),
    surfaceContainerHigh = Color(0xFF282725),
    surfaceContainerHighest = Color(0xFF282725),
    outline = Color(0xFF4A4843),
    outlineVariant = Color(0xFF34332F),
    error = Color(0xFFE5806C),
)

// Headlines and card content are set in serif; everything else in the system sans.
private val Type = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Serif),
    )
}

private val Corners = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Corners,
        typography = Type,
        content = content,
    )
}
