package com.boot.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
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
        // Full-bleed isometric office — inset so top HUD never covers the room ridge.
        when (store.tab) {
            Tab.OFFICE -> OfficeScene(
                state,
                store.playing,
                Modifier.fillMaxSize(),
                // Keep the room ridge below Menu / bubbles / project card.
                insetTop = if (state.games?.project != null) 168.dp else 120.dp,
            )
            else -> {
                // Soft cream world behind other tabs so chrome stays GDT-light.
                Box(Modifier.fillMaxSize().background(Color(0xFFF4F1EA)))
                AnimatedContent(
                    targetState = store.tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "desktopTab",
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    when (tab) {
                        Tab.OFFICE -> Unit
                        Tab.LANES -> LanesScreen(store, state)
                        Tab.RESEARCH -> ResearchScreen(store, state)
                        Tab.TEAM -> TeamScreen(store, state)
                        Tab.COMPANY -> CompanyScreen(store, state)
                    }
                }
            }
        }

        // Floating HUD overlays the office.
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

        FloatingNav(store, Modifier.align(Alignment.BottomCenter))

        if (toast != null) {
            // Sit under the center HUD stack so toasts never cover bubbles or the project card.
            Box(Modifier.align(Alignment.TopCenter).padding(top = 210.dp)) {
                ToastBanner(toast) { store.dismissNotice() }
            }
        }
    }
}

/**
 * Game Dev Tycoon / macOS floating chrome:
 * Menu · resource circles · project card · stacked date/fans/cash pills · clock.
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
    // Side gutters reserve Menu + status pills; center stack is width-capped so nothing collides.
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 20.dp)) {
        val sideGutter = 168.dp
        val centerMax = (maxWidth - sideGutter * 2).coerceIn(200.dp, 340.dp)
        val bubbleSize = if (maxWidth < 960.dp) 46.dp else 50.dp

        // Top-left Menu
        Row(
            Modifier
                .align(Alignment.TopStart)
                .shadow(8.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(h.glass)
                .clickable(onClick = onMenu)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(Modifier.width(14.dp).height(2.dp).background(h.text, RoundedCornerShape(1.dp)))
                }
            }
            Text("Menu", style = MaterialTheme.typography.labelLarge, color = h.text, fontWeight = FontWeight.SemiBold)
        }
        if (menuOpen) {
            GlassPanel(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 52.dp)
                    .widthIn(min = 200.dp, max = 240.dp),
            ) {
                Text(
                    state.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    color = h.text,
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

        // Center stack: bubbles then project card — one column so they never overlap each other or the side chrome.
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
                ResourceBubble("$design", h.designBubble, "◇", size = bubbleSize)
                ResourceBubble("$tech", Color(0xFFFFCC00), "▣", size = bubbleSize)
                Spacer(Modifier.width(12.dp))
                ResourceBubble("${state.researchPoints.toInt()}", h.research, "⚛", size = bubbleSize)
                ResourceBubble("$bugs", h.bugs, "!", size = bubbleSize)
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
                        color = h.text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${project.concept.topic.title} / ${project.concept.genreTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = h.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Hint(seasonLine, if (lift >= 1.0) h.accent else h.textDim)
                    Spacer(Modifier.height(4.dp))
                    ProgressRow("Progress", project.completion.toFloat(), h.accent)
                    ProgressRow("Hype ${project.hype.toInt()}", (project.hype / 100.0).toFloat().coerceIn(0f, 1f), Color(0xFF6B7B3A))
                }
            } else if (store.tab == Tab.OFFICE) {
                GlassPanel(Modifier.widthIn(max = 280.dp)) {
                    Text(
                        state.companyName,
                        style = MaterialTheme.typography.titleLarge,
                        color = h.text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Hint("${state.world.era.title} · ${state.office.title}")
                    Hint("${season.title} · ${season.marketTip}", h.accent)
                }
            }
        }

        // Top-right stacked pills — fixed end column, never shares space with the center stack.
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 120.dp, max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            StatusPill(h.accent, "◷", "${state.dateLabel} · ${season.title}")
            val fans = state.games?.fans
            if (fans != null) {
                StatusPill(h.fans, "♥", "${Format.count(fans)} Fans") { store.openLane(com.boot.core.Lane.GAMES) }
            } else {
                StatusPill(h.fans, "★", "Rep ${state.reputation.score.toInt()}") { store.tab = Tab.COMPANY }
            }
            StatusPill(h.cash, "$", Format.money(state.cash), h.cash) { store.tab = Tab.COMPANY }
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
            Text(label, style = MaterialTheme.typography.labelMedium, color = h.textDim)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = h.textDim)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0x14000000),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TinyChip(label: String, filled: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val h = hudColors
    Box(
        Modifier
            .shadow(4.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(if (filled && enabled) h.accent else h.glass)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled && enabled) Color.White else if (enabled) h.text else h.textDim,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}
