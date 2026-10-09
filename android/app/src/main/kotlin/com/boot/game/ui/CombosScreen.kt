package com.boot.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.boot.core.Combo
import com.boot.core.Crossover
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.buildCrossover
import com.boot.core.setIntensity
import com.boot.game.GameStore

@Composable
fun CombosScreen(store: GameStore, state: GameState) {
    ScreenColumn {
        item {
            Column {
                Text("Combos", style = MaterialTheme.typography.headlineSmall)
                Hint("Every pairing of lanes unlocks a crossover product only that combination gets, along with a problem unique to it.")
            }
        }
        if (state.crossovers.isEmpty()) {
            item { Section(null) { Hint("You run one lane. Your first combo unlocks at the Pivot Point, when you take on a second.") } }
        }
        state.crossovers.forEach { c -> item(key = c.combo.name) { CrossoverCard(store, c) } }
        item { AllCombosCard(state) }
    }
}

@Composable
private fun CrossoverCard(store: GameStore, c: Crossover) {
    val colors = gameColors
    val combo = c.combo
    Section(combo.product) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LaneBadge(combo.first, 28)
            Text(" + ", style = MaterialTheme.typography.bodyMedium)
            LaneBadge(combo.second, 28)
            Spacer(Modifier.width(8.dp))
            Hint("${combo.first.title} + ${combo.second.title}")
        }
        Text(combo.pitch, style = MaterialTheme.typography.bodyMedium)
        Row {
            Icon(Icons.Filled.Warning, null, tint = colors.warn, modifier = Modifier.size(18.dp).padding(top = 2.dp))
            Spacer(Modifier.width(6.dp))
            Text(combo.problem, style = MaterialTheme.typography.bodyMedium, color = colors.warn)
        }
        when {
            c.built -> CommitSlider(c.intensity, { "${combo.dial}: ${Format.percent(it.toDouble())}" }) { v ->
                store.perform { setIntensity(combo, v) }
            }
            c.isBuilding -> Meter(
                "Building", 1 - c.buildWeeksLeft.toDouble() / combo.buildWeeks,
                caption = "${c.buildWeeksLeft} weeks left",
            )
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { store.perform { buildCrossover(combo) } }) { Text("Build for ${Format.money(combo.buildCost)}") }
                Spacer(Modifier.width(8.dp))
                Hint("About ${combo.buildWeeks} weeks")
            }
        }
        Meter(
            "Trouble brewing", c.pressure, colors.bad,
            if (c.incidents > 0) "${c.incidents} incident${if (c.incidents == 1) "" else "s"} so far" else null,
        )
    }
}

@Composable
private fun AllCombosCard(state: GameState) {
    Section("All crossovers") {
        Combo.entries.forEachIndexed { i, combo ->
            if (i > 0) HorizontalDivider()
            val unlocked = state.crossover(combo) != null
            Row(Modifier.alpha(if (unlocked) 1f else 0.6f), verticalAlignment = Alignment.Top) {
                Icon(
                    if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock, null,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "${combo.product} · ${combo.first.emoji} ${combo.first.title} + ${combo.second.emoji} ${combo.second.title}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Hint(combo.pitch)
                }
            }
        }
    }
}
