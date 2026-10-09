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
 * Boot's look is iOS structure with Material 3 Expressive character:
 * - iOS: grouped backgrounds, inset grouped lists with hairline separators, large titles,
 *   the iOS system color palette, sheets for decisions, a tab bar.
 * - Expressive: the expressive motion scheme (springs everywhere), shape-morphing buttons,
 *   MaterialShapes lane badges that morph when selected, connected button groups, wavy progress,
 *   a floating toolbar for time, emphasized type for the numbers that matter.
 */

private val Light = lightColorScheme(
    primary = Color(0xFF4B3FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF160E6E),
    secondary = Color(0xFF00696E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC6EEEF),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFFB4452A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD0),
    onTertiaryContainer = Color(0xFF3B0900),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF9F9FB),
    surfaceContainerHigh = Color(0xFFE9E9EF),
    surfaceContainerHighest = Color(0xFFDEDEE4),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC6C0FF),
    onPrimary = Color(0xFF231596),
    primaryContainer = Color(0xFF3D31C9),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFF7FD6DB),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF004F53),
    onSecondaryContainer = Color(0xFFC6EEEF),
    tertiary = Color(0xFFFFB4A0),
    onTertiary = Color(0xFF5F1500),
    tertiaryContainer = Color(0xFF8A2D14),
    onTertiaryContainer = Color(0xFFFFDAD0),
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF000000),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceContainerLowest = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF232325),
    surfaceContainer = Color(0xFF161618),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF38383A),
)

/** Colors with a meaning in the game, from the iOS system palette, plus the grouped-list surfaces. */
@Immutable
data class GameColors(
    val good: Color,
    val bad: Color,
    val warn: Color,
    val milestone: Color,
    val lanes: Map<Lane, Color>,
    val other: Color,
    /** The page behind grouped lists. */
    val grouped: Color,
    /** A grouped list's card. */
    val card: Color,
    /** Quiet fills inside a card: tiles, tracks, unselected chips. */
    val fill: Color,
    /** Hairlines between rows. */
    val separator: Color,
    /** Tab bar and floating chrome. */
    val bar: Color,
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
    good = Color(0xFF248A3D),
    bad = Color(0xFFD70015),
    warn = Color(0xFFC93400),
    milestone = Color(0xFF5856D6),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFFF2D55),
        Lane.WEBSITES to Color(0xFF007AFF),
        Lane.APPS to Color(0xFF34C759),
        Lane.SOCIAL to Color(0xFFFF9500),
        Lane.HARDWARE to Color(0xFFAF52DE),
        Lane.AI to Color(0xFF32ADE6),
    ),
    other = Color(0xFF8E8E93),
    grouped = Color(0xFFF2F2F7),
    card = Color.White,
    fill = Color(0xFFF2F2F7),
    separator = Color(0xFFC6C6C8),
    bar = Color(0xF7F9F9FB),
)

private val DarkGame = GameColors(
    good = Color(0xFF30D158),
    bad = Color(0xFFFF453A),
    warn = Color(0xFFFF9F0A),
    milestone = Color(0xFF7D7AFF),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFFF375F),
        Lane.WEBSITES to Color(0xFF0A84FF),
        Lane.APPS to Color(0xFF30D158),
        Lane.SOCIAL to Color(0xFFFF9F0A),
        Lane.HARDWARE to Color(0xFFBF5AF2),
        Lane.AI to Color(0xFF64D2FF),
    ),
    other = Color(0xFF8E8E93),
    grouped = Color.Black,
    card = Color(0xFF1C1C1E),
    fill = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    bar = Color(0xF2161618),
)

val LocalGameColors = staticCompositionLocalOf { LightGame }

private fun TextStyle.ios(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)

/** The iOS type ramp (large title 34, headline 17, body 17, footnote 13) mapped onto Material roles. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val BootTypography = Typography().let { t ->
    t.copy(
        // iOS large title. Material's flexible large top bar sets its expanded title in displaySmall.
        displaySmall = t.displaySmall.ios(34, 41, FontWeight.Bold, -0.6),
        displayMedium = t.displayMedium.ios(44, 50, FontWeight.Black, -1.4),
        headlineLarge = t.headlineLarge.ios(34, 41, FontWeight.Bold, -0.6),
        headlineMedium = t.headlineMedium.ios(28, 34, FontWeight.Bold, -0.4),
        headlineSmall = t.headlineSmall.ios(22, 28, FontWeight.Bold, -0.3),
        titleLarge = t.titleLarge.ios(20, 25, FontWeight.SemiBold, -0.2),
        titleMedium = t.titleMedium.ios(17, 22, FontWeight.SemiBold, -0.2),
        titleSmall = t.titleSmall.ios(15, 20, FontWeight.SemiBold, -0.1),
        bodyLarge = t.bodyLarge.ios(17, 22, FontWeight.Normal, -0.2),
        bodyMedium = t.bodyMedium.ios(15, 20, FontWeight.Normal, -0.1),
        bodySmall = t.bodySmall.ios(13, 18, FontWeight.Normal),
        labelLarge = t.labelLarge.ios(15, 20, FontWeight.SemiBold, -0.1),
        labelMedium = t.labelMedium.ios(13, 18, FontWeight.Medium),
        labelSmall = t.labelSmall.ios(11, 13, FontWeight.Medium, 0.1),
        displaySmallEmphasized = t.displaySmallEmphasized.ios(40, 46, FontWeight.Black, -1.2),
        headlineLargeEmphasized = t.headlineLargeEmphasized.ios(34, 40, FontWeight.ExtraBold, -0.8),
        headlineMediumEmphasized = t.headlineMediumEmphasized.ios(28, 34, FontWeight.ExtraBold, -0.6),
        titleLargeEmphasized = t.titleLargeEmphasized.ios(22, 28, FontWeight.ExtraBold, -0.4),
    )
}

private val BootShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
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
    }

/** The short tab label for a lane. */
val Lane.short: String
    get() = when (this) {
        Lane.SOCIAL -> "Social"
        else -> title
    }
