package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Collections.
 *
 * Once a series has a few games behind it, package the older ones together: a double pack, a
 * trilogy, a collection. An outside port house puts it together in a couple of months for not much,
 * and it sells at a budget price to people who missed them and people who want them on today's
 * machines. The originals sell less while it's out, since that's where their buyers go.
 */

/** How old a game has to be before it goes in a collection. */
const val COLLECTION_AGE = 104

/** A collection being put together. */
@Serializable
data class CollectionJob(val rootId: Int, val ids: List<Int>, val title: String, val cost: Double, val startWeek: Int, val doneWeek: Int)

/** A collection: a release that brings older games together. */
val GameRelease.isCollection: Boolean get() = concept.collects.isNotEmpty()

/** What to call a collection of [n] games of [series]. */
fun collectionTitle(series: String, n: Int): String = when (n) {
    2 -> "$series Double Pack"
    3 -> "$series Trilogy"
    else -> "$series Collection"
}

/** The games of [rootId]'s series that could go in a collection now: yours, two years old, not already collected. */
fun GameState.collectable(rootId: Int): List<GameRelease> {
    val g = games ?: return emptyList()
    val root = g.release(rootId) ?: return emptyList()
    return g.series(root).filter {
        !it.isCollection && it.developer == null && it.publisher == null && it.id !in g.collected && week - it.week >= COLLECTION_AGE
    }
}

/** Your series with games old enough to collect, best-selling first. */
fun GameState.collectableSeries(): List<Franchise> {
    val g = games ?: return emptyList()
    return g.releases.filter { it.concept.sequelOf == null && !it.isCollection && it.developer == null && it.publisher == null }
        .map { Franchise(it, g.series(it)) }
        .filter { it.entries.size >= 2 }
        .sortedByDescending { it.lifetime }
}

/** What putting [ids] together costs, and how long it takes. */
fun GameState.collectionQuote(ids: List<Int>): Pair<Double, Int> {
    val games = ids.mapNotNull { this.games?.release(it) }
    val cost = games.sumOf { scaled(40_000.0) * (it.concept.size.launchSales / GameSize.MEDIUM.launchSales).coerceIn(0.4, 3.0) }
    return cost to 6 + 2 * games.size
}

/** Why [rootId]'s series can't get a collection now, or null if it can. */
fun GameState.collectionBlocker(rootId: Int): String? {
    val g = games ?: return "You don't make games."
    if (g.collectionJobs.any { it.rootId == rootId }) return "One's already being put together."
    val ready = collectable(rootId)
    if (ready.size < 2) {
        val waiting = g.release(rootId)?.let { root -> g.series(root).filter { !it.isCollection && it.id !in g.collected && week - it.week < COLLECTION_AGE } }.orEmpty()
        return if (ready.size + waiting.size >= 2) {
            "Its games need to be ${Format.weeks(COLLECTION_AGE)} old. The next is ready in ${Format.weeks(waiting.minOf { COLLECTION_AGE - (week - it.week) })}."
        } else {
            "It needs two games that aren't in a collection yet."
        }
    }
    val (cost, _) = collectionQuote(ready.map { it.id })
    if (cash < cost) return "It costs ${money(cost)}."
    return null
}

/** Put the older games of [rootId]'s series together in a collection. */
fun GameState.makeCollection(rootId: Int) {
    requireLive()
    collectionBlocker(rootId)?.let { throw notAvailable(it) }
    val g = games!!
    val ready = collectable(rootId)
    val name = seriesName(g.release(rootId)!!.title)
    val (cost, weeks) = collectionQuote(ready.map { it.id })
    spend(cost, Lane.GAMES)
    val title = collectionTitle(name, ready.size).let { t -> if (g.releases.any { it.title == t }) "$t ${calendarYear}" else t }
    g.collectionJobs += CollectionJob(rootId, ready.map { it.id }, title, cost, week, week + weeks)
    g.collected += ready.map { it.id }
    addLog("$title is being put together: ${ready.joinToString { it.title }}. Out in about ${Format.weeks(weeks)}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Each week: finished collections come out. */
internal fun GameState.collectionWeek(g: GamesStudio) {
    val done = g.collectionJobs.filter { week >= it.doneWeek }
    for (job in done) {
        g.collectionJobs -= job
        releaseCollection(g, job)
    }
}

private fun GameState.releaseCollection(g: GamesStudio, job: CollectionJob) {
    val games = job.ids.mapNotNull { g.release(it) }
    if (games.isEmpty()) return
    val latest = games.last()
    // It comes out on today's machines: the newest game's, where they're still on sale.
    val platforms = latest.concept.platforms.filter { availablePlatform(it) }.ifEmpty { listOf(Platform.PC) }
    val score = (games.map { it.score }.average() - 0.3 + 0.15 * (games.size - 2)).clamped(1.0, 9.5)
    val concept = latest.concept.copy(
        title = job.title, sequelOf = null, remasterOf = null, price = PriceTier.BUDGET, live = false, hook = null,
        production = Production.STANDARD, platforms = platforms, collects = games.map { it.title },
    )
    // People who missed them, and people who want them on today's machines.
    val buyers = games.sumOf { it.unitsSold } * 0.05 * (score / 7) * world.demand(Lane.GAMES)
    val weekly = max(scaled(10_000.0), buyers * concept.unitPrice / 6)
    val release = GameRelease(
        id = g.nextId(), title = job.title, concept = concept, week = week, reviews = emptyList(), score = score, weeklySales = weekly,
        netShare = 0.7, design = 0.0, tech = 0.0, bugs = 0.0, hype = 0.0, devCost = job.cost,
        report = listOf(
            "Brings together ${games.joinToString { it.title }}.",
            "Critics scored it ${String.format(java.util.Locale.US, "%.1f", score)}: the games, a little older, at a budget price.",
            "The originals sell less while it's out: their buyers come here.",
        ),
        userScore = min(10.0, score + 0.3), userScoreSettled = true,
    )
    g.releases.add(0, release)
    for (r in games) r.weeklySales *= 0.6
    stats.collections += 1
    addLog("${job.title} is out: ${Format.money(weekly)} in its first week.", LogTone.GOOD, Lane.GAMES)
    publish(
        NewsDesk.GAMES, "Eurogamer", "${job.title} review: ${if (score >= 8) "a bargain" else if (score >= 6.5) "worth it for the price" else "show their age"}",
        "${games.size} games for one budget price.", if (score >= 7) LogTone.GOOD else LogTone.NEUTRAL, Lane.GAMES,
        tag = NewsTag.REVIEW, cover = release.newsCover(),
    )
}
