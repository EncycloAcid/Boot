package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.FOUNDER_MAX_LEVEL
import com.boot.core.FounderSkill
import com.boot.core.FounderTrait
import com.boot.core.GameState
import com.boot.core.SECOND_TRAIT_LEVEL
import com.boot.core.SkillBranch
import com.boot.core.StoryKind
import com.boot.core.activeStories
import com.boot.core.founderXpToNext
import com.boot.core.learnSkill
import com.boot.core.pickSecondTrait
import com.boot.core.secondTraitChoices
import com.boot.core.skillBlocker
import com.boot.core.skillPoints
import com.boot.core.storyStatus
import com.boot.game.GameStore

/*
 * The founder growing (your level, your skill tree and your second trait) and the staff stories
 * on the Team tab.
 */

/** Your level and experience, as a line and a bar. */
@Composable
fun FounderLevel(state: GameState, modifier: Modifier = Modifier) {
    val f = state.founder
    val colors = gameColors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Level ${f.level}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                if (f.level >= FOUNDER_MAX_LEVEL) "Max level" else "${f.xp.toInt()} / ${founderXpToNext(f.level).toInt()} XP",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val done = if (f.level >= FOUNDER_MAX_LEVEL) 1f else (f.xp / founderXpToNext(f.level)).toFloat().coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.fill)) {
            Box(Modifier.fillMaxWidth(done).height(6.dp).background(colors.milestone))
        }
    }
}

/** What the founder row on the Team tab says about your growth. */
fun growthLine(state: GameState): String {
    val f = state.founder
    val points = f.skillPoints
    val trait = if (f.level >= SECOND_TRAIT_LEVEL && f.trait2 == null) " and a second trait" else ""
    return when {
        points > 0 -> "Level ${f.level} · $points skill point${if (points == 1) "" else "s"}$trait to spend"
        trait.isNotEmpty() -> "Level ${f.level} · a second trait to pick"
        else -> "Level ${f.level} · ${f.skills.size} skill${if (f.skills.size == 1) "" else "s"}"
    }
}

@Composable
fun FounderSheet(store: GameStore, state: GameState) {
    val colors = gameColors
    val close = { store.founderOpen = false }
    val f = state.founder
    var picked by remember { mutableStateOf<FounderSkill?>(null) }
    GameDialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close") }
                    FounderPortrait(state, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(f.name, style = MaterialTheme.typography.titleMedium)
                        Hint(growthLine(state))
                    }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 640.dp
                    val tree: @Composable () -> Unit = {
                        SkillTree(state, picked) { picked = it }
                    }
                    val side: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CardBlock { FounderLevel(state) }
                            Hint("You earn experience every week, and more for each launch (a hit counts most). Each level brings a skill point.")
                            SkillDetail(store, state, picked) { picked = null }
                            Traits(store, state)
                        }
                    }
                    if (wide) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1.4f).verticalScroll(rememberScrollState())) { tree() }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { side() }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            tree()
                            side()
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

/** Three branches side by side, each three skills deep. */
@Composable
private fun SkillTree(state: GameState, picked: FounderSkill?, onPick: (FounderSkill) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (branch in SkillBranch.entries) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    branch.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp),
                )
                val skills = FounderSkill.entries.filter { it.branch == branch }.sortedBy { it.tier }
                skills.forEachIndexed { i, skill ->
                    if (i > 0) {
                        val lit = skill in state.founder.skills || skills[i - 1] in state.founder.skills
                        Box(Modifier.width(3.dp).height(14.dp).background(if (lit) gameColors.milestone else gameColors.separator))
                    }
                    SkillNode(state, skill, skill == picked) { onPick(skill) }
                }
            }
        }
    }
}

@Composable
private fun SkillNode(state: GameState, skill: FounderSkill, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = gameColors
    val learned = skill in state.founder.skills
    val open = state.skillBlocker(skill) == null
    val reachable = learned || FounderSkill.entries.none { it.branch == skill.branch && it.tier < skill.tier && it !in state.founder.skills }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(
                when {
                    learned -> colors.milestone.copy(alpha = 0.22f)
                    open -> scheme.primaryContainer.copy(alpha = 0.5f)
                    else -> colors.card
                },
            )
            .border(
                if (selected) 2.dp else 1.dp,
                when {
                    selected -> scheme.primary
                    learned -> colors.milestone
                    open -> scheme.primary.copy(alpha = 0.6f)
                    else -> colors.separator
                },
                shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            skill.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = if (reachable) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            when {
                learned -> "Learned"
                open -> "Ready to learn"
                else -> "Tier ${skill.tier}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when {
                learned -> colors.milestone
                open -> scheme.primary
                else -> scheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun SkillDetail(store: GameStore, state: GameState, skill: FounderSkill?, onDone: () -> Unit) {
    if (skill == null) {
        Hint("Tap a skill to see what it does.")
        return
    }
    val blocker = state.skillBlocker(skill)
    GroupedSection("${skill.title} · ${skill.branch.title.lowercase()}, tier ${skill.tier}") {
        CardBlock {
            Text(skill.detail, style = MaterialTheme.typography.bodyMedium)
            if (blocker != null && skill !in state.founder.skills) Hint(blocker, gameColors.warn)
            if (skill !in state.founder.skills) {
                PrimaryButton("Learn ${skill.title.lowercase()}", {
                    store.perform { learnSkill(skill) }
                    onDone()
                }, Modifier.fillMaxWidth(), enabled = blocker == null)
            }
        }
    }
}

/** Your trait, and the second one when it comes. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Traits(store: GameStore, state: GameState) {
    val f = state.founder
    val colors = gameColors
    GroupedSection(
        "Traits",
        footer = when {
            f.trait2 != null -> null
            f.level >= SECOND_TRAIT_LEVEL -> "Pick a second trait to go with your first. It's for good."
            else -> "A second trait comes at level $SECOND_TRAIT_LEVEL."
        },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (t in listOfNotNull(f.trait, f.trait2)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(t.title, colors.milestone)
                    Text(t.detail.substringAfter(": ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (f.trait == null && f.trait2 == null) Hint("You started without a trait.")
            if (f.trait2 == null && f.level >= SECOND_TRAIT_LEVEL) {
                var choosing by remember { mutableStateOf<FounderTrait?>(null) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in state.secondTraitChoices) {
                        val on = t == choosing
                        Text(
                            t.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .border(1.dp, if (on) MaterialTheme.colorScheme.primary else colors.separator, CircleShape)
                                .clickable(role = Role.RadioButton) { choosing = t }
                                .semantics { selected = on }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                choosing?.let { t ->
                    Hint(t.detail)
                    PrimaryButton("Become a ${t.title.lowercase()}", { store.perform { pickSecondTrait(t) } }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** The staff stories under way and the last few that ended, for the Team tab. */
@Composable
fun StoriesSection(state: GameState) {
    val active = state.activeStories
    val lasting = state.stories.filter { it.ending != null && it.lasting }
    val past = state.stories.filter { it.ending != null && !it.lasting }.takeLast(3).reversed()
    if (active.isEmpty() && lasting.isEmpty() && past.isEmpty()) return
    GroupedSection("Stories", footer = "Now and then someone on the team has a story: how it goes is up to you.") {
        val rows = active.map { it to "Under way" } + lasting.map { it to "For good" } + past.map { it to "Ended" }
        rows.forEachIndexed { i, (s, label) ->
            if (i > 0) RowDivider()
            ListRow(
                s.kind.title,
                subtitle = state.storyStatus(s),
                value = label,
                valueColor = when (label) {
                    "Under way" -> MaterialTheme.colorScheme.primary
                    "For good" -> gameColors.milestone
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                leading = {
                    state.staff.firstOrNull { it.id == s.people.first() }?.let { StaffPortrait(it, 40.dp) }
                        ?: Box(Modifier.width(40.dp))
                },
            )
        }
    }
}

/** A person's story, for their card. */
internal fun storyLine(state: GameState, employeeId: Int): Pair<StoryKind, String>? =
    state.stories.lastOrNull { (it.ending == null || it.lasting) && employeeId in it.people }?.let { it.kind to state.storyStatus(it) }
