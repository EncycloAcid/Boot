package com.boot.game.ui

import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import com.boot.core.ranked
import com.boot.core.GameClock
import com.boot.core.EndingKind
import com.boot.core.LegacyPerk
import com.boot.core.RunRecord
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import com.boot.game.R
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boot.core.Difficulty
import com.boot.core.MODERN_YEAR
import com.boot.core.StartPoint
import com.boot.core.Scenario
import com.boot.core.Medal
import com.boot.core.bestMedals
import com.boot.core.era
import com.boot.core.laneOpen
import com.boot.core.years
import com.boot.core.arrives
import com.boot.core.arrival
import com.boot.core.platformHoldersIn
import com.boot.core.publishersIn
import com.boot.core.rivalIn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.ui.draw.alpha
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.MENTOR
import com.boot.game.GameStore
import com.boot.game.ScreenLayout

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewGameScreen(store: GameStore) {
    var company by rememberSaveable { mutableStateOf("Garage Labs") }
    var founder by rememberSaveable { mutableStateOf("") }
    var lane by rememberSaveable { mutableStateOf(Lane.GAMES) }
    var difficulty by rememberSaveable { mutableStateOf(Difficulty.NORMAL) }
    var start by rememberSaveable { mutableStateOf(StartPoint.TODAY) }
    var mode by rememberSaveable { mutableStateOf(GameMode.CAREER) }
    var tutorial by rememberSaveable { mutableStateOf(store.hall.isEmpty()) }
    var scenario by rememberSaveable { mutableStateOf(Scenario.GARAGE_TO_GLORY) }
    var whenReason by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = gameColors
    val companyName = company.trim().ifEmpty { "Garage Labs" }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenHeightDp < 600
    val founderName = founder.trim().ifEmpty { "You" }

    // Start the run: a warning for the capital-intensive businesses, then the button.
    val startPanel: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (landscape) Modifier else Modifier.navigationBarsPadding())
                .padding(if (landscape) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (mode == GameMode.CAREER && lane.isCapitalIntensive) {
                Hint(
                    "Hard mode: ${lane.title} starts with a seed round and a board that can overrule you, or fire you.",
                    colors.warn,
                )
            }
            when (mode) {
                GameMode.SCENARIOS -> PrimaryButton(
                    "Play ${scenario.title}", { store.newScenario(scenario, companyName, founderName) }, Modifier.fillMaxWidth(),
                )
                GameMode.SANDBOX -> PrimaryButton(
                    "Start your sandbox ${lane.businessName.lowercase()}",
                    {
                        store.newGame(
                            companyName, founderName, lane,
                            difficulty = difficulty, start = start, sandbox = true, tutorial = tutorial,
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
                GameMode.CAREER -> PrimaryButton(
                    "Start your ${lane.businessName.lowercase()}",
                    { store.newGame(companyName, founderName, lane, difficulty = difficulty, start = start, tutorial = tutorial) },
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Everything to choose, in a scrolling grid.
    val settings: @Composable (Modifier, PaddingValues) -> Unit = { modifier, padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = modifier,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!landscape) item(span = { GridItemSpan(maxLineSpan) }) {
                // Where it all starts: the apartment, with a poster of whatever you pick.
                val preview = remember(lane) { GameState.create("Garage Labs", "You", lane, 1) }
                OfficeScene(
                    preview, playing = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(28.dp)),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!landscape) BootTitle()
                    Text(
                        "You start solo in a cramped apartment with ${Format.money(difficulty.startingCash)} in savings" +
                            (start.year?.let { " in $it" } ?: "") + ". Pick a lane.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Hint(
                        "Mid-game, the Pivot Point offers a second lane; take it or stay focused. Every pairing unlocks its own crossover. " +
                            "Hardware, software and services together make an ecosystem company. " +
                            "IPO, sell, stay indie or go bust: the ending reflects how you got there.",
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection("How to play", footer = mode.blurb) {
                    Column(Modifier.padding(8.dp)) { Segmented(GameMode.entries, mode, { it.title }) { mode = it } }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection("Screen", footer = store.layout.blurb) {
                    Column(Modifier.padding(8.dp)) {
                        Segmented(ScreenLayout.entries, store.layout, { it.title }) { store.chooseLayout(it) }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection("Sound") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { SoundSliders(store) }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection("Your company") {
                    FormRow("Company", company, "Garage Labs", ImeAction.Next) { company = it }
                    RowDivider()
                    FormRow("Founder", founder, "You", ImeAction.Done) { founder = it }
                }
            }
            if (mode == GameMode.SCENARIOS) {
                item(span = { GridItemSpan(maxLineSpan) }) { ScenarioList(scenario, store.hall.bestMedals()) { scenario = it } }
            }
            if (mode == GameMode.CAREER || mode == GameMode.SANDBOX) item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection("Difficulty", footer = difficulty.blurb) {
                    Column(Modifier.padding(8.dp)) {
                        Segmented(Difficulty.entries, difficulty, { it.title }) { difficulty = it }
                    }
                }
            }
            if (mode == GameMode.CAREER || mode == GameMode.SANDBOX) item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection(null) {
                    ToggleRow(
                        "Show me the ropes", tutorial,
                        "$MENTOR, a founder who's done it all before, walks you through your first weeks.",
                    ) { tutorial = it }
                }
            }
            if (mode != GameMode.SCENARIOS) item(span = { GridItemSpan(maxLineSpan) }) {
                StartSection(start, whenReason) { picked ->
                    start = picked
                    // Snap to the best open lane for that year; explain with Lane.arrival.
                    if (!picked.laneOpen(lane)) {
                        val closed = lane
                        lane = if (picked.laneOpen(Lane.GAMES)) Lane.GAMES else Lane.entries.first { picked.laneOpen(it) }
                        whenReason = "${closed.title} isn't a business yet. ${closed.arrival}"
                    } else {
                        whenReason = null
                    }
                }
            }
            if (mode != GameMode.SCENARIOS) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Pick a lane",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            if (mode != GameMode.SCENARIOS) items(Lane.entries) { l ->
                val open = start.laneOpen(l)
                LaneTile(l, selected = l == lane, arrives = if (open) null else l.arrives) { if (open) lane = l }
            }
            if (mode != GameMode.SCENARIOS) item(span = { GridItemSpan(maxLineSpan) }) {
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = lane,
                    transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut() },
                    label = "pitch",
                ) { l ->
                    GroupedSection(null) {
                        CardBlock(spacing = 6.dp) {
                            Text(l.businessName, style = MaterialTheme.typography.titleMedium)
                            Text(l.pitch, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (store.hall.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { LegacySection(store.hall) }
                item(span = { GridItemSpan(maxLineSpan) }) { HallOfFame(store.hall) }
            }
        }
    }

    if (landscape) {
        // Sideways: the office, the name and the start button on the left; the choices scroll on the right.
        Row(
            Modifier
                .fillMaxSize()
                .background(colors.grouped)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(
                Modifier.width(320.dp).fillMaxHeight().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val preview = remember(lane) { GameState.create("Garage Labs", "You", lane, 1) }
                OfficeScene(preview, playing = false, modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp)))
                BootTitle()
                startPanel()
            }
            settings(Modifier.weight(1f).fillMaxHeight(), PaddingValues(0.dp))
        }
    } else {
        Scaffold(
            containerColor = colors.grouped,
            bottomBar = {
                Surface(color = colors.bar) {
                    Column {
                        HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                        startPanel()
                    }
                }
            },
        ) { padding -> settings(Modifier.fillMaxSize(), padding) }
    }
}

/** The logo and the name. */
@Composable
private fun BootTitle() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(52.dp))
        Text("Boot", style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.primary)
    }
}

/** An iOS form cell: label on the left, an inline text field on the right. */
@Composable
private fun FormRow(label: String, value: String, placeholder: String, ime: ImeAction, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(96.dp))
        val style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ime),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline))
                    inner()
                }
            },
        )
    }
}

@Composable
private fun LaneTile(lane: Lane, selected: Boolean, arrives: Int? = null, onClick: () -> Unit) {
    val colors = gameColors
    val accent = colors.lane(lane)
    val border by animateColorAsState(if (selected) accent else colors.card, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "border")
    val width by animateDpAsState(if (selected) 2.5.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "width")
    Card(
        onClick = onClick,
        enabled = arrives == null,
        modifier = Modifier.fillMaxWidth().alpha(if (arrives == null) 1f else 0.55f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(width, border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                LaneBadge(lane, 56.dp, selected = selected, morph = true)
                Spacer(Modifier.weight(1f))
                if (arrives != null) Pill("From $arrives", MaterialTheme.colorScheme.onSurfaceVariant)
                else if (lane.isCapitalIntensive) Pill("VC", colors.warn)
            }
            Spacer(Modifier.height(12.dp))
            Text(lane.title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (arrives != null) "From $arrives · ${lane.arrival}" else lane.businessName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Career, a scenario, or a sandbox. */
private enum class GameMode(val title: String, val blurb: String) {
    CAREER("Career", "Build a company your way, today or from any point in tech history. Your runs go in the Hall of Fame."),
    SCENARIOS("Scenarios", "A starting situation, an objective and a deadline. Beat it in half the time for gold."),
    SANDBOX("Sandbox", "A billion dollars and no bankruptcy. Just build. Sandbox runs don't go in the Hall of Fame and don't earn achievements."),
}

/** The scenarios, with your best medal in each. */
@Composable
private fun ScenarioList(selected: Scenario, medals: Map<Scenario, Medal>, onPick: (Scenario) -> Unit) {
    val colors = gameColors
    GroupedSection("Scenarios", footer = "Gold for meeting the objective in the first half of the time, silver in three quarters, bronze by the deadline.") {
        Scenario.entries.forEachIndexed { i, sc ->
            if (i > 0) RowDivider(68.dp)
            val on = sc == selected
            ListRow(
                sc.title,
                subtitle = sc.blurb + "\n" + sc.goal + " in ${sc.years} years · " + sc.lane.businessName +
                    (sc.year?.let { " · from $it" } ?: "") + if (sc.difficulty == Difficulty.HARD) " · hard" else "",
                leading = { LaneBadge(sc.lane, 40.dp) },
                onClick = { onPick(sc) },
                trailing = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        medals[sc]?.let { m -> Pill(m.title, medalColor(m, colors)) }
                        Icon(
                            if (on) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                            tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        )
                    }
                },
            )
        }
    }
}

/** A medal's colour. */
fun medalColor(m: Medal, colors: GameColors): androidx.compose.ui.graphics.Color = when (m) {
    Medal.GOLD -> androidx.compose.ui.graphics.Color(0xFFC9A227)
    Medal.SILVER -> androidx.compose.ui.graphics.Color(0xFF8E9AAF)
    Medal.BRONZE -> androidx.compose.ui.graphics.Color(0xFFB0703C)
}

/** When the run starts: today, or at one of tech's revolutions, playing through to the present. */
@Composable
private fun StartSection(start: StartPoint, reason: String?, onPick: (StartPoint) -> Unit) {
    val year = start.year ?: MODERN_YEAR
    val faces = buildList {
        add(rivalIn(Lane.GAMES, year))
        addAll(publishersIn(year).take(5))
        addAll(platformHoldersIn(year).take(3))
    }.distinct().take(8)
    GroupedSection("When", footer = listOfNotNull(start.blurb, reason).joinToString("\n")) {
        StartPoint.entries.forEachIndexed { i, p ->
            if (i > 0) RowDivider(56.dp)
            val selected = p == start
            ListRow(
                p.title,
                subtitle = if (p.year == null) "${p.years} years from now" else "Decades: ${p.years} years to today · ${p.era?.title}",
                leading = { Text(p.era?.emoji ?: "🚀", style = MaterialTheme.typography.titleLarge) },
                onClick = { onPick(p) },
                trailing = {
                    Icon(
                        if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
            )
        }
        if (faces.isNotEmpty()) {
            RowDivider()
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Firms of $year", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    faces.forEach { CompanyLogo(it, size = 32.dp) }
                }
                Hint(faces.joinToString(" · "))
            }
        }
    }
}

/** Head starts earned in earlier runs, and how to earn the rest. */
@Composable
private fun LegacySection(hall: List<RunRecord>) {
    val colors = gameColors
    val earned = LegacyPerk.earned(hall)
    GroupedSection("Your legacy · ${earned.size} of ${LegacyPerk.entries.size}", footer = "What you've done before gives every new company a head start.") {
        LegacyPerk.entries.forEachIndexed { i, p ->
            if (i > 0) RowDivider(56.dp)
            val has = p in earned
            ListRow(
                p.title,
                subtitle = if (has) p.detail else p.progress(hall),
                leading = {
                    Icon(
                        if (has) Icons.Filled.CheckCircle else Icons.Filled.Lock, null,
                        tint = if (has) colors.good else MaterialTheme.colorScheme.outline,
                    )
                },
            )
        }
    }
}

/** Your best runs, biggest first. */
@Composable
private fun HallOfFame(hall: List<RunRecord>) {
    val colors = gameColors
    GroupedSection("Hall of Fame · ${hall.size} run${if (hall.size == 1) "" else "s"}") {
        hall.ranked().take(5).forEachIndexed { i, r ->
            if (i > 0) RowDivider(56.dp)
            val good = r.kind == EndingKind.IPO || r.kind == EndingKind.ACQUIRED || r.kind == EndingKind.INDIE
            ListRow(
                r.company,
                subtitle = "${r.kind.title} · ${r.lane.businessName} · ${GameClock.label(r.week)}" +
                    (if (r.difficulty != Difficulty.NORMAL) " · ${r.difficulty.title.lowercase()}" else ""),
                leading = {
                    Text(
                        "${i + 1}", style = MaterialTheme.typography.titleMedium,
                        color = if (i == 0) colors.warn else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                    )
                },
                value = Format.money(r.valuation),
                valueColor = if (good) MaterialTheme.colorScheme.onSurface else colors.bad,
                valueDetail = "you took ${Format.money(r.takeHome)}",
            )
        }
    }
}
