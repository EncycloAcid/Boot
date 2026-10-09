package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.boot.core.GameState
import com.boot.core.Lane

@Composable
fun NewsScreen(state: GameState) {
    var filter by rememberSaveable { mutableStateOf<Lane?>(null) }
    val entries = state.log.asReversed().filter { filter == null || it.lane == filter }.take(200)
    ScreenColumn {
        item { ScreenHeader("News") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Everything") }) }
                items(Lane.entries) { lane ->
                    FilterChip(selected = filter == lane, onClick = { filter = lane }, label = { Text("${lane.emoji} ${lane.short}") })
                }
            }
        }
        item {
            GroupedSection(null) {
                if (entries.isEmpty()) CardBlock { Hint("Nothing yet.") }
                entries.forEachIndexed { i, entry ->
                    if (i > 0) RowDivider(36.dp)
                    LogRow(entry)
                }
            }
        }
    }
}
