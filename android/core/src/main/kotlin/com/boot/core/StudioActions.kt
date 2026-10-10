package com.boot.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

// Everything a game studio can do: design and start games, steer development, market, ship,
// take side work, and build engines.

private fun GameState.studio(): GamesStudio {
    if (ending != null) throw gameOver()
    return games ?: throw notAvailable("You don't run a game studio.")
}

/** Game sizes your research allows. */
val GameState.unlockedSizes: List<GameSize> get() = GameSize.entries.filter { it.requires == null || it.requires in researched }

/** Whether a platform is on sale and open to you this week. */
fun GameState.availablePlatform(p: Platform): Boolean = when (p) {
    Platform.OWN_CONSOLE -> crossover(Combo.GAMES_HARDWARE)?.built == true && (hardware?.lifetimeUnits ?: 0) > 0
    else -> p.isAvailable(week)
}

/** Platforms you could ship on today. */
val GameState.platformsOnSale: List<Platform> get() = Platform.entries.filter { availablePlatform(it) }

fun GameState.platformPlayers(p: Platform): Double = p.players(week, hardware?.lifetimeUnits ?: 0)

/** Sales reach of a set of platforms, about 1 for one big console. */
fun GameState.reach(platforms: List<Platform>): Double = platforms.sumOf { platformPlayers(it) * it.spend } / 75.0

internal val GameState.factoryLeadTime: Int get() = if (Research.SUPPLY_CHAIN in researched) 8 else HardwareLab.LEAD_TIME_WEEKS

/** Staff assigned to the game studio. */
val GameState.gameTeam: Int get() = staff.count { it.lane == Lane.GAMES }

/** A balanced starting split: a third each. */
fun evenFocus(): List<MutableList<Double>> = List(3) { mutableListOf(1.0, 1.0, 1.0) }

/** Why a concept can't be started, or null if it can. */
fun GameState.conceptBlocker(c: GameConcept, deal: PublisherDeal? = null): String? {
    val g = games ?: return "You don't run a game studio."
    if (g.project != null) return "A game is already in development."
    if (g.contract != null) return "Finish the contract first."
    if (g.expansion != null) return "Finish the expansion first."
    if (c.topic !in g.topics) return "Research ${c.topic.title} first."
    c.size.requires?.let { if (it !in researched) return "Research ${it.title} first." }
    if (gameTeam < c.size.minTeam) return "${c.size.title} games need ${c.size.minTeam} people on the game team. You have $gameTeam."
    if (c.genre2 != null) {
        if (Research.MULTI_GENRE !in researched) return "Research multi-genre games first."
        if (c.genre2 == c.genre) return "Pick two different genres."
    }
    if (c.audience != Audience.EVERYONE && Research.TARGET_AUDIENCE !in researched && deal == null) return "Research target audience first."
    if (c.platforms.isEmpty()) return "Pick a platform."
    if (c.platforms.size > 1 && Research.MULTI_PLATFORM !in researched) return "Research multi-platform releases first."
    if (c.platforms.size > 3) return "Three platforms at most."
    c.platforms.firstOrNull { !availablePlatform(it) }?.let { return "${it.title} isn't on sale." }
    if (g.engines.none { it.id == c.engineId }) return "That engine doesn't exist."
    c.sequelOf?.let { id ->
        if (Research.SEQUELS !in researched) return "Research sequels first."
        val original = g.release(id) ?: return "No such game."
        if (original.concept.topic != c.topic) return "A sequel or spin-off keeps the topic."
    }
    val upfront = c.platforms.sumOf { it.fee } + c.size.weeklyBudget * 4
    if (cash < upfront) return "You need ${money(upfront)} for licence fees and the first month."
    return null
}

/** Rough weeks to finish at the current team's pace. */
fun GameState.estimatedWeeks(size: GameSize): Int = ceil(size.work / max(1.0, staffing(Lane.GAMES).output)).toInt()

fun GameState.startGame(concept: GameConcept, focus: List<MutableList<Double>> = evenFocus()) {
    val g = studio()
    conceptBlocker(concept)?.let { throw notAvailable(it) }
    val fees = concept.platforms.sumOf { it.fee }
    if (fees > 0) spend(fees, Lane.GAMES)
    val original = concept.sequelOf?.let { g.release(it) }
    var hype = minOf(25.0, g.fans / 4_000)
    if (original != null) {
        val loved = g.series(g.seriesRoot(original)).count { it.score >= 7 }
        hype += minOf(30.0, loved * 6.0 + if (original.score >= 8) 8.0 else 3.0)
    }
    val promised = g.promisedSequel?.let { g.release(it) }
    if (original != null && promised != null && g.seriesRoot(promised).id == g.seriesRoot(original).id) {
        hype += 20.0
        g.promisedSequel = null
    }
    g.project = GameProject(concept, focus.map { it.toMutableList() }, week, hype = hype)
    addLog(
        "Started ${concept.title}: ${Format.a(concept.size.lower)} ${concept.topic.title.lowercase()} " +
            "${concept.genreLower} game for ${concept.platforms.joinToString { it.title }}." +
            (if (fees > 0) " Licence fees: ${money(fees)}." else ""),
        LogTone.NEUTRAL, Lane.GAMES,
    )
}

/** Set how the team splits its time in one stage. Values are relative weights. */
fun GameState.setStageFocus(stage: Int, allocation: List<Double>) {
    val p = studio().project ?: throw invalid("Nothing in development.")
    if (stage !in 0..2 || allocation.size != 3) throw invalid("Three areas per stage.")
    p.focus[stage].clear()
    p.focus[stage].addAll(allocation.map { it.clamped(0.0, 1.0) })
}

fun GameState.cancelGame() {
    val g = studio()
    val p = g.project ?: throw invalid("Nothing in development.")
    g.project = null
    addLog("Cancelled ${p.title} after ${p.weeksInDev} weeks.", LogTone.BAD, Lane.GAMES)
}

fun GameState.buyCampaign(c: Campaign) {
    val g = studio()
    if (Research.MARKETING !in researched) throw notAvailable("Research marketing campaigns first.")
    val p = g.project ?: throw invalid("Nothing to market.")
    if (c in p.campaigns) throw busy("You've already run a ${c.title.lowercase()} for ${p.title}.")
    if (cash < c.cost) throw insufficientFunds("${c.title} costs ${money(c.cost)}.")
    spend(c.cost, Lane.GAMES)
    p.campaigns += c
    p.hype = minOf(100.0, p.hype + c.hype)
    addLog("${c.title} for ${p.title}. Hype is building.", LogTone.NEUTRAL, Lane.GAMES)
}

fun GameState.launchGame() {
    val g = studio()
    val p = g.project ?: throw invalid("No game in development.")
    val suited = p.concept.platforms.any { it.suits(p.concept.audience) }
    val report = g.launch(context(Lane.GAMES), rng, reach(p.concept.platforms), suited, g.clashFor(p.concept, week))
    cash += report.revenue - report.expenses
    recordRevenue(report.revenue, Lane.GAMES)
    recordCost(report.expenses, Lane.GAMES)
    handle(report.incidents, Lane.GAMES)
}

/** Dismiss the review screen. */
fun GameState.acknowledgeReview() {
    games?.unseenReview = null
}

fun GameState.takeGameContract(id: Int) {
    val g = studio()
    if (g.busy) throw busy("The team is busy.")
    val c = g.contractOffers.firstOrNull { it.id == id } ?: throw invalid("That job is gone.")
    g.contractOffers.remove(c)
    g.contract = c.copy()
    addLog("Took on contract work: ${c.title}. Due in ${c.weeksAllowed} weeks.", LogTone.NEUTRAL, Lane.GAMES)
}

fun GameState.acceptDeal(id: Int, focus: List<MutableList<Double>> = evenFocus()) {
    val g = studio()
    val d = g.dealOffers.firstOrNull { it.id == id } ?: throw invalid("That deal is off the table.")
    val concept = GameConcept(
        title = g.suggestTitle(rng, d.topic, d.genre), topic = d.topic, genre = d.genre, platforms = listOf(d.platform), size = d.size,
        engineId = g.engines.maxBy { it.level }.id,
    )
    val wasKnown = d.topic in g.topics
    if (!wasKnown) g.topics += d.topic
    // The publisher pays the licence, so check everything but money.
    val blocker = conceptBlocker(concept.copy(platforms = listOf(Platform.PC)), d)
    if (blocker != null) {
        if (!wasKnown) g.topics -= d.topic
        throw notAvailable(blocker)
    }
    g.dealOffers.remove(d)
    g.project = GameProject(concept, focus.map { it.toMutableList() }, week, hype = 20.0, deal = d)
    cash += d.advance * 0.5
    recordRevenue(d.advance * 0.5, Lane.GAMES)
    addLog(
        "Signed with ${d.publisher}: ${Format.a(d.size.lower)} ${d.topic.title.lowercase()} ${d.genre.lower} game " +
            "for ${d.platform.title}. ${money(d.advance * 0.5)} up front, the rest if it scores ${d.minScore}+.",
        LogTone.GOOD, Lane.GAMES,
    )
}

/** What a new engine with everything you've researched would cost. */
fun GameState.engineQuote(): Triple<List<Research>, Double, Int> {
    val features = researched.filter { it.kind == Research.Kind.ENGINE }.sortedBy { it.ordinal }
    val level = features.sumOf { it.engineLevel }
    val cost = 20_000 + 9_000 * level.toDouble().pow(1.35)
    return Triple(features, cost, 3 + level / 3)
}

fun GameState.buildEngine() {
    val g = studio()
    if (Research.CUSTOM_ENGINES !in researched) throw notAvailable("Research custom engines first.")
    if (g.engineBuild != null) throw busy("An engine is already being built.")
    val (features, cost, weeks) = engineQuote()
    if (features.isEmpty()) throw notAvailable("Research some engine features first.")
    val best = g.engines.maxOf { it.level }
    if (features.sumOf { it.engineLevel } <= best) throw notAvailable("You've no new features to build in.")
    if (cash < cost) throw insufficientFunds("A level ${features.sumOf { it.engineLevel }} engine costs ${money(cost)}.")
    spend(cost, Lane.GAMES)
    val name = "Engine Mk ${g.engines.size}"
    g.engineBuild = EngineBuild(name, features, weeks)
    addLog("Building $name with ${features.size} features. Ready in $weeks weeks.", LogTone.NEUTRAL, Lane.GAMES)
}

/** A sequel's concept, ready to tweak: same topic and genre, a numbered title. */
fun GameState.sequelConcept(releaseId: Int, genre: Genre? = null): GameConcept {
    val g = studio()
    val r = g.release(releaseId) ?: throw invalid("No such game.")
    val root = g.seriesRoot(r)
    val entries = g.series(root)
    val latest = entries.last()
    val spinOff = genre != null && genre != latest.concept.genre
    return latest.concept.copy(
        title = g.nextTitle(releaseId, genre), sequelOf = latest.id, engineId = g.engines.maxBy { it.level }.id,
        genre = genre ?: latest.concept.genre, genre2 = if (spinOff) null else latest.concept.genre2,
        platforms = latest.concept.platforms.filter { availablePlatform(it) }.ifEmpty { listOf(Platform.PC) },
    )
}

/** What training an employee costs in research points and cash, or why they can't train. */
fun GameState.trainingCost(e: Employee): Pair<Int, Double> = (4 + e.skill * 2) to 1_500.0 * e.skill

fun GameState.trainingBlocker(e: Employee): String? {
    if (Research.TRAINING !in researched) return "Research a training program first."
    if (e.skill >= 10) return "${e.name} is as good as it gets."
    if (week - e.lastTrainedWeek < 26) return "${e.name} trained recently."
    val (rp, cost) = trainingCost(e)
    if (researchPoints < rp) return "Needs $rp RP."
    if (cash < cost) return "Costs ${money(cost)}."
    return null
}

fun GameState.train(employeeId: Int) {
    if (ending != null) throw gameOver()
    val e = staff.firstOrNull { it.id == employeeId } ?: throw invalid("No such employee.")
    trainingBlocker(e)?.let { throw notAvailable(it) }
    val (rp, cost) = trainingCost(e)
    researchPoints -= rp
    spend(cost, null)
    e.skill += 1
    when (e.role) {
        Role.DESIGNER, Role.MARKETER -> e.design = (e.design + 1).clamped(1, 10)
        Role.ENGINEER, Role.OPERATOR -> e.tech = (e.tech + 1).clamped(1, 10)
        Role.RESEARCHER -> e.research = (e.research + 1).clamped(1, 10)
    }
    e.salary *= 1.06
    e.lastTrainedWeek = week
    e.morale = minOf(1.0, e.morale + 0.1)
    addLog("${e.name} finished a training course. Skill ${e.skill}.", LogTone.GOOD, e.lane)
}

/** License an engine to other studios: weekly royalties, but rival games get a little better. */
fun GameState.setEngineLicensed(id: Int, on: Boolean) {
    requireLive()
    val g = studio()
    val e = g.engines.firstOrNull { it.id == id } ?: throw invalid("That engine is gone.")
    if (e.level == 0) throw notAvailable("Nobody will pay for the starter kit.")
    e.licensed = on
    addLog(
        if (on) "${e.name} is now licensed to other studios. Royalties start next week." else "You stopped licensing ${e.name}.",
        LogTone.NEUTRAL, Lane.GAMES,
    )
}

// After launch

/** A patch: a few weeks of fixing what shipped broken. */
fun GameRelease.patchCost(): Double = max(2_000.0, concept.size.weeklyBudget * 2 + bugs * 150)

fun GameState.patchBlocker(id: Int): String? {
    val g = games ?: return "You don't run a game studio."
    val r = g.release(id) ?: return "That game is gone."
    if (r.patched) return "Already patched."
    if (r.publisher != null) return "${r.publisher} handles patches for that one."
    if (r.bugs < 2) return "Nothing worth patching."
    if (week - r.week > 52) return "Too long ago. Players have moved on."
    if (cash < r.patchCost()) return "A patch costs ${money(r.patchCost())}."
    return null
}

/** Fix the bugs that shipped. Players notice, and sales hold up a little longer. */
fun GameState.patchGame(id: Int) {
    requireLive()
    patchBlocker(id)?.let { throw notAvailable(it) }
    val g = studio()
    val r = g.release(id)!!
    val cost = r.patchCost()
    spend(cost, Lane.GAMES)
    val fixed = r.bugs
    r.bugs = 0.0
    r.patched = true
    if (r.weeklySales > 0) r.weeklySales *= 1.15
    g.fanTrust = minOf(1.2, g.fanTrust + 0.03)
    reputation.adjust(1.0)
    addLog("Patched ${r.title}: ${fixed.toInt()} bugs fixed for ${money(cost)}. Players notice.", LogTone.GOOD, Lane.GAMES)
}

fun GameState.expansionBlocker(id: Int): String? {
    val g = games ?: return "You don't run a game studio."
    val r = g.release(id) ?: return "That game is gone."
    if (Research.EXPANSIONS !in researched) return "Research expansion packs first."
    if (r.publisher != null) return "${r.publisher} owns that one."
    if (r.score < 6) return "Only games that scored 6 or more have an audience for more."
    if (r.expansions.size >= 2) return "Two expansions is plenty. Make a sequel."
    if (week - r.week > 104) return "Players have moved on."
    if (g.busy) return "The team is busy."
    return null
}

/** Start an expansion pack for a released game. The team works on it until it's done. */
fun GameState.startExpansion(id: Int) {
    requireLive()
    expansionBlocker(id)?.let { throw notAvailable(it) }
    val g = studio()
    val r = g.release(id)!!
    val used = r.expansions.toSet()
    val name = GameNames.expansions.filter { "${r.title}: $it" !in used }.let { rng.pick(it) }
    val x = GameExpansion(r.id, "${r.title}: $name", work = r.concept.size.work * 0.3, weeklyBudget = r.concept.size.weeklyBudget * 0.5)
    g.expansion = x
    addLog("Work starts on ${x.title}.", LogTone.NEUTRAL, Lane.GAMES)
}
