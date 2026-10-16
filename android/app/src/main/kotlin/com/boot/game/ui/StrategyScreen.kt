package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.boot.core.VentureHolding
import com.boot.core.VentureStage
import com.boot.core.exitQuote
import com.boot.core.exitVenture
import com.boot.core.exitVentureBlocker
import com.boot.core.followOnBlocker
import com.boot.core.followOnTicket
import com.boot.core.followOnVenture
import com.boot.core.followOnWeeksLeft
import com.boot.core.localizeOffice
import com.boot.core.localizeOfficeBlocker
import com.boot.core.marketCycle
import com.boot.core.marketCycleLabel
import com.boot.core.openOffice
import com.boot.core.openOfficeBlocker
import com.boot.core.seedOwnershipPreview
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
    val cycle = state.marketCycle()
    val cycleLabel = state.marketCycleLabel()
    val marked = book.liveHoldings.sumOf { it.marketValue }
    val basis = book.liveHoldings.sumOf { it.basis }
    GroupedSection(
        "Strategy",
        footer = "Offices grow territory. Alliances last two years. Venture Arm marks and exits lean on the market cycle ($cycleLabel).",
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
                    "Cycle",
                    cycleLabel,
                    Modifier.weight(1f),
                    String.format(java.util.Locale.US, "%.0f%% appetite", cycle * 100),
                    when {
                        cycle >= 1.1 -> colors.good
                        cycle <= 0.8 -> colors.bad
                        else -> colors.warn
                    },
                )
            }
        }
        RowDivider()
        ValueRow(
            "Venture Arm",
            "${book.liveHoldings.size} live · ${Format.money(marked)} marked",
            if (marked >= basis) colors.good else if (basis > 0) colors.bad else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (basis > 0) {
            RowDivider()
            ValueRow("Paper P&L", Format.signedMoney(marked - basis), if (marked >= basis) colors.good else colors.bad)
        }
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
    var lane by remember(state.lanes) { mutableStateOf(state.lanes.firstOrNull() ?: Lane.GAMES) }
    if (lane !in state.lanes && state.lanes.isNotEmpty()) lane = state.lanes.first()
    var conviction by remember { mutableFloatStateOf(0.55f) }
    val researched = Research.VENTURE_ARM in state.researched
    GroupedSection(
        "Venture Arm",
        footer = if (!researched) {
            "Research Venture Arm to seed minority investments in any open lane."
        } else {
            "Pick a live lane, set conviction, seed a minority stake. Follow on Seed→Series A after six months. Exit turns marks into cash — cycle ${state.marketCycleLabel().lowercase()} right now."
        },
    ) {
        val live = state.strategy.liveHoldings
        val exited = state.strategy.holdings.filter { it.exited }.takeLast(4)
        if (live.isEmpty() && exited.isEmpty()) {
            CardBlock { Hint(if (researched) "No holdings yet. Seed a startup in one of your open lanes." else "Locked until researched.") }
        }
        live.forEachIndexed { i, h ->
            if (i > 0) RowDivider(68.dp)
            HoldingCard(store, state, h)
        }
        if (exited.isNotEmpty()) {
            RowDivider()
            Text(
                "Recent exits",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
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
                Hint("Any active lane — ${state.lanes.joinToString { it.title }}.")
                Segmented(state.lanes, lane, { it.title }) { lane = it }
                Text("Conviction", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = conviction,
                    onValueChange = { conviction = it },
                    valueRange = 0.25f..0.95f,
                )
                val ticket = state.seedTicket(lane)
                val own = state.seedOwnershipPreview(conviction.toDouble())
                Hint(
                    "${Format.money(ticket)} cheque · about ${Format.percent(own)} ownership · " +
                        "marks move with a ${state.marketCycleLabel().lowercase()} cycle",
                )
                val blocker = state.seedVentureBlocker(lane)
                PrimaryButton(
                    "Seed in ${lane.title} · ${Format.money(ticket)}",
                    {
                        if (blocker == null) store.perform { seedVenture(lane, conviction.toDouble()) }
                        else store.error = blocker
                    },
                    Modifier.fillMaxWidth(),
                    enabled = blocker == null,
                )
                if (blocker != null) Hint(blocker, colors.bad)
            }
        }
    }
}

@Composable
private fun HoldingCard(store: GameStore, state: GameState, h: VentureHolding) {
    val colors = gameColors
    val quote = state.exitQuote(h)
    val followLeft = state.followOnWeeksLeft(h)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(h.name, style = MaterialTheme.typography.titleMedium)
            Pill(h.stage.title, if (h.stage == VentureStage.SERIES_A) colors.good else MaterialTheme.colorScheme.primary)
        }
        Hint("${h.lane.title} · seeded ${GameClock.label(h.seededWeek)}")
        Meter("Ownership", h.ownership / 0.28, colors.good, caption = Format.percent(h.ownership))
        Meter("Conviction", h.conviction, MaterialTheme.colorScheme.primary)
        ValueRow("Basis", Format.money(h.basis))
        RowDivider()
        ValueRow("Company valuation", Format.money(h.valuation))
        RowDivider()
        ValueRow(
            "Your mark",
            Format.money(h.marketValue),
            if (h.paperGain >= 0) colors.good else colors.bad,
        )
        RowDivider()
        ValueRow(
            "Exit quote now",
            Format.money(quote),
            if (quote >= h.basis) colors.good else colors.bad,
        )
        Hint(
            when {
                h.stage == VentureStage.SEED && followLeft > 0 -> "Series A follow-on in ${followLeft}w · cycle ${state.marketCycleLabel().lowercase()}"
                h.stage == VentureStage.SEED -> "Ready for Series A follow-on"
                else -> "Series A · exit when the ${state.marketCycleLabel().lowercase()} cycle suits you"
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (h.stage == VentureStage.SEED) {
                val fb = state.followOnBlocker(h.id)
                TonalButton(
                    if (followLeft > 0) "Follow-on in ${followLeft}w" else "Follow-on · ${Format.money(state.followOnTicket(h))}",
                    { if (fb == null) store.perform { followOnVenture(h.id) } else store.error = fb },
                    enabled = fb == null,
                )
            }
            val eb = state.exitVentureBlocker(h.id)
            PlainButton(
                "Exit · ${Format.money(quote)}",
                { if (eb == null) store.perform { exitVenture(h.id) } else store.error = eb },
                enabled = eb == null,
            )
        }
        state.exitVentureBlocker(h.id)?.takeIf { h.stage != VentureStage.SEED || followLeft == 0 }?.let {
            Hint(it, colors.warn)
        }
    }
}
