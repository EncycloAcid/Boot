package com.boot.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.boot.game.audio.Sfx
import com.boot.game.audio.verdictFor
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Format
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.Research
import com.boot.core.Review
import com.boot.core.acknowledgeReview
import com.boot.core.expansionBlocker
import com.boot.core.patchBlocker
import com.boot.core.patchCost
import com.boot.core.patchGame
import com.boot.core.leagueBlocker
import com.boot.core.leagueCost
import com.boot.core.remasterBlocker
import com.boot.core.startLeague
import com.boot.core.remasterConcept
import com.boot.core.sequelConcept
import com.boot.core.startExpansion
import com.boot.game.GameStore
import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Launch day: the critics give their verdicts one after another, then the players weigh in. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewSheet(store: GameStore, state: GameState, releaseId: Int) {
    val r = state.games?.release(releaseId) ?: return
    val sheet = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(
        onDismissRequest = { store.perform { acknowledgeReview() } },
        sheetState = sheet,
        containerColor = gameColors.grouped,
    ) {
        Scaled {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Hint("The reviews are in")
                    Spacer(Modifier.size(10.dp))
                    GameCover(r.concept, 132.dp, studio = r.developer ?: r.publisher ?: state.companyName)
                    Spacer(Modifier.size(12.dp))
                    Text(r.title, style = MaterialTheme.typography.headlineMediumEmphasized)
                    Spacer(Modifier.size(12.dp))
                    ScoreSummary(r)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "${r.verdict} · launch week ${Format.money(r.salesHistory.firstOrNull() ?: r.weeklySales)}" +
                            if (r.chartRank > 0) " · #${r.chartRank} on the charts" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Why, before what each critic said.
                if (r.breakdown.isNotEmpty()) ScoreBreakdown(r, top = 4)
                GroupedSection("Critics") {
                    r.reviews.forEachIndexed { i, review ->
                        if (i > 0) RowDivider()
                        // The last critic's ding is followed by a cheer or a groan for the whole game.
                        RevealedReview(review, i, verdict = if (i == r.reviews.lastIndex) verdictFor(r.score) else null)
                    }
                }
                ProsCons(r.pros, r.cons)
                PlayerReviews(r.playerQuotes)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton("Game report", {
                        store.openRelease = r.id
                        store.perform { acknowledgeReview() }
                    }, Modifier.weight(1f))
                    PrimaryButton("Continue", { store.perform { acknowledgeReview() } }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** One outlet's score, springing in a beat after the one before. */
@Composable
private fun RevealedReview(review: Review, index: Int, verdict: Sfx? = null) {
    val reveal = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val sound = LocalSound.current
    LaunchedEffect(Unit) {
        // A beat longer than the animation alone, so each score gets its own ding.
        kotlinx.coroutines.delay(380L * index)
        sound.review(review.score)
        reveal.animateTo(1f, spec)
        if (verdict != null) {
            kotlinx.coroutines.delay(250)
            sound.play(verdict)
        }
    }
    ReviewRow(
        review,
        Modifier
            .alpha(reveal.value.coerceIn(0f, 1f))
            .scale(0.92f + 0.08f * reveal.value),
    )
}

/** Everything about a released game: reviews, sales, and the report that teaches you for next time. */
@Composable
fun ReleaseSheet(store: GameStore, state: GameState, releaseId: Int) {
    val g = state.games ?: return
    val r: GameRelease = g.release(releaseId) ?: return
    val colors = gameColors
    val canSequel = Research.SEQUELS in state.researched && !g.busy && r.developer == null && r.publisher == null
    GameDialog(onDismissRequest = { store.openRelease = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { store.openRelease = null }) { Icon(Icons.Filled.Close, "Close") }
                    Text("Game report", style = MaterialTheme.typography.titleMedium)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GameCover(r.concept, 108.dp, studio = r.developer ?: r.publisher ?: state.companyName)
                        Column {
                            Text(r.title, style = MaterialTheme.typography.headlineMediumEmphasized)
                            Hint(
                                "${r.concept.size.title} ${r.concept.topic.title.lowercase()} ${r.concept.genreLower} · " +
                                    r.concept.platforms.joinToString { it.title } + (r.publisher?.let { " · published by $it" } ?: "") +
                                    (r.developer?.let { " · made by $it, published by you" } ?: ""),
                            )
                            r.publisher?.let {
                                Spacer(Modifier.size(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CompanyLogo(it, size = 28.dp)
                                    Text(it, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { ScoreSummary(r) }
                    GroupedSection("Sales") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                            TileRow {
                                StatTile("Earned", Format.money(r.lifetimeSales), Modifier.weight(1f), "after the cut")
                                StatTile("Copies", Format.count(r.unitsSold), Modifier.weight(1f))
                                StatTile("Best rank", if (r.bestRank > 0) "#${r.bestRank}" else "—", Modifier.weight(1f))
                            }
                        }
                        if (r.salesHistory.size > 1) CardBlock { AreaChart(r.salesHistory, colors.lane(com.boot.core.Lane.GAMES), Format::money) }
                        RowDivider()
                        ValueRow("Development cost", Format.money(r.devCost))
                        RowDivider()
                        ValueRow(
                            "Profit so far", Format.signedMoney(r.lifetimeSales - r.devCost),
                            if (r.lifetimeSales >= r.devCost) colors.good else colors.bad,
                        )
                    }
                    LiveServiceSection(store, state, r)
                    GroupedSection("Critics") {
                        r.reviews.forEachIndexed { i, review ->
                            if (i > 0) RowDivider()
                            ReviewRow(review)
                        }
                    }
                    AspectBreakdown(r.aspects)
                    ProsCons(r.pros, r.cons)
                    PlayerReviews(r.playerQuotes)
                    if (r.breakdown.isNotEmpty()) ScoreBreakdown(r)
                    GroupedSection(
                        "What you learned",
                        footer = String.format(Locale.US, "Design %.0f · Tech %.0f · Hype %.0f · ", r.design, r.tech, r.hype) +
                            if (r.patched) "patched" else String.format(Locale.US, "%.0f bugs shipped", r.bugs),
                    ) {
                        r.report.forEachIndexed { i, line ->
                            if (i > 0) RowDivider(52.dp)
                            ListRow(line, leading = { Icon(Icons.Filled.Lightbulb, null, tint = colors.warn) })
                        }
                    }
                    AfterLaunch(store, state, r)
                    if (canSequel) {
                        PrimaryButton("Make a sequel", {
                            val seed = store.state?.sequelConcept(r.id)
                            store.openRelease = null
                            store.designGame(seed)
                        }, Modifier.fillMaxWidth())
                    }
                    if (r.developer == null && r.publisher == null && r.concept.remasterOf == null && Research.REMASTERS in state.researched) {
                        val blocker = state.remasterBlocker(r.id)
                        TonalButton("Remaster ${r.title}", {
                            val seed = store.state?.remasterConcept(r.id)
                            store.openRelease = null
                            store.designGame(seed)
                        }, Modifier.fillMaxWidth(), enabled = blocker == null && !g.busy)
                        Hint(blocker ?: if (g.busy) "The main team is busy." else "Rebuild it for today's platforms at half the work. Its old fans come back for it.")
                    }
                }
            }
        }
    }
}

/** Keep a game alive after launch: patch what shipped broken, and sell more to the people who loved it. */
@Composable
private fun AfterLaunch(store: GameStore, state: GameState, r: GameRelease) {
    if (r.publisher != null || r.developer != null) return
    val patch = state.patchBlocker(r.id)
    val expansion = state.expansionBlocker(r.id)
    GroupedSection(
        "After launch",
        footer = if (r.expansions.isEmpty()) null else "Expansions: ${r.expansions.joinToString()}.",
    ) {
        ActionRow(
            if (r.patched) "Patched" else "Release a patch",
            if (r.patched) {
                "Bugs cleared. Player score recovered, and sales held up longer."
            } else {
                patch ?: "Fixes ${r.bugs.toInt()} bugs for ${Format.money(r.patchCost())}. Raises the player score, refreshes sales, and brings live players back."
            },
            if (r.patched) "Done" else "Patch",
            enabled = patch == null,
        ) { store.perform { patchGame(r.id) } }
        if (Research.ESPORTS in state.researched) {
            RowDivider()
            val league = r.league
            if (league == null) {
                val blocker = state.leagueBlocker(r.id)
                ActionRow(
                    "Start an esports league",
                    blocker ?: "Costs ${Format.money(r.leagueCost())}. Sponsors pay for viewers, and watching keeps people playing.",
                    "Start",
                    enabled = blocker == null,
                ) { store.perform { startLeague(r.id) } }
            } else {
                ValueRow(league.name, if (league.closed) "folded" else "${Format.count(league.viewers)} viewers")
            }
        }
        RowDivider()
        ActionRow(
            "Make an expansion pack",
            expansion ?: "New content the team builds like a small game. Owners of ${r.title} come back for it.",
            "Start",
            enabled = expansion == null,
        ) {
            store.perform { startExpansion(r.id) }
            store.openRelease = null
        }
    }
}

/**
 * Why a game scored what it did: the baseline, then every part that added or took points off,
 * biggest first. [top] shows only the biggest few, for the verdict screen.
 */
@Composable
fun ScoreBreakdown(r: GameRelease, top: Int? = null) {
    val colors = gameColors
    val baseline = r.breakdown.first()
    val all = r.breakdown.drop(1).sortedByDescending { kotlin.math.abs(it.points) }
    // The verdict screen shows real reasons; the critics' mood is in the full report.
    val shown = top?.let { all.filter { p -> p.label != "Critics' mood" }.take(it) } ?: all
    val total = r.breakdown.sumOf { it.points }.coerceIn(1.0, 10.0)
    GroupedSection(
        if (top != null) "Why it scored ${String.format(Locale.US, "%.1f", r.score)}" else "How the score added up",
        footer = if (top != null && all.size > top) "The full breakdown is in the game report." else
            "Every game starts at ${String.format(Locale.US, "%.1f", baseline.points)}. The critics' average lands near the total, each with their own taste.",
    ) {
        if (top == null) {
            ListRow(baseline.label, subtitle = baseline.note, value = String.format(Locale.US, "%.1f", baseline.points))
            RowDivider()
        }
        shown.forEachIndexed { i, part ->
            if (i > 0) RowDivider()
            ListRow(
                part.label,
                subtitle = part.note.ifEmpty { null },
                value = (if (part.points > 0) "+" else "") + String.format(Locale.US, "%.1f", part.points),
                valueColor = when {
                    part.points > 0 -> colors.good
                    part.points < 0 -> colors.bad
                    else -> Color.Unspecified
                },
            )
        }
        if (top == null) {
            RowDivider()
            ListRow("Total", value = String.format(Locale.US, "%.1f", total))
        }
    }
}
