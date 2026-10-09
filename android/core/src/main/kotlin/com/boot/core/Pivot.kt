package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/** What forces the mid-game choice. */
@Serializable
enum class PivotTrigger {
    BUYOUT_OFFER, MARKET_CRASH, VIRAL_ACCIDENT;

    val title: String
        get() = when (this) {
            BUYOUT_OFFER -> "A buyout offer"
            MARKET_CRASH -> "Your market crashed"
            VIRAL_ACCIDENT -> "Viral by accident"
        }

    /** Multiplier on what the first business sells for if you pivot. */
    internal val saleMultiplier: Double
        get() = when (this) {
            BUYOUT_OFFER -> 1.3
            MARKET_CRASH -> 0.65
            VIRAL_ACCIDENT -> 1.1
        }
}

@Serializable
data class PivotOffer(
    val trigger: PivotTrigger,
    val week: Int,
    val headline: String,
    val body: String,
    /** What the first business would sell for if you pivot. */
    val salePrice: Double,
    val bidder: String? = null,
    /** The lane the accident points you toward. Picking it comes with a head start. */
    val suggestedLane: Lane? = null,
)

@Serializable
data class PivotRecord(
    val trigger: PivotTrigger,
    val week: Int,
    val from: Lane,
    val to: Lane,
    val mode: PivotMode,
    val salePrice: Double? = null,
)

/** A lane opened after the Pivot Point, beyond the second. */
@Serializable
data class ExpansionRecord(val week: Int, val lane: Lane)

/** The Pivot Point lands somewhere between year 2 and year 4. */
val PIVOT_WINDOW = 78..156
const val EXPANSION_STREAK_NEEDED = 12

internal fun GameState.checkPivotPoint() {
    if (pivot != null || pendingPivot != null || week < PIVOT_WINDOW.first) return
    var trigger: PivotTrigger? = when {
        world.era.demand(startingLane) <= 0.85 -> PivotTrigger.MARKET_CRASH
        valuation >= 300_000 && rng.chance(0.03) -> PivotTrigger.BUYOUT_OFFER
        rng.chance(0.02) -> PivotTrigger.VIRAL_ACCIDENT
        else -> null
    }
    if (trigger == null && week >= PIVOT_WINDOW.last) {
        trigger = if (valuation >= 150_000) PivotTrigger.BUYOUT_OFFER else PivotTrigger.VIRAL_ACCIDENT
    }
    if (trigger == null) return
    val offer = makePivotOffer(trigger)
    pendingPivot = offer
    addLog("The Pivot Point: ${offer.headline}", LogTone.MILESTONE)
}

private fun GameState.makePivotOffer(t: PivotTrigger): PivotOffer {
    val lane = startingLane
    val business = lane.businessName.lowercase()
    val price = max(laneValue(lane) * t.saleMultiplier, 20_000.0).roundToLong().toDouble()
    return when (t) {
        PivotTrigger.BUYOUT_OFFER -> {
            val bidder = world.rival(lane)?.name ?: "MegaCorp Holdings"
            PivotOffer(
                t, week,
                headline = "$bidder wants to buy your $business.",
                body = "$bidder has offered ${money(price)} for the $business. You can take the money and go all in on " +
                    "something new, keep it and expand into a second lane, or sell everything and walk away.",
                salePrice = price, bidder = bidder,
            )
        }
        PivotTrigger.MARKET_CRASH -> PivotOffer(
            t, week,
            headline = "${world.era.title} has gutted the ${lane.title.lowercase()} market.",
            body = "Demand in your lane has collapsed. The business would only fetch about ${money(price)} right now, " +
                "but staying put means riding out the slump. Time to pick a second lane.",
            salePrice = price,
        )
        PivotTrigger.VIRAL_ACCIDENT -> {
            val suggestion = when (lane) {
                Lane.GAMES, Lane.APPS, Lane.AI -> Lane.SOCIAL
                Lane.WEBSITES, Lane.HARDWARE, Lane.SOCIAL -> Lane.APPS
            }
            val story = when (lane) {
                Lane.GAMES -> "A clip from your game exploded online and the fans need somewhere to gather."
                Lane.WEBSITES -> "A little tool you built for a client went viral, and strangers want to download it."
                Lane.APPS -> "People started using your app to talk to each other instead of what it was built for."
                Lane.SOCIAL -> "A mini-app someone built inside your platform is suddenly everywhere."
                Lane.HARDWARE -> "Someone's DIY companion app for your device went viral."
                Lane.AI -> "Screenshots of your chatbot's best answers are all over the internet."
            }
            PivotOffer(
                t, week,
                headline = "Something went viral by accident.",
                body = "$story Lean into ${suggestion.title.lowercase()} and you start with a head start. " +
                    "Or take the attention somewhere else entirely.",
                salePrice = price, suggestedLane = suggestion,
            )
        }
    }
}

/** Lanes you can move into at the Pivot Point. */
val GameState.pivotChoices: List<Lane> get() = Lane.entries.filter { it !in lanes }

/** Why a choice can't be taken, or null if it can. */
fun GameState.pivotBlocker(lane: Lane, mode: PivotMode, fundWithVenture: Boolean): String? {
    val offer = pendingPivot ?: return "There's no decision to make right now."
    if (lane in lanes) return "You already run that lane."
    val available = cash + (if (mode == PivotMode.PIVOT) offer.salePrice else 0.0) -
        (if (mode == PivotMode.EXPAND) lane.setupCost else 0.0)
    if (lane.isCapitalIntensive && !fundWithVenture && available < lane.selfFundThreshold) {
        return "${lane.title} needs about ${money(lane.selfFundThreshold)} in the bank. Raise venture money to do it."
    }
    if (mode == PivotMode.EXPAND && !fundWithVenture && cash < lane.setupCost) {
        return "Setting up costs ${money(lane.setupCost)}; you have ${money(cash)}."
    }
    if (fundWithVenture) ventureForLane(lane).exceptionOrNull()?.let { return it.message }
    return null
}

/** The round you would raise to fund a move into [lane]. */
fun GameState.ventureForLane(lane: Lane): Result<RoundOffer> = roundOffer().map { offer ->
    val floor = when (lane) {
        Lane.AI -> 5_000_000.0
        Lane.HARDWARE -> 3_000_000.0
        else -> 500_000.0
    }
    if (offer.amount < floor) offer.copy(amount = floor, preMoney = max(offer.preMoney, floor * 3)) else offer
}

fun GameState.resolvePivot(lane: Lane, mode: PivotMode, fundWithVenture: Boolean = false) {
    val offer = pendingPivot ?: throw notAvailable("There's no Pivot Point to resolve.")
    pivotBlocker(lane, mode, fundWithVenture)?.let { throw notAvailable(it) }
    val from = startingLane
    if (fundWithVenture) ventureForLane(lane).getOrNull()?.let { takeRound(it) }

    when (mode) {
        PivotMode.PIVOT -> {
            cash += offer.salePrice
            stats.saleProceeds += offer.salePrice
            // People whose craft carries over follow you; the rest stay with the buyer.
            val followers = staff.count { it.lane == from && lane.weight(it.role) >= 0.8 }
            val stayed = staff.count { it.lane == from } - followers
            staff = staff.filterNot { it.lane == from && lane.weight(it.role) < 0.8 }
            closeLane(from)
            openLane(lane)
            for (e in staff) if (e.lane == from || e.lane == null) e.lane = lane
            val detail = if (followers == 0) "" else " $followers of your people came with you."
            addLog("You sold the ${from.businessName.lowercase()} for ${money(offer.salePrice)} and went all in on ${lane.title}.$detail", LogTone.MILESTONE)
            if (stayed > 0) addLog("$stayed staff stayed on with the buyer.")
        }
        PivotMode.EXPAND -> {
            spend(lane.setupCost, lane)
            openLane(lane)
            addLog("You kept the ${from.businessName.lowercase()} and expanded into ${lane.title}. Two lanes, one you.", LogTone.MILESTONE)
        }
    }

    if (lane == offer.suggestedLane) applyViralHeadStart(lane)
    stats.profitableStreak = 0
    pivot = PivotRecord(offer.trigger, week, from, lane, mode, if (mode == PivotMode.PIVOT) offer.salePrice else null)
    pendingPivot = null
}

/** Only for a buyout: sell the whole company and walk away. */
fun GameState.acceptPivotBuyout() {
    val offer = pendingPivot
    if (offer == null || offer.trigger != PivotTrigger.BUYOUT_OFFER) {
        throw notAvailable("Nobody is offering to buy the whole company.")
    }
    val price = max(offer.salePrice, valuation * 1.1)
    pendingPivot = null
    end(EndingKind.ACQUIRED, price, offer.bidder)
}

private fun GameState.applyViralHeadStart(lane: Lane) {
    reputation.adjust(5.0)
    when (lane) {
        Lane.SOCIAL -> social?.let { it.spark += 0.35 }
        Lane.APPS -> apps?.let { it.quality += 10 }
        Lane.GAMES -> games?.let { it.fans += 8_000 }
        Lane.WEBSITES -> web?.let { it.standing += 0.2 }
        Lane.HARDWARE -> {}
        Lane.AI -> ai?.let { it.capability += 5 }
    }
    addLog("The viral moment gives ${lane.title} a head start.", LogTone.GOOD)
}

// Later expansion and the ecosystem

/** Third lane (or a second after a clean pivot) unlocks once every lane you run is profitable. */
val GameState.expansionBlocker: String?
    get() {
        if (pivot == null) return "Expansion opens up after the Pivot Point."
        if (lanes.size >= 3) return "Three lanes is the limit. You're running an empire already."
        if (pendingPivot != null) return "Resolve the Pivot Point first."
        if (stats.profitableStreak < EXPANSION_STREAK_NEEDED) {
            return "Every lane you run has to be profitable for $EXPANSION_STREAK_NEEDED straight weeks " +
                "(${stats.profitableStreak}/$EXPANSION_STREAK_NEEDED)."
        }
        return null
    }

fun GameState.expand(lane: Lane, fundWithVenture: Boolean = false) {
    expansionBlocker?.let { throw notAvailable(it) }
    if (lane in lanes) throw invalid("You already run ${lane.title}.")
    if (fundWithVenture) {
        val round = ventureForLane(lane).getOrElse { throw it }
        takeRound(round)
    } else if (lane.isCapitalIntensive && cash < lane.selfFundThreshold) {
        throw insufficientFunds("${lane.title} needs about ${money(lane.selfFundThreshold)} in the bank. Raise venture money to do it.")
    }
    if (cash < lane.setupCost) throw insufficientFunds("Setting up costs ${money(lane.setupCost)}.")
    spend(lane.setupCost, lane)
    openLane(lane)
    expansions = expansions + ExpansionRecord(week, lane)
    stats.profitableStreak = 0
    addLog("You expanded into ${lane.title}. ${lanes.size} lanes under one roof.", LogTone.MILESTONE)
    if (isEcosystem) {
        addLog("Hardware, software and services together. You're an ecosystem company now. Hold on to it.", LogTone.MILESTONE)
    }
}

/** The prestige goal: hardware, software and services together. */
val GameState.isEcosystem: Boolean
    get() = Lane.HARDWARE in lanes &&
        (Lane.APPS in lanes || Lane.GAMES in lanes) &&
        (Lane.AI in lanes || Lane.SOCIAL in lanes || Lane.WEBSITES in lanes)
