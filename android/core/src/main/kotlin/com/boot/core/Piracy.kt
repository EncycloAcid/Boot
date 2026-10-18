package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Piracy and copy protection.
 *
 * Popular games get copied. How much depends on the year and the machine: tapes and floppies were
 * copied in every school yard, then cracks and CD burners, then torrents. Cartridges were hard to
 * copy; modded disc consoles weren't. Some of the copies are sales you'd otherwise have made.
 *
 * Copy protection cuts that loss, at a price, and players don't like it: the stronger it is, the
 * louder they complain, and the heavy kinds can set off a wave of angry user reviews.
 */

/** Copy protection a game can ship with. Which ones exist depends on the year. */
@Serializable
enum class Protection(
    val title: String,
    val blurb: String,
    val from: Int,
    val until: Int,
    /** Share of piracy it stops. */
    val stops: Double,
    /** How much players mind it, 0...1. */
    val annoys: Double,
    /** What it costs for a medium-sized game today. */
    val cost: Double,
) {
    NONE("None", "Nothing stops copying.", 0, 9_999, 0.0, 0.0, 0.0),
    MANUAL_CHECK("Manual check", "Type in a word from page 12 of the manual. Mild, and easy to photocopy.", 0, 1_999, 0.25, 0.1, 20_000.0),
    DISK_LOCK("Disk protection", "Odd sectors a copier can't read. Stops casual copying; some real drives choke on it too.", 1_978, 1_997, 0.45, 0.3, 60_000.0),
    DISC_CHECK("Disc check", "The disc has to be in the drive to play.", 1_995, 2_012, 0.4, 0.2, 80_000.0),
    INTRUSIVE_DRM("Intrusive DRM", "Drivers that dig into the system. Hard to crack, and players hate it.", 2_003, 2_013, 0.7, 0.9, 250_000.0),
    ACTIVATION("Online activation", "A one-time check online when you first play.", 2_008, 9_999, 0.55, 0.3, 150_000.0),
    ALWAYS_ONLINE("Always online", "Needs a connection to play at all. Stops pirates, and people on trains.", 2_010, 9_999, 0.85, 1.0, 400_000.0),
    ANTI_TAMPER("Anti-tamper", "Holds off the crackers through launch, at some cost to performance.", 2_014, 9_999, 0.7, 0.5, 600_000.0),
}

/** The copy protection on offer in [year]. */
fun protections(year: Int): List<Protection> = Protection.entries.filter { year >= it.from && year <= it.until }

/** What [p] costs for a game of [size] in [year]. */
fun GameState.protectionCost(p: Protection, size: GameSize): Double =
    if (p == Protection.NONE) 0.0 else scaled(p.cost * (size.launchSales / GameSize.MEDIUM.launchSales).coerceIn(0.3, 4.0))

/** How copying works in [year], for the UI. */
fun piracyEra(year: Int): String = when {
    year < 1990 -> "Tape-to-tape and floppy copying"
    year < 2000 -> "Cracks and copy parties"
    year < 2008 -> "CD burners and warez sites"
    else -> "Torrents and day-one cracks"
}

/** Computers were always the easy ones to copy for. */
private val COMPUTERS = setOf(Platform.PC, Platform.MAC, Platform.AMIGA, Platform.C64, Platform.VAPOR_DECK, Platform.ROG_ALLY, Platform.STEAM_MACHINE)

/** Share of a game's would-be buyers on [p] who copy it instead, in [year], before any protection. */
fun piracyRate(p: Platform, year: Int): Double = when {
    p == Platform.WEB || p == Platform.OWN_CONSOLE -> 0.01
    p in COMPUTERS -> when {
        year < 1990 -> 0.4
        year < 2008 -> 0.32
        else -> 0.18
    }
    p == Platform.GRPHONE -> 0.12
    // Cartridges were hard to copy; modded disc consoles weren't; then consoles locked down.
    else -> when {
        year < 1995 -> 0.03
        year < 2013 -> 0.12
        else -> 0.03
    }
}

/** Share of [c]'s launch sales lost to piracy with [protection]: more for popular games. */
fun GameState.piracyLoss(c: GameConcept, protection: Protection, popularity: Double = 1.0): Double {
    val total = reach(c.platforms).takeIf { it > 0 } ?: return 0.0
    val rate = c.platforms.sumOf { piracyRate(it, calendarYear) * reach(listOf(it)) } / total
    // Live games and always-online ones are mostly safe: there's a server to check in with.
    val live = if (c.live) 0.3 else 1.0
    return (rate * popularity.coerceIn(0.5, 1.5) * (1 - protection.stops) * live).coerceIn(0.0, 0.5)
}

/** Why [team]'s game can't take [protection], or null if it can. */
fun GameState.protectionBlocker(team: Int, protection: Protection): String? {
    val p = games?.projectOf(team) ?: return "No game in development."
    if (protection != Protection.NONE && protection !in protections(calendarYear)) return "${protection.title} isn't around in $calendarYear."
    if (p.protection == protection) return "It already has ${protection.title.lowercase()}."
    return null
}

/** Set the copy protection [team]'s game ships with. It's paid for at launch. */
fun GameState.setProtection(team: Int, protection: Protection) {
    requireLive()
    protectionBlocker(team, protection)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    p.protection = protection
    if (protection != Protection.NONE) addLog("${p.title} will ship with ${protection.title.lowercase()}: ${money(protectionCost(protection, p.concept.size))} at launch.", LogTone.NEUTRAL, Lane.GAMES)
}

/** At launch: pay for the protection, count what pirates took, and hear from players about it. */
internal fun GameState.afterPiracy(p: GameProject, r: GameRelease, kept: Double, shelf: Double = 1.0) {
    val g = games ?: return
    val cost = protectionCost(p.protection, r.concept.size)
    if (cost > 0) spend(cost, Lane.GAMES)
    r.protection = p.protection
    val lost = 1 - kept
    // What it would have sold with nobody copying it.
    val untouched = r.weeklySales / max(0.01, kept * shelf) * shelf
    // Most people with a copy wouldn't have bought it; count about three copies per lost sale.
    r.pirated = untouched * lost / r.concept.unitPrice * 3
    stats.piracyLoss += untouched * lost
    if (p.protection.annoys > 0 && r.userScore > 0) {
        r.userScore = max(1.0, r.userScore - p.protection.annoys * 1.1)
        g.fanTrust = max(0.3, g.fanTrust - p.protection.annoys * 0.02)
    }
    val popular = r.score >= 7.5
    when {
        p.protection.annoys >= 0.8 -> publish(
            NewsDesk.GAMES, "Kotaku", "Players fume over ${r.title}'s ${p.protection.title.lowercase()}",
            "\"I paid for this and I can't play it.\" The forums aren't happy.", LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS,
        )
        popular && lost >= 0.12 -> publish(
            NewsDesk.GAMES, if (calendarYear < 2000) "Zzap!64" else "TorrentFreak",
            if (calendarYear < 1990) "Copies of ${r.title} are going round every school yard" else "${r.title} cracked within days",
            "Pirated copies are everywhere. ${if (p.protection == Protection.NONE) "It shipped with no copy protection." else "${p.protection.title} didn't hold for long."}",
            LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS,
        )
        p.protection.stops >= 0.7 && popular -> publish(
            NewsDesk.GAMES, "PC Gamer", "${r.title} still uncracked", "${p.protection.title} is holding up. Pirates are annoyed; so are some buyers.",
            LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS,
        )
    }
    if (r.pirated >= 1_000) addLog("Pirates took about ${Format.count(r.pirated)} copies of ${r.title}: roughly ${Format.percent(lost)} of its sales.", LogTone.NEUTRAL, Lane.GAMES)
}

/** How popular a game looks before launch, for working out who'd copy it. */
fun popularity(score: Double, hype: Double): Double = min(1.5, (score / 8) * (1 + hype / 200))
