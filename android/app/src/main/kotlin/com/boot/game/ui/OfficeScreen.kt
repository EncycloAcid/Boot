package com.boot.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.GameClock
import com.boot.core.Campaign
import com.boot.core.Format
import com.boot.core.GameProject
import com.boot.core.GameSeason
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.showsAhead
import com.boot.core.Research
import com.boot.core.buyCampaign
import com.boot.core.exclusiveBlocker
import com.boot.core.expansionBlocker
import com.boot.core.pitchExclusive
import com.boot.core.pitchOdds
import com.boot.core.freeDesks
import com.boot.core.gameForecast
import com.boot.core.launchGame
import com.boot.core.researchBlocker
import com.boot.core.researchPerWeek
import com.boot.game.GameStore
import com.boot.game.StudioPage
import com.boot.game.Tab
import java.util.Locale

/**
 * Today briefing: everything that used to sit under the office card. The isometric office is now
 * the full-bleed stage behind floating windows — this panel keeps every home-screen feature.
 */
@Composable
fun OfficeScreen(store: GameStore, state: GameState) {
    val inPanel = LocalInFloatingPanel.current
    ScreenColumn {
        if (inPanel) {
            // Sideways: the office is the stage behind this window, so lead with the numbers.
            item { VitalsGlance(store, state) }
            item {
                GroupedSection("Office") {
                    CardBlock {
                        val crowded = state.staff.size > state.office.desks
                        Text(state.office.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Hint(
                            "${state.staff.size}/${state.office.desks} desks · " + when {
                                crowded -> "crowded: move somewhere bigger before you hire"
                                state.staff.size == state.office.desks -> "full: move somewhere bigger to grow"
                                else -> "hire from the Team tab"
                            },
                        )
                        Ticker(store, state)
                    }
                }
            }
        }
        // The tutorial leads, so its button is on screen without scrolling.
        if (state.tutorial != null) item { LessonCard(store, state) }
        // Upright: the office itself sits at the top of the home page, under the lesson.
        if (!inPanel) item { OfficeCard(store, state) }
        if (state.scenario != null) item { ScenarioCard(state) }
        // While the tutorial runs, the next step only speaks up about money.
        if (nextStep(state)?.let { it.key != store.dismissedStep && (state.tutorial == null || it.key in urgentSteps) } == true) {
            item { NextStepCard(store, state) }
        }
        if (state.inbox.isNotEmpty()) item { InboxSection(store, state) }
        if (Lane.GAMES in state.lanes) item { StudioNow(store, state) }
        val others = state.lanes.filter { it != Lane.GAMES }
        if (others.isNotEmpty()) item { LanesNow(store, state, others) }
        item {
            GoalsCard(state) {
                store.companyPage = 5
                store.tab = Tab.COMPANY
            }
        }
        if (state.achievements.isNotEmpty()) {
            item {
                RecentAchievements(state) {
                    store.companyPage = 5
                    store.tab = Tab.COMPANY
                }
            }
        }
        if (state.games != null) {
            item { ChartCard(state, limit = 5, action = { PlainButton("Full chart", { store.openStudio(StudioPage.MARKET) }) }) { store.openRelease = it } }
            item { ComingSoonCard(state, limit = 3) }
        }
        item { HeadlinesCard(store, state) }
        item { Shortcuts(store, state) }
        item {
            GroupedSection("Latest", action = { PlainButton("All news", { store.tab = Tab.NEWS }) }) {
                state.log.takeLast(4).asReversed().forEachIndexed { i, entry ->
                    if (i > 0) RowDivider(36.dp)
                    LogRow(entry)
                }
            }
        }
    }
}

/** The office with everyone at their desks, for the upright layout, with the latest news under it. */
@Composable
private fun OfficeCard(store: GameStore, state: GameState) {
    val colors = gameColors
    androidx.compose.material3.Surface(shape = RoundedCornerShape(28.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
        Column {
            val crowded = state.staff.size > state.office.desks
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            ) {
                // Tap someone for their card; tap the room itself for the Team tab.
                OfficeScene(
                    state, store.playing, Modifier.fillMaxSize(), insetTop = 40.dp,
                    people = true, onManage = { store.tab = Tab.TEAM }, onTap = { store.tab = Tab.TEAM }, onFounder = { store.founderOpen = true },
                )
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SceneChip(state.office.title)
                    SceneChip(
                        "${state.staff.size}/${state.office.desks} desks" + if (crowded) " · crowded" else "",
                        icon = Icons.Filled.Groups,
                        tint = if (crowded) colors.bad else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Ticker(store, state)
        }
    }
}

@Composable
private fun SceneChip(text: String, icon: ImageVector? = null, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(gameColors.card.copy(alpha = 0.86f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        FitText(text, MaterialTheme.typography.labelLarge, color = tint)
    }
}

/** Scannable vitals that used to live only on the top HUD — labeled so nothing is cryptic. */
@Composable
private fun VitalsGlance(store: GameStore, state: GameState) {
    val colors = gameColors
    val weekly = state.trailingProfit(weeks = 4) / 4
    val fans = state.games?.fans
    val avgMorale = state.staff.takeIf { it.isNotEmpty() }?.map { it.morale }?.average()
    GroupedSection("At a glance") {
        val money: @Composable RowScope.() -> Unit = {
            GlanceTile("Cash", Format.money(state.cash), Format.signedMoney(weekly) + "/wk", if (state.cash < 0) colors.bad else colors.good, Modifier.weight(1f)) {
                store.companyPage = 1
                store.tab = Tab.COMPANY
            }
            GlanceTile(
                "Research",
                "${Format.count(state.researchPoints)} RP",
                "+${String.format(Locale.US, "%.1f", state.researchPerWeek)}/wk",
                colors.milestone,
                Modifier.weight(1f),
            ) { store.tab = Tab.RESEARCH }
            if (fans != null && Lane.GAMES in state.lanes) {
                GlanceTile("Fans", Format.count(fans), "loyal", Color(0xFFC44D8A), Modifier.weight(1f)) {
                    store.openStudio(StudioPage.GAMES)
                }
            }
            GlanceTile("Rep", "${state.reputation.score.toInt()}", "out of 100", colors.warn, Modifier.weight(1f)) {
                store.companyPage = 0
                store.tab = Tab.COMPANY
            }
        }
        val company: @Composable RowScope.() -> Unit = {
            GlanceTile(
                "Team",
                "${state.staff.size + 1}",
                avgMorale?.let { "morale ${String.format(Locale.US, "%.0f", it * 100)}%" } ?: "just you",
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f),
            ) { store.tab = Tab.TEAM }
            GlanceTile(
                "Desks",
                "${state.staff.size}/${state.office.desks}",
                when {
                    state.staff.size > state.office.desks -> "crowded"
                    state.staff.size == state.office.desks -> "all full"
                    else -> "${state.office.desks - state.staff.size} free"
                },
                if (state.staff.size > state.office.desks) colors.bad else MaterialTheme.colorScheme.primary,
                Modifier.weight(1f),
            ) { store.tab = Tab.TEAM }
            state.listing?.let { listing ->
                GlanceTile(
                    listing.ticker,
                    "$${String.format(Locale.US, "%.0f", listing.price)}",
                    Format.money(listing.marketCap) + " cap",
                    MaterialTheme.colorScheme.primary,
                    Modifier.weight(1f),
                ) {
                    store.companyPage = 0
                    store.tab = Tab.COMPANY
                }
            } ?: GlanceTile(
                "Date",
                state.dateLabel,
                state.world.era.title.removePrefix("The "),
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.weight(1f),
            ) { /* calendar glance only */ }
        }
        CardBlock {
            if (LocalCompact.current) {
                // Sideways there's room for every tile on one row.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    money()
                    company()
                }
            } else {
                // Tiles in a row share a height, whatever their captions.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp), content = money)
                Spacer(Modifier.size(8.dp))
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp), content = company)
            }
        }
    }
}

@Composable
private fun GlanceTile(
    label: String,
    value: String,
    caption: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = if (LocalCompact.current) 6.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** The newest headline, sliding in as it happens. */
@Composable
private fun Ticker(store: GameStore, state: GameState) {
    val colors = gameColors
    val motion = MaterialTheme.motionScheme
    val latest = state.log.lastOrNull()
    val hint = if (state.week < 2 && !store.playing) "Tap Play to let time run. It stops by itself when you're needed." else null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { store.tab = Tab.NEWS }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = hint ?: latest?.text.orEmpty(),
            transitionSpec = {
                (slideInVertically(motion.fastSpatialSpec()) { it } + fadeIn()) togetherWith (slideOutVertically(motion.fastSpatialSpec()) { -it } + fadeOut())
            },
            label = "ticker",
            modifier = Modifier.weight(1f),
        ) { text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            if (hint != null || latest == null) MaterialTheme.colorScheme.primary else colors.tone(latest.tone, MaterialTheme.colorScheme.outline),
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// What's happening now

@Composable
private fun StudioNow(store: GameStore, state: GameState) {
    val colors = gameColors
    val g = state.games ?: return
    val p = g.project
    val c = g.contract
    val x = g.expansion
    // The shows of the next couple of months, so there's time to plan a reveal for one.
    val show = state.showsAhead(8).filter { it.weeks >= 1 }.take(3).takeIf { it.isNotEmpty() }?.let { shows ->
        "Coming up: " + shows.joinToString(" · ") { "${it.name} in ${Format.weeks(it.weeks)}" } + ". " +
            (p?.let { game -> "A stage for ${game.title}." } ?: "Have something in development to show off.")
    }
    GroupedSection("Game studio", footer = show, action = { PlainButton("Studio", { store.openStudio(StudioPage.MAKE) }) }) {
        g.project2?.let { p2 -> ProjectCard(store, state, p2, team = 2) }
        if (g.project2 != null && (p != null || c != null || x != null)) RowDivider()
        when {
            p != null -> ProjectCard(store, state, p)
            g.project2 != null && c == null && x == null -> CardBlock {
                Hint("Team 1 is free.")
                TonalButton("Design a game for Team 1", { store.designGame() }, Modifier.fillMaxWidth())
            }
            x != null -> CardBlock {
                Text(x.title, style = MaterialTheme.typography.titleLargeEmphasized)
                Hint("Expansion pack. Everyone who bought the original is a potential customer.")
                Meter("Progress", x.completion, colors.milestone, wavy = store.playing, caption = Format.weeks(x.weeks) + " in")
            }
            c != null -> CardBlock {
                Text("Contract: ${c.title}", style = MaterialTheme.typography.titleLargeEmphasized)
                Meter("Progress", c.completion, wavy = store.playing, caption = "week ${c.weeksElapsed} of ${c.weeksAllowed} · pays ${Format.money(c.pay)}")
            }
            else -> CardBlock {
                Text("The studio is free", style = MaterialTheme.typography.titleLargeEmphasized)
                Hint(
                    if (g.releases.isEmpty()) {
                        "Pick a topic, a genre and a platform, then guide the team through three stages of development."
                    } else {
                        (if (g.releases.size == 1) "1 game shipped" else "${g.releases.size} games shipped") +
                            (if (g.topScore > 0) ", best review ${String.format(java.util.Locale.US, "%.1f", g.topScore)}" else "") + ". What's next?"
                    },
                )
                PrimaryButton("Develop a new game", { store.designGame() }, Modifier.fillMaxWidth())
                val offers = g.contractOffers.size + g.dealOffers.size
                if (offers > 0) {
                    TonalButton("$offers contract${if (offers == 1) "" else "s"} and deals on offer", { store.openStudio(StudioPage.MAKE) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(store: GameStore, state: GameState, p: GameProject, team: Int = 1) {
    val colors = gameColors
    val c = p.concept
    val stage = if (p.isReady) "Polish" else listOf("Foundations", "Content", "Presentation")[p.stage]
    CardBlock {
        Row(verticalAlignment = Alignment.Top) {
            GameCover(c, 48.dp, studio = state.companyName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stage,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Text(p.title, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Hint((if (team == 2) "Team 2 · " else "") + "${c.size.title} ${c.topic.title.lowercase()} ${c.genreLower} · ${c.platforms.joinToString { it.title }}")
            }
        }
        if (p.isReady) {
            Meter("Polish", p.polish / (p.work * 0.4), colors.milestone)
            val forecast = state.gameForecast(team)
            if (forecast != null) {
                Hint(
                    String.format(
                        java.util.Locale.US,
                        "Content complete. Expected ~%.1f/10 if you ship now%s.",
                        forecast.expectedScore,
                        if (forecast.bugs >= 2) " — ${forecast.bugs.toInt()} bugs still open" else "",
                    ),
                )
            } else {
                Hint("Content complete. Bugs get fixed while you polish.")
            }
        } else {
            val eta = state.gameForecast(team)?.weeksToReady
            val caption = when {
                eta == null -> Format.weeks(p.weeksInDev) + " in"
                eta <= 0 -> "ready this week"
                else -> "~${Format.weeks(eta)} left · ${Format.weeks(p.weeksInDev)} in"
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Development · ${Format.percent(p.completion)}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.padding(end = 12.dp))
                Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
            }
            BuildTimeline(p)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat("Design", Format.count(p.design), colors.design, Modifier.weight(1f))
            MiniStat("Tech", Format.count(p.tech), colors.tech, Modifier.weight(1f))
            MiniStat("Bugs", Format.count(p.bugs), colors.bad, Modifier.weight(1f))
            MiniStat("Hype", "${p.hype.toInt()}", colors.warn, Modifier.weight(1f))
        }
        ForecastTrend(p.forecasts)
        if (p.lastDesign + p.lastTech > 0.5) {
            Hint("This week · +${Format.count(p.lastDesign)} design, +${Format.count(p.lastTech)} tech" + if (p.lastBugs >= 1) ", ${Format.count(p.lastBugs)} new bugs" else "")
        }
        if (c.production != com.boot.core.Production.STANDARD || c.hook != null) {
            Hint(listOfNotNull("${c.production.title} production".takeIf { c.production != com.boot.core.Production.STANDARD }, c.hook?.let { "${it.title} hook" }).joinToString(" · "))
        }
        if (p.choices.isNotEmpty()) Hint("Decided · " + p.choices.joinToString(" · "))
        val signed = p.exclusive
        if (signed != null) {
            val where = com.boot.core.exclusivePlatforms(c, signed.maker).joinToString(" and ") { it.title }
            Hint(
                if (signed.timed) "Timed exclusive · $where first, everywhere else ${Format.weeks(signed.window)} later · ${signed.maker} paid ${Format.money(signed.payment)}"
                else "$where exclusive · ${signed.maker} paid ${Format.money(signed.payment)}",
                MaterialTheme.colorScheme.primary,
            )
        } else if (!p.isReady) {
            val makers = com.boot.core.exclusiveMakers(c).filter { state.exclusiveBlocker(team, it) == null }
            if (makers.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Console makers pay for exclusives.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    MenuButton("Pitch an exclusive", makers, { "$it · ${Format.percent(state.pitchOdds(team, it))} chance" }) { maker ->
                        store.perform { pitchExclusive(team, maker) }
                    }
                }
            }
        }
        val season = GameSeason.forWeek(state.week)
        val lift = season.launchMultiplier(c.genre)
        val seasonPct = ((lift - 1) * 100).toInt()
        Hint(
            when {
                lift > 1.0 -> "${season.title} · ${c.genreTitle} +$seasonPct% launch demand"
                lift < 1.0 -> "${season.title} · ${c.genreTitle} $seasonPct% launch demand"
                else -> "${season.title} · ${season.weeksLabel}"
            },
            if (lift >= 1.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Hint("Direction · ${p.creativeFocus.title}. ${p.creativeFocus.blurb}")
        Hint(
            (if (p.reveals > 0) "${Format.count(p.wishlists)} wishlists" else "Not announced yet") + " · " +
                (p.releaseWeek?.let { "launches ${GameClock.label(it)}, in ${Format.weeks(it - state.week)}" } ?: "no release date"),
            if (p.releaseWeek != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Release", { store.perform { launchGame(team) } }, Modifier.weight(1f), enabled = p.isReady)
            TonalButton("Calendar", { store.openCalendar(team) })
            if (Research.MARKETING in state.researched) {
                val open = Campaign.entries.filter { it !in p.campaigns }
                MenuButton("Market", open, { "${it.title} · ${Format.money(it.cost)}" }, enabled = open.isNotEmpty()) { camp ->
                    store.perform { buyCampaign(camp, team) }
                }
            }
            if (!p.isReady) TonalButton("Focus", { store.openStudio(StudioPage.MAKE) })
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** What a lane is building right now, if anything, and how far along it is. */
private fun GameState.inProgress(lane: Lane): Pair<String, Double>? = when (lane) {
    Lane.GAMES -> null
    Lane.WEBSITES -> web?.active?.firstOrNull()?.let { "${it.client}${if ((web?.active?.size ?: 0) > 1) " and ${web!!.active.size - 1} more" else ""}" to it.completion }
    Lane.APPS -> apps?.takeIf { !it.isLive }?.let { "Building ${it.name}" to it.buildProgress / it.buildWork }
    Lane.SOCIAL -> social?.takeIf { !it.isLive }?.let { "Building ${it.name}" to it.buildProgress / it.buildWork }
    Lane.HARDWARE -> hardware?.design?.let { "Designing ${it.name}" to it.completion }
    Lane.AI -> ai?.training?.let { "Training ${it.codename}" to it.completion }
    Lane.OS -> os?.project?.let { "Building ${it.codename}" to it.completion }
}

@Composable
private fun LanesNow(store: GameStore, state: GameState, lanes: List<Lane>) {
    val colors = gameColors
    GroupedSection(if (Lane.GAMES in state.lanes) "Other businesses" else "Your business") {
        lanes.forEachIndexed { i, lane ->
            if (i > 0) RowDivider(68.dp)
            val profit = state.trailingProfit(lane, 4) / 4
            ListRow(
                lane.businessName,
                subtitle = state.statusLine(lane),
                leading = { LaneBadge(lane, 40.dp) },
                value = (state.trailingRevenue(lane, 4) / 4).perWeek(),
                valueColor = MaterialTheme.colorScheme.onSurface,
                valueDetail = "${Format.signedMoney(profit)} profit",
                valueDetailColor = if (profit >= 0) colors.good else colors.bad,
                chevron = true,
                onClick = { store.openLane(lane) },
            )
            state.inProgress(lane)?.let { (what, done) ->
                Column(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 14.dp)) {
                    Meter(what, done, colors.lane(lane), wavy = store.playing)
                }
            }
        }
    }
}

// Shortcuts

@Composable
private fun Shortcuts(store: GameStore, state: GameState) {
    val colors = gameColors
    val ready = Research.entries.count { it !in state.researched && state.researchBlocker(it) == null }
    val canExpand = state.pivot != null && state.lanes.size < 3 && state.expansionBlocker == null
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Shortcut(
            Icons.Filled.Science, "Research",
            if (ready > 0) "$ready ready" else "${Format.count(state.researchPoints)} RP",
            colors.milestone, Modifier.weight(1f), highlight = ready > 0,
        ) { store.tab = Tab.RESEARCH }
        Shortcut(
            Icons.Filled.Groups, "Hire",
            if (state.freeDesks > 0) "${state.candidates.size} candidates" else "No free desks",
            colors.lane(Lane.APPS), Modifier.weight(1f),
        ) { store.tab = Tab.TEAM }
        Shortcut(
            Icons.Filled.RocketLaunch, if (canExpand) "Expand" else "Money",
            if (canExpand) "New lane ready" else state.venture.board?.let { "Board ${Format.percent(it.confidence)}" } ?: "You own ${Format.percent(state.venture.founderEquity)}",
            colors.lane(Lane.SOCIAL), Modifier.weight(1f), highlight = canExpand,
        ) {
            store.companyPage = 1
            store.tab = Tab.COMPANY
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, detail: String, tint: Color, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    val colors = gameColors
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (highlight) tint.copy(alpha = 0.16f) else colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(40.dp).background(tint.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(
            detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}
