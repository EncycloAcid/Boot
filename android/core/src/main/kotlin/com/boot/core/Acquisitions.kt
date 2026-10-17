package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.pow

/*
 * Buying other studios. Every studio on the games market, from one-person indies to the giants, can
 * be bought for what it's worth plus a premium. Once it's yours its games stop competing with yours,
 * show up as yours on the chart, and a share of its profits comes home every week: more when it has
 * hits out. What you paid counts towards your company's value, and you can sell it on later.
 */

/** A games studio on the market: how good its games usually are, what it's worth, and whether it's an indie. */
data class Studio(val name: String, val quality: Double, val baseValue: Double, val indie: Boolean = false)

val Studios = listOf(
    Studio("Electronic Arts", 0.6, 40e9),
    Studio("Ubisoft", 0.55, 2e9),
    Studio("Activision Blizzard", 0.65, 69e9),
    Studio("Rockstar Games", 0.9, 25e9),
    Studio("Nintendo", 0.85, 80e9),
    Studio("Capcom", 0.75, 12e9),
    Studio("FromSoftware", 0.85, 3e9),
    Studio("CD Projekt Red", 0.75, 6e9),
    Studio("Square Enix", 0.65, 5e9),
    Studio("Epic Games", 0.6, 30e9),
    Studio("Bandai Namco", 0.6, 15e9),
    Studio("Supercell", 0.55, 10e9),
    Studio("PlayStation Studios", 0.85, 50e9),
    Studio("Xbox Game Studios", 0.7, 60e9),
    Studio("Valve", 0.85, 20e9),
    // Indies: smaller games, now and then.
    Studio("ConcernedApe", 0.85, 50e6, indie = true),
    Studio("Team Cherry", 0.85, 80e6, indie = true),
    Studio("Mega Crit", 0.8, 80e6, indie = true),
    Studio("Playdead", 0.8, 60e6, indie = true),
    Studio("Innersloth", 0.6, 100e6, indie = true),
    Studio("Supergiant Games", 0.85, 120e6, indie = true),
    Studio("Thatgamecompany", 0.7, 120e6, indie = true),
    Studio("Hello Games", 0.6, 150e6, indie = true),
    Studio("Re-Logic", 0.75, 150e6, indie = true),
    Studio("Klei Entertainment", 0.75, 200e6, indie = true),
    Studio("Annapurna Interactive", 0.7, 200e6, indie = true),
    Studio("Devolver Digital", 0.6, 300e6, indie = true),
    Studio("Remedy Entertainment", 0.75, 500e6, indie = true),
    Studio("Larian Studios", 0.9, 1.5e9, indie = true),
    // Around for a while, or only in the past: they show up in the years they were making games.
    Studio("Sega", 0.65, 4e9),
    Studio("Konami", 0.65, 8e9),
    Studio("Atari", 0.5, 2e9),
    Studio("id Software", 0.85, 1e9, indie = true),
    Studio("Sierra On-Line", 0.65, 3e9),
    Studio("Infocom", 0.75, 1e9, indie = true),
    Studio("Origin Systems", 0.75, 2e9),
    Studio("Westwood Studios", 0.8, 2e9),
    Studio("LucasArts", 0.8, 3e9),
)

fun studio(name: String): Studio? = Studios.firstOrNull { it.name == name }

/** A studio you own. */
@Serializable
data class Subsidiary(
    val name: String,
    val boughtWeek: Int,
    val paid: Double,
    var lastProfit: Double = 0.0,
    var lifetimeProfit: Double = 0.0,
    /** What was really going on there, found by due diligence or not. */
    val finding: Finding? = null,
    var plan: Plan = Plan.INDEPENDENT,
    /** When the plan last changed: once a year. */
    var planWeek: Int = -1_000,
    /** Their best people already joined your team. */
    var folded: Boolean = false,
    /** How the people there feel, 0...1. */
    var morale: Double = 0.7,
    /** The game they're making with your money, if any. */
    var project: StudioGame? = null,
    /** Games of theirs you paid for that came out, and how many were hits. */
    var shipped: Int = 0,
    var hits: Int = 0,
    /** When they last spoke up, and how many times senior people walked. */
    var unrestWeek: Int = -1_000,
    var talentLost: Int = 0,
)

/** What buyers pay over a studio's value. */
const val ACQUISITION_PREMIUM = 0.15

/** What a studio is worth today: values drift with the industry the way the league table's do. */
fun GameState.studioValue(s: Studio): Double {
    return s.baseValue * world.demand(Lane.GAMES).pow(1.5) * industryGrowth
}

/** What it costs to buy a studio today, premium included. */
fun GameState.acquisitionPrice(s: Studio): Double = studioValue(s) * (1 + ACQUISITION_PREMIUM)

/** What selling one of yours brings in. */
fun GameState.divestPrice(s: Studio): Double =
    studioValue(s) * 0.9 * (games?.subsidiaries?.firstOrNull { it.name == s.name }?.valueFactor(week) ?: 1.0)

fun GamesStudio.owns(name: String): Boolean = subsidiaries.any { it.name == name || it.name == canonicalStudio(name) }

/** The studios you could buy, cheapest first. */
fun GameState.studiosForSale(): List<Studio> {
    val g = games ?: return emptyList()
    return Studios.filter { !g.owns(it.name) && studioActive(it.name, calendarYear) }.sortedBy { it.baseValue }
}

/** Why you can't buy [s] right now, or null if you can. */
fun GameState.acquisitionBlocker(s: Studio): String? {
    val g = games ?: return "You need a game studio to buy one."
    if (g.owns(s.name)) return "It's already yours."
    if (!s.indie && listing == null && week < 52 * 3) return "The big publishers won't talk to a company this young."
    val price = acquisitionPrice(s)
    if (cash < price) return "Costs ${money(price)}. You have ${money(cash)}."
    return null
}

fun GameState.acquire(name: String) {
    requireLive()
    val s = studio(name) ?: throw invalid("No such studio.")
    acquisitionBlocker(s)?.let { throw notAvailable(it) }
    val price = acquisitionPrice(s)
    spend(price, Lane.GAMES)
    games!!.subsidiaries += Subsidiary(s.name, week, price, finding = findingAt(s))
    stats.acquisitions += 1
    reputation.adjust(if (s.indie) 1.0 else 3.0)
    addLog("You bought ${s.name} for ${money(price)}. Their games are yours now.", LogTone.MILESTONE, Lane.GAMES)
    publish(NewsDesk.YOU, rng.pick(listOf("The Verge", "Bloomberg", "Kotaku")), "$companyName buys ${s.name} for ${money(price)}", studioFranchises(s.name).take(2).joinToString().let { if (it.isEmpty()) "" else "The maker of $it." }, LogTone.GOOD, Lane.GAMES, lead = !s.indie)
}

fun GameState.divest(name: String) {
    requireLive()
    val g = games ?: throw notAvailable("You don't run a game studio.")
    val sub = g.subsidiaries.firstOrNull { it.name == name } ?: throw invalid("You don't own $name.")
    val s = studio(name) ?: throw invalid("No such studio.")
    val price = divestPrice(s)
    g.subsidiaries.remove(sub)
    cash += price
    addLog("You sold ${s.name} for ${money(price)}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** What your studios are worth, for the company's valuation. */
val GameState.subsidiaryValue: Double
    get() = games?.subsidiaries?.sumOf { sub -> studio(sub.name)?.let { studioValue(it) * sub.valueFactor(week) } ?: 0.0 } ?: 0.0

/** About 6% of a studio's value a year comes home as profit, more when it has hits on sale. */
internal fun GameState.subsidiaryWeek() {
    val g = games ?: return
    for (sub in g.subsidiaries) {
        val s = studio(sub.name) ?: continue
        val theirs = g.market.games.filter { canonicalStudio(it.studio) == s.name && week - it.week <= 52 }
        val hits = theirs.count { it.score >= 8 }
        val flops = theirs.count { it.score < 5 }
        val form = (0.8 + 0.15 * hits - 0.1 * flops).clamped(0.6, 1.5)
        val profit = studioValue(s) * 0.06 / 52 * form * sub.profitFactor(week) * rng.range(0.9, 1.1) * (0.8 + 0.4 * sub.morale) + fundedSales(sub)
        sub.lastProfit = profit
        sub.lifetimeProfit += profit
        cash += profit
        recordRevenue(profit, Lane.GAMES)
    }
}

/** Their weekly profit, as last paid. */
val GamesStudio.subsidiaryProfit: Double get() = subsidiaries.sumOf { it.lastProfit }

/** The real series a studio makes, for the shop window. */
fun studioFranchises(name: String): List<String> = RealFranchises.filter { it.studio == name }.map { it.name }
