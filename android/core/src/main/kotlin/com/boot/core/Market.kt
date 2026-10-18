package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/*
 * The games market: rival studios ship their own games, everything on sale competes on a weekly
 * sales chart, and once a year the industry hands out Game of the Year.
 */

/** A game a rival studio shipped. */
@Serializable
data class RivalGame(
    val id: Int,
    val title: String,
    val studio: String,
    val topic: Topic,
    val genre: Genre,
    val size: GameSize,
    val score: Double,
    val week: Int,
    var weeklySales: Double,
    var lifetime: Double = 0.0,
    /** The real series it belongs to, if any. */
    val franchise: String? = null,
    /** You paid for it: one of your studios made it with your money. */
    val funded: Boolean = false,
) {
    /** The kind of release everyone stops to play. */
    val mega: Boolean get() = size == GameSize.AAA && score >= 9
}

/** One line of the weekly sales chart. Your games carry their release id. */
@Serializable
data class ChartEntry(
    val title: String,
    val studio: String,
    val weeklySales: Double,
    val score: Double,
    val releaseId: Int? = null,
    /** Made by a studio you own. */
    val owned: Boolean = false,
    /** Cover cues — defaults keep old saves / award lists decoding. */
    val topic: Topic = Topic.CITY,
    val genre: Genre = Genre.ACTION,
    val platform: Platform = Platform.PC,
) {
    val mine: Boolean get() = releaseId != null

    /** Enough of a concept to paint box art in chart rows. */
    fun coverConcept(): GameConcept =
        GameConcept(title, topic, genre, platforms = listOf(platform), size = GameSize.MEDIUM)
}

@Serializable
data class Award(
    val year: Int,
    val title: String,
    val studio: String,
    val score: Double,
    val mine: Boolean,
    /** The prize, or empty for Game of the Year. */
    val prize: String = "",
    /** The show that gave it. */
    val show: String = "",
)

@Serializable
class GameMarket {
    val games = mutableListOf<RivalGame>()

    /** Announced releases, soonest first. */
    val upcoming = mutableListOf<Announcement>()

    /** When each real franchise next releases, and how many entries it has had this run. */
    val franchiseNext = mutableMapOf<String, Int>()
    val franchiseEntries = mutableMapOf<String, Int>()
    var chart: List<ChartEntry> = emptyList()
    val awards = mutableListOf<Award>()
    internal var nextId = 1

    /** Game of the Year wins. */
    val wins: Int get() = awards.count { it.mine && it.prize.isEmpty() }
}


/** Week of the year the awards are held. */
const val AWARDS_WEEK = 50

internal fun salesDecay(age: Int, score: Double): Double =
    if (age < 6) (0.75 + 0.02 * (score - 7)).clamped(0.68, 0.84) else (0.95 + 0.005 * (score - 7)).clamped(0.92, 0.97)

/** Rivals release, sales decay, the chart is drawn up, and in December the awards come round. */
internal fun GameState.marketWeek() {
    val g = games ?: return
    val m = g.market
    val rival = world.rival(Lane.GAMES)?.strength ?: 0.5
    franchiseWeek(m)
    if (rng.chance(0.04 + 0.05 * rival)) releaseRivalGame(g)

    for (r in m.games) {
        if (r.weeklySales <= 0) continue
        r.lifetime += r.weeklySales
        r.weeklySales *= salesDecay(week - r.week, r.score)
        if (r.weeklySales < 300) r.weeklySales = 0.0
    }
    m.games.removeAll { it.weeklySales <= 0 && week - it.week > 104 }
    while (m.games.size > 60) m.games.removeAt(0)

    val entries = m.games.filter { it.weeklySales > 0 }.map {
        ChartEntry(it.title, it.studio, it.weeklySales, it.score, owned = g.owns(it.studio), topic = it.topic, genre = it.genre)
    } + g.releases.filter { it.weeklySales > 0 }.map {
        ChartEntry(
            it.title, companyName, it.weeklySales, it.score, it.id,
            topic = it.concept.topic, genre = it.concept.genre,
            platform = it.concept.platforms.firstOrNull() ?: Platform.PC,
        )
    } + g.published.filter { it.weeklySales > 0 }.map {
        ChartEntry(
            it.title, "${it.developer} · $companyName", it.weeklySales, it.score, it.id,
            topic = it.concept.topic, genre = it.concept.genre,
            platform = it.concept.platforms.firstOrNull() ?: Platform.PC,
        )
    }
    val sorted = entries.sortedByDescending { it.weeklySales }
    m.chart = sorted.take(10)
    for (r in g.releases + g.published) {
        val rank = sorted.indexOfFirst { it.releaseId == r.id } + 1
        r.chartRank = rank
        if (rank > 0) {
            val wasTop = r.bestRank == 1
            r.bestRank = if (r.bestRank == 0) rank else minOf(r.bestRank, rank)
            if (rank == 1 && !wasTop) {
                stats.chartToppers += 1
                addLog("${r.title} is the best-selling game in the world this week.", LogTone.MILESTONE, Lane.GAMES)
                feedOnChart(r.title)
                publish(
                    NewsDesk.GAMES, techOutlet(Lane.GAMES, rng),
                    "${r.title} tops the charts",
                    "$companyName's ${r.concept.size.lower} ${r.concept.topic.title.lowercase()} ${r.concept.genreLower} is the week's best-seller.",
                    LogTone.GOOD, Lane.GAMES, lead = true, tag = NewsTag.CHARTS, cover = r.newsCover(),
                )
            }
        }
    }

    if (GameClock.weekOfYear(week) == AWARDS_WEEK) holdAwards(g)
}

private fun GameState.releaseRivalGame(g: GamesStudio) {
    val m = g.market
    // The big studios release most of what's out; indies turn up now and then with something small.
    val active = Studios.filter { studioActive(it.name, calendarYear) }
    val indies = active.filter { it.indie }
    val s = if (rng.chance(0.75) || indies.isEmpty()) rng.pick(active.filter { !it.indie }) else rng.pick(indies)
    val studio = studioName(s.name)
    val quality = s.quality
    val squeezed = ownedQualityPenalty(s.name)
    val year = GameClock.year(week)
    val size = when {
        s.indie -> if (rng.chance(0.75)) GameSize.SMALL else GameSize.MEDIUM
        year <= 2 -> if (rng.chance(0.7)) GameSize.SMALL else GameSize.MEDIUM
        year <= 5 -> rng.pick(listOf(GameSize.SMALL, GameSize.MEDIUM, GameSize.MEDIUM, GameSize.LARGE, GameSize.LARGE))
        else -> rng.pick(listOf(GameSize.MEDIUM, GameSize.LARGE, GameSize.LARGE, GameSize.AAA, GameSize.AAA))
    }.let { if (isDecades) minOf(it, biggestRivalGame(calendarYear)) else it }
    val genre = rng.pick(Genre.entries)
    val topic = rng.pick(Topic.entries.filter { it.fit(genre) != Fit.BAD })
    // Licensing your engine helps them too.
    val licensed = g.engines.filter { it.licensed }.maxOfOrNull { it.level } ?: 0
    val score = (rng.normal(6.2 + 1.2 * quality, 1.2) + licensed * 0.02 - squeezed).clamped(2.5, 9.7)
    val sales = size.launchSales * (score / 7).pow(3) * rng.range(0.7, 1.3) * world.demand(Lane.GAMES)
    val title = gameTitle(topic, genre, rng.int(0, 1_000_000).toLong(), m.games.map { it.title }.toSet(), calendarYear)
    val game = RivalGame(m.nextId++, title, studio, topic, genre, size, score, week, sales)
    m.games += game
    // Mid-score and chart-weight releases make the Games desk, not only mega-hits and AAA flops.
    if (score >= 7.0 || game.mega || (size >= GameSize.LARGE && score < 5.5) || (size == GameSize.AAA && score < 6.5) ||
        (size >= GameSize.MEDIUM && score in 5.5..6.9 && rng.chance(0.35))
    ) {
        val sc = String.format(java.util.Locale.US, "%.1f", score)
        publish(
            NewsDesk.GAMES, techOutlet(Lane.GAMES, rng),
            flavour("$title rival headline").pick(
                when {
                    game.mega -> listOf("$title is here, and everyone's playing it", "$title arrives to record crowds", "The world stops for $title")
                    score >= 9 -> listOf("$title review: an instant classic", "$title review: $studio at its very best", "$title is the year's one to beat")
                    score >= 8 -> listOf("$title review: $studio does it again", "$title review: a must-play", "Critics love $studio's $title")
                    score >= 7 -> listOf("$title review: critics warm to $studio's latest", "$title review: good, not great", "$studio's $title lands well")
                    score >= 5.5 -> listOf("$title review: mixed notices for $studio", "$title review: not quite there", "Critics split on $studio's $title")
                    else -> listOf("$studio's $title is a costly misfire", "$title review: avoid", "$title stumbles out of the gate")
                },
            ),
            "${Format.a(size.lower).replaceFirstChar { it.uppercase() }} ${topic.title.lowercase()} ${genre.lower} game by $studio. $sc/10.",
            lane = Lane.GAMES, lead = game.mega, tag = NewsTag.REVIEW, cover = game.newsCover(),
            quote = flavour(title).pick(
                when {
                    score >= 9 -> listOf("An instant classic.", "Unmissable.", "One for the ages.")
                    score >= 8 -> listOf("Brilliant.", "A triumph.", "Easy to recommend.")
                    score >= 7 -> listOf("Well worth your time.", "Solid and confident.", "Flawed, but fun.")
                    score >= 5.5 -> listOf("A mixed bag.", "Uneven.", "So close.")
                    else -> listOf("A mess.", "Avoid.", "Hard to love.")
                },
            ),
            quoteBy = techOutlet(Lane.GAMES, flavour("$title by")),
        )
    }
    if (score >= 8 || size >= GameSize.LARGE || g.owns(studio)) {
        val sc = String.format(java.util.Locale.US, "%.1f", score)
        val who = if (g.owns(studio)) "Your studio $studio" else studio
        addLog("$who released $title, ${Format.a(topic.title.lowercase())} ${genre.lower} game: $sc/10.", if (g.owns(studio) && score >= 8) LogTone.GOOD else LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** What launching next to someone else's release costs you. */
data class Clash(val game: RivalGame, val penalty: Double) {
    val mega: Boolean get() = game.mega
}

/**
 * A strong rival game in the same genre out in the last six weeks takes buyers from yours (-15%).
 * A mega release (an AAA 9+) in the last three weeks takes everyone's attention, whatever the genre (-20%).
 */
fun GamesStudio.clashFor(concept: GameConcept, week: Int): Clash? {
    // Your own studios' games don't compete with yours.
    val recent = market.games.filter { week - it.week in 0..6 && !owns(it.studio) }
    recent.filter { it.mega && week - it.week <= 3 }.maxByOrNull { it.weeklySales }?.let { return Clash(it, 0.8) }
    return recent
        // A small indie hit doesn't take buyers from a big release, and the other way round matters less too.
        .filter { it.score >= 7.5 && (it.genre == concept.genre || it.genre == concept.genre2) && it.size.ordinal >= concept.size.ordinal - 1 }
        .maxByOrNull { it.score }
        ?.let { Clash(it, 0.85) }
}

/** This year's best games, rivals' and yours. */
fun GameState.awardNominees(): List<ChartEntry> {
    val g = games ?: return emptyList()
    val year = GameClock.year(week)
    val rivals = g.market.games.filter { GameClock.year(it.week) == year }.map {
        ChartEntry(it.title, it.studio, it.weeklySales, it.score, topic = it.topic, genre = it.genre)
    }
    val mine = g.releases.filter { GameClock.year(it.week) == year && it.publisher == null && !it.isCollection }
        .map {
            ChartEntry(
                it.title, companyName, it.weeklySales, it.score, it.id,
                topic = it.concept.topic, genre = it.concept.genre,
                platform = it.concept.platforms.firstOrNull() ?: Platform.PC,
            )
        }
    return (rivals + mine).sortedByDescending { it.score }.take(4)
}

internal fun GameState.holdAwards(g: GamesStudio) {
    val show = yearEndShow(calendarYear)
    val indie = listOfNotNull(awardPrize(YEAR_END_INDIE, show))
    val nominees = awardNominees()
    if (nominees.isEmpty()) {
        watchCeremony(show, indie)
        return
    }
    if (nominees.any { it.mine }) {
        // Game of the Year is read out after you decide whether to go.
        pendingCategories = indie
        enqueue(EventKind.GameAwards)
    } else {
        val goty = announceWinner(g, nominees, attended = false)
        watchCeremony(show, indie + goty)
    }
}

/** Better games are likelier to win; showing up helps a little. */
internal fun GameState.announceWinner(g: GamesStudio, nominees: List<ChartEntry>, attended: Boolean): Category {
    val weights = nominees.map { exp((it.score - 8) * 3) * (if (it.mine && attended) 1.25 else 1.0) }
    var roll = rng.range(0.0, weights.sum())
    var winner = nominees.first()
    for ((i, w) in weights.withIndex()) {
        roll -= w
        if (roll <= 0) {
            winner = nominees[i]
            break
        }
    }
    val year = GameClock.year(week)
    val yearName = if (isDecades) "$calendarYear" else "year $year"
    val show = yearEndShow(calendarYear)
    g.market.awards += Award(year, winner.title, winner.studio, winner.score, winner.mine, show = show)
    if (winner.mine) {
        stats.awards += 1
        g.fans += max(2_000.0, g.fans * 0.25)
        g.fanTrust = minOf(1.2, g.fanTrust + 0.05)
        reputation.adjust(5.0)
        g.release(winner.releaseId ?: -1)?.let { r -> r.weeklySales = max(r.weeklySales * 1.6, r.lifetimeSales / r.netShare * 0.05) }
        g.project?.let { it.hype = minOf(100.0, it.hype + 15) }
        addLog("${winner.title} wins Game of the Year at $show! Fans flock to you and sales jump.", LogTone.MILESTONE, Lane.GAMES)
        publish(
            NewsDesk.YOU, "IGN", "${winner.title} is Game of the Year", "$companyName takes the industry's biggest prize for $yearName at $show, ahead of ${nominees.filter { it !== winner }.joinToString(", ") { it.title }}.",
            LogTone.GOOD, Lane.GAMES, lead = true, tag = NewsTag.AWARDS,
            quote = flavour("goty").pick(listOf("I don't know what to say. Thank you to everyone who played it.", "To the team: we did it.", "Not long ago this was a dream in a cramped apartment.")),
            quoteBy = founder.name, cover = NewsCover(winner.title, winner.topic, winner.genre),
        )
    } else {
        addLog("Game of the Year for $yearName goes to ${winner.title} by ${winner.studio}, at $show.", LogTone.NEUTRAL, Lane.GAMES)
        publish(
            NewsDesk.GAMES, "IGN", "${winner.title} is Game of the Year", "${winner.studio} takes the prize for $yearName at $show.", lane = Lane.GAMES, lead = true,
            tag = NewsTag.AWARDS, cover = NewsCover(winner.title, winner.topic, winner.genre),
        )
    }
    feedOnAward(winner.title, "Game of the Year", show, nominees.any { it.mine }, winner.mine)
    val size = { e: ChartEntry -> g.release(e.releaseId ?: -1)?.concept?.size ?: g.market.games.firstOrNull { it.title == e.title }?.size ?: GameSize.LARGE }
    return Category(
        "Game of the Year",
        nominees.map { Nominee(it.title, it.studio, it.score, it.mine, it.topic, it.genre, size(it)) },
        nominees.indexOf(winner),
    )
}
