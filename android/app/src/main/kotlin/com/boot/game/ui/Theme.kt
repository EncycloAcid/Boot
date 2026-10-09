package com.boot.game.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.boot.core.Lane
import com.boot.core.LogTone

private val Light = lightColorScheme(
    primary = Color(0xFF3B2FC9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2DFFF),
    onPrimaryContainer = Color(0xFF130A6B),
    secondary = Color(0xFF00696E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9ECEE),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF8A5100),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF8F7FC),
    surface = Color(0xFFF8F7FC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F1F8),
    surfaceContainer = Color(0xFFECEBF3),
    surfaceContainerHigh = Color(0xFFE6E5EE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC3C0FF),
    onPrimary = Color(0xFF22139C),
    primaryContainer = Color(0xFF3B2FC9),
    onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFF80D4D9),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF004F53),
    onSecondaryContainer = Color(0xFFB9ECEE),
    tertiary = Color(0xFFFFB870),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF131218),
    surface = Color(0xFF131218),
    surfaceContainerLowest = Color(0xFF0E0D13),
    surfaceContainerLow = Color(0xFF1B1A21),
    surfaceContainer = Color(0xFF1F1E25),
    surfaceContainerHigh = Color(0xFF2A2930),
)

/** Colors with a meaning in the game: good news, bad news, milestones, and one hue per lane. */
@Immutable
data class GameColors(
    val good: Color,
    val bad: Color,
    val warn: Color,
    val milestone: Color,
    val lanes: Map<Lane, Color>,
    val other: Color,
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
    good = Color(0xFF1B8A3E),
    bad = Color(0xFFC62828),
    warn = Color(0xFFD9730D),
    milestone = Color(0xFF7B3FD1),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFE11D48),
        Lane.WEBSITES to Color(0xFF2563EB),
        Lane.APPS to Color(0xFF16A34A),
        Lane.SOCIAL to Color(0xFFF59E0B),
        Lane.HARDWARE to Color(0xFF8B5CF6),
        Lane.AI to Color(0xFF0D9488),
    ),
    other = Color(0xFF94A3B8),
)

private val DarkGame = GameColors(
    good = Color(0xFF6DD58C),
    bad = Color(0xFFFF8A80),
    warn = Color(0xFFFFB74D),
    milestone = Color(0xFFCDA6FF),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFFB7185),
        Lane.WEBSITES to Color(0xFF60A5FA),
        Lane.APPS to Color(0xFF4ADE80),
        Lane.SOCIAL to Color(0xFFFBBF24),
        Lane.HARDWARE to Color(0xFFA78BFA),
        Lane.AI to Color(0xFF2DD4BF),
    ),
    other = Color(0xFF64748B),
)

val LocalGameColors = staticCompositionLocalOf { LightGame }

private val BootTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun BootTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalGameColors provides if (dark) DarkGame else LightGame) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = BootTypography, content = content)
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
