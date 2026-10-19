package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

/*
 * Wishlists and the release calendar. Once a game is announced, fans wishlist it: every trailer
 * adds a burst, and the hype keeps adding more each week (faster once there's a date to count
 * down to), while interest slowly fades if it drags on. On launch day wishlists turn into sales,
 * more of them the better the reviews.
 *
 * You can set the release date on a calendar that shows what the timing would mean: the season
 * (each genre has one), the holiday rush and the January slump, the shows, and the big rival
 * releases that have been announced. On the day, a finished game launches by itself; an unfinished
 * one has to be delayed, and fans don't love a delay.
 */

/** Weeks of the year with a pull of their own, on top of the seasons. */
enum class LaunchWindow(val title: String, val lift: Double, val note: String) {
    HOLIDAY("Holiday rush", 1.2, "Gift buying lifts every launch."),
    SLUMP("January slump", 0.9, "Wallets are empty after the holidays."),
    ;

    companion object {
        fun at(week: Int): LaunchWindow? = when (GameClock.weekOfYear(week)) {
            in 46..51 -> HOLIDAY
            in 1..4 -> SLUMP
            else -> null
        }
    }
}

/** What launching a game in a given week would mean, as far as anyone can tell in advance. */
data class LaunchTiming(
    val week: Int,
    val season: GameSeason,
    /** The season's pull for this game's genre. */
    val seasonLift: Double,
    val window: LaunchWindow?,
    /** An announced rival release just before, which would take buyers from yours. */
    val rival: Announcement?,
    /** What that rival would cost your launch week. */
    val clash: Double = 1.0,
) {
    val lift: Double get() = seasonLift * (window?.lift ?: 1.0) * clash
}

fun GameState.launchTiming(concept: GameConcept, at: Int): LaunchTiming {
    val season = GameSeason.forWeek(at)
    val g = games
    val before = g?.market?.upcoming.orEmpty().filter { at - it.week in 0..6 && !g!!.owns(it.studio) }
    // The same rules as on the day: a release everyone's playing buries anything within three weeks,
    // and a big game in your genre takes some of your buyers.
    val mega = before.filter { it.mega && at - it.week <= 3 }.maxByOrNull { it.week }
    val rival = mega ?: before
        .filter { (it.genre == concept.genre || it.genre == concept.genre2) && it.size.ordinal >= concept.size.ordinal - 1 }
        .maxByOrNull { it.size.ordinal * 100 + it.week }
    val clash = when {
        mega != null -> 0.8
        rival != null -> 0.85
        else -> 1.0
    }
    return LaunchTiming(at, season, season.launchMultiplier(concept.genre), LaunchWindow.at(at), rival, clash)
}

/** The next [weeks] weeks, as launch weeks for the game a team is making. */
fun GameState.releaseCalendar(team: Int = 1, weeks: Int = 26): List<LaunchTiming> {
    val p = games?.projectOf(team) ?: return emptyList()
    return (1..weeks).map { launchTiming(p.concept, week + it) }
}

/** The share of wishlists that buy on launch day: 2% for a dud, up to 20% for a masterpiece. */
fun wishlistConversion(score: Double): Double = ((score - 4.0) / 30.0).clamped(0.02, 0.2)

/** Wishlists from a trailer that built [hype], with a bigger fanbase spreading the word further. */
internal fun GameState.wishlistsFrom(hype: Double): Double = hype * (150.0 + (games?.fans ?: 0.0) * 0.03)

/** A week of wishlists for the games that have been announced. */
internal fun GameState.wishlistWeek(g: GamesStudio) {
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        if (p.reveals == 0) continue
        p.wishlists += p.hype * (8.0 + g.fans * 0.0008) * (if (p.releaseWeek != null) 1.25 else 1.0)
        p.wishlists *= 0.996
    }
}

/** Launch day: a finished game with a date goes out by itself; an unfinished one has to be delayed. */
internal fun GameState.releaseDayWeek(g: GamesStudio) {
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        val due = p.releaseWeek ?: continue
        if (week < due) continue
        if (p.isReady) {
            val cert = launchCertBlocker(team)
            if (cert != null) {
                // Slip a week rather than crash the calendar — chase cert or wait it out.
                p.releaseWeek = week + 1
                p.delays += 1
                addLog("Launch day for ${p.title} slipped: $cert", LogTone.BAD, Lane.GAMES)
            } else {
                addLog("Launch day for ${p.title}.", LogTone.MILESTONE, Lane.GAMES)
                launchGame(team)
            }
        } else {
            enqueue(ReleaseSlip(team))
        }
    }
}

/** Sets (or with null, clears) the week a team's game launches. */
fun GameState.setReleaseDate(team: Int, at: Int?) {
    val p = studio().projectOf(team) ?: throw invalid("No game in development.")
    if (at != null && at <= week) throw invalid("Pick a week that hasn't happened yet.")
    val announced = p.releaseWeek == null && at != null && p.reveals > 0
    p.releaseWeek = at
    if (at == null) {
        addLog("${p.title} has no release date for now.", LogTone.NEUTRAL, Lane.GAMES)
    } else {
        addLog("${p.title} launches ${GameClock.label(at)}.", LogTone.NEUTRAL, Lane.GAMES)
        if (announced) {
            feedOnDate(p, GameClock.label(at))
            publish(
                NewsDesk.YOU, "IGN", "${p.title} launches ${GameClock.label(at)}", "Wishlist it now. ${Format.count(p.wishlists)} people already have.", LogTone.GOOD, Lane.GAMES,
                tag = NewsTag.SHOWS, quote = "We can't wait for everyone to play it.", quoteBy = founder.name, cover = p.newsCover(),
            )
        }
    }
}

/** A game that isn't ready on its release date. */
@Serializable @SerialName("releaseSlip")
data class ReleaseSlip(val team: Int) : EventKind

internal fun GameState.describeSlip(k: ReleaseSlip): Description? {
    val p = games?.projectOf(k.team) ?: return null
    val left = gameForecast(k.team)?.weeksToReady
    return Description(
        "${p.title} won't be ready for launch day",
        "It was due out this week, but the team needs " + (left?.let { "about ${Format.weeks(it)} more" } ?: "more time") + ". " +
            (if (p.wishlists >= 1) "${Format.count(p.wishlists)} people have it wishlisted." else "Nobody's waiting on it yet."),
        listOf(
            choice("Delay four weeks", "A new date. Fans grumble and a few wishlists drop off."),
            choice("Delay until it's done", "No date for now. More fans grumble, more wishlists drop off."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveSlip(k: ReleaseSlip, choice: Int) {
    val g = games ?: return
    val p = g.projectOf(k.team) ?: return
    p.delays += 1
    feedOnDelay(p, indefinite = choice != 0)
    if (choice == 0) {
        p.releaseWeek = week + 4
        g.fanTrust = max(0.5, g.fanTrust - 0.02)
        p.wishlists *= 0.92
        publish(
            NewsDesk.YOU, "IGN", "${p.title} delayed to ${GameClock.label(week + 4)}", "$companyName needs a few more weeks.", LogTone.BAD, Lane.GAMES,
            tag = NewsTag.DELAY, quote = "We'd rather it was right than on time. Thank you for your patience.", quoteBy = founder.name, cover = p.newsCover(),
        )
    } else {
        p.releaseWeek = null
        g.fanTrust = max(0.5, g.fanTrust - 0.03)
        p.wishlists *= 0.88
        publish(
            NewsDesk.YOU, "IGN", "${p.title} delayed indefinitely", "$companyName says it will be out when it's ready.", LogTone.BAD, Lane.GAMES,
            tag = NewsTag.DELAY, quote = "It'll be out when it's ready, and not a day before.", quoteBy = founder.name, cover = p.newsCover(),
        )
    }
}
