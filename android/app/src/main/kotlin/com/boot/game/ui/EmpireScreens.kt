package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boot.core.stanceOf
import com.boot.core.Stance
import com.boot.core.Achievement
import com.boot.core.studioName
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.Monetization
import com.boot.core.SEASON_GAP
import com.boot.core.STALE_AFTER
import com.boot.core.Studio
import com.boot.core.acquire
import com.boot.core.acquisitionBlocker
import com.boot.core.acquisitionPrice
import com.boot.core.divest
import com.boot.core.divestPrice
import com.boot.core.endService
import com.boot.core.launchSeason
import com.boot.core.seasonBlocker
import com.boot.core.seasonCost
import com.boot.core.setMonetization
import com.boot.core.studioFranchises
import com.boot.core.studioValue
import com.boot.core.studiosForSale
import com.boot.core.subsidiaryProfit
import com.boot.game.GameStore

/*
 * The empire: studios you've bought and could buy, live games and their seasons, and the
 * achievements cabinet.
 */

/** A studio's mark: its initials on a colour of its own. */
@Composable
fun StudioBadge(name: String, size: Dp = 40.dp) {
    CompanyLogo(name, size = size)
}

private fun Studio.blurb(): String {
    val series = studioFranchises(name)
    val kind = if (indie) "Indie" else "Publisher"
    return if (series.isEmpty()) kind else "$kind · ${series.take(3).joinToString()}"
}

/** Studios you own, and the ones you could buy. Tap one to look it over. */
@Composable
fun StudiosSection(store: GameStore, state: GameState, g: GamesStudio) {
    var showAll by remember { mutableStateOf(false) }
    val colors = gameColors
    g.deal?.let { d ->
        GroupedSection("Waiting on an answer") {
            ListRow(
                d.studio,
                subtitle = "You offered ${Format.money(d.price)}" + (if (d.inShares) ", half in shares" else "") + ". They answer " +
                    (if (d.answerWeek <= state.week + 1) "next week." else "in ${Format.weeks(d.answerWeek - state.week)}."),
                leading = { StudioBadge(d.studio) },
                chevron = true,
                onClick = { store.dealStudio = d.studio },
            )
        }
    }
    if (g.subsidiaries.isNotEmpty()) {
        GroupedSection("Your studios · ${g.subsidiaryProfit.perWeek()}", footer = "Each studio sends home about 6% of its value a year, more when it has hits out, and how you run it changes that. Their games don't compete with yours.") {
            g.subsidiaries.forEachIndexed { i, sub ->
                if (i > 0) RowDivider(68.dp)
                val s = com.boot.core.studio(sub.name) ?: return@forEachIndexed
                ListRow(
                    sub.name,
                    subtitle = "${sub.plan.title} · morale ${Format.percent(sub.morale)} · " +
                        (sub.project?.let { "making ${it.title}" } ?: "earned ${Format.money(sub.lifetimeProfit)}"),
                    leading = { StudioBadge(sub.name) },
                    value = sub.lastProfit.perWeek(),
                    valueColor = if (sub.lastProfit > 0) colors.good else Color.Unspecified,
                    chevron = true,
                    onClick = { store.dealStudio = s.name },
                )
            }
        }
    }
    val forSale = state.studiosForSale()
    if (forSale.isEmpty()) return
    val shown = if (showAll) forSale else forSale.take(4)
    GroupedSection(
        "Buy a studio",
        action = { PlainButton(if (showAll) "Fewer" else "All ${forSale.size}", { showAll = !showAll }) },
        footer = "Tap a studio to look it over, check its books and make an offer. The big publishers only talk to companies three years old or public.",
    ) {
        shown.forEachIndexed { i, s ->
            if (i > 0) RowDivider(68.dp)
            val stance = state.stanceOf(s)
            ListRow(
                state.studioName(s.name),
                subtitle = s.blurb(),
                leading = { StudioBadge(s.name) },
                value = Format.money(state.studioValue(s)),
                valueDetail = stance.title,
                valueDetailColor = when (stance) {
                    Stance.OPEN -> colors.good
                    Stance.WARY -> colors.warn
                    Stance.CLOSED -> colors.bad
                },
                chevron = true,
                onClick = { store.dealStudio = s.name },
            )
        }
    }
}

/** Live games at a glance, for the studio page. */
@Composable
fun LiveGamesSection(store: GameStore, state: GameState, g: GamesStudio) {
    val live = g.releases.filter { it.live?.closed == false }
    if (live.isEmpty()) return
    val colors = gameColors
    GroupedSection("Live games", footer = "Players drift away, and go fast once a game has gone ${STALE_AFTER} weeks without a season. Tap a game to launch one.") {
        live.forEachIndexed { i, r ->
            if (i > 0) RowDivider(78.dp)
            val l = r.live!!
            val stale = state.week - l.lastSeasonWeek > STALE_AFTER
            ListRow(
                r.title,
                subtitle = "${Format.count(l.players)} players · season ${l.seasons + 1}" +
                    if (stale) " · going stale" else " · ${Format.weeks(state.week - l.lastSeasonWeek)} in",
                leading = { CoverWithScore(r.concept, r.score) },
                value = (l.lastRevenue - l.lastCost).perWeek(),
                valueColor = when { l.lastRevenue > l.lastCost -> colors.good; l.lastRevenue < l.lastCost -> colors.bad; else -> Color.Unspecified },
                chevron = true,
                onClick = { store.openRelease = r.id },
            )
        }
    }
}

/** Running a live game: players, money, seasons, the store and the servers. */
@Composable
fun LiveServiceSection(store: GameStore, state: GameState, r: GameRelease) {
    val l = r.live ?: return
    val colors = gameColors
    var confirmEnd by remember { mutableStateOf(false) }
    if (l.closed) {
        GroupedSection("Live service") { CardBlock { Hint("The servers are off. ${Format.money(l.lifetimeRevenue)} came in from players while it ran, over ${l.seasons} seasons.") } }
        return
    }
    val since = state.week - l.lastSeasonWeek
    GroupedSection(
        "Live service",
        footer = when (l.monetization) {
            Monetization.COSMETIC -> "Cosmetics only: players spend less but stay longer, and nobody calls it pay-to-win."
            Monetization.FAIR -> "A fair store: battle passes and skins at fair prices."
            Monetization.AGGRESSIVE -> "Aggressive: loot boxes and pay-to-win. Players spend more and leave faster, and the press notices."
        },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            TileRow {
                StatTile("Players", Format.count(l.players), Modifier.weight(1f), "peak ${Format.count(l.peakPlayers)}")
                StatTile("In-game", l.lastRevenue.perWeek(), Modifier.weight(1f), "servers ${l.lastCost.perWeek()}", tint = if (l.lastRevenue > 0) colors.good else Color.Unspecified)
                StatTile("Seasons", "${l.seasons}", Modifier.weight(1f), if (since > STALE_AFTER) "going stale" else "${Format.weeks(since)} since")
            }
        }
        CardBlock { Segmented(Monetization.entries, l.monetization, { it.title }) { m -> store.perform { setMonetization(r.id, m) } } }
        RowDivider()
        val blocker = state.seasonBlocker(r.id)
        ActionRow(
            "Launch season ${l.seasons + 2}",
            blocker ?: "New content brings lapsed players back. Seasons need at least $SEASON_GAP weeks between them.",
            Format.money(r.seasonCost()),
            enabled = blocker == null,
        ) { store.perform { launchSeason(r.id) } }
        RowDivider()
        ActionRow("Shut down the servers", "Stops the bills for good. Players who are still there won't be happy.", "End", enabled = true) { confirmEnd = true }
    }
    if (confirmEnd) {
        ConfirmDialog(
            title = "Shut down ${r.title}?",
            text = "${Format.count(l.players)} players lose access to the game for good.",
            confirm = "Shut down",
            destructive = true,
            onConfirm = { store.perform { endService(r.id) } },
            onDismiss = { confirmEnd = false },
        )
    }
}

private fun Achievement.Tier.color(): Color = when (this) {
    Achievement.Tier.BRONZE -> Color(0xFFC07A42)
    Achievement.Tier.SILVER -> Color(0xFF8E9AAF)
    Achievement.Tier.GOLD -> Color(0xFFE0A800)
    Achievement.Tier.PLATINUM -> Color(0xFF5FC4D9)
}

/** The trophy cabinet: what you've earned, then what's left. */
@Composable
fun AchievementsSection(state: GameState) {
    val earned = state.achievements
    val all = Achievement.entries.sortedWith(compareBy({ it !in earned }, { -(earned[it] ?: 0) }, { it.tier.ordinal }))
    GroupedSection("Achievements · ${earned.size} of ${Achievement.entries.size}", footer = "Achievements pay nothing. They're there to be collected.") {
        all.forEachIndexed { i, a ->
            if (i > 0) RowDivider(68.dp)
            val got = earned[a]
            ListRow(
                a.title,
                subtitle = a.detail + (got?.let { " · ${GameClock.label(it)}" } ?: ""),
                leading = {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(if (got != null) a.tier.color().copy(alpha = 0.18f) else gameColors.fill, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (got != null) Icons.Filled.EmojiEvents else Icons.Filled.Lock, null,
                            tint = if (got != null) a.tier.color() else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                value = a.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                valueColor = if (got != null) a.tier.color() else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** The newest few, for the office. */
@Composable
fun RecentAchievements(state: GameState, onMore: () -> Unit) {
    val recent = state.achievements.entries.sortedByDescending { it.value }.take(3)
    if (recent.isEmpty()) return
    GroupedSection(
        "Achievements · ${state.achievements.size} of ${Achievement.entries.size}",
        action = { PlainButton("All", onMore) },
    ) {
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            recent.forEach { (a, _) ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(a.tier.color().copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = a.tier.color(), modifier = Modifier.size(28.dp))
                    // Long single words shrink rather than break mid-word.
                    FitText(a.title, MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
