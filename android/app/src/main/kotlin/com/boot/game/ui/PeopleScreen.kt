package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.Employee
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.assign
import com.boot.core.fire
import com.boot.core.hire
import com.boot.core.setCrunch
import com.boot.core.setFocus
import com.boot.core.setFreelancing
import com.boot.core.takeVacation
import com.boot.game.GameStore
import java.util.Locale

@Composable
fun PeopleScreen(store: GameStore, state: GameState) {
    ScreenColumn {
        item { FounderCard(store, state) }
        item { HiringCard(store, state) }
        item { TeamCard(store, state) }
    }
}

@Composable
private fun FounderCard(store: GameStore, state: GameState) {
    val colors = gameColors
    val f = state.founder
    Section("You, ${f.name}") {
        Meter("Energy", f.energy / 100, if (f.energy < 25) colors.bad else colors.good, "${f.energy.toInt()}/100")
        if (state.lanes.size > 1) {
            Text("Attention", style = MaterialTheme.typography.bodyMedium)
            state.lanes.forEach { lane ->
                CommitSlider(
                    f.focus[lane] ?: 1.0,
                    { "${lane.title}: ${Format.percent(state.founder.attentionShare(lane, state.lanes))} of you" },
                ) { v -> store.perform { setFocus(lane, v) } }
            }
            Hint("Running more than one lane drains your energy every week.")
        }
        state.lanes.forEach { lane ->
            KeyValue("${lane.title} experience", String.format(Locale.US, "+%.1f output", f.skillBonus(lane)))
        }
        HorizontalDivider()
        ToggleRow("Crunch mode (company-wide)", state.crunch, "+40% output, worse polish, morale and energy fall fast.") { on ->
            store.perform { setCrunch(on) }
        }
        ToggleRow(
            "Take freelance gigs", f.freelancing,
            "${f.freelanceIncome(state.week).perWeek()} to cover rent. The company gets a third of you.",
        ) { on -> store.perform { setFreelancing(on) } }
        OutlinedButton(
            onClick = { store.perform { takeVacation() } },
            enabled = !f.isOnVacation,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (f.isOnVacation) "On vacation (${f.vacationWeeksLeft} weeks left)" else "Take two weeks off")
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, detail: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Hint(detail)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HiringCard(store: GameStore, state: GameState) {
    Section("Hiring") {
        Hint("Rivals compete for the same people. Contested candidates vanish fast. Recruiting costs two weeks' salary.")
        state.candidates.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    Hint("${c.role.title} · skill ${c.skill} · ${c.askingSalary.perWeek()}")
                    c.contestedBy?.let { Hint("$it is circling", gameColors.warn) }
                }
                if (state.lanes.size == 1) {
                    OutlinedButton(onClick = { store.perform { hire(c.id, state.lanes.first()) } }) { Text("Hire") }
                } else {
                    MenuButton("Hire into…", state.lanes, { it.businessName }) { lane ->
                        store.perform { hire(c.id, lane) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamCard(store: GameStore, state: GameState) {
    var firing by remember { mutableStateOf<Employee?>(null) }
    Section("Team") {
        Hint(
            "${state.payroll.perWeek()} payroll · ${state.headquarters.name} " +
                "(${state.headquarters.rent.perWeek()})",
        )
        if (state.staff.isEmpty()) Hint("It's just you in the apartment.")
        state.staff.forEachIndexed { i, e ->
            if (i > 0) HorizontalDivider()
            EmployeeRow(e, state.lanes, onAssign = { lane -> store.perform { assign(e.id, lane) } }, onFire = { firing = e })
        }
    }
    firing?.let { e ->
        ConfirmDialog(
            title = "Let ${e.name} go?",
            text = "Four weeks' severance (${Format.money(e.salary * 4)}). Everyone else's morale dips a little.",
            confirm = "Let go",
            onConfirm = { store.perform { fire(e.id) } },
            onDismiss = { firing = null },
        )
    }
}

@Composable
private fun EmployeeRow(e: Employee, lanes: List<Lane>, onAssign: (Lane?) -> Unit, onFire: () -> Unit) {
    val colors = gameColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(e.name, style = MaterialTheme.typography.titleSmall)
            Hint("${e.role.title} · skill ${e.skill} · ${e.salary.perWeek()}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Hint("Morale ")
                LinearProgressIndicator(
                    progress = { e.morale.toFloat() },
                    modifier = Modifier.width(80.dp).height(4.dp),
                    color = if (e.morale < 0.35) colors.bad else colors.good,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
            }
        }
        MenuButton(e.lane?.title ?: "Unassigned", listOf<Lane?>(null) + lanes, { it?.businessName ?: "Unassigned" }, onPick = onAssign)
        IconButton(onClick = onFire) { Icon(Icons.Filled.PersonRemove, "Let ${e.name} go") }
    }
}
