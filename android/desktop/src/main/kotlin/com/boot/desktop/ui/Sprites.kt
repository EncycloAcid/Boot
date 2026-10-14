package com.boot.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boot.desktop.Tab

/** 16×16 pixel icons drawn as packed bitmaps — chrome sprites for the floating nav. */
@Composable
fun NavSprite(tab: Tab, tint: Color, selected: Boolean, size: Dp = 28.dp) {
    val pixels = when (tab) {
        Tab.OFFICE -> OFFICE
        Tab.LANES -> LANES
        Tab.RESEARCH -> RESEARCH
        Tab.TEAM -> TEAM
        Tab.COMPANY -> COMPANY
    }
    Canvas(Modifier.size(size)) {
        val cell = minOf(this.size.width, this.size.height) / 16f
        val ox = (this.size.width - cell * 16f) / 2f
        val oy = (this.size.height - cell * 16f) / 2f
        // Soft selected glow behind the glyph.
        if (selected) {
            drawRect(
                color = tint.copy(alpha = 0.18f),
                topLeft = Offset(ox - cell, oy - cell),
                size = Size(cell * 18f, cell * 18f),
            )
        }
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                if (pixels[y][x] == 1) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(ox + x * cell, oy + y * cell),
                        size = Size(cell + 0.4f, cell + 0.4f),
                    )
                }
            }
        }
    }
}

/** Tiny lane badge sprite (8×8). */
@Composable
fun LaneDot(color: Color, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val cell = minOf(this.size.width, this.size.height) / 8f
        val ox = (this.size.width - cell * 8f) / 2f
        val oy = (this.size.height - cell * 8f) / 2f
        for (y in 0 until 8) for (x in 0 until 8) {
            if (LANE_DOT[y][x] == 1) {
                drawRect(color, Offset(ox + x * cell, oy + y * cell), Size(cell + 0.3f, cell + 0.3f))
            }
        }
    }
}

/** Dithered scanline backdrop for the shell. */
fun DrawScope.drawScanBackdrop(ink: Color, raised: Color) {
    drawRect(ink)
    val step = 4f
    var y = 0f
    while (y < size.height) {
        drawRect(raised.copy(alpha = 0.22f), Offset(0f, y), Size(size.width, 1f))
        y += step
    }
}

// --- Bitmaps: 1 = on, 0 = off ---

private val OFFICE = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,1,0,1,1,0,0,1,1,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,1,1,0,0,1,1,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0,0,0,0,0,1,0,0,0),
    intArrayOf(0,0,0,1,1,1,1,1,1,1,1,1,1,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0,0,0,0,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,1,1,1,1,1,1,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,1,0,0,0,0,1,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,1,0,0,0,0,1,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,1,1,1,1,1,1,0,1,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0,0,0,0,0,1,0,0,0),
    intArrayOf(0,0,0,1,1,1,1,1,1,1,1,1,1,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
)

private val LANES = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,1,1,1,0,0,0,0,0,1,1,1,0,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,0,1,1,1,0,0,0,0,0,1,1,1,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,1,1,1,1,1,1,1,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,0,1,1,1,1,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,0,1,0,0,1,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,0,1,1,1,1,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,0,0,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,1,1,1,1,1,1,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
)

private val RESEARCH = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,1,0,0,1,0,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,0,1,1,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,1,1,1,1,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,0,0,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
)

private val TEAM = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,1,1,0,0,0,1,1,0,0,0,0,0),
    intArrayOf(0,0,0,1,0,0,1,0,1,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,1,0,0,1,0,1,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,1,0,0,0,1,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,1,1,1,0,0,0,0,0,1,1,1,0,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,0),
    intArrayOf(0,0,1,1,1,0,0,0,0,0,1,1,1,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
)

private val COMPANY = arrayOf(
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,1,0,0,1,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0),
    intArrayOf(0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,1,0,1,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,1,0,1,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,1,0,1,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,1,0,1,0,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0),
    intArrayOf(0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
    intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0),
)

private val LANE_DOT = arrayOf(
    intArrayOf(0,0,1,1,1,1,0,0),
    intArrayOf(0,1,1,1,1,1,1,0),
    intArrayOf(1,1,0,1,1,0,1,1),
    intArrayOf(1,1,1,1,1,1,1,1),
    intArrayOf(1,1,1,1,1,1,1,1),
    intArrayOf(1,1,0,1,1,0,1,1),
    intArrayOf(0,1,1,1,1,1,1,0),
    intArrayOf(0,0,1,1,1,1,0,0),
)
