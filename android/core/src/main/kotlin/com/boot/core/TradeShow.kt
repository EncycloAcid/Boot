package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * The year's big games show (the Consumer Electronics Show, then E3, then Summer Game Fest).
 * Once a year you can book a booth to show off what you're making. A bigger booth draws a bigger
 * crowd, but the crowd judges what's on it: a promising, nearly finished game builds real hype,
 * an early or buggy demo can fall flat, and a great game on a big stage can win Best in Show.
 * With nothing in development the booth shows your back catalogue: fans come, hype has nowhere
 * to go.
 */

/** The week of the year the show opens. */
const val SHOW_WEEK = 26

/** Fans needed before the organisers offer you the main hall. */
const val MAIN_HALL_FANS = 50_000.0

enum class Booth(val title: String, val blurb: String, internal val base: Double, internal val crowd: Double, internal val hype: Double, internal val fanShare: Double) {
    SMALL("Small booth", "A corner of the hall. Some hype, a few new fans.", 15_000.0, 1.0, 8.0, 0.02),
    MEDIUM("Medium booth", "A proper stand. Real hype and a crowd.", 90_000.0, 3.0, 18.0, 0.045),
    LARGE("Large booth", "The main hall. Everyone's watching, for better or worse.", 400_000.0, 8.0, 32.0, 0.08),
    KEYNOTE("Keynote stage", "Your own hour on the main stage. The whole industry tunes in.", 1_200_000.0, 15.0, 45.0, 0.12),
}

/** Fans needed before the organisers offer you a keynote. */
const val KEYNOTE_FANS = 250_000.0

/** What a booth costs this year. Shows were cheaper in the past, like everything else. */
fun GameState.boothCost(b: Booth): Double = max(1_000.0, (b.base * pastScale(calendarYear.toDouble()) / 1_000).roundToLong() * 1_000.0)

/** The booths on offer. The main hall is kept for studios with a following. */
fun GameState.boothsOpen(): List<Booth> = Booth.entries.filter {
    val fans = games?.fans ?: 0.0
    when (it) {
        Booth.LARGE -> fans >= MAIN_HALL_FANS
        Booth.KEYNOTE -> fans >= KEYNOTE_FANS
        else -> true
    }
}

/** The next show: what it's called and how many weeks until it opens (0 for this week). */
data class UpcomingShow(val name: String, val weeks: Int)

fun GameState.nextShow(): UpcomingShow? {
    if (games == null) return null
    val w = GameClock.weekOfYear(week)
    val weeks = Math.floorMod(SHOW_WEEK - w, GameClock.WEEKS_PER_YEAR)
    val year = calendarYear + if (w > SHOW_WEEK) 1 else 0
    return UpcomingShow(gamesShowIn(year).replaceFirstChar { it.uppercase() }, weeks)
}

/** How the show went, for the report that follows it. */
@Serializable
data class ShowReport(
    val show: String,
    /** Your booth at the trade show; null for a trailer on someone's stage or your own stream. */
    val booth: Booth?,
    /** The game on the booth, or null for the back catalogue. */
    val game: String?,
    val visitors: Int,
    val hype: Double,
    val fans: Double,
    val bestInShow: Boolean,
    /** What happened, a line or two. */
    val press: List<String>,
    val wishlists: Double = 0.0,
    /** How the crowd took it, 0 to 1. */
    val reaction: Double = 0.5,
    /** What the outlets said. */
    val quotes: List<PressQuote> = emptyList(),
    /** What the chat said. */
    val chat: List<String> = emptyList(),
    /** Watched on a stream rather than visited in a hall. */
    val stream: Boolean = false,
)

@Serializable @SerialName("showResults")
data class ShowResults(val report: ShowReport) : EventKind

/** What the crowd makes of the booth: about 0.2 for a dull one to 1.5 for a sensation, and why. */
internal data class Showcase(val appeal: Double, val expected: Double, val early: Boolean, val buggy: Boolean, val notes: List<String>)

internal fun GameState.showcase(): Showcase {
    val g = games ?: return Showcase(0.2, 0.0, early = false, buggy = false, notes = emptyList())
    val p = g.project
    if (p == null) {
        // The back catalogue is only as exciting as your best recent game.
        val best = g.releases.filter { week - it.week < 104 }.maxOfOrNull { it.score } ?: 4.0
        return Showcase(((best - 4.0) / 10.0 + 0.2).clamped(0.15, 0.6), 0.0, early = false, buggy = false, notes = emptyList())
    }
    val expected = gameForecast()?.expectedScore ?: 5.0
    var appeal = ((expected - 4.0) / 4.5).clamped(0.1, 1.25)
    // Half-finished shows as well as finished; a first-week prototype doesn't.
    appeal *= 0.55 + 0.45 * min(1.0, p.completion / 0.5)
    val notes = mutableListOf<String>()
    if (g.trend?.matches(p.concept) == true) {
        appeal *= 1.15
        notes += "on trend"
    }
    val original = p.concept.sequelOf?.let { g.release(it) }
    if (original != null && original.score >= 8.0) {
        appeal *= 1.2
        notes += "the sequel to a hit"
    }
    val buggy = p.bugs > 5 && p.bugs > (p.design + p.tech) * 0.25
    return Showcase(appeal.clamped(0.1, 1.5), expected, early = p.completion < 0.25, buggy = buggy, notes = notes)
}

/** The show itself: [booth] null to stay home. */
internal fun GameState.holdShow(booth: Booth?) {
    val g = games ?: return
    val name = gamesShowIn(calendarYear).replaceFirstChar { it.uppercase() }
    if (booth == null) {
        addLog("You skipped $name this year.", LogTone.NEUTRAL, Lane.GAMES)
        return
    }
    spend(boothCost(booth), Lane.GAMES)
    val p = g.project
    val look = showcase()
    val press = mutableListOf<String>()
    var appeal = look.appeal
    var hype = if (p != null) booth.hype * (0.4 + appeal) * p.revealBoost * founderHype else 0.0
    var crashed = false
    if (p != null) {
        when {
            look.buggy && booth >= Booth.LARGE && rng.chance(0.4) -> {
                crashed = true
                hype *= 0.3
                appeal *= 0.6
                reputation.adjust(-1.0)
                press += "The ${p.title} demo crashed on the main stage. The clip is everywhere."
            }
            look.early && booth != Booth.SMALL -> {
                hype *= 0.5
                press += "Critics said ${p.title} was too early to judge: a trailer and a lot of promises."
            }
            appeal >= 1.0 -> press += "Queues wrapped round the hall for ${p.title}" + (look.notes.takeIf { it.isNotEmpty() }?.let { ", ${it.joinToString(" and ")}" } ?: "") + "."
            appeal >= 0.6 -> press += "${p.title} drew a steady crowd" + (look.notes.takeIf { it.isNotEmpty() }?.let { ", helped by being ${it.joinToString(" and ")}" } ?: "") + "."
            else -> press += "Few stopped for long at ${p.title}. The press had bigger games to cover."
        }
    } else {
        press += "With nothing new to show, fans came by to talk about your old games."
    }
    val visitors = (booth.crowd * 6_000 * (0.4 + appeal) * rng.normal(1.0, 0.08).clamped(0.8, 1.2)).roundToInt()
    var fans = max(400.0 * booth.crowd, g.fans * booth.fanShare * (0.5 + appeal)) * (if (p == null) 0.6 else 1.0)
    val best = p != null && !crashed && booth != Booth.SMALL && look.expected >= 8.0 && appeal >= 0.85 &&
        rng.chance(
            when (booth) {
                Booth.KEYNOTE -> 0.7
                Booth.LARGE -> 0.6
                else -> 0.25
            },
        )
    if (best) {
        hype += 15.0
        fans *= 1.25
        reputation.adjust(3.0)
        stats.bestInShow += 1
        press += "${p!!.title} won Best in Show."
    }
    if (booth >= Booth.LARGE && !crashed) reputation.adjust(if (booth == Booth.KEYNOTE) 2.0 else 1.0)
    if (booth == Booth.KEYNOTE) stats.keynotes += 1
    // A trip to the show is good for the team.
    if (booth != Booth.SMALL) for (e in staff) e.morale = min(1.0, e.morale + 0.05)
    val listsBefore = p?.wishlists ?: 0.0
    p?.let {
        it.hype = min(100.0, it.hype + hype)
        revealed(it, name, hype)
    }
    val reaction = if (crashed) 0.1 else reactionFor(appeal)
    g.fans += fans
    stats.tradeShows += 1
    addLog(
        "$name: ${Format.count(visitors.toDouble())} visitors at your ${booth.title.lowercase()}" +
            (p?.let { if (best) ". ${it.title} won Best in Show." else ". Hype for ${it.title} is up." } ?: ".") +
            " ${Format.count(fans)} new fans.",
        if (crashed) LogTone.BAD else LogTone.GOOD, Lane.GAMES,
    )
    enqueue(
        ShowResults(
            ShowReport(
                name, booth, p?.title, visitors, hype, fans, best, press,
                wishlists = (p?.wishlists ?: 0.0) - listsBefore, reaction = reaction,
                quotes = p?.let { quotesFor(it.title, reaction) }.orEmpty(), chat = if (p != null) chatFor(reaction) else emptyList(),
            ),
        ),
    )
}

internal fun GameState.describeShow(): Description {
    val p = games?.project
    val name = gamesShowIn(calendarYear).replaceFirstChar { it.uppercase() }
    val about = when {
        p == null -> "You've nothing in development, so a booth would show your back catalogue: fans come, but hype has nowhere to go."
        p.completion < 0.25 -> "${p.title} is only ${Format.percent(p.completion)} done. A demo this early could fall flat on a big stage."
        else -> "Show off ${p.title} and build hype before launch. The crowd judges what it sees."
    }
    val hall = when {
        Booth.KEYNOTE in boothsOpen() -> " As one of the biggest studios around, you're offered a keynote."
        Booth.LARGE in boothsOpen() -> ""
        else -> " The main hall is kept for studios with ${Format.count(MAIN_HALL_FANS)} fans."
    }
    return Description(
        "$name opens this week",
        "The biggest games show of the year. $about$hall",
        boothsOpen().map { choice(it.title, "${money(boothCost(it))}. ${it.blurb}") } + choice("Skip it", "Save the money."),
        Lane.GAMES,
    )
}

/** The report after the show. The numbers themselves are shown as figures beside it. */
internal fun describeShowResults(r: ShowReport): Description {
    return Description(
        when {
            r.bestInShow -> "Best in Show at ${r.show}!"
            r.booth == null && r.game != null -> when {
                r.reaction >= 0.8 -> "${r.game} stole the show at ${r.show}"
                r.reaction >= 0.55 -> "${r.game} went down a storm at ${r.show}"
                r.reaction >= 0.3 -> "A warm welcome for ${r.game} at ${r.show}"
                else -> "A cold reception for ${r.game} at ${r.show}"
            }
            else -> "${r.show}: ${Format.count(r.visitors.toDouble())} visitors"
        },
        r.press.joinToString(" "),
        listOf(choice("Back to work", if (r.game != null) "Hype fades a little each week, so ship while it's hot." else "Next year, bring something new.")),
        Lane.GAMES,
    )
}
