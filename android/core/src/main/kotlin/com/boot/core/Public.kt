package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Life after the IPO. Ringing the bell doesn't have to end the game: list the company and keep
 * running it with a share price that follows the business and its quarterly earnings, raise money
 * by issuing shares or buy them back, and step down whenever you like. Runs that reach year 15
 * can carry on too.
 */

@Serializable
class Listing(
    val ticker: String,
    val ipoWeek: Int,
    val ipoPrice: Double,
    var shares: Double,
) {
    var price = ipoPrice

    /** How the market feels about you, around 1. Earnings beats push it up, misses down. */
    var sentiment = 1.0
    val history = mutableListOf<Double>()
    var lastQuarterRevenue = 0.0
    var lastEarnings: String? = null
    var lastOfferingWeek = -1_000

    val marketCap: Double get() = price * shares
    val changeSinceIpo: Double get() = price / ipoPrice - 1
}

/** Revenue growth per quarter the market expects. */
const val EXPECTED_QUARTERLY_GROWTH = 0.03

internal fun tickerFor(name: String): String {
    val letters = name.uppercase().filter { it in 'A'..'Z' }
    return letters.take(4).padEnd(3, 'X')
}

/** Ring the bell. Retire on the spot, or list the company and keep running it. */
fun GameState.goPublic(keepRunning: Boolean = false) {
    ipoBlocker?.let { throw notAvailable(it) }
    if (keepRunning) list() else end(EndingKind.IPO)
}

/** List on the stock market: sell a fifth of the company to the public and keep going. */
internal fun GameState.list() {
    if (listing != null) return
    val raise = valuation * 0.2
    val shares = 100_000_000.0
    venture.founderEquity *= 0.8
    cash += raise
    stats.ipoRaise = raise
    val l = Listing(tickerFor(companyName), week, valuation / shares, shares)
    l.lastQuarterRevenue = trailingRevenue(weeks = 13)
    listing = l
    if (venture.board == null) venture.board = Board(lastReviewWeek = week)
    val price = String.format(java.util.Locale.US, "%.2f", l.price)
    addLog(
        "You rang the bell. ${l.ticker} opens at \$$price, valuing ${companyName} at ${money(l.marketCap)}. " +
            "The offering raised ${money(raise)}. You own ${Format.percent(venture.founderEquity)}.",
        LogTone.MILESTONE,
    )
}

/** What your stake is worth today. */
val GameState.founderNetWorth: Double
    get() = venture.founderEquity * (listing?.marketCap ?: valuation)

internal fun GameState.tradeWeek() {
    val l = listing ?: return
    l.sentiment += (1 - l.sentiment) * 0.03 + rng.normal(sd = 0.012)
    l.sentiment = l.sentiment.clamped(0.4, 2.0)
    l.price = max(0.01, valuation / l.shares * l.sentiment)
    l.history += l.price
    if (l.history.size > 104) l.history.removeAt(0)

    if (week % GameClock.WEEKS_PER_QUARTER != 0 || week - l.ipoWeek < GameClock.WEEKS_PER_QUARTER) return
    val q = trailingRevenue(weeks = 13)
    val prev = l.lastQuarterRevenue
    l.lastQuarterRevenue = q
    if (prev <= 0) return
    val growth = q / prev - 1
    val surprise = growth - EXPECTED_QUARTERLY_GROWTH
    val move = (surprise * 1.5).clamped(-0.25, 0.25)
    l.sentiment = (l.sentiment * (1 + move)).clamped(0.4, 2.0)
    val pct = (growth * 100).roundToInt()
    val shares = (move * 100).roundToInt()
    val line = "Quarterly earnings: revenue ${if (pct >= 0) "up" else "down"} ${kotlin.math.abs(pct)}%, " +
        (if (surprise >= 0) "beating" else "missing") + " expectations. ${l.ticker} " +
        (if (shares >= 0) "jumps $shares%." else "falls ${-shares}%.")
    l.lastEarnings = line
    addLog(line, if (surprise >= 0) LogTone.GOOD else LogTone.BAD)
}

fun GameState.offeringBlocker(): String? {
    val l = listing ?: return "You're not a public company."
    if (week - l.lastOfferingWeek < 26) return "Investors want six months between offerings."
    return null
}

/** Sell new shares for cash: a tenth more shares, a little dilution, a small dip. */
fun GameState.issueShares() {
    requireLive()
    offeringBlocker()?.let { throw notAvailable(it) }
    val l = listing!!
    val raise = l.marketCap * 0.1
    cash += raise
    l.shares *= 1.1
    venture.founderEquity /= 1.1
    l.sentiment *= 0.97
    l.lastOfferingWeek = week
    addLog("A secondary offering raised ${money(raise)}. You now own ${Format.percent(venture.founderEquity)}.", LogTone.NEUTRAL)
}

/** What buying back 5% of the shares costs today. */
val GameState.buybackCost: Double get() = (listing?.marketCap ?: 0.0) * 0.05

/** Buy back 5% of the shares: your stake grows, the cash goes. */
fun GameState.buyBack() {
    requireLive()
    val l = listing ?: throw notAvailable("You're not a public company.")
    val cost = buybackCost
    if (cash < cost) throw insufficientFunds("A buyback costs ${money(cost)}.")
    spend(cost, null)
    l.shares /= 1.05
    venture.founderEquity = minOf(1.0, venture.founderEquity * 1.05)
    l.sentiment *= 1.02
    addLog("You bought back ${money(cost)} of stock. You now own ${Format.percent(venture.founderEquity)}.", LogTone.GOOD)
}

/** Step down and cash out. Ends the run. */
fun GameState.retire() {
    requireLive()
    if (listing != null) end(EndingKind.IPO) else end(EndingKind.INDIE)
}

/** After an IPO or year 15, keep going. A retired IPO lists the company and carries on. */
fun GameState.keepPlaying() {
    val e = ending ?: throw notAvailable("The game isn't over.")
    if (e.kind != EndingKind.IPO && e.kind != EndingKind.INDIE) throw notAvailable("There's no coming back from this one.")
    ending = null
    freeplay = true
    if (e.kind == EndingKind.IPO && listing == null) list()
    addLog("Not done yet. ${companyName} carries on.", LogTone.MILESTONE)
}
