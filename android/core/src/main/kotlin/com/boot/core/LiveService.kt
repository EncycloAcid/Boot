package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Games as a service. Once researched, a medium or bigger game can ship as a live game: it sells a
 * little less up front, but keeps a community of players who spend in the game every week for as
 * long as you keep them happy. Players drift away over time, faster when the game is weak and much
 * faster when it goes stale, so you launch seasons of new content to bring them back. Servers cost
 * money every week, and you choose how hard the store pushes: cosmetics only, fair, or aggressive.
 */

@Serializable
enum class Monetization {
    COSMETIC, FAIR, AGGRESSIVE;

    val title: String
        get() = when (this) {
            COSMETIC -> "Cosmetics only"
            FAIR -> "Fair"
            AGGRESSIVE -> "Aggressive"
        }

    /** What an average player spends a week. */
    internal val spend: Double
        get() = when (this) {
            COSMETIC -> 0.22
            FAIR -> 0.4
            AGGRESSIVE -> 0.75
        }

    /** How much faster players leave. */
    internal val churn: Double
        get() = when (this) {
            COSMETIC -> 0.85
            FAIR -> 1.0
            AGGRESSIVE -> 1.45
        }
}

@Serializable
data class LiveService(
    var players: Double,
    var lastSeasonWeek: Int,
    var peakPlayers: Double = players,
    var seasons: Int = 0,
    var monetization: Monetization = Monetization.FAIR,
    var lastRevenue: Double = 0.0,
    var lastCost: Double = 0.0,
    var lifetimeRevenue: Double = 0.0,
    var closed: Boolean = false,
)

/** Weeks between seasons, at least. */
const val SEASON_GAP = 8

/** Weeks before players start calling a live game dead. */
const val STALE_AFTER = 20

/** Players a live game starts with, from its launch week. */
internal fun livePlayers(launchUnits: Double): Double = launchUnits * 6

/** The weekly tick for one live game: players come and go, spend, and the servers get paid. Returns (revenue, cost). */
internal fun LiveService.week(week: Int, wordOfMouth: Double, demand: Double): Pair<Double, Double> {
    if (closed) return 0.0 to 0.0
    val since = week - lastSeasonWeek
    val stale = if (since > STALE_AFTER) min(2.5, 1 + (since - STALE_AFTER) * 0.05) else 1.0
    val churn = (0.012 * (7.0 / wordOfMouth.coerceAtLeast(2.0)).pow(1.5) * stale * monetization.churn).clamped(0.004, 0.08)
    players *= 1 - churn
    val revenue = players * monetization.spend * demand
    val cost = 2_000 + players * 0.03
    lastRevenue = revenue
    lastCost = cost
    lifetimeRevenue += revenue
    return revenue to cost
}

/** What the next season costs to make. */
fun GameRelease.seasonCost(): Double = concept.size.weeklyBudget + (live?.players ?: 0.0) * 0.6

fun GameState.seasonBlocker(releaseId: Int): String? {
    val r = games?.release(releaseId) ?: return "No such game."
    val l = r.live ?: return "${r.title} isn't a live game."
    if (l.closed) return "${r.title}'s servers are shut down."
    val wait = SEASON_GAP - (week - l.lastSeasonWeek)
    if (wait > 0) return "The team needs ${Format.weeks(wait)} more for the next season."
    if (cash < r.seasonCost()) return "A season costs ${money(r.seasonCost())}."
    return null
}

/** New content: lapsed players come back and new ones try it. */
fun GameState.launchSeason(releaseId: Int) {
    requireLive()
    seasonBlocker(releaseId)?.let { throw notAvailable(it) }
    val g = games!!
    val r = g.release(releaseId)!!
    val l = r.live!!
    spend(r.seasonCost(), Lane.GAMES)
    l.seasons += 1
    val boost = 0.18 + 0.04 * ((r.wordOfMouth - 6).clamped(0.0, 3.0))
    l.players *= 1 + boost
    l.peakPlayers = max(l.peakPlayers, l.players)
    l.lastSeasonWeek = week
    g.fans += l.players * 0.01
    stats.seasons += 1
    addLog("${r.title} Season ${l.seasons + 1} is live. ${Format.count(l.players)} players are back in.", LogTone.GOOD, Lane.GAMES)
}

fun GameState.setMonetization(releaseId: Int, m: Monetization) {
    requireLive()
    val l = games?.release(releaseId)?.live ?: throw invalid("Not a live game.")
    if (l.closed) throw notAvailable("The servers are shut down.")
    l.monetization = m
}

/** Turn the servers off for good. Players who were still there don't take it well. */
fun GameState.endService(releaseId: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You don't run a game studio.")
    val r = g.release(releaseId) ?: throw invalid("No such game.")
    val l = r.live ?: throw invalid("Not a live game.")
    if (l.closed) return
    val upset = l.players
    l.closed = true
    l.players = 0.0
    if (upset > 20_000) {
        g.fanTrust = max(0.3, g.fanTrust - 0.03)
        addLog("You shut down ${r.title}. ${Format.count(upset)} players lost what they'd bought, and they remember.", LogTone.BAD, Lane.GAMES)
    } else {
        addLog("You shut down ${r.title}'s servers.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** Live games running right now. */
val GamesStudio.liveGames: List<GameRelease> get() = releases.filter { it.live?.closed == false }
