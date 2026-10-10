package com.boot.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boot.core.Audience
import com.boot.core.GameConcept
import com.boot.core.Platform
import com.boot.core.Topic
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/*
 * Box art for every game: a scene that fits the topic, the platform's colours across the top, the
 * title, and a rating in the corner. Each title gets its own variation, so two games about the
 * same thing still look different on the shelf.
 */

private enum class Motif { CASTLE, SPACE, CITY, ROAD, STADIUM, SPOOKY, OCEAN, COZY, VOLCANO, ROBOT, DESERT, WAR }

private val Topic.motif: Motif
    get() = when (this) {
        Topic.FANTASY, Topic.MEDIEVAL, Topic.MYTHOLOGY, Topic.NINJA -> Motif.CASTLE
        Topic.SCI_FI, Topic.SPACE, Topic.ALIENS, Topic.MARS, Topic.TIME_TRAVEL -> Motif.SPACE
        Topic.CITY, Topic.BUSINESS, Topic.SPY, Topic.DETECTIVE, Topic.HACKING, Topic.CYBERPUNK, Topic.SUPERHEROES -> Motif.CITY
        Topic.RACING -> Motif.ROAD
        Topic.SPORTS -> Motif.STADIUM
        Topic.HORROR, Topic.ZOMBIES, Topic.VAMPIRES, Topic.POST_APOCALYPTIC, Topic.MYSTERY -> Motif.SPOOKY
        Topic.OCEAN, Topic.PIRATES -> Motif.OCEAN
        Topic.DINOSAURS -> Motif.VOLCANO
        Topic.ROBOTS, Topic.ARTIFICIAL_INTELLIGENCE -> Motif.ROBOT
        Topic.WILD_WEST -> Motif.DESERT
        Topic.MILITARY -> Motif.WAR
        else -> Motif.COZY
    }

private fun platformColor(p: Platform): Color = when (p.maker) {
    "Sony" -> Color(0xFF0070D1)
    "Microsoft" -> Color(0xFF107C10)
    "Nintendo" -> Color(0xFFE60012)
    "Meta" -> Color(0xFF0866FF)
    "Amazon" -> Color(0xFF5A31F4)
    "Apple", "Apple and Google" -> Color(0xFF2C2C2E)
    "You" -> Color(0xFFFF9F0A)
    else -> Color(0xFF1B2838)
}

private val Audience.rating: String
    get() = when (this) {
        Audience.YOUNG -> "E"
        Audience.EVERYONE -> "T"
        Audience.MATURE -> "M"
    }

/** Box art for [concept]. Small covers drop the text and keep the picture. */
@Composable
fun GameCover(concept: GameConcept, width: Dp, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val detailed = width >= 72.dp
    val seed = remember(concept.title) { concept.title.hashCode() }
    Canvas(
        modifier
            .size(width, width * 4 / 3)
            .shadow(if (detailed) 6.dp else 2.dp, RoundedCornerShape(width * 0.07f))
            .clip(RoundedCornerShape(width * 0.07f)),
    ) {
        val w = size.width
        val h = size.height
        val band = h * 0.1f
        clipRect(top = band) { drawMotif(concept.topic, w, h, band, seed) }

        // The platform band across the top.
        val platform = concept.platforms.firstOrNull() ?: Platform.PC
        drawRect(platformColor(platform), size = Size(w, band))
        drawRect(Color.White.copy(alpha = 0.18f), topLeft = Offset(0f, band - 1.5f), size = Size(w, 1.5f))
        if (detailed) {
            val label = measurer.measure(
                platform.title.uppercase(Locale.US),
                TextStyle(fontSize = (band * 0.42f).toSp(), fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.sp),
                maxLines = 1, overflow = TextOverflow.Clip, constraints = Constraints(maxWidth = (w * 0.9f).toInt()),
            )
            drawText(label, topLeft = Offset(w * 0.06f, (band - label.size.height) / 2))
        }

        // A scrim for the title.
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)), startY = h * 0.52f, endY = h),
            topLeft = Offset(0f, h * 0.52f), size = Size(w, h * 0.48f),
        )
        if (detailed) {
            var fs = w * 0.13f
            var layout = measurer.measure(
                concept.title,
                TextStyle(
                    fontSize = fs.toSp(), fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Start,
                    lineHeight = (fs * 1.02f).toSp(), shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f),
                ),
                maxLines = 3, constraints = Constraints(maxWidth = (w * 0.88f).toInt()),
            )
            while ((layout.hasVisualOverflow || layout.lineCount > 2) && fs > w * 0.07f) {
                fs *= 0.88f
                layout = measurer.measure(
                    concept.title,
                    TextStyle(
                        fontSize = fs.toSp(), fontWeight = FontWeight.Black, color = Color.White,
                        lineHeight = (fs * 1.02f).toSp(), shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f),
                    ),
                    maxLines = 3, constraints = Constraints(maxWidth = (w * 0.88f).toInt()),
                )
            }
            val genre = measurer.measure(
                concept.genre.title.uppercase(Locale.US) + (concept.genre2?.let { " · " + it.title.uppercase(Locale.US) } ?: ""),
                TextStyle(fontSize = (w * 0.05f).toSp(), fontWeight = FontWeight.Bold, color = Color(0xFFFFD27A), letterSpacing = 1.sp),
                maxLines = 1,
            )
            val ratingBox = w * 0.11f
            val titleTop = h - w * 0.06f - ratingBox - w * 0.04f - layout.size.height
            drawText(genre, topLeft = Offset(w * 0.06f, titleTop - genre.size.height))
            drawText(layout, topLeft = Offset(w * 0.06f, titleTop))
            // Rating box.
            val rx = w * 0.06f
            val ry = h - w * 0.06f - ratingBox
            drawRect(Color.White, Offset(rx, ry), Size(ratingBox * 0.8f, ratingBox))
            drawRect(Color.Black, Offset(rx + 1.5f, ry + 1.5f), Size(ratingBox * 0.8f - 3f, ratingBox * 0.72f))
            val r = measurer.measure(concept.audience.rating, TextStyle(fontSize = (ratingBox * 0.5f).toSp(), fontWeight = FontWeight.Black, color = Color.White))
            drawText(r, topLeft = Offset(rx + (ratingBox * 0.8f - r.size.width) / 2, ry + (ratingBox * 0.72f - r.size.height) / 2))
        }
        // A sheen along the spine side.
        drawRect(
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), startX = 0f, endX = w * 0.12f),
            size = Size(w * 0.12f, h),
        )
    }
}

/** A thumbnail cover with the review score in the corner. */
@Composable
fun CoverWithScore(concept: GameConcept, score: Double, width: Dp = 44.dp) {
    val colors = gameColors
    val color = when {
        score >= 8 -> colors.good
        score >= 6 -> MaterialTheme.colorScheme.primary
        score >= 5 -> colors.warn
        else -> colors.bad
    }
    Box(Modifier.size(width + 6.dp, width * 4 / 3 + 4.dp)) {
        GameCover(concept, width)
        Text(
            String.format(Locale.US, if (score >= 9.95 || score % 1.0 == 0.0) "%.0f" else "%.1f", score),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(color, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

private fun DrawScope.sky(top: Color, bottom: Color, y0: Float, y1: Float) =
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = y0, endY = y1), topLeft = Offset(0f, y0), size = Size(size.width, y1 - y0))

private fun DrawScope.poly(color: Color, pts: List<Offset>) {
    val p = Path()
    pts.forEachIndexed { i, o -> if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }
    p.close()
    drawPath(p, color)
}

private fun DrawScope.stars(rng: java.util.Random, n: Int, y0: Float, y1: Float, alpha: Float = 0.8f) {
    repeat(n) {
        drawCircle(Color.White.copy(alpha = alpha * (0.4f + 0.6f * rng.nextFloat())), size.width * (0.004f + 0.006f * rng.nextFloat()), Offset(size.width * rng.nextFloat(), y0 + (y1 - y0) * rng.nextFloat()))
    }
}

private fun DrawScope.drawMotif(topic: Topic, w: Float, h: Float, top: Float, seed: Int) {
    val rng = java.util.Random(seed.toLong() * 31 + topic.ordinal)
    val v = rng.nextFloat()
    val horizon = h * (0.58f + 0.06f * v)
    when (topic.motif) {
        Motif.CASTLE -> {
            val night = topic == Topic.NINJA || v > 0.5f
            sky(if (night) Color(0xFF2B1B5A) else Color(0xFF5B3E96), if (night) Color(0xFFE0607E) else Color(0xFFFFB46B), top, horizon)
            drawCircle(Color(0xFFFFF1C9), w * 0.11f, Offset(w * (0.68f + 0.1f * v), h * 0.27f))
            if (night) stars(rng, 20, top, horizon * 0.8f)
            poly(Color(0xFF4A3470), listOf(Offset(0f, horizon), Offset(w * 0.25f, h * 0.38f), Offset(w * 0.5f, horizon), Offset(w * 0.75f, h * 0.42f), Offset(w, horizon), Offset(w, h), Offset(0f, h)))
            // The castle.
            val cx = w * (0.42f + 0.12f * v)
            val base = horizon + h * 0.04f
            val body = Color(0xFF241A3A)
            drawRect(body, Offset(cx - w * 0.18f, base - h * 0.16f), Size(w * 0.36f, h * 0.2f))
            for (k in -1..1) {
                val tx = cx + k * w * 0.16f
                val th = h * (if (k == 0) 0.3f else 0.22f)
                drawRect(body, Offset(tx - w * 0.045f, base - th), Size(w * 0.09f, th))
                poly(body, listOf(Offset(tx - w * 0.06f, base - th), Offset(tx, base - th - h * 0.08f), Offset(tx + w * 0.06f, base - th)))
                drawRect(Color(0xFFFFD27A), Offset(tx - w * 0.012f, base - th + h * 0.05f), Size(w * 0.024f, h * 0.035f))
            }
            poly(Color(0xFF1A1230), listOf(Offset(0f, h * 0.8f), Offset(w * 0.5f, h * 0.72f), Offset(w, h * 0.82f), Offset(w, h), Offset(0f, h)))
        }
        Motif.SPACE -> {
            sky(Color(0xFF060B26), Color(0xFF2A1450), top, h)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFB04BFF).copy(alpha = 0.35f), Color.Transparent), Offset(w * 0.3f, h * 0.45f), w * 0.5f), w * 0.5f, Offset(w * 0.3f, h * 0.45f))
            stars(rng, 40, top, h)
            val pc = Offset(w * (0.62f + 0.1f * v), h * 0.42f)
            val pr = w * 0.24f
            val planet = when (topic) {
                Topic.MARS -> listOf(Color(0xFFFF8A5B), Color(0xFF9C2F12))
                Topic.ALIENS -> listOf(Color(0xFF8CF5A0), Color(0xFF1E7A4A))
                else -> listOf(Color(0xFF7FD3FF), Color(0xFF2A4FB0))
            }
            drawCircle(Brush.radialGradient(planet, Offset(pc.x - pr * 0.4f, pc.y - pr * 0.4f), pr * 1.6f), pr, pc)
            drawOval(Color(0xFFFFE3A8).copy(alpha = 0.8f), Offset(pc.x - pr * 1.6f, pc.y - pr * 0.22f), Size(pr * 3.2f, pr * 0.44f), style = Stroke(w * 0.015f))
            // A ship.
            val sx = w * 0.25f
            val sy = h * 0.52f
            poly(Color(0xFFE6E9F2), listOf(Offset(sx, sy - h * 0.07f), Offset(sx + w * 0.06f, sy + h * 0.04f), Offset(sx - w * 0.06f, sy + h * 0.04f)))
            poly(Color(0xFFFF9F0A), listOf(Offset(sx - w * 0.03f, sy + h * 0.04f), Offset(sx, sy + h * 0.1f), Offset(sx + w * 0.03f, sy + h * 0.04f)))
        }
        Motif.CITY -> {
            val neon = topic == Topic.CYBERPUNK || topic == Topic.HACKING
            sky(if (neon) Color(0xFF14002E) else Color(0xFF1F2A5C), if (neon) Color(0xFFFF2E88) else Color(0xFFFF9E6B), top, horizon + h * 0.1f)
            drawCircle(Color(0xFFFFF1C9).copy(alpha = 0.9f), w * 0.09f, Offset(w * 0.75f, h * 0.24f))
            var x = -w * 0.05f
            var k = 0
            while (x < w) {
                val bw = w * (0.12f + 0.08f * rng.nextFloat())
                val bh = h * (0.22f + 0.32f * rng.nextFloat())
                val by = h * 0.9f - bh
                drawRect(if (neon) Color(0xFF1A0B33) else Color(0xFF151B3A).copy(alpha = 0.92f), Offset(x, by), Size(bw, bh + h * 0.2f))
                var wy = by + h * 0.03f
                while (wy < h * 0.88f) {
                    var wx = x + bw * 0.15f
                    while (wx < x + bw * 0.8f) {
                        if (rng.nextFloat() > 0.45f) drawRect(if (neon) listOf(Color(0xFF00F0FF), Color(0xFFFF2E88))[k % 2] else Color(0xFFFFD27A), Offset(wx, wy), Size(bw * 0.14f, h * 0.018f))
                        wx += bw * 0.25f
                    }
                    wy += h * 0.04f
                }
                x += bw + w * 0.01f
                k++
            }
            if (topic == Topic.SUPERHEROES) {
                drawCircle(Color(0xFFFFE066).copy(alpha = 0.3f), w * 0.2f, Offset(w * 0.3f, h * 0.3f))
                poly(Color(0xFFFFE066), listOf(Offset(w * 0.24f, h * 0.24f), Offset(w * 0.36f, h * 0.24f), Offset(w * 0.3f, h * 0.36f)))
            }
        }
        Motif.ROAD -> {
            sky(Color(0xFF2B0F54), Color(0xFFFF6A5C), top, horizon)
            val sun = Offset(w / 2, horizon - h * 0.02f)
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE66D), Color(0xFFFF4E8A)), sun.y - w * 0.25f, sun.y), w * 0.25f, sun)
            for (k in 0..3) drawRect(Color(0xFFFF6A5C), Offset(0f, sun.y - h * (0.02f + k * 0.035f)), Size(w, h * (0.006f + k * 0.002f)))
            drawRect(Color(0xFF16082E), Offset(0f, horizon), Size(w, h - horizon))
            for (k in 1..6) {
                val y = horizon + (h - horizon) * (k / 6f) * (k / 6f)
                drawLine(Color(0xFFFF2E88).copy(alpha = 0.6f), Offset(0f, y), Offset(w, y), strokeWidth = w * 0.006f)
            }
            poly(Color(0xFF2E2E3A), listOf(Offset(w * 0.47f, horizon), Offset(w * 0.53f, horizon), Offset(w * 0.95f, h), Offset(w * 0.05f, h)))
            for (k in 0..4) {
                val f0 = 0.1f + k * 0.2f
                val y0 = horizon + (h - horizon) * f0 * f0
                val y1 = horizon + (h - horizon) * (f0 + 0.08f) * (f0 + 0.08f)
                drawLine(Color.White, Offset(w / 2, y0), Offset(w / 2, y1), strokeWidth = w * (0.006f + 0.02f * f0))
            }
            // A car.
            val cy = h * 0.86f
            drawRoundRect(Color(0xFFFF3B30), Offset(w * 0.36f, cy - h * 0.06f), Size(w * 0.28f, h * 0.07f), CornerRadius(w * 0.03f))
            drawRoundRect(Color(0xFFB71C1C), Offset(w * 0.41f, cy - h * 0.1f), Size(w * 0.18f, h * 0.05f), CornerRadius(w * 0.03f))
            drawCircle(Color(0xFFFFE66D), w * 0.02f, Offset(w * 0.39f, cy - h * 0.03f))
            drawCircle(Color(0xFFFFE66D), w * 0.02f, Offset(w * 0.61f, cy - h * 0.03f))
        }
        Motif.STADIUM -> {
            sky(Color(0xFF1565C0), Color(0xFF90CAF9), top, horizon)
            for (side in listOf(0.12f, 0.88f)) {
                drawLine(Color(0xFF37474F), Offset(w * side, horizon), Offset(w * side, h * 0.2f), strokeWidth = w * 0.02f)
                drawRect(Color(0xFFFFFDE7), Offset(w * side - w * 0.06f, h * 0.17f), Size(w * 0.12f, h * 0.04f))
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent), Offset(w * side, h * 0.19f), w * 0.2f), w * 0.2f, Offset(w * side, h * 0.19f))
            }
            drawRect(Color(0xFF455A64), Offset(0f, horizon - h * 0.06f), Size(w, h * 0.06f))
            stars(rng, 30, horizon - h * 0.06f, horizon, 0.6f)
            poly(Color(0xFF2E7D32), listOf(Offset(w * 0.1f, horizon), Offset(w * 0.9f, horizon), Offset(w * 1.2f, h), Offset(-w * 0.2f, h)))
            for (k in 0..3) {
                val y = horizon + (h - horizon) * k / 4f
                poly(Color(0xFF388E3C), listOf(Offset(0f, y), Offset(w, y), Offset(w, y + (h - horizon) / 8f), Offset(0f, y + (h - horizon) / 8f)))
            }
            drawLine(Color.White.copy(alpha = 0.8f), Offset(w / 2, horizon), Offset(w / 2, h), strokeWidth = w * 0.01f)
            val bc = Offset(w * (0.5f + 0.1f * (v - 0.5f)), h * 0.5f)
            val br = w * 0.12f
            drawCircle(Color.White, br, bc)
            for (k in 0..4) {
                val a = k * 1.2566f + v
                drawCircle(Color(0xFF212121), br * 0.28f, Offset(bc.x + cos(a) * br * 0.62f, bc.y + sin(a) * br * 0.62f))
            }
            drawCircle(Color(0xFF212121), br * 0.3f, bc)
        }
        Motif.SPOOKY -> {
            sky(Color(0xFF0B1A1E), Color(0xFF3B4E3E).let { if (topic == Topic.VAMPIRES) Color(0xFF5A1020) else it }, top, horizon)
            drawCircle(Color(0xFFF5F0C8), w * 0.17f, Offset(w * 0.66f, h * 0.32f))
            drawCircle(Color(0xFFF5F0C8).copy(alpha = 0.15f), w * 0.28f, Offset(w * 0.66f, h * 0.32f))
            drawRect(Color(0xFF0A0F0F), Offset(0f, horizon), Size(w, h - horizon))
            // A bare tree.
            val tx = w * 0.25f
            val trunk = Color(0xFF050808)
            drawLine(trunk, Offset(tx, horizon + h * 0.02f), Offset(tx, h * 0.3f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
            for ((dx, dy) in listOf(-0.12f to 0.24f, 0.14f to 0.22f, -0.08f to 0.36f, 0.1f to 0.4f)) {
                drawLine(trunk, Offset(tx, h * (dy + 0.1f)), Offset(tx + w * dx, h * dy), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
            }
            for (k in 0..2) {
                val gx = w * (0.48f + k * 0.17f)
                drawRoundRect(Color(0xFF2E3436), Offset(gx, horizon - h * 0.06f), Size(w * 0.09f, h * 0.1f), CornerRadius(w * 0.045f, w * 0.045f))
            }
            for (k in 0..2) drawRect(Color.White.copy(alpha = 0.06f + 0.03f * k), Offset(0f, horizon - h * (0.02f - k * 0.04f)), Size(w, h * 0.03f))
            if (topic == Topic.ZOMBIES || topic == Topic.POST_APOCALYPTIC) {
                drawCircle(Color(0xFF7CB342), w * 0.05f, Offset(w * 0.82f, horizon + h * 0.06f))
                drawRect(Color(0xFF7CB342), Offset(w * 0.8f, horizon + h * 0.1f), Size(w * 0.04f, h * 0.1f))
            }
        }
        Motif.OCEAN -> {
            sky(Color(0xFF4FC3F7), Color(0xFFFFF3E0), top, horizon)
            drawCircle(Color(0xFFFFE082), w * 0.12f, Offset(w * 0.75f, h * 0.28f))
            for (k in 0..4) {
                val y = horizon + k * h * 0.08f
                val p = Path().apply {
                    moveTo(0f, y)
                    var x = 0f
                    while (x <= w) {
                        quadraticTo(x + w * 0.06f, y - h * 0.02f, x + w * 0.12f, y)
                        x += w * 0.12f
                    }
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(p, Color(0xFF0277BD).copy(alpha = 1f - k * 0.08f).let { if (k % 2 == 1) it.copy(red = 0.0f, green = 0.45f, blue = 0.7f) else it })
            }
            if (topic == Topic.PIRATES) {
                val sx = w * 0.4f
                val sy = horizon + h * 0.02f
                poly(Color(0xFF5D4037), listOf(Offset(sx - w * 0.2f, sy - h * 0.04f), Offset(sx + w * 0.2f, sy - h * 0.04f), Offset(sx + w * 0.14f, sy + h * 0.03f), Offset(sx - w * 0.14f, sy + h * 0.03f)))
                drawLine(Color(0xFF3E2723), Offset(sx, sy - h * 0.04f), Offset(sx, sy - h * 0.3f), strokeWidth = w * 0.015f)
                poly(Color(0xFFFAFAFA), listOf(Offset(sx + w * 0.01f, sy - h * 0.28f), Offset(sx + w * 0.16f, sy - h * 0.12f), Offset(sx + w * 0.01f, sy - h * 0.08f)))
                drawRect(Color(0xFF212121), Offset(sx, sy - h * 0.32f), Size(w * 0.08f, h * 0.04f))
            } else {
                drawCircle(Color(0xFFFF7043), w * 0.06f, Offset(w * 0.3f, horizon + h * 0.15f))
                drawCircle(Color(0xFFFFCA28), w * 0.04f, Offset(w * 0.62f, horizon + h * 0.22f))
            }
        }
        Motif.COZY -> {
            sky(Color(0xFFB2EBF2).mix(Color(0xFFFFCDD2), v), Color(0xFFFFF8E1), top, horizon)
            val sun = Offset(w * 0.24f, h * 0.27f)
            for (k in 0 until 10) {
                val a = k * 0.628f
                drawLine(Color(0xFFFFCA28).copy(alpha = 0.6f), Offset(sun.x + cos(a) * w * 0.12f, sun.y + sin(a) * w * 0.12f), Offset(sun.x + cos(a) * w * 0.17f, sun.y + sin(a) * w * 0.17f), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
            }
            drawCircle(Color(0xFFFFCA28), w * 0.09f, sun)
            val hill = Path().apply {
                moveTo(0f, horizon)
                quadraticTo(w * 0.3f, horizon - h * 0.1f, w * 0.6f, horizon)
                quadraticTo(w * 0.85f, horizon + h * 0.06f, w, horizon - h * 0.04f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hill, Color(0xFF81C784))
            // A little house.
            val hx = w * 0.6f
            val hy = horizon - h * 0.01f
            drawRect(Color(0xFFFFF3E0), Offset(hx - w * 0.12f, hy - h * 0.12f), Size(w * 0.24f, h * 0.12f))
            poly(Color(0xFFE57373), listOf(Offset(hx - w * 0.15f, hy - h * 0.12f), Offset(hx, hy - h * 0.22f), Offset(hx + w * 0.15f, hy - h * 0.12f)))
            drawRect(Color(0xFF8D6E63), Offset(hx - w * 0.03f, hy - h * 0.07f), Size(w * 0.06f, h * 0.07f))
            drawCircle(Color(0xFF66BB6A), w * 0.09f, Offset(w * 0.2f, horizon + h * 0.02f))
            drawCircle(Color(0xFFE91E63).copy(alpha = 0.8f), w * 0.025f, Offset(w * 0.85f, horizon + h * 0.12f))
            drawCircle(Color(0xFFFFEB3B), w * 0.025f, Offset(w * 0.75f, horizon + h * 0.16f))
        }
        Motif.VOLCANO -> {
            sky(Color(0xFF3E1F47), Color(0xFFFF8F00), top, horizon)
            poly(Color(0xFF3E2723), listOf(Offset(w * 0.2f, horizon + h * 0.05f), Offset(w * 0.45f, h * 0.3f), Offset(w * 0.6f, h * 0.3f), Offset(w * 0.9f, horizon + h * 0.05f)))
            poly(Color(0xFFFF5722), listOf(Offset(w * 0.47f, h * 0.3f), Offset(w * 0.58f, h * 0.3f), Offset(w * 0.55f, h * 0.42f), Offset(w * 0.5f, h * 0.38f)))
            for (k in 0..3) drawCircle(Color(0xFF5D4037).copy(alpha = 0.6f), w * (0.07f + k * 0.02f), Offset(w * (0.52f + k * 0.04f), h * (0.24f - k * 0.05f)))
            drawRect(Color(0xFF1B5E20), Offset(0f, horizon + h * 0.04f), Size(w, h))
            for (k in 0..6) drawCircle(Color(0xFF2E7D32).shade(0.8f + 0.06f * (k % 3)), w * 0.1f, Offset(w * k / 6f, horizon + h * 0.06f))
            for (k in 0..1) {
                val bx = w * (0.2f + k * 0.18f)
                val by = h * (0.22f + k * 0.05f)
                drawLine(Color(0xFF1A1A1A), Offset(bx - w * 0.05f, by - h * 0.02f), Offset(bx, by), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
                drawLine(Color(0xFF1A1A1A), Offset(bx, by), Offset(bx + w * 0.05f, by - h * 0.02f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
            }
        }
        Motif.ROBOT -> {
            sky(Color(0xFF0D1B2A), Color(0xFF1B3A5C), top, h)
            for (k in 0..8) {
                val x = w * k / 8f
                drawLine(Color(0xFF00E5FF).copy(alpha = 0.12f), Offset(x, top), Offset(x, h), strokeWidth = 1.5f)
                drawLine(Color(0xFF00E5FF).copy(alpha = 0.12f), Offset(0f, top + (h - top) * k / 8f), Offset(w, top + (h - top) * k / 8f), strokeWidth = 1.5f)
            }
            val c = Offset(w / 2, h * 0.42f)
            drawLine(Color(0xFFB0BEC5), Offset(c.x, c.y - h * 0.16f), Offset(c.x, c.y - h * 0.24f), strokeWidth = w * 0.02f)
            drawCircle(Color(0xFFFF5252), w * 0.03f, Offset(c.x, c.y - h * 0.25f))
            drawRoundRect(Color(0xFFCFD8DC), Offset(c.x - w * 0.24f, c.y - h * 0.16f), Size(w * 0.48f, h * 0.27f), CornerRadius(w * 0.08f))
            drawRoundRect(Color(0xFF263238), Offset(c.x - w * 0.19f, c.y - h * 0.11f), Size(w * 0.38f, h * 0.14f), CornerRadius(w * 0.05f))
            val eye = if (topic == Topic.ARTIFICIAL_INTELLIGENCE) Color(0xFFFF4081) else Color(0xFF00E5FF)
            drawCircle(eye, w * 0.045f, Offset(c.x - w * 0.09f, c.y - h * 0.04f))
            drawCircle(eye, w * 0.045f, Offset(c.x + w * 0.09f, c.y - h * 0.04f))
            drawCircle(eye.copy(alpha = 0.25f), w * 0.1f, Offset(c.x - w * 0.09f, c.y - h * 0.04f))
            drawCircle(eye.copy(alpha = 0.25f), w * 0.1f, Offset(c.x + w * 0.09f, c.y - h * 0.04f))
        }
        Motif.DESERT -> {
            sky(Color(0xFFFF7043), Color(0xFFFFE0B2), top, horizon)
            drawCircle(Color(0xFFFFF59D), w * 0.16f, Offset(w * 0.5f, horizon - h * 0.08f))
            poly(Color(0xFFBF5B2C), listOf(Offset(0f, horizon), Offset(w * 0.05f, h * 0.42f), Offset(w * 0.3f, h * 0.42f), Offset(w * 0.34f, horizon)))
            poly(Color(0xFFA0461F), listOf(Offset(w * 0.66f, horizon), Offset(w * 0.72f, h * 0.46f), Offset(w * 0.95f, h * 0.46f), Offset(w, horizon)))
            drawRect(Color(0xFFE0A15E), Offset(0f, horizon), Size(w, h - horizon))
            val cx = w * 0.25f
            val cg = Color(0xFF2E7D32)
            drawRoundRect(cg, Offset(cx - w * 0.04f, horizon - h * 0.12f), Size(w * 0.08f, h * 0.28f), CornerRadius(w * 0.04f))
            drawRoundRect(cg, Offset(cx - w * 0.13f, horizon - h * 0.06f), Size(w * 0.06f, h * 0.12f), CornerRadius(w * 0.03f))
            drawRoundRect(cg, Offset(cx + w * 0.07f, horizon - h * 0.09f), Size(w * 0.06f, h * 0.12f), CornerRadius(w * 0.03f))
        }
        Motif.WAR -> {
            sky(Color(0xFF263238), Color(0xFF8D6E63), top, horizon)
            for (k in 0..1) {
                val bx = w * (0.25f + k * 0.5f)
                poly(Color.White.copy(alpha = 0.12f), listOf(Offset(bx, horizon), Offset(bx - w * 0.1f + k * w * 0.3f, top), Offset(bx + w * 0.05f + k * w * 0.3f, top)))
            }
            poly(Color(0xFF37474F), listOf(Offset(0f, horizon), Offset(w * 0.3f, horizon - h * 0.08f), Offset(w * 0.7f, horizon - h * 0.03f), Offset(w, horizon - h * 0.1f), Offset(w, h), Offset(0f, h)))
            drawRect(Color(0xFF263238), Offset(0f, horizon + h * 0.06f), Size(w, h))
            // A tank.
            val tx = w * 0.5f
            val ty = horizon + h * 0.06f
            drawRoundRect(Color(0xFF1C2529), Offset(tx - w * 0.22f, ty - h * 0.06f), Size(w * 0.44f, h * 0.07f), CornerRadius(w * 0.04f))
            drawRoundRect(Color(0xFF1C2529), Offset(tx - w * 0.1f, ty - h * 0.11f), Size(w * 0.2f, h * 0.06f), CornerRadius(w * 0.03f))
            drawLine(Color(0xFF1C2529), Offset(tx + w * 0.05f, ty - h * 0.085f), Offset(tx + w * 0.3f, ty - h * 0.1f), strokeWidth = w * 0.02f)
        }
    }
}
