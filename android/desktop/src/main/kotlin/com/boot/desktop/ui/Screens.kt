package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.boot.core.AgencyRate
import com.boot.core.Campaign
import com.boot.core.DeviceCategory
import com.boot.core.DeviceTier
import com.boot.core.Difficulty
import com.boot.core.FocusArea
import com.boot.core.Format
import com.boot.core.GameFocus
import com.boot.core.GameSeason
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Research
import com.boot.core.Scenario
import com.boot.core.ScopePolicy
import com.boot.core.StartPoint
import com.boot.core.Topic
import com.boot.core.Office
import com.boot.core.acceptContract
import com.boot.core.acceptDeal
import com.boot.core.arrival
import com.boot.core.arrives
import com.boot.core.assign
import com.boot.core.buyCampaign
import com.boot.core.cancelGame
import com.boot.core.declineContract
import com.boot.core.expand
import com.boot.core.expansionBlocker
import com.boot.core.fire
import com.boot.core.freeDesks
import com.boot.core.gameForecast
import com.boot.core.goPublic
import com.boot.core.hire
import com.boot.core.ipoBlocker
import com.boot.core.laneOpen
import com.boot.core.launchDevice
import com.boot.core.launchGame
import com.boot.core.loanBlocker
import com.boot.core.loanLimit
import com.boot.core.loanOffer
import com.boot.core.loanPayoff
import com.boot.core.moveOffice
import com.boot.core.orderUnits
import com.boot.core.playtestGame
import com.boot.core.rankings
import com.boot.core.awareness
import com.boot.core.brandLift
import com.boot.core.naturalAwareness
import com.boot.core.raiseRound
import com.boot.core.repayLoan
import com.boot.core.research
import com.boot.core.researchBlocker
import com.boot.core.researchPerWeek
import com.boot.core.researchTopic
import com.boot.core.roundOffer
import com.boot.core.setAgencyRate
import com.boot.core.setCreativeFocus
import com.boot.core.setCrunch
import com.boot.core.setFocus
import com.boot.core.setFreelancing
import com.boot.core.setScopePolicy
import com.boot.core.setStageFocus
import com.boot.core.startDevice
import com.boot.core.takeGameContract
import com.boot.core.takeLoan
import com.boot.core.train
import com.boot.desktop.DesktopStore

@Composable
fun NewGameScreen(store: DesktopStore) {
    val h = hudColors
    var company by remember { mutableStateOf("Garage Labs") }
    var founder by remember { mutableStateOf("You") }
    var lane by remember { mutableStateOf(Lane.GAMES) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var start by remember { mutableStateOf(StartPoint.TODAY) }
    var sandbox by remember { mutableStateOf(false) }
    var tutorial by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(0) } // 0 career, 1 scenarios

    Box(Modifier.fillMaxSize()) {
        WorldBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .width(720.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Boot", style = MaterialTheme.typography.displayLarge, color = h.ink)
            Text(
                "Build a company across games, web, hardware, and more. Start a career below — the office and dock guide your next move.",
                style = MaterialTheme.typography.bodyLarge,
                color = h.inkMute,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip("Career", mode == 0) { mode = 0 }
                SelectChip("Scenarios", mode == 1) { mode = 1 }
            }
            GlassPanel {
                Text("Company", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                LightField(company) { company = it }
                Spacer(Modifier.height(8.dp))
                Text("Founder", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                LightField(founder) { founder = it }
            }
            if (mode == 0) {
                GlassPanel {
                    Text("Starting lane", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                    FlowLanePicker(start, lane) { lane = it }
                    Hint(lane.pitch)
                    if (lane.isCapitalIntensive) {
                        Hint("Capital-intensive: you'll raise a seed round on day one.", gameColors.warn)
                    }
                }
                GlassPanel {
                    Text("Difficulty", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (d in Difficulty.entries) {
                            SelectChip(d.title, difficulty == d) { difficulty = d }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("When", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                    FlowRowChips(StartPoint.entries.map { it to it.title.substringBefore(":").ifEmpty { it.title } }) { s, label ->
                        SelectChip(label, start == s) {
                            start = s
                            if (!start.laneOpen(lane)) {
                                lane = Lane.entries.first { start.laneOpen(it) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip(if (tutorial) "Tutorial · on" else "Tutorial · off", tutorial) { tutorial = !tutorial }
                        SelectChip(if (sandbox) "Sandbox · on" else "Sandbox · off", sandbox) { sandbox = !sandbox }
                    }
                    if (sandbox) Hint("Sandbox: a billion dollars, no bankruptcy, stays out of the Hall of Fame.")
                }
                MacButton(
                    "New Game",
                    onClick = {
                        store.newGame(
                            company.trim().ifEmpty { "Garage Labs" },
                            founder.trim().ifEmpty { "You" },
                            lane,
                            difficulty = difficulty,
                            start = start,
                            sandbox = sandbox,
                            tutorial = tutorial,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                GlassPanel {
                    Text("Scenarios", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    Hint("Handcrafted challenges with medals — same core as Android.")
                    Scenario.entries.forEach { sc ->
                        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(sc.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                            Hint("${sc.lane.title} · ${sc.goal} · ${sc.years} years")
                            MacButton(
                                "Play ${sc.title}",
                                onClick = {
                                    store.newScenario(
                                        sc,
                                        company.trim().ifEmpty { "Garage Labs" },
                                        founder.trim().ifEmpty { "You" },
                                    )
                                },
                                primary = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            if (store.hall.isNotEmpty()) {
                Hint("${store.hall.size} finished run(s) under ~/.boot-desktop")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowLanePicker(start: StartPoint, lane: Lane, onLane: (Lane) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (l in Lane.entries) {
            val open = start.laneOpen(l)
            SelectChip(
                if (open) l.title else "${l.title} · ${l.arrives}",
                lane == l,
            ) {
                if (open) onLane(l)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowRowChips(items: List<Pair<T, String>>, chip: @Composable (T, String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (value, label) -> chip(value, label) }
    }
}

@Composable
private fun LightField(value: String, onChange: (String) -> Unit) {
    val h = hudColors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = h.ink),
        cursorBrush = SolidColor(h.accent),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .border(1.dp, h.panelBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
    )
}

@Composable
fun LanesScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val lane = store.lane?.takeIf { it in state.lanes } ?: state.lanes.first()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = WindowPad,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionTitle(
                "Your businesses",
                "Pick a lane below — each one is its own management game.",
            )
        }
        if (state.lanes.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (l in state.lanes) {
                        SelectChip(l.title, l == lane) { store.lane = l }
                    }
                }
            }
        } else {
            item {
                Hint("Starting lane · ${lane.title}", h.accent)
            }
        }
        item {
            GlassPanel {
                Text(lane.businessName, style = MaterialTheme.typography.headlineMedium, color = h.text)
                Hint(lane.pitch)
                Spacer(Modifier.height(8.dp))
                val rev = state.trailingRevenue(lane, 4) / 4
                val profit = state.trailingProfit(lane, 4) / 4
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Revenue /wk", Format.money(rev))
                    Stat("Profit /wk", Format.signedMoney(profit), if (profit >= 0) colors.good else colors.bad)
                    val team = state.staff(lane).size
                    val attention = state.founder.attentionShare(lane, state.lanes)
                    Stat(
                        "Team",
                        if (attention > 0.05) "$team · you ${Format.percent(attention)}" else "$team",
                    )
                    Stat("Market", Format.percent(state.world.era.demand(lane) * state.world.competition(lane)))
                }
                state.world.rival(lane)?.let { rival ->
                    Hint("vs ${rival.name} on the market")
                }
                val awareness = state.awareness(lane)
                val lift = state.brandLift(lane)
                StatMeter(
                    "Brand awareness",
                    awareness.toFloat().coerceIn(0f, 1f),
                    colors.lane(lane),
                    caption = Format.percent(awareness) +
                        if (lift > 1.001) " · demand +${Format.percent(lift - 1)}" else "",
                )
                val natural = state.naturalAwareness(lane)
                Hint(
                    "${Format.percent(natural)} on its own" +
                        if (awareness > natural + 0.005) ", ${Format.percent(awareness - natural)} from marketing" else "",
                )
            }
        }
        item { LaneDepthPanel(store, state, lane) }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color = hudColors.text) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = hudColors.textDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

@Composable
private fun LaneDepthPanel(store: DesktopStore, state: GameState, lane: Lane) {
    when (lane) {
        Lane.GAMES -> GamesStudioPanel(store, state)
        Lane.WEBSITES -> WebAgencyPanel(store, state)
        Lane.HARDWARE -> HardwarePanel(store, state)
        Lane.APPS -> AppsLanePanel(store, state)
        Lane.SOCIAL -> SocialLanePanel(store, state)
        Lane.AI -> AiLanePanel(store, state)
        Lane.OS -> OsLanePanel(store, state)
    }
}

@Composable
private fun WebAgencyPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val w = state.web
    GlassPanel {
        Text("Web agency", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (w == null) {
            Hint("Agency not open.")
            return@GlassPanel
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Standing", Format.percent(w.standing))
            Stat("Active", "${w.active.size}")
            Stat("Offers", "${w.offers.size}")
            Stat("Done", "${w.completed}")
        }
        StatMeter("Client wariness", w.clientWariness.toFloat().coerceIn(0f, 1f), gameColors.warn)
        Hint("Scope · ${w.policy.title}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScopePolicy.entries.forEach { p ->
                SelectChip(p.title, w.policy == p) { store.perform { setScopePolicy(p) } }
            }
        }
        Hint("Rate · ${w.rate.title}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AgencyRate.entries.forEach { r ->
                SelectChip(r.title, w.rate == r) { store.perform { setAgencyRate(r) } }
            }
        }
        if (w.offers.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Hint("Open briefs")
            w.offers.take(5).forEach { c ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.client, style = MaterialTheme.typography.titleMedium, color = h.text)
                        Hint("${Format.money(c.value)} · ${c.weeksAllowed}w · work ${c.work.toInt()}")
                    }
                    MacButton("Accept", onClick = { store.perform { acceptContract(c.id) } })
                    Spacer(Modifier.width(6.dp))
                    MacButton("Pass", onClick = { store.perform { declineContract(c.id) } }, primary = false)
                }
            }
        }
        if (w.active.isNotEmpty()) {
            Hint("In progress")
            w.active.take(4).forEach { c ->
                Text(c.client, style = MaterialTheme.typography.titleMedium, color = h.text)
                StatMeter("Done", c.completion.toFloat().coerceIn(0f, 1f), h.accent)
                StatMeter("Happiness", c.satisfaction.toFloat().coerceIn(0f, 1f), gameColors.good)
            }
        }
    }
}

@Composable
private fun HardwarePanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val hw = state.hardware
    GlassPanel {
        Text("Device lab", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (hw == null) {
            Hint("Hardware lane not open.")
            return@GlassPanel
        }
        StatMeter("Brand awareness", hw.brandAwareness.toFloat().coerceIn(0f, 1f), h.accent)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Models", "${hw.modelsShipped}")
            Stat("Demand", Format.count(hw.lastDemand))
            Stat("Sold", Format.count(hw.lifetimeUnits.toDouble()))
        }
        val d = hw.design
        if (d != null) {
            Text(d.name, style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint(
                "${d.formFactor.title} · ${Format.money(d.spec.price)} · " +
                    if (d.isReady) "locked" else Format.percent(d.completion),
            )
            if (!d.isReady) {
                StatMeter("Design", d.completion.toFloat().coerceIn(0f, 1f), colors.design)
            }
            Hint("Stock booked: ${d.preorderedStock}")
            var units by remember { mutableIntStateOf(5_000) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacButton("−1k", onClick = { units = (units - 1_000).coerceAtLeast(100) }, primary = false)
                Hint("$units units")
                MacButton("+1k", onClick = { units += 1_000 }, primary = false)
                MacButton("Book factory", onClick = { store.perform { orderUnits(units) } })
            }
            MacButton(
                "Launch ${d.name}",
                onClick = { store.perform { launchDevice() } },
                enabled = d.isReady,
                primary = true,
            )
        } else {
            Hint("Pick a category and tier — same startDevice path as Android's Device Designer.")
            var category by remember { mutableStateOf(DeviceCategory.PHONE) }
            var tier by remember { mutableStateOf(DeviceTier.BUDGET) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceCategory.entries.forEach { c ->
                    SelectChip(c.title, category == c) { category = c }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceTier.entries.forEach { t ->
                    SelectChip(t.title, tier == t) { tier = t }
                }
            }
            MacButton(
                "Start design",
                onClick = { store.perform { startDevice(category, tier) } },
                primary = true,
            )
        }
        hw.onMarket?.let { m ->
            Hint("On market: ${m.name} · scored ${m.quality.toInt()}", colors.good)
        }
    }
}

/** Games lane with real choices: designer, stage focus, deals, hype, playtest, ship. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GamesStudioPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val g = state.games
    val season = GameSeason.forWeek(state.week)

    GlassPanel {
        Text("Game studio", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (g == null) {
            Hint("Games lane book not open yet.")
            return@GlassPanel
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Fans", Format.count(g.fans))
            Stat("Trust", Format.percent(g.fanTrust))
            Stat("Best", if (g.topScore > 0) "%.1f".format(g.topScore) else "—")
            Stat("Games", "${g.releases.size}")
        }
        Hint(season.marketTip)
        if (g.lastRoyalties > 0 || g.lastMerch > 0) {
            Hint(
                buildString {
                    if (g.lastRoyalties > 0) append("Royalties ${Format.money(g.lastRoyalties)}")
                    if (g.lastRoyalties > 0 && g.lastMerch > 0) append(" · ")
                    if (g.lastMerch > 0) append("Merch ${Format.money(g.lastMerch)}")
                    append(" last week")
                },
                colors.good,
            )
        }
        state.world.rival(Lane.GAMES)?.let { rival ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompanyLogo(rival.name, size = 26.dp)
                Hint("Rival · ${rival.name}")
            }
        }

        if (g.dealOffers.isNotEmpty()) {
            Hint("Publisher deals — accept starts the project with their concept.")
            g.dealOffers.take(3).forEach { d ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompanyLogo(d.publisher, size = 24.dp)
                    Column(Modifier.weight(1f)) {
                        Hint("${d.publisher} · ${d.topic.title} ${d.genre.title}")
                        Hint("Advance ${Format.money(d.advance)} · hit ${d.minScore}+ · you keep ${Format.percent(d.share)}")
                    }
                    MacButton("Accept", onClick = { store.perform { acceptDeal(d.id) } })
                }
            }
        }

        if (g.contractOffers.isNotEmpty() && g.project == null && g.contract == null) {
            Hint("Contract work")
            g.contractOffers.take(2).forEach { c ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Hint(c.title)
                        Hint("${Format.money(c.pay)} · ${c.weeksAllowed}w · +${c.rp.toInt()} RP")
                    }
                    MacButton("Take", onClick = { store.perform { takeGameContract(c.id) } }, primary = false)
                }
            }
        }

        g.contract?.let { c ->
            Spacer(Modifier.height(4.dp))
            Text(c.title, style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint("Contract · ${Format.money(c.pay)} on delivery")
            StatMeter("Progress", c.completion.toFloat(), h.accent)
        }
        g.expansion?.let { x ->
            Spacer(Modifier.height(4.dp))
            Text(x.title, style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint("Expansion pack in progress")
            StatMeter("Progress", x.completion.toFloat(), h.accent)
        }

        val project = g.project
        if (project != null) {
            Spacer(Modifier.height(4.dp))
            Text(project.title, style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint(
                "${project.concept.size.title} ${project.concept.topic.title.lowercase()} ${project.concept.genreLower} · " +
                    "stage ${if (project.isReady) "polish" else "${project.stage + 1}/3"} · ${Format.percent(project.completion)}",
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(
                    "Design",
                    Format.count(project.design) + deltaSuffix(project.lastDesign),
                    colors.design,
                )
                Stat(
                    "Tech",
                    Format.count(project.tech) + deltaSuffix(project.lastTech),
                    colors.tech,
                )
                Stat(
                    "Bugs",
                    Format.count(project.bugs) + deltaSuffix(project.lastBugs),
                    colors.bad,
                )
                Stat("Hype", "${project.hype.toInt()}", colors.warn)
            }
            StatMeter("Development", project.completion.toFloat(), h.accent)
            StatMeter(
                "Hype",
                (project.hype / 100.0).toFloat().coerceIn(0f, 1f),
                colors.warn,
                caption = "${project.hype.toInt()}",
            )
            if (!project.isReady) {
                StatMeter(
                    "Genre fit",
                    project.designLeanFit().toFloat(),
                    if (project.designLeanFit() >= 0.7) colors.design else colors.warn,
                    caption = "${Format.percent(project.projectedDesignShare())} design",
                )
            } else {
                StatMeter(
                    "Polish",
                    (project.polish / (project.work * 0.4)).toFloat().coerceIn(0f, 1f),
                    colors.milestone,
                )
            }
            state.gameForecast()?.let { f ->
                Hint("Expected ~${"%.1f".format(f.expectedScore)} (${f.low}–${f.high})")
            }

            Hint("Creative direction — trades speed, polish, bugs, and score:")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (focus in GameFocus.entries) {
                    SelectChip(focus.title, project.creativeFocus == focus) {
                        store.perform { setCreativeFocus(focus) }
                    }
                }
            }
            Hint(project.creativeFocus.blurb)

            if (!project.isReady) {
                val stage = project.stage
                val areas = FocusArea.forStage(stage)
                val alloc = project.focus[stage]
                Hint(FocusArea.STAGES[stage])
                areas.forEachIndexed { i, area ->
                    val total = alloc.sum().takeIf { it > 0 } ?: 1.0
                    Text(
                        "${area.title} · ${Format.percent(alloc[i] / total)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = h.text,
                    )
                    Slider(
                        value = alloc[i].toFloat().coerceIn(0.05f, 1f),
                        onValueChange = { v ->
                            val next = alloc.toMutableList().also { it[i] = v.toDouble() }
                            store.perform { setStageFocus(stage, next) }
                        },
                        valueRange = 0.05f..1f,
                    )
                }
            }

            if (Research.MARKETING in state.researched) {
                Hint("Hype campaigns")
                Campaign.entries.forEach { c ->
                    val done = c in project.campaigns
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Hint(c.title)
                            Hint("${Format.money(c.cost)} · +${c.hype.toInt()} hype")
                        }
                        MacButton(
                            if (done) "Done" else "Run",
                            onClick = { store.perform { buyCampaign(c) } },
                            enabled = !done && state.cash >= c.cost,
                            primary = false,
                        )
                    }
                }
            }

            val playStage = if (project.isReady) 3 else project.stage
            val playCost = project.concept.size.weeklyBudget * (1.5 + playStage * 0.5)
            if (playStage !in project.playtestedStages) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Hint("Playtest this stage — cut ~35% of bugs and gain craft.")
                        Hint(Format.money(playCost), h.accent)
                    }
                    MacButton(
                        "Playtest",
                        onClick = { store.perform { playtestGame() } },
                        enabled = state.cash >= playCost,
                    )
                }
            } else {
                Hint("Stage playtested. Next stage unlocks another test.", colors.good)
            }

            if (project.isReady) {
                val lift = season.launchMultiplier(project.concept.genre)
                Hint(
                    if (lift >= 1.08) "${season.title} boosts ${project.concept.genre.lower} launches (+${((lift - 1) * 100).toInt()}%)."
                    else if (lift < 1.0) "Off-season for ${project.concept.genre.lower} (−${((1 - lift) * 100).toInt()}%). Wait or ship anyway."
                    else season.marketTip,
                )
                MacButton(
                    "Release ${project.title}",
                    onClick = { store.perform { launchGame() } },
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Hint("Next · advance weeks from the HUD Play / Week controls, or playtest between ticks.")
            }
            MacButton(
                "Cancel project",
                onClick = { store.perform { cancelGame() } },
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (g.contract == null && g.expansion == null) {
            EmptyState(
                title = "No game in development",
                body = "Design a concept — topic, genre, platforms, size, and stage focus. This is your primary studio action.",
                actionLabel = "Design a game",
                onAction = { store.designGame() },
            )
            if (Research.SECOND_TEAM in state.researched) {
                MacButton(
                    "Design for Team 2",
                    onClick = { store.designGame(team = 2) },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        g.project2?.let { p2 ->
            Spacer(Modifier.height(4.dp))
            Text("Team 2 · ${p2.title}", style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint(
                "${p2.concept.size.title} ${p2.concept.genreLower} · " +
                    "stage ${if (p2.isReady) "polish" else "${p2.stage + 1}/3"}",
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Design", Format.count(p2.design) + deltaSuffix(p2.lastDesign), colors.design)
                Stat("Tech", Format.count(p2.tech) + deltaSuffix(p2.lastTech), colors.tech)
                Stat("Bugs", Format.count(p2.bugs) + deltaSuffix(p2.lastBugs), colors.bad)
                Stat("Hype", "${p2.hype.toInt()}", colors.warn)
            }
            StatMeter("Development", p2.completion.toFloat(), h.accent)
            if (!p2.isReady) {
                StatMeter("Genre fit", p2.designLeanFit().toFloat(), colors.design)
            } else {
                StatMeter(
                    "Polish",
                    (p2.polish / (p2.work * 0.4)).toFloat().coerceIn(0f, 1f),
                    colors.milestone,
                )
            }
            state.gameForecast(2)?.let { f ->
                Hint("Expected ~${"%.1f".format(f.expectedScore)} (${f.low}–${f.high})")
            }
            if (p2.isReady) {
                MacButton("Release Team 2's ${p2.title}", onClick = { store.perform { launchGame(2) } })
            }
        }

        if (g.releases.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Hint("Recent releases")
            g.releases.take(4).forEach { r ->
                val player = if (r.userScore > 0) {
                    " · players ${"%.1f".format(r.userScore)}${if (!r.userScoreSettled) "…" else ""}"
                } else {
                    ""
                }
                val weekly = if (r.onSale) " · ${Format.money(r.weeklySales)}/wk" else ""
                Text(
                    "★ ${r.title} · critics ${"%.1f".format(r.score)}$player · ${Format.money(r.lifetimeSales)}$weekly",
                    style = MaterialTheme.typography.bodyMedium,
                    color = h.textDim,
                )
            }
        }
    }
}

private fun deltaSuffix(delta: Double): String =
    when {
        delta >= 1 -> " (+${delta.toInt()})"
        delta <= -1 -> " (${delta.toInt()})"
        else -> ""
    }

@Composable
fun ResearchScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val affordable = remember(state.researchPoints, state.researched, state.lanes, state.week) {
        Research.entries.filter { state.researchBlocker(it) == null }.take(20)
    }
    val lockedTopics = state.games?.let { g -> Topic.entries.filter { it !in g.topics } }.orEmpty()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = WindowPad,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Research lab", "Spend RP to unlock tools, teams, and topics.")
        }
        item {
            GlassPanel {
                Text(
                    "${state.researchPoints.toInt()} RP available",
                    style = MaterialTheme.typography.headlineMedium,
                    color = h.accent,
                )
                Hint(
                    String.format("+%.1f RP a week · %d researched", state.researchPerWeek, state.researched.size),
                    colors.milestone,
                )
                state.world.era.let { era ->
                    Hint("Era · ${era.title}: ${era.blurb}")
                }
            }
        }
        if (lockedTopics.isNotEmpty()) {
            item {
                GlassPanel {
                    Text("Topics", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    Hint("New topics open fresh genre combos.")
                    lockedTopics.take(12).forEach { t ->
                        val trending = state.games?.trend?.topic == t
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                                if (trending) Hint("Trending now", colors.good)
                            }
                            MacButton(
                                "${t.researchCost} RP",
                                onClick = { store.perform { researchTopic(t) } },
                                enabled = state.researchPoints >= t.researchCost,
                                primary = false,
                            )
                        }
                    }
                }
            }
        }
        items(affordable, key = { it.name }) { r ->
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                        Hint(r.detail)
                        val cashBit = if (r.cash > 0) " · ${Format.money(r.cash)}" else ""
                        Hint("${r.rp} RP$cashBit · ${r.kind.name.lowercase()}", h.accent)
                    }
                    MacButton(
                        if (r.cash > 0) "Buy · ${r.rp} RP" else "Buy",
                        onClick = { store.perform { research(r) } },
                        enabled = state.researchPoints >= r.rp && state.cash >= r.cash,
                    )
                }
            }
        }
        if (affordable.isEmpty() && lockedTopics.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing to buy yet",
                    body = "Advance weeks from the HUD to earn research points, then come back here.",
                )
            }
        }
    }
}

@Composable
fun TeamScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = WindowPad,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle(
                "Team",
                "${state.staff.size} hired · ${state.office.title} · ${state.freeDesks}/${state.office.desks} desks · rent ${Format.money(state.office.rent)}/wk",
            )
        }
        item {
            GlassPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FounderPortrait(state, 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(state.founder.name, style = MaterialTheme.typography.titleLarge, color = h.text)
                        Hint("Founder · ${Format.percent(state.venture.founderEquity)} of company")
                    }
                }
                state.lanes.forEach { lane ->
                    val bonus = state.founder.skillBonus(lane)
                    if (bonus > 0.05) {
                        Hint("${lane.title} skill +${"%.1f".format(bonus)}")
                    }
                }
                if (state.lanes.size > 1) {
                    Hint("Your attention across lanes")
                    state.lanes.forEach { lane ->
                        val share = state.founder.attentionShare(lane, state.lanes)
                        Text(
                            "${lane.title} · ${Format.percent(share)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = h.text,
                        )
                        Slider(
                            value = (state.founder.focus[lane] ?: 1.0).toFloat().coerceIn(0.05f, 3f),
                            onValueChange = { v -> store.perform { setFocus(lane, v.toDouble()) } },
                            valueRange = 0.05f..3f,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(if (state.crunch) "Crunch · on" else "Crunch · off", state.crunch) {
                        store.perform { setCrunch(!state.crunch) }
                    }
                    SelectChip(
                        if (state.founder.freelancing) {
                            "Freelance · ${Format.money(state.founder.freelanceIncome(state.week))}/wk"
                        } else {
                            "Freelance · off"
                        },
                        state.founder.freelancing,
                    ) {
                        store.perform { setFreelancing(!state.founder.freelancing) }
                    }
                }
                val nextOffice = Office.entries.firstOrNull { it.ordinal == state.office.ordinal + 1 }
                if (nextOffice != null) {
                    Hint("Next: ${nextOffice.title} · ${nextOffice.desks} desks · rent ${Format.money(nextOffice.rent)}/wk · move ${Format.money(nextOffice.moveCost)}")
                    MacButton("Move office", onClick = { store.perform { moveOffice() } }, primary = false)
                }
            }
        }
        items(state.staff, key = { it.id }) { e ->
            val colors = gameColors
            GlassPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StaffPortrait(e, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, color = h.text)
                        Hint("${e.role.title} · Lv ${e.level} · ${e.lane?.title ?: "unassigned"} · ${Format.money(e.salary)}/wk")
                        Hint("Skill ${e.skill} · D${e.design} · T${e.tech} · R${e.research}")
                        e.trait?.let { Hint(it.title, h.cash) }
                    }
                }
                StatMeter("Morale", e.morale.toFloat().coerceIn(0f, 1f), if (e.morale >= 0.5) colors.good else colors.bad)
                if (!e.maxLevel) {
                    StatMeter(
                        "XP",
                        (e.xp / e.xpToNext).toFloat().coerceIn(0f, 1f),
                        colors.milestone,
                        caption = "${e.xp.toInt()}/${e.xpToNext.toInt()}",
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectChip("Unassigned", e.lane == null) { store.perform { assign(e.id, null) } }
                    state.lanes.forEach { l ->
                        SelectChip(l.title, e.lane == l) { store.perform { assign(e.id, l) } }
                    }
                    MacButton("Train", onClick = { store.perform { train(e.id) } }, primary = false)
                    MacButton("Fire", onClick = { store.perform { fire(e.id) } }, primary = false)
                }
            }
        }
        item {
            GlassPanel {
                Text("Talent pool", style = MaterialTheme.typography.titleLarge, color = h.accent)
                Hint("Primary action · hire into your starting lane, then assign desks.")
                if (state.candidates.isEmpty()) {
                    EmptyState(
                        title = "No candidates this week",
                        body = "Advance time from the HUD to refresh the talent pool.",
                    )
                } else {
                    state.candidates.take(6).forEach { cand ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cand.name, style = MaterialTheme.typography.titleMedium, color = h.text)
                                Hint("${cand.role.title} · skill ${cand.skill} · D${cand.design} T${cand.tech} R${cand.research}")
                                Hint("${Format.money(cand.askingSalary)}/wk ask" + (cand.contestedBy?.let { " · contested by $it" } ?: ""))
                            }
                            val hireLane = store.lane?.takeIf { it in state.lanes } ?: state.startingLane
                            MacButton("Hire", onClick = { store.perform { hire(cand.id, hireLane) } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val weekly = state.trailingProfit(weeks = 4) / 4
    var page by remember { mutableIntStateOf(store.companyPage) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowPad),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(state.companyName, "Cash, expansion, and long-game strategy.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip("Overview", page == 0) { page = 0; store.companyPage = 0 }
            SelectChip("Money", page == 1) { page = 1; store.companyPage = 1 }
            SelectChip("Expand", page == 2) { page = 2; store.companyPage = 2 }
            SelectChip("Strategy", page == 3) { page = 3; store.companyPage = 3 }
        }
        when (page) {
            1 -> MoneyPanel(store, state)
            2 -> ExpandPanel(store, state)
            3 -> StrategyPanel(store, state)
            else -> {
                val listing = state.listing
                GlassPanel {
                    Text(
                        Format.money(listing?.marketCap ?: state.valuation),
                        style = MaterialTheme.typography.displayLarge,
                        color = h.accent,
                    )
                    Hint(
                        listing?.let {
                            "${it.ticker} · \$${"%.2f".format(it.price)}/share · market cap"
                        } ?: "Valuation",
                    )
                    Hint(
                        "${Format.signedMoney(weekly)}/wk · you own ${Format.percent(state.venture.founderEquity)}",
                        if (weekly >= 0) colors.good else colors.bad,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Cash", Format.money(state.cash), if (state.cash < 0) colors.bad else h.text)
                        Stat("Weekly", Format.signedMoney(weekly), if (weekly >= 0) colors.good else colors.bad)
                        val scars = state.reputation.scars.size
                        Stat(
                            "Reputation",
                            "${state.reputation.score.toInt()}",
                            if (scars > 0) colors.warn else h.text,
                        )
                        val runway = state.runwayWeeks
                        Stat(
                            "Runway",
                            when {
                                !runway.isFinite() -> "∞"
                                runway > 260 -> "5+ yrs"
                                runway >= 52 -> String.format("%.1fy", runway / 52)
                                else -> "${runway.toInt()}w"
                            },
                        )
                    }
                    if (state.reputation.scars.isNotEmpty()) {
                        Hint("Scars: ${state.reputation.scars.joinToString { it.kind.title }}", colors.bad)
                        state.reputation.scars.take(3).forEach { scar ->
                            StatMeter(scar.kind.title, scar.severity.toFloat().coerceIn(0f, 1f), colors.bad)
                        }
                    }
                }
                if (state.venture.board != null || state.listing != null) {
                    val board = state.venture.board
                    if (board != null) {
                        GlassPanel {
                            Text("Board", style = MaterialTheme.typography.titleLarge, color = h.accent)
                            StatMeter("Confidence", board.confidence.toFloat().coerceIn(0f, 1f), colors.milestone)
                            Hint("Growth target ${Format.percent(board.growthTarget)}")
                        }
                    }
                }
                GlassPanel {
                    Text("Lane P&L · 4 weeks", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    state.lanes.forEach { lane ->
                        val rev = state.trailingRevenue(lane, 4) / 4
                        val profit = state.trailingProfit(lane, 4) / 4
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Hint(lane.businessName)
                            Hint(
                                "${Format.money(rev)} · ${Format.signedMoney(profit)}",
                                if (profit >= 0) colors.good else colors.bad,
                            )
                        }
                    }
                }
                GlassPanel {
                    Text("Quarterly results", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    Hint("Most recent first · 13-week windows")
                    (0 until 4).forEach { q ->
                        val rev = state.trailingRevenue(weeks = 13, offset = q * 13)
                        val end = state.ledger.size - q * 13
                        val profit = if (end <= 0) {
                            0.0
                        } else {
                            state.ledger.subList(maxOf(0, end - 13), end).sumOf { it.profit }
                        }
                        if (rev > 0 || profit != 0.0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Hint(if (q == 0) "Last 13 weeks" else "${q * 13}–${q * 13 + 13} weeks ago")
                                Hint(
                                    "${Format.money(rev)} · ${Format.signedMoney(profit)}",
                                    if (profit >= 0) colors.good else colors.bad,
                                )
                            }
                        }
                    }
                }
                GlassPanel {
                    Text("Era · ${state.world.era.title}", style = MaterialTheme.typography.titleLarge, color = h.text)
                    Hint(state.world.era.blurb)
                }
                GlassPanel {
                    Text("Biggest companies", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    val ranks = state.rankings()
                    val myRank = ranks.indexOfFirst { it.mine } + 1
                    if (myRank > 0) Hint("You're #$myRank of ${ranks.size}")
                    ranks.take(6).forEachIndexed { i, c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = h.textDim, modifier = Modifier.width(18.dp))
                            if (!c.mine) CompanyLogo(c.name, size = 22.dp)
                            Text(
                                c.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (c.mine) h.accent else h.text,
                                modifier = Modifier.weight(1f),
                            )
                            Hint(Format.money(c.value))
                        }
                    }
                }
                GlassPanel {
                    Text("Abandon run", style = MaterialTheme.typography.titleMedium, color = colors.bad)
                    Hint("Ends this company and returns to New Game.")
                    MacButton("Abandon", onClick = { store.abandon() }, primary = false)
                }
            }
        }
    }
}

@Composable
private fun MoneyPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    GlassPanel {
        Text("Cash", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Text(Format.money(state.cash), style = MaterialTheme.typography.displayLarge, color = if (state.cash < 0) colors.bad else h.ink)
        Hint("Loans and raises keep red weeks from ending the run — same as Android.")
    }
    val loan = state.loan
    if (loan != null) {
        GlassPanel {
            Text("Bank loan", style = MaterialTheme.typography.titleLarge, color = h.text)
            Hint("${Format.money(loan.balance)} left · ${Format.money(loan.weeklyPayment)}/wk")
            val payoff = state.loanPayoff ?: loan.balance
            MacButton(
                "Repay ${Format.money(payoff)}",
                onClick = { store.perform { repayLoan() } },
                enabled = state.cash >= payoff,
            )
        }
    } else {
        val offer = state.loanOffer()
        val blocker = state.loanBlocker
        GlassPanel {
            Text("Bank loan", style = MaterialTheme.typography.titleLarge, color = h.text)
            Hint(blocker ?: "Borrow ${Format.money(state.loanLimit)} · then ${Format.money(offer.weeklyPayment)}/wk for a year.")
            MacButton(
                "Borrow ${Format.money(state.loanLimit)}",
                onClick = { store.perform { takeLoan() } },
                enabled = blocker == null,
            )
        }
    }
    val raise = state.roundOffer()
    GlassPanel {
        Text("Venture", style = MaterialTheme.typography.titleLarge, color = h.text)
        raise.onSuccess { o ->
            Hint("${o.lead.ifEmpty { "Investors" }} · ${o.name} · ${Format.money(o.amount)} for ${Format.percent(o.dilution)}")
            MacButton("Raise a ${o.name}", onClick = { store.perform { raiseRound() } })
        }.onFailure { e ->
            Hint(e.message ?: "No round available.")
        }
    }
    if (state.listing == null) {
        val ipo = state.ipoBlocker
        GlassPanel {
            Text("Going public", style = MaterialTheme.typography.titleLarge, color = h.text)
            Hint(ipo ?: "List and keep running, or retire.")
            // Soft progress toward typical IPO gates (shown even when blocked).
            StatMeter(
                "Valuation toward IPO",
                (state.valuation / 50_000_000.0).toFloat().coerceIn(0f, 1f),
                colors.milestone,
                caption = Format.money(state.valuation),
            )
            StatMeter(
                "Annual revenue",
                (state.trailingRevenue(weeks = 52) / 10_000_000.0).toFloat().coerceIn(0f, 1f),
                h.accent,
                caption = Format.money(state.trailingRevenue(weeks = 52)),
            )
            MacButton(
                "Go public and keep going",
                onClick = { store.perform { goPublic(keepRunning = true) } },
                enabled = ipo == null,
            )
            MacButton(
                "Go public and retire",
                onClick = { store.perform { goPublic() } },
                enabled = ipo == null,
                primary = false,
            )
        }
    } else {
        val listing = state.listing!!
        GlassPanel {
            Text("${listing.ticker} listing", style = MaterialTheme.typography.titleLarge, color = h.accent)
            Hint("\$${"%.2f".format(listing.price)}/share · cap ${Format.money(listing.marketCap)}")
            Hint("You own ${Format.percent(state.venture.founderEquity)}")
        }
    }
}

@Composable
private fun ExpandPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    var useVenture by remember { mutableStateOf(false) }
    GlassPanel {
        Text("Expansion", style = MaterialTheme.typography.titleLarge, color = h.accent)
        val blocker = state.expansionBlocker
        if (blocker != null) {
            Hint(blocker, colors.bad)
            return@GlassPanel
        }
        SelectChip(
            if (useVenture) "Fund with venture · on" else "Fund with venture · off",
            useVenture,
        ) { useVenture = !useVenture }
        Lane.entries.filter { it !in state.lanes }.forEach { lane ->
            val open = state.laneOpen(lane)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(lane.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                    Hint(
                        if (!open) lane.arrival
                        else "Setup ${Format.money(lane.setupCost)}" +
                            if (lane.isCapitalIntensive) " · needs cash or VC" else "",
                    )
                }
                MacButton(
                    "Expand",
                    onClick = { store.perform { expand(lane, useVenture) } },
                    enabled = open,
                    primary = false,
                )
            }
        }
    }
}
