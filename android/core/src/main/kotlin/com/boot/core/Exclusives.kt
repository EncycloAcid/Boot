package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Console exclusives.
 *
 * A console maker can pay to have a game only on its machines: for good (a full exclusive) or for
 * a while (a timed exclusive, after which the other versions come out). The money comes up front;
 * the price is the players on other platforms, who can't buy it and don't like being left out.
 *
 * Offers come to promising games in development, and you can pitch a game to a maker yourself.
 */

/** How long a timed exclusive keeps the other versions back. */
const val TIMED_WINDOW = 26

/** A signed exclusive. */
@Serializable
data class Exclusive(val maker: String, val timed: Boolean, val payment: Double, val window: Int = TIMED_WINDOW)

/** A console maker's offer. */
@Serializable @SerialName("exclusiveOffer")
data class ExclusiveOffer(val team: Int, val maker: String, val full: Double, val timed: Double, val pitched: Boolean = false) : EventKind

/** The makers of the consoles a game is on (not PC, not your own console). */
fun exclusiveMakers(c: GameConcept): List<String> =
    c.platforms.filter { it != Platform.PC && it != Platform.OWN_CONSOLE }.map { it.maker }.distinct()

/** The platforms a game keeps when it's exclusive to [maker]. */
fun exclusivePlatforms(c: GameConcept, maker: String): List<Platform> = c.platforms.filter { it.maker == maker }

/** Share of the game's players it gives up by going exclusive to [maker]. */
fun GameState.exclusiveLoss(c: GameConcept, maker: String): Double {
    val all = reach(c.platforms)
    if (all <= 0) return 0.0
    return (1 - reach(exclusivePlatforms(c, maker)) / all).coerceIn(0.0, 1.0)
}

/**
 * What [maker] pays for a full exclusive: more for a bigger, better game, and more when going
 * exclusive costs it more players. A timed one pays a little under half.
 */
fun GameState.exclusiveTerms(p: GameProject, maker: String): Pair<Double, Double> {
    val forecast = games?.let { g -> (1..2).firstOrNull { g.projectOf(it) === p }?.let { gameForecast(it)?.expectedScore } } ?: 6.0
    val quality = (forecast / 7).pow(2).coerceIn(0.4, 1.8)
    val loss = exclusiveLoss(p.concept, maker)
    val full = marketingDeal(p) * 3 * quality * (0.6 + loss) * (1 + p.hype / 200)
    return full to full * 0.45
}

/** Why [team]'s game can't be pitched to [maker], or null if it can. */
fun GameState.exclusiveBlocker(team: Int, maker: String): String? {
    val p = games?.projectOf(team) ?: return "No game in development."
    if (p.exclusive != null) return "${p.title} is already ${p.exclusive!!.maker}'s."
    if (maker !in exclusiveMakers(p.concept)) return "${p.title} isn't on a $maker console."
    if (maker in p.pitched) return "$maker has already heard the pitch."
    if (p.isReady) return "It's too close to launch to sign a deal."
    if (events.any { it.kind is ExclusiveOffer }) return "An offer is already on the table."
    return null
}

/** Chances [maker] takes a pitch: a good forecast and some buzz help. */
fun GameState.pitchOdds(team: Int, maker: String): Double {
    val p = games?.projectOf(team) ?: return 0.0
    val f = gameForecast(team)?.expectedScore ?: 5.0
    return ((f - 5) / 4 + p.hype / 150 + (if (p.reveals > 0) 0.1 else 0.0)).coerceIn(0.05, 0.9)
}

/** Pitch [team]'s game to [maker] as an exclusive. They say yes with terms, or no. */
fun GameState.pitchExclusive(team: Int, maker: String) {
    requireLive()
    exclusiveBlocker(team, maker)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    p.pitched += maker
    if (rng.chance(pitchOdds(team, maker))) {
        val (full, timed) = exclusiveTerms(p, maker)
        enqueue(ExclusiveOffer(team, maker, full * 0.85, timed * 0.85, pitched = true))
    } else {
        addLog("$maker passed on ${p.title} as an exclusive. Maybe once it looks further along.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** Each week, a maker may come asking about a promising game. */
internal fun GameState.exclusiveWeek(g: GamesStudio) {
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        if (p.exclusive != null || p.isReady || p.stage < 1) continue
        if (events.any { it.kind is ExclusiveOffer }) return
        val makers = exclusiveMakers(p.concept).filter { it !in p.pitched }
        if (makers.isEmpty()) continue
        val f = gameForecast(team)?.expectedScore ?: continue
        if (f < 6.5 && p.hype < 25) continue
        val dice = flavour("exclusive offer $team")
        if (!dice.chance(0.02 + p.hype / 2000)) continue
        val maker = dice.pick(makers)
        p.pitched += maker
        val (full, timed) = exclusiveTerms(p, maker)
        enqueue(ExclusiveOffer(team, maker, full, timed))
    }
}

internal fun GameState.describeExclusive(k: ExclusiveOffer): Description? {
    val p = games?.projectOf(k.team) ?: return null
    val keep = exclusivePlatforms(p.concept, k.maker).joinToString { it.title }
    val others = p.concept.platforms.filter { it.maker != k.maker }
    val loss = exclusiveLoss(p.concept, k.maker)
    return Description(
        "${k.maker} wants an exclusive",
        "${if (k.pitched) "${k.maker} liked the pitch." else "${k.maker} has been watching ${p.title}."} " +
            "They'd pay to have it only on $keep" +
            (if (others.isNotEmpty()) ", which means giving up ${others.joinToString { it.title }}: about ${Format.percent(loss)} of its players." else ". It's only on their console anyway; they want it to stay that way.") +
            " Their marketing comes with it.",
        listOfNotNull(
            choice("Full exclusive", "${money(k.full)} now. Only ever on $keep. Fans elsewhere won't forget it."),
            if (others.isNotEmpty()) choice("Timed exclusive", "${money(k.timed)} now. The other versions come ${Format.weeks(TIMED_WINDOW)} after launch.") else null,
            choice("Turn it down", "Keep it everywhere."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveExclusive(k: ExclusiveOffer, choice: Int) {
    val g = games ?: return
    val p = g.projectOf(k.team) ?: return
    val label = describeExclusive(k)?.choices?.getOrNull(choice)?.label ?: return
    val keep = exclusivePlatforms(p.concept, k.maker)
    p.pitched += k.maker
    when (label) {
        "Full exclusive", "Timed exclusive" -> {
            val timed = label == "Timed exclusive"
            val pay = if (timed) k.timed else k.full
            cash += pay
            recordRevenue(pay, Lane.GAMES)
            p.exclusive = Exclusive(k.maker, timed, pay)
            p.exclusiveWith = k.maker
            p.hype = min(100.0, p.hype + if (timed) 4 else 8)
            g.fanTrust = max(0.5, g.fanTrust - if (timed) 0.02 else 0.05)
            stats.exclusives += 1
            val where = keep.joinToString(" and ") { it.title }
            addLog(
                "${p.title} is now ${if (timed) "a timed" else "an"} exclusive for $where. ${k.maker} paid ${money(pay)}.",
                LogTone.GOOD, Lane.GAMES,
            )
            if (p.reveals > 0) {
                publish(
                    NewsDesk.GAMES, "IGN",
                    if (timed) "${p.title} will launch first on $where" else "${p.title} is a $where exclusive",
                    if (timed) "${k.maker} has the game for ${Format.weeks(TIMED_WINDOW)} before it comes anywhere else." else "${k.maker} has signed $companyName's game. It won't be on other platforms.",
                    LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS,
                )
            }
            feedOnExclusive(p.title, k.maker, timed)
        }
        else -> addLog("You turned down ${k.maker}'s exclusive for ${p.title}.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** Before launch: an exclusive game ships only on its maker's platforms. */
internal fun GameState.exclusiveConcept(p: GameProject): GameConcept {
    val e = p.exclusive ?: return p.concept
    val keep = exclusivePlatforms(p.concept, e.maker)
    return if (keep.isEmpty()) p.concept else p.concept.copy(platforms = keep)
}

/** After launch: a timed exclusive's other versions are lined up; a full one closes the door. */
internal fun GameState.afterExclusiveLaunch(e: Exclusive, held: List<Platform>, r: GameRelease) {
    val g = games ?: return
    r.exclusiveTo = e.maker
    r.exclusiveUntil = if (e.timed) week + e.window else Int.MAX_VALUE
    if (e.timed) for (pl in held) g.ports += Port(r.id, pl, week + e.window)
}
