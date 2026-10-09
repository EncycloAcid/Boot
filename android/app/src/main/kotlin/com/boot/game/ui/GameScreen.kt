package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.game.GameStore
import com.boot.game.Tab

@Composable
fun BootApp(store: GameStore) {
    val state = store.state
    if (state == null) NewGameScreen(store) else GameScreen(store, state)
}

private data class TabSpec(val tab: Tab, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabSpec(Tab.HOME, "Home", Icons.Filled.Dashboard),
    TabSpec(Tab.LANES, "Lanes", Icons.Filled.Work),
    TabSpec(Tab.RESEARCH, "Research", Icons.Filled.Science),
    TabSpec(Tab.PEOPLE, "People", Icons.Filled.Groups),
    TabSpec(Tab.COMPANY, "Company", Icons.Filled.AccountBalance),
)

private fun GameState.title(tab: Tab): String = when (tab) {
    Tab.HOME -> companyName
    Tab.LANES -> "Lanes"
    Tab.PEOPLE -> "People"
    Tab.RESEARCH -> "Research"
    Tab.COMPANY -> "Company"
    Tab.NEWS -> "News"
}

/** Lists scroll under the floating toolbar; leave room so the last row isn't hidden. */
private val ToolbarClearance = 104.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameScreen(store: GameStore, state: GameState) {
    val colors = gameColors
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }
    // Each tab gets its own large title that collapses as you scroll, like iOS.
    val barState = remember(store.tab) { TopAppBarState(-Float.MAX_VALUE, 0f, 0f) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = barState)

    LaunchedEffect(store.error) {
        store.error?.let {
            snackbar.showSnackbar(it)
            store.error = null
        }
    }
    BackHandler(enabled = store.tab != Tab.HOME) { store.tab = Tab.HOME }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = colors.grouped,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(state.title(store.tab), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                subtitle = { Text("${state.dateLabel} · ${state.world.era.title}", maxLines = 1) },
                actions = {
                    IconButton(onClick = { store.tab = Tab.NEWS }) { Icon(Icons.Filled.Newspaper, "News") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreHoriz, "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Abandon run and start over") }, onClick = {
                                menuOpen = false
                                confirmAbandon = true
                            })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.grouped, scrolledContainerColor = colors.bar),
                scrollBehavior = scroll,
            )
        },
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
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = ToolbarClearance - 16.dp)) },
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
                    Tab.HOME -> DashboardScreen(store, state)
                    Tab.LANES -> LanesScreen(store, state)
                    Tab.PEOPLE -> PeopleScreen(store, state)
                    Tab.RESEARCH -> ResearchScreen(store, state)
                    Tab.COMPANY -> CompanyScreen(store, state)
                    Tab.NEWS -> NewsScreen(state)
                }
            }
            TimeToolbar(
                store, state,
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset),
            )
        }
    }

    // Decisions block the calendar. Endings beat the Pivot Point, which beats ordinary events.
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
 * Time lives in a floating toolbar in the Material expressive style: a shadowed pill with cash on
 * the left, then a quarter, a month, and the big next-week button. It stops by itself whenever a
 * decision needs you. (Material's own HorizontalFloatingToolbar is sized for icon buttons and
 * balloons around labelled ones, so this is the same pill built directly.)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeToolbar(store: GameStore, state: GameState, modifier: Modifier) {
    val colors = gameColors
    val haptics = LocalHapticFeedback.current
    val enabled = store.canAdvance
    val runway = state.runwayWeeks
    val motion = MaterialTheme.motionScheme
    fun advance(weeks: Int) {
        haptics.performHapticFeedback(if (weeks == 1) HapticFeedbackType.Confirm else HapticFeedbackType.LongPress)
        store.advance(weeks)
    }
    Surface(
        modifier = modifier.padding(horizontal = 12.dp),
        shape = CircleShape,
        color = colors.bar,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                AnimatedContent(
                    targetState = Format.money(state.cash),
                    transitionSpec = {
                        slideInVertically(motion.fastSpatialSpec()) { -it } + fadeIn() togetherWith
                            slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut()
                    },
                    label = "cash",
                ) { cash ->
                    Text(
                        cash,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.cash < 0) colors.bad else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
                Text(
                    when {
                        !runway.isFinite() -> "Profitable"
                        runway > 260 -> "5+ yrs left"
                        runway >= 52 -> String.format(java.util.Locale.US, "%.1f yrs left", runway / 52)
                        else -> "${runway.toInt()} wks left"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (runway < 8) colors.bad else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = { advance(13) }, enabled = enabled, shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) { Text("+Qtr") }
            Spacer(Modifier.width(6.dp))
            FilledTonalButton(
                onClick = { advance(4) }, enabled = enabled, shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) { Text("+Mo") }
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = { advance(1) }, enabled = enabled, shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.SkipNext, null, Modifier.padding(end = 4.dp))
                Text("Next week", maxLines = 1, softWrap = false)
            }
        }
    }
}

/** Standard scrolling body for a tab: grouped sections on the grouped background. */
@Composable
fun ScreenColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = ToolbarClearance),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content,
    )
}
