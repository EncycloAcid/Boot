package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.MAX_PREMIUM
import com.boot.core.MIN_PREMIUM
import com.boot.core.Plan
import com.boot.core.Stance
import com.boot.core.Studio
import com.boot.core.acceptOdds
import com.boot.core.diligenceCost
import com.boot.core.divest
import com.boot.core.divestPrice
import com.boot.core.dueDiligence
import com.boot.core.findingAt
import com.boot.core.makeOffer
import com.boot.core.offerBlocker
import com.boot.core.offerPrice
import com.boot.core.recentGamesBy
import com.boot.core.runStudio
import com.boot.core.stanceOf
import com.boot.core.studioFranchises
import com.boot.core.studioName
import com.boot.core.studioValue
import com.boot.core.withdrawOffer
import com.boot.game.GameStore
import kotlin.math.roundToInt

/*
 * A studio up close: what it makes, how its owners feel, what due diligence found, and either the
 * offer you'd make or (once it's yours) how you run it.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealSheet(store: GameStore, state: GameState, name: String) {
    val colors = gameColors
    val s = com.boot.core.studio(name) ?: return
    val g = state.games ?: return
    val close = { store.dealStudio = null }
    val owned = g.subsidiaries.firstOrNull { it.name == s.name }
    GameDialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close") }
                    StudioBadge(s.name, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(state.studioName(s.name), style = MaterialTheme.typography.titleMedium)
                        Hint((if (s.indie) "Independent studio" else "Publisher") + " · worth ${Format.money(state.studioValue(s))}")
                    }
                    if (owned == null) StanceChip(state.stanceOf(s))
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val about: @Composable () -> Unit = { About(store, state, s) }
                    val act: @Composable () -> Unit = { if (owned != null) Running(store, state, s) else Offer(store, state, s) }
                    if (maxWidth >= 640.dp) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { about() }
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) { act() }
                        }
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            about()
                            act()
                        }
                    }
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun StanceChip(stance: Stance) {
    val colors = gameColors
    val tint = when (stance) {
        Stance.OPEN -> colors.good
        Stance.WARY -> colors.warn
        Stance.CLOSED -> colors.bad
    }
    Pill(stance.title, tint)
}

/** What the studio makes, what its owners think, and what's under the hood. */
@Composable
private fun About(store: GameStore, state: GameState, s: Studio) {
    val colors = gameColors
    val g = state.games ?: return
    val recent = state.recentGamesBy(s)
    GroupedSection("Their games", footer = studioFranchises(s.name).takeIf { it.isNotEmpty() }?.let { "Series: ${it.take(4).joinToString()}." }) {
        if (recent.isEmpty()) {
            CardBlock { Hint("Nothing out lately.") }
        } else {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (r in recent) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CoverWithScore(chartCoverConcept(r.title, r.topic, r.genre), r.score, 64.dp)
                        Text(r.title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                        Hint(GameClock.label(r.week))
                    }
                }
                repeat(4 - recent.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (g.subsidiaries.none { it.name == s.name }) {
        val stance = state.stanceOf(s)
        GroupedSection("The owners") {
            CardBlock {
                Text(stance.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Hint(stance.detail)
            }
        }
    }
    val found = s.name in g.diligence || g.subsidiaries.any { it.name == s.name }
    GroupedSection("Due diligence", footer = if (found) null else "A look at the books, the team and what's in development. Whatever's there comes with the studio either way.") {
        CardBlock {
            if (found) {
                val f = g.subsidiaries.firstOrNull { it.name == s.name }?.finding ?: state.findingAt(s)
                Text(f.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (f.good) colors.good else if (f.name == "CLEAN") MaterialTheme.colorScheme.onSurface else colors.warn)
                Hint(f.detail)
            } else {
                TonalButton("Look at the books · ${Format.money(state.diligenceCost(s))}", { store.perform { dueDiligence(s.name) } }, Modifier.fillMaxWidth())
            }
        }
    }
}

/** The offer you'd make: the premium, what it costs, and the odds. */
@Composable
private fun Offer(store: GameStore, state: GameState, s: Studio) {
    val colors = gameColors
    val g = state.games ?: return
    val pending = g.deal?.takeIf { it.studio == s.name }
    if (pending != null) {
        GroupedSection("Your offer") {
            CardBlock {
                Text("${Format.money(pending.price)} on the table", style = MaterialTheme.typography.titleMedium)
                Hint("${s.name}'s owners answer ${if (pending.answerWeek <= state.week + 1) "next week" else "in ${Format.weeks(pending.answerWeek - state.week)}"}.")
                TonalButton("Withdraw the offer", { store.perform { withdrawOffer() } })
            }
        }
        return
    }
    var premium by remember(s.name) { mutableFloatStateOf(0.15f) }
    var inShares by remember(s.name) { mutableStateOf(false) }
    val price = state.offerPrice(s, premium.toDouble())
    val odds = state.acceptOdds(s, premium.toDouble())
    val blocker = state.offerBlocker(s, premium.toDouble(), inShares)
    GroupedSection("Your offer", footer = "They answer in two weeks: yes, a counter-offer, or no (and then they won't hear another for six months). A big studio can draw a rival bidder, and the biggest deals go before the regulators.") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(Format.money(price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Hint(
                        (if (premium >= 0) "${(premium * 100).roundToInt()}% over" else "${(-premium * 100).roundToInt()}% under") + " what it's worth" +
                            if (inShares) " · ${Format.money(price * 0.5)} in cash, the rest in shares" else "",
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(odds * 100).roundToInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (odds >= 0.6) colors.good else if (odds >= 0.3) colors.warn else colors.bad)
                    Hint("chance of a yes")
                }
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.fill)) {
                Box(Modifier.fillMaxWidth(odds.toFloat()).fillMaxHeight().background(if (odds >= 0.6) colors.good else if (odds >= 0.3) colors.warn else colors.bad))
            }
            Slider(
                value = premium, onValueChange = { premium = (it * 20).roundToInt() / 20f },
                valueRange = MIN_PREMIUM.toFloat()..MAX_PREMIUM.toFloat(),
                modifier = Modifier.semantics { contentDescription = "Premium" },
            )
        }
        if (state.listing != null) {
            RowDivider()
            ToggleRow("Pay half in shares", inShares, "Half the cash. You own a little less of ${state.companyName}.") { inShares = it }
        }
    }
    if (blocker != null) Hint(blocker, colors.bad)
    PrimaryButton("Offer ${Format.money(price)}", { store.perform { makeOffer(s.name, premium.toDouble(), inShares) } }, Modifier.fillMaxWidth(), enabled = blocker == null)
}

/** A studio you own: how it's run, what it earns, and selling it on. */
@Composable
private fun Running(store: GameStore, state: GameState, s: Studio) {
    val colors = gameColors
    val sub = state.games?.subsidiaries?.firstOrNull { it.name == s.name } ?: return
    var selling by remember { mutableStateOf(false) }
    GroupedSection("Earnings") {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (sub.lifetimeProfit <= 0) StatTile("This week", "—", Modifier.weight(1f), "first payout next week")
            else StatTile("This week", sub.lastProfit.perWeek(), Modifier.weight(1f), tint = colors.good)
            StatTile("Since you bought it", Format.money(sub.lifetimeProfit), Modifier.weight(1f), "paid ${Format.money(sub.paid)}")
        }
    }
    val canChange = state.week - sub.planWeek >= 52
    GroupedSection("How you run it", footer = if (canChange) "You can change this once a year." else "You can change this in ${GameClock.label(sub.planWeek + 52)}.") {
        Plan.entries.forEachIndexed { i, p ->
            if (i > 0) RowDivider()
            val on = sub.plan == p
            ListRow(
                p.title, subtitle = p.detail,
                onClick = if (!on && canChange && !(p == Plan.INTEGRATED && sub.folded)) ({ store.perform { runStudio(s.name, p) } }) else null,
                trailing = { if (on) Pill("Now", MaterialTheme.colorScheme.primary) },
            )
        }
    }
    TonalButton("Sell for ${Format.money(state.divestPrice(s))}", { selling = true }, Modifier.fillMaxWidth())
    if (selling) {
        ConfirmDialog(
            title = "Sell ${s.name}?",
            text = "A buyer will pay ${Format.money(state.divestPrice(s))}.",
            confirm = "Sell",
            destructive = true,
            onConfirm = {
                store.perform { divest(s.name) }
                store.dealStudio = null
            },
            onDismiss = { selling = false },
        )
    }
}
