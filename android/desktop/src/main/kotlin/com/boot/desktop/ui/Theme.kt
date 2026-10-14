package com.boot.desktop.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.boot.core.Lane

/**
 * Sprite-shell palette: late-shift office teal ink, warm amber chrome, CRT-ish greens.
 * Avoids Material phone purple and cream/serif defaults.
 */
@Immutable
data class SpriteColors(
    val ink: Color,
    val inkRaised: Color,
    val panel: Color,
    val panelEdge: Color,
    val floor: Color,
    val wall: Color,
    val desk: Color,
    val monitor: Color,
    val glow: Color,
    val amber: Color,
    val amberDim: Color,
    val mint: Color,
    val danger: Color,
    val mute: Color,
    val text: Color,
    val textDim: Color,
    val navPill: Color,
    val navSelected: Color,
    val lanes: Map<Lane, Color>,
) {
    fun lane(lane: Lane): Color = lanes.getValue(lane)
}

val LocalSpriteColors = staticCompositionLocalOf { SpritePalette }

val SpritePalette = SpriteColors(
    ink = Color(0xFF0A1618),
    inkRaised = Color(0xFF122226),
    panel = Color(0xFF1A2E32),
    panelEdge = Color(0xFF2A454B),
    floor = Color(0xFF1E3330),
    wall = Color(0xFF243C42),
    desk = Color(0xFF3A2A1C),
    monitor = Color(0xFF0D1F18),
    glow = Color(0xFF5EE0A0),
    amber = Color(0xFFE8A84A),
    amberDim = Color(0xFFB8792E),
    mint = Color(0xFF6FCFB2),
    danger = Color(0xFFE06B6B),
    mute = Color(0xFF6A858A),
    text = Color(0xFFE8F0EE),
    textDim = Color(0xFF9BB0B4),
    navPill = Color(0xE6122226),
    navSelected = Color(0xFF2A4A3A),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFE07A8A),
        Lane.WEBSITES to Color(0xFF5B9BD8),
        Lane.APPS to Color(0xFF5CB88A),
        Lane.SOCIAL to Color(0xFFE0A84A),
        Lane.HARDWARE to Color(0xFFA07AD0),
        Lane.AI to Color(0xFF4AB8D0),
        Lane.OS to Color(0xFF7A8AD0),
    ),
)

val spriteColors: SpriteColors
    @Composable get() = LocalSpriteColors.current

private val DesktopDark = darkColorScheme(
    primary = SpritePalette.amber,
    onPrimary = SpritePalette.ink,
    primaryContainer = SpritePalette.navSelected,
    onPrimaryContainer = SpritePalette.text,
    secondary = SpritePalette.mint,
    onSecondary = SpritePalette.ink,
    background = SpritePalette.ink,
    onBackground = SpritePalette.text,
    surface = SpritePalette.panel,
    onSurface = SpritePalette.text,
    onSurfaceVariant = SpritePalette.textDim,
    outline = SpritePalette.panelEdge,
)

private val PixelType = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

@Composable
fun BootDesktopTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpriteColors provides SpritePalette,
        LocalIndication provides ripple(color = SpritePalette.amber.copy(alpha = 0.35f)),
    ) {
        MaterialTheme(
            colorScheme = DesktopDark,
            typography = PixelType,
            content = content,
        )
    }
}
