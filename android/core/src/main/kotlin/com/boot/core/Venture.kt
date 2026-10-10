package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

@Serializable
data class FundingRound(val name: String, val amount: Double, val preMoney: Double, val week: Int, val lead: String = "") {
    val postMoney: Double get() = preMoney + amount
}

/** Investors who can overrule you, push growth targets, and fire you from your own company. */
@Serializable
class Board(var lastReviewWeek: Int = 0) {
    /** 0...1. At zero they replace you. */
    var confidence = 0.7

    /** Quarter-over-quarter revenue growth they expect. */
    var growthTarget = 0.12

    /** Quarters they'll tolerate with no revenue while you build. */
    var graceQuarters = 4
    var lastGrowth: Double? = null
    var demandsMet = 0
    var demandsDefied = 0
}

@Serializable
class Venture {
    val rounds = mutableListOf<FundingRound>()
    var founderEquity = 1.0
    var board: Board? = null

    val raised: Double get() = rounds.sumOf { it.amount }
    val isBacked: Boolean get() = rounds.isNotEmpty()

    companion object {
        val roundNames = listOf("Seed", "Series A", "Series B", "Series C", "Series D", "Series E")
    }

    internal fun take(offer: RoundOffer, week: Int) {
        founderEquity *= 1 - offer.dilution
        rounds += FundingRound(offer.name, offer.amount, offer.preMoney, week, offer.lead)
        val b = board
        if (b == null) {
            board = Board(lastReviewWeek = week)
        } else {
            b.confidence = min(1.0, b.confidence + 0.15)
            b.growthTarget = min(0.25, b.growthTarget + 0.02)
            b.graceQuarters = max(b.graceQuarters, 2)
        }
    }
}

data class RoundOffer(val name: String, val amount: Double, val preMoney: Double, val lead: String = "") {
    val dilution: Double get() = amount / (preMoney + amount)
}

/** Who leads each stage of round. */
private val leadInvestors = listOf(
    listOf("Y Combinator", "First Round Capital", "SV Angel", "Initialized Capital"),
    listOf("Sequoia Capital", "Andreessen Horowitz", "Benchmark", "Accel"),
    listOf("Lightspeed Venture Partners", "Kleiner Perkins", "Index Ventures", "General Catalyst"),
    listOf("Tiger Global", "Coatue", "Thrive Capital", "Insight Partners"),
    listOf("SoftBank Vision Fund", "Fidelity", "T. Rowe Price"),
    listOf("SoftBank Vision Fund", "BlackRock", "Mubadala"),
)

/** The lead for the next round. Picked from the seed so the offer doesn't change every time you look at it. */
internal fun GameState.nextLead(): String {
    val pool = leadInvestors[minOf(venture.rounds.size, leadInvestors.lastIndex)]
    return pool[Math.floorMod(seed + venture.rounds.size * 7919L, pool.size.toLong()).toInt()]
}

/** Things a nervous board insists on. */
@Serializable
enum class BoardDemand {
    LAYOFFS, MONETIZE, CRUNCH, GROWTH_HIRES;

    val title: String
        get() = when (this) {
            LAYOFFS -> "The board wants a 20% layoff"
            MONETIZE -> "The board wants you to squeeze harder"
            CRUNCH -> "The board wants it shipped, now"
            GROWTH_HIRES -> "The board wants a sales blitz"
        }

    val body: String
        get() = when (this) {
            LAYOFFS -> "Growth missed target again and the runway is short. They want headcount cut by a fifth, starting with the weakest performers."
            MONETIZE -> "They see users, not revenue. They want ad load and monetization turned up across the board."
            CRUNCH -> "Missed targets mean the next launch has to land sooner. They want the whole company in crunch mode."
            GROWTH_HIRES -> "They want four marketers hired this week to juice the top line, whatever it does to burn."
        }
}

const val IPO_VALUATION = 500_000_000.0
const val IPO_ANNUAL_REVENUE = 50_000_000.0
const val IPO_EARLIEST_WEEK = 208

/** What investors would offer right now, or why they won't. */
fun GameState.roundOffer(): Result<RoundOffer> {
    if (venture.rounds.size >= Venture.roundNames.size) {
        return Result.failure(notAvailable("You've raised every round there is. Time to think about going public."))
    }
    venture.rounds.lastOrNull()?.let { last ->
        if (week - last.week < 26) {
            return Result.failure(notAvailable("Investors want to see six months of progress since your ${last.name}."))
        }
    }
    venture.board?.let { b ->
        if (b.confidence < 0.35) {
            return Result.failure(notAvailable("Your own board won't back a new round while confidence is this low."))
        }
    }
    val name = Venture.roundNames[venture.rounds.size]
    val appetite = world.era.investorAppetite * (if (lanes.any { it in world.era.hotLanes }) 1.3 else 1.0)
    var pre = enterpriseValue * appetite + max(0.0, cash)
    if (venture.rounds.isEmpty()) {
        lanes.firstOrNull { it.isCapitalIntensive }?.let { heavy ->
            pre = max(pre, if (heavy == Lane.AI) 15_000_000.0 else 9_000_000.0)
        }
    }
    // Angels pay for growth curves, not revenue.
    social?.let { s ->
        if (s.isLive) pre = max(pre, s.realUsers * 60 * (1 + 20 * max(0.0, s.lastGrowth)) * appetite)
    }
    if (pre < 250_000) {
        return Result.failure(notAvailable("Investors pass. Come back when the company is worth more than ${money(250_000.0)}."))
    }
    return Result.success(RoundOffer(name, (pre / 3).roundToLong().toDouble(), pre.roundToLong().toDouble(), nextLead()))
}

/** The board's quarterly verdict. */
internal fun GameState.boardReview() {
    val b = venture.board ?: return
    if (week - b.lastReviewWeek < GameClock.WEEKS_PER_QUARTER) return
    b.lastReviewWeek = week
    val q = trailingRevenue(weeks = 13)
    val prev = trailingRevenue(weeks = 13, offset = 13)
    val note: String

    if (q < 5_000 && b.graceQuarters > 0) {
        b.graceQuarters -= 1
        b.confidence -= if (stats.releasesThisQuarter > 0) -0.03 else 0.04
        note = "The board is patient for now. ${b.graceQuarters} quarter${if (b.graceQuarters == 1) "" else "s"} of grace left."
    } else {
        val growth = if (prev > 1_000) (q - prev) / prev else if (q > prev) 1.0 else -0.5
        b.lastGrowth = growth
        if (growth >= b.growthTarget) {
            b.confidence += 0.08
            note = "Quarterly review: revenue up ${Format.percent(growth)}. The board is pleased."
        } else {
            b.confidence -= min(0.25, 0.08 + (b.growthTarget - growth) * 0.3)
            note = "Quarterly review: revenue ${if (growth >= 0) "up" else "down"} ${Format.percent(abs(growth))} " +
                "against a ${Format.percent(b.growthTarget)} target. The board is unhappy."
        }
        if (stats.releasesThisQuarter > 0) b.confidence += 0.04
    }
    if (runwayWeeks < 13) b.confidence -= 0.05
    b.confidence = b.confidence.clamped(0.0, 1.0)
    stats.releasesThisQuarter = 0
    addLog(note, if (b.confidence < 0.4) LogTone.BAD else LogTone.NEUTRAL)

    if (b.confidence <= 0.05) {
        end(EndingKind.OUSTED)
    } else if (b.confidence < 0.4) {
        enqueue(EventKind.BoardDemandEvent(pickBoardDemand()))
    }
}

private fun GameState.pickBoardDemand(): BoardDemand {
    if (runwayWeeks < 20 && staff.size >= 5) return BoardDemand.LAYOFFS
    if (social != null || apps != null || crossover(Combo.APPS_GAMES)?.built == true) return BoardDemand.MONETIZE
    if (games?.project != null || hardware?.design != null) return BoardDemand.CRUNCH
    return BoardDemand.GROWTH_HIRES
}

internal fun GameState.applyBoardDemand(d: BoardDemand) {
    when (d) {
        BoardDemand.LAYOFFS -> {
            val cut = max(1, staff.size / 5)
            val leaving = staff.sortedBy { it.skill }.take(cut).map { it.id }.toSet()
            staff = staff.filterNot { it.id in leaving }
            for (e in staff) e.morale -= 0.2
            reputation.scar(ScarKind.LAYOFFS, 0.25, week, "Cut $cut jobs at the board's request.")
            stats.layoffs += cut
            addLog("You laid off $cut people. The office is quiet.", LogTone.BAD)
        }
        BoardDemand.MONETIZE -> {
            social?.let { it.adLoad = max(it.adLoad, 0.8) }
            apps?.let { if (it.monetization == AppMonetization.PAID) it.monetization = AppMonetization.ADS }
            crossover(Combo.APPS_GAMES)?.let { it.intensity = max(it.intensity, 0.9) }
            addLog("Monetization turned up on the board's orders.")
        }
        BoardDemand.CRUNCH -> {
            crunch = true
            addLog("Crunch mode, by order of the board.", LogTone.BAD)
        }
        BoardDemand.GROWTH_HIRES -> {
            repeat(4) { hireDirect(makeCandidate(Role.MARKETER)) }
            addLog("Four new marketers start Monday.")
        }
    }
}
