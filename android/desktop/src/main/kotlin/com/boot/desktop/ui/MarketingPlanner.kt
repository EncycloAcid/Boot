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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.AdBudget
import com.boot.core.Channel
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.awareness
import com.boot.core.campaignBlocker
import com.boot.core.campaignCost
import com.boot.core.campaignForecast
import com.boot.core.campaignFor
import com.boot.core.channels
import com.boot.core.startCampaign
import com.boot.core.stopCampaign
import com.boot.desktop.DesktopStore

@Composable
fun MarketingPlanner(store: DesktopStore, state: GameState, lane: Lane) {
    val h = hudColors
    val colors = gameColors
    val available = remember(state.week) { state.channels() }
    var channel by remember(available) { mutableStateOf(available.firstOrNull() ?: Channel.PRINT) }
    var budget by remember { mutableStateOf(AdBudget.MEDIUM) }
    var weeks by remember { mutableIntStateOf(8) }
    val active = state.campaignFor(lane)
    Dialog(
        onDismissRequest = { store.campaignLane = null },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 380.dp, max = 520.dp)
                    .fillMaxWidth(0.5f)
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp)),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Marketing · ${lane.title}", style = MaterialTheme.typography.titleLarge, color = h.ink)
                        Hint("Awareness ${Format.percent(state.awareness(lane))}")
                    }
                    MacButton("Close", onClick = { store.campaignLane = null }, primary = false)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (active != null) {
                        GlassPanel {
                            Text("Running · ${active.channel.title}", style = MaterialTheme.typography.titleMedium, color = h.accent)
                            Hint("${active.budget.title} · ${active.weeksLeft}w left · ${Format.money(active.weeklyCost)}/wk")
                            MacButton(
                                "Stop campaign",
                                onClick = { store.perform { stopCampaign(lane) } },
                                primary = false,
                            )
                        }
                    } else {
                        GlassPanel {
                            Text("Channel", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                            available.forEach { c ->
                                SelectChip("${c.title} · ${c.fitLabel(lane)}", channel == c) { channel = c }
                            }
                            Hint(channel.blurb)
                        }
                        GlassPanel {
                            Text("Budget", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdBudget.entries.forEach { b ->
                                    SelectChip(b.title, budget == b) { budget = b }
                                }
                            }
                            Hint("Weeks")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(4, 8, 12).forEach { w ->
                                    SelectChip("${w}w", weeks == w) { weeks = w }
                                }
                            }
                            val cost = state.campaignCost(channel, budget)
                            val forecast = state.campaignForecast(lane, channel, budget, weeks)
                            Hint("${Format.money(cost)}/wk · forecast awareness ~${Format.percent(forecast)}")
                            val blocker = state.campaignBlocker(lane, channel, budget, weeks)
                            if (blocker != null) Hint(blocker, colors.bad)
                            MacButton(
                                "Run campaign",
                                onClick = {
                                    store.perform { startCampaign(lane, channel, budget, weeks) }
                                    store.campaignLane = null
                                },
                                enabled = blocker == null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    store.error?.let { Hint(it, colors.bad) }
                    Spacer(Modifier.heightIn(min = 8.dp))
                }
            }
        }
    }
}

/** Compact awareness row for lane panels. */
@Composable
fun LaneMarketingRow(store: DesktopStore, state: GameState, lane: Lane) {
    val active = state.campaignFor(lane)
    GlassPanel {
        Text("Marketing", style = MaterialTheme.typography.titleMedium, color = hudColors.accent)
        Hint("Awareness ${Format.percent(state.awareness(lane))}" + (active?.let { " · ${it.channel.title} ${it.weeksLeft}w" } ?: ""))
        MacButton(
            if (active != null) "Manage campaign" else "Plan a campaign",
            onClick = { store.campaignLane = lane },
            primary = active == null,
        )
    }
}
