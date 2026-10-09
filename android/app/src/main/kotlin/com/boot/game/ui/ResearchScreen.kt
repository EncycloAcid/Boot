package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Research
import com.boot.core.Topic
import com.boot.core.research
import com.boot.core.researchBlocker
import com.boot.core.researchPerWeek
import com.boot.core.researchTopic
import com.boot.game.GameStore
import java.util.Locale

private enum class ResearchPage(val title: String) { STUDIO("Studio"), ENGINE("Engine"), TOPICS("Topics"), COMPANY("Company") }

/** Spend research points: game studio unlocks, engine features, topics, and upgrades for every lane. */
@Composable
fun ResearchScreen(store: GameStore, state: GameState) {
    val studio = state.games != null
    val pages = if (studio) ResearchPage.entries else listOf(ResearchPage.COMPANY)
    var page by rememberSaveable { mutableStateOf(if (studio) ResearchPage.STUDIO else ResearchPage.COMPANY) }
    val current = page.takeIf { it in pages } ?: pages.first()
    ScreenColumn {
        item { PointsHero(state) }
        if (pages.size > 1) item { Segmented(pages, current, { it.title }) { page = it } }
        when (current) {
            ResearchPage.STUDIO -> item { ResearchList(store, state, "Game studio", Research.entries.filter { it.kind == Research.Kind.STUDIO }) }
            ResearchPage.ENGINE -> {
                item {
                    ResearchList(
                        store, state, "Engine features",
                        Research.entries.filter { it.kind == Research.Kind.ENGINE },
                        footer = "Features do nothing until you build them into an engine. Build engines in the Lanes tab under your game studio.",
                    )
                }
            }
            ResearchPage.TOPICS -> item { TopicList(store, state) }
            ResearchPage.COMPANY -> {
                item { ResearchList(store, state, "Company", Research.entries.filter { it.kind == Research.Kind.COMPANY }) }
                for (lane in state.lanes) {
                    val items = Research.entries.filter { it.lane == lane }
                    if (items.isNotEmpty()) item { ResearchList(store, state, lane.businessName, items) }
                }
            }
        }
    }
}

@Composable
private fun PointsHero(state: GameState) {
    Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Research points", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(
                "${state.researchPoints.toInt()} RP",
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                String.format(Locale.US, "+%.1f RP a week · %d researched", state.researchPerWeek, state.researched.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.size(4.dp))
            Hint(
                "Points come from your researchers, from you, and from every week the team spends making games.",
                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ResearchList(store: GameStore, state: GameState, header: String, items: List<Research>, footer: String? = null) {
    val colors = gameColors
    GroupedSection(header, footer = footer) {
        items.forEachIndexed { i, r ->
            if (i > 0) RowDivider()
            val done = r in state.researched
            val blocker = state.researchBlocker(r)
            val locked = r.requires?.let { it !in state.researched } == true
            val cost = "${r.rp} RP" + if (r.cash > 0) " + ${Format.money(r.cash)}" else ""
            ListRow(
                r.title,
                subtitle = r.detail + if (locked) "\nNeeds ${r.requires!!.title}." else "",
                leading = {
                    Icon(
                        when {
                            done -> Icons.Filled.CheckCircle
                            locked -> Icons.Filled.Lock
                            else -> Icons.Filled.Science
                        },
                        null,
                        tint = when {
                            done -> colors.good
                            locked -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(26.dp),
                    )
                },
            ) {
                if (!done) {
                    Spacer(Modifier.width(8.dp))
                    TonalButton(cost, { store.perform { research(r) } }, enabled = blocker == null)
                }
            }
        }
    }
}

@Composable
private fun TopicList(store: GameStore, state: GameState) {
    val g = state.games ?: return
    val locked = Topic.entries.filter { it !in g.topics }
    GroupedSection("Topics", footer = "You know ${g.topics.size} of ${Topic.entries.size} topics. New ones open fresh combos.") {
        if (locked.isEmpty()) CardBlock { Hint("You've researched every topic.") }
        locked.forEachIndexed { i, t ->
            if (i > 0) RowDivider()
            ListRow(t.title, subtitle = g.trend?.takeIf { it.topic == t }?.let { "Trending right now" }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton("${t.researchCost} RP", { store.perform { researchTopic(t) } }, enabled = state.researchPoints >= t.researchCost)
                }
            }
        }
    }
}
