package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    TabSpec(Tab.PEOPLE, "People", Icons.Filled.Groups),
    TabSpec(Tab.COMBOS, "Combos", Icons.Filled.Hub),
    TabSpec(Tab.MONEY, "Money", Icons.Filled.AccountBalance),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScreen(store: GameStore, state: GameState) {
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }

    LaunchedEffect(store.error) {
        store.error?.let {
            snackbar.showSnackbar(it)
            store.error = null
        }
    }
    BackHandler(enabled = store.tab != Tab.HOME) { store.tab = Tab.HOME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.companyName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${state.dateLabel} · ${state.world.era.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { store.tab = Tab.NEWS }) { Icon(Icons.Filled.Newspaper, "News") }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Abandon run and start over") }, onClick = {
                            menuOpen = false
                            confirmAbandon = true
                        })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Column {
                AdvanceBar(store, state)
                NavigationBar {
                    for (t in tabs) {
                        NavigationBarItem(
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
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (store.tab) {
                Tab.HOME -> DashboardScreen(store, state)
                Tab.LANES -> LanesScreen(store, state)
                Tab.PEOPLE -> PeopleScreen(store, state)
                Tab.COMBOS -> CombosScreen(store, state)
                Tab.MONEY -> MoneyScreen(store, state)
                Tab.NEWS -> NewsScreen(state)
            }
        }
    }

    // Decisions block the calendar. Endings beat the Pivot Point, which beats ordinary events.
    when {
        state.isOver -> EndingDialog(store, state)
        state.pendingPivot != null -> PivotDialog(store, state)
        state.events.isNotEmpty() -> EventDialog(store, state)
    }

    if (confirmAbandon) {
        ConfirmDialog(
            title = "Abandon ${state.companyName}?",
            text = "This run ends for good and you start over in the apartment.",
            confirm = "Abandon",
            onConfirm = { store.abandon() },
            onDismiss = { confirmAbandon = false },
        )
    }
}

/** Cash on the left, time controls on the right. Always in reach of a thumb. */
@Composable
private fun AdvanceBar(store: GameStore, state: GameState) {
    val runway = state.runwayWeeks
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Format.money(state.cash),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.cash < 0) gameColors.bad else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    when {
                        !runway.isFinite() -> "Profitable"
                        runway > 260 -> "5+ yrs runway"
                        else -> "${runway.toInt()} wks runway"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (runway < 8) gameColors.bad else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val enabled = store.canAdvance
            FilledTonalButton(onClick = { store.advance(13) }, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("+Qtr")
            }
            FilledTonalButton(onClick = { store.advance(4) }, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Text("+Mo")
            }
            Button(onClick = { store.advance(1) }, enabled = enabled) { Text("Next week") }
        }
    }
}

/** Standard scrolling body for a tab. */
@Composable
fun ScreenColumn(content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
