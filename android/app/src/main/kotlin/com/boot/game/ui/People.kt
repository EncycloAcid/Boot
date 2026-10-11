package com.boot.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boot.core.Employee
import com.boot.core.GameState
import kotlin.math.cos
import kotlin.math.sin

/*
 * The people of the company, drawn the same way everywhere: seated at their desks in the office,
 * and as portraits on the team page. How someone looks comes from their id, so a candidate looks
 * the same once hired.
 */

internal enum class HairStyle { SHORT, SWEPT, LONG, BUN, CURLY, BALD, PONYTAIL, BOB }

/** What someone wears. */
internal enum class Outfit { HOODIE, TEE, COLLAR, SWEATER, BLAZER }

internal data class Look(
    val skin: Color,
    val hair: Color,
    val style: HairStyle,
    val glasses: Boolean,
    val beard: Boolean,
    val outfit: Outfit,
    /** The colour of what they're wearing. Their business shows on their lanyard instead. */
    val top: Color,
) {
    val hoodie: Boolean get() = outfit == Outfit.HOODIE
}

/** Everyday clothes: navy, denim, olive, mustard, burgundy, teal, charcoal, cream, coral, forest, lavender, sky, white, black, red. */
private val Clothes = listOf(
    0xFF2F3B59, 0xFF4A6FA5, 0xFF6B7B3A, 0xFFE0A526, 0xFF8E2C48, 0xFF2A9D8F, 0xFF3D3D46, 0xFFE8DFCB,
    0xFFE76F51, 0xFF2D6A4F, 0xFF9D8EC7, 0xFF6CB4EE, 0xFFF1F1EE, 0xFF26262C, 0xFFD64545,
).map { Color(it) }

private val Skins = listOf(0xFFF7D9C4, 0xFFEFC3A2, 0xFFD9A07A, 0xFFB97C55, 0xFF8F5A38, 0xFF63402A).map { Color(it) }
private val Hairs = listOf(0xFF2A1E17, 0xFF2A1E17, 0xFF4B2F1D, 0xFF7A4A2A, 0xFFB8692F, 0xFFE2BE78, 0xFF1C1C28, 0xFF9A9AA3, 0xFFC0473B).map { Color(it) }

internal fun lookFor(seed: Int): Look {
    val r = java.util.Random(seed * 7_919L + 104_729L)
    val style = HairStyle.entries[r.nextInt(HairStyle.entries.size)]
    val longish = style == HairStyle.LONG || style == HairStyle.BUN || style == HairStyle.PONYTAIL || style == HairStyle.BOB
    return Look(
        skin = Skins[r.nextInt(Skins.size)],
        hair = Hairs[r.nextInt(Hairs.size)],
        style = style,
        glasses = r.nextInt(4) == 0,
        beard = !longish && style != HairStyle.BALD && r.nextInt(4) == 0 || style == HairStyle.BALD && r.nextInt(2) == 0,
        outfit = if (r.nextInt(3) == 0) Outfit.HOODIE else listOf(Outfit.TEE, Outfit.COLLAR, Outfit.SWEATER, Outfit.BLAZER)[r.nextInt(4)],
        top = Clothes[r.nextInt(Clothes.size)],
    )
}

internal fun Color.shade(f: Float) = Color((red * f).coerceIn(0f, 1f), (green * f).coerceIn(0f, 1f), (blue * f).coerceIn(0f, 1f), alpha)

internal fun Color.mix(other: Color, t: Float) = Color(
    red + (other.red - red) * t,
    green + (other.green - green) * t,
    blue + (other.blue - blue) * t,
    alpha + (other.alpha - alpha) * t,
)

/** Which way someone sits: facing the viewer (down and to the left), or with their back to us. */
internal enum class Facing { FRONT, BACK }

/** Where things are on a seated figure, in multiples of its size, from the hips. */
private const val HEAD_Y = -0.62f
private const val HEAD_R = 0.155f
private const val SHOULDER_Y = -0.4f

/** A path in a head's own units: r = 1, centred on the head. */
private class Shape(private val c: Offset, private val r: Float) {
    val path = Path()
    fun move(x: Float, y: Float) = path.moveTo(c.x + x * r, c.y + y * r)
    fun line(x: Float, y: Float) = path.lineTo(c.x + x * r, c.y + y * r)
    fun quad(cx: Float, cy: Float, x: Float, y: Float) = path.quadraticTo(c.x + cx * r, c.y + cy * r, c.x + x * r, c.y + y * r)
}

private fun circle(c: Offset, r: Float) = Path().apply { addOval(Rect(c, r)) }

/** Where a front-facing sitter's elbows rest, so the forearms can be drawn over the desk later. */
internal fun frontElbows(hip: Offset, s: Float): Pair<Offset, Offset> =
    Offset(hip.x - 0.24f * s, hip.y - 0.2f * s) to Offset(hip.x + 0.06f * s, hip.y - 0.14f * s)

/**
 * Someone sitting down, drawn around their hips at [hip]. [s] is the size of the figure: the top of
 * the head is about 0.78 [s] above the hips. [hands] are where their hands rest on the keyboard;
 * for someone facing us the forearms go over the desk, so [drawForearms] draws those after it.
 */
internal fun DrawScope.drawSeated(
    hip: Offset,
    s: Float,
    look: Look,
    shirt: Color,
    facing: Facing,
    phase: Float?,
    crown: Boolean,
    hands: Pair<Offset, Offset>,
    /** Their business's colour, on a lanyard. */
    badge: Color? = null,
) {
    val wave = phase?.let { sin(2 * Math.PI.toFloat() * it) } ?: 0f
    val bob = wave * 0.005f
    fun o(x: Float, y: Float) = Offset(hip.x + x * s, hip.y + y * s)
    val sleeve = shirt.shade(0.86f)
    val head = o(0f, HEAD_Y + bob)

    // Hair that hangs behind the shoulders.
    if (facing == Facing.FRONT) drawHairBehind(head, HEAD_R * s, look, portrait = false)

    if (facing == Facing.BACK) {
        // Both arms reach ahead to the keyboard; the near one shows past the body.
        val tap = wave * 0.012f * s
        val (left, right) = hands
        arm(o(-0.15f, SHOULDER_Y + 0.03f), o(0.02f, -0.33f), left + Offset(0f, tap), sleeve, look.skin, s)
        arm(o(0.15f, SHOULDER_Y + 0.03f), o(0.3f, -0.3f), right + Offset(0f, -tap), shirt, look.skin, s)
    }

    // Torso, lit from the left.
    val torso = Path().apply {
        moveTo(hip.x - 0.15f * s, hip.y)
        quadraticTo(hip.x - 0.2f * s, hip.y - 0.2f * s, hip.x - 0.19f * s, hip.y + (SHOULDER_Y + 0.04f + bob) * s)
        quadraticTo(hip.x - 0.18f * s, hip.y + (SHOULDER_Y - 0.05f + bob) * s, hip.x - 0.06f * s, hip.y + (SHOULDER_Y - 0.07f + bob) * s)
        lineTo(hip.x + 0.06f * s, hip.y + (SHOULDER_Y - 0.07f + bob) * s)
        quadraticTo(hip.x + 0.18f * s, hip.y + (SHOULDER_Y - 0.05f + bob) * s, hip.x + 0.19f * s, hip.y + (SHOULDER_Y + 0.04f + bob) * s)
        quadraticTo(hip.x + 0.2f * s, hip.y - 0.2f * s, hip.x + 0.15f * s, hip.y)
        close()
    }
    drawPath(torso, Brush.horizontalGradient(listOf(shirt.mix(Color.White, 0.08f), shirt, shirt.shade(0.8f)), hip.x - 0.2f * s, hip.x + 0.2f * s))
    if (look.hoodie) {
        drawOval(shirt.shade(0.78f), topLeft = o(-0.13f, SHOULDER_Y - 0.1f + bob), size = Size(0.26f * s, 0.1f * s))
        if (facing == Facing.FRONT) {
            drawLine(Color.White.copy(alpha = 0.85f), o(-0.05f, SHOULDER_Y - 0.02f + bob), o(-0.055f, SHOULDER_Y + 0.1f + bob), strokeWidth = 0.014f * s, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.85f), o(0.03f, SHOULDER_Y - 0.02f + bob), o(0.035f, SHOULDER_Y + 0.1f + bob), strokeWidth = 0.014f * s, cap = StrokeCap.Round)
            drawRoundRect(shirt.shade(0.88f), o(-0.1f, -0.16f), Size(0.18f * s, 0.08f * s), CornerRadius(0.03f * s))
        }
    } else if (facing == Facing.FRONT) {
        val top = SHOULDER_Y - 0.065f + bob
        when (look.outfit) {
            Outfit.TEE -> drawOval(look.skin.shade(0.9f), o(-0.06f, top - 0.005f), Size(0.11f * s, 0.05f * s))
            Outfit.SWEATER -> {
                drawOval(shirt.shade(0.72f), o(-0.075f, top - 0.012f), Size(0.14f * s, 0.06f * s))
                drawOval(look.skin.shade(0.9f), o(-0.055f, top - 0.004f), Size(0.1f * s, 0.038f * s))
            }
            else -> {
                // A V neck: skin, or a white tee under a blazer.
                val inner = if (look.outfit == Outfit.BLAZER) Color(0xFFF4F4F2) else look.skin.shade(0.9f)
                val v = Path().apply {
                    moveTo(hip.x - 0.065f * s, hip.y + top * s)
                    lineTo(hip.x - 0.01f * s, hip.y + (top + (if (look.outfit == Outfit.BLAZER) 0.2f else 0.105f)) * s)
                    lineTo(hip.x + 0.045f * s, hip.y + top * s)
                    close()
                }
                drawPath(v, inner)
                if (look.outfit == Outfit.COLLAR) {
                    // Collar points either side of the neck.
                    val white = Color.White.copy(alpha = 0.9f)
                    drawPath(Path().apply { moveTo(hip.x - 0.07f * s, hip.y + top * s); lineTo(hip.x - 0.015f * s, hip.y + (top + 0.06f) * s); lineTo(hip.x - 0.08f * s, hip.y + (top + 0.045f) * s); close() }, white)
                    drawPath(Path().apply { moveTo(hip.x + 0.05f * s, hip.y + top * s); lineTo(hip.x - 0.005f * s, hip.y + (top + 0.06f) * s); lineTo(hip.x + 0.06f * s, hip.y + (top + 0.045f) * s); close() }, white)
                }
                if (look.outfit == Outfit.BLAZER) {
                    drawLine(shirt.shade(0.62f), o(-0.065f, top), o(-0.01f, top + 0.2f), strokeWidth = 0.012f * s, cap = StrokeCap.Round)
                    drawLine(shirt.shade(0.62f), o(0.045f, top), o(-0.01f, top + 0.2f), strokeWidth = 0.012f * s, cap = StrokeCap.Round)
                }
            }
        }
    } else {
        // A collar from behind.
        drawLine(shirt.shade(0.7f), o(-0.07f, SHOULDER_Y - 0.055f + bob), o(0.07f, SHOULDER_Y - 0.055f + bob), strokeWidth = 0.02f * s, cap = StrokeCap.Round)
    }

    if (facing == Facing.FRONT && badge != null) {
        // A lanyard with a badge in their business's colour.
        val strap = Color(0xFF30303A).copy(alpha = 0.75f)
        drawLine(strap, o(-0.05f, SHOULDER_Y - 0.06f + bob), o(-0.035f, SHOULDER_Y + 0.12f + bob), strokeWidth = 0.008f * s)
        drawLine(strap, o(0.035f, SHOULDER_Y - 0.06f + bob), o(-0.015f, SHOULDER_Y + 0.12f + bob), strokeWidth = 0.008f * s)
        drawRoundRect(badge, o(-0.06f, SHOULDER_Y + 0.115f + bob), Size(0.07f * s, 0.085f * s), CornerRadius(0.012f * s))
        drawRect(Color.White.copy(alpha = 0.8f), o(-0.048f, SHOULDER_Y + 0.135f + bob), Size(0.046f * s, 0.012f * s))
    }

    if (facing == Facing.FRONT) {
        // Upper arms down to the elbows; the forearms rest on the desk.
        val (le, re) = frontElbows(hip, s)
        drawLine(shirt.mix(Color.White, 0.05f), o(-0.17f, SHOULDER_Y + 0.02f + bob), le, strokeWidth = 0.09f * s, cap = StrokeCap.Round)
        drawLine(sleeve, o(0.16f, SHOULDER_Y + 0.02f + bob), re, strokeWidth = 0.09f * s, cap = StrokeCap.Round)
    }

    // Neck and head.
    drawRect(look.skin.shade(0.86f), topLeft = o(-0.045f, HEAD_Y + HEAD_R * 0.6f + bob), size = Size(0.09f * s, 0.09f * s))
    drawHead(head, HEAD_R * s, look, facing)
    if (crown) drawCrown(Offset(head.x - 0.01f * s, head.y - (HEAD_R + 0.025f) * s), s)
}

/** Forearms and hands on the keyboard, for someone facing us; drawn after their desk so they rest on it. */
internal fun DrawScope.drawForearms(hip: Offset, s: Float, look: Look, shirt: Color, hands: Pair<Offset, Offset>, phase: Float?) {
    val wave = phase?.let { sin(2 * Math.PI.toFloat() * it) } ?: 0f
    val tap = wave * 0.012f * s
    val (le, re) = frontElbows(hip, s)
    val (lh, rh) = hands
    arm(null, le, lh + Offset(0f, tap), shirt.mix(Color.White, 0.05f), look.skin, s)
    arm(null, re, rh + Offset(0f, -tap), shirt.shade(0.86f), look.skin, s)
}

private fun DrawScope.arm(shoulder: Offset?, elbow: Offset, hand: Offset, sleeve: Color, skin: Color, s: Float) {
    if (shoulder != null) drawLine(sleeve, shoulder, elbow, strokeWidth = 0.09f * s, cap = StrokeCap.Round)
    drawLine(sleeve, elbow, hand, strokeWidth = 0.075f * s, cap = StrokeCap.Round)
    drawCircle(skin, 0.04f * s, hand)
}

/** Long hair that falls behind the shoulders, and buns and ponytails behind the head. */
private fun DrawScope.drawHairBehind(c: Offset, r: Float, look: Look, portrait: Boolean) {
    val hair = look.hair.shade(0.85f)
    fun p(x: Float, y: Float) = Offset(c.x + x * r, c.y + y * r)
    when (look.style) {
        HairStyle.LONG -> drawRoundRect(hair, p(-1.08f, -0.5f), Size(2.16f * r, 2.25f * r), CornerRadius(0.7f * r))
        HairStyle.BOB -> drawRoundRect(hair, p(-1.1f, -0.6f), Size(2.2f * r, 1.55f * r), CornerRadius(0.6f * r))
        HairStyle.BUN -> drawCircle(hair, 0.42f * r, if (portrait) p(0f, -1.05f) else p(0.25f, -1.0f))
        HairStyle.PONYTAIL -> if (portrait) {
            drawRoundRect(hair, p(0.75f, -0.2f), Size(0.42f * r, 1.3f * r), CornerRadius(0.21f * r))
        } else {
            drawRoundRect(hair, p(0.82f, -0.25f), Size(0.42f * r, 1.25f * r), CornerRadius(0.21f * r))
        }
        else -> {}
    }
}

/** The part of a head the hair covers, before trimming to the skull. */
private fun hairRegion(c: Offset, r: Float, style: HairStyle, facing: Facing, portrait: Boolean): Shape {
    val h = Shape(c, r)
    h.move(-1.5f, -1.6f)
    h.line(1.5f, -1.6f)
    when {
        facing == Facing.BACK -> {
            // Everything but a sliver of cheek on the right and the nape of the neck.
            h.line(1.5f, -0.35f)
            h.line(0.74f, -0.32f)
            h.quad(0.72f, 0.3f, 0.42f, 0.62f)
            h.quad(0.0f, 0.82f, -0.6f, 0.62f)
            h.quad(-0.95f, 0.46f, -1.5f, 0.38f)
        }
        portrait -> {
            when (style) {
                HairStyle.LONG, HairStyle.BOB -> {
                    h.line(1.5f, 1.1f)
                    h.line(0.84f, 1.1f)
                    h.line(0.9f, 0.05f)
                    h.quad(0.8f, -0.6f, 0.12f, -0.72f)
                    h.quad(-0.75f, -0.6f, -0.9f, 0.05f)
                    h.line(-0.84f, 1.1f)
                    h.line(-1.5f, 1.1f)
                }
                HairStyle.SWEPT -> {
                    h.line(1.5f, 0f)
                    h.line(0.93f, 0f)
                    h.quad(0.88f, -0.45f, 0.6f, -0.55f)
                    h.quad(-0.1f, -0.32f, -0.93f, -0.12f)
                    h.line(-1.5f, -0.12f)
                }
                HairStyle.BUN, HairStyle.PONYTAIL -> {
                    h.line(1.5f, -0.05f)
                    h.line(0.93f, -0.05f)
                    h.quad(0.82f, -0.5f, 0.4f, -0.64f)
                    h.quad(0f, -0.74f, -0.4f, -0.64f)
                    h.quad(-0.82f, -0.5f, -0.93f, -0.05f)
                    h.line(-1.5f, -0.05f)
                }
                else -> {
                    h.line(1.5f, 0f)
                    h.line(0.93f, 0f)
                    h.quad(0.88f, -0.4f, 0.55f, -0.5f)
                    h.quad(0f, -0.66f, -0.55f, -0.5f)
                    h.quad(-0.88f, -0.4f, -0.93f, 0f)
                    h.line(-1.5f, 0f)
                }
            }
        }
        else -> {
            // Turned to the left: the back of the head shows on the right, behind the ear.
            h.line(1.5f, 0.55f)
            h.line(0.96f, 0.55f)
            h.line(0.94f, -0.12f)
            h.line(0.6f, -0.28f)
            when (style) {
                HairStyle.SWEPT -> {
                    h.quad(0.05f, -0.38f, -0.95f, -0.16f)
                    h.line(-1.5f, -0.16f)
                }
                HairStyle.LONG, HairStyle.BOB -> {
                    h.quad(0.35f, -0.78f, -0.05f, -0.74f)
                    h.quad(-0.68f, -0.64f, -0.94f, 0.08f)
                    h.line(-0.86f, 1.1f)
                    h.line(-1.5f, 1.1f)
                }
                HairStyle.BUN, HairStyle.PONYTAIL -> {
                    h.quad(0.05f, -0.86f, -0.66f, -0.5f)
                    h.quad(-0.9f, -0.34f, -0.95f, -0.04f)
                    h.line(-1.5f, -0.04f)
                }
                else -> {
                    h.quad(0.1f, -0.78f, -0.55f, -0.52f)
                    h.quad(-0.86f, -0.4f, -0.93f, 0.02f)
                    h.line(-1.5f, 0.02f)
                }
            }
        }
    }
    h.path.close()
    return h
}

/** A head of radius [r] centred at [c]. Facing the viewer it's turned slightly to the left. */
internal fun DrawScope.drawHead(c: Offset, r: Float, look: Look, facing: Facing, portrait: Boolean = false) {
    fun p(x: Float, y: Float) = Offset(c.x + x * r, c.y + y * r)
    val hair = look.hair
    val hairBrush = Brush.verticalGradient(listOf(hair.mix(Color.White, 0.14f), hair, hair.shade(0.82f)), c.y - 1.1f * r, c.y + r)
    val bald = look.style == HairStyle.BALD
    val earColor = look.skin.shade(0.9f)

    if (facing == Facing.BACK) {
        drawCircle(look.skin, r, c)
        if (!bald) {
            val cap = circle(p(0f, -0.03f), 1.08f * r)
            clipPath(cap) { drawPath(hairRegion(c, r, look.style, facing, false).path, hairBrush) }
        } else {
            clipPath(circle(c, r)) { drawRect(hair.shade(0.9f), topLeft = p(-1f, 0.15f), size = Size(2 * r, 0.42f * r)) }
            drawArc(Color.White.copy(alpha = 0.3f), 200f, 60f, false, p(-0.7f, -0.8f), Size(1.2f * r, 1.2f * r), style = Stroke(0.12f * r, cap = StrokeCap.Round))
        }
        drawOval(earColor, p(0.68f, -0.08f), Size(0.24f * r, 0.36f * r))
        when (look.style) {
            HairStyle.LONG -> drawRoundRect(hairBrush, p(-0.98f, 0.1f), Size(1.96f * r, 1.75f * r), CornerRadius(0.6f * r))
            HairStyle.BOB -> drawRoundRect(hairBrush, p(-1.04f, 0.0f), Size(2.08f * r, 0.95f * r), CornerRadius(0.45f * r))
            HairStyle.PONYTAIL -> drawRoundRect(hair.shade(0.92f), p(-0.2f, 0.35f), Size(0.4f * r, 1.25f * r), CornerRadius(0.2f * r))
            HairStyle.BUN -> drawCircle(hairBrush, 0.42f * r, p(0f, -0.98f))
            HairStyle.CURLY -> curls(c, r, hair, back = true)
            else -> {}
        }
        if (!bald) drawArc(Color.White.copy(alpha = 0.16f), 200f, 70f, false, p(-0.75f, -0.85f), Size(1.4f * r, 1.4f * r), style = Stroke(0.13f * r, cap = StrokeCap.Round))
        return
    }

    val turn = if (portrait) 0f else -0.2f
    // Face, softly lit from the upper left.
    drawCircle(Brush.radialGradient(listOf(look.skin.mix(Color.White, 0.1f), look.skin, look.skin.shade(0.9f)), p(-0.35f, -0.35f), 1.7f * r), r, c)

    // Beard along the jaw.
    if (look.beard) {
        clipPath(circle(c, r * 1.01f)) {
            drawCircle(hair, 0.95f * r, p(turn * 0.5f, 0.98f))
        }
    }

    // Eyes, brows, nose and a smile.
    val ex = turn
    val eye = Color(0xFF231A16)
    for (dx in listOf(-0.34f, 0.3f)) {
        drawOval(eye, p(ex + dx - 0.085f, -0.07f), Size(0.17f * r, 0.23f * r))
        drawCircle(Color.White, 0.04f * r, p(ex + dx + 0.03f, -0.02f))
        val brow = if (bald) look.skin.shade(0.6f) else hair.shade(0.85f)
        drawLine(brow, p(ex + dx - 0.12f, -0.28f), p(ex + dx + 0.11f, -0.31f), strokeWidth = 0.07f * r, cap = StrokeCap.Round)
    }
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.32f), 0.17f * r, p(ex - 0.55f, 0.3f))
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.32f), 0.17f * r, p(ex + 0.48f, 0.3f))
    if (!portrait) drawArc(look.skin.shade(0.78f), 90f, 120f, false, p(ex - 0.16f, 0.02f), Size(0.18f * r, 0.24f * r), style = Stroke(0.06f * r, cap = StrokeCap.Round))
    else drawArc(look.skin.shade(0.82f), 20f, 140f, false, p(-0.09f, 0.04f), Size(0.18f * r, 0.12f * r), style = Stroke(0.05f * r, cap = StrokeCap.Round))
    drawArc(
        if (look.beard) look.skin.mix(Color.White, 0.2f) else Color(0xFF7A3B2E), 15f, 150f, false,
        topLeft = p(ex - 0.22f, 0.22f), size = Size(0.42f * r, 0.3f * r),
        style = Stroke(0.08f * r, cap = StrokeCap.Round),
    )
    if (look.glasses) {
        val frame = Color(0xFF26262E)
        drawCircle(frame, 0.24f * r, p(ex - 0.34f, 0.02f), style = Stroke(0.07f * r))
        drawCircle(frame, 0.24f * r, p(ex + 0.3f, 0.02f), style = Stroke(0.07f * r))
        drawLine(frame, p(ex - 0.1f, -0.01f), p(ex + 0.06f, -0.01f), strokeWidth = 0.06f * r)
    }

    // Hair on top, trimmed to the skull, then the ear in front of it.
    if (!bald) {
        val cap = circle(p(if (portrait) 0f else 0.05f, -0.06f), 1.1f * r)
        clipPath(cap) { drawPath(hairRegion(c, r, look.style, facing, portrait).path, hairBrush) }
        if (look.style == HairStyle.CURLY) curls(c, r, hair, back = false)
        drawArc(Color.White.copy(alpha = 0.18f), 205f, 55f, false, p(-0.8f, -0.95f), Size(1.3f * r, 1.3f * r), style = Stroke(0.12f * r, cap = StrokeCap.Round))
    } else {
        drawArc(Color.White.copy(alpha = 0.3f), 200f, 60f, false, p(-0.7f, -0.8f), Size(1.2f * r, 1.2f * r), style = Stroke(0.12f * r, cap = StrokeCap.Round))
    }
    if (portrait) {
        drawOval(earColor, p(-1.1f, -0.15f), Size(0.24f * r, 0.36f * r))
        drawOval(earColor, p(0.86f, -0.15f), Size(0.24f * r, 0.36f * r))
    } else {
        drawOval(earColor, p(0.7f, -0.14f), Size(0.24f * r, 0.36f * r))
    }
}

private fun DrawScope.curls(c: Offset, r: Float, hair: Color, back: Boolean) {
    val from = if (back) 1.0 else 1.12
    val to = if (back) 2.0 else 1.88
    var a = Math.PI * from
    while (a <= Math.PI * to) {
        val off = Offset(c.x + (1.0f * r * cos(a)).toFloat(), c.y - 0.08f * r + (1.0f * r * sin(a)).toFloat())
        drawCircle(hair, 0.28f * r, off)
        a += Math.PI * 0.12
    }
}

internal fun DrawScope.drawCrown(bottom: Offset, s: Float) {
    val w = 0.12f * s
    val crown = Path().apply {
        moveTo(bottom.x - w, bottom.y)
        lineTo(bottom.x - w, bottom.y - 0.07f * s)
        lineTo(bottom.x - w * 0.45f, bottom.y - 0.035f * s)
        lineTo(bottom.x, bottom.y - 0.1f * s)
        lineTo(bottom.x + w * 0.45f, bottom.y - 0.035f * s)
        lineTo(bottom.x + w, bottom.y - 0.07f * s)
        lineTo(bottom.x + w, bottom.y)
        close()
    }
    drawPath(crown, Color(0xFFFFC53D))
    drawPath(crown, Color(0xFFB7791F), style = Stroke(0.008f * s))
    drawCircle(Color(0xFFFF5A5F), 0.014f * s, Offset(bottom.x, bottom.y - 0.03f * s))
}

/**
 * A portrait for lists: head and shoulders on a circle tinted with their business's colour, in
 * their own clothes, the same person as at their desk.
 */
@Composable
internal fun Avatar(look: Look, tint: Color, size: Dp = 44.dp, crown: Boolean = false, ring: Color? = null, top: Color? = null) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val c = Offset(d / 2, d / 2)
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, d / 2)) }
        drawCircle(Brush.verticalGradient(listOf(tint.mix(Color.White, 0.8f), tint.mix(Color.White, 0.58f))), d / 2, c)
        clipPath(circle) {
            val s = d * 1.25f
            val shirt = top ?: look.top
            val head = Offset(c.x, c.y - 0.05f * s)
            // Light falls from the upper left: a soft glow behind the head.
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), Offset(c.x - 0.12f * s, c.y - 0.2f * s), 0.4f * s), 0.4f * s, Offset(c.x - 0.12f * s, c.y - 0.2f * s))
            drawHairBehind(head, 0.19f * s, look, portrait = true)
            val body = Path().apply {
                moveTo(c.x - 0.36f * s, d + 2f)
                quadraticTo(c.x - 0.36f * s, c.y + 0.2f * s, c.x - 0.12f * s, c.y + 0.17f * s)
                lineTo(c.x + 0.12f * s, c.y + 0.17f * s)
                quadraticTo(c.x + 0.36f * s, c.y + 0.2f * s, c.x + 0.36f * s, d + 2f)
                close()
            }
            drawPath(body, Brush.horizontalGradient(listOf(shirt.mix(Color.White, 0.1f), shirt, shirt.shade(0.84f)), c.x - 0.36f * s, c.x + 0.36f * s))
            // Neck, with the jaw's shadow across the top.
            drawRect(look.skin.shade(0.9f), topLeft = Offset(c.x - 0.045f * s, c.y + 0.08f * s), size = Size(0.09f * s, 0.1f * s))
            drawOval(look.skin.shade(0.7f).copy(alpha = 0.55f), topLeft = Offset(c.x - 0.06f * s, c.y + 0.075f * s), size = Size(0.12f * s, 0.05f * s))
            val neck = c.y + 0.165f * s
            fun tri(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, color: Color) = drawPath(
                Path().apply { moveTo(c.x + x0 * s, y0); lineTo(c.x + x1 * s, y1); lineTo(c.x + x2 * s, y2); close() }, color,
            )
            when (look.outfit) {
                Outfit.HOODIE -> {
                    drawOval(shirt.shade(0.8f), topLeft = Offset(c.x - 0.13f * s, c.y + 0.14f * s), size = Size(0.26f * s, 0.07f * s))
                    val cord = Color.White.copy(alpha = 0.85f)
                    drawLine(cord, Offset(c.x - 0.05f * s, neck + 0.02f * s), Offset(c.x - 0.055f * s, neck + 0.14f * s), strokeWidth = 0.014f * s, cap = StrokeCap.Round)
                    drawLine(cord, Offset(c.x + 0.05f * s, neck + 0.02f * s), Offset(c.x + 0.055f * s, neck + 0.14f * s), strokeWidth = 0.014f * s, cap = StrokeCap.Round)
                }
                Outfit.TEE -> drawOval(look.skin.shade(0.9f), topLeft = Offset(c.x - 0.075f * s, neck - 0.025f * s), size = Size(0.15f * s, 0.06f * s))
                Outfit.SWEATER -> {
                    drawOval(shirt.shade(0.72f), topLeft = Offset(c.x - 0.1f * s, neck - 0.035f * s), size = Size(0.2f * s, 0.08f * s))
                    drawOval(look.skin.shade(0.9f), topLeft = Offset(c.x - 0.075f * s, neck - 0.025f * s), size = Size(0.15f * s, 0.05f * s))
                    for (k in -2..2) drawLine(shirt.shade(0.8f), Offset(c.x + (k * 0.12f - 0.02f) * s, d), Offset(c.x + (k * 0.12f - 0.02f) * s, d - 0.04f * s), strokeWidth = 0.01f * s)
                }
                Outfit.COLLAR, Outfit.BLAZER -> {
                    val inner = if (look.outfit == Outfit.BLAZER) Color(0xFFF4F4F2) else look.skin.shade(0.9f)
                    tri(-0.08f, neck, 0f, neck + (if (look.outfit == Outfit.BLAZER) 0.3f else 0.09f) * s, 0.08f, neck, inner)
                    if (look.outfit == Outfit.COLLAR) {
                        tri(-0.09f, neck - 0.01f * s, -0.01f, neck + 0.07f * s, -0.12f, neck + 0.06f * s, Color.White.copy(alpha = 0.92f))
                        tri(0.09f, neck - 0.01f * s, 0.01f, neck + 0.07f * s, 0.12f, neck + 0.06f * s, Color.White.copy(alpha = 0.92f))
                    } else {
                        // Lapels.
                        tri(-0.08f, neck, 0f, neck + 0.3f * s, -0.14f, neck + 0.16f * s, shirt.shade(0.8f))
                        tri(0.08f, neck, 0f, neck + 0.3f * s, 0.14f, neck + 0.16f * s, shirt.shade(0.72f))
                    }
                }
            }
            drawHead(head, 0.19f * s, look, Facing.FRONT, portrait = true)
            if (crown) drawCrown(Offset(c.x, c.y - 0.05f * s - 0.19f * s - 0.01f * s), s * 1.1f)
        }
        // A fine rim keeps light portraits off light cards.
        drawCircle(Color.Black.copy(alpha = 0.06f), d / 2 - 0.5f, c, style = Stroke(1f))
        if (ring != null) drawCircle(ring, d / 2 - 1.5f, c, style = Stroke(3f))
    }
}

/** A member of staff in their lane's colours. */
@Composable
internal fun StaffPortrait(e: Employee, size: Dp = 44.dp) {
    Avatar(lookFor(e.id), e.lane?.let { gameColors.lane(it) } ?: Color(0xFF9AA0A6), size)
}

/** A candidate, who looks the same once hired. */
@Composable
internal fun CandidatePortrait(id: Int, size: Dp = 44.dp, ring: Color? = null) {
    Avatar(lookFor(id), Color(0xFF9AA0A6), size, ring = ring)
}

/** You, crowned. */
@Composable
internal fun FounderPortrait(state: GameState, size: Dp = 44.dp) {
    Avatar(state.founderLook(), MaterialTheme.colorScheme.primary, size, crown = true, top = MaterialTheme.colorScheme.primary)
}
