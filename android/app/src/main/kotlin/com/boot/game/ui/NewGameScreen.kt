package com.boot.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.game.GameStore

@Composable
fun NewGameScreen(store: GameStore) {
    var company by rememberSaveable { mutableStateOf("Garage Labs") }
    var founder by rememberSaveable { mutableStateOf("") }
    var lane by rememberSaveable { mutableStateOf(Lane.GAMES) }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (lane.isCapitalIntensive) {
                        Hint(
                            "Hard mode: ${lane.title} starts with a seed round and a board that can overrule you, or fire you.",
                            gameColors.warn,
                        )
                    }
                    Button(
                        onClick = {
                            store.newGame(company.trim().ifEmpty { "Garage Labs" }, founder.trim().ifEmpty { "You" }, lane)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start your ${lane.businessName.lowercase()}") }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 24.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Boot", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "You start solo in a cramped apartment with ${Format.money(GameState.STARTING_CASH)} in savings. Pick a lane.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Hint(
                        "Mid-game, the Pivot Point forces a second lane. Every pairing unlocks its own crossover. " +
                            "Hardware, software and services together make an ecosystem company. " +
                            "IPO, sell, stay indie or burn out: the ending reflects how you got there.",
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = company, onValueChange = { company = it }, label = { Text("Company name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    OutlinedTextField(
                        value = founder, onValueChange = { founder = it }, label = { Text("Your name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    )
                }
            }
            items(Lane.entries) { l -> LaneCard(l, selected = l == lane) { lane = l } }
        }
    }
}

@Composable
private fun LaneCard(lane: Lane, selected: Boolean, onClick: () -> Unit) {
    val accent = gameColors.lane(lane)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LaneBadge(lane)
                Spacer(Modifier.width(12.dp))
                Text(lane.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (lane.isCapitalIntensive) Pill("VC", gameColors.warn)
            }
            Text(lane.pitch, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
