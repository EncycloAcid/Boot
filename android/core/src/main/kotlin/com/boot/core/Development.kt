package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Making a game, more hands-on.
 *
 * - In the designer: production values (lean, standard or lavish) trade money and time for
 *   quality, and a hook (an open world, co-op, a roguelike structure, crafting...) gives the
 *   game something to be known for. A hook suits some genres and costs a little in the rest.
 * - At each milestone, a decision: at the vertical slice, lock the vision, add a feature or cut
 *   scope; at alpha, fix bugs now, crunch for a month or stay the course; at beta, open it up,
 *   test it quietly, or ship it as it is.
 * - Week by week, the forecast is kept, so you can see the game getting better (or not).
 * - After launch: port a game to another platform, or put it on sale.
 */

/** How much money goes into making it look and feel expensive. */
@Serializable
enum class Production(val title: String, val blurb: String, val cost: Double, val work: Double, val score: Double) {
    LEAN("Lean", "A smaller budget and a shorter schedule. It shows.", 0.7, 0.85, -0.3),
    STANDARD("Standard", "The usual budget for a game of this size.", 1.0, 1.0, 0.0),
    LAVISH("Lavish", "More money, more time, and every corner polished.", 1.5, 1.15, 0.4),
}

/** The one thing a game is known for. */
@Serializable
enum class Hook(val title: String, val blurb: String, val suits: Set<Genre>, val pro: String) {
    OPEN_WORLD("Open world", "Go anywhere, do anything.", setOf(Genre.ACTION, Genre.ADVENTURE, Genre.RPG), "the open world is a joy to explore"),
    CO_OP("Co-op", "Better with friends.", setOf(Genre.ACTION, Genre.STRATEGY, Genre.CASUAL), "playing with friends is a blast"),
    STORY("Story-driven", "A tale people talk about.", setOf(Genre.ADVENTURE, Genre.RPG), "the story grips from start to finish"),
    ROGUELIKE("Roguelike runs", "Die, learn, go again.", setOf(Genre.ACTION, Genre.STRATEGY), "every run feels different"),
    CRAFTING("Crafting", "Build anything from what you find.", setOf(Genre.SIMULATION, Genre.ADVENTURE), "the crafting is deep and satisfying"),
    COMPETITIVE("Competitive", "Ranked play and a scene to grow.", setOf(Genre.ACTION, Genre.STRATEGY), "the competitive play has real depth"),
    MODS("Mod support", "Players make more of it.", setOf(Genre.SIMULATION, Genre.STRATEGY), "the modding scene is already thriving"),
    COZY("Cozy", "A gentle game to unwind with.", setOf(Genre.CASUAL, Genre.SIMULATION), "it's the most relaxing game in years"),
    ;

    fun fits(c: GameConcept): Boolean = c.genre in suits || c.genre2 in suits

    /** What a hook adds to the score: a well-chosen one lifts it; a forced one drags it down. */
    fun points(c: GameConcept): Double = if (fits(c)) 0.4 else -0.2
}

/** Extra work a hook brings. */
const val HOOK_WORK = 1.08

// Milestones

/** The milestones, after each stage: the vertical slice, alpha and beta. */
enum class Milestone(val title: String) { SLICE("Vertical slice"), ALPHA("Alpha"), BETA("Beta") }

@Serializable @SerialName("milestone")
data class MilestoneReview(val team: Int, val milestone: Int) : EventKind

/** Checks for a milestone each week, once its stage is done. */
internal fun GameState.milestoneWeek(g: GamesStudio) {
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        val due = when {
            p.isReady -> 2
            p.stage >= 2 -> 1
            p.stage >= 1 -> 0
            else -> -1
        }
        for (m in 0..due) {
            if (m in p.milestones) continue
            p.milestones += m
            enqueue(MilestoneReview(team, m))
            break
        }
        // The forecast, kept week by week.
        gameForecast(team)?.let {
            p.forecasts += it.expectedScore
            if (p.forecasts.size > 60) p.forecasts.removeAt(0)
        }
    }
}

/** What a closed beta costs. */
fun GameState.closedBetaCost(p: GameProject): Double = scaled(
    when (p.concept.size) {
        GameSize.SMALL -> 8_000.0
        GameSize.MEDIUM -> 25_000.0
        GameSize.LARGE -> 70_000.0
        GameSize.AAA -> 180_000.0
    },
)

internal fun GameState.describeMilestone(k: MilestoneReview): Description? {
    val p = games?.projectOf(k.team) ?: return null
    val f = gameForecast(k.team)
    val build = f?.let { String.format(java.util.Locale.US, "The build is tracking at about %.1f, with %d bugs open. ", it.expectedScore, p.bugs.toInt()) } ?: ""
    val m = Milestone.entries[k.milestone]
    return when (m) {
        Milestone.SLICE -> Description(
            "${p.title}: the vertical slice",
            "${build}The foundations are in and one level plays the way the whole game will. Now's the time to decide how big it gets.",
            listOf(
                choice("Lock the vision", "Everyone builds the same game: the effort split counts for more."),
                choice("Add a feature", "More game: a better score, and about 12% more work."),
                choice("Cut scope", "Ship sooner: 15% less work, and a slightly smaller game."),
                choice("Keep to the plan", "Build what you planned."),
            ),
            Lane.GAMES,
        )
        Milestone.ALPHA -> Description(
            "${p.title} hits alpha",
            "${build}Everything's in, and most of it works. The last stretch decides how polished it feels.",
            listOf(
                choice("Fix bugs now", "Two-fifths of the bugs gone, for a little more work."),
                choice("Crunch for a month", "A big jump in progress. More bugs, and the team is exhausted."),
                choice("Stay the course", "Keep going as planned."),
            ),
            Lane.GAMES,
        )
        Milestone.BETA -> Description(
            "${p.title} is ready for a beta",
            "${build}It's content complete. Before it ships, players could have a go.",
            listOf(
                choice("Open beta", "Free. Players find a quarter of the bugs, and the buzz builds. Builds sometimes leak."),
                choice("Closed beta", "${money(closedBetaCost(p))}. Testers find two-fifths of the bugs, quietly."),
                choice("Ship as it is", "No beta."),
            ),
            Lane.GAMES,
        )
    }
}

internal fun GameState.resolveMilestone(k: MilestoneReview, choice: Int) {
    val g = games ?: return
    val p = g.projectOf(k.team) ?: return
    val m = Milestone.entries[k.milestone]
    val label = describeMilestone(k)?.choices?.getOrNull(choice)?.label ?: return
    p.choices += "${m.title}: ${label.lowercase()}"
    when (label) {
        "Lock the vision" -> p.matchSum = min(p.matchWork, p.matchSum + p.matchWork * 0.12)
        "Add a feature" -> {
            p.workScale += 0.12
            p.qualityBonus += 0.35
        }
        "Cut scope" -> {
            p.workScale = max(0.6, p.workScale - 0.15)
            p.qualityBonus -= 0.25
        }
        "Fix bugs now" -> {
            p.bugs *= 0.6
            p.workScale += 0.04
        }
        "Crunch for a month" -> {
            p.progress += p.work * 0.08
            p.bugs *= 1.2
            for (e in staff(Lane.GAMES)) e.morale = max(0.0, e.morale - 0.1)
            stats.crunchWeeks += 4
        }
        "Open beta" -> {
            p.bugs *= 0.75
            if (p.reveals > 0) {
                p.hype = min(100.0, p.hype + 6)
                p.wishlists *= 1.1
            }
            if (rng.chance(0.2)) {
                p.hype = max(0.0, p.hype - 3)
                addLog("The ${p.title} beta build leaked. Some of the surprises are spoiled.", LogTone.BAD, Lane.GAMES)
            }
        }
        "Closed beta" -> {
            spend(closedBetaCost(p), Lane.GAMES)
            p.bugs *= 0.6
        }
        else -> Unit
    }
    addLog("${p.title}, ${m.title.lowercase()}: ${label.lowercase()}.", LogTone.NEUTRAL, Lane.GAMES)
}

// After launch

/** A port in progress. */
@Serializable
data class Port(val releaseId: Int, val platform: Platform, val doneWeek: Int)

/** What porting a game costs, and how long it takes. */
fun GameState.portQuote(r: GameRelease): Pair<Double, Int> = when (r.concept.size) {
    GameSize.SMALL -> scaled(20_000.0) to 6
    GameSize.MEDIUM -> scaled(60_000.0) to 8
    GameSize.LARGE -> scaled(150_000.0) to 10
    GameSize.AAA -> scaled(400_000.0) to 12
}

/** Platforms a game could be ported to. */
fun GameState.portTargets(r: GameRelease): List<Platform> =
    Platform.entries.filter {
        availablePlatform(it) && it != Platform.OWN_CONSOLE && it !in r.concept.platforms && it !in r.extraPlatforms &&
            (r.exclusiveTo == null || week >= r.exclusiveUntil || it.maker == r.exclusiveTo)
    }

/** Why a game can't be ported to [platform], or null if it can. */
fun GameState.portBlocker(r: GameRelease, platform: Platform): String? {
    val g = games ?: return "You need a game studio."
    if (platform !in portTargets(r)) return "It's already on ${platform.title}, or it can't go there."
    if (g.ports.any { it.releaseId == r.id }) return "A port of ${r.title} is already under way."
    if (r.developer != null) return "Ports are for games you made."
    val (cost, _) = portQuote(r)
    if (cash < cost) return "A port costs ${money(cost)}."
    return null
}

fun GameState.portGame(releaseId: Int, platform: Platform) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(releaseId) ?: throw invalid("No such game.")
    portBlocker(r, platform)?.let { throw notAvailable(it) }
    val (cost, weeks) = portQuote(r)
    spend(cost, Lane.GAMES)
    g.ports += Port(r.id, platform, week + weeks)
    addLog("${r.title} is being ported to ${platform.title}. It'll be out in ${Format.weeks(weeks)}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Finished ports come out and sell to a new crowd. */
internal fun GameState.portWeek(g: GamesStudio) {
    val done = g.ports.filter { it.doneWeek <= week }
    for (port in done) {
        g.ports.remove(port)
        val r = g.release(port.releaseId) ?: continue
        r.extraPlatforms += port.platform
        val burst = r.concept.size.launchSales * (r.score / 7).pow(3) * 0.3 * world.demand(Lane.GAMES) * pastScale(calendarYear.toDouble())
        r.weeklySales += burst
        stats.ports += 1
        addLog("${r.title} is out on ${port.platform.title}.", LogTone.GOOD, Lane.GAMES)
        publish(NewsDesk.YOU, "IGN", "${r.title} comes to ${port.platform.title}", "$companyName's game is out on a new platform.", LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS, cover = r.newsCover())
    }
}

/** Weeks between sales of the same game. */
const val SALE_GAP = 13

/** Why a game can't go on sale, or null if it can. */
fun GameState.saleBlocker(r: GameRelease): String? = when {
    r.live != null -> "Live games make their money in the store, not on sale."
    week - r.week < 8 -> "It's too new to discount."
    r.weeklySales <= 0 -> "It isn't selling any more."
    week - r.lastSaleWeek < SALE_GAP -> "It was on sale recently. Again from ${GameClock.label(r.lastSaleWeek + SALE_GAP)}."
    else -> null
}

/** Half price for a week or two: a lot more copies, a little more money, and new fans. */
fun GameState.runSale(releaseId: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(releaseId) ?: throw invalid("No such game.")
    saleBlocker(r)?.let { throw notAvailable(it) }
    r.lastSaleWeek = week
    r.weeklySales *= 1.5
    val buyers = r.weeklySales / (r.concept.unitPrice * 0.5)
    g.fans += buyers * 0.05
    stats.sales += 1
    addLog("${r.title} is half price this week. Sales are up.", LogTone.GOOD, Lane.GAMES)
}
