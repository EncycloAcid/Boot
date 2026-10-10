package com.boot.game.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.boot.core.Campaign
import com.boot.core.calendarYear
import com.boot.core.timelineWeek
import com.boot.core.FocusArea
import com.boot.core.Fit
import com.boot.core.Format
import com.boot.core.GameProject
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.Topic
import com.boot.core.acceptDeal
import com.boot.core.buildEngine
import com.boot.core.setEngineLicensed
import com.boot.core.buyCampaign
import com.boot.core.cancelGame
import com.boot.core.clashFor
import com.boot.core.engineQuote
import com.boot.core.launchGame
import com.boot.core.platformPlayers
import com.boot.core.platformsOnSale
import com.boot.core.sequelConcept
import com.boot.core.setStageFocus
import com.boot.core.gameTeam
import com.boot.core.teamSize
import com.boot.core.setTeamSplit
import com.boot.core.takeGameContract
import com.boot.game.GameStore
import com.boot.game.StudioPage
import java.util.Locale

/** The game studio, Game Dev Tycoon style. */
fun LazyListScope.studioItems(store: GameStore, state: GameState) {
    val g = state.games ?: return
    item {
        Segmented(StudioPage.entries, store.studioPage, { page ->
            // A count where something is waiting for you.
            val waiting = when (page) {
                StudioPage.MAKE -> g.contractOffers.size + g.dealOffers.size
                StudioPage.MARKET -> g.pitches.size
                else -> 0
            }
            if (waiting > 0) "${page.title} · $waiting" else page.title
        }) { store.studioPage = it }
    }
    when (store.studioPage) {
        StudioPage.MAKE -> makeItems(store, state, g)
        StudioPage.GAMES -> {
            item { StudioOverview(state, g) }
            if (g.franchises.isNotEmpty()) item { FranchisesSection(store, state, g) }
            if (g.releases.any { it.live?.closed == false }) item { LiveGamesSection(store, state, g) }
            if (Research.ESPORTS in state.researched) item { EsportsSection(store, state, g) }
            item { ReleasesSection(store, g) }
            if (g.market.awards.isNotEmpty()) item { AwardsCard(state) }
        }
        StudioPage.MARKET -> {
            item { ChartCard(state, limit = 10) { store.openRelease = it } }
            item { ComingSoonCard(state, limit = 8) }
            if (Research.PUBLISHING in state.researched) item { LabelSection(store, state, g) }
            item { StudiosSection(store, state, g) }
        }
        StudioPage.TECH -> {
            item { EnginesSection(store, state, g) }
            item { PlatformsSection(state) }
            item { NotebookSection(g) }
        }
    }
}

/** What the teams are making now, and what they could start. */
private fun LazyListScope.makeItems(store: GameStore, state: GameState, g: GamesStudio) {
    val p = g.project
    val c = g.contract
    val x = g.expansion
    when {
        p != null -> {
            item { DevelopmentSection(store, state, p) }
            if (!p.isReady) item { FocusSection(store, g, p) }
            item { HypeSection(store, state, p) }
        }
        x != null -> item {
            GroupedSection("Expansion pack", footer = "When it's done, owners of the original come back and buy it. Sales of the original pick up too.") {
                CardBlock {
                    Text(x.title, style = MaterialTheme.typography.titleLargeEmphasized)
                    Meter("Progress", x.completion, gameColors.milestone, wavy = true, caption = Format.weeks(x.weeks) + " in")
                }
            }
        }
        c != null -> item {
            GroupedSection("Contract work", footer = "Deliver on time and you get paid plus a few research points. Miss it and they want a refund.") {
                CardBlock {
                    Text(c.title, style = MaterialTheme.typography.titleLargeEmphasized)
                    Meter("Progress", c.completion, wavy = true, caption = "week ${c.weeksElapsed} of ${c.weeksAllowed}")
                    ValueRow("Pays", Format.money(c.pay))
                }
            }
        }
        else -> {
            item {
                GroupedSection(null) {
                    CardBlock {
                        Text("What's next?", style = MaterialTheme.typography.titleLargeEmphasized)
                        Hint("Pick a topic, genre, platform and size, then split the team's effort across three stages of development.")
                        PrimaryButton("Design a new game", { store.designGame() }, Modifier.fillMaxWidth())
                    }
                }
            }
            if (g.contractOffers.isNotEmpty()) item { ContractOffers(store, g) }
            if (g.dealOffers.isNotEmpty()) item { DealOffers(store, g) }
        }
    }
    if (Research.SECOND_TEAM in state.researched) secondTeamItems(store, state, g)
    if (Research.REMASTERS in state.researched) item { RemastersSection(store, state, g) }
}

@Composable
private fun StudioOverview(state: GameState, g: GamesStudio) {
    GroupedSection(
        "Studio",
        footer = g.trend?.let { "Trend: gamers can't get enough of ${it.title.lowercase()} games right now." },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            TileRow {
                StatTile("Fans", Format.count(g.fans), Modifier.weight(1f), "trust ${String.format(Locale.US, "%.2f", g.fanTrust)}")
                StatTile("Best review", if (g.topScore > 0) String.format(Locale.US, "%.1f", g.topScore) else "—", Modifier.weight(1f), "${g.releases.size} games")
                StatTile("Research", "${state.researchPoints.toInt()}", Modifier.weight(1f), "RP to spend")
            }
        }
        if (g.lastRoyalties > 0 || g.lastMerch > 0) {
            RowDivider()
            if (g.lastRoyalties > 0) ValueRow("Engine royalties", g.lastRoyalties.perWeek(), gameColors.good)
            if (g.lastRoyalties > 0 && g.lastMerch > 0) RowDivider()
            if (g.lastMerch > 0) ValueRow("Merchandise", g.lastMerch.perWeek(), gameColors.good)
        }
    }
}

// Development

/** The second team: its game, or an invitation to start one, and how the staff split. */
private fun LazyListScope.secondTeamItems(store: GameStore, state: GameState, g: GamesStudio) {
    val p2 = g.project2
    if (p2 != null) {
        item { DevelopmentSection(store, state, p2, team = 2) }
        if (!p2.isReady) item { FocusSection(store, g, p2, team = 2) }
        item { HypeSection(store, state, p2, team = 2) }
    } else {
        item {
            GroupedSection("Team 2") {
                CardBlock {
                    Text("Team 2 is free", style = MaterialTheme.typography.titleLargeEmphasized)
                    Hint(
                        "A second game alongside the first, by ${state.teamSize(2)} of your ${state.gameTeam} game staff. " +
                            "Small, medium and large games; AAA needs the main team.",
                    )
                    PrimaryButton("Design a game for Team 2", { store.designGame(team = 2) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
    item {
        GroupedSection(
            "Teams",
            footer = "While both teams are busy, the game staff split like this. A team working alone gets everyone. Bigger games need bigger teams.",
        ) {
            CardBlock {
                CommitSlider(g.team2Share, { v ->
                    val second = kotlin.math.round(state.gameTeam * v.toDouble().coerceIn(0.2, 0.8)).toInt()
                    "Team 1: ${state.gameTeam - second} people · Team 2: $second people"
                }) { v -> store.perform { setTeamSplit(v) } }
            }
        }
    }
}

@Composable
private fun DevelopmentSection(store: GameStore, state: GameState, p: GameProject, team: Int = 1) {
    val colors = gameColors
    val c = p.concept
    val stageLabels = listOf("Foundations", "Content", "Presentation", "Polish")
    val current = if (p.isReady) 3 else p.stage
    GroupedSection(
        (if (team == 2) "Team 2 · " else if (state.games?.project2 != null) "Team 1 · " else "") +
            if (p.deal != null) "In development for ${p.deal!!.publisher}" else "In development",
        footer = if (p.isReady) "Content complete. Every week now fixes bugs and adds polish. Ship when you're happy." else null,
    ) {
        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GameCover(c, 64.dp, studio = state.companyName)
                Column {
                    Text(p.title, style = MaterialTheme.typography.titleLargeEmphasized)
                    Hint(
                        "${c.size.title} ${c.topic.title.lowercase()} ${c.genreLower} · ${c.platforms.joinToString { it.title }} · " +
                            "for ${c.audience.phrase} · ${Format.weeks(p.weeksInDev)} in",
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                stageLabels.forEachIndexed { i, label ->
                    val on = i == current
                    val done = i < current
                    FitText(
                        label,
                        MaterialTheme.typography.labelMedium,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            on -> MaterialTheme.colorScheme.onPrimary
                            done -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (on) MaterialTheme.colorScheme.primary else colors.fill)
                            .padding(vertical = 6.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (p.isReady) {
                val g = state.games!!
                val clash = g.clashFor(c, state.week)
                val giant = g.market.upcoming.firstOrNull { it.mega && it.week - state.week in 0..2 }
                val warning = when {
                    clash?.mega == true -> "${clash.game.title} just came out and everyone's playing it. Launching now costs you players."
                    clash != null -> "${clash.game.title} is a fresh ${clash.game.genre.lower} hit. Launching now means sharing its audience."
                    giant != null -> "${giant.title} lands ${if (giant.week == state.week) "this week" else "in ${Format.weeks(giant.week - state.week)}"}. Release before it hits, or wait it out."
                    else -> null
                }
                warning?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.warn) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton("Release ${p.title}", { store.perform { launchGame(team) } }, Modifier.weight(1f), enabled = p.isReady)
                if (p.deal == null) {
                    var confirming by remember { mutableStateOf(false) }
                    PlainButton("Cancel", { confirming = true }, color = colors.bad)
                    if (confirming) {
                        ConfirmDialog(
                            title = "Cancel ${p.title}?",
                            text = "Everything spent on it so far (${Format.money(p.spent)}) is gone, and the team starts over.",
                            confirm = "Cancel the game",
                            destructive = true,
                            onConfirm = { store.perform { cancelGame(team) } },
                            onDismiss = { confirming = false },
                        )
                    }
                }
            }
            Meter("Development", p.completion, wavy = !p.isReady)
            TileRow {
                PointTile("Design", p.design, p.lastDesign, colors.design, state.week, Modifier.weight(1f))
                PointTile("Tech", p.tech, p.lastTech, colors.tech, state.week, Modifier.weight(1f))
                PointTile("Bugs", p.bugs, p.lastBugs, colors.bad, state.week, Modifier.weight(1f))
            }
            if (p.isReady) Meter("Polish", p.polish / (p.work * 0.4), colors.milestone)
        }
    }
}

/** A running total with a bubble that floats up each week showing what was just added, like the points popping off a dev's desk. */
@Composable
private fun PointTile(label: String, value: Double, delta: Double, color: Color, week: Int, modifier: Modifier) {
    val rise = remember(week) { Animatable(0f) }
    LaunchedEffect(week) { rise.animateTo(1f, tween(1_100)) }
    Box(modifier) {
        StatTile(label, Format.count(value), Modifier.fillMaxWidth(), tint = color)
        if (kotlin.math.abs(delta) >= 0.5) {
            Text(
                (if (delta > 0) "+" else "−") + Format.count(kotlin.math.abs(delta)),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset((-6).dp.roundToPx(), (2 - 18 * rise.value).dp.roundToPx()) }
                    .graphicsLayer { alpha = 1f - rise.value * 0.85f }
                    .background(color, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun FocusSection(store: GameStore, g: GamesStudio, p: GameProject, team: Int = 1) {
    val stage = p.stage
    val areas = FocusArea.forStage(stage)
    val alloc = p.focus[stage]
    GroupedSection(
        (if (team == 2) "Team 2 · " else "") + "Focus · ${FocusArea.STAGES[stage]}",
        footer = "Split this stage's effort. Each genre cares about different things; game reports teach you which.",
    ) {
        CardBlock {
            areas.forEachIndexed { i, area ->
                FocusSlider(area, alloc, i, insight(g, p.concept.genre, area)) { v ->
                    val next = alloc.toMutableList().also { it[i] = v }
                    store.perform { setStageFocus(stage, next, team) }
                }
            }
        }
    }
}

/** What you've learned about how much an area matters to a genre, if anything. */
fun insight(g: GamesStudio, genre: Genre, area: FocusArea): String? {
    if (!g.knowsWeight(genre, area)) return null
    return when (genre.weight(area)) {
        2 -> "matters a lot"
        1 -> "matters some"
        else -> "barely matters"
    }
}

@Composable
fun FocusSlider(area: FocusArea, alloc: List<Double>, index: Int, insight: String?, onCommit: (Double) -> Unit) {
    val total = alloc.sum().takeIf { it > 0 } ?: 1.0
    CommitSlider(alloc[index], { v ->
        val share = v / (total - alloc[index] + v).coerceAtLeast(0.01)
        "${area.title} · ${Format.percent(share)}" + (insight?.let { "  ·  $it" } ?: "")
    }, onCommit)
}

@Composable
private fun HypeSection(store: GameStore, state: GameState, p: GameProject, team: Int = 1) {
    val colors = gameColors
    val marketing = Research.MARKETING in state.researched
    GroupedSection(if (team == 2) "Team 2 · Hype" else "Hype", footer = if (!marketing) "Research marketing campaigns to buy hype. Summer Game Fest each June helps too." else "Hype sells launch weeks. Overhype a weak game and fans feel misled.") {
        CardBlock { Meter("Hype", p.hype / 100, colors.warn, "${p.hype.toInt()}/100") }
        if (marketing) {
            Campaign.entries.forEach { c ->
                RowDivider()
                val done = c in p.campaigns
                ActionRow(
                    c.title, "${Format.money(c.cost)} · +${c.hype.toInt()} hype",
                    if (done) "Done" else "Run", enabled = !done && state.cash >= c.cost,
                ) { store.perform { buyCampaign(c, team) } }
            }
        }
    }
}

// Between games

@Composable
private fun ContractOffers(store: GameStore, g: GamesStudio) {
    GroupedSection("Contract work", footer = "Small paid jobs for other companies. They keep the lights on and earn a few research points.") {
        g.contractOffers.forEachIndexed { i, c ->
            if (i > 0) RowDivider()
            ActionRow(c.title, "Due in ${c.weeksAllowed} weeks · ${Format.count(c.work)} work · +${c.rp.toInt()} RP", Format.money(c.pay)) {
                store.perform { takeGameContract(c.id) }
            }
        }
    }
}

@Composable
private fun DealOffers(store: GameStore, g: GamesStudio) {
    GroupedSection("Publisher deals", footer = "They pay half the advance now and pay the licence. Hit the score for the rest. You keep 35% of sales.") {
        g.dealOffers.forEachIndexed { i, d ->
            if (i > 0) RowDivider()
            ActionRow(
                "${d.publisher}: ${d.topic.title} ${d.genre.title}",
                "${d.size.title} for ${d.platform.title} · ${Format.money(d.advance)} advance · needs ${d.minScore}/10",
                "Sign",
            ) { store.perform { acceptDeal(d.id) } }
        }
    }
}

// Catalogue, engines, platforms and what you've learned

@Composable
private fun FranchisesSection(store: GameStore, state: GameState, g: GamesStudio) {
    val canSequel = Research.SEQUELS in state.researched && !g.busy
    GroupedSection(
        "Your franchises",
        footer = "A series that averages 7 or better sells more with every entry. Let it rest between games, and don't let quality slip.",
    ) {
        g.franchises.forEachIndexed { i, f ->
            if (i > 0) RowDivider(78.dp)
            val rest = state.week - f.latest.week
            ListRow(
                f.name,
                subtitle = "${f.entries.size} games · " + if (rest == 0) "new this week" else "last out ${Format.weeks(rest)} ago",
                leading = { CoverWithScore(f.latest.concept, f.averageScore) },
                value = Format.money(f.lifetime),
                valueDetail = "lifetime",
                onClick = { store.openRelease = f.latest.id },
                trailing = if (canSequel) {
                    { PlainButton("Next", { store.designGame(state.sequelConcept(f.latest.id)) }) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ReleasesSection(store: GameStore, g: GamesStudio) {
    GroupedSection("Released games") {
        if (g.releases.isEmpty()) CardBlock { Hint("Nothing shipped yet. Your first game is waiting.") }
        g.releases.take(12).forEachIndexed { i, r ->
            if (i > 0) RowDivider(78.dp)
            ListRow(
                r.title,
                subtitle = "${r.verdict} · ${r.concept.topic.title} ${r.concept.genreTitle}" +
                    (if (r.chartRank > 0) " · #${r.chartRank} this week" else "") +
                    (if (r.expansions.isNotEmpty()) " · ${r.expansions.size} expansion${if (r.expansions.size > 1) "s" else ""}" else "") +
                    (if (r.patched) " · patched" else "") +
                    (r.live?.let { if (it.closed) " · servers off" else " · live, ${Format.count(it.players)} players" } ?: ""),
                leading = { CoverWithScore(r.concept, r.score) },
                value = if (r.onSale) (r.weeklySales * r.netShare).perWeek() else Format.money(r.lifetimeSales),
                valueDetail = if (r.onSale) "on sale" else "lifetime",
                chevron = true,
                onClick = { store.openRelease = r.id },
            )
        }
    }
}

/** A review score on a Material expressive shape, colored by how it landed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScoreBadge(score: Double, size: Dp) {
    val colors = gameColors
    val color = when {
        score >= 8 -> colors.good
        score >= 6 -> MaterialTheme.colorScheme.primary
        score >= 5 -> colors.warn
        else -> colors.bad
    }
    Box(
        Modifier
            .size(size)
            .clip((if (score >= 8) MaterialShapes.Cookie9Sided else MaterialShapes.Circle).toShape())
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            String.format(Locale.US, if (score >= 9.95 || score % 1.0 == 0.0) "%.0f" else "%.1f", score),
            style = if (size >= 56.dp) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.titleSmall,
            color = color,
        )
    }
}

@Composable
private fun EnginesSection(store: GameStore, state: GameState, g: GamesStudio) {
    val canBuild = Research.CUSTOM_ENGINES in state.researched
    val (features, cost, weeks) = state.engineQuote()
    val level = features.sumOf { it.engineLevel }
    val best = g.engines.maxOf { it.level }
    GroupedSection(
        "Engines",
        footer = when {
            !canBuild -> "Research custom engines, then engine features, to build better tech. Games on dated engines look it."
            else -> "Reviewers expect about level ${state.week / 20} by now."
        },
    ) {
        g.engines.sortedByDescending { it.level }.forEachIndexed { i, e ->
            if (i > 0) RowDivider()
            ListRow(e.name, subtitle = if (e.features.isEmpty()) "No features" else e.features.joinToString { it.title }, value = "level ${e.level}")
            if (e.level > 0) {
                ToggleRow(
                    "License to other studios", e.licensed,
                    "About ${e.royalty(state.week).perWeek()} in royalties. Rival games get a little better on your tech.",
                ) { on -> store.perform { setEngineLicensed(e.id, on) } }
            }
        }
        g.engineBuild?.let { b ->
            RowDivider()
            CardBlock { Meter("Building ${b.name}", 0.5, wavy = true, caption = "${Format.weeks(b.weeksLeft)} left") }
        }
        if (canBuild && g.engineBuild == null) {
            RowDivider()
            ActionRow(
                "New engine: level $level",
                "${features.size} features · ${Format.money(cost)} · $weeks weeks",
                "Build",
                enabled = level > best && state.cash >= cost,
            ) { store.perform { buildEngine() } }
        }
    }
}

@Composable
private fun PlatformsSection(state: GameState) {
    val onSale = state.platformsOnSale
    val total = onSale.sumOf { state.platformPlayers(it) * it.spend }.coerceAtLeast(1.0)
    val now = state.timelineWeek
    val upcoming = Platform.entries.filter { it.launch > now && it.launch <= now + 78 && it != Platform.OWN_CONSOLE }
    GroupedSection(
        "Platforms",
        footer = upcoming.takeIf { it.isNotEmpty() }?.let { list -> "Coming soon: " + list.joinToString { it.title } + "." },
    ) {
        onSale.sortedByDescending { state.platformPlayers(it) * it.spend }.forEachIndexed { i, p ->
            if (i > 0) RowDivider()
            val players = state.platformPlayers(p)
            val leaving = p.retire - now in 1..104
            ListRow(
                p.title,
                subtitle = "${p.makerIn(state.calendarYear)} · ${String.format(Locale.US, "%.0f", players)}M players" +
                    (if (p.fee > 0) " · ${Format.money(p.fee)} licence" else " · no licence fee") +
                    (if (leaving) " · being retired" else ""),
                value = Format.percent(players * p.spend / total),
                valueDetail = "of sales",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotebookSection(g: GamesStudio) {
    val colors = gameColors
    val combos = g.knownFits.filterKeys { it.startsWith("TG:") }
    GroupedSection(
        "Design notebook",
        footer = "Every game report adds what you learned about topics, genres, audiences and platforms. ${g.insights.size} focus lessons so far.",
    ) {
        if (combos.isEmpty()) {
            CardBlock { Hint("Empty. Ship a game and read its report.") }
        } else {
            CardBlock(spacing = 8.dp) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    combos.entries.sortedBy { it.key }.forEach { (key, fit) ->
                        val (_, topic, genre) = key.split(":")
                        Pill(
                            "${Topic.valueOf(topic).title} + ${Genre.valueOf(genre).title}",
                            fitColor(fit, colors),
                        )
                    }
                }
            }
        }
    }
}

fun fitColor(fit: Fit?, colors: GameColors): Color = when (fit) {
    Fit.GREAT -> colors.good
    Fit.GOOD -> colors.lane(Lane.WEBSITES)
    Fit.OKAY -> colors.warn
    Fit.BAD -> colors.bad
    null -> colors.other
}
