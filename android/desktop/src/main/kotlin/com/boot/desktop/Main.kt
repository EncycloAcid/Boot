package com.boot.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.boot.core.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

private val savePath: Path = Path.of(System.getProperty("user.home"), ".boot", "desktop-save.json")
private val mint = Color(0xFF9EE6C1)
private val panel = Color(0xFF202A24)
private val muted = Color(0xFFB7C1BA)

private enum class DesktopPage(val label: String) {
    OVERVIEW("Overview"), MAKE("Make"), GAMES("Games"), BUSINESSES("Businesses"), MARKET("Market"),
    TECH("Tech & Research"), STRATEGY("Strategy"), TEAM("Team"), COMPANY("Company"), NEWS("News")
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Boot — Game Tycoon",) {
        var state by remember { mutableStateOf(newGame()) }
        var page by remember { mutableStateOf(DesktopPage.OVERVIEW) }
        var notice by remember { mutableStateOf("Welcome to your studio. Your next decision is yours.") }
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
        var deviceCategory by remember { mutableStateOf(DeviceCategory.PHONE) }
        var deviceTier by remember { mutableStateOf(DeviceTier.FLAGSHIP) }
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
            colorScheme = darkColorScheme(
                primary = mint, secondary = Color(0xFFB8C7FF),
                background = Color(0xFF101411), surface = Color(0xFF1B211D),
            ),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.width(238.dp).fillMaxHeight().background(Color(0xFF171D19)).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("BOOT", style = MaterialTheme.typography.headlineLarge, color = mint)
                        Text("GAME TYCOON", style = MaterialTheme.typography.labelLarge, color = muted)
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFF354139))
                        Text(state.companyName, style = MaterialTheme.typography.titleMedium)
                        Text("Founder: " + state.founder.name, color = muted, style = MaterialTheme.typography.bodySmall)
                        Text("Week " + state.week + " · " + state.dateLabel, color = muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        DesktopPage.entries.forEach { item ->
                            NavigationButton(item.label, page == item) { page = item }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                perform("Week closed. Review the studio feed for consequences.") { advance() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isOver && !state.needsDecision && state.games?.unseenReview == null,
                        ) { Text("Advance week  ↵") }
                        OutlinedButton(onClick = {
                            try {
                                Files.createDirectories(savePath.parent)
                                Files.writeString(savePath, state.encode())
                                notice = "Saved to " + savePath.toAbsolutePath()
                            } catch (e: Exception) { notice = "Save failed: " + e.message }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save game") }
                        OutlinedButton(onClick = {
                            try {
                                if (Files.exists(savePath)) {
                                    state = GameState.decode(Files.readString(savePath))
                                    notice = "Save loaded."
                                } else notice = "No desktop save found yet."
                            } catch (e: Exception) { notice = "Load failed: " + e.message }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Load game") }
                        OutlinedButton(onClick = {
                            state = newGame()
                            page = DesktopPage.OVERVIEW
                            notice = "New studio founded. Good luck."
                        }, modifier = Modifier.fillMaxWidth()) { Text("New game") }
                    }

                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(page.label, style = MaterialTheme.typography.headlineMedium)
                                Text("Build a studio, make games, learn from the market.", color = muted)
                            }
                            Text("SEASON · " + GameSeason.forWeek(state.week + 1).title.uppercase(), color = mint, style = MaterialTheme.typography.labelLarge)
                        }
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
                            DesktopPage.OVERVIEW -> {
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
                                            Text(p.title, style = MaterialTheme.typography.titleLarge)
                                            Text(p.concept.topic.title + " · " + p.concept.genre.title + " · " + p.concept.size.title, color = muted)
                                            Meter("Production", p.completion)
                                            Text(if (p.isReady) "Ready to launch" else "Stage " + (p.stage + 1) + " of 3 · " + p.weeksInDev + " weeks in", color = muted)
                                            Button(onClick = { page = DesktopPage.MAKE }) { Text("Open project") }
                                        }
                                    }
                                    CardBlock("WEEKLY FINANCES", modifier = Modifier.weight(1f)) {
                                        ValueLine("Estimated weekly burn", moneyDesktop(state.weeklyBurn))
                                        ValueLine("Runway", if (state.runwayWeeks.isInfinite()) "No projected burn" else state.runwayWeeks.toInt().toString() + " weeks")
                                        ValueLine("Studio valuation", moneyDesktop(state.valuation))
                                        ValueLine("Lifetime game fans", Format.count(state.games?.fans ?: 0.0))
                                    }
                                }
                                CardBlock("STUDIO FEED", notice) {
                                    if (state.needsDecision) Text("A decision is waiting. Resolve it before advancing the calendar.", color = Color(0xFFFFD18A))
                                    state.log.takeLast(8).reversed().forEach { Text("•  " + it.message, color = muted) }
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
                                    if (project != null) DevelopmentPanel(state, project, 1, notice, ::perform, focusStage, { focusStage = it }, focusValues, { focusValues = it })
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
                                            engineId = studio?.engines?.maxByOrNull { it.level }?.id ?: 0,
                                            sequelOf = sequelOf, live = liveService,
                                        )
                                        val blocker = state.conceptBlocker(concept, team = targetTeam)
                                        Text("Target platform: PC · " + state.estimatedWeeks(concept) + " estimated weeks", color = muted)
                                        Text("Budget: " + moneyDesktop(size.weeklyBudget) + "/week · " + moneyDesktop(size.weeklyBudget * 4) + " first-month planning cost", color = muted)
                                        if (blocker != null) Text(blocker, color = Color(0xFFFFD18A))
                                        Button(
                                            onClick = { perform("Started " + concept.title + " for team " + targetTeam + ".") { startGame(concept, team = targetTeam) }; sequelOf = null },
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
                                    DevelopmentPanel(state, project, 1, notice, ::perform, focusStage, { focusStage = it }, focusValues, { focusValues = it })
                                }
                                if (project2 != null) DevelopmentPanel(state, project2, 2, notice, ::perform, focusStage, { focusStage = it }, focusValues, { focusValues = it })
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
                                if (g == null) CardBlock("GAME STUDIO NOT OPEN", "Start a new run with the Games lane to manage game projects.")
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(release.title, style = MaterialTheme.typography.titleMedium)
                                                        Text(release.concept.topic.title + " · " + release.concept.genreTitle + " · " + release.concept.size.title, color = muted)
                                                    }
                                                    Text(String.format(Locale.US, "%.1f/10", release.score), color = if (release.score >= 8) mint else Color.White, style = MaterialTheme.typography.titleLarge)
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
                                                            liveService = sequel.live
                                                            page = DesktopPage.MAKE
                                                            notice = "Sequel concept loaded into the designer."
                                                        }
                                                    }) { Text("Design sequel") }
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
                                            HorizontalDivider(color = Color(0xFF354139))
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
                                                        EnumPicker("Monetization", AppMonetization.entries, app.monetization, { choice -> perform("App monetization changed.") { setMonetization(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        EnumPicker("Update cadence", UpdateStyle.entries, app.updateStyle, { choice -> perform("App update cadence changed.") { setUpdateStyle(choice) } }, Modifier.fillMaxWidth()) { it.title }
                                                        EnumPicker("Pricing", AppPricing.entries, app.pricing, { choice -> perform("App pricing changed.") { apps!!.pricing = choice } }, Modifier.fillMaxWidth()) { it.title }
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
                                                            Text(design.name + " · " + design.spec.category.title + " · " + design.spec.tier.title, style = MaterialTheme.typography.titleMedium)
                                                            Meter("Device design", design.completion)
                                                            if (design.isReady) Button(onClick = { perform("Launched device.") { launchDevice() } }) { Text("Launch device") }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            EnumPicker("Device", DeviceCategory.entries, deviceCategory, { deviceCategory = it }, Modifier.weight(1f)) { it.title }
                                                            EnumPicker("Tier", DeviceTier.entries, deviceTier, { deviceTier = it }, Modifier.weight(1f)) { it.title }
                                                        }
                                                        Button(onClick = { perform("Started device design.") { startDevice(deviceCategory, deviceTier) } }, enabled = lab.design == null) { Text("Start device design") }
                                                        if (lab.design != null) Button(onClick = { perform("Booked 100 production units.") { orderUnits(100) } }, enabled = lab.design?.isReady == true && state.cash > lab.design!!.spec.unitCost * 100) { Text("Book 100 units") }
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
                                            HorizontalDivider(color = Color(0xFF354139))
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
                                        HorizontalDivider(color = Color(0xFF354139))
                                    }
                                }
                            }
                            DesktopPage.TEAM -> {
                                CardBlock("YOUR TEAM", "Hiring costs two weeks of salary up front. Skills and roles affect output.") {
                                    ValueLine("Payroll / week", moneyDesktop(state.payroll))
                                    ValueLine("Office", state.office.title + " · " + state.freeDesks + " free desks")
                                    if (state.staff.isEmpty()) Text("You're working solo. Hire specialists when you can afford them.", color = muted)
                                    state.staff.forEach { employee ->
                                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(candidate.name + " · " + candidate.role.title)
                                                Text("Skill " + candidate.skill + " · " + moneyDesktop(candidate.askingSalary) + "/week · hiring fee " + moneyDesktop(candidate.askingSalary * 2), color = muted)
                                            }
                                            Button(onClick = { perform("Hired " + candidate.name) { hire(candidate.id, Lane.GAMES) }, enabled = state.cash >= candidate.askingSalary * 2 && state.freeDesks > 0) { Text("Hire for Games") }
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
                            }
                            DesktopPage.NEWS -> {
                                CardBlock("THE PRESS & INDUSTRY", "Stories are generated by the shared simulation and react to your choices.") {
                                    if (state.news.isEmpty()) Text("No headlines yet. Advance the calendar to see industry news.", color = muted)
                                    state.news.takeLast(40).reversed().forEach { story ->
                                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(story.headline, style = MaterialTheme.typography.titleMedium)
                                            Text(story.outlet + " · " + story.body, color = muted)
                                        }
                                        HorizontalDivider(color = Color(0xFF354139))
                                    }
                                }
                                CardBlock("MILESTONES", "Achievements and goals give your studio longer-term objectives.") {
                                    state.achievements.entries.sortedByDescending { it.value }.take(20).forEach { Text(it.key.title + " · week " + it.value, color = muted) }
                                    state.goalsDone.takeLast(12).reversed().forEach { Text("Goal: " + it.title, color = mint) }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(notice, color = mint, style = MaterialTheme.typography.bodyMedium)
                        Text("Desktop port · shared Kotlin simulation · save file: " + savePath.toString(), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
    notice: String,
    perform: (String, GameState.() -> Unit) -> Unit,
    stage: Int,
    setStage: (Int) -> Unit,
    focus: List<Float>,
    setFocus: (List<Float>) -> Unit,
) {
    CardBlock(if (team == 1) "ACTIVE PROJECT" else "TEAM 2 PROJECT", project.title + " · " + project.concept.topic.title + " " + project.concept.genreTitle) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
private fun NavigationButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth()) }
    else TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label, modifier = Modifier.fillMaxWidth(), color = muted) }
}

@Composable
private fun MetricCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = muted)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = mint)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}

@Composable
private fun CardBlock(title: String, subtitle: String = "", modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B211D))) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            if (subtitle.isNotBlank()) Text(subtitle, color = muted)
            content()
        }
    }
}


@Composable
private fun FieldLabel(label: String) { Text(label, style = MaterialTheme.typography.labelLarge, color = muted) }

@Composable
private fun ValueLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = muted)
        Text(value, color = Color.White)
    }
}

@Composable
private fun Meter(label: String, value: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = muted)
            Text((value.coerceIn(0.0, 1.0) * 100).toInt().toString() + "%", color = mint)
        }
        LinearProgressIndicator(progress = { value.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
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
