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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Campaign
import com.boot.core.Format
import com.boot.core.GameProject
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Research
import com.boot.core.buyCampaign
import com.boot.core.expansionBlocker
import com.boot.core.freeDesks
import com.boot.core.launchGame
import com.boot.core.researchBlocker
import com.boot.game.GameStore
import com.boot.game.Tab

/**
 * The home screen: your office with everyone at their desks, what each business is doing right
 * now, shortcuts to what's waiting for you, and the latest news. Everything important is one tap
 * from here.
 */
@Composable
fun OfficeScreen(store: GameStore, state: GameState) {
    ScreenColumn {
        item { OfficeCard(store, state) }
        if (Lane.GAMES in state.lanes) item { StudioNow(store, state) }
        val others = state.lanes.filter { it != Lane.GAMES }
        if (others.isNotEmpty()) item { LanesNow(store, state, others) }
        item {
            GoalsCard(state) {
                store.companyPage = 3
                store.tab = Tab.COMPANY
            }
        }
        if (state.achievements.isNotEmpty()) {
            item {
                RecentAchievements(state) {
                    store.companyPage = 3
                    store.tab = Tab.COMPANY
                }
            }
        }
        if (state.games != null) {
            item { ChartCard(state, limit = 5, action = { PlainButton("Full chart", { store.openLane(Lane.GAMES) }) }) { store.openRelease = it } }
            item { ComingSoonCard(state, limit = 3) }
        }
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

@Composable
private fun OfficeCard(store: GameStore, state: GameState) {
    val colors = gameColors
    Surface(shape = RoundedCornerShape(28.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
        Column {
            val crowded = state.staff.size > state.office.desks
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable { store.tab = Tab.TEAM },
            ) {
                OfficeScene(state, store.playing, Modifier.fillMaxSize(), insetTop = 40.dp)
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

/** A frosted label floating over the office. */
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
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
    }
}

/** The newest headline, sliding in as it happens. */
@Composable
private fun Ticker(store: GameStore, state: GameState) {
    val colors = gameColors
    val motion = MaterialTheme.motionScheme
    val latest = state.log.lastOrNull()
    val hint = if (state.week < 2 && !store.playing) "Tap ▶ to let time run. It stops by itself when you're needed." else null
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { store.tab = Tab.NEWS }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
    GroupedSection("Game studio", action = { PlainButton("Studio", { store.openLane(Lane.GAMES) }) }) {
        when {
            p != null -> ProjectCard(store, state, p)
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
                        "${g.releases.size} games shipped, best review ${String.format(java.util.Locale.US, "%.1f", g.topScore)}. What's next?"
                    },
                )
                PrimaryButton("Develop a new game", { store.designGame() }, Modifier.fillMaxWidth())
                val offers = g.contractOffers.size + g.dealOffers.size
                if (offers > 0) {
                    TonalButton("$offers contract${if (offers == 1) "" else "s"} and deals on offer", { store.openLane(Lane.GAMES) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(store: GameStore, state: GameState, p: GameProject) {
    val colors = gameColors
    val c = p.concept
    val stage = if (p.isReady) "Polish" else listOf("Foundations", "Content", "Presentation")[p.stage]
    CardBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameCover(c, 48.dp, studio = state.companyName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.title, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Hint("${c.size.title} ${c.topic.title.lowercase()} ${c.genreLower} · ${c.platforms.joinToString { it.title }}")
            }
            Text(
                stage,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (p.isReady) {
            Meter("Polish", p.polish / (p.work * 0.4), colors.milestone, caption = "Content complete. Bugs get fixed while you polish.")
        } else {
            Meter("Development", p.completion, wavy = store.playing, caption = Format.weeks(p.weeksInDev) + " in")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat("Design", Format.count(p.design), colors.design, Modifier.weight(1f))
            MiniStat("Tech", Format.count(p.tech), colors.tech, Modifier.weight(1f))
            MiniStat("Bugs", Format.count(p.bugs), colors.bad, Modifier.weight(1f))
            MiniStat("Hype", "${p.hype.toInt()}", colors.warn, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Release", { store.perform { launchGame() } }, Modifier.weight(1f), enabled = p.isReady)
            if (Research.MARKETING in state.researched) {
                val open = Campaign.entries.filter { it !in p.campaigns }
                MenuButton("Market", open, { "${it.title} · ${Format.money(it.cost)}" }, enabled = open.isNotEmpty()) { camp ->
                    store.perform { buyCampaign(camp) }
                }
            }
            if (!p.isReady) TonalButton("Focus", { store.openLane(Lane.GAMES) })
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
            if (ready > 0) "$ready ready" else "${state.researchPoints.toInt()} RP",
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
