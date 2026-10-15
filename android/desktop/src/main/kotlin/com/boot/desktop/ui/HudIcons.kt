package com.boot.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Soft-float HUD glyphs — solid single-color silhouettes that stay readable when
 * tinted white on a colored disc (no white-on-white interior detail).
 */
enum class HudGlyph {
    DESIGN,
    TECH,
    RESEARCH,
    BUGS,
    OFFICE,
    LANES,
    TEAM,
    COMPANY,
    CALENDAR,
    FANS,
    CASH,
    MENU,
}

@Composable
fun HudIcon(
    glyph: HudGlyph,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        drawHudGlyph(glyph, tint)
    }
}

fun DrawScope.drawHudGlyph(glyph: HudGlyph, tint: Color) {
    val s = size.minDimension
    if (s <= 0f) return
    val c = Offset(size.width / 2f, size.height / 2f)
    when (glyph) {
        HudGlyph.DESIGN -> designBrush(c, s, tint)
        HudGlyph.TECH -> techChip(c, s, tint)
        HudGlyph.RESEARCH -> researchAtom(c, s, tint)
        HudGlyph.BUGS -> bugBeetle(c, s, tint)
        HudGlyph.OFFICE -> officeDesk(c, s, tint)
        HudGlyph.LANES -> lanePaths(c, s, tint)
        HudGlyph.TEAM -> teamPeople(c, s, tint)
        HudGlyph.COMPANY -> companyMark(c, s, tint)
        HudGlyph.CALENDAR -> calendarMark(c, s, tint)
        HudGlyph.FANS -> heartMark(c, s, tint)
        HudGlyph.CASH -> cashMark(c, s, tint)
        HudGlyph.MENU -> menuBars(c, s, tint)
    }
}

private fun DrawScope.designBrush(c: Offset, s: Float, tint: Color) {
    // Angled brush: thick handle + flared tip — Art/Design at a glance.
    val path = Path().apply {
        // Handle.
        moveTo(c.x - s * 0.28f, c.y + s * 0.32f)
        lineTo(c.x - s * 0.18f, c.y + s * 0.38f)
        lineTo(c.x + s * 0.22f, c.y - s * 0.08f)
        lineTo(c.x + s * 0.12f, c.y - s * 0.14f)
        close()
    }
    drawPath(path, tint)
    // Metal ferrule.
    drawRoundRect(tint, Offset(c.x + s * 0.1f, c.y - s * 0.2f), Size(s * 0.16f, s * 0.12f), CornerRadius(s * 0.02f))
    // Bristle tip fan.
    val tip = Path().apply {
        moveTo(c.x + s * 0.12f, c.y - s * 0.2f)
        lineTo(c.x + s * 0.26f, c.y - s * 0.2f)
        lineTo(c.x + s * 0.38f, c.y - s * 0.42f)
        lineTo(c.x + s * 0.08f, c.y - s * 0.38f)
        close()
    }
    drawPath(tip, tint)
    // Tiny paint drop.
    drawCircle(tint, s * 0.07f, Offset(c.x - s * 0.28f, c.y - s * 0.22f))
}

private fun DrawScope.techChip(c: Offset, s: Float, tint: Color) {
    val body = s * 0.4f
    // Body with center cutout so it reads as a chip, not a square.
    val chip = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = c.x - body / 2,
                top = c.y - body / 2,
                right = c.x + body / 2,
                bottom = c.y + body / 2,
                cornerRadius = CornerRadius(s * 0.07f),
            ),
        )
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = c.x - body * 0.22f,
                top = c.y - body * 0.22f,
                right = c.x + body * 0.22f,
                bottom = c.y + body * 0.22f,
                cornerRadius = CornerRadius(s * 0.03f),
            ),
        )
    }
    drawPath(chip, tint)
    for (i in -1..1) {
        val y = c.y + i * body * 0.28f
        drawLine(tint, Offset(c.x - body * 0.72f, y), Offset(c.x - body * 0.52f, y), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        drawLine(tint, Offset(c.x + body * 0.52f, y), Offset(c.x + body * 0.72f, y), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        val x = c.x + i * body * 0.28f
        drawLine(tint, Offset(x, c.y - body * 0.72f), Offset(x, c.y - body * 0.52f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        drawLine(tint, Offset(x, c.y + body * 0.52f), Offset(x, c.y + body * 0.72f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.researchAtom(c: Offset, s: Float, tint: Color) {
    val r = s * 0.3f
    drawCircle(tint, s * 0.1f, c)
    for (k in 0 until 3) {
        val rot = k * PI.toFloat() / 3f
        val path = Path().apply {
            val a = r * 1.28f
            val b = r * 0.48f
            moveTo(c.x + cos(rot) * a, c.y + sin(rot) * a)
            for (i in 1..28) {
                val t = i * (2f * PI.toFloat() / 28f)
                val x = c.x + cos(t) * a * cos(rot) - sin(t) * b * sin(rot)
                val y = c.y + cos(t) * a * sin(rot) + sin(t) * b * cos(rot)
                lineTo(x, y)
            }
            close()
        }
        drawPath(path, tint, style = Stroke(width = s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.bugBeetle(c: Offset, s: Float, tint: Color) {
    val bodyW = s * 0.4f
    val bodyH = s * 0.48f
    drawOval(tint, Offset(c.x - bodyW / 2, c.y - bodyH * 0.22f), Size(bodyW, bodyH * 0.82f))
    drawCircle(tint, s * 0.12f, Offset(c.x, c.y - bodyH * 0.3f))
    for (side in listOf(-1f, 1f)) {
        for (i in 0..2) {
            val y = c.y - bodyH * 0.02f + i * bodyH * 0.16f
            drawLine(
                tint,
                Offset(c.x + side * bodyW * 0.4f, y),
                Offset(c.x + side * bodyW * 0.85f, y + bodyH * 0.07f * (i - 1)),
                strokeWidth = s * 0.075f,
                cap = StrokeCap.Round,
            )
        }
    }
    drawLine(tint, Offset(c.x - s * 0.05f, c.y - bodyH * 0.36f), Offset(c.x - s * 0.22f, c.y - bodyH * 0.62f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
    drawLine(tint, Offset(c.x + s * 0.05f, c.y - bodyH * 0.36f), Offset(c.x + s * 0.22f, c.y - bodyH * 0.62f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
    drawCircle(tint, s * 0.05f, Offset(c.x - s * 0.22f, c.y - bodyH * 0.62f))
    drawCircle(tint, s * 0.05f, Offset(c.x + s * 0.22f, c.y - bodyH * 0.62f))
}

private fun DrawScope.officeDesk(c: Offset, s: Float, tint: Color) {
    // Monitor with screen hole + stand + desk.
    val screen = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = c.x - s * 0.3f,
                top = c.y - s * 0.32f,
                right = c.x + s * 0.3f,
                bottom = c.y + s * 0.08f,
                cornerRadius = CornerRadius(s * 0.06f),
            ),
        )
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = c.x - s * 0.22f,
                top = c.y - s * 0.24f,
                right = c.x + s * 0.22f,
                bottom = c.y,
                cornerRadius = CornerRadius(s * 0.03f),
            ),
        )
    }
    drawPath(screen, tint)
    drawRect(tint, Offset(c.x - s * 0.045f, c.y + s * 0.06f), Size(s * 0.09f, s * 0.12f))
    drawRoundRect(tint, Offset(c.x - s * 0.34f, c.y + s * 0.18f), Size(s * 0.68f, s * 0.12f), CornerRadius(s * 0.04f))
}

private fun DrawScope.lanePaths(c: Offset, s: Float, tint: Color) {
    for (i in 0..2) {
        val y = c.y - s * 0.24f + i * s * 0.24f
        val inset = i * s * 0.05f
        drawRoundRect(tint, Offset(c.x - s * 0.36f + inset, y - s * 0.07f), Size(s * 0.72f - inset * 2, s * 0.14f), CornerRadius(s * 0.07f))
    }
}

private fun DrawScope.teamPeople(c: Offset, s: Float, tint: Color) {
    fun person(at: Offset, scale: Float) {
        drawCircle(tint, s * 0.135f * scale, at + Offset(0f, -s * 0.22f * scale))
        val shoulders = Path().apply {
            val w = s * 0.22f * scale
            val top = at.y - s * 0.02f * scale
            moveTo(at.x - w, top + s * 0.3f * scale)
            quadraticTo(at.x - w, top, at.x, top)
            quadraticTo(at.x + w, top, at.x + w, top + s * 0.3f * scale)
            close()
        }
        drawPath(shoulders, tint)
    }
    person(Offset(c.x - s * 0.18f, c.y + s * 0.1f), 1.05f)
    person(Offset(c.x + s * 0.2f, c.y + s * 0.14f), 0.95f)
}

private fun DrawScope.companyMark(c: Offset, s: Float, tint: Color) {
    drawRoundRect(tint, Offset(c.x - s * 0.34f, c.y - s * 0.1f), Size(s * 0.68f, s * 0.44f), CornerRadius(s * 0.07f))
    drawRoundRect(
        tint,
        Offset(c.x - s * 0.13f, c.y - s * 0.3f),
        Size(s * 0.26f, s * 0.22f),
        CornerRadius(s * 0.04f),
        style = Stroke(s * 0.08f),
    )
}

private fun DrawScope.calendarMark(c: Offset, s: Float, tint: Color) {
    val cal = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = c.x - s * 0.3f,
                top = c.y - s * 0.2f,
                right = c.x + s * 0.3f,
                bottom = c.y + s * 0.32f,
                cornerRadius = CornerRadius(s * 0.08f),
            ),
        )
        // Header band hole so the top stripe reads.
        addRect(
            androidx.compose.ui.geometry.Rect(
                left = c.x - s * 0.22f,
                top = c.y - s * 0.08f,
                right = c.x + s * 0.22f,
                bottom = c.y + s * 0.02f,
            ),
        )
    }
    drawPath(cal, tint)
    drawLine(tint, Offset(c.x - s * 0.12f, c.y - s * 0.32f), Offset(c.x - s * 0.12f, c.y - s * 0.1f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    drawLine(tint, Offset(c.x + s * 0.12f, c.y - s * 0.32f), Offset(c.x + s * 0.12f, c.y - s * 0.1f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    for (col in 0..2) {
        drawCircle(tint, s * 0.045f, Offset(c.x - s * 0.16f + col * s * 0.16f, c.y + s * 0.16f))
    }
}

private fun DrawScope.heartMark(c: Offset, s: Float, tint: Color) {
    val path = Path().apply {
        val top = c.y - s * 0.08f
        moveTo(c.x, c.y + s * 0.3f)
        cubicTo(c.x - s * 0.42f, c.y + s * 0.02f, c.x - s * 0.38f, c.y - s * 0.28f, c.x, top)
        cubicTo(c.x + s * 0.38f, c.y - s * 0.28f, c.x + s * 0.42f, c.y + s * 0.02f, c.x, c.y + s * 0.3f)
        close()
    }
    drawPath(path, tint)
}

private fun DrawScope.cashMark(c: Offset, s: Float, tint: Color) {
    // Coin with cutout so $ reads as a ring + stem (evenOdd).
    val coin = Path().apply {
        fillType = PathFillType.EvenOdd
        addOval(androidx.compose.ui.geometry.Rect(c.x - s * 0.32f, c.y - s * 0.32f, c.x + s * 0.32f, c.y + s * 0.32f))
        addOval(androidx.compose.ui.geometry.Rect(c.x - s * 0.2f, c.y - s * 0.2f, c.x + s * 0.2f, c.y + s * 0.2f))
    }
    drawPath(coin, tint)
    drawLine(tint, Offset(c.x, c.y - s * 0.14f), Offset(c.x, c.y + s * 0.14f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
}

private fun DrawScope.menuBars(c: Offset, s: Float, tint: Color) {
    for (i in -1..1) {
        val y = c.y + i * s * 0.18f
        drawRoundRect(tint, Offset(c.x - s * 0.3f, y - s * 0.04f), Size(s * 0.6f, s * 0.08f), CornerRadius(s * 0.04f))
    }
}
