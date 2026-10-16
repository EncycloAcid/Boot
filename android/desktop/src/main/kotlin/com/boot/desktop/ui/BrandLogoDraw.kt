package com.boot.desktop.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.boot.core.CompanyMark
import com.boot.core.LogoId
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Hand-drawn vector recreations of real logo silhouettes for small UI badges.
 * Paths approximate the public brand marks; nothing scraped as bitmap art.
 */

/** Paint [mark]'s logo into this draw scope (full size). */
fun DrawScope.drawBrandLogo(mark: CompanyMark) {
    val s = min(size.width, size.height)
    withTransform({
        translate((size.width - s) / 2f, (size.height - s) / 2f)
        scale(s, s, pivot = Offset.Zero)
    }) {
        when (mark.logo) {
            LogoId.APPLE -> apple()
            LogoId.MICROSOFT -> microsoft()
            LogoId.GOOGLE -> google()
            LogoId.NINTENDO -> nintendo()
            LogoId.SONY -> sony()
            LogoId.PLAYSTATION -> playstation()
            LogoId.XBOX -> xbox()
            LogoId.SEGA -> sega()
            LogoId.ATARI -> atari()
            LogoId.EA -> ea()
            LogoId.UBISOFT -> ubisoft()
            LogoId.ACTIVISION -> activision()
            LogoId.STEAM, LogoId.VALVE -> steam()
            LogoId.EPIC -> epic()
            LogoId.NVIDIA -> nvidia()
            LogoId.AMAZON -> amazon()
            LogoId.META -> meta()
            LogoId.FACEBOOK -> facebook()
            LogoId.OPENAI -> openai()
            LogoId.NETFLIX -> netflix()
            LogoId.TESLA -> tesla()
            LogoId.SPOTIFY -> spotify()
            LogoId.DISCORD -> discord()
            LogoId.REDDIT -> reddit()
            LogoId.TIKTOK -> tiktok()
            LogoId.TWITTER_X -> twitterX()
            LogoId.SNAP -> snap()
            LogoId.SEQUOIA -> sequoia()
            LogoId.Y_COMBINATOR -> yCombinator()
            LogoId.A16Z -> a16z()
            LogoId.SOFTBANK -> softbank()
            LogoId.KLEINER_PERKINS -> kleiner()
            LogoId.IGN -> ign()
            LogoId.TECHCRUNCH -> techcrunch()
            LogoId.THE_VERGE -> theVerge()
            LogoId.WIRED -> wired()
            LogoId.BLOOMBERG -> bloomberg()
            LogoId.REUTERS -> reuters()
            LogoId.CNBC -> cnbc()
            LogoId.IBM -> ibm()
            LogoId.COMMODORE -> commodore()
            LogoId.CAPCOM -> capcom()
            LogoId.KONAMI -> konami()
            LogoId.SQUARE_ENIX -> squareEnix()
            LogoId.BANDAI_NAMCO -> bandaiNamco()
            LogoId.ROCKSTAR -> rockstar()
            LogoId.TAKE_TWO -> takeTwo()
            LogoId.BETHESDA -> bethesda()
            LogoId.DEVOLVER -> devolver()
            LogoId.CDPR -> cdpr()
            LogoId.SUPERCELL -> supercell()
            LogoId.CANONICAL -> canonical()
            LogoId.SAMSUNG -> samsung()
            LogoId.DELL -> dell()
            LogoId.NOKIA -> nokia()
            LogoId.ROBLOX -> roblox()
            LogoId.PINTEREST -> pinterest()
            LogoId.SHOPIFY -> shopify()
            LogoId.DUOLINGO -> duolingo()
            LogoId.WORDPRESS -> wordpress()
            LogoId.SQUARESPACE -> squarespace()
            LogoId.WIX -> wix()
            LogoId.TENCENT -> tencent()
            LogoId.ANTHROPIC -> anthropic()
            LogoId.XAI -> xai()
            LogoId.MYSPACE -> myspace()
            LogoId.AOL -> aol()
            LogoId.YAHOO -> yahoo()
            LogoId.GAMESPOT -> gamespot()
            LogoId.EUROGAMER -> eurogamer()
            LogoId.PC_GAMER -> pcGamer()
            LogoId.EDGE -> edgeMag()
            LogoId.KOTAKU -> kotaku()
            LogoId.POLYGON -> polygon()
            LogoId.ARS -> ars()
            LogoId.ENGADGET -> engadget()
            LogoId.NYT -> nyt()
            LogoId.WSJ -> wsj()
            LogoId.FT -> ft()
            LogoId.BYTE -> byteMag()
            LogoId.CGW -> cgw()
            LogoId.NONE -> fallbackMonogram(mark)
        }
    }
}

private fun DrawScope.fallbackMonogram(mark: CompanyMark) {
    drawRoundRect(Color(mark.bg), Offset(0.08f, 0.08f), Size(0.84f, 0.84f), CornerRadius(0.18f))
}

// —— Majors ——

private fun DrawScope.apple() {
    // Classic apple silhouette with leaf + bite (unit square).
    val body = Path().apply {
        moveTo(0.52f, 0.18f)
        cubicTo(0.48f, 0.10f, 0.38f, 0.08f, 0.34f, 0.14f)
        cubicTo(0.22f, 0.12f, 0.10f, 0.24f, 0.12f, 0.42f)
        cubicTo(0.14f, 0.62f, 0.22f, 0.82f, 0.36f, 0.90f)
        cubicTo(0.42f, 0.94f, 0.48f, 0.88f, 0.52f, 0.88f)
        cubicTo(0.56f, 0.88f, 0.62f, 0.94f, 0.68f, 0.90f)
        cubicTo(0.82f, 0.82f, 0.90f, 0.62f, 0.88f, 0.44f)
        // Bite
        cubicTo(0.78f, 0.46f, 0.68f, 0.38f, 0.68f, 0.28f)
        cubicTo(0.68f, 0.20f, 0.74f, 0.14f, 0.82f, 0.14f)
        cubicTo(0.72f, 0.06f, 0.58f, 0.08f, 0.52f, 0.18f)
        close()
    }
    drawPath(body, Color(0xFF212121))
    val leaf = Path().apply {
        moveTo(0.52f, 0.18f)
        cubicTo(0.56f, 0.06f, 0.68f, 0.02f, 0.74f, 0.10f)
        cubicTo(0.66f, 0.14f, 0.58f, 0.16f, 0.52f, 0.18f)
        close()
    }
    drawPath(leaf, Color(0xFF212121))
}

private fun DrawScope.microsoft() {
    val gap = 0.04f
    val cell = (1f - gap) / 2f
    drawRect(Color(0xFFF25022), Offset(0f, 0f), Size(cell, cell))
    drawRect(Color(0xFF7FBA00), Offset(cell + gap, 0f), Size(cell, cell))
    drawRect(Color(0xFF00A4EF), Offset(0f, cell + gap), Size(cell, cell))
    drawRect(Color(0xFFFFB900), Offset(cell + gap, cell + gap), Size(cell, cell))
}

private fun DrawScope.google() {
    // Multicolor G: ring with open gap and blue bar.
    val stroke = Stroke(width = 0.16f, cap = StrokeCap.Butt)
    drawArc(Color(0xFFEA4335), -20f, 100f, false, Offset(0.12f, 0.12f), Size(0.76f, 0.76f), style = stroke)
    drawArc(Color(0xFFFBBC05), 80f, 70f, false, Offset(0.12f, 0.12f), Size(0.76f, 0.76f), style = stroke)
    drawArc(Color(0xFF34A853), 150f, 80f, false, Offset(0.12f, 0.12f), Size(0.76f, 0.76f), style = stroke)
    drawArc(Color(0xFF4285F4), 230f, 110f, false, Offset(0.12f, 0.12f), Size(0.76f, 0.76f), style = stroke)
    drawRect(Color(0xFF4285F4), Offset(0.50f, 0.42f), Size(0.38f, 0.16f))
}

private fun DrawScope.nintendo() {
    // Red stadium (classic oval wordmark capsule) with white N.
    val capsule = Path().apply {
        addRoundRect(RoundRect(Rect(0.06f, 0.22f, 0.94f, 0.78f), CornerRadius(0.28f)))
    }
    drawPath(capsule, Color(0xFFE60012))
    // Stylized N
    val n = Path().apply {
        moveTo(0.28f, 0.70f)
        lineTo(0.28f, 0.30f)
        lineTo(0.40f, 0.30f)
        lineTo(0.60f, 0.58f)
        lineTo(0.60f, 0.30f)
        lineTo(0.72f, 0.30f)
        lineTo(0.72f, 0.70f)
        lineTo(0.60f, 0.70f)
        lineTo(0.40f, 0.42f)
        lineTo(0.40f, 0.70f)
        close()
    }
    drawPath(n, Color.White)
}

private fun DrawScope.sony() {
    drawRoundRect(Color(0xFF212121), Offset(0.06f, 0.28f), Size(0.88f, 0.44f), CornerRadius(0.08f))
    // SONY bars as four white blocks approximating the wordmark rhythm
    drawRoundRect(Color.White, Offset(0.14f, 0.40f), Size(0.14f, 0.20f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.32f, 0.40f), Size(0.16f, 0.20f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.52f, 0.40f), Size(0.14f, 0.20f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.70f, 0.40f), Size(0.16f, 0.20f), CornerRadius(0.03f))
}

private fun DrawScope.playstation() {
    drawCircle(Color(0xFF003087), 0.48f, Offset(0.5f, 0.5f))
    // Simplified P / like mark: upright stem + bowl
    val p = Path().apply {
        moveTo(0.32f, 0.22f)
        lineTo(0.44f, 0.22f)
        lineTo(0.44f, 0.78f)
        lineTo(0.32f, 0.78f)
        close()
        moveTo(0.44f, 0.22f)
        lineTo(0.62f, 0.22f)
        cubicTo(0.78f, 0.22f, 0.78f, 0.48f, 0.62f, 0.48f)
        lineTo(0.44f, 0.48f)
        close()
    }
    drawPath(p, Color.White)
    // Small S hint
    drawArc(Color(0xFF00A9E0), 200f, 140f, false, Offset(0.52f, 0.52f), Size(0.28f, 0.28f), style = Stroke(0.06f))
}

private fun DrawScope.xbox() {
    drawCircle(Color(0xFF107C10), 0.48f, Offset(0.5f, 0.5f))
    // Sphere-X: two arcs forming the ball X
    val xMark = Path().apply {
        moveTo(0.22f, 0.28f)
        cubicTo(0.40f, 0.42f, 0.60f, 0.42f, 0.78f, 0.28f)
        lineTo(0.72f, 0.38f)
        cubicTo(0.58f, 0.48f, 0.42f, 0.48f, 0.28f, 0.38f)
        close()
        moveTo(0.22f, 0.72f)
        cubicTo(0.40f, 0.58f, 0.60f, 0.58f, 0.78f, 0.72f)
        lineTo(0.72f, 0.62f)
        cubicTo(0.58f, 0.52f, 0.42f, 0.52f, 0.28f, 0.62f)
        close()
    }
    drawPath(xMark, Color.White)
    drawLine(Color.White, Offset(0.30f, 0.30f), Offset(0.70f, 0.70f), strokeWidth = 0.10f, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(0.70f, 0.30f), Offset(0.30f, 0.70f), strokeWidth = 0.10f, cap = StrokeCap.Round)
}

private fun DrawScope.sega() {
    drawRoundRect(Color(0xFF0050A0), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.08f))
    // SEGA italic blocks
    fun block(x: Float, skew: Float = 0.04f) {
        val p = Path().apply {
            moveTo(x + skew, 0.34f)
            lineTo(x + 0.18f + skew, 0.34f)
            lineTo(x + 0.18f, 0.66f)
            lineTo(x, 0.66f)
            close()
        }
        drawPath(p, Color.White)
    }
    block(0.12f); block(0.32f); block(0.52f); block(0.72f)
}

private fun DrawScope.atari() {
    // Fuji mark: three peaks
    drawRect(Color(0xFFE53935), Offset.Zero, Size(1f, 1f))
    val fuji = Path().apply {
        moveTo(0.12f, 0.82f)
        lineTo(0.28f, 0.22f)
        lineTo(0.40f, 0.22f)
        lineTo(0.50f, 0.55f)
        lineTo(0.60f, 0.22f)
        lineTo(0.72f, 0.22f)
        lineTo(0.88f, 0.82f)
        close()
        // cut middle notch already via peaks
    }
    drawPath(fuji, Color.White)
    // Carve the valleys darker red for the classic three-pillar look
    val cut = Path().apply {
        moveTo(0.38f, 0.82f)
        lineTo(0.50f, 0.48f)
        lineTo(0.62f, 0.82f)
        close()
    }
    drawPath(cut, Color(0xFFE53935))
}

private fun DrawScope.ea() {
    drawRoundRect(Color.Black, Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Interlocking EA: E bars + A chevron
    val e = Path().apply {
        moveTo(0.22f, 0.28f)
        lineTo(0.52f, 0.28f)
        lineTo(0.52f, 0.38f)
        lineTo(0.34f, 0.38f)
        lineTo(0.34f, 0.45f)
        lineTo(0.48f, 0.45f)
        lineTo(0.48f, 0.55f)
        lineTo(0.34f, 0.55f)
        lineTo(0.34f, 0.62f)
        lineTo(0.52f, 0.62f)
        lineTo(0.52f, 0.72f)
        lineTo(0.22f, 0.72f)
        close()
    }
    drawPath(e, Color.White)
    val a = Path().apply {
        moveTo(0.50f, 0.72f)
        lineTo(0.64f, 0.28f)
        lineTo(0.76f, 0.28f)
        lineTo(0.90f, 0.72f)
        lineTo(0.78f, 0.72f)
        lineTo(0.75f, 0.60f)
        lineTo(0.65f, 0.60f)
        lineTo(0.62f, 0.72f)
        close()
        moveTo(0.67f, 0.50f)
        lineTo(0.70f, 0.38f)
        lineTo(0.73f, 0.50f)
        close()
    }
    drawPath(a, Color.White)
}

private fun DrawScope.ubisoft() {
    drawCircle(Color(0xFF1A237E), 0.48f, Offset(0.5f, 0.5f))
    // Spiral swirl (concentric arcs)
    for (i in 0..3) {
        val inset = 0.12f + i * 0.08f
        drawArc(
            Color.White.copy(alpha = 0.95f - i * 0.15f),
            startAngle = -40f + i * 35f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(1f - inset * 2, 1f - inset * 2),
            style = Stroke(width = 0.07f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.activision() {
    drawRoundRect(Color(0xFF0B5CAB), Offset(0.04f, 0.18f), Size(0.92f, 0.64f), CornerRadius(0.08f))
    // Blue wordmark bar with white "A" chevron motif
    val a = Path().apply {
        moveTo(0.20f, 0.70f)
        lineTo(0.42f, 0.30f)
        lineTo(0.58f, 0.30f)
        lineTo(0.80f, 0.70f)
        lineTo(0.66f, 0.70f)
        lineTo(0.61f, 0.58f)
        lineTo(0.39f, 0.58f)
        lineTo(0.34f, 0.70f)
        close()
        moveTo(0.44f, 0.48f)
        lineTo(0.50f, 0.36f)
        lineTo(0.56f, 0.48f)
        close()
    }
    drawPath(a, Color.White)
}

private fun DrawScope.steam() {
    drawCircle(Color(0xFF171A21), 0.48f, Offset(0.5f, 0.5f))
    // Arm / pipe: circle joints + link
    drawCircle(Color(0xFFC7D5E0), 0.14f, Offset(0.38f, 0.62f))
    drawCircle(Color(0xFF171A21), 0.08f, Offset(0.38f, 0.62f))
    drawCircle(Color(0xFFC7D5E0), 0.10f, Offset(0.62f, 0.38f))
    drawCircle(Color(0xFF171A21), 0.05f, Offset(0.62f, 0.38f))
    drawLine(Color(0xFFC7D5E0), Offset(0.45f, 0.55f), Offset(0.58f, 0.42f), strokeWidth = 0.09f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF66C0F4), 0.06f, Offset(0.72f, 0.28f))
}

private fun DrawScope.epic() {
    drawRoundRect(Color(0xFF212121), Offset(0.08f, 0.08f), Size(0.84f, 0.84f), CornerRadius(0.16f))
    // Stylized E / black-white epic mark
    drawRect(Color.White, Offset(0.28f, 0.28f), Size(0.44f, 0.10f))
    drawRect(Color.White, Offset(0.28f, 0.45f), Size(0.36f, 0.10f))
    drawRect(Color.White, Offset(0.28f, 0.62f), Size(0.44f, 0.10f))
    drawRect(Color.White, Offset(0.28f, 0.28f), Size(0.10f, 0.44f))
}

private fun DrawScope.nvidia() {
    drawRoundRect(Color(0xFF76B900), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.18f))
    // Eye mark
    val eye = Path().apply {
        moveTo(0.18f, 0.55f)
        cubicTo(0.35f, 0.28f, 0.65f, 0.28f, 0.82f, 0.55f)
        cubicTo(0.65f, 0.48f, 0.35f, 0.48f, 0.18f, 0.55f)
        close()
    }
    drawPath(eye, Color(0xFF212121))
    drawCircle(Color(0xFF212121), 0.08f, Offset(0.50f, 0.52f))
    drawCircle(Color(0xFF76B900), 0.04f, Offset(0.50f, 0.52f))
}

private fun DrawScope.amazon() {
    drawRoundRect(Color(0xFF232F3E), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.16f))
    // a
    drawCircle(Color.White, 0.16f, Offset(0.42f, 0.48f), style = Stroke(0.08f))
    drawLine(Color.White, Offset(0.56f, 0.34f), Offset(0.56f, 0.64f), strokeWidth = 0.08f, cap = StrokeCap.Round)
    // Smile arrow
    drawArc(Color(0xFFFF9900), 200f, 140f, false, Offset(0.22f, 0.42f), Size(0.56f, 0.42f), style = Stroke(0.07f, cap = StrokeCap.Round))
    val tip = Path().apply {
        moveTo(0.72f, 0.68f)
        lineTo(0.82f, 0.62f)
        lineTo(0.74f, 0.78f)
        close()
    }
    drawPath(tip, Color(0xFFFF9900))
}

private fun DrawScope.meta() {
    drawCircle(Color(0xFF0866FF), 0.48f, Offset(0.5f, 0.5f))
    // Infinity loop
    val inf = Path().apply {
        moveTo(0.22f, 0.50f)
        cubicTo(0.22f, 0.32f, 0.36f, 0.28f, 0.50f, 0.42f)
        cubicTo(0.64f, 0.28f, 0.78f, 0.32f, 0.78f, 0.50f)
        cubicTo(0.78f, 0.68f, 0.64f, 0.72f, 0.50f, 0.58f)
        cubicTo(0.36f, 0.72f, 0.22f, 0.68f, 0.22f, 0.50f)
        close()
    }
    drawPath(inf, Color.White, style = Stroke(0.10f, join = StrokeJoin.Round))
}

private fun DrawScope.facebook() {
    drawRoundRect(Color(0xFF1877F2), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.18f))
    val f = Path().apply {
        moveTo(0.58f, 0.22f)
        lineTo(0.58f, 0.38f)
        lineTo(0.70f, 0.38f)
        lineTo(0.70f, 0.50f)
        lineTo(0.58f, 0.50f)
        lineTo(0.58f, 0.82f)
        lineTo(0.44f, 0.82f)
        lineTo(0.44f, 0.50f)
        lineTo(0.34f, 0.50f)
        lineTo(0.34f, 0.38f)
        lineTo(0.44f, 0.38f)
        lineTo(0.44f, 0.30f)
        cubicTo(0.44f, 0.18f, 0.52f, 0.16f, 0.62f, 0.18f)
        lineTo(0.70f, 0.20f)
        lineTo(0.70f, 0.32f)
        lineTo(0.58f, 0.30f)
        close()
    }
    drawPath(f, Color.White)
}

private fun DrawScope.openai() {
    drawCircle(Color(0xFF10A37F), 0.48f, Offset(0.5f, 0.5f))
    // Hexagonal blossom (6 petals)
    for (i in 0 until 6) {
        val a = Math.toRadians(60.0 * i - 90.0)
        val cx = 0.5f + 0.18f * cos(a).toFloat()
        val cy = 0.5f + 0.18f * sin(a).toFloat()
        drawCircle(Color.White, 0.10f, Offset(cx, cy), style = Stroke(0.045f))
    }
    drawCircle(Color.White, 0.08f, Offset(0.5f, 0.5f), style = Stroke(0.045f))
}

private fun DrawScope.netflix() {
    drawRoundRect(Color(0xFF000000), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.12f))
    // N ribbons
    val n = Path().apply {
        moveTo(0.28f, 0.78f)
        lineTo(0.28f, 0.22f)
        lineTo(0.42f, 0.22f)
        lineTo(0.58f, 0.58f)
        lineTo(0.58f, 0.22f)
        lineTo(0.72f, 0.22f)
        lineTo(0.72f, 0.78f)
        lineTo(0.58f, 0.78f)
        lineTo(0.42f, 0.42f)
        lineTo(0.42f, 0.78f)
        close()
    }
    drawPath(n, Color(0xFFE50914))
}

private fun DrawScope.tesla() {
    drawRoundRect(Color(0xFFCC0000), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // T wordmark / shield T
    val t = Path().apply {
        moveTo(0.20f, 0.28f)
        lineTo(0.80f, 0.28f)
        lineTo(0.80f, 0.40f)
        lineTo(0.58f, 0.40f)
        lineTo(0.58f, 0.78f)
        lineTo(0.42f, 0.78f)
        lineTo(0.42f, 0.40f)
        lineTo(0.20f, 0.40f)
        close()
    }
    drawPath(t, Color.White)
    // Tripoint above
    val tip = Path().apply {
        moveTo(0.50f, 0.14f)
        lineTo(0.62f, 0.28f)
        lineTo(0.38f, 0.28f)
        close()
    }
    drawPath(tip, Color.White)
}

private fun DrawScope.spotify() {
    drawCircle(Color(0xFF1DB954), 0.48f, Offset(0.5f, 0.5f))
    // Sound waves
    drawArc(Color.Black, 200f, 140f, false, Offset(0.22f, 0.28f), Size(0.56f, 0.36f), style = Stroke(0.07f, cap = StrokeCap.Round))
    drawArc(Color.Black, 200f, 140f, false, Offset(0.28f, 0.40f), Size(0.44f, 0.28f), style = Stroke(0.06f, cap = StrokeCap.Round))
    drawArc(Color.Black, 200f, 140f, false, Offset(0.34f, 0.50f), Size(0.32f, 0.22f), style = Stroke(0.055f, cap = StrokeCap.Round))
}

private fun DrawScope.discord() {
    drawRoundRect(Color(0xFF5865F2), Offset(0.06f, 0.10f), Size(0.88f, 0.80f), CornerRadius(0.28f))
    // Controller / Clyde face: body + eyes + antennae
    drawOval(Color.White, Offset(0.20f, 0.32f), Size(0.60f, 0.42f))
    drawCircle(Color(0xFF5865F2), 0.07f, Offset(0.38f, 0.50f))
    drawCircle(Color(0xFF5865F2), 0.07f, Offset(0.62f, 0.50f))
    drawLine(Color.White, Offset(0.30f, 0.28f), Offset(0.24f, 0.16f), strokeWidth = 0.06f, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(0.70f, 0.28f), Offset(0.76f, 0.16f), strokeWidth = 0.06f, cap = StrokeCap.Round)
}

private fun DrawScope.reddit() {
    drawCircle(Color(0xFFFF4500), 0.48f, Offset(0.5f, 0.5f))
    drawCircle(Color.White, 0.22f, Offset(0.5f, 0.58f))
    drawCircle(Color(0xFFFF4500), 0.05f, Offset(0.42f, 0.56f))
    drawCircle(Color(0xFFFF4500), 0.05f, Offset(0.58f, 0.56f))
    drawCircle(Color.White, 0.06f, Offset(0.72f, 0.28f))
    drawLine(Color.White, Offset(0.50f, 0.36f), Offset(0.68f, 0.28f), strokeWidth = 0.04f)
    drawArc(Color(0xFFFF4500), 20f, 140f, false, Offset(0.38f, 0.58f), Size(0.24f, 0.14f), style = Stroke(0.035f))
}

private fun DrawScope.tiktok() {
    drawRoundRect(Color(0xFF010101), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.16f))
    // Note silhouette with cyan/magenta offset (doodle)
    fun note(ox: Float, oy: Float, c: Color) {
        val p = Path().apply {
            moveTo(0.42f + ox, 0.22f + oy)
            lineTo(0.58f + ox, 0.18f + oy)
            lineTo(0.58f + ox, 0.55f + oy)
            cubicTo(0.58f + ox, 0.70f + oy, 0.38f + ox, 0.78f + oy, 0.34f + ox, 0.62f + oy)
            cubicTo(0.30f + ox, 0.48f + oy, 0.48f + ox, 0.48f + oy, 0.48f + ox, 0.58f + oy)
            lineTo(0.48f + ox, 0.28f + oy)
            lineTo(0.42f + ox, 0.30f + oy)
            close()
        }
        drawPath(p, c)
    }
    note(0.04f, 0.02f, Color(0xFF25F4EE))
    note(-0.04f, -0.02f, Color(0xFFFE2C55))
    note(0f, 0f, Color.White)
}

private fun DrawScope.twitterX() {
    drawRoundRect(Color.Black, Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.16f))
    val x = Path().apply {
        moveTo(0.24f, 0.24f)
        lineTo(0.42f, 0.24f)
        lineTo(0.52f, 0.40f)
        lineTo(0.62f, 0.24f)
        lineTo(0.80f, 0.24f)
        lineTo(0.58f, 0.52f)
        lineTo(0.82f, 0.80f)
        lineTo(0.64f, 0.80f)
        lineTo(0.52f, 0.62f)
        lineTo(0.40f, 0.80f)
        lineTo(0.22f, 0.80f)
        lineTo(0.46f, 0.50f)
        close()
    }
    drawPath(x, Color.White)
}

private fun DrawScope.snap() {
    drawCircle(Color(0xFFFFFC00), 0.48f, Offset(0.5f, 0.5f))
    // Ghost
    val ghost = Path().apply {
        moveTo(0.28f, 0.55f)
        cubicTo(0.28f, 0.30f, 0.72f, 0.30f, 0.72f, 0.55f)
        lineTo(0.72f, 0.72f)
        lineTo(0.64f, 0.66f)
        lineTo(0.56f, 0.72f)
        lineTo(0.50f, 0.66f)
        lineTo(0.44f, 0.72f)
        lineTo(0.36f, 0.66f)
        lineTo(0.28f, 0.72f)
        close()
    }
    drawPath(ghost, Color(0xFF212121))
}

private fun DrawScope.sequoia() {
    drawRoundRect(Color(0xFF0B3D2E), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Tree silhouette
    val tree = Path().apply {
        moveTo(0.50f, 0.14f)
        lineTo(0.68f, 0.38f)
        lineTo(0.58f, 0.38f)
        lineTo(0.72f, 0.58f)
        lineTo(0.60f, 0.58f)
        lineTo(0.76f, 0.78f)
        lineTo(0.24f, 0.78f)
        lineTo(0.40f, 0.58f)
        lineTo(0.28f, 0.58f)
        lineTo(0.42f, 0.38f)
        lineTo(0.32f, 0.38f)
        close()
    }
    drawPath(tree, Color(0xFFA8D08D))
    drawRect(Color(0xFF8B5A2B), Offset(0.46f, 0.78f), Size(0.08f, 0.10f))
}

private fun DrawScope.yCombinator() {
    drawRoundRect(Color(0xFFF26522), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.12f))
    // YC letters as shapes
    val y = Path().apply {
        moveTo(0.22f, 0.22f)
        lineTo(0.36f, 0.22f)
        lineTo(0.44f, 0.40f)
        lineTo(0.52f, 0.22f)
        lineTo(0.66f, 0.22f)
        lineTo(0.50f, 0.50f)
        lineTo(0.50f, 0.78f)
        lineTo(0.38f, 0.78f)
        lineTo(0.38f, 0.50f)
        close()
    }
    drawPath(y, Color.White)
    drawRect(Color.White, Offset(0.58f, 0.38f), Size(0.20f, 0.10f))
    drawRect(Color.White, Offset(0.64f, 0.38f), Size(0.10f, 0.40f))
}

private fun DrawScope.a16z() {
    drawRoundRect(Color(0xFF212121), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.08f))
    // a16z as four white ticks
    drawRoundRect(Color.White, Offset(0.12f, 0.38f), Size(0.14f, 0.24f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.30f, 0.38f), Size(0.10f, 0.24f), CornerRadius(0.02f))
    drawRoundRect(Color.White, Offset(0.44f, 0.38f), Size(0.16f, 0.24f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.64f, 0.38f), Size(0.22f, 0.24f), CornerRadius(0.03f))
}

private fun DrawScope.softbank() {
    drawRoundRect(Color(0xFFA6CE39), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Silver / gray S ribbon
    drawArc(Color(0xFF212121), 200f, 220f, false, Offset(0.22f, 0.22f), Size(0.56f, 0.56f), style = Stroke(0.12f, cap = StrokeCap.Round))
}

private fun DrawScope.kleiner() {
    drawRoundRect(Color(0xFF1B5E20), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color(0xFFA5D6A7), 0.18f, Offset(0.50f, 0.42f))
    drawRect(Color(0xFFA5D6A7), Offset(0.42f, 0.55f), Size(0.16f, 0.28f))
}

private fun DrawScope.ign() {
    drawRoundRect(Color(0xFFD32F2F), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.10f))
    drawRoundRect(Color.White, Offset(0.14f, 0.36f), Size(0.18f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.38f, 0.36f), Size(0.20f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.64f, 0.36f), Size(0.22f, 0.28f), CornerRadius(0.03f))
}

private fun DrawScope.techcrunch() {
    drawRoundRect(Color(0xFF0A9E01), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // TC
    drawRect(Color.White, Offset(0.22f, 0.28f), Size(0.28f, 0.10f))
    drawRect(Color.White, Offset(0.31f, 0.28f), Size(0.10f, 0.44f))
    drawArc(Color.White, 40f, 280f, false, Offset(0.52f, 0.34f), Size(0.30f, 0.36f), style = Stroke(0.10f))
}

private fun DrawScope.theVerge() {
    drawRoundRect(Color(0xFF1A1A1A), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // V chevron
    val v = Path().apply {
        moveTo(0.22f, 0.28f)
        lineTo(0.38f, 0.28f)
        lineTo(0.50f, 0.62f)
        lineTo(0.62f, 0.28f)
        lineTo(0.78f, 0.28f)
        lineTo(0.56f, 0.78f)
        lineTo(0.44f, 0.78f)
        close()
    }
    drawPath(v, Color(0xFFE01A22))
}

private fun DrawScope.wired() {
    drawRoundRect(Color(0xFF212121), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.10f))
    drawRect(Color.White, Offset(0.22f, 0.28f), Size(0.56f, 0.10f))
    drawRect(Color.White, Offset(0.45f, 0.28f), Size(0.10f, 0.44f))
}

private fun DrawScope.bloomberg() {
    drawRoundRect(Color(0xFF2800D7), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.12f))
    drawRoundRect(Color.White, Offset(0.28f, 0.28f), Size(0.44f, 0.44f), CornerRadius(0.06f))
    drawRect(Color(0xFF2800D7), Offset(0.40f, 0.40f), Size(0.20f, 0.20f))
}

private fun DrawScope.reuters() {
    drawCircle(Color(0xFFFF8000), 0.48f, Offset(0.5f, 0.5f))
    drawCircle(Color.White, 0.22f, Offset(0.5f, 0.5f), style = Stroke(0.08f))
    drawCircle(Color.White, 0.08f, Offset(0.5f, 0.5f))
}

private fun DrawScope.cnbc() {
    drawRoundRect(Color(0xFF005594), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.08f))
    drawRoundRect(Color.White, Offset(0.12f, 0.36f), Size(0.18f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.34f, 0.36f), Size(0.18f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.56f, 0.36f), Size(0.14f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.74f, 0.36f), Size(0.14f, 0.28f), CornerRadius(0.03f))
}

private fun DrawScope.ibm() {
    drawRoundRect(Color(0xFF0D47A1), Offset(0.06f, 0.18f), Size(0.88f, 0.64f), CornerRadius(0.06f))
    // 8-bar IBM
    for (i in 0 until 8) {
        val y = 0.26f + i * 0.07f
        drawRect(Color.White, Offset(0.18f, y), Size(0.64f, 0.04f))
    }
}

private fun DrawScope.commodore() {
    drawCircle(Color(0xFF1565C0), 0.48f, Offset(0.5f, 0.5f))
    // C= mark
    drawArc(Color.White, 40f, 280f, false, Offset(0.22f, 0.22f), Size(0.46f, 0.56f), style = Stroke(0.10f))
    drawRect(Color.White, Offset(0.58f, 0.38f), Size(0.22f, 0.08f))
    drawRect(Color.White, Offset(0.58f, 0.54f), Size(0.22f, 0.08f))
}

private fun DrawScope.capcom() {
    drawRoundRect(Color(0xFFC62828), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawArc(Color.White, 40f, 280f, false, Offset(0.22f, 0.22f), Size(0.56f, 0.56f), style = Stroke(0.12f))
}

private fun DrawScope.konami() {
    drawRoundRect(Color(0xFFB71C1C), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Bat / K wing silhouette simplified as K
    val k = Path().apply {
        moveTo(0.28f, 0.22f)
        lineTo(0.42f, 0.22f)
        lineTo(0.42f, 0.45f)
        lineTo(0.62f, 0.22f)
        lineTo(0.78f, 0.22f)
        lineTo(0.52f, 0.50f)
        lineTo(0.78f, 0.78f)
        lineTo(0.62f, 0.78f)
        lineTo(0.42f, 0.55f)
        lineTo(0.42f, 0.78f)
        lineTo(0.28f, 0.78f)
        close()
    }
    drawPath(k, Color.White)
}

private fun DrawScope.squareEnix() {
    drawRoundRect(Color(0xFF212121), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.10f))
    drawRect(Color.White, Offset(0.22f, 0.22f), Size(0.56f, 0.12f))
    drawRect(Color.White, Offset(0.22f, 0.22f), Size(0.12f, 0.56f))
    drawRect(Color.White, Offset(0.22f, 0.66f), Size(0.56f, 0.12f))
}

private fun DrawScope.bandaiNamco() {
    drawRoundRect(Color(0xFFF9A825), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color(0xFF212121), 0.22f, Offset(0.5f, 0.5f), style = Stroke(0.10f))
    drawCircle(Color(0xFF212121), 0.08f, Offset(0.5f, 0.5f))
}

private fun DrawScope.rockstar() {
    val diamond = Path().apply {
        moveTo(0.50f, 0.06f)
        lineTo(0.94f, 0.50f)
        lineTo(0.50f, 0.94f)
        lineTo(0.06f, 0.50f)
        close()
    }
    drawPath(diamond, Color(0xFFFFD600))
    // R star
    val r = Path().apply {
        moveTo(0.38f, 0.68f)
        lineTo(0.38f, 0.32f)
        lineTo(0.55f, 0.32f)
        cubicTo(0.68f, 0.32f, 0.68f, 0.50f, 0.55f, 0.50f)
        lineTo(0.38f, 0.50f)
        close()
        moveTo(0.50f, 0.50f)
        lineTo(0.66f, 0.68f)
        lineTo(0.54f, 0.68f)
        lineTo(0.42f, 0.52f)
        close()
    }
    drawPath(r, Color(0xFF212121))
}

private fun DrawScope.takeTwo() {
    drawRoundRect(Color(0xFF6A1B9A), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawRoundRect(Color.White, Offset(0.22f, 0.28f), Size(0.22f, 0.44f), CornerRadius(0.04f))
    drawRoundRect(Color.White, Offset(0.52f, 0.28f), Size(0.26f, 0.44f), CornerRadius(0.04f))
}

private fun DrawScope.bethesda() {
    drawRoundRect(Color(0xFF37474F), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.12f))
    // Scroll B
    drawArc(Color.White, 90f, 180f, false, Offset(0.28f, 0.22f), Size(0.40f, 0.28f), style = Stroke(0.08f))
    drawArc(Color.White, 90f, 180f, false, Offset(0.28f, 0.48f), Size(0.44f, 0.30f), style = Stroke(0.08f))
    drawRect(Color.White, Offset(0.28f, 0.22f), Size(0.08f, 0.56f))
}

private fun DrawScope.devolver() {
    drawRoundRect(Color(0xFFE65100), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Cowboy / skull-ish D
    drawCircle(Color.White, 0.22f, Offset(0.5f, 0.48f))
    drawCircle(Color(0xFFE65100), 0.06f, Offset(0.42f, 0.46f))
    drawCircle(Color(0xFFE65100), 0.06f, Offset(0.58f, 0.46f))
    drawRect(Color.White, Offset(0.30f, 0.22f), Size(0.40f, 0.10f))
}

private fun DrawScope.cdpr() {
    drawRoundRect(Color(0xFFE53935), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color.White, 0.28f, Offset(0.5f, 0.5f), style = Stroke(0.10f))
    drawRect(Color.White, Offset(0.22f, 0.45f), Size(0.56f, 0.10f))
}

private fun DrawScope.supercell() {
    drawRoundRect(Color(0xFFFFEB3B), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.18f))
    // Soft supercell blob
    drawOval(Color(0xFF212121), Offset(0.28f, 0.30f), Size(0.44f, 0.40f))
    drawCircle(Color(0xFFFFEB3B), 0.06f, Offset(0.40f, 0.46f))
    drawCircle(Color(0xFFFFEB3B), 0.06f, Offset(0.58f, 0.46f))
}

private fun DrawScope.canonical() {
    drawCircle(Color(0xFFE95420), 0.48f, Offset(0.5f, 0.5f))
    // Circle of friends
    for (i in 0 until 3) {
        val a = Math.toRadians(120.0 * i - 90.0)
        drawCircle(Color.White, 0.10f, Offset(0.5f + 0.22f * cos(a).toFloat(), 0.5f + 0.22f * sin(a).toFloat()))
    }
}

private fun DrawScope.samsung() {
    drawRoundRect(Color(0xFF1428A0), Offset(0.04f, 0.28f), Size(0.92f, 0.44f), CornerRadius(0.22f))
    drawRoundRect(Color.White, Offset(0.12f, 0.36f), Size(0.76f, 0.28f), CornerRadius(0.14f))
    drawRoundRect(Color(0xFF1428A0), Offset(0.20f, 0.42f), Size(0.60f, 0.16f), CornerRadius(0.08f))
}

private fun DrawScope.dell() {
    drawCircle(Color(0xFF007DB8), 0.48f, Offset(0.5f, 0.5f))
    drawArc(Color.White, 40f, 280f, false, Offset(0.18f, 0.18f), Size(0.64f, 0.64f), style = Stroke(0.10f))
    drawRect(Color.White, Offset(0.42f, 0.28f), Size(0.16f, 0.44f))
}

private fun DrawScope.nokia() {
    drawRoundRect(Color(0xFF124191), Offset(0.06f, 0.22f), Size(0.88f, 0.56f), CornerRadius(0.10f))
    for (i in 0 until 4) {
        drawRoundRect(Color.White, Offset(0.16f + i * 0.18f, 0.38f), Size(0.12f, 0.24f), CornerRadius(0.03f))
    }
}

private fun DrawScope.roblox() {
    drawRoundRect(Color(0xFFE2231A), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.16f))
    // Tilted square
    withTransform({ rotate(20f, Offset(0.5f, 0.5f)) }) {
        drawRoundRect(Color.White, Offset(0.28f, 0.28f), Size(0.44f, 0.44f), CornerRadius(0.06f))
    }
}

private fun DrawScope.pinterest() {
    drawCircle(Color(0xFFE60023), 0.48f, Offset(0.5f, 0.5f))
    val p = Path().apply {
        moveTo(0.42f, 0.78f)
        lineTo(0.42f, 0.28f)
        lineTo(0.58f, 0.28f)
        cubicTo(0.74f, 0.28f, 0.74f, 0.52f, 0.58f, 0.52f)
        lineTo(0.50f, 0.52f)
        lineTo(0.58f, 0.78f)
        lineTo(0.48f, 0.78f)
        lineTo(0.42f, 0.58f)
        close()
        moveTo(0.50f, 0.40f)
        lineTo(0.54f, 0.40f)
        cubicTo(0.60f, 0.40f, 0.60f, 0.48f, 0.54f, 0.48f)
        lineTo(0.50f, 0.48f)
        close()
    }
    drawPath(p, Color.White)
}

private fun DrawScope.shopify() {
    drawRoundRect(Color(0xFF96BF48), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.16f))
    // Shopping bag
    val bag = Path().apply {
        moveTo(0.30f, 0.38f)
        lineTo(0.34f, 0.78f)
        lineTo(0.66f, 0.78f)
        lineTo(0.70f, 0.38f)
        close()
    }
    drawPath(bag, Color.White)
    drawArc(Color.White, 180f, 180f, false, Offset(0.36f, 0.22f), Size(0.28f, 0.28f), style = Stroke(0.07f))
}

private fun DrawScope.duolingo() {
    drawCircle(Color(0xFF58CC02), 0.48f, Offset(0.5f, 0.5f))
    // Owl face
    drawCircle(Color.White, 0.10f, Offset(0.38f, 0.46f))
    drawCircle(Color.White, 0.10f, Offset(0.62f, 0.46f))
    drawCircle(Color(0xFF212121), 0.05f, Offset(0.38f, 0.46f))
    drawCircle(Color(0xFF212121), 0.05f, Offset(0.62f, 0.46f))
    val beak = Path().apply {
        moveTo(0.50f, 0.52f)
        lineTo(0.58f, 0.62f)
        lineTo(0.42f, 0.62f)
        close()
    }
    drawPath(beak, Color(0xFFFFC800))
}

private fun DrawScope.wordpress() {
    drawCircle(Color(0xFF21759B), 0.48f, Offset(0.5f, 0.5f))
    drawCircle(Color.White, 0.28f, Offset(0.5f, 0.5f), style = Stroke(0.08f))
    drawArc(Color.White, 200f, 100f, false, Offset(0.30f, 0.30f), Size(0.40f, 0.40f), style = Stroke(0.07f))
}

private fun DrawScope.squarespace() {
    drawRoundRect(Color.Black, Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Nested squares
    drawRoundRect(Color.White, Offset(0.22f, 0.22f), Size(0.56f, 0.56f), CornerRadius(0.06f), style = Stroke(0.07f))
    drawRoundRect(Color.White, Offset(0.34f, 0.34f), Size(0.32f, 0.32f), CornerRadius(0.04f), style = Stroke(0.06f))
}

private fun DrawScope.wix() {
    drawRoundRect(Color(0xFFFAAD4D), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    val w = Path().apply {
        moveTo(0.18f, 0.30f)
        lineTo(0.30f, 0.30f)
        lineTo(0.38f, 0.58f)
        lineTo(0.46f, 0.30f)
        lineTo(0.54f, 0.30f)
        lineTo(0.62f, 0.58f)
        lineTo(0.70f, 0.30f)
        lineTo(0.82f, 0.30f)
        lineTo(0.68f, 0.72f)
        lineTo(0.56f, 0.72f)
        lineTo(0.50f, 0.48f)
        lineTo(0.44f, 0.72f)
        lineTo(0.32f, 0.72f)
        close()
    }
    drawPath(w, Color(0xFF212121))
}

private fun DrawScope.tencent() {
    drawRoundRect(Color(0xFF1565C0), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    // Penguin hint
    drawOval(Color.White, Offset(0.32f, 0.28f), Size(0.36f, 0.48f))
    drawCircle(Color(0xFF212121), 0.04f, Offset(0.42f, 0.42f))
    drawCircle(Color(0xFF212121), 0.04f, Offset(0.58f, 0.42f))
    drawOval(Color(0xFFFF9800), Offset(0.44f, 0.50f), Size(0.12f, 0.10f))
}

private fun DrawScope.anthropic() {
    drawRoundRect(Color(0xFFD4A27F), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.16f))
    // Asterisk / starburst
    for (i in 0 until 6) {
        val a = Math.toRadians(30.0 + 60.0 * i)
        drawLine(
            Color(0xFF212121),
            Offset(0.5f - 0.28f * cos(a).toFloat(), 0.5f - 0.28f * sin(a).toFloat()),
            Offset(0.5f + 0.28f * cos(a).toFloat(), 0.5f + 0.28f * sin(a).toFloat()),
            strokeWidth = 0.07f, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.xai() {
    drawRoundRect(Color(0xFF212121), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawLine(Color.White, Offset(0.28f, 0.28f), Offset(0.72f, 0.72f), strokeWidth = 0.12f, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(0.72f, 0.28f), Offset(0.28f, 0.72f), strokeWidth = 0.12f, cap = StrokeCap.Round)
}

private fun DrawScope.myspace() {
    drawRoundRect(Color(0xFF003399), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color.White, 0.10f, Offset(0.35f, 0.40f))
    drawCircle(Color.White, 0.10f, Offset(0.65f, 0.40f))
    drawCircle(Color.White, 0.12f, Offset(0.50f, 0.62f))
}

private fun DrawScope.aol() {
    drawRoundRect(Color(0xFF00A4E4), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color.White, 0.22f, Offset(0.5f, 0.5f), style = Stroke(0.10f))
    drawCircle(Color.White, 0.08f, Offset(0.5f, 0.5f))
}

private fun DrawScope.yahoo() {
    drawRoundRect(Color(0xFF6001D2), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    val y = Path().apply {
        moveTo(0.22f, 0.24f)
        lineTo(0.38f, 0.24f)
        lineTo(0.50f, 0.48f)
        lineTo(0.62f, 0.24f)
        lineTo(0.78f, 0.24f)
        lineTo(0.56f, 0.58f)
        lineTo(0.56f, 0.78f)
        lineTo(0.44f, 0.78f)
        lineTo(0.44f, 0.58f)
        close()
    }
    drawPath(y, Color.White)
}

private fun DrawScope.gamespot() {
    drawRoundRect(Color(0xFFF9A825), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color(0xFF212121), 0.28f, Offset(0.5f, 0.5f), style = Stroke(0.10f))
    drawCircle(Color(0xFF212121), 0.08f, Offset(0.5f, 0.5f))
}

private fun DrawScope.eurogamer() {
    drawRoundRect(Color(0xFF2E7D32), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawArc(Color.White, 40f, 280f, false, Offset(0.22f, 0.22f), Size(0.56f, 0.56f), style = Stroke(0.12f))
}

private fun DrawScope.pcGamer() {
    drawRoundRect(Color(0xFF00695C), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.10f))
    drawRoundRect(Color.White, Offset(0.16f, 0.36f), Size(0.20f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.42f, 0.36f), Size(0.18f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.66f, 0.36f), Size(0.18f, 0.28f), CornerRadius(0.03f))
}

private fun DrawScope.edgeMag() {
    drawRoundRect(Color(0xFF212121), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.10f))
    drawRect(Color.White, Offset(0.28f, 0.28f), Size(0.44f, 0.10f))
    drawRect(Color.White, Offset(0.28f, 0.45f), Size(0.36f, 0.10f))
    drawRect(Color.White, Offset(0.28f, 0.62f), Size(0.44f, 0.10f))
}

private fun DrawScope.kotaku() {
    drawRoundRect(Color(0xFFC2185B), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color.White, 0.18f, Offset(0.5f, 0.42f))
    drawRect(Color.White, Offset(0.42f, 0.55f), Size(0.16f, 0.28f))
}

private fun DrawScope.polygon() {
    drawRoundRect(Color(0xFFE040FB), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    val tri = Path().apply {
        moveTo(0.50f, 0.20f)
        lineTo(0.80f, 0.75f)
        lineTo(0.20f, 0.75f)
        close()
    }
    drawPath(tri, Color(0xFF212121))
}

private fun DrawScope.ars() {
    drawRoundRect(Color(0xFFFF4E00), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawArc(Color(0xFF212121), 40f, 280f, false, Offset(0.22f, 0.22f), Size(0.40f, 0.56f), style = Stroke(0.10f))
    drawRoundRect(Color(0xFF212121), Offset(0.58f, 0.34f), Size(0.20f, 0.32f), CornerRadius(0.04f))
}

private fun DrawScope.engadget() {
    drawRoundRect(Color(0xFF6B2D5C), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.14f))
    drawCircle(Color.White, 0.22f, Offset(0.5f, 0.5f), style = Stroke(0.10f))
    drawLine(Color.White, Offset(0.50f, 0.28f), Offset(0.50f, 0.72f), strokeWidth = 0.08f)
}

private fun DrawScope.nyt() {
    drawRoundRect(Color(0xFF212121), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.06f))
    // Gothic T
    drawRect(Color.White, Offset(0.22f, 0.34f), Size(0.56f, 0.10f))
    drawRect(Color.White, Offset(0.45f, 0.34f), Size(0.10f, 0.36f))
}

private fun DrawScope.wsj() {
    drawRoundRect(Color(0xFF0274B6), Offset(0.04f, 0.22f), Size(0.92f, 0.56f), CornerRadius(0.08f))
    drawRoundRect(Color.White, Offset(0.14f, 0.36f), Size(0.20f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.40f, 0.36f), Size(0.20f, 0.28f), CornerRadius(0.03f))
    drawRoundRect(Color.White, Offset(0.66f, 0.36f), Size(0.20f, 0.28f), CornerRadius(0.03f))
}

private fun DrawScope.ft() {
    drawRoundRect(Color(0xFFFFF1E5), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.10f))
    drawRect(Color(0xFF212121), Offset(0.28f, 0.28f), Size(0.44f, 0.10f))
    drawRect(Color(0xFF212121), Offset(0.28f, 0.28f), Size(0.10f, 0.44f))
    drawRect(Color(0xFF212121), Offset(0.28f, 0.50f), Size(0.32f, 0.08f))
}

private fun DrawScope.byteMag() {
    drawRoundRect(Color(0xFF1565C0), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.12f))
    drawRoundRect(Color.White, Offset(0.28f, 0.28f), Size(0.44f, 0.44f), CornerRadius(0.06f), style = Stroke(0.10f))
}

private fun DrawScope.cgw() {
    drawRoundRect(Color(0xFF2E7D32), Offset(0.06f, 0.06f), Size(0.88f, 0.88f), CornerRadius(0.12f))
    drawRoundRect(Color.White, Offset(0.22f, 0.34f), Size(0.56f, 0.32f), CornerRadius(0.06f), style = Stroke(0.08f))
}
