package com.boot.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Office
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The office, drawn like a Game Dev Tycoon room: the founder and the team at their desks, the
 * room changing as the company moves from the apartment to the campus, and points floating up
 * off people's desks every week while a game is in development. Dark mode is the night shift.
 */

private data class Seat(val x: Float, val floor: Float, val scale: Float)

private data class Worker(val key: Int, val lane: Lane?, val founder: Boolean)

private data class Bubble(val id: Long, val x: Dp, val y: Dp, val text: String, val color: Color, val delayMs: Int)

/** How many desks the drawing shows, the founder's included; bigger offices say how many more there are. */
private val Office.shownDesks: Int
    get() = min(
        desks + 1,
        when (this) {
            Office.APARTMENT -> 3
            Office.COWORKING -> 7
            Office.STUDIO -> 8
            Office.DOWNTOWN -> 10
            Office.CAMPUS -> 12
        },
    )

private fun seats(w: Float, h: Float, n: Int): List<Seat> {
    val rows = if (n <= 3) 1 else 2
    val perRow = ceil(n / rows.toFloat()).toInt()
    val out = mutableListOf<Seat>()
    for (r in 0 until rows) {
        val count = if (r == rows - 1) n - perRow * (rows - 1) else perRow
        val floor = when {
            rows == 1 -> h * 0.94f
            r == 0 -> h * 0.71f
            else -> h * 0.96f
        }
        val depth = if (rows == 2 && r == 0) 0.8f else 1f
        val s = min(w * 0.94f / perRow / 100f, (if (rows == 1) h * 0.66f else h * 0.42f) / 86f) * depth
        val stationW = 100f * s
        val gap = (w - stationW * count) / (count + 1)
        for (i in 0 until count) out += Seat(gap + i * (stationW + gap), floor, s)
    }
    return out
}

private fun workers(state: GameState): List<Worker> {
    val founderLane = state.lanes.maxByOrNull { state.founder.attentionShare(it, state.lanes) }
    return listOf(Worker(-1, founderLane, founder = true)) +
        state.staff.sortedWith(compareBy({ it.lane?.ordinal ?: 99 }, { it.id })).map { Worker(it.id, it.lane, founder = false) }
}

@Composable
fun OfficeScene(state: GameState, playing: Boolean, modifier: Modifier = Modifier) {
    val night = isSystemInDarkTheme()
    val colors = gameColors
    val primary = MaterialTheme.colorScheme.primary
    val measurer = rememberTextMeasurer()
    val all = workers(state)
    val shown = state.office.shownDesks
    val people = all.take(shown)
    val anim = if (playing) {
        val loop = rememberInfiniteTransition(label = "office")
        loop.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "t").value
    } else {
        0f
    }
    val bubbles = remember { mutableStateListOf<Bubble>() }
    var seenWeek by remember { mutableIntStateOf(state.week) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val layout = remember(w, h, shown) { seats(w, h, shown) }
        val palette = remember(state.office, night) { RoomPalette.of(state.office, night) }

        LaunchedEffect(state.week) {
            if (state.week == seenWeek + 1) {
                with(density) { spawnBubbles(state, people, layout, colors, bubbles) }
            }
            seenWeek = state.week
        }

        Canvas(Modifier.fillMaxSize()) {
            drawRoom(state, palette, night, measurer)
            for (i in layout.indices) {
                val worker = people.getOrNull(i)
                val shirt = when {
                    worker == null -> Color.Transparent
                    worker.founder -> primary
                    worker.lane == null -> Color(0xFF9AA0A6)
                    else -> colors.lane(worker.lane)
                }
                drawStation(layout[i], worker, shirt, palette, if (playing) anim + i * 0.37f else null)
            }
        }

        for (b in bubbles.toList()) {
            key(b.id) { FloatingBubble(b) { bubbles.remove(b) } }
        }
    }
}

/** The points this week's work produced, floating up from the desks that made them. */
private fun androidx.compose.ui.unit.Density.spawnBubbles(
    state: GameState,
    people: List<Worker>,
    layout: List<Seat>,
    colors: GameColors,
    bubbles: MutableList<Bubble>,
) {
    fun head(i: Int): Pair<Dp, Dp> {
        val seat = layout[i]
        return (seat.x + 27f * seat.scale).toDp() to (seat.floor - 92f * seat.scale).toDp()
    }
    val seatsOn = { lane: Lane -> people.indices.filter { i -> i < layout.size && people[i].lane == lane } }
    val stamp = state.week.toLong() * 1_000
    var n = 0
    fun add(i: Int, text: String, color: Color) {
        val (x, y) = head(i)
        bubbles += Bubble(stamp + n, x, y, text, color, delayMs = (n % 6) * 140)
        n++
    }

    state.games?.project?.let { p ->
        val desks = seatsOn(Lane.GAMES).ifEmpty { listOf(0) }
        val k = min(desks.size, 3)
        val start = state.week % desks.size
        fun pick(j: Int) = desks[(start + j) % desks.size]
        if (p.lastDesign >= 1) repeat(k) { j -> add(pick(j), "+${(p.lastDesign / k).roundToInt().coerceAtLeast(1)}", colors.design) }
        if (p.lastTech >= 1) repeat(k) { j -> add(pick(j + 1), "+${(p.lastTech / k).roundToInt().coerceAtLeast(1)}", colors.tech) }
        if (p.lastBugs >= 1) add(pick(2), "+${p.lastBugs.roundToInt()}", colors.bad)
        if (p.lastBugs <= -1) add(pick(2), "${p.lastBugs.roundToInt()}", colors.good)
    }
    for (id in state.levelUps) {
        val i = people.indexOfFirst { it.key == id }
        if (i in layout.indices) add(i, "LV UP", Color(0xFFE0A800))
    }
    val week = state.ledger.lastOrNull()
    for (lane in state.lanes) {
        if (lane == Lane.GAMES && state.games?.project != null) continue
        if ((week?.revenue?.get(lane) ?: 0.0) <= 0) continue
        val desks = seatsOn(lane).ifEmpty { listOf(0) }
        add(desks[state.week % desks.size], "$", colors.good)
    }
    while (bubbles.size > 24) bubbles.removeAt(0)
}

@Composable
private fun FloatingBubble(b: Bubble, onDone: () -> Unit) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        rise.animateTo(1f, tween(1_200, delayMillis = b.delayMs))
        onDone()
    }
    Box(
        Modifier
            .offset { IntOffset((b.x - 14.dp).roundToPx(), (b.y - 40.dp * rise.value).roundToPx()) }
            .graphicsLayer {
                alpha = if (rise.value == 0f) 0f else (1.4f - rise.value).coerceIn(0f, 1f)
                val pop = (rise.value * 5f).coerceAtMost(1f)
                scaleX = 0.6f + 0.4f * pop
                scaleY = 0.6f + 0.4f * pop
            }
            .background(b.color, CircleShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(b.text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Each office tier has its own room; night mode dims it and turns the lights on. */
private data class RoomPalette(
    val wall: Color,
    val floor: Color,
    val floorLine: Color,
    val skyTop: Color,
    val skyBottom: Color,
    val frame: Color,
    val desk: Color,
    val deskDark: Color,
    val chair: Color,
    val accent: Color,
) {
    companion object {
        fun of(office: Office, night: Boolean): RoomPalette {
            val (wall, floor) = when (office) {
                Office.APARTMENT -> if (night) Color(0xFF3A3242) to Color(0xFF4A3A30) else Color(0xFFF4E4CF) to Color(0xFFC99B6C)
                Office.COWORKING -> if (night) Color(0xFF2A3440) to Color(0xFF39424C) else Color(0xFFE3EEF2) to Color(0xFFB8C2C8)
                Office.STUDIO -> if (night) Color(0xFF2D2940) to Color(0xFF3B3754) else Color(0xFFE9E3F7) to Color(0xFF9A94B3)
                Office.DOWNTOWN -> if (night) Color(0xFF262C36) to Color(0xFF333B47) else Color(0xFFEEF1F4) to Color(0xFFA7B0BA)
                Office.CAMPUS -> if (night) Color(0xFF23302A) to Color(0xFF34423A) else Color(0xFFE5F2E8) to Color(0xFFB4C6AD)
            }
            return RoomPalette(
                wall = wall,
                floor = floor,
                floorLine = if (night) Color(0x22FFFFFF) else Color(0x22000000),
                skyTop = if (night) Color(0xFF0B1530) else Color(0xFF7FC0FF),
                skyBottom = if (night) Color(0xFF2E2756) else Color(0xFFDDF1FF),
                frame = if (night) Color(0xFF55506A) else Color.White,
                desk = if (night) Color(0xFF8C7A66) else Color(0xFFD9B98F),
                deskDark = if (night) Color(0xFF5E5245) else Color(0xFFA9875F),
                chair = if (night) Color(0xFF4A4F5C) else Color(0xFF3E4452),
                accent = if (night) Color(0xFFFFD27A) else Color(0xFFFF9F0A),
            )
        }
    }
}

private val Skin = listOf(Color(0xFFF2D2BD), Color(0xFFE0AC8A), Color(0xFFC68863), Color(0xFF8D5A3B), Color(0xFF5C3A24))
private val Hair = listOf(Color(0xFF2B2118), Color(0xFF5A3A22), Color(0xFFA0522D), Color(0xFFD9B26F), Color(0xFF1F1F2E), Color(0xFF7A2E2E))

private fun DrawScope.drawRoom(state: GameState, p: RoomPalette, night: Boolean, measurer: androidx.compose.ui.text.TextMeasurer) {
    val w = size.width
    val h = size.height
    val twoRows = state.office.shownDesks > 3
    val floorTop = h * (if (twoRows) 0.53f else 0.6f)
    drawRect(p.wall, size = Size(w, floorTop))
    drawRect(p.floor, topLeft = Offset(0f, floorTop), size = Size(w, h - floorTop))
    // Planks or carpet lines.
    val lines = if (state.office == Office.APARTMENT) 6 else 3
    for (i in 1..lines) {
        val y = floorTop + (h - floorTop) * i / (lines + 1)
        drawLine(p.floorLine, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
    }
    drawRect(p.floorLine.copy(alpha = 0.3f), topLeft = Offset(0f, floorTop - 6f), size = Size(w, 6f))

    fun window(x0: Float, x1: Float, y0: Float, y1: Float, skyline: Boolean = false, trees: Boolean = false) {
        val tl = Offset(w * x0, h * y0)
        val sz = Size(w * (x1 - x0), h * (y1 - y0))
        drawRoundRect(p.frame, tl - Offset(6f, 6f), Size(sz.width + 12f, sz.height + 12f), CornerRadius(14f))
        drawRoundRect(Brush.verticalGradient(listOf(p.skyTop, p.skyBottom), tl.y, tl.y + sz.height), tl, sz, CornerRadius(10f))
        if (night) {
            for (k in 0 until 14) {
                val sx = tl.x + sz.width * ((k * 0.618f) % 1f)
                val sy = tl.y + sz.height * 0.6f * ((k * 0.377f + 0.1f) % 1f)
                drawCircle(Color.White.copy(alpha = 0.7f), radius = 2.2f, center = Offset(sx, sy))
            }
        } else {
            drawCircle(Color.White.copy(alpha = 0.85f), sz.height * 0.12f, Offset(tl.x + sz.width * 0.3f, tl.y + sz.height * 0.32f))
            drawCircle(Color.White.copy(alpha = 0.85f), sz.height * 0.16f, Offset(tl.x + sz.width * 0.42f, tl.y + sz.height * 0.3f))
        }
        if (skyline) {
            val bw = sz.width / 11
            for (k in 0 until 11) {
                val bh = sz.height * (0.3f + 0.45f * (((k * 7 + 3) % 10) / 10f))
                val bx = tl.x + k * bw
                drawRect(if (night) Color(0xFF1C2340) else Color(0xFF9BB3CC), Offset(bx, tl.y + sz.height - bh), Size(bw * 0.9f, bh))
                if (night) {
                    for (r in 0 until (bh / 22).toInt()) {
                        if ((k + r) % 3 != 0) drawRect(p.accent.copy(alpha = 0.7f), Offset(bx + bw * 0.3f, tl.y + sz.height - bh + 10f + r * 22f), Size(bw * 0.25f, 8f))
                    }
                }
            }
        }
        if (trees) {
            for (k in 0 until 6) {
                val cx = tl.x + sz.width * (0.08f + k * 0.17f)
                val r = sz.height * (0.22f + 0.06f * (k % 2))
                drawRect(Color(0xFF6B4F3A), Offset(cx - 4f, tl.y + sz.height - r * 0.9f), Size(8f, r * 0.9f))
                drawCircle(if (night) Color(0xFF1E4A33) else Color(0xFF5DBB63), r, Offset(cx, tl.y + sz.height - r * 1.2f))
            }
        }
        // Mullions.
        drawLine(p.frame, Offset(tl.x + sz.width / 2, tl.y), Offset(tl.x + sz.width / 2, tl.y + sz.height), strokeWidth = 5f)
        if (!skyline && !trees) drawLine(p.frame, Offset(tl.x, tl.y + sz.height / 2), Offset(tl.x + sz.width, tl.y + sz.height / 2), strokeWidth = 5f)
    }

    fun plant(cx: Float, base: Float, scale: Float) {
        val pot = Path().apply {
            moveTo(cx - 18 * scale, base - 34 * scale)
            lineTo(cx + 18 * scale, base - 34 * scale)
            lineTo(cx + 13 * scale, base)
            lineTo(cx - 13 * scale, base)
            close()
        }
        val leaf = if (night) Color(0xFF2F6B45) else Color(0xFF3FA35B)
        drawCircle(leaf, 20 * scale, Offset(cx - 12 * scale, base - 52 * scale))
        drawCircle(leaf, 22 * scale, Offset(cx + 10 * scale, base - 60 * scale))
        drawCircle(leaf.copy(red = leaf.red * 0.8f), 18 * scale, Offset(cx, base - 78 * scale))
        drawPath(pot, Color(0xFFCB6E46))
    }

    fun sign(text: String, cx: Float, cy: Float) {
        val style = TextStyle(fontSize = (h * 0.055f).toSp(), fontWeight = FontWeight.Black, color = Color.White)
        val layout = measurer.measure(text, style)
        val pad = h * 0.025f
        val tl = Offset(cx - layout.size.width / 2f - pad * 1.6f, cy - layout.size.height / 2f - pad)
        drawRoundRect(p.accent, tl, Size(layout.size.width + pad * 3.2f, layout.size.height + pad * 2), CornerRadius(h))
        drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f))
    }

    fun lamp(cx: Float) {
        drawLine(p.chair, Offset(cx, 0f), Offset(cx, h * 0.12f), strokeWidth = 3f)
        drawArc(p.chair, 180f, 180f, true, Offset(cx - h * 0.06f, h * 0.09f), Size(h * 0.12f, h * 0.08f))
        if (night) drawCircle(p.accent.copy(alpha = 0.25f), h * 0.09f, Offset(cx, h * 0.15f))
    }

    val name = state.companyName
    when (state.office) {
        Office.APARTMENT -> {
            window(0.62f, 0.9f, 0.1f, 0.42f)
            // A poster of whatever you're making.
            val lane = state.lanes.first()
            drawRoundRect(Color.White, Offset(w * 0.36f, h * 0.17f), Size(w * 0.13f, h * 0.24f), CornerRadius(6f))
            drawRoundRect(gameLaneTint(lane), Offset(w * 0.37f, h * 0.185f), Size(w * 0.11f, h * 0.21f), CornerRadius(4f))
            drawCircle(Color.White.copy(alpha = 0.8f), h * 0.04f, Offset(w * 0.425f, h * 0.27f))
            plant(w * 0.95f, floorTop + 4f, h / 260f)
        }
        Office.COWORKING -> {
            window(0.05f, 0.27f, 0.12f, 0.42f)
            window(0.73f, 0.95f, 0.12f, 0.42f)
            lamp(w * 0.38f)
            lamp(w * 0.62f)
            sign(name, w * 0.5f, h * 0.3f)
        }
        Office.STUDIO -> {
            window(0.04f, 0.3f, 0.1f, 0.42f)
            // Whiteboard with the plan.
            drawRoundRect(p.frame, Offset(w * 0.68f, h * 0.1f), Size(w * 0.27f, h * 0.3f), CornerRadius(8f))
            drawRoundRect(Color(0xFFFDFDFD), Offset(w * 0.69f, h * 0.115f), Size(w * 0.25f, h * 0.27f), CornerRadius(6f))
            listOf(Color(0xFF2F7BF6), Color(0xFFFF6B3D), Color(0xFF34C759)).forEachIndexed { i, c ->
                drawLine(c, Offset(w * 0.71f, h * (0.17f + i * 0.07f)), Offset(w * (0.8f + i * 0.04f), h * (0.17f + i * 0.07f)), strokeWidth = 5f, cap = StrokeCap.Round)
            }
            sign(name, w * 0.49f, h * 0.2f)
        }
        Office.DOWNTOWN -> {
            window(0.04f, 0.96f, 0.16f, 0.46f, skyline = true)
            sign(name, w * 0.5f, h * 0.075f)
        }
        Office.CAMPUS -> {
            window(0.04f, 0.96f, 0.16f, 0.46f, trees = true)
            sign(name, w * 0.5f, h * 0.075f)
        }
    }
}

private fun gameLaneTint(lane: Lane): Color = when (lane) {
    Lane.GAMES -> Color(0xFFFF2D55)
    Lane.WEBSITES -> Color(0xFF007AFF)
    Lane.APPS -> Color(0xFF34C759)
    Lane.SOCIAL -> Color(0xFFFF9500)
    Lane.HARDWARE -> Color(0xFFAF52DE)
    Lane.AI -> Color(0xFF32ADE6)
}

/** One workstation in side view, 100 units wide, standing on the seat's floor line. */
private fun DrawScope.drawStation(seat: Seat, worker: Worker?, shirt: Color, p: RoomPalette, phase: Float?) {
    val s = seat.scale
    fun x(u: Float) = seat.x + u * s
    fun y(u: Float) = seat.floor + u * s
    fun box(x0: Float, y0: Float, x1: Float, y1: Float, c: Color, r: Float = 0f) =
        drawRoundRect(c, Offset(x(x0), y(y0)), Size((x1 - x0) * s, (y1 - y0) * s), CornerRadius(r * s))
    fun line(x0: Float, y0: Float, x1: Float, y1: Float, c: Color, width: Float) =
        drawLine(c, Offset(x(x0), y(y0)), Offset(x(x1), y(y1)), strokeWidth = width * s, cap = StrokeCap.Round)

    val wave = phase?.let { sin(2 * PI.toFloat() * it) } ?: 0f
    // Shadow.
    drawOval(Color.Black.copy(alpha = 0.08f), Offset(x(4f), y(-3f)), Size(96f * s, 6f * s))

    // Chair.
    box(8f, -54f, 13.5f, -24f, p.chair, 2.5f)
    box(10f, -28f, 38f, -22f, p.chair, 2.5f)
    box(22f, -22f, 26f, -6f, p.chair)
    box(12f, -6f, 36f, -3f, p.chair, 1.5f)

    if (worker != null) {
        val skin = Skin[Math.floorMod(worker.key * 7 + 3, Skin.size)]
        val hair = if (worker.founder) Color(0xFF3B2A20) else Hair[Math.floorMod(worker.key * 5 + 1, Hair.size)]
        val pants = Color(0xFF2F3A4F)
        val bob = if (phase != null) wave * 0.6f else 0f
        // Legs under the desk.
        box(20f, -31f, 51f, -23f, pants, 4f)
        box(44f, -27f, 51f, -4f, pants, 3f)
        box(44f, -5f, 57f, 0f, Color(0xFF1E1E24), 2f)
        // Body and head.
        box(15f, -61f + bob, 36f, -25f, shirt, 9f)
        box(24f, -66f + bob, 29.5f, -59f + bob, skin)
        drawCircle(hair, 10.5f * s, Offset(x(25f), y(-75f + bob)))
        drawCircle(skin, 9.5f * s, Offset(x(28.5f), y(-71f + bob)))
        drawCircle(Color(0xFF1E1E24), 1.3f * s, Offset(x(33.5f), y(-72f + bob)))
        if (worker.founder) {
            val crown = Path().apply {
                moveTo(x(20f), y(-84f + bob))
                lineTo(x(20f), y(-91f + bob))
                lineTo(x(24f), y(-87f + bob))
                lineTo(x(27.5f), y(-94f + bob))
                lineTo(x(31f), y(-87f + bob))
                lineTo(x(35f), y(-91f + bob))
                lineTo(x(35f), y(-84f + bob))
                close()
            }
            drawPath(crown, Color(0xFFFFC53D))
        }
        // Arm reaching to the keyboard, typing while the clock runs.
        val tap = if (phase != null) wave * 1.6f else 0f
        line(27f, -54f + bob, 37f, -38f, shirt, 6f)
        line(37f, -38f, 55f, -42f + tap, shirt, 5.5f)
        drawCircle(skin, 3.2f * s, Offset(x(57f), y(-42f + tap)))
    }

    // Desk, keyboard, monitor.
    box(42f, -41f, 99f, -36f, p.desk, 1.5f)
    box(46f, -36f, 49.5f, 0f, p.deskDark)
    box(80f, -36f, 96f, -18f, p.deskDark, 1.5f)
    box(50f, -43f, 66f, -41f, Color(0xFF6E7380), 1f)
    box(74f, -48f, 77f, -41f, Color(0xFF2B2B30))
    box(68f, -42f, 83f, -41f, Color(0xFF2B2B30))
    box(61f, -74f, 91f, -47f, Color(0xFF2B2B30), 3f)
    val screen = when {
        worker == null -> Color(0xFF3A3F4B)
        phase != null -> lerpColor(shirt, Color.White, 0.25f + 0.15f * ((wave + 1) / 2))
        else -> lerpColor(shirt, Color.White, 0.3f)
    }
    box(63f, -72f, 89f, -49f, screen, 2f)
    if (worker != null) {
        box(66f, -68f, 80f, -66f, Color.White.copy(alpha = 0.7f), 1f)
        box(66f, -63f, 85f, -61f, Color.White.copy(alpha = 0.5f), 1f)
        box(66f, -58f, 76f, -56f, Color.White.copy(alpha = 0.5f), 1f)
        box(92f, -47f, 97f, -41f, p.accent, 1f)
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
