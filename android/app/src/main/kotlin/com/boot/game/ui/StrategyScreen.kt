package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.AllianceKind
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Region
import com.boot.core.Research
import com.boot.core.VentureStage
import com.boot.core.exitVenture
import com.boot.core.exitVentureBlocker
import com.boot.core.followOnBlocker
import com.boot.core.followOnTicket
import com.boot.core.followOnVenture
import com.boot.core.localizeOffice
import com.boot.core.localizeOfficeBlocker
import com.boot.core.openOffice
import com.boot.core.openOfficeBlocker
import com.boot.core.seedTicket
import com.boot.core.seedVenture
import com.boot.core.seedVentureBlocker
import com.boot.core.signAlliance
import com.boot.core.signAllianceBlocker
import com.boot.game.GameStore

/** Company → Strategy: regional offices, alliances, and the Venture Arm. */
fun LazyListScope.strategyItems(store: GameStore, state: GameState) {
    item { StrategyPulse(state) }
    item { RegionalOfficesSection(store, state) }
    item { AlliancesSection(store, state) }
    item { VentureArmSection(store, state) }
}

@Composable
private fun StrategyPulse(state: GameState) {
    val colors = gameColors
    val book = state.strategy
    GroupedSection(
        "Strategy",
        footer = "Offices grow local brand and territory revenue. Alliances last two years. The Venture Arm seeds minority stakes you can follow on and exit.",
    ) {
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Offices", "${book.offices.size}", Modifier.weight(1f), "of ${Region.entries.size}")
                StatTile(
                    "Territory",
                    Format.money(book.lastTerritory),
                    Modifier.weight(1f),
                    "a week",
                    if (book.lastTerritory > 0) colors.good else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatTile(
                    "Alliances",
                    "${book.activeAlliances.size}",
                    Modifier.weight(1f),
                    Format.money(book.lastAllianceYield) + "/wk",
                )
            }
        }
        RowDivider()
        ValueRow(
            "Venture Arm",
            "${book.liveHoldings.size} live · ${Format.money(book.liveHoldings.sumOf { it.marketValue })} marked",
        )
    }
}

@Composable
private fun RegionalOfficesSection(store: GameStore, state: GameState) {
    val colors = gameColors
    val needsResearch = Research.REGIONAL_EXPANSION !in state.researched && state.strategy.offices.isNotEmpty()
    GroupedSection(
        "Regional offices",
        footer = when {
            needsResearch -> "Research regional expansion to open another office."
            state.strategy.offices.isEmpty() -> "Open a hub to grow local brand, share, and recurring territory revenue."
            else -> "Localize each region to accelerate brand and share."
        },
    ) {
        Region.entries.forEachIndexed { i, region ->
            if (i > 0) RowDivider(68.dp)
            val office = state.strategy.office(region)
            if (office == null) {
                val blocker = state.openOfficeBlocker(region)
                ListRow(
                    region.title,
                    subtitle = "${Format.money(region.openCost)} to open · ${region.short}",
                    value = "Open",
                    valueColor = if (blocker == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    onClick = { if (blocker == null) store.perform { openOffice(region) } else store.error = blocker },
                )
            } else {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(region.title, style = MaterialTheme.typography.titleMedium)
                    Hint("Opened ${GameClock.label(office.openedWeek)}" + if (office.localized) " · Localized" else "")
                    Meter("Local brand", office.brand, colors.good)
                    Meter("Market share", office.share, MaterialTheme.colorScheme.primary)
                    Text(
                        "${Format.money(office.weeklyRevenue)} a week from territory",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (office.weeklyRevenue > 0) colors.good else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!office.localized) {
                        val blocker = state.localizeOfficeBlocker(region)
                        PrimaryButton(
                            "Localize · ${Format.money(region.localizeCost)}",
                            { if (blocker == null) store.perform { localizeOffice(region) } else store.error = blocker },
                            Modifier.fillMaxWidth(),
                            enabled = blocker == null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlliancesSection(store: GameStore, state: GameState) {
    val colors = gameColors
    var kind by remember { mutableStateOf(AllianceKind.DISTRIBUTION) }
    var lane by remember { mutableStateOf(state.lanes.firstOrNull() ?: Lane.GAMES) }
    val researched = Research.GLOBAL_ALLIANCES in state.researched
    GroupedSection(
        "Alliances",
        footer = if (!researched) {
            "Research global alliances to sign two-year, lane-specific partnerships."
        } else {
            "Distribution, cloud, content, or platform — trust and value grow with the market cycle."
        },
    ) {
        if (state.strategy.activeAlliances.isEmpty()) {
            CardBlock { Hint(if (researched) "No live alliances yet." else "Locked until researched.") }
        } else {
            state.strategy.activeAlliances.forEachIndexed { i, a ->
                if (i > 0) RowDivider(68.dp)
                ListRow(
                    a.partner,
                    subtitle = "${a.kind.title} · ${a.lane.title} · ${a.weeksLeft(state.week)}w left",
                    value = Format.money(a.value),
                    valueDetail = "trust ${Format.percent(a.trust)}",
                    valueColor = if (a.trust >= 0.5) colors.good else colors.warn,
                )
            }
        }
        if (researched && state.lanes.isNotEmpty()) {
            RowDivider()
            CardBlock(spacing = 10.dp) {
                Text("New alliance", style = MaterialTheme.typography.titleSmall)
                Segmented(AllianceKind.entries, kind, { it.title }) { kind = it }
                Hint(kind.detail)
                Segmented(state.lanes, lane, { it.title }) { lane = it }
                val blocker = state.signAllianceBlocker(kind, lane)
                PrimaryButton(
                    "Sign · ${Format.money(kind.signCost)}",
                    { if (blocker == null) store.perform { signAlliance(kind, lane) } else store.error = blocker },
                    Modifier.fillMaxWidth(),
                    enabled = blocker == null,
                )
                if (blocker != null) Hint(blocker, colors.bad)
            }
        }
    }
}

@Composable
private fun VentureArmSection(store: GameStore, state: GameState) {
    val colors = gameColors
    var lane by remember { mutableStateOf(state.lanes.firstOrNull() ?: Lane.GAMES) }
    val researched = Research.VENTURE_ARM in state.researched
    GroupedSection(
        "Venture Arm",
        footer = if (!researched) {
            "Research Venture Arm to seed minority investments across your open lanes."
        } else {
            "Seed, follow on from Seed to Series A, and exit when the cycle is right. Marks move with the market."
        },
    ) {
        val live = state.strategy.liveHoldings
        val exited = state.strategy.holdings.filter { it.exited }.takeLast(3)
        if (live.isEmpty() && exited.isEmpty()) {
            CardBlock { Hint(if (researched) "No holdings yet. Seed a startup in one of your lanes." else "Locked until researched.") }
        }
        live.forEachIndexed { i, h ->
            if (i > 0) RowDivider(68.dp)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(h.name, style = MaterialTheme.typography.titleMedium)
                Hint("${h.lane.title} · ${h.stage.title} · ${Format.percent(h.ownership)} owned · conviction ${Format.percent(h.conviction)}")
                ValueRow("Basis", Format.money(h.basis))
                RowDivider()
                ValueRow(
                    "Marked value",
                    Format.money(h.marketValue),
                    if (h.paperGain >= 0) colors.good else colors.bad,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (h.stage == VentureStage.SEED) {
                        val fb = state.followOnBlocker(h.id)
                        TonalButton(
                            "Follow-on · ${Format.money(state.followOnTicket(h))}",
                            { if (fb == null) store.perform { followOnVenture(h.id) } else store.error = fb },
                            enabled = fb == null,
                        )
                    }
                    val eb = state.exitVentureBlocker(h.id)
                    PlainButton(
                        "Exit",
                        { if (eb == null) store.perform { exitVenture(h.id) } else store.error = eb },
                    )
                }
            }
        }
        if (exited.isNotEmpty()) {
            RowDivider()
            exited.forEachIndexed { i, h ->
                if (i > 0) RowDivider(68.dp)
                ListRow(
                    h.name,
                    subtitle = "Exited ${h.exitWeek?.let { GameClock.label(it) } ?: ""} · ${h.lane.title}",
                    value = Format.money(h.exitProceeds),
                    valueDetail = "on ${Format.money(h.basis)} basis",
                    valueColor = if (h.exitProceeds >= h.basis) colors.good else colors.bad,
                )
            }
        }
        if (researched && state.lanes.isNotEmpty()) {
            RowDivider()
            CardBlock(spacing = 10.dp) {
                Text("Seed a startup", style = MaterialTheme.typography.titleSmall)
                Segmented(state.lanes, lane, { it.title }) { lane = it }
                val ticket = state.seedTicket(lane)
                val blocker = state.seedVentureBlocker(lane)
                PrimaryButton(
                    "Seed · ${Format.money(ticket)}",
                    { if (blocker == null) store.perform { seedVenture(lane) } else store.error = blocker },
                    Modifier.fillMaxWidth(),
                    enabled = blocker == null,
                )
                if (blocker != null) Hint(blocker, colors.bad)
            }
        }
    }
}
