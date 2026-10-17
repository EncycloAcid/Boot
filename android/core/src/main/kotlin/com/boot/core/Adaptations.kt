package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Film and TV deals.
 *
 * A hit series gets offers from film and TV studios, and you can shop the rights yourself. You can
 * license it (they pay you and make it how they like) or co-produce (you put money in, get a say,
 * and share the takings). A year or two later it comes out:
 * - A good adaptation brings new fans, sends people back to the games, and gives the next game in
 *   the series a bigger launch.
 * - A bad one costs you the fans' trust, and the next game carries the stink.
 */

/** What gets made. */
@Serializable
enum class ScreenKind(val title: String, val lower: String, val weeks: Int, val value: Double, val since: Int) {
    FILM("Film", "film", 104, 1.0, 1993),
    SERIES("TV series", "TV series", 78, 0.8, 1990),
    ANIMATED("Animated series", "animated series", 65, 0.5, 1970),
}

/** How you take part. */
@Serializable
enum class ScreenDeal(val title: String) {
    LICENSE("Licensed"),
    COPRODUCE("Co-produced"),
}

/** A film or show of one of your series, in production or out. */
@Serializable
data class Adaptation(
    val id: Int,
    val rootId: Int,
    val series: String,
    val kind: ScreenKind,
    val studio: String,
    val deal: ScreenDeal,
    /** What the studio paid you (licensed) or what you put in (co-produced). */
    val money: Double,
    val startWeek: Int,
    val premiereWeek: Int,
    /** Critics' score out of 100, once it's out. */
    var critics: Int? = null,
    /** What it brought in for you when it came out. */
    var takings: Double = 0.0,
) {
    val out: Boolean get() = critics != null
    val hit: Boolean get() = (critics ?: 0) >= GOOD_ADAPTATION
    val flop: Boolean get() = critics != null && critics!! < BAD_ADAPTATION
    val verdict: String get() = when {
        critics == null -> "In production"
        hit -> "A hit"
        flop -> "A flop"
        else -> "Mixed"
    }
}

/** A studio wants to adapt one of your series. */
@Serializable @SerialName("screenOffer")
data class ScreenOffer(val rootId: Int, val studio: String, val kind: ScreenKind, val fee: Double, val stake: Double, val pitched: Boolean = false) : EventKind

/** At or above this, critics loved it. */
const val GOOD_ADAPTATION = 70

/** Below this, it's a flop. */
const val BAD_ADAPTATION = 45

/** Launch lift for the next game in a series after a hit adaptation, and the drag after a flop. */
const val SCREEN_HIT_LIFT = 1.3
const val SCREEN_FLOP_LIFT = 0.82

/** Who makes films and shows in [year]. */
fun screenStudios(kind: ScreenKind, year: Int): List<String> = when (kind) {
    ScreenKind.FILM -> listOfNotNull(
        "Universal", "Warner Bros.", "Paramount", "Sony Pictures", "20th Century", "Disney",
        if (year >= 2000) "Lionsgate" else "TriStar",
        if (year >= 2005) "Legendary" else null,
    )
    ScreenKind.SERIES -> listOfNotNull(
        "HBO", if (year < 2010) "NBC" else null, if (year < 2008) "Fox" else null,
        if (year >= 2013) "Netflix" else null, if (year >= 2015) "Amazon" else null,
        if (year >= 2019) "Apple TV+" else null, if (year >= 2019) "Disney+" else null,
    )
    ScreenKind.ANIMATED -> listOfNotNull(
        if (year >= 1992) "Cartoon Network" else "Hanna-Barbera", if (year < 2005) "Fox Kids" else null,
        if (year >= 1990) "Nickelodeon" else "DIC", if (year >= 2013) "Netflix" else null,
        if (year >= 2017) "Crunchyroll" else null,
    )
}

/** The kinds of adaptation that exist in [year]. */
fun screenKinds(year: Int): List<ScreenKind> = ScreenKind.entries.filter { year >= it.since }

/** Your series a studio would adapt: your own games that averaged 7.5 or better, with a hit among them. */
fun GameState.adaptableSeries(): List<Franchise> {
    val g = games ?: return emptyList()
    // One pass with a lookup table to sort every game into its series: a long run has hundreds of games.
    val byId = g.releases.associateBy { it.id }
    fun root(r: GameRelease): Int {
        var current = r
        repeat(64) { current = current.concept.sequelOf?.let { byId[it] } ?: return current.id }
        return current.id
    }
    val bySeries = g.releases.groupBy { root(it) }
    return g.releases.filter { it.concept.sequelOf == null && it.concept.remasterOf == null && it.developer == null && it.publisher == null }
        .map { Franchise(it, bySeries[it.id].orEmpty().sortedBy { r -> r.id }.ifEmpty { listOf(it) }) }
        .filter { f -> f.averageScore >= 7.5 && f.entries.maxOf { it.score } >= 8 }
        .sortedByDescending { it.lifetime }
}

/** The adaptations of [rootId]'s series, newest first. */
fun GameState.adaptationsOf(rootId: Int): List<Adaptation> =
    games?.adaptations?.filter { it.rootId == rootId }?.sortedByDescending { it.startWeek }.orEmpty()

/** Why [rootId]'s series can't be adapted right now, or null if it can. */
fun GameState.screenBlocker(rootId: Int, series: List<Franchise>? = null): String? {
    val g = games ?: return "You don't make games."
    val f = (series ?: adaptableSeries()).firstOrNull { it.root.id == rootId }
        ?: return "Studios want a series that averages 7.5 or better, with an 8 among them."
    val last = adaptationsOf(rootId).firstOrNull()
    if (last != null && !last.out) return "${f.name} is already being made into a ${last.kind.lower}."
    if (last != null && week - last.premiereWeek < 52) return "Give the last ${last.kind.lower} a year first."
    if (events.any { it.kind is ScreenOffer }) return "An offer is already on the table."
    if (g.adaptations.count { !it.out } >= 2) return "Two are already in production."
    return null
}

/** How much a studio would pay to license [f], before the kind of adaptation. */
internal fun GameState.screenValue(f: Franchise): Double {
    val fame = (f.averageScore - 7) / 2 + 0.6 * f.entries.size.coerceAtMost(4) / 4
    val base = max(scaled(400_000.0), f.lifetime * 0.05)
    return min(base * (0.6 + fame), scaled(30_000_000.0))
}

/** The offer a studio makes for [f]: a licence fee, and the stake it'd want from you to co-produce. */
internal fun GameState.screenTerms(f: Franchise, kind: ScreenKind): Pair<Double, Double> {
    val fee = (screenValue(f) * kind.value / 1_000).roundToInt() * 1_000.0
    val stake = (fee * 1.6 / 1_000).roundToInt() * 1_000.0
    return fee to stake
}

/** Chances a studio bites when you shop [rootId]'s rights. */
fun GameState.screenPitchOdds(rootId: Int): Double {
    val f = adaptableSeries().firstOrNull { it.root.id == rootId } ?: return 0.0
    return ((f.averageScore - 7) / 3 + 0.12 * f.entries.size + (games?.fans ?: 0.0) / 4_000_000).coerceIn(0.1, 0.85)
}

/** Why [rootId]'s rights can't be shopped around right now, or null if they can. */
fun GameState.screenPitchBlocker(rootId: Int): String? {
    screenBlocker(rootId)?.let { return it }
    val last = games?.screenPitched?.get(rootId) ?: return null
    return if (week - last < 26) "You shopped it around ${Format.weeks(week - last)} ago. Give it time." else null
}

/** Shop [rootId]'s film and TV rights. A studio says yes with an offer, or nobody bites. */
fun GameState.pitchScreen(rootId: Int) {
    requireLive()
    screenPitchBlocker(rootId)?.let { throw notAvailable(it) }
    val g = games!!
    g.screenPitched[rootId] = week
    val f = adaptableSeries().first { it.root.id == rootId }
    val dice = flavour("screen pitch $rootId")
    if (dice.chance(screenPitchOdds(rootId))) {
        val kind = dice.pick(screenKinds(calendarYear))
        val studio = dice.pick(screenStudios(kind, calendarYear))
        val (fee, stake) = screenTerms(f, kind)
        enqueue(ScreenOffer(rootId, studio, kind, fee * 0.9, stake, pitched = true))
    } else {
        addLog("Nobody in Hollywood bit on ${f.name}. Another hit might change their minds.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** How likely a [deal] is to turn out well, as an average critics' score. */
fun screenOutlook(f: Franchise, deal: ScreenDeal): Int {
    val base = if (deal == ScreenDeal.COPRODUCE) 62.0 else 48.0
    return (base + (f.averageScore - 7.5) * 6).roundToInt().coerceIn(20, 85)
}

/** Each week: studios come calling about your best series, and finished ones come out. */
internal fun GameState.screenWeek(g: GamesStudio) {
    for (a in g.adaptations) if (!a.out && week >= a.premiereWeek) premiere(g, a)
    if (events.any { it.kind is ScreenOffer }) return
    // Most weeks nobody calls; only then is it worth looking through the catalogue.
    val dice = flavour("screen offer")
    if (!dice.chance(0.05)) return
    val all = adaptableSeries()
    val open = all.filter { screenBlocker(it.root.id, all) == null }
    if (open.isEmpty()) return
    for (f in open) {
        // A bigger, better series gets more calls (these odds are on top of the one in twenty above).
        val odds = (0.004 + 0.002 * f.entries.size + (f.averageScore - 7.5) * 0.004) * 20
        if (!dice.chance(odds)) continue
        val kind = dice.pick(screenKinds(calendarYear))
        val studio = dice.pick(screenStudios(kind, calendarYear))
        val (fee, stake) = screenTerms(f, kind)
        enqueue(ScreenOffer(f.root.id, studio, kind, fee, stake))
        return
    }
}

private fun GameState.premiere(g: GamesStudio, a: Adaptation) {
    val f = Franchise(g.release(a.rootId) ?: return, g.series(g.release(a.rootId)!!))
    val dice = SeededRNG.fromSeed(seed * 13 + a.id * 7_919L + a.startWeek)
    val critics = dice.normal(screenOutlook(f, a.deal).toDouble(), 17.0).roundToInt().coerceIn(5, 98)
    a.critics = critics
    stats.adaptations += 1
    val games = g.series(f.root)
    val outlet = if (calendarYear >= 1999) "Rotten Tomatoes" else "Variety"
    val made = "${a.studio}'s ${a.kind.lower} of ${f.name}"
    when {
        a.hit -> {
            stats.adaptationHits += 1
            val newFans = max(20_000.0, f.copies * 0.15) * (critics / 70.0)
            g.fans += newFans
            g.fanTrust = min(1.2, g.fanTrust + 0.05)
            g.screenLift[f.root.id] = SCREEN_HIT_LIFT
            // People who watched it go and buy the games.
            for (r in games) r.weeklySales += max(r.weeklySales * 1.5, scaled(15_000.0) * r.score / 8)
            a.takings = when (a.deal) {
                ScreenDeal.COPRODUCE -> a.money * (1.2 + (critics - 60) / 20.0)
                ScreenDeal.LICENSE -> a.money * 0.4
            }
            addLog("$made is a hit: $critics% with critics. ${Format.count(newFans)} new fans, and the games are selling again.", LogTone.GOOD, Lane.GAMES)
            publish(NewsDesk.GAMES, outlet, "The ${f.name} ${a.kind.lower} is a hit", "$critics% with critics. Finally, a game adaptation that works.", LogTone.GOOD, Lane.GAMES, tag = NewsTag.REVIEW, cover = NewsCover(f.latest.title, f.latest.concept.topic, f.latest.concept.genre, f.latest.concept.size))
        }
        a.flop -> {
            g.fanTrust = max(0.3, g.fanTrust - 0.05)
            g.screenLift[f.root.id] = SCREEN_FLOP_LIFT
            for (r in games) r.weeklySales *= 0.85
            a.takings = if (a.deal == ScreenDeal.COPRODUCE) a.money * 0.3 else 0.0
            addLog("$made flopped: $critics% with critics. Fans are furious, and the next ${f.name} game will feel it.", LogTone.BAD, Lane.GAMES)
            publish(NewsDesk.GAMES, outlet, "The ${f.name} ${a.kind.lower} is another game adaptation disaster", "$critics% with critics. Fans of the games are not happy.", LogTone.BAD, Lane.GAMES, tag = NewsTag.REVIEW, cover = NewsCover(f.latest.title, f.latest.concept.topic, f.latest.concept.genre, f.latest.concept.size))
        }
        else -> {
            g.fans += f.copies * 0.03
            for (r in games) r.weeklySales += r.weeklySales * 0.3
            a.takings = if (a.deal == ScreenDeal.COPRODUCE) a.money * 0.9 else 0.0
            addLog("$made is out: $critics% with critics. Fine, not memorable. A few new fans found the games.", LogTone.NEUTRAL, Lane.GAMES)
            publish(NewsDesk.GAMES, outlet, "The ${f.name} ${a.kind.lower} is watchable", "$critics% with critics.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.REVIEW)
        }
    }
    if (a.takings > 0) {
        cash += a.takings
        recordRevenue(a.takings, Lane.GAMES)
    }
}

internal fun GameState.describeScreen(k: ScreenOffer): Description? {
    val g = games ?: return null
    val root = g.release(k.rootId) ?: return null
    val f = Franchise(root, g.series(root))
    val made = if (k.kind == ScreenKind.FILM) "a ${f.name} film" else "a ${f.name} ${k.kind.lower}"
    return Description(
        "${k.studio} wants to make $made",
        (if (k.pitched) "${k.studio} liked your pitch. " else "${k.studio} has been watching ${f.name} sell. ") +
            "Out in about ${Format.weeks(k.kind.weeks)}. Game adaptations have a bad name: a good one brings new fans and sells the games; a bad one hurts the series.",
        listOf(
            choice("License it", "${money(k.fee)} now. They make it their way: about ${screenOutlook(f, ScreenDeal.LICENSE)}% with critics, on average."),
            choice("Co-produce", "Put in ${money(k.stake)}. You get a say, about ${screenOutlook(f, ScreenDeal.COPRODUCE)}% with critics on average, and a share of what it makes."),
            choice("Turn it down", "Keep the series in games."),
        ),
        Lane.GAMES,
    )
}

/** Why [k]'s co-production can't be signed, or null if it can. */
fun GameState.coproduceBlocker(k: ScreenOffer): String? = if (cash < k.stake) "You need ${money(k.stake)} to co-produce." else null

internal fun GameState.resolveScreen(k: ScreenOffer, choice: Int) {
    val g = games ?: return
    val root = g.release(k.rootId) ?: return
    val name = seriesName(root.title)
    var label = describeScreen(k)?.choices?.getOrNull(choice)?.label ?: return
    if (label == "Co-produce" && coproduceBlocker(k) != null) label = "License it"
    val deal = if (label == "Co-produce") ScreenDeal.COPRODUCE else ScreenDeal.LICENSE
    when (label) {
        "License it", "Co-produce" -> {
            if (deal == ScreenDeal.LICENSE) {
                cash += k.fee
                recordRevenue(k.fee, Lane.GAMES)
            } else {
                spend(k.stake, Lane.GAMES)
            }
            val a = Adaptation(
                g.nextId(), k.rootId, name, k.kind, k.studio, deal, if (deal == ScreenDeal.LICENSE) k.fee else k.stake,
                week, week + k.kind.weeks,
            )
            g.adaptations += a
            addLog(
                if (deal == ScreenDeal.LICENSE) "You licensed $name to ${k.studio} for ${money(k.fee)}. The ${k.kind.lower} is out in about ${Format.weeks(k.kind.weeks)}."
                else "You're co-producing the $name ${k.kind.lower} with ${k.studio}: ${money(k.stake)} in. Out in about ${Format.weeks(k.kind.weeks)}.",
                LogTone.MILESTONE, Lane.GAMES,
            )
            publish(NewsDesk.GAMES, "Variety", "${k.studio} is making a $name ${k.kind.lower}", "$companyName's series is heading to the screen.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS)
        }
        else -> addLog("You turned down ${k.studio}'s $name ${k.kind.lower}.", LogTone.NEUTRAL, Lane.GAMES)
    }
}
