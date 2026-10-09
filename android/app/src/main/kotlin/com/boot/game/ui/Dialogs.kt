package com.boot.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Combo
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.PivotMode
import com.boot.core.PivotTrigger
import com.boot.core.acceptPivotBuyout
import com.boot.core.choose
import com.boot.core.pivotBlocker
import com.boot.core.pivotChoices
import com.boot.core.resolvePivot
import com.boot.game.GameStore

private val blocking = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
private val fullScreen = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)

/** A decision that blocks the calendar until you answer it. */
@Composable
fun EventDialog(store: GameStore, state: GameState) {
    val e = state.events.first()
    Dialog(onDismissRequest = {}, properties = blocking) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                e.lane?.let { lane ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LaneBadge(lane, 28)
                        Spacer(Modifier.width(8.dp))
                        Hint(lane.businessName)
                    }
                }
                Text(e.title, style = MaterialTheme.typography.headlineSmall)
                Text(e.body, style = MaterialTheme.typography.bodyMedium)
                e.choices.forEachIndexed { i, choice ->
                    OutlinedCard(onClick = { store.perform { choose(i) } }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(choice.label, style = MaterialTheme.typography.titleSmall)
                            Hint(choice.detail)
                        }
                    }
                }
            }
        }
    }
}

/** The mid-game choice: pick a second lane, then expand into it or pivot into it. */
@Composable
fun PivotDialog(store: GameStore, state: GameState) {
    val offer = state.pendingPivot ?: return
    val colors = gameColors
    var lane by rememberSaveable(offer.week) { mutableStateOf(offer.suggestedLane) }
    var mode by rememberSaveable(offer.week) { mutableStateOf(PivotMode.EXPAND) }
    var useVenture by rememberSaveable(offer.week) { mutableStateOf(false) }
    var confirmSale by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {}, properties = fullScreen) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "THE PIVOT POINT · ${offer.trigger.title.uppercase()}",
                        style = MaterialTheme.typography.labelLarge, color = colors.milestone, fontWeight = FontWeight.Bold,
                    )
                    Text(offer.headline, style = MaterialTheme.typography.headlineSmall)
                    Text(offer.body, style = MaterialTheme.typography.bodyMedium)

                    Text("Pick a second lane", style = MaterialTheme.typography.titleMedium)
                    state.pivotChoices.forEach { l ->
                        PivotLaneCard(state, l, selected = l == lane, suggested = l == offer.suggestedLane) { lane = l }
                    }

                    Text("How do you take it on?", style = MaterialTheme.typography.titleMedium)
                    Segmented(PivotMode.entries, mode, { it.title }) { mode = it }
                    Hint(
                        when (mode) {
                            PivotMode.PIVOT ->
                                "${mode.summary} The ${state.startingLane.businessName.lowercase()} sells for " +
                                    "${Format.money(offer.salePrice)}. People whose skills carry over come with you."
                            PivotMode.EXPAND -> "${mode.summary} Setup costs ${Format.money(lane?.setupCost ?: 0.0)}."
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Fund it with venture money", style = MaterialTheme.typography.bodyLarge)
                            Hint("Hardware and AI need millions. A round brings a board that can overrule you.")
                        }
                        Switch(checked = useVenture, onCheckedChange = { useVenture = it })
                    }
                }
                HorizontalDivider()
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val blocker = lane?.let { state.pivotBlocker(it, mode, useVenture) }
                    if (blocker != null) Hint(blocker, colors.bad)
                    Button(
                        onClick = { lane?.let { l -> store.perform { resolvePivot(l, mode, useVenture) } } },
                        enabled = lane != null && blocker == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("${mode.title} into ${lane?.title ?: "…"}") }
                    if (offer.trigger == PivotTrigger.BUYOUT_OFFER) {
                        TextButton(onClick = { confirmSale = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Sell the whole company and walk away", color = colors.bad)
                        }
                    }
                }
            }
        }
        if (confirmSale) {
            ConfirmDialog(
                title = "Sell everything?",
                text = "${offer.bidder ?: "The buyer"} takes the whole company. The game ends here.",
                confirm = "Sell",
                onConfirm = { store.perform { acceptPivotBuyout() } },
                onDismiss = { confirmSale = false },
            )
        }
    }
}

@Composable
private fun PivotLaneCard(state: GameState, lane: Lane, selected: Boolean, suggested: Boolean, onClick: () -> Unit) {
    val colors = gameColors
    val accent = colors.lane(lane)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pivot-lane-${lane.name}"),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LaneBadge(lane, 36)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(lane.title, style = MaterialTheme.typography.titleSmall)
                    if (suggested) Pill("head start", colors.good)
                    if (lane.isCapitalIntensive) Pill("VC", colors.warn)
                }
                Combo.between(state.startingLane, lane)?.let { Hint("Unlocks ${it.product}: ${it.pitch}") }
            }
        }
    }
}

/** The story of your run. */
@Composable
fun EndingDialog(store: GameStore, state: GameState) {
    val e = state.ending ?: return
    Dialog(onDismissRequest = {}, properties = fullScreen) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(e.kind.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(e.title, style = MaterialTheme.typography.displaySmall)
                    Text(e.summary, style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    e.story.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    HorizontalDivider()
                    TileRow {
                        StatTile("Final valuation", Format.money(e.valuation), Modifier.weight(1f))
                        StatTile("You walk away with", Format.money(e.founderTakeHome), Modifier.weight(1f))
                    }
                    TileRow {
                        StatTile("Ended", GameClock.label(e.week), Modifier.weight(1f))
                        StatTile("Prestige", if (e.ecosystem) "Ecosystem" else "—", Modifier.weight(1f))
                    }
                }
                Button(onClick = { store.abandon() }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("New game") }
            }
        }
    }
}
