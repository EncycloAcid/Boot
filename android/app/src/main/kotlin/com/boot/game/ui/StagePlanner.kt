package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Engine
import com.boot.core.FeatureGroup
import com.boot.core.FocusArea
import com.boot.core.Format
import com.boot.core.GameConcept
import com.boot.core.GameState
import com.boot.core.MilestoneReview
import com.boot.core.Research
import com.boot.core.calendarYear
import com.boot.core.engineGroups
import com.boot.core.featureCost
import com.boot.core.leadCandidates
import com.boot.core.leadFit
import com.boot.core.planStage
import com.boot.core.stageFeatureCost

/*
 * Planning a stage the way Game Dev Tycoon does: how the stage's effort is split between its three
 * areas, which of the engine's features go in, and who leads each area.
 */

/** Which engine features go into [stage]: one row per group, a chip per option, with what it costs. */
@Composable
fun FeaturePicker(state: GameState, concept: GameConcept, engine: Engine, stage: Int, chosen: Set<Research>, editable: Boolean = true, onChange: (Set<Research>) -> Unit) {
    val g = state.games ?: return
    val groups = engineGroups(engine, stage)
    if (groups.isEmpty()) {
        Hint("${engine.name} has nothing for this stage yet. Research engine features and build a new engine.")
        return
    }
    Column(Modifier.testTag("features-$stage"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((group, have) in groups) {
            val pick = chosen.firstOrNull { it in group.members }
            val lesson = insight(g, concept.genre, group.area)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(group.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Hint(group.area.title + (lesson?.let { " · $it" } ?: ""), if (lesson != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FilterChip(
                    selected = pick == null, enabled = editable,
                    onClick = { onChange(chosen.filter { it !in group.members }.toSet()) },
                    label = { Text("None") },
                )
                for (r in have) {
                    FilterChip(
                        selected = pick == r, enabled = editable,
                        onClick = { onChange(chosen.filter { it !in group.members }.toSet() + r) },
                        label = { Text("${r.title} · ${Format.money(featureCost(r, concept.size, state.calendarYear))}") },
                    )
                }
            }
        }
        Hint("These features cost ${Format.money(stageFeatureCost(chosen, stage, concept.size, state.calendarYear))} when the stage starts. Each brings a few bugs, and they count for most where the genre cares.")
    }
}

/** Who leads each area of [stage]. */
@Composable
fun LeadPicker(state: GameState, stage: Int, leads: Map<FocusArea, Int>, onChange: (Map<FocusArea, Int>) -> Unit) {
    if (Research.LEAD_ROLES !in state.researched) return
    val people = state.leadCandidates()
    if (people.isEmpty()) return
    Column(Modifier.testTag("leads-$stage"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Leads", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        for (area in FocusArea.forStage(stage)) {
            val lead = people.firstOrNull { it.id == leads[area] }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(area.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (lead != null) Hint("${Format.percent(leadFit(lead, area))} fit  ")
                val options = listOf<Int?>(null) + people.filter { it.id !in leads.filterKeys { k -> k != area }.values }.sortedByDescending { leadFit(it, area) }.map { it.id }
                MenuButton(lead?.name ?: "No lead", options, { id ->
                    if (id == null) "No lead" else people.first { it.id == id }.let { "${it.name}${it.specialty?.let { s -> " · " + s.person.lowercase() } ?: ""} · ${Format.percent(leadFit(it, area))} fit" }
                }) { id -> onChange(if (id == null) leads - area else leads + (area to id)) }
            }
        }
        Hint("A lead makes their area's work count for up to a third more: design for the design-heavy areas, tech for the rest.")
    }
}

/** The three sliders for one stage. */
@Composable
fun StageSliders(state: GameState, concept: GameConcept, stage: Int, alloc: MutableList<Double>) {
    val g = state.games ?: return
    val areas = FocusArea.forStage(stage)
    areas.forEachIndexed { i, area ->
        val total = alloc.sum().coerceAtLeast(0.01)
        val lesson = insight(g, concept.genre, area)
        val slider = remember(stage, i) { SliderState(alloc[i].toFloat()) }
        Column {
            Row {
                Text(area.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (lesson != null) Hint(lesson, MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(Format.percent(alloc[i] / total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(state = slider, onValueChange = {
                slider.value = it
                alloc[i] = it.toDouble()
            })
        }
    }
}

/** The plan for the next stage, held while a stage gate is open. */
class StagePlan(val team: Int, val stage: Int, val focus: MutableList<Double>, features: Set<Research>?, leads: Map<FocusArea, Int>, val editableFeatures: Boolean) {
    var features by mutableStateOf(features)
    val leads = mutableStateMapOf<FocusArea, Int>().apply { putAll(leads) }

    fun apply(s: GameState) {
        s.planStage(team, stage, focus.toList(), if (editableFeatures) features else null, leads.toMap())
    }
}

@Composable
fun rememberStagePlan(state: GameState, k: MilestoneReview): StagePlan? {
    val p = state.games?.projectOf(k.team) ?: return null
    val stage = k.milestone + 1
    if (stage > 2 || p.isReady) return null
    return remember(k) {
        val stageFeatures = p.features?.filter { FeatureGroup.of(it)?.area?.stage == stage }?.toSet()
        StagePlan(
            k.team, stage, mutableStateListOf<Double>().apply { addAll(p.focus[stage]) },
            stageFeatures, p.leads.filterKeys { it.stage == stage }, editableFeatures = p.features != null && stage !in p.paidStages,
        )
    }
}

/** A stage gate: the next stage's split, features and leads, above the milestone's choices. */
@Composable
fun StagePlanSection(state: GameState, plan: StagePlan) {
    val p = state.games?.projectOf(plan.team) ?: return
    val engine = state.games!!.engine(p.concept.engineId)
    Column(Modifier.testTag("stage-plan"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("Plan ${FocusArea.STAGES[plan.stage]}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        CardBlock {
            StageSliders(state, p.concept, plan.stage, plan.focus)
            plan.features?.let { chosen ->
                FeaturePicker(state, p.concept, engine, plan.stage, chosen, plan.editableFeatures) { plan.features = it }
            }
            LeadPicker(state, plan.stage, plan.leads) { next ->
                plan.leads.clear()
                plan.leads.putAll(next)
            }
        }
    }
}
