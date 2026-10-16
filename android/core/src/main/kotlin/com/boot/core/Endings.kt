package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max

/** There's no single win condition. The ending reflects how you got there. */
@Serializable
enum class EndingKind {
    IPO, ACQUIRED, INDIE, BANKRUPT, OUSTED;

    val title: String
        get() = when (this) {
            IPO -> "IPO"
            ACQUIRED -> "Acquired"
            INDIE -> "Stayed Independent"
            BANKRUPT -> "Bankrupt"
            OUSTED -> "Ousted"
        }
}

@Serializable
data class EndingReport(
    val kind: EndingKind,
    val title: String,
    val summary: String,
    val story: List<String>,
    val week: Int,
    val valuation: Double,
    val founderTakeHome: Double,
    val ecosystem: Boolean,
)

@Serializable
class Stats {
    var peakValuation = 0.0
    var peakHeadcount = 0
    var peakCash = 0.0
    var totalRevenue = 0.0
    var hits = 0
    var flops = 0
    var releases = 0
    var releasesThisQuarter = 0
    var recalls = 0
    var scandals = 0
    var hearings = 0
    var soldOuts = 0
    var overstocks = 0
    var layoffs = 0
    var buyoutsDeclined = 0
    var saleProceeds = 0.0
    var weeksInRed = 0

    /** Consecutive weeks every lane has been profitable over the trailing quarter. */
    var profitableStreak = 0

    /** Weeks spent as a profitable hardware + software + services company. */
    var ecosystemWeeks = 0
    var ecosystemPressure = 0.0
    var ecosystemOpened = false
    var crunchWeeks = 0
    var chartToppers = 0
    var awards = 0
    var loans = 0
    var ipoRaise = 0.0
    var acquisitions = 0
    var seasons = 0
    var finals = 0
    var rivalMoves = 0
    var rivalsBeaten = 0

    /** Booths taken at the year's big games show, and Best in Show awards won there. */
    var tradeShows = 0
    var bestInShow = 0

    /** Companies taken over on the stock market. */
    var takeovers = 0
    var counterMoves = 0
    var inboxAnswered = 0
    var eraChallengesWon = 0
    val erasSeen = mutableListOf(Era.BOOTSTRAP_YEARS)
}

/** IPO-ready, or the reason you aren't. */
val GameState.ipoBlocker: String?
    get() {
        if (listing != null) return "You're already public."
        if (week < IPO_EARLIEST_WEEK) return "Bankers won't take you public before year ${IPO_EARLIEST_WEEK / 52 + 1}."
        if (enterpriseValue < IPO_VALUATION) {
            return "Bankers want the business valued at ${money(IPO_VALUATION)}, not counting cash. You're at ${money(enterpriseValue)}."
        }
        val annual = trailingRevenue(weeks = 52)
        if (annual < IPO_ANNUAL_REVENUE) {
            return "You need ${money(IPO_ANNUAL_REVENUE)} in annual revenue. You're at ${money(annual)}."
        }
        venture.board?.let { if (it.confidence < 0.4) return "The board won't sign off on an IPO right now." }
        return null
    }

internal fun GameState.end(kind: EndingKind, price: Double? = null, buyer: String? = null) {
    if (ending != null) return
    val value = price ?: listing?.marketCap ?: valuation
    publish(
        NewsDesk.YOU, businessOutlet(),
        when (kind) {
            EndingKind.IPO -> "$companyName goes public at ${money(value)}"
            EndingKind.ACQUIRED -> "${buyer ?: "A buyer"} buys $companyName for ${money(value)}"
            EndingKind.INDIE -> "$companyName stays independent"
            EndingKind.BANKRUPT -> "$companyName files for bankruptcy"
            EndingKind.OUSTED -> "$companyName's board ousts its founder"
        },
        tone = if (kind == EndingKind.BANKRUPT || kind == EndingKind.OUSTED) LogTone.BAD else LogTone.GOOD, lead = true,
    )
    stats.peakValuation = max(stats.peakValuation, valuation)
    val equity = venture.founderEquity
    val takeHome = when (kind) {
        EndingKind.IPO, EndingKind.ACQUIRED -> value * equity
        EndingKind.INDIE -> valuation * equity
        EndingKind.BANKRUPT -> 0.0
        EndingKind.OUSTED -> valuation * equity * 0.6
    }
    val report = EndingReport(
        kind, endingTitle(kind, buyer), endingSummary(kind, value, buyer), endingStory(), week, value, takeHome,
        ecosystem = stats.ecosystemWeeks >= 52,
    )
    ending = report
    checkAchievements()
    addLog("THE END: ${report.title}", LogTone.MILESTONE)
}

private val GameState.isPredatory: Boolean get() = reputation.has(ScarKind.PREDATORY_MONETIZATION, 0.15)

internal fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

private fun GameState.endingTitle(kind: EndingKind, buyer: String?): String {
    val titan = stats.ecosystemWeeks >= 52
    return when (kind) {
        EndingKind.IPO -> when {
            titan -> "The Ecosystem IPO"
            isPredatory -> "IPO, Whale-Funded"
            Lane.AI in lanes -> "The Compute IPO"
            venture.rounds.isEmpty() -> "Bootstrapped to the Bell"
            else -> "Ring the Bell"
        }
        EndingKind.ACQUIRED -> when {
            pivot == null -> "The Quick Exit"
            buyer != null && world.rivals.any { it.name == buyer } -> "Absorbed by $buyer"
            else -> "Acquired"
        }
        EndingKind.INDIE -> when {
            venture.isBacked -> if (valuation < venture.raised) "Zombie Unicorn" else "Private and Thriving"
            staff.size <= 3 -> "The Lifestyle Business"
            titan -> "The Independent Empire"
            else -> "Still Indie"
        }
        EndingKind.BANKRUPT -> if (Lane.AI in lanes) "Burned the Compute Budget" else "Out of Runway"
        EndingKind.OUSTED -> "Fired From Your Own Company"
    }
}

private fun GameState.endingSummary(kind: EndingKind, value: Double, buyer: String?): String {
    val stake = Format.percent(venture.founderEquity)
    return when (kind) {
        EndingKind.IPO -> listing?.let { l ->
            "You stepped down after ${plural((week - l.ipoWeek) / 52, "year")} as a public company. ${l.ticker} is worth ${money(value)}, " +
                "and your $stake stake ${money(value * venture.founderEquity)}."
        } ?: "$companyName went public at a ${money(value)} valuation. Your $stake stake is worth ${money(value * venture.founderEquity)}."
        EndingKind.ACQUIRED ->
            "${buyer ?: "A buyer"} acquired $companyName for ${money(value)}. You walk away with ${money(value * venture.founderEquity)}."
        EndingKind.INDIE -> if (freeplay) {
            "You stepped away after ${plural(week / 52, "year")}. $companyName is worth ${money(valuation)}."
        } else {
            (startYear?.let { "From $it to today, " } ?: "Fifteen years in, ") + "$companyName is still yours${if (venture.isBacked) ", more or less" else ""}. " +
                "Worth ${money(valuation)} and answering to ${if (venture.board == null) "nobody" else "a board"}."
        }
        EndingKind.BANKRUPT ->
            "$companyName couldn't make payroll for two months straight. The lights went out in ${GameClock.label(week)}."
        EndingKind.OUSTED -> "The board lost confidence and replaced you as CEO of the company you started."
    }
}

private fun GameState.endingStory(): List<String> {
    val story = mutableListOf<String>()
    var origin = "You started $companyName in a cramped apartment as a one-person ${startingLane.businessName.lowercase()}"
    if (startingLane.isCapitalIntensive) origin += ", with a seed cheque and a board from day one"
    story += "$origin."

    val p = pivot
    if (p != null) {
        val why = when (p.trigger) {
            PivotTrigger.BUYOUT_OFFER -> "a buyout offer"
            PivotTrigger.MARKET_CRASH -> "a market crash"
            PivotTrigger.VIRAL_ACCIDENT -> "an accidental viral hit"
        }
        val how = when (p.mode) {
            PivotMode.PIVOT -> {
                val forPrice = p.salePrice?.let { " for ${money(it)}" } ?: ""
                "sold the ${p.from.businessName.lowercase()}$forPrice and went all in on ${p.to.title}"
            }
            PivotMode.EXPAND -> "kept it and expanded into ${p.to.title}"
            PivotMode.STAY -> "turned it all down and stayed focused on the ${p.from.businessName.lowercase()}"
        }
        story += "${GameClock.inYear(p.week)}, $why forced the question. You $how."
    } else {
        story += "You never reached the Pivot Point."
    }
    for (e in expansions) story += "${GameClock.inYear(e.week)} you added ${e.lane.title}."
    if (stats.ecosystemWeeks >= 52) {
        val years = stats.ecosystemWeeks / 52
        story += "For ${plural(years, "year")} you ran a true ecosystem: hardware, software and services under one roof."
    } else if (isEcosystem) {
        story += "You built an ecosystem company, but never held it long enough to make it count."
    }

    games?.let { g ->
        g.bestGame?.let { best ->
            val score = String.format(java.util.Locale.US, "%.1f", best.score)
            story += "You shipped ${plural(g.releases.size, "game")}. The best was ${best.title}, ${Format.a(best.concept.topic.title.lowercase())} " +
                "${best.concept.genreLower} game that scored $score/10."
        }
    }
    games?.let { g ->
        if (g.market.wins > 0) story += "You won Game of the Year ${if (g.market.wins == 1) "once" else "${g.market.wins} times"}."
        if (stats.chartToppers > 0) story += "${plural(stats.chartToppers, "game")} topped the sales chart."
    }
    games?.published?.takeIf { it.isNotEmpty() }?.let { pub ->
        val best = pub.maxBy { it.score }
        story += "Your label published ${plural(pub.size, "game")} by other developers. The best was ${best.title} by ${best.developer} " +
            "(${String.format(java.util.Locale.US, "%.1f", best.score)})."
    }
    games?.subsidiaries?.takeIf { it.isNotEmpty() }?.let { subs ->
        story += "You bought ${plural(subs.size, "studio")}: ${subs.joinToString(", ") { it.name }}."
    }
    if (researched.isNotEmpty()) story += "Your labs researched ${plural(researched.size, "breakthrough")}."
    if (goalsDone.isNotEmpty()) story += "You reached ${plural(goalsDone.size, "goal")} along the way."

    val built = crossovers.filter { it.built }.map { it.combo.product }
    if (built.isNotEmpty()) story += "Crossovers: ${built.joinToString(", ")}."

    val record = mutableListOf<String>()
    if (stats.releases > 0) record += "${stats.releases} launches"
    if (stats.hits > 0) record += plural(stats.hits, "hit")
    if (stats.flops > 0) record += plural(stats.flops, "flop")
    if (stats.recalls > 0) record += plural(stats.recalls, "recall")
    if (stats.scandals > 0) record += plural(stats.scandals, "scandal")
    if (stats.hearings > 0) record += "${plural(stats.hearings, "hearing")} and lawsuits"
    if (record.isNotEmpty()) story += "The record: ${record.joinToString(", ")}."

    val scars = reputation.scars.filter { it.severity >= 0.15 }.sortedByDescending { it.severity }
    story += if (scars.isEmpty()) {
        "Your name stayed clean. Reputation finished at ${reputation.score.toInt()}."
    } else {
        "People still remember: ${scars.joinToString(", ") { it.kind.title.lowercase() }}."
    }
    if (isPredatory) story += "Somewhere, a whale is still paying for a loot box you designed."

    story += "Peak valuation ${money(stats.peakValuation)}. Peak headcount ${stats.peakHeadcount}. " +
        "Lifetime revenue ${money(stats.totalRevenue)}."
    story += if (venture.isBacked) {
        "You raised ${money(venture.raised)} over ${plural(venture.rounds.size, "round")} and kept " +
            "${Format.percent(venture.founderEquity)} of the company."
    } else {
        "You never took a cent of outside money."
    }
    story += "You lived through ${stats.erasSeen.joinToString(", ") { it.title }}."
    return story
}
