package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Running the studios you own.
 *
 * - Each studio pitches its next game. You greenlight it with a budget (tight, normal or
 *   generous) and pay for it; when it's done it comes out under their name, and since you paid
 *   for it, most of what it sells comes to you.
 * - Each studio has a morale. How you run it sets where it heads; hits, flops, budgets and
 *   cancellations push it around. Happy studios make better games and earn more.
 * - When morale falls too far the studio's people say so, and you decide what to do about it.
 */

/** How much you give a studio to make its game. */
@Serializable
enum class StudioBudget(val title: String, val blurb: String, val cost: Double, val score: Double, val morale: Double) {
    TIGHT("Tight", "Less money, and it shows. The team notices.", 0.6, -0.4, -0.05),
    NORMAL("Normal", "What a game like this usually costs.", 1.0, 0.0, 0.0),
    GENEROUS("Generous", "Everything they asked for. Better game, happier team.", 1.6, 0.5, 0.08),
}

/** A game an owned studio is making with your money. */
@Serializable
data class StudioGame(
    val title: String,
    val topic: Topic,
    val genre: Genre,
    val size: GameSize,
    val budget: StudioBudget,
    val startWeek: Int,
    val doneWeek: Int,
    val cost: Double,
    val franchise: String? = null,
)

/** Your studio's people are unhappy. */
@Serializable @SerialName("studioUnrest")
data class StudioUnrest(val studio: String) : EventKind

/** Your share of what a game you paid for sells. */
const val FUNDED_SHARE = 0.6

/** Where morale heads under each way of running a studio. */
fun Plan.moraleTarget(): Double = when (this) {
    Plan.INDEPENDENT -> 0.75
    Plan.INTEGRATED -> 0.5
    Plan.SQUEEZED -> 0.2
}

/** The game a studio wants to make next. The same pitch until it's made or the studio changes hands. */
fun GameState.studioPitch(sub: Subsidiary): StudioGame {
    val s = studio(sub.name)
    val dice = SeededRNG.fromSeed(seed * 31 + sub.name.hashCode() * 7_919L + sub.shipped * 104_729L + sub.boughtWeek)
    val indie = s?.indie == true
    val quality = s?.quality ?: 0.6
    var size = when {
        indie -> if (dice.chance(0.7)) GameSize.SMALL else GameSize.MEDIUM
        quality >= 0.8 -> if (dice.chance(0.6)) GameSize.AAA else GameSize.LARGE
        else -> if (dice.chance(0.6)) GameSize.LARGE else GameSize.MEDIUM
    }
    if (isDecades) size = minOf(size, biggestRivalGame(calendarYear))
    val genre = dice.pick(Genre.entries)
    val topic = dice.pick(Topic.entries.filter { it.fit(genre) != Fit.BAD })
    val series = studioFranchises(sub.name)
    // Once you've said what you want, that's what they pitch.
    val franchise = if (sub.pitchChosen) sub.pitchSeries?.takeIf { it in series } else if (series.isNotEmpty() && dice.chance(0.7)) dice.pick(series) else null
    val name = gameTitle(topic, genre, dice.int(0, 1_000_000).toLong(), games?.market?.games?.map { it.title }?.toSet().orEmpty())
    val title = if (franchise != null) "$franchise: $name" else name
    val (cost, weeks) = studioGameQuote(size)
    return StudioGame(title, topic, genre, size, StudioBudget.NORMAL, week, week + weeks, cost, franchise)
}

/** What a studio game of [size] costs at a normal budget, and how long it takes. */
fun GameState.studioGameQuote(size: GameSize): Pair<Double, Int> = when (size) {
    GameSize.SMALL -> scaled(1_000_000.0) to 30
    GameSize.MEDIUM -> scaled(5_000_000.0) to 45
    GameSize.LARGE -> scaled(25_000_000.0) to 65
    GameSize.AAA -> scaled(80_000_000.0) to 90
}

/** Why [name]'s pitch can't be greenlit at [budget], or null if it can. */
fun GameState.greenlightBlocker(name: String, budget: StudioBudget): String? {
    val sub = games?.subsidiaries?.firstOrNull { it.name == name } ?: return "You don't own $name."
    if (sub.project != null) return "They're already making ${sub.project!!.title}."
    val cost = studioPitch(sub).cost * budget.cost
    if (cash < cost) return "It costs ${money(cost)}."
    return null
}

/** Pays for a studio's next game. */
fun GameState.greenlight(name: String, budget: StudioBudget) {
    requireLive()
    greenlightBlocker(name, budget)?.let { throw notAvailable(it) }
    val sub = games!!.subsidiaries.first { it.name == name }
    val pitch = studioPitch(sub)
    val cost = pitch.cost * budget.cost
    spend(cost, Lane.GAMES)
    val weeks = pitch.doneWeek - pitch.startWeek
    sub.project = pitch.copy(budget = budget, cost = cost, startWeek = week, doneWeek = week + weeks)
    sub.pitchChosen = false
    sub.morale = (sub.morale + budget.morale + 0.03).coerceIn(0.0, 1.0)
    addLog("You greenlit ${pitch.title} at $name: ${money(cost)}, out in about ${Format.weeks(weeks)}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Stops a studio's game. The money's gone, and the team takes it hard. */
fun GameState.cancelStudioGame(name: String) {
    requireLive()
    val sub = games?.subsidiaries?.firstOrNull { it.name == name } ?: throw invalid("You don't own $name.")
    val p = sub.project ?: throw notAvailable("$name isn't making anything for you.")
    sub.project = null
    sub.morale = max(0.0, sub.morale - 0.15)
    addLog("You cancelled ${p.title} at $name. ${money(p.cost)} written off.", LogTone.BAD, Lane.GAMES)
    publish(NewsDesk.GAMES, "Kotaku", "$companyName cancels ${p.title}", "$name's next game is dead. Staff are said to be devastated.", LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS)
}

/** How far along a studio's game is, 0...1. */
fun GameState.studioGameProgress(p: StudioGame): Double =
    ((week - p.startWeek).toDouble() / max(1, p.doneWeek - p.startWeek)).coerceIn(0.0, 1.0)

/** Each week: morale drifts, games come out, and an unhappy studio speaks up. */
internal fun GameState.ownedStudioWeek(g: GamesStudio) {
    for (sub in g.subsidiaries) {
        sub.morale += (sub.plan.moraleTarget() - sub.morale) * 0.03
        val p = sub.project
        if (p != null && week >= p.doneWeek) {
            sub.project = null
            sub.shipped += 1
            shipStudioGame(g, sub, p)
        }
        sub.morale = sub.morale.coerceIn(0.0, 1.0)
        if (sub.morale < 0.3 && week - sub.unrestWeek >= 26 && events.none { (it.kind as? StudioUnrest)?.studio == sub.name }) {
            sub.unrestWeek = week
            enqueue(StudioUnrest(sub.name))
        }
    }
}

private fun GameState.shipStudioGame(g: GamesStudio, sub: Subsidiary, p: StudioGame) {
    val s = studio(sub.name)
    val quality = (s?.quality ?: 0.6) - 0.05 * sub.talentLost
    val dice = SeededRNG.fromSeed(seed * 17 + p.title.hashCode() + week)
    val score = (dice.normal(6.2 + 1.2 * quality + p.budget.score + (sub.morale - 0.6) * 2 - ownedQualityPenalty(sub.name), 0.8)).clamped(2.5, 9.8)
    val sales = p.size.launchSales * (score / 7).pow(3) * dice.range(0.8, 1.2) * world.demand(Lane.GAMES)
    val studioLabel = studioName(sub.name)
    g.market.games += RivalGame(g.market.nextId++, p.title, studioLabel, p.topic, p.genre, p.size, score, week, sales, franchise = p.franchise, funded = true)
    sub.morale += when {
        score >= 8 -> 0.12
        score < 5 -> -0.12
        else -> 0.0
    }
    if (score >= 8) sub.hits += 1
    stats.studioGames += 1
    val sc = String.format(java.util.Locale.US, "%.1f", score)
    addLog("${p.title} is out from $studioLabel: $sc/10.", if (score >= 8) LogTone.GOOD else if (score < 5) LogTone.BAD else LogTone.NEUTRAL, Lane.GAMES)
    publish(
        NewsDesk.YOU, "IGN", "${p.title} review: $sc/10",
        "$studioLabel's ${p.size.lower} ${p.genre.lower} game, paid for by $companyName.",
        if (score >= 8) LogTone.GOOD else if (score < 5) LogTone.BAD else LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.REVIEW,
    )
}

/** The games of [sub]'s that you paid for, newest first. */
fun GameState.fundedGames(sub: Subsidiary): List<RivalGame> {
    val label = studioName(sub.name)
    return games?.market?.games?.filter { it.funded && (it.studio == label || canonicalStudio(it.studio) == sub.name) }?.sortedByDescending { it.week }.orEmpty()
}

/** Your share of what the games you paid for are selling this week. */
fun GameState.fundedSales(sub: Subsidiary): Double = fundedGames(sub).sumOf { it.weeklySales * FUNDED_SHARE }

/** What a retention bonus costs at [s]. */
fun GameState.retentionCost(s: Studio): Double = max(scaled(100_000.0), studioValue(s) * 0.004)

internal fun GameState.describeUnrest(k: StudioUnrest): Description? {
    val sub = games?.subsidiaries?.firstOrNull { it.name == k.studio } ?: return null
    val s = studio(k.studio) ?: return null
    val why = when (sub.plan) {
        Plan.SQUEEZED -> "The cost cuts have gone too far."
        Plan.INTEGRATED -> "They feel like a department, not a studio."
        Plan.INDEPENDENT -> "Things haven't gone their way lately."
    }
    return Description(
        "Trouble at ${k.studio}",
        "Morale at ${k.studio} is down to ${Format.percent(sub.morale)}. $why Senior people are taking calls from recruiters.",
        listOfNotNull(
            choice("Pay retention bonuses", "${money(retentionCost(s))}. People stay, and feel valued."),
            if (sub.plan != Plan.INDEPENDENT) choice("Let them run themselves", "Back to how they were before you bought them, starting now.") else null,
            choice("Let them walk", "Some of their best leave. Their games and value suffer."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveUnrest(k: StudioUnrest, choice: Int) {
    val sub = games?.subsidiaries?.firstOrNull { it.name == k.studio } ?: return
    val s = studio(k.studio) ?: return
    when (describeUnrest(k)?.choices?.getOrNull(choice)?.label) {
        "Pay retention bonuses" -> {
            spend(retentionCost(s), Lane.GAMES)
            sub.morale = min(1.0, sub.morale + 0.3)
            addLog("Retention bonuses at ${k.studio}. The team's staying.", LogTone.NEUTRAL, Lane.GAMES)
        }
        "Let them run themselves" -> {
            sub.plan = Plan.INDEPENDENT
            sub.planWeek = week
            sub.morale = min(1.0, sub.morale + 0.2)
            addLog("${k.studio} runs itself again.", LogTone.GOOD, Lane.GAMES)
        }
        else -> {
            sub.talentLost += 1
            sub.morale = min(1.0, sub.morale + 0.05)
            addLog("Some of ${k.studio}'s best people left.", LogTone.BAD, Lane.GAMES)
            publish(NewsDesk.GAMES, "Kotaku", "Senior staff walk out of ${k.studio}", "Sources blame life under $companyName.", LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS)
        }
    }
}
