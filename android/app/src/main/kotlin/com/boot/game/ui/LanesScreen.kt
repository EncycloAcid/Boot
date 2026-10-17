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
import com.boot.core.AppCategory
import com.boot.core.AppMonetization
import com.boot.core.DeviceAspect
import com.boot.core.DeviceFocus
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.HardwareLab
import com.boot.core.Lane
import com.boot.core.AgencyRate
import com.boot.core.ScopePolicy
import com.boot.core.TrainingSize
import com.boot.core.UpdateFocus
import com.boot.core.UpdateStyle
import com.boot.core.acceptContract
import com.boot.core.buyGPUs
import com.boot.core.declineContract
import com.boot.core.gpuCost
import com.boot.core.launchDevice
import com.boot.core.orderUnits
import com.boot.core.setAIFocus
import com.boot.core.setAdLoad
import com.boot.core.setAppCategory
import com.boot.core.setModeration
import com.boot.core.setMonetization
import com.boot.core.setAgencyRate
import com.boot.core.setScopePolicy
import com.boot.core.setUpdateFocus
import com.boot.core.setUpdateStyle
import com.boot.core.OsTarget
import com.boot.core.cancelOsBeta
import com.boot.core.osTargetOpen
import com.boot.core.shipOs
import com.boot.core.startOsBeta
import com.boot.core.startOsVersion
import com.boot.core.startTraining
import com.boot.game.GameStore
import java.util.Locale

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
            Lane.OS -> osItems(store, state)
        }
        depthItems(store, state, lane)
        item { MarketingSection(store, state, lane) }
        item { RivalSection(store, state, lane) }
        if (lane != Lane.GAMES) item { RoadmapSection(store, state, lane) }
    }
}

@Composable
private fun LaneHero(state: GameState, lane: Lane) {
    val colors = gameColors
    val profit = state.trailingProfit(lane, 4) / 4
    GroupedSection(null) {
        val compact = LocalCompact.current
        Row(Modifier.padding(if (compact) 12.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
            LaneBadge(lane, if (compact) 44.dp else 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(lane.businessName, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall)
                Text(
                    lane.pitch, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.padding(start = if (compact) 12.dp else 20.dp, end = if (compact) 12.dp else 20.dp, bottom = if (compact) 12.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TileGrid(
                first = {
                    StatTile("Revenue", (state.trailingRevenue(lane, 4) / 4).perWeek(), Modifier.weight(1f), "4-week average")
                    StatTile("Profit", profit.signedPerWeek(), Modifier.weight(1f), "after this lane's payroll", when {
                        profit >= 0.5 -> colors.good
                        profit <= -0.5 -> colors.bad
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
                },
                second = {
                    StatTile(
                        "Team", "${state.staff(lane).size}", Modifier.weight(1f),
                        "and ${Format.percent(state.founder.attentionShare(lane, state.lanes))} of your time",
                    )
                    StatTile(
                        "Market", Format.percent(state.world.era.demand(lane) * state.world.competition(lane)), Modifier.weight(1f),
                        state.world.rival(lane)?.let { "vs ${it.name}" },
                    )
                },
            )
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
                    Hint("Building ${a.category.title.lowercase()} v1.0 — design and tech accumulate toward store score.")
                    Meter("Progress", (a.buildProgress / a.buildWork).coerceAtMost(1.0), wavy = true)
                    Meter("Design", (a.design / 80).coerceAtMost(1.0), gameColors.design, Format.count(a.design))
                    Meter("Tech", (a.tech / 80).coerceAtMost(1.0), gameColors.tech, Format.count(a.tech))
                }
            } else {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TileGrid(
                        first = {
                            StatTile("Users", Format.count(a.users), Modifier.weight(1f))
                            StatTile("Store rank", a.rankLabel, Modifier.weight(1f))
                        },
                        second = {
                            StatTile(
                                "Churn", Format.percent(a.lastChurn, 1), Modifier.weight(1f), "per week",
                                if (a.lastChurn > 0.08) gameColors.bad else Color.Unspecified,
                            )
                            StatTile("Installs", Format.count(a.lastInstalls), Modifier.weight(1f), "this week")
                        },
                    )
                }
                CardBlock {
                    Meter("Store score", a.quality / 100, gameColors.milestone, "${a.quality.toInt()}")
                    Meter(
                        "Design", (a.design / 100).coerceAtMost(1.0), gameColors.design,
                        "${Format.count(a.design)} · ${Format.percent(a.designShare())}",
                    )
                    Meter("Tech", (a.tech / 100).coerceAtMost(1.0), gameColors.tech, Format.count(a.tech))
                    Meter(
                        "Next update", (a.updateProgress / a.updateStyle.work).coerceAtMost(1.0),
                        caption = "${a.weeksSinceUpdate} weeks since · ${a.updateFocus.title.lowercase()} focus", wavy = true,
                    )
                }
            }
        }
    }
    item {
        GroupedSection(
            "Product brief",
            footer = if (a.isLive) {
                "Category is locked after launch. Monetization can still change (some users leave). Platform research stacks into the product."
            } else {
                "Pick a category before v1.0 — it leans the craft balance. Feature research (SDK, offline, sync, collab) stacks like a light engine."
            },
        ) {
            CardBlock(spacing = 8.dp) {
                if (!a.isLive) {
                    Text("Category", style = MaterialTheme.typography.bodyMedium)
                    Segmented(AppCategory.entries, a.category, { it.title }) { c -> store.perform { setAppCategory(c) } }
                    Spacer(Modifier.size(4.dp))
                } else {
                    ValueRow("Category", a.category.title)
                    RowDivider()
                }
                Text("Monetization", style = MaterialTheme.typography.bodyMedium)
                Segmented(AppMonetization.entries, a.monetization, {
                    when (it) {
                        AppMonetization.ADS -> "Ads"
                        AppMonetization.SUBSCRIPTION -> "Subs"
                        AppMonetization.PAID -> "Paid"
                    }
                }) { m -> store.perform { setMonetization(m) } }
                Spacer(Modifier.size(4.dp))
                if (a.features.isNotEmpty()) {
                    Hint("Feature stack: " + a.features.joinToString { it.title })
                } else {
                    Hint("No platform features yet — research Native SDK kit on the Research tab.")
                }
            }
        }
    }
    item {
        GroupedSection(
            "Update roadmap",
            footer = "Cadence sets how often you ship. Focus sets what each drop improves: stability cuts churn, features grow installs, polish lifts reviews.",
        ) {
            CardBlock(spacing = 8.dp) {
                Text("Cadence", style = MaterialTheme.typography.bodyMedium)
                Segmented(UpdateStyle.entries, a.updateStyle, { if (it == UpdateStyle.SMALL) "Small & often" else "Big & rare" }) { u ->
                    store.perform { setUpdateStyle(u) }
                }
                Spacer(Modifier.size(4.dp))
                Text("Next update focus", style = MaterialTheme.typography.bodyMedium)
                Segmented(UpdateFocus.entries, a.updateFocus, { it.title }) { f ->
                    store.perform { setUpdateFocus(f) }
                }
                Hint(a.updateFocus.detail)
            }
        }
    }
    if (a.isLive && a.userHistory.size >= 2) {
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
    if (s.isLive && s.userHistory.size >= 2) {
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
                ListRow(
                    m.name,
                    subtitle = "${m.formFactor.title} · ${Format.money(m.spec.price)} · reviewed ${m.quality.toInt()}",
                )
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
                if (m.aspects.isNotEmpty()) {
                    RowDivider()
                    DeviceAspectBars(m.aspects)
                }
                if (m.pros.isNotEmpty() || m.cons.isNotEmpty()) {
                    RowDivider()
                    CardBlock(spacing = 6.dp) {
                        m.pros.forEach { Hint("+ $it", gameColors.good) }
                        m.cons.forEach { Hint("− $it", gameColors.bad) }
                    }
                }
                RowDivider()
                ValueRow("BOM / unit", Format.money(m.bomCost))
                RowDivider()
                ValueRow("List price", Format.money(m.spec.price))
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
    val d = h.design
    if (d != null) {
        val forecast = h.forecast(d)
        val weeksCover = if (h.lastDemand > 0) d.preorderedStock / h.lastDemand else d.preorderedStock / 100.0
        val stockRisk = when {
            d.isReady && d.preorderedStock == 0 -> "No stock booked — day-one sell-out risk."
            weeksCover < 4 -> "Thin preorder cover. Scalpers may win."
            weeksCover > 20 -> "Heavy stock. Overstock risk if reviews miss."
            else -> null
        }
        GroupedSection(
            "Design",
            footer = stockRisk ?: if (d.isReady) "Design locked. Launch when stock arrives." else null,
        ) {
            CardBlock {
                Text(d.name, style = MaterialTheme.typography.titleLargeEmphasized)
                Hint(
                    "${d.formFactor.title} · ${Format.money(d.spec.price)} retail · " +
                        "${Format.money(d.effectiveUnitCost)} BOM · ${d.spec.tier.weeklyRnD.perWeek()} R&D",
                )
                Meter("Design", d.completion, wavy = !d.isReady)
            }
            RowDivider()
            DeviceFocusBars(d.spec.category, d.normalizedFocus)
            RowDivider()
            ValueRow("Craft", "design ${d.design.toInt()} · tech ${d.tech.toInt()}")
            RowDivider()
            ValueRow("Preorder forecast", "~${forecast.preorderUnits} units · score ~${forecast.expectedScore}")
            RowDivider()
            ValueRow("Stock booked for launch", "${d.preorderedStock} units")
            if (d.bundleOs || d.appPack) {
                RowDivider()
                Hint(
                    buildList {
                        if (d.bundleOs) add("Bundled OS")
                        if (d.appPack) add("App pack")
                    }.joinToString(" · "),
                    MaterialTheme.colorScheme.primary,
                )
            }
            CardBlock { PrimaryButton("Launch the ${d.name}", { store.perform { launchDevice() } }, Modifier.fillMaxWidth(), enabled = d.isReady) }
        }
    } else {
        GroupedSection("New device", footer = "Open the Device Designer: category, form factor, and component mix.") {
            CardBlock {
                PrimaryButton("Design a device", { store.designDevice() }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun DeviceFocusBars(category: com.boot.core.DeviceCategory, weights: List<Double>) {
    CardBlock(spacing = 8.dp) {
        Text("Component mix", style = MaterialTheme.typography.bodyMedium)
        DeviceFocus.ALL.forEachIndexed { i, area ->
            val v = weights.getOrElse(i) { 0.0 }
            Meter(area.title(category), v.coerceIn(0.0, 1.0), MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
internal fun DeviceAspectBars(aspects: Map<DeviceAspect, Double>) {
    CardBlock(spacing = 8.dp) {
        Text("Review aspects", style = MaterialTheme.typography.bodyMedium)
        for ((aspect, score) in aspects.entries.sortedByDescending { it.value }) {
            val tint = when {
                score >= 75 -> Color(0xFF00CE7A)
                score >= 50 -> Color(0xFFFFBD3F)
                else -> Color(0xFFFF6874)
            }
            Meter(
                aspect.title,
                (score / 100).coerceIn(0.03, 1.0),
                tint,
                caption = String.format(Locale.US, "%.0f", score),
            )
        }
    }
}

@Composable
private fun FactorySection(store: GameStore, state: GameState) {
    val h = state.hardware ?: return
    var units by rememberSaveable { mutableIntStateOf(5_000) }
    val unitCost = h.design?.effectiveUnitCost ?: h.onMarket?.bomCost
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
            Hint(
                if (unitCost != null) "$units units cost ${Format.money(units * unitCost)} up front (BOM)"
                else "Start a design before you book the factory.",
            )
            PrimaryButton("Book factory capacity", { store.perform { orderUnits(units) } }, Modifier.fillMaxWidth(), enabled = unitCost != null)
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

// Platform Lab / OS

private fun LazyListScope.osItems(store: GameStore, state: GameState) {
    val lab = state.os ?: return
    val hardwareLive = state.hardware?.onMarket != null && Lane.HARDWARE in state.lanes
    val appsLive = state.apps?.isLive == true && Lane.APPS in state.lanes
    item {
        GroupedSection(
            "Platform Lab",
            footer = when {
                hardwareLive && appsLive -> "Shipping with your own hardware and live apps boosts score and ecosystem."
                hardwareLive -> "A device on the market sweetens the next OS launch."
                appsLive -> "First-party apps lift ecosystem when you ship."
                else -> "Open Hardware or Apps for optional launch synergies."
            },
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TileRow {
                    StatTile("OEM trust", Format.percent(lab.oemTrust), Modifier.weight(1f))
                    StatTile("Ecosystem", Format.percent(lab.ecosystem), Modifier.weight(1f))
                    StatTile("Versions", "${lab.versionsShipped}", Modifier.weight(1f))
                }
                if (lab.lastLicense > 0) {
                    StatTile("License revenue", lab.lastLicense.perWeek(), Modifier.fillMaxWidth(), "this week")
                }
            }
        }
    }
    val p = lab.project
    if (p != null) {
        val bugHeavy = p.bugs > p.work * 0.25
        item {
            GroupedSection(
                p.codename,
                footer = "Feature-complete versions can beta (safer for OEMs) or ship raw (faster, riskier). Waiting after ready still burns a light weekly cost.",
            ) {
                CardBlock {
                    Hint("v${p.version} · ${p.target.title} · ${Format.weeks(p.weeksInDev)} in")
                    Meter("Progress", p.completion, wavy = !p.isReady)
                    Meter(
                        "Design", (p.design / p.work).coerceAtMost(1.0), gameColors.design,
                        Format.count(p.design),
                    )
                    Meter(
                        "Tech", (p.tech / p.work).coerceAtMost(1.0), gameColors.tech,
                        Format.count(p.tech),
                    )
                    Meter("Bugs", (p.bugs / (p.work * 0.5)).coerceAtMost(1.0), gameColors.bad, Format.count(p.bugs))
                    if (p.inBeta) {
                        if (p.betaWeeksLeft > 0) {
                            Hint("Beta: ${Format.weeks(p.betaWeeksLeft)} left — bugs fall each week.")
                        } else {
                            Hint("Beta wrapped. Ship when you're ready — OEMs already saw the polish.", gameColors.good)
                        }
                        PlainButton("Cancel beta", { store.perform { cancelOsBeta() } })
                    }
                    if (p.isReady && !p.inBeta) {
                        if (bugHeavy) {
                            Hint("Bug load is high — shipping raw risks a rollback that freezes OEM orders.", gameColors.bad)
                        }
                        PrimaryButton("Start 6-week beta", { store.perform { startOsBeta(6) } }, Modifier.fillMaxWidth())
                        PrimaryButton("Ship raw", { store.perform { shipOs() } }, Modifier.fillMaxWidth())
                    } else if (p.isReady && p.inBeta && p.betaWeeksLeft <= 0) {
                        PrimaryButton("Ship ${p.codename}", { store.perform { shipOs() } }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    } else {
        item {
            GroupedSection("New version", footer = "Phone targets unlock in 2007; PC & phone dual targets in 2012. Kernel research raises release quality.") {
                OsTarget.entries.forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    val open = state.osTargetOpen(t)
                    ActionRow(
                        t.title,
                        if (open) "${t.work.toInt()} work · ${Format.money(t.weeklyBurn)}/wk burn" else "Unlocks in ${t.arrives}",
                        "Start",
                        enabled = open,
                    ) { store.perform { startOsVersion(t) } }
                }
            }
        }
    }
    if (lab.releases.isNotEmpty()) {
        item {
            GroupedSection("Shipped platforms") {
                lab.releases.take(8).forEachIndexed { i, r ->
                    if (i > 0) RowDivider()
                    ListRow(
                        "${r.codename} v${r.version}",
                        subtitle = "${r.target.title} · ${String.format(java.util.Locale.US, "%.1f", r.score)}/10" +
                            (if (r.beta) " · beta" else " · raw") +
                            (if (r.rolledBack) " · rolled back" else "") +
                            " · ${r.oemPartners} OEMs",
                        value = Format.count(r.installedBase),
                        valueDetail = "devices",
                    )
                }
            }
        }
    }
}
