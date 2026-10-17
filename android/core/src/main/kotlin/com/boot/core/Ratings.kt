package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Age ratings and review bombing.
 *
 * Ratings: from 1994 every game gets one, from who it's for. A mature game can go further and ship
 * uncut as Adults Only: it suits the darkest topics, gets talked about, and console makers, phone
 * stores and the big shops won't carry it. The ratings board can also hand a mature game an Adults
 * Only rating you didn't ask for, and you decide what to do.
 *
 * Review bombing: user reviews came with the web. An unpopular choice (heavy DRM, a pushy store, a
 * full exclusive, a buggy launch, a high price for a so-so game) can bring a wave of angry user
 * reviews, and how you answer decides how much it costs.
 */

/** The ratings board's verdict. */
@Serializable
enum class AgeRating(val code: String, val title: String) {
    EVERYONE("E", "Everyone"),
    TEEN("T", "Teen"),
    MATURE("M", "Mature 17+"),
    ADULTS_ONLY("AO", "Adults Only"),
}

/** The year games started getting age ratings. */
const val RATINGS_YEAR = 1994

/** The year user reviews (and review bombs) took off. */
const val USER_REVIEWS_YEAR = 2004

/** The rating a game for [audience] gets. */
fun ratingFor(audience: Audience, uncut: Boolean = false): AgeRating = when {
    uncut -> AgeRating.ADULTS_ONLY
    audience == Audience.YOUNG -> AgeRating.EVERYONE
    audience == Audience.MATURE -> AgeRating.MATURE
    else -> AgeRating.TEEN
}

/** Who carries Adults Only games: computers, the web and your own hardware. Console makers and phone stores don't. */
fun Platform.carriesAdultsOnly(): Boolean = this in setOf(
    Platform.PC, Platform.MAC, Platform.AMIGA, Platform.C64, Platform.VAPOR_DECK, Platform.ROG_ALLY, Platform.STEAM_MACHINE,
    Platform.WEB, Platform.OWN_CONSOLE,
)

/** Shops that won't stock Adults Only games: most of them, until games were bought online. */
fun adultsOnlyShelf(year: Int): Double = if (year < 2008) 0.6 else 0.85

/** The rating [team]'s game is heading for, or null before ratings existed. */
fun GameState.projectRating(team: Int): AgeRating? {
    val p = games?.projectOf(team) ?: return null
    if (calendarYear < RATINGS_YEAR) return null
    return ratingFor(p.concept.audience, p.uncut)
}

/** Why [team]'s game can't go uncut (or back), or null if it can. */
fun GameState.uncutBlocker(team: Int, uncut: Boolean): String? {
    val p = games?.projectOf(team) ?: return "No game in development."
    if (calendarYear < RATINGS_YEAR) return "There's no ratings board yet."
    if (p.concept.audience != Audience.MATURE) return "Only a game for mature players can go further."
    if (p.uncut == uncut) return if (uncut) "It's already uncut." else "It's already rated M."
    if (uncut && p.concept.platforms.none { it.carriesAdultsOnly() }) return "None of its platforms carry Adults Only games."
    return null
}

/** Ship [team]'s game uncut as Adults Only, or cut it back to an M. */
fun GameState.setUncut(team: Int, uncut: Boolean) {
    requireLive()
    uncutBlocker(team, uncut)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    p.uncut = uncut
    if (uncut && "Went for an Adults Only rating" !in p.choices) {
        p.choices += "Went for an Adults Only rating"
        p.hype = min(100.0, p.hype + 6)
        addLog("${p.title} will ship uncut, rated Adults Only. People are talking; most consoles and shops won't carry it.", LogTone.NEUTRAL, Lane.GAMES)
        publish(NewsDesk.GAMES, "Kotaku", "${p.title} will ship rated Adults Only", "$companyName isn't cutting anything. Console makers are expected to refuse it.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.RUMOUR)
    } else if (uncut) {
        addLog("${p.title} will ship uncut, rated Adults Only.", LogTone.NEUTRAL, Lane.GAMES)
    } else {
        addLog("${p.title} is back to an M rating.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** What going uncut does to the score: the darkest topics are better for it, the rest just gratuitous. */
fun uncutPoints(c: GameConcept): Double = if (c.topic.fit(Audience.MATURE) == Fit.GOOD) 0.3 else -0.2

/** The platforms an Adults Only game actually ships on. */
fun adultsOnlyPlatforms(c: GameConcept): List<Platform> = c.platforms.filter { it.carriesAdultsOnly() }

/** The ratings board looks at a mature game once it's content complete. */
@Serializable @SerialName("ratingDispute")
data class RatingDispute(val team: Int) : EventKind

/** What appealing a rating costs. */
fun GameState.appealCost(): Double = scaled(150_000.0)

/** Each week: the ratings board looks at mature games that are done. */
internal fun GameState.ratingsWeek(g: GamesStudio) {
    if (calendarYear < RATINGS_YEAR) return
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        if (!p.isReady || p.rated || p.uncut || p.concept.audience != Audience.MATURE) continue
        p.rated = true
        val odds = if (p.concept.topic.fit(Audience.MATURE) == Fit.GOOD) 0.25 else 0.08
        if (flavour("rating ${p.title}").chance(odds)) enqueue(RatingDispute(team))
    }
}

internal fun GameState.describeDispute(k: RatingDispute): Description? {
    val p = games?.projectOf(k.team) ?: return null
    val lost = p.concept.platforms.filterNot { it.carriesAdultsOnly() }
    return Description(
        "The ratings board says Adults Only",
        "The board played ${p.title} and won't give it an M. As Adults Only it can't go on " +
            (if (lost.isEmpty()) "consoles, though none of its platforms mind." else lost.joinToString { it.title } + ", and many shops won't stock it.") +
            " You can cut the scenes they object to, appeal, or ship it as it is.",
        listOf(
            choice("Cut it to an M", "It loses a little of its edge. Everywhere carries it."),
            choice("Appeal", "${money(appealCost())} in lawyers. About even odds they back down."),
            choice("Ship it Adults Only", "Nothing cut. It only goes out on PC and the like, and gets talked about."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveDispute(k: RatingDispute, choice: Int) {
    val p = games?.projectOf(k.team) ?: return
    when (describeDispute(k)?.choices?.getOrNull(choice)?.label) {
        "Cut it to an M" -> {
            p.qualityBonus -= 0.25
            p.choices += "Cut for an M rating"
            addLog("You cut ${p.title} to get an M. Some fans can tell.", LogTone.NEUTRAL, Lane.GAMES)
        }
        "Appeal" -> {
            spend(appealCost(), Lane.GAMES)
            if (flavour("appeal ${p.title}").chance(0.5)) {
                addLog("The appeal worked: ${p.title} gets its M, uncut.", LogTone.GOOD, Lane.GAMES)
                publish(NewsDesk.GAMES, "Kotaku", "${p.title} wins its ratings appeal", "Rated M, with nothing cut.", LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS)
            } else {
                p.uncut = adultsOnlyPlatforms(p.concept).isNotEmpty()
                if (!p.uncut) p.qualityBonus -= 0.25
                addLog(
                    if (p.uncut) "The appeal failed. ${p.title} ships Adults Only." else "The appeal failed, and none of its platforms carry Adults Only games. You cut it after all.",
                    LogTone.BAD, Lane.GAMES,
                )
            }
        }
        else -> {
            p.uncut = adultsOnlyPlatforms(p.concept).isNotEmpty()
            if (p.uncut) {
                p.hype = min(100.0, p.hype + 8)
                publish(NewsDesk.GAMES, "Kotaku", "${p.title} rated Adults Only", "$companyName is shipping it anyway. Consoles won't carry it.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS)
            } else {
                p.qualityBonus -= 0.25
                addLog("None of ${p.title}'s platforms carry Adults Only games. You had to cut it.", LogTone.BAD, Lane.GAMES)
            }
        }
    }
}

// Review bombing

/** What players are angry about. */
@Serializable
enum class BombReason(val title: String, val fix: String) {
    DRM("its copy protection", "Take the copy protection out"),
    STORE("its in-game store", "Tone the store down"),
    EXCLUSIVE("being a console exclusive", "Promise a version for everyone"),
    BUGS("how broken it is", "Rush out a patch"),
    PRICE("its price", "Cut the price"),
}

@Serializable @SerialName("reviewBomb")
data class ReviewBomb(val releaseId: Int, val reason: BombReason, val drop: Double) : EventKind

/** How likely [r] is to get bombed this week, and over what. */
fun GameState.bombRisk(r: GameRelease): Pair<Double, BombReason>? {
    if (calendarYear < USER_REVIEWS_YEAR || r.bombed || r.userScore <= 0) return null
    val reasons = buildList {
        if (r.protection.annoys >= 0.5) add(r.protection.annoys * 0.05 to BombReason.DRM)
        if (r.live?.monetization == Monetization.AGGRESSIVE) add(0.04 to BombReason.STORE)
        if (r.exclusiveTo != null && r.exclusiveUntil == Int.MAX_VALUE && r.score >= 7.5) add(0.02 to BombReason.EXCLUSIVE)
        if (r.bugs > r.concept.size.work * 0.08 && !r.patched) add(0.03 to BombReason.BUGS)
        if (r.concept.price == PriceTier.PREMIUM && r.score < 7) add(0.03 to BombReason.PRICE)
    }
    return reasons.maxByOrNull { it.first }?.let { worst -> min(0.2, reasons.sumOf { it.first }) to worst.second }
}

/** Each week: angry players pile on in the first few months. */
internal fun GameState.bombWeek(g: GamesStudio) {
    if (events.any { it.kind is ReviewBomb }) return
    for (r in g.releases) {
        if (week - r.week !in 1..12) continue
        val (odds, reason) = bombRisk(r) ?: continue
        if (!flavour("bomb ${r.id}").chance(odds)) continue
        r.bombed = true
        // Since 2019 the biggest store flags a sudden flood of off-topic reviews and leaves them out.
        val drop = (if (calendarYear >= 2019) 1.0 else 1.8).coerceAtMost(r.userScore - 1)
        r.userScore = max(1.0, r.userScore - drop)
        stats.reviewBombs += 1
        enqueue(ReviewBomb(r.id, reason, drop))
        publish(
            NewsDesk.GAMES, "PC Gamer", "${r.title} is being review bombed",
            "Thousands of negative user reviews over ${reason.title}. Its user score is down to ${String.format(java.util.Locale.US, "%.1f", r.userScore)}.",
            LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS,
        )
        return
    }
}

/** What fixing the problem behind a review bomb costs. */
fun GameState.bombFixCost(r: GameRelease, reason: BombReason): Double = when (reason) {
    BombReason.BUGS -> scaled(120_000.0) * (r.concept.size.launchSales / GameSize.MEDIUM.launchSales).coerceIn(0.5, 4.0)
    BombReason.EXCLUSIVE -> scaled(250_000.0)
    else -> 0.0
}

internal fun GameState.describeBomb(k: ReviewBomb): Description? {
    val r = games?.release(k.releaseId) ?: return null
    val score = String.format(java.util.Locale.US, "%.1f", r.userScore)
    val fixCost = bombFixCost(r, k.reason)
    val fixDetail = when (k.reason) {
        BombReason.DRM -> "Patch out ${r.protection.title.lowercase()}. Pirates get in; players come back."
        BombReason.STORE -> "Make the store fair. Less money from it; players come back."
        BombReason.EXCLUSIVE -> "${money(fixCost)} to buy back the rights for a later version. The exclusive money stays spent."
        BombReason.BUGS -> "${money(fixCost)} for a patch now. Players come back."
        BombReason.PRICE -> "A cheaper price from now on. Each copy earns less; more people buy."
    }
    return Description(
        "${r.title} is being review bombed",
        "Angry players are flooding ${r.title}'s user reviews over ${k.reason.title}. Its user score fell to $score." +
            if (calendarYear >= 2019) " The stores flag some of it as off-topic." else "",
        listOf(
            choice(k.reason.fix, fixDetail),
            choice("Apologise", "A post saying you've heard them. It takes some of the heat out."),
            choice("Ride it out", "It blows over eventually. Sales feel it until then."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveBomb(k: ReviewBomb, choice: Int) {
    val g = games ?: return
    val r = g.release(k.releaseId) ?: return
    when (choice) {
        0 -> {
            val cost = bombFixCost(r, k.reason)
            if (cost > 0) spend(cost, Lane.GAMES)
            when (k.reason) {
                BombReason.DRM -> {
                    r.protection = Protection.NONE
                    r.weeklySales *= 0.92
                }
                BombReason.STORE -> r.live?.monetization = Monetization.FAIR
                BombReason.EXCLUSIVE -> {
                    val held = r.concept.platforms.filter { it.maker != r.exclusiveTo }
                    r.exclusiveUntil = week + 26
                    for (pl in held) g.ports += Port(r.id, pl, week + 26)
                }
                BombReason.BUGS -> {
                    r.bugs *= 0.3
                    r.patched = true
                }
                BombReason.PRICE -> r.weeklySales *= 1.1
            }
            r.userScore = min(10.0, r.userScore + k.drop * 0.9)
            g.fanTrust = min(1.2, g.fanTrust + 0.03)
            addLog("You listened: ${k.reason.fix.lowercase()} for ${r.title}. The user score is climbing back.", LogTone.GOOD, Lane.GAMES)
            publish(NewsDesk.GAMES, "Kotaku", "$companyName listens to ${r.title} players", "${k.reason.fix}. The review bomb is over.", LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS)
        }
        1 -> {
            r.userScore = min(10.0, r.userScore + k.drop * 0.3)
            r.weeklySales *= 0.95
            addLog("You apologised to ${r.title}'s players. Some of them took it well.", LogTone.NEUTRAL, Lane.GAMES)
        }
        else -> {
            r.weeklySales *= 0.85
            g.fanTrust = max(0.3, g.fanTrust - 0.04)
            reputation.scar(ScarKind.PREDATORY_MONETIZATION, 0.01, week, "Ignored a review bomb")
            addLog("You rode out the review bomb on ${r.title}. Sales took a hit.", LogTone.BAD, Lane.GAMES)
        }
    }
}
