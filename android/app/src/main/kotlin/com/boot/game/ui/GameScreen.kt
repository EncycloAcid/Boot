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
import com.boot.core.GameProject
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

/** True when a tab body is hosted inside a floating window / phone sheet. */
val LocalInFloatingPanel = compositionLocalOf { false }

@Composable
fun BootApp(store: GameStore) {
    val state = store.state
    when {
        state == null -> NewGameScreen(store)
        store.layout == ScreenLayout.VERTICAL -> ClassicGameScreen(store, state)
        // Sideways on a phone gets its own layout; tablets and unlocked screens keep the floating one.
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            if (isLandscape(maxWidth, maxHeight)) LandscapeGameScreen(store, state) else GameScreen(store, state)
        }
    }
}

internal data class TabSpec(val tab: Tab, val label: String, val glyph: HudGlyph)

internal val dockTabs = listOf(
    TabSpec(Tab.OFFICE, "Office", HudGlyph.OFFICE),
    TabSpec(Tab.LANES, "Lanes", HudGlyph.LANES),
    TabSpec(Tab.RESEARCH, "Research", HudGlyph.RESEARCH),
    TabSpec(Tab.TEAM, "Team", HudGlyph.TEAM),
    TabSpec(Tab.COMPANY, "Company", HudGlyph.COMPANY),
)

/** A phone held sideways: wide and short. The dock becomes a rail and windows open beside it. */
internal fun isLandscape(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) = width > height && height < 600.dp

/** Width of the sideways rail, for everything else to clear. */
private val RailWidth = 84.dp

/** Display-only season label (matches the Windows calendar without requiring core seasons). */
internal fun seasonTitle(week: Int): String =
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
    val needsToday = state.tutorial != null ||
        nextStep(state)?.let { it.key != store.dismissedStep && (state.tutorial == null || it.key in urgentSteps) } == true ||
        state.inbox.isNotEmpty()

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

    val panelOpen = store.tab != Tab.OFFICE || store.todayOpen
    BackHandler(enabled = panelOpen) {
        when {
            store.tab != Tab.OFFICE -> store.tab = Tab.OFFICE
            store.todayOpen -> store.todayOpen = false
        }
    }

    // The floating pieces sit on their own backgrounds, not in a Surface: give their text the
    // theme's colour so it reads at night.
    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.grouped)) {
        val phone = maxWidth < 600.dp
        val landscape = isLandscape(maxWidth, maxHeight)
        // Discs are always on now — keep the office art clear of the HUD stack.
        val insetTop = when {
            landscape -> 64.dp
            phone -> 148.dp
            else -> 188.dp
        }
        // Office is always the main plane — panels float over it.
        Box(Modifier.fillMaxSize()) {
            OfficeScene(
                state,
                store.playing,
                Modifier
                    .fillMaxSize()
                    .clickable { store.tab = Tab.TEAM },
                insetTop = insetTop,
            )
            DeskChip(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = if (landscape) RailWidth + 8.dp else 16.dp, bottom = if (landscape) 12.dp else 96.dp),
            ) { store.tab = Tab.TEAM }
        }

        FloatingHud(
            store = store,
            state = state,
            todayOpen = store.todayOpen,
            onToggleToday = { store.todayOpen = !store.todayOpen },
            onAbandon = { confirmAbandon = true },
        )

        FloatingPanels(store, state, store.todayOpen) { store.todayOpen = false }

        FloatingDock(
            landscape = landscape,
            store = store,
            todayOpen = store.todayOpen,
            onOffice = {
                if (store.tab == Tab.OFFICE) {
                    store.todayOpen = !store.todayOpen
                } else {
                    store.tab = Tab.OFFICE
                    // Returning to the stage keeps Today closed so the office stays visible.
                    store.todayOpen = false
                }
            },
            onOpenOther = { tab ->
                store.todayOpen = false
                store.tab = if (store.tab == tab) Tab.OFFICE else tab
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = snackbar,
        )

        WeekProgress(
            store,
            state,
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 2.dp),
        )
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
 * Whatever sits above the game, in either layout. Decisions stop the clock; endings beat the
 * Pivot Point, which beats ordinary events, then reviews, then the designers and the sheets.
 */
@Composable
internal fun GameOverlays(store: GameStore, state: GameState) {
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
}

@Composable
private fun FloatingPanels(
    store: GameStore,
    state: GameState,
    todayOpen: Boolean,
    onCloseToday: () -> Unit,
) {
    val open = store.tab != Tab.OFFICE || todayOpen
    var lastTab by remember { mutableStateOf(Tab.LANES) }
    LaunchedEffect(store.tab) {
        if (store.tab != Tab.OFFICE) lastTab = store.tab
    }
    val showingToday = store.tab == Tab.OFFICE && todayOpen
    val shown = if (store.tab != Tab.OFFICE) store.tab else lastTab
    val title = when {
        showingToday -> "Today"
        shown == Tab.LANES -> "Lanes"
        shown == Tab.RESEARCH -> "Research"
        shown == Tab.TEAM -> "Team"
        shown == Tab.COMPANY -> "Company"
        shown == Tab.NEWS -> "News"
        else -> ""
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = isLandscape(maxWidth, maxHeight)
        val phone = maxWidth < 600.dp && !landscape
        val panelCap = maxHeight * 0.72f
        val sheetWidth = minOf(460.dp, maxWidth * 0.52f)
        // Leave the floating HUD clear: generous top inset, dock clearance below.
        val frame = if (landscape) {
            // Sideways: a full-height sheet beside the rail; the status column and play stay clear on the right.
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                // Below the Menu chip, so the menu stays reachable with a window open.
                .padding(start = RailWidth, top = 52.dp, bottom = 6.dp)
        } else if (phone) {
            Modifier
                .fillMaxSize()
                // Below the HUD's week and play controls, so time can still be run with a panel open.
                .padding(start = 10.dp, end = 10.dp, top = 268.dp, bottom = 96.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 132.dp, bottom = 108.dp)
        }
        Box(frame, contentAlignment = if (landscape) Alignment.TopStart else if (phone) Alignment.BottomCenter else Alignment.Center) {
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
                    modifier = if (landscape) {
                        Modifier
                            .width(sheetWidth)
                            .fillMaxHeight()
                    } else if (phone) {
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
                color = MaterialTheme.colorScheme.onSurface,
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
 * Resource discs (Design / Tech / RP / Bugs) stay visible even when idle — Windows parity.
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
    val project = state.games?.project ?: state.games?.project2
    val projectTeam = when {
        state.games?.project != null -> 1
        state.games?.project2 != null -> 2
        else -> 1
    }
    val unread = state.news.count { it.id > store.newsSeen }
    val inbox = state.inbox.size
    val attention = unread + inbox + if (state.tutorial != null) 1 else 0
    val season = seasonTitle(state.week)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        val narrow = maxWidth < 420.dp
        val landscape = isLandscape(maxWidth, maxHeight)
        val bubble = if (narrow || landscape) 40.dp else 48.dp

        // Menu — badge for news, inbox, or tutorial waiting
        Box(Modifier.align(Alignment.TopStart)) {
            BadgedBox(badge = { if (attention > 0) Badge() }) {
                SoftChip(
                    onClick = { menuOpen = true },
                    description = if (attention > 0) "Menu, $attention waiting" else "Menu",
                ) {
                    HudIcon(HudGlyph.MENU, colors.other, 14.dp)
                    Text("Menu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            buildString {
                                append(if (todayOpen) "Hide Today" else "Today")
                                if (inbox > 0) append(" · $inbox inbox")
                                if (state.tutorial != null) append(" · lesson")
                            },
                        )
                    },
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
                DropdownMenuItem(text = { Text("Play upright (classic)") }, onClick = {
                    menuOpen = false
                    store.chooseLayout(ScreenLayout.VERTICAL)
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

        // Center: always-on discs + tip / project card (clear Menu left + status pills right)
        Column(
            if (landscape) {
                // Sideways: between the rail and the status column, at a readable width.
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = RailWidth, end = 200.dp)
                    .widthIn(max = 300.dp)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = if (narrow) maxWidth - 8.dp else 340.dp)
                    .padding(start = if (narrow) 72.dp else 64.dp, end = if (narrow) 116.dp else 64.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ResourceDiscRow(store, state, project, bubble, narrow)
            if (project != null) {
                ProjectSoftPanel(store, state, project, projectTeam, todayOpen, onToggleToday)
            } else {
                IdleSoftPanel(store, state, season, todayOpen, onToggleToday)
            }
        }

        // Right: status + transport — always show cash/wk, fans, rep, date, founder
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .widthIn(max = if (narrow) 148.dp else if (landscape) 200.dp else 176.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Tip("Calendar week and season") {
                StatusPill(HudGlyph.CALENDAR, MaterialTheme.colorScheme.primary, "${state.dateLabel} · $season")
            }
            val fans = state.games?.fans
            if (fans != null && Lane.GAMES in state.lanes) {
                Tip("Fans buy your games at launch. Tap for your catalogue.") {
                    StatusPill(HudGlyph.FANS, Color(0xFFC44D8A), "${Format.count(fans)} Fans") {
                        store.openStudio(StudioPage.GAMES)
                    }
                }
            }
            Tip("Reputation out of 100. Scandals leave scars.") {
                StatusPill(HudGlyph.FANS, colors.warn, "Rep ${state.reputation.score.toInt()}") {
                    store.companyPage = 0
                    store.tab = Tab.COMPANY
                }
            }
            Tip("Cash in the bank, and weekly profit or loss lately. Tap for money.") {
                StatusPill(
                    HudGlyph.CASH,
                    colors.good,
                    Format.money(state.cash),
                    detail = "${Format.signedMoney(weekly)}/wk",
                    detailColor = if (weekly >= 0) colors.good else colors.bad,
                    valueColor = if (state.cash < 0) colors.bad else colors.good,
                ) {
                    store.companyPage = 1
                    store.tab = Tab.COMPANY
                }
            }
            state.listing?.let { listing ->
                Tip("${listing.ticker} share price. Tap for company overview.") {
                    StatusPill(
                        HudGlyph.COMPANY,
                        MaterialTheme.colorScheme.primary,
                        "${listing.ticker} $${String.format(Locale.US, "%.0f", listing.price)}",
                    ) {
                        store.companyPage = 0
                        store.tab = Tab.COMPANY
                    }
                }
            }
            val speed: @Composable () -> Unit = {
                TinyChip(store.speed.label, description = "Speed ${store.speed.label}") {
                    store.speed = Speed.entries[(store.speed.ordinal + 1) % Speed.entries.size]
                }
            }
            val nextWeek: @Composable () -> Unit = {
                TinyChip(
                    "Week",
                    enabled = store.canAdvance && !store.playing && !store.holdsClock,
                    description = "Next week",
                ) {
                    store.advance(1)
                }
            }
            val play: @Composable () -> Unit = {
                TinyChip(
                    if (store.playing) "Pause" else "Play",
                    filled = true,
                    enabled = store.canAdvance || store.playing,
                    description = if (store.playing) "Pause" else "Play",
                ) { store.togglePlay() }
            }
            if (landscape) {
                // Sideways there's room across but not down: the clock on one line.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    speed()
                    nextWeek()
                    play()
                }
            } else {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        speed()
                        nextWeek()
                    }
                    play()
                }
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .clickable { store.tab = Tab.TEAM }
                    .semantics { contentDescription = "You, ${state.founder.name}" },
            ) { FounderPortrait(state, if (narrow) 30.dp else 36.dp) }
        }
    }
}

@Composable
private fun ResourceDiscRow(
    store: GameStore,
    state: GameState,
    project: GameProject?,
    bubble: androidx.compose.ui.unit.Dp,
    narrow: Boolean,
) {
    val colors = gameColors
    val design = project?.design?.toInt() ?: 0
    val tech = project?.tech?.toInt() ?: 0
    val bugs = project?.bugs?.toInt() ?: 0
    val gap = if (narrow) 4.dp else 8.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Top,
    ) {
        LabeledDisc("$design", Color(0xFFF08A24), HudGlyph.DESIGN, "Design", "Design points on the active project. Tap to open Studio.", bubble) {
            store.openStudio(StudioPage.MAKE)
        }
        LabeledDisc("$tech", Color(0xFFE8B923), HudGlyph.TECH, "Tech", "Tech points on the active project. Tap to open Studio.", bubble) {
            store.openStudio(StudioPage.MAKE)
        }
        if (!narrow) Spacer(Modifier.width(4.dp))
        LabeledDisc(
            "${state.researchPoints.toInt()}",
            colors.milestone,
            HudGlyph.RESEARCH,
            "RP",
            "Research points to spend. +${String.format(Locale.US, "%.1f", state.researchPerWeek)} a week. Tap for Research.",
            bubble,
        ) {
            store.tab = Tab.RESEARCH
        }
        LabeledDisc("$bugs", colors.bad, HudGlyph.BUGS, "Bugs", "Open bugs on the active project. Tap to open Studio.", bubble) {
            store.openStudio(StudioPage.MAKE)
        }
    }
}

@Composable
private fun LabeledDisc(
    value: String,
    fill: Color,
    glyph: HudGlyph,
    label: String,
    tip: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Tip(tip) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ResourceDisc(value, fill, glyph, label, size, onClick)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun ProjectSoftPanel(
    store: GameStore,
    state: GameState,
    project: GameProject,
    team: Int,
    todayOpen: Boolean,
    onToggleToday: () -> Unit,
) {
    val colors = gameColors
    val stage = if (project.isReady) "Polish" else listOf("Foundations", "Content", "Presentation")[project.stage]
    val forecast = state.gameForecast(team)
    val eta = forecast?.weeksToReady
    val etaLabel = when {
        project.isReady -> forecast?.let { String.format(Locale.US, "Ship ~%.1f/10", it.expectedScore) } ?: "Ready to polish"
        eta == null -> Format.weeks(project.weeksInDev) + " in"
        eta <= 0 -> "Ready this week"
        else -> "~${Format.weeks(eta)} left"
    }
    SoftPanel(
        Modifier
            .fillMaxWidth()
            .clickable {
                store.tab = Tab.OFFICE
                if (!todayOpen) onToggleToday()
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stage,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Text(
                if (team == 2) "Team 2" else state.companyName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(etaLabel, style = MaterialTheme.typography.labelSmall, color = colors.milestone, maxLines = 1)
        }
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
}

@Composable
private fun IdleSoftPanel(
    store: GameStore,
    state: GameState,
    season: String,
    todayOpen: Boolean,
    onToggleToday: () -> Unit,
) {
    val crowded = state.staff.size > state.office.desks
    val avgMorale = state.staff.takeIf { it.isNotEmpty() }?.map { it.morale }?.average()
    val tip = when {
        todayOpen -> "Today is open — Close or tap Office"
        state.tutorial != null -> "Lesson waiting — open Today"
        state.inbox.isNotEmpty() -> "${state.inbox.size} in inbox — open Today"
        else -> "$season · tap Office for Today"
    }
    SoftPanel(
        Modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .clickable {
                store.tab = Tab.OFFICE
                if (!todayOpen) onToggleToday()
            },
    ) {
        Text(state.companyName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${state.world.era.title.removePrefix("The ")} · ${state.office.title}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            buildString {
                append("+${String.format(Locale.US, "%.1f", state.researchPerWeek)} RP/wk")
                append(" · ${state.staff.size}/${state.office.desks} desks")
                if (crowded) append(" · crowded")
                avgMorale?.let { append(" · morale ${String.format(Locale.US, "%.0f", it * 100)}%") }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(tip, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** Desk count over the office stage — restores the old SceneChip glance. */
@Composable
internal fun DeskChip(state: GameState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = gameColors
    val crowded = state.staff.size > state.office.desks
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.92f))
            .border(1.dp, colors.separator, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HudIcon(HudGlyph.TEAM, if (crowded) colors.bad else MaterialTheme.colorScheme.onSurface, 14.dp)
        Text(
            "${state.staff.size}/${state.office.desks} desks" + if (crowded) " · crowded" else "",
            style = MaterialTheme.typography.labelLarge,
            color = if (crowded) colors.bad else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
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
    detail: String? = null,
    detailColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .widthIn(max = 168.dp)
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .background(colors.bar.copy(alpha = 0.94f))
            .border(1.dp, colors.separator, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
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
        Column(
            Modifier.weight(1f, fill = false),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = valueColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = detailColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
internal fun TinyChip(
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
    onOpenOther: (Tab) -> Unit,
    modifier: Modifier,
    snackbar: SnackbarHostState,
    landscape: Boolean = false,
) {
    val colors = gameColors
    val shape = RoundedCornerShape(18.dp)
    if (landscape) {
        // Sideways: a rail down the left edge, under the Menu chip; snackbars at the bottom.
        Box(Modifier.fillMaxSize()) {
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp))
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 8.dp, top = 52.dp, bottom = 6.dp)
                    .width(RailWidth - 14.dp)
                    .shadow(12.dp, shape, clip = false)
                    .clip(shape)
                    .background(colors.bar.copy(alpha = 0.96f))
                    .border(1.dp, colors.separator, shape)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (t in dockTabs) {
                    val onThis = store.tab == t.tab
                    FloatingDockTab(
                        t = t,
                        selected = onThis,
                        emphasized = if (t.tab == Tab.OFFICE) onThis && todayOpen else onThis,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true,
                    ) {
                        when (t.tab) {
                            Tab.OFFICE -> onOffice()
                            else -> onOpenOther(t.tab)
                        }
                    }
                }
            }
        }
        return
    }
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
                for (t in dockTabs) {
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
                            else -> onOpenOther(t.tab)
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
    compact: Boolean = false,
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
            .padding(vertical = if (compact) 5.dp else 8.dp),
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
internal fun WeekProgress(store: GameStore, state: GameState, modifier: Modifier = Modifier) {
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

/** A tab's own title, at the top of its scrolling content. Inside floating panels the window title already shows — keep only the useful subtitle. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null) {
    val inPanel = LocalInFloatingPanel.current
    if (inPanel) {
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        return
    }
    Column(Modifier.padding(horizontal = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMediumEmphasized)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
