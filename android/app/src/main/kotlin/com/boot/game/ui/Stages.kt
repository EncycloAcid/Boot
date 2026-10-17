package com.boot.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * The stages, after the real ones. Each show has the look people know it by, drawn in code (no
 * logos, no trophies copied): the giant curved LED wall and catwalk of The Game Awards, the
 * rock-show flames of the Spike awards, a London theatre's red velvet for the BAFTAs, the gold of
 * the Golden Joysticks, a Las Vegas ballroom for D.I.C.E., a conference hall for GDC, an expo hall
 * for E3, a festival stage at sunset for Summer Game Fest, and each console maker's own set.
 */

/** The look of one stage: its colours and what it's built from. */
enum class StageLook(
    val top: Color,
    val bottom: Color,
    /** Text and highlights on this stage. */
    val accent: Color,
    internal val beams: List<Color>,
) {
    GAME_AWARDS(Color(0xFF07060F), Color(0xFF14102A), Color(0xFFFFB347), listOf(Color(0xFFFF5A1F), Color(0xFFB04BFF), Color(0xFF3FA9FF))),
    SPIKE(Color(0xFF0B0505), Color(0xFF2A0A06), Color(0xFFFF6A3D), listOf(Color(0xFFFF3B1F), Color(0xFFFFB020))),
    BAFTA(Color(0xFF0A0607), Color(0xFF1A0B0E), Color(0xFFE6C27A), listOf(Color(0xFFFFE6B0))),
    GOLDEN_JOYSTICKS(Color(0xFF0B0904), Color(0xFF221A08), Color(0xFFF2C94C), listOf(Color(0xFFFFD86B), Color(0xFFFFF1B8))),
    DICE(Color(0xFF060A1A), Color(0xFF151A3E), Color(0xFF9DB8FF), listOf(Color(0xFF5B7BFF), Color(0xFFB58CFF))),
    GDC(Color(0xFF041014), Color(0xFF0C2830), Color(0xFF6FE3E1), listOf(Color(0xFF4FD1D9))),
    ARKIE(Color(0xFF120A06), Color(0xFF2E1A0C), Color(0xFFFFC46B), listOf(Color(0xFFFF7AD9), Color(0xFF5CE1E6))),
    EXPO(Color(0xFF02060F), Color(0xFF0A1A33), Color(0xFF7FC8FF), listOf(Color(0xFF2F8BFF), Color(0xFFFFFFFF))),
    FESTIVAL(Color(0xFF1A0C24), Color(0xFF4A1D1F), Color(0xFFFFB25B), listOf(Color(0xFFFF8A3D), Color(0xFFFFD36E))),
    NINTENDO(Color(0xFFF4F4F6), Color(0xFFE2E2E8), Color(0xFFE60012), emptyList()),
    PLAYSTATION(Color(0xFF020B26), Color(0xFF0A2A6B), Color(0xFF9CC8FF), listOf(Color(0xFF3D8BFF))),
    XBOX(Color(0xFF030A04), Color(0xFF0B2A10), Color(0xFF7CE36B), listOf(Color(0xFF45D33A))),
    SEGA(Color(0xFF02081C), Color(0xFF0A1F5C), Color(0xFF7FB2FF), listOf(Color(0xFF3B6CFF))),
    GAMESCOM(Color(0xFF040812), Color(0xFF101C38), Color(0xFFA9C7FF), listOf(Color(0xFF6E8CFF), Color(0xFFFF6B9A))),
    SHOWCASE(Color(0xFF0E1020), Color(0xFF1C2147), Color(0xFFE9C46A), listOf(Color(0xFF9BB4FF), Color(0xFFFFE6A8), Color(0xFFFFA8D8))),
    ;

    /** Most stages are dark; Nintendo's is bright white. */
    val light: Boolean get() = this == NINTENDO
}

/** The stage a show is held on, from its name. */
fun stageFor(show: String): StageLook {
    val s = show.lowercase()
    return when {
        "spike" in s -> StageLook.SPIKE
        "game awards" in s -> StageLook.GAME_AWARDS
        "bafta" in s -> StageLook.BAFTA
        "golden joystick" in s -> StageLook.GOLDEN_JOYSTICKS
        "d.i.c.e" in s -> StageLook.DICE
        "developers choice" in s -> StageLook.GDC
        "arkie" in s -> StageLook.ARKIE
        "summer game fest" in s -> StageLook.FESTIVAL
        "gamescom" in s -> StageLook.GAMESCOM
        "nintendo" in s -> StageLook.NINTENDO
        "playstation" in s || "sony" in s -> StageLook.PLAYSTATION
        "xbox" in s || "microsoft" in s -> StageLook.XBOX
        "sega" in s -> StageLook.SEGA
        "e3" in s || "consumer electronics" in s || "trade show" in s -> StageLook.EXPO
        "game of the year" in s -> StageLook.GAME_AWARDS
        else -> StageLook.SHOWCASE
    }
}

/** A stage, drawn to fill whatever it's given: a full-screen ceremony or a banner. */
@Composable
fun StageBackdrop(look: StageLook, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(look.top, look.bottom)))
        when (look) {
            StageLook.GAME_AWARDS -> gameAwards(look)
            StageLook.SPIKE -> rockShow(look)
            StageLook.BAFTA -> theatre(look)
            StageLook.GOLDEN_JOYSTICKS -> goldShow(look)
            StageLook.DICE -> ballroom(look)
            StageLook.GDC -> conference(look)
            StageLook.ARKIE -> eighties(look)
            StageLook.EXPO -> expo(look)
            StageLook.FESTIVAL -> festival(look)
            StageLook.NINTENDO -> directSet(look)
            StageLook.PLAYSTATION, StageLook.SEGA -> pressStage(look, rings = true)
            StageLook.XBOX -> pressStage(look, rings = false)
            StageLook.GAMESCOM -> openingNight(look)
            StageLook.SHOWCASE -> showcase(look)
        }
    }
}

// Pieces every stage is built from

/** Cones of light from a rig above. */
private fun DrawScope.beams(colors: List<Color>, count: Int, spread: Float = 0.16f, alpha: Float = 0.16f, sway: Float = 0f) {
    if (colors.isEmpty()) return
    val w = size.width
    val h = size.height
    for (i in 0 until count) {
        val x = w * (i + 0.5f) / count
        val lean = sway * (if (i % 2 == 0) 1 else -1) * w
        val c = colors[i % colors.size]
        val path = Path().apply {
            moveTo(x - w * 0.008f, 0f)
            lineTo(x + w * 0.008f, 0f)
            lineTo(x + lean + w * spread, h)
            lineTo(x + lean - w * spread, h)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(c.copy(alpha = alpha), Color.Transparent)))
        drawCircle(c.copy(alpha = 0.9f), max(2f, w * 0.004f), Offset(x, max(3f, h * 0.012f)))
    }
}

/** A lighting truss across the top. */
private fun DrawScope.truss(color: Color, y: Float) {
    val w = size.width
    val t = max(3f, size.height * 0.018f)
    drawRect(color, Offset(0f, y), Size(w, t))
    drawRect(color, Offset(0f, y + t * 2.2f), Size(w, t * 0.6f))
    var x = 0f
    val step = t * 2.4f
    while (x < w) {
        drawLine(color, Offset(x, y), Offset(x + step / 2, y + t * 2.5f), strokeWidth = max(1f, t * 0.25f))
        drawLine(color, Offset(x + step / 2, y + t * 2.5f), Offset(x + step, y), strokeWidth = max(1f, t * 0.25f))
        x += step
    }
}

/** The front rows, in silhouette. */
private fun DrawScope.audience(color: Color, rows: Int = 2, density: Int = 22) {
    val w = size.width
    val h = size.height
    for (row in 0 until rows) {
        val n = density + row * 4
        val r = h * (0.085f + 0.03f * row)
        val y = h - r * (0.35f - 0.5f * row) + row * r * 0.4f
        for (i in 0..n) {
            val x = w * (i + (if (row % 2 == 0) 0.25f else 0.75f)) / n
            val rr = r * (0.85f + 0.15f * ((i * 7 + row * 3) % 4) / 3f)
            drawCircle(color, rr, Offset(x, y))
            drawRoundRect(color, Offset(x - rr * 1.4f, y + rr * 0.6f), Size(rr * 2.8f, rr * 3f), CornerRadius(rr, rr))
        }
    }
}

/** A glossy stage floor with the lights in it. */
private fun DrawScope.floor(color: Color, glow: Color, at: Float = 0.78f) {
    val w = size.width
    val h = size.height
    val y = h * at
    drawRect(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f)), startY = y, endY = h), Offset(0f, y), Size(w, h - y))
    drawOval(Brush.radialGradient(listOf(glow.copy(alpha = 0.35f), Color.Transparent), center = Offset(w / 2, y), radius = w * 0.35f), Offset(w * 0.15f, y - h * 0.06f), Size(w * 0.7f, h * 0.2f))
}

/** A screen of LED panels in a gradient of colours. */
private fun DrawScope.ledWall(left: Float, top: Float, width: Float, height: Float, colors: List<Color>, cols: Int = 28, rows: Int = 10, curve: Float = 0f) {
    val cw = width / cols
    val rh = height / rows
    for (cx in 0 until cols) {
        // A curved wall bows towards the audience at its middle.
        val bow = curve * sin(PI * (cx + 0.5) / cols).toFloat() * height
        for (ry in 0 until rows) {
            val t = (cx.toFloat() / (cols - 1))
            val c = lerpAll(colors, t)
            val shade = 0.65f + 0.35f * ((cx * 13 + ry * 7) % 5) / 4f
            drawRect(
                c.copy(alpha = 0.85f * shade),
                Offset(left + cx * cw + cw * 0.06f, top - bow + ry * rh + rh * 0.06f),
                Size(cw * 0.88f, rh * 0.88f),
            )
        }
    }
}

private fun lerpAll(colors: List<Color>, t: Float): Color {
    if (colors.size == 1) return colors[0]
    val span = t * (colors.size - 1)
    val i = min(colors.size - 2, span.toInt())
    val f = span - i
    val a = colors[i]
    val b = colors[i + 1]
    return Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f)
}

/** A plain award statuette (a star on a column), not anyone's real trophy. */
private fun DrawScope.trophy(x: Float, base: Float, height: Float, color: Color) {
    val w = height * 0.32f
    drawRect(color.copy(alpha = 0.85f), Offset(x - w / 2, base - height * 0.12f), Size(w, height * 0.12f))
    drawRect(color, Offset(x - w * 0.12f, base - height * 0.7f), Size(w * 0.24f, height * 0.58f))
    val star = Path()
    val r = height * 0.18f
    val cy = base - height * 0.82f
    for (k in 0 until 10) {
        val a = -PI / 2 + k * PI / 5
        val rr = if (k % 2 == 0) r else r * 0.45f
        val px = x + (cos(a) * rr).toFloat()
        val py = cy + (sin(a) * rr).toFloat()
        if (k == 0) star.moveTo(px, py) else star.lineTo(px, py)
    }
    star.close()
    drawPath(star, color)
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), center = Offset(x, cy), radius = r * 3), r * 3, Offset(x, cy))
}

// The stages

/** A huge curved wall of LEDs, a catwalk into the crowd, beams in orange and violet. */
private fun DrawScope.gameAwards(look: StageLook) {
    val w = size.width
    val h = size.height
    ledWall(w * 0.04f, h * 0.12f, w * 0.92f, h * 0.55f, listOf(Color(0xFF3FA9FF), Color(0xFFB04BFF), Color(0xFFFF5A1F), Color(0xFFFFB347), Color(0xFFB04BFF), Color(0xFF3FA9FF)), curve = 0.08f)
    // The wall dims towards its edges.
    drawRect(Brush.horizontalGradient(listOf(look.top.copy(alpha = 0.8f), Color.Transparent, Color.Transparent, look.top.copy(alpha = 0.8f))), Offset(0f, 0f), Size(w, h))
    beams(look.beams, 7, spread = 0.12f, alpha = 0.18f, sway = 0.06f)
    floor(Color(0xFF0C0A18), Color(0xFFFF7A2F), 0.72f)
    // The catwalk into the audience.
    drawPath(Path().apply { moveTo(w * 0.44f, h * 0.72f); lineTo(w * 0.56f, h * 0.72f); lineTo(w * 0.62f, h); lineTo(w * 0.38f, h); close() }, Brush.verticalGradient(listOf(Color(0xFF2A1F4A), Color(0xFF0C0A18)), startY = h * 0.72f, endY = h))
    trophy(w * 0.5f, h * 0.72f, h * 0.3f, look.accent)
    audience(Color.Black.copy(alpha = 0.75f))
}

/** A rock show: flame jets, a red wash, a crowd with its hands up. */
private fun DrawScope.rockShow(look: StageLook) {
    val w = size.width
    val h = size.height
    truss(Color(0xFF3A3A3A), h * 0.05f)
    beams(look.beams, 8, spread = 0.1f, alpha = 0.22f, sway = 0.08f)
    // Flames on both sides of the stage.
    for (x in listOf(0.12f, 0.24f, 0.76f, 0.88f)) {
        val fx = w * x
        val base = h * 0.74f
        drawPath(
            Path().apply { moveTo(fx - w * 0.025f, base); quadraticTo(fx - w * 0.03f, base - h * 0.2f, fx, base - h * 0.42f); quadraticTo(fx + w * 0.03f, base - h * 0.2f, fx + w * 0.025f, base); close() },
            Brush.verticalGradient(listOf(Color(0xFFFFF1A8), Color(0xFFFF8A1F), Color(0xFFFF2A0A).copy(alpha = 0.6f)), startY = base - h * 0.42f, endY = base),
        )
    }
    floor(Color(0xFF140504), Color(0xFFFF4A1F), 0.74f)
    trophy(w * 0.5f, h * 0.74f, h * 0.28f, look.accent)
    audience(Color.Black.copy(alpha = 0.8f), rows = 2, density = 26)
}

/** A London theatre: red velvet curtains, a gilded arch, a warm spotlight. */
private fun DrawScope.theatre(look: StageLook) {
    val w = size.width
    val h = size.height
    val gold = look.accent
    // The proscenium arch.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A0F12), Color(0xFF12070A))), Offset(w * 0.1f, h * 0.1f), Size(w * 0.8f, h * 0.65f))
    drawRoundRect(gold.copy(alpha = 0.7f), Offset(w * 0.1f, h * 0.08f), Size(w * 0.8f, h * 0.68f), CornerRadius(w * 0.05f, w * 0.05f), style = Stroke(width = max(2f, h * 0.012f)))
    // Curtains, swagged at the sides and across the top.
    for (side in listOf(0, 1)) {
        val x0 = if (side == 0) w * 0.1f else w * 0.9f
        val dir = if (side == 0) 1f else -1f
        for (fold in 0 until 6) {
            val x = x0 + dir * w * 0.022f * fold
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF7A0E16), Color(0xFFB3202A), Color(0xFF5C0A10)), startX = x, endX = x + w * 0.022f * dir), Offset(min(x, x + w * 0.022f * dir), h * 0.1f), Size(w * 0.022f, h * 0.65f - fold * h * 0.03f))
        }
    }
    for (k in 0 until 7) {
        val x = w * (0.1f + 0.8f * k / 7)
        drawOval(Color(0xFF9C1822), Offset(x, h * 0.06f), Size(w * 0.8f / 7 * 1.1f, h * 0.12f))
    }
    beams(look.beams, 1, spread = 0.18f, alpha = 0.22f)
    floor(Color(0xFF1A0A06), gold, 0.75f)
    trophy(w * 0.5f, h * 0.75f, h * 0.26f, gold)
    audience(Color.Black.copy(alpha = 0.8f))
}

/** Gold everything: a ring of light behind the stage and sparkle in the air. */
private fun DrawScope.goldShow(look: StageLook) {
    val w = size.width
    val h = size.height
    val c = Offset(w / 2, h * 0.42f)
    for (k in 0 until 4) {
        drawCircle(look.accent.copy(alpha = 0.55f - k * 0.12f), h * (0.3f + k * 0.05f), c, style = Stroke(width = max(2f, h * (0.02f - k * 0.004f))))
    }
    drawCircle(Brush.radialGradient(listOf(look.accent.copy(alpha = 0.3f), Color.Transparent), center = c, radius = h * 0.45f), h * 0.45f, c)
    beams(look.beams, 6, spread = 0.1f, alpha = 0.14f, sway = 0.04f)
    // Sparkle.
    for (k in 0 until 60) {
        val x = w * ((k * 37 % 100) / 100f)
        val y = h * ((k * 53 % 70) / 100f)
        drawCircle(look.accent.copy(alpha = 0.3f + (k % 3) * 0.2f), max(1f, h * 0.004f * (1 + k % 2)), Offset(x, y))
    }
    floor(Color(0xFF120E04), look.accent, 0.76f)
    trophy(w * 0.5f, h * 0.76f, h * 0.28f, look.accent)
    audience(Color.Black.copy(alpha = 0.8f))
}

/** A Las Vegas ballroom: chandeliers, a blue screen, round tables with white cloths. */
private fun DrawScope.ballroom(look: StageLook) {
    val w = size.width
    val h = size.height
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF2B3C9E), Color(0xFF6C4BD8), Color(0xFF2B3C9E))), Offset(w * 0.25f, h * 0.14f), Size(w * 0.5f, h * 0.36f), CornerRadius(h * 0.02f, h * 0.02f))
    for (x in listOf(0.15f, 0.5f, 0.85f)) {
        val cx = w * x
        drawLine(Color(0xFFCFC6A0).copy(alpha = 0.6f), Offset(cx, 0f), Offset(cx, h * 0.07f), strokeWidth = max(1f, h * 0.004f))
        for (k in 0 until 7) {
            drawCircle(Color(0xFFFFF4D0).copy(alpha = 0.8f), max(1.5f, h * 0.008f), Offset(cx + (k - 3) * h * 0.02f, h * 0.08f + (if (k % 2 == 0) 0f else h * 0.012f)))
        }
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFF4D0), Color.Transparent), center = Offset(cx, h * 0.085f), radius = h * 0.12f), h * 0.12f, Offset(cx, h * 0.085f))
    }
    beams(look.beams, 4, spread = 0.12f, alpha = 0.12f)
    floor(Color(0xFF0B0E26), look.accent, 0.62f)
    trophy(w * 0.5f, h * 0.62f, h * 0.22f, look.accent)
    // Round tables in the foreground.
    for (row in 0 until 2) {
        for (k in 0 until 6) {
            val x = w * (0.08f + 0.17f * k + (if (row == 1) 0.085f else 0f))
            val y = h * (0.8f + 0.12f * row)
            val r = h * (0.07f + 0.02f * row)
            drawOval(Color(0xFFE9E6F2).copy(alpha = 0.85f), Offset(x - r * 1.6f, y - r * 0.4f), Size(r * 3.2f, r * 0.8f))
            drawCircle(Color(0xFFFFE6A8), max(1.5f, r * 0.08f), Offset(x, y - r * 0.15f))
            drawCircle(Color.Black.copy(alpha = 0.75f), r * 0.35f, Offset(x - r * 1.8f, y - r * 0.6f))
            drawCircle(Color.Black.copy(alpha = 0.75f), r * 0.35f, Offset(x + r * 1.8f, y - r * 0.6f))
        }
    }
}

/** A conference hall: a big projection screen, a podium, rows of chairs. */
private fun DrawScope.conference(look: StageLook) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF103A44), Color(0xFF0B2A31))), Offset(w * 0.2f, h * 0.1f), Size(w * 0.6f, h * 0.42f))
    drawRect(Brush.linearGradient(listOf(Color(0xFF1B6F7A), Color(0xFF4FD1D9).copy(alpha = 0.6f)), Offset(w * 0.2f, h * 0.1f), Offset(w * 0.8f, h * 0.52f)), Offset(w * 0.22f, h * 0.12f), Size(w * 0.56f, h * 0.38f))
    // A podium to the side.
    drawRect(Color(0xFF0E1C20), Offset(w * 0.8f, h * 0.5f), Size(w * 0.06f, h * 0.18f))
    drawRect(look.accent.copy(alpha = 0.8f), Offset(w * 0.8f, h * 0.5f), Size(w * 0.06f, h * 0.012f))
    beams(look.beams, 3, spread = 0.1f, alpha = 0.1f)
    floor(Color(0xFF071417), look.accent, 0.68f)
    trophy(w * 0.5f, h * 0.68f, h * 0.2f, look.accent)
    // Chairs.
    for (row in 0 until 3) {
        val y = h * (0.78f + 0.08f * row)
        val n = 14 + row * 2
        for (k in 0 until n) {
            val x = w * (k + 0.5f) / n
            val s = h * (0.035f + 0.008f * row)
            drawRoundRect(Color(0xFF0A2025), Offset(x - s, y), Size(s * 2, s * 1.4f), CornerRadius(s * 0.3f, s * 0.3f))
            drawCircle(Color.Black.copy(alpha = 0.7f), s * 0.6f, Offset(x, y - s * 0.4f))
        }
    }
}

/** A hotel ballroom in the early eighties: a neon grid and a banner. */
private fun DrawScope.eighties(look: StageLook) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.45f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2B0F3A), Color(0xFFFF7AD9).copy(alpha = 0.5f)), startY = 0f, endY = horizon), Offset(w * 0.1f, h * 0.08f), Size(w * 0.8f, horizon - h * 0.08f))
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFD36E), Color(0xFFFF5A8A)), startY = horizon - h * 0.2f, endY = horizon), h * 0.16f, Offset(w / 2, horizon))
    for (k in 0..10) {
        val x = w * (0.1f + 0.8f * k / 10)
        drawLine(Color(0xFF5CE1E6).copy(alpha = 0.7f), Offset(w / 2 + (x - w / 2) * 0.3f, horizon), Offset(x, h * 0.75f), strokeWidth = max(1f, h * 0.004f))
    }
    for (k in 0..5) {
        val y = horizon + (h * 0.75f - horizon) * (k / 5f) * (k / 5f)
        drawLine(Color(0xFF5CE1E6).copy(alpha = 0.7f), Offset(w * 0.1f, y), Offset(w * 0.9f, y), strokeWidth = max(1f, h * 0.004f))
    }
    floor(Color(0xFF1A0E06), look.accent, 0.75f)
    trophy(w * 0.5f, h * 0.75f, h * 0.24f, look.accent)
    audience(Color.Black.copy(alpha = 0.8f), rows = 1)
}

/** An expo hall: booths under giant hanging signs, blue light everywhere. */
private fun DrawScope.expo(look: StageLook) {
    val w = size.width
    val h = size.height
    truss(Color(0xFF1C2A44), h * 0.04f)
    // Hanging signs and booth towers.
    for (k in 0 until 5) {
        val x = w * (0.04f + 0.2f * k)
        val tall = h * (0.3f + 0.08f * (k % 3))
        val c = listOf(Color(0xFF2F8BFF), Color(0xFFE5484D), Color(0xFF45D33A), Color(0xFFFFB020), Color(0xFFB04BFF))[k]
        drawRect(c.copy(alpha = 0.25f), Offset(x, h * 0.75f - tall), Size(w * 0.14f, tall))
        drawRect(c.copy(alpha = 0.9f), Offset(x, h * 0.75f - tall), Size(w * 0.14f, h * 0.04f))
        drawRect(Color.White.copy(alpha = 0.15f), Offset(x + w * 0.02f, h * 0.75f - tall + h * 0.08f), Size(w * 0.1f, h * 0.12f))
    }
    beams(look.beams, 6, spread = 0.08f, alpha = 0.1f, sway = 0.03f)
    floor(Color(0xFF060C18), look.accent, 0.75f)
    audience(Color.Black.copy(alpha = 0.8f), rows = 2, density = 30)
}

/** A festival stage at sunset: an orange sky, a big screen, a field of people. */
private fun DrawScope.festival(look: StageLook) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2A1B4A), Color(0xFFFF7A3D), Color(0xFFFFC86B)), startY = 0f, endY = h * 0.7f), Offset.Zero, Size(w, h * 0.7f))
    // The stage's roof and screen.
    drawPath(Path().apply { moveTo(w * 0.15f, h * 0.2f); lineTo(w * 0.5f, h * 0.06f); lineTo(w * 0.85f, h * 0.2f); close() }, Color(0xFF151018))
    drawRect(Color(0xFF151018), Offset(w * 0.15f, h * 0.2f), Size(w * 0.7f, h * 0.06f))
    drawRect(Color(0xFF151018), Offset(w * 0.15f, h * 0.2f), Size(w * 0.03f, h * 0.55f))
    drawRect(Color(0xFF151018), Offset(w * 0.82f, h * 0.2f), Size(w * 0.03f, h * 0.55f))
    ledWall(w * 0.25f, h * 0.3f, w * 0.5f, h * 0.3f, listOf(Color(0xFFFF8A3D), Color(0xFFFFD36E), Color(0xFFFF5A8A)), cols = 18, rows = 7)
    beams(look.beams, 5, spread = 0.1f, alpha = 0.14f, sway = 0.05f)
    floor(Color(0xFF1A0E10), look.accent, 0.75f)
    audience(Color(0xFF0E0810).copy(alpha = 0.9f), rows = 3, density = 30)
}

/** A bright white studio set with one red stripe: a presenter's direct-to-camera show. */
private fun DrawScope.directSet(look: StageLook) {
    val w = size.width
    val h = size.height
    drawRect(Color.White, Offset(w * 0.06f, h * 0.08f), Size(w * 0.88f, h * 0.7f))
    drawRect(look.accent, Offset(w * 0.06f, h * 0.62f), Size(w * 0.88f, h * 0.025f))
    // Soft panels.
    for (k in 0 until 6) {
        drawRect(Color(0xFFE9E9EF), Offset(w * (0.08f + 0.145f * k), h * 0.12f), Size(w * 0.13f, h * 0.46f))
    }
    drawRect(Color(0xFFD6D6DE), Offset(0f, h * 0.78f), Size(w, h * 0.22f))
    // A presenter with a mug: a direct is one person talking to you.
    val x = w * 0.5f
    drawCircle(Color(0xFF2B2B33), h * 0.06f, Offset(x, h * 0.4f))
    drawRoundRect(Color(0xFF2B2B33), Offset(x - h * 0.09f, h * 0.47f), Size(h * 0.18f, h * 0.31f), CornerRadius(h * 0.04f, h * 0.04f))
}

/** A console maker's press stage: a deep wash of its colour and rings of light. */
private fun DrawScope.pressStage(look: StageLook, rings: Boolean) {
    val w = size.width
    val h = size.height
    val c = Offset(w / 2, h * 0.4f)
    val glow = look.beams.firstOrNull() ?: look.accent
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center = c, radius = h * 0.6f), h * 0.6f, c)
    if (rings) {
        for (k in 0 until 3) drawCircle(glow.copy(alpha = 0.5f - k * 0.13f), h * (0.18f + k * 0.08f), c, style = Stroke(width = max(1.5f, h * 0.01f)))
    } else {
        // Strips of green light across the back wall.
        for (k in 0 until 9) drawRect(glow.copy(alpha = 0.18f + 0.05f * (k % 3)), Offset(w * (0.06f + 0.1f * k), h * 0.12f), Size(w * 0.06f, h * 0.5f))
    }
    beams(look.beams, 5, spread = 0.1f, alpha = 0.14f)
    floor(look.top, glow, 0.74f)
    audience(Color.Black.copy(alpha = 0.8f))
}

/** Opening night in a huge hall: a stage wrapped in screens, blue and pink. */
private fun DrawScope.openingNight(look: StageLook) {
    val w = size.width
    val h = size.height
    ledWall(w * 0.02f, h * 0.1f, w * 0.28f, h * 0.45f, listOf(Color(0xFF6E8CFF), Color(0xFFFF6B9A)), cols = 8, rows = 9)
    ledWall(w * 0.7f, h * 0.1f, w * 0.28f, h * 0.45f, listOf(Color(0xFFFF6B9A), Color(0xFF6E8CFF)), cols = 8, rows = 9)
    ledWall(w * 0.33f, h * 0.06f, w * 0.34f, h * 0.5f, listOf(Color(0xFF6E8CFF), Color(0xFFA9C7FF), Color(0xFFFF6B9A)), cols = 12, rows = 10)
    beams(look.beams, 8, spread = 0.08f, alpha = 0.14f, sway = 0.07f)
    floor(Color(0xFF070B18), look.accent, 0.72f)
    audience(Color.Black.copy(alpha = 0.85f), rows = 3, density = 32)
}

/** Your own show: the house lights of a showcase stream. */
private fun DrawScope.showcase(look: StageLook) {
    beams(look.beams, 3, spread = 0.16f, alpha = 0.16f)
    floor(look.bottom, look.accent, 0.8f)
    audience(Color.Black.copy(alpha = 0.55f), rows = 1)
}
