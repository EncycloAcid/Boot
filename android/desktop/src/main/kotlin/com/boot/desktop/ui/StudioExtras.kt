package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import com.boot.core.AppPricing
import com.boot.core.BUILD_DRAG
import com.boot.core.Campaign
import com.boot.core.Combo
import com.boot.core.ContentPolicy
import com.boot.core.CounterMove
import com.boot.core.ExperimentKind
import com.boot.core.Feature
import com.boot.core.FocusArea
import com.boot.core.Format
import com.boot.core.GameFocus
import com.boot.core.GameProject
import com.boot.core.GameRelease
import com.boot.core.GameSeason
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.IndiePitch
import com.boot.core.LABEL_SLOTS
import com.boot.core.Lane
import com.boot.core.Perk
import com.boot.core.PrizePool
import com.boot.core.Research
import com.boot.core.TakeoverPremiums
import com.boot.core.bidFor
import com.boot.core.bookKeynote
import com.boot.core.buildCrossover
import com.boot.core.buildEngine
import com.boot.core.buildFeature
import com.boot.core.buyBlocker
import com.boot.core.buyCampaign
import com.boot.core.buyLabelCampaign
import com.boot.core.buyPerk
import com.boot.core.buyShares
import com.boot.core.cancelGame
import com.boot.core.counter
import com.boot.core.counterBlocker
import com.boot.core.counterCost
import com.boot.core.dropRetainer
import com.boot.core.endLeague
import com.boot.core.engineQuote
import com.boot.core.expectedEngineStandard
import com.boot.core.experimentBlocker
import com.boot.core.experimentCost
import com.boot.core.featureBlocker
import com.boot.core.gameForecast
import com.boot.core.holdingValue
import com.boot.core.keynoteCost
import com.boot.core.labelCampaignBlocker
import com.boot.core.launchGame
import com.boot.core.leagueBlocker
import com.boot.core.leagueCandidates
import com.boot.core.leagueCost
import com.boot.core.leagues
import com.boot.core.maxBuy
import com.boot.core.passPitch
import com.boot.core.perkBlocker
import com.boot.core.perkUpkeep
import com.boot.core.perkSlots
import com.boot.core.playtestGame
import com.boot.core.portfolioValue
import com.boot.core.pressuresOn
import com.boot.core.remasterConcept
import com.boot.core.remasterable
import com.boot.core.removePerk
import com.boot.core.reserveCompute
import com.boot.core.reservedRate
import com.boot.core.sellCompany
import com.boot.core.sellShares
import com.boot.core.setAppPricing
import com.boot.core.setContentPolicy
import com.boot.core.setCreativeFocus
import com.boot.core.setCreatorFund
import com.boot.core.setDevicePrice
import com.boot.core.setEngineLicensed
import com.boot.core.setFocus
import com.boot.core.setIntensity
import com.boot.core.setPremium
import com.boot.core.setPrizePool
import com.boot.core.setStageFocus
import com.boot.core.sharePrice
import com.boot.core.signBlocker
import com.boot.core.signPitch
import com.boot.core.startExperiment
import com.boot.core.startLeague
import com.boot.core.stock
import com.boot.core.stocks
import com.boot.core.takeoverBlocker
import com.boot.core.takeoverQuote
import com.boot.desktop.DesktopStore

private fun projectDeltaSuffix(delta: Double): String =
    when {
        delta >= 1 -> " (+${delta.toInt()})"
        delta <= -1 -> " (${delta.toInt()})"
        else -> ""
    }

/** Shared project controls for Team 1 / Team 2. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProjectControls(
    store: DesktopStore,
    state: GameState,
    project: GameProject,
    team: Int,
) {
    val h = hudColors
    val colors = gameColors
    val season = GameSeason.forWeek(state.week)
    Text(
        if (team == 2) "Team 2 · ${project.title}" else project.title,
        style = MaterialTheme.typography.titleMedium,
        color = h.text,
    )
    Hint(
        "${project.concept.size.title} ${project.concept.topic.title.lowercase()} ${project.concept.genreLower} · " +
            "stage ${if (project.isReady) "polish" else "${project.stage + 1}/3"} · ${Format.percent(project.completion)}",
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat(
            "Design",
            Format.count(project.design) + projectDeltaSuffix(project.lastDesign),
            colors.design,
        )
        Stat(
            "Tech",
            Format.count(project.tech) + projectDeltaSuffix(project.lastTech),
            colors.tech,
        )
        Stat(
            "Bugs",
            Format.count(project.bugs) + projectDeltaSuffix(project.lastBugs),
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
    state.gameForecast(team)?.let { f ->
        Hint("Expected ~${"%.1f".format(f.expectedScore)} (${f.low}–${f.high})")
    }

    Hint("Creative direction — trades speed, polish, bugs, and score:")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (focus in GameFocus.entries) {
            SelectChip(focus.title, project.creativeFocus == focus) {
                store.perform { setCreativeFocus(focus, team) }
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
            CommitSlider(
                alloc[i],
                { v -> "${area.title} · ${Format.percent(v.toDouble() / (total - alloc[i] + v.toDouble()).coerceAtLeast(0.01))}" },
            ) { v ->
                val next = alloc.toMutableList().also { it[i] = v }
                store.perform { setStageFocus(stage, next, team) }
            }
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
                    onClick = { store.perform { buyCampaign(c, team) } },
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
                onClick = { store.perform { playtestGame(team) } },
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
            if (team == 2) "Release Team 2's ${project.title}" else "Release ${project.title}",
            onClick = { store.perform { launchGame(team) } },
            primary = true,
        )
    } else {
        Hint("Advance weeks from the HUD — change direction or playtest between ticks.")
    }
    MacButton("Cancel project", onClick = { store.perform { cancelGame(team) } }, primary = false)
}

@Composable
internal fun EnginesPanel(store: DesktopStore, state: GameState, g: GamesStudio) {
    val h = hudColors
    val colors = gameColors
    val canBuild = Research.CUSTOM_ENGINES in state.researched
    val (features, cost, weeks) = state.engineQuote()
    val best = g.engines.maxOfOrNull { it.level } ?: 0
    val standard = state.expectedEngineStandard().toInt()
    GlassPanel {
        Text("Engines", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Hint(
            if (!canBuild) "Research custom engines, then graphic styles and tech features."
            else "Reviewers expect about level $standard by now.",
        )
        g.engines.sortedByDescending { it.level }.forEach { e ->
            Hint("${e.name} · level ${e.level}" + if (e.features.isNotEmpty()) " · ${e.features.joinToString { it.title }}" else "")
            if (e.level > 0) {
                SelectChip(
                    if (e.licensed) "Licensed · on" else "License · off",
                    e.licensed,
                ) { store.perform { setEngineLicensed(e.id, !e.licensed) } }
            }
        }
        g.engineBuild?.let { b ->
            Hint("Building ${b.name} · ${Format.weeks(b.weeksLeft)} left · L${b.combo.level}", colors.warn)
        }
        if (canBuild && g.engineBuild == null && features.isNotEmpty()) {
            val improves = features.isNotEmpty()
            Hint("${features.size} features · ${Format.money(cost)} · $weeks weeks · market ~L$standard")
            MacButton(
                "Build engine",
                onClick = { store.perform { buildEngine() } },
                enabled = improves && state.cash >= cost && (best >= 0),
            )
        }
    }
}

@Composable
internal fun LabelPanel(store: DesktopStore, state: GameState, g: GamesStudio) {
    if (Research.PUBLISHING !in state.researched) {
        Hint("Research publishing to run a label.")
        return
    }
    val h = hudColors
    val colors = gameColors
    var looking by remember { mutableStateOf<IndiePitch?>(null) }

    if (g.signed.isNotEmpty()) {
        GlassPanel {
            Text("Your label · ${g.signed.size} of $LABEL_SLOTS", style = MaterialTheme.typography.titleLarge, color = h.accent)
            g.signed.forEach { s ->
                Hint("${s.title} by ${s.pitch.developer} · ${Format.percent(s.completion)}")
                if (s.weeksLeft <= 1) Hint("Out next week", colors.good) else Hint("${Format.weeks(s.weeksLeft)} to go")
                if (Research.MARKETING in state.researched) {
                    Campaign.entries.forEach { c ->
                        val block = state.labelCampaignBlocker(s.pitch.id, c)
                        MacButton(
                            "${c.title} · ${Format.money(c.cost)}",
                            onClick = { store.perform { buyLabelCampaign(s.pitch.id, c) } },
                            enabled = block == null,
                            primary = false,
                        )
                    }
                }
            }
        }
    }

    GlassPanel {
        Text("Pitches", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (g.pitches.isEmpty()) {
            Hint("No pitches right now. Developers send them every few weeks.")
        }
        g.pitches.forEach { p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Hint(p.title)
                    Hint("${p.developer} · ${Format.money(p.funding)} for ${Format.percent(p.labelShare)}")
                }
                MacButton("Review", onClick = { looking = p }, primary = false)
            }
        }
    }

    if (g.published.isNotEmpty()) {
        GlassPanel {
            Text("Published", style = MaterialTheme.typography.titleLarge, color = h.accent)
            g.published.take(8).forEach { r ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${r.title} · ${"%.1f".format(r.score)} · ${Format.money(r.lifetimeSales)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = h.textDim,
                        modifier = Modifier.weight(1f),
                    )
                    MacButton("Report", onClick = { store.openRelease = r.id }, primary = false)
                }
            }
        }
    }

    looking?.let { p ->
        PitchReview(store, state, p) { looking = null }
    }
}

@Composable
private fun PitchReview(store: DesktopStore, state: GameState, p: IndiePitch, onDone: () -> Unit) {
    val blocker = state.signBlocker(p.id)
    val h = hudColors
    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 360.dp, max = 480.dp)
                    .fillMaxWidth(0.5f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(p.title, style = MaterialTheme.typography.headlineMedium, color = h.ink)
                Hint(
                    "${p.developer} needs ${Format.money(p.funding)} for ~${Format.weeks(p.weeks)}. " +
                        "You keep ${Format.percent(p.labelShare)}.",
                )
                Hint("${p.demoTitle}. ${p.lastScore?.let { "Last game $it." } ?: "First game."}")
                if (blocker != null) Hint(blocker, gameColors.bad)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacButton(
                        "Sign",
                        onClick = {
                            store.perform { signPitch(p.id) }
                            onDone()
                        },
                        enabled = blocker == null,
                    )
                    MacButton(
                        "Pass",
                        onClick = {
                            store.perform { passPitch(p.id) }
                            onDone()
                        },
                        primary = false,
                    )
                    MacButton("Not now", onClick = onDone, primary = false)
                }
            }
        }
    }
}

@Composable
internal fun RemastersPanel(store: DesktopStore, state: GameState) {
    if (Research.REMASTERS !in state.researched) return
    val ready = state.remasterable.take(4)
    if (ready.isEmpty()) return
    val h = hudColors
    GlassPanel {
        Text("Ready to remaster", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Hint("Hits three or more years old, rebuilt for today's platforms.")
        ready.forEach { r ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Hint(r.title)
                    Hint("${r.verdict} · sold ${Format.count(r.unitsSold)}")
                }
                MacButton(
                    "Remaster",
                    onClick = { store.designGame(state.remasterConcept(r.id)) },
                    primary = false,
                )
            }
        }
    }
}

@Composable
internal fun EsportsPanel(store: DesktopStore, state: GameState, g: GamesStudio) {
    if (Research.ESPORTS !in state.researched) return
    val h = hudColors
    val colors = gameColors
    var ending by remember { mutableStateOf<GameRelease?>(null) }
    val running = g.leagues
    if (running.isNotEmpty()) {
        GlassPanel {
            Text("Esports", style = MaterialTheme.typography.titleLarge, color = h.accent)
            running.forEach { r ->
                val l = r.league!!
                Hint(l.name)
                Hint("Viewers ${Format.count(l.viewers)} · finals ${l.finals}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrizePool.entries.forEach { p ->
                        SelectChip(p.title, l.prize == p) {
                            store.perform { setPrizePool(r.id, p) }
                        }
                    }
                }
                MacButton("Wind up", onClick = { ending = r }, primary = false)
            }
        }
    }
    val candidates = state.leagueCandidates
    if (candidates.isNotEmpty()) {
        GlassPanel {
            Text("Start a league", style = MaterialTheme.typography.titleLarge, color = h.accent)
            candidates.take(4).forEach { r ->
                val blocker = state.leagueBlocker(r.id)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Hint(r.title)
                        Hint(blocker ?: "${Format.count(maxOf(r.unitsSold, r.live?.players ?: 0.0))} players · ${Format.money(r.leagueCost())}")
                    }
                    MacButton(
                        "Start",
                        onClick = { store.perform { startLeague(r.id) } },
                        enabled = blocker == null,
                    )
                }
            }
        }
    }
    ending?.let { r ->
        ConfirmDialog(
            title = "Wind up the ${r.league!!.name}?",
            text = "The teams go home and the sponsors with them. You can't start another league for ${r.title}.",
            confirm = "Wind up",
            destructive = true,
            onConfirm = { store.perform { endLeague(r.id) } },
            onDismiss = { ending = null },
        )
    }
}

@Composable
internal fun RoadmapPanel(store: DesktopStore, state: GameState, lane: Lane) {
    val features = Feature.forLane(lane)
    if (features.isEmpty()) return
    val h = hudColors
    val colors = gameColors
    val building = state.building.firstOrNull { it.feature.lane == lane }
    GlassPanel {
        Text(
            "Roadmap · ${features.count { it in state.features }} of ${features.size} built",
            style = MaterialTheme.typography.titleLarge,
            color = h.accent,
        )
        Hint("One at a time. While building, the team gets ${Format.percent(1 - BUILD_DRAG)} less done.")
        features.forEach { f ->
            val built = f in state.features
            val inProgress = building?.feature == f
            val blocker = state.featureBlocker(f)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                    Hint(f.detail)
                    Hint(
                        when {
                            built -> "Built"
                            inProgress -> "${Format.weeks(building!!.weeksLeft)} left"
                            else -> "${Format.money(f.cost)} · ${Format.weeks(f.weeks)}"
                        },
                        if (built) colors.good else if (inProgress) colors.warn else h.inkMute,
                    )
                }
                if (!built && !inProgress) {
                    MacButton(
                        "Build",
                        onClick = { store.perform { buildFeature(f) } },
                        enabled = blocker == null,
                        primary = false,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PerksPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    var removing by remember { mutableStateOf<Perk?>(null) }
    GlassPanel {
        Text(
            "Perks · ${state.perks.size} of ${state.office.perkSlots}",
            style = MaterialTheme.typography.titleLarge,
            color = h.accent,
        )
        Hint("${Format.money(state.perkUpkeep)}/wk upkeep. Bigger offices fit more.")
        Perk.entries.forEach { perk ->
            val owned = perk in state.perks
            val blocker = state.perkBlocker(perk)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Hint(perk.title)
                    Hint(
                        if (owned) perk.blurb
                        else "${perk.blurb} · ${Format.money(perk.cost)} + ${Format.money(perk.weeklyCost(state.staff.size))}/wk",
                    )
                }
                if (owned) {
                    MacButton("Remove", onClick = { removing = perk }, primary = false)
                } else {
                    MacButton(
                        "Buy",
                        onClick = { store.perform { buyPerk(perk) } },
                        enabled = blocker == null,
                        primary = false,
                    )
                }
            }
        }
    }
    removing?.let { perk ->
        ConfirmDialog(
            title = "Remove the ${perk.title.lowercase()}?",
            text = "No refund, and people will miss it.",
            confirm = "Remove",
            destructive = true,
            onConfirm = { store.perform { removePerk(perk) } },
            onDismiss = { removing = null },
        )
    }
}

@Composable
internal fun AttentionPanel(store: DesktopStore, state: GameState) {
    if (state.lanes.size <= 1) return
    val h = hudColors
    GlassPanel {
        Text("Attention", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Hint("Your own output is split between your lanes.")
        state.lanes.forEach { lane ->
            CommitSlider(
                state.founder.focus[lane] ?: 1.0,
                { "${lane.title}: ${Format.percent(state.founder.attentionShare(lane, state.lanes))} of you" },
            ) { v -> store.perform { setFocus(lane, v) } }
        }
    }
}

@Composable
internal fun MarketsPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val value = state.portfolioValue
    val cost = state.portfolio.holdings.sumOf { it.cost }
    val gain = value - cost
    GlassPanel {
        Text("Your portfolio", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Shares", Format.money(value), if (gain >= 0) colors.good else colors.bad)
            Stat("Realized", Format.signedMoney(state.portfolio.realized + state.portfolio.dividends))
        }
        if (state.portfolio.holdings.isNotEmpty()) {
            Hint(Format.signedMoney(gain) + " on paper")
        }
    }
    if (state.portfolio.owned.isNotEmpty()) {
        var selling by remember { mutableStateOf<String?>(null) }
        GlassPanel {
            Text("Companies you own", style = MaterialTheme.typography.titleLarge, color = h.accent)
            state.portfolio.owned.forEach { o ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Hint(o.name)
                        Hint("${Format.money(o.lastProfit)}/wk")
                    }
                    MacButton("Sell", onClick = { selling = o.key }, primary = false)
                }
            }
        }
        selling?.let { key ->
            val o = state.portfolio.owned.firstOrNull { it.key == key }
            if (o != null) {
                ConfirmDialog(
                    title = "Sell ${o.name}?",
                    text = "A buyer will pay about 90% of what it's worth.",
                    confirm = "Sell",
                    onConfirm = { store.perform { sellCompany(key) } },
                    onDismiss = { selling = null },
                )
            }
        }
    }
    GlassPanel {
        Text("The market", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Hint("Tap a listed company to buy or sell shares.")
        val shown = state.stocks().filter { it.lane in state.lanes }.ifEmpty { state.stocks() }.take(12)
        shown.forEach { s ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompanyLogo(s.name, size = 22.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Hint(s.name)
                    Hint(
                        if (s.listed) "${s.ticker} · ${sharePrice(s.price)}" +
                            if (s.stake > 0) " · you ${Format.percent(s.stake)}" else ""
                        else "Private",
                    )
                }
                Hint(Format.money(s.value))
                if (s.listed) {
                    MacButton("Trade", onClick = { store.openStock = s.key }, primary = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockSheet(store: DesktopStore, state: GameState, key: String) {
    val s = state.stock(key)
    val h = hudColors
    val colors = gameColors
    var bidding by remember { mutableStateOf<Double?>(null) }
    Dialog(
        onDismissRequest = { store.openStock = null },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 400.dp, max = 560.dp)
                    .fillMaxWidth(0.58f)
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            s?.let { "${it.name} · ${it.ticker}" } ?: "Gone",
                            style = MaterialTheme.typography.headlineMedium,
                            color = h.ink,
                        )
                    }
                    MacButton("Close", onClick = { store.openStock = null }, primary = false)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (s == null) {
                        Hint("This company isn't on the market any more.")
                        return@Column
                    }
                    if (!s.listed) {
                        Hint("${s.name} is privately held. Its shares don't trade.")
                        return@Column
                    }
                    val holding = state.portfolio.holdings.firstOrNull { it.key == key }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Share price", sharePrice(s.price), if (s.change >= 0) colors.good else colors.bad)
                        Stat("Worth", Format.money(s.value))
                    }
                    if (holding != null) {
                        val worth = state.holdingValue(holding)
                        Hint("You own ${Format.percent(s.stake)} · paid ${Format.money(holding.cost)} · now ${Format.money(worth)}")
                    }
                    Hint("Buy")
                    val most = state.maxBuy(key)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (amount in listOf(10_000.0, 100_000.0, 1e6, 1e7)) {
                            if (amount > most * 10 && amount > 10_000) continue
                            MacButton(
                                Format.money(amount),
                                onClick = { store.perform { buyShares(key, amount) } },
                                enabled = state.buyBlocker(key, amount) == null,
                                primary = false,
                            )
                        }
                        if (most >= 1_000) {
                            MacButton("Max ${Format.money(most)}", onClick = { store.perform { buyShares(key, most) } }, primary = false)
                        }
                    }
                    if (holding != null) {
                        Hint("Sell")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MacButton("Half", onClick = { store.perform { sellShares(key, 0.5) } }, primary = false)
                            MacButton("All", onClick = { store.perform { sellShares(key, 1.0) } }, primary = false)
                        }
                    }
                    Hint("Take it over — bid for every share you don't own.")
                    TakeoverPremiums.forEach { premium ->
                        val quote = state.takeoverQuote(key, premium)
                        val blocker = state.takeoverBlocker(key, premium)
                        if (quote != null) {
                            val (cost, odds) = quote
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Hint("${Format.percent(premium)} premium")
                                    Hint("${Format.money(cost)} · ${Format.percent(odds)} likely" + (blocker?.let { " · $it" } ?: ""))
                                }
                                MacButton(
                                    "Bid",
                                    onClick = { bidding = premium },
                                    enabled = blocker == null,
                                    primary = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    bidding?.let { premium ->
        val quote = state.takeoverQuote(key, premium) ?: return
        val (cost, odds) = quote
        ConfirmDialog(
            title = "Bid for ${s?.name}?",
            text = "${Format.money(cost)} is set aside for four weeks. About ${Format.percent(odds)} chance shareholders accept.",
            confirm = "Bid",
            onConfirm = {
                store.perform { bidFor(key, premium) }
                bidding = null
                store.openStock = null
            },
            onDismiss = { bidding = null },
        )
    }
}

@Composable
internal fun CombosPanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    Hint("Every pairing of lanes unlocks a crossover product — and a problem unique to it.")
    if (state.crossovers.isEmpty()) {
        GlassPanel {
            Hint("You run one lane. Combos unlock when you take on a second.")
        }
        return
    }
    state.crossovers.forEach { c ->
        val combo = c.combo
        GlassPanel {
            Text("${combo.first.title} + ${combo.second.title}", style = MaterialTheme.typography.titleLarge, color = h.accent)
            Hint(combo.product)
            Hint(combo.pitch)
            Hint(combo.problem, colors.warn)
            when {
                c.built -> CommitSlider(
                    c.intensity,
                    { "${combo.dial}: ${Format.percent(it.toDouble())}" },
                ) { v -> store.perform { setIntensity(combo, v) } }
                c.isBuilding -> Hint("Building · ${Format.weeks(c.buildWeeksLeft)} left", colors.warn)
                else -> MacButton(
                    "Build for ${Format.money(combo.buildCost)}",
                    onClick = { store.perform { buildCrossover(combo) } },
                )
            }
            if (c.built) {
                Hint("Trouble ${Format.percent(c.pressure)}" + if (c.incidents > 0) " · ${c.incidents} incident(s)" else "")
            }
        }
    }
    GlassPanel {
        Text("All crossovers", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Combo.entries.forEach { combo ->
            val unlocked = state.crossover(combo) != null
            Hint(
                "${combo.product} · ${combo.first.title} + ${combo.second.title}" +
                    if (unlocked) " · unlocked" else " · locked",
            )
        }
    }
}

@Composable
internal fun LaneDepthExtras(store: DesktopStore, state: GameState, lane: Lane) {
    val h = hudColors
    val colors = gameColors
    when (lane) {
        Lane.WEBSITES -> state.web?.let { w ->
            GlassPanel {
                Text("Retainers", style = MaterialTheme.typography.titleLarge, color = h.accent)
                if (w.retainers.isEmpty()) {
                    Hint("No retainers yet. Finish a project that a client loves.")
                } else {
                    w.retainers.forEach { r ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Hint(r.client)
                                Hint("${Format.money(r.weekly)}/wk")
                            }
                            MacButton("End", onClick = { store.perform { dropRetainer(r.client) } }, primary = false)
                        }
                    }
                }
            }
        }
        Lane.APPS -> state.apps?.let { a ->
            GlassPanel {
                Text("Price", style = MaterialTheme.typography.titleLarge, color = h.accent)
                Hint(a.pricing.detail)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppPricing.entries.forEach { p ->
                        SelectChip(p.title, a.pricing == p) { store.perform { setAppPricing(p) } }
                    }
                }
            }
            if (a.isLive) {
                GlassPanel {
                    Text("Experiments", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    val running = a.experiment
                    if (running != null) {
                        Hint("Testing ${running.kind.title.lowercase()} · ${Format.weeks(running.weeksLeft)} left", colors.warn)
                    } else {
                        val untried = ExperimentKind.entries.filter { it !in a.experimentsRun }
                        if (untried.isEmpty()) Hint("You've tried every experiment.")
                        untried.forEach { k ->
                            val blocker = state.experimentBlocker(k)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Hint(k.title)
                                    Hint(blocker ?: "${Format.money(state.experimentCost)} · ${k.detail}")
                                }
                                MacButton(
                                    "Run",
                                    onClick = { store.perform { startExperiment(k) } },
                                    enabled = blocker == null,
                                    primary = false,
                                )
                            }
                        }
                    }
                }
            }
        }
        Lane.SOCIAL -> state.social?.let { s ->
            GlassPanel {
                Text("Growth and rules", style = MaterialTheme.typography.titleLarge, color = h.accent)
                Hint("Creator fund" + if (s.creatorFund > 0) " · ${Format.money(s.creatorFundCost)}/wk" else "")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Off", 1 to "Small", 2 to "Big").forEach { (v, label) ->
                        SelectChip(label, s.creatorFund == v) { store.perform { setCreatorFund(v) } }
                    }
                }
                Hint("House rules · ${s.policy.detail}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ContentPolicy.entries.forEach { p ->
                        SelectChip(p.title, s.policy == p) { store.perform { setContentPolicy(p) } }
                    }
                }
                SelectChip(
                    if (s.premium) "Premium · on" else "Premium · off",
                    s.premium,
                ) { store.perform { setPremium(!s.premium) } }
            }
        }
        Lane.HARDWARE -> state.hardware?.let { hw ->
            if (hw.onMarket != null) {
                GlassPanel {
                    Text("Price", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    val shifts = listOf(-0.2, -0.1, 0.0, 0.1, 0.2)
                    val now = shifts.minByOrNull { kotlin.math.abs(it - hw.priceShift) } ?: 0.0
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        shifts.forEach { v ->
                            SelectChip(
                                if (v == 0.0) "List" else (if (v > 0) "+" else "−") + Format.percent(kotlin.math.abs(v)),
                                now == v,
                            ) { store.perform { setDevicePrice(v) } }
                        }
                    }
                }
            }
            if (hw.design != null) {
                GlassPanel {
                    Text("Launch event", style = MaterialTheme.typography.titleLarge, color = h.accent)
                    if (hw.keynote > 0) {
                        Hint(if (hw.keynote == 2) "Keynote booked" else "Livestream booked")
                    } else {
                        MacButton(
                            "Livestream · ${Format.money(keynoteCost(1))}",
                            onClick = { store.perform { bookKeynote(1) } },
                            enabled = state.cash >= keynoteCost(1),
                            primary = false,
                        )
                        MacButton(
                            "Keynote · ${Format.money(keynoteCost(2))}",
                            onClick = { store.perform { bookKeynote(2) } },
                            enabled = state.cash >= keynoteCost(2),
                            primary = false,
                        )
                    }
                }
            }
        }
        Lane.AI -> state.ai?.let { lab ->
            GlassPanel {
                Text("Reserved compute", style = MaterialTheme.typography.titleLarge, color = h.accent)
                Hint("Reserved GPUs cost 40% less than renting, for a year.")
                if (lab.reservedUnits > 0 && state.week < lab.reservedUntil) {
                    Hint("${lab.reservedUnits} units reserved until week ${lab.reservedUntil}")
                } else {
                    listOf(4, 16).forEach { units ->
                        MacButton(
                            "Reserve $units · ${Format.money(state.reservedRate * units)}/wk",
                            onClick = { store.perform { reserveCompute(units) } },
                            enabled = state.cash >= state.reservedRate * units * 4,
                            primary = false,
                        )
                    }
                }
            }
        }
        Lane.GAMES, Lane.OS -> {}
    }
    // Rival counters for this lane
    state.world.rival(lane)?.let { rival ->
        GlassPanel {
            Text("Rival · ${rival.name}", style = MaterialTheme.typography.titleLarge, color = h.accent)
            CompanyLogo(rival.name, size = 28.dp)
            Hint("Pressure moves against them.")
            CounterMove.entries.forEach { move ->
                val blocker = state.counterBlocker(rival, move)
                val cost = state.counterCost(rival, move)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Hint(move.title)
                        Hint(blocker ?: "${move.detail}" + if (cost > 0) " · ${Format.money(cost)}" else "")
                    }
                    MacButton(
                        "Counter",
                        onClick = { store.perform { counter(rival.name, move) } },
                        enabled = blocker == null,
                        primary = false,
                    )
                }
            }
            val pressures = state.pressuresOn(lane)
            if (pressures.isNotEmpty()) {
                Hint("${pressures.size} pressure(s) active on this market", colors.warn)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = hudColors.text) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = hudColors.textDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}
