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
)

/** One line of the weekly sales chart. Your games carry their release id. */
@Serializable
data class ChartEntry(
    val title: String,
    val studio: String,
    val weeklySales: Double,
    val score: Double,
    val releaseId: Int? = null,
) {
    val mine: Boolean get() = releaseId != null
}

@Serializable
data class Award(val year: Int, val title: String, val studio: String, val score: Double, val mine: Boolean)

@Serializable
class GameMarket {
    val games = mutableListOf<RivalGame>()
    var chart: List<ChartEntry> = emptyList()
    val awards = mutableListOf<Award>()
    internal var nextId = 1

    val wins: Int get() = awards.count { it.mine }
}

/** Rival game studios and how good they usually are. */
internal val RivalStudios = listOf(
    "Electronic Arts" to 0.6,
    "Ubisoft" to 0.55,
    "Activision Blizzard" to 0.65,
    "Rockstar Games" to 0.9,
    "Nintendo" to 0.85,
    "Capcom" to 0.75,
    "FromSoftware" to 0.85,
    "CD Projekt Red" to 0.75,
    "Square Enix" to 0.65,
    "Epic Games" to 0.6,
    "Bandai Namco" to 0.6,
    "Supercell" to 0.55,
)

/** Week of the year the awards are held. */
const val AWARDS_WEEK = 50

internal fun salesDecay(age: Int, score: Double): Double =
    if (age < 6) (0.75 + 0.02 * (score - 7)).clamped(0.68, 0.84) else (0.95 + 0.005 * (score - 7)).clamped(0.92, 0.97)

/** Rivals release, sales decay, the chart is drawn up, and in December the awards come round. */
internal fun GameState.marketWeek() {
    val g = games ?: return
    val m = g.market
    val rival = world.rival(Lane.GAMES)?.strength ?: 0.5
    if (rng.chance(0.07 + 0.08 * rival)) releaseRivalGame(g)

    for (r in m.games) {
        if (r.weeklySales <= 0) continue
        r.lifetime += r.weeklySales
        r.weeklySales *= salesDecay(week - r.week, r.score)
        if (r.weeklySales < 300) r.weeklySales = 0.0
    }
    m.games.removeAll { it.weeklySales <= 0 && week - it.week > 104 }
    while (m.games.size > 60) m.games.removeAt(0)

    val entries = m.games.filter { it.weeklySales > 0 }.map { ChartEntry(it.title, it.studio, it.weeklySales, it.score) } +
        g.releases.filter { it.weeklySales > 0 }.map { ChartEntry(it.title, companyName, it.weeklySales, it.score, it.id) }
    val sorted = entries.sortedByDescending { it.weeklySales }
    m.chart = sorted.take(10)
    for (r in g.releases) {
        val rank = sorted.indexOfFirst { it.releaseId == r.id } + 1
        r.chartRank = rank
        if (rank > 0) {
            val wasTop = r.bestRank == 1
            r.bestRank = if (r.bestRank == 0) rank else minOf(r.bestRank, rank)
            if (rank == 1 && !wasTop) {
                stats.chartToppers += 1
                addLog("${r.title} is the best-selling game in the world this week.", LogTone.MILESTONE, Lane.GAMES)
            }
        }
    }

    if (GameClock.weekOfYear(week) == AWARDS_WEEK) holdAwards(g)
}

private fun GameState.releaseRivalGame(g: GamesStudio) {
    val m = g.market
    val (studio, quality) = rng.pick(RivalStudios)
    val year = GameClock.year(week)
    val size = when {
        year <= 2 -> if (rng.chance(0.7)) GameSize.SMALL else GameSize.MEDIUM
        year <= 5 -> rng.pick(listOf(GameSize.SMALL, GameSize.MEDIUM, GameSize.MEDIUM, GameSize.LARGE, GameSize.LARGE))
        else -> rng.pick(listOf(GameSize.MEDIUM, GameSize.LARGE, GameSize.LARGE, GameSize.AAA, GameSize.AAA))
    }
    val genre = rng.pick(Genre.entries)
    val topic = rng.pick(Topic.entries.filter { it.fit(genre) != Fit.BAD })
    val score = rng.normal(6.2 + 1.2 * quality, 1.2).clamped(2.5, 9.7)
    val sales = size.launchSales * (score / 7).pow(3) * rng.range(0.7, 1.3) * world.era.demand(Lane.GAMES)
    val title = "${rng.pick(GameNames.first)} ${rng.pick(GameNames.second)}"
    val game = RivalGame(m.nextId++, title, studio, topic, genre, size, score, week, sales)
    m.games += game
    if (score >= 8 || size >= GameSize.LARGE) {
        val s = String.format(java.util.Locale.US, "%.1f", score)
        addLog("$studio released $title, ${Format.a(topic.title.lowercase())} ${genre.lower} game: $s/10.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** A strong rival game in the same genre, out in the last few weeks, takes buyers from yours. */
fun GamesStudio.clashFor(concept: GameConcept, week: Int): RivalGame? =
    market.games
        .filter { week - it.week in 0..6 && it.score >= 7 && (it.genre == concept.genre || it.genre == concept.genre2) }
        .maxByOrNull { it.score }

/** This year's best games, rivals' and yours. */
fun GameState.awardNominees(): List<ChartEntry> {
    val g = games ?: return emptyList()
    val year = GameClock.year(week)
    val rivals = g.market.games.filter { GameClock.year(it.week) == year }.map { ChartEntry(it.title, it.studio, it.weeklySales, it.score) }
    val mine = g.releases.filter { GameClock.year(it.week) == year && it.publisher == null }
        .map { ChartEntry(it.title, companyName, it.weeklySales, it.score, it.id) }
    return (rivals + mine).sortedByDescending { it.score }.take(4)
}

private fun GameState.holdAwards(g: GamesStudio) {
    val nominees = awardNominees()
    if (nominees.isEmpty()) return
    if (nominees.any { it.mine }) {
        enqueue(EventKind.GameAwards)
    } else {
        announceWinner(g, nominees, attended = false)
    }
}

/** Better games are likelier to win; showing up helps a little. */
internal fun GameState.announceWinner(g: GamesStudio, nominees: List<ChartEntry>, attended: Boolean) {
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
    g.market.awards += Award(year, winner.title, winner.studio, winner.score, winner.mine)
    if (winner.mine) {
        stats.awards += 1
        g.fans += max(2_000.0, g.fans * 0.25)
        g.fanTrust = minOf(1.2, g.fanTrust + 0.05)
        reputation.adjust(5.0)
        g.release(winner.releaseId ?: -1)?.let { r -> r.weeklySales = max(r.weeklySales * 1.6, r.lifetimeSales / r.netShare * 0.05) }
        g.project?.let { it.hype = minOf(100.0, it.hype + 15) }
        addLog("${winner.title} wins Game of the Year! Fans flock to you and sales jump.", LogTone.MILESTONE, Lane.GAMES)
    } else {
        addLog("Game of the Year $year goes to ${winner.title} by ${winner.studio}.", LogTone.NEUTRAL, Lane.GAMES)
    }
}
