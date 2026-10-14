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
 * Game Dev Tycoon / macOS-light shell: cream world, white translucent floating chrome.
 * Lane / design / tech colors mirror Android's light [GameColors] so office sprites match.
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

/** macOS-ish floating HUD tokens. */
@Immutable
data class HudColors(
    val glass: Color,
    val glassBorder: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val cash: Color,
    val fans: Color,
    val research: Color,
    val bugs: Color,
    val designBubble: Color,
    val techBubble: Color,
    val shadow: Color,
)

val LocalGameColors = staticCompositionLocalOf { LightGame }
val LocalHudColors = staticCompositionLocalOf { LightHud }

val LightGame = GameColors(
    good = Color(0xFF1F8A4C),
    bad = Color(0xFFD93D42),
    warn = Color(0xFFC9650E),
    milestone = Color(0xFF6E56CF),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFE5486F),
        Lane.WEBSITES to Color(0xFF3B7FE8),
        Lane.APPS to Color(0xFF2E9E62),
        Lane.SOCIAL to Color(0xFFE8920C),
        Lane.HARDWARE to Color(0xFF9B59E0),
        Lane.AI to Color(0xFF1F9BC9),
        Lane.OS to Color(0xFF5856D6),
    ),
    other = Color(0xFF9A9BA1),
    grouped = Color(0xFFF4F1EA),
    card = Color.White,
    fill = Color(0xFFF1F1EE),
    separator = Color(0xFFE5E2DB),
    bar = Color(0xF2FFFFFF),
    design = Color(0xFF3B7FE8),
    tech = Color(0xFFE8792A),
)

val LightHud = HudColors(
    glass = Color(0xE6FFFFFF),
    glassBorder = Color(0x33FFFFFF),
    text = Color(0xFF1C1C1E),
    textDim = Color(0xFF6C6C70),
    accent = Color(0xFF007AFF),
    cash = Color(0xFF34C759),
    fans = Color(0xFFAF52DE),
    research = Color(0xFF5AC8FA),
    bugs = Color(0xFFFF3B30),
    designBubble = Color(0xFFFF9F0A),
    techBubble = Color(0xFFFFD60A),
    shadow = Color(0x33000000),
)

val gameColors: GameColors
    @Composable get() = LocalGameColors.current

val hudColors: HudColors
    @Composable get() = LocalHudColors.current

private val LightScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003A75),
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFD1D1D6),
)

/** Clean sans — GDT/macOS chrome, not pixel monospace. */
private val MacType = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun BootDesktopTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGameColors provides LightGame,
        LocalHudColors provides LightHud,
        LocalIndication provides ripple(color = LightHud.accent.copy(alpha = 0.25f)),
    ) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = MacType,
            content = content,
        )
    }
}
