package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.LaunchTiming
import com.boot.core.gameForecast
import com.boot.core.releaseCalendar
import com.boot.core.setReleaseDate
import com.boot.core.showsAhead
import com.boot.game.GameStore
import kotlin.math.roundToInt

/*
 * The release calendar: the next six months as launch weeks for the game in development, each
 * tinted by how well a launch would do then, with the shows and the big rival releases marked.
 * Pick a week to see why, and set it as the release date.
 */

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private fun monthOf(week: Int) = monthNames[(GameClock.weekOfYear(week) - 1) * 12 / 52]

private fun percent(lift: Double): String {
    val pct = ((lift - 1) * 100).roundToInt()
    return if (pct > 0) "+$pct%" else if (pct < 0) "$pct%" else "±0%"
}

@Composable
fun ReleaseCalendar(store: GameStore, state: GameState, team: Int) {
    val colors = gameColors
    val close = { store.calendarTeam = null }
    val p = state.games?.projectOf(team)
    if (p == null) {
        close()
        return
    }
    val calendar = state.releaseCalendar(team)
    val shows = state.showsAhead(calendar.size).groupBy { it.weeks }
    val rivals = state.games!!.market.upcoming.groupBy { it.week }
    val eta = if (p.isReady) 0 else state.gameForecast(team)?.weeksToReady
    val best = calendar.filter { eta == null || it.week - state.week >= eta }.maxByOrNull { it.lift }
    var picked by remember(team) { mutableIntStateOf(p.releaseWeek ?: best?.week ?: calendar.first().week) }
    val chosen = calendar.firstOrNull { it.week == picked } ?: calendar.first()

    GameDialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close") }
                    Column(Modifier.weight(1f)) {
                        Text("Release calendar", style = MaterialTheme.typography.titleMedium)
                        Hint(
                            p.title + " · " + (
                                p.releaseWeek?.let { "launches ${GameClock.label(it)}, in ${Format.weeks(it - state.week)}" }
                                    ?: "no date yet"
                                ),
                        )
                    }
                    Wishlists(p.wishlists, p.reveals > 0)
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 640.dp
                    if (wide) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Column(Modifier.weight(1.5f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                WeekGrid(state, calendar, shows.keys, rivals, picked, p.releaseWeek, eta, columns = 9) { picked = it }
                                Legend()
                            }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                WeekDetail(store, state, team, chosen, shows[chosen.week - state.week].orEmpty().map { it.name }, eta, p.releaseWeek)
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            WeekGrid(state, calendar, shows.keys, rivals, picked, p.releaseWeek, eta, columns = 6) { picked = it }
                            Legend()
                            WeekDetail(store, state, team, chosen, shows[chosen.week - state.week].orEmpty().map { it.name }, eta, p.releaseWeek)
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun Wishlists(count: Double, announced: Boolean) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            if (announced) Format.count(count) else "—",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
        )
        Text("wishlists", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The weeks as a grid of tiles, tinted from red (a poor week to launch) to green (a great one). */
@Composable
private fun WeekGrid(
    state: GameState,
    calendar: List<LaunchTiming>,
    showWeeks: Set<Int>,
    rivals: Map<Int, List<com.boot.core.Announcement>>,
    picked: Int,
    date: Int?,
    eta: Int?,
    columns: Int,
    onPick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in calendar.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (t in row) {
                    val ahead = t.week - state.week
                    WeekTile(
                        t, ahead,
                        month = if (t == calendar.first() || monthOf(t.week) != monthOf(t.week - 1)) monthOf(t.week) else null,
                        show = ahead in showWeeks,
                        rival = rivals[t.week]?.any { it.mega } == true,
                        early = eta != null && ahead < eta,
                        selected = t.week == picked,
                        isDate = t.week == date,
                        modifier = Modifier.weight(1f),
                    ) { onPick(t.week) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WeekTile(
    t: LaunchTiming,
    ahead: Int,
    month: String?,
    show: Boolean,
    rival: Boolean,
    early: Boolean,
    selected: Boolean,
    isDate: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = gameColors
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        t.lift >= 1.12 -> colors.good
        t.lift >= 1.02 -> colors.good.copy(alpha = 0.6f)
        t.lift <= 0.88 -> colors.bad
        t.lift <= 0.98 -> colors.bad.copy(alpha = 0.6f)
        else -> colors.other
    }
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (isDate) scheme.primary.copy(alpha = 0.22f) else tint.copy(alpha = if (early) 0.06f else 0.16f))
            .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else colors.separator, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "Week ${ahead}"
                this.selected = selected
            }
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            month ?: "W${GameClock.weekOfYear(t.week)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (month != null) FontWeight.Bold else FontWeight.Normal,
            color = if (month != null) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            percent(t.lift),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (early) scheme.onSurfaceVariant.copy(alpha = 0.6f) else tint.copy(alpha = 1f),
            maxLines = 1,
        )
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isDate) Dot(scheme.primary)
            if (show) Dot(colors.warn)
            if (rival) Dot(colors.bad)
        }
    }
}

@Composable
private fun Dot(color: Color) = Box(Modifier.size(6.dp).clip(CircleShape).background(color))

@Composable
private fun Legend() {
    val colors = gameColors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        for ((color, label) in listOf(MaterialTheme.colorScheme.primary to "Your date", colors.warn to "A show", colors.bad to "Big rival release")) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(color)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Why a week is good or bad to launch in, and the button to pick it. */
@Composable
private fun WeekDetail(
    store: GameStore,
    state: GameState,
    team: Int,
    t: LaunchTiming,
    shows: List<String>,
    eta: Int?,
    date: Int?,
) {
    val colors = gameColors
    val p = state.games?.projectOf(team) ?: return
    val ahead = t.week - state.week
    Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val label = GameClock.label(t.week)
        val heading = (if (label.startsWith("Y")) "Week ${GameClock.weekOfYear(t.week)} of year ${GameClock.year(t.week)}" else "$label · week ${GameClock.weekOfYear(t.week)}") +
            " · in ${Format.weeks(ahead)}"
        GroupedSection(heading) {
            Factor("${t.season.title} for ${p.concept.genreTitle} games", t.seasonLift)
            t.window?.let {
                RowDivider()
                Factor("${it.title}. ${it.note}", it.lift)
            }
            t.rival?.let {
                RowDivider()
                val mega = t.clash < 0.85
                Factor(
                    "${it.title} from ${it.studio} " + (if (it.week == t.week) "launches the same week" else "launches ${Format.weeks(t.week - it.week)} before") +
                        (if (mega) ". Everyone will be playing it." else ", and it's after the same players."),
                    t.clash,
                )
            }
            for (name in shows) {
                RowDivider()
                Factor("$name is on that week.", null)
            }
            RowDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Launch demand", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    percent(t.lift), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (t.lift >= 1) colors.good else colors.bad,
                )
            }
        }
        when {
            p.reveals == 0 -> Hint("Nobody's heard of ${p.title} yet. Show a trailer at a show to start the wishlists; a date makes them come in faster.")
            else -> Hint("${Format.count(p.wishlists)} people have it wishlisted. With a date to count down to, more sign up each week. Good reviews turn up to a fifth of them into launch-day sales.")
        }
        if (eta != null && ahead < eta) {
            Hint("The team needs about ${Format.weeks(eta)} more. If it isn't finished by then, you'll have to delay, and fans don't like a delay.", colors.bad)
        } else if (eta == null) {
            Hint("Nobody can say yet when it'll be finished.", colors.warn)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                if (t.week == date) "This is the release date" else "Set as release date",
                {
                    store.perform { setReleaseDate(team, t.week) }
                    store.calendarTeam = null
                },
                Modifier.weight(1f),
                enabled = t.week != date,
            )
            if (date != null) TonalButton("Clear date", { store.perform { setReleaseDate(team, null) } })
        }
    }
}

@Composable
private fun Factor(text: String, lift: Double?) {
    val colors = gameColors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (lift != null) {
            Text(
                percent(lift), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
                color = if (lift > 1.001) colors.good else if (lift < 0.999) colors.bad else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
