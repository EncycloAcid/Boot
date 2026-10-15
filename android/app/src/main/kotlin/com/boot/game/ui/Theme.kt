package com.boot.game.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.boot.core.Lane
import com.boot.core.LogTone

/*
 * Boot's look is calm and airy: white cards on a soft off-white page (near-black in the dark),
 * plenty of space, sentence-case headings, one big number where it matters, and colour kept for
 * meaning: money up or down, each business, the thing to press. Material 3 Expressive supplies
 * the motion (springs everywhere), the lane badges that morph when picked and the wavy progress.
 */

private val Light = lightColorScheme(
    // Sky studio blue from the Windows paper HUD, not the older indigo.
    primary = Color(0xFF3F7FC4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EEF8),
    onPrimaryContainer = Color(0xFF163A5C),
    secondary = Color(0xFF3E7C76),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDEDFC),
    onSecondaryContainer = Color(0xFF27266B),
    tertiary = Color(0xFFB4593A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBEAE2),
    onTertiaryContainer = Color(0xFF4A1B0C),
    background = Color(0xFFECE7DE),
    onBackground = Color(0xFF22262C),
    surface = Color(0xFFF7F4EE),
    onSurface = Color(0xFF22262C),
    onSurfaceVariant = Color(0xFF6E6F76),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFAF8),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F0EE),
    surfaceContainerHighest = Color(0xFFE6E6E3),
    outline = Color(0xFF9A9BA1),
    outlineVariant = Color(0xFFE2E2DF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8EB7E8),
    onPrimary = Color(0xFF0E2A44),
    primaryContainer = Color(0xFF1C3A56),
    onPrimaryContainer = Color(0xFFE3EEF8),
    secondary = Color(0xFF8CC9C2),
    onSecondary = Color(0xFF0E3632),
    secondaryContainer = Color(0xFF2B2B5E),
    onSecondaryContainer = Color(0xFFE2E2FF),
    tertiary = Color(0xFFF2AE94),
    onTertiary = Color(0xFF4A1B0C),
    tertiaryContainer = Color(0xFF5A2A19),
    onTertiaryContainer = Color(0xFFFBEAE2),
    background = Color(0xFF111113),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF111113),
    onSurface = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFF9E9FA6),
    surfaceContainerLowest = Color(0xFF1B1B1E),
    surfaceContainerLow = Color(0xFF1E1E21),
    surfaceContainer = Color(0xFF1B1B1E),
    surfaceContainerHigh = Color(0xFF26262A),
    surfaceContainerHighest = Color(0xFF303035),
    outline = Color(0xFF7D7E85),
    outlineVariant = Color(0xFF2C2C30),
)

/** Colors with a meaning in the game, plus the page and card surfaces. */
@Immutable
data class GameColors(
    val good: Color,
    val bad: Color,
    val warn: Color,
    val milestone: Color,
    val lanes: Map<Lane, Color>,
    val other: Color,
    /** The page behind the cards. */
    val grouped: Color,
    /** A card. */
    val card: Color,
    /** Quiet fills inside a card: tracks, unselected chips, the segmented control. */
    val fill: Color,
    /** Hairlines between rows. */
    val separator: Color,
    /** Tab bar and floating chrome. */
    val bar: Color,
    /** Game development points, as they pop off the desks. */
    val design: Color,
    val tech: Color,
) {
    fun lane(lane: Lane): Color = lanes.getValue(lane)

    fun tone(tone: LogTone, neutral: Color): Color = when (tone) {
        LogTone.GOOD -> good
        LogTone.BAD -> bad
        LogTone.MILESTONE -> milestone
        LogTone.NEUTRAL -> neutral
    }
}

private val LightGame = GameColors(
    good = Color(0xFF1F8A4C),
    bad = Color(0xFFD93D42),
    warn = Color(0xFFC9650E),
    milestone = Color(0xFF4AA3D9),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFE5486F),
        Lane.WEBSITES to Color(0xFF3B7FE8),
        Lane.APPS to Color(0xFF2E9E62),
        Lane.SOCIAL to Color(0xFFE8920C),
        Lane.HARDWARE to Color(0xFF9B59E0),
        Lane.AI to Color(0xFF1F9BC9),
        Lane.OS to Color(0xFF5856D6),
    ),
    other = Color(0xFF9AA3AD),
    grouped = Color(0xFFECE7DE),
    card = Color(0xF7FFFFFF),
    fill = Color(0xFFF3F0EA),
    separator = Color(0xFFD9D3C8),
    bar = Color(0xF2FFFFFF),
    design = Color(0xFF3F7FC4),
    tech = Color(0xFFE8792A),
)

private val DarkGame = GameColors(
    good = Color(0xFF4CC38A),
    bad = Color(0xFFFF6369),
    warn = Color(0xFFF1A10D),
    milestone = Color(0xFF9E8CFC),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFFF6B8F),
        Lane.WEBSITES to Color(0xFF6AA2FF),
        Lane.APPS to Color(0xFF4CC38A),
        Lane.SOCIAL to Color(0xFFF5B13D),
        Lane.HARDWARE to Color(0xFFBF8CFF),
        Lane.AI to Color(0xFF5BC5F0),
        Lane.OS to Color(0xFF7D7AFF),
    ),
    other = Color(0xFF7D7E85),
    grouped = Color(0xFF111113),
    card = Color(0xFF1B1B1E),
    fill = Color(0xFF26262A),
    separator = Color(0xFF2A2A2E),
    bar = Color(0xFF1E1E21),
    design = Color(0xFF6AA2FF),
    tech = Color(0xFFFF9F5A),
)

val LocalGameColors = staticCompositionLocalOf { LightGame }

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)

/** A calm type ramp: generous line heights, semibold rather than black, big numbers kept bold. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val BootTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.sized(34, 42, FontWeight.SemiBold, -0.6),
        displayMedium = t.displayMedium.sized(44, 52, FontWeight.Bold, -1.2),
        headlineLarge = t.headlineLarge.sized(32, 40, FontWeight.SemiBold, -0.6),
        headlineMedium = t.headlineMedium.sized(26, 34, FontWeight.SemiBold, -0.4),
        headlineSmall = t.headlineSmall.sized(21, 28, FontWeight.SemiBold, -0.2),
        titleLarge = t.titleLarge.sized(19, 26, FontWeight.SemiBold, -0.2),
        titleMedium = t.titleMedium.sized(17, 23, FontWeight.SemiBold, -0.1),
        titleSmall = t.titleSmall.sized(15, 21, FontWeight.SemiBold, -0.1),
        bodyLarge = t.bodyLarge.sized(16, 23, FontWeight.Normal, -0.1),
        bodyMedium = t.bodyMedium.sized(15, 21, FontWeight.Normal, -0.1),
        bodySmall = t.bodySmall.sized(13, 19, FontWeight.Normal),
        labelLarge = t.labelLarge.sized(15, 20, FontWeight.SemiBold, -0.1),
        labelMedium = t.labelMedium.sized(13, 18, FontWeight.Medium),
        labelSmall = t.labelSmall.sized(11, 14, FontWeight.Medium, 0.2),
        displaySmallEmphasized = t.displaySmallEmphasized.sized(38, 44, FontWeight.Bold, -1.0),
        headlineLargeEmphasized = t.headlineLargeEmphasized.sized(32, 40, FontWeight.Bold, -0.8),
        headlineMediumEmphasized = t.headlineMediumEmphasized.sized(28, 34, FontWeight.Bold, -0.6),
        titleLargeEmphasized = t.titleLargeEmphasized.sized(22, 28, FontWeight.Bold, -0.4),
    )
}

private val BootShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BootTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalGameColors provides if (dark) DarkGame else LightGame) {
        MaterialExpressiveTheme(
            colorScheme = if (dark) Dark else Light,
            motionScheme = MotionScheme.expressive(),
            shapes = BootShapes,
            typography = BootTypography,
            content = content,
        )
    }
}

/** Shorthand for the game's semantic colors. */
val gameColors: GameColors
    @Composable get() = LocalGameColors.current

val Lane.emoji: String
    get() = when (this) {
        Lane.GAMES -> "🎮"
        Lane.WEBSITES -> "🌐"
        Lane.APPS -> "📱"
        Lane.SOCIAL -> "💬"
        Lane.HARDWARE -> "💻"
        Lane.AI -> "🤖"
        Lane.OS -> "🖥️"
    }

/** Each lane has its own Material expressive shape; badges morph into it when the lane is picked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Lane.polygon: RoundedPolygon
    get() = when (this) {
        Lane.GAMES -> MaterialShapes.Clover4Leaf
        Lane.WEBSITES -> MaterialShapes.Sunny
        Lane.APPS -> MaterialShapes.Cookie4Sided
        Lane.SOCIAL -> MaterialShapes.Puffy
        Lane.HARDWARE -> MaterialShapes.Cookie6Sided
        Lane.AI -> MaterialShapes.SoftBurst
        Lane.OS -> MaterialShapes.Cookie9Sided
    }

/** The short tab label for a lane. */
val Lane.short: String
    get() = when (this) {
        Lane.SOCIAL -> "Social"
        Lane.OS -> "OS"
        else -> title
    }
