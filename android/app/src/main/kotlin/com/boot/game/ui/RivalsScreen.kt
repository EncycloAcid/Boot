package com.boot.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.boot.core.CounterMove
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Rival
import com.boot.core.RivalStance
import com.boot.core.buyStrugglingRival
import com.boot.core.calendarYear
import com.boot.core.counter
import com.boot.core.counterBlocker
import com.boot.core.counterCost
import com.boot.core.defaultCoDevSplit
import com.boot.core.CoDevKind
import com.boot.core.gameRivals
import com.boot.core.proposeCoDev
import com.boot.core.proposeCoDevBlocker
import com.boot.core.stance
import com.boot.core.strugglingRivalBlocker
import com.boot.core.strugglingRivalPrice
import com.boot.core.studioFranchises
import com.boot.core.studioName
import com.boot.core.yourRivals
import com.boot.game.GameStore

/*
 * The Rivals screen: each real-world studio's story, how angry they are, counter-moves, and a
 * late-game buyout when they're struggling.
 */

/** Company / studio tab: every rival you face, games roster first. */
fun LazyListScope.rivalsItems(store: GameStore, state: GameState) {
    val games = state.gameRivals
    val others = state.yourRivals.filter { it.lane != Lane.GAMES || !it.roster }
    item {
        ScreenHeader(
            "Rivals",
            if (games.isNotEmpty()) "Real studios of the era — logos, personalities, and buyouts."
            else "Who's watching you in each market.",
        )
    }
    if (games.isNotEmpty()) {
        item {
            GroupedSection(
                "Games industry",
                Modifier.testTag("rivals-games"),
                footer = "Studios appear only in the decades they were really active. Closed houses fade unless they survive this run or you buy them. Late game, a struggling rival can be absorbed cheaply.",
            ) {
                games.forEachIndexed { i, r ->
                    if (i > 0) RowDivider(68.dp)
                    RivalCard(store, state, r, showBuyout = true)
                }
            }
        }
    }
    if (others.isNotEmpty()) {
        item {
            GroupedSection("Other markets", Modifier.testTag("rivals-other")) {
                others.forEachIndexed { i, r ->
                    if (i > 0) RowDivider(68.dp)
                    RivalCard(store, state, r, showBuyout = false)
                }
            }
        }
    }
    if (games.isEmpty() && others.isEmpty()) {
        item { CardBlock { Hint("No rivals yet. Open a lane and they'll show up.") } }
    }
}

@Composable
private fun RivalCard(store: GameStore, state: GameState, r: Rival, showBuyout: Boolean) {
    val colors = gameColors
    val quiet = r.quietUntil > state.week
    val closed = r.closedYear
    val faded = closed != null && state.calendarYear >= closed && !r.survived && quiet
    val stanceColor = when {
        quiet -> colors.good
        r.stance == RivalStance.CALM -> colors.good
        r.stance == RivalStance.WARY -> colors.warn
        else -> colors.bad
    }
    val display = if (r.lane == Lane.GAMES) state.studioName(r.name) else r.name
    val series = if (r.roster) studioFranchises(r.name).take(3) else emptyList()
    val genres = r.genres.joinToString { it.title }.ifEmpty { null }
    Column(Modifier.testTag("rival-${r.name}")) {
    CardBlock(spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompanyLogo(r.name, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(display, style = MaterialTheme.typography.titleMedium)
                Hint(
                    buildString {
                        if (r.founded > 0) append("Est. ${r.founded}")
                        r.closedYear?.let { append(" · closed $it") }
                        if (r.survived) append(" · survived")
                        if (faded) append(" · faded")
                        if (genres != null) append(" · $genres")
                    },
                )
            }
            Pill(
                when {
                    faded -> "Faded"
                    quiet -> "Beaten"
                    else -> r.stance.title
                },
                stanceColor,
            )
        }
        if (r.blurb.isNotEmpty()) {
            Text(r.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (series.isNotEmpty()) Hint("Known for ${series.joinToString()}")
        Meter("Strength", r.strength, colors.lane(r.lane))
        Meter("Heat", r.heat, stanceColor)
        Meter("Reputation", r.reputation, colors.warn)
        r.lastMove?.let { Hint("Last move: $it") }
        if (!quiet && r.lane in state.lanes) {
            CounterMove.entries.forEach { m ->
                RowDivider()
                val blocker = state.counterBlocker(r, m)
                val cost = state.counterCost(r, m)
                ActionRow(
                    title = m.title,
                    detail = blocker ?: ((if (cost > 0) "${Format.money(cost)}. " else "") + m.detail),
                    action = "Go",
                    enabled = blocker == null,
                ) { store.perform { counter(r.name, m) } }
            }
        }
        if (r.lane == Lane.GAMES && !quiet && !faded) {
            RowDivider()
            val team = listOf(1, 2).firstOrNull { t ->
                state.proposeCoDevBlocker(t, r.name) == null
            }
            val split = defaultCoDevSplit(CoDevKind.RIVAL)
            val block = when {
                team == null -> state.games?.project?.let { state.proposeCoDevBlocker(1, r.name) }
                    ?: "Start a large or AAA project to propose co-dev."
                else -> null
            }
            ActionRow(
                title = "Propose co-dev",
                detail = block
                    ?: "Split cost, workload, credit and royalties on your mega-project. Shared glory — and shared blame.",
                action = "Propose",
                enabled = block == null && team != null,
            ) {
                val t = team ?: return@ActionRow
                store.perform {
                    proposeCoDev(t, r.name, split.cost, split.work, split.credit, split.royalty)
                }
            }
        }
        if (showBuyout && !r.absorbed) {
            RowDivider()
            val price = state.strugglingRivalPrice(r)
            val blocker = state.strugglingRivalBlocker(r)
            ActionRow(
                title = "Buy out ${display}",
                detail = blocker
                    ?: "Absorb the studio and its franchises for ${Format.money(price)}. Only when they're struggling.",
                action = if (blocker == null) Format.money(price) else "Locked",
                enabled = blocker == null,
            ) { store.perform { buyStrugglingRival(r.name) } }
            if (blocker == null) {
                Hint("Or open a full deal to negotiate premium, stakes and earn-outs.")
                PlainButton("Open deal sheet", { store.dealStudio = r.name })
            }
        }
    }
    }
}
