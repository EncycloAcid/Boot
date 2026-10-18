package com.boot.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import com.boot.core.Format
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.PrizePool
import com.boot.core.Research
import com.boot.core.endLeague
import com.boot.core.leagueBlocker
import com.boot.core.leagueCandidates
import com.boot.core.leagueCost
import com.boot.core.leagues
import com.boot.core.setPrizePool
import com.boot.core.startLeague
import com.boot.game.GameStore

/** Your pro leagues, and the games that could have one. */
@Composable
fun EsportsSection(store: GameStore, state: GameState, g: GamesStudio) {
    if (Research.ESPORTS !in state.researched) return
    val colors = gameColors
    var ending by remember { mutableStateOf<GameRelease?>(null) }
    val running = g.leagues
    if (running.isNotEmpty()) {
        val weekly = running.sumOf { it.league!!.lastRevenue - it.league!!.lastCost }
        GroupedSection(
            "Esports · ${weekly.signedPerWeek()}",
            footer = "Sponsors pay for every viewer. Each championship wins over some of the people who own the game but don't watch yet: " +
                PrizePool.entries.joinToString { "${it.title.lowercase()} ${Format.money(it.amount)}" } +
                ". Big prize pools pay off on big games.",
        ) {
            running.forEachIndexed { i, r ->
                if (i > 0) RowDivider()
                val l = r.league!!
                CardBlock {
                    Row(verticalAlignment = Alignment.Top) {
                        CoverWithScore(r.concept, r.score)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Hint(
                                "Final in ${Format.weeks((l.nextFinal - state.week).coerceAtLeast(0))}" +
                                    (l.lastChampion?.let { " · last won by $it" } ?: " · no champion yet"),
                            )
                        }
                    }
                    TileRow {
                        StatTile("Viewers", Format.count(l.viewers), Modifier.weight(1f), "peak ${Format.count(l.peakViewers)}")
                        StatTile(
                            "Profit", (l.lastRevenue - l.lastCost).signedPerWeek(), Modifier.weight(1f), "sponsors ${l.lastRevenue.perWeek()}",
                            if (l.lastRevenue >= l.lastCost) colors.good else colors.bad,
                        )
                        StatTile("Finals", "${l.finals}", Modifier.weight(1f))
                    }
                    Segmented(PrizePool.entries, l.prize, { it.title }) { p -> store.perform { setPrizePool(r.id, p) } }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Hint("Next prize pool: ${Format.money(l.prize.amount)}")
                        Spacer(Modifier.weight(1f))
                        PlainButton("Wind up", { ending = r }, color = colors.bad)
                    }
                }
            }
        }
    }
    val candidates = state.leagueCandidates
    if (candidates.isNotEmpty()) {
        GroupedSection(
            "Start a league",
            footer = "Action and strategy games that scored 7.5 or more, built on an engine with multiplayer, once 100k people play them. " +
                "Watching keeps people buying and playing.",
        ) {
            candidates.take(4).forEachIndexed { i, r ->
                if (i > 0) RowDivider(78.dp)
                val blocker = state.leagueBlocker(r.id)
                ListRow(
                    r.title,
                    subtitle = blocker ?: "${Format.count(maxOf(r.unitsSold, r.live?.players ?: 0.0))} players · costs ${Format.money(r.leagueCost())} to set up",
                    leading = { CoverWithScore(r.concept, r.score) },
                    trailing = { TonalButton("Start", { store.perform { startLeague(r.id) } }, enabled = blocker == null) },
                )
            }
        }
    }
    ending?.let { r ->
        ConfirmDialog(
            title = "Wind up the ${r.league!!.name}?",
            text = "The teams go home and the sponsors with them. You can't start another league for ${r.title}.",
            confirm = "Wind up",
            destructive = true,
            onConfirm = { store.perform { endLeague(r.id) } },
            onDismiss = { ending = null },
        )
    }
}
