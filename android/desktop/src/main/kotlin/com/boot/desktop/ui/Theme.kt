package com.boot.desktop.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
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
 * Boot desktop shell — GDT soft-float chrome over the isometric office.
 * Layout stays Menu · discs/card · status · dock · floating windows.
 */
@Immutable
data class GameColors(
    val good: Color,
    val bad: Color,
    val warn: Color,
    val milestone: Color,
    val lanes: Map<Lane, Color>,
    val other: Color,
    val grouped: Color,
    val card: Color,
    val fill: Color,
    val separator: Color,
    val bar: Color,
    val design: Color,
    val tech: Color,
) {
    fun lane(lane: Lane): Color = lanes.getValue(lane)
}

/** Soft white floating HUD tokens (Game Dev Tycoon reference). */
@Immutable
data class HudColors(
    val world: Color,
    val worldDeep: Color,
    val panel: Color,
    val panelBorder: Color,
    val ink: Color,
    val inkMute: Color,
    val accent: Color,
    val accentSoft: Color,
    val cash: Color,
    val fans: Color,
    val research: Color,
    val bugs: Color,
    val designBubble: Color,
    val techBubble: Color,
    val hype: Color,
    val shadow: Color,
    val glass: Color,
    val glassBorder: Color,
    val text: Color,
    val textDim: Color,
)

val LocalGameColors = staticCompositionLocalOf { LightGame }
val LocalHudColors = staticCompositionLocalOf { LightHud }

val LightGame = GameColors(
    good = Color(0xFF2F9E5B),
    bad = Color(0xFFE0454A),
    warn = Color(0xFFD07A12),
    milestone = Color(0xFF3F7FC4),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFE5486F),
        Lane.WEBSITES to Color(0xFF3B7FE8),
        Lane.APPS to Color(0xFF2E9E62),
        Lane.SOCIAL to Color(0xFFE8920C),
        Lane.HARDWARE to Color(0xFF9B59E0),
        Lane.AI to Color(0xFF1F9BC9),
        Lane.OS to Color(0xFF4A6FA5),
    ),
    other = Color(0xFF9AA3AD),
    grouped = Color(0xFFEDE8DF),
    card = Color(0xF7FFFFFF),
    fill = Color(0xFFF3F0EA),
    separator = Color(0xFFD9D3C8),
    bar = Color(0xF2FFFFFF),
    design = Color(0xFF3F7FC4),
    tech = Color(0xFFE8792A),
)

val LightHud = HudColors(
    // Soft sand wash for new-game / empty chrome — not warm cream + terracotta.
    world = Color(0xFFECE7DE),
    worldDeep = Color(0xFFDDD6CA),
    panel = Color(0xF2FFFFFF),
    panelBorder = Color(0x66FFFFFF),
    ink = Color(0xFF22262C),
    inkMute = Color(0xFF6B7280),
    // Sky studio blue (not iOS #007AFF).
    accent = Color(0xFF3F7FC4),
    accentSoft = Color(0xFFE3EEF8),
    cash = Color(0xFF2F9E5B),
    fans = Color(0xFFC44D8A),
    research = Color(0xFF4AA3D9),
    bugs = Color(0xFFE0454A),
    designBubble = Color(0xFFF08A24),
    techBubble = Color(0xFFE8B923),
    hype = Color(0xFF6B7B3A),
    shadow = Color(0x28000000),
    glass = Color(0xF2FFFFFF),
    glassBorder = Color(0x66FFFFFF),
    text = Color(0xFF22262C),
    textDim = Color(0xFF6B7280),
)

val gameColors: GameColors
    @Composable get() = LocalGameColors.current

val hudColors: HudColors
    @Composable get() = LocalHudColors.current

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F7FC4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EEF8),
    onPrimaryContainer = Color(0xFF163A5C),
    secondary = Color(0xFF2F9E5B),
    onSecondary = Color.White,
    background = Color(0xFFECE7DE),
    onBackground = Color(0xFF22262C),
    surface = Color(0xFFF7FAFC),
    onSurface = Color(0xFF22262C),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD0CBC2),
)

/** Clean HUD sans; serif reserved for the Boot wordmark on New Game. */
private val BootType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.15.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun BootDesktopTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGameColors provides LightGame,
        LocalHudColors provides LightHud,
        LocalIndication provides ripple(color = LightHud.accent.copy(alpha = 0.2f)),
    ) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = BootType,
            content = content,
        )
    }
}
