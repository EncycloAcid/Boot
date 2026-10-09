package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.boot.core.AIProduct
import com.boot.core.AppMonetization
import com.boot.core.DeviceCategory
import com.boot.core.DeviceSpec
import com.boot.core.DeviceTier
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameScope
import com.boot.core.GameState
import com.boot.core.HardwareLab
import com.boot.core.Lane
import com.boot.core.ScopePolicy
import com.boot.core.TrainingSize
import com.boot.core.UpdateStyle
import com.boot.core.acceptContract
import com.boot.core.buyGPUs
import com.boot.core.cancelGame
import com.boot.core.declineContract
import com.boot.core.gpuCost
import com.boot.core.launchDevice
import com.boot.core.launchGame
import com.boot.core.orderUnits
import com.boot.core.setAIFocus
import com.boot.core.setAdLoad
import com.boot.core.setModeration
import com.boot.core.setMonetization
import com.boot.core.setScopePolicy
import com.boot.core.setUpdateStyle
import com.boot.core.startDevice
import com.boot.core.startGame
import com.boot.core.startTraining
import com.boot.game.GameStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanesScreen(store: GameStore, state: GameState) {
    val lane = store.lane?.takeIf { it in state.lanes } ?: state.lanes.first()
    Column {
        if (state.lanes.size > 1) {
            PrimaryScrollableTabRow(selectedTabIndex = state.lanes.indexOf(lane), edgePadding = 16.dp) {
                state.lanes.forEach { l ->
                    Tab(
                        selected = l == lane,
                        onClick = { store.lane = l },
                        text = { Text("${l.emoji} ${l.title}") },
                    )
                }
            }
        }
        ScreenColumn {
            item { LaneHeader(lane) }
            item { LaneTiles(state, lane) }
            when (lane) {
                Lane.GAMES -> gamesItems(store, state)
                Lane.WEBSITES -> webItems(store, state)
                Lane.APPS -> appsItems(store, state)
                Lane.SOCIAL -> socialItems(store, state)
                Lane.HARDWARE -> hardwareItems(store, state)
                Lane.AI -> aiItems(store, state)
            }
        }
    }
}

@Composable
private fun LaneTiles(state: GameState, lane: Lane) {
    val colors = gameColors
    val profit = state.trailingProfit(lane, 4) / 4
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TileRow {
            StatTile("Revenue", (state.trailingRevenue(lane, 4) / 4).perWeek(), Modifier.weight(1f), "4-week average")
            StatTile("Profit", profit.signedPerWeek(), Modifier.weight(1f), "after this lane's payroll", if (profit >= 0) colors.good else colors.bad)
        }
        TileRow {
            StatTile(
                "Team", "${state.staff(lane).size}", Modifier.weight(1f),
                "plus ${Format.percent(state.founder.attentionShare(lane, state.lanes))} of you",
            )
            StatTile(
                "Market", Format.percent(state.world.era.demand(lane) * state.world.competition(lane)), Modifier.weight(1f),
                state.world.rival(lane)?.let { "vs ${it.name}" },
            )
        }
    }
}

// Games

private fun LazyListScope.gamesItems(store: GameStore, state: GameState) {
    val g = state.games ?: return
    item {
        Section("In development") {
            val p = g.project
            if (p != null) {
                Text(p.title, style = MaterialTheme.typography.titleLarge)
                Hint("${p.scope.title} · ${p.weeksInDev} weeks in · ${p.scope.weeklyBudget.perWeek()} budget")
                Meter("Content", p.completion)
                Meter(
                    "Polish", (p.polish / (p.scope.work * 1.5)).coerceAtMost(1.0), gameColors.milestone,
                    if (p.isReady) "Keep polishing for a better review" else null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { store.perform { launchGame() } }, enabled = p.isReady) { Text("Launch") }
                    TextButton(onClick = { store.perform { cancelGame() } }) { Text("Cancel project", color = gameColors.bad) }
                }
            } else {
                Hint("Nothing in development. What's next?")
                GameScope.entries.forEach { scope ->
                    ActionRow(
                        title = scope.title,
                        detail = "${scope.work.toInt()} work · ${scope.weeklyBudget.perWeek()} budget",
                        action = "Start",
                    ) { store.perform { startGame(scope) } }
                }
            }
        }
    }
    item {
        Section("Fanbase") {
            KeyValue("Fans", Format.count(g.fans))
            Meter(
                "Fan trust", g.fanTrust / 1.2, if (g.fanTrust < 0.7) gameColors.warn else gameColors.good,
                String.format(java.util.Locale.US, "%.2f", g.fanTrust),
            )
            KeyValue("Hits", "${g.hits}")
            KeyValue("Flops", "${g.flops}")
            Hint("Flops knock fan trust down and only hits bring it back. The fanbase never forgets.")
        }
    }
    item {
        Section("Releases") {
            if (g.releases.isEmpty()) Hint("No games shipped yet.")
            g.releases.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(6.dp))
                            Pill(
                                r.verdict,
                                when {
                                    r.review >= 85 -> gameColors.good
                                    r.review < 55 -> gameColors.bad
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Hint("Review ${r.review} · ${Format.money(r.lifetimeSales)} lifetime")
                    }
                    Text(r.weeklySales.perWeek(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// Websites

private fun LazyListScope.webItems(store: GameStore, state: GameState) {
    val w = state.web ?: return
    item {
        Section("New briefs") {
            if (w.offers.isEmpty()) Hint("No offers this week. Standing and marketers bring more.")
            w.offers.forEach { o ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(o.client, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(Format.money(o.value), style = MaterialTheme.typography.titleSmall)
                    }
                    Hint("${o.work.toInt()} work · due in ${o.weeksAllowed} weeks · 40% upfront")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { store.perform { acceptContract(o.id) } }) { Text("Accept") }
                        OutlinedButton(onClick = { store.perform { declineContract(o.id) } }) { Text("Pass") }
                    }
                }
            }
        }
    }
    item {
        Section("Agency") {
            Meter("Standing", w.standing, gameColors.good)
            if (w.clientWariness > 0.05) Meter("Client wariness", w.clientWariness, gameColors.warn)
            KeyValue("Projects delivered", "${w.completed}")
            Text("When the brief changes at 11pm", style = MaterialTheme.typography.bodyMedium)
            ScopePolicy.entries.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = w.policy == p, onClick = { store.perform { setScopePolicy(p) } })
                    Text(p.title, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Hint("Absorbing keeps clients happy and margins thin. Billing pays, but clients bristle and some walk.")
        }
    }
    item {
        Section("Active projects") {
            if (w.active.isEmpty()) Hint("Nothing on the go. Accept a brief.")
            w.active.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.client, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (c.isOverdue) Pill("OVERDUE", gameColors.bad)
                        Spacer(Modifier.width(6.dp))
                        Hint("week ${c.weeksElapsed}/${c.weeksAllowed}")
                    }
                    if (c.creepCount > 0) Hint("${c.creepCount} brief change${if (c.creepCount == 1) "" else "s"}", gameColors.warn)
                    Meter("Done", c.completion)
                    Meter("Client happiness", c.satisfaction, if (c.satisfaction < 0.35) gameColors.bad else gameColors.good)
                }
            }
        }
    }
}

// Apps

private fun LazyListScope.appsItems(store: GameStore, state: GameState) {
    val a = state.apps ?: return
    item {
        Section(a.name) {
            if (!a.isLive) {
                Hint("Building version 1.0")
                Meter("Progress", (a.buildProgress / a.buildWork).coerceAtMost(1.0))
            } else {
                TileRow {
                    StatTile("Users", Format.count(a.users), Modifier.weight(1f))
                    StatTile("Store rank", a.rankLabel, Modifier.weight(1f))
                }
                TileRow {
                    StatTile(
                        "Churn", Format.percent(a.lastChurn, 1), Modifier.weight(1f), "per week",
                        if (a.lastChurn > 0.08) gameColors.bad else Color.Unspecified,
                    )
                    StatTile("Installs", Format.count(a.lastInstalls), Modifier.weight(1f), "this week")
                }
                Meter("Quality", a.quality / 100, gameColors.milestone, "${a.quality.toInt()}")
                Meter(
                    "Next update", (a.updateProgress / a.updateStyle.work).coerceAtMost(1.0),
                    caption = "${a.weeksSinceUpdate} weeks since last update",
                )
            }
        }
    }
    item {
        Section("Strategy") {
            Text("Monetization", style = MaterialTheme.typography.bodyMedium)
            Segmented(AppMonetization.entries, a.monetization, { it.shortLabel }) { m -> store.perform { setMonetization(m) } }
            Hint(a.monetization.title)
            Text("Updates", style = MaterialTheme.typography.bodyMedium)
            Segmented(UpdateStyle.entries, a.updateStyle, { if (it == UpdateStyle.SMALL) "Small & often" else "Big & rare" }) { u ->
                store.perform { setUpdateStyle(u) }
            }
            Hint("Stale apps churn. Ship often to stay fresh, or ship big to raise quality.")
        }
    }
    if (a.isLive) {
        item {
            Section("Users") { AreaChart(a.userHistory, gameColors.lane(Lane.APPS), Format::count) }
        }
        item {
            Section("Weekly churn") { LineChart(a.churnHistory, gameColors.bad, { Format.percent(it, 1) }) }
        }
    }
}

private val AppMonetization.shortLabel: String
    get() = when (this) {
        AppMonetization.ADS -> "Ads"
        AppMonetization.SUBSCRIPTION -> "Subs"
        AppMonetization.PAID -> "Paid"
    }

// Social

private fun LazyListScope.socialItems(store: GameStore, state: GameState) {
    val s = state.social ?: return
    item {
        Section(s.name) {
            if (!s.isLive) {
                Hint("Building the platform")
                Meter("Progress", (s.buildProgress / s.buildWork).coerceAtMost(1.0))
            } else {
                TileRow {
                    StatTile("Accounts", Format.count(s.users), Modifier.weight(1f))
                    StatTile("Real people", Format.count(s.realUsers), Modifier.weight(1f))
                    StatTile(
                        "Growth", Format.percent(s.lastGrowth, 1), Modifier.weight(1f), "per week",
                        if (s.lastGrowth > 0) gameColors.good else gameColors.bad,
                    )
                }
                if (s.caughtOn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = gameColors.warn)
                        Spacer(Modifier.width(6.dp))
                        Text("It caught on.", color = gameColors.warn)
                    }
                } else {
                    Hint("Slow start. If it catches on (around 250k users), growth goes exponential.")
                }
                Meter("Bots", s.botShare / 0.6, gameColors.warn, Format.percent(s.botShare))
                Meter("Toxicity", s.toxicity, gameColors.bad)
            }
        }
    }
    item {
        Section("Policy") {
            CommitSlider(s.moderation, { "Moderation ${Format.percent(it.toDouble())}" }) { v -> store.perform { setModeration(v) } }
            Hint("Costs money per user. Keeps bots, fires and scandals down.")
            CommitSlider(s.adLoad, { "Ad load ${Format.percent(it.toDouble())}" }) { v -> store.perform { setAdLoad(v) } }
            Hint("Revenue now versus growth. Ads pay little until you're big.")
        }
    }
    if (s.isLive) {
        item { Section("Accounts") { AreaChart(s.userHistory, gameColors.lane(Lane.SOCIAL), Format::count) } }
    }
}

// Hardware

private fun LazyListScope.hardwareItems(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    item { DesignCard(store, state) }
    item { FactoryCard(store, state) }
    item {
        Section("On the market") {
            val m = h.onMarket
            if (m != null) {
                Text(m.name, style = MaterialTheme.typography.titleLarge)
                Hint("${Format.money(m.spec.price)} · reviewed ${m.quality.toInt()}")
                TileRow {
                    StatTile("Demand", "${Format.count(h.lastDemand)}/wk", Modifier.weight(1f))
                    StatTile(
                        "In stock", Format.count(h.inventory.toDouble()), Modifier.weight(1f),
                        tint = if (h.inventory < h.lastDemand) gameColors.warn else Color.Unspecified,
                    )
                    StatTile("Sold", Format.count(m.unitsSold.toDouble()), Modifier.weight(1f))
                }
            } else {
                Hint("Nothing on sale yet.")
            }
            Meter("Brand awareness", h.brandAwareness, MaterialTheme.colorScheme.primary)
            KeyValue("Models shipped", "${h.modelsShipped}")
        }
    }
}

@Composable
private fun DesignCard(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    var category by rememberSaveable { mutableStateOf(DeviceCategory.HEADPHONES) }
    var tier by rememberSaveable { mutableStateOf(DeviceTier.BUDGET) }
    Section("Design") {
        val d = h.design
        if (d != null) {
            Text(d.name, style = MaterialTheme.typography.titleLarge)
            Hint("${Format.money(d.spec.price)} retail · ${Format.money(d.spec.unitCost)} to build · ${d.spec.tier.weeklyRnD.perWeek()} R&D")
            Meter("Design", d.completion)
            KeyValue("Stock booked for launch", "${d.preorderedStock} units")
            Button(onClick = { store.perform { launchDevice() } }, enabled = d.isReady) { Text("Launch the ${d.name}") }
            if (d.isReady && d.preorderedStock == 0) Hint("No stock has arrived yet. Launch now and you'll sell out on day one.", gameColors.warn)
        } else {
            Hint("Phones, laptops or headphones. Budget sells more units; flagship earns more per unit.")
            Segmented(DeviceCategory.entries, category, { it.title }) { category = it }
            Segmented(DeviceTier.entries, tier, { it.title }) { tier = it }
            val spec = DeviceSpec(category, tier)
            Hint("${Format.money(spec.price)} retail, ${Format.money(spec.unitCost)} per unit, ${tier.weeklyRnD.perWeek()} R&D")
            Button(onClick = { store.perform { startDevice(category, tier) } }) { Text("Start design") }
        }
    }
}

@Composable
private fun FactoryCard(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    var units by rememberSaveable { mutableIntStateOf(5_000) }
    val spec = h.design?.spec ?: h.onMarket?.spec
    Section("Factory") {
        Hint(
            "Lead time ${HardwareLab.LEAD_TIME_WEEKS} weeks. Guess low and scalpers profit; " +
                "guess high and you're stuck with last year's model.",
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { units = (units - 1_000).coerceAtLeast(100) }) { Text("−1k") }
            OutlinedTextField(
                value = units.toString(),
                onValueChange = { text -> text.filter(Char::isDigit).take(8).toIntOrNull()?.let { units = it } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Units") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedButton(onClick = { units += 1_000 }) { Text("+1k") }
        }
        if (spec != null) Hint("Costs ${Format.money(units * spec.unitCost)} up front")
        Button(
            onClick = { store.perform { orderUnits(units) } },
            enabled = spec != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Book factory capacity") }
        h.orders.forEach { o -> KeyValue("${o.units} × ${o.model}", "arrives ${GameClock.label(o.arrivalWeek)}") }
        if (state.week < h.productionHaltedUntil) {
            Hint("Production halted until ${GameClock.label(h.productionHaltedUntil)}", gameColors.bad)
        }
    }
}

// AI

private fun LazyListScope.aiItems(store: GameStore, state: GameState) {
    val lab = state.ai ?: return
    item {
        Section("Models") {
            TileRow {
                StatTile("Best model", lab.modelName ?: "None", Modifier.weight(1f))
                StatTile("Capability", "${lab.capability.toInt()}", Modifier.weight(1f))
                StatTile("Failed runs", "${lab.failedRuns}", Modifier.weight(1f))
            }
            val t = lab.training
            if (t != null) {
                Text("Training ${t.codename} (${t.size.title.lowercase()})", style = MaterialTheme.typography.titleSmall)
                Meter("Run", t.completion, caption = "${t.weeksDone}/${t.size.weeks} weeks · ${state.trainingWeeklyCost(t.size).perWeek()}")
                Hint("You won't know how good it is until it finishes.")
            } else {
                TrainingSize.entries.forEach { size ->
                    ActionRow(
                        title = size.title,
                        detail = "${size.weeks} weeks · ${state.trainingWeeklyCost(size).perWeek()} · tops out near ${size.ceiling.toInt()}",
                        action = "Train",
                    ) { store.perform { startTraining(size) } }
                }
            }
        }
    }
    item {
        Section("Business") {
            Text("Focus", style = MaterialTheme.typography.bodyMedium)
            Segmented(AIProduct.entries, lab.focus, {
                when (it) {
                    AIProduct.CHATBOT -> "Chatbot"
                    AIProduct.API -> "API"
                    AIProduct.ENTERPRISE -> "Enterprise"
                }
            }) { p -> store.perform { setAIFocus(p) } }
            KeyValue("Chatbot subscribers", Format.count(lab.subscribers))
            KeyValue("API volume", "${Format.count(lab.apiVolume)}k calls/wk")
            KeyValue("Enterprise clients", "${lab.enterpriseClients}")
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            KeyValue("GPU units owned", "${lab.gpusOwned}")
            OutlinedButton(onClick = { store.perform { buyGPUs(4) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Buy 4 GPU units (${Format.money(state.gpuCost(4))})")
            }
            Hint("Owning GPUs ends the rent bill for training. Prices swing with the era.")
            Meter("Regulatory heat", lab.regulatoryHeat, gameColors.bad)
        }
    }
}

/** A titled option with one action on the right. */
@Composable
fun ActionRow(title: String, detail: String, action: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(detail)
        }
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(action) }
    }
}
