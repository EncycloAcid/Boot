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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Difficulty
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Research
import com.boot.core.StartPoint
import com.boot.core.hire
import com.boot.core.research
import com.boot.core.researchBlocker
import com.boot.desktop.DesktopStore

@Composable
fun NewGameScreen(store: DesktopStore) {
    val c = spriteColors
    var company by remember { mutableStateOf("Garage Labs") }
    var founder by remember { mutableStateOf("You") }
    var lane by remember { mutableStateOf(Lane.GAMES) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var start by remember { mutableStateOf(StartPoint.TODAY) }

    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawScanBackdrop(c.ink, c.inkRaised)
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .width(720.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("BOOT", style = MaterialTheme.typography.displayLarge, color = c.amber)
            Text(
                "Windows sprite port · fullscreen office shell",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textDim,
            )
            SpritePanel {
                Text("Company", style = MaterialTheme.typography.labelLarge, color = c.mute)
                PixelField(company) { company = it }
                Spacer(Modifier.height(8.dp))
                Text("Founder", style = MaterialTheme.typography.labelLarge, color = c.mute)
                PixelField(founder) { founder = it }
            }
            SpritePanel {
                Text("Starting lane", style = MaterialTheme.typography.labelLarge, color = c.mute)
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
                Hint(lane.pitch, c.textDim)
            }
            SpritePanel {
                Text("Difficulty", style = MaterialTheme.typography.labelLarge, color = c.mute)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in Difficulty.entries) {
                        SelectChip(d.title, difficulty == d) { difficulty = d }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("When", style = MaterialTheme.typography.labelLarge, color = c.mute)
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
            SpriteButton(
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
                Hint("${store.hall.size} finished run(s) in the Hall of Fame under ~/.boot-desktop")
            }
        }
    }
}

@Composable
private fun PixelField(value: String, onChange: (String) -> Unit) {
    val c = spriteColors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = c.text),
        cursorBrush = SolidColor(c.amber),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .background(c.inkRaised, RoundedCornerShape(4.dp))
            .border(2.dp, c.panelEdge, RoundedCornerShape(4.dp))
            .padding(12.dp),
    )
}

@Composable
fun OfficeScreen(store: DesktopStore, state: GameState) {
    val c = spriteColors
    Box(Modifier.fillMaxSize()) {
        OfficeDiorama(state, Modifier.fillMaxSize())
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(ScreenPad)
                .width(420.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpritePanel {
                Text(state.office.title, style = MaterialTheme.typography.headlineMedium, color = c.amber)
                Hint("${state.companyName} · ${state.dateLabel}")
                Hint(state.world.era.title, c.mint)
                val log = state.log.takeLast(3)
                if (log.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    for (entry in log.reversed()) {
                        Text("· ${entry.text}", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
                    }
                }
            }
            if (state.needsDecision) {
                SpritePanel {
                    Text("Decision waiting", style = MaterialTheme.typography.titleMedium, color = c.danger)
                    Hint("Pause the clock and resolve events on Android for full sheets — desktop v1 shows the hold.")
                    val ev = state.events.firstOrNull()
                    if (ev != null) Hint(ev.title, c.text)
                }
            }
        }
    }
}

@Composable
fun LanesScreen(store: DesktopStore, state: GameState) {
    val c = spriteColors
    val lane = store.lane?.takeIf { it in state.lanes } ?: state.lanes.first()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ScreenPad,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle("Lanes", "Each business is its own management game.")
        }
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
            SpritePanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LaneDot(c.lane(lane), 28.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(lane.businessName, style = MaterialTheme.typography.headlineMedium, color = c.text)
                        Hint(lane.pitch)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val rev = state.trailingRevenue(lane, 4) / 4
                val profit = state.trailingProfit(lane, 4) / 4
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HudStat("Revenue /wk", Format.money(rev))
                    HudStat("Profit /wk", Format.signedMoney(profit), if (profit >= 0) c.mint else c.danger)
                    HudStat("Team", "${state.staff(lane).size}")
                    HudStat("Market", Format.percent(state.world.era.demand(lane) * state.world.competition(lane)))
                }
            }
        }
        item { LaneDepthPanel(store, state, lane) }
    }
}

@Composable
private fun LaneDepthPanel(store: DesktopStore, state: GameState, lane: Lane) {
    val c = spriteColors
    when (lane) {
        Lane.GAMES -> {
            val g = state.games
            SpritePanel {
                Text("Game studio", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (g == null) {
                    Hint("Games lane book not open yet.")
                    return@SpritePanel
                }
                HudStat("Fans", Format.count(g.fans))
                val project = g.project
                if (project != null) {
                    Hint("In progress: ${project.title} · stage ${project.stage + 1}/3")
                    Hint("Progress ${Format.percent(project.completion)}")
                } else {
                    Hint("No game in development. Design games on Android; desktop shows live status.")
                }
                g.releases.takeLast(3).reversed().forEach { r ->
                    Text("★ ${r.title} · ${"%.1f".format(r.score)}", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
                }
            }
        }
        Lane.WEBSITES -> {
            val w = state.web
            SpritePanel {
                Text("Web agency", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (w == null) {
                    Hint("Agency not open.")
                    return@SpritePanel
                }
                HudStat("Standing", Format.percent(w.standing))
                Hint("${w.active.size} active · ${w.offers.size} offers · ${w.completed} delivered")
                w.active.take(4).forEach { job ->
                    Text(
                        "${job.client} · ${Format.percent(job.completion)}" + if (job.isOverdue) " OVERDUE" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (job.isOverdue) c.danger else c.textDim,
                    )
                }
            }
        }
        Lane.APPS -> {
            val a = state.apps
            SpritePanel {
                Text("App studio", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (a == null) {
                    Hint("Apps not open.")
                    return@SpritePanel
                }
                Hint("Focus this screen on retention & updates. Full craft UI is on Android.")
                Text(
                    if (a.isLive) "Live · ${Format.count(a.users)} users · quality ${a.quality.toInt()}"
                    else "Building ${Format.percent(a.buildProgress / a.buildWork)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.text,
                )
            }
        }
        Lane.SOCIAL -> {
            val s = state.social
            SpritePanel {
                Text("Social platform", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (s == null) {
                    Hint("Social not open.")
                    return@SpritePanel
                }
                Hint("Users and moderation live here. Desktop shows headline stats.")
            }
        }
        Lane.HARDWARE -> {
            val h = state.hardware
            SpritePanel {
                Text("Device lab", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (h == null) {
                    Hint("Hardware not open.")
                    return@SpritePanel
                }
                Hint("Device Designer depth is on Android; this pane tracks the lab.")
            }
        }
        Lane.AI -> {
            val ai = state.ai
            SpritePanel {
                Text("AI lab", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (ai == null) {
                    Hint("AI not open.")
                    return@SpritePanel
                }
                Hint("Compute burn and training runs — full controls on Android.")
            }
        }
        Lane.OS -> {
            val os = state.os
            SpritePanel {
                Text("Platform Lab", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (os == null) {
                    Hint("OS division not open.")
                    return@SpritePanel
                }
                Hint("Kernel / shell projects and OEM licensing.")
            }
        }
    }
}

@Composable
fun ResearchScreen(store: DesktopStore, state: GameState) {
    val c = spriteColors
    val affordable = remember(state.researchPoints, state.researched, state.lanes, state.week) {
        Research.entries.filter { state.researchBlocker(it) == null }.take(24)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ScreenPad,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Research", "${state.researchPoints.toInt()} RP in the bank")
        }
        item {
            SpritePanel {
                Hint("Buy unlocks with research points. Engine / studio / company / lane trees scale from here.")
                Text("Unlocked: ${state.researched.size}", style = MaterialTheme.typography.titleMedium, color = c.mint)
            }
        }
        items(affordable, key = { it.name }) { r ->
            SpritePanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Hint(r.detail)
                        Hint("${r.rp} RP · ${r.kind.name.lowercase()}", c.amber)
                    }
                    SpriteButton("Buy", onClick = { store.perform { research(r) } }, primary = true)
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
    val c = spriteColors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = ScreenPad,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Team", "${state.staff.size} hired · ${state.office.title}")
        }
        item {
            SpritePanel {
                Text(state.founder.name, style = MaterialTheme.typography.titleLarge, color = c.amber)
                Hint("Founder · attention across ${state.lanes.joinToString { it.title }}")
            }
        }
        items(state.staff, key = { it.id }) { e ->
            SpritePanel {
                Text(e.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                Hint("${e.role.title} · ${e.lane?.title ?: "unassigned"} · ${Format.money(e.salary)}/wk")
                e.trait?.let { Hint(it.title, c.mint) }
            }
        }
        item {
            SpritePanel {
                Text("Talent pool", style = MaterialTheme.typography.titleLarge, color = c.amber)
                if (state.candidates.isEmpty()) {
                    Hint("No candidates this week. Advance time to refresh the pool.")
                } else {
                    state.candidates.take(6).forEach { cand ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cand.name, style = MaterialTheme.typography.titleMedium, color = c.text)
                                Hint("${cand.role.title} · ${Format.money(cand.askingSalary)}/wk ask")
                            }
                            SpriteButton("Hire", onClick = { store.perform { hire(cand.id) } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompanyScreen(store: DesktopStore, state: GameState) {
    val c = spriteColors
    val weekly = state.trailingProfit(weeks = 4) / 4
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPad),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("Company", state.companyName)
        SpritePanel {
            Text(Format.money(state.valuation), style = MaterialTheme.typography.displayLarge, color = c.amber)
            Hint("Valuation")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HudStat("Cash", Format.money(state.cash), if (state.cash < 0) c.danger else c.text)
                HudStat("Weekly", Format.signedMoney(weekly), if (weekly >= 0) c.mint else c.danger)
                HudStat("Reputation", "${state.reputation.score.toInt()}")
                HudStat(
                    "Runway",
                    if (state.runwayWeeks.isInfinite()) "∞" else "${state.runwayWeeks.toInt()}w",
                )
            }
        }
        SpritePanel {
            Text("Era · ${state.world.era.title}", style = MaterialTheme.typography.titleLarge, color = c.mint)
            Hint(state.world.era.blurb)
        }
        SpritePanel {
            Text("Abandon run", style = MaterialTheme.typography.titleMedium, color = c.danger)
            Hint("Ends this company and returns to New Game. Save is cleared.")
            SpriteButton("Abandon", onClick = { store.abandon() }, primary = false)
        }
    }
}
