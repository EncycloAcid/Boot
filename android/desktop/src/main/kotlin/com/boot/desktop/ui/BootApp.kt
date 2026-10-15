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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameSeason
import com.boot.core.GameState
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
            insetTop = if (state.games?.project != null) 168.dp else 120.dp,
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

        FloatingNav(store, Modifier.align(Alignment.BottomCenter))

        if (toast != null) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 210.dp)) {
                ToastBanner(toast) { store.dismissNotice() }
            }
        }
    }
}

/** Non-office tabs as a single floating window; Office stays visible underneath. */
@Composable
private fun FloatingTabWindow(store: DesktopStore, state: GameState) {
    val open = store.tab != Tab.OFFICE
    // Keep last tab so exit animation still has content while tab snaps back to Office.
    var shown by remember { mutableStateOf(Tab.LANES) }
    if (store.tab != Tab.OFFICE) shown = store.tab
    val title = when (shown) {
        Tab.LANES -> "Lanes"
        Tab.RESEARCH -> "Research"
        Tab.TEAM -> "Team"
        Tab.COMPANY -> "Company"
        Tab.OFFICE -> ""
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            // Leave generous office margins so the window reads as chrome, not a page.
            BoxWithConstraints(Modifier.fillMaxSize().padding(start = 72.dp, end = 72.dp, top = 132.dp, bottom = 108.dp)) {
                FloatingWindow(
                    title = title,
                    onClose = { store.tab = Tab.OFFICE },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(min = 380.dp, max = 560.dp)
                        .fillMaxWidth(0.55f)
                        .heightIn(min = 280.dp, max = maxHeight * 0.78f)
                        .fillMaxHeight(0.68f),
                ) {
                    when (shown) {
                        Tab.LANES -> LanesScreen(store, state)
                        Tab.RESEARCH -> ResearchScreen(store, state)
                        Tab.TEAM -> TeamScreen(store, state)
                        Tab.COMPANY -> CompanyScreen(store, state)
                        Tab.OFFICE -> Unit
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
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 20.dp)) {
        val sideGutter = 168.dp
        val centerMax = (maxWidth - sideGutter * 2).coerceIn(200.dp, 340.dp)
        val bubbleSize = if (maxWidth < 960.dp) 48.dp else 54.dp

        Row(
            Modifier
                .align(Alignment.TopStart)
                .shadow(6.dp, menuShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
                .clip(menuShape)
                .background(h.panel)
                .border(1.dp, h.panelBorder, menuShape)
                .clickable(onClick = onMenu)
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HudIcon(HudGlyph.MENU, h.inkMute, 14.dp)
            Text("Menu", style = MaterialTheme.typography.labelLarge, color = h.ink, fontWeight = FontWeight.SemiBold)
        }
        if (menuOpen) {
            GlassPanel(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp)
                    .widthIn(min = 200.dp, max = 240.dp),
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
                )
                MacButton(
                    if (store.playing) "Pause" else "Play",
                    onClick = { store.togglePlay(); onMenu() },
                    enabled = store.canAdvance || store.playing,
                )
                MacButton("Advance week", onClick = { store.advance(1); onMenu() }, enabled = store.canAdvance && !store.playing, primary = false)
                MacButton("Abandon run", onClick = onAbandon, primary = false)
            }
        }

        val season = GameSeason.forWeek(state.week)

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = centerMax)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val design = project?.design?.toInt() ?: 0
                val tech = project?.tech?.toInt() ?: 0
                val bugs = project?.bugs?.toInt() ?: 0
                ResourceBubble("$design", h.designBubble, HudGlyph.DESIGN, size = bubbleSize)
                ResourceBubble("$tech", h.techBubble, HudGlyph.TECH, size = bubbleSize)
                Spacer(Modifier.width(12.dp))
                ResourceBubble("${state.researchPoints.toInt()}", h.research, HudGlyph.RESEARCH, size = bubbleSize)
                ResourceBubble("$bugs", h.bugs, HudGlyph.BUGS, size = bubbleSize)
            }
            if (project != null) {
                val lift = season.launchMultiplier(project.concept.genre)
                val seasonPct = ((lift - 1) * 100).toInt()
                val seasonLine = when {
                    lift > 1.0 -> "${season.title} · ${project.concept.genreTitle} +$seasonPct% demand"
                    lift < 1.0 -> "${season.title} · ${project.concept.genreTitle} $seasonPct% demand"
                    else -> "${season.title} · ${season.weeksLabel}"
                }
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
                    ProgressRow("Progress", project.completion.toFloat(), h.accent)
                    ProgressRow("Hype ${project.hype.toInt()}", (project.hype / 100.0).toFloat().coerceIn(0f, 1f), h.hype)
                }
            } else {
                // Always show company tip over the office (windows float below / around it).
                GlassPanel(Modifier.widthIn(max = 280.dp)) {
                    Text(
                        state.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        color = h.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Hint("${state.world.era.title} · ${state.office.title}")
                    Hint("${season.title} · ${season.marketTip}", h.accent)
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 120.dp, max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            StatusPill(h.accent, HudGlyph.CALENDAR, "${state.dateLabel} · ${season.title}")
            val fans = state.games?.fans
            if (fans != null) {
                StatusPill(h.fans, HudGlyph.FANS, "${Format.count(fans)} Fans") { store.openLane(com.boot.core.Lane.GAMES) }
            } else {
                StatusPill(h.fans, HudGlyph.FANS, "Rep ${state.reputation.score.toInt()}") { store.tab = Tab.COMPANY }
            }
            StatusPill(h.cash, HudGlyph.CASH, Format.money(state.cash), h.cash) { store.tab = Tab.COMPANY }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TinyChip(store.speed.label) {
                    store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                }
                TinyChip("Week", enabled = store.canAdvance && !store.playing) { store.advance(1) }
                TinyChip(if (store.playing) "Pause" else "Play", filled = true, enabled = store.canAdvance || store.playing) {
                    store.togglePlay()
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, value: Float, color: Color) {
    val h = hudColors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = h.inkMute)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = h.inkMute)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0x22000000),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TinyChip(label: String, filled: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val h = hudColors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .shadow(if (filled && enabled) 4.dp else 2.dp, shape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(shape)
            .background(if (filled && enabled) h.accent else h.panel)
            .border(1.dp, if (filled && enabled) h.accent else h.panelBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled && enabled) Color.White else if (enabled) h.ink else h.inkMute,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
