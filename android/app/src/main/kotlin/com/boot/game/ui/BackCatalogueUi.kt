package com.boot.game.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.collectorCost
import com.boot.core.boxCost
import com.boot.core.orderEditions
import com.boot.core.collectorEstimate
import com.boot.core.boxEstimate
import com.boot.core.editionsBlocker
import com.boot.core.Research
import com.boot.core.AgeRating
import com.boot.core.Audience
import com.boot.core.Format
import com.boot.core.GameProject
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.Protection
import com.boot.core.RATINGS_YEAR
import com.boot.core.RatingDispute
import com.boot.core.ReviewBomb
import com.boot.core.adultsOnlyPlatforms
import com.boot.core.calendarYear
import com.boot.core.collectable
import com.boot.core.collectableSeries
import com.boot.core.collectionBlocker
import com.boot.core.collectionQuote
import com.boot.core.collectionTitle
import com.boot.core.gameForecast
import com.boot.core.isCollection
import com.boot.core.makeCollection
import com.boot.core.piracyEra
import com.boot.core.piracyLoss
import com.boot.core.popularity
import com.boot.core.projectRating
import com.boot.core.protectionCost
import com.boot.core.protections
import com.boot.core.setProtection
import com.boot.core.setUncut
import com.boot.core.uncutBlocker
import com.boot.game.GameStore
import java.util.Locale

/*
 * The shelf a game lands on: its copy protection and age rating while it's being made, the
 * collections that bring old games back, and the art on rating and review-bomb decisions.
 */

/** How much players mind a kind of protection, in words. */
private fun Protection.mood(): String = when {
    annoys >= 0.8 -> "players hate it"
    annoys >= 0.45 -> "players grumble"
    annoys > 0 -> "barely noticed"
    else -> "nothing to mind"
}

/** Copy protection and age rating, on the card of a game in development. */
@Composable
fun ShelfControls(store: GameStore, state: GameState, p: GameProject, team: Int) {
    val c = p.concept
    val year = state.calendarYear
    Column(Modifier.testTag("shelf-$team"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Copy protection", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            val options = listOf(Protection.NONE) + protections(year).filter { it != Protection.NONE }
            MenuButton(p.protection.title, options, { pr ->
                if (pr == Protection.NONE) "None · free" else "${pr.title} · ${Format.money(state.protectionCost(pr, c.size))} · stops ${Format.percent(pr.stops)}, ${pr.mood()}"
            }) { pr -> if (pr != p.protection) store.perform { setProtection(team, pr) } }
        }
        val forecast = state.gameForecast(team)?.expectedScore ?: 6.0
        val loss = state.piracyLoss(c, p.protection, popularity(forecast, p.hype))
        Hint(
            "${piracyEra(year)} · pirates would take about ${Format.percent(loss)} of its sales" +
                if (p.protection != Protection.NONE) " · ${p.protection.blurb}" else "",
        )
        StoreControls(store, state, p, team)
        EditionsControls(store, state, p, team)
        val rating = state.projectRating(team)
        if (rating != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingBadge(rating)
                Text(
                    "Rated ${rating.title}", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                if (c.audience == Audience.MATURE) {
                    val next = !p.uncut
                    val blocker = state.uncutBlocker(team, next)
                    PlainButton(if (next) "Go uncut" else "Cut to M", { store.perform { setUncut(team, next) } }, enabled = blocker == null)
                }
            }
            if (p.uncut) {
                val keep = adultsOnlyPlatforms(c)
                Hint("Adults Only: only on ${keep.joinToString { it.title }}, and many shops won't stock it.", gameColors.warn)
            } else if (c.audience == Audience.MATURE) {
                Hint("Going uncut makes the darkest topics better and gets people talking. Consoles and phone stores won't carry it.")
            }
        } else if (year < RATINGS_YEAR) {
            Hint("No ratings board yet: anything goes, until $RATINGS_YEAR.")
        }
    }
}

/** A small ratings-board style badge. */
@Composable
fun RatingBadge(rating: AgeRating) {
    val color = when (rating) {
        AgeRating.EVERYONE -> gameColors.good
        AgeRating.TEEN -> MaterialTheme.colorScheme.primary
        AgeRating.MATURE -> gameColors.warn
        AgeRating.ADULTS_ONLY -> gameColors.bad
    }
    Text(
        rating.code, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = color,
        modifier = Modifier
            .border(1.5.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Collections: series with old games to package, the ones being put together, and the ones out. */
@Composable
fun CollectionsSection(store: GameStore, state: GameState, g: GamesStudio) {
    val series = state.collectableSeries()
    val out = g.releases.filter { it.isCollection }
    if (series.isEmpty() && out.isEmpty() && g.collectionJobs.isEmpty()) return
    GroupedSection(
        "Collections",
        Modifier.testTag("collections"),
        footer = "Package a series' older games, two years old or more, at a budget price. Cheap to make; the originals sell less while it's out.",
    ) {
        var rows = 0
        for (job in g.collectionJobs) {
            if (rows++ > 0) RowDivider(78.dp)
            val games = job.ids.mapNotNull { g.release(it) }
            ListRow(
                job.title,
                subtitle = "${games.size} games · out in ${Format.weeks((job.doneWeek - state.week).coerceAtLeast(1))}",
                leading = { GameCover(games.last().concept.copy(title = job.title, collects = games.map { it.title }), 44.dp, studio = state.companyName) },
                value = "In the works",
                valueColor = MaterialTheme.colorScheme.primary,
            )
        }
        for (r in out.take(4)) {
            if (rows++ > 0) RowDivider(78.dp)
            ListRow(
                r.title,
                subtitle = "${r.concept.collects.size} games · out ${Format.weeks((state.week - r.week).coerceAtLeast(0))} ago",
                leading = { CoverWithScore(r.concept, r.score) },
                value = if (r.onSale) "${Format.money(r.weeklySales * r.netShare)}/wk" else Format.money(r.lifetimeSales),
                valueDetail = if (r.onSale) "on sale" else "lifetime",
                onClick = { store.openRelease = r.id },
            )
        }
        for (f in series.filter { s -> g.collectionJobs.none { it.rootId == s.root.id } }.take(4)) {
            val ready = state.collectable(f.root.id)
            val blocker = state.collectionBlocker(f.root.id)
            if (ready.size < 2 && blocker != null && blocker.startsWith("It needs")) continue
            if (rows++ > 0) RowDivider(78.dp)
            val (cost, weeks) = state.collectionQuote(ready.map { it.id })
            ListRow(
                if (ready.size >= 2) collectionTitle(f.name, ready.size) else f.name,
                subtitle = blocker ?: "${ready.joinToString { it.title }} · ${Format.money(cost)} · ${Format.weeks(weeks)}",
                leading = { CoverWithScore(f.latest.concept, f.averageScore) },
                trailing = { PlainButton("Package", { store.perform { makeCollection(f.root.id) } }, enabled = blocker == null) },
            )
        }
        if (rows == 0) CardBlock { Hint("No series has two games old enough yet.") }
    }
}

/** Where a game ended up: its rating, its protection and the pirates, and any review bomb. */
@Composable
fun ShelfSection(r: GameRelease) {
    if (r.isCollection) {
        GroupedSection("In the box") {
            r.concept.collects.forEachIndexed { i, t ->
                if (i > 0) RowDivider()
                ValueRow(t, "")
            }
        }
        return
    }
    if (r.rating == null && r.protection == Protection.NONE && r.pirated < 1 && !r.bombed && r.boxes == 0 && r.collectors == 0) return
    GroupedSection("On the shelf") {
        var first = true
        r.rating?.let {
            ValueRow("Age rating", it.title)
            first = false
        }
        if (!first) RowDivider()
        ValueRow("Copy protection", r.protection.title)
        if (r.boxes > 0) {
            RowDivider()
            ValueRow("Boxes sold", "${Format.count(r.boxesSold.toDouble())} of ${Format.count(r.boxes.toDouble())}")
        }
        if (r.collectors > 0) {
            RowDivider()
            ValueRow("Collector's editions sold", "${Format.count(r.collectorsSold.toDouble())} of ${Format.count(r.collectors.toDouble())}")
        }
        if (r.pirated >= 1) {
            RowDivider()
            ValueRow("Pirated copies", "~${Format.count(r.pirated)}", gameColors.bad)
        }
        if (r.bombed) {
            RowDivider()
            ValueRow("Review bombed", "Players: ${String.format(Locale.US, "%.1f", r.userScore)}", gameColors.bad)
        }
    }
}

/** The art on a ratings decision: the game, and what an Adults Only rating would cost it. */
@Composable
fun RatingDisputeArt(state: GameState, kind: RatingDispute): Boolean {
    val p = state.games?.projectOf(kind.team) ?: return false
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GameCover(p.concept, 52.dp, studio = state.companyName)
        RatingBadge(AgeRating.ADULTS_ONLY)
        Column(Modifier.padding(start = 4.dp)) {
            Text(p.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val lost = p.concept.platforms - adultsOnlyPlatforms(p.concept).toSet()
            Hint(if (lost.isEmpty()) "Its platforms all carry Adults Only games" else "Adults Only loses ${lost.joinToString { it.title }}")
        }
    }
    return true
}

/** The art on a review bomb: the game and its user score. */
@Composable
fun ReviewBombArt(state: GameState, kind: ReviewBomb): Boolean {
    val r = state.games?.release(kind.releaseId) ?: return false
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GameCover(r.concept, 52.dp, studio = state.companyName)
        Column(Modifier.padding(start = 4.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                String.format(Locale.US, "Players %.1f  ▼ %.1f", r.userScore, kind.drop),
                style = MaterialTheme.typography.labelLarge, color = gameColors.bad, fontWeight = FontWeight.Bold,
            )
            Hint("Critics ${String.format(Locale.US, "%.1f", r.score)}")
        }
    }
    return true
}

/** Boxes and collector's editions to order, for a game you publish yourself. */
@Composable
fun EditionsControls(store: GameStore, state: GameState, p: GameProject, team: Int) {
    if (Research.BOXED_EDITIONS !in state.researched) return
    val blocker = state.editionsBlocker(team)
    if (blocker != null) {
        Hint("Boxed editions: $blocker")
        return
    }
    val estimate = state.boxEstimate(team)
    fun round(n: Double, to: Int) = (kotlin.math.round(n / to) * to).toInt().coerceAtLeast(to)
    val boxOptions = (listOf(0) + listOf(0.5, 0.8, 1.0, 1.3, 2.0).map { round(estimate * it, 1_000) }).distinct()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Boxes", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        MenuButton(if (p.boxes == 0) "Distributor" else Format.count(p.boxes.toDouble()), boxOptions, { n ->
            if (n == 0) "Leave it to a distributor" else "${Format.count(n.toDouble())} · ${Format.money(n * state.boxCost())}"
        }) { n -> store.perform { orderEditions(team, n, p.collectors) } }
    }
    val fans = state.collectorEstimate(team)
    if (fans >= 100) {
        val options = (listOf(0) + listOf(0.5, 1.0, 2.0).map { round(fans * it, 100) }).distinct()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Collector's editions", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            MenuButton(if (p.collectors == 0) "None" else Format.count(p.collectors.toDouble()), options, { n ->
                if (n == 0) "None" else "${Format.count(n.toDouble())} · ${Format.money(n * state.collectorCost())}"
            }) { n -> store.perform { orderEditions(team, p.boxes, n) } }
        }
    }
    Hint(
        "Shops would take about ${Format.count(estimate.toDouble())} boxes over its life" +
            (if (fans >= 100) ", and fans about ${Format.count(fans.toDouble())} collector's editions" else "") +
            ". Your own boxes earn more than a distributor pays; too many and the rest go cheap, too few and buyers go without.",
    )
}
