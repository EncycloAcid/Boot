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
 * Boot desktop shell — cool studio paper chrome over the isometric office.
 * Lane / design / tech colors stay aligned with Android light [GameColors].
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

/** Floating HUD tokens — matte studio sheet, not iOS glass. */
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
    /** @deprecated use [panel] — kept so older call sites compile during the redesign. */
    val glass: Color,
    val glassBorder: Color,
    val text: Color,
    val textDim: Color,
)

val LocalGameColors = staticCompositionLocalOf { LightGame }
val LocalHudColors = staticCompositionLocalOf { LightHud }

val LightGame = GameColors(
    good = Color(0xFF1F8A4C),
    bad = Color(0xFFD93D42),
    warn = Color(0xFFC9650E),
    milestone = Color(0xFF0B6E6A),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFE5486F),
        Lane.WEBSITES to Color(0xFF3B7FE8),
        Lane.APPS to Color(0xFF2E9E62),
        Lane.SOCIAL to Color(0xFFE8920C),
        Lane.HARDWARE to Color(0xFF9B59E0),
        Lane.AI to Color(0xFF1F9BC9),
        Lane.OS to Color(0xFF3D5A80),
    ),
    other = Color(0xFF8B93A0),
    grouped = Color(0xFFE7EEF3),
    card = Color(0xFFF7FAFC),
    fill = Color(0xFFEDF2F6),
    separator = Color(0xFFCBD5E1),
    bar = Color(0xF2F7FAFC),
    design = Color(0xFF3B7FE8),
    tech = Color(0xFFE8792A),
)

val LightHud = HudColors(
    world = Color(0xFFE7EEF3),
    worldDeep = Color(0xFFD5E0E8),
    panel = Color(0xF5F7FAFC),
    panelBorder = Color(0xFFB8C4CE),
    ink = Color(0xFF15202B),
    inkMute = Color(0xFF5B6B7C),
    accent = Color(0xFF0B6E6A),
    accentSoft = Color(0xFFD5EDEA),
    cash = Color(0xFF15803D),
    fans = Color(0xFFBE123C),
    research = Color(0xFF1D6FA5),
    bugs = Color(0xFFDC2626),
    designBubble = Color(0xFFE67E22),
    techBubble = Color(0xFFCA8A04),
    hype = Color(0xFF5C6B3A),
    shadow = Color(0x1A15202B),
    glass = Color(0xF5F7FAFC),
    glassBorder = Color(0xFFB8C4CE),
    text = Color(0xFF15202B),
    textDim = Color(0xFF5B6B7C),
)

val gameColors: GameColors
    @Composable get() = LocalGameColors.current

val hudColors: HudColors
    @Composable get() = LocalHudColors.current

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B6E6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EDEA),
    onPrimaryContainer = Color(0xFF063E3C),
    secondary = Color(0xFF15803D),
    onSecondary = Color.White,
    background = Color(0xFFE7EEF3),
    onBackground = Color(0xFF15202B),
    surface = Color(0xFFF7FAFC),
    onSurface = Color(0xFF15202B),
    onSurfaceVariant = Color(0xFF5B6B7C),
    outline = Color(0xFFB8C4CE),
)

/**
 * Serif for brand / titles, geometric sans for dense HUD chrome.
 * Avoids the default Inter/system look while staying readable on desktop.
 */
private val BootType = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
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
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun BootDesktopTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGameColors provides LightGame,
        LocalHudColors provides LightHud,
        LocalIndication provides ripple(color = LightHud.accent.copy(alpha = 0.22f)),
    ) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = BootType,
            content = content,
        )
    }
}
