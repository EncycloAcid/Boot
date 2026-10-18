package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Strategy layer: regional offices, lane-specific alliances, and a Venture Arm of minority
 * investments. Weekly outcomes react to the era's economic cycle, company brand strength, and
 * how big the active portfolio is.
 */

/** Where you can put a regional office. */
@Serializable
enum class Region {
    NORTH_AMERICA, EUROPE, ASIA_PACIFIC, LATIN_AMERICA, MIDDLE_EAST_AFRICA;

    val title: String
        get() = when (this) {
            NORTH_AMERICA -> "North America"
            EUROPE -> "Europe"
            ASIA_PACIFIC -> "Asia Pacific"
            LATIN_AMERICA -> "Latin America"
            MIDDLE_EAST_AFRICA -> "Middle East & Africa"
        }

    val short: String
        get() = when (this) {
            NORTH_AMERICA -> "NA"
            EUROPE -> "EU"
            ASIA_PACIFIC -> "APAC"
            LATIN_AMERICA -> "LATAM"
            MIDDLE_EAST_AFRICA -> "MEA"
        }

    /** Up-front cost to open an office here. */
    val openCost: Double
        get() = when (this) {
            NORTH_AMERICA -> 120_000.0
            EUROPE -> 140_000.0
            ASIA_PACIFIC -> 110_000.0
            LATIN_AMERICA -> 70_000.0
            MIDDLE_EAST_AFRICA -> 80_000.0
        }

    /** Cost to run a localization push. */
    val localizeCost: Double get() = openCost * 0.35

    /** Base weekly territory revenue potential once brand and share are healthy. */
    val baseTerritory: Double
        get() = when (this) {
            NORTH_AMERICA -> 8_000.0
            EUROPE -> 7_500.0
            ASIA_PACIFIC -> 9_000.0
            LATIN_AMERICA -> 4_500.0
            MIDDLE_EAST_AFRICA -> 5_000.0
        }
}

/** What a two-year alliance is for. */
@Serializable
enum class AllianceKind {
    DISTRIBUTION, CLOUD, CONTENT, PLATFORM;

    val title: String
        get() = when (this) {
            DISTRIBUTION -> "Distribution"
            CLOUD -> "Cloud capacity"
            CONTENT -> "Content"
            PLATFORM -> "Platform ecosystem"
        }

    val detail: String
        get() = when (this) {
            DISTRIBUTION -> "Shelf space, store featuring, and channel partners in your lane."
            CLOUD -> "Reserved compute and bandwidth at friendlier rates."
            CONTENT -> "Co-marketing, shared IP windows, and creator deals."
            PLATFORM -> "SDK access, certification fast-lanes, and install-base reach."
        }

    val signCost: Double
        get() = when (this) {
            DISTRIBUTION -> 90_000.0
            CLOUD -> 120_000.0
            CONTENT -> 75_000.0
            PLATFORM -> 150_000.0
        }
}

@Serializable
enum class VentureStage {
    SEED, SERIES_A;

    val title: String
        get() = when (this) {
            SEED -> "Seed"
            SERIES_A -> "Series A"
        }
}

/** A regional office: localize to grow brand, share, and recurring territory revenue. */
@Serializable
data class RegionalOffice(
    val region: Region,
    val openedWeek: Int,
    /** Local brand strength, 0...1. */
    var brand: Double = 0.12,
    /** Local market share, 0...1. */
    var share: Double = 0.02,
    var localized: Boolean = false,
    var weeklyRevenue: Double = 0.0,
)

/** A two-year, lane-specific partnership. */
@Serializable
data class Alliance(
    val id: Int,
    val partner: String,
    val kind: AllianceKind,
    val lane: Lane,
    val signedWeek: Int,
    var expiresWeek: Int,
    /** 0...1. */
    var trust: Double = 0.55,
    /** Economic value of the deal; grows while trust holds. */
    var value: Double,
    var expired: Boolean = false,
) {
    val yearsLeft: Double get() = max(0.0, (expiresWeek - signedWeek).toDouble()) // filled by caller with current week
    fun weeksLeft(week: Int): Int = max(0, expiresWeek - week)
    val active: Boolean get() = !expired
}

/** A minority stake in an external startup (Venture Arm). */
@Serializable
data class VentureHolding(
    val id: Int,
    val name: String,
    val lane: Lane,
    /** Ownership fraction, typically 0.05...0.25. */
    var ownership: Double,
    /** How strongly you believe in the thesis, 0...1. */
    var conviction: Double,
    /** Cash you've put in (investment basis). */
    var basis: Double,
    /** Current company valuation. */
    var valuation: Double,
    var stage: VentureStage,
    val seededWeek: Int,
    var exitWeek: Int? = null,
    var exitProceeds: Double = 0.0,
) {
    val exited: Boolean get() = exitWeek != null
    val marketValue: Double get() = if (exited) exitProceeds else valuation * ownership
    val paperGain: Double get() = marketValue - basis
}

/** Regional offices, alliances, and Venture Arm holdings. Save-stable. */
@Serializable
class StrategyBook {
    val offices = mutableListOf<RegionalOffice>()
    val alliances = mutableListOf<Alliance>()
    val holdings = mutableListOf<VentureHolding>()
    var nextAllianceId = 1
    var nextHoldingId = 1
    var lastTerritory = 0.0
    var lastAllianceYield = 0.0

    fun office(region: Region): RegionalOffice? = offices.firstOrNull { it.region == region }
    val activeAlliances: List<Alliance> get() = alliances.filter { it.active && !it.expired }
    val liveHoldings: List<VentureHolding> get() = holdings.filter { !it.exited }
}

private val alliancePartners = listOf(
    "Horizon Partners", "Lattice Channel", "Nimbus Cloud Co", "Beacon Media", "Keystone Platforms",
    "Atlas Retail", "Cedar Compute", "Folio Rights", "Summit Storefronts", "Orbit Kits",
)

private val startupNames = listOf(
    "Pinecone Labs", "Brightwire", "Nollie", "Kotlin Cove", "Amber Circuit", "Fieldnote",
    "Quillstack", "Harborbit", "Mossline", "Copperframe", "Lumenfold", "Driftkit",
)

/** How hot/cold the economy feels for strategy outcomes. */
fun GameState.marketCycle(): Double {
    val appetite = world.investorAppetite
    val phase = when (world.phase) {
        EraPhase.DAWN -> 1.05
        EraPhase.PEAK -> 1.15
        EraPhase.FADING -> 0.88
    }
    return (appetite * phase).coerceIn(0.4, 1.8)
}

/** Brand strength across open lanes, 0.2...1.4-ish. */
internal fun GameState.companyBrandStrength(): Double {
    if (lanes.isEmpty()) return 0.4
    val lifts = lanes.map { brandLift(it) }.average()
    val rep = (reputation.score / 100.0).coerceIn(0.2, 1.2)
    return (0.55 * lifts + 0.45 * rep).coerceIn(0.25, 1.5)
}

/** How large the active portfolio feels (lanes + products), for ops scaling. */
internal fun GameState.portfolioScale(): Double {
    var score = lanes.size * 0.35
    games?.let { score += 0.15 + it.releases.size * 0.04 }
    apps?.takeIf { it.isLive }?.let { score += 0.25 }
    social?.let { score += 0.2 }
    hardware?.let { score += 0.2 }
    ai?.let { score += 0.25 }
    os?.let { score += 0.2 + it.versionsShipped * 0.05 }
    score += strategy.offices.size * 0.12
    score += strategy.activeAlliances.size * 0.08
    score += strategy.liveHoldings.size * 0.06
    return score.coerceIn(0.2, 4.0)
}

fun GameState.openOfficeBlocker(region: Region): String? {
    if (strategy.office(region) != null) return "You already have an office in ${region.title}."
    if (Research.REGIONAL_EXPANSION !in researched && strategy.offices.isNotEmpty()) {
        return "Research regional expansion to open another office."
    }
    if (office < Office.COWORKING && strategy.offices.isEmpty()) {
        return "Move out of the apartment before opening a regional office."
    }
    if (cash < region.openCost) return "Opening in ${region.title} costs ${money(region.openCost)}."
    return null
}

fun GameState.openOffice(region: Region) {
    openOfficeBlocker(region)?.let { throw notAvailable(it) }
    spend(region.openCost, null)
    strategy.offices += RegionalOffice(region, week)
    addLog("Opened a regional office in ${region.title} for ${money(region.openCost)}.", LogTone.MILESTONE)
    publish(
        NewsDesk.YOU, businessOutlet(),
        "$companyName opens in ${region.title}",
        "A new regional hub to grow local brand and territory revenue.",
        LogTone.GOOD,
    )
}

fun GameState.localizeOfficeBlocker(region: Region): String? {
    val o = strategy.office(region) ?: return "No office in ${region.title}."
    if (o.localized) return "${region.title} is already localized."
    if (cash < region.localizeCost) return "Localizing ${region.title} costs ${money(region.localizeCost)}."
    return null
}

fun GameState.localizeOffice(region: Region) {
    localizeOfficeBlocker(region)?.let { throw notAvailable(it) }
    val o = strategy.office(region)!!
    spend(region.localizeCost, null)
    o.localized = true
    o.brand = min(1.0, o.brand + 0.18)
    o.share = min(0.35, o.share + 0.04)
    addLog("Localized the ${region.title} office. Local brand and share are climbing.", LogTone.GOOD)
}

fun GameState.signAllianceBlocker(kind: AllianceKind, lane: Lane): String? {
    if (Research.GLOBAL_ALLIANCES !in researched) return "Research global alliances first."
    if (lane !in lanes) return "Open ${lane.title} before signing a ${lane.title.lowercase()} alliance."
    if (strategy.activeAlliances.any { it.kind == kind && it.lane == lane }) {
        return "You already have a ${kind.title.lowercase()} alliance for ${lane.title}."
    }
    if (strategy.activeAlliances.size >= 4) return "Four live alliances is enough for now."
    if (cash < kind.signCost) return "Signing costs ${money(kind.signCost)}."
    return null
}

fun GameState.signAlliance(kind: AllianceKind, lane: Lane) {
    signAllianceBlocker(kind, lane)?.let { throw notAvailable(it) }
    spend(kind.signCost, lane)
    val partner = rng.pick(alliancePartners.filter { name -> strategy.alliances.none { it.partner == name && it.active } }
        .ifEmpty { alliancePartners })
    val a = Alliance(
        id = strategy.nextAllianceId++,
        partner = partner,
        kind = kind,
        lane = lane,
        signedWeek = week,
        expiresWeek = week + 104,
        trust = (0.5 + companyBrandStrength() * 0.15).coerceIn(0.35, 0.85),
        value = kind.signCost * (0.8 + portfolioScale() * 0.1),
    )
    strategy.alliances += a
    addLog(
        "Signed a two-year ${kind.title.lowercase()} alliance with $partner for ${lane.title}.",
        LogTone.GOOD, lane,
    )
}

/** Seed a minority investment in a startup in an open lane. */
fun GameState.seedVentureBlocker(lane: Lane): String? {
    if (Research.VENTURE_ARM !in researched) return "Research the Venture Arm first."
    if (lane !in lanes) return "${lane.title} isn't one of your businesses yet."
    if (strategy.liveHoldings.size >= 6) return "Six live holdings is enough. Exit some first."
    val ticket = seedTicket(lane)
    if (cash < ticket) return "A seed cheque in ${lane.title} costs ${money(ticket)}."
    return null
}

fun GameState.seedTicket(lane: Lane): Double =
    (40_000.0 + lane.setupCost * 0.08) * (0.85 + marketCycle() * 0.2)

/** Expected ownership fraction for a seed at [conviction] (preview for UI). */
fun GameState.seedOwnershipPreview(conviction: Double): Double =
    (0.08 + conviction.coerceIn(0.2, 1.0) * 0.06).coerceIn(0.05, 0.18)

fun GameState.seedVenture(lane: Lane, conviction: Double = 0.55) {
    seedVentureBlocker(lane)?.let { throw notAvailable(it) }
    val ticket = seedTicket(lane)
    spend(ticket, null)
    val name = rng.pick(startupNames.filter { n -> strategy.holdings.none { it.name == n && !it.exited } }
        .ifEmpty { startupNames })
    val c = conviction.coerceIn(0.2, 1.0)
    val ownership = seedOwnershipPreview(c)
    val valuation = ticket / ownership
    val h = VentureHolding(
        id = strategy.nextHoldingId++,
        name = name,
        lane = lane,
        ownership = ownership,
        conviction = c,
        basis = ticket,
        valuation = valuation,
        stage = VentureStage.SEED,
        seededWeek = week,
    )
    strategy.holdings += h
    addLog(
        "Venture Arm seeded $name (${lane.title}) for ${money(ticket)} — ${Format.percent(ownership)} ownership.",
        LogTone.GOOD, lane,
    )
}

/** Rough cash if you exited [h] this week (cycle + conviction multiple). */
fun GameState.exitQuote(h: VentureHolding): Double {
    if (h.exited) return h.exitProceeds
    val multiple = (0.85 + h.conviction * 0.35) * marketCycle().coerceIn(0.55, 1.5)
    return h.valuation * h.ownership * multiple
}

/** Weeks until a Seed holding can take a Series A follow-on (0 if ready or not Seed). */
fun GameState.followOnWeeksLeft(h: VentureHolding): Int {
    if (h.exited || h.stage != VentureStage.SEED) return 0
    return max(0, 26 - (week - h.seededWeek))
}

/** Human label for the current market cycle (Venture Arm marks / exits). */
fun GameState.marketCycleLabel(): String {
    val c = marketCycle()
    return when {
        c >= 1.25 -> "Hot"
        c >= 1.05 -> "Warm"
        c >= 0.9 -> "Steady"
        c >= 0.7 -> "Cool"
        else -> "Cold"
    }
}

fun GameState.followOnBlocker(id: Int): String? {
    val h = strategy.holdings.firstOrNull { it.id == id } ?: return "No such holding."
    if (h.exited) return "${h.name} already exited."
    if (h.stage != VentureStage.SEED) return "${h.name} is already at ${h.stage.title}."
    if (week - h.seededWeek < 26) return "Wait six months after seed before a follow-on."
    val cheque = followOnTicket(h)
    if (cash < cheque) return "A Series A follow-on costs ${money(cheque)}."
    return null
}

fun GameState.followOnTicket(h: VentureHolding): Double =
    h.basis * (1.4 + marketCycle() * 0.35) * (1.1 - h.conviction * 0.15)

fun GameState.followOnVenture(id: Int) {
    followOnBlocker(id)?.let { throw notAvailable(it) }
    val h = strategy.holdings.first { it.id == id }
    val cheque = followOnTicket(h)
    spend(cheque, null)
    // Follow-on buys more ownership at a higher post-money.
    val newOwnership = (h.ownership + 0.04 + h.conviction * 0.03).coerceAtMost(0.28)
    val post = cheque / (newOwnership - h.ownership).coerceAtLeast(0.02)
    h.valuation = max(h.valuation * 1.35 * marketCycle().coerceIn(0.7, 1.4), post)
    h.ownership = newOwnership
    h.basis += cheque
    h.stage = VentureStage.SERIES_A
    h.conviction = min(1.0, h.conviction + 0.08)
    addLog(
        "Followed on into ${h.name}'s Series A for ${money(cheque)}. Ownership ${Format.percent(h.ownership)}.",
        LogTone.GOOD, h.lane,
    )
}

fun GameState.exitVentureBlocker(id: Int): String? {
    val h = strategy.holdings.firstOrNull { it.id == id } ?: return "No such holding."
    if (h.exited) return "Already exited."
    if (week - h.seededWeek < 13) return "Hold at least a quarter before exiting."
    return null
}

fun GameState.exitVenture(id: Int) {
    exitVentureBlocker(id)?.let { throw notAvailable(it) }
    val h = strategy.holdings.first { it.id == id }
    val proceeds = exitQuote(h)
    cash += proceeds
    recordRevenue(proceeds, null)
    h.exitWeek = week
    h.exitProceeds = proceeds
    val tone = if (proceeds >= h.basis) LogTone.GOOD else LogTone.BAD
    addLog(
        "Exited ${h.name}: ${money(proceeds)} on a ${money(h.basis)} basis" +
            (if (proceeds >= h.basis) " (gain ${money(proceeds - h.basis)})." else " (loss ${money(h.basis - proceeds)})."),
        tone, h.lane,
    )
}

/** Weekly strategy tick: territory revenue, alliance yield, venture marks, expiries. */
internal fun GameState.strategyWeek() {
    val cycle = marketCycle()
    val brand = companyBrandStrength()
    val scale = portfolioScale()
    var territory = 0.0
    for (o in strategy.offices) {
        val localLift = if (o.localized) 1.35 else 1.0
        // Grow brand and share toward a soft ceiling shaped by company brand and cycle.
        val brandTarget = (0.25 + brand * 0.45 * localLift).coerceIn(0.15, 0.95)
        val shareTarget = (0.03 + brand * 0.08 * localLift * cycle.coerceIn(0.6, 1.3)).coerceIn(0.02, 0.28)
        o.brand += (brandTarget - o.brand) * 0.04
        o.share += (shareTarget - o.share) * 0.03
        o.brand = o.brand.coerceIn(0.05, 1.0)
        o.share = o.share.coerceIn(0.01, 0.4)
        val rev = o.region.baseTerritory * o.brand * (0.4 + o.share * 4) * cycle * (0.7 + scale * 0.15) * localLift
        o.weeklyRevenue = rev
        territory += rev
    }
    strategy.lastTerritory = territory
    if (territory > 0) {
        cash += territory
        recordRevenue(territory, null)
    }

    var allianceYield = 0.0
    for (a in strategy.alliances) {
        if (a.expired) continue
        if (week >= a.expiresWeek) {
            a.expired = true
            addLog("Alliance with ${a.partner} (${a.kind.title.lowercase()} · ${a.lane.title}) expired.", LogTone.NEUTRAL, a.lane)
            continue
        }
        if (a.lane !in lanes) {
            a.trust = max(0.1, a.trust - 0.02)
            continue
        }
        val laneDemand = world.demand(a.lane)
        val trustDelta = when {
            cycle >= 1.1 && brand >= 0.7 -> 0.01
            cycle <= 0.75 -> -0.015
            else -> 0.003
        }
        a.trust = (a.trust + trustDelta).coerceIn(0.1, 1.0)
        a.value *= 1.0 + 0.004 * a.trust * cycle * laneDemand
        val weekly = a.value * 0.004 * a.trust * (0.8 + scale * 0.1)
        allianceYield += weekly
        // Soft lane boost via marketing brand awareness.
        val cur = marketing.boost[a.lane] ?: 0.0
        marketing.boost[a.lane] = min(1.0, cur + 0.002 * a.trust)
    }
    strategy.lastAllianceYield = allianceYield
    if (allianceYield > 0) {
        cash += allianceYield
        recordRevenue(allianceYield, null)
    }

    for (h in strategy.liveHoldings) {
        val laneDemand = world.demand(h.lane)
        val drift = 1.0 + rng.normal(sd = 0.025) + (cycle - 1.0) * 0.04 + (laneDemand - 1.0) * 0.03 +
            (h.conviction - 0.5) * 0.01
        h.valuation = max(h.basis * 0.4, h.valuation * drift.coerceIn(0.92, 1.08))
    }
}
