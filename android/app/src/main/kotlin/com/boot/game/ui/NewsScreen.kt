package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.GameState
import com.boot.core.Lane

@Composable
fun NewsScreen(state: GameState) {
    var filter by rememberSaveable { mutableStateOf<Lane?>(null) }
    val entries = state.log.asReversed().filter { filter == null || it.lane == filter }
    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Everything") }) }
            items(Lane.entries) { lane ->
                FilterChip(selected = filter == lane, onClick = { filter = lane }, label = { Text("${lane.emoji} ${lane.title}") })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("News", style = MaterialTheme.typography.headlineSmall) }
            if (entries.isEmpty()) item { Hint("Nothing yet.") }
            items(entries, key = { it.id }) { LogRow(it) }
        }
    }
}
