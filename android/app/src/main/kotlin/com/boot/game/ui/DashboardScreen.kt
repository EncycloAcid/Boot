package com.boot.game.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.expansionBlocker
import com.boot.core.isEcosystem
import com.boot.game.GameStore
import com.boot.game.Tab

@Composable
fun DashboardScreen(store: GameStore, state: GameState) {
    ScreenColumn {
        item { Tiles(state) }
        item { RevenueCard(state) }
        item { LanesCard(store, state) }
        item { EraCard(state) }
        item { ReputationCard(state) }
        item {
            Section("Latest", trailing = { TextButton(onClick = { store.tab = Tab.NEWS }) { Text("All news") } }) {
                state.log.takeLast(8).asReversed().forEach { LogRow(it) }
            }
        }
    }
}

@Composable
private fun Tiles(state: GameState) {
    val colors = gameColors
    val weeklyProfit = state.trailingProfit(weeks = 4) / 4
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TileRow {
            StatTile("Valuation", Format.money(state.valuation), Modifier.weight(1f), "You own ${Format.percent(state.venture.founderEquity)}")
            StatTile(
                "Weekly profit", Format.signedMoney(weeklyProfit), Modifier.weight(1f), "4-week average",
                if (weeklyProfit >= 0) colors.good else colors.bad,
            )
        }
        TileRow {
            StatTile(
                "Reputation", "${state.reputation.score.toInt()}", Modifier.weight(1f),
                if (state.reputation.scars.isEmpty()) "Clean" else "${state.reputation.scars.size} scar(s)",
            )
            StatTile(
                "Energy", "${state.founder.energy.toInt()}", Modifier.weight(1f),
                when {
                    state.founder.isOnVacation -> "On vacation"
                    state.crunch -> "Crunching"
                    else -> "Founder"
                },
                if (state.founder.energy < 25) colors.bad else Color.Unspecified,
            )
            StatTile("Team", "${state.staff.size + 1}", Modifier.weight(1f), state.headquarters.name)
        }
    }
}

@Composable
private fun RevenueCard(state: GameState) {
    val colors = gameColors
    val weeks = state.ledger.takeLast(52)
    val sources = Lane.entries.filter { lane -> weeks.any { (it.revenue[lane] ?: 0.0) > 0 } }
    val hasOther = weeks.any { it.otherRevenue > 0 }
    Section("Revenue and costs, last 52 weeks") {
        StackedBarChart(
            bars = weeks.map { w ->
                sources.map { (w.revenue[it] ?: 0.0) to colors.lane(it) } + (w.otherRevenue to colors.other)
            },
            line = weeks.map { it.totalCosts },
            lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
            format = Format::money,
        )
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            sources.forEach { Legend(it.title, colors.lane(it)) }
            if (hasOther) Legend("Other", colors.other)
            Legend("Costs (dashed)", MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LanesCard(store: GameStore, state: GameState) {
    val colors = gameColors
    Section("Your lanes") {
        state.lanes.forEachIndexed { i, lane ->
            if (i > 0) HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { store.openLane(lane) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LaneBadge(lane, 36)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(lane.businessName, style = MaterialTheme.typography.titleSmall)
                    Text(state.statusLine(lane), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text((state.trailingRevenue(lane, 4) / 4).perWeek(), style = MaterialTheme.typography.bodyMedium)
                    val profit = state.trailingProfit(lane, 4) / 4
                    Text(
                        "${Format.signedMoney(profit)} profit",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (profit >= 0) colors.good else colors.bad,
                    )
                }
            }
        }
        if (state.isEcosystem) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, null, tint = colors.milestone)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ecosystem company: ${state.stats.ecosystemWeeks} profitable weeks. Hold it for 52.",
                    color = colors.milestone, style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        when {
            state.pivot == null -> Hint("The Pivot Point arrives between year 2 and year 4.")
            state.expansionBlocker == null -> Hint("Every lane is profitable. You can expand from the Money tab.", colors.good)
        }
    }
}

@Composable
private fun EraCard(state: GameState) {
    val colors = gameColors
    val era = state.world.era
    Section("Era") {
        Text(era.title, style = MaterialTheme.typography.titleLarge)
        Hint(era.blurb)
        HorizontalDivider()
        for (lane in Lane.entries) {
            val d = era.demand(lane)
            Row {
                Text(
                    lane.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (lane in state.lanes) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (d == 1.0) "—" else (if (d > 1) "+" else "") + Format.percent(d - 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        d > 1 -> colors.good
                        d < 1 -> colors.warn
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        if (era.gpuPrice != 1.0) {
            Hint("GPU prices ${if (era.gpuPrice > 1) "up" else "down"} ${Format.percent(kotlin.math.abs(era.gpuPrice - 1))}")
        }
    }
}

@Composable
private fun ReputationCard(state: GameState) {
    val colors = gameColors
    Section("Reputation") {
        Meter("Score", state.reputation.score / 100, MaterialTheme.colorScheme.primary, "${state.reputation.score.toInt()}/100")
        if (state.reputation.scars.isEmpty()) {
            Hint("No scars. People trust you, for now.")
        } else {
            Hint("Scars follow you into every lane. Ship a predatory game and your social app launches to suspicion.")
        }
        for (scar in state.reputation.scars) {
            Column(Modifier.padding(top = 2.dp)) {
                Meter(scar.kind.title, scar.severity, colors.warn)
                Hint(scar.note)
            }
        }
    }
}
