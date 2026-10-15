package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.EndingKind
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.PivotMode
import com.boot.core.PivotTrigger
import com.boot.core.acceptPivotBuyout
import com.boot.core.acknowledgeReview
import com.boot.core.choose
import com.boot.core.keepPlaying
import com.boot.core.pivotBlocker
import com.boot.core.pivotChoices
import com.boot.core.resolvePivot
import com.boot.core.stayFocused
import com.boot.desktop.DesktopStore

private val modalProps = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
)

/** Blocking overlays that mirror Android GameScreen: ending → pivot → event → review. */
@Composable
fun DecisionOverlays(store: DesktopStore, state: GameState) {
    when {
        state.isOver -> EndingDialog(store, state)
        state.pendingPivot != null -> PivotDialog(store, state)
        state.events.isNotEmpty() -> EventSheet(store, state)
        state.games?.unseenReview != null -> ReviewSheet(store, state, state.games!!.unseenReview!!)
    }
}

@Composable
private fun ModalScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    val h = hudColors
    Dialog(onDismissRequest = {}, properties = modalProps) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 360.dp, max = 520.dp)
                    .fillMaxWidth(0.55f)
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp)),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = h.ink)
                    if (subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Hint(subtitle)
                    }
                }
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    content()
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F6F8))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    actions()
                }
            }
        }
    }
}

@Composable
fun EventSheet(store: DesktopStore, state: GameState) {
    val e = state.events.first()
    // Choices live in the scrollable body so multi-option events can't clip off-screen.
    ModalScaffold(
        title = e.title,
        subtitle = e.lane?.businessName,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(e.body, style = MaterialTheme.typography.bodyLarge, color = hudColors.ink)
                Spacer(Modifier.height(4.dp))
                e.choices.forEachIndexed { i, choice ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        MacButton(
                            choice.label,
                            onClick = { store.perform { choose(i) } },
                            primary = i == 0,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Hint(choice.detail)
                    }
                }
            }
        },
        actions = {
            Hint("Pick an option above — the calendar waits until you do.")
            store.error?.let { Hint(it, gameColors.bad) }
        },
    )
}

@Composable
fun PivotDialog(store: DesktopStore, state: GameState) {
    val offer = state.pendingPivot ?: return
    var lane by remember(offer.week) { mutableStateOf(offer.suggestedLane) }
    var mode by remember(offer.week) { mutableStateOf(PivotMode.EXPAND) }
    var useVenture by remember(offer.week) { mutableStateOf(false) }
    val colors = gameColors
    ModalScaffold(
        title = "The Pivot Point",
        subtitle = "${offer.trigger.title} · ${offer.headline}",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Hint(offer.body)
                Text("Second lane", style = MaterialTheme.typography.labelLarge, color = hudColors.textDim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.pivotChoices.forEach { l ->
                        SelectChip(
                            l.title + if (l == offer.suggestedLane) " · tip" else "",
                            l == lane,
                        ) { lane = l }
                    }
                }
                Text("How", style = MaterialTheme.typography.labelLarge, color = hudColors.textDim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in PivotMode.intoNewLane) {
                        SelectChip(m.title, mode == m) { mode = m }
                    }
                }
                Hint(
                    when (mode) {
                        PivotMode.PIVOT ->
                            "${mode.summary} Sells the ${state.startingLane.businessName.lowercase()} for ${Format.money(offer.salePrice)}."
                        PivotMode.EXPAND -> "${mode.summary} Setup ${Format.money(lane?.setupCost ?: 0.0)}."
                        PivotMode.STAY -> mode.summary
                    },
                )
                SelectChip(
                    if (useVenture) "Venture funding · on" else "Venture funding · off",
                    useVenture,
                ) { useVenture = !useVenture }
                lane?.let { l ->
                    state.pivotBlocker(l, mode, useVenture)?.let { Hint(it, colors.bad) }
                }
            }
        },
        actions = {
            val blocker = lane?.let { state.pivotBlocker(it, mode, useVenture) }
            MacButton(
                "${mode.title} into ${lane?.title ?: "…"}",
                onClick = { lane?.let { l -> store.perform { resolvePivot(l, mode, useVenture) } } },
                enabled = lane != null && blocker == null,
                modifier = Modifier.fillMaxWidth(),
            )
            MacButton(
                "Stay focused on ${state.startingLane.businessName.lowercase()}",
                onClick = { store.perform { stayFocused() } },
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
            if (offer.trigger == PivotTrigger.BUYOUT_OFFER) {
                MacButton(
                    "Sell everything and walk away",
                    onClick = { store.perform { acceptPivotBuyout() } },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
fun EndingDialog(store: DesktopStore, state: GameState) {
    val e = state.ending ?: return
    val colors = gameColors
    val good = e.kind == EndingKind.IPO || e.kind == EndingKind.ACQUIRED || e.kind == EndingKind.INDIE
    ModalScaffold(
        title = e.kind.title,
        subtitle = e.title,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Hint(e.summary, if (good) colors.good else colors.bad)
                Stat("Final valuation", Format.money(e.valuation))
                Stat("You walk away with", Format.money(e.founderTakeHome))
                e.story.take(4).forEach { Hint(it) }
            }
        },
        actions = {
            if (e.kind == EndingKind.IPO || e.kind == EndingKind.INDIE) {
                MacButton(
                    if (e.kind == EndingKind.IPO && state.listing == null) {
                        "Keep playing as a public company"
                    } else {
                        "Keep playing"
                    },
                    onClick = { store.perform { keepPlaying() } },
                    modifier = Modifier.fillMaxWidth(),
                )
                MacButton("New game", onClick = { store.abandon() }, primary = false, modifier = Modifier.fillMaxWidth())
            } else {
                MacButton("New game", onClick = { store.abandon() }, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@Composable
fun ReviewSheet(store: DesktopStore, state: GameState, releaseId: Int) {
    val r = state.games?.release(releaseId) ?: return
    val colors = gameColors
    ModalScaffold(
        title = "The reviews are in",
        subtitle = r.title,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%.1f", r.score),
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.good,
                    fontWeight = FontWeight.Bold,
                )
                Hint("${r.verdict} · launch week ${Format.money(r.salesHistory.firstOrNull() ?: r.weeklySales)}")
                if (r.chartRank > 0) Hint("#${r.chartRank} on the charts", colors.milestone)
                r.reviews.take(3).forEach { review ->
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text(
                            "${review.outlet.title} · ${review.formatted}",
                            style = MaterialTheme.typography.titleMedium,
                            color = hudColors.text,
                        )
                        Hint("“${review.comment}”")
                    }
                }
                if (r.pros.isNotEmpty()) Hint("Pros: ${r.pros.take(2).joinToString()}", colors.good)
                if (r.cons.isNotEmpty()) Hint("Cons: ${r.cons.take(2).joinToString()}", colors.bad)
            }
        },
        actions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacButton(
                    "Game report",
                    onClick = {
                        store.openRelease = r.id
                        store.perform { acknowledgeReview() }
                    },
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
                MacButton(
                    "Continue",
                    onClick = { store.perform { acknowledgeReview() } },
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Hint(label)
        Text(value, style = MaterialTheme.typography.titleMedium, color = hudColors.ink, fontWeight = FontWeight.SemiBold)
    }
}
