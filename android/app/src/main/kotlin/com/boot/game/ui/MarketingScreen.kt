package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.AdBudget
import com.boot.core.CampaignLengths
import com.boot.core.Channel
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.awareness
import com.boot.core.brandLift
import com.boot.core.campaignBlocker
import com.boot.core.campaignCost
import com.boot.core.campaignFor
import com.boot.core.campaignForecast
import com.boot.core.channels
import com.boot.core.naturalAwareness
import com.boot.core.startCampaign
import com.boot.core.stopCampaign
import com.boot.game.GameStore

/** A business's brand: how well known it is, the campaign running, and how the last ones did. */
@Composable
fun MarketingSection(store: GameStore, state: GameState, lane: Lane) {
    val colors = gameColors
    val awareness = state.awareness(lane)
    val natural = state.naturalAwareness(lane)
    val lift = state.brandLift(lane)
    val running = state.campaignFor(lane)
    val results = state.marketing.results.filter { it.lane == lane }.takeLast(3).reversed()
    GroupedSection(
        "Marketing",
        footer = "Awareness has a natural level that grows with the business. Campaigns push it higher, and every point above lifts demand; " +
            "once they stop, people slowly forget.",
    ) {
        CardBlock(spacing = 10.dp) {
            Meter(
                "Brand awareness", awareness, colors.lane(lane),
                caption = Format.percent(awareness) + if (lift > 1.001) " · demand +${Format.percent(lift - 1)}" else "",
            )
            Hint("${Format.percent(natural)} on its own" + if (awareness > natural + 0.005) ", ${Format.percent(awareness - natural)} from marketing" else "")
        }
        RowDivider()
        if (running != null) {
            ListRow(
                running.channel.title, subtitle = "${running.budget.title} budget · ${Format.weeks(running.weeksLeft)} left · ${Format.money(running.weeklyCost)}/wk",
                value = Format.money(running.spent), valueDetail = "earned ~${Format.money(running.earned)}",
                valueDetailColor = if (running.earned >= running.spent) colors.good else colors.bad,
            )
            RowDivider()
            ActionRow("Stop early", "What's spent is spent. Awareness starts to fade.", "Stop") { store.perform { stopCampaign(lane) } }
        } else {
            ActionRow("Plan a campaign", "Pick a channel, a budget and how long it runs.", "Plan") { store.campaignLane = lane }
        }
        for (r in results) {
            RowDivider()
            ListRow(
                "${r.channel.title} · ${Format.weeks(r.weeks)}", subtitle = "Awareness ${Format.percent(r.from)} → ${Format.percent(r.to)}",
                value = Format.money(r.spent), valueDetail = "earned ~${Format.money(r.earned)}",
                valueDetailColor = if (r.earned >= r.spent) colors.good else colors.bad,
            )
        }
    }
}

/** Plan a campaign: channel, budget, length, and what it should do. */
@Composable
fun CampaignSheet(store: GameStore, state: GameState, lane: Lane) {
    val colors = gameColors
    val channels = state.channels()
    var channel by rememberSaveable { mutableStateOf(channels.maxByOrNull { it.fit(lane) } ?: Channel.PRINT) }
    var budget by rememberSaveable { mutableStateOf(AdBudget.SMALL) }
    var weeks by rememberSaveable { mutableIntStateOf(8) }
    val close = { store.campaignLane = null }
    GameDialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close") }
                    Text("Campaign for your ${lane.businessName.lowercase()}", style = MaterialTheme.typography.titleMedium)
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    GroupedSection("Channel") {
                        channels.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            val on = c == channel
                            ListRow(
                                c.title, subtitle = "${c.blurb}\n${c.fitLabel(lane)} · from ${Format.money(state.campaignCost(c, AdBudget.SMALL))}/wk",
                                onClick = { channel = c },
                                trailing = {
                                    Icon(
                                        if (on) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                                        tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )
                        }
                    }
                    GroupedSection("Budget", footer = "Bigger budgets reach more people, but each dollar does a little less.") {
                        Column(Modifier.padding(8.dp)) {
                            Segmented(AdBudget.entries, budget, { "${it.title} · ${Format.money(state.campaignCost(channel, it))}" }) { budget = it }
                        }
                    }
                    GroupedSection("Length") {
                        Column(Modifier.padding(8.dp)) { Segmented(CampaignLengths, weeks, { "$it weeks" }) { weeks = it } }
                    }
                }
                val forecast = state.campaignForecast(lane, channel, budget, weeks)
                val total = state.campaignCost(channel, budget) * weeks
                val blocker = state.campaignBlocker(lane, channel, budget, weeks)
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Hint(
                        "Awareness ${Format.percent(state.awareness(lane))} → about ${Format.percent(forecast)}, " +
                            "demand +${Format.percent((forecast - state.naturalAwareness(lane)) * 0.5)} at the end. ${Format.money(total)} in all.",
                    )
                    if (blocker != null) Hint(blocker, colors.bad)
                    PrimaryButton("Launch campaign", {
                        store.perform { startCampaign(lane, channel, budget, weeks) }
                        close()
                    }, Modifier.fillMaxWidth(), enabled = blocker == null)
                    Spacer(Modifier)
                }
            }
        }
    }
}
