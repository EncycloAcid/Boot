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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boot.core.Audience
import com.boot.core.GameConcept
import com.boot.core.Genre
import com.boot.core.Platform
import com.boot.core.Topic
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Box art for every game, painted in layers like real covers: a sky and a scene of its own for
 * every topic, a hero in front that fits the genre and is rim-lit by the scene's light,
 * atmosphere (light rays, embers, rain, fireflies, bubbles, confetti), and a title set as a logo
 * with the sequel number picked out. Across the top runs the platform's band; at the bottom sit
 * the rating and the studio. The title seeds the variations, so every game gets its own cover.
 */

private enum class Scene {
    CASTLE, DRAGON, TEMPLE, PAGODA, PLANET, UFO, MARS, VORTEX, CITY, NEON, NOIR, HERO_CITY, ROAD, STADIUM,
    GRAVEYARD, GOTHIC, RUINS, REEF, SHIP, FARM, HOUSE, STAGE, JUNGLE, MECH, NETWORK, DESERT, WAR, RUNWAY, KITCHEN, SCHOOL, CLINIC, PETS, TOWER,
}

private enum class Fx { EMBERS, STARS, RAIN, FIREFLIES, BUBBLES, CONFETTI, SPARKLES, NONE }

/** A topic's look: its sky, ground, the colour of its light, its scene and its atmosphere. */
private class Art(sky: Long, horizon: Long, ground: Long, light: Long, val scene: Scene, val fx: Fx) {
    val sky = Color(sky)
    val horizon = Color(horizon)
    val ground = Color(ground)
    val light = Color(light)
}

private val Topic.art: Art
    get() = when (this) {
        Topic.FANTASY -> Art(0xFF2A1B5C, 0xFFF08A5D, 0xFF1C1236, 0xFFFFC86B, Scene.DRAGON, Fx.EMBERS)
        Topic.MEDIEVAL -> Art(0xFF3E2D6E, 0xFFF4B26B, 0xFF241A33, 0xFFFFD58A, Scene.CASTLE, Fx.EMBERS)
        Topic.MYTHOLOGY -> Art(0xFF14254F, 0xFFE6C27A, 0xFF1B1A2E, 0xFFFFE9A8, Scene.TEMPLE, Fx.SPARKLES)
        Topic.NINJA -> Art(0xFF0D1633, 0xFFB23A48, 0xFF0A0D18, 0xFFFF6B6B, Scene.PAGODA, Fx.NONE)
        Topic.SCI_FI -> Art(0xFF050A26, 0xFF3A1C71, 0xFF0B0F24, 0xFF6FE3FF, Scene.PLANET, Fx.STARS)
        Topic.SPACE -> Art(0xFF02040F, 0xFF1C2A6B, 0xFF070A18, 0xFF9AD4FF, Scene.PLANET, Fx.STARS)
        Topic.ALIENS -> Art(0xFF06141A, 0xFF1F5E4A, 0xFF07110E, 0xFF8CFFB0, Scene.UFO, Fx.STARS)
        Topic.MARS -> Art(0xFF2B0F12, 0xFFE07A4B, 0xFF5A2318, 0xFFFFB27A, Scene.MARS, Fx.NONE)
        Topic.TIME_TRAVEL -> Art(0xFF120A30, 0xFF4B2C8F, 0xFF0C0820, 0xFF7FF0FF, Scene.VORTEX, Fx.SPARKLES)
        Topic.CITY -> Art(0xFF2E3A7A, 0xFFFFA77A, 0xFF1A1E3A, 0xFFFFD27A, Scene.CITY, Fx.NONE)
        Topic.BUSINESS -> Art(0xFF0F2742, 0xFF5B8DB8, 0xFF0B1B2E, 0xFF7CF0B5, Scene.TOWER, Fx.SPARKLES)
        Topic.CYBERPUNK -> Art(0xFF14002E, 0xFFFF2E88, 0xFF0A0118, 0xFF00F0FF, Scene.NEON, Fx.RAIN)
        Topic.HACKING -> Art(0xFF020A05, 0xFF0B3B1E, 0xFF010603, 0xFF39FF88, Scene.NETWORK, Fx.RAIN)
        Topic.SPY -> Art(0xFF0B0F1C, 0xFF3C4A6B, 0xFF05070D, 0xFFFFE08A, Scene.NOIR, Fx.RAIN)
        Topic.DETECTIVE -> Art(0xFF151820, 0xFF5A5E6E, 0xFF0A0B10, 0xFFFFD27A, Scene.NOIR, Fx.RAIN)
        Topic.MYSTERY -> Art(0xFF14102A, 0xFF4E3D7A, 0xFF0B0816, 0xFFC9A7FF, Scene.NOIR, Fx.FIREFLIES)
        Topic.SUPERHEROES -> Art(0xFF0E1E5A, 0xFF3F7BFF, 0xFF0A1236, 0xFFFFE066, Scene.HERO_CITY, Fx.NONE)
        Topic.RACING -> Art(0xFF2B0F54, 0xFFFF6A5C, 0xFF16082E, 0xFFFFE66D, Scene.ROAD, Fx.NONE)
        Topic.SPORTS -> Art(0xFF0B3A7A, 0xFF6EC1FF, 0xFF1F6B2E, 0xFFFFFFFF, Scene.STADIUM, Fx.CONFETTI)
        Topic.HORROR -> Art(0xFF070B0C, 0xFF2F4A3E, 0xFF040606, 0xFFE8F0C8, Scene.GRAVEYARD, Fx.FIREFLIES)
        Topic.ZOMBIES -> Art(0xFF0C130A, 0xFF5A6B2E, 0xFF070A05, 0xFFB8FF6B, Scene.GRAVEYARD, Fx.EMBERS)
        Topic.VAMPIRES -> Art(0xFF12040A, 0xFF7A1024, 0xFF080206, 0xFFFF4D5E, Scene.GOTHIC, Fx.NONE)
        Topic.POST_APOCALYPTIC -> Art(0xFF2E1A0E, 0xFFD98A3E, 0xFF1E140C, 0xFFFFC46B, Scene.RUINS, Fx.EMBERS)
        Topic.OCEAN -> Art(0xFF06264A, 0xFF1E8FC2, 0xFF04182E, 0xFF9FF0FF, Scene.REEF, Fx.BUBBLES)
        Topic.PIRATES -> Art(0xFF1E2F5C, 0xFFF4A261, 0xFF0D2A4A, 0xFFFFD58A, Scene.SHIP, Fx.NONE)
        Topic.FARMING -> Art(0xFF7EC8F0, 0xFFFFF0C2, 0xFF6BB24A, 0xFFFFE27A, Scene.FARM, Fx.FIREFLIES)
        Topic.PETS -> Art(0xFFFFB3C7, 0xFFFFF1D6, 0xFF8FD18E, 0xFFFFFFFF, Scene.PETS, Fx.SPARKLES)
        Topic.COOKING -> Art(0xFFFFD08A, 0xFFFFF4E0, 0xFFE07A5F, 0xFFFFFFFF, Scene.KITCHEN, Fx.SPARKLES)
        Topic.SCHOOL -> Art(0xFF8EC5FF, 0xFFFFF4D6, 0xFF7CC47A, 0xFFFFE27A, Scene.SCHOOL, Fx.CONFETTI)
        Topic.FASHION -> Art(0xFF2A0F2E, 0xFFFF7EB6, 0xFF15081A, 0xFFFFD1E8, Scene.RUNWAY, Fx.SPARKLES)
        Topic.HOSPITAL -> Art(0xFF9FD8F0, 0xFFF2FBFF, 0xFF8FC9B8, 0xFFFFFFFF, Scene.CLINIC, Fx.NONE)
        Topic.MUSIC -> Art(0xFF120624, 0xFF6A1B9A, 0xFF0A0314, 0xFFFF7AE0, Scene.STAGE, Fx.CONFETTI)
        Topic.DINOSAURS -> Art(0xFF3E1F47, 0xFFFF8F00, 0xFF1B3A1B, 0xFFFFB347, Scene.JUNGLE, Fx.EMBERS)
        Topic.ROBOTS -> Art(0xFF0D1B2A, 0xFF35618C, 0xFF0A1420, 0xFF00E5FF, Scene.MECH, Fx.SPARKLES)
        Topic.ARTIFICIAL_INTELLIGENCE -> Art(0xFF0A0A1F, 0xFF3B2A7A, 0xFF07071A, 0xFFFF6AD5, Scene.NETWORK, Fx.SPARKLES)
        Topic.WILD_WEST -> Art(0xFFE8613C, 0xFFFFD98A, 0xFFB86A35, 0xFFFFF2B0, Scene.DESERT, Fx.NONE)
        Topic.MILITARY -> Art(0xFF1C2328, 0xFF8D6E63, 0xFF161B1E, 0xFFFFD08A, Scene.WAR, Fx.EMBERS)
    }

private fun platformColor(p: Platform): Color = when (p.maker) {
    "Sony" -> Color(0xFF0070D1)
    "Microsoft" -> Color(0xFF107C10)
    "Nintendo" -> Color(0xFFE60012)
    "Meta" -> Color(0xFF0866FF)
    "Amazon" -> Color(0xFF5A31F4)
    "Sega" -> Color(0xFF1E5BC6)
    "Atari" -> Color(0xFFB5121B)
    "Commodore" -> Color(0xFF6C4E2E)
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

/** Box art for [concept], by [studio] if given. Small covers keep the picture and drop the words. */
@Composable
fun GameCover(concept: GameConcept, width: Dp, modifier: Modifier = Modifier, studio: String? = null) {
    val measurer = rememberTextMeasurer()
    val detailed = width >= 72.dp
    val seed = remember(concept.title) { concept.title.hashCode() }
    Canvas(
        modifier
            .size(width, width * 4 / 3)
            .shadow(if (detailed) 8.dp else 2.dp, RoundedCornerShape(width * 0.06f))
            .clip(RoundedCornerShape(width * 0.06f)),
    ) {
        val w = size.width
        val h = size.height
        val band = h * 0.09f
        val art = concept.topic.art
        val rng = java.util.Random(seed.toLong() * 31 + concept.topic.ordinal)
        // Half the covers put the title at the top, half at the bottom; the hero takes the other end.
        val titleTop = detailed && (seed and 1) == 0
        val mirror = (seed shr 1 and 1) == 1
        val horizon = h * (0.6f + 0.05f * rng.nextFloat())

        clipRect(top = band) {
            drawRect(Brush.verticalGradient(listOf(art.sky, art.horizon), startY = band, endY = horizon), topLeft = Offset(0f, band), size = Size(w, horizon - band))
            drawRect(art.ground, topLeft = Offset(0f, horizon), size = Size(w, h - horizon))
            val lightAt = Offset(if (mirror) w * 0.28f else w * 0.72f, band + h * (0.16f + 0.06f * rng.nextFloat()))
            rays(lightAt, art.light, w, h)
            scene(art, concept.topic, w, h, band, horizon, lightAt, rng)
            val heroX = if (mirror) w * 0.68f else w * 0.32f
            // The hero stands on the horizon under a bottom title, and at the front under a top one.
            val heroBase = if (titleTop || !detailed) h * 0.97f else horizon + h * 0.02f
            hero(concept.genre, heroX, heroBase, h * (if (titleTop || !detailed) 0.44f else 0.34f), art, lightAt, mirror)
            atmosphere(art.fx, art.light, w, h, band, rng)
            // Vignette.
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), Offset(w / 2, h * 0.5f), h * 0.75f))
        }

        // The platform band.
        val platform = concept.platforms.firstOrNull() ?: Platform.PC
        val pc = platformColor(platform)
        drawRect(Brush.verticalGradient(listOf(pc.mix(Color.White, 0.12f), pc.shade(0.85f)), 0f, band), size = Size(w, band))
        drawRect(Color.White.copy(alpha = 0.25f), topLeft = Offset(0f, band - 1.2f), size = Size(w, 1.2f))
        if (detailed) {
            val label = measurer.measure(
                platform.title.uppercase(Locale.US),
                TextStyle(fontSize = (band * 0.4f).toSp(), fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.2.sp),
                maxLines = 1, overflow = TextOverflow.Clip, constraints = Constraints(maxWidth = (w * 0.9f).toInt()),
            )
            drawText(label, topLeft = Offset(w * 0.05f, (band - label.size.height) / 2))
        }

        if (detailed) {
            // A scrim behind the title.
            if (titleTop) {
                drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), startY = band, endY = h * 0.5f), topLeft = Offset(0f, band), size = Size(w, h * 0.5f - band))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = h * 0.84f, endY = h), topLeft = Offset(0f, h * 0.84f), size = Size(w, h * 0.16f))
            } else {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = h * 0.5f, endY = h), topLeft = Offset(0f, h * 0.5f), size = Size(w, h * 0.5f))
            }
            titleLogo(measurer, concept, art, w, h, band, titleTop)
            footer(measurer, concept, studio, w, h)
        }
        // A sheen along the spine side.
        drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), startX = 0f, endX = w * 0.1f), size = Size(w * 0.1f, h))
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

// Lettering

private val sequelSuffix = Regex("^(.*?)( (?:\\d+|[IVX]+))(:.*)?$")

/** The title as a logo: a light-to-accent fill, a dark outline and a drop shadow, with any sequel number picked out. */
private fun DrawScope.titleLogo(measurer: TextMeasurer, c: GameConcept, art: Art, w: Float, h: Float, band: Float, top: Boolean) {
    val maxW = (w * 0.88f).toInt()
    val italic = c.genre == Genre.ACTION || c.topic == Topic.RACING || c.topic == Topic.SPORTS
    val m = sequelSuffix.matchEntire(c.title)
    val main = m?.groupValues?.get(1)?.let { it + (m.groupValues.getOrNull(3) ?: "") } ?: c.title
    val number = m?.groupValues?.get(2)?.trim()
    fun style(fs: Float) = TextStyle(
        fontSize = fs.toSp(), fontWeight = FontWeight.Black, lineHeight = (fs * 1.0f).toSp(),
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )
    var fs = w * 0.13f
    var layout: TextLayoutResult = measurer.measure(main.uppercase(Locale.US), style(fs), maxLines = 3, constraints = Constraints(maxWidth = maxW))
    while ((layout.hasVisualOverflow || layout.lineCount > 2) && fs > w * 0.07f) {
        fs *= 0.9f
        layout = measurer.measure(main.uppercase(Locale.US), style(fs), maxLines = 3, constraints = Constraints(maxWidth = maxW))
    }
    val num = number?.let { measurer.measure(it, style(fs * 1.5f).copy(fontStyle = FontStyle.Italic), maxLines = 1) }
    val genre = measurer.measure(
        c.genre.title.uppercase(Locale.US) + (c.genre2?.let { " · " + it.title.uppercase(Locale.US) } ?: ""),
        TextStyle(fontSize = (w * 0.048f).toSp(), fontWeight = FontWeight.Bold, color = art.light, letterSpacing = 1.5.sp),
        maxLines = 1,
    )
    val blockH = layout.size.height + (num?.size?.height?.times(0.8f) ?: 0f) + genre.size.height
    val y0 = if (top) band + h * 0.035f else h - w * 0.2f - blockH
    val x = w * 0.06f
    val at = Offset(x, y0)
    drawText(layout, color = Color.Black.copy(alpha = 0.45f), topLeft = at + Offset(0f, fs * 0.07f))
    drawText(layout, color = Color(0xFF14101E), topLeft = at, drawStyle = Stroke(fs * 0.09f, join = StrokeJoin.Round))
    drawText(layout, brush = Brush.verticalGradient(listOf(Color.White, art.light.mix(Color.White, 0.35f)), y0, y0 + layout.size.height), topLeft = at)
    var y = y0 + layout.size.height
    if (num != null) {
        val nAt = Offset(x, y - num.size.height * 0.15f)
        drawText(num, color = Color(0xFF14101E), topLeft = nAt, drawStyle = Stroke(fs * 0.12f, join = StrokeJoin.Round))
        drawText(num, brush = Brush.verticalGradient(listOf(art.light, art.light.shade(0.75f)), nAt.y, nAt.y + num.size.height), topLeft = nAt)
        y += num.size.height * 0.8f
    }
    drawText(genre, topLeft = Offset(x + 1f, y))
}

/** The rating box and the studio's name. */
private fun DrawScope.footer(measurer: TextMeasurer, c: GameConcept, studio: String?, w: Float, h: Float) {
    val box = w * 0.11f
    val rx = w * 0.06f
    val ry = h - w * 0.05f - box
    drawRect(Color.White, Offset(rx, ry), Size(box * 0.8f, box))
    drawRect(Color.Black, Offset(rx + 1.5f, ry + 1.5f), Size(box * 0.8f - 3f, box * 0.72f))
    val r = measurer.measure(c.audience.rating, TextStyle(fontSize = (box * 0.5f).toSp(), fontWeight = FontWeight.Black, color = Color.White))
    drawText(r, topLeft = Offset(rx + (box * 0.8f - r.size.width) / 2, ry + (box * 0.72f - r.size.height) / 2))
    if (studio != null) {
        val s = measurer.measure(
            studio.uppercase(Locale.US),
            TextStyle(fontSize = (w * 0.045f).toSp(), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = (w * 0.6f).toInt()),
        )
        drawText(s, topLeft = Offset(w - w * 0.06f - s.size.width, ry + box - s.size.height))
    }
}

// Painting helpers

private fun DrawScope.poly(color: Color, vararg xy: Float) {
    val p = Path()
    var i = 0
    while (i < xy.size) {
        if (i == 0) p.moveTo(xy[i], xy[i + 1]) else p.lineTo(xy[i], xy[i + 1])
        i += 2
    }
    p.close()
    drawPath(p, color)
}

private fun DrawScope.glow(at: Offset, r: Float, c: Color, alpha: Float = 0.45f) =
    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = alpha), Color.Transparent), at, r), r, at)

/** Soft beams fanning out from the light. */
private fun DrawScope.rays(from: Offset, c: Color, w: Float, h: Float) {
    for (k in 0 until 6) {
        val a = PI.toFloat() * (0.3f + k * 0.08f)
        val len = h * 1.2f
        val spread = 0.035f
        val p1 = Offset(from.x + cos(a - spread) * len, from.y + sin(a - spread) * len)
        val p2 = Offset(from.x + cos(a + spread) * len, from.y + sin(a + spread) * len)
        poly(c.copy(alpha = 0.06f), from.x, from.y, p1.x, p1.y, p2.x, p2.y)
    }
    glow(from, w * 0.5f, c, 0.35f)
}

private fun DrawScope.skyline(color: Color, base: Float, w: Float, h: Float, rng: java.util.Random, lit: Color?, min: Float = 0.12f, max: Float = 0.32f) {
    var x = -w * 0.05f
    while (x < w) {
        val bw = w * (0.09f + 0.08f * rng.nextFloat())
        val bh = h * (min + (max - min) * rng.nextFloat())
        drawRect(color, Offset(x, base - bh), Size(bw, bh + 2f))
        if (lit != null) {
            var wy = base - bh + h * 0.02f
            while (wy < base - h * 0.02f) {
                var wx = x + bw * 0.18f
                while (wx < x + bw * 0.8f) {
                    if (rng.nextFloat() > 0.55f) drawRect(lit, Offset(wx, wy), Size(bw * 0.12f, h * 0.012f))
                    wx += bw * 0.24f
                }
                wy += h * 0.03f
            }
        }
        x += bw + w * 0.008f
    }
}

private fun DrawScope.mountains(color: Color, base: Float, w: Float, h: Float, rng: java.util.Random, height: Float) {
    val p = Path().apply {
        moveTo(0f, base)
        var x = 0f
        while (x < w) {
            val peak = base - h * height * (0.5f + 0.5f * rng.nextFloat())
            lineTo(x + w * 0.12f, peak)
            x += w * 0.25f
            lineTo(x, base - h * height * 0.25f * rng.nextFloat())
        }
        lineTo(w, base)
        close()
    }
    drawPath(p, color)
}

// Scenes, one per topic

private fun DrawScope.scene(a: Art, topic: Topic, w: Float, h: Float, top: Float, horizon: Float, light: Offset, rng: java.util.Random) {
    val dark = a.ground
    val mid = a.sky.mix(a.horizon, 0.45f).shade(0.55f)
    when (a.scene) {
        Scene.CASTLE, Scene.DRAGON -> {
            drawCircle(a.light.mix(Color.White, 0.5f), w * 0.1f, light)
            mountains(mid, horizon, w, h, rng, 0.2f)
            val cx = w * (0.4f + 0.2f * rng.nextFloat())
            val base = horizon + h * 0.02f
            for (k in -2..2) {
                val tx = cx + k * w * 0.09f
                val th = h * (0.14f + 0.08f * (2 - kotlin.math.abs(k)))
                drawRect(dark, Offset(tx - w * 0.035f, base - th), Size(w * 0.07f, th))
                poly(dark, tx - w * 0.05f, base - th, tx, base - th - h * 0.07f, tx + w * 0.05f, base - th)
                drawRect(a.light, Offset(tx - w * 0.008f, base - th + h * 0.04f), Size(w * 0.016f, h * 0.025f))
                // Banners.
                poly(a.light.shade(0.8f), tx, base - th - h * 0.07f, tx + w * 0.04f, base - th - h * 0.06f, tx, base - th - h * 0.05f)
            }
            if (a.scene == Scene.DRAGON) {
                // A dragon circling the keep.
                val d = Offset(w * (0.25f + 0.5f * rng.nextFloat()), top + h * 0.2f)
                val s = w * 0.12f
                poly(dark.shade(0.8f), d.x - s * 1.6f, d.y - s * 0.2f, d.x, d.y + s * 0.15f, d.x - s * 0.3f, d.y - s * 0.9f)
                poly(dark.shade(0.8f), d.x + s * 1.6f, d.y - s * 0.3f, d.x, d.y + s * 0.15f, d.x + s * 0.4f, d.y - s * 0.95f)
                drawOval(dark.shade(0.8f), Offset(d.x - s * 0.5f, d.y - s * 0.1f), Size(s, s * 0.35f))
                drawLine(dark.shade(0.8f), Offset(d.x + s * 0.4f, d.y + s * 0.05f), Offset(d.x + s * 0.8f, d.y - s * 0.25f), strokeWidth = s * 0.15f, cap = StrokeCap.Round)
                drawLine(dark.shade(0.8f), Offset(d.x - s * 0.4f, d.y + s * 0.1f), Offset(d.x - s * 1.0f, d.y + s * 0.35f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)
                glow(Offset(d.x + s * 0.9f, d.y - s * 0.3f), s * 0.6f, Color(0xFFFF7A2E), 0.7f)
            }
        }
        Scene.TEMPLE -> {
            mountains(mid, horizon, w, h, rng, 0.15f)
            val cx = w * 0.5f
            val base = horizon + h * 0.02f
            val tw = w * 0.56f
            poly(dark, cx - tw / 2 - w * 0.03f, base - h * 0.22f, cx, base - h * 0.3f, cx + tw / 2 + w * 0.03f, base - h * 0.22f)
            drawRect(dark, Offset(cx - tw / 2, base - h * 0.22f), Size(tw, h * 0.025f))
            for (k in 0 until 5) drawRect(dark, Offset(cx - tw / 2 + k * tw / 4.6f, base - h * 0.2f), Size(tw * 0.08f, h * 0.2f))
            drawRect(dark, Offset(cx - tw / 2 - w * 0.03f, base - h * 0.01f), Size(tw + w * 0.06f, h * 0.02f))
            // Lightning.
            val lx = w * (0.2f + 0.6f * rng.nextFloat())
            val bolt = Path().apply {
                moveTo(lx, top)
                lineTo(lx - w * 0.04f, top + h * 0.12f)
                lineTo(lx + w * 0.02f, top + h * 0.13f)
                lineTo(lx - w * 0.03f, top + h * 0.26f)
            }
            drawPath(bolt, a.light, style = Stroke(w * 0.012f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            glow(Offset(lx, top + h * 0.13f), w * 0.2f, a.light, 0.3f)
        }
        Scene.PAGODA -> {
            drawCircle(a.light.mix(Color.White, 0.3f), w * 0.17f, light)
            mountains(mid, horizon, w, h, rng, 0.12f)
            val cx = w * (0.55f + 0.15f * rng.nextFloat())
            var y = horizon + h * 0.01f
            for (k in 0 until 4) {
                val tw = w * (0.3f - k * 0.05f)
                drawRect(dark, Offset(cx - tw * 0.35f, y - h * 0.06f), Size(tw * 0.7f, h * 0.06f))
                poly(dark, cx - tw / 2, y - h * 0.06f, cx + tw / 2, y - h * 0.06f, cx + tw * 0.32f, y - h * 0.09f, cx - tw * 0.32f, y - h * 0.09f)
                y -= h * 0.09f
            }
            drawLine(dark, Offset(cx, y), Offset(cx, y - h * 0.06f), strokeWidth = w * 0.01f)
            for (k in 0 until 5) {
                val bx = w * (0.04f + k * 0.05f)
                drawLine(dark, Offset(bx, h), Offset(bx + w * 0.01f, top + h * (0.15f + 0.05f * k)), strokeWidth = w * 0.018f)
            }
        }
        Scene.PLANET, Scene.UFO, Scene.MARS -> {
            glow(Offset(w * 0.3f, h * 0.4f), w * 0.6f, a.horizon, 0.5f)
            val pc = Offset(if (light.x > w / 2) w * 0.68f else w * 0.32f, top + h * 0.25f)
            val pr = w * (if (a.scene == Scene.MARS) 0.16f else 0.26f)
            val pcol = if (a.scene == Scene.MARS) Color(0xFFFF8A5B) else a.light
            drawCircle(Brush.radialGradient(listOf(pcol.mix(Color.White, 0.3f), pcol.shade(0.45f), pcol.shade(0.2f)), Offset(pc.x - pr * 0.4f, pc.y - pr * 0.4f), pr * 1.6f), pr, pc)
            if (a.scene == Scene.PLANET) drawOval(Color(0xFFFFE3A8).copy(alpha = 0.75f), Offset(pc.x - pr * 1.7f, pc.y - pr * 0.25f), Size(pr * 3.4f, pr * 0.5f), style = Stroke(w * 0.012f))
            if (a.scene == Scene.MARS) {
                mountains(Color(0xFF8A3A22), horizon, w, h, rng, 0.08f)
                drawRect(Color(0xFF6B2A18), Offset(0f, horizon), Size(w, h - horizon))
                // A rover.
                val rx = w * 0.62f
                drawRect(Color(0xFFDDDDDD), Offset(rx, horizon + h * 0.04f), Size(w * 0.14f, h * 0.035f))
                for (k in 0..2) drawCircle(Color(0xFF2B2B2B), w * 0.02f, Offset(rx + w * (0.02f + k * 0.05f), horizon + h * 0.085f))
            } else {
                // A curved horizon of a moon.
                drawOval(dark, Offset(-w * 0.3f, horizon - h * 0.03f), Size(w * 1.6f, h * 0.6f))
            }
            if (a.scene == Scene.UFO) {
                val u = Offset(w * 0.5f, top + h * 0.42f)
                poly(a.light.copy(alpha = 0.25f), u.x - w * 0.06f, u.y, u.x + w * 0.06f, u.y, u.x + w * 0.22f, horizon + h * 0.05f, u.x - w * 0.22f, horizon + h * 0.05f)
                drawOval(Color(0xFF9AA4B2), Offset(u.x - w * 0.17f, u.y - h * 0.02f), Size(w * 0.34f, h * 0.045f))
                drawOval(a.light.mix(Color.White, 0.4f), Offset(u.x - w * 0.07f, u.y - h * 0.055f), Size(w * 0.14f, h * 0.05f))
                for (k in -2..2) drawCircle(a.light, w * 0.012f, Offset(u.x + k * w * 0.06f, u.y + h * 0.003f))
            }
        }
        Scene.VORTEX -> {
            val c = Offset(w * 0.5f, top + h * 0.3f)
            for (k in 0 until 7) {
                drawCircle(a.light.copy(alpha = 0.15f + 0.1f * (k % 2)), w * (0.05f + k * 0.06f), c, style = Stroke(w * 0.012f))
            }
            drawCircle(Color.White.copy(alpha = 0.9f), w * 0.04f, c)
            // Clock hands.
            drawLine(a.light, c, Offset(c.x + w * 0.16f, c.y - h * 0.05f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
            drawLine(a.light, c, Offset(c.x - w * 0.05f, c.y - h * 0.12f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
            skyline(dark, horizon + h * 0.03f, w, h, rng, null, 0.05f, 0.15f)
        }
        Scene.CITY, Scene.TOWER, Scene.HERO_CITY -> {
            drawCircle(a.light.mix(Color.White, 0.4f), w * 0.08f, light)
            skyline(mid, horizon - h * 0.02f, w, h, rng, null, 0.15f, 0.3f)
            skyline(dark, horizon + h * 0.04f, w, h, rng, a.light.copy(alpha = 0.8f), 0.12f, 0.28f)
            if (a.scene == Scene.TOWER) {
                val tx = w * 0.5f
                poly(dark.shade(0.7f), tx - w * 0.08f, horizon + h * 0.05f, tx - w * 0.04f, top + h * 0.1f, tx + w * 0.04f, top + h * 0.1f, tx + w * 0.08f, horizon + h * 0.05f)
                val chart = Path().apply {
                    moveTo(w * 0.1f, top + h * 0.38f)
                    lineTo(w * 0.3f, top + h * 0.3f)
                    lineTo(w * 0.45f, top + h * 0.34f)
                    lineTo(w * 0.85f, top + h * 0.12f)
                }
                drawPath(chart, a.light, style = Stroke(w * 0.02f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                poly(a.light, w * 0.86f, top + h * 0.09f, w * 0.88f, top + h * 0.15f, w * 0.81f, top + h * 0.13f)
            }
            if (a.scene == Scene.HERO_CITY) {
                // The signal in the sky.
                val s = Offset(w * 0.5f, top + h * 0.17f)
                poly(a.light.copy(alpha = 0.18f), w * 0.5f, horizon, s.x - w * 0.14f, s.y, s.x + w * 0.14f, s.y)
                drawCircle(a.light.copy(alpha = 0.8f), w * 0.11f, s)
                poly(dark, s.x - w * 0.07f, s.y - h * 0.02f, s.x, s.y + h * 0.035f, s.x + w * 0.07f, s.y - h * 0.02f, s.x, s.y - h * 0.005f)
            }
        }
        Scene.NEON, Scene.NETWORK -> {
            if (a.scene == Scene.NEON) {
                skyline(mid, horizon, w, h, rng, null, 0.2f, 0.45f)
                skyline(dark, horizon + h * 0.05f, w, h, rng, a.light, 0.15f, 0.4f)
                // Neon signs.
                for (k in 0..2) {
                    val nx = w * (0.15f + k * 0.3f)
                    val ny = horizon - h * (0.1f + 0.08f * rng.nextFloat())
                    drawRoundRect(listOf(a.light, a.horizon, Color(0xFFFFE066))[k], Offset(nx, ny), Size(w * 0.1f, h * 0.025f), CornerRadius(w * 0.01f), style = Stroke(w * 0.008f))
                    glow(Offset(nx + w * 0.05f, ny), w * 0.12f, listOf(a.light, a.horizon, Color(0xFFFFE066))[k], 0.35f)
                }
            } else {
                // A network of nodes.
                val nodes = List(12) { Offset(w * (0.08f + 0.84f * rng.nextFloat()), top + h * (0.08f + 0.5f * rng.nextFloat())) }
                for (i in nodes.indices) for (j in i + 1 until nodes.size) {
                    if ((nodes[i] - nodes[j]).getDistance() < w * 0.35f) drawLine(a.light.copy(alpha = 0.35f), nodes[i], nodes[j], strokeWidth = w * 0.005f)
                }
                for (n in nodes) {
                    glow(n, w * 0.05f, a.light, 0.5f)
                    drawCircle(a.light.mix(Color.White, 0.4f), w * 0.012f, n)
                }
                if (topic == Topic.HACKING) {
                    // Falling code.
                    for (k in 0 until 14) {
                        val x = w * k / 14f
                        val len = h * (0.1f + 0.3f * rng.nextFloat())
                        val y = top + h * 0.5f * rng.nextFloat()
                        drawLine(Brush.verticalGradient(listOf(Color.Transparent, a.light.copy(alpha = 0.7f)), y, y + len), Offset(x, y), Offset(x, y + len), strokeWidth = w * 0.012f)
                    }
                }
                skyline(dark, horizon + h * 0.05f, w, h, rng, a.light.copy(alpha = 0.6f), 0.08f, 0.2f)
            }
        }
        Scene.NOIR -> {
            glow(light, w * 0.4f, a.light, 0.25f)
            skyline(mid, horizon, w, h, rng, a.light.copy(alpha = 0.5f), 0.2f, 0.4f)
            // A street lamp.
            val lx = if (light.x > w / 2) w * 0.82f else w * 0.18f
            drawLine(dark, Offset(lx, h), Offset(lx, top + h * 0.3f), strokeWidth = w * 0.02f)
            drawLine(dark, Offset(lx, top + h * 0.3f), Offset(lx + (if (lx > w / 2) -1 else 1) * w * 0.08f, top + h * 0.3f), strokeWidth = w * 0.02f)
            val bulb = Offset(lx + (if (lx > w / 2) -1 else 1) * w * 0.08f, top + h * 0.32f)
            poly(a.light.copy(alpha = 0.18f), bulb.x, bulb.y, bulb.x - w * 0.18f, h, bulb.x + w * 0.18f, h)
            drawCircle(a.light, w * 0.02f, bulb)
            // Fog.
            for (k in 0..2) drawRect(Color.White.copy(alpha = 0.05f + 0.02f * k), Offset(0f, horizon + h * (0.02f + k * 0.04f)), Size(w, h * 0.05f))
            if (topic == Topic.MYSTERY) {
                val d = Offset(w * 0.5f, horizon + h * 0.05f)
                drawRoundRect(dark.shade(0.6f), Offset(d.x - w * 0.08f, d.y - h * 0.22f), Size(w * 0.16f, h * 0.22f), CornerRadius(w * 0.08f, w * 0.08f))
                drawCircle(a.light, w * 0.018f, Offset(d.x, d.y - h * 0.12f))
                poly(a.light, d.x - w * 0.01f, d.y - h * 0.11f, d.x + w * 0.01f, d.y - h * 0.11f, d.x + w * 0.015f, d.y - h * 0.08f, d.x - w * 0.015f, d.y - h * 0.08f)
            }
        }
        Scene.ROAD -> {
            val sun = Offset(w / 2, horizon - h * 0.02f)
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE66D), Color(0xFFFF4E8A)), sun.y - w * 0.25f, sun.y), w * 0.25f, sun)
            for (k in 0..3) drawRect(a.horizon, Offset(0f, sun.y - h * (0.02f + k * 0.035f)), Size(w, h * (0.006f + k * 0.002f)))
            for (k in 1..7) {
                val y = horizon + (h - horizon) * (k / 7f) * (k / 7f)
                drawLine(Color(0xFFFF2E88).copy(alpha = 0.6f), Offset(0f, y), Offset(w, y), strokeWidth = w * 0.005f)
            }
            for (k in -4..4) drawLine(Color(0xFFFF2E88).copy(alpha = 0.5f), Offset(w / 2, horizon), Offset(w / 2 + k * w * 0.35f, h), strokeWidth = w * 0.005f)
            poly(Color(0xFF2E2E3A), w * 0.47f, horizon, w * 0.53f, horizon, w * 0.9f, h, w * 0.1f, h)
            for (k in 0..4) {
                val f0 = 0.1f + k * 0.2f
                drawLine(Color.White, Offset(w / 2, horizon + (h - horizon) * f0 * f0), Offset(w / 2, horizon + (h - horizon) * (f0 + 0.08f) * (f0 + 0.08f)), strokeWidth = w * (0.006f + 0.02f * f0))
            }
        }
        Scene.STADIUM -> {
            for (side in listOf(0.1f, 0.9f)) {
                drawLine(Color(0xFF37474F), Offset(w * side, horizon), Offset(w * side, top + h * 0.08f), strokeWidth = w * 0.02f)
                drawRect(Color(0xFFFFFDE7), Offset(w * side - w * 0.06f, top + h * 0.05f), Size(w * 0.12f, h * 0.04f))
                glow(Offset(w * side, top + h * 0.07f), w * 0.3f, Color.White, 0.6f)
            }
            drawRect(Color(0xFF263238), Offset(0f, horizon - h * 0.08f), Size(w, h * 0.08f))
            for (k in 0 until 40) drawCircle(Color.White.copy(alpha = 0.3f + 0.4f * rng.nextFloat()), w * 0.006f, Offset(w * rng.nextFloat(), horizon - h * 0.08f * rng.nextFloat()))
            for (k in 0..3) {
                val y = horizon + (h - horizon) * k / 4f
                drawRect(Color(0xFF2E7D32).mix(Color(0xFF43A047), (k % 2).toFloat()), Offset(0f, y), Size(w, (h - horizon) / 4f))
            }
            drawLine(Color.White.copy(alpha = 0.7f), Offset(0f, horizon + (h - horizon) * 0.5f), Offset(w, horizon + (h - horizon) * 0.5f), strokeWidth = w * 0.008f)
        }
        Scene.GRAVEYARD, Scene.GOTHIC -> {
            drawCircle(a.light, w * 0.16f, light)
            glow(light, w * 0.35f, a.light, 0.3f)
            if (a.scene == Scene.GOTHIC) {
                val cx = w * 0.5f
                val base = horizon + h * 0.02f
                for (k in -1..1) {
                    val th = h * (0.3f - kotlin.math.abs(k) * 0.08f)
                    val tx = cx + k * w * 0.13f
                    drawRect(dark, Offset(tx - w * 0.045f, base - th), Size(w * 0.09f, th))
                    poly(dark, tx - w * 0.06f, base - th, tx, base - th - h * 0.12f, tx + w * 0.06f, base - th)
                    drawRect(a.light, Offset(tx - w * 0.01f, base - th + h * 0.05f), Size(w * 0.02f, h * 0.03f))
                }
                // Bats.
                for (k in 0..4) {
                    val b = Offset(w * (0.15f + 0.7f * rng.nextFloat()), top + h * (0.05f + 0.25f * rng.nextFloat()))
                    val s = w * 0.035f
                    poly(dark, b.x - s, b.y - s * 0.3f, b.x, b.y + s * 0.2f, b.x + s, b.y - s * 0.3f, b.x, b.y - s * 0.05f)
                }
            } else {
                val tx = if (light.x > w / 2) w * 0.2f else w * 0.8f
                drawLine(dark, Offset(tx, horizon + h * 0.03f), Offset(tx, top + h * 0.25f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                for ((dx, dy) in listOf(-0.12f to 0.32f, 0.14f to 0.3f, -0.08f to 0.42f, 0.1f to 0.46f)) {
                    drawLine(dark, Offset(tx, top + h * (dy + 0.1f)), Offset(tx + w * dx, top + h * dy), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
                }
                for (k in 0..3) {
                    val gx = w * (0.08f + k * 0.24f)
                    drawRoundRect(dark.mix(Color.Gray, 0.15f), Offset(gx, horizon - h * 0.05f), Size(w * 0.08f, h * 0.09f), CornerRadius(w * 0.04f, w * 0.04f))
                }
                if (topic == Topic.ZOMBIES) {
                    for (k in 0..2) {
                        val hx = w * (0.15f + k * 0.3f)
                        drawLine(Color(0xFF6B8E23), Offset(hx, h), Offset(hx + w * 0.02f, horizon + h * 0.05f), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
                    }
                }
            }
            for (k in 0..2) drawRect(Color.White.copy(alpha = 0.05f + 0.025f * k), Offset(0f, horizon - h * (0.02f - k * 0.04f)), Size(w, h * 0.04f))
        }
        Scene.RUINS -> {
            drawCircle(a.light.mix(Color.White, 0.3f), w * 0.12f, light)
            var x = 0f
            while (x < w) {
                val bw = w * (0.12f + 0.08f * rng.nextFloat())
                val bh = h * (0.12f + 0.25f * rng.nextFloat())
                poly(mid, x, horizon, x, horizon - bh, x + bw * 0.4f, horizon - bh * (0.8f + 0.15f * rng.nextFloat()), x + bw * 0.7f, horizon - bh * 0.9f, x + bw, horizon - bh * 0.6f, x + bw, horizon)
                x += bw + w * 0.02f
            }
            for (k in 0..2) glow(Offset(w * (0.2f + k * 0.3f), horizon), w * 0.15f, Color(0xFFFF7A2E), 0.35f)
        }
        Scene.REEF -> {
            for (k in 0 until 5) {
                val x = w * (0.1f + k * 0.2f)
                poly(Color.White.copy(alpha = 0.06f), x, top, x + w * 0.08f, top, x + w * 0.2f, h, x + w * 0.05f, h)
            }
            val sand = Path().apply {
                moveTo(0f, horizon + h * 0.08f)
                quadraticTo(w * 0.5f, horizon - h * 0.02f, w, horizon + h * 0.06f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(sand, Color(0xFFC9A66B).shade(0.6f))
            val coral = listOf(Color(0xFFFF6F91), Color(0xFFFFC75F), Color(0xFF845EC2), Color(0xFF00C9A7))
            for (k in 0 until 6) {
                val cx = w * (0.05f + k * 0.18f)
                val ch = h * (0.08f + 0.08f * rng.nextFloat())
                drawLine(coral[k % 4], Offset(cx, horizon + h * 0.06f), Offset(cx, horizon + h * 0.06f - ch), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
                drawLine(coral[k % 4], Offset(cx, horizon + h * 0.03f - ch * 0.5f), Offset(cx + w * 0.04f, horizon - ch * 0.6f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
            }
            for (k in 0 until 4) {
                val f = Offset(w * (0.15f + 0.7f * rng.nextFloat()), top + h * (0.15f + 0.3f * rng.nextFloat()))
                val c = coral[k % 4]
                drawOval(c, Offset(f.x - w * 0.05f, f.y - h * 0.015f), Size(w * 0.1f, h * 0.03f))
                poly(c, f.x + w * 0.04f, f.y, f.x + w * 0.08f, f.y - h * 0.02f, f.x + w * 0.08f, f.y + h * 0.02f)
            }
        }
        Scene.SHIP -> {
            drawCircle(a.light, w * 0.13f, Offset(light.x, horizon - h * 0.03f))
            drawRect(Color(0xFF0B2545), Offset(0f, horizon), Size(w, h - horizon))
            for (k in 0..5) {
                val y = horizon + h * (0.02f + k * 0.05f)
                drawLine(a.light.copy(alpha = 0.25f), Offset(w * 0.1f * k, y), Offset(w * (0.4f + 0.1f * k), y), strokeWidth = w * 0.006f)
            }
            val sx = w * 0.45f
            val sy = horizon + h * 0.01f
            poly(dark, sx - w * 0.25f, sy - h * 0.05f, sx + w * 0.25f, sy - h * 0.05f, sx + w * 0.18f, sy + h * 0.03f, sx - w * 0.18f, sy + h * 0.03f)
            for (k in -1..1) {
                val mx = sx + k * w * 0.12f
                drawLine(dark, Offset(mx, sy - h * 0.05f), Offset(mx, sy - h * (0.36f - kotlin.math.abs(k) * 0.06f)), strokeWidth = w * 0.012f)
                poly(Color(0xFFF2E6CF).shade(0.85f), mx - w * 0.07f, sy - h * (0.3f - kotlin.math.abs(k) * 0.06f), mx + w * 0.07f, sy - h * (0.3f - kotlin.math.abs(k) * 0.06f), mx + w * 0.08f, sy - h * 0.1f, mx - w * 0.08f, sy - h * 0.1f)
            }
            drawRect(Color(0xFF111111), Offset(sx, sy - h * 0.4f), Size(w * 0.07f, h * 0.035f))
        }
        Scene.FARM, Scene.HOUSE, Scene.PETS, Scene.KITCHEN, Scene.SCHOOL, Scene.CLINIC -> {
            drawCircle(a.light.mix(Color(0xFFFFD54F), 0.6f), w * 0.09f, light)
            for (k in 0 until 3) {
                val c = Offset(w * (0.15f + 0.35f * k + 0.1f * rng.nextFloat()), top + h * (0.08f + 0.06f * rng.nextFloat()))
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.05f, c)
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.07f, c + Offset(w * 0.05f, -h * 0.01f))
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.05f, c + Offset(w * 0.11f, 0f))
            }
            val hill = Path().apply {
                moveTo(0f, horizon)
                quadraticTo(w * 0.3f, horizon - h * 0.1f, w * 0.6f, horizon)
                quadraticTo(w * 0.85f, horizon + h * 0.06f, w, horizon - h * 0.04f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hill, a.ground.shade(1.08f))
            val bx = if (light.x > w / 2) w * 0.25f else w * 0.72f
            val by = horizon + h * 0.01f
            when (a.scene) {
                Scene.FARM -> {
                    drawRect(Color(0xFFC0392B), Offset(bx - w * 0.12f, by - h * 0.13f), Size(w * 0.24f, h * 0.13f))
                    poly(Color(0xFF8E2A20), bx - w * 0.15f, by - h * 0.13f, bx, by - h * 0.22f, bx + w * 0.15f, by - h * 0.13f)
                    drawRect(Color.White, Offset(bx - w * 0.04f, by - h * 0.08f), Size(w * 0.08f, h * 0.08f), style = Stroke(w * 0.01f))
                    for (k in 0 until 6) drawLine(Color(0xFF4E8A2E), Offset(0f, by + h * (0.04f + k * 0.04f)), Offset(w, by + h * (0.02f + k * 0.05f)), strokeWidth = w * 0.01f)
                }
                Scene.SCHOOL -> {
                    drawRect(Color(0xFFE57373), Offset(bx - w * 0.17f, by - h * 0.14f), Size(w * 0.34f, h * 0.14f))
                    poly(Color(0xFF8D6E63), bx - w * 0.06f, by - h * 0.14f, bx, by - h * 0.2f, bx + w * 0.06f, by - h * 0.14f)
                    drawCircle(Color.White, w * 0.025f, Offset(bx, by - h * 0.155f))
                    for (k in -2..2) drawRect(Color(0xFFFFF3E0), Offset(bx + k * w * 0.06f - w * 0.018f, by - h * 0.1f), Size(w * 0.036f, h * 0.035f))
                }
                Scene.CLINIC -> {
                    drawRect(Color.White, Offset(bx - w * 0.15f, by - h * 0.17f), Size(w * 0.3f, h * 0.17f))
                    drawRect(Color(0xFFE53935), Offset(bx - w * 0.015f, by - h * 0.15f), Size(w * 0.03f, h * 0.07f))
                    drawRect(Color(0xFFE53935), Offset(bx - w * 0.05f, by - h * 0.125f), Size(w * 0.1f, h * 0.022f))
                }
                Scene.KITCHEN -> {
                    // A steaming pot and a chef's hat.
                    drawRoundRect(Color(0xFF455A64), Offset(bx - w * 0.12f, by - h * 0.08f), Size(w * 0.24f, h * 0.1f), CornerRadius(w * 0.03f))
                    drawRect(Color(0xFF37474F), Offset(bx - w * 0.14f, by - h * 0.09f), Size(w * 0.28f, h * 0.015f))
                    for (k in 0..2) drawCircle(Color.White.copy(alpha = 0.6f), w * (0.03f + k * 0.01f), Offset(bx + (k - 1) * w * 0.05f, by - h * (0.13f + k * 0.03f)))
                }
                Scene.PETS -> {
                    // A cat and a dog.
                    val cat = Offset(bx - w * 0.07f, by - h * 0.05f)
                    drawCircle(Color(0xFFFFA726), w * 0.07f, cat)
                    poly(Color(0xFFFFA726), cat.x - w * 0.06f, cat.y - h * 0.02f, cat.x - w * 0.05f, cat.y - h * 0.08f, cat.x - w * 0.015f, cat.y - h * 0.045f)
                    poly(Color(0xFFFFA726), cat.x + w * 0.06f, cat.y - h * 0.02f, cat.x + w * 0.05f, cat.y - h * 0.08f, cat.x + w * 0.015f, cat.y - h * 0.045f)
                    val dog = Offset(bx + w * 0.09f, by - h * 0.04f)
                    drawCircle(Color(0xFF8D6E63), w * 0.075f, dog)
                    drawOval(Color(0xFF5D4037), Offset(dog.x - w * 0.1f, dog.y - h * 0.03f), Size(w * 0.04f, h * 0.06f))
                    drawOval(Color(0xFF5D4037), Offset(dog.x + w * 0.06f, dog.y - h * 0.03f), Size(w * 0.04f, h * 0.06f))
                    for (p in listOf(cat, dog)) {
                        drawCircle(Color(0xFF212121), w * 0.01f, p + Offset(-w * 0.025f, -h * 0.005f))
                        drawCircle(Color(0xFF212121), w * 0.01f, p + Offset(w * 0.025f, -h * 0.005f))
                    }
                }
                else -> {
                    drawRect(Color(0xFFFFF3E0), Offset(bx - w * 0.12f, by - h * 0.12f), Size(w * 0.24f, h * 0.12f))
                    poly(Color(0xFFE57373), bx - w * 0.15f, by - h * 0.12f, bx, by - h * 0.22f, bx + w * 0.15f, by - h * 0.12f)
                }
            }
        }
        Scene.STAGE -> {
            for (k in 0..4) {
                val sx = w * (0.1f + k * 0.2f)
                poly(listOf(a.light, Color(0xFF7AD1FF), Color(0xFFFFE066))[k % 3].copy(alpha = 0.18f), sx, top, sx - w * 0.12f + k * w * 0.05f, horizon, sx + w * 0.12f + k * w * 0.05f, horizon)
            }
            drawRect(dark.mix(a.horizon, 0.2f), Offset(0f, horizon), Size(w, h * 0.05f))
            // A crowd.
            for (k in 0 until 14) drawCircle(Color(0xFF050208), w * 0.045f, Offset(w * k / 13f, h - h * 0.02f * (k % 3)))
            // A guitar neck raised high.
            drawLine(a.light.mix(Color.White, 0.3f), Offset(w * 0.6f, horizon - h * 0.05f), Offset(w * 0.8f, top + h * 0.15f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
        }
        Scene.JUNGLE -> {
            poly(Color(0xFF3E2723), w * 0.25f, horizon + h * 0.05f, w * 0.45f, top + h * 0.22f, w * 0.58f, top + h * 0.22f, w * 0.88f, horizon + h * 0.05f)
            poly(Color(0xFFFF5722), w * 0.47f, top + h * 0.22f, w * 0.56f, top + h * 0.22f, w * 0.54f, top + h * 0.32f, w * 0.49f, top + h * 0.29f)
            for (k in 0..3) drawCircle(Color(0xFF5D4037).copy(alpha = 0.6f), w * (0.07f + k * 0.02f), Offset(w * (0.52f + k * 0.04f), top + h * (0.17f - k * 0.04f)))
            for (k in 0..7) drawCircle(Color(0xFF1B5E20).shade(0.8f + 0.05f * (k % 3)), w * 0.11f, Offset(w * k / 7f, horizon + h * 0.05f))
            // A long neck in the trees.
            val nx = w * 0.18f
            drawLine(Color(0xFF2E3B2A), Offset(nx, horizon + h * 0.06f), Offset(nx + w * 0.06f, top + h * 0.3f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
            drawOval(Color(0xFF2E3B2A), Offset(nx + w * 0.03f, top + h * 0.27f), Size(w * 0.1f, h * 0.035f))
        }
        Scene.MECH -> {
            for (k in 0..8) {
                drawLine(a.light.copy(alpha = 0.12f), Offset(w * k / 8f, top), Offset(w * k / 8f, h), strokeWidth = 1.5f)
                drawLine(a.light.copy(alpha = 0.12f), Offset(0f, top + (h - top) * k / 8f), Offset(w, top + (h - top) * k / 8f), strokeWidth = 1.5f)
            }
            val c = Offset(w * 0.5f, top + h * 0.3f)
            drawRoundRect(Color(0xFF90A4AE), Offset(c.x - w * 0.28f, c.y - h * 0.12f), Size(w * 0.56f, h * 0.26f), CornerRadius(w * 0.08f))
            drawRoundRect(Color(0xFF263238), Offset(c.x - w * 0.22f, c.y - h * 0.07f), Size(w * 0.44f, h * 0.12f), CornerRadius(w * 0.05f))
            for (dx in listOf(-0.1f, 0.1f)) {
                glow(Offset(c.x + w * dx, c.y - h * 0.01f), w * 0.12f, a.light, 0.6f)
                drawCircle(a.light, w * 0.04f, Offset(c.x + w * dx, c.y - h * 0.01f))
            }
            drawLine(Color(0xFFB0BEC5), Offset(c.x, c.y - h * 0.12f), Offset(c.x, c.y - h * 0.19f), strokeWidth = w * 0.02f)
            drawCircle(Color(0xFFFF5252), w * 0.025f, Offset(c.x, c.y - h * 0.2f))
        }
        Scene.DESERT -> {
            drawCircle(a.light, w * 0.18f, Offset(w * 0.5f, horizon - h * 0.06f))
            poly(Color(0xFFBF5B2C), 0f, horizon, w * 0.05f, horizon - h * 0.18f, w * 0.3f, horizon - h * 0.18f, w * 0.34f, horizon)
            poly(Color(0xFFA0461F), w * 0.66f, horizon, w * 0.72f, horizon - h * 0.14f, w * 0.95f, horizon - h * 0.14f, w, horizon)
            val cx = if (light.x > w / 2) w * 0.18f else w * 0.82f
            val cg = Color(0xFF2E5E2E)
            drawRoundRect(cg, Offset(cx - w * 0.035f, horizon - h * 0.12f), Size(w * 0.07f, h * 0.3f), CornerRadius(w * 0.035f))
            drawRoundRect(cg, Offset(cx - w * 0.11f, horizon - h * 0.06f), Size(w * 0.05f, h * 0.1f), CornerRadius(w * 0.025f))
            drawRoundRect(cg, Offset(cx + w * 0.06f, horizon - h * 0.09f), Size(w * 0.05f, h * 0.1f), CornerRadius(w * 0.025f))
        }
        Scene.WAR -> {
            for (k in 0..2) {
                val bx = w * (0.15f + k * 0.35f)
                poly(Color.White.copy(alpha = 0.1f), bx, horizon, bx - w * 0.15f + k * w * 0.1f, top, bx + w * 0.02f + k * w * 0.1f, top)
            }
            mountains(mid, horizon, w, h, rng, 0.1f)
            for (k in 0..3) glow(Offset(w * (0.1f + k * 0.28f), horizon), w * 0.12f, Color(0xFFFF8A3D), 0.4f)
            val tx = if (light.x > w / 2) w * 0.3f else w * 0.7f
            val ty = horizon + h * 0.04f
            drawRoundRect(dark.shade(0.7f), Offset(tx - w * 0.2f, ty - h * 0.05f), Size(w * 0.4f, h * 0.06f), CornerRadius(w * 0.03f))
            drawRoundRect(dark.shade(0.7f), Offset(tx - w * 0.09f, ty - h * 0.09f), Size(w * 0.18f, h * 0.05f), CornerRadius(w * 0.03f))
            drawLine(dark.shade(0.7f), Offset(tx, ty - h * 0.07f), Offset(tx + w * 0.26f, ty - h * 0.1f), strokeWidth = w * 0.018f)
        }
        Scene.RUNWAY -> {
            for (k in 0..3) {
                val sx = w * (0.1f + k * 0.27f)
                poly(a.light.copy(alpha = 0.16f), sx, top, sx - w * 0.1f, horizon, sx + w * 0.1f, horizon)
            }
            poly(Color(0xFF2A1430), w * 0.4f, horizon - h * 0.02f, w * 0.6f, horizon - h * 0.02f, w * 0.85f, h, w * 0.15f, h)
            drawLine(a.light.copy(alpha = 0.5f), Offset(w * 0.5f, horizon), Offset(w * 0.5f, h), strokeWidth = w * 0.006f)
            for (k in 0 until 30) {
                val f = Offset(w * rng.nextFloat(), horizon - h * 0.1f * rng.nextFloat())
                drawCircle(Color.White.copy(alpha = 0.8f), w * 0.008f, f)
            }
        }
    }
}

// Heroes, one per genre

/** The hero in front of the scene, in silhouette, with a rim of the scene's light on the side facing it. */
private fun DrawScope.hero(genre: Genre, x: Float, base: Float, size: Float, a: Art, light: Offset, mirror: Boolean) {
    val body = a.ground.shade(0.5f).mix(Color.Black, 0.4f)
    val rim = a.light
    val towardLight = if (light.x > x) 1f else -1f
    fun shape(c: Color, dx: Float) = withTransform({
        translate(x + dx, base)
        if (mirror) scale(-1f, 1f, Offset.Zero)
    }) { figure(genre, size, c, a) }
    if (genre == Genre.CASUAL) {
        withTransform({ translate(x, base) }) { mascot(size, a) }
        return
    }
    shape(rim, towardLight * size * 0.025f)
    shape(body, 0f)
    // What the hero holds glows.
    val glowAt = when (genre) {
        Genre.RPG -> Offset(x + (if (mirror) -1 else 1) * size * 0.32f, base - size * 1.05f)
        Genre.ADVENTURE -> Offset(x + (if (mirror) -1 else 1) * size * 0.3f, base - size * 0.5f)
        Genre.ACTION -> Offset(x + (if (mirror) -1 else 1) * size * 0.42f, base - size * 1.02f)
        else -> null
    }
    glowAt?.let {
        glow(it, size * 0.35f, rim, 0.7f)
        drawCircle(rim.mix(Color.White, 0.5f), size * 0.045f, it)
    }
}

/** A hero in local space: feet at the origin, about [s] tall, facing right. */
private fun DrawScope.figure(genre: Genre, s: Float, c: Color, a: Art) {
    fun limb(x0: Float, y0: Float, x1: Float, y1: Float, wd: Float) =
        drawLine(c, Offset(x0 * s, -y0 * s), Offset(x1 * s, -y1 * s), strokeWidth = wd * s, cap = StrokeCap.Round)
    when (genre) {
        Genre.ACTION -> {
            // Legs apart, sword raised.
            limb(-0.08f, 0f, -0.02f, 0.45f, 0.1f)
            limb(0.14f, 0f, 0.04f, 0.45f, 0.1f)
            poly(c, -0.12f * s, -0.44f * s, 0.12f * s, -0.44f * s, 0.16f * s, -0.78f * s, -0.16f * s, -0.78f * s)
            drawCircle(c, 0.1f * s, Offset(0f, -0.88f * s))
            limb(-0.13f, 0.74f, -0.24f, 0.5f, 0.08f)
            limb(0.13f, 0.74f, 0.3f, 0.86f, 0.08f)
            limb(0.3f, 0.86f, 0.44f, 1.08f, 0.035f)
            // A cape.
            poly(c, -0.14f * s, -0.76f * s, -0.36f * s, -0.3f * s, -0.12f * s, -0.42f * s)
        }
        Genre.ADVENTURE -> {
            // Hat, pack and lantern, mid-stride.
            limb(-0.06f, 0f, 0f, 0.42f, 0.09f)
            limb(0.16f, 0f, 0.05f, 0.42f, 0.09f)
            poly(c, -0.1f * s, -0.42f * s, 0.12f * s, -0.42f * s, 0.13f * s, -0.76f * s, -0.12f * s, -0.76f * s)
            drawRoundRect(c, Offset(-0.24f * s, -0.74f * s), Size(0.14f * s, 0.26f * s), CornerRadius(0.04f * s))
            drawCircle(c, 0.09f * s, Offset(0.01f * s, -0.85f * s))
            drawOval(c, Offset(-0.17f * s, -0.92f * s), Size(0.36f * s, 0.06f * s))
            drawRoundRect(c, Offset(-0.08f * s, -1.0f * s), Size(0.18f * s, 0.1f * s), CornerRadius(0.03f * s))
            limb(0.11f, 0.72f, 0.28f, 0.56f, 0.07f)
            limb(0.28f, 0.56f, 0.3f, 0.5f, 0.02f)
        }
        Genre.RPG -> {
            // A hooded mage with a staff.
            poly(c, -0.2f * s, 0f, 0.2f * s, 0f, 0.1f * s, -0.8f * s, -0.1f * s, -0.8f * s)
            drawCircle(c, 0.11f * s, Offset(0f, -0.86f * s))
            poly(c, -0.11f * s, -0.86f * s, 0f, -1.04f * s, 0.11f * s, -0.86f * s)
            limb(0.1f, 0.7f, 0.3f, 0.62f, 0.07f)
            limb(0.32f, 0f, 0.32f, 1.02f, 0.035f)
        }
        Genre.STRATEGY -> {
            // A commander with a banner planted beside them.
            limb(-0.06f, 0f, -0.03f, 0.42f, 0.09f)
            limb(0.08f, 0f, 0.04f, 0.42f, 0.09f)
            poly(c, -0.12f * s, -0.42f * s, 0.12f * s, -0.42f * s, 0.14f * s, -0.76f * s, -0.14f * s, -0.76f * s)
            drawCircle(c, 0.09f * s, Offset(0f, -0.86f * s))
            poly(c, -0.14f * s, -0.74f * s, -0.3f * s, -0.1f * s, -0.04f * s, -0.42f * s)
            limb(0.36f, 0f, 0.36f, 1.15f, 0.03f)
            poly(a.light.shade(0.85f), 0.37f * s, -1.13f * s, 0.7f * s, -1.05f * s, 0.62f * s, -0.95f * s, 0.7f * s, -0.85f * s, 0.37f * s, -0.85f * s)
            limb(0.12f, 0.72f, 0.35f, 0.7f, 0.07f)
        }
        Genre.SIMULATION -> {
            // Hands on hips, surveying what they've built.
            limb(-0.07f, 0f, -0.04f, 0.42f, 0.1f)
            limb(0.07f, 0f, 0.04f, 0.42f, 0.1f)
            poly(c, -0.13f * s, -0.42f * s, 0.13f * s, -0.42f * s, 0.15f * s, -0.78f * s, -0.15f * s, -0.78f * s)
            drawCircle(c, 0.1f * s, Offset(0f, -0.88f * s))
            limb(-0.14f, 0.76f, -0.26f, 0.6f, 0.07f)
            limb(-0.26f, 0.6f, -0.13f, 0.48f, 0.06f)
            limb(0.14f, 0.76f, 0.26f, 0.6f, 0.07f)
            limb(0.26f, 0.6f, 0.13f, 0.48f, 0.06f)
        }
        Genre.CASUAL -> {}
    }
}

/** For casual games: a round, cheerful mascot instead of a silhouette. */
private fun DrawScope.mascot(s: Float, a: Art) {
    val c = a.light.mix(Color(0xFFFF7AB6), 0.5f)
    val r = s * 0.32f
    val center = Offset(0f, -r * 1.05f)
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(-r * 0.9f, -r * 0.15f), Size(r * 1.8f, r * 0.3f))
    drawCircle(Brush.radialGradient(listOf(c.mix(Color.White, 0.45f), c, c.shade(0.7f)), center - Offset(r * 0.35f, r * 0.35f), r * 1.6f), r, center)
    for (dx in listOf(-0.35f, 0.35f)) {
        drawCircle(Color.White, r * 0.22f, center + Offset(dx * r, -r * 0.12f))
        drawCircle(Color(0xFF1E1E2A), r * 0.12f, center + Offset(dx * r + r * 0.04f, -r * 0.1f))
        drawCircle(Color.White, r * 0.04f, center + Offset(dx * r + r * 0.08f, -r * 0.16f))
    }
    drawArc(Color(0xFF1E1E2A), 20f, 140f, false, center + Offset(-r * 0.25f, r * 0.05f), Size(r * 0.5f, r * 0.35f), style = Stroke(r * 0.08f, cap = StrokeCap.Round))
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.5f), r * 0.12f, center + Offset(-r * 0.62f, r * 0.15f))
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.5f), r * 0.12f, center + Offset(r * 0.62f, r * 0.15f))
}

// Atmosphere

private fun DrawScope.atmosphere(fx: Fx, light: Color, w: Float, h: Float, top: Float, rng: java.util.Random) {
    when (fx) {
        Fx.STARS -> repeat(45) {
            drawCircle(Color.White.copy(alpha = 0.3f + 0.6f * rng.nextFloat()), w * (0.003f + 0.006f * rng.nextFloat()), Offset(w * rng.nextFloat(), top + (h - top) * 0.6f * rng.nextFloat()))
        }
        Fx.EMBERS -> repeat(22) {
            val p = Offset(w * rng.nextFloat(), top + (h - top) * rng.nextFloat())
            glow(p, w * 0.03f, Color(0xFFFF9A3D), 0.6f)
            drawCircle(Color(0xFFFFD08A), w * 0.006f, p)
        }
        Fx.RAIN -> repeat(40) {
            val x = w * rng.nextFloat()
            val y = top + (h - top) * rng.nextFloat()
            drawLine(light.mix(Color.White, 0.5f).copy(alpha = 0.25f), Offset(x, y), Offset(x - w * 0.02f, y + h * 0.05f), strokeWidth = w * 0.004f)
        }
        Fx.FIREFLIES -> repeat(14) {
            val p = Offset(w * rng.nextFloat(), top + (h - top) * (0.3f + 0.6f * rng.nextFloat()))
            glow(p, w * 0.035f, Color(0xFFF4FF7A), 0.6f)
        }
        Fx.BUBBLES -> repeat(16) {
            val p = Offset(w * rng.nextFloat(), top + (h - top) * rng.nextFloat())
            drawCircle(Color.White.copy(alpha = 0.45f), w * (0.01f + 0.02f * rng.nextFloat()), p, style = Stroke(w * 0.004f))
        }
        Fx.CONFETTI -> repeat(30) {
            val colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF4ECDC4), Color(0xFF6C8CFF), Color(0xFFFF7AE0))
            val p = Offset(w * rng.nextFloat(), top + (h - top) * 0.7f * rng.nextFloat())
            withTransform({ rotate(360f * rng.nextFloat(), p) }) {
                drawRect(colors[it % colors.size], p, Size(w * 0.025f, w * 0.012f))
            }
        }
        Fx.SPARKLES -> repeat(12) {
            val p = Offset(w * rng.nextFloat(), top + (h - top) * 0.7f * rng.nextFloat())
            val r = w * (0.012f + 0.02f * rng.nextFloat())
            poly(light.mix(Color.White, 0.5f).copy(alpha = 0.85f), p.x, p.y - r, p.x + r * 0.25f, p.y - r * 0.25f, p.x + r, p.y, p.x + r * 0.25f, p.y + r * 0.25f, p.x, p.y + r, p.x - r * 0.25f, p.y + r * 0.25f, p.x - r, p.y, p.x - r * 0.25f, p.y - r * 0.25f)
        }
        Fx.NONE -> {}
    }
}
