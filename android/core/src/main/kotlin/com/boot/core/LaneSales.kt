package com.boot.core

import kotlin.math.max
import kotlin.math.roundToLong

/*
 * Selling a business, and keeping one to focus on.
 *
 * Any business but your last can be sold, any time. A buyer (usually the rival in that market)
 * pays what it's worth: more if it's making money, less if it isn't, and a games business takes
 * the studios you own with it. People whose craft carries over to a business you keep come with
 * you; the rest stay with the buyer. Sell down to one and the whole company pulls one way: a year
 * of extra output in the business you kept.
 */

/** How long being down to one business keeps everyone pulling the same way. */
const val FOCUS_WEEKS = 52

/** What focusing adds to the business you kept, while it lasts. */
const val FOCUS_OUTPUT = 1.12

/** What a buyer pays for [lane] today. */
fun GameState.laneSalePrice(lane: Lane): Double {
    if (lane !in lanes) return 0.0
    val profit = trailingProfit(lane, 13)
    // Buyers pay up for a business that makes money and haggle over one that doesn't.
    val mood = if (profit > 0) 1.15 else 0.8
    val studios = if (lane == Lane.GAMES) (subsidiaryValue + stakesValue + storeValue) * 0.9 else 0.0
    val price = laneValue(lane) * mood + studios
    return (max(scaled(25_000.0), price) / 1_000).roundToLong() * 1_000.0
}

/** Who'd buy [lane]: the rival in that market, or a company that's always shopping. */
fun GameState.laneBuyer(lane: Lane): String = world.rival(lane)?.name ?: when (lane) {
    Lane.GAMES -> "Embracer"
    Lane.WEBSITES -> "Accenture"
    Lane.APPS -> "IAC"
    Lane.SOCIAL -> "Meta"
    Lane.HARDWARE -> "Samsung"
    Lane.AI -> "Microsoft"
    Lane.OS -> "Google"
}

/** Why [lane] can't be sold, or null if it can. */
fun GameState.sellLaneBlocker(lane: Lane): String? = when {
    ending != null -> "The run is over."
    lane !in lanes -> "You don't run ${lane.businessName.lowercase()}."
    lanes.size < 2 -> "It's your only business. Keep at least one."
    pendingPivot != null -> "Decide the Pivot Point first."
    else -> null
}

/** The people who'd come with you if [lane] were sold: those whose craft fits a business you keep. */
fun GameState.followersOf(lane: Lane, keeping: List<Lane> = lanes - lane): List<Employee> =
    staff(lane).filter { e -> keeping.any { it.weight(e.role) >= 0.8 } }

/** Sells [lane] to its buyer. */
fun GameState.sellLane(lane: Lane) {
    requireLive()
    sellLaneBlocker(lane)?.let { throw notAvailable(it) }
    val price = laneSalePrice(lane)
    val buyer = laneBuyer(lane)
    val keeping = lanes - lane
    // People whose craft carries over move to the business they fit best; the rest go with the sale.
    val followers = followersOf(lane, keeping)
    for (e in followers) e.lane = keeping.maxBy { it.weight(e.role) }
    val stayed = staff.count { it.lane == lane }
    staff = staff.filterNot { it.lane == lane }
    if (lane == Lane.GAMES) games?.subsidiaries?.clear()
    // Decisions about a business you no longer own go with it.
    events = events.filterNot { it.lane == lane }
    inbox = inbox.filterNot { it.lane == lane }
    closeLane(lane)
    cash += price
    stats.saleProceeds += price
    stats.lanesSold += 1
    addLog(
        "You sold the ${lane.businessName.lowercase()} to $buyer for ${money(price)}." +
            (if (followers.isNotEmpty()) " ${followers.size} of your people came with you." else "") +
            (if (stayed > 0) " $stayed stayed with $buyer." else ""),
        LogTone.MILESTONE,
    )
    publish(
        NewsDesk.YOU, "Bloomberg", "$buyer buys $companyName's ${lane.businessName.lowercase()} for ${money(price)}",
        if (keeping.size == 1) "$companyName is going all in on ${keeping.first().title}." else "$companyName is slimming down to ${keeping.size} businesses.",
        LogTone.NEUTRAL, null, tag = NewsTag.MONEY,
    )
    stats.profitableStreak = 0
    if (keeping.size == 1) {
        focusedUntil = week + FOCUS_WEEKS
        addLog("Everyone's on ${keeping.first().title} now. The whole company pulls one way: more done there for a year.", LogTone.GOOD, keeping.first())
    }
}

/** What selling everything but [keep] brings in, all told. */
fun GameState.focusPrice(keep: Lane): Double = (lanes - keep).sumOf { laneSalePrice(it) }

/** Why you can't focus on [keep], or null if you can. */
fun GameState.focusBlocker(keep: Lane): String? = when {
    keep !in lanes -> "You don't run ${keep.businessName.lowercase()}."
    lanes.size < 2 -> "It's already your only business."
    else -> (lanes - keep).firstNotNullOfOrNull { sellLaneBlocker(it) }
}

/** Sells every business but [keep]. */
fun GameState.focusOn(keep: Lane) {
    requireLive()
    focusBlocker(keep)?.let { throw notAvailable(it) }
    for (lane in lanes - keep) sellLane(lane)
}

/** True while being down to one business is still paying off. */
val GameState.focused: Boolean get() = lanes.size == 1 && week < focusedUntil
