package com.boot.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Office
import kotlin.math.min

/**
 * Isometric office diorama adapted from Android's OfficeScene spirit:
 * desks, monitors with lane glow, founder + staff silhouettes — sprite/pixel chrome, not Material cards.
 */
@Composable
fun OfficeDiorama(state: GameState, modifier: Modifier = Modifier) {
    val c = spriteColors
    val lanes = remember(state.lanes, state.staff.size, state.office) {
        state.lanes.toList()
    }
    Canvas(modifier.fillMaxSize()) {
        drawScanBackdrop(c.ink, c.inkRaised)
        // Soft window glow.
        drawRect(
            Brush.radialGradient(
                colors = listOf(c.glow.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.72f, size.height * 0.28f),
                radius = size.minDimension * 0.55f,
            ),
        )
        val iso = Iso(
            ox = size.width * 0.48f,
            oy = size.height * 0.28f,
            t = min(size.width, size.height) * 0.055f,
        )
        drawRoom(iso, state.office, c)
        val seats = seatCount(state.office)
        val people = buildPeople(state, seats)
        people.forEachIndexed { i, person ->
            val col = i % colsFor(state.office)
            val row = i / colsFor(state.office)
            val cx = 1.2f + col * 1.15f
            val cy = 1.1f + row * 1.35f
            drawDesk(iso, cx, cy, c, person.lane?.let { c.lane(it) } ?: c.glow, person.founder)
            drawPerson(iso, cx, cy, c, person.founder)
        }
        // Floor label plaque.
        drawRoundRect(
            c.panel.copy(alpha = 0.85f),
            topLeft = Offset(24f, size.height - 72f),
            size = Size(280f, 40f),
            cornerRadius = CornerRadius(4f, 4f),
        )
    }
}

private data class Person(val founder: Boolean, val lane: Lane?)

private fun buildPeople(state: GameState, seats: Int): List<Person> {
    val list = mutableListOf(Person(true, state.lanes.firstOrNull()))
    for (e in state.staff.take(seats - 1)) {
        list += Person(false, e.lane)
    }
    return list
}

private fun seatCount(office: Office) = when (office) {
    Office.APARTMENT -> 3
    Office.COWORKING -> 6
    Office.STUDIO -> 8
    Office.DOWNTOWN -> 10
    Office.CAMPUS -> 12
}

private fun colsFor(office: Office) = when (office) {
    Office.APARTMENT -> 2
    else -> 3
}

private class Iso(val ox: Float, val oy: Float, val t: Float) {
    val zpx = t * 1.1f
    fun p(x: Float, y: Float, z: Float = 0f) =
        Offset(ox + (x - y) * t, oy + (x + y) * t * 0.5f - z * zpx)
}

private fun DrawScope.drawRoom(iso: Iso, office: Office, c: SpriteColors) {
    val w = when (office) {
        Office.APARTMENT -> 4.2f
        Office.COWORKING -> 5.2f
        else -> 6.4f
    }
    val d = w * 0.85f
    val h = 2.4f
    val floor = Path().apply {
        moveTo(iso.p(0f, 0f).x, iso.p(0f, 0f).y)
        lineTo(iso.p(w, 0f).x, iso.p(w, 0f).y)
        lineTo(iso.p(w, d).x, iso.p(w, d).y)
        lineTo(iso.p(0f, d).x, iso.p(0f, d).y)
        close()
    }
    drawPath(floor, c.floor)
    drawPath(floor, c.panelEdge, style = Stroke(width = 2f))

    val leftWall = Path().apply {
        val a = iso.p(0f, 0f)
        val b = iso.p(0f, d)
        val c1 = iso.p(0f, d, h)
        val d1 = iso.p(0f, 0f, h)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c1.x, c1.y); lineTo(d1.x, d1.y); close()
    }
    drawPath(leftWall, c.wall)
    val backWall = Path().apply {
        val a = iso.p(0f, 0f)
        val b = iso.p(w, 0f)
        val c1 = iso.p(w, 0f, h)
        val d1 = iso.p(0f, 0f, h)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c1.x, c1.y); lineTo(d1.x, d1.y); close()
    }
    drawPath(backWall, c.wall.copy(alpha = 0.92f))
    // Window on back wall.
    val win = Path().apply {
        val a = iso.p(w * 0.35f, 0.05f, h * 0.35f)
        val b = iso.p(w * 0.75f, 0.05f, h * 0.35f)
        val c1 = iso.p(w * 0.75f, 0.05f, h * 0.85f)
        val d1 = iso.p(w * 0.35f, 0.05f, h * 0.85f)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c1.x, c1.y); lineTo(d1.x, d1.y); close()
    }
    drawPath(win, c.glow.copy(alpha = 0.25f))
    drawPath(win, c.glow.copy(alpha = 0.5f), style = Stroke(2f))
}

private fun DrawScope.drawDesk(
    iso: Iso,
    cx: Float,
    cy: Float,
    c: SpriteColors,
    screen: Color,
    founder: Boolean,
) {
    val top = Path().apply {
        val a = iso.p(cx - 0.4f, cy - 0.25f, 0.55f)
        val b = iso.p(cx + 0.4f, cy - 0.25f, 0.55f)
        val c1 = iso.p(cx + 0.4f, cy + 0.25f, 0.55f)
        val d1 = iso.p(cx - 0.4f, cy + 0.25f, 0.55f)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c1.x, c1.y); lineTo(d1.x, d1.y); close()
    }
    drawPath(top, c.desk)
    drawPath(top, c.amberDim.copy(alpha = 0.45f), style = Stroke(1.5f))
    // Monitor
    val mon = Path().apply {
        val a = iso.p(cx - 0.22f, cy - 0.18f, 0.7f)
        val b = iso.p(cx + 0.22f, cy - 0.18f, 0.7f)
        val c1 = iso.p(cx + 0.22f, cy - 0.18f, 1.05f)
        val d1 = iso.p(cx - 0.22f, cy - 0.18f, 1.05f)
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c1.x, c1.y); lineTo(d1.x, d1.y); close()
    }
    drawPath(mon, c.monitor)
    drawPath(mon, screen.copy(alpha = if (founder) 0.95f else 0.75f))
}

private fun DrawScope.drawPerson(iso: Iso, cx: Float, cy: Float, c: SpriteColors, founder: Boolean) {
    val head = iso.p(cx, cy + 0.35f, 1.15f)
    val r = iso.t * 0.22f
    drawCircle(if (founder) c.amber else c.mint, r, head)
    drawCircle(c.ink, r, head, style = Stroke(1.5f))
    val bodyTop = iso.p(cx, cy + 0.35f, 0.95f)
    val bodyBot = iso.p(cx, cy + 0.35f, 0.55f)
    drawLine(c.textDim, bodyTop, bodyBot, strokeWidth = 3f)
}
