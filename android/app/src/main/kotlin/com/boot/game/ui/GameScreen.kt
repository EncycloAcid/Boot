package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Desk
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.game.GameStore
import com.boot.game.Speed
import com.boot.game.Tab

@Composable
fun BootApp(store: GameStore) {
    val state = store.state
    if (state == null) NewGameScreen(store) else GameScreen(store, state)
}

private data class TabSpec(val tab: Tab, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabSpec(Tab.OFFICE, "Office", Icons.Filled.Desk),
    TabSpec(Tab.LANES, "Lanes", Icons.Filled.Work),
    TabSpec(Tab.RESEARCH, "Research", Icons.Filled.Science),
    TabSpec(Tab.TEAM, "Team", Icons.Filled.Groups),
    TabSpec(Tab.COMPANY, "Company", Icons.Filled.AccountBalance),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameScreen(store: GameStore, state: GameState) {
    val colors = gameColors
    val snackbar = remember { SnackbarHostState() }
    var confirmAbandon by remember { mutableStateOf(false) }

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
    // The clock, timed off display frames. Each beat is one week; it stops itself when something needs you.
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
    // Leaving the app pauses the game.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { store.pause() }
    BackHandler(enabled = store.tab != Tab.OFFICE) { store.tab = Tab.OFFICE }

    Scaffold(
        containerColor = colors.grouped,
        topBar = { Hud(store, state, onAbandon = { confirmAbandon = true }) },
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                ShortNavigationBar(containerColor = colors.bar) {
                    for (t in tabs) {
                        ShortNavigationBarItem(
                            selected = store.tab == t.tab,
                            onClick = { store.tab = t.tab },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val motion = MaterialTheme.motionScheme
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = store.tab,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) + slideInVertically(motion.defaultSpatialSpec()) { it / 24 }) togetherWith
                        fadeOut(motion.fastEffectsSpec())
                },
                label = "tab",
            ) { tab ->
                when (tab) {
                    Tab.OFFICE -> OfficeScreen(store, state)
                    Tab.LANES -> LanesScreen(store, state)
                    Tab.TEAM -> TeamScreen(store, state)
                    Tab.RESEARCH -> ResearchScreen(store, state)
                    Tab.COMPANY -> CompanyScreen(store, state)
                    Tab.NEWS -> NewsScreen(state)
                }
            }
        }
    }

    // Decisions stop the clock. Endings beat the Pivot Point, which beats ordinary events.
    when {
        state.isOver -> EndingDialog(store, state)
        state.pendingPivot != null -> PivotDialog(store, state)
        state.events.isNotEmpty() -> EventSheet(store, state)
        state.games?.unseenReview != null -> ReviewSheet(store, state, state.games!!.unseenReview!!)
        store.designing && state.games != null -> GameDesigner(store, state)
        store.openRelease != null -> ReleaseSheet(store, state, store.openRelease!!)
    }

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
 * The heads-up display, always on top: who you are and when, the clock controls, and the three
 * numbers you check constantly (cash, research points, fans or reputation). A thin line under it
 * fills as each week passes while the game runs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Hud(store: GameStore, state: GameState, onAbandon: () -> Unit) {
    val colors = gameColors
    var menuOpen by remember { mutableStateOf(false) }
    val weekly = state.trailingProfit(weeks = 4) / 4
    Surface(color = colors.bar) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable { store.tab = Tab.TEAM }
                        .semantics { contentDescription = "You, ${state.founder.name}" },
                ) { FounderPortrait(state, 42.dp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    FitText(state.companyName, MaterialTheme.typography.titleLargeEmphasized, min = 14.sp)
                    FitText(
                        "${state.dateLabel} · ${state.world.era.title.removePrefix("The ")}",
                        MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ClockControls(store)
            }
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HudChip(
                    Format.money(state.cash),
                    detail = "${Format.signedMoney(weekly)}/wk",
                    detailColor = if (weekly >= 0) colors.good else colors.bad,
                    valueColor = if (state.cash < 0) colors.bad else MaterialTheme.colorScheme.onSurface,
                    description = "Cash",
                ) {
                    store.companyPage = 1
                    store.tab = Tab.COMPANY
                }
                HudChip("${state.researchPoints.toInt()}", icon = Icons.Filled.Science, iconTint = colors.milestone, description = "Research points") {
                    store.tab = Tab.RESEARCH
                }
                val fans = state.games?.fans
                if (fans != null && Lane.GAMES in state.lanes) {
                    HudChip(Format.count(fans), icon = Icons.Filled.Star, iconTint = colors.warn, description = "Fans") { store.openLane(Lane.GAMES) }
                } else {
                    HudChip("${state.reputation.score.toInt()}", icon = Icons.Filled.Star, iconTint = colors.warn, description = "Reputation") {
                        store.companyPage = 0
                        store.tab = Tab.COMPANY
                    }
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreHoriz, "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("All news") }, onClick = {
                            menuOpen = false
                            store.tab = Tab.NEWS
                        })
                        DropdownMenuItem(text = { Text("Abandon run and start over") }, onClick = {
                            menuOpen = false
                            store.pause()
                            onAbandon()
                        })
                    }
                }
            }
            WeekProgress(store, state)
            HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
        }
    }
}

@Composable
private fun HudChip(
    value: String,
    detail: String? = null,
    detailColor: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(gameColors.fill)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor, maxLines = 1)
        if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall, color = detailColor, maxLines = 1)
    }
}

/**
 * Play and pause, Game Dev Tycoon style, plus the speed and a single-week step. The play button
 * morphs from a circle to a rounded square while time runs.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClockControls(store: GameStore) {
    val haptics = LocalHapticFeedback.current
    val enabled = store.canAdvance && !store.holdsClock
    val corner by animateDpAsState(if (store.playing) 16.dp else 26.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "play")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            store.speed.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                }
                .semantics { contentDescription = "Speed ${store.speed.label}" }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                store.advance(1)
            },
            enabled = enabled && !store.playing,
        ) { Icon(Icons.Filled.SkipNext, "Next week") }
        Surface(
            onClick = {
                haptics.performHapticFeedback(if (store.playing) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                store.togglePlay()
            },
            enabled = enabled || store.playing,
            shape = RoundedCornerShape(corner),
            color = if (enabled || store.playing) MaterialTheme.colorScheme.primary else gameColors.fill,
            contentColor = if (enabled || store.playing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (store.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (store.playing) "Pause" else "Play",
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

/** Fills over one week while the clock runs. */
@Composable
private fun WeekProgress(store: GameStore, state: GameState) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(state.week, store.playing, store.speed) {
        progress.snapTo(0f)
        if (store.playing) progress.animateTo(1f, tween(store.speed.millis.toInt(), easing = LinearEasing))
    }
    Box(Modifier.fillMaxWidth().height(3.dp)) {
        Box(
            Modifier
                .fillMaxWidth(progress.value)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (store.playing) 0.9f else 0f)),
        )
    }
}

/** Standard scrolling body for a tab: grouped sections on the grouped background. */
@Composable
fun ScreenColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

/** A tab's own title, at the top of its scrolling content. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(horizontal = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMediumEmphasized)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
