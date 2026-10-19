package com.boot.game.ui

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
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
import com.boot.core.Role
import com.boot.core.skillPoints
import com.boot.core.Employee
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Perk
import com.boot.core.Research
import com.boot.core.assign
import com.boot.core.buyPerk
import com.boot.core.careerLine
import com.boot.core.effectiveLoyalty
import com.boot.core.fire
import com.boot.core.freeDesks
import com.boot.core.hire
import com.boot.core.isStar
import com.boot.core.moveOffice
import com.boot.core.offerLine
import com.boot.core.perkBlocker
import com.boot.core.perkSlots
import com.boot.core.perkUpkeep
import com.boot.core.poachMarketBlocker
import com.boot.core.poachMarketStar
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
        item {
            val morale = state.staff.takeIf { it.isNotEmpty() }?.map { it.morale }?.average()
            val moraleBit = morale?.let { " · morale ${String.format(java.util.Locale.US, "%.0f", it * 100)}%" }.orEmpty()
            ScreenHeader(
                "Team",
                "${state.staff.size + 1} people · ${state.payroll.perWeek()} payroll$moraleBit",
            )
        }
        item { TeamSection(store, state) }
        if (state.stories.isNotEmpty()) item { StoriesSection(state) }
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
        ListRow(
            "Level and skills",
            subtitle = growthLine(state),
            chevron = true,
            onClick = { store.founderOpen = true },
            trailing = if (state.founder.skillPoints > 0) ({ Pill("${state.founder.skillPoints}", gameColors.milestone) }) else null,
        )
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) { FounderLevel(state) }
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
            "Rivals compete for the same people. Contested candidates vanish fast. Recruiting costs two weeks' salary. " +
            "Stars show a career chapter — poach them with a signing bonus when contested.",
    ) {
        val joiner = if (LocalCompact.current) " · " else "\n"
        state.candidates.forEachIndexed { i, c ->
            if (i > 0) RowDivider(68.dp)
            val star = c.notable || c.skill >= 8 || c.reputation >= 0.4
            val arc = if (star) {
                val hits = if (c.pastHits.isNotEmpty()) " · ${c.pastHits.take(2).joinToString(", ")}" else ""
                "\n${c.chapter.title}$hits"
            } else ""
            ListRow(
                c.name,
                subtitle = "${c.role.title}${c.specialty?.let { " · " + it.person.lowercase() } ?: ""} · skill ${c.skill} · ${c.askingSalary.perWeek()}${joiner}Design ${c.design} · Tech ${c.tech} · Research ${c.research}" +
                    (c.trait?.let { "\n${it.title}: ${it.detail.replaceFirstChar { ch -> ch.lowercase() }}" } ?: "") +
                    arc +
                    (c.contestedBy?.let { "\n$it is circling" } ?: ""),
                leading = { CandidatePortrait(c.id, 44.dp, ring = if (c.contestedBy != null || star) gameColors.warn else null) },
            ) {
                val canPoach = star && state.poachMarketBlocker(c.id) == null
                if (canPoach) {
                    if (state.lanes.size == 1) {
                        TonalButton("Poach", { store.perform { poachMarketStar(c.id, state.lanes.first()) } })
                    } else {
                        MenuButton("Poach…", state.lanes, { "Into ${it.businessName}" }) { lane -> store.perform { poachMarketStar(c.id, lane) } }
                    }
                } else if (state.lanes.size == 1) {
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
    var specialising by remember { mutableStateOf<Employee?>(null) }
    val canTrain = Research.TRAINING in state.researched
    GroupedSection(
        "Staff",
        footer = if (canTrain) "Tap the three dots beside someone to move them to another lane, send them on a course or let them go." else "Tap the three dots beside someone to move them to another lane. Research a training program to raise people's skills.",
    ) {
        if (state.staff.isEmpty()) CardBlock { Hint("It's just you in the apartment.") }
        state.staff.forEachIndexed { i, e ->
            if (i > 0) RowDivider(if (LocalCompact.current) 58.dp else 78.dp)
            EmployeeRow(
                e, state.lanes,
                onAssign = { lane -> store.perform { assign(e.id, lane) } },
                onFire = { firing = e },
                onTrain = if (canTrain) ({ training = e }) else null,
                onSpecialise = if (state.games != null && (e.role == Role.DESIGNER || e.role == Role.ENGINEER)) ({ specialising = e }) else null,
                note = state.chemistryNote(e),
                pressure = e.offerLine(),
            )
        }
    }
    specialising?.let { e -> SpecialiseDialog(store, state, e) { specialising = null } }
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
private fun EmployeeRow(
    e: Employee,
    lanes: List<Lane>,
    onAssign: (Lane?) -> Unit,
    onFire: () -> Unit,
    onTrain: (() -> Unit)?,
    onSpecialise: (() -> Unit)? = null,
    note: Pair<String, Boolean>? = null,
    pressure: String? = null,
) {
    val colors = gameColors
    var menu by remember { mutableStateOf(false) }
    val compact = LocalCompact.current
    val role = "${e.role.title} · skill\u00A0${e.skill} · ${e.salary.perWeek()}" +
        // Only worth saying which lane when there's more than one, or none.
        (if (lanes.size > 1 || e.lane == null) " · ${e.lane?.short ?: "Unassigned"}" else "")
    val skills = "Design\u00A0${e.design} · Tech\u00A0${e.tech} · Research\u00A0${e.research}"
    val bars: @Composable () -> Unit = {
        MiniBar("Morale", e.morale, if (e.morale < 0.35) colors.bad else colors.good)
        MiniBar("Loyalty", e.effectiveLoyalty, if (e.effectiveLoyalty < 0.35) colors.bad else colors.good)
        MiniBar(if (e.maxLevel) "Max level" else "XP", if (e.maxLevel) 1.0 else e.xp / e.xpToNext, colors.milestone)
    }
    Row(
        Modifier.fillMaxWidth().padding(start = if (compact) 12.dp else 20.dp, end = if (compact) 4.dp else 8.dp, top = if (compact) 6.dp else 14.dp, bottom = if (compact) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StaffPortrait(e, if (compact) 36.dp else 44.dp)
        Column(Modifier.weight(1f).padding(start = if (compact) 10.dp else 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Name and level first; the tags wrap onto the next line rather than squeezing the name.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(e.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Lv ${e.level}", style = MaterialTheme.typography.labelMedium, color = colors.milestone)
                Tip(e.chapter.detail) { Pill(e.chapter.title, if (e.isStar) colors.warn else colors.milestone) }
                // The trait by name; long-press for what it does.
                e.trait?.let { t -> Tip("${t.title}: ${t.detail}") { Pill(t.title, colors.milestone) } }
                e.specialty?.let { s -> Tip("${s.person}: best at ${s.areas.joinToString(" and ") { it.lower }}") { SpecialtyPill(s) } }
            }
            Text(e.careerLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            pressure?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.warn) }
            note?.let { (text, good) -> Text(text, style = MaterialTheme.typography.bodySmall, color = if (good) colors.good else colors.bad) }
            if (compact) {
                // Sideways: one line of details, the bars to the right.
                Text("$role · $skills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(skills, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) { bars() }
            }
        }
        if (compact) {
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) { bars() }
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
                if (onSpecialise != null) {
                    DropdownMenuItem(text = { Text("Make a specialist") }, leadingIcon = { Icon(Icons.Filled.School, null) }, onClick = {
                        menu = false
                        onSpecialise()
                    })
                }
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
