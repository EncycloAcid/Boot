package com.boot.game.ui

import androidx.compose.ui.platform.testTag
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
import com.boot.core.setNextSeries
import com.boot.core.EARN_OUT_LATER
import com.boot.core.EARN_OUT_NOW
import com.boot.core.offerCash
import com.boot.core.MAX_STUDIO_STAKE
import com.boot.core.STAKE_STEP
import com.boot.core.stakeValue
import com.boot.core.stakePrice
import com.boot.core.stakeIn
import com.boot.core.stakeBlocker
import com.boot.core.sellStake
import com.boot.core.buyStake
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
import com.boot.core.StudioBudget
import com.boot.core.cancelStudioGame
import com.boot.core.fundedGames
import com.boot.core.greenlight
import com.boot.core.greenlightBlocker
import com.boot.core.moraleTarget
import com.boot.core.studioGameProgress
import com.boot.core.studioPitch
import com.boot.core.CoDevKind
import com.boot.core.defaultCoDevSplit
import com.boot.core.proposeCoDev
import com.boot.core.proposeCoDevBlocker
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
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(colors.separator))
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
        val owner = g.rivalOwned[s.name]
        GroupedSection("The owners") {
            CardBlock {
                if (owner != null) {
                    Text("Owned by $owner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Hint("They bought it ${GameClock.label(g.rivalOwnedWeek[s.name] ?: state.week)}. Giants sell studios on now and then.")
                } else {
                    Text(stance.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Hint(stance.detail)
                }
            }
        }
        if (owner == null) StakeSection(store, state, s)
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
    var payment by remember(s.name) { mutableStateOf(Payment.CASH) }
    val inShares = payment == Payment.SHARES
    val earnOut = payment == Payment.EARN_OUT
    val price = state.offerPrice(s, premium.toDouble())
    val odds = state.acceptOdds(s, premium.toDouble(), earnOut)
    val blocker = state.offerBlocker(s, premium.toDouble(), inShares, earnOut)
    val now = state.offerCash(s, premium.toDouble(), inShares, earnOut)
    val stake = state.stakeIn(s.name)
    GroupedSection("Your offer", footer = "They answer in two weeks: yes, a counter-offer, or no (and then they won't hear another for six months). A big studio can draw a rival bidder, and the biggest deals go before the regulators.") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(Format.money(price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Hint(
                        (if (premium >= 0) "${(premium * 100).roundToInt()}% over" else "${(-premium * 100).roundToInt()}% under") + " what it's worth" +
                            (if (stake > 0) " · your ${Format.percent(stake)} stake covers ${Format.money(price * stake)}" else "") +
                            when (payment) {
                                Payment.SHARES -> " · ${Format.money(now)} in cash, the rest in shares"
                                Payment.EARN_OUT -> " · ${Format.money(now)} now, ${Format.money(price * (1 - stake) * EARN_OUT_LATER)} in two years if they hit their targets"
                                Payment.CASH -> if (stake > 0) " · ${Format.money(now)} in cash" else ""
                            },
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
        RowDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = Payment.entries.filter { it != Payment.SHARES || state.listing != null }
            Segmented(options, payment, { it.title }) { payment = it }
            Hint(
                when (payment) {
                    Payment.CASH -> "All of it now."
                    Payment.SHARES -> "Half the cash. You own a little less of ${state.companyName}."
                    Payment.EARN_OUT -> "${Format.percent(EARN_OUT_NOW)} now, and ${Format.percent(EARN_OUT_LATER)} more in two years if the studio's still happy, kept its people and made decent games. Owners would rather have the money now."
                },
            )
        }
    }
    if (blocker != null) Hint(blocker, colors.bad)
    PrimaryButton("Offer ${Format.money(price)}", { store.perform { makeOffer(s.name, premium.toDouble(), inShares, earnOut) } }, Modifier.fillMaxWidth(), enabled = blocker == null)
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
        if (sub.earnOutDue > 0) {
            RowDivider()
            ValueRow("Still owed if they hit their targets", "${Format.money(sub.earnOutDue)} · ${GameClock.label(sub.earnOutWeek)}")
        }
    }
    GroupedSection(
        "The team",
        footer = "Morale heads toward ${Format.percent(sub.plan.moraleTarget())} the way you run them. Hits, bigger budgets and leaving them be raise it; flops, cuts and cancelled games lower it." +
            (if (sub.talentLost > 0) " Senior people have walked out ${if (sub.talentLost == 1) "once" else "${sub.talentLost} times"}." else ""),
    ) {
        CardBlock {
            Meter(
                "Morale", sub.morale,
                when {
                    sub.morale >= 0.6 -> colors.good
                    sub.morale >= 0.35 -> colors.warn
                    else -> colors.bad
                },
                caption = "${Format.percent(sub.morale)} · " + when {
                    sub.morale >= 0.75 -> "thriving"
                    sub.morale >= 0.6 -> "happy"
                    sub.morale >= 0.35 -> "uneasy"
                    else -> "close to walking out"
                },
            )
        }
    }
    NextGame(store, state, s, sub)
    CoDevFromSubsidiary(store, state, s)
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

/** Invite an owned studio onto your large/AAA project as a co-dev partner. */
@Composable
private fun CoDevFromSubsidiary(store: GameStore, state: GameState, s: Studio) {
    val team = listOf(1, 2).firstOrNull { t -> state.proposeCoDevBlocker(t, s.name) == null }
    val split = defaultCoDevSplit(CoDevKind.SUBSIDIARY)
    val block = when {
        team == null -> state.games?.project?.let { state.proposeCoDevBlocker(1, s.name) }
            ?: "Start a large or AAA project to bring them on as co-dev."
        else -> null
    }
    GroupedSection(
        "Co-dev on your project",
        footer = "Different from greenlighting their own game — they join your mega-project. Shared cost, credit, and royalties.",
    ) {
        ActionRow(
            title = "Propose co-dev with ${s.name}",
            detail = block
                ?: "Default split ${Format.percent(split.cost)} cost · ${Format.percent(split.work)} work · ${Format.percent(split.royalty)} royalties.",
            action = "Propose",
            enabled = block == null && team != null,
        ) {
            val t = team ?: return@ActionRow
            store.perform {
                proposeCoDev(t, s.name, split.cost, split.work, split.credit, split.royalty)
            }
        }
    }
}

/** The game a studio you own is making with your money, or the one it wants to make next. */
@Composable
private fun NextGame(store: GameStore, state: GameState, s: Studio, sub: com.boot.core.Subsidiary) {
    val p = sub.project
    if (p != null) {
        var cancelling by remember { mutableStateOf(false) }
        GroupedSection("Their next game", footer = "You keep ${Format.percent(com.boot.core.FUNDED_SHARE)} of what it sells, on top of the studio's own profit.") {
            CardBlock {
                Text(p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Hint("${p.size.title} ${p.topic.title.lowercase()} ${p.genre.lower} game · ${p.budget.title.lowercase()} budget, ${Format.money(p.cost)}")
                Meter("In development", state.studioGameProgress(p), caption = "out ${GameClock.label(p.doneWeek)}")
                PlainButton("Cancel it", { cancelling = true })
            }
        }
        if (cancelling) {
            ConfirmDialog(
                title = "Cancel ${p.title}?",
                text = "The ${Format.money(p.cost)} is gone, and the team at ${s.name} will take it hard.",
                confirm = "Cancel the game",
                destructive = true,
                onConfirm = {
                    store.perform { cancelStudioGame(s.name) }
                    cancelling = false
                },
                onDismiss = { cancelling = false },
            )
        }
        return
    }
    val pitch = state.studioPitch(sub)
    var budget by remember { mutableStateOf(StudioBudget.NORMAL) }
    val blocker = state.greenlightBlocker(s.name, budget)
    GroupedSection("Their pitch", footer = blocker ?: "${budget.blurb} About ${Format.weeks(pitch.doneWeek - pitch.startWeek)} to make. It comes out under their name; you keep ${Format.percent(com.boot.core.FUNDED_SHARE)} of what it sells.") {
        CardBlock {
            Text(pitch.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Hint(
                "${pitch.size.title} ${pitch.topic.title.lowercase()} ${pitch.genre.lower} game" +
                    (pitch.franchise?.let { " · the next $it" } ?: " · something new"),
            )
            val series = studioFranchises(s.name)
            if (series.isNotEmpty()) {
                Text("What should they make?", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf<String?>(null) + series).forEach { option ->
                        androidx.compose.material3.FilterChip(
                            selected = pitch.franchise == option,
                            onClick = { store.perform { setNextSeries(s.name, option) } },
                            label = { Text(option ?: "Something new") },
                        )
                    }
                }
            }
            Segmented(StudioBudget.entries, budget, { it.title }) { budget = it }
            PrimaryButton("Greenlight it · ${Format.money(pitch.cost * budget.cost)}", { store.perform { greenlight(s.name, budget) } }, Modifier.fillMaxWidth(), enabled = blocker == null)
        }
    }
    val funded = state.fundedGames(sub)
    if (funded.isNotEmpty()) {
        GroupedSection("Games you paid for") {
            funded.take(4).forEachIndexed { i, r ->
                if (i > 0) RowDivider()
                ValueRow(r.title, String.format(java.util.Locale.US, "%.1f", r.score))
            }
        }
    }
}

/** A stake: a fifth of the studio at a time, up to two fifths, without a takeover. */
@Composable
private fun StakeSection(store: GameStore, state: GameState, s: Studio) {
    val colors = gameColors
    val held = state.stakeIn(s.name)
    val blocker = state.stakeBlocker(s)
    GroupedSection(
        "A stake",
        Modifier.testTag("stake"),
        footer = "Buy ${Format.percent(STAKE_STEP)} at a time, up to ${Format.percent(MAX_STUDIO_STAKE)}. That share of their profit comes home every week, the owners warm to you, and what you hold comes off the price if you buy the rest. If someone else buys them, they buy you out.",
    ) {
        if (held > 0) {
            ValueRow("You hold", Format.percent(held))
            RowDivider()
            ValueRow("Worth", Format.money(state.stakeValue(s)), colors.good)
            RowDivider()
        }
        CardBlock {
            PrimaryButton(
                if (held > 0) "Buy another ${Format.percent(STAKE_STEP)} · ${Format.money(state.stakePrice(s))}" else "Buy ${Format.percent(STAKE_STEP)} · ${Format.money(state.stakePrice(s))}",
                { store.perform { buyStake(s.name) } }, Modifier.fillMaxWidth(), enabled = blocker == null,
            )
            if (blocker != null) Hint(blocker)
            if (held > 0) TonalButton("Sell your stake · ${Format.money(state.stakeValue(s) * 0.95)}", { store.perform { sellStake(s.name) } }, Modifier.fillMaxWidth())
        }
    }
}

/** How an offer is paid. */
private enum class Payment(val title: String) { CASH("Cash"), SHARES("Half shares"), EARN_OUT("Earn-out") }
