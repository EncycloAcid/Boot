package com.boot.core

import kotlin.math.max
import kotlin.math.min

/*
 * Accessibility & localisation bets: costly upfront (budget / time), long-tail goodwill and
 * new regional markets if you invest. Hooks into Development projects and Strategy regions —
 * not a second marketing lane.
 */

/** Up-front cost to bake accessibility into a project. */
fun GameState.accessibilityCost(p: GameProject): Double = scaled(p.concept.size.weeklyBudget * 3.5)

/** Weeks of extra work (as a work-scale bump). */
const val ACCESSIBILITY_WORK = 1.08

fun GameState.accessibilityBlocker(team: Int = 1): String? {
    val p = games?.projectOf(team) ?: return "Nothing in development."
    if (p.accessibilityBet) return "${p.title} already has an accessibility pass."
    if (p.stage < 1 && p.completion < 0.25) return "Wait until foundations are in before the accessibility pass."
    if (cash < accessibilityCost(p)) return "An accessibility pass costs ${money(accessibilityCost(p))}."
    return null
}

/** Invest in accessibility: more work now, goodwill and user score later. */
fun GameState.investAccessibility(team: Int = 1) {
    requireLive()
    accessibilityBlocker(team)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    val cost = accessibilityCost(p)
    spend(cost, Lane.GAMES)
    p.accessibilityBet = true
    p.workScale *= ACCESSIBILITY_WORK
    p.spent += cost
    stats.accessibilityBets += 1
    addLog(
        "Accessibility pass on ${p.title}: ${money(cost)}, about ${Format.percent(ACCESSIBILITY_WORK - 1)} more work. Long-tail goodwill if it ships.",
        LogTone.GOOD, Lane.GAMES,
    )
    feedOnAccessLocal(p.title, "access")
}

/** Cost to localise a project for a region (or a broad multi-region push). */
fun GameState.localisationCost(p: GameProject, region: Region? = null): Double {
    val base = scaled(p.concept.size.weeklyBudget * 2.2)
    return if (region == null) base * 2.4 else base * (0.7 + region.openCost / 200_000.0)
}

const val LOCALISATION_WORK = 1.05

fun GameState.localisationBlocker(team: Int = 1, region: Region? = null): String? {
    val p = games?.projectOf(team) ?: return "Nothing in development."
    if (region != null) {
        if (region in p.localisationRegions) return "${p.title} is already localised for ${region.title}."
    } else if (p.localisationRegions.size >= 3) {
        return "${p.title} already has a broad localisation push."
    }
    val cost = localisationCost(p, region)
    if (cash < cost) return "Localisation costs ${money(cost)}."
    if (p.completion < 0.15) return "Get a little further in development first."
    return null
}

/** Localise for one region, or a broad push (null = three major regions). */
fun GameState.investLocalisation(team: Int = 1, region: Region? = null) {
    requireLive()
    localisationBlocker(team, region)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    val cost = localisationCost(p, region)
    spend(cost, Lane.GAMES)
    p.workScale *= LOCALISATION_WORK
    p.spent += cost
    if (region != null) {
        p.localisationRegions += region
    } else {
        p.localisationRegions += listOf(Region.EUROPE, Region.ASIA_PACIFIC, Region.LATIN_AMERICA)
            .filter { it !in p.localisationRegions }
            .take(3)
    }
    stats.localisationBets += 1
    val where = if (region != null) region.title else p.localisationRegions.joinToString { it.short }
    addLog(
        "Localisation bet on ${p.title} ($where): ${money(cost)}. New regional markets if it lands.",
        LogTone.GOOD, Lane.GAMES,
    )
    feedOnAccessLocal(p.title, "local")
}

/** On ship: goodwill, fan trust, and warm regional offices / territory. */
internal fun GameState.applyAccessLocalShip(r: GameRelease, p: GameProject) {
    val g = games ?: return
    if (p.accessibilityBet) {
        r.accessibility = true
        g.fanTrust = min(1.2, g.fanTrust + 0.025)
        reputation.adjust(1.5)
        addLog("${r.title}'s accessibility work earns quiet goodwill.", LogTone.GOOD, Lane.GAMES)
        museumBeat(LegacyBeatKind.TRIUMPH, "Accessibility: ${r.title}", "Long-tail goodwill locked in.")
    }
    if (p.localisationRegions.isNotEmpty()) {
        r.localisedRegions = p.localisationRegions.toList()
        g.fanTrust = min(1.2, g.fanTrust + 0.015 * p.localisationRegions.size)
        for (region in p.localisationRegions) {
            val o = strategy.office(region)
            if (o != null) {
                o.brand = min(1.0, o.brand + 0.08)
                o.share = min(0.4, o.share + 0.025)
            } else {
                // Soft awareness in that territory even without an office.
                marketing.boost[Lane.GAMES] = min(0.9, (marketing.boost[Lane.GAMES] ?: 0.0) + 0.01)
            }
        }
        addLog(
            "${r.title} opens new regional markets: ${p.localisationRegions.joinToString { it.short }}.",
            LogTone.GOOD, Lane.GAMES,
        )
        museumBeat(LegacyBeatKind.TRIUMPH, "Localised ${r.title}", p.localisationRegions.joinToString { it.title })
    }
}

/** Long-tail sales lift while the game is still selling. */
internal fun GameRelease.accessLocalLift(): Double {
    var lift = 1.0
    if (accessibility) lift += 0.04
    if (localisedRegions.isNotEmpty()) lift += 0.03 * localisedRegions.size.coerceAtMost(4)
    return lift
}
