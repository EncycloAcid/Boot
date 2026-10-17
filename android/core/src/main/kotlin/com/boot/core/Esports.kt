package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.pow

/*
 * Esports. Once researched, a competitive hit (an action or strategy game that scored 7.5 or more,
 * built on an engine with multiplayer, with a real player base) can get a pro league. The league
 * draws viewers, and sponsors pay for them every week; production costs grow with the audience.
 * People who watch keep buying and playing: the game's sales fade more slowly and a live game
 * keeps more of its players. Twice a year the league holds its championship. You set the prize
 * pool: a bigger one costs more and wins over more of the people who own the game but don't watch
 * yet, so it pays off on the biggest games and not on small ones. Now and then a final ends in scandal. A league with almost nobody watching folds.
 */

@Serializable
enum class PrizePool(val title: String, val amount: Double, internal val reach: Double) {
    MODEST("Modest", 100_000.0, 0.05),
    MAJOR("Major", 1_000_000.0, 0.15),
    RECORD("Record", 5_000_000.0, 0.3),
}

@Serializable
data class League(
    val name: String,
    val startedWeek: Int,
    var viewers: Double,
    var nextFinal: Int,
    var peakViewers: Double = viewers,
    var prize: PrizePool = PrizePool.MODEST,
    var finals: Int = 0,
    var lastChampion: String? = null,
    var lastRevenue: Double = 0.0,
    var lastCost: Double = 0.0,
    var lifetimeProfit: Double = 0.0,
    var closed: Boolean = false,
)

/** Weeks between championships. */
const val FINAL_GAP = 26

/** Copies sold before a game has the player base for a league. */
const val LEAGUE_MIN_PLAYERS = 100_000.0

private val teams = listOf(
    "Team Liquid", "Fnatic", "T1", "Cloud9", "G2 Esports", "Natus Vincere", "FaZe Clan", "Team Vitality",
    "100 Thieves", "Gen.G", "Team Spirit", "OG", "Evil Geniuses", "Sentinels", "MOUZ", "Heroic",
)

/** Everyone who could watch: the people who own it, or play it now if it's live. */
private val GameRelease.audience: Double get() = max(unitsSold, live?.players ?: 0.0)

/** The most a league can draw: a share of its audience. */
private val GameRelease.viewerCap: Double get() = audience * 0.4

/** What setting up a league costs. */
fun GameRelease.leagueCost(): Double = 100_000 + unitsSold * 0.4

private val GameRelease.competitive: Boolean
    get() = concept.genre in COMPETITIVE || concept.genre2 in COMPETITIVE

private val COMPETITIVE = setOf(Genre.ACTION, Genre.STRATEGY)

/** Why [releaseId] can't get a league, or null if it can. */
fun GameState.leagueBlocker(releaseId: Int): String? {
    val g = games ?: return "You don't run a game studio."
    if (Research.ESPORTS !in researched) return "Research esports first."
    val r = g.release(releaseId) ?: return "That game is gone."
    if (r.developer != null) return "${r.developer} runs the competitive side of ${r.title}."
    if (r.publisher != null) return "${r.publisher} owns ${r.title}."
    r.league?.let { if (!it.closed) return "${r.title} already has a league." else return "${r.title}'s league has folded." }
    if (!r.competitive) return "Esports need an action or strategy game."
    if (r.score < 7.5) return "Only games that scored 7.5 or more draw a crowd."
    if (Research.MULTIPLAYER !in g.engine(r.concept.engineId).features) return "${r.title} was built on an engine without multiplayer."
    if (r.audience < LEAGUE_MIN_PLAYERS) return "Needs ${Format.count(LEAGUE_MIN_PLAYERS)} players. ${r.title} has ${Format.count(r.audience)}."
    if (cash < r.leagueCost()) return "Setting up a league costs ${money(r.leagueCost())}."
    return null
}

/** Your games that could get a league now, or soon. */
val GameState.leagueCandidates: List<GameRelease>
    get() {
        val g = games ?: return emptyList()
        return g.releases.filter { r ->
            r.league == null && r.developer == null && r.publisher == null && r.competitive && r.score >= 7.5 &&
                Research.MULTIPLAYER in g.engine(r.concept.engineId).features
        }
    }

/** Your leagues that are still running. */
val GamesStudio.leagues: List<GameRelease> get() = releases.filter { it.league?.closed == false }

fun GameState.startLeague(releaseId: Int) {
    if (ending != null) throw gameOver()
    leagueBlocker(releaseId)?.let { throw notAvailable(it) }
    val g = games!!
    val r = g.release(releaseId)!!
    spend(r.leagueCost(), Lane.GAMES)
    val name = "${seriesName(g.seriesRoot(r).title)} ${if (r.concept.genre == Genre.STRATEGY) "Championship Series" else "Pro League"}"
    val viewers = r.audience * 0.04 * (r.score / 8).pow(2)
    r.league = League(name, week, viewers, nextFinal = week + FINAL_GAP)
    addLog("The $name is on. ${Format.count(viewers)} people tuned in for the first week. The championship is in ${Format.weeks(FINAL_GAP)}.", LogTone.GOOD, Lane.GAMES)
}

fun GameState.setPrizePool(releaseId: Int, prize: PrizePool) {
    val l = games?.release(releaseId)?.league ?: throw invalid("That game has no league.")
    if (l.closed) throw notAvailable("That league has folded.")
    l.prize = prize
}

fun GameState.endLeague(releaseId: Int) {
    val r = games?.release(releaseId) ?: throw invalid("No such game.")
    val l = r.league ?: throw invalid("That game has no league.")
    if (l.closed) return
    l.closed = true
    addLog("You wound up the ${l.name}.", LogTone.NEUTRAL, Lane.GAMES)
}

internal fun GameState.esportsWeek() {
    val g = games ?: return
    val demand = context(Lane.GAMES).marketDemand
    for (r in g.leagues) {
        val l = r.league!!
        // Without a game people still buy or play, the audience drifts off faster.
        val alive = r.weeklySales > 0 || (r.live?.closed == false && (r.live?.players ?: 0.0) > 0)
        l.viewers *= if (alive) 0.996 else 0.985
        val revenue = l.viewers * 0.9 * demand
        val cost = 8_000 + l.viewers * 0.15
        cash += revenue - cost
        recordRevenue(revenue, Lane.GAMES)
        recordCost(cost, Lane.GAMES)
        l.lastRevenue = revenue
        l.lastCost = cost
        l.lifetimeProfit += revenue - cost
        // Watching makes people play.
        if (r.weeklySales > 0) r.weeklySales *= 1.008
        r.live?.let { if (!it.closed) it.players *= 1.003 }
        g.fans += l.viewers * 0.0004
        if (week >= l.nextFinal) championship(r, l)
        if (l.viewers < 2_000) {
            l.closed = true
            addLog("Almost nobody watches the ${l.name} any more. It's folded.", LogTone.BAD, Lane.GAMES)
        }
    }
}

private fun GameState.championship(r: GameRelease, l: League) {
    l.nextFinal = week + FINAL_GAP
    val prize = PrizePool.entries.filter { it <= l.prize }.lastOrNull { cash > it.amount * 2 }
    if (prize == null) {
        l.viewers *= 0.85
        addLog("The ${l.name} couldn't fund a championship this time. Viewers noticed.", LogTone.BAD, Lane.GAMES)
        return
    }
    spend(prize.amount, Lane.GAMES)
    l.finals += 1
    stats.finals += 1
    val champion = rng.pick(teams)
    l.lastChampion = champion
    if (rng.chance(0.06)) {
        l.viewers *= 0.7
        addLog("Match-fixing scandal at the ${l.name} final. $champion's title is under review, and a lot of viewers walked away.", LogTone.BAD, Lane.GAMES)
        return
    }
    l.viewers += (r.viewerCap - l.viewers).coerceAtLeast(0.0) * prize.reach
    l.peakViewers = max(l.peakViewers, l.viewers)
    if (r.weeklySales > 0) r.weeklySales *= 1.15
    games!!.fans += l.viewers * 0.02
    publish(
        NewsDesk.GAMES, rng.pick(listOf("Dexerto", "ESPN Esports", "The Esports Observer")),
        "$champion win the ${l.name}", "${Format.count(l.viewers)} watched the final. Prize pool: ${money(prize.amount)}.", LogTone.GOOD, Lane.GAMES,
    )
    addLog(
        "$champion win the ${l.name} and ${money(prize.amount)} in front of ${Format.count(l.viewers)} viewers." +
            if (prize < l.prize) " You could only afford a ${prize.title.lowercase()} prize pool." else "",
        LogTone.MILESTONE, Lane.GAMES,
    )
}
