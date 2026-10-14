package com.boot.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Format
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
    val c = spriteColors
    val toast = store.error ?: store.notice

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

    Box(Modifier.fillMaxSize().background(c.ink)) {
        Column(Modifier.fillMaxSize()) {
            DesktopHud(store, state, fullscreen, onToggleFullscreen, onExitFullscreen)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = store.tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "desktopTab",
                ) { tab ->
                    when (tab) {
                        Tab.OFFICE -> OfficeScreen(store, state)
                        Tab.LANES -> LanesScreen(store, state)
                        Tab.RESEARCH -> ResearchScreen(store, state)
                        Tab.TEAM -> TeamScreen(store, state)
                        Tab.COMPANY -> CompanyScreen(store, state)
                    }
                }
                FloatingNav(store, Modifier.align(Alignment.BottomCenter))
            }
        }
        if (toast != null) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 72.dp)) {
                ToastBanner(toast) { store.dismissNotice() }
            }
        }
    }
}

@Composable
private fun DesktopHud(
    store: DesktopStore,
    state: GameState,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val c = spriteColors
    val weekly = state.trailingProfit(weeks = 4) / 4
    val enabled = store.canAdvance
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.inkRaised.copy(alpha = 0.94f))
            .border(width = 0.dp, color = c.panelEdge, shape = RoundedCornerShape(0))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(state.companyName, style = MaterialTheme.typography.titleLarge, color = c.text)
                Text(
                    "${state.dateLabel} · ${state.world.era.title.removePrefix("The ")} · ${state.office.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textDim,
                )
            }
            ClockCluster(store, enabled)
            Spacer(Modifier.width(12.dp))
            WindowChrome(fullscreen, onToggleFullscreen, onExitFullscreen)
        }
        Row(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HudStat(
                "Cash",
                Format.money(state.cash),
                if (state.cash < 0) c.danger else c.text,
            ) {
                store.tab = Tab.COMPANY
            }
            HudStat("Weekly", Format.signedMoney(weekly), if (weekly >= 0) c.mint else c.danger) {
                store.tab = Tab.COMPANY
            }
            HudStat("RP", "${state.researchPoints.toInt()}", c.amber) {
                store.tab = Tab.RESEARCH
            }
            HudStat("Team", "${state.staff.size + 1}") {
                store.tab = Tab.TEAM
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (fullscreen) "Esc windowed · F11 toggle" else "F11 fullscreen",
                style = MaterialTheme.typography.labelSmall,
                color = c.mute,
            )
        }
    }
}

@Composable
private fun ClockCluster(store: DesktopStore, enabled: Boolean) {
    val c = spriteColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(c.panel)
                .clickable {
                    store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(store.speed.label, style = MaterialTheme.typography.labelLarge, color = c.text)
        }
        SpriteButton("Week", onClick = { store.advance(1) }, enabled = enabled && !store.playing, primary = false)
        SpriteButton(
            if (store.playing) "Pause" else "Play",
            onClick = { store.togglePlay() },
            enabled = enabled || store.playing,
            primary = true,
        )
    }
}

@Composable
private fun WindowChrome(
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val c = spriteColors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ChromeBtn(if (fullscreen) "Window" else "Full") {
            if (fullscreen) onExitFullscreen() else onToggleFullscreen()
        }
        Text("■", color = c.mute, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ChromeBtn(label: String, onClick: () -> Unit) {
    val c = spriteColors
    Box(
        Modifier
            .size(width = 72.dp, height = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.amber, fontWeight = FontWeight.Bold)
    }
}
