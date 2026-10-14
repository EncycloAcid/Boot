package com.boot.desktop

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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
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
import com.boot.core.Perk
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The office as an isometric diorama, Game Dev Tycoon style: the founder and the team at
 * back-to-back desks, everyone's screen showing what their lane works on, the room changing as the
 * company moves from the apartment to the campus, the perks you've bought along the walls, and
 * points floating up off people's heads every week while a game is in development. Dark mode is
 * the night shift, lit by monitors.
 */

/** Isometric projection: x runs down to the right, y down to the left, z straight up. [t] is half a tile's width. */
internal class Iso(val ox: Float, val oy: Float, val t: Float) {
    val zpx = t * 1.15f
    fun p(x: Float, y: Float, z: Float = 0f) = Offset(ox + (x - y) * t, oy + (x + y) * t * 0.5f - z * zpx)
}

private data class Worker(val key: Int, val lane: Lane?, val founder: Boolean, val look: Look)

private data class Bubble(val id: Long, val x: Dp, val y: Dp, val text: String, val color: Color, val delayMs: Int)

/** Where someone sits: the chair's centre on the floor and which way they face. */
private data class Seat(val cx: Float, val cy: Float, val facing: Facing)

/** What the drawing needs to know: the room, the people in it, and what's on the walls. */
private class SceneModel(
    val office: Office,
    val name: String,
    val firstLane: Lane,
    val perks: Set<Perk>,
    val people: List<Worker>,
    val wins: Int,
    /** Live games need servers. */
    val servers: Boolean = false,
)

private fun GameState.sceneModel(office: Office = this.office) = SceneModel(
    office, companyName, lanes.first(), perks, workers(this).take(office.shownDesks), games?.market?.wins ?: 0,
    servers = games?.releases?.any { it.live?.closed == false } == true,
)

private class Room(val w: Float, val d: Float, val h: Float, val cols: Int, val rows: Int, val seats: List<Seat>)

private const val X0 = 1.45f
private const val Y0 = 1.2f
private const val DESK_W = 0.98f
private const val ROW_PITCH = 2.55f
private const val DESK_Z = 0.62f
private const val SEAT_Z = 0.4f

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

/** Desks come in back-to-back islands: one person faces us, the one opposite has their back to us. */
private fun roomFor(n: Int): Room {
    val (cols, rows) = when {
        n <= 4 -> 2 to 1
        n <= 8 -> 4 to 1
        else -> 3 to 2
    }
    val seats = buildList {
        for (r in 0 until rows) for (c in 0 until cols) for (side in 0..1) {
            if (size >= n) break
            val cx = X0 + c * DESK_W + DESK_W / 2
            val y0 = Y0 + r * ROW_PITCH
            add(if (side == 0) Seat(cx, y0 + 0.3f, Facing.FRONT) else Seat(cx, y0 + 1.86f, Facing.BACK))
        }
    }
    return Room(X0 + cols * DESK_W + 0.8f, Y0 + rows * ROW_PITCH + 0.2f, 2.0f, cols, rows, seats)
}

private fun workers(state: GameState): List<Worker> {
    val founderLane = state.lanes.maxByOrNull { state.founder.attentionShare(it, state.lanes) }
    val founder = Worker(-1, founderLane, founder = true, look = lookFor(state.founder.name.hashCode() xor state.companyName.hashCode()))
    return listOf(founder) +
        state.staff.sortedWith(compareBy({ it.lane?.ordinal ?: 99 }, { it.id })).map { Worker(it.id, it.lane, founder = false, look = lookFor(it.id)) }
}

/** The founder's look, for portraits. */
internal fun GameState.founderLook(): Look = lookFor(founder.name.hashCode() xor companyName.hashCode())

private fun fit(room: Room, w: Float, fullHeight: Float, top: Float = 0f): Iso {
    val h = fullHeight - top
    // Bounds of the whole diorama at t = 1 with the origin at the back corner.
    val minX = -room.d - 0.2f
    val maxX = room.w + 0.2f
    val minY = -0.15f - room.h * 1.15f - 0.1f
    // The empty front corner of the floor can run off the bottom: people matter more than floor.
    val maxY = (room.w + room.d) / 2f - 0.55f
    val t = min(w * 0.98f / (maxX - minX), h * 0.97f / (maxY - minY))
    val ox = w / 2f - t * (minX + maxX) / 2f
    val oy = top + h / 2f - t * (minY + maxY) / 2f
    return Iso(ox, oy, t)
}

private fun Iso.head(seat: Seat): Offset {
    val hip = p(seat.cx, seat.cy + (if (seat.facing == Facing.FRONT) 0.02f else 0f), SEAT_Z)
    return Offset(hip.x, hip.y - 0.77f * zpx)
}

@Composable
fun OfficeScene(state: GameState, playing: Boolean, modifier: Modifier = Modifier, insetTop: Dp = 0.dp) {
    val colors = gameColors
    val model = state.sceneModel()
    val bubbles = remember { mutableStateListOf<Bubble>() }
    var seenWeek by remember { mutableIntStateOf(state.week) }
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val room = remember(model.office) { roomFor(model.office.shownDesks) }
        val top = with(density) { insetTop.toPx() }
        val iso = remember(constraints.maxWidth, constraints.maxHeight, room, top) {
            fit(room, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), top)
        }
        LaunchedEffect(state.week) {
            if (state.week == seenWeek + 1) {
                with(density) { spawnBubbles(state, model.people, room.seats.map { iso.head(it) }, colors, bubbles) }
            }
            seenWeek = state.week
        }
        SceneCanvas(model, room, iso, playing)
        for (b in bubbles.toList()) {
            key(b.id) { FloatingBubble(b) { bubbles.remove(b) } }
        }
    }
}

/** A still of an office you could move to, with today's team in it. */
@Composable
fun OfficePreview(state: GameState, office: Office, modifier: Modifier = Modifier, insetTop: Dp = 0.dp) {
    val model = state.sceneModel(office)
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val room = remember(office) { roomFor(office.shownDesks) }
        val top = with(density) { insetTop.toPx() }
        val iso = remember(constraints.maxWidth, constraints.maxHeight, room, top) {
            fit(room, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), top)
        }
        SceneCanvas(model, room, iso, playing = false)
    }
}

@Composable
private fun SceneCanvas(model: SceneModel, room: Room, iso: Iso, playing: Boolean) {
    val night = isSystemInDarkTheme()
    val colors = gameColors
    val primary = MaterialTheme.colorScheme.primary
    val measurer = rememberTextMeasurer()
    val anim = if (playing) {
        val loop = rememberInfiniteTransition(label = "office")
        loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1_200, easing = LinearEasing), RepeatMode.Restart), label = "t").value
    } else {
        0f
    }
    val palette = remember(model.office, night) { RoomPalette.of(model.office, night) }
    Canvas(Modifier.fillMaxSize()) {
        drawBackdrop(iso, room, palette, night)
        drawShell(iso, room, model, palette, night, measurer)
        val props = mutableListOf<Prop>()
        furnish(props, iso, room, model, palette, night, if (playing) anim else null)
        room.seats.forEachIndexed { i, seat ->
            val worker = model.people.getOrNull(i)
            val shirt = when {
                worker == null -> Color.Transparent
                worker.founder -> primary
                else -> worker.look.top.let { if (night) it.shade(0.8f) else it }
            }
            val badge = worker?.takeIf { !it.founder }?.let { w -> w.lane?.let { colors.lane(it) } ?: Color(0xFF9AA0A6) }
            station(props, iso, i, seat, worker, shirt, badge, palette, night, if (playing) anim + i * 0.37f else null)
        }
        props.sortBy { it.depth }
        for (prop in props) prop.draw(this)
        if (night) drawNightGlow(iso, room, model.people)
    }
}

/** The points this week's work produced, floating up from the heads that made them. */
private fun androidx.compose.ui.unit.Density.spawnBubbles(
    state: GameState,
    people: List<Worker>,
    heads: List<Offset>,
    colors: DesktopSceneColors,
    bubbles: MutableList<Bubble>,
) {
    val seatsOn = { lane: Lane -> people.indices.filter { i -> i < heads.size && people[i].lane == lane } }
    val stamp = state.week.toLong() * 1_000
    var n = 0
    fun add(i: Int, text: String, color: Color) {
        val head = heads[i]
        bubbles += Bubble(stamp + n, head.x.toDp(), head.y.toDp(), text, color, delayMs = (n % 6) * 140)
        n++
    }

    listOfNotNull(state.games?.project, state.games?.project2).forEach { p ->
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
        if (i in heads.indices) add(i, "LV UP", Color(0xFFE0A800))
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
        rise.animateTo(1f, tween(1_300, delayMillis = b.delayMs))
        onDone()
    }
    Box(
        Modifier
            .offset { IntOffset((b.x - 14.dp).roundToPx(), (b.y - 18.dp - 36.dp * rise.value).roundToPx()) }
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

// Palettes

/** Each office tier has its own room; night mode dims it and lets the screens light it. */
private class RoomPalette(
    val backdropTop: Color,
    val backdropBottom: Color,
    val wall: Color,
    val wallCap: Color,
    val trim: Color,
    val floorA: Color,
    val floorB: Color,
    val slab: Color,
    val deskTop: Color,
    val deskSide: Color,
    val chair: Color,
    val seat: Color,
    val accent: Color,
    val frame: Color,
    val skyTop: Color,
    val skyBottom: Color,
    val rug: Color?,
    val pattern: FloorPattern,
) {
    companion object {
        fun of(office: Office, night: Boolean): RoomPalette {
            fun n(c: Long, f: Float = 0.42f): Color = Color(c).let { if (night) it.shade(f).mix(Color(0xFF1A2040), 0.28f) else it }
            val base = when (office) {
                Office.APARTMENT -> listOf(0xFFF4E3CB, 0xFFC98F5C, 0xFFBD8352, 0xFFE2C59D, 0xFF3E4452, 0xFFD9774B)
                Office.COWORKING -> listOf(0xFFDCEAE6, 0xFFA3B4BB, 0xFF9AACB3, 0xFFF4F4F2, 0xFF2D6E72, 0xFF7FB7A9)
                Office.STUDIO -> listOf(0xFFE7E0F6, 0xFFBDB3DA, 0xFFAFA4CF, 0xFFF1ECF9, 0xFF5B4B8A, 0xFFEFA6C6)
                Office.DOWNTOWN -> listOf(0xFFEEF1F5, 0xFFC0C5CD, 0xFFB7BCC4, 0xFFFAFAFA, 0xFF2A2D35, 0xFF6C7A96)
                Office.CAMPUS -> listOf(0xFFF2EEE4, 0xFFDABB8E, 0xFFD0AF80, 0xFFE8CFA6, 0xFF3C6E52, 0xFF6FA57C)
            }
            return RoomPalette(
                backdropTop = if (night) Color(0xFF10131F) else Color(0xFFE6ECF7),
                backdropBottom = if (night) Color(0xFF1B2032) else Color(0xFFF6F2EC),
                wall = n(base[0]),
                wallCap = n(0xFFFFFFFF, 0.5f),
                trim = n(base[0]).shade(0.82f),
                floorA = n(base[1]),
                floorB = n(base[2]),
                slab = n(0xFF8D7E70),
                deskTop = n(base[3]),
                deskSide = n(base[3]).shade(0.8f),
                chair = n(base[4], 0.7f),
                seat = n(base[4], 0.7f).mix(Color.White, 0.12f),
                accent = if (night) Color(0xFFFFC56B) else Color(0xFFFF9F0A),
                frame = n(0xFFFFFFFF, 0.55f),
                skyTop = if (night) Color(0xFF0A1330) else Color(0xFF7DBEFF),
                skyBottom = if (night) Color(0xFF2E2756) else Color(0xFFDDF0FF),
                rug = if (office == Office.COWORKING || office == Office.DOWNTOWN) null else n(base[5]),
                pattern = when (office) {
                    Office.APARTMENT, Office.CAMPUS -> FloorPattern.PLANKS
                    Office.COWORKING -> FloorPattern.CARPET
                    Office.STUDIO -> FloorPattern.CHECKER
                    Office.DOWNTOWN -> FloorPattern.CONCRETE
                },
            )
        }
    }
}

private enum class FloorPattern { PLANKS, CARPET, CHECKER, CONCRETE }

// Primitives

private fun DrawScope.quad(a: Offset, b: Offset, c: Offset, d: Offset, color: Color) {
    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        close()
    }
    drawPath(path, color)
}

private fun quadPath(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    lineTo(d.x, d.y)
    close()
}

/** A box from (x0, y0, z0) to (x1, y1, z1): its top and the two faces that look at us, lit from the left. */
private fun DrawScope.solid(iso: Iso, x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, base: Color, top: Color = base.shade(1.06f)) {
    quad(iso.p(x0, y1, z0), iso.p(x1, y1, z0), iso.p(x1, y1, z1), iso.p(x0, y1, z1), base.shade(0.88f))
    quad(iso.p(x1, y0, z0), iso.p(x1, y1, z0), iso.p(x1, y1, z1), iso.p(x1, y0, z1), base.shade(0.74f))
    quad(iso.p(x0, y0, z1), iso.p(x1, y0, z1), iso.p(x1, y1, z1), iso.p(x0, y1, z1), top)
}

/** A soft shadow on the floor: darkest in the middle, fading out at the edges. */
private fun DrawScope.shadow(iso: Iso, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float = 0.14f) {
    val c = iso.p(cx, cy)
    // The footprint's width on screen; a floor ellipse is half as tall as it is wide.
    val r = (rx + ry) * iso.t * 1.15f
    withTransform({ scale(1f, 0.5f, pivot = c) }) {
        drawCircle(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = alpha * 1.6f), Color.Black.copy(alpha = alpha * 0.8f), Color.Transparent), c, r),
            r, c,
        )
    }
}

/** Sunlight through a window on the back wall, falling across the floor. */
private fun DrawScope.sunOnFloor(iso: Iso, room: Room, x0: Float, x1: Float, z0: Float, z1: Float, night: Boolean) {
    val k = 0.9f
    val slant = 0.45f
    val light = if (night) Color(0xFF9FB8FF).copy(alpha = 0.05f) else Color(0xFFFFF1C9).copy(alpha = 0.22f)
    val floor = quadPath(iso.p(0f, 0f), iso.p(room.w, 0f), iso.p(room.w, room.d), iso.p(0f, room.d))
    clipPath(floor) {
        val a = iso.p(x0 + slant * z0, z0 * k)
        val b = iso.p(x1 + slant * z0, z0 * k)
        val c = iso.p(x1 + slant * z1, z1 * k)
        val d = iso.p(x0 + slant * z1, z1 * k)
        drawPath(quadPath(a, b, c, d), Brush.linearGradient(listOf(light, light.copy(alpha = light.alpha * 0.35f)), a, d))
    }
}

/** Soft shade where the floor meets the walls. */
private fun DrawScope.wallShade(iso: Iso, room: Room, night: Boolean) {
    val dark = Color.Black.copy(alpha = if (night) 0.22f else 0.1f)
    val e = 0.45f
    drawPath(
        quadPath(iso.p(0f, 0f), iso.p(room.w, 0f), iso.p(room.w, e), iso.p(0f, e)),
        Brush.linearGradient(listOf(dark, Color.Transparent), iso.p(room.w / 2, 0f), iso.p(room.w / 2, e)),
    )
    drawPath(
        quadPath(iso.p(0f, 0f), iso.p(e, 0f), iso.p(e, room.d), iso.p(0f, room.d)),
        Brush.linearGradient(listOf(dark, Color.Transparent), iso.p(0f, room.d / 2), iso.p(e, room.d / 2)),
    )
}

private class Prop(val depth: Float, val draw: DrawScope.() -> Unit)

/** Draws in the plane of the back-right wall (y = 0), with u running along it in tile widths and v down in pixels. */
private inline fun DrawScope.onRightWall(iso: Iso, x: Float, z: Float, block: DrawScope.() -> Unit) {
    val o = iso.p(x, 0f, z)
    val m = Matrix().apply { values[Matrix.SkewY] = 0.5f }
    withTransform({
        translate(o.x, o.y)
        transform(m)
    }) { block() }
}

/** Draws in the plane of the back-left wall (x = 0), reading left to right towards the corner from [y]. */
private inline fun DrawScope.onLeftWall(iso: Iso, y: Float, z: Float, block: DrawScope.() -> Unit) {
    val o = iso.p(0f, y, z)
    val m = Matrix().apply { values[Matrix.SkewY] = -0.5f }
    withTransform({
        translate(o.x, o.y)
        transform(m)
    }) { block() }
}

// The room

private fun DrawScope.drawBackdrop(iso: Iso, room: Room, p: RoomPalette, night: Boolean) {
    drawRect(Brush.verticalGradient(listOf(p.backdropTop, p.backdropBottom)))
    // A soft pool of shadow under the diorama.
    val c = iso.p(room.w / 2, room.d / 2, -0.25f)
    val rx = (room.w + room.d) * iso.t * 0.62f
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = if (night) 0.45f else 0.16f), Color.Transparent), center = Offset(c.x, c.y + iso.t * 0.6f), radius = rx),
        topLeft = Offset(c.x - rx, c.y - rx * 0.3f), size = Size(2 * rx, rx * 1.2f),
    )
    if (night) {
        for (k in 0 until 26) {
            val sx = size.width * ((k * 0.618f + 0.11f) % 1f)
            val sy = size.height * 0.5f * ((k * 0.377f + 0.05f) % 1f)
            drawCircle(Color.White.copy(alpha = 0.25f + 0.3f * ((k * 7) % 3) / 2f), 1.4f + (k % 3) * 0.5f, Offset(sx, sy))
        }
    }
}

private fun DrawScope.drawShell(iso: Iso, room: Room, model: SceneModel, p: RoomPalette, night: Boolean, measurer: TextMeasurer) {
    val w = room.w
    val d = room.d
    val h = room.h
    val th = 0.14f

    // The floor slab, then its surface.
    solid(iso, 0f, 0f, -0.25f, w, d, 0f, p.slab, top = p.floorA)
    drawFloor(iso, room, p)
    wallShade(iso, room, night)
    when (model.office) {
        Office.APARTMENT -> sunOnFloor(iso, room, 1.5f, w - 0.6f, 0.75f, 1.65f, night)
        Office.COWORKING -> {
            sunOnFloor(iso, room, 1.2f, 2.9f, 0.7f, 1.7f, night)
            sunOnFloor(iso, room, 3.4f, w - 0.5f, 0.7f, 1.7f, night)
        }
        Office.STUDIO -> sunOnFloor(iso, room, 1.0f, w - 2.4f, 0.7f, 1.7f, night)
        else -> sunOnFloor(iso, room, 0.3f, w - 0.3f, 0.35f, 1.85f, night)
    }

    // Walls with a cap along the top.
    quad(iso.p(0f, 0f), iso.p(0f, d), iso.p(0f, d, h), iso.p(0f, 0f, h), p.wall.shade(0.9f))
    quad(iso.p(-th, d), iso.p(0f, d), iso.p(0f, d, h), iso.p(-th, d, h), p.wall.shade(0.75f))
    quad(iso.p(-th, -th, h), iso.p(0f, -th, h), iso.p(0f, d, h), iso.p(-th, d, h), p.wallCap)
    quad(iso.p(0f, 0f), iso.p(w, 0f), iso.p(w, 0f, h), iso.p(0f, 0f, h), p.wall)
    quad(iso.p(w, -th), iso.p(w, 0f), iso.p(w, 0f, h), iso.p(w, -th, h), p.wall.shade(0.75f))
    quad(iso.p(-th, -th, h), iso.p(w, -th, h), iso.p(w, 0f, h), iso.p(0f, 0f, h), p.wallCap)
    // Skirting boards.
    quad(iso.p(0f, 0f), iso.p(0f, d), iso.p(0f, d, 0.08f), iso.p(0f, 0f, 0.08f), p.trim.shade(0.9f))
    quad(iso.p(0f, 0f), iso.p(w, 0f), iso.p(w, 0f, 0.08f), iso.p(0f, 0f, 0.08f), p.trim)
    // The corner where the walls meet.
    drawLine(Color.Black.copy(alpha = 0.08f), iso.p(0f, 0f), iso.p(0f, 0f, h), strokeWidth = iso.t * 0.04f)

    val name = model.name
    when (model.office) {
        Office.APARTMENT -> {
            rightWindow(iso, 1.5f, w - 0.6f, 0.75f, 1.65f, p, night, Outside.SKY)
            poster(iso, model.firstLane, p)
        }
        Office.COWORKING -> {
            rightWindow(iso, 1.2f, 2.9f, 0.7f, 1.7f, p, night, Outside.SKY)
            rightWindow(iso, 3.4f, w - 0.5f, 0.7f, 1.7f, p, night, Outside.SKY)
            leftSign(iso, name, d - 0.6f, 1.55f, p, measurer)
            clock(iso, d - 0.5f - 0.15f * name.length.coerceAtMost(14) - 0.9f, p)
        }
        Office.STUDIO -> {
            rightWindow(iso, 1.0f, w - 2.4f, 0.7f, 1.7f, p, night, Outside.SKY)
            whiteboard(iso, d - 0.5f, p)
            leftSign(iso, name, d - 2.4f, 1.6f, p, measurer)
        }
        Office.DOWNTOWN -> {
            rightWindow(iso, 0.3f, w - 0.3f, 0.35f, 1.85f, p, night, Outside.CITY)
            leftSign(iso, name, d - 0.6f, 1.6f, p, measurer)
            whiteboard(iso, d - 0.4f - 0.17f * name.length.coerceAtMost(14) - 0.6f, p)
        }
        Office.CAMPUS -> {
            rightWindow(iso, 0.3f, w - 0.3f, 0.35f, 1.85f, p, night, Outside.TREES)
            leftWindow(iso, d - 2.2f, d - 0.3f, 0.6f, 1.7f, p, night)
            leftSign(iso, name, d - 2.5f - 0.05f, 1.62f, p, measurer, alignRight = true)
        }
    }
    trophies(iso, model.wins, room, p)
    if (Perk.ROOFTOP in model.perks) stringLights(iso, room, night)
}

private fun DrawScope.drawFloor(iso: Iso, room: Room, p: RoomPalette) {
    val w = room.w
    val d = room.d
    when (p.pattern) {
        FloorPattern.PLANKS -> {
            val band = 0.32f
            var y = 0f
            var row = 0
            while (y < d) {
                val y1 = minOf(d, y + band)
                if (row % 2 == 1) quad(iso.p(0f, y), iso.p(w, y), iso.p(w, y1), iso.p(0f, y1), p.floorB)
                // Board ends, staggered.
                var x = ((row * 0.73f) % 1.4f) + 0.4f
                while (x < w) {
                    drawLine(Color.Black.copy(alpha = 0.1f), iso.p(x, y), iso.p(x, y1), strokeWidth = iso.t * 0.02f)
                    x += 1.4f
                }
                drawLine(Color.Black.copy(alpha = 0.08f), iso.p(0f, y1), iso.p(w, y1), strokeWidth = iso.t * 0.02f)
                y = y1
                row++
            }
        }
        FloorPattern.CHECKER, FloorPattern.CARPET -> {
            val step = if (p.pattern == FloorPattern.CHECKER) 0.5f else 1f
            var i = 0
            var x = 0f
            while (x < w) {
                var j = 0
                var y = 0f
                while (y < d) {
                    if ((i + j) % 2 == 1) {
                        val x1 = minOf(w, x + step)
                        val y1 = minOf(d, y + step)
                        quad(iso.p(x, y), iso.p(x1, y), iso.p(x1, y1), iso.p(x, y1), p.floorB)
                    }
                    y += step
                    j++
                }
                x += step
                i++
            }
        }
        FloorPattern.CONCRETE -> {
            var x = 1.5f
            while (x < w) {
                drawLine(Color.Black.copy(alpha = 0.07f), iso.p(x, 0f), iso.p(x, d), strokeWidth = iso.t * 0.02f)
                x += 1.5f
            }
            var y = 1.5f
            while (y < d) {
                drawLine(Color.Black.copy(alpha = 0.07f), iso.p(0f, y), iso.p(w, y), strokeWidth = iso.t * 0.02f)
                y += 1.5f
            }
        }
    }
    // A rug under the desks.
    p.rug?.let { rug ->
        val x0 = X0 - 0.3f
        val y0 = Y0 - 0.25f
        val x1 = room.w - 0.45f
        val y1 = room.d - 0.1f
        quad(iso.p(x0, y0), iso.p(x1, y0), iso.p(x1, y1), iso.p(x0, y1), rug)
        val m = 0.12f
        val border = Path().apply {
            val a = iso.p(x0 + m, y0 + m)
            val b = iso.p(x1 - m, y0 + m)
            val c = iso.p(x1 - m, y1 - m)
            val e = iso.p(x0 + m, y1 - m)
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            lineTo(e.x, e.y)
            close()
        }
        drawPath(border, Color.White.copy(alpha = 0.35f), style = Stroke(iso.t * 0.035f))
    }
}

private enum class Outside { SKY, CITY, TREES }

private fun DrawScope.rightWindow(iso: Iso, x0: Float, x1: Float, z0: Float, z1: Float, p: RoomPalette, night: Boolean, outside: Outside) {
    val glass = quadPath(iso.p(x0, 0f, z0), iso.p(x1, 0f, z0), iso.p(x1, 0f, z1), iso.p(x0, 0f, z1))
    // Frame.
    val f = 0.06f
    quad(iso.p(x0 - f, 0f, z0 - f), iso.p(x1 + f, 0f, z0 - f), iso.p(x1 + f, 0f, z1 + f), iso.p(x0 - f, 0f, z1 + f), p.frame)
    val top = iso.p(x0, 0f, z1).y
    val bottom = iso.p(x1, 0f, z0).y
    drawPath(glass, Brush.verticalGradient(listOf(p.skyTop, p.skyBottom), startY = top, endY = bottom))
    clipPath(glass) {
        if (night) {
            for (k in 0 until 16) {
                val u = x0 + (x1 - x0) * ((k * 0.618f) % 1f)
                val v = z1 - (z1 - z0) * 0.5f * ((k * 0.377f + 0.1f) % 1f)
                drawCircle(Color.White.copy(alpha = 0.75f), iso.t * 0.025f, iso.p(u, 0f, v))
            }
            drawCircle(Color(0xFFFFF4D6), iso.t * 0.13f, iso.p(x0 + (x1 - x0) * 0.8f, 0f, z1 - 0.25f))
        } else {
            drawCircle(Color(0xFFFFF2B3).copy(alpha = 0.9f), iso.t * 0.14f, iso.p(x0 + (x1 - x0) * 0.82f, 0f, z1 - 0.25f))
            for ((u, v, r) in listOf(Triple(0.25f, 0.28f, 0.11f), Triple(0.33f, 0.3f, 0.15f), Triple(0.42f, 0.27f, 0.1f))) {
                drawCircle(Color.White.copy(alpha = 0.9f), iso.t * r, iso.p(x0 + (x1 - x0) * u, 0f, z1 - (z1 - z0) * v))
            }
        }
        when (outside) {
            Outside.SKY -> {
                // Rooftops across the street.
                var u = x0
                var k = 0
                while (u < x1) {
                    val bw = 0.35f + 0.15f * (k % 3)
                    val bh = (z1 - z0) * (0.22f + 0.12f * ((k * 7) % 4) / 3f)
                    quad(iso.p(u, 0f, z0), iso.p(u + bw, 0f, z0), iso.p(u + bw, 0f, z0 + bh), iso.p(u, 0f, z0 + bh), if (night) Color(0xFF232A4A) else Color(0xFFB9C8D8))
                    u += bw + 0.05f
                    k++
                }
            }
            Outside.CITY -> {
                var u = x0
                var k = 0
                while (u < x1) {
                    val bw = 0.32f + 0.12f * (k % 3)
                    val bh = (z1 - z0) * (0.35f + 0.5f * ((k * 7 + 3) % 10) / 10f)
                    val col = if (night) Color(0xFF1E2546) else Color(0xFF9DB4CD).shade(0.92f + 0.04f * (k % 3))
                    quad(iso.p(u, 0f, z0), iso.p(u + bw, 0f, z0), iso.p(u + bw, 0f, z0 + bh), iso.p(u, 0f, z0 + bh), col)
                    // Lit windows.
                    var v = z0 + 0.08f
                    var r = 0
                    while (v < z0 + bh - 0.1f) {
                        for (c in 0..1) {
                            if (!night || (k + r + c) % 3 != 0) {
                                val wx = u + 0.06f + c * bw * 0.45f
                                quad(
                                    iso.p(wx, 0f, v), iso.p(wx + bw * 0.25f, 0f, v), iso.p(wx + bw * 0.25f, 0f, v + 0.06f), iso.p(wx, 0f, v + 0.06f),
                                    if (night) p.accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f),
                                )
                            }
                        }
                        v += 0.14f
                        r++
                    }
                    u += bw + 0.06f
                    k++
                }
            }
            Outside.TREES -> {
                quad(iso.p(x0, 0f, z0), iso.p(x1, 0f, z0), iso.p(x1, 0f, z0 + 0.3f), iso.p(x0, 0f, z0 + 0.3f), if (night) Color(0xFF173226) else Color(0xFF8FD18E))
                var u = x0 + 0.3f
                var k = 0
                while (u < x1) {
                    val r = iso.t * (0.32f + 0.08f * (k % 3))
                    val base = iso.p(u, 0f, z0 + 0.28f)
                    drawRect(Color(0xFF6B4F3A), Offset(base.x - iso.t * 0.03f, base.y - r * 1.2f), Size(iso.t * 0.06f, r * 1.2f))
                    drawCircle(if (night) Color(0xFF1E4A33) else Color(0xFF4DAA5A).shade(0.9f + 0.06f * (k % 3)), r, Offset(base.x, base.y - r * 1.5f))
                    u += 0.7f
                    k++
                }
            }
        }
        // A glint on the glass.
        val g = (x1 - x0) * 0.18f
        quad(iso.p(x0 + g, 0f, z1), iso.p(x0 + g * 1.6f, 0f, z1), iso.p(x0 + g * 0.6f, 0f, z0), iso.p(x0, 0f, z0), Color.White.copy(alpha = if (night) 0.04f else 0.14f))
    }
    // Mullions and the sill.
    val mull = p.frame
    var u = x0 + (x1 - x0) / (if (x1 - x0 > 2.5f) 4 else 2)
    while (u < x1 - 0.05f) {
        drawLine(mull, iso.p(u, 0f, z0), iso.p(u, 0f, z1), strokeWidth = iso.t * 0.05f)
        u += (x1 - x0) / (if (x1 - x0 > 2.5f) 4 else 2)
    }
    if (outside == Outside.SKY) drawLine(mull, iso.p(x0, 0f, (z0 + z1) / 2), iso.p(x1, 0f, (z0 + z1) / 2), strokeWidth = iso.t * 0.05f)
    solid(iso, x0 - 0.08f, 0f, z0 - 0.07f, x1 + 0.08f, 0.1f, z0, p.frame)
    // Daylight on the floor.
    if (!night) {
        val reach = 1.3f
        quad(iso.p(x0 + 0.25f, 0.12f), iso.p(x1 + 0.25f, 0.12f), iso.p(x1 + 0.8f, reach), iso.p(x0 + 0.8f, reach), Color.White.copy(alpha = 0.13f))
    }
}

private fun DrawScope.leftWindow(iso: Iso, y0: Float, y1: Float, z0: Float, z1: Float, p: RoomPalette, night: Boolean) {
    val f = 0.06f
    quad(iso.p(0f, y1 + f, z0 - f), iso.p(0f, y0 - f, z0 - f), iso.p(0f, y0 - f, z1 + f), iso.p(0f, y1 + f, z1 + f), p.frame.shade(0.92f))
    val glass = quadPath(iso.p(0f, y1, z0), iso.p(0f, y0, z0), iso.p(0f, y0, z1), iso.p(0f, y1, z1))
    drawPath(glass, Brush.verticalGradient(listOf(p.skyTop, p.skyBottom), startY = iso.p(0f, y0, z1).y, endY = iso.p(0f, y1, z0).y))
    clipPath(glass) {
        quad(iso.p(0f, y1, z0), iso.p(0f, y0, z0), iso.p(0f, y0, z0 + 0.3f), iso.p(0f, y1, z0 + 0.3f), if (night) Color(0xFF173226) else Color(0xFF8FD18E))
        var v = y0 + 0.35f
        var k = 0
        while (v < y1) {
            val r = iso.t * (0.3f + 0.08f * (k % 2))
            val base = iso.p(0f, v, z0 + 0.28f)
            drawRect(Color(0xFF6B4F3A), Offset(base.x - iso.t * 0.03f, base.y - r * 1.2f), Size(iso.t * 0.06f, r * 1.2f))
            drawCircle(if (night) Color(0xFF1E4A33) else Color(0xFF55B061), r, Offset(base.x, base.y - r * 1.5f))
            v += 0.65f
            k++
        }
    }
    drawLine(p.frame, iso.p(0f, (y0 + y1) / 2, z0), iso.p(0f, (y0 + y1) / 2, z1), strokeWidth = iso.t * 0.05f)
    solid(iso, 0f, y0 - 0.08f, z0 - 0.07f, 0.1f, y1 + 0.08f, z0, p.frame)
}

/** The company name on the left wall, slanted along it. */
private fun DrawScope.leftSign(iso: Iso, name: String, yStart: Float, z: Float, p: RoomPalette, measurer: TextMeasurer, alignRight: Boolean = false) {
    val style = TextStyle(fontSize = (iso.t * 0.3f).toSp(), fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp)
    val layout = measurer.measure(name.take(18), style)
    val padX = iso.t * 0.18f
    val padY = iso.t * 0.1f
    val width = layout.size.width + padX * 2
    val startY = if (alignRight) yStart + width / iso.t else yStart
    onLeftWall(iso, startY, z) {
        drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(iso.t * 0.04f, iso.t * 0.06f), Size(width, layout.size.height + padY * 2), CornerRadius(iso.t))
        drawRoundRect(p.accent, Offset.Zero, Size(width, layout.size.height + padY * 2), CornerRadius(iso.t))
        drawText(layout, topLeft = Offset(padX, padY))
    }
}

private fun DrawScope.poster(iso: Iso, lane: Lane, p: RoomPalette) {
    val tint = laneTint(lane)
    onLeftWall(iso, 2.7f, 1.7f) {
        val pw = iso.t * 0.8f
        val ph = iso.t * 1.05f
        drawRect(p.frame, Offset(-iso.t * 0.05f, -iso.t * 0.05f), Size(pw + iso.t * 0.1f, ph + iso.t * 0.1f))
        drawRect(Brush.verticalGradient(listOf(tint, tint.shade(0.7f)), 0f, ph), Offset.Zero, Size(pw, ph))
        drawCircle(Color.White.copy(alpha = 0.85f), pw * 0.2f, Offset(pw / 2, ph * 0.4f))
        drawRect(Color.White.copy(alpha = 0.7f), Offset(pw * 0.2f, ph * 0.75f), Size(pw * 0.6f, ph * 0.06f))
    }
}

private fun DrawScope.whiteboard(iso: Iso, yStart: Float, p: RoomPalette) {
    onLeftWall(iso, yStart, 1.6f) {
        val bw = iso.t * 1.5f
        val bh = iso.t * 0.85f
        drawRoundRect(p.frame.shade(0.85f), Offset(-iso.t * 0.05f, -iso.t * 0.05f), Size(bw + iso.t * 0.1f, bh + iso.t * 0.1f), CornerRadius(iso.t * 0.05f))
        drawRect(Color(0xFFFBFBFD).shade(if (p.wall.red < 0.5f) 0.55f else 1f), Offset.Zero, Size(bw, bh))
        val marks = listOf(Color(0xFF2F7BF6), Color(0xFFFF6B3D), Color(0xFF34C759))
        marks.forEachIndexed { i, c ->
            drawLine(c, Offset(bw * 0.1f, bh * (0.25f + i * 0.22f)), Offset(bw * (0.45f + i * 0.12f), bh * (0.25f + i * 0.22f)), strokeWidth = iso.t * 0.05f, cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFFFF6B3D), bh * 0.18f, Offset(bw * 0.8f, bh * 0.45f), style = Stroke(iso.t * 0.04f))
        drawRect(p.frame.shade(0.7f), Offset(bw * 0.1f, bh), Size(bw * 0.8f, iso.t * 0.05f))
    }
}

private fun DrawScope.clock(iso: Iso, yStart: Float, p: RoomPalette) {
    onLeftWall(iso, yStart, 1.75f) {
        val r = iso.t * 0.22f
        drawCircle(p.frame.shade(0.8f), r * 1.12f, Offset(r, r))
        drawCircle(Color.White.shade(if (p.wall.red < 0.5f) 0.6f else 1f), r, Offset(r, r))
        drawLine(Color(0xFF2B2B30), Offset(r, r), Offset(r, r * 0.4f), strokeWidth = iso.t * 0.035f, cap = StrokeCap.Round)
        drawLine(Color(0xFF2B2B30), Offset(r, r), Offset(r * 1.45f, r * 1.15f), strokeWidth = iso.t * 0.035f, cap = StrokeCap.Round)
    }
}

/** Game of the Year trophies on a shelf. */
private fun DrawScope.trophies(iso: Iso, wins: Int, room: Room, p: RoomPalette) {
    if (wins <= 0) return
    val n = wins.coerceAtMost(5)
    val x0 = room.w - 0.35f - n * 0.28f
    solid(iso, x0, 0f, 1.88f, room.w - 0.25f, 0.18f, 1.92f, p.deskSide)
    for (k in 0 until n) {
        val cx = x0 + 0.17f + k * 0.28f
        val base = iso.p(cx, 0.09f, 1.92f)
        val s = iso.t
        drawRect(Color(0xFF8A6A1F), Offset(base.x - s * 0.06f, base.y - s * 0.05f), Size(s * 0.12f, s * 0.05f))
        drawRect(Color(0xFFE0B23A), Offset(base.x - s * 0.02f, base.y - s * 0.12f), Size(s * 0.04f, s * 0.08f))
        drawArc(Color(0xFFFFD45C), 0f, 180f, true, Offset(base.x - s * 0.08f, base.y - s * 0.26f), Size(s * 0.16f, s * 0.2f))
    }
}

private fun DrawScope.stringLights(iso: Iso, room: Room, night: Boolean) {
    val bulbs = listOf(Color(0xFFFFD27A), Color(0xFFFF7A9C), Color(0xFF7AD1FF), Color(0xFFB4FF7A))
    fun strand(a: (Float) -> Offset, n: Int, offset: Int) {
        val path = Path()
        for (k in 0..40) {
            val t = k / 40f
            val pt = a(t)
            if (k == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path, Color(0xFF3A3A3A), style = Stroke(iso.t * 0.02f))
        for (k in 0 until n) {
            val pt = a((k + 0.5f) / n)
            val c = bulbs[(k + offset) % bulbs.size]
            if (night) drawCircle(c.copy(alpha = 0.25f), iso.t * 0.12f, pt)
            drawCircle(c, iso.t * 0.045f, pt)
        }
    }
    fun sag(t: Float) = 0.18f * (1 - (2 * ((t * 3) % 1f) - 1).let { it * it })
    strand({ t -> iso.p(room.w * t, 0f, room.h - 0.08f - sag(t)) }, 14, 0)
    strand({ t -> iso.p(0f, room.d * t, room.h - 0.08f - sag(t)) }, 12, 2)
}

// Furniture along the walls

/** A spot along a wall: u runs along it (left to right on screen), w out into the room, v up. */
private class Spot(val iso: Iso, val leftWall: Boolean, val a: Float, val len: Float) {
    fun p(u: Float, w: Float, v: Float): Offset = if (!leftWall) iso.p(a + u, w, v) else iso.p(w, a + len - u, v)
    fun box(scope: DrawScope, u0: Float, u1: Float, w0: Float, w1: Float, v0: Float, v1: Float, c: Color, top: Color = c.shade(1.06f)) {
        if (!leftWall) scope.solid(iso, a + u0, w0, v0, a + u1, w1, v1, c, top) else scope.solid(iso, w0, a + len - u1, v0, w1, a + len - u0, v1, c, top)
    }
    fun face(scope: DrawScope, u0: Float, u1: Float, v0: Float, v1: Float, w: Float, c: Color) =
        scope.quad(p(u0, w, v0), p(u1, w, v0), p(u1, w, v1), p(u0, w, v1), c)
    val depth: Float get() = if (!leftWall) a + len / 2 + 0.3f else 0.3f + a + len / 2
}

private class WallItem(val len: Float, val draw: DrawScope.(Spot) -> Unit)

private fun furnish(props: MutableList<Prop>, iso: Iso, room: Room, model: SceneModel, p: RoomPalette, night: Boolean, anim: Float?) {
    val perks = model.perks
    val items = mutableListOf<WallItem>()
    val leftItems = mutableListOf<WallItem>()
    if (model.office == Office.APARTMENT) leftItems += WallItem(1.5f) { couch(it, p) }
    if (Perk.COFFEE in perks) items += WallItem(0.95f) { coffee(it, p, anim) }
    if (Perk.LIBRARY in perks) items += WallItem(0.9f) { bookshelf(it, p) }
    if (Perk.ARCADE in perks) items += WallItem(0.55f) { arcade(it, night, anim) }
    if (Perk.CHEF in perks) items += WallItem(1.35f) { kitchen(it, p, anim) }
    if (Perk.NAP_PODS in perks) leftItems += WallItem(1.0f) { napPod(it, night) }
    if (Perk.GYM in perks) leftItems += WallItem(0.9f) { treadmill(it) }
    if (Perk.DAYCARE in perks) leftItems += WallItem(1.0f) { playMat(it) }
    if (model.office != Office.APARTMENT) items += WallItem(0.45f) { waterCooler(it) }
    if (model.servers) leftItems += WallItem(0.55f) { serverRack(it, anim) }

    // Along the right wall from the corner, then down the left wall; whatever doesn't fit moves over.
    var x = 1.3f
    var y = 1.15f
    val rightEnd = room.w - 0.15f
    val leftEnd = room.d - 0.1f
    fun place(item: WallItem, preferLeft: Boolean) {
        val fitsRight = x + item.len <= rightEnd
        val fitsLeft = y + item.len <= leftEnd
        val left = if (preferLeft) fitsLeft || !fitsRight else !fitsRight && fitsLeft
        if (left && fitsLeft) {
            val spot = Spot(iso, true, y, item.len)
            props += Prop(spot.depth) { item.draw(this, spot) }
            y += item.len + 0.12f
        } else if (fitsRight) {
            val spot = Spot(iso, false, x, item.len)
            props += Prop(spot.depth) { item.draw(this, spot) }
            x += item.len + 0.12f
        }
    }
    leftItems.forEach { place(it, preferLeft = true) }
    items.forEach { place(it, preferLeft = false) }

    // Plants in the corners, more of them with the perk.
    val plants = mutableListOf(0.45f to 0.45f)
    if (Perk.PLANTS in perks) {
        plants += room.w - 0.4f to Y0 + 0.2f
        plants += 0.4f to room.d - 0.45f
        plants += room.w - 0.35f to room.d - 0.4f
    }
    if (model.office == Office.CAMPUS) plants += room.w - 0.4f to room.d / 2
    for ((px, py) in plants) props += Prop(px + py) { plant(iso, px, py, p, big = model.office >= Office.STUDIO) }
}

private fun DrawScope.plant(iso: Iso, x: Float, y: Float, p: RoomPalette, big: Boolean) {
    val s = if (big) 1.25f else 1f
    shadow(iso, x, y, 0.2f * s, 0.2f * s)
    solid(iso, x - 0.15f * s, y - 0.15f * s, 0f, x + 0.15f * s, y + 0.15f * s, 0.32f * s, Color(0xFFCB6E46))
    val top = iso.p(x, y, 0.32f * s)
    val leaf = Color(0xFF3FA35B).let { if (p.wall.red < 0.5f) it.shade(0.55f) else it }
    val r = iso.t * 0.24f * s
    drawCircle(leaf.shade(0.85f), r, Offset(top.x - r * 0.7f, top.y - r * 0.9f))
    drawCircle(leaf, r * 1.1f, Offset(top.x + r * 0.5f, top.y - r * 1.3f))
    drawCircle(leaf.shade(1.1f), r * 0.95f, Offset(top.x - r * 0.1f, top.y - r * 2.1f))
    if (big) drawCircle(leaf.shade(0.95f), r * 0.8f, Offset(top.x + r * 0.2f, top.y - r * 2.9f))
}

private fun DrawScope.couch(s: Spot, p: RoomPalette) {
    val fabric = p.rug?.shade(0.85f) ?: Color(0xFF6C7A96)
    shadow(s.iso, if (s.leftWall) 0.4f else s.a + 0.75f, if (s.leftWall) s.a + 0.75f else 0.4f, 0.45f, 0.75f, 0.1f)
    s.box(this, 0f, 1.5f, 0f, 0.2f, 0f, 0.62f, fabric)
    s.box(this, 0f, 1.5f, 0.2f, 0.72f, 0f, 0.3f, fabric)
    s.box(this, 0f, 0.18f, 0.2f, 0.72f, 0.3f, 0.45f, fabric.shade(0.95f))
    s.box(this, 1.32f, 1.5f, 0.2f, 0.72f, 0.3f, 0.45f, fabric.shade(0.95f))
    s.box(this, 0.25f, 0.65f, 0.15f, 0.28f, 0.3f, 0.55f, Color(0xFFFFD27A))
}

private fun DrawScope.coffee(s: Spot, p: RoomPalette, anim: Float?) {
    s.box(this, 0f, 0.95f, 0f, 0.45f, 0f, 0.6f, p.deskSide, top = p.deskTop)
    s.box(this, 0.1f, 0.45f, 0.05f, 0.33f, 0.6f, 0.95f, Color(0xFF3A3A42))
    s.face(this, 0.15f, 0.4f, 0.8f, 0.9f, 0.331f, Color(0xFF9AA0A6))
    s.box(this, 0.6f, 0.7f, 0.15f, 0.25f, 0.6f, 0.7f, Color.White)
    s.box(this, 0.75f, 0.85f, 0.2f, 0.3f, 0.6f, 0.7f, Color(0xFFFF9F0A))
    if (anim != null) steam(s.p(0.28f, 0.2f, 1.0f), s.iso.t, anim)
}

private fun DrawScope.steam(at: Offset, t: Float, anim: Float) {
    for (k in 0..2) {
        val f = (anim + k / 3f) % 1f
        drawCircle(Color.White.copy(alpha = 0.55f * (1 - f)), t * (0.05f + 0.05f * f), Offset(at.x + sin(f * 6f + k) * t * 0.05f, at.y - f * t * 0.6f))
    }
}

private fun DrawScope.bookshelf(s: Spot, p: RoomPalette) {
    s.box(this, 0f, 0.9f, 0f, 0.32f, 0f, 1.5f, p.deskSide.shade(0.9f), top = p.deskSide)
    s.face(this, 0.05f, 0.85f, 0.05f, 1.45f, 0.321f, p.deskSide.shade(0.6f))
    val spines = listOf(Color(0xFFFF453A), Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF9F0A), Color(0xFFBF5AF2), Color(0xFFF2F2F7))
    for (row in 0..3) {
        val v0 = 0.08f + row * 0.35f
        var u = 0.08f
        var k = row
        while (u < 0.8f) {
            val bw = 0.06f + 0.02f * (k % 3)
            val bh = 0.24f + 0.04f * (k % 2)
            s.face(this, u, minOf(0.82f, u + bw), v0, v0 + bh, 0.322f, spines[k % spines.size])
            u += bw + 0.012f
            k++
        }
        s.face(this, 0.05f, 0.85f, v0 - 0.03f, v0, 0.323f, p.deskSide)
    }
}

private fun DrawScope.arcade(s: Spot, night: Boolean, anim: Float?) {
    val body = Color(0xFF5B2EBF)
    s.box(this, 0f, 0.55f, 0f, 0.5f, 0f, 1.45f, body)
    s.box(this, 0f, 0.55f, 0.5f, 0.68f, 0.72f, 0.82f, body.shade(0.8f))
    s.face(this, 0.07f, 0.48f, 0.9f, 1.25f, 0.501f, Color(0xFF101018))
    val glow = if (anim != null) 0.75f + 0.25f * sin(anim * 2 * PI.toFloat()) else 0.9f
    s.face(this, 0.1f, 0.45f, 0.93f, 1.22f, 0.502f, Color(0xFF2DD4E8).copy(alpha = glow).shade(if (night) 1.1f else 1f))
    s.face(this, 0.15f, 0.25f, 1.0f, 1.1f, 0.503f, Color(0xFFFFD60A))
    s.face(this, 0.03f, 0.52f, 1.3f, 1.42f, 0.501f, Color(0xFFFF453A))
    drawCircle(Color(0xFFFF453A), s.iso.t * 0.04f, s.p(0.18f, 0.6f, 0.83f))
    drawCircle(Color(0xFF30D158), s.iso.t * 0.035f, s.p(0.36f, 0.6f, 0.83f))
}

private fun DrawScope.kitchen(s: Spot, p: RoomPalette, anim: Float?) {
    s.box(this, 0f, 1.35f, 0f, 0.5f, 0f, 0.6f, Color(0xFF4F8F8B), top = Color(0xFFF2F2F2))
    for (k in 0..2) s.face(this, 0.05f + k * 0.44f, 0.43f + k * 0.44f, 0.08f, 0.5f, 0.501f, Color(0xFF5FA39E))
    s.box(this, 0.15f, 0.55f, 0.08f, 0.42f, 0.6f, 0.62f, Color(0xFF2B2B30))
    s.box(this, 0.22f, 0.45f, 0.15f, 0.36f, 0.62f, 0.78f, Color(0xFFB0B4BA))
    s.box(this, 0.8f, 1.2f, 0.1f, 0.4f, 0.6f, 0.66f, Color(0xFFFFFFFF))
    drawCircle(Color(0xFFFF6B3D), s.iso.t * 0.06f, s.p(0.9f, 0.25f, 0.68f))
    drawCircle(Color(0xFF34C759), s.iso.t * 0.06f, s.p(1.05f, 0.25f, 0.68f))
    if (anim != null) steam(s.p(0.33f, 0.25f, 0.8f), s.iso.t, anim)
}

private fun DrawScope.napPod(s: Spot, night: Boolean) {
    s.box(this, 0f, 1.0f, 0f, 0.75f, 0f, 0.62f, Color(0xFFF2F2F7))
    s.face(this, 0.18f, 0.82f, 0.12f, 0.5f, 0.751f, Color(0xFF2E3340))
    s.face(this, 0.24f, 0.76f, 0.12f, 0.2f, 0.752f, if (night) Color(0xFF7AA7FF) else Color(0xFFB8C6E6))
}

private fun DrawScope.treadmill(s: Spot) {
    s.box(this, 0.05f, 0.85f, 0.1f, 0.65f, 0f, 0.1f, Color(0xFF3A3F4B))
    s.box(this, 0.1f, 0.8f, 0.15f, 0.6f, 0.1f, 0.12f, Color(0xFF22252C))
    s.box(this, 0.05f, 0.12f, 0.05f, 0.12f, 0.1f, 0.85f, Color(0xFF8E939E))
    s.box(this, 0.78f, 0.85f, 0.05f, 0.12f, 0.1f, 0.85f, Color(0xFF8E939E))
    s.box(this, 0.05f, 0.85f, 0.05f, 0.18f, 0.82f, 0.9f, Color(0xFF8E939E))
    s.face(this, 0.35f, 0.55f, 0.84f, 0.88f, 0.181f, Color(0xFF30D158))
}

private fun DrawScope.playMat(s: Spot) {
    val mat = listOf(Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF4ECDC4), Color(0xFF6C8CFF))
    for (i in 0..1) for (j in 0..1) {
        val u0 = i * 0.5f
        val w0 = 0.05f + j * 0.45f
        s.box(this, u0, u0 + 0.5f, w0, w0 + 0.45f, 0f, 0.02f, mat[(i + j * 2) % 4])
    }
    s.box(this, 0.15f, 0.33f, 0.2f, 0.38f, 0.02f, 0.2f, Color(0xFFFF453A))
    s.box(this, 0.38f, 0.56f, 0.25f, 0.43f, 0.02f, 0.2f, Color(0xFF0A84FF))
    s.box(this, 0.26f, 0.44f, 0.22f, 0.4f, 0.2f, 0.38f, Color(0xFFFFD60A))
    drawCircle(Color(0xFF30D158), s.iso.t * 0.11f, s.p(0.78f, 0.6f, 0.11f))
    drawCircle(Color.White.copy(alpha = 0.5f), s.iso.t * 0.035f, s.p(0.75f, 0.6f, 0.15f))
}

private fun DrawScope.serverRack(s: Spot, anim: Float?) {
    s.box(this, 0.02f, 0.53f, 0.02f, 0.5f, 0f, 1.45f, Color(0xFF23262E))
    val leds = listOf(Color(0xFF30D158), Color(0xFF0A84FF), Color(0xFFFFD60A))
    for (row in 0 until 7) {
        val v0 = 0.12f + row * 0.18f
        s.face(this, 0.07f, 0.48f, v0, v0 + 0.13f, 0.501f, Color(0xFF353A45))
        for (k in 0..2) {
            val on = anim == null || ((anim * 7 + row * 0.37f + k * 0.51f) % 1f) > 0.3f
            val c = leds[(row + k) % leds.size]
            drawCircle(if (on) c else c.copy(alpha = 0.25f), s.iso.t * 0.018f, s.p(0.12f + k * 0.06f, 0.502f, v0 + 0.065f))
        }
    }
}

private fun DrawScope.waterCooler(s: Spot) {
    s.box(this, 0.08f, 0.38f, 0.08f, 0.38f, 0f, 0.62f, Color(0xFFE5E5EA))
    val c = s.p(0.23f, 0.23f, 0.85f)
    val r = s.iso.t * 0.15f
    drawRoundRect(Color(0xFF8FCBFF).copy(alpha = 0.85f), Offset(c.x - r, c.y - r * 1.6f), Size(2 * r, r * 2.4f), CornerRadius(r * 0.6f))
    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(c.x - r * 0.7f, c.y - r * 1.4f), Size(r * 0.3f, r * 1.8f), CornerRadius(r * 0.15f))
}

// Desks and people

private fun laneTint(lane: Lane?): Color = when (lane) {
    Lane.GAMES -> Color(0xFFFF2D55)
    Lane.WEBSITES -> Color(0xFF007AFF)
    Lane.APPS -> Color(0xFF34C759)
    Lane.SOCIAL -> Color(0xFFFF9500)
    Lane.HARDWARE -> Color(0xFFAF52DE)
    Lane.AI -> Color(0xFF32ADE6)
    Lane.OS -> Color(0xFF5856D6)
    null -> Color(0xFF8E8E93)
}

private fun station(props: MutableList<Prop>, iso: Iso, index: Int, seat: Seat, worker: Worker?, shirt: Color, badge: Color?, p: RoomPalette, night: Boolean, phase: Float?) {
    val cx = seat.cx
    val cy = seat.cy
    val front = seat.facing == Facing.FRONT
    val deskY0 = if (front) cy + 0.27f else cy - 0.79f
    val deskY1 = deskY0 + 0.52f
    val x0 = cx - DESK_W / 2 + 0.02f
    val x1 = cx + DESK_W / 2 - 0.02f
    val occupied = worker != null
    val wave = phase?.let { sin(2 * PI.toFloat() * it) } ?: 0f

    // The chair: base and seat first, then whoever sits on it, then the backrest if it's between us and them.
    val chairY = if (occupied) cy else cy + (if (front) 0.12f else -0.12f)
    props += Prop(cx + chairY - 0.05f) {
        shadow(iso, cx, chairY, 0.22f, 0.22f, 0.1f)
        solid(iso, cx - 0.2f, chairY - 0.03f, 0f, cx + 0.2f, chairY + 0.03f, 0.04f, p.chair.shade(0.7f))
        solid(iso, cx - 0.03f, chairY - 0.2f, 0f, cx + 0.03f, chairY + 0.2f, 0.04f, p.chair.shade(0.7f))
        solid(iso, cx - 0.025f, chairY - 0.025f, 0.04f, cx + 0.025f, chairY + 0.025f, 0.34f, Color(0xFF7A7F8A))
        solid(iso, cx - 0.18f, chairY - 0.17f, 0.34f, cx + 0.18f, chairY + 0.17f, SEAT_Z, p.seat)
        if (front) solid(iso, cx - 0.17f, chairY - 0.22f, 0.42f, cx + 0.17f, chairY - 0.16f, 0.8f, p.chair)
    }
    // Where they sit and where their hands rest on the keyboard.
    val ky = if (front) deskY0 + 0.08f else deskY1 - 0.18f
    val hip = iso.p(cx, cy + (if (front) 0.02f else 0f), SEAT_Z)
    val hands = iso.p(cx - 0.1f, ky + 0.05f, DESK_Z + 0.03f) to iso.p(cx + 0.08f, ky + 0.05f, DESK_Z + 0.03f)
    if (worker != null) {
        props += Prop(cx + cy) {
            drawSeated(hip, iso.zpx, worker.look, shirt, seat.facing, phase, crown = worker.founder, hands = hands, badge = badge)
        }
    }
    if (!front) {
        props += Prop(cx + chairY + 0.2f) {
            solid(iso, cx - 0.17f, chairY + 0.16f, 0.46f, cx + 0.17f, chairY + 0.22f, 0.76f, p.chair)
            solid(iso, cx - 0.04f, chairY + 0.17f, SEAT_Z, cx + 0.04f, chairY + 0.21f, 0.42f, Color(0xFF7A7F8A))
        }
    }

    // The desk and everything on it.
    props += Prop(cx + (deskY0 + deskY1) / 2) {
        shadow(iso, cx, (deskY0 + deskY1) / 2, DESK_W / 2, 0.3f, 0.08f)
        solid(iso, x0, deskY0 + 0.04f, 0f, x0 + 0.05f, deskY1 - 0.04f, DESK_Z - 0.04f, p.deskSide)
        solid(iso, x1 - 0.05f, deskY0 + 0.04f, 0f, x1, deskY1 - 0.04f, DESK_Z - 0.04f, p.deskSide)
        // A modesty panel down the middle of the island hides the legs of whoever faces us.
        if (front) solid(iso, x0 + 0.05f, deskY1 - 0.05f, 0.12f, x1 - 0.05f, deskY1 - 0.02f, DESK_Z - 0.04f, p.deskSide.shade(0.9f))
        solid(iso, x0, deskY0, DESK_Z - 0.04f, x1, deskY1, DESK_Z, p.deskSide, top = p.deskTop)

        // Keyboard and mouse on the sitter's side.
        solid(iso, cx - 0.17f, ky, DESK_Z, cx + 0.15f, ky + 0.1f, DESK_Z + 0.02f, Color(0xFF4A4F5C), top = Color(0xFF5D6370))
        solid(iso, cx + 0.22f, ky + 0.02f, DESK_Z, cx + 0.27f, ky + 0.08f, DESK_Z + 0.025f, Color(0xFF4A4F5C))

        // Something personal on every desk.
        val my = if (front) deskY0 + 0.12f else deskY1 - 0.2f
        when (index % 4) {
            0 -> mug(iso, x0 + 0.13f, my, Color(0xFFFF9F0A))
            1 -> {
                solid(iso, x1 - 0.22f, my - 0.04f, DESK_Z, x1 - 0.12f, my + 0.06f, DESK_Z + 0.09f, Color(0xFFCB6E46))
                val top = iso.p(x1 - 0.17f, my + 0.01f, DESK_Z + 0.09f)
                drawCircle(Color(0xFF3FA35B).let { if (night) it.shade(0.6f) else it }, iso.t * 0.08f, Offset(top.x, top.y - iso.t * 0.06f))
            }
            2 -> solid(iso, x0 + 0.06f, my - 0.05f, DESK_Z, x0 + 0.26f, my + 0.1f, DESK_Z + 0.012f, Color(0xFFF7F7F7))
            else -> mug(iso, x1 - 0.15f, my, Color(0xFF0A84FF))
        }
        // Someone facing us rests their forearms on the desk.
        if (worker != null && front) drawForearms(hip, iso.zpx, worker.look, shirt, hands, phase)
        if (night && index % 3 == 1 && occupied) lamp(iso, x0 + 0.1f, if (front) deskY0 + 0.38f else deskY0 + 0.1f, p)

        // The monitor, back to back with the one opposite.
        val my0 = if (front) deskY1 - 0.12f else deskY0 + 0.05f
        val my1 = my0 + 0.05f
        solid(iso, cx - 0.04f, my0 - 0.03f, DESK_Z, cx + 0.04f, my1 + 0.03f, DESK_Z + 0.01f, Color(0xFF2B2B30))
        solid(iso, cx - 0.02f, my0 + 0.01f, DESK_Z, cx + 0.02f, my1 - 0.01f, DESK_Z + 0.08f, Color(0xFF3A3A42))
        solid(iso, cx - 0.27f, my0, DESK_Z + 0.07f, cx + 0.27f, my1, DESK_Z + 0.4f, Color(0xFF2B2B30))
        if (front) {
            // We see its back: a small light shows whose lane it is.
            if (occupied) drawCircle(laneTint(worker?.lane).copy(alpha = 0.9f), iso.t * 0.025f, iso.p(cx, my1 + 0.001f, DESK_Z + 0.32f))
        } else {
            screen(iso, cx - 0.24f, cx + 0.24f, my1 + 0.002f, DESK_Z + 0.1f, DESK_Z + 0.37f, if (occupied) worker?.lane ?: Lane.GAMES else null, index, wave, night)
        }
    }
}

private fun DrawScope.mug(iso: Iso, x: Float, y: Float, c: Color) {
    solid(iso, x - 0.04f, y - 0.04f, DESK_Z, x + 0.04f, y + 0.04f, DESK_Z + 0.09f, c)
    val top = iso.p(x, y, DESK_Z + 0.09f)
    drawOval(Color(0xFF4A2C1D), Offset(top.x - iso.t * 0.05f, top.y - iso.t * 0.02f), Size(iso.t * 0.1f, iso.t * 0.04f))
}

private fun DrawScope.lamp(iso: Iso, x: Float, y: Float, p: RoomPalette) {
    solid(iso, x - 0.05f, y - 0.05f, DESK_Z, x + 0.05f, y + 0.05f, DESK_Z + 0.02f, Color(0xFF3A3A42))
    drawLine(Color(0xFF5A5A64), iso.p(x, y, DESK_Z + 0.02f), iso.p(x + 0.05f, y, DESK_Z + 0.3f), strokeWidth = iso.t * 0.025f)
    val head = iso.p(x + 0.08f, y, DESK_Z + 0.3f)
    drawCircle(Brush.radialGradient(listOf(p.accent.copy(alpha = 0.45f), Color.Transparent), head, iso.t * 0.7f), iso.t * 0.7f, head)
    drawCircle(p.accent, iso.t * 0.05f, head)
}

/** A screen facing us, showing what the lane works on. */
private fun DrawScope.screen(iso: Iso, x0: Float, x1: Float, y: Float, z0: Float, z1: Float, lane: Lane?, index: Int, wave: Float, night: Boolean) {
    fun sp(x: Float, z: Float) = iso.p(x, y, z)
    fun rect(a: Float, b: Float, c: Float, d: Float, color: Color) = quad(sp(a, c), sp(b, c), sp(b, d), sp(a, d), color)
    if (lane == null) {
        rect(x0, x1, z0, z1, Color(0xFF1E2129))
        quad(sp(x0 + 0.05f, z1), sp(x0 + 0.12f, z1), sp(x0 + 0.02f, z0), sp(x0, z0), Color.White.copy(alpha = 0.06f))
        return
    }
    val w = x1 - x0
    val h = z1 - z0
    val tint = laneTint(lane)
    when (lane) {
        Lane.GAMES -> {
            if (index % 2 == 0) {
                // A level in the editor.
                rect(x0, x1, z0, z1, Color(0xFF6EC6FF))
                rect(x0, x1, z0, z0 + h * 0.3f, Color(0xFF4CAF50))
                rect(x0 + w * 0.55f, x0 + w * 0.8f, z0 + h * 0.3f, z0 + h * 0.5f, Color(0xFF8D6E63))
                val hop = (wave + 1) * 0.5f * h * 0.15f
                rect(x0 + w * 0.2f, x0 + w * 0.32f, z0 + h * 0.3f + hop, z0 + h * 0.5f + hop, Color(0xFFFF3B30))
                rect(x0 + w * 0.78f, x0 + w * 0.86f, z0 + h * 0.62f, z0 + h * 0.72f, Color(0xFFFFD60A))
            } else {
                codeScreen(iso, x0, x1, y, z0, z1, tint, index, wave)
            }
        }
        Lane.WEBSITES -> {
            rect(x0, x1, z0, z1, Color(0xFFF7F8FA))
            rect(x0, x1, z1 - h * 0.18f, z1, tint)
            rect(x0 + w * 0.08f, x0 + w * 0.55f, z0 + h * 0.45f, z0 + h * 0.72f, tint.copy(alpha = 0.3f))
            rect(x0 + w * 0.62f, x0 + w * 0.92f, z0 + h * 0.45f, z0 + h * 0.72f, Color(0xFFE0E3E8))
            rect(x0 + w * 0.08f, x0 + w * 0.92f, z0 + h * 0.12f, z0 + h * 0.3f, Color(0xFFE0E3E8))
        }
        Lane.APPS -> {
            rect(x0, x1, z0, z1, Color(0xFF20242E))
            rect(x0 + w * 0.36f, x0 + w * 0.64f, z0 + h * 0.06f, z1 - h * 0.06f, Color(0xFFF7F8FA))
            for (k in 0..2) rect(x0 + w * 0.4f, x0 + w * 0.6f, z1 - h * (0.25f + k * 0.22f), z1 - h * (0.12f + k * 0.22f), tint.copy(alpha = 0.5f + 0.2f * k))
        }
        Lane.SOCIAL -> {
            rect(x0, x1, z0, z1, Color(0xFFF7F8FA))
            for (k in 0..2) {
                val top = z1 - h * (0.08f + k * 0.31f)
                rect(x0 + w * 0.08f, x0 + w * 0.92f, top - h * 0.25f, top, Color(0xFFE9ECF1))
                drawCircle(tint, iso.t * 0.03f, sp(x0 + w * 0.15f, top - h * 0.12f))
                rect(x0 + w * 0.25f, x0 + w * (0.55f + 0.1f * ((k + index) % 3)), top - h * 0.16f, top - h * 0.1f, Color(0xFFB0B6C0))
            }
        }
        Lane.HARDWARE -> {
            rect(x0, x1, z0, z1, Color(0xFF14233A))
            for (k in 1..4) {
                drawLine(Color(0xFF2B4A70), sp(x0 + w * k / 5f, z0), sp(x0 + w * k / 5f, z1), strokeWidth = iso.t * 0.01f)
            }
            rect(x0 + w * 0.3f, x0 + w * 0.7f, z0 + h * 0.2f, z0 + h * 0.8f, tint.copy(alpha = 0.25f))
            drawCircle(Color(0xFF7FD3FF), iso.t * 0.04f, sp(x0 + w * 0.5f, z0 + h * 0.3f), style = Stroke(iso.t * 0.012f))
        }
        Lane.AI -> {
            rect(x0, x1, z0, z1, Color(0xFF111827))
            val path = Path()
            for (k in 0..12) {
                val f = k / 12f
                val v = z0 + h * (0.15f + 0.7f * (1 - f) * (1 - f) + 0.04f * sin(f * 9f + wave))
                val pt = sp(x0 + w * (0.06f + 0.88f * f), v)
                if (k == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, tint, style = Stroke(iso.t * 0.025f, cap = StrokeCap.Round))
        }
        Lane.OS -> {
            rect(x0, x1, z0, z1, Color(0xFF1A1F2E))
            rect(x0 + w * 0.08f, x0 + w * 0.92f, z0 + h * 0.12f, z1 - h * 0.12f, Color(0xFF2A3144))
            rect(x0 + w * 0.08f, x0 + w * 0.92f, z1 - h * 0.28f, z1 - h * 0.12f, tint.copy(alpha = 0.55f))
            for (k in 0..3) {
                rect(x0 + w * 0.14f, x0 + w * (0.35f + 0.1f * k), z0 + h * (0.55f - k * 0.1f), z0 + h * (0.62f - k * 0.1f), Color.White.copy(alpha = 0.25f))
            }
        }
    }
    // Glare.
    quad(sp(x0 + 0.05f, z1), sp(x0 + 0.1f, z1), sp(x0 + 0.02f, z0), sp(x0, z0), Color.White.copy(alpha = if (night) 0.05f else 0.12f))
}

private fun DrawScope.codeScreen(iso: Iso, x0: Float, x1: Float, y: Float, z0: Float, z1: Float, tint: Color, index: Int, wave: Float) {
    fun sp(x: Float, z: Float) = iso.p(x, y, z)
    quad(sp(x0, z0), sp(x1, z0), sp(x1, z1), sp(x0, z1), Color(0xFF1E2230))
    val w = x1 - x0
    val h = z1 - z0
    val scroll = ((wave + 1) * 2).toInt()
    val colors = listOf(tint, Color.White.copy(alpha = 0.7f), Color(0xFFFFD27A), Color(0xFF7FD3FF))
    for (k in 0 until 6) {
        val z = z1 - h * (0.14f + k * 0.15f)
        val seed = k + scroll + index * 3
        val indent = if (seed % 3 == 0) 0.12f else 0.04f
        val len = 0.25f + 0.45f * ((seed * 37) % 10) / 10f
        drawLine(colors[seed % colors.size], sp(x0 + w * indent, z), sp(x0 + w * minOf(0.95f, indent + len), z), strokeWidth = iso.t * 0.03f, cap = StrokeCap.Round)
    }
}

/** At night the screens light the room. */
private fun DrawScope.drawNightGlow(iso: Iso, room: Room, people: List<Worker>) {
    room.seats.forEachIndexed { i, seat ->
        val worker = people.getOrNull(i) ?: return@forEachIndexed
        val glowAt = if (seat.facing == Facing.FRONT) iso.p(seat.cx, seat.cy + 0.2f, 0.95f) else iso.p(seat.cx, seat.cy - 0.55f, 0.85f)
        val c = laneTint(worker.lane).mix(Color(0xFF9FD8FF), 0.6f)
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.22f), Color.Transparent), glowAt, iso.t * 1.1f), iso.t * 1.1f, glowAt)
    }
}
