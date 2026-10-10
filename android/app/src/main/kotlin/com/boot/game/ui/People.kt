package com.boot.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.boot.core.Employee
import com.boot.core.GameState

/*
 * The people of the company, drawn the same way everywhere: seated at their desks in the office,
 * and as portraits on the team page. How someone looks comes from their id, so a candidate looks
 * the same once hired.
 */

internal enum class HairStyle { SHORT, SWEPT, LONG, BUN, CURLY, BALD, PONYTAIL, BOB }

internal data class Look(
    val skin: Color,
    val hair: Color,
    val style: HairStyle,
    val glasses: Boolean,
    val beard: Boolean,
    val hoodie: Boolean,
)

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
        hoodie = r.nextInt(3) == 0,
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

private const val HEAD_Y = -0.6f
private const val HEAD_R = 0.15f

/**
 * Someone sitting down, drawn around their hips at [hip]. [s] is the size of the figure: the top of
 * the head is about 0.77 [s] above the hips. [phase] animates typing while the clock runs.
 */
internal fun DrawScope.drawSeated(hip: Offset, s: Float, look: Look, shirt: Color, facing: Facing, phase: Float?, crown: Boolean) {
    val wave = phase?.let { kotlin.math.sin(2 * Math.PI.toFloat() * it) } ?: 0f
    val bob = wave * 0.006f
    fun o(x: Float, y: Float) = Offset(hip.x + x * s, hip.y + y * s)
    val shirtDark = shirt.shade(0.82f)
    val skinDark = look.skin.shade(0.9f)

    if (facing == Facing.BACK) {
        // Arms reach past the body to a keyboard ahead and to the right.
        val tap = wave * 0.015f
        drawLine(shirtDark, o(0.13f, -0.37f), o(0.27f, -0.33f + tap), strokeWidth = 0.085f * s, cap = StrokeCap.Round)
        drawCircle(look.skin, 0.035f * s, o(0.3f, -0.34f + tap))
    }

    // Torso: a soft trapezoid, lit from the left.
    val torso = Path().apply {
        moveTo(hip.x - 0.15f * s, hip.y)
        lineTo(hip.x + 0.15f * s, hip.y)
        quadraticTo(hip.x + 0.2f * s, hip.y - 0.24f * s, hip.x + 0.17f * s, hip.y + (-0.42f + bob) * s)
        quadraticTo(hip.x, hip.y + (-0.49f + bob) * s, hip.x - 0.17f * s, hip.y + (-0.42f + bob) * s)
        quadraticTo(hip.x - 0.2f * s, hip.y - 0.24f * s, hip.x - 0.15f * s, hip.y)
        close()
    }
    drawPath(torso, shirt)
    clipPath(torso) {
        drawRect(shirtDark, topLeft = o(0.05f, -0.6f), size = Size(0.3f * s, 0.7f * s))
    }
    if (look.hoodie) {
        // Hood bunched behind the neck and drawstrings at the front.
        drawOval(shirtDark, topLeft = o(-0.11f, -0.5f + bob), size = Size(0.22f * s, 0.09f * s))
        if (facing == Facing.FRONT) {
            drawLine(Color.White.copy(alpha = 0.8f), o(-0.05f, -0.4f + bob), o(-0.055f, -0.3f + bob), strokeWidth = 0.012f * s)
            drawLine(Color.White.copy(alpha = 0.8f), o(0.02f, -0.4f + bob), o(0.025f, -0.3f + bob), strokeWidth = 0.012f * s)
        }
    } else if (facing == Facing.FRONT) {
        // A collar.
        val v = Path().apply {
            moveTo(hip.x - 0.06f * s, hip.y + (-0.45f + bob) * s)
            lineTo(hip.x - 0.01f * s, hip.y + (-0.37f + bob) * s)
            lineTo(hip.x + 0.04f * s, hip.y + (-0.45f + bob) * s)
            close()
        }
        drawPath(v, skinDark)
    }

    if (facing == Facing.FRONT) {
        // Arms down to the desk in front.
        drawLine(shirt, o(-0.15f, -0.38f + bob), o(-0.25f, -0.17f), strokeWidth = 0.085f * s, cap = StrokeCap.Round)
        drawLine(shirtDark, o(0.15f, -0.38f + bob), o(0.1f, -0.16f), strokeWidth = 0.085f * s, cap = StrokeCap.Round)
    }

    // Neck and head.
    drawRect(skinDark, topLeft = o(-0.04f, -0.5f + bob), size = Size(0.08f * s, 0.07f * s))
    drawHead(o(0f, HEAD_Y + bob), HEAD_R * s, look, facing)
    if (crown) drawCrown(o(0f, HEAD_Y + bob - HEAD_R - 0.02f), s)
}

/** A head of radius [r] centred at [c]. Facing the viewer it's turned slightly to the left. */
internal fun DrawScope.drawHead(c: Offset, r: Float, look: Look, facing: Facing, portrait: Boolean = false) {
    val hair = look.hair
    val turn = if (portrait) 0f else -0.18f * r
    // Hair that falls behind the head.
    when (look.style) {
        HairStyle.LONG -> drawRoundRect(hair, Offset(c.x - 1.05f * r, c.y - 0.6f * r), Size(2.1f * r, 2.0f * r), CornerRadius(0.8f * r))
        HairStyle.BOB -> drawRoundRect(hair, Offset(c.x - 1.08f * r, c.y - 0.7f * r), Size(2.16f * r, 1.45f * r), CornerRadius(0.7f * r))
        HairStyle.BUN -> drawCircle(hair, 0.45f * r, Offset(c.x + (if (facing == Facing.BACK) 0f else 0.15f * r), c.y - 1.05f * r))
        HairStyle.PONYTAIL -> if (facing == Facing.BACK) {
            drawRoundRect(hair, Offset(c.x - 0.22f * r, c.y), Size(0.44f * r, 1.5f * r), CornerRadius(0.22f * r))
        } else {
            drawCircle(hair, 0.42f * r, Offset(c.x + 1.0f * r, c.y + 0.1f * r))
        }
        else -> {}
    }
    // Ears.
    if (facing == Facing.FRONT) {
        drawCircle(look.skin.shade(0.92f), 0.2f * r, Offset(c.x + 0.92f * r + turn * 0.5f, c.y + 0.08f * r))
        if (portrait) drawCircle(look.skin.shade(0.92f), 0.2f * r, Offset(c.x - 0.92f * r, c.y + 0.08f * r))
    }
    drawCircle(look.skin, r, c)
    // Shade the far side of the face.
    if (!portrait) {
        clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }) {
            drawRect(look.skin.shade(0.93f), topLeft = Offset(c.x + 0.45f * r, c.y - r), size = Size(r, 2 * r))
        }
    }

    val bald = look.style == HairStyle.BALD
    val cap = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x, c.y - 0.04f * r), 1.07f * r)) }
    if (facing == Facing.BACK) {
        // From behind: hair over nearly all of it, an ear and a sliver of cheek on the right.
        if (!bald) {
            clipPath(cap) {
                drawRect(hair, topLeft = Offset(c.x - 1.2f * r, c.y - 1.2f * r), size = Size(1.95f * r, 2.4f * r))
                drawRect(hair, topLeft = Offset(c.x - 1.2f * r, c.y - 1.2f * r), size = Size(2.4f * r, 0.85f * r))
            }
        } else {
            drawCircle(look.hair.copy(alpha = 0.5f), 0.9f * r, Offset(c.x - 0.1f * r, c.y + 0.2f * r), style = Stroke(0.12f * r))
        }
        drawCircle(look.skin.shade(0.92f), 0.2f * r, Offset(c.x + 0.78f * r, c.y + 0.12f * r))
        if (look.style == HairStyle.LONG) {
            drawRoundRect(hair, Offset(c.x - 1.0f * r, c.y), Size(2.0f * r, 1.3f * r), CornerRadius(0.5f * r))
        }
        return
    }

    // Face.
    val eye = Color(0xFF231A16)
    val ex = c.x + turn
    drawCircle(eye, 0.11f * r, Offset(ex - 0.36f * r, c.y + 0.05f * r))
    drawCircle(eye, 0.11f * r, Offset(ex + 0.3f * r, c.y + 0.05f * r))
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.35f), 0.17f * r, Offset(ex - 0.5f * r, c.y + 0.38f * r))
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.35f), 0.17f * r, Offset(ex + 0.45f * r, c.y + 0.38f * r))
    drawArc(
        eye, 20f, 140f, false,
        topLeft = Offset(ex - 0.24f * r, c.y + 0.2f * r), size = Size(0.42f * r, 0.32f * r),
        style = Stroke(0.08f * r, cap = StrokeCap.Round),
    )
    if (look.beard) {
        clipPath(Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r * 1.02f)) }) {
            drawRect(hair, topLeft = Offset(c.x - r, c.y + 0.32f * r), size = Size(2 * r, r))
        }
        drawArc(
            look.skin.shade(0.8f), 20f, 140f, false,
            topLeft = Offset(ex - 0.2f * r, c.y + 0.28f * r), size = Size(0.34f * r, 0.2f * r),
            style = Stroke(0.08f * r, cap = StrokeCap.Round),
        )
    }
    if (look.glasses) {
        val frame = Color(0xFF26262E)
        drawCircle(frame, 0.24f * r, Offset(ex - 0.36f * r, c.y + 0.05f * r), style = Stroke(0.07f * r))
        drawCircle(frame, 0.24f * r, Offset(ex + 0.3f * r, c.y + 0.05f * r), style = Stroke(0.07f * r))
        drawLine(frame, Offset(ex - 0.12f * r, c.y + 0.02f * r), Offset(ex + 0.06f * r, c.y + 0.02f * r), strokeWidth = 0.06f * r)
    }

    // Hair on top.
    if (bald) return
    clipPath(cap) {
        val line = c.y - (if (look.style == HairStyle.SWEPT) 0.2f else 0.32f) * r
        drawRect(hair, topLeft = Offset(c.x - 1.2f * r, c.y - 1.2f * r), size = Size(2.4f * r, line - (c.y - 1.2f * r)))
        if (!portrait) drawRect(hair, topLeft = Offset(c.x + 0.62f * r, c.y - 1.2f * r), size = Size(0.6f * r, 1.25f * r))
        if (look.style == HairStyle.SWEPT) {
            val swoop = Path().apply {
                moveTo(c.x - 1.1f * r, line)
                quadraticTo(c.x - 0.2f * r, line + 0.45f * r, c.x + 0.3f * r, line)
                close()
            }
            drawPath(swoop, hair)
        }
        if (look.style == HairStyle.LONG || look.style == HairStyle.BOB) {
            drawRect(hair, topLeft = Offset(c.x - 1.2f * r, c.y - 1.2f * r), size = Size(0.42f * r, 2.0f * r))
            drawRect(hair, topLeft = Offset(c.x + 0.78f * r, c.y - 1.2f * r), size = Size(0.42f * r, 2.0f * r))
        }
    }
    if (look.style == HairStyle.CURLY) {
        for (k in 0..6) {
            val a = Math.PI * (0.95 + k * 0.183)
            drawCircle(hair, 0.3f * r, Offset(c.x + (1.0f * r * kotlin.math.cos(a)).toFloat(), c.y - 0.1f * r + (1.0f * r * kotlin.math.sin(a)).toFloat()))
        }
    }
    // A highlight on the hair.
    drawArc(
        Color.White.copy(alpha = 0.18f), 200f, 50f, false,
        topLeft = Offset(c.x - 0.8f * r, c.y - 0.95f * r), size = Size(1.3f * r, 1.3f * r),
        style = Stroke(0.12f * r, cap = StrokeCap.Round),
    )
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

/** A portrait for lists: head and shoulders on a tinted circle, the same person as at their desk. */
@Composable
internal fun Avatar(look: Look, shirt: Color, size: Dp = 44.dp, crown: Boolean = false, ring: Color? = null) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val c = Offset(d / 2, d / 2)
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, d / 2)) }
        drawCircle(Brush.verticalGradient(listOf(shirt.mix(Color.White, 0.82f), shirt.mix(Color.White, 0.62f))), d / 2, c)
        clipPath(circle) {
            val s = d * 1.25f
            val body = Path().apply {
                moveTo(c.x - 0.36f * s, d + 2f)
                quadraticTo(c.x - 0.36f * s, c.y + 0.2f * s, c.x - 0.12f * s, c.y + 0.17f * s)
                lineTo(c.x + 0.12f * s, c.y + 0.17f * s)
                quadraticTo(c.x + 0.36f * s, c.y + 0.2f * s, c.x + 0.36f * s, d + 2f)
                close()
            }
            drawPath(body, shirt)
            clipRect(left = c.x + 0.05f * s) { drawPath(body, shirt.shade(0.88f)) }
            drawRect(look.skin.shade(0.9f), topLeft = Offset(c.x - 0.045f * s, c.y + 0.08f * s), size = Size(0.09f * s, 0.1f * s))
            if (!look.hoodie) {
                val v = Path().apply {
                    moveTo(c.x - 0.07f * s, c.y + 0.165f * s)
                    lineTo(c.x, c.y + 0.24f * s)
                    lineTo(c.x + 0.07f * s, c.y + 0.165f * s)
                    close()
                }
                drawPath(v, look.skin.shade(0.9f))
            } else {
                drawOval(shirt.shade(0.8f), topLeft = Offset(c.x - 0.13f * s, c.y + 0.14f * s), size = Size(0.26f * s, 0.07f * s))
            }
            drawHead(Offset(c.x, c.y - 0.05f * s), 0.19f * s, look, Facing.FRONT, portrait = true)
            if (crown) drawCrown(Offset(c.x, c.y - 0.05f * s - 0.19f * s - 0.01f * s), s * 1.1f)
        }
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
    Avatar(state.founderLook(), MaterialTheme.colorScheme.primary, size, crown = true)
}
