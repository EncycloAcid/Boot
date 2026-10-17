package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.Franchise
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.ScreenDeal
import com.boot.core.ScreenOffer
import com.boot.core.adaptableSeries
import com.boot.core.adaptationsOf
import com.boot.core.pitchScreen
import com.boot.core.screenOutlook
import com.boot.core.screenPitchBlocker
import com.boot.core.screenPitchOdds
import com.boot.game.GameStore

/*
 * Film and TV: what's in production, how the finished ones did, and your series that studios
 * would want.
 */

/** Film and TV deals for your series. */
@Composable
fun ScreenSection(store: GameStore, state: GameState, g: GamesStudio) {
    val series = state.adaptableSeries()
    val colors = gameColors
    GroupedSection(
        "Film and TV",
        Modifier.testTag("screen-deals"),
        footer = "Studios call about series that average 7.5 or better. License it for money up front, or co-produce for a say in it and a share of the takings. " +
            "A good adaptation brings fans and sells the games; a bad one hurts the series.",
    ) {
        g.adaptations.sortedByDescending { it.startWeek }.take(6).forEachIndexed { i, a ->
            if (i > 0) RowDivider(78.dp)
            val cover = g.release(a.rootId)?.let { root -> g.series(root).lastOrNull() } ?: g.release(a.rootId)
            ListRow(
                "${a.series} · ${a.kind.title}",
                subtitle = "${a.studio} · ${a.deal.title.lowercase()} · " + when (val c = a.critics) {
                    null -> "out in ${Format.weeks((a.premiereWeek - state.week).coerceAtLeast(1))}"
                    else -> "$c% with critics" + (if (a.takings > 0) " · made you ${Format.money(a.takings)}" else "")
                },
                leading = cover?.let { r -> { GameCover(r.concept, 44.dp, studio = state.companyName) } },
                value = a.verdict,
                valueColor = when {
                    !a.out -> MaterialTheme.colorScheme.primary
                    a.hit -> colors.good
                    a.flop -> colors.bad
                    else -> colors.warn
                },
                valueDetail = if (a.deal == ScreenDeal.LICENSE) "paid ${Format.money(a.money)}" else "put in ${Format.money(a.money)}",
            )
        }
        val idle = series.filter { f -> state.adaptationsOf(f.root.id).none { !it.out } }
        if (g.adaptations.isNotEmpty() && idle.isNotEmpty()) RowDivider(78.dp)
        idle.take(4).forEachIndexed { i, f ->
            if (i > 0) RowDivider(78.dp)
            val blocker = state.screenPitchBlocker(f.root.id)
            ListRow(
                f.name,
                subtitle = blocker ?: "${f.entries.size} game${if (f.entries.size > 1) "s" else ""} · ${Format.percent(state.screenPitchOdds(f.root.id))} chance a studio bites",
                leading = { CoverWithScore(f.latest.concept, f.averageScore) },
                trailing = {
                    PlainButton("Shop rights", { store.perform { pitchScreen(f.root.id) } }, enabled = blocker == null)
                },
            )
        }
        if (g.adaptations.isEmpty() && idle.isEmpty()) CardBlock { Hint("Nothing to adapt yet.") }
    }
}

/** The art on a studio's offer: who's asking, and the series they want. */
@Composable
fun ScreenOfferArt(state: GameState, kind: ScreenOffer): Boolean {
    val g = state.games ?: return false
    val root = g.release(kind.rootId) ?: return false
    val f = Franchise(root, g.series(root))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CompanyLogo(kind.studio, size = 52.dp)
        GameCover(f.latest.concept, 52.dp, studio = state.companyName)
        Column(Modifier.padding(start = 4.dp)) {
            Text("${kind.studio} × ${f.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Hint(
                "${kind.kind.title} · ${f.entries.size} game${if (f.entries.size > 1) "s" else ""} averaging ${String.format(java.util.Locale.US, "%.1f", f.averageScore)} · " +
                    "critics ~${screenOutlook(f, ScreenDeal.LICENSE)}% licensed, ~${screenOutlook(f, ScreenDeal.COPRODUCE)}% co-produced",
            )
        }
    }
    return true
}
