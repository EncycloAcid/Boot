package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.boot.core.Employee
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Perk
import com.boot.core.Research
import com.boot.core.assign
import com.boot.core.buyPerk
import com.boot.core.fire
import com.boot.core.freeDesks
import com.boot.core.hire
import com.boot.core.moveOffice
import com.boot.core.perkBlocker
import com.boot.core.perkSlots
import com.boot.core.perkUpkeep
import com.boot.core.removePerk
import com.boot.core.setCrunch
import com.boot.core.setFocus
import com.boot.core.setFreelancing
import com.boot.core.train
import com.boot.core.trainingBlocker
import com.boot.core.trainingCost
import com.boot.game.GameStore
import java.util.Locale

@Composable
fun TeamScreen(store: GameStore, state: GameState) {
    ScreenColumn {
        item { ScreenHeader("Team", "${state.staff.size + 1} people · ${state.payroll.perWeek()} payroll") }
        item { TeamSection(store, state) }
        item { HiringSection(store, state) }
        item { OfficeSection(store, state) }
        item { PerksSection(store, state) }
        item { FounderSection(store, state) }
        if (state.lanes.size > 1) item { AttentionSection(store, state) }
    }
}

@Composable
private fun FounderSection(store: GameStore, state: GameState) {
    val f = state.founder
    GroupedSection("You, ${f.name}") {
        ListRow(
            f.name,
            subtitle = "Founder and CEO · ${Format.percent(state.venture.founderEquity)} of ${state.companyName}",
            leading = { FounderPortrait(state, 48.dp) },
        )
        RowDivider()
        state.lanes.forEachIndexed { i, lane ->
            if (i > 0) RowDivider()
            ValueRow("${lane.title} experience", String.format(Locale.US, "+%.1f output", f.skillBonus(lane)))
        }
        RowDivider()
        ToggleRow(
            "Crunch mode", state.crunch,
            if (state.staff.isEmpty()) "Needs a team. It's just you for now." else "The team works overtime: +40% of their output, more bugs, worse polish. Morale falls fast.",
        ) { on ->
            store.perform { setCrunch(on) }
        }
        RowDivider()
        ToggleRow("Freelance gigs", f.freelancing, "${f.freelanceIncome(state.week).perWeek()} to cover rent. The company gets a third of you.") { on ->
            store.perform { setFreelancing(on) }
        }
    }
}

@Composable
private fun AttentionSection(store: GameStore, state: GameState) {
    GroupedSection("Attention", footer = "Your own output is split between your lanes.") {
        CardBlock {
            state.lanes.forEach { lane ->
                CommitSlider(
                    state.founder.focus[lane] ?: 1.0,
                    { "${lane.emoji} ${lane.title}: ${Format.percent(state.founder.attentionShare(lane, state.lanes))} of you" },
                ) { v -> store.perform { setFocus(lane, v) } }
            }
        }
    }
}

@Composable
private fun OfficeSection(store: GameStore, state: GameState) {
    var confirm by remember { mutableStateOf(false) }
    val next = state.office.next
    GroupedSection(
        "Office",
        footer = if (state.staff.size > state.office.desks) "It's crowded in here. Morale drops every week until you move." else "Every hire needs a desk. Move up to grow.",
    ) {
        if (next != null) {
            Box(Modifier.fillMaxWidth().height(190.dp)) {
                OfficePreview(state, next, Modifier.fillMaxSize(), insetTop = 34.dp)
                Text(
                    "Next: ${next.title}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(gameColors.card.copy(alpha = 0.86f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        ValueRow(state.office.title, "${state.staff.size}/${state.office.desks} desks")
        RowDivider()
        ValueRow("Rent", state.office.rent.perWeek())
        if (next != null) {
            RowDivider()
            ActionRow("Move to a ${next.title.lowercase()}", "${next.desks} desks · ${next.rent.perWeek()} rent", Format.money(next.moveCost), enabled = state.cash >= next.moveCost) {
                confirm = true
            }
        }
    }
    if (confirm && next != null) {
        ConfirmDialog(
            title = "Move to a ${next.title.lowercase()}?",
            text = "Moving costs ${Format.money(next.moveCost)} and rent goes up to ${next.rent.perWeek()}.",
            confirm = "Move",
            onConfirm = { store.perform { moveOffice() } },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun HiringSection(store: GameStore, state: GameState) {
    GroupedSection(
        "Hiring",
        footer = (if (state.freeDesks <= 0) "No free desks. Move to a bigger office to hire. " else "") +
            "Rivals compete for the same people. Contested candidates vanish fast. Recruiting costs two weeks' salary.",
    ) {
        state.candidates.forEachIndexed { i, c ->
            if (i > 0) RowDivider(68.dp)
            ListRow(
                c.name,
                subtitle = "${c.role.title} · skill ${c.skill} · ${c.askingSalary.perWeek()}\nDesign ${c.design} · Tech ${c.tech} · Research ${c.research}" +
                    (c.contestedBy?.let { "\n$it is circling" } ?: ""),
                leading = { CandidatePortrait(c.id, 44.dp, ring = if (c.contestedBy != null) gameColors.warn else null) },
            ) {
                if (state.lanes.size == 1) {
                    TonalButton("Hire", { store.perform { hire(c.id, state.lanes.first()) } })
                } else {
                    MenuButton("Hire…", state.lanes, { "Into ${it.businessName}" }) { lane -> store.perform { hire(c.id, lane) } }
                }
            }
        }
    }
}

@Composable
private fun TeamSection(store: GameStore, state: GameState) {
    var firing by remember { mutableStateOf<Employee?>(null) }
    var training by remember { mutableStateOf<Employee?>(null) }
    val canTrain = Research.TRAINING in state.researched
    GroupedSection(
        "Staff",
        footer = if (canTrain) "Tap ⋮ to move someone to another lane, send them on a course or let them go." else "Tap ⋮ to move someone to another lane. Research a training program to raise people's skills.",
    ) {
        if (state.staff.isEmpty()) CardBlock { Hint("It's just you in the apartment.") }
        state.staff.forEachIndexed { i, e ->
            if (i > 0) RowDivider(68.dp)
            EmployeeRow(
                e, state.lanes,
                onAssign = { lane -> store.perform { assign(e.id, lane) } },
                onFire = { firing = e },
                onTrain = if (canTrain) ({ training = e }) else null,
            )
        }
    }
    training?.let { e ->
        val (rp, cost) = state.trainingCost(e)
        val blocker = state.trainingBlocker(e)
        ConfirmDialog(
            title = "Train ${e.name}?",
            text = blocker ?: "A course costs $rp RP and ${Format.money(cost)}. Skill goes up by one, and so does their pay.",
            confirm = if (blocker == null) "Train" else "OK",
            onConfirm = { if (blocker == null) store.perform { train(e.id) } },
            onDismiss = { training = null },
        )
    }
    firing?.let { e ->
        ConfirmDialog(
            title = "Let ${e.name} go?",
            text = "Four weeks' severance (${Format.money(e.salary * 4)}). Everyone else's morale dips a little.",
            confirm = "Let go",
            destructive = true,
            onConfirm = { store.perform { fire(e.id) } },
            onDismiss = { firing = null },
        )
    }
}

@Composable
private fun EmployeeRow(e: Employee, lanes: List<Lane>, onAssign: (Lane?) -> Unit, onFire: () -> Unit, onTrain: (() -> Unit)?) {
    val colors = gameColors
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        StaffPortrait(e, 48.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Pill("Lv ${e.level}", colors.milestone)
                Pill(e.lane?.short ?: "Unassigned", e.lane?.let { colors.lane(it) } ?: MaterialTheme.colorScheme.outline)
            }
            Text("${e.role.title} · skill ${e.skill} · ${e.salary.perWeek()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                "Design ${e.design} · Tech ${e.tech} · Research ${e.research}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                MiniBar("Morale", e.morale, if (e.morale < 0.35) colors.bad else colors.good)
                MiniBar(if (e.maxLevel) "Max level" else "XP", if (e.maxLevel) 1.0 else e.xp / e.xpToNext, colors.milestone)
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Manage ${e.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                (listOf<Lane?>(null) + lanes).forEach { lane ->
                    DropdownMenuItem(
                        text = { Text(lane?.let { "Work in the ${it.businessName.lowercase()}" } ?: "Unassigned") },
                        leadingIcon = { if (lane == e.lane) Icon(Icons.Filled.Check, null) else Spacer(Modifier.size(24.dp)) },
                        onClick = {
                            menu = false
                            onAssign(lane)
                        },
                    )
                }
                HorizontalDivider()
                if (onTrain != null) {
                    DropdownMenuItem(text = { Text("Send on a course") }, leadingIcon = { Icon(Icons.Filled.School, null) }, onClick = {
                        menu = false
                        onTrain()
                    })
                }
                DropdownMenuItem(
                    text = { Text("Let ${e.name.substringBefore(' ')} go", color = colors.bad) },
                    leadingIcon = { Icon(Icons.Filled.PersonRemove, null, tint = colors.bad) },
                    onClick = {
                        menu = false
                        onFire()
                    },
                )
            }
        }
    }
}

/** A tiny labelled bar for a list row. */
@Composable
private fun MiniBar(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    val colors = gameColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            Modifier
                .width(44.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.fill),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value.toFloat().coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

/** Office perks: bought once, paid for every week, felt in morale, learning and hiring. */
@Composable
private fun PerksSection(store: GameStore, state: GameState) {
    val colors = gameColors
    var removing by remember { mutableStateOf<Perk?>(null) }
    GroupedSection(
        "Perks · ${state.perks.size} of ${state.office.perkSlots}",
        footer = "${state.perkUpkeep.perWeek()} upkeep. Bigger offices fit more perks.",
    ) {
        Perk.entries.forEachIndexed { i, perk ->
            if (i > 0) RowDivider()
            val owned = perk in state.perks
            val blocker = state.perkBlocker(perk)
            ListRow(
                perk.title,
                subtitle = if (owned) perk.blurb else (blocker?.takeIf { state.office < perk.minOffice } ?: perk.blurb) +
                    "\n${Format.money(perk.cost)} + ${perk.weeklyCost(state.staff.size).perWeek()}",
            ) {
                if (owned) {
                    PlainButton("Remove", { removing = perk }, color = colors.bad)
                } else {
                    TonalButton("Buy", { store.perform { buyPerk(perk) } }, enabled = blocker == null)
                }
            }
        }
    }
    removing?.let { perk ->
        ConfirmDialog(
            title = "Remove the ${perk.title.lowercase()}?",
            text = "No refund, and people will miss it.",
            confirm = "Remove",
            destructive = true,
            onConfirm = { store.perform { removePerk(perk) } },
            onDismiss = { removing = null },
        )
    }
}
