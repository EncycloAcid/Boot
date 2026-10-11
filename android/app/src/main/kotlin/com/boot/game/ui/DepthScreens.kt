package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.AppPricing
import com.boot.core.ContentPolicy
import com.boot.core.CounterMove
import com.boot.core.ExperimentKind
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.RivalStance
import com.boot.core.answer
import com.boot.core.bookKeynote
import com.boot.core.counter
import com.boot.core.counterBlocker
import com.boot.core.counterCost
import com.boot.core.dropRetainer
import com.boot.core.experimentBlocker
import com.boot.core.experimentCost
import com.boot.core.keynoteCost
import com.boot.core.pressuresOn
import com.boot.core.reserveCompute
import com.boot.core.reservedRate
import com.boot.core.setAppPricing
import com.boot.core.setContentPolicy
import com.boot.core.setCreatorFund
import com.boot.core.setDevicePrice
import com.boot.core.setPremium
import com.boot.core.stance
import com.boot.core.startExperiment
import com.boot.game.GameStore

/*
 * The 3.0 screens: the inbox on the office, a rivals card on every business, and the new levers
 * for the agency, the app, the platform, the devices and the lab.
 */

// The inbox

/** Decisions that wait for you without stopping the clock. */
@Composable
fun InboxSection(store: GameStore, state: GameState) {
    if (state.inbox.isEmpty()) return
    val colors = gameColors
    GroupedSection(
        "Inbox · ${state.inbox.size}",
        footer = "These don't stop the clock. Leave one too long and it's as good as a no.",
    ) {
        state.inbox.forEachIndexed { i, item ->
            if (i > 0) RowDivider()
            val left = item.expiresWeek - state.week
            CardBlock(spacing = 8.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Pill(if (left <= 0) "Last week" else "${Format.weeks(left)} left", if (left <= 1) colors.warn else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Hint(item.body)
                item.choices.forEachIndexed { c, choice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(choice.label, style = MaterialTheme.typography.bodyMedium)
                            Hint(choice.detail)
                        }
                        Spacer(Modifier.width(8.dp))
                        if (c == 0) {
                            TonalButton("Choose", { store.perform { answer(item.id, c) } })
                        } else {
                            PlainButton("Choose", { store.perform { answer(item.id, c) } })
                        }
                    }
                }
            }
        }
    }
}

// Rivals

/** The lane's main rival: how they feel about you, their strength, what's squeezing you, and your moves. */
@Composable
fun RivalSection(store: GameStore, state: GameState, lane: Lane) {
    val r = state.world.rival(lane) ?: return
    val colors = gameColors
    val stanceColor = when (r.stance) {
        RivalStance.CALM -> colors.good
        RivalStance.WARY -> colors.warn
        RivalStance.HOSTILE -> colors.bad
    }
    val pressures = state.pressuresOn(lane)
    val quiet = r.quietUntil > state.week
    GroupedSection(
        "Your rival",
        footer = if (quiet) "${r.name} pulled back after you beat them. They'll be back ${GameClock.label(r.quietUntil)}." else
            "The more of their market you take, the angrier they get. Your moves hit them, and make them angrier still.",
    ) {
        CardBlock(spacing = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium)
                    Hint(if (quiet) "Out of the market for now" else r.stance.detail)
                }
                Pill(if (quiet) "Beaten" else r.stance.title, if (quiet) colors.good else stanceColor)
            }
            Meter("Their strength", r.strength, colors.lane(lane))
            Meter("How angry they are", r.heat, stanceColor)
            r.lastMove?.let { Hint("Last move: $it") }
        }
        if (pressures.isNotEmpty()) {
            RowDivider()
            pressures.forEach { p ->
                ListRow(
                    p.title,
                    subtitle = "${Format.weeks(p.weeksLeft)} left",
                    value = (if (p.demand >= 1) "+" else "−") + Format.percent(kotlin.math.abs(p.demand - 1)) + " demand",
                    valueColor = if (p.demand >= 1) colors.good else colors.bad,
                )
            }
        }
        if (!quiet) {
            CounterMove.entries.forEach { m ->
                RowDivider()
                val blocker = state.counterBlocker(r, m)
                val cost = state.counterCost(r, m)
                ActionRow(
                    title = m.title,
                    detail = blocker ?: (if (cost > 0) "${Format.money(cost)}. " else "") + m.detail,
                    action = "Go",
                    enabled = blocker == null,
                ) { store.perform { counter(r.name, m) } }
            }
        }
    }
}

// The new levers, per business.

fun LazyListScope.depthItems(store: GameStore, state: GameState, lane: Lane) {
    when (lane) {
        Lane.WEBSITES -> state.web?.let { w ->
            item {
                GroupedSection(
                    "Retainers",
                    footer = "Happy clients sometimes offer a monthly retainer: steady money for a steady slice of the team's week. " +
                        "Fall too far behind and they leave.",
                ) {
                    if (w.retainers.isEmpty()) CardBlock { Hint("No retainers yet. Finish a project that a client loves.") }
                    w.retainers.forEachIndexed { i, r ->
                        if (i > 0) RowDivider()
                        ListRow(r.client, subtitle = "Takes ${String.format(java.util.Locale.US, "%.1f", r.load)} points a week", value = r.weekly.perWeek()) {
                            Spacer(Modifier.width(8.dp))
                            PlainButton("End", { store.perform { dropRetainer(r.client) } })
                        }
                    }
                }
            }
        }
        Lane.APPS -> state.apps?.let { a ->
            item {
                GroupedSection("Price", footer = a.pricing.detail) {
                    CardBlock { Segmented(AppPricing.entries, a.pricing, { it.title }) { p -> store.perform { setAppPricing(p) } } }
                }
            }
            if (a.isLive) {
                item {
                    GroupedSection(
                        "Experiments",
                        footer = "Test a change on some of your users for four weeks, then roll it out or back. Results can surprise you.",
                    ) {
                        val running = a.experiment
                        if (running != null) {
                            CardBlock {
                                Meter("Testing ${running.kind.title.lowercase()}", 1 - running.weeksLeft / 4.0, wavy = true, caption = "${Format.weeks(running.weeksLeft)} left")
                            }
                        } else {
                            val untried = ExperimentKind.entries.filter { it !in a.experimentsRun }
                            if (untried.isEmpty()) CardBlock { Hint("You've tried every experiment.") }
                            untried.forEachIndexed { i, k ->
                                if (i > 0) RowDivider()
                                val blocker = state.experimentBlocker(k)
                                ActionRow(k.title, blocker ?: "${Format.money(state.experimentCost)} · ${k.detail}", "Run", enabled = blocker == null) {
                                    store.perform { startExperiment(k) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Lane.SOCIAL -> state.social?.let { s ->
            item {
                GroupedSection("Growth and rules", footer = s.policy.detail) {
                    CardBlock(spacing = 8.dp) {
                        Text("Creator fund" + if (s.creatorFund > 0) " · ${s.creatorFundCost.perWeek()}" else "", style = MaterialTheme.typography.bodyMedium)
                        Segmented(listOf(0, 1, 2), s.creatorFund, { listOf("Off", "Small", "Big")[it] }) { v -> store.perform { setCreatorFund(v) } }
                        Spacer(Modifier.width(4.dp))
                        Text("House rules", style = MaterialTheme.typography.bodyMedium)
                        Segmented(ContentPolicy.entries, s.policy, { it.title }) { p -> store.perform { setContentPolicy(p) } }
                    }
                    RowDivider()
                    ToggleRow("Premium tier", s.premium, "A paid tier with a badge and no ads. Earns from your most loyal users; bots hate paying.") { on ->
                        store.perform { setPremium(on) }
                    }
                }
            }
        }
        Lane.HARDWARE -> state.hardware?.let { h ->
            if (h.onMarket != null) {
                item {
                    GroupedSection("Price", footer = "A discount sells a lot more units; a markup earns more on each one you sell.") {
                        CardBlock {
                            val shifts = listOf(-0.2, -0.1, 0.0, 0.1, 0.2)
                            val now = shifts.minByOrNull { kotlin.math.abs(it - h.priceShift) } ?: 0.0
                            Segmented(shifts, now, { if (it == 0.0) "List" else (if (it > 0) "+" else "−") + Format.percent(kotlin.math.abs(it)) }) { v ->
                                store.perform { setDevicePrice(v) }
                            }
                        }
                    }
                }
            }
            if (h.design != null) {
                item {
                    GroupedSection("Launch event", footer = "A launch event builds hype for the next device. Book it before you launch.") {
                        if (h.keynote > 0) {
                            ListRow(if (h.keynote == 2) "Keynote booked" else "Livestream booked", subtitle = "For the ${h.design!!.name} launch")
                        } else {
                            ActionRow("Livestream", "${Format.money(keynoteCost(1))}. A good crowd online.", "Book", enabled = state.cash >= keynoteCost(1)) {
                                store.perform { bookKeynote(1) }
                            }
                            RowDivider()
                            ActionRow("Keynote", "${Format.money(keynoteCost(2))}. A stage, a countdown, the world watching.", "Book", enabled = state.cash >= keynoteCost(2)) {
                                store.perform { bookKeynote(2) }
                            }
                        }
                    }
                }
            }
        }
        Lane.AI -> state.ai?.let { lab ->
            item {
                GroupedSection(
                    "Releases and compute",
                    footer = "Reserved GPUs cost 40% less than renting, for a year, whether you train or not.",
                ) {
                    lab.pendingModel?.let { p ->
                        ListRow(
                            p.codename,
                            subtitle = if (p.releaseWeek >= 0) "In safety testing until ${GameClock.label(p.releaseWeek)}" else "Trained, waiting for your call",
                        )
                        RowDivider()
                    }
                    if (lab.openWeights) {
                        ListRow("Open weights", subtitle = "Developers build on your model; the API earns less.")
                        RowDivider()
                    }
                    if (lab.reservedUnits > 0 && state.week < lab.reservedUntil) {
                        ListRow(
                            "${lab.reservedUnits} GPU units reserved", subtitle = "Until ${GameClock.label(lab.reservedUntil)}",
                            value = (lab.reservedUnits * lab.reservedRate).perWeek(),
                        )
                    } else {
                        listOf(4, 16).forEachIndexed { i, units ->
                            if (i > 0) RowDivider()
                            ActionRow(
                                "Reserve $units units", "${(state.reservedRate * units).perWeek()} for a year", "Reserve",
                                enabled = state.cash >= state.reservedRate * units * 4,
                            ) { store.perform { reserveCompute(units) } }
                        }
                    }
                }
            }
        }
        Lane.GAMES -> {}
    }
}
