package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
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
            Text(
                "Every pairing of lanes unlocks a crossover product only that combination gets, along with a problem unique to it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (state.crossovers.isEmpty()) {
            item { GroupedSection(null) { CardBlock { Hint("You run one lane. Your first combo unlocks at the Pivot Point, when you take on a second.") } } }
        }
        state.crossovers.forEach { c -> item(key = c.combo.name) { CrossoverSection(store, c) } }
        item { AllCombosSection(state) }
    }
}

@Composable
private fun CrossoverSection(store: GameStore, c: Crossover) {
    val colors = gameColors
    val combo = c.combo
    GroupedSection("${combo.first.title} + ${combo.second.title}") {
        CardBlock {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                LaneBadge(combo.first, 44.dp)
                LaneBadge(combo.second, 44.dp)
                Spacer(Modifier.width(22.dp))
                Text(combo.product, style = MaterialTheme.typography.titleLargeEmphasized)
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
                    caption = "${Format.weeks(c.buildWeeksLeft)} left", wavy = true,
                )
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Build for ${Format.money(combo.buildCost)}", { store.perform { buildCrossover(combo) } })
                    Spacer(Modifier.width(10.dp))
                    Hint("About ${combo.buildWeeks} weeks")
                }
            }
            Meter(
                "Trouble brewing", c.pressure, colors.bad,
                if (c.incidents > 0) "${c.incidents} incident${if (c.incidents == 1) "" else "s"} so far" else null,
            )
        }
    }
}

@Composable
private fun AllCombosSection(state: GameState) {
    GroupedSection("All crossovers") {
        Combo.entries.forEachIndexed { i, combo ->
            if (i > 0) RowDivider(52.dp)
            val unlocked = state.crossover(combo) != null
            Row(Modifier.alpha(if (unlocked) 1f else 0.55f)) {
                ListRow(
                    combo.product,
                    subtitle = "${combo.first.emoji} ${combo.first.title} + ${combo.second.emoji} ${combo.second.title}\n${combo.pitch}",
                    leading = {
                        Icon(
                            if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock, null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}
