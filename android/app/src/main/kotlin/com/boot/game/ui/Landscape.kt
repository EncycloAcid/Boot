package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.gameForecast
import com.boot.core.researchPerWeek
import com.boot.game.GameStore
import com.boot.game.ScreenLayout
import com.boot.game.Speed
import com.boot.game.StudioPage
import com.boot.game.Tab
import java.util.Locale

/*
 * Sideways on a phone. The screen is wide and short, so nothing floats over anything else: a top
 * bar with who and when, the resources, the money and the clock; a rail of tabs down the left; the
 * office in the middle; and, when a tab is open, a full-height window on the right. Opening a
 * window shrinks the office rather than covering it, so the room and its people stay in view.
 */

/** Everything in the game, held sideways. */
@Composable
internal fun LandscapeGameScreen(store: GameStore, state: GameState) {
    val colors = gameColors
    val snackbar = remember { SnackbarHostState() }
    var confirmAbandon by remember { mutableStateOf(false) }
    val needsToday = state.tutorial != null || state.inbox.isNotEmpty() ||
        nextStep(state)?.let { it.key != store.dismissedStep && (state.tutorial == null || it.key in urgentSteps) } == true

    // Lessons, inbox items and urgent tips open Today by themselves, once a week at most.
    LaunchedEffect(state.week, needsToday) {
        if (needsToday && store.tab == Tab.OFFICE) store.todayOpen = true
    }
    LaunchedEffect(store.error) {
        store.error?.let {
            snackbar.showSnackbar(it)
            store.error = null
        }
    }
    LaunchedEffect(store.notice) {
        store.notice?.let {
            snackbar.showSnackbar(it)
            store.notice = null
        }
    }
    LaunchedEffect(store.playing, store.speed) {
        if (!store.playing) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (store.playing) {
            val now = withFrameMillis { it }
            if (now - last >= store.speed.millis) {
                last = now
                store.tick()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { store.pause() }

    val windowOpen = store.tab != Tab.OFFICE || store.todayOpen
    BackHandler(enabled = windowOpen) {
        if (store.tab != Tab.OFFICE) store.tab = Tab.OFFICE else store.todayOpen = false
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(colors.grouped)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.displayCutout)),
        ) {
            val windowWidth = (maxWidth * 0.46f).coerceIn(340.dp, 560.dp)
            Column(Modifier.fillMaxSize()) {
                TopBar(store, state, onAbandon = { confirmAbandon = true })
                WeekProgress(store, state)
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Rail(store, state)
                    VerticalDivider(color = colors.separator)
                    // The office takes whatever's left, and recentres when a window opens.
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Stage(store, state)
                        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
                    }
                    AnimatedVisibility(
                        visible = windowOpen,
                        enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
                    ) {
                        Row(Modifier.fillMaxHeight()) {
                            VerticalDivider(color = colors.separator)
                            SideWindow(store, state, Modifier.width(windowWidth).fillMaxHeight())
                        }
                    }
                }
            }
        }
    }

    GameOverlays(store, state)

    if (confirmAbandon) {
        ConfirmDialog(
            title = "Abandon ${state.companyName}?",
            text = "This run ends for good and you start over in the apartment.",
            confirm = "Abandon",
            destructive = true,
            onConfirm = { store.abandon() },
            onDismiss = { confirmAbandon = false },
        )
    }
}

/**
 * One line across the top: the menu, who and when, the resources (design, tech and bugs while a
 * game is in development, research points always), the money, and the clock.
 */
@Composable
private fun TopBar(store: GameStore, state: GameState, onAbandon: () -> Unit) {
    val colors = gameColors
    var menuOpen by remember { mutableStateOf(false) }
    val project = state.games?.project ?: state.games?.project2
    val weekly = state.trailingProfit(weeks = 4) / 4
    val unread = state.news.count { it.id > store.newsSeen }
    val attention = unread + state.inbox.size + if (state.tutorial != null) 1 else 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.bar)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            BadgedBox(badge = { if (attention > 0) Badge() }) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true }
                        .semantics { contentDescription = if (attention > 0) "Menu, $attention waiting" else "Menu" },
                    contentAlignment = Alignment.Center,
                ) { HudIcon(HudGlyph.MENU, MaterialTheme.colorScheme.onSurface, 18.dp) }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (state.inbox.isNotEmpty()) "Today · ${state.inbox.size} inbox" else "Today") },
                    onClick = {
                        menuOpen = false
                        store.tab = Tab.OFFICE
                        store.todayOpen = true
                    },
                )
                DropdownMenuItem(text = { Text(if (unread > 0) "News · $unread new" else "News") }, onClick = {
                    menuOpen = false
                    store.tab = Tab.NEWS
                })
                DropdownMenuItem(text = { Text("How to play") }, onClick = {
                    menuOpen = false
                    store.guideOpen = true
                })
                DropdownMenuItem(text = { Text("Play upright (classic)") }, onClick = {
                    menuOpen = false
                    store.chooseLayout(ScreenLayout.VERTICAL)
                })
                DropdownMenuItem(text = { Text("Abandon run and start over") }, onClick = {
                    menuOpen = false
                    store.pause()
                    onAbandon()
                })
            }
        }
        // Who and when.
        Column(Modifier.weight(1f).clickable { store.companyPage = 0; store.tab = Tab.COMPANY }) {
            Text(
                state.companyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${state.dateLabel} · ${seasonTitle(state.week)} · ${state.world.era.title.removePrefix("The ")}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // Resources.
        if (project != null) {
            BarStat(HudGlyph.DESIGN, Color(0xFFF08A24), "${project.design.toInt()}", "Design points") { store.openStudio(StudioPage.MAKE) }
            BarStat(HudGlyph.TECH, Color(0xFFE8B923), "${project.tech.toInt()}", "Tech points") { store.openStudio(StudioPage.MAKE) }
            BarStat(HudGlyph.BUGS, colors.bad, "${project.bugs.toInt()}", "Bugs") { store.openStudio(StudioPage.MAKE) }
        }
        BarStat(
            HudGlyph.RESEARCH, colors.milestone, "${state.researchPoints.toInt()}",
            "Research points, +${String.format(Locale.US, "%.1f", state.researchPerWeek)} a week",
        ) { store.tab = Tab.RESEARCH }
        BarDivider()
        // Money and standing.
        Column(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { store.companyPage = 1; store.tab = Tab.COMPANY }
                .semantics { contentDescription = "Cash ${Format.money(state.cash)}" }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                Format.money(state.cash), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = if (state.cash < 0) colors.bad else colors.good, maxLines = 1,
            )
            Text(
                "${Format.signedMoney(weekly)}/wk", style = MaterialTheme.typography.labelSmall,
                color = if (weekly >= 0) colors.good else colors.bad, maxLines = 1,
            )
        }
        val fans = state.games?.fans
        if (fans != null && Lane.GAMES in state.lanes) {
            BarStat(HudGlyph.FANS, Color(0xFFC44D8A), Format.count(fans), "Fans") { store.openStudio(StudioPage.GAMES) }
        } else {
            BarStat(HudGlyph.FANS, colors.warn, "${state.reputation.score.toInt()}", "Reputation") { store.companyPage = 0; store.tab = Tab.COMPANY }
        }
        BarDivider()
        // The clock.
        TinyChip(store.speed.label, description = "Speed ${store.speed.label}") {
            store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
        }
        TinyChip("Week", enabled = store.canAdvance && !store.playing && !store.holdsClock, description = "Next week") { store.advance(1) }
        TinyChip(
            if (store.playing) "Pause" else "Play",
            filled = true,
            enabled = store.canAdvance || store.playing,
            description = if (store.playing) "Pause" else "Play",
        ) { store.togglePlay() }
    }
    HorizontalDivider(color = colors.separator)
}

/** A number with its icon, compact enough for the top bar. */
@Composable
private fun BarStat(glyph: HudGlyph, tint: Color, value: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$description: $value" }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HudIcon(glyph, tint, 14.dp)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun BarDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(gameColors.separator))
}

/** The tabs, down the left edge. News has its own place here; there's room for it sideways. */
@Composable
private fun Rail(store: GameStore, state: GameState) {
    val colors = gameColors
    val unread = state.news.count { it.id > store.newsSeen }
    Column(
        Modifier
            .width(76.dp)
            .fillMaxHeight()
            .background(colors.bar)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        for (t in dockTabs) {
            val selected = if (t.tab == Tab.OFFICE) store.tab == Tab.OFFICE && store.todayOpen else store.tab == t.tab
            RailItem(t.label, selected, { HudIcon(t.glyph, it, 20.dp) }) {
                if (t.tab == Tab.OFFICE) {
                    // Office toggles the Today briefing; the office itself is always on screen.
                    store.todayOpen = !(store.tab == Tab.OFFICE && store.todayOpen)
                    store.tab = Tab.OFFICE
                } else {
                    store.todayOpen = false
                    store.tab = if (store.tab == t.tab) Tab.OFFICE else t.tab
                }
            }
        }
        RailItem("News", store.tab == Tab.NEWS, { tint ->
            BadgedBox(badge = { if (unread > 0) Badge() }) { Icon(Icons.Filled.Newspaper, null, tint = tint, modifier = Modifier.size(20.dp)) }
        }) {
            store.todayOpen = false
            store.tab = if (store.tab == Tab.NEWS) Tab.OFFICE else Tab.NEWS
        }
    }
}

@Composable
private fun RailItem(label: String, selected: Boolean, icon: @Composable (Color) -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.primary else scheme.onSurfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        icon(tint)
        FitText(label, MaterialTheme.typography.labelMedium, color = tint, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

/** The office, with the game in development and the desk count over its lower corners. */
@Composable
private fun Stage(store: GameStore, state: GameState) {
    val project = state.games?.project ?: state.games?.project2
    val team = if (state.games?.project != null) 1 else 2
    Box(Modifier.fillMaxSize()) {
        OfficeScene(state, store.playing, Modifier.fillMaxSize().clickable { store.tab = Tab.TEAM }, insetTop = 8.dp)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)) {
            if (project != null) {
                ProjectStrip(store, state, project, team)
            } else if (!store.todayOpen && store.tab == Tab.OFFICE) {
                TodayPrompt(store, state)
            }
        }
        DeskChip(state, Modifier.align(Alignment.TopEnd).padding(10.dp)) { store.tab = Tab.TEAM }
    }
}

/** The game in development, on one line along the bottom of the office: stage, title, progress, time left. */
@Composable
private fun ProjectStrip(store: GameStore, state: GameState, project: com.boot.core.GameProject, team: Int) {
    val colors = gameColors
    val stage = if (project.isReady) "Polish" else listOf("Foundations", "Content", "Presentation")[project.stage]
    val forecast = state.gameForecast(team)
    val eta = forecast?.weeksToReady
    val etaLabel = when {
        project.isReady -> forecast?.let { String.format(Locale.US, "ships ~%.1f", it.expectedScore) } ?: "ready"
        eta == null || eta <= 0 -> "ready soon"
        else -> "~${Format.weeks(eta)} left"
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.95f))
            .border(1.dp, colors.separator, shape)
            .clickable { store.openStudio(StudioPage.MAKE) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stage, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1,
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                (if (team == 2) "Team 2 · " else "") + project.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp).clip(CircleShape).background(colors.fill)) {
                Box(Modifier.fillMaxWidth(project.completion.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Format.percent(project.completion), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(etaLabel, style = MaterialTheme.typography.labelSmall, color = colors.milestone, maxLines = 1)
        }
    }
}

/** What's waiting in Today, as one tap, when no game is in development. */
@Composable
private fun TodayPrompt(store: GameStore, state: GameState) {
    val colors = gameColors
    val waiting = when {
        state.tutorial != null -> "A lesson from $MENTOR_NAME is waiting"
        state.inbox.isNotEmpty() -> "${state.inbox.size} in your inbox"
        else -> "Today: goals, the studio and the news"
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .widthIn(max = 420.dp)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.95f))
            .border(1.dp, colors.separator, shape)
            .clickable { store.todayOpen = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HudIcon(HudGlyph.OFFICE, MaterialTheme.colorScheme.primary, 16.dp)
        Text(waiting, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** The open tab, full height on the right, with its title and a close button. */
@Composable
private fun SideWindow(store: GameStore, state: GameState, modifier: Modifier) {
    val colors = gameColors
    val today = store.tab == Tab.OFFICE
    val title = when (store.tab) {
        Tab.OFFICE -> "Today"
        Tab.LANES -> "Lanes"
        Tab.RESEARCH -> "Research"
        Tab.TEAM -> "Team"
        Tab.COMPANY -> "Company"
        Tab.NEWS -> "News"
    }
    Column(modifier.background(colors.grouped)) {
        Row(
            Modifier.fillMaxWidth().background(colors.bar).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            PlainButton("Close", {
                if (today) store.todayOpen = false else store.tab = Tab.OFFICE
            })
        }
        HorizontalDivider(color = colors.separator)
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalInFloatingPanel provides true) {
                when (store.tab) {
                    Tab.OFFICE -> OfficeScreen(store, state)
                    Tab.LANES -> LanesScreen(store, state)
                    Tab.RESEARCH -> ResearchScreen(store, state)
                    Tab.TEAM -> TeamScreen(store, state)
                    Tab.COMPANY -> CompanyScreen(store, state)
                    Tab.NEWS -> NewsScreen(store, state)
                }
            }
        }
    }
}

private const val MENTOR_NAME = com.boot.core.MENTOR
