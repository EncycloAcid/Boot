package com.boot.game.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.Combo
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.IPO_ANNUAL_REVENUE
import com.boot.core.IPO_VALUATION
import com.boot.core.Lane
import com.boot.core.expand
import com.boot.core.expansionBlocker
import com.boot.core.goPublic
import com.boot.core.ipoBlocker
import com.boot.core.isEcosystem
import com.boot.core.raiseRound
import com.boot.core.roundOffer
import com.boot.game.GameStore

@Composable
fun MoneyScreen(store: GameStore, state: GameState) {
    ScreenColumn {
        item { OwnershipCard(store, state) }
        item { BoardCard(state) }
        item { ExpansionCard(store, state) }
        item { ExitCard(store, state) }
    }
}

@Composable
private fun OwnershipCard(store: GameStore, state: GameState) {
    val v = state.venture
    Section("Ownership") {
        KeyValue("You own", Format.percent(v.founderEquity))
        KeyValue("Raised", Format.money(v.raised))
        v.rounds.forEach { r ->
            KeyValue("${r.name}, ${GameClock.label(r.week)}", "${Format.money(r.amount)} at ${Format.money(r.preMoney)} pre")
        }
        HorizontalDivider()
        val offer = state.roundOffer()
        offer.onSuccess { o ->
            Text(
                "Investors would put in ${Format.money(o.amount)} for ${Format.percent(o.dilution)} of the company.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { store.perform { raiseRound() } }) { Text("Raise a ${o.name}") }
            Hint("Money comes with a board that sets growth targets and can overrule or fire you.")
        }
        offer.onFailure { Hint(it.message ?: "") }
    }
}

@Composable
private fun BoardCard(state: GameState) {
    val colors = gameColors
    Section("Board") {
        val b = state.venture.board
        if (b == null) {
            Hint("No board. Nobody can overrule you.")
        } else {
            Meter("Confidence", b.confidence, if (b.confidence < 0.4) colors.bad else colors.good)
            KeyValue("Growth target", "${Format.percent(b.growthTarget)} per quarter")
            b.lastGrowth?.let { KeyValue("Last quarter", Format.percent(it), if (it >= b.growthTarget) colors.good else colors.bad) }
            KeyValue("Grace quarters left", "${b.graceQuarters}")
            KeyValue("Demands met / defied", "${b.demandsMet} / ${b.demandsDefied}")
            Hint("Below 40% they start overruling you. At zero they replace you.")
        }
    }
}

@Composable
private fun ExpansionCard(store: GameStore, state: GameState) {
    var useVenture by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Lane?>(null) }
    val colors = gameColors
    Section("Expansion") {
        val blocker = state.expansionBlocker
        if (blocker != null) {
            Hint(blocker)
        } else {
            Text(
                "Pick another lane. Hardware, software and services together make an ecosystem company.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fund it with venture money", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = useVenture, onCheckedChange = { useVenture = it })
            }
            Lane.entries.filter { it !in state.lanes }.forEach { lane ->
                val combos = state.lanes.mapNotNull { Combo.between(it, lane)?.product }
                ActionRow(
                    title = "${lane.emoji} ${lane.title}",
                    detail = "Setup ${Format.money(lane.setupCost)}" +
                        (if (lane.isCapitalIntensive) " · needs ${Format.money(lane.selfFundThreshold)} or VC" else "") +
                        (if (combos.isEmpty()) "" else " · unlocks ${combos.joinToString(", ")}"),
                    action = "Expand",
                ) { confirm = lane }
            }
        }
        if (state.isEcosystem) {
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, null, tint = colors.milestone)
                Spacer(Modifier.width(8.dp))
                Text("Ecosystem company", color = colors.milestone, style = MaterialTheme.typography.titleSmall)
            }
            Meter("Profitable ecosystem weeks", state.stats.ecosystemWeeks / 52.0, colors.milestone, "${state.stats.ecosystemWeeks}/52")
            Hint("Holding it for a full year is the prestige goal. Antitrust pressure builds the whole time.")
        } else if (Lane.HARDWARE in state.lanes || state.lanes.size >= 2) {
            Hint("Ecosystem company: Hardware + software (Apps or Games) + services (AI, Social or Websites).")
        }
    }
    confirm?.let { lane ->
        ConfirmDialog(
            title = "Expand into ${lane.title}?",
            text = "Setup costs ${Format.money(lane.setupCost)}" +
                (if (useVenture) ", funded by a new venture round." else ".") +
                " Another lane splits your attention and drains your energy faster.",
            confirm = "Expand",
            onConfirm = { store.perform { expand(lane, useVenture) } },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun ExitCard(store: GameStore, state: GameState) {
    var confirm by remember { mutableStateOf(false) }
    Section("Exit") {
        KeyValue("Business value (ex-cash)", Format.money(state.enterpriseValue))
        Meter("Toward an IPO valuation", state.enterpriseValue / IPO_VALUATION, caption = Format.money(IPO_VALUATION))
        val annual = state.trailingRevenue(weeks = 52)
        Meter("Toward IPO revenue", annual / IPO_ANNUAL_REVENUE, caption = "${Format.money(annual)} of ${Format.money(IPO_ANNUAL_REVENUE)}")
        val blocker = state.ipoBlocker
        if (blocker != null) {
            Hint(blocker)
        } else {
            Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Go public") }
            Hint("This ends the game.")
        }
        Hint("Or stay private: reach year 15 and you stay independent. Buyout offers can end things sooner.")
    }
    if (confirm) {
        ConfirmDialog(
            title = "Ring the bell?",
            text = "${state.companyName} goes public and the game ends.",
            confirm = "Go public",
            onConfirm = { store.perform { goPublic() } },
            onDismiss = { confirm = false },
        )
    }
}
