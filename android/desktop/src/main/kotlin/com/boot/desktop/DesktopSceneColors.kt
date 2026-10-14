package com.boot.desktop

import androidx.compose.ui.graphics.Color
import com.boot.core.Lane

/** The semantic tints used by the Android office scene and portraits. */
internal data class DesktopSceneColors(
    val good: Color,
    val bad: Color,
    val design: Color,
    val tech: Color,
    private val lanes: Map<Lane, Color>,
) {
    fun lane(lane: Lane): Color = lanes[lane] ?: Color(0xFF9AA0A6)
}

internal val gameColors = DesktopSceneColors(
    good = Color(0xFF4CC38A),
    bad = Color(0xFFFF6369),
    design = Color(0xFF6AA2FF),
    tech = Color(0xFFFF9F5A),
    lanes = mapOf(
        Lane.GAMES to Color(0xFFFF6B8F),
        Lane.WEBSITES to Color(0xFF6AA2FF),
        Lane.APPS to Color(0xFF4CC38A),
        Lane.SOCIAL to Color(0xFFF5B13D),
        Lane.HARDWARE to Color(0xFFBF8CFF),
        Lane.AI to Color(0xFF5BC5F0),
        Lane.OS to Color(0xFF7D7AFF),
    ),
)
