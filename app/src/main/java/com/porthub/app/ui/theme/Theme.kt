package com.porthub.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Dark-first palette: near-black canvas, glassy white layers, one cool accent.
 * Everything is desaturated so the game art does the talking.
 */
object Ink {
    val Background = Color(0xFF050508)
    val BackgroundLift = Color(0xFF0B0B12)

    val TextPrimary = Color(0xFFF6F6F8)
    val TextSecondary = Color(0x99F6F6F8)
    val TextTertiary = Color(0x5CF6F6F8)

    val Accent = Color(0xFF9AA7FF)
    val AccentPink = Color(0xFFFF6B9A)
    val AccentTeal = Color(0xFF4ADEDE)

    val GlassFillTop = Color(0x1AFFFFFF)
    val GlassFillBottom = Color(0x0AFFFFFF)
    val GlassBorderTop = Color(0x3DFFFFFF)
    val GlassBorderBottom = Color(0x0FFFFFFF)

    val Positive = Color(0xFF57D9A3)
    val Warning = Color(0xFFFFC061)
}

object AppText {
    val LargeTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.1).sp,
        color = Ink.TextPrimary,
    )
    val ScreenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.5).sp,
        color = Ink.TextPrimary,
    )
    val CardTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
        color = Ink.TextPrimary,
    )
    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
        color = Ink.TextSecondary,
    )
    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = Ink.TextSecondary,
    )
    val Eyebrow = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
        color = Ink.TextTertiary,
    )
}

private val Scheme = darkColorScheme(
    primary = Ink.Accent,
    onPrimary = Color(0xFF0A0A12),
    background = Ink.Background,
    onBackground = Ink.TextPrimary,
    surface = Ink.BackgroundLift,
    onSurface = Ink.TextPrimary,
    secondary = Ink.AccentPink,
    error = Color(0xFFFF6E6E),
)

@Composable
fun PortHubTheme(content: @Composable () -> Unit) {
    // Always dark: the whole identity depends on it.
    @Suppress("UNUSED_VARIABLE")
    val systemDark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = Scheme, content = content)
}
