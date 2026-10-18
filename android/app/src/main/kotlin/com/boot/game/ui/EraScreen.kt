package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.EraPhase
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.MODERN_YEAR
import com.boot.core.historyAhead
import com.boot.core.historyComing
import com.boot.core.historySoFar
import androidx.compose.ui.platform.testTag
import com.boot.core.inSentence
import com.boot.core.challengeProgress
import com.boot.core.research
import com.boot.core.laneOpen
import com.boot.core.arrives
import kotlin.math.abs

/** The era: what it is, where it is in its life, and what people say is next. */
@Composable
fun EraCard(state: GameState) {
    val w = state.world
    val era = w.era
    val scheme = MaterialTheme.colorScheme
    val colors = gameColors
    val weeks = state.week - w.eraStartWeek
    Surface(shape = RoundedCornerShape(24.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
        Box {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(era.emoji, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("The era", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        Text(era.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    }
                }
                Text(era.blurb, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                val only = era.research.filter { it !in state.researched }
                if (only.isNotEmpty()) {
                    Text(
                        "Research only this era: " + only.joinToString { it.inSentence } + ".",
                        style = MaterialTheme.typography.labelLarge, color = colors.milestone,
                    )
                }
                if (era != com.boot.core.Era.BOOTSTRAP_YEARS) {
                    // Dawn, peak, fading: where the era is in its life.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EraPhase.entries.forEach { p ->
                            val on = p == w.phase
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .background(if (on) scheme.primary else colors.fill, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                FitText(
                                    p.title, MaterialTheme.typography.labelMedium,
                                    color = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                Text(
                    "${Format.weeks(weeks)} so far · felt at ${Format.percent(w.eraIntensity)} strength",
                    style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                )
                w.challenge?.let { c ->
                    val progress = state.challengeProgress(c)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.fill, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("This era's challenge", style = MaterialTheme.typography.labelMedium, color = colors.milestone, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (c.done) Pill("Done", colors.good)
                            }
                            Text(c.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(c.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            Meter("Progress", progress, if (c.done) colors.good else colors.milestone)
                            Text(
                                "Reward: ${Format.money(c.reward)}, 25 research points and reputation.",
                                style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                w.nextEra?.let { next ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.fill, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("On the horizon", style = MaterialTheme.typography.labelMedium, color = colors.milestone, fontWeight = FontWeight.SemiBold)
                            Text(next.rumor, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/** What the era does to each business, and to costs and investors. */
@Composable
fun EraEffects(state: GameState) {
    val w = state.world
    val colors = gameColors
    fun pct(x: Double) = if (abs(x - 1) < 0.005) "—" else (if (x > 1) "+" else "") + Format.percent(x - 1)
    @Composable
    fun row(label: String, x: Double, mine: Boolean = false, goodWhenUp: Boolean = true, note: String? = null) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (mine) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
            val better = if (goodWhenUp) x > 1.005 else x < 0.995
            val worse = if (goodWhenUp) x < 0.995 else x > 1.005
            Text(
                note ?: pct(x), style = MaterialTheme.typography.bodyLarge,
                color = when {
                    note != null -> MaterialTheme.colorScheme.onSurfaceVariant
                    better -> colors.good
                    worse -> colors.warn
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    GroupedSection("Demand this era", footer = "At today's strength. Eras build up at dawn and ease off as they fade.") {
        Lane.entries.forEachIndexed { i, lane ->
            if (i > 0) RowDivider()
            row("${lane.emoji}  ${lane.title}", w.demand(lane), mine = lane in state.lanes, note = if (state.laneOpen(lane)) null else "From ${lane.arrives}")
        }
    }
    GroupedSection("Costs and money") {
        row("Salaries", w.salaries, goodWhenUp = false)
        RowDivider()
        row("GPU prices", w.gpuPrice, goodWhenUp = false)
        RowDivider()
        row("Hardware parts", w.hardwareCost, goodWhenUp = false)
        RowDivider()
        row("Investor appetite", w.investorAppetite)
    }
}

/** Every era so far, oldest first. */
@Composable
fun EraTimeline(state: GameState) {
    val w = state.world
    GroupedSection("Timeline") {
        w.history.forEachIndexed { i, span ->
            if (i > 0) RowDivider(56.dp)
            val dates = "${GameClock.label(span.startWeek)} to ${GameClock.label(span.endWeek)}"
            ListRow(
                span.era.title,
                // Eras you lived through get a short review.
                subtitle = if (span.lived) "$dates\nEarned ${Format.money(span.revenue)} · worth ${Format.money(span.valueStart)} → ${Format.money(span.valueEnd)}" else dates,
                leading = { Text(span.era.emoji, style = MaterialTheme.typography.titleLarge) },
            ) {
                span.challengeWon?.let { won ->
                    Spacer(Modifier.width(8.dp))
                    Pill(if (won) "Challenge met" else "Missed", if (won) gameColors.good else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (w.history.isNotEmpty()) RowDivider(56.dp)
        ListRow(
            w.era.title,
            subtitle = "Since ${GameClock.label(w.eraStartWeek)} · now",
            leading = { Text(w.era.emoji, style = MaterialTheme.typography.titleLarge) },
        )
    }
    // A run set in the past knows what history has in store.
    val ahead = state.historyAhead
    if (ahead.isNotEmpty()) {
        GroupedSection("Still to come", footer = "History, as it happened. You reach the present in $MODERN_YEAR.") {
            ahead.forEachIndexed { i, (year, era) ->
                if (i > 0) RowDivider(56.dp)
                ListRow(
                    era.title, subtitle = "From $year",
                    leading = { Text(era.emoji, style = MaterialTheme.typography.titleLarge) },
                )
            }
        }
    }
}

/** History as it happened: what's been and what's coming. Only in a run that started in the past. */
@Composable
fun HistorySection(state: GameState) {
    val past = state.historySoFar().take(4)
    val coming = state.historyComing().take(4)
    if (past.isEmpty() && coming.isEmpty()) return
    val colors = gameColors
    GroupedSection(
        "History",
        Modifier.testTag("history"),
        footer = "You know what's coming. When it happens the market feels it: demand shifts for a while, gamers want something new, and now and then you'll have a call to make.",
    ) {
        past.forEachIndexed { i, (week, e) ->
            if (i > 0) RowDivider()
            ListRow(e.headline, subtitle = "${GameClock.label(week)} · ${e.body}")
        }
        if (past.isNotEmpty() && coming.isNotEmpty()) RowDivider()
        coming.forEachIndexed { i, (week, e) ->
            if (i > 0) RowDivider()
            val soon = week - state.week <= 52
            ListRow(
                e.headline,
                subtitle = if (soon) "${GameClock.label(week)} · coming up" else "${e.year}",
                trailing = { if (e.choice) Pill("A choice", colors.milestone) else if (soon) Pill("Soon", MaterialTheme.colorScheme.primary) },
            )
        }
    }
}

/** The decades you've lived through, each with its game of the decade. */
@Composable
fun DecadesSection(state: GameState) {
    val decades = state.world.decades
    if (decades.isEmpty()) return
    val colors = gameColors
    GroupedSection("Decades", Modifier.testTag("decades"), footer = "As each decade ends the papers pick its best game. Make it, and the decade is yours.") {
        decades.asReversed().forEachIndexed { i, d ->
            if (i > 0) RowDivider()
            val top = if (d.crowned) d.yourBest else d.topGame
            ListRow(
                d.title,
                subtitle = listOfNotNull(
                    top?.let { "Game of the decade: $it" + if (d.crowned) "" else " (${d.topStudio})" },
                    d.yourBest?.takeIf { !d.crowned }?.let { "Your best: $it (${String.format(java.util.Locale.US, "%.1f", d.yourBestScore)})" },
                    "Earned ${Format.money(d.revenue)} · worth ${Format.money(d.valueStart)} → ${Format.money(d.valueEnd)}",
                ).joinToString("\n"),
                trailing = { if (d.crowned) Pill("Yours", colors.good) },
            )
        }
    }
}
