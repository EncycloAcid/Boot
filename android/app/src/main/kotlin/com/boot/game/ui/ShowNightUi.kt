package com.boot.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.AwardNight
import com.boot.core.Burnout
import com.boot.core.Category
import com.boot.core.EquityAsk
import com.boot.core.EventKind
import com.boot.core.FanCampaign
import com.boot.core.Format
import com.boot.core.GameConcept
import com.boot.core.GameState
import com.boot.core.Nominee
import com.boot.core.PendingEvent
import com.boot.core.Platform
import com.boot.core.PlatformInvite
import com.boot.core.PremiereSlot
import com.boot.core.PresenterInvite
import com.boot.core.ShowReport
import com.boot.core.ShowResults
import com.boot.core.SideProject
import com.boot.core.StaffClash
import com.boot.core.StoryBeat
import com.boot.core.StreamerPreview
import com.boot.core.OwnShowcase
import com.boot.core.choose
import com.boot.core.reactionWord
import com.boot.game.GameStore
import com.boot.game.audio.Sfx
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * Show night on screen: the award ceremony, the art at the top of a decision, and the stage report.
 */

private val Gold = Color(0xFFE9C46A)
private val StageDark = Color(0xFF0E1020)
private val StageDeep = Color(0xFF1C2147)

/** The ceremony: each category's nominees, then the envelope, then (if you won) your speech. */
@Composable
fun AwardNightSheet(store: GameStore, state: GameState, e: PendingEvent) {
    val night = (e.kind as AwardNight).ceremony
    val sound = LocalSound.current
    // How many categories have had their winner read out.
    var revealed by remember(e.id) { mutableIntStateOf(0) }
    LaunchedEffect(e.id) {
        while (revealed < night.categories.size) {
            delay(1_600)
            val c = night.categories[revealed]
            sound.play(if (c.won) Sfx.HIT else Sfx.CLICK)
            revealed += 1
        }
    }
    val done = revealed >= night.categories.size
    GameDialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(StageDeep, StageDark)))) {
            Spotlights(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LivePill()
                    Spacer(Modifier.width(12.dp))
                    Text(
                        night.show, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                        color = Gold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (!done) TextButton(onClick = { revealed = night.categories.size }) { Text("Skip", color = Color.White.copy(alpha = 0.8f)) }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    night.categories.forEachIndexed { i, c ->
                        CategoryCard(state, c, opened = i < revealed, live = i == revealed)
                    }
                    if (done) {
                        Text(e.title, style = MaterialTheme.typography.headlineSmallEmphasized, color = Color.White)
                        Text(e.body, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            e.choices.forEachIndexed { i, choice ->
                                ChoiceCard(choice, primary = i == 0, Modifier.weight(1f).fillMaxHeight()) { store.perform { choose(i) } }
                            }
                        }
                    }
                    Spacer(Modifier.navigationBarsPadding().height(12.dp))
                }
            }
        }
    }
}

/** One prize: its nominees in a row, the winner in gold once the envelope is open. */
@Composable
private fun CategoryCard(state: GameState, c: Category, opened: Boolean, live: Boolean) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (opened && c.won) Gold else Color.White.copy(alpha = 0.12f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.prize, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.weight(1f))
            Text(
                when {
                    opened -> if (c.won) "You won!" else "Winner: ${c.nominees[c.winner].title}"
                    live -> "And the award goes to…"
                    else -> "Nominees"
                },
                style = MaterialTheme.typography.labelLarge, fontStyle = if (live && !opened) FontStyle.Italic else FontStyle.Normal,
                color = if (opened && c.won) Gold else Color.White.copy(alpha = 0.7f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            c.nominees.forEachIndexed { i, n ->
                NomineeTile(state, n, winner = opened && i == c.winner, dimmed = opened && i != c.winner, Modifier.weight(1f))
            }
            repeat(5 - c.nominees.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun NomineeTile(state: GameState, n: Nominee, winner: Boolean, dimmed: Boolean, modifier: Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(winner) { if (winner) { scale.snapTo(0.9f); scale.animateTo(1f) } }
    Column(
        modifier
            .alpha(if (dimmed) 0.45f else 1f)
            .semantics { contentDescription = (if (winner) "Winner: " else "Nominee: ") + n.title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(if (winner) 3.dp else 0.dp, if (winner) Gold else Color.Transparent, RoundedCornerShape(10.dp))
                .padding(if (winner) 3.dp else 0.dp),
        ) {
            GameCover(GameConcept(n.title, n.topic, n.genre, platforms = listOf(Platform.PC), size = n.size), 64.dp, studio = n.studio)
        }
        Text(n.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        Text(
            if (n.mine) "You" else n.studio, style = MaterialTheme.typography.labelSmall,
            color = if (n.mine) Gold else Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LivePill() {
    Text(
        "LIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White,
        modifier = Modifier.background(Color(0xFFE5484D), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Three cones of light from the rig, and a glow on the stage floor. */
@Composable
private fun Spotlights(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for ((x, tint) in listOf(0.18f to Color(0xFF9BB4FF), 0.5f to Color(0xFFFFE6A8), 0.82f to Color(0xFFFFA8D8))) {
            val path = Path().apply {
                moveTo(w * x - w * 0.02f, 0f)
                lineTo(w * x + w * 0.02f, 0f)
                lineTo(w * x + w * 0.16f, h)
                lineTo(w * x - w * 0.16f, h)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(tint.copy(alpha = 0.16f), Color.Transparent)))
        }
        drawOval(Brush.radialGradient(listOf(Color(0x33FFE6A8), Color.Transparent), center = Offset(w / 2, h), radius = w * 0.45f), Offset(w * 0.05f, h * 0.8f), Size(w * 0.9f, h * 0.4f))
    }
}

// The art at the top of a decision

/** The people a decision is about. */
private fun peopleIn(state: GameState, kind: EventKind): List<com.boot.core.Employee> {
    val ids = when (kind) {
        is EventKind.Poach -> listOf(kind.employeeId)
        is EventKind.RaiseRequest -> listOf(kind.employeeId)
        is Burnout -> listOf(kind.employeeId)
        is StaffClash -> listOf(kind.a, kind.b)
        is EquityAsk -> listOf(kind.employeeId)
        is SideProject -> listOf(kind.employeeId)
        is StoryBeat -> state.stories.firstOrNull { it.id == kind.storyId }?.people.orEmpty()
        else -> emptyList()
    }
    return ids.mapNotNull { id -> state.staff.firstOrNull { it.id == id } }
}

/** The stage a decision is about, if it's about one. */
private fun stageOf(state: GameState, kind: EventKind): String? = when (kind) {
    EventKind.Convention -> "The trade show"
    is PlatformInvite -> kind.show.replaceFirstChar { it.uppercase() }
    is PremiereSlot -> kind.show
    OwnShowcase -> "The ${state.companyName} Showcase"
    EventKind.GameAwards -> "Game of the Year"
    is ShowResults -> kind.report.show.replaceFirstChar { it.uppercase() }
    is StreamerPreview -> "Live on stream"
    is FanCampaign -> kind.show.replaceFirstChar { it.uppercase() }
    is PresenterInvite -> kind.show
    else -> null
}

/** A header for a decision: the people in it, or the stage it's on. Null if there's nothing to draw. */
@Composable
fun EventArt(state: GameState, kind: EventKind): Boolean {
    val people = peopleIn(state, kind)
    if (people.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            for (p in people) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StaffPortrait(p, 52.dp)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Hint("Level ${p.level} ${p.role.title.lowercase()} · morale ${Format.percent(p.morale)}")
                    }
                }
            }
        }
        return true
    }
    val stage = stageOf(state, kind) ?: return false
    StageBanner(stage, live = kind is ShowResults && kind.report.stream)
    return true
}

/** A lit stage with the show's name across it. */
@Composable
fun StageBanner(name: String, height: Dp = 84.dp, live: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(StageDeep, StageDark))),
        contentAlignment = Alignment.CenterStart,
    ) {
        Spotlights(Modifier.fillMaxSize())
        // A few heads in the front row.
        Canvas(Modifier.fillMaxSize()) {
            val n = 18
            for (i in 0 until n) {
                val x = size.width * (i + 0.5f) / n
                val r = size.height * (0.09f + 0.02f * ((i * 7) % 3))
                drawCircle(Color.Black.copy(alpha = 0.55f), r, Offset(x, size.height - r * 0.4f))
            }
        }
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            if (live) {
                LivePill()
                Spacer(Modifier.width(10.dp))
            }
            Text(name, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// The stage report

/** The show in numbers, how the crowd took it, and what the press and the chat said. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StageFigures(r: ShowReport) {
    val colors = gameColors
    GroupedSection(null) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatTile(if (r.stream) "Watching" else "Visitors", Format.count(r.visitors.toDouble()), Modifier.weight(1f), r.booth?.title?.lowercase())
                if (r.game != null) StatTile("Hype", "+${r.hype.roundToInt()}", Modifier.weight(1f), r.game, colors.milestone)
                if (r.wishlists >= 1) StatTile("Wishlists", "+${Format.count(r.wishlists)}", Modifier.weight(1f), null, MaterialTheme.colorScheme.primary)
                StatTile("New fans", Format.count(r.fans), Modifier.weight(1f), if (r.bestInShow) "Best in Show" else null, colors.good)
            }
            if (r.game != null) CrowdMeter(r.reaction)
        }
    }
    if (r.quotes.isNotEmpty()) {
        GroupedSection("The press") {
            r.quotes.forEachIndexed { i, q ->
                if (i > 0) RowDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CompanyLogo(q.outlet, size = 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("“${q.line}”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f))
                    Text(q.outlet, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (r.chat.isNotEmpty()) {
        GroupedSection(if (r.stream) "Chat" else "The crowd") {
            FlowRow(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (line in r.chat) {
                    Text(
                        line, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.background(colors.fill, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** How the crowd took it, from cold to on fire. */
@Composable
private fun CrowdMeter(reaction: Double) {
    val colors = gameColors
    val tint = when {
        reaction >= 0.8 -> colors.bad
        reaction >= 0.55 -> colors.warn
        reaction >= 0.3 -> colors.milestone
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text("Crowd", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(reactionWord(reaction), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = tint)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.fill)) {
            Box(Modifier.fillMaxWidth(reaction.toFloat().coerceIn(0.03f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(colors.milestone, tint))))
        }
    }
}
