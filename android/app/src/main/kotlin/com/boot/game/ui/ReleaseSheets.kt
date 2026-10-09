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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Format
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.Research
import com.boot.core.Review
import com.boot.core.acknowledgeReview
import com.boot.core.sequelConcept
import com.boot.game.GameStore
import java.util.Locale

/** Launch day: four outlets give their verdicts, one after another. */
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
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Hint("THE REVIEWS ARE IN")
                Text(r.title, style = MaterialTheme.typography.headlineMediumEmphasized)
                Spacer(Modifier.size(8.dp))
                ScoreBadge(r.score, 88.dp)
                Spacer(Modifier.size(6.dp))
                Text(
                    "${r.verdict} · launch week ${Format.money(r.salesHistory.firstOrNull() ?: r.weeklySales)}" +
                        if (r.chartRank > 0) " · #${r.chartRank} on the charts" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GroupedSection(null) {
                r.reviews.forEachIndexed { i, review ->
                    if (i > 0) RowDivider(72.dp)
                    RevealedReview(review, i)
                }
            }
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

/** One outlet's score, springing in a beat after the one before. */
@Composable
private fun RevealedReview(review: Review, index: Int) {
    val reveal = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250L * index)
        reveal.animateTo(1f, spec)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(reveal.value.coerceIn(0f, 1f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.scale(0.6f + 0.4f * reveal.value)) { ScoreBadge(review.score.toDouble(), 48.dp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(review.outlet.title, style = MaterialTheme.typography.titleSmall)
            Text("“${review.comment}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Everything about a released game: reviews, sales, and the report that teaches you for next time. */
@Composable
fun ReleaseSheet(store: GameStore, state: GameState, releaseId: Int) {
    val g = state.games ?: return
    val r: GameRelease = g.release(releaseId) ?: return
    val colors = gameColors
    val canSequel = Research.SEQUELS in state.researched && g.project == null && g.contract == null
    Dialog(onDismissRequest = { store.openRelease = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScoreBadge(r.score, 72.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(r.title, style = MaterialTheme.typography.headlineMediumEmphasized)
                            Hint(
                                "${r.concept.size.title} ${r.concept.topic.title.lowercase()} ${r.concept.genreLower} · " +
                                    r.concept.platforms.joinToString { it.title } + (r.publisher?.let { " · published by $it" } ?: ""),
                            )
                        }
                    }
                    GroupedSection("Sales") {
                        Column(Modifier.padding(8.dp)) {
                            TileRow {
                                StatTile("Earned", Format.money(r.lifetimeSales), Modifier.weight(1f), "after platform cut")
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
                    GroupedSection("Reviews") {
                        r.reviews.forEachIndexed { i, review ->
                            if (i > 0) RowDivider(72.dp)
                            ListRow(review.outlet.title, subtitle = "“${review.comment}”", leading = { ScoreBadge(review.score.toDouble(), 44.dp) })
                        }
                    }
                    GroupedSection(
                        "What you learned",
                        footer = String.format(Locale.US, "Design %.0f · Tech %.0f · Bugs at launch %.0f · Hype %.0f", r.design, r.tech, r.bugs, r.hype),
                    ) {
                        r.report.forEachIndexed { i, line ->
                            if (i > 0) RowDivider(52.dp)
                            ListRow(line, leading = { Icon(Icons.Filled.Lightbulb, null, tint = colors.warn) })
                        }
                    }
                    if (canSequel) {
                        PrimaryButton("Make a sequel", {
                            val seed = store.state?.sequelConcept(r.id)
                            store.openRelease = null
                            store.designGame(seed)
                        }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
