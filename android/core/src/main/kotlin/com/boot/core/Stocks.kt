package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * The stock market. Every listed company in the league table has a share price that follows what
 * it's worth, the era, and a mood of its own that wanders week to week. Buy and sell shares with
 * spare cash (a little goes on fees), collect dividends from the old giants, and take a stake big
 * enough to matter: past 5% the papers notice, and a rival you're buying into gets angry.
 *
 * With enough money you can bid for a whole company. The bid sits with shareholders for four weeks;
 * a bigger premium and a bigger stake make it likelier to pass. A company you own pays you a share
 * of its profits, counts towards your value and leaves the table. Take over a business rival and
 * they're out of your market for good; take over a games publisher and its games become yours.
 *
 * Once you're public, it works the other way: raiders build stakes in you, and with under half the
 * company yourself, a hostile bid can take it away.
 */

/** A company you can trade: who it is today, what it's worth, and its share price. */
data class Stock(
    val key: String,
    val name: String,
    val ticker: String,
    val lane: Lane?,
    val value: Double,
    val price: Double,
    /** Its shares trade on a public market. Private companies can only be watched. */
    val listed: Boolean,
    /** Price change over the last four weeks, as a fraction. */
    val change: Double,
    /** How much of it you own, 0...1. */
    val stake: Double,
)

/** Shares you hold in one company, and what you paid for them in all. */
@Serializable
data class Holding(val key: String, var shares: Double, var cost: Double, var disclosed: Boolean = false)

/** A company you took over: it's yours now. */
@Serializable
data class OwnedCompany(
    val key: String,
    val name: String,
    val week: Int,
    val paid: Double,
    var lastProfit: Double = 0.0,
    var lifetimeProfit: Double = 0.0,
)

/** Your bid for a company, with the shareholders until [decidesWeek]. The money is held aside. */
@Serializable
data class TakeoverBid(val key: String, val name: String, val premium: Double, val cost: Double, val odds: Double, val decidesWeek: Int)

/** Someone building a stake in you. */
@Serializable
data class Raid(val raider: String, var stake: Double, var bidWeek: Int = -1, var friendly: Boolean = false)

@Serializable
class Portfolio {
    val holdings = mutableListOf<Holding>()
    val owned = mutableListOf<OwnedCompany>()

    /** Each company's mood, around 1: how the market feels about it this week. */
    val moods = mutableMapOf<String, Double>()

    /** Share prices for the last year, oldest first. */
    val history = mutableMapOf<String, MutableList<Double>>()

    /** Profit taken from sales, and dividends paid, since the start. */
    var realized = 0.0
    var dividends = 0.0
    var bid: TakeoverBid? = null
    var raid: Raid? = null

    /** A poison pill keeps raiders out until this week. */
    var pillUntil = -1
}

/** Bids premiums on offer: what you pay over the share price. */
val TakeoverPremiums = listOf(0.25, 0.4, 0.6)

/** Fees on every trade. */
const val TRADING_FEE = 0.005

/** Most you can own by buying on the market. Beyond that you have to bid for the whole company. */
const val MAX_STAKE = 0.3

/** When the papers notice your stake. */
const val DISCLOSED_STAKE = 0.05

/** The year each company's shares started trading. Companies missing here are private. */
private val ListedSince = mapOf(
    "NVIDIA" to 1999, "Apple" to 1980, "Microsoft" to 1986, "Alphabet" to 2004, "Amazon" to 1997, "Meta" to 2012, "Tesla" to 2010,
    "TSMC" to 1994, "Tencent" to 2004, "Netflix" to 2002, "Samsung" to 1975, "Sony" to 1958, "Shopify" to 2015, "Spotify" to 2018,
    "Nintendo" to 1962, "Roblox" to 2021, "Take-Two Interactive" to 1997, "Electronic Arts" to 1989, "Reddit" to 2024,
    "Pinterest" to 2019, "Duolingo" to 2021, "Snap" to 2017, "Capcom" to 1990, "Wix" to 2013, "Squarespace" to 2021,
    "CD Projekt" to 1994, "Ubisoft" to 1996, "IBM" to 1915, "Atari" to 1972, "Commodore" to 1962, "AOL" to 1992, "Yahoo" to 1996,
    "Nokia" to 1915, "BlackBerry" to 1997,
)

/** Real tickers, where they're well known. */
private val Tickers = mapOf(
    "NVIDIA" to "NVDA", "Apple" to "AAPL", "Microsoft" to "MSFT", "Alphabet" to "GOOGL", "Amazon" to "AMZN", "Meta" to "META",
    "Tesla" to "TSLA", "TSMC" to "TSM", "Tencent" to "TCEHY", "Netflix" to "NFLX", "Samsung" to "SSNLF", "Sony" to "SONY",
    "Shopify" to "SHOP", "Spotify" to "SPOT", "Nintendo" to "NTDOY", "Roblox" to "RBLX", "Take-Two Interactive" to "TTWO",
    "Electronic Arts" to "EA", "Reddit" to "RDDT", "Pinterest" to "PINS", "Duolingo" to "DUOL", "Snap" to "SNAP", "Capcom" to "CCOEY",
    "Wix" to "WIX", "Squarespace" to "SQSP", "CD Projekt" to "CDR", "Ubisoft" to "UBI", "IBM" to "IBM", "Atari" to "ATARI",
    "Commodore" to "CBU", "AOL" to "AOL", "Yahoo" to "YHOO", "Nokia" to "NOK", "BlackBerry" to "BB",
)

/** Games publishers you can take over that are also studios on the games market. */
private fun studioFor(key: String): Studio? = studio(if (key == "CD Projekt") "CD Projekt Red" else key)

/** How the market feels about a company this week. */
internal fun GameState.mood(key: String): Double = portfolio.moods[key] ?: 1.0

/** How many shares a company has: set so its price starts somewhere between $20 and $320. */
private fun sharesOutstanding(key: String): Double {
    val base = industryBase(key)?.first ?: 1e9
    return base / (20.0 + key.hashCode().absoluteValue % 300)
}

private fun GameState.isListed(key: String): Boolean = (ListedSince[key] ?: Int.MAX_VALUE) <= calendarYear

private fun GameState.holding(key: String): Holding? = portfolio.holdings.firstOrNull { it.key == key }

/** Everyone you can trade or watch, biggest first. */
fun GameState.stocks(): List<Stock> = industry().sortedByDescending { it.value }.map { c -> stockOf(c) }

private fun GameState.stockOf(c: RankedCompany): Stock {
    val shares = sharesOutstanding(c.key)
    val price = c.value / shares
    val past = portfolio.history[c.key]?.let { it.getOrNull(it.size - 5) }
    return Stock(
        c.key, c.name, Tickers[c.key] ?: tickerFor(c.name), c.lane, c.value, price, isListed(c.key),
        change = if (past != null && past > 0) price / past - 1 else 0.0,
        stake = (holding(c.key)?.shares ?: 0.0) / shares,
    )
}

fun GameState.stock(key: String): Stock? = industry().firstOrNull { it.key == key }?.let { stockOf(it) }

/** What your shares are worth today. */
val GameState.portfolioValue: Double
    get() {
        if (portfolio.holdings.isEmpty()) return 0.0
        val prices = industry().associate { it.key to it.value / sharesOutstanding(it.key) }
        return portfolio.holdings.sumOf { h -> h.shares * (prices[h.key] ?: lastPrice(h.key)) }
    }

/** What the companies you took over are worth. */
val GameState.ownedCompaniesValue: Double
    get() = portfolio.owned.sumOf { ownedValue(it) }

private fun GameState.lastPrice(key: String): Double = portfolio.history[key]?.lastOrNull() ?: 0.0

/** A company you own is worth what it would be on the table. */
fun GameState.ownedValue(o: OwnedCompany): Double {
    val (base, lane) = industryBase(o.key) ?: return o.paid
    return base * world.demand(lane).pow(1.5) * industryGrowth * mood(o.key)
}

// Trading

fun GameState.buyBlocker(key: String, amount: Double): String? {
    val s = stock(key) ?: return "That company isn't around any more."
    if (!s.listed) return "${s.name} is private: its shares don't trade."
    if (amount < 1_000) return "The smallest trade is \$1k."
    if (cash < amount * (1 + TRADING_FEE)) return "You have ${money(cash)}."
    if (s.stake + amount / s.value > MAX_STAKE + 1e-9) return "Past ${Format.percent(MAX_STAKE)} you'd have to bid for the whole company."
    return null
}

/** The most you could put into a company right now. */
fun GameState.maxBuy(key: String): Double {
    val s = stock(key) ?: return 0.0
    return max(0.0, min(cash / (1 + TRADING_FEE), (MAX_STAKE - s.stake) * s.value))
}

/** Buy [amount] worth of a company's shares. */
fun GameState.buyShares(key: String, amount: Double) {
    requireLive()
    buyBlocker(key, amount)?.let { throw notAvailable(it) }
    val s = stock(key)!!
    cash -= amount * (1 + TRADING_FEE)
    val h = holding(key) ?: Holding(key, 0.0, 0.0).also { portfolio.holdings += it }
    h.shares += amount / s.price
    h.cost += amount * (1 + TRADING_FEE)
    addLog("You bought ${money(amount)} of ${s.name} (${s.ticker}) at ${price(s.price)}.", LogTone.NEUTRAL)
    val stake = h.shares / sharesOutstanding(key)
    if (!h.disclosed && stake >= DISCLOSED_STAKE) {
        h.disclosed = true
        publish(
            NewsDesk.MARKETS, businessOutlet(), "$companyName takes a ${Format.percent(stake)} stake in ${s.name}",
            "Analysts wonder what $companyName wants with it.", LogTone.NEUTRAL,
        )
        world.rivals.firstOrNull { it.name == s.name }?.let { r ->
            r.heat = min(1.0, r.heat + 0.25)
            addLog("${r.name} noticed your stake. They're not happy about it.", LogTone.BAD, r.lane)
        }
    }
}

/** Sell [fraction] of what you hold in a company. */
fun GameState.sellShares(key: String, fraction: Double = 1.0) {
    requireLive()
    val h = holding(key) ?: throw invalid("You don't own any.")
    val s = stock(key) ?: throw notAvailable("That company isn't around any more.")
    val f = fraction.coerceIn(0.0, 1.0)
    val shares = h.shares * f
    val proceeds = shares * s.price * (1 - TRADING_FEE)
    val basis = h.cost * f
    cash += proceeds
    portfolio.realized += proceeds - basis
    h.shares -= shares
    h.cost -= basis
    if (h.shares * s.price < 1.0) portfolio.holdings.remove(h)
    val gain = proceeds - basis
    addLog(
        "You sold ${money(proceeds)} of ${s.name}, " + (if (gain >= 0) "a ${money(gain)} profit." else "a ${money(-gain)} loss."),
        if (gain >= 0) LogTone.GOOD else LogTone.BAD,
    )
}

/** What a holding is worth today, and how it's done. */
fun GameState.holdingValue(h: Holding): Double = h.shares * (stock(h.key)?.price ?: lastPrice(h.key))

private fun price(p: Double): String = if (p >= 100) "\$${p.toInt()}" else "\$" + String.format(java.util.Locale.US, "%.2f", p)

/** A share price, the way a ticker shows it. */
fun sharePrice(p: Double): String = price(p)

// Takeovers

/** What buying the rest of a company costs at [premium] over its price, and the chance shareholders say yes. */
fun GameState.takeoverQuote(key: String, premium: Double): Pair<Double, Double>? {
    val s = stock(key) ?: return null
    val cost = (1 - s.stake) * s.value * (1 + premium)
    val odds = (0.15 + premium * 1.1 + s.stake * 1.5).coerceAtMost(0.95)
    return cost to odds
}

fun GameState.takeoverBlocker(key: String, premium: Double): String? {
    val s = stock(key) ?: return "That company isn't around any more."
    if (portfolio.bid != null) return "You already have a bid with shareholders."
    if (!s.listed) return "${s.name} is private. Its owners aren't selling."
    if (listing == null && week < 52 * 3) return "Nobody takes a bid from a company this young seriously."
    val (cost, _) = takeoverQuote(key, premium)!!
    if (cash < cost) return "Costs ${money(cost)}. You have ${money(cash)}."
    return null
}

/** Bid for the whole company. The money is set aside until the shareholders decide. */
fun GameState.bidFor(key: String, premium: Double) {
    requireLive()
    takeoverBlocker(key, premium)?.let { throw notAvailable(it) }
    val s = stock(key)!!
    val (cost, odds) = takeoverQuote(key, premium)!!
    cash -= cost
    portfolio.bid = TakeoverBid(key, s.name, premium, cost, odds, week + 4)
    addLog("You bid ${money(cost)} for ${s.name}, ${Format.percent(premium)} over the market. Shareholders decide in four weeks.", LogTone.MILESTONE)
    publish(
        NewsDesk.MARKETS, businessOutlet(), "$companyName bids ${money(cost)} for ${s.name}",
        "The offer is ${Format.percent(premium)} over ${s.ticker}'s share price.", LogTone.NEUTRAL, lead = true,
    )
}

private fun GameState.decideBid(b: TakeoverBid, rng: SeededRNG) {
    portfolio.bid = null
    if (!rng.chance(b.odds) || stock(b.key) == null) {
        val fees = b.cost * 0.02
        cash += b.cost - fees
        portfolio.moods[b.key] = mood(b.key) * 1.05
        addLog("${b.name}'s shareholders turned your bid down. Advisers' fees cost ${money(fees)}.", LogTone.BAD)
        publish(NewsDesk.MARKETS, businessOutlet(), "${b.name} shareholders reject $companyName", "The bid was ${money(b.cost)}.", LogTone.BAD)
        return
    }
    portfolio.holdings.removeAll { it.key == b.key }
    stats.acquisitions += 1
    stats.takeovers += 1
    reputation.adjust(4.0)
    val studio = studioFor(b.key)
    val g = games
    if (studio != null && g != null && !g.owns(studio.name)) {
        // A games publisher joins the studio: its games are yours now.
        g.subsidiaries += Subsidiary(studio.name, week, b.cost)
    } else {
        portfolio.owned += OwnedCompany(b.key, b.name, week, b.cost)
    }
    addLog("${b.name} is yours, for ${money(b.cost)}.", LogTone.MILESTONE)
    publish(NewsDesk.YOU, businessOutlet(), "$companyName takes over ${b.name}", "Shareholders accepted ${money(b.cost)}.", LogTone.GOOD, lead = true)
    world.rivals.firstOrNull { it.name == b.name }?.let { r ->
        // Your rival is part of you now.
        r.strength = 0.05
        r.heat = 0.0
        r.quietUntil = Int.MAX_VALUE / 2
        stats.rivalsBeaten += 1
        addLog("${r.name} won't be fighting you in ${r.lane.industry} any more.", LogTone.MILESTONE, r.lane)
    }
}

/** Sell a company you took over, for 90% of what it's worth. */
fun GameState.sellCompany(key: String) {
    requireLive()
    val o = portfolio.owned.firstOrNull { it.key == key } ?: throw invalid("You don't own that.")
    val price = ownedValue(o) * 0.9
    portfolio.owned.remove(o)
    cash += price
    addLog("You sold ${o.name} for ${money(price)}.", LogTone.NEUTRAL)
    publish(NewsDesk.MARKETS, businessOutlet(), "$companyName sells ${o.name} for ${money(price)}", tone = LogTone.NEUTRAL)
}

// The week

/** Each week: moods wander, prices are written down, dividends and profits paid, bids decided, raiders stir. */
internal fun GameState.stockMarketWeek() {
    // The market has its own dice, so trading never changes what happens elsewhere.
    val rng = SeededRNG.fromSeed(seed * 7_919 + week * 104_729L)
    val companies = industry()
    for (c in companies + portfolio.owned.map { RankedCompany(it.name, 0.0, null, false, it.key) }) {
        val m = portfolio.moods[c.key] ?: 1.0
        portfolio.moods[c.key] = (m + (1 - m) * 0.04 + rng.normal(sd = 0.03)).clamped(0.55, 1.7)
    }
    for (c in industry()) {
        val h = portfolio.history.getOrPut(c.key) { mutableListOf() }
        h += c.value / sharesOutstanding(c.key)
        if (h.size > 52) h.removeAt(0)
    }

    portfolio.bid?.let { if (week >= it.decidesWeek) decideBid(it, rng) }

    // A company that's gone (bought, merged, history) pays out at its last price.
    val here = companies.map { it.key }.toSet()
    for (h in portfolio.holdings.toList()) {
        if (h.key in here) continue
        val proceeds = h.shares * lastPrice(h.key)
        cash += proceeds
        portfolio.realized += proceeds - h.cost
        portfolio.holdings.remove(h)
        addLog("${h.key} is gone from the market. Your shares paid out ${money(proceeds)}.", LogTone.NEUTRAL)
    }

    // Dividends from companies that have been public ten years or more: 2% a year, paid quarterly.
    if (week > 0 && week % GameClock.WEEKS_PER_QUARTER == 0) {
        val paid = portfolio.holdings.sumOf { h ->
            if (calendarYear - (ListedSince[h.key] ?: calendarYear) >= 10) holdingValue(h) * 0.02 / 4 else 0.0
        }
        if (paid >= 1) {
            cash += paid
            portfolio.dividends += paid
            recordRevenue(paid, null)
            addLog("Dividends: ${money(paid)} from your shares.", LogTone.GOOD)
        }
    }

    // Companies you own send profits home: about 5% of their value a year.
    for (o in portfolio.owned) {
        val profit = ownedValue(o) * 0.05 / 52 * rng.range(0.85, 1.15)
        o.lastProfit = profit
        o.lifetimeProfit += profit
        cash += profit
        recordRevenue(profit, null)
    }

    raidWeek(rng)
}

// Raiders

@Serializable
sealed interface MarketEvent : EventKind

/** A raider has bought a slice of you. */
@Serializable @SerialName("raiderStake")
data class RaiderStake(val raider: String, val stake: Double) : MarketEvent

/** A bid for your company. */
@Serializable @SerialName("takeoverOffer")
data class TakeoverOffer(val raider: String, val price: Double, val hostile: Boolean) : MarketEvent

/** How likely shareholders are to sell you out in a hostile bid: the less you own, the likelier. */
val GameState.hostileOdds: Double
    get() = (0.25 + (0.5 - venture.founderEquity) * 1.2).coerceIn(0.05, 0.85)

private fun GameState.raidWeek(rng: SeededRNG) {
    val l = listing ?: return
    val raid = portfolio.raid
    if (raid == null) {
        if (venture.founderEquity >= 0.5 || week < portfolio.pillUntil || week - l.ipoWeek < 26) return
        if (events.any { it.kind is MarketEvent } || !rng.chance(0.006)) return
        val big = industry().filter { it.value > l.marketCap * 3 }
        if (big.isEmpty()) return
        val raider = big.firstOrNull { it.lane in lanes && rng.chance(0.5) } ?: rng.pick(big)
        val stake = rng.range(0.05, 0.12)
        portfolio.raid = Raid(raider.name, stake)
        enqueue(RaiderStake(raider.name, stake))
        return
    }
    if (raid.bidWeek in 0..week) {
        raid.bidWeek = -1
        val premium = if (raid.friendly) 1.4 else 1.3
        enqueue(TakeoverOffer(raid.raider, l.marketCap * premium, hostile = !raid.friendly))
    }
}

internal fun GameState.describeMarket(k: MarketEvent): Description? = when (k) {
    is RaiderStake -> {
        val l = listing
        Description(
            "${k.raider} is buying ${l?.ticker ?: "your shares"}",
            "${k.raider} has quietly built a ${Format.percent(k.stake)} stake in $companyName. You own ${Format.percent(venture.founderEquity)}. " +
                "Raiders who buy this much usually want the rest.",
            listOf(
                choice("Adopt a poison pill", "Keeps raiders out for two years. The market doesn't love it: the share price drops."),
                choice("Buy back 10% of the shares", "Costs ${money((l?.marketCap ?: 0.0) * 0.1)}. You own more, and they own less of what's left."),
                choice("Talk to them", "Hear what they'd pay. A friendly offer comes with a better price."),
                choice("Ignore it", "They might lose interest. They might not."),
            ),
        )
    }
    is TakeoverOffer -> Description(
        if (k.hostile) "${k.raider} goes hostile" else "${k.raider} makes an offer",
        "${k.raider} offers ${money(k.price)} for $companyName, " +
            "${Format.percent(k.price / max(1.0, listing?.marketCap ?: k.price) - 1)} over the share price. " +
            "Your ${Format.percent(venture.founderEquity)} would be worth ${money(k.price * venture.founderEquity)}." +
            if (k.hostile) " If you fight, it goes to the shareholders, and with what you own, there's about a " +
                "${Format.percent(hostileOdds)} chance they sell anyway." else "",
        listOf(
            choice("Sell the company", "Take the money. The run ends here."),
            choice(if (k.hostile) "Fight it" else "Turn it down", if (k.hostile) "Rally the shareholders and hope." else "Thank them, and keep going."),
        ),
    )
}

internal fun GameState.resolveMarket(k: MarketEvent, choice: Int) {
    when (k) {
        is RaiderStake -> {
            val raid = portfolio.raid ?: return
            val l = listing ?: return
            when (choice) {
                0 -> {
                    portfolio.pillUntil = week + 104
                    portfolio.raid = null
                    l.sentiment *= 0.93
                    addLog("You adopted a poison pill. ${k.raider} sold out at a loss, and so did your shareholders.", LogTone.NEUTRAL)
                }
                1 -> {
                    val cost = l.marketCap * 0.1
                    if (cash < cost) {
                        addLog("You couldn't afford the buyback. ${k.raider} keeps buying.", LogTone.BAD)
                        raid.bidWeek = week + 8
                    } else {
                        spend(cost, null)
                        l.shares /= 1.1
                        venture.founderEquity = min(1.0, venture.founderEquity * 1.1)
                        l.sentiment *= 1.03
                        addLog("You bought back ${money(cost)} of stock and now own ${Format.percent(venture.founderEquity)}.", LogTone.GOOD)
                        if (venture.founderEquity >= 0.5) {
                            portfolio.raid = null
                            addLog("With half the company in your hands, ${k.raider} gives up.", LogTone.GOOD)
                        } else raid.bidWeek = week + 10
                    }
                }
                2 -> {
                    raid.friendly = true
                    raid.bidWeek = week + 6
                    addLog("You agreed to hear ${k.raider} out.", LogTone.NEUTRAL)
                }
                else -> if (rng.chance(0.5)) {
                    portfolio.raid = null
                    addLog("${k.raider} lost interest and sold its stake.", LogTone.GOOD)
                } else raid.bidWeek = week + 10
            }
        }
        is TakeoverOffer -> {
            if (choice == 0) {
                portfolio.raid = null
                end(EndingKind.ACQUIRED, k.price, k.raider)
                return
            }
            portfolio.raid = null
            if (k.hostile && rng.chance(hostileOdds)) {
                addLog("Your shareholders took ${k.raider}'s money.", LogTone.BAD)
                end(EndingKind.ACQUIRED, k.price, k.raider)
            } else {
                portfolio.pillUntil = week + 52
                listing?.let { it.sentiment *= 0.96 }
                addLog(if (k.hostile) "Your shareholders stood by you. ${k.raider} walks away." else "You turned ${k.raider} down.", LogTone.GOOD)
            }
        }
    }
}
