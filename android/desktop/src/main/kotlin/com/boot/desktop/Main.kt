package com.boot.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.boot.core.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

private val savePath: Path = Path.of(System.getProperty("user.home"), ".boot", "desktop-save.json")
private val canvas = Color(0xFFF3F5F7)
private val sidebar = Color(0xFFEBEEF1)
private val panel = Color(0xFFFFFFFF)
private val panelRaised = Color(0xFFF8FAFB)
private val borderLine = Color(0xFFD9E0E5)
private val ink = Color(0xFF172320)
private val mint = Color(0xFF187A50)
private val sky = Color(0xFF315F9F)
private val coral = Color(0xFFAA4B35)
private val muted = Color(0xFF586762)

private enum class DesktopPage(
    val label: String,
    val group: String,
    val marker: String,
    val description: String,
) {
    OVERVIEW("Overview", "STUDIO", "01", "Your studio at a glance: people, money, and current work."),
    SETUP("New Game", "STUDIO", "+", "Choose a starting business, era, and challenge."),
    MAKE("Production", "STUDIO", "02", "Design, develop, and ship your next release."),
    GAMES("Game Library", "STUDIO", "03", "Follow your releases, reviews, fans, and franchises."),
    TEAM("People", "STUDIO", "04", "Hire and grow a team with the skills your studio needs."),
    BUSINESSES("Businesses", "COMPANY", "05", "Build a portfolio across six business lanes."),
    MARKET("Market", "COMPANY", "06", "Spot customer demand, trends, and new opportunities."),
    TECH("Research", "COMPANY", "07", "Unlock tools, topics, and new ways to grow."),
    STRATEGY("Strategy", "COMPANY", "08", "Expand into new regions and build partnerships."),
    COMPANY("Company", "COMPANY", "09", "Keep your finances healthy and shape your workplace."),
    NEWS("Industry", "WORLD", "10", "Catch up on stories and milestones from the industry."),
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Boot — Game Tycoon",
        state = rememberWindowState(width = 1440.dp, height = 900.dp),
    ) {
        var state by remember { mutableStateOf(newGame()) }
        var page by remember { mutableStateOf(DesktopPage.OVERVIEW) }
        var notice by remember { mutableStateOf("Welcome to your studio. Your next decision is yours.") }
        var companyNameInput by remember { mutableStateOf("Boot Studio") }
        var founderNameInput by remember { mutableStateOf("Founder") }
        var startingLane by remember { mutableStateOf(Lane.GAMES) }
        var startingDifficulty by remember { mutableStateOf(Difficulty.NORMAL) }
        var startingPoint by remember { mutableStateOf(StartPoint.TODAY) }
        var selectedScenario by remember { mutableStateOf<Scenario?>(null) }
        var sandboxMode by remember { mutableStateOf(false) }
        var tutorialMode by remember { mutableStateOf(false) }
        var projectTitle by remember { mutableStateOf("Untitled Project") }
        var topic by remember { mutableStateOf(Topic.FANTASY) }
        var genre by remember { mutableStateOf(Genre.RPG) }
        var audience by remember { mutableStateOf(Audience.EVERYONE) }
        var size by remember { mutableStateOf(GameSize.SMALL) }
        var price by remember { mutableStateOf(PriceTier.STANDARD) }
        var secondGenre by remember { mutableStateOf<Genre?>(null) }
        var selectedPlatforms by remember { mutableStateOf(listOf(Platform.PC)) }
        var liveService by remember { mutableStateOf(false) }
        var sequelOf by remember { mutableStateOf<Int?>(null) }
        var remasterOf by remember { mutableStateOf<Int?>(null) }
        var selectedEngineId by remember { mutableIntStateOf(1) }
        var deviceCategory by remember { mutableStateOf(DeviceCategory.PHONE) }
        var deviceTier by remember { mutableStateOf(DeviceTier.FLAGSHIP) }
        var deviceFormFactor by remember { mutableStateOf(FormFactor.defaultFor(DeviceCategory.PHONE)) }
        var deviceFocusValues by remember { mutableStateOf(List(DeviceFocus.ALL.size) { 1f / DeviceFocus.ALL.size }) }
        var bundleOs by remember { mutableStateOf(false) }
        var appPack by remember { mutableStateOf(false) }
        var appCategory by remember { mutableStateOf(AppCategory.PRODUCTIVITY) }
        var updateFocus by remember { mutableStateOf(UpdateFocus.FEATURES) }
        var trainingSize by remember { mutableStateOf(TrainingSize.SMALL) }
        var aiProduct by remember { mutableStateOf(AIProduct.CHATBOT) }
        var osTarget by remember { mutableStateOf(OsTarget.PC) }
        var strategyRegion by remember { mutableStateOf(Region.NORTH_AMERICA) }
        var allianceKind by remember { mutableStateOf(AllianceKind.DISTRIBUTION) }
        var allianceLane by remember { mutableStateOf(Lane.GAMES) }
        var focusStage by remember { mutableIntStateOf(0) }
        var focusValues by remember { mutableStateOf(listOf(0.6f, 0.6f, 0.6f)) }
        var rename by remember { mutableStateOf("Boot Studio") }

        fun perform(label: String, action: GameState.() -> Unit) {
            try {
                val next = state.copy()
                next.action()
                state = next
                notice = label
            } catch (e: Exception) {
                notice = e.message ?: "That action couldn't be completed."
            }
        }

        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = mint,
                onPrimary = Color.White,
                secondary = sky,
                onSecondary = Color.White,
                tertiary = coral,
                background = canvas,
                onBackground = ink,
                surface = panel,
                onSurface = ink,
                surfaceVariant = panelRaised,
                onSurfaceVariant = muted,
                outline = borderLine,
            ),
        ) {
            Surface(Modifier.fillMaxSize(), color = canvas) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.width(232.dp).fillMaxHeight().background(sidebar).padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(mint),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("B", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("BOOT", style = MaterialTheme.typography.titleLarge, color = ink, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                Text("GAME STUDIO", style = MaterialTheme.typography.labelSmall, color = muted, letterSpacing = 1.3.sp)
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = panel),
                            border = BorderStroke(1.dp, borderLine),
                        ) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                FounderPortrait(state, size = 38.dp)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(state.companyName, style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("FOUNDER · " + state.founder.name, style = MaterialTheme.typography.labelSmall, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DesktopPage.entries.map { it.group }.distinct().forEach { group ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(group, Modifier.padding(start = 10.dp, bottom = 1.dp), style = MaterialTheme.typography.labelSmall, color = muted.copy(alpha = 0.78f), letterSpacing = 1.25.sp)
                                    DesktopPage.entries.filter { it.group == group }.forEach { item ->
                                        NavigationButton(item, page == item) { page = item }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = borderLine)
                        OutlinedButton(
                            onClick = { page = DesktopPage.SETUP },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("＋  Start a new run") }
                        Text("Your studio, your next move.", Modifier.padding(start = 3.dp), color = muted, style = MaterialTheme.typography.labelSmall)
                    }

                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            Modifier.fillMaxWidth().background(panel).padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(state.companyName.uppercase(Locale.US) + "  /  " + page.group, style = MaterialTheme.typography.labelSmall, color = mint, letterSpacing = 1.2.sp)
                                Text(page.label, style = MaterialTheme.typography.titleLarge, color = ink, fontWeight = FontWeight.Bold)
                                Text(page.description, color = muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusPill("WEEK", state.week.toString() + " · " + state.dateLabel, sky)
                                StatusPill("CASH", moneyDesktop(state.cash), mint)
                                StatusPill("NEXT SEASON", GameSeason.forWeek(state.week + 1).title, coral)
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            Files.createDirectories(savePath.parent)
                                            Files.writeString(savePath, state.encode())
                                            notice = "Saved to " + savePath.toAbsolutePath()
                                        } catch (e: Exception) { notice = "Save failed: " + e.message }
                                    },
                                    modifier = Modifier.height(42.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
                                ) { Text("Save") }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            if (Files.exists(savePath)) {
                                                state = GameState.decode(Files.readString(savePath))
                                                notice = "Save loaded."
                                            } else notice = "No desktop save found yet."
                                        } catch (e: Exception) { notice = "Load failed: " + e.message }
                                    },
                                    modifier = Modifier.height(42.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
                                ) { Text("Load") }
                                Button(
                                    onClick = { perform("Week closed. Review the studio feed for consequences.") { advance() } },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !state.isOver && !state.needsDecision && state.games?.unseenReview == null,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                ) {
                                    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text("Advance week", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text(state.dateLabel, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = borderLine)
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 30.dp, vertical = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                        ActionNotice(notice)
                        state.events.firstOrNull()?.let { event ->
                            CardBlock("DECISION REQUIRED", event.title) {
                                Text(event.body, color = muted)
                                event.choices.forEachIndexed { index, choice ->
                                    Button(onClick = { perform("Decision resolved: " + event.title) { choose(index) } }, modifier = Modifier.fillMaxWidth()) { Text(choice.label + " — " + choice.detail) }
                                }
                            }
                        }
                        state.inbox.forEach { item ->
                            CardBlock("INBOX · EXPIRES WEEK " + item.expiresWeek, item.title) {
                                Text(item.body, color = muted)
                                item.choices.forEachIndexed { index, choice ->
                                    OutlinedButton(onClick = { perform("Inbox answered: " + item.title) { answer(item.id, index) } }, modifier = Modifier.fillMaxWidth()) { Text(choice.label + " — " + choice.detail) }
                                }
                            }
                        }
                        state.pendingPivot?.let { offer ->
                            CardBlock("PIVOT POINT", offer.headline) {
                                Text(offer.body, color = muted)
                                if (offer.trigger == PivotTrigger.BUYOUT_OFFER) {
                                    Button(onClick = { perform("Accepted the buyout offer.") { acceptPivotBuyout() } }, modifier = Modifier.fillMaxWidth()) { Text("Accept buyout · " + moneyDesktop(offer.salePrice)) }
                                }
                                Lane.entries.filter { it !in state.lanes }.forEach { lane ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(onClick = { perform("Expanded into " + lane.title) { resolvePivot(lane, PivotMode.EXPAND) } }, enabled = state.pivotBlocker(lane, PivotMode.EXPAND, false) == null, modifier = Modifier.weight(1f)) { Text("Expand into " + lane.title) }
                                        Button(onClick = { perform("Pivoted to " + lane.title) { resolvePivot(lane, PivotMode.PIVOT) } }, enabled = state.pivotBlocker(lane, PivotMode.PIVOT, false) == null, modifier = Modifier.weight(1f)) { Text("Pivot to " + lane.title) }
                                    }
                                }
                                OutlinedButton(onClick = { perform("Stayed focused on the current business.") { stayFocused() } }) { Text("Stay focused") }
                            }
                        }
                        when (page) {
                            DesktopPage.SETUP -> {
                                CardBlock("START A NEW RUN", "Choose a starting business, era, and challenge. Scenarios set their own lane, year, and objectives.") {
                                    FieldLabel("Company name")
                                    OutlinedTextField(companyNameInput, { companyNameInput = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    FieldLabel("Founder name")
                                    OutlinedTextField(founderNameInput, { founderNameInput = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    EnumPicker("Scenario", listOf(null) + Scenario.entries, selectedScenario, { scenario ->
                                        selectedScenario = scenario
                                        if (scenario != null) {
                                            startingLane = scenario.lane
                                            startingDifficulty = scenario.difficulty
                                            startingPoint = StartPoint.TODAY
                                        }
                                    }, Modifier.fillMaxWidth()) { it?.title ?: "Free play" }
                                    selectedScenario?.let { scenario ->
                                        Text(scenario.blurb, color = muted)
                                        Text("Goal: " + scenario.goal + " · deadline " + scenario.years + " years · lane " + scenario.lane.title, color = mint)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        EnumPicker("Starting business", Lane.entries.filter { startingPoint.laneOpen(it) }, startingLane, { lane ->
                                            startingLane = lane
                                            if (!startingPoint.laneOpen(lane)) startingPoint = StartPoint.TODAY
                                        }, Modifier.weight(1f)) { it.title }
                                        EnumPicker("Difficulty", Difficulty.entries, startingDifficulty, { startingDifficulty = it }, Modifier.weight(1f)) { it.title + " — " + it.blurb }
                                    }
                                    EnumPicker("Starting era", StartPoint.entries.filter { it.laneOpen(startingLane) }, startingPoint, { startingPoint = it }, Modifier.fillMaxWidth()) { it.title + " · " + it.blurb }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = sandboxMode, onCheckedChange = { sandboxMode = it }, enabled = selectedScenario == null)
                                        Column {
                                            Text("Sandbox mode")
                                            Text("Start with $1 billion and disable bankruptcy. Not eligible for Hall of Fame.", color = muted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = tutorialMode, onCheckedChange = { tutorialMode = it })
                                        Column {
                                            Text("Guided introduction")
                                            Text("Adds tutorial guidance for a first run.", color = muted, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Button(onClick = {
                                        try {
                                            state = if (selectedScenario != null) {
                                                GameState.scenario(selectedScenario!!, companyNameInput.ifBlank { "Boot Studio" }, founderNameInput.ifBlank { "Founder" })
                                            } else {
                                                GameState.create(
                                                    companyName = companyNameInput.ifBlank { "Boot Studio" },
                                                    founderName = founderNameInput.ifBlank { "Founder" },
                                                    lane = startingLane,
                                                    difficulty = startingDifficulty,
                                                    start = startingPoint,
                                                    sandbox = sandboxMode,
                                                    tutorial = tutorialMode,
                                                )
                                            }
                                            page = DesktopPage.OVERVIEW
                                            notice = if (selectedScenario != null) "Scenario started: " + selectedScenario!!.title else "New studio founded. Good luck."
                                        } catch (e: Exception) {
                                            notice = e.message ?: "Couldn't start a new run."
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("Start game") }
                                }
                            }
                            DesktopPage.OVERVIEW -> {
                                StudioOfficeCard(state) { page = DesktopPage.TEAM }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    MetricCard("CASH ON HAND", moneyDesktop(state.cash), "Current studio balance", Modifier.weight(1f))
                                    MetricCard("WEEK", state.week.toString(), state.dateLabel, Modifier.weight(1f))
                                    MetricCard("TEAM", state.staff.size.toString(), "People on the roster", Modifier.weight(1f))
                                    MetricCard("RESEARCH", state.researchPoints.toInt().toString() + " RP", "Available research", Modifier.weight(1f))
                                }
                                CardBlock("SEASONAL MARKET", GameSeason.forWeek(state.week + 1).title + " · " + GameSeason.forWeek(state.week + 1).weeksLabel) {
                                    Text(GameSeason.forWeek(state.week + 1).marketTip, color = muted)
                                    Text("Featured genres receive +18% launch demand, the runner-up +8%, other genres -4%.", color = muted)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                                    CardBlock("GAME DEVELOPMENT", modifier = Modifier.weight(1f)) {
                                        val p = state.games?.project
                                        if (p == null) {
                                            Text("No game in production.", color = muted)
                                            Button(onClick = { page = DesktopPage.MAKE }) { Text("Design a game") }
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                                                GameCover(p.concept, 88.dp, studio = state.companyName)
                                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(p.title, style = MaterialTheme.typography.titleLarge)
                                                    Text(p.concept.topic.title + " · " + p.concept.genre.title + " · " + p.concept.size.title, color = muted)
                                                    Meter("Production", p.completion)
                                                    Text(if (p.isReady) "Ready to launch" else "Stage " + (p.stage + 1) + " of 3 · " + p.weeksInDev + " weeks in", color = muted)
                                                    Button(onClick = { page = DesktopPage.MAKE }) { Text("Open project") }
                                                }
                                            }
                                        }
                                    }
                                    CardBlock("WEEKLY FINANCES", modifier = Modifier.weight(1f)) {
                                        ValueLine("Estimated weekly burn", moneyDesktop(state.weeklyBurn))
                                        ValueLine("Runway", if (state.runwayWeeks.isInfinite()) "No projected burn" else state.runwayWeeks.toInt().toString() + " weeks")
                                        ValueLine("Studio valuation", moneyDesktop(state.valuation))
                                        ValueLine("Lifetime game fans", Format.count(state.games?.fans ?: 0.0))
                                    }
                                }
                                CardBlock("STUDIO FEED", "Recent decisions, production updates, and studio news.") {
                                    if (state.needsDecision) Text("A decision is waiting. Resolve it before advancing the calendar.", color = Color(0xFF8A5A10))
                                    state.log.takeLast(8).reversed().forEach { Text("•  " + it.text, color = muted) }
                                    if (state.log.isEmpty()) Text("Your studio updates will appear here.", color = muted)
                                }
                                if (state.games?.unseenReview != null) {
                                    val review = state.games?.release(state.games!!.unseenReview!!)
                                    CardBlock("NEW REVIEW", review?.title ?: "Review ready") {
                                        Text(review?.let { it.verdict + " · " + String.format(Locale.US, "%.1f/10", it.score) } ?: "Open the Games page to review the release.")
                                        Button(onClick = { perform("Review acknowledged.") { acknowledgeReview() } }) { Text("Acknowledge review") }
                                    }
                                }
                            }
                            DesktopPage.MAKE -> {
                                val studio = state.games
                                val project = studio?.project
                                val project2 = studio?.project2
                                if (project == null || (Research.SECOND_TEAM in state.researched && project2 == null)) {
                                    if (project != null) DevelopmentPanel(state, project, 1, ::perform, focusStage, { focusStage = it }, focusValues, { focusValues = it })
                                    val targetTeam = if (project == null) 1 else 2
                                    CardBlock(if (targetTeam == 1) "DESIGN A GAME" else "DESIGN A GAME · TEAM 2", "Choose a concept before committing the team's time and budget.") {
                                        FieldLabel("Game title")
                                        OutlinedTextField(projectTitle, { projectTitle = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                            EnumPicker("Topic", Topic.entries.filter { it.starter || studio?.topics?.contains(it) == true }, topic, { topic = it }, Modifier.weight(1f)) { it.title }
                                            EnumPicker("Genre", Genre.entries, genre, { genre = it }, Modifier.weight(1f)) { it.title }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                            EnumPicker("Audience", Audience.entries, audience, { audience = it }, Modifier.weight(1f)) { it.title }
                                            EnumPicker("Size", state.unlockedSizes, size, { size = it }, Modifier.weight(1f)) { it.title }
                                            EnumPicker("Price", PriceTier.entries, price, { price = it }, Modifier.weight(1f)) { it.title }
                                        }
                                        if (studio?.engines?.isNotEmpty() == true) {
                                            val engineChoices = studio.engines
                                            val selectedEngine = engineChoices.firstOrNull { it.id == selectedEngineId } ?: engineChoices.maxBy { it.level }
                                            EnumPicker("Engine", engineChoices, selectedEngine, { selectedEngineId = it.id }, Modifier.fillMaxWidth()) { it.name + " · level " + it.level }
                                        }
                                        if (Research.MULTI_GENRE in state.researched) {
                                            EnumPicker("Second genre (optional)", listOf(null) + Genre.entries.filter { it != genre }, secondGenre, { secondGenre = it }, Modifier.fillMaxWidth()) { it?.title ?: "Single genre" }
                                        }
                                        Text("Target platforms", style = MaterialTheme.typography.titleMedium)
                                        state.platformsOnSale.take(10).forEach { platform ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = platform in selectedPlatforms,
                                                    enabled = platform in selectedPlatforms || selectedPlatforms.size == 1 || Research.MULTI_PLATFORM in state.researched,
                                                    onCheckedChange = { checked -> selectedPlatforms = if (checked) (selectedPlatforms + platform).distinct() else (selectedPlatforms - platform).ifEmpty { listOf(Platform.PC) } },
                                                )
                                                Text(platform.title + if (platform.fee > 0) " · licence " + moneyDesktop(platform.fee) else "", color = muted)
                                            }
                                        }
                                        if (Research.LIVE_SERVICE in state.researched) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = liveService, onCheckedChange = { liveService = it })
                                                Column {
                                                    Text("Live-service game")
                                                    Text("Recurring seasons and player spending; ongoing server costs.", color = muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                        val concept = GameConcept(
                                            title = projectTitle.ifBlank { "Untitled Project" }.trim(),
                                            topic = topic, genre = genre, genre2 = secondGenre, audience = audience,
                                            platforms = selectedPlatforms, size = size, price = price,
                                            engineId = selectedEngineId.takeIf { id -> studio?.engines?.any { it.id == id } == true } ?: (studio?.engines?.maxByOrNull { it.level }?.id ?: 0),
                                            sequelOf = sequelOf, remasterOf = remasterOf, live = liveService,
                                        )
                                        val blocker = state.conceptBlocker(concept, team = targetTeam)
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            GameCover(concept, 108.dp, studio = state.companyName)
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("BOX ART PREVIEW", style = MaterialTheme.typography.labelLarge, color = mint)
                                                Text(concept.title, style = MaterialTheme.typography.titleLarge)
                                                Text(concept.topic.title + " · " + concept.genre.title + " · " + (selectedPlatforms.firstOrNull()?.title ?: "PC"), color = muted)
                                                Text("Theme, genre and platform shape the cover as your concept changes.", color = muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Text("Target platform: PC · " + state.estimatedWeeks(concept) + " estimated weeks", color = muted)
                                        Text("Budget: " + moneyDesktop(size.weeklyBudget) + "/week · " + moneyDesktop(size.weeklyBudget * 4) + " first-month planning cost", color = muted)
                                        if (blocker != null) Text(blocker, color = Color(0xFF8A5A10))
                                        Button(
                                            onClick = { perform("Started " + concept.title + " for team " + targetTeam + ".") { startGame(concept, team = targetTeam) }; sequelOf = null; remasterOf = null },
                                            enabled = blocker == null,
                                        ) { Text("Start development") }
                                    }
                                    if (project == null && project2 == null && studio?.contractOffers?.isNotEmpty() == true) {
                                        CardBlock("CONTRACT WORK", "Side jobs can keep cash flowing between releases.") {
                                            studio.contractOffers.take(4).forEach { offer ->
                                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(offer.title)
                                                        Text(moneyDesktop(offer.pay) + " · " + offer.weeksAllowed + " weeks", color = muted)
                                                    }
                                                    Button(onClick = { perform("Accepted contract: " + offer.title) { takeGameContract(offer.id) } }) { Text("Accept") }
                                                }
                                            }
                                        }
                                    }
                                    if (project == null && project2 == null && studio?.dealOffers?.isNotEmpty() == true) {
                                        CardBlock("PUBLISHER OFFERS", "An advance now in exchange for a share of sales.") {
                                            studio.dealOffers.take(4).forEach { offer ->
                                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(offer.publisher + " · " + offer.topic.title + " " + offer.genre.title)
                                                        Text(moneyDesktop(offer.advance) + " advance · " + offer.minScore + "/10 target", color = muted)
                                                    }
                                                    Button(onClick = { perform("Accepted publisher deal from " + offer.publisher) { acceptDeal(offer.id) } }) { Text("Accept") }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    DevelopmentPanel(state, project, 1, ::perform, focusStage, { focusStage = it }, focusValues, { focusValues = it })
                                }
                                if (project2 != null) DevelopmentPanel(state, project2, 2, ::perform, focusStage, { focusStage = it }, focusValues, { focusValues = it })
                                if (project == null && project2 == null && studio?.contract != null) {
                                    CardBlock("CONTRACT IN PROGRESS", studio.contract!!.title) {
                                        Meter("Progress", studio.contract!!.completion)
                                        Text("Week " + studio.contract!!.weeksElapsed + " of " + studio.contract!!.weeksAllowed, color = muted)
                                    }
                                }
                                if (studio?.expansion != null) {
                                    CardBlock("EXPANSION IN PROGRESS", studio.expansion!!.title) {
                                        Meter("Progress", studio.expansion!!.completion)
                                        Text(studio.expansion!!.weeks.toString() + " weeks in", color = muted)
                                    }
                                }
                            }
                            DesktopPage.GAMES -> {
                                val g = state.games
                                if (g == null) CardBlock("GAME STUDIO NOT OPEN", "Start a new run with the Games lane to manage game projects.") { }
                                else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        MetricCard("FANS", Format.count(g.fans), "Studio fanbase", Modifier.weight(1f))
                                        MetricCard("HITS", g.hits.toString(), "Games scoring 8+", Modifier.weight(1f))
                                        MetricCard("FLOPS", g.flops.toString(), "Games below 5", Modifier.weight(1f))
                                        MetricCard("WEEKLY SALES", moneyDesktop(g.weeklySales), "Your revenue share", Modifier.weight(1f))
                                    }
                                    if (g.franchises.isNotEmpty()) CardBlock("FRANCHISES", "Build on successful games and returning fans.") {
                                        g.franchises.forEach { f ->
                                            Text(f.name + " · " + f.entries.size + " games · avg " + String.format(Locale.US, "%.1f/10", f.averageScore) + " · " + moneyDesktop(f.lifetime), color = muted)
                                        }
                                    }
                                    CardBlock("RELEASE LIBRARY", "Reviews, word of mouth, patches, expansions, sequels and live-service management.") {
                                        if (g.releases.isEmpty()) Text("No games released yet. Finish a project and launch it to begin building a catalogue.", color = muted)
                                        g.releases.sortedByDescending { it.week }.forEach { release ->
                                            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    GameCover(release.concept, 84.dp, studio = state.companyName)
                                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(release.title, style = MaterialTheme.typography.titleMedium)
                                                        Text(release.concept.topic.title + " · " + release.concept.genreTitle + " · " + release.concept.size.title, color = muted)
                                                    }
                                                    Text(String.format(Locale.US, "%.1f/10", release.score), color = if (release.score >= 8) mint else ink, style = MaterialTheme.typography.titleLarge)
                                                }
                                                Text(release.verdict + " · " + Format.count(release.unitsSold) + " copies · " + moneyDesktop(release.lifetimeSales), color = muted)
                                                if (release.pros.isNotEmpty()) Text("Strengths: " + release.pros.take(2).joinToString(), color = muted)
                                                if (release.cons.isNotEmpty()) Text("Weaknesses: " + release.cons.take(2).joinToString(), color = muted)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (!release.patched) OutlinedButton(onClick = { perform("Patched " + release.title) { patchGame(release.id) } }) { Text("Ship patch") }
                                                    if (Research.EXPANSIONS in state.researched) OutlinedButton(onClick = { perform("Started an expansion for " + release.title) { startExpansion(release.id) } }) { Text("Make expansion") }
                                                    if (Research.SEQUELS in state.researched) OutlinedButton(onClick = {
                                                        val sequel = runCatching { state.sequelConcept(release.id) }.getOrNull()
                                                        if (sequel == null) notice = "Couldn't prepare sequel." else {
                                                            projectTitle = sequel.title
                                                            topic = sequel.topic
                                                            genre = sequel.genre
                                                            secondGenre = sequel.genre2
                                                            size = sequel.size
                                                            audience = sequel.audience
                                                            price = sequel.price
                                                            selectedPlatforms = sequel.platforms
                                                            sequelOf = sequel.sequelOf
                                                            remasterOf = sequel.remasterOf
                                                            selectedEngineId = sequel.engineId
                                                            liveService = sequel.live
                                                            page = DesktopPage.MAKE
                                                            notice = "Sequel concept loaded into the designer."
                                                        }
                                                    }) { Text("Design sequel") }
                                                    if (Research.REMASTERS in state.researched) OutlinedButton(onClick = {
                                                        val remaster = release.concept.copy(
                                                            title = release.title + " Remastered",
                                                            sequelOf = null,
                                                            remasterOf = release.id,
                                                            engineId = state.games!!.engines.maxBy { it.level }.id,
                                                            platforms = release.concept.platforms.filter { state.availablePlatform(it) }.ifEmpty { listOf(Platform.PC) },
                                                        )
                                                        projectTitle = remaster.title
                                                        topic = remaster.topic
                                                        genre = remaster.genre
                                                        secondGenre = remaster.genre2
                                                        size = remaster.size
                                                        audience = remaster.audience
                                                        price = remaster.price
                                                        selectedPlatforms = remaster.platforms
                                                        selectedEngineId = remaster.engineId
                                                        sequelOf = null
                                                        remasterOf = release.id
                                                        liveService = false
                                                        page = DesktopPage.MAKE
                                                        notice = "Remaster concept loaded into the designer."
                                                    }) { Text("Design remaster") }
                                                }
                                                if (release.live != null) {
                                                    val live = release.live!!
                                                    Text("LIVE SERVICE · " + if (live.closed) "SHUT DOWN" else "ACTIVE", color = mint)
                                                    ValueLine("Players", Format.count(live.players))
                                                    ValueLine("Seasons released", live.seasons.toString())
                                                    ValueLine("Lifetime live revenue", moneyDesktop(live.lifetimeRevenue))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(onClick = { perform("Launched a new season for " + release.title) { launchSeason(release.id) } }, enabled = state.seasonBlocker(release.id) == null) { Text("Launch season") }
                                                        OutlinedButton(onClick = { perform("Set cosmetic monetization.") { setMonetization(release.id, Monetization.COSMETIC) } }, enabled = !live.closed) { Text("Cosmetic") }
                                                        OutlinedButton(onClick = { perform("Set fair monetization.") { setMonetization(release.id, Monetization.FAIR) } }, enabled = !live.closed) { Text("Fair") }
                                                        OutlinedButton(onClick = { perform("Set aggressive monetization.") { setMonetization(release.id, Monetization.AGGRESSIVE) } }, enabled = !live.closed) { Text("Aggressive") }
                                                        if (!live.closed) OutlinedButton(onClick = { perform("Ended live service.") { endService(release.id) } }) { Text("End service") }
                                                    }
                                                }
                                                if (release.league != null) {
                                                    val league = release.league!!
                                                    Text("ESPORTS · " + if (league.closed) "LEAGUE ENDED" else league.name, color = mint)
                                                    ValueLine("Viewers", Format.count(league.viewers))
                                                    ValueLine("Prize pool", league.prize.title + " · " + moneyDesktop(league.prize.amount))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        PrizePool.entries.forEach { prize ->
                                                            OutlinedButton(onClick = { perform("Updated esports prize pool.") { setPrizePool(release.id, prize) } }, enabled = !league.closed) { Text(prize.title) }
                                                        }
                                                        if (!league.closed) OutlinedButton(onClick = { perform("Ended esports league.") { endLeague(release.id) } }) { Text("End league") }
                                                    }
                                                } else if (Research.ESPORTS in state.researched) {
                                                    Button(onClick = { perform("Started esports league.") { startLeague(release.id) } }, enabled = state.leagueBlocker(release.id) == null) { Text("Start esports league") }
                                                    state.leagueBlocker(release.id)?.let { Text(it, color = muted, style = MaterialTheme.typography.bodySmall) }
                                                }
                                            }
                                            HorizontalDivider(color = borderLine)
                                        }
                                    }
                                    if (g.market.awards.isNotEmpty()) CardBlock("INDUSTRY AWARDS", "Your studio's place in the wider market.") {
                                        g.market.awards.takeLast(8).reversed().forEach { Text(it.toString(), color = muted) }
                                    }
                                    if (g.published.isNotEmpty()) CardBlock("PUBLISHED INDIE GAMES", "Your label's catalogue.") {
                                        g.published.forEach { Text(it.title + " · " + String.format(Locale.US, "%.1f/10", it.score), color = muted) }
                                    }
                                }
                            }
                            DesktopPage.BUSINESSES -> {
                                Text("These panels use the same simulation state as Android. Each business has its own operating loop; inactive lanes stay visible so you can see what unlocking a second business would mean.", color = muted)
                                Lane.entries.forEach { lane ->
                                    CardBlock(lane.businessName, lane.pitch) {
                                        if (lane !in state.lanes) {
                                            ValueLine("Status", "Not open")
                                            ValueLine("Setup cost", moneyDesktop(lane.setupCost))
                                            Text("This run does not operate this lane yet.", color = muted)
                                        } else {
                                            ValueLine("Status", "Active")
                                            ValueLine("Weekly revenue (recent)", moneyDesktop(state.trailingRevenue(lane, 4) / 4))
                                            ValueLine("Weekly profit (recent)", moneyDesktop(state.trailingProfit(lane, 4) / 4))
                                            when (lane) {
                                                Lane.GAMES -> {
                                                    Text("Game development, releases, seasonal demand, live games, and esports are on the dedicated pages.", color = muted)
                                                    Button(onClick = { page = DesktopPage.MAKE }) { Text("Open game studio") }
                                                }
                                                Lane.WEBSITES -> {
                                                    val web = state.web
                                                    if (web != null) {
                                                        ValueLine("Agency standing", (web.standing * 100).toInt().toString() + "%")
                                                        EnumPicker("Scope policy", ScopePolicy.entries, web.policy, { choice -> perform("Scope policy updated.") { setScopePolicy(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        EnumPicker("Agency rate", AgencyRate.entries, web.rate, { choice -> perform("Agency rate updated.") { setAgencyRate(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        Text("Client offers", style = MaterialTheme.typography.titleMedium)
                                                        web.offers.forEach { offer ->
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Column(Modifier.weight(1f)) {
                                                                    Text(offer.client)
                                                                    Text(moneyDesktop(offer.value) + " · " + offer.weeksAllowed + " weeks", color = muted)
                                                                }
                                                                Button(onClick = { perform("Accepted client contract.") { acceptContract(offer.id) } }) { Text("Accept") }
                                                                OutlinedButton(onClick = { perform("Declined client contract.") { declineContract(offer.id) } }) { Text("Decline") }
                                                            }
                                                        }
                                                        web.active.forEach { contract ->
                                                            Text(contract.client + " · " + (contract.completion * 100).toInt() + "% complete" + if (contract.isOverdue) " · OVERDUE" else "", color = muted)
                                                            Meter("Contract progress", contract.completion)
                                                        }
                                                        Text("Retainers: " + web.retainers.size + " · completed contracts: " + web.completed, color = muted)
                                                    }
                                                }
                                                Lane.APPS -> {
                                                    val app = state.apps
                                                    if (app != null) {
                                                        ValueLine("App", app.name)
                                                        ValueLine("Release status", if (app.isLive) "Live · " + app.rankLabel else "In development · " + (app.buildProgress / app.buildWork * 100).toInt() + "%")
                                                        ValueLine("Active users", Format.count(app.users))
                                                        ValueLine("Quality", app.quality.toInt().toString() + "/100")
                                                        ValueLine("Last installs", Format.count(app.lastInstalls))
                                                        EnumPicker("Category", AppCategory.entries, app.category, { choice -> appCategory = choice; perform("App category changed.") { setAppCategory(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        EnumPicker("Monetization", AppMonetization.entries, app.monetization, { choice -> perform("App monetization changed.") { setMonetization(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        EnumPicker("Update cadence", UpdateStyle.entries, app.updateStyle, { choice -> perform("App update cadence changed.") { setUpdateStyle(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        EnumPicker("Update focus", UpdateFocus.entries, app.updateFocus, { choice -> updateFocus = choice; perform("App update focus changed.") { setUpdateFocus(choice) } }, Modifier.fillMaxWidth()) { it.title + " — " + it.detail }
                                                        EnumPicker("Pricing", AppPricing.entries, app.pricing, { choice -> perform("App pricing changed.") { apps!!.pricing = choice } }, Modifier.fillMaxWidth()) { it.title + " — " + it.detail }
                                                        ValueLine("Design / tech", app.design.toInt().toString() + " / " + app.tech.toInt())
                                                        Text("Research features: " + (app.features.joinToString { it.title }.ifBlank { "none" }), color = muted)
                                                        Meter("Next update", app.updateProgress / app.buildWork)
                                                    }
                                                }
                                                Lane.SOCIAL -> {
                                                    val social = state.social
                                                    if (social != null) {
                                                        ValueLine("Platform", social.name + if (social.isLive) " · LIVE" else " · BUILDING")
                                                        ValueLine("Users", Format.count(social.users))
                                                        ValueLine("Real users", Format.count(social.realUsers))
                                                        ValueLine("Bot share", (social.botShare * 100).toInt().toString() + "%")
                                                        ValueLine("Toxicity", (social.toxicity * 100).toInt().toString() + "%")
                                                        EnumPicker("Content policy", ContentPolicy.entries, social.policy, { choice -> perform("Content policy changed.") { social!!.policy = choice } }, Modifier.fillMaxWidth()) { it.title }
                                                        Column {
                                                            ValueLine("Moderation", (social.moderation * 100).toInt().toString() + "%")
                                                            Slider(value = social.moderation.toFloat(), onValueChange = { value -> perform("Moderation budget adjusted.") { setModeration(value.toDouble()) } })
                                                        }
                                                        Column {
                                                            ValueLine("Ad load", (social.adLoad * 100).toInt().toString() + "%")
                                                            Slider(value = social.adLoad.toFloat(), onValueChange = { value -> perform("Ad load adjusted.") { setAdLoad(value.toDouble()) } })
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Premium tier", Modifier.weight(1f))
                                                            Switch(checked = social.premium, onCheckedChange = { value -> perform("Premium tier setting changed.") { social!!.premium = value } })
                                                        }
                                                    }
                                                }
                                                Lane.HARDWARE -> {
                                                    val lab = state.hardware
                                                    if (lab != null) {
                                                        ValueLine("Brand awareness", (lab.brandAwareness * 100).toInt().toString() + "%")
                                                        ValueLine("Inventory", lab.inventory.toString())
                                                        ValueLine("Lifetime units", lab.lifetimeUnits.toString())
                                                        lab.design?.let { design ->
                                                            Text(design.name + " · " + design.spec.category.title + " · " + design.spec.tier.title + " · " + design.formFactor.title, style = MaterialTheme.typography.titleMedium)
                                                            Meter("Device design", design.completion)
                                                            ValueLine("Design / tech", design.design.toInt().toString() + " / " + design.tech.toInt())
                                                            ValueLine("Planned BOM", moneyDesktop(design.effectiveUnitCost))
                                                            ValueLine("Suggested price band", moneyDesktop(design.priceBand.start) + "–" + moneyDesktop(design.priceBand.endInclusive))
                                                            if (design.isReady) Button(onClick = { perform("Launched device.") { launchDevice() } }, enabled = lab.inventory > 0) { Text("Launch device") }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            EnumPicker("Device", DeviceCategory.entries, deviceCategory, { deviceCategory = it; deviceFormFactor = FormFactor.defaultFor(it) }, Modifier.weight(1f)) { it.title }
                                                            EnumPicker("Tier", DeviceTier.entries, deviceTier, { deviceTier = it }, Modifier.weight(1f)) { it.title }
                                                        }
                                                        EnumPicker("Form factor", FormFactor.options(deviceCategory), deviceFormFactor, { deviceFormFactor = it }, Modifier.fillMaxWidth()) { it.title }
                                                        Text("Component focus", style = MaterialTheme.typography.titleMedium)
                                                        DeviceFocus.entries.forEachIndexed { index, focus ->
                                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(focus.title(deviceCategory), Modifier.weight(1f))
                                                                    Text((deviceFocusValues[index] * 100).toInt().toString() + "%", color = muted)
                                                                }
                                                                Slider(value = deviceFocusValues[index], onValueChange = { value -> deviceFocusValues = deviceFocusValues.toMutableList().also { it[index] = value } })
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(checked = bundleOs, onCheckedChange = { bundleOs = it }, enabled = state.canBundleOs())
                                                            Column {
                                                                Text("Bundle recent OS")
                                                                Text(if (state.canBundleOs()) "Boosts device appeal and the platform ecosystem." else "Ship an OS version first to unlock this.", color = muted, style = MaterialTheme.typography.bodySmall)
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Checkbox(checked = appPack, onCheckedChange = { appPack = it }, enabled = state.canOfferAppPack())
                                                            Column {
                                                                Text("Include first-party app pack")
                                                                Text("A coordinated ecosystem launch when Apps is open.", color = muted, style = MaterialTheme.typography.bodySmall)
                                                            }
                                                        }
                                                        Button(
                                                            onClick = { perform("Started device design.") { startDevice(deviceCategory, deviceTier, deviceFormFactor, deviceFocusValues.map { it.toDouble() }, bundleOs, appPack) } },
                                                            enabled = lab.design == null,
                                                        ) { Text("Start device design") }
                                                        if (lab.design?.isReady == true) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                listOf(100, 500, 1000).forEach { units ->
                                                                    Button(onClick = { perform("Booked " + units + " production units.") { orderUnits(units) } }, enabled = state.cash > lab.design!!.effectiveUnitCost * units) { Text("Book " + units) }
                                                                }
                                                            }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text("Launch event", Modifier.weight(1f))
                                                            OutlinedButton(onClick = { perform("Launch event set to none.") { hardware!!.keynote = 0 } }) { Text("None") }
                                                            OutlinedButton(onClick = { perform("Launch livestream selected.") { hardware!!.keynote = 1 } }) { Text("Livestream") }
                                                            OutlinedButton(onClick = { perform("Keynote selected.") { hardware!!.keynote = 2 } }) { Text("Keynote") }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text("Price shift " + String.format(Locale.US, "%+.0f%%", lab.priceShift * 100), Modifier.weight(1f), color = muted)
                                                            OutlinedButton(onClick = { perform("Device price lowered.") { hardware!!.priceShift = (hardware!!.priceShift - 0.05).coerceAtLeast(-0.2) } }, enabled = lab.priceShift > -0.2) { Text("−5%") }
                                                            OutlinedButton(onClick = { perform("Device price raised.") { hardware!!.priceShift = (hardware!!.priceShift + 0.05).coerceAtMost(0.2) } }, enabled = lab.priceShift < 0.2) { Text("+5%") }
                                                        }
                                                        lab.orders.forEach { order -> Text(order.model + " · " + order.units + " units · arrives week " + order.arrivalWeek, color = muted) }
                                                        lab.onMarket?.let { ValueLine("On sale", it.name + " · " + it.unitsSold + " units sold") }
                                                    }
                                                }
                                                Lane.OS -> {
                                                    val lab = state.os
                                                    if (lab != null) {
                                                        ValueLine("Platform Lab", "OS releases " + lab.versionsShipped)
                                                        ValueLine("OEM trust", (lab.oemTrust * 100).toInt().toString() + "%")
                                                        ValueLine("Ecosystem strength", (lab.ecosystem * 100).toInt().toString() + "%")
                                                        ValueLine("Lifetime licenses", Format.count(lab.lifetimeLicenses))
                                                        lab.project?.let { project ->
                                                            Text(project.codename + " v" + project.version + " · " + project.target.title, style = MaterialTheme.typography.titleMedium)
                                                            Meter("OS development", project.completion)
                                                            ValueLine("Known bugs", project.bugs.toInt().toString())
                                                            if (project.inBeta) {
                                                                ValueLine("Beta time remaining", project.betaWeeksLeft.toString() + " weeks")
                                                                OutlinedButton(onClick = { perform("OS beta cancelled.") { cancelOsBeta() } }) { Text("Cancel beta") }
                                                            } else if (project.isReady) {
                                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    Button(onClick = { perform("OS beta started.") { startOsBeta() } }) { Text("Start 6-week beta") }
                                                                    Button(onClick = { perform("OS version shipped.") { shipOs() } }) { Text("Ship OS") }
                                                                }
                                                            }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            EnumPicker("Release target", OsTarget.entries.filter { state.osTargetOpen(it) }, osTarget, { osTarget = it }, Modifier.weight(1f)) { it.title }
                                                            Button(onClick = { perform("Started OS version for " + osTarget.title) { startOsVersion(osTarget) } }, enabled = lab.project == null && state.osTargetOpen(osTarget)) { Text("Start version") }
                                                        }
                                                        if (lab.releases.isNotEmpty()) {
                                                            Text("Shipped versions", style = MaterialTheme.typography.titleMedium)
                                                            lab.releases.takeLast(8).reversed().forEach { release ->
                                                                Text(release.codename + " v" + release.version + " · " + release.target.title + " · score " + release.score.toInt() + " · " + Format.count(release.installedBase) + " devices · " + release.oemPartners + " OEMs", color = muted)
                                                            }
                                                        }
                                                    }
                                                }
                                                Lane.AI -> {
                                                    val lab = state.ai
                                                    if (lab != null) {
                                                        ValueLine("Best model", lab.modelName ?: "No model shipped")
                                                        ValueLine("Capability", lab.capability.toInt().toString())
                                                        ValueLine("Generation", lab.generation.toString())
                                                        ValueLine("GPU units owned", lab.gpusOwned.toString())
                                                        ValueLine("Subscribers", Format.count(lab.subscribers))
                                                        ValueLine("Regulatory heat", (lab.regulatoryHeat * 100).toInt().toString() + "%")
                                                        EnumPicker("Product focus", AIProduct.entries, aiProduct, { choice -> aiProduct = choice; perform("AI product focus changed.") { setAIFocus(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        lab.training?.let { run ->
                                                            Text(run.codename + " · " + run.size.title, style = MaterialTheme.typography.titleMedium)
                                                            Meter("Training run", run.completion)
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            TrainingSize.entries.forEach { option ->
                                                                Button(onClick = { trainingSize = option; perform("Started " + option.title + ".") { startTraining(option) } }, enabled = lab.training == null) { Text(option.title) }
                                                            }
                                                        }
                                                        Button(onClick = { perform("Bought one GPU unit.") { buyGPUs(1) } }, enabled = state.cash >= state.gpuCost(1)) { Text("Buy one GPU · " + moneyDesktop(state.gpuCost(1))) }
                                                        lab.pendingModel?.let { Text("Model ready for release: " + it.codename + " · capability gain " + it.gain.toInt(), color = mint) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            DesktopPage.MARKET -> {
                                val g = state.games
                                CardBlock("SEASONAL RELEASE STRATEGY", GameSeason.forWeek(state.week + 1).title + " · " + GameSeason.forWeek(state.week + 1).weeksLabel) {
                                    Text(GameSeason.forWeek(state.week + 1).marketTip, color = muted)
                                    Text("Plan releases around seasonal genre demand; a delay can help polish but may miss the window.", color = muted)
                                }
                                if (g != null) {
                                    CardBlock("TRENDING NOW", "Current demand can influence the audience for your next release.") {
                                        Text(g.trend?.let { "Trend: " + it.title + " · until week " + it.until } ?: "No special topic or genre trend right now.", color = muted)
                                        Text("Rival releases and sales charts are tracked in the simulation each week.", color = muted)
                                    }
                                    CardBlock("PUBLISHING LABEL", "Sign promising indie games when publishing research is unlocked.") {
                                        if (Research.PUBLISHING !in state.researched) Text("Research the publishing label to unlock indie pitches.", color = muted)
                                        else if (g.pitches.isEmpty()) Text("No pitches are waiting right now.", color = muted)
                                        else g.pitches.forEach { pitch ->
                                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(pitch.title + " · " + pitch.developer, style = MaterialTheme.typography.titleMedium)
                                                Text(pitch.topic.title + " · " + pitch.genre.title + " · " + pitch.size.title + " · " + pitch.demoTitle, color = muted)
                                                Text("Funding " + moneyDesktop(pitch.funding) + " · " + pitch.weeks + " weeks · label share " + (pitch.labelShare * 100).toInt() + "%", color = muted)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(onClick = { perform("Signed " + pitch.title + " for the label.") { signPitch(pitch.id) } }, enabled = state.signBlocker(pitch.id) == null) { Text("Sign game") }
                                                    OutlinedButton(onClick = { perform("Passed on " + pitch.title + ".") { passPitch(pitch.id) } }) { Text("Pass") }
                                                }
                                            }
                                            HorizontalDivider(color = borderLine)
                                        }
                                    }
                                    CardBlock("RIVAL STUDIOS", "Competition changes the market and your opportunities.") {
                                        g.market.games.take(8).forEach { Text(it.toString(), color = muted) }
                                    }
                                    CardBlock("CONTRACT & PUBLISHER PIPELINE", "Optional work for cash, or publisher advances for a larger project.") {
                                        Text("Open the Make page to accept currently available contracts and publisher offers.", color = muted)
                                        Text("Contract offers: " + g.contractOffers.size + " · Publisher deals: " + g.dealOffers.size, color = muted)
                                    }
                                }
                            }
                            DesktopPage.TECH -> {
                                val g = state.games
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    MetricCard("RESEARCH POINTS", state.researchPoints.toInt().toString(), "Spend on new capabilities", Modifier.weight(1f))
                                    MetricCard("UNLOCKED", state.researched.size.toString(), "Research projects", Modifier.weight(1f))
                                    MetricCard("ENGINE LEVEL", (g?.engines?.maxOfOrNull { it.level } ?: 0).toString(), "Best engine", Modifier.weight(1f))
                                }
                                CardBlock("RESEARCH TREE", "Unlock new genres, production sizes, tools and business capabilities.") {
                                    Research.entries.filter { it !in state.researched }
                                        .forEach { item ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(item.title)
                                                    Text(item.detail + " · " + item.rp + " RP" + if (item.cash > 0) " · " + moneyDesktop(item.cash) else "", color = muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                                val blocker = state.researchBlocker(item)
                                                Button(onClick = { perform("Research completed: " + item.title) { research(item) } }, enabled = blocker == null) { Text("Research") }
                                            }
                                        }
                                }
                                if (g != null) {
                                    CardBlock("GAME TOPICS", "Topics unlock more combinations for future projects.") {
                                        Topic.entries.filter { it !in g.topics }.take(18).forEach { item ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(item.title)
                                                    Text(item.researchCost.toString() + " RP", color = muted)
                                                }
                                                Button(onClick = { perform("Topic researched: " + item.title) { researchTopic(item) } }, enabled = state.researchPoints >= item.researchCost) { Text("Unlock") }
                                            }
                                        }
                                    }
                                    CardBlock("ENGINES", "Custom engines can improve games and be licensed for royalties.") {
                                        g.engines.forEach { engine -> Text(engine.name + " · level " + engine.level + " · " + engine.features.joinToString { it.title }, color = muted) }
                                        val quote = runCatching { state.engineQuote() }.getOrNull()
                                        if (quote != null) {
                                            Text("Next engine: " + moneyDesktop(quote.second) + " · " + quote.third + " weeks", color = muted)
                                            Button(onClick = { perform("Started custom engine build.") { buildEngine() } }, enabled = Research.CUSTOM_ENGINES in state.researched && quote.first.isNotEmpty() && quote.first.sumOf { it.engineLevel } > (g.engines.maxOfOrNull { it.level } ?: 0) && state.cash >= quote.second) { Text("Build engine") }
                                        }
                                    }
                                }
                            }
                            DesktopPage.STRATEGY -> {
                                CardBlock("GLOBAL STRATEGY", "The 3.4.1 strategy layer adds regional offices, lane alliances, and minority investments.") {
                                    ValueLine("Market cycle", state.marketCycleLabel())
                                    ValueLine("Regional offices", state.strategy.offices.size.toString())
                                    ValueLine("Active alliances", state.strategy.activeAlliances.size.toString())
                                    ValueLine("Live venture holdings", state.strategy.liveHoldings.size.toString())
                                    ValueLine("Territory revenue", moneyDesktop(state.strategy.lastTerritory))
                                    ValueLine("Alliance yield", moneyDesktop(state.strategy.lastAllianceYield))
                                }
                                CardBlock("REGIONAL OFFICES", "Open a market, then pay to localize it for stronger brand and share.") {
                                    EnumPicker("Region", Region.entries, strategyRegion, { strategyRegion = it }, Modifier.fillMaxWidth()) { it.title }
                                    val openBlocker = state.openOfficeBlocker(strategyRegion)
                                    val localizeBlocker = state.localizeOfficeBlocker(strategyRegion)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { perform("Opened office in " + strategyRegion.title) { openOffice(strategyRegion) } }, enabled = openBlocker == null) { Text("Open · " + moneyDesktop(strategyRegion.openCost)) }
                                        Button(onClick = { perform("Localized office in " + strategyRegion.title) { localizeOffice(strategyRegion) } }, enabled = localizeBlocker == null) { Text("Localize · " + moneyDesktop(strategyRegion.localizeCost)) }
                                    }
                                    (openBlocker ?: localizeBlocker)?.let { Text(it, color = muted) }
                                    state.strategy.offices.forEach { office ->
                                        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                            Text(office.region.title + if (office.localized) " · LOCALIZED" else "", style = MaterialTheme.typography.titleMedium)
                                            ValueLine("Local brand", (office.brand * 100).toInt().toString() + "%")
                                            ValueLine("Market share", (office.share * 100).toInt().toString() + "%")
                                            ValueLine("Weekly revenue", moneyDesktop(office.weeklyRevenue))
                                        }
                                    }
                                }
                                CardBlock("GLOBAL ALLIANCES", "Two-year partnerships boost a specific business lane.") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        EnumPicker("Alliance", AllianceKind.entries, allianceKind, { allianceKind = it }, Modifier.weight(1f)) { it.title }
                                        EnumPicker("Business", Lane.entries.filter { it in state.lanes }, allianceLane, { allianceLane = it }, Modifier.weight(1f)) { it.title }
                                    }
                                    val allianceBlocker = state.signAllianceBlocker(allianceKind, allianceLane)
                                    Button(onClick = { perform("Signed " + allianceKind.title + " alliance for " + allianceLane.title) { signAlliance(allianceKind, allianceLane) } }, enabled = allianceBlocker == null) { Text("Sign alliance · " + moneyDesktop(allianceKind.signCost)) }
                                    allianceBlocker?.let { Text(it, color = muted) }
                                    state.strategy.activeAlliances.forEach { alliance ->
                                        Text(alliance.partner + " · " + alliance.kind.title + " · " + alliance.lane.title + " · " + alliance.weeksLeft(state.week) + " weeks left", color = muted)
                                    }
                                }
                                CardBlock("VENTURE ARM", "Invest in startups in lanes you understand; follow-on rounds cost more, and exits depend on the market cycle.") {
                                    Lane.entries.filter { it in state.lanes }.forEach { lane ->
                                        val blocker = state.seedVentureBlocker(lane)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(lane.title)
                                                Text("Seed ticket " + moneyDesktop(state.seedTicket(lane)), color = muted)
                                            }
                                            Button(onClick = { perform("Seeded a startup in " + lane.title) { seedVenture(lane) } }, enabled = blocker == null) { Text("Seed") }
                                        }
                                    }
                                    state.strategy.holdings.forEach { holding ->
                                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                            Text(holding.name + " · " + holding.lane.title + " · " + holding.stage.title, style = MaterialTheme.typography.titleMedium)
                                            ValueLine("Ownership", (holding.ownership * 100).toInt().toString() + "%")
                                            ValueLine("Market value", moneyDesktop(holding.marketValue))
                                            ValueLine("Paper gain", moneyDesktop(holding.paperGain))
                                            if (!holding.exited) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(onClick = { perform("Follow-on investment in " + holding.name) { followOnVenture(holding.id) } }, enabled = state.followOnBlocker(holding.id) == null) { Text("Series A · " + moneyDesktop(state.followOnTicket(holding))) }
                                                    OutlinedButton(onClick = { perform("Exited " + holding.name) { exitVenture(holding.id) } }, enabled = state.exitVentureBlocker(holding.id) == null) { Text("Exit · " + moneyDesktop(state.exitQuote(holding))) }
                                                }
                                                state.followOnBlocker(holding.id)?.let { Text(it, color = muted, style = MaterialTheme.typography.bodySmall) }
                                            } else {
                                                Text("Exited · proceeds " + moneyDesktop(holding.exitProceeds), color = muted)
                                            }
                                        }
                                        HorizontalDivider(color = borderLine)
                                    }
                                }
                            }
                            DesktopPage.TEAM -> {
                                CardBlock("YOUR TEAM", "Hiring costs two weeks of salary up front. Skills and roles affect output.") {
                                    ValueLine("Payroll / week", moneyDesktop(state.payroll))
                                    ValueLine("Office", state.office.title + " · " + state.freeDesks + " free desks")
                                    if (state.staff.isEmpty()) Text("You're working solo. Hire specialists when you can afford them.", color = muted)
                                    state.staff.forEach { employee ->
                                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            StaffPortrait(employee, size = 44.dp)
                                            Column(Modifier.weight(1f)) {
                                                Text(employee.name + " · " + employee.role.title)
                                                Text("Skill " + employee.skill + " · " + moneyDesktop(employee.salary) + "/week · " + (employee.lane?.title ?: "Unassigned"), color = muted)
                                            }
                                            OutlinedButton(onClick = { perform("Fired " + employee.name) { fire(employee.id) } }) { Text("Let go") }
                                        }
                                    }
                                }
                                CardBlock("CANDIDATES", "Choose people who match the work you want to do.") {
                                    if (state.candidates.isEmpty()) Text("No candidates available this week. Advance the calendar to refresh the pool.", color = muted)
                                    state.candidates.take(12).forEach { candidate ->
                                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            CandidatePortrait(candidate.id, size = 42.dp)
                                            Column(Modifier.weight(1f)) {
                                                Text(candidate.name + " · " + candidate.role.title)
                                                Text("Skill " + candidate.skill + " · " + moneyDesktop(candidate.askingSalary) + "/week · hiring fee " + moneyDesktop(candidate.askingSalary * 2), color = muted)
                                            }
                                            Button(onClick = { perform("Hired " + candidate.name) { hire(candidate.id, Lane.GAMES) } }, enabled = state.cash >= candidate.askingSalary * 2 && state.freeDesks > 0) { Text("Hire for Games") }
                                        }
                                    }
                                }
                                CardBlock("TRAINING", "Training is available after researching a training program.") {
                                    state.staff.filter { it.skill < 10 }.forEach { employee ->
                                        val blocker = state.trainingBlocker(employee)
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(employee.name + " · skill " + employee.skill, Modifier.weight(1f))
                                            Button(onClick = { perform("Trained " + employee.name) { train(employee.id) } }, enabled = blocker == null) { Text("Train") }
                                        }
                                    }
                                }
                            }
                            DesktopPage.COMPANY -> {
                                CardBlock("COMPANY SETTINGS", "A few decisions have effects across the entire studio.") {
                                    FieldLabel("Company name")
                                    OutlinedTextField(rename, { rename = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    Button(onClick = { perform("Company renamed.") { this.rename(rename) } }) { Text("Rename company") }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Crunch mode")
                                            Text("More pressure on the team. Use sparingly.", color = muted)
                                        }
                                        Switch(checked = state.crunch, onCheckedChange = { value -> perform(if (value) "Crunch enabled." else "Crunch ended.") { setCrunch(value) } })
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Founder freelancing")
                                            Text("Earn side income at the cost of company focus.", color = muted)
                                        }
                                        Switch(checked = state.founder.freelancing, onCheckedChange = { value -> perform("Freelancing setting updated.") { setFreelancing(value) } })
                                    }
                                }
                                CardBlock("BUSINESS PORTFOLIO", "Boot's simulation supports six business lanes.") {
                                    Lane.entries.forEach { lane ->
                                        val active = lane in state.lanes
                                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                            Text(lane.businessName + if (active) " · ACTIVE" else " · NOT OPEN", style = MaterialTheme.typography.titleMedium)
                                            Text(lane.pitch, color = muted)
                                        }
                                    }
                                }
                                CardBlock("FINANCIAL HEALTH", "Watch cash flow before committing to large projects.") {
                                    ValueLine("Cash", moneyDesktop(state.cash))
                                    ValueLine("Weekly burn", moneyDesktop(state.weeklyBurn))
                                    ValueLine("Runway", if (state.runwayWeeks.isInfinite()) "No projected burn" else state.runwayWeeks.toInt().toString() + " weeks")
                                    ValueLine("Valuation", moneyDesktop(state.valuation))
                                    ValueLine("Research", state.researchPoints.toInt().toString() + " RP")
                                }
                                CardBlock("OFFICE & PERKS", "Your office controls headcount capacity, rent, and the perks you can support.") {
                                    ValueLine("Current office", state.office.title)
                                    ValueLine("Desks", state.freeDesks.toString() + " free of " + state.office.desks)
                                    ValueLine("Weekly rent", moneyDesktop(state.office.rent))
                                    state.office.next?.let { next ->
                                        Button(onClick = { perform("Moved into " + next.title) { moveOffice() } }, enabled = state.cash >= next.moveCost) { Text("Move to " + next.title + " · " + moneyDesktop(next.moveCost)) }
                                    }
                                    Text("Installed perks", style = MaterialTheme.typography.titleMedium)
                                    if (state.perks.isEmpty()) Text("No office perks yet.", color = muted)
                                    state.perks.forEach { perk ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(perk.title)
                                                Text(perk.blurb, color = muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            OutlinedButton(onClick = { perform("Removed " + perk.title) { removePerk(perk) } }) { Text("Remove") }
                                        }
                                    }
                                    Perk.entries.filter { it !in state.perks }.forEach { perk ->
                                        val blocker = state.perkBlocker(perk)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(perk.title)
                                                Text(perk.blurb + " · " + moneyDesktop(perk.cost) + " upfront", color = muted, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Button(onClick = { perform("Bought " + perk.title) { buyPerk(perk) } }, enabled = blocker == null) { Text("Buy") }
                                        }
                                    }
                                }
                                CardBlock("BANK LOAN", "Loans give you cash now but lock in weekly repayments.") {
                                    if (state.loan == null) {
                                        val offer = state.loanOffer()
                                        ValueLine("Maximum offer", moneyDesktop(state.loanLimit))
                                        ValueLine("Weekly payment", moneyDesktop(offer.weeklyPayment))
                                        ValueLine("Rate", String.format(Locale.US, "%.1f%%", offer.rate * 100))
                                        Button(onClick = { perform("Took a bank loan.") { takeLoan() } }, enabled = state.loanBlocker == null) { Text("Take maximum loan") }
                                    } else {
                                        ValueLine("Remaining balance", moneyDesktop(state.loan!!.balance))
                                        ValueLine("Weekly payment", moneyDesktop(state.loan!!.weeklyPayment))
                                        ValueLine("Early payoff", moneyDesktop(state.loanPayoff ?: 0.0))
                                        Button(onClick = { perform("Paid off the bank loan early.") { repayLoan() } }, enabled = state.cash >= (state.loanPayoff ?: Double.POSITIVE_INFINITY)) { Text("Repay early") }
                                    }
                                }
                                CardBlock("STOCK MARKET", "Trade listed companies and watch the value of your portfolio.") {
                                    ValueLine("Portfolio value", moneyDesktop(state.portfolio.holdings.sumOf { state.holdingValue(it) }))
                                    ValueLine("Realized gains / dividends", moneyDesktop(state.portfolio.realized + state.portfolio.dividends))
                                    state.stocks().filter { it.listed }.take(12).forEach { stock ->
                                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(stock.name + " (" + stock.ticker + ")", style = MaterialTheme.typography.titleMedium)
                                                    Text(stock.lane?.title ?: "Conglomerate" + " · " + String.format(Locale.US, "%+.1f%%", stock.change * 100), color = muted)
                                                }
                                                Text(moneyDesktop(stock.price) + "/share", color = mint)
                                            }
                                            val holding = state.portfolio.holdings.firstOrNull { it.key == stock.key }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(5_000.0, 25_000.0).forEach { amount ->
                                                    Button(onClick = { perform("Bought shares in " + stock.name) { buyShares(stock.key, amount) } }, enabled = state.buyBlocker(stock.key, amount) == null) { Text("Buy " + moneyDesktop(amount)) }
                                                }
                                                if (holding != null && holding.shares > 0) OutlinedButton(onClick = { perform("Sold part of " + stock.name) { sellShares(stock.key, 0.25) } }) { Text("Sell 25%") }
                                            }
                                        }
                                        HorizontalDivider(color = borderLine)
                                    }
                                }
                                if (state.games != null) {
                                    CardBlock("ACQUISITIONS", "Buy a studio to add its releases and profit to your portfolio.") {
                                        state.games!!.subsidiaries.forEach { sub ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(sub.name)
                                                    Text("Lifetime profit " + moneyDesktop(sub.lifetimeProfit), color = muted)
                                                }
                                                OutlinedButton(onClick = { perform("Divested " + sub.name) { divest(sub.name) } }) { Text("Sell studio") }
                                            }
                                        }
                                        state.studiosForSale().take(10).forEach { studio ->
                                            val blocker = state.acquisitionBlocker(studio)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(studio.name + if (studio.indie) " · indie" else "")
                                                    Text("Valuation " + moneyDesktop(state.studioValue(studio)) + " · asking " + moneyDesktop(state.acquisitionPrice(studio)), color = muted)
                                                }
                                                Button(onClick = { perform("Acquired " + studio.name) { acquire(studio.name) } }, enabled = blocker == null) { Text("Acquire") }
                                            }
                                        }
                                    }
                                }
                                CardBlock("CROSSOVER PRODUCTS", "Pair two active businesses to build a product unique to your company.") {
                                    Combo.available(state.lanes).forEach { combo ->
                                        val crossover = state.crossovers.firstOrNull { it.combo == combo }
                                        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(combo.product, style = MaterialTheme.typography.titleMedium)
                                            Text(combo.pitch, color = muted)
                                            if (crossover == null) {
                                                Button(onClick = { perform("Started crossover " + combo.product) { buildCrossover(combo) } }, enabled = state.cash >= combo.buildCost) { Text("Build · " + moneyDesktop(combo.buildCost) + " · " + combo.buildWeeks + " weeks") }
                                            } else {
                                                ValueLine("Status", if (crossover.built) "Built" else "Building · " + crossover.buildWeeksLeft + " weeks left")
                                                if (crossover.built) {
                                                    ValueLine("Pressure", (crossover.pressure * 100).toInt().toString() + "%")
                                                    ValueLine("Intensity", (crossover.intensity * 100).toInt().toString() + "%")
                                                    Slider(value = crossover.intensity.toFloat(), onValueChange = { value -> perform("Adjusted " + combo.product + " intensity.") { setIntensity(combo, value.toDouble()) } })
                                                    Text("Risk: " + combo.problem, color = muted, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            DesktopPage.NEWS -> {
                                CardBlock("THE PRESS & INDUSTRY", "Stories are generated by the shared simulation and react to your choices.") {
                                    if (state.news.isEmpty()) Text("No headlines yet. Advance the calendar to see industry news.", color = muted)
                                    state.news.takeLast(40).reversed().forEach { story ->
                                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(story.headline, style = MaterialTheme.typography.titleMedium)
                                            Text(story.outlet + " · " + story.body, color = muted)
                                        }
                                        HorizontalDivider(color = borderLine)
                                    }
                                }
                                CardBlock("MILESTONES", "Achievements and goals give your studio longer-term objectives.") {
                                    state.achievements.entries.sortedByDescending { it.value }.take(20).forEach { Text(it.key.title + " · week " + it.value, color = muted) }
                                    state.goalsDone.takeLast(12).reversed().forEach { Text("Goal: " + it.title, color = mint) }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                            }
                            StudioInspector(
                                state = state,
                                onNavigate = { page = it },
                                modifier = Modifier.width(270.dp).fillMaxHeight().background(sidebar).verticalScroll(rememberScrollState()).padding(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevelopmentPanel(
    state: GameState,
    project: GameProject,
    team: Int,
    perform: (String, GameState.() -> Unit) -> Unit,
    stage: Int,
    setStage: (Int) -> Unit,
    focus: List<Float>,
    setFocus: (List<Float>) -> Unit,
) {
    CardBlock(if (team == 1) "ACTIVE PROJECT" else "TEAM 2 PROJECT", project.title + " · " + project.concept.topic.title + " " + project.concept.genreTitle) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameCover(project.concept, 72.dp, studio = state.companyName)
            MetricCard("PROGRESS", (project.completion * 100).toInt().toString() + "%", "Production complete", Modifier.weight(1f))
            MetricCard("DESIGN", project.design.toInt().toString(), "Design points", Modifier.weight(1f))
            MetricCard("TECH", project.tech.toInt().toString(), "Tech points", Modifier.weight(1f))
            MetricCard("BUGS", project.bugs.toInt().toString(), "Known bugs", Modifier.weight(1f))
        }
        Meter("Production", project.completion)
        Text(if (project.isReady) "Ready to launch. You can ship now or keep polishing." else "Stage " + (project.stage + 1) + " of 3 · week " + project.weeksInDev, color = muted)
        state.gameForecast(team)?.let { forecast ->
            Text("Forecast: " + String.format(Locale.US, "%.1f/10 (%.1f–%.1f)", forecast.expectedScore, forecast.low, forecast.high) + " · " + forecast.bugs.toInt() + " expected bugs", color = mint)
            if (forecast.polishPastSweetSpot) Text("Polish is near its sweet spot; extra time may be better spent elsewhere.", color = muted)
        }
        Text("Effort allocation · tune the three areas in a stage to match the genre.", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..2).forEach { s -> FilterChip(selected = stage == s, onClick = { setStage(s) }, label = { Text("Stage " + (s + 1)) }) }
        }
        val areas = FocusArea.forStage(stage)
        areas.forEachIndexed { i, area ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(area.title, Modifier.weight(1f))
                    Text((focus[i] * 100).toInt().toString() + "%", color = muted)
                }
                Slider(value = focus[i], onValueChange = { next -> setFocus(focus.toMutableList().also { it[i] = next }) })
            }
        }
        Button(onClick = { perform("Saved stage " + (stage + 1) + " focus allocation.") { setStageFocus(stage, focus.map { it.toDouble() }, team) } }) { Text("Apply stage focus") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { perform("Playtest completed for " + project.title + ".") { playtestGame(team) } }, enabled = !project.playtestedStages.contains(if (project.isReady) 3 else project.stage) && state.cash >= project.concept.size.weeklyBudget * (1.5 + (if (project.isReady) 3 else project.stage) * 0.5)) { Text("Paid playtest") }
            OutlinedButton(onClick = { perform("Press release bought.") { buyCampaign(Campaign.PRESS_RELEASE, team) } }, enabled = Research.MARKETING in state.researched && Campaign.PRESS_RELEASE !in project.campaigns && state.cash >= Campaign.PRESS_RELEASE.cost) { Text("Press release") }
            OutlinedButton(onClick = { perform("Ad campaign bought.") { buyCampaign(Campaign.AD_CAMPAIGN, team) } }, enabled = Research.MARKETING in state.researched && Campaign.AD_CAMPAIGN !in project.campaigns && state.cash >= Campaign.AD_CAMPAIGN.cost) { Text("Ad campaign") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { perform("Launched " + project.title + ". Review the result.") { launchGame(team) } }, enabled = project.isReady) { Text("Launch game") }
            OutlinedButton(onClick = { perform("Cancelled " + project.title + ".") { cancelGame(team) } }) { Text("Cancel project") }
        }
    }
}

@Composable
private fun NavigationButton(item: DesktopPage, selected: Boolean, onClick: () -> Unit) {
    val selectedColor = mint.copy(alpha = 0.12f)
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(37.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedColor else Color.Transparent,
            contentColor = if (selected) mint else muted,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(27.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) mint.copy(alpha = 0.15f) else panelRaised),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.marker, style = MaterialTheme.typography.labelSmall, color = if (selected) mint else muted, fontWeight = FontWeight.Bold)
            }
            Text(item.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (selected) ink else muted, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (selected) Box(Modifier.width(3.dp).height(18.dp).clip(CircleShape).background(mint))
        }
    }
}

@Composable
private fun StudioOfficeCard(state: GameState, onTeamClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = panelRaised),
        border = BorderStroke(1.dp, borderLine),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(278.dp).background(panelRaised),
            ) {
                OfficeScene(state, playing = true, modifier = Modifier.fillMaxSize(), insetTop = 38.dp)
                Row(
                    Modifier.align(Alignment.TopStart).padding(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SceneBadge(state.office.title.uppercase(Locale.US), mint)
                    SceneBadge("${state.staff.size}/${state.office.desks} DESKS", sky)
                }
            }
            Row(
                Modifier.fillMaxWidth().background(panel).padding(horizontal = 20.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FounderPortrait(state, size = 42.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("YOUR STUDIO FLOOR", color = mint, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.1.sp)
                    val project = state.games?.project
                    Text(
                        project?.let { "${it.title} is in production" } ?: "${state.companyName} is ready for work",
                        color = ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        project?.let { "${it.concept.topic.title} ${it.concept.genreTitle} · ${(it.completion * 100).toInt()}% complete" }
                            ?: "Meet your team, improve the office, and start a project.",
                        color = muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(onClick = onTeamClick, shape = RoundedCornerShape(13.dp)) {
                    Text("Manage team")
                }
            }
        }
    }
}

@Composable
private fun ActionNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = mint.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, mint.copy(alpha = 0.23f)),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(mint))
            Text(message, Modifier.weight(1f), color = ink, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("WEEK", color = muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.7.sp)
        }
    }
}

@Composable
private fun StudioInspector(
    state: GameState,
    onNavigate: (DesktopPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("STUDIO INSPECTOR", Modifier.padding(start = 3.dp, top = 3.dp), style = MaterialTheme.typography.labelSmall, color = muted, letterSpacing = 1.1.sp)
        CardBlock("Studio snapshot", "A quick read on the business") {
            ValueLine("Weekly burn", moneyDesktop(state.weeklyBurn))
            ValueLine("Runway", if (state.runwayWeeks.isInfinite()) "No projected burn" else "${state.runwayWeeks.toInt()} weeks")
            ValueLine("Valuation", moneyDesktop(state.valuation))
            ValueLine("Team", state.staff.size.toString() + " people")
            ValueLine("Research", state.researchPoints.toInt().toString() + " RP")
        }
        val project = state.games?.project
        CardBlock(if (project == null) "Next step" else "In production", project?.title ?: "Give your team a project") {
            if (project == null) {
                Text(
                    if (state.games == null) "Grow the company or explore the game studio when it is available." else "Start a game project and turn your studio into a release team.",
                    color = muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = { onNavigate(if (state.games == null) DesktopPage.BUSINESSES else DesktopPage.MAKE) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(if (state.games == null) "Explore businesses" else "Design a game") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GameCover(project.concept, 58.dp, studio = state.companyName)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(project.concept.topic.title + " · " + project.concept.genreTitle, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (project.isReady) "Ready to launch" else "Stage ${project.stage + 1} of 3", style = MaterialTheme.typography.labelMedium, color = ink, fontWeight = FontWeight.SemiBold)
                    }
                }
                Meter("Production", project.completion)
                Button(onClick = { onNavigate(DesktopPage.MAKE) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(if (project.isReady) "Review and launch" else "Open production")
                }
            }
        }
        CardBlock("Quick links", "Jump to a studio area") {
            InspectorLink(DesktopPage.MAKE, onNavigate)
            InspectorLink(DesktopPage.TEAM, onNavigate)
            InspectorLink(DesktopPage.MARKET, onNavigate)
            InspectorLink(DesktopPage.COMPANY, onNavigate)
        }
    }
}

@Composable
private fun InspectorLink(page: DesktopPage, onNavigate: (DesktopPage) -> Unit) {
    TextButton(
        onClick = { onNavigate(page) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(page.label, color = ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(page.description, color = muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SceneBadge(text: String, tint: Color) {
    Surface(
        shape = CircleShape,
        color = panel.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ink.copy(alpha = 0.1f)),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    val accent = when {
        label.contains("BUG", ignoreCase = true) || label.contains("FLOP", ignoreCase = true) -> coral
        label.contains("WEEK", ignoreCase = true) || label.contains("HIT", ignoreCase = true) || label.contains("RESEARCH", ignoreCase = true) || label.contains("TECH", ignoreCase = true) -> sky
        label.contains("TEAM", ignoreCase = true) || label.contains("DESIGN", ignoreCase = true) || label.contains("PROGRESS", ignoreCase = true) -> Color(0xFF654E9B)
        else -> mint
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, borderLine),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label.uppercase(Locale.US), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = muted, letterSpacing = 0.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box(Modifier.width(30.dp).height(3.dp).clip(CircleShape).background(accent.copy(alpha = 0.65f)))
        }
    }
}

@Composable
private fun CardBlock(title: String, subtitle: String = "", modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val accent = when {
        title.contains("NEWS", ignoreCase = true) || title.contains("INDUSTRY", ignoreCase = true) -> sky
        title.contains("FINANCE", ignoreCase = true) || title.contains("CASH", ignoreCase = true) -> mint
        title.contains("RESEARCH", ignoreCase = true) || title.contains("TECH", ignoreCase = true) -> Color(0xFF654E9B)
        title.contains("DECISION", ignoreCase = true) || title.contains("REVIEW", ignoreCase = true) -> coral
        else -> mint
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, borderLine),
    ) {
        Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(31.dp).clip(CircleShape).background(accent))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = ink, fontWeight = FontWeight.Bold)
                    if (subtitle.isNotBlank()) Text(subtitle, color = muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            content()
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = panelRaised,
        border = BorderStroke(1.dp, borderLine),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = muted, letterSpacing = 0.7.sp)
                Text(value, style = MaterialTheme.typography.titleSmall, color = ink, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FieldLabel(label: String) { Text(label, style = MaterialTheme.typography.labelLarge, color = muted) }

@Composable
private fun ValueLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = muted)
        Text(value, color = ink)
    }
}

@Composable
private fun Meter(label: String, value: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = muted, style = MaterialTheme.typography.labelMedium)
            Text((value.coerceIn(0.0, 1.0) * 100).toInt().toString() + "%", color = mint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { value.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = mint,
            trackColor = borderLine,
        )
    }
}

@Composable
private fun <T> EnumPicker(label: String, values: List<T>, selected: T, onSelected: (T) -> Unit, modifier: Modifier = Modifier, display: (T) -> String) {
    var expanded by remember(label) { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(display(selected), maxLines = 1) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { item ->
                    DropdownMenuItem(text = { Text(display(item)) }, onClick = { onSelected(item); expanded = false })
                }
            }
        }
    }
}

private fun moneyDesktop(value: Double): String = "$" + String.format(Locale.US, "%,.0f", value)
private fun newGame(): GameState = GameState.create(
    companyName = "Boot Studio",
    founderName = "Founder",
    lane = Lane.GAMES,
)
