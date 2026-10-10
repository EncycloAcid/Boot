package com.boot.game.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.boot.core.BUILD_DRAG
import com.boot.core.Feature
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.buildFeature
import com.boot.core.featureBlocker
import com.boot.game.GameStore

/** A business's roadmap: what it has built, what it's building, and what it could build next. */
@Composable
fun RoadmapSection(store: GameStore, state: GameState, lane: Lane) {
    val features = Feature.forLane(lane)
    if (features.isEmpty()) return
    val colors = gameColors
    val building = state.building.firstOrNull { it.feature.lane == lane }
    GroupedSection(
        "Roadmap · ${features.count { it in state.features }} of ${features.size} built",
        footer = "Each feature costs money up front and a few weeks of the team's time, when it gets ${Format.percent(1 - BUILD_DRAG)} less done. " +
            "Then it helps for good. One at a time.",
    ) {
        features.forEachIndexed { i, f ->
            if (i > 0) RowDivider(56.dp)
            val built = f in state.features
            val inProgress = building?.feature == f
            val blocker = state.featureBlocker(f)
            ListRow(
                f.title,
                subtitle = f.detail,
                leading = {
                    Icon(
                        when {
                            built -> Icons.Filled.CheckCircle
                            inProgress -> Icons.Filled.Construction
                            else -> Icons.Filled.RadioButtonUnchecked
                        },
                        null,
                        tint = when {
                            built -> colors.good
                            inProgress -> colors.warn
                            else -> MaterialTheme.colorScheme.outline
                        },
                    )
                },
                value = when {
                    built -> "Built"
                    inProgress -> "${Format.weeks(building!!.weeksLeft)} left"
                    else -> Format.money(f.cost)
                },
                valueDetail = if (!built && !inProgress) Format.weeks(f.weeks) else null,
                trailing = if (!built && !inProgress) {
                    { TonalButton("Build", { store.perform { buildFeature(f) } }, enabled = blocker == null) }
                } else {
                    null
                },
            )
            if (inProgress) {
                CardBlock { Meter("Building", 1 - building!!.weeksLeft.toDouble() / f.weeks, colors.warn, wavy = store.playing) }
            }
        }
    }
}
