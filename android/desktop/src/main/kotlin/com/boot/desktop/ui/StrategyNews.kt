package com.boot.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.AllianceKind
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.NewsDesk
import com.boot.core.Region
import com.boot.core.Research
import com.boot.core.exitVenture
import com.boot.core.exitVentureBlocker
import com.boot.core.followOnVenture
import com.boot.core.localizeOffice
import com.boot.core.localizeOfficeBlocker
import com.boot.core.marketCycleLabel
import com.boot.core.openOffice
import com.boot.core.openOfficeBlocker
import com.boot.core.seedTicket
import com.boot.core.seedVenture
import com.boot.core.seedVentureBlocker
import com.boot.core.signAlliance
import com.boot.core.signAllianceBlocker
import com.boot.desktop.DesktopStore

@Composable
fun StrategyPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val book = state.strategy
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassPanel {
            Text("Strategy", style = MaterialTheme.typography.titleLarge, color = h.accent)
            Hint("Offices, alliances, Venture Arm — same core as Android.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StratStat("Offices", "${book.offices.size}/${Region.entries.size}")
                StratStat("Territory", Format.money(book.lastTerritory) + "/wk")
                StratStat("Cycle", state.marketCycleLabel())
            }
            val marked = book.liveHoldings.sumOf { it.marketValue }
            val basis = book.liveHoldings.sumOf { it.basis }
            Hint("Venture · ${book.liveHoldings.size} live · ${Format.money(marked)} marked")
            if (basis > 0) {
                Hint("Paper P&L ${Format.signedMoney(marked - basis)}", if (marked >= basis) colors.good else colors.bad)
            }
        }
        GlassPanel {
            Text("Regional offices", style = MaterialTheme.typography.titleLarge, color = h.text)
            Region.entries.forEach { region ->
                val office = book.office(region)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(region.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                        if (office != null) {
                            Hint("Brand ${Format.percent(office.brand)} · share ${Format.percent(office.share)}" +
                                if (office.localized) " · localized" else "")
                        } else {
                            val blocker = state.openOfficeBlocker(region)
                            Hint(blocker ?: "Open a hub for territory revenue.")
                        }
                    }
                    if (office == null) {
                        MacButton(
                            "Open",
                            onClick = { store.perform { openOffice(region) } },
                            enabled = state.openOfficeBlocker(region) == null,
                            primary = false,
                        )
                    } else if (!office.localized) {
                        MacButton(
                            "Localize",
                            onClick = { store.perform { localizeOffice(region) } },
                            enabled = state.localizeOfficeBlocker(region) == null,
                            primary = false,
                        )
                    }
                }
            }
            if (Research.REGIONAL_EXPANSION !in state.researched && book.offices.isNotEmpty()) {
                Hint("Research regional expansion to open another office.", colors.warn)
            }
        }
        GlassPanel {
            Text("Alliances", style = MaterialTheme.typography.titleLarge, color = h.text)
            Hint("Two-year lane alliances.")
            AllianceKind.entries.forEach { kind ->
                state.lanes.forEach { lane ->
                    val blocker = state.signAllianceBlocker(kind, lane)
                    val active = book.alliances.any { it.kind == kind && it.lane == lane && it.active && it.expiresWeek > state.week }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Hint("${kind.title} · ${lane.title}")
                            if (active) Hint("Active", colors.good)
                            else if (blocker != null) Hint(blocker)
                        }
                        if (!active) {
                            MacButton(
                                "Sign",
                                onClick = { store.perform { signAlliance(kind, lane) } },
                                enabled = blocker == null,
                                primary = false,
                            )
                        }
                    }
                }
            }
        }
        GlassPanel {
            Text("Venture Arm", style = MaterialTheme.typography.titleLarge, color = h.text)
            var conviction by remember { mutableFloatStateOf(0.55f) }
            Hint("Seed conviction ${Format.percent(conviction.toDouble())}")
            Slider(value = conviction, onValueChange = { conviction = it }, valueRange = 0.2f..0.9f)
            Lane.entries.filter { it !in state.lanes }.forEach { lane ->
                val blocker = state.seedVentureBlocker(lane)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Hint("Seed ${lane.title}")
                        Hint(blocker ?: "Ticket ~${Format.money(state.seedTicket(lane))}")
                    }
                    MacButton(
                        "Seed",
                        onClick = { store.perform { seedVenture(lane, conviction.toDouble()) } },
                        enabled = blocker == null,
                        primary = false,
                    )
                }
            }
            book.liveHoldings.forEach { holding ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${holding.lane.title} · ${holding.stage.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = h.text,
                        )
                        Hint("Marked ${Format.money(holding.marketValue)} · basis ${Format.money(holding.basis)}")
                    }
                    MacButton(
                        "Follow-on",
                        onClick = { store.perform { followOnVenture(holding.id) } },
                        primary = false,
                    )
                    MacButton(
                        "Exit",
                        onClick = { store.perform { exitVenture(holding.id) } },
                        enabled = state.exitVentureBlocker(holding.id) == null,
                        primary = false,
                    )
                }
            }
        }
    }
}

@Composable
fun NewsScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    var desk by remember { mutableStateOf<NewsDesk?>(null) }
    LaunchedEffect(state.news.lastOrNull()?.id) {
        state.news.lastOrNull()?.let { store.newsSeen = maxOf(store.newsSeen, it.id) }
    }
    val stories = remember(state.news, desk) {
        val all = state.news.asReversed()
        if (desk == null) all else all.filter { it.desk == desk }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = WindowPad,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GlassPanel {
                Text("The Boot Times", style = MaterialTheme.typography.headlineMedium, color = h.ink)
                Hint("${state.dateLabel} · ${state.world.era.title}")
                Hint(state.world.era.blurb)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("All", desk == null) { desk = null }
                NewsDesk.entries.forEach { d ->
                    if (d == NewsDesk.GAMES && state.games == null) return@forEach
                    SelectChip(d.title, desk == d) { desk = d }
                }
            }
        }
        if (stories.isEmpty()) {
            item { Hint("No news yet. The press hasn't noticed you.") }
        }
        items(stories.take(40), key = { it.id }) { story ->
            GlassPanel(Modifier.fillMaxWidth()) {
                Hint("${GameClock.label(story.week)} · ${story.desk.title}")
                Text(story.headline, style = MaterialTheme.typography.titleMedium, color = h.text)
                Hint(story.body)
            }
        }
    }
}

@Composable
private fun StratStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = hudColors.textDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = hudColors.text)
    }
}
