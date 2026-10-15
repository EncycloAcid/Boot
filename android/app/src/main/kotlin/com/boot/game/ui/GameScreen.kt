package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.boot.game.StudioPage
import com.boot.game.Tab

/** True when a tab body is hosted inside a floating window / phone sheet. */
val LocalInFloatingPanel = compositionLocalOf { false }

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

/** Display-only season label (matches the Windows calendar without requiring core seasons). */
private fun seasonTitle(week: Int): String =
    listOf("Spring", "Summer", "Autumn", "Winter")[((week.coerceAtLeast(1) - 1) % 52) / 13]

/**
 * Windows-style shell on Android: fullscreen office stage, floating HUD, floating panels for
 * Lanes / Research / Team / Company / News / Today, soft dock with re-tap-to-close.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameScreen(store: GameStore, state: GameState) {
    val colors = gameColors
    val snackbar = remember { SnackbarHostState() }
    var confirmAbandon by remember { mutableStateOf(false) }
    var todayOpen by remember { mutableStateOf(false) }
    val needsToday = state.tutorial != null ||
        nextStep(state)?.let { it.key != store.dismissedStep && (state.tutorial == null || it.key in urgentSteps) } == true ||
        state.inbox.isNotEmpty()

    LaunchedEffect(state.week, needsToday) {
        if (needsToday && store.tab == Tab.OFFICE) todayOpen = true
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

    val panelOpen = store.tab != Tab.OFFICE || todayOpen
    BackHandler(enabled = panelOpen) {
        when {
            store.tab != Tab.OFFICE -> store.tab = Tab.OFFICE
            todayOpen -> todayOpen = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.grouped)) {
        val phone = maxWidth < 600.dp
        val project = state.games?.project != null
        val insetTop = when {
            phone && project -> 132.dp
            phone -> 96.dp
            project -> 168.dp
            else -> 120.dp
        }
        // Office is always the main plane — panels float over it.
        OfficeScene(
            state,
            store.playing,
            Modifier
                .fillMaxSize()
                .clickable { store.tab = Tab.TEAM },
            insetTop = insetTop,
        )

        FloatingHud(
            store = store,
            state = state,
            todayOpen = todayOpen,
            onToggleToday = { todayOpen = !todayOpen },
            onAbandon = { confirmAbandon = true },
        )

        FloatingPanels(store, state, todayOpen) { todayOpen = false }

        FloatingDock(
            store = store,
            todayOpen = todayOpen,
            onOffice = {
                if (store.tab == Tab.OFFICE) {
                    todayOpen = !todayOpen
                } else {
                    store.tab = Tab.OFFICE
                    // Returning to the stage keeps Today closed so the office stays visible.
                    todayOpen = false
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = snackbar,
        )

        WeekProgress(store, state, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }

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

@Composable
private fun FloatingPanels(
    store: GameStore,
    state: GameState,
    todayOpen: Boolean,
    onCloseToday: () -> Unit,
) {
    val open = store.tab != Tab.OFFICE || todayOpen
    var shown by remember { mutableStateOf(Tab.LANES) }
    var showingToday by remember { mutableStateOf(false) }
    if (store.tab != Tab.OFFICE) {
        shown = store.tab
        showingToday = false
    } else if (todayOpen) {
        showingToday = true
    }
    val title = when {
        showingToday && store.tab == Tab.OFFICE -> "Today"
        shown == Tab.LANES -> "Lanes"
        shown == Tab.RESEARCH -> "Research"
        shown == Tab.TEAM -> "Team"
        shown == Tab.COMPANY -> "Company"
        shown == Tab.NEWS -> "News"
        else -> ""
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val phone = maxWidth < 600.dp
        val panelCap = maxHeight * 0.72f
        // Leave the floating HUD clear: generous top inset, dock clearance below.
        val frame = if (phone) {
            Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 118.dp, bottom = 96.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 132.dp, bottom = 108.dp)
        }
        Box(frame, contentAlignment = if (phone) Alignment.BottomCenter else Alignment.Center) {
            AnimatedVisibility(
                visible = open,
                enter = fadeIn() + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + scaleOut(targetScale = 0.96f),
            ) {
                FloatingWindow(
                    title = title,
                    onClose = {
                        if (store.tab != Tab.OFFICE) store.tab = Tab.OFFICE else onCloseToday()
                    },
                    phoneSheet = phone,
                    modifier = if (phone) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.92f)
                    } else {
                        Modifier
                            .widthIn(min = 360.dp, max = 560.dp)
                            .fillMaxWidth(0.62f)
                            .heightIn(min = 280.dp, max = panelCap)
                            .fillMaxHeight(0.68f)
                    },
                ) {
                    CompositionLocalProvider(LocalInFloatingPanel provides true) {
                        when {
                            showingToday && store.tab == Tab.OFFICE -> OfficeScreen(store, state)
                            shown == Tab.LANES -> LanesScreen(store, state)
                            shown == Tab.RESEARCH -> ResearchScreen(store, state)
                            shown == Tab.TEAM -> TeamScreen(store, state)
                            shown == Tab.COMPANY -> CompanyScreen(store, state)
                            shown == Tab.NEWS -> NewsScreen(store, state)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

/** Soft white floating window — title + Close; phone gets a drag handle for sheet readability. */
@Composable
fun FloatingWindow(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    phoneSheet: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(if (phoneSheet) 22.dp else 16.dp)
    Column(
        modifier
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.97f))
            .border(1.dp, colors.separator, shape),
    ) {
        if (phoneSheet) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.separator),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.fill)
                .padding(start = 14.dp, end = 8.dp, top = if (phoneSheet) 6.dp else 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Close",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

/**
 * Floating HUD: Menu · resource discs · tip/project card · status pills · transport.
 * Keeps Boot's colors/type; layout mirrors the Windows desktop chrome.
 */
@Composable
private fun FloatingHud(
    store: GameStore,
    state: GameState,
    todayOpen: Boolean,
    onToggleToday: () -> Unit,
    onAbandon: () -> Unit,
) {
    val colors = gameColors
    var menuOpen by remember { mutableStateOf(false) }
    val weekly = state.trailingProfit(weeks = 4) / 4
    val project = state.games?.project
    val unread = state.news.count { it.id > store.newsSeen }
    val season = seasonTitle(state.week)
    val roomy = LocalDensity.current.fontScale <= 1.15f

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        val narrow = maxWidth < 420.dp
        val bubble = if (narrow) 44.dp else 50.dp

        // Menu
        Box(Modifier.align(Alignment.TopStart)) {
            BadgedBox(badge = { if (unread > 0) Badge() }) {
                SoftChip(onClick = { menuOpen = true }, description = if (unread > 0) "Menu, $unread new" else "Menu") {
                    HudIcon(HudGlyph.MENU, colors.other, 14.dp)
                    Text("Menu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (todayOpen) "Hide Today" else "Today") },
                    onClick = {
                        menuOpen = false
                        store.tab = Tab.OFFICE
                        onToggleToday()
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
                DropdownMenuItem(text = { Text(if (store.playing) "Pause" else "Play") }, onClick = {
                    menuOpen = false
                    store.togglePlay()
                })
                DropdownMenuItem(
                    text = { Text("Advance week") },
                    onClick = {
                        menuOpen = false
                        if (store.canAdvance && !store.playing) store.advance(1)
                    },
                    enabled = store.canAdvance && !store.holdsClock && !store.playing,
                )
                DropdownMenuItem(text = { Text("Abandon run and start over") }, onClick = {
                    menuOpen = false
                    store.pause()
                    onAbandon()
                })
            }
        }

        // Center: discs + tip / project
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (narrow) maxWidth - 8.dp else 320.dp)
                .padding(horizontal = if (narrow) 0.dp else 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (project != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ResourceDisc("${project.design.toInt()}", Color(0xFFF08A24), HudGlyph.DESIGN, "Design", bubble) {
                        store.openStudio(StudioPage.MAKE)
                    }
                    ResourceDisc("${project.tech.toInt()}", Color(0xFFE8B923), HudGlyph.TECH, "Tech", bubble) {
                        store.openStudio(StudioPage.MAKE)
                    }
                    Spacer(Modifier.width(6.dp))
                    ResourceDisc("${state.researchPoints.toInt()}", colors.milestone, HudGlyph.RESEARCH, "Research", bubble) {
                        store.tab = Tab.RESEARCH
                    }
                    ResourceDisc("${project.bugs.toInt()}", colors.bad, HudGlyph.BUGS, "Bugs", bubble) {
                        store.openStudio(StudioPage.MAKE)
                    }
                }
                SoftPanel(Modifier.fillMaxWidth()) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${project.concept.topic.title} / ${project.concept.genreTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    ProgressRow("Progress", project.completion.toFloat(), MaterialTheme.colorScheme.primary)
                    ProgressRow("Hype ${project.hype.toInt()}", (project.hype / 100.0).toFloat().coerceIn(0f, 1f), colors.warn)
                }
            } else {
                SoftPanel(Modifier.widthIn(max = 280.dp).fillMaxWidth()) {
                    Text(state.companyName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${state.world.era.title.removePrefix("The ")} · ${state.office.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$season · tap Office for Today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // Right: status + transport
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .widthIn(max = if (narrow) 132.dp else 168.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End,
        ) {
            StatusPill(HudGlyph.CALENDAR, MaterialTheme.colorScheme.primary, "${state.dateLabel} · $season")
            val fans = state.games?.fans
            if (fans != null && Lane.GAMES in state.lanes) {
                StatusPill(HudGlyph.FANS, Color(0xFFC44D8A), "${Format.count(fans)} Fans") {
                    store.openStudio(StudioPage.GAMES)
                }
            } else {
                StatusPill(HudGlyph.FANS, colors.warn, "Rep ${state.reputation.score.toInt()}") {
                    store.companyPage = 0
                    store.tab = Tab.COMPANY
                }
            }
            StatusPill(
                HudGlyph.CASH,
                colors.good,
                Format.money(state.cash),
                valueColor = if (state.cash < 0) colors.bad else colors.good,
            ) {
                store.companyPage = 1
                store.tab = Tab.COMPANY
            }
            if (!narrow || roomy) {
                Text(
                    "${Format.signedMoney(weekly)}/wk",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (weekly >= 0) colors.good else colors.bad,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TinyChip(store.speed.label, description = "Speed ${store.speed.label}") {
                        store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                    }
                    TinyChip(
                        "Week",
                        enabled = store.canAdvance && !store.playing && !store.holdsClock,
                        description = "Next week",
                    ) {
                        store.advance(1)
                    }
                }
                TinyChip(
                    if (store.playing) "Pause" else "Play",
                    filled = true,
                    enabled = store.canAdvance || store.playing,
                    description = if (store.playing) "Pause" else "Play",
                ) { store.togglePlay() }
            }
            if (!narrow) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable { store.tab = Tab.TEAM }
                        .semantics { contentDescription = "You, ${state.founder.name}" },
                ) { FounderPortrait(state, 36.dp) }
            }
        }
    }
}

@Composable
private fun SoftChip(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    val colors = gameColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.94f))
            .border(1.dp, colors.separator, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

@Composable
private fun SoftPanel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = gameColors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.94f))
            .border(1.dp, colors.separator, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun StatusPill(
    glyph: HudGlyph,
    tint: Color,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.94f))
            .border(1.dp, colors.separator, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { HudIcon(glyph, tint, 12.dp) }
        Text(label, style = MaterialTheme.typography.labelLarge, color = valueColor, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun ResourceDisc(
    value: String,
    fill: Color,
    glyph: HudGlyph,
    description: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(fill, Color(fill.red * 0.88f, fill.green * 0.88f, fill.blue * 0.88f))))
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HudIcon(glyph, Color.White, size * 0.34f)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.24f).sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProgressRow(label: String, value: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = gameColors.fill,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TinyChip(
    label: String,
    filled: Boolean = false,
    enabled: Boolean = true,
    description: String = label,
    onClick: () -> Unit,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(12.dp)
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .heightIn(min = 36.dp)
            .shadow(if (filled && enabled) 4.dp else 2.dp, shape, clip = false)
            .clip(shape)
            .background(
                when {
                    filled && enabled -> MaterialTheme.colorScheme.primary
                    else -> colors.bar.copy(alpha = 0.94f)
                },
            )
            .border(1.dp, if (filled && enabled) MaterialTheme.colorScheme.primary else colors.separator, shape)
            .clickable(enabled = enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                filled && enabled -> MaterialTheme.colorScheme.onPrimary
                enabled -> MaterialTheme.colorScheme.onSurface
                else -> colors.other
            },
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Soft dock — re-tap closes the open panel back to Office. */
@Composable
private fun FloatingDock(
    store: GameStore,
    todayOpen: Boolean,
    onOffice: () -> Unit,
    modifier: Modifier,
    snackbar: SnackbarHostState,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(18.dp)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SnackbarHost(snackbar)
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, shape, clip = false)
                    .clip(shape)
                    .background(colors.bar.copy(alpha = 0.96f))
                    .border(1.dp, colors.separator, shape)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                for (t in tabs) {
                    val onThis = store.tab == t.tab
                    // Office stays selected on the stage; underline only while the Today panel is open.
                    val emphasized = when (t.tab) {
                        Tab.OFFICE -> store.tab == Tab.OFFICE && todayOpen
                        else -> onThis
                    }
                    FloatingDockTab(
                        t = t,
                        selected = onThis,
                        emphasized = emphasized,
                        modifier = Modifier.weight(1f),
                    ) {
                        when (t.tab) {
                            Tab.OFFICE -> onOffice()
                            else -> {
                                store.tab = when {
                                    store.tab == t.tab -> Tab.OFFICE
                                    else -> t.tab
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingDockTab(
    t: TabSpec,
    selected: Boolean,
    emphasized: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val tint by animateColorAsState(
        if (selected) scheme.primary else scheme.onSurfaceVariant,
        motion.defaultEffectsSpec(),
        label = "dockTint",
    )
    val bg by animateColorAsState(
        if (emphasized) scheme.primaryContainer else Color.Transparent,
        motion.defaultEffectsSpec(),
        label = "dockBg",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        HudIcon(t.glyph, tint, 20.dp)
        FitText(
            t.label,
            MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            Modifier
                .width(if (emphasized) 16.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (emphasized) scheme.primary else Color.Transparent),
        )
    }
}

@Composable
private fun WeekProgress(store: GameStore, state: GameState, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(state.week, store.playing, store.speed) {
        progress.snapTo(0f)
        if (store.playing) progress.animateTo(1f, tween(store.speed.millis.toInt(), easing = LinearEasing))
    }
    Box(modifier.fillMaxWidth().height(2.dp)) {
        Box(
            Modifier
                .fillMaxWidth(progress.value)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (store.playing) 0.9f else 0f)),
        )
    }
}

/** Height of the floating dock plus gap, for full-page scroll content to clear. */
private val FloatingBarClearance = 76.dp + 12.dp

/** Standard scrolling body for a tab: grouped sections, tighter inside floating panels. */
@Composable
fun ScreenColumn(content: LazyListScope.() -> Unit) {
    val inPanel = LocalInFloatingPanel.current
    val bottom = if (inPanel) {
        24.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + FloatingBarClearance + 32.dp
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().then(if (inPanel) Modifier.background(gameColors.grouped.copy(alpha = 0.55f)) else Modifier),
        contentPadding = PaddingValues(
            start = if (inPanel) 12.dp else 16.dp,
            end = if (inPanel) 12.dp else 16.dp,
            top = if (inPanel) 12.dp else 16.dp,
            bottom = bottom,
        ),
        verticalArrangement = Arrangement.spacedBy(if (inPanel) 20.dp else 28.dp),
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
