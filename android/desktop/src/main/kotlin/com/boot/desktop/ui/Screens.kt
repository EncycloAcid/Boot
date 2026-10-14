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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.boot.core.Difficulty
import com.boot.core.Fit
import com.boot.core.Format
import com.boot.core.GameConcept
import com.boot.core.GameFocus
import com.boot.core.GameSeason
import com.boot.core.GameSize
import com.boot.core.GameState
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.StartPoint
import com.boot.core.GameException
import com.boot.core.acknowledgeReview
import com.boot.core.estimatedWeeks
import com.boot.core.evenFocus
import com.boot.core.gameForecast
import com.boot.core.gameTeam
import com.boot.core.hire
import com.boot.core.launchGame
import com.boot.core.platformPlayers
import com.boot.core.platformsOnSale
import com.boot.core.playtestGame
import com.boot.core.research
import com.boot.core.researchBlocker
import com.boot.core.setCreativeFocus
import com.boot.core.startGame
import com.boot.core.suggestGameTitle
import com.boot.core.unlockedSizes
import com.boot.desktop.DesktopStore

@Composable
fun NewGameScreen(store: DesktopStore) {
    val h = hudColors
    var company by remember { mutableStateOf("Garage Labs") }
    var founder by remember { mutableStateOf("You") }
    var lane by remember { mutableStateOf(Lane.GAMES) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var start by remember { mutableStateOf(StartPoint.TODAY) }

    Box(Modifier.fillMaxSize().background(Color(0xFFF4F1EA))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .width(720.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Boot", style = MaterialTheme.typography.displayLarge, color = h.text)
            Text(
                "Windows · Game Dev Tycoon–style floating HUD",
                style = MaterialTheme.typography.bodyMedium,
                color = h.textDim,
            )
            GlassPanel {
                Text("Company", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                LightField(company) { company = it }
                Spacer(Modifier.height(8.dp))
                Text("Founder", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                LightField(founder) { founder = it }
            }
            GlassPanel {
                Text("Starting lane", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (l in listOf(Lane.GAMES, Lane.WEBSITES, Lane.APPS, Lane.SOCIAL)) {
                        SelectChip(l.title, lane == l) { lane = l }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (l in listOf(Lane.HARDWARE, Lane.AI, Lane.OS)) {
                        SelectChip(l.title, lane == l) { lane = l }
                    }
                }
                Hint(lane.pitch)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in listOf(
                        StartPoint.TODAY,
                        StartPoint.HOME_COMPUTERS,
                        StartPoint.CONSOLE_WARS,
                        StartPoint.SMARTPHONES,
                    )) {
                        SelectChip(s.title.substringBefore(":").ifEmpty { s.title }, start == s) { start = s }
                    }
                }
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
                        tutorial = false,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (store.hall.isNotEmpty()) {
                Hint("${store.hall.size} finished run(s) under ~/.boot-desktop")
            }
        }
    }
}

@Composable
private fun LightField(value: String, onChange: (String) -> Unit) {
    val h = hudColors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = h.text),
        cursorBrush = SolidColor(h.accent),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(10.dp))
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
        contentPadding = ScreenPad,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionTitle("Lanes", "Each business is its own management game.") }
        if (state.lanes.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (l in state.lanes) {
                        SelectChip(l.title, l == lane) { store.lane = l }
                    }
                }
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
                    Stat("Team", "${state.staff(lane).size}")
                    Stat("Market", Format.percent(state.world.era.demand(lane) * state.world.competition(lane)))
                }
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
    val h = hudColors
    when (lane) {
        Lane.GAMES -> GamesStudioPanel(store, state)
        Lane.WEBSITES -> {
            val w = state.web
            GlassPanel {
                Text("Web agency", style = MaterialTheme.typography.titleLarge, color = h.accent)
                if (w == null) {
                    Hint("Agency not open.")
                    return@GlassPanel
                }
                Stat("Standing", Format.percent(w.standing))
                Hint("${w.active.size} active · ${w.offers.size} offers · ${w.completed} delivered")
            }
        }
        else -> {
            GlassPanel {
                Text(lane.businessName, style = MaterialTheme.typography.titleLarge, color = h.accent)
                Hint("Headline stats on desktop; deep craft UI stays on Android.")
            }
        }
    }
}

/** Games lane with real choices: start, creative direction, playtest, seasonal timing, ship. */
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
            Stat("Best", if (g.topScore > 0) "%.1f".format(g.topScore) else "—")
            Stat("Season", season.title, h.accent)
        }
        Hint(season.marketTip)

        g.unseenReview?.let { id -> g.release(id) }?.let { review ->
            Spacer(Modifier.height(4.dp))
            Hint("${review.title} scored ${"%.1f".format(review.score)} — ${review.verdict}", colors.good)
            MacButton("Continue", onClick = { store.perform { acknowledgeReview() } })
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
                Stat("Design", Format.count(project.design), colors.design)
                Stat("Tech", Format.count(project.tech), colors.tech)
                Stat("Bugs", Format.count(project.bugs), colors.bad)
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
                MacButton("Release ${project.title}", onClick = { store.perform { launchGame() } }, primary = true)
            } else {
                Hint("Advance weeks from the HUD — change direction or playtest between ticks.")
            }
        } else {
            Hint("No game in development. Kick off a solid concept with one click.")
            MacButton(
                "Start a new game",
                onClick = { store.perform { quickStartDesktopGame() } },
                primary = true,
            )
        }

        if (g.releases.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Hint("Recent releases")
            g.releases.take(4).forEach { r ->
                Text(
                    "★ ${r.title} · ${"%.1f".format(r.score)} · ${Format.money(r.lifetimeSales)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = h.textDim,
                )
            }
        }
    }
}

/** Desktop one-click concept: best known topic/genre fit, biggest affordable size, PC (plus reach). */
private fun GameState.quickStartDesktopGame() {
    val g = games ?: throw GameException(GameException.Kind.INVALID, "Games lane isn't open.")
    val genre = Genre.entries.maxBy { candidate ->
        g.topics.maxOf { t -> (g.knownFit(t, candidate)?.multiplier ?: 0.98) }
    }
    val topic = g.topics.maxBy { (g.knownFit(it, genre)?.multiplier ?: 0.98) }
    val size = unlockedSizes
        .filter { gameTeam >= it.minTeam }
        .lastOrNull { cash > it.weeklyBudget * estimatedWeeks(it) * 2.5 + 10_000 }
        ?: GameSize.SMALL
    val platforms = platformsOnSale
        .filter { it.fit(genre) != Fit.BAD || it == Platform.PC }
        .sortedByDescending { platformPlayers(it) * it.spend }
        .take(if (Research.MULTI_PLATFORM in researched) 2 else 1)
        .ifEmpty { listOf(Platform.PC) }
    val concept = GameConcept(
        title = suggestGameTitle(topic, genre),
        topic = topic,
        genre = genre,
        platforms = platforms,
        size = size,
        engineId = g.engines.maxBy { it.level }.id,
    )
    startGame(concept, evenFocus())
}

@Composable
fun ResearchScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    val affordable = remember(state.researchPoints, state.researched, state.lanes, state.week) {
        Research.entries.filter { state.researchBlocker(it) == null }.take(24)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ScreenPad,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("Research", "${state.researchPoints.toInt()} RP in the bank") }
        item {
            GlassPanel {
                Hint("Buy unlocks with research points.")
                Text("Unlocked: ${state.researched.size}", style = MaterialTheme.typography.titleMedium, color = h.cash)
            }
        }
        items(affordable, key = { it.name }) { r ->
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, color = h.text)
                        Hint(r.detail)
                        Hint("${r.rp} RP · ${r.kind.name.lowercase()}", h.accent)
                    }
                    MacButton("Buy", onClick = { store.perform { research(r) } })
                }
            }
        }
        if (affordable.isEmpty()) {
            item { Hint("Nothing affordable right now — advance weeks to earn RP.") }
        }
    }
}

@Composable
fun TeamScreen(store: DesktopStore, state: GameState) {
    val h = hudColors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ScreenPad,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionTitle("Team", "${state.staff.size} hired · ${state.office.title}") }
        item {
            GlassPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FounderPortrait(state, 48.dp)
                    Column {
                        Text(state.founder.name, style = MaterialTheme.typography.titleLarge, color = h.text)
                        Hint("Founder")
                    }
                }
            }
        }
        items(state.staff, key = { it.id }) { e ->
            GlassPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StaffPortrait(e, 44.dp)
                    Column {
                        Text(e.name, style = MaterialTheme.typography.titleMedium, color = h.text)
                        Hint("${e.role.title} · ${e.lane?.title ?: "unassigned"} · ${Format.money(e.salary)}/wk")
                        e.trait?.let { Hint(it.title, h.cash) }
                    }
                }
            }
        }
        item {
            GlassPanel {
                Text("Talent pool", style = MaterialTheme.typography.titleLarge, color = h.accent)
                if (state.candidates.isEmpty()) {
                    Hint("No candidates this week. Advance time to refresh the pool.")
                } else {
                    state.candidates.take(6).forEach { cand ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cand.name, style = MaterialTheme.typography.titleMedium, color = h.text)
                                Hint("${cand.role.title} · ${Format.money(cand.askingSalary)}/wk ask")
                            }
                            MacButton("Hire", onClick = { store.perform { hire(cand.id) } })
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
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPad),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("Company", state.companyName)
        GlassPanel {
            Text(Format.money(state.valuation), style = MaterialTheme.typography.displayLarge, color = h.accent)
            Hint("Valuation")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Cash", Format.money(state.cash), if (state.cash < 0) colors.bad else h.text)
                Stat("Weekly", Format.signedMoney(weekly), if (weekly >= 0) colors.good else colors.bad)
                Stat("Reputation", "${state.reputation.score.toInt()}")
                Stat("Runway", if (state.runwayWeeks.isInfinite()) "∞" else "${state.runwayWeeks.toInt()}w")
            }
        }
        GlassPanel {
            Text("Era · ${state.world.era.title}", style = MaterialTheme.typography.titleLarge, color = h.text)
            Hint(state.world.era.blurb)
        }
        GlassPanel {
            Text("Abandon run", style = MaterialTheme.typography.titleMedium, color = colors.bad)
            Hint("Ends this company and returns to New Game.")
            MacButton("Abandon", onClick = { store.abandon() }, primary = false)
        }
    }
}
