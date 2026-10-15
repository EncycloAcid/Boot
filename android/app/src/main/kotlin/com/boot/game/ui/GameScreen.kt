package com.boot.game.ui

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.boot.core.Format
import com.boot.core.GameSeason
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.game.GameStore
import com.boot.game.StudioPage
import com.boot.game.Speed
import com.boot.game.Tab

@Composable
fun BootApp(store: GameStore) {
    val state = store.state
    if (state == null) NewGameScreen(store) else GameScreen(store, state)
}

private data class TabSpec(val tab: Tab, val label: String, val glyph: HudGlyph)

private val tabs = listOf(
    TabSpec(Tab.OFFICE, "Office", HudGlyph.OFFICE),
    TabSpec(Tab.LANES, "Lanes", HudGlyph.LANES),
    TabSpec(Tab.RESEARCH, "Research", HudGlyph.RESEARCH),
    TabSpec(Tab.TEAM, "Team", HudGlyph.TEAM),
    TabSpec(Tab.COMPANY, "Company", HudGlyph.COMPANY),
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
    ) { padding ->
        val motion = MaterialTheme.motionScheme
        // The content runs to the bottom of the screen and scrolls under the floating tab bar.
        Box(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
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
                    Tab.NEWS -> NewsScreen(store, state)
                }
            }
            FloatingTabBar(store, Modifier.align(Alignment.BottomCenter), snackbar)
        }
    }

    // Decisions stop the clock. Endings beat the Pivot Point, which beats ordinary events.
    when {
        state.isOver -> EndingDialog(store, state)
        state.pendingPivot != null -> PivotDialog(store, state)
        state.events.isNotEmpty() -> EventSheet(store, state)
        state.games?.unseenReview != null -> ReviewSheet(store, state, state.games!!.unseenReview!!)
        store.designing && state.games != null -> GameDesigner(store, state)
        store.designingDevice && state.hardware != null -> DeviceDesigner(store, state)
        store.openRelease != null -> ReleaseSheet(store, state, store.openRelease!!)
        store.guideOpen -> GuideSheet(store, state)
        store.openStock != null -> StockSheet(store, state, store.openStock!!)
        store.campaignLane != null -> CampaignSheet(store, state, store.campaignLane!!)
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
 * Phone studio chrome, matching the Windows paper HUD: menu, date and season, clock, then either
 * the development discs (design, tech, research, bugs) or cash, research and fans.
 */
@Composable
private fun Hud(store: GameStore, state: GameState, onAbandon: () -> Unit) {
    val colors = gameColors
    var menuOpen by remember { mutableStateOf(false) }
    val weekly = state.trailingProfit(weeks = 4) / 4
    val season = GameSeason.forWeek(state.week)
    val project = state.games?.project
    val unread = state.news.count { it.id > store.newsSeen }
    Surface(color = colors.grouped) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box {
                    BadgedBox(badge = { if (unread > 0) Badge() }) {
                        PaperChip(
                            onClick = { menuOpen = true },
                            description = if (unread > 0) "More, $unread new stories" else "More",
                        ) {
                            HudIcon(HudGlyph.MENU, colors.inkMute(), 14.dp)
                            Text("Menu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (unread > 0) "News · $unread new" else "News") }, onClick = {
                            menuOpen = false
                            store.tab = Tab.NEWS
                        })
                        DropdownMenuItem(text = { Text("How to play") }, onClick = {
                            menuOpen = false
                            store.guideOpen = true
                        })
                        DropdownMenuItem(text = { Text(if (store.playing) "Pause" else "Play") }, onClick = {
                            menuOpen = false
                            store.togglePlay()
                        })
                        DropdownMenuItem(text = { Text("Advance week") }, onClick = {
                            menuOpen = false
                            if (store.canAdvance && !store.playing) store.advance(1)
                        }, enabled = store.canAdvance && !store.holdsClock && !store.playing)
                        DropdownMenuItem(text = { Text("Abandon run and start over") }, onClick = {
                            menuOpen = false
                            store.pause()
                            onAbandon()
                        })
                    }
                }
                Column(Modifier.weight(1f)) {
                    FitText(state.companyName, MaterialTheme.typography.titleLarge, min = 14.sp)
                    FitText(
                        "${state.dateLabel} · ${season.title}",
                        MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable { store.tab = Tab.TEAM }
                        .semantics { contentDescription = "You, ${state.founder.name}" },
                ) { FounderPortrait(state, 36.dp) }
                ClockControls(store)
            }
            val roomy = LocalDensity.current.fontScale <= 1.15f
            if (project != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResourceDisc("${project.design.toInt()}", Color(0xFFF08A24), HudGlyph.DESIGN, "Design points", Modifier.weight(1f)) {
                        store.openStudio(StudioPage.MAKE)
                    }
                    ResourceDisc("${project.tech.toInt()}", Color(0xFFE8B923), HudGlyph.TECH, "Tech points", Modifier.weight(1f)) {
                        store.openStudio(StudioPage.MAKE)
                    }
                    ResourceDisc("${state.researchPoints.toInt()}", colors.milestone, HudGlyph.RESEARCH, "Research points", Modifier.weight(1f)) {
                        store.tab = Tab.RESEARCH
                    }
                    ResourceDisc("${project.bugs.toInt()}", colors.bad, HudGlyph.BUGS, "Bugs", Modifier.weight(1f)) {
                        store.openStudio(StudioPage.MAKE)
                    }
                }
                Row(
                    Modifier.padding(start = 8.dp, end = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HudChip(
                        Format.money(state.cash),
                        detail = "${Format.signedMoney(weekly)}/wk",
                        detailColor = if (weekly >= 0) colors.good else colors.bad,
                        valueColor = if (state.cash < 0) colors.bad else MaterialTheme.colorScheme.onSurface,
                        glyph = HudGlyph.CASH,
                        glyphTint = colors.good,
                        description = "Cash",
                        tip = "Cash in the bank, and what you've made or lost a week lately. Tap for money.",
                        modifier = Modifier.weight(1.4f, fill = false),
                    ) {
                        store.companyPage = 1
                        store.tab = Tab.COMPANY
                    }
                    FansOrRep(store, state, roomy, Modifier.weight(1f, fill = false))
                }
            } else {
                Row(
                    Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HudChip(
                        Format.money(state.cash),
                        detail = "${Format.signedMoney(weekly)}/wk",
                        detailColor = if (weekly >= 0) colors.good else colors.bad,
                        valueColor = if (state.cash < 0) colors.bad else MaterialTheme.colorScheme.onSurface,
                        glyph = HudGlyph.CASH,
                        glyphTint = colors.good,
                        description = "Cash",
                        tip = "Cash in the bank, and what you've made or lost a week lately. Tap for money.",
                        modifier = Modifier.weight(1.8f, fill = false),
                    ) {
                        store.companyPage = 1
                        store.tab = Tab.COMPANY
                    }
                    HudChip(
                        "${state.researchPoints.toInt()}",
                        detail = if (roomy) "RP" else null,
                        glyph = HudGlyph.RESEARCH,
                        glyphTint = colors.milestone,
                        description = "Research points",
                        tip = "Research points, to spend on new tech. Tap for research.",
                        modifier = Modifier.weight(1f, fill = false),
                    ) { store.tab = Tab.RESEARCH }
                    FansOrRep(store, state, roomy, Modifier.weight(1f, fill = false))
                }
            }
            WeekProgress(store, state)
        }
    }
}

@Composable
private fun FansOrRep(store: GameStore, state: GameState, roomy: Boolean, modifier: Modifier) {
    val colors = gameColors
    val fans = state.games?.fans
    if (fans != null && Lane.GAMES in state.lanes) {
        HudChip(
            Format.count(fans),
            detail = if (roomy) "fans" else null,
            glyph = HudGlyph.FANS,
            glyphTint = Color(0xFFC44D8A),
            description = "Fans",
            tip = "Fans buy your games at launch. Hits win more; flops lose some. Tap for your games.",
            modifier = modifier,
        ) { store.openStudio(StudioPage.GAMES) }
    } else {
        HudChip(
            "${state.reputation.score.toInt()}",
            detail = if (roomy) "rep" else null,
            glyph = HudGlyph.FANS,
            glyphTint = colors.warn,
            description = "Reputation",
            tip = "Reputation, out of 100. Scandals leave scars. Tap for the company.",
            modifier = modifier,
        ) {
            store.companyPage = 0
            store.tab = Tab.COMPANY
        }
    }
}

private fun GameColors.inkMute(): Color = other

@Composable
private fun PaperChip(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar)
            .border(1.dp, colors.separator, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

/** Saturated development disc from the Windows HUD, sized for a phone row. */
@Composable
private fun ResourceDisc(
    value: String,
    fill: Color,
    glyph: HudGlyph,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .size(48.dp)
                .shadow(5.dp, shape, clip = false)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(fill, Color(fill.red * 0.86f, fill.green * 0.86f, fill.blue * 0.86f)),
                    ),
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), shape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HudIcon(glyph, Color.White, 14.dp)
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HudChip(
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    detailColor: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified,
    glyph: HudGlyph? = null,
    glyphTint: Color = Color.Unspecified,
    description: String,
    tip: String = description,
    onClick: () -> Unit,
) = Tip(tip, modifier) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (glyph != null) HudIcon(glyph, if (glyphTint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else glyphTint, 16.dp)
        // The last one in the row gets what width is left, so its text shrinks rather than clips.
        FitText(value, MaterialTheme.typography.titleSmall, color = valueColor, modifier = Modifier.weight(1f, fill = false))
        if (detail != null) {
            FitText(
                detail, MaterialTheme.typography.labelSmall,
                color = if (detailColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else detailColor,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
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
    val corner by animateDpAsState(if (store.playing) 14.dp else 24.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "play")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tip("Game speed: ${store.speed.label}. Tap to change.") {
        Text(
            store.speed.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(CircleShape)
                .background(gameColors.fill)
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                }
                .semantics { contentDescription = "Speed ${store.speed.label}" }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        }
        Tip("Play one week") {
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                store.advance(1)
            },
            enabled = enabled && !store.playing,
        ) { Icon(Icons.Filled.SkipNext, "Next week") }
        }
        Tip(if (store.playing) "Pause" else "Let time run. It stops by itself when something needs you.") {
        Surface(
            onClick = {
                haptics.performHapticFeedback(if (store.playing) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                store.togglePlay()
            },
            enabled = enabled || store.playing,
            shape = RoundedCornerShape(corner),
            color = if (enabled || store.playing) MaterialTheme.colorScheme.primary else gameColors.fill,
            contentColor = if (enabled || store.playing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (store.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (store.playing) "Pause" else "Play",
                    modifier = Modifier.size(26.dp),
                )
            }
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
    Box(Modifier.fillMaxWidth().height(2.dp)) {
        Box(
            Modifier
                .fillMaxWidth(progress.value)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (store.playing) 0.9f else 0f)),
        )
    }
}

/** Height of the floating tab bar plus the gap under it, for content to clear. */
private val FloatingBarClearance = 68.dp + 12.dp

/**
 * The tabs, on a pill that floats over the content: content scrolls underneath, fading out just
 * behind it. Snackbars sit above it.
 */
@Composable
private fun FloatingTabBar(store: GameStore, modifier: Modifier, snackbar: SnackbarHostState) {
    val colors = gameColors
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SnackbarHost(snackbar)
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.grouped.copy(alpha = 0f), colors.grouped.copy(alpha = 0.92f)))),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.bar,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, colors.separator),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    for (t in tabs) FloatingTab(t, store.tab == t.tab, Modifier.weight(1f)) { store.tab = t.tab }
                }
            }
        }
    }
}

/** One tab: the icon on an expressive pill when selected, the label underneath. */
@Composable
private fun FloatingTab(t: TabSpec, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val indicator by animateColorAsState(if (selected) scheme.primaryContainer else Color.Transparent, motion.defaultEffectsSpec(), label = "tab")
    val width by animateDpAsState(if (selected) 56.dp else 32.dp, motion.defaultSpatialSpec(), label = "tabWidth")
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.width(width).height(30.dp).background(indicator, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            HudIcon(t.glyph, if (selected) scheme.primary else scheme.onSurfaceVariant, 18.dp)
        }
        FitText(
            t.label,
            MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Standard scrolling body for a tab: grouped sections on the grouped background. */
@Composable
fun ScreenColumn(content: LazyListScope.() -> Unit) {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingBarClearance + 32.dp
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(28.dp),
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
