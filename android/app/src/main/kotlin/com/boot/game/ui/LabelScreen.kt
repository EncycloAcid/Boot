package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Campaign
import com.boot.core.Fit
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.IndiePitch
import com.boot.core.LABEL_SLOTS
import com.boot.core.Research
import com.boot.core.buyLabelCampaign
import com.boot.core.labelCampaignBlocker
import com.boot.core.labelWeekly
import com.boot.core.passPitch
import com.boot.core.remasterConcept
import com.boot.core.remasterable
import com.boot.core.signBlocker
import com.boot.core.signPitch
import com.boot.game.GameStore

/** Your label: games you're funding, pitches waiting for an answer, and what you've put out. */
@Composable
fun LabelSection(store: GameStore, state: GameState, g: GamesStudio) {
    if (Research.PUBLISHING !in state.researched) return
    val colors = gameColors
    var looking by remember { mutableStateOf<IndiePitch?>(null) }

    if (g.signed.isNotEmpty()) {
        GroupedSection(
            "Your label · ${g.signed.size} of $LABEL_SLOTS",
            footer = "You've paid for these. Their developers finish them and you release them. Marketing builds hype for launch.",
        ) {
            g.signed.forEachIndexed { i, s ->
                if (i > 0) RowDivider()
                CardBlock {
                    Row(verticalAlignment = Alignment.Top) {
                        GameCover(s.pitch.concept, 48.dp, studio = s.pitch.developer)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Hint("by ${s.pitch.developer} · ${s.pitch.size.title} ${s.pitch.topic.title.lowercase()} ${s.pitch.genre.lower}")
                        }
                    }
                    Meter("Development", s.completion, caption = if (s.weeksLeft <= 1) "out next week" else "${Format.weeks(s.weeksLeft)} to go", wavy = store.playing)
                    if (Research.MARKETING in state.researched) {
                        Meter("Hype", s.hype / 100, colors.warn, "${s.hype.toInt()}/100")
                        val open = Campaign.entries.filter { state.labelCampaignBlocker(s.pitch.id, it) == null }
                        MenuButton("Market it", open, { "${it.title} · ${Format.money(it.cost)}" }, enabled = open.isNotEmpty()) { c ->
                            store.perform { buyLabelCampaign(s.pitch.id, c) }
                        }
                    }
                }
            }
        }
    }

    GroupedSection(
        "Pitches",
        footer = "Developers pitch you their games. You can't see how good a team really is: a combo you know works is the best sign, " +
            "and the demo and their last game are hints. Pitches go elsewhere after six weeks.",
    ) {
        if (g.pitches.isEmpty()) CardBlock { Hint("No pitches right now. Developers send them every few weeks.") }
        g.pitches.forEachIndexed { i, p ->
            if (i > 0) RowDivider(78.dp)
            val fit = g.knownFit(p.topic, p.genre)
            ListRow(
                p.title,
                subtitle = "${p.developer} · ${p.size.title} ${p.topic.title.lowercase()} ${p.genre.lower}\n" +
                    "${p.demoTitle} · " + (p.lastScore?.let { "last game $it" } ?: "first game") + " · ${Format.weeks(p.weeks)}",
                leading = {
                    Box {
                        GameCover(p.concept, 44.dp, studio = p.developer)
                        if (fit != null) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Box(Modifier.size(8.dp).background(fitColor(fit, colors), CircleShape)) }
                        }
                    }
                },
                value = Format.money(p.funding),
                valueDetail = "for ${Format.percent(p.labelShare)}",
                chevron = true,
                onClick = { looking = p },
            )
        }
    }

    if (g.published.isNotEmpty()) {
        GroupedSection(
            "Published · ${g.labelWeekly.perWeek()}",
            footer = "${Format.money(g.published.sumOf { it.lifetimeSales })} earned so far, against ${Format.money(g.published.sumOf { it.devCost })} put in.",
        ) {
            g.published.take(10).forEachIndexed { i, r ->
                if (i > 0) RowDivider(78.dp)
                ListRow(
                    r.title,
                    subtitle = "by ${r.developer} · ${r.verdict}" + if (r.chartRank > 0) " · #${r.chartRank} this week" else "",
                    leading = { CoverWithScore(r.concept, r.score) },
                    value = if (r.onSale) (r.weeklySales * r.netShare).perWeek() else Format.money(r.lifetimeSales),
                    valueDetail = if (r.onSale) "on sale" else "lifetime",
                    chevron = true,
                    onClick = { store.openRelease = r.id },
                )
            }
        }
    }

    looking?.let { p ->
        val fit = g.knownFit(p.topic, p.genre)
        val blocker = state.signBlocker(p.id)
        val read = when (fit) {
            null -> "You haven't made ${p.topic.title.lowercase()} ${p.genre.lower} games, so you don't know how that combo plays."
            Fit.GREAT, Fit.GOOD -> "You know ${p.topic.title} + ${p.genre.title} is a ${fit.title.lowercase()} combination."
            else -> "You know ${p.topic.title} + ${p.genre.title} is ${Format.a(fit.title.lowercase())} combination. Be careful."
        }
        PitchDialog(
            p, read, blocker,
            onSign = { store.perform { signPitch(p.id) } },
            onPass = { store.perform { passPitch(p.id) } },
            onDismiss = { looking = null },
        )
    }
}

@Composable
private fun PitchDialog(p: IndiePitch, read: String, blocker: String?, onSign: () -> Unit, onPass: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { GameCover(p.concept, 72.dp, studio = p.developer) },
        title = { Text(p.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${p.developer} is making ${Format.a(p.size.lower)} ${p.topic.title.lowercase()} ${p.genre.lower} game " +
                        "for ${p.platforms.joinToString { it.title }}. They need ${Format.money(p.funding)} to finish it " +
                        "in about ${Format.weeks(p.weeks)}, and you'd keep ${Format.percent(p.labelShare)} of what it earns.",
                )
                Text(
                    "${p.demoTitle}. " + (p.lastScore?.let { "Their last game scored $it." } ?: "It's their first game."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(read, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (blocker != null) Text(blocker, color = gameColors.bad)
            }
        },
        confirmButton = {
            PlainButton("Sign", {
                onDismiss()
                onSign()
            }, enabled = blocker == null)
        },
        dismissButton = {
            Row {
                PlainButton("Pass", {
                    onDismiss()
                    onPass()
                }, color = gameColors.bad)
                PlainButton("Not now", onDismiss)
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    )
}

/** Old hits that are ready for a remaster. */
@Composable
fun RemastersSection(store: GameStore, state: GameState, g: GamesStudio) {
    if (Research.REMASTERS !in state.researched) return
    val ready = state.remasterable.take(4)
    if (ready.isEmpty()) return
    GroupedSection("Ready to remaster", footer = "Hits three or more years old, rebuilt for today's platforms at half the work. The better the original, the more players come back.") {
        ready.forEachIndexed { i, r ->
            if (i > 0) RowDivider(78.dp)
            ListRow(
                r.title,
                subtitle = "${r.verdict} · ${com.boot.core.GameClock.label(r.week)} · sold ${Format.count(r.unitsSold)}",
                leading = { CoverWithScore(r.concept, r.score) },
                trailing = {
                    TonalButton("Remaster", { store.designGame(state.remasterConcept(r.id)) }, enabled = !g.busy)
                },
            )
        }
    }
}
