package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boot.core.AIProduct
import com.boot.core.AppMonetization
import com.boot.core.DeviceCategory
import com.boot.core.DeviceSpec
import com.boot.core.DeviceTier
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.HardwareLab
import com.boot.core.Lane
import com.boot.core.AgencyRate
import com.boot.core.ScopePolicy
import com.boot.core.TrainingSize
import com.boot.core.UpdateStyle
import com.boot.core.acceptContract
import com.boot.core.buyGPUs
import com.boot.core.declineContract
import com.boot.core.gpuCost
import com.boot.core.launchDevice
import com.boot.core.orderUnits
import com.boot.core.setAIFocus
import com.boot.core.setAdLoad
import com.boot.core.setModeration
import com.boot.core.setMonetization
import com.boot.core.setAgencyRate
import com.boot.core.setScopePolicy
import com.boot.core.setUpdateStyle
import com.boot.core.startDevice
import com.boot.core.startTraining
import com.boot.game.GameStore

@Composable
fun LanesScreen(store: GameStore, state: GameState) {
    val lane = store.lane?.takeIf { it in state.lanes } ?: state.lanes.first()
    ScreenColumn {
        if (state.lanes.size > 1) {
            item { Segmented(state.lanes, lane, { "${it.emoji} ${it.short}" }) { store.lane = it } }
        }
        item { LaneHero(state, lane) }
        when (lane) {
            Lane.GAMES -> studioItems(store, state)
            Lane.WEBSITES -> webItems(store, state)
            Lane.APPS -> appsItems(store, state)
            Lane.SOCIAL -> socialItems(store, state)
            Lane.HARDWARE -> hardwareItems(store, state)
            Lane.AI -> aiItems(store, state)
        }
        if (lane != Lane.GAMES) item { RoadmapSection(store, state, lane) }
    }
}

@Composable
private fun LaneHero(state: GameState, lane: Lane) {
    val colors = gameColors
    val profit = state.trailingProfit(lane, 4) / 4
    GroupedSection(null) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LaneBadge(lane, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(lane.businessName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    lane.pitch, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
}

// Websites

private fun LazyListScope.webItems(store: GameStore, state: GameState) {
    val w = state.web ?: return
    item {
        GroupedSection("New briefs", footer = "40% is paid up front, the rest on delivery. Overcommit and every project slips.") {
            if (w.offers.isEmpty()) CardBlock { Hint("No offers this week. Standing and marketers bring more.") }
            w.offers.forEachIndexed { i, o ->
                if (i > 0) RowDivider()
                CardBlock(spacing = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(o.client, style = MaterialTheme.typography.titleMedium)
                            Hint("${o.work.toInt()} work · due in ${o.weeksAllowed} weeks")
                        }
                        Text(Format.money(o.value), style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TonalButton("Accept", { store.perform { acceptContract(o.id) } })
                        PlainButton("Pass", { store.perform { declineContract(o.id) } })
                    }
                }
            }
        }
    }
    item {
        GroupedSection("Agency", footer = "Absorbing keeps clients happy and margins thin. Billing pays, but clients bristle and some walk.") {
            CardBlock {
                Meter("Standing", w.standing, gameColors.good)
                if (w.clientWariness > 0.05) Meter("Client wariness", w.clientWariness, gameColors.warn)
            }
            RowDivider()
            ValueRow("Projects delivered", "${w.completed}")
            RowDivider()
            CardBlock(spacing = 8.dp) {
                Text("When the brief changes at 11pm", style = MaterialTheme.typography.bodyMedium)
                Segmented(ScopePolicy.entries, w.policy, { if (it == ScopePolicy.ABSORB) "Absorb it" else "Bill for it" }) { p ->
                    store.perform { setScopePolicy(p) }
                }
            }
            RowDivider()
            CardBlock(spacing = 8.dp) {
                Text("Your rates", style = MaterialTheme.typography.bodyMedium)
                Segmented(AgencyRate.entries, w.rate, { it.title }) { r -> store.perform { setAgencyRate(r) } }
                Hint(w.rate.blurb)
            }
        }
    }
    item {
        GroupedSection("Active projects") {
            if (w.active.isEmpty()) CardBlock { Hint("Nothing on the go. Accept a brief.") }
            w.active.forEachIndexed { i, c ->
                if (i > 0) RowDivider()
                CardBlock(spacing = 8.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.client, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (c.isOverdue) Pill("Overdue", gameColors.bad)
                        Spacer(Modifier.width(6.dp))
                        Hint("week ${c.weeksElapsed}/${c.weeksAllowed}")
                    }
                    if (c.creepCount > 0) Hint("${c.creepCount} brief change${if (c.creepCount == 1) "" else "s"}", gameColors.warn)
                    Meter("Done", c.completion, wavy = true)
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
        GroupedSection(a.name) {
            if (!a.isLive) {
                CardBlock {
                    Hint("Building version 1.0")
                    Meter("Progress", (a.buildProgress / a.buildWork).coerceAtMost(1.0), wavy = true)
                }
            } else {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                }
                CardBlock {
                    Meter("Quality", a.quality / 100, gameColors.milestone, "${a.quality.toInt()}")
                    Meter(
                        "Next update", (a.updateProgress / a.updateStyle.work).coerceAtMost(1.0),
                        caption = "${a.weeksSinceUpdate} weeks since the last one", wavy = true,
                    )
                }
            }
        }
    }
    item {
        GroupedSection("Strategy", footer = "Stale apps churn. Ship often to stay fresh, or ship big to raise quality.") {
            CardBlock(spacing = 8.dp) {
                Text("Monetization", style = MaterialTheme.typography.bodyMedium)
                Segmented(AppMonetization.entries, a.monetization, {
                    when (it) {
                        AppMonetization.ADS -> "Ads"
                        AppMonetization.SUBSCRIPTION -> "Subs"
                        AppMonetization.PAID -> "Paid"
                    }
                }) { m -> store.perform { setMonetization(m) } }
                Spacer(Modifier.size(4.dp))
                Text("Updates", style = MaterialTheme.typography.bodyMedium)
                Segmented(UpdateStyle.entries, a.updateStyle, { if (it == UpdateStyle.SMALL) "Small & often" else "Big & rare" }) { u ->
                    store.perform { setUpdateStyle(u) }
                }
            }
        }
    }
    if (a.isLive) {
        item { GroupedSection("Users") { CardBlock { AreaChart(a.userHistory, gameColors.lane(Lane.APPS), Format::count) } } }
        item { GroupedSection("Weekly churn") { CardBlock { LineChart(a.churnHistory, gameColors.bad, { Format.percent(it, 1) }) } } }
    }
}

// Social

private fun LazyListScope.socialItems(store: GameStore, state: GameState) {
    val s = state.social ?: return
    item {
        GroupedSection(
            s.name,
            footer = if (s.isLive && !s.caughtOn) "Slow start. If it catches on (around 250k users), growth goes exponential." else null,
        ) {
            if (!s.isLive) {
                CardBlock {
                    Hint("Building the platform")
                    Meter("Progress", (s.buildProgress / s.buildWork).coerceAtMost(1.0), wavy = true)
                }
            } else {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    TileRow {
                        StatTile("Accounts", Format.count(s.users), Modifier.weight(1f))
                        StatTile("Real people", Format.count(s.realUsers), Modifier.weight(1f))
                        StatTile(
                            "Growth", Format.percent(s.lastGrowth, 1), Modifier.weight(1f), "per week",
                            if (s.lastGrowth > 0) gameColors.good else gameColors.bad,
                        )
                    }
                }
                if (s.caughtOn) {
                    ListRow(
                        "It caught on.",
                        leading = { Icon(Icons.Filled.LocalFireDepartment, null, tint = gameColors.warn) },
                    )
                    RowDivider()
                }
                CardBlock {
                    Meter("Bots", s.botShare / 0.6, gameColors.warn, Format.percent(s.botShare))
                    Meter("Toxicity", s.toxicity, gameColors.bad)
                }
            }
        }
    }
    item {
        GroupedSection("Policy", footer = "Moderation costs money per user and keeps bots, fires and scandals down. Ads pay little until you're big.") {
            CardBlock {
                CommitSlider(s.moderation, { "Moderation ${Format.percent(it.toDouble())}" }) { v -> store.perform { setModeration(v) } }
                CommitSlider(s.adLoad, { "Ad load ${Format.percent(it.toDouble())}" }) { v -> store.perform { setAdLoad(v) } }
            }
        }
    }
    if (s.isLive) {
        item { GroupedSection("Accounts") { CardBlock { AreaChart(s.userHistory, gameColors.lane(Lane.SOCIAL), Format::count) } } }
    }
}

// Hardware

private fun LazyListScope.hardwareItems(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    item { DesignSection(store, state) }
    item { FactorySection(store, state) }
    item {
        GroupedSection("On the market") {
            val m = h.onMarket
            if (m != null) {
                ListRow(m.name, subtitle = "${Format.money(m.spec.price)} · reviewed ${m.quality.toInt()}")
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp)) {
                    TileRow {
                        StatTile("Demand", "${Format.count(h.lastDemand)}/wk", Modifier.weight(1f))
                        StatTile(
                            "In stock", Format.count(h.inventory.toDouble()), Modifier.weight(1f),
                            tint = if (h.inventory < h.lastDemand) gameColors.warn else Color.Unspecified,
                        )
                        StatTile("Sold", Format.count(m.unitsSold.toDouble()), Modifier.weight(1f))
                    }
                }
            } else {
                CardBlock { Hint("Nothing on sale yet.") }
            }
            RowDivider()
            CardBlock { Meter("Brand awareness", h.brandAwareness, MaterialTheme.colorScheme.primary) }
            RowDivider()
            ValueRow("Models shipped", "${h.modelsShipped}")
        }
    }
}

@Composable
private fun DesignSection(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    var category by rememberSaveable { mutableStateOf(DeviceCategory.HEADPHONES) }
    var tier by rememberSaveable { mutableStateOf(DeviceTier.BUDGET) }
    val d = h.design
    if (d != null) {
        GroupedSection(
            "Design",
            footer = if (d.isReady && d.preorderedStock == 0) "No stock has arrived yet. Launch now and you'll sell out on day one." else null,
        ) {
            CardBlock {
                Text(d.name, style = MaterialTheme.typography.titleLargeEmphasized)
                Hint("${Format.money(d.spec.price)} retail · ${Format.money(d.spec.unitCost)} to build · ${d.spec.tier.weeklyRnD.perWeek()} R&D")
                Meter("Design", d.completion, wavy = !d.isReady)
            }
            RowDivider()
            ValueRow("Stock booked for launch", "${d.preorderedStock} units")
            CardBlock { PrimaryButton("Launch the ${d.name}", { store.perform { launchDevice() } }, Modifier.fillMaxWidth(), enabled = d.isReady) }
        }
    } else {
        GroupedSection("New device", footer = "Budget sells more units; flagship earns more per unit.") {
            CardBlock {
                Segmented(DeviceCategory.entries, category, { it.title }) { category = it }
                Segmented(DeviceTier.entries, tier, { it.title }) { tier = it }
                val spec = DeviceSpec(category, tier)
                Hint("${Format.money(spec.price)} retail, ${Format.money(spec.unitCost)} per unit, ${tier.weeklyRnD.perWeek()} R&D")
                PrimaryButton("Start design", { store.perform { startDevice(category, tier) } }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FactorySection(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    var units by rememberSaveable { mutableIntStateOf(5_000) }
    val spec = h.design?.spec ?: h.onMarket?.spec
    GroupedSection(
        "Factory",
        footer = "Lead time ${HardwareLab.LEAD_TIME_WEEKS} weeks. Guess low and scalpers profit; guess high and you're stuck with last year's model.",
    ) {
        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalButton("−1k", { units = (units - 1_000).coerceAtLeast(100) })
                Surface(shape = RoundedCornerShape(14.dp), color = gameColors.fill, modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = units.toString(),
                        onValueChange = { text -> text.filter(Char::isDigit).take(8).toIntOrNull()?.let { units = it } },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                TonalButton("+1k", { units += 1_000 })
            }
            Hint(if (spec != null) "$units units cost ${Format.money(units * spec.unitCost)} up front" else "Start a design before you book the factory.")
            PrimaryButton("Book factory capacity", { store.perform { orderUnits(units) } }, Modifier.fillMaxWidth(), enabled = spec != null)
        }
        h.orders.forEach { o ->
            RowDivider()
            ValueRow("${o.units} × ${o.model}", "arrives ${GameClock.label(o.arrivalWeek)}")
        }
        if (state.week < h.productionHaltedUntil) {
            RowDivider()
            ValueRow("Production halted", "until ${GameClock.label(h.productionHaltedUntil)}", gameColors.bad)
        }
    }
}

// AI

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.aiItems(store: GameStore, state: GameState) {
    val lab = state.ai ?: return
    item {
        GroupedSection("Models") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                TileRow {
                    StatTile("Best model", lab.modelName ?: "None", Modifier.weight(1f))
                    StatTile("Capability", "${lab.capability.toInt()}", Modifier.weight(1f))
                    StatTile("Failed runs", "${lab.failedRuns}", Modifier.weight(1f))
                }
            }
            val t = lab.training
            if (t != null) {
                RowDivider()
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The expressive loading indicator morphs through shapes as the run progresses.
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(progress = { t.completion.toFloat() }, modifier = Modifier.size(64.dp), color = gameColors.lane(Lane.AI))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Training ${t.codename}", style = MaterialTheme.typography.titleMedium)
                        Hint("${t.size.title} · ${t.weeksDone}/${t.size.weeks} weeks · ${state.trainingWeeklyCost(t.size).perWeek()}")
                        Hint("You won't know how good it is until it finishes.")
                    }
                }
            } else {
                TrainingSize.entries.forEach { size ->
                    RowDivider()
                    ActionRow(
                        size.title,
                        "${size.weeks} weeks · ${state.trainingWeeklyCost(size).perWeek()} · tops out near ${size.ceiling.toInt()}",
                        "Train",
                    ) { store.perform { startTraining(size) } }
                }
            }
        }
    }
    item {
        GroupedSection("Business", footer = "Owning GPUs ends the rent bill for training. Prices swing with the era.") {
            CardBlock(spacing = 8.dp) {
                Text("Focus", style = MaterialTheme.typography.bodyMedium)
                Segmented(AIProduct.entries, lab.focus, {
                    when (it) {
                        AIProduct.CHATBOT -> "Chatbot"
                        AIProduct.API -> "API"
                        AIProduct.ENTERPRISE -> "Enterprise"
                    }
                }) { p -> store.perform { setAIFocus(p) } }
            }
            RowDivider()
            ValueRow("Chatbot subscribers", Format.count(lab.subscribers))
            RowDivider()
            ValueRow("API volume", "${Format.count(lab.apiVolume)}k calls/wk")
            RowDivider()
            ValueRow("Enterprise clients", "${lab.enterpriseClients}")
            RowDivider()
            ActionRow("GPU units owned: ${lab.gpusOwned}", "4 more cost ${Format.money(state.gpuCost(4))}", "Buy 4") {
                store.perform { buyGPUs(4) }
            }
            RowDivider()
            CardBlock { Meter("Regulatory heat", lab.regulatoryHeat, gameColors.bad) }
        }
    }
}
