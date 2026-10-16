package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
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
import com.boot.game.audio.Sfx
import com.boot.game.ScreenLayout
import com.boot.game.Speed
import com.boot.game.StudioPage
import com.boot.game.Tab
import java.util.Locale

/*
 * Sideways on a phone, laid out the way Game Dev Tycoon uses a wide screen: the office fills it,
 * small readouts sit in the corners (who and when top left, the money top right), the game in
 * development runs across the top, and one dock along the bottom holds the tabs and the clock.
 * Tabs open as windows in the middle with the office dimmed behind; the dock stays put, so time
 * can run with a window open.
 */

/** Height of the bottom dock plus its margin, for windows to clear. */
private val DockClearance = 80.dp

/** Everything in the game, held sideways. */
@Composable
internal fun LandscapeGameScreen(store: GameStore, state: GameState) {
    val colors = gameColors
    val snackbar = remember { SnackbarHostState() }
    var confirmAbandon by remember { mutableStateOf(false) }
    val needsToday = state.tutorial != null || state.inbox.isNotEmpty() ||
        nextStep(state)?.let { it.key != store.dismissedStep && (state.tutorial == null || it.key in urgentSteps) } == true

    // A new lesson, inbox item or urgent tip opens Today by itself; quiet weeks leave the office in view.
    val todayKey = listOf(state.tutorial?.toString(), state.inbox.size, nextStep(state)?.key)
    LaunchedEffect(todayKey, needsToday) {
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
    val closeWindow = { if (store.tab != Tab.OFFICE) store.tab = Tab.OFFICE else store.todayOpen = false }
    BackHandler(enabled = windowOpen) { closeWindow() }
    val project = state.games?.project ?: state.games?.project2
    val team = if (state.games?.project != null) 1 else 2

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        BoxWithConstraints(Modifier.fillMaxSize().background(colors.grouped)) {
            // An open window gets the whole height between the top of the screen and the dock: the
            // corner cards step aside for it, and its title bar carries the cash and the date.
            val windowWidth = (maxWidth * 0.82f).coerceIn(460.dp, 820.dp)
            // The office, edge to edge, under everything. Tap someone to see their card; taps
            // anywhere else do nothing, so a stray touch never yanks you into another screen.
            OfficeScene(state, store.playing, Modifier.fillMaxSize(), insetTop = 48.dp, people = true, onManage = { store.tab = Tab.TEAM })

            val safe = WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.displayCutout)
            // Bottom left, above the dock: the desks.
            if (!windowOpen) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(safe).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    DeskChip(state, Modifier.align(Alignment.BottomStart).padding(bottom = DockClearance)) { store.tab = Tab.TEAM }
                }
            }

            // A window in the middle, with the office dimmed behind it. Tap outside to close.
            AnimatedVisibility(visible = windowOpen, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { closeWindow() },
                )
            }
            AnimatedVisibility(
                visible = windowOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.97f),
                exit = fadeOut() + scaleOut(targetScale = 0.97f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(safe)
                    .padding(top = 12.dp, bottom = DockClearance),
            ) {
                CenterWindow(store, state, Modifier.width(windowWidth).fillMaxHeight())
            }

            // One row along the top: who and when on the left, the game in development (or what's
            // waiting in Today) in the middle, research and money on the right. It steps aside for
            // an open window.
            AnimatedVisibility(visible = !windowOpen, enter = fadeIn(), exit = fadeOut()) {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(safe).padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
                    Identity(store, state, Modifier, onAbandon = { confirmAbandon = true })
                    Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.TopCenter) {
                        if (project != null) {
                            Column(
                                Modifier.widthIn(max = 400.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                ProjectStrip(store, state, project, team)
                                // The points the team is making, under the game, the way Game Dev Tycoon
                                // shows them. A window would cover them, so they step aside for one.
                                if (!windowOpen) {
                                    Floating {
                                        Row(Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            BarStat(HudGlyph.DESIGN, Color(0xFFF08A24), "${project.design.toInt()}", "Design points") { store.openStudio(StudioPage.MAKE) }
                                            BarStat(HudGlyph.TECH, Color(0xFFE8B923), "${project.tech.toInt()}", "Tech points") { store.openStudio(StudioPage.MAKE) }
                                            BarStat(HudGlyph.BUGS, colors.bad, "${project.bugs.toInt()}", "Bugs") { store.openStudio(StudioPage.MAKE) }
                                        }
                                    }
                                }
                            }
                        } else if (!windowOpen) {
                            Box(Modifier.widthIn(max = 400.dp)) { TodayPrompt(store, state) }
                        }
                    }
                    Readouts(store, state, Modifier)
                }
            }

            // The dock: tabs and the clock, always on top.
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.displayCutout))
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SnackbarHost(snackbar)
                Dock(store, state)
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

/** A soft card that floats over the office. */
@Composable
private fun Floating(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = gameColors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.95f))
            .border(1.dp, colors.separator, shape),
    ) { content() }
}

/** Top left: the menu, the company, and the date, season and era. */
@Composable
private fun Identity(store: GameStore, state: GameState, modifier: Modifier, onAbandon: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val unread = state.news.count { it.id > store.newsSeen }
    val attention = unread + state.inbox.size + if (state.tutorial != null) 1 else 0
    Floating(modifier.widthIn(max = 260.dp)) {
        Row(Modifier.padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                BadgedBox(badge = { if (attention > 0) Badge() }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { menuOpen = true }
                            .semantics { contentDescription = if (attention > 0) "Menu, $attention waiting" else "Menu" },
                        contentAlignment = Alignment.Center,
                    ) { HudIcon(HudGlyph.MENU, MaterialTheme.colorScheme.onSurface, 18.dp) }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("How to play") }, onClick = {
                        menuOpen = false
                        store.guideOpen = true
                    })
                    DropdownMenuItem(text = { Text("Sound and music") }, onClick = {
                        menuOpen = false
                        store.soundOpen = true
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
            Column(Modifier.clickable { store.companyPage = 0; store.tab = Tab.COMPANY }) {
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
        }
    }
}

/** Top right: research points, cash and the week's profit, and fans or reputation. */
@Composable
private fun Readouts(store: GameStore, state: GameState, modifier: Modifier) {
    val colors = gameColors
    val weekly = state.trailingProfit(weeks = 4) / 4
    Floating(modifier) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BarStat(
                HudGlyph.RESEARCH, colors.milestone, "${state.researchPoints.toInt()}",
                "Research points, +${String.format(Locale.US, "%.1f", state.researchPerWeek)} a week",
            ) { store.tab = Tab.RESEARCH }
            val fans = state.games?.fans
            if (fans != null && Lane.GAMES in state.lanes) {
                BarStat(HudGlyph.FANS, Color(0xFFC44D8A), Format.count(fans), "Fans") { store.openStudio(StudioPage.GAMES) }
            } else {
                BarStat(HudGlyph.FANS, colors.warn, "${state.reputation.score.toInt()}", "Reputation") { store.companyPage = 0; store.tab = Tab.COMPANY }
            }
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
        }
    }
}

/** The bottom dock: the tabs, then the clock. */
@Composable
private fun Dock(store: GameStore, state: GameState) {
    val colors = gameColors
    val unread = state.news.count { it.id > store.newsSeen }
    // Across the whole width of the screen, the way a game's bottom bar sits: the tabs share the
    // room evenly and the clock keeps to the right-hand end.
    Floating(Modifier.fillMaxWidth().widthIn(max = 1_200.dp)) {
        Column(Modifier.fillMaxWidth()) {
            WeekProgress(store, state)
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    for (t in dockTabs) {
                        val selected = if (t.tab == Tab.OFFICE) store.tab == Tab.OFFICE && store.todayOpen else store.tab == t.tab
                        DockItem(if (t.tab == Tab.OFFICE) "Today" else t.label, selected, { HudIcon(t.glyph, it, 18.dp) }, Modifier.weight(1f)) {
                            if (t.tab == Tab.OFFICE) {
                                // The office is always on screen; this opens the Today briefing over it.
                                store.todayOpen = !(store.tab == Tab.OFFICE && store.todayOpen)
                                store.tab = Tab.OFFICE
                            } else {
                                store.todayOpen = false
                                store.tab = if (store.tab == t.tab) Tab.OFFICE else t.tab
                            }
                        }
                    }
                    DockItem("News", store.tab == Tab.NEWS, { tint ->
                        BadgedBox(badge = { if (unread > 0) Badge() }) { Icon(Icons.Filled.Newspaper, null, tint = tint, modifier = Modifier.size(18.dp)) }
                    }, Modifier.weight(1f)) {
                        store.todayOpen = false
                        store.tab = if (store.tab == Tab.NEWS) Tab.OFFICE else Tab.NEWS
                    }
                }
                Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(28.dp).background(colors.separator))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
            }
        }
    }
}

@Composable
private fun DockItem(label: String, selected: Boolean, icon: @Composable (Color) -> Unit, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val sound = LocalSound.current
    val tint = if (selected) scheme.primary else scheme.onSurfaceVariant
    // Each tab gets an even share of the bar; the highlight stays a pill in the middle of it.
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(min = 64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) scheme.primaryContainer else Color.Transparent)
                .selectable(selected = selected, onClick = { sound.play(Sfx.CLICK); onClick() }, role = Role.Tab)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            icon(tint)
            Text(
                label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
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

/** The game in development across the top of the office: stage, title, progress, time left. */
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

/** The open tab as a window in the middle of the screen, with its title and a close button. */
@Composable
private fun CenterWindow(store: GameStore, state: GameState, modifier: Modifier) {
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
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .shadow(16.dp, shape, clip = false)
            .clip(shape)
            .background(colors.grouped)
            .border(1.dp, colors.separator, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().background(colors.bar).padding(start = 14.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            // The corner cards are hidden behind a window, so the money and the date ride along here.
            Text(
                state.dateLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                Format.money(state.cash), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = if (state.cash < 0) colors.bad else colors.good,
                modifier = Modifier.padding(end = 4.dp),
            )
            IconButton(onClick = { if (today) store.todayOpen = false else store.tab = Tab.OFFICE }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
            }
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
