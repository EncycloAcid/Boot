package com.boot.core

import kotlin.math.max
import kotlin.math.min

/*
 * More to buying studios.
 *
 * - Stakes: buy a fifth of a studio (up to two fifths) without a takeover. A share of its profit
 *   comes home every week, its owners warm to you, and what you hold counts toward the price if
 *   you buy the rest.
 * - Earn-outs: pay 70% now and the rest (40% more) in two years, if the studio's still happy,
 *   kept its people and made decent games. Owners like the cash up front better.
 * - The market moves: the industry's giants buy studios too, and now and then sell them on.
 *   If they buy one you hold a stake in, they buy you out.
 * - Buying a studio teaches you what it knows: the topic and genre pairings of its recent games.
 */

/** A stake is bought a fifth at a time, up to two fifths. */
const val STAKE_STEP = 0.2
const val MAX_STUDIO_STAKE = 0.4

/** An earn-out pays this much now, and this much more later if the studio does well. */
const val EARN_OUT_NOW = 0.7
const val EARN_OUT_LATER = 0.4

/** How long an earn-out runs. */
const val EARN_OUT_WEEKS = 104

/** The companies that buy studios when you don't. */
val StudioBuyers = listOf("Tencent", "Microsoft", "Sony", "Embracer Group", "Savvy Games Group")

/** Your stake in [name], 0 if none. */
fun GameState.stakeIn(name: String): Double = games?.stakes?.get(name) ?: 0.0

/** What the next fifth of [s] costs. */
fun GameState.stakePrice(s: Studio): Double = studioValue(s) * STAKE_STEP * 1.05

/** Why you can't buy a (bigger) stake in [s], or null if you can. */
fun GameState.stakeBlocker(s: Studio): String? {
    val g = games ?: return "You need a game studio first."
    if (g.owns(s.name)) return "It's already yours."
    g.rivalOwned[s.name]?.let { return "$it owns ${s.name} now." }
    if (s.name == "Valve") return "Valve is private, and it isn't selling any of itself."
    if (!s.indie && listing == null && week < 52 * 3) return "The big publishers won't talk to a company this young."
    if (stakeIn(s.name) >= MAX_STUDIO_STAKE - 1e-9) return "You hold ${Format.percent(MAX_STUDIO_STAKE)}. To go further, buy them outright."
    val price = stakePrice(s)
    if (cash < price) return "A ${Format.percent(STAKE_STEP)} stake costs ${money(price)}."
    return null
}

/** Buys the next fifth of [name]. */
fun GameState.buyStake(name: String) {
    requireLive()
    val s = studio(name) ?: throw invalid("No such studio.")
    stakeBlocker(s)?.let { throw notAvailable(it) }
    val g = games!!
    val price = stakePrice(s)
    spend(price, Lane.GAMES)
    g.stakes[s.name] = stakeIn(s.name) + STAKE_STEP
    g.stakePaid[s.name] = (g.stakePaid[s.name] ?: 0.0) + price
    stats.stakes += 1
    addLog("You bought ${Format.percent(STAKE_STEP)} of ${s.name} for ${money(price)}. You hold ${Format.percent(stakeIn(s.name))}.", LogTone.GOOD, Lane.GAMES)
    publish(NewsDesk.GAMES, "Bloomberg", "$companyName takes a stake in ${s.name}", "${Format.percent(stakeIn(s.name))} of the studio, for now.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.MONEY)
}

/** What your stake in [s] would sell for. */
fun GameState.stakeValue(s: Studio): Double = studioValue(s) * stakeIn(s.name)

/** Sells your whole stake in [name]. */
fun GameState.sellStake(name: String) {
    requireLive()
    val s = studio(name) ?: throw invalid("No such studio.")
    val g = games ?: throw notAvailable("You don't make games.")
    if (stakeIn(name) <= 0) throw notAvailable("You don't hold any of ${s.name}.")
    val price = stakeValue(s) * 0.95
    g.stakes.remove(name)
    g.stakePaid.remove(name)
    cash += price
    addLog("You sold your stake in ${s.name} for ${money(price)}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** What your stakes are worth, for the company's value. */
val GameState.stakesValue: Double
    get() = games?.stakes?.entries?.sumOf { (name, share) -> studio(name)?.let { studioValue(it) * share } ?: 0.0 } ?: 0.0

/** Each week: your share of the profit of the studios you hold a stake in. */
internal fun GameState.stakesWeek(g: GamesStudio) {
    var total = 0.0
    for ((name, share) in g.stakes) {
        val s = studio(name) ?: continue
        val theirs = g.market.games.filter { canonicalStudio(it.studio) == s.name && week - it.week <= 52 }
        val form = (0.8 + 0.15 * theirs.count { it.score >= 8 } - 0.1 * theirs.count { it.score < 5 }).clamped(0.6, 1.5)
        total += studioValue(s) * 0.06 / 52 * form * share
    }
    g.lastStakeProfit = total
    g.stakeEarned += total
    if (total > 0) {
        cash += total
        recordRevenue(total, Lane.GAMES)
    }
}

/** How much a stake warms the owners to a full offer. */
fun GameState.stakeGoodwill(s: Studio): Double = stakeIn(s.name) * 0.5

// Earn-outs

/** At the end of an earn-out: pay up if the studio did well. */
internal fun GameState.earnOutWeek(g: GamesStudio) {
    for (sub in g.subsidiaries) {
        if (sub.earnOutDue <= 0 || week < sub.earnOutWeek) continue
        val due = sub.earnOutDue
        sub.earnOutDue = 0.0
        val theirs = g.market.games.filter { canonicalStudio(it.studio) == sub.name && it.week >= sub.boughtWeek }
        val good = theirs.isEmpty() || theirs.map { it.score }.average() >= 6.5
        if (sub.morale >= 0.5 && sub.talentLost == 0 && good) {
            spend(due, Lane.GAMES)
            sub.paid += due
            sub.morale = min(1.0, sub.morale + 0.05)
            addLog("${sub.name} hit its targets: you paid the last ${money(due)} of the deal.", LogTone.NEUTRAL, Lane.GAMES)
        } else {
            sub.morale = max(0.0, sub.morale - 0.15)
            reputation.adjust(-1.0)
            addLog("${sub.name} missed its targets, so the sellers don't get the last ${money(due)}. They're bitter about it.", LogTone.NEUTRAL, Lane.GAMES)
            publish(NewsDesk.GAMES, "Kotaku", "${sub.name}'s founders miss out on their payout", "The studio didn't hit the targets $companyName set.", LogTone.BAD, Lane.GAMES, tag = NewsTag.MONEY)
        }
    }
}

// The market moves

/** Each week: now and then a giant buys a studio, and now and then sells one on. */
internal fun GameState.studioMarketWeek(g: GamesStudio) {
    val dice = flavour("studio market")
    // Sold on after five years or so.
    g.rivalOwned.keys.toList().forEach { name ->
        val since = g.rivalOwnedWeek[name] ?: week
        if (week - since >= 260 && dice.chance(0.01)) {
            val owner = g.rivalOwned.remove(name)
            g.rivalOwnedWeek.remove(name)
            publish(NewsDesk.GAMES, "Bloomberg", "$owner puts $name up for sale", "The studio is looking for a new owner.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.MONEY)
        }
    }
    if (!dice.chance(0.003)) return
    val targets = Studios.filter {
        !g.owns(it.name) && it.name !in g.rivalOwned && it.name != "Nintendo" && it.name != "Valve" &&
            g.deal?.studio != it.name && studioActive(it.name, calendarYear) && it.name !in StudioBuyers
    }
    if (targets.isEmpty()) return
    val target = dice.pick(targets)
    val buyer = dice.pick(StudioBuyers.filter { canonicalStudio(it) != target.name })
    g.rivalOwned[target.name] = buyer
    g.rivalOwnedWeek[target.name] = week
    publish(NewsDesk.GAMES, "Bloomberg", "$buyer buys ${target.name}", "The deal is worth about ${money(studioValue(target) * 1.2)}.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.MONEY, lead = !target.indie)
    // A stake gets bought out, at a good price.
    val share = g.stakes.remove(target.name)
    if (share != null) {
        g.stakePaid.remove(target.name)
        val payout = studioValue(target) * share * 1.2
        cash += payout
        recordRevenue(payout, Lane.GAMES)
        addLog("$buyer bought ${target.name} and your ${Format.percent(share)} with it: ${money(payout)}.", LogTone.GOOD, Lane.GAMES)
    } else {
        addLog("$buyer bought ${target.name}. It's off the market.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** What you learn from a studio you buy: the topic and genre pairings of its recent games. */
internal fun GameState.learnFrom(g: GamesStudio, s: Studio): Int {
    var learned = 0
    for (r in g.market.games.filter { canonicalStudio(it.studio) == s.name }.sortedByDescending { it.week }.take(8)) {
        val key = "TG:${r.topic.name}:${r.genre.name}"
        if (key !in g.knownFits) {
            g.knownFits[key] = r.topic.fit(r.genre)
            learned += 1
        }
    }
    return learned
}

/** The genre a studio makes most, if it's made anything. */
fun GameState.mainGenre(s: Studio): Genre? =
    games?.market?.games?.filter { canonicalStudio(it.studio) == s.name }?.groupingBy { it.genre }?.eachCount()?.maxByOrNull { it.value }?.key

/** What a studio you own is working toward next: one of its series, or something new. */
fun GameState.setNextSeries(name: String, series: String?) {
    requireLive()
    val sub = games?.subsidiaries?.firstOrNull { it.name == name } ?: throw invalid("You don't own $name.")
    if (sub.project != null) throw notAvailable("They're already making ${sub.project!!.title}.")
    if (series != null && series !in studioFranchises(name)) throw invalid("$name doesn't make $series.")
    sub.pitchChosen = true
    sub.pitchSeries = series
}

/** What you've put into your studios and stakes, what it's all worth now, and what it's earned. */
data class StudioHoldings(val paid: Double, val worth: Double, val earned: Double)

fun GameState.studioHoldings(): StudioHoldings {
    val g = games ?: return StudioHoldings(0.0, 0.0, 0.0)
    val paid = g.subsidiaries.sumOf { it.paid } + g.stakePaid.values.sum()
    return StudioHoldings(paid, subsidiaryValue + stakesValue, g.subsidiaries.sumOf { it.lifetimeProfit } + g.stakeEarned)
}
