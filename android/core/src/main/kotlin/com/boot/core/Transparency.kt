package com.boot.core

import kotlin.math.max
import kotlin.math.min

/*
 * Dev diary / transparency meter: community trust from open development vs radio silence.
 * Meter moves with diaries, silence during crises, and oversharing roadmaps. Affects
 * wishlists, fan trust, and mod goodwill when a scene exists. Lives on GamesStudio —
 * not a second reputation system.
 */

/** How open the studio feels, 0 (radio silence) … 1 (open book). Old saves default mid. */
fun GamesStudio.transparencyLabel(): String = when {
    transparency >= 0.75 -> "Open book"
    transparency >= 0.55 -> "Talkative"
    transparency >= 0.4 -> "Even-handed"
    transparency >= 0.25 -> "Guarded"
    else -> "Radio silence"
}

/** Cost of a public dev diary for the active project (or catalogue title). */
fun GameState.devDiaryCost(team: Int = 1): Double {
    val p = games?.projectOf(team)
    val size = p?.concept?.size ?: GameSize.MEDIUM
    return scaled(size.weeklyBudget * 0.35)
}

fun GameState.devDiaryBlocker(team: Int = 1): String? {
    val g = games ?: return "You don't run a game studio."
    val p = g.projectOf(team) ?: return "Nothing in development to diary about."
    if (week - g.lastDiaryWeek < 3) return "Fans want a few weeks between diaries."
    if (cash < devDiaryCost(team)) return "A diary costs ${money(devDiaryCost(team))}."
    if (p.reveals == 0 && p.hype < 5) return "Announce the game first — or build a little hype."
    return null
}

/** Publish a development diary: transparency up, wishlists and trust tick. */
fun GameState.publishDevDiary(team: Int = 1) {
    requireLive()
    devDiaryBlocker(team)?.let { throw notAvailable(it) }
    val g = games!!
    val p = g.projectOf(team)!!
    val cost = devDiaryCost(team)
    spend(cost, Lane.GAMES)
    g.lastDiaryWeek = week
    g.diariesPublished += 1
    g.transparency = min(1.0, g.transparency + 0.06)
    g.fanTrust = min(1.2, g.fanTrust + 0.01)
    p.wishlists += wishlistsFrom(2.5 + g.transparency * 4.0)
    p.hype = min(100.0, p.hype + 1.5 + g.transparency * 2.0)
    stats.devDiaries += 1
    addLog(
        "Dev diary for ${p.title}: ${money(cost)}. Transparency ${Format.percent(g.transparency)} (${g.transparencyLabel()}).",
        LogTone.GOOD, Lane.GAMES,
    )
    feedOnTransparency(p.title, "diary")
    publish(
        NewsDesk.YOU, "Community",
        "$companyName posts a ${p.title} development diary",
        "Open development builds trust — and wishlists.",
        LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS,
    )
}

fun GameState.overshareRoadmapBlocker(team: Int = 1): String? {
    val g = games ?: return "You don't run a game studio."
    val p = g.projectOf(team) ?: return "Nothing in development."
    if (p.roadmapOvershared) return "You've already overshared ${p.title}'s roadmap."
    if (p.reveals == 0) return "Announce the game before dumping the whole roadmap."
    if (cash < scaled(8_000.0)) return "A big roadmap reveal costs ${money(scaled(8_000.0))}."
    return null
}

/** Dump the whole roadmap: hype spike now, transparency up, but overpromise risk. */
fun GameState.overshareRoadmap(team: Int = 1) {
    requireLive()
    overshareRoadmapBlocker(team)?.let { throw notAvailable(it) }
    val g = games!!
    val p = g.projectOf(team)!!
    spend(scaled(8_000.0), Lane.GAMES)
    p.roadmapOvershared = true
    g.transparency = min(1.0, g.transparency + 0.1)
    p.hype = min(100.0, p.hype + 12.0)
    p.wishlists += wishlistsFrom(8.0)
    p.techDemoPromise = max(p.techDemoPromise, p.hype * 0.35)
    g.roadmapOvershareWeek = week
    addLog(
        "You overshared ${p.title}'s roadmap. Hype surges — if the game undersells it, reviews will remember.",
        LogTone.NEUTRAL, Lane.GAMES,
    )
    feedOnTransparency(p.title, "roadmap")
    museumBeat(LegacyBeatKind.MILESTONE, "Roadmap dump: ${p.title}", "Transparency ${Format.percent(g.transparency)}.")
}

/** Choose radio silence: transparency drops. Costly during crises (scars / unrest / bombs). */
fun GameState.goSilentBlocker(): String? {
    games ?: return "You don't run a game studio."
    return null
}

fun GameState.goSilent() {
    requireLive()
    goSilentBlocker()?.let { throw notAvailable(it) }
    val g = games!!
    val crisis = reputation.scars.any { it.severity > 0.1 } ||
        g.releases.any { it.bombed && week - it.week < 26 } ||
        events.any { it.kind is ModCrisis || it.kind is EventKind.PressScandal }
    val drop = if (crisis) 0.12 else 0.05
    g.transparency = max(0.0, g.transparency - drop)
    g.lastSilentWeek = week
    if (crisis) {
        g.fanTrust = max(0.3, g.fanTrust - 0.03)
        // Wishlists on announced games cool off when you stonewall a crisis.
        for (p in listOfNotNull(g.project, g.project2).filter { it.reveals > 0 }) {
            p.wishlists *= 0.94
        }
        addLog("Radio silence during a crisis. Transparency ${Format.percent(g.transparency)}. Fans notice.", LogTone.BAD, Lane.GAMES)
        feedOnTransparency(null, "silence_crisis")
    } else {
        addLog("You go quiet. Transparency ${Format.percent(g.transparency)} (${g.transparencyLabel()}).", LogTone.NEUTRAL, Lane.GAMES)
        feedOnTransparency(null, "silence")
    }
}

/** Weekly drift: diaries keep trust warm; long silence cools it; open studios help mod scenes. */
internal fun GameState.transparencyWeek(g: GamesStudio) {
    val quiet = week - g.lastDiaryWeek
    when {
        quiet > 26 && g.transparency > 0.2 -> g.transparency = max(0.05, g.transparency - 0.008)
        quiet > 13 -> g.transparency = max(0.1, g.transparency - 0.003)
        g.diariesPublished > 0 && quiet <= 6 -> g.transparency = min(1.0, g.transparency + 0.002)
    }
    // Transparency lifts wishlists slightly for announced projects.
    val lift = 0.5 + g.transparency
    for (p in listOfNotNull(g.project, g.project2).filter { it.reveals > 0 }) {
        p.wishlists += p.hype * 0.15 * (g.transparency - 0.45).coerceAtLeast(-0.2)
    }
    // Mod goodwill: open studios grow healthier scenes a little faster.
    if (g.transparency >= 0.55) {
        for (r in g.releases.filter { it.hasModScene }) {
            r.modHealth = min(1.0, r.modHealth + 0.002 * g.transparency)
            if (r.modPolicy != ModPolicy.CLAMP) {
                r.modBuzz = min(1.0, r.modBuzz + 0.0015 * g.transparency)
            }
        }
    } else if (g.transparency <= 0.25) {
        for (r in g.releases.filter { it.hasModScene && it.modPolicy == ModPolicy.IGNORE }) {
            r.modHealth = max(0.1, r.modHealth - 0.002)
        }
    }
    // Soft fan trust drift toward transparency.
    val trustPull = (0.85 + g.transparency * 0.3) - g.fanTrust
    g.fanTrust = (g.fanTrust + trustPull * 0.01).coerceIn(0.3, 1.2)
}

/** Wishlist conversion nudge from studio transparency (applied at launch via fanTrust already). */
internal fun GameState.transparencyWishlistFactor(): Double {
    val t = games?.transparency ?: 0.5
    return 0.92 + t * 0.16
}
