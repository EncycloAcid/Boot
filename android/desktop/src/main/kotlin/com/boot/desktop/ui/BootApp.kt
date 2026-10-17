package com.boot.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameSeason
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.gameForecast
import com.boot.desktop.DesktopStore
import com.boot.desktop.Speed
import com.boot.desktop.Tab

@Composable
fun BootDesktopApp(
    store: DesktopStore,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val state = store.state
    if (state == null) {
        NewGameScreen(store)
        return
    }
    GameShell(store, state, fullscreen, onToggleFullscreen, onExitFullscreen)
}

@Composable
private fun GameShell(
    store: DesktopStore,
    state: GameState,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val toast = store.error ?: store.notice
    var menuOpen by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize()) {
        // Office is always the main screen — tabs open as floating windows over it.
        OfficeScene(
            state,
            store.playing,
            Modifier.fillMaxSize(),
            insetTop = if (state.games?.project != null) 188.dp else 140.dp,
        )

        GdtHud(
            store = store,
            state = state,
            fullscreen = fullscreen,
            menuOpen = menuOpen,
            onMenu = { menuOpen = !menuOpen },
            onToggleFullscreen = onToggleFullscreen,
            onExitFullscreen = onExitFullscreen,
            onAbandon = { store.abandon(); menuOpen = false },
        )

        FloatingTabWindow(store, state)

        // Office-layer cards when no floating tab / modal is open.
        if (store.tab == Tab.OFFICE && !store.holdsClock &&
            state.events.isEmpty() && state.pendingPivot == null && !state.isOver &&
            state.games?.unseenReview == null
        ) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 36.dp, bottom = 118.dp)
                    .widthIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OfficeHints(store, state)
                if (state.inbox.isNotEmpty()) InboxPanel(store, state)
            }
        }

        if (store.showCoach && store.tab == Tab.OFFICE && !store.designing &&
            state.events.isEmpty() && state.pendingPivot == null && !state.isOver &&
            state.games?.unseenReview == null
        ) {
            FirstRunCoach(
                store = store,
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 96.dp)
                    .widthIn(max = 300.dp),
            )
        }

        FloatingNav(store, Modifier.align(Alignment.BottomCenter))

        if (toast != null) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 230.dp)) {
                ToastBanner(toast) { store.dismissNotice() }
            }
        }

        // Same decision stack as Android GameScreen — blocks the calendar.
        DecisionOverlays(store, state)
        if (store.designing) GameDesigner(store, state)
        if (store.designingDevice) DeviceDesigner(store, state)
        store.openRelease?.let { ReleaseReport(store, state, it) }
        store.campaignLane?.let { MarketingPlanner(store, state, it) }
        store.openStock?.let { StockSheet(store, state, it) }

        // Toasts above every Dialog so designer / event failures aren't invisible.
        if (toast != null) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 210.dp)) {
                ToastBanner(toast) { store.dismissToast() }
            }
        }
    }
}

/** Guided next steps after New Game — dismissible, never blocks play. */
@Composable
private fun FirstRunCoach(
    store: DesktopStore,
    state: GameState,
    modifier: Modifier = Modifier,
) {
    val h = hudColors
    val lane = state.startingLane
    val primaryLabel = when (lane) {
        Lane.GAMES -> "Open Lanes · design a game"
        Lane.WEBSITES -> "Open Lanes · take a brief"
        Lane.HARDWARE -> "Open Lanes · start a device"
        else -> "Open Lanes · get started"
    }
    Column(
        modifier
            .shadow(12.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(RoundedCornerShape(16.dp))
            .background(h.panel)
            .border(1.5.dp, h.accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Getting started", style = MaterialTheme.typography.titleLarge, color = h.ink, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { store.dismissCoach() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Got it", style = MaterialTheme.typography.labelLarge, color = h.accent, fontWeight = FontWeight.Bold)
            }
        }
        Hint("Your studio is live. Use the dock below to manage businesses, research, and people.")
        MacButton(
            primaryLabel,
            onClick = {
                store.openLane(lane)
                store.dismissCoach()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        MacButton(
            "Advance one week",
            onClick = {
                store.advance(1)
                store.dismissCoach()
            },
            primary = false,
            enabled = store.canAdvance && !store.playing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Non-office tabs as a single floating window; Office stays visible underneath. */
@Composable
private fun FloatingTabWindow(store: DesktopStore, state: GameState) {
    val open = store.tab != Tab.OFFICE
    // Keep last tab so exit animation still has content while tab snaps back to Office.
    var shown by remember { mutableStateOf(Tab.LANES) }
    var drag by remember { mutableStateOf(DpOffset.Zero) }
    LaunchedEffect(store.tab) {
        if (store.tab != Tab.OFFICE) {
            shown = store.tab
            drag = DpOffset.Zero
        }
    }
    val title = when (shown) {
        Tab.LANES -> "Lanes"
        Tab.RESEARCH -> "Research"
        Tab.TEAM -> "Team"
        Tab.COMPANY -> "Company"
        Tab.NEWS -> "News"
        Tab.OFFICE -> ""
    }
    val subtitle = when (shown) {
        Tab.LANES -> "Businesses · invent, ship, grow"
        Tab.RESEARCH -> "Spend RP · unlock tools & topics"
        Tab.TEAM -> "Hire, assign, train"
        Tab.COMPANY -> "Cash, expand, strategy"
        Tab.NEWS -> "Press & industry headlines"
        Tab.OFFICE -> null
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            Box(Modifier.fillMaxSize()) {
                // Soft scrim — office still visible, window clearly focused.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x33000000))
                        .clickable { store.tab = Tab.OFFICE },
                )
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 64.dp, end = 64.dp, top = 120.dp, bottom = 100.dp),
                ) {
                    FloatingWindow(
                        title = title,
                        subtitle = subtitle,
                        onClose = { store.tab = Tab.OFFICE },
                        focused = true,
                        onDrag = { delta -> drag += delta },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(drag.x, drag.y)
                            .widthIn(min = 400.dp, max = 600.dp)
                            .fillMaxWidth(0.58f)
                            .heightIn(min = 300.dp, max = maxHeight * 0.82f)
                            .fillMaxHeight(0.72f),
                    ) {
                        when (shown) {
                            Tab.LANES -> LanesScreen(store, state)
                            Tab.RESEARCH -> ResearchScreen(store, state)
                            Tab.TEAM -> TeamScreen(store, state)
                            Tab.COMPANY -> CompanyScreen(store, state)
                            Tab.NEWS -> NewsScreen(store, state)
                            Tab.OFFICE -> Unit
                        }
                    }
                }
            }
        }
    }
}

/**
 * Studio floating chrome:
 * Menu · resource discs · project card · stacked status chips · transport.
 */
@Composable
private fun GdtHud(
    store: DesktopStore,
    state: GameState,
    fullscreen: Boolean,
    menuOpen: Boolean,
    onMenu: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onAbandon: () -> Unit,
) {
    val h = hudColors
    val project = state.games?.project
    val menuShape = RoundedCornerShape(12.dp)
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp)) {
        val sideGutter = 176.dp
        val centerMax = (maxWidth - sideGutter * 2).coerceIn(220.dp, 360.dp)
        val bubbleSize = if (maxWidth < 960.dp) 50.dp else 56.dp

        Row(
            Modifier
                .align(Alignment.TopStart)
                .shadow(6.dp, menuShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
                .clip(menuShape)
                .background(h.panel)
                .border(1.dp, h.panelBorder, menuShape)
                .clickable(onClick = onMenu)
                .defaultMinSize(minHeight = 40.dp)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudIcon(HudGlyph.MENU, h.inkMute, 16.dp)
            Text("Menu", style = MaterialTheme.typography.labelLarge, color = h.ink, fontWeight = FontWeight.SemiBold)
        }
        if (menuOpen) {
            GlassPanel(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 52.dp)
                    .widthIn(min = 220.dp, max = 260.dp),
            ) {
                Text(
                    state.companyName,
                    style = MaterialTheme.typography.titleLarge,
                    color = h.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Hint(state.office.title)
                MacButton(
                    if (fullscreen) "Windowed (Esc)" else "Fullscreen (F11)",
                    onClick = { if (fullscreen) onExitFullscreen() else onToggleFullscreen(); onMenu() },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                MacButton(
                    if (store.playing) "Pause" else "Play",
                    onClick = { store.togglePlay(); onMenu() },
                    enabled = store.canAdvance || store.playing,
                    modifier = Modifier.fillMaxWidth(),
                )
                MacButton(
                    "Advance week",
                    onClick = { store.advance(1); onMenu() },
                    enabled = store.canAdvance && !store.holdsClock && !store.playing,
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                val unread = (state.news.lastOrNull()?.id ?: 0) - store.newsSeen
                MacButton(
                    if (unread > 0) "News ($unread)" else "News",
                    onClick = { store.tab = Tab.NEWS; onMenu() },
                    primary = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                MacButton("Abandon run", onClick = onAbandon, primary = false, modifier = Modifier.fillMaxWidth())
            }
        }

        val season = GameSeason.forWeek(state.week)
        val weekly = state.trailingProfit(weeks = 4) / 4
        val colors = gameColors

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = centerMax)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (project != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    ResourceBubble(
                        "${project.design.toInt()}",
                        h.designBubble,
                        HudGlyph.DESIGN,
                        size = bubbleSize,
                        caption = "Design",
                        onClick = { store.openLane(Lane.GAMES) },
                    )
                    ResourceBubble(
                        "${project.tech.toInt()}",
                        h.techBubble,
                        HudGlyph.TECH,
                        size = bubbleSize,
                        caption = "Tech",
                        onClick = { store.openLane(Lane.GAMES) },
                    )
                    Spacer(Modifier.width(8.dp))
                    ResourceBubble(
                        "${state.researchPoints.toInt()}",
                        h.research,
                        HudGlyph.RESEARCH,
                        size = bubbleSize,
                        caption = "RP",
                        onClick = { store.tab = Tab.RESEARCH },
                    )
                    ResourceBubble(
                        "${project.bugs.toInt()}",
                        h.bugs,
                        HudGlyph.BUGS,
                        size = bubbleSize,
                        caption = "Bugs",
                        onClick = { store.openLane(Lane.GAMES) },
                    )
                }
                val lift = season.launchMultiplier(project.concept.genre)
                val seasonPct = ((lift - 1) * 100).toInt()
                val seasonLine = when {
                    lift > 1.0 -> "${season.title} · ${project.concept.genreTitle} +$seasonPct% demand"
                    lift < 1.0 -> "${season.title} · ${project.concept.genreTitle} $seasonPct% demand"
                    else -> "${season.title} · ${season.weeksLabel}"
                }
                val forecast = state.gameForecast()
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text(
                        project.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = h.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${project.concept.topic.title} / ${project.concept.genreTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = h.inkMute,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Hint(seasonLine, if (lift >= 1.0) h.accent else h.inkMute)
                    Spacer(Modifier.height(4.dp))
                    StatMeter(
                        "Progress",
                        project.completion.toFloat(),
                        h.accent,
                        caption = forecast?.weeksToReady?.let { "${(project.completion * 100).toInt()}% · ~${it}w" }
                            ?: "${(project.completion * 100).toInt()}%",
                    )
                    StatMeter(
                        "Hype ${project.hype.toInt()}",
                        (project.hype / 100.0).toFloat().coerceIn(0f, 1f),
                        h.hype,
                    )
                    if (!project.isReady) {
                        StatMeter(
                            "Genre fit",
                            project.designLeanFit().toFloat(),
                            if (project.designLeanFit() >= 0.7) colors.design else colors.warn,
                        )
                    } else {
                        StatMeter(
                            "Polish",
                            (project.polish / (project.work * 0.4)).toFloat().coerceIn(0f, 1f),
                            colors.milestone,
                        )
                    }
                    if (project.isReady) {
                        MacButton(
                            "Open Lanes · ship ${project.title}",
                            onClick = { store.openLane(Lane.GAMES) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                // Idle: match Android — Cash / RP / Fans discs instead of empty Design/Tech/Bugs zeros.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    ResourceBubble(
                        Format.money(state.cash),
                        h.cash,
                        HudGlyph.CASH,
                        size = bubbleSize,
                        caption = "Cash",
                        onClick = { store.companyPage = 1; store.tab = Tab.COMPANY },
                    )
                    ResourceBubble(
                        "${state.researchPoints.toInt()}",
                        h.research,
                        HudGlyph.RESEARCH,
                        size = bubbleSize,
                        caption = "RP",
                        onClick = { store.tab = Tab.RESEARCH },
                    )
                    val fans = state.games?.fans
                    if (fans != null && Lane.GAMES in state.lanes) {
                        ResourceBubble(
                            Format.count(fans),
                            h.fans,
                            HudGlyph.FANS,
                            size = bubbleSize,
                            caption = "Fans",
                            onClick = { store.openLane(Lane.GAMES) },
                        )
                    } else {
                        ResourceBubble(
                            "${state.reputation.score.toInt()}",
                            h.fans,
                            HudGlyph.FANS,
                            size = bubbleSize,
                            caption = "Rep",
                            onClick = { store.companyPage = 0; store.tab = Tab.COMPANY },
                        )
                    }
                }
                // Compact company strip always visible when idle (even with a floating tab open).
                GlassPanel(Modifier.widthIn(max = 300.dp)) {
                    Text(
                        state.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        color = h.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Hint("${state.world.era.title} · ${state.office.title}")
                    Hint("${season.title} · ${season.marketTip}", h.accent)
                    if (store.tab == Tab.OFFICE && Lane.GAMES in state.lanes) {
                        MacButton(
                            "Start a game",
                            onClick = { store.openLane(Lane.GAMES) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 132.dp, max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            StatusPill(h.accent, HudGlyph.CALENDAR, "${state.dateLabel} · ${season.title}")
            val fans = state.games?.fans
            if (fans != null && Lane.GAMES in state.lanes) {
                val trust = state.games?.fanTrust
                StatusPill(
                    h.fans,
                    HudGlyph.FANS,
                    "${Format.count(fans)} Fans",
                    detail = trust?.let { "Trust ${Format.percent(it)}" },
                ) { store.openLane(Lane.GAMES) }
            } else {
                val scars = state.reputation.scars.size
                StatusPill(
                    h.fans,
                    HudGlyph.FANS,
                    "Rep ${state.reputation.score.toInt()}",
                    detail = if (scars > 0) "$scars scar${if (scars == 1) "" else "s"}" else "Clean",
                ) { store.tab = Tab.COMPANY }
            }
            StatusPill(
                h.cash,
                HudGlyph.CASH,
                Format.money(state.cash),
                valueColor = if (state.cash < 0) colors.bad else h.cash,
                detail = "${Format.signedMoney(weekly)}/wk",
                detailColor = if (weekly >= 0) colors.good else colors.bad,
            ) { store.companyPage = 1; store.tab = Tab.COMPANY }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportChip(store.speed.label) {
                    store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                }
                TransportChip("Week", enabled = store.canAdvance && !store.holdsClock && !store.playing) {
                    store.advance(1)
                }
                TransportChip(
                    if (store.playing) "Pause" else "Play",
                    filled = true,
                    enabled = (store.canAdvance && !store.holdsClock) || store.playing,
                ) {
                    store.togglePlay()
                }
            }
        }
    }
}

@Composable
private fun TransportChip(label: String, filled: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val h = hudColors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .shadow(if (filled && enabled) 4.dp else 2.dp, shape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(shape)
            .background(if (filled && enabled) h.accent else h.panel)
            .border(1.dp, if (filled && enabled) h.accent else h.panelBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 38.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled && enabled) Color.White else if (enabled) h.ink else h.inkMute,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
