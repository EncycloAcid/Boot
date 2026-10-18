package com.boot.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Boxed and collector's editions.
 *
 * Once researched, a game you publish yourself can have its boxes made by you instead of a
 * distributor. You decide how many standard boxes to manufacture and how many collector's
 * editions, and pay for them at launch:
 * - Boxes you sell earn more than a distributor would pay you.
 * - Order too many and the rest are cleared at a loss.
 * - Order too few and some buyers go without; once games could be downloaded, some of them buy it
 *   that way instead.
 * Collector's editions sell to your fans at a premium, as many as you made and they want.
 *
 * Leave it to a distributor (the default) and nothing changes: the shops get what they need.
 */

/** Share of a game's buyers who'd buy a box in [year]: nearly all at first, a few today. */
fun physicalShare(year: Int): Double = when {
    year < 1998 -> 0.95
    year < 2008 -> 0.8
    year < 2013 -> 0.55
    year < 2018 -> 0.35
    else -> 0.2
}

/** Share of buyers who can't find a box but download it instead. */
fun downloadInstead(year: Int): Double = when {
    year < 2004 -> 0.1
    year < 2010 -> 0.4
    else -> 0.7
}

/** What a standard box costs to make, and a collector's edition. */
fun GameState.boxCost(): Double = 4.0 * pastScale(calendarYear.toDouble())

fun GameState.collectorCost(): Double = boxCost() * 7

/** A collector's edition sells for this many times a standard copy. */
const val COLLECTOR_PRICE = 2.5

/** Extra a box you made yourself earns over what a distributor pays you. */
const val OWN_BOX_MARGIN = 0.25

/** About how many boxes [team]'s game will sell over its life: the distributor's guess. */
fun GameState.boxEstimate(team: Int): Int {
    val p = games?.projectOf(team) ?: return 0
    val c = p.concept
    val score = gameForecast(team)?.expectedScore ?: 6.0
    val fans = games?.fans ?: 0.0
    val launchWeek = c.size.launchSales * (score / 7).pow(3) * (1 + p.hype / 80) * (1 + 1.5 * fans / (fans + c.size.fanScale)) / c.unitPrice
    return (launchWeek * 5 * physicalShare(calendarYear)).roundToInt()
}

/** About how many collector's editions your fans would buy. */
fun GameState.collectorEstimate(team: Int): Int {
    val p = games?.projectOf(team) ?: return 0
    val fans = games?.fans ?: 0.0
    val score = gameForecast(team)?.expectedScore ?: 6.0
    return (fans * 0.03 * (0.5 + p.hype / 100) * (score / 7).pow(2)).roundToInt()
}

/** Why [team]'s game can't have its own boxes, or null if it can. */
fun GameState.editionsBlocker(team: Int): String? {
    val p = games?.projectOf(team) ?: return "No game in development."
    if (Research.BOXED_EDITIONS !in researched) return "Research boxed editions first."
    if (p.deal != null) return "${p.deal!!.publisher} makes the boxes."
    if (p.concept.live) return "Live games are downloaded."
    if (p.concept.platforms.all { it == Platform.WEB || it == Platform.GRPHONE }) return "Nobody sells boxes for browser and phone games."
    return null
}

/** Order [boxes] standard and [collectors] collector's editions for [team]'s game; 0 and 0 leaves it to a distributor. */
fun GameState.orderEditions(team: Int, boxes: Int, collectors: Int) {
    requireLive()
    editionsBlocker(team)?.let { throw notAvailable(it) }
    require(boxes >= 0 && collectors >= 0)
    val p = games!!.projectOf(team)!!
    p.boxes = boxes
    p.collectors = collectors
    val cost = boxes * boxCost() + collectors * collectorCost()
    if (boxes + collectors > 0) {
        addLog("${p.title}: ${Format.count(boxes.toDouble())} boxes and ${Format.count(collectors.toDouble())} collector's editions on order, ${money(cost)} at launch.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** At launch: pay for the boxes, sell what people want, and clear what's left. */
internal fun GameState.afterEditions(p: GameProject, r: GameRelease) {
    if (p.boxes == 0 && p.collectors == 0) return
    val g = games ?: return
    val year = calendarYear
    spend(p.boxes * boxCost() + p.collectors * collectorCost(), Lane.GAMES)
    // About five launch weeks' worth over its life, and a share of it in boxes.
    val lifetime = r.weeklySales / r.concept.unitPrice * 5
    val demand = lifetime * physicalShare(year)
    r.boxes = p.boxes
    r.collectors = p.collectors
    var lump = 0.0
    if (p.boxes > 0) {
        val sold = min(p.boxes.toDouble(), demand)
        val left = p.boxes - sold
        val missed = max(0.0, demand - p.boxes)
        r.boxesSold = sold.roundToInt()
        // Your own boxes earn more than a distributor pays; unsold ones go for a song.
        lump += sold * r.concept.unitPrice * OWN_BOX_MARGIN * r.netShare + left * r.concept.unitPrice * 0.15
        // Buyers who couldn't find a box, less those who downloaded it instead.
        val lost = missed * (1 - downloadInstead(year))
        if (lifetime > 0) r.weeklySales *= (1 - lost / lifetime).coerceIn(0.3, 1.0)
        when {
            left >= p.boxes * 0.3 -> addLog("${Format.count(left)} boxes of ${r.title} didn't sell. They went to the bargain bin.", LogTone.BAD, Lane.GAMES)
            lost >= demand * 0.15 -> {
                addLog("${r.title} sold out: about ${Format.count(lost)} people couldn't find a copy.", LogTone.NEUTRAL, Lane.GAMES)
                publish(NewsDesk.GAMES, "IGN", "${r.title} sold out in shops", "$companyName didn't make enough. Fans are hunting for copies.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS)
            }
            else -> addLog("${r.title}'s boxes sold well: ${Format.count(sold)} of ${Format.count(p.boxes.toDouble())}.", LogTone.GOOD, Lane.GAMES)
        }
    }
    if (p.collectors > 0) {
        val want = g.fans * 0.03 * (0.5 + r.hype / 100) * (r.score / 7).pow(2)
        val sold = min(p.collectors.toDouble(), want)
        val left = p.collectors - sold
        r.collectorsSold = sold.roundToInt()
        lump += sold * r.concept.unitPrice * COLLECTOR_PRICE * 0.8 + left * r.concept.unitPrice * COLLECTOR_PRICE * 0.25
        g.fans += sold * 0.5
        addLog(
            if (left > p.collectors * 0.3) "Only ${Format.count(sold)} of ${Format.count(p.collectors.toDouble())} collector's editions of ${r.title} sold."
            else "${Format.count(sold)} collector's editions of ${r.title} sold${if (sold < want * 0.8) ", and fans wanted more" else ""}.",
            if (left > p.collectors * 0.3) LogTone.BAD else LogTone.GOOD, Lane.GAMES,
        )
    }
    if (lump > 0) {
        cash += lump
        recordRevenue(lump, Lane.GAMES)
        r.lifetimeSales += lump
    }
}
