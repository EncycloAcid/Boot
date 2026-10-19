package com.boot.core

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

// Everything a game studio can do: design and start games, steer development, market, ship,
// take side work, and build engines.

internal fun GameState.studio(): GamesStudio {
    if (ending != null) throw gameOver()
    return games ?: throw notAvailable("You don't run a game studio.")
}

/** Game sizes your research allows. */
val GameState.unlockedSizes: List<GameSize> get() = GameSize.entries.filter { it.requires == null || it.requires in researched }

/** Whether a platform is on sale and open to you this week. */
fun GameState.availablePlatform(p: Platform): Boolean = when (p) {
    Platform.OWN_CONSOLE -> crossover(Combo.GAMES_HARDWARE)?.built == true && (hardware?.lifetimeUnits ?: 0) > 0
    else -> p.isAvailable(timelineWeek)
}

/** Platforms you could ship on today. */
val GameState.platformsOnSale: List<Platform> get() = Platform.entries.filter { availablePlatform(it) }

fun GameState.platformPlayers(p: Platform): Double = p.players(timelineWeek, hardware?.lifetimeUnits ?: 0)

/** Sales reach of a set of platforms, about 1 for one big console. */
fun GameState.reach(platforms: List<Platform>): Double = platforms.sumOf { platformPlayers(it) * it.spend } / 75.0

internal val GameState.factoryLeadTime: Int get() = if (Research.SUPPLY_CHAIN in researched) 8 else HardwareLab.LEAD_TIME_WEEKS

/** Staff assigned to the game studio. */
val GameState.gameTeam: Int get() = staff.count { it.lane == Lane.GAMES }

/** People on a team: while both teams are busy the game staff split by the slider; a team alone gets everyone. */
fun GameState.teamSize(team: Int): Int {
    val g = games ?: return 0
    if (Research.SECOND_TEAM !in researched) return if (team == 1) gameTeam else 0
    val second = kotlin.math.round(gameTeam * g.team2Share).toInt()
    val team1Busy = g.project != null || g.contract != null || g.expansion != null
    return if (team == 2) (if (team1Busy) second else gameTeam) else (if (g.project2 != null) gameTeam - second else gameTeam)
}

/** Split the game staff between the two teams. */
fun GameState.setTeamSplit(share: Double) {
    val g = studio()
    if (Research.SECOND_TEAM !in researched) throw notAvailable("Research a second team first.")
    g.team2Share = share.clamped(0.2, 0.8)
}

/** A balanced starting split: a third each. */
fun evenFocus(): List<MutableList<Double>> = List(3) { mutableListOf(0.5, 0.5, 0.5) }

/** Why a concept can't be started, or null if it can. */
fun GameState.conceptBlocker(
    c: GameConcept,
    deal: PublisherDeal? = null,
    team: Int = 1,
    /** Publisher deals cover platform licence fees; still check real platforms and burn cash. */
    publisherPaysFees: Boolean = false,
): String? {
    val g = games ?: return "You don't run a game studio."
    if (team == 2) {
        if (Research.SECOND_TEAM !in researched) return "Research a second team first."
        if (g.project2 != null) return "Team 2 already has a game."
        if (teamSize(2) < 2) return "Team 2 needs at least two people. Put more staff on games."
        if (c.size > GameSize.LARGE) return "AAA games need the main team."
    } else {
        if (g.project != null) return "A game is already in development."
        if (g.contract != null) return "Finish the contract first."
        if (g.expansion != null) return "Finish the expansion first."
        g.engineBuild?.let { return "The team is building ${it.name}." }
    }
    c.sequelOf?.let { g.release(it) }?.let { if (it.developer != null) return "${it.title} belongs to ${it.developer}." }
    if (c.topic !in g.topics) return "Research ${c.topic.title} first."
    for (gn in listOfNotNull(c.genre, c.genre2)) if (gn !in g.genres) return "Research the ${gn.lower} genre first."
    c.size.requires?.let { if (it !in researched) return "Research ${it.title} first." }
    if (teamSize(team) < c.size.minTeam) {
        return "${c.size.title} games need ${c.size.minTeam} people on the team. ${if (team == 2) "Team 2 has" else "You have"} ${teamSize(team)}."
    }
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
    c.remasterOf?.let { id ->
        remasterBlocker(id)?.let { return it }
        val original = g.release(id)!!
        if (c.sequelOf != null || original.concept.topic != c.topic || original.concept.genre != c.genre || original.concept.size != c.size) {
            return "A remaster keeps the original's topic, genre and size."
        }
    }
    c.antiqueIp?.let { id ->
        if (g.ownedIps.none { it.id == id }) return "You don't own that brand."
        val ip = dormantIp(id) ?: return "No such brand."
        if (calendarYear <= ip.diedYear) return "${ip.brand} isn't available to revive yet."
        if (c.ipPath == null) return "Pick a remake or a reboot."
        if (c.topic != ip.topic || c.genre != ip.genre) return "An antique revival keeps ${ip.brand}'s topic and genre."
    }
    if (c.live) {
        if (Research.LIVE_SERVICE !in researched) return "Research live service games first."
        if (c.size == GameSize.SMALL) return "Live games need to be medium or bigger."
    }
    val fees = if (publisherPaysFees) 0.0 else licenceFees(c.platforms)
    val upfront = fees + c.size.weeklyBudget * 4
    if (cash < upfront) {
        return if (publisherPaysFees) {
            "You need ${money(upfront)} for the first month of development."
        } else {
            "You need ${money(upfront)} for licence fees and the first month."
        }
    }
    return null
}

/** What the development licences for [platforms] cost: only the ones you don't hold yet. */
fun GameState.licenceFees(platforms: List<Platform>): Double {
    val held = games?.licences.orEmpty()
    return platforms.filter { it !in held }.sumOf { it.fee }
}

/** Why you can't buy [p]'s development licence now, or null if you can. */
fun GameState.licenceBlocker(p: Platform): String? {
    val g = games ?: return "You don't make games."
    if (p in g.licences) return "You already hold it."
    if (p.fee <= 0) return "${p.title} doesn't charge for one."
    if (!availablePlatform(p)) return "${p.title} isn't on sale."
    if (cash < p.fee) return "It costs ${money(p.fee)}."
    return null
}

/** Buy [p]'s development licence ahead of time. It's yours for good. */
fun GameState.buyLicence(p: Platform) {
    requireLive()
    licenceBlocker(p)?.let { throw notAvailable(it) }
    spend(p.fee, Lane.GAMES)
    games!!.licences += p
    addLog("You bought the ${p.title} development licence: ${money(p.fee)}, yours for good.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Rough weeks to finish at the current team's pace. */
fun GameState.estimatedWeeks(size: GameSize): Int = ceil(size.work / max(1.0, staffing(Lane.GAMES).output)).toInt()

/** Weeks a concept takes with today's team: remasters take half as long. */
fun GameState.estimatedWeeks(c: GameConcept): Int = ceil(c.work / max(1.0, staffing(Lane.GAMES).output)).toInt()

/** Public title invent for UI modules that cannot see the internal RNG. */
fun GameState.suggestGameTitle(topic: Topic, genre: Genre): String =
    studio().suggestTitle(rng, topic, genre, calendarYear)

fun GameState.startGame(
    concept: GameConcept,
    focus: List<MutableList<Double>> = evenFocus(),
    team: Int = 1,
    /** The engine features to build in; null for the engine's whole kit, the old way. */
    features: Set<Research>? = null,
    leads: Map<FocusArea, Int> = emptyMap(),
) {
    val g = studio()
    conceptBlocker(concept, team = team)?.let { throw notAvailable(it) }
    val fees = licenceFees(concept.platforms)
    if (fees > 0) spend(fees, Lane.GAMES)
    g.licences += concept.platforms
    val original = concept.sequelOf?.let { g.release(it) }
    var hype = minOf(25.0, g.fans / 4_000)
    if (original != null) {
        val loved = g.series(g.seriesRoot(original)).count { it.score >= 7 }
        hype += minOf(30.0, loved * 6.0 + if (original.score >= 8) 8.0 else 3.0)
    }
    concept.remasterOf?.let { g.release(it) }?.let { hype += if (it.score >= 8) 15.0 else 8.0 }
    concept.antiqueIp?.let { id ->
        dormantIp(id)?.let { ip ->
            hype += (concept.ipPath?.hype ?: 10.0) * (0.5 + ip.qualityMemory * 0.7)
        }
    }
    val promised = g.promisedSequel?.let { g.release(it) }
    if (original != null && promised != null && g.seriesRoot(promised).id == g.seriesRoot(original).id) {
        hype += 20.0
        g.promisedSequel = null
    }
    val engine = g.engine(concept.engineId)
    val project = GameProject(
        concept, focus.map { it.toMutableList() }, week, hype = hype,
        features = fitFeatures(features?.filter { it in engine.features }?.toSet(), engine)?.toMutableSet(),
        leads = leads.filterValues { id -> staff.any { it.id == id && it.lane == Lane.GAMES } }.toMutableMap(),
    )
    g.setProject(team, project)
    seedCertSchedule(project)
    val certBit = project.certClearWeek.takeIf { it > week }?.let { " Cert clears ${GameClock.label(it)}." }.orEmpty()
    addLog(
        (if (team == 2) "Team 2 started " else "Started ") + "${concept.title}: ${Format.a(concept.size.lower)} ${concept.topic.title.lowercase()} " +
            "${concept.genreLower} game for ${concept.platforms.joinToString { it.title }}." +
            (if (fees > 0) " Licence fees: ${money(fees)}." else "") + certBit,
        LogTone.NEUTRAL, Lane.GAMES,
    )
}

/**
 * Spend money on a focused playtest. It finds real bugs and gives the team feedback before launch.
 * Each development stage can be tested once, so testing is a strategic choice rather than a free button.
 */
fun GameState.playtestGame(team: Int = 1) {
    val g = studio()
    val p = g.projectOf(team) ?: throw invalid("Nothing in development to playtest.")
    val stage = if (p.isReady) 3 else p.stage
    if (stage in p.playtestedStages) throw busy("You've already playtested ${p.title} during this stage.")
    val cost = p.concept.size.weeklyBudget * (1.5 + stage * 0.5)
    if (cash < cost) throw insufficientFunds("A playtest costs ${money(cost)}. You have ${money(cash)}.")
    spend(cost, Lane.GAMES)
    val bugsBefore = p.bugs
    val fixed = minOf(p.bugs, maxOf(1.0, p.bugs * 0.35))
    p.bugs = (p.bugs - fixed).coerceAtLeast(0.0)
    // Feedback turns into better decisions, represented by small, tangible design and tech gains.
    p.design += p.work * 0.015
    p.tech += p.work * 0.01
    p.playtestedStages.add(stage)
    val stageName = when (stage) {
        0 -> "foundations"
        1 -> "content"
        2 -> "presentation"
        else -> "final polish"
    }
    addLog(
        "Playtested ${p.title} during $stageName: fixed ${Format.count(fixed)} bugs and improved the build's design and tech. " +
            "Cost ${money(cost)}" +
            if (bugsBefore < 1.0) ". Few bugs were found, but feedback still improved the build." else ".",
        LogTone.GOOD, Lane.GAMES,
    )
}

/** Set the weekly creative direction for the game in development. */
fun GameState.setCreativeFocus(focus: GameFocus, team: Int = 1) {
    val p = studio().projectOf(team) ?: throw invalid("Nothing in development.")
    if (p.creativeFocus == focus) return
    p.creativeFocus = focus
    addLog(
        "${p.title} is now ${focus.title.lowercase()}: ${focus.blurb}",
        LogTone.NEUTRAL, Lane.GAMES,
    )
}

/** Set how the team splits its time in one stage. Values are relative weights. */
fun GameState.setStageFocus(stage: Int, allocation: List<Double>, team: Int = 1) {
    val p = studio().projectOf(team) ?: throw invalid("Nothing in development.")
    if (stage !in 0..2 || allocation.size != 3) throw invalid("Three areas per stage.")
    p.focus[stage].clear()
    p.focus[stage].addAll(allocation.map { it.clamped(0.0, 1.0) })
}

fun GameState.cancelGame(team: Int = 1) {
    val g = studio()
    val p = g.projectOf(team) ?: throw invalid("Nothing in development.")
    coDevOnCancel(p)
    g.coDevOffers.removeAll { it.team == team }
    g.setProject(team, null)
    addLog("Cancelled ${p.title} after ${p.weeksInDev} weeks.", LogTone.BAD, Lane.GAMES)
}

fun GameState.buyCampaign(c: Campaign, team: Int = 1) {
    val g = studio()
    if (Research.MARKETING !in researched) throw notAvailable("Research marketing campaigns first.")
    val p = g.projectOf(team) ?: throw invalid("Nothing to market.")
    if (c in p.campaigns) throw busy("You've already run a ${c.title.lowercase()} for ${p.title}.")
    if (cash < c.cost) throw insufficientFunds("${c.title} costs ${money(c.cost)}.")
    spend(c.cost, Lane.GAMES)
    p.campaigns += c
    p.hype = minOf(100.0, p.hype + c.hype)
    addLog("${c.title} for ${p.title}. Hype is building.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Expected critic score if you shipped [team]'s project today. */
fun GameState.gameForecast(team: Int = 1): LaunchForecast? {
    val g = games ?: return null
    val p = g.projectOf(team) ?: return null
    val suited = p.concept.platforms.any { it.suits(p.concept.audience) }
    return g.forecast(context(Lane.GAMES), suited, context(Lane.GAMES).workOutput, team)
}

fun GameState.launchGame(team: Int = 1) {
    val g = studio()
    val signed = g.projectOf(team) ?: throw invalid("No game in development.")
    launchCertBlocker(team)?.let { throw notAvailable(it) }
    // An exclusive ships only on its maker's platforms.
    val exclusive = signed.exclusive
    val held = exclusive?.let { e -> signed.concept.platforms.filter { it.maker != e.maker } }.orEmpty()
    if (exclusive != null && held.isNotEmpty()) g.setProject(team, signed.copy(concept = exclusiveConcept(signed)))
    // An Adults Only game only goes out where it's carried.
    val rated = calendarYear >= RATINGS_YEAR
    g.projectOf(team)?.let { q ->
        val keep = adultsOnlyPlatforms(q.concept)
        if (rated && q.uncut && keep.isNotEmpty() && keep.size < q.concept.platforms.size) g.setProject(team, q.copy(concept = q.concept.copy(platforms = keep)))
    }
    val p = g.projectOf(team)!!
    val suited = p.concept.platforms.any { it.suits(p.concept.audience) }
    // Pirates take a share of a popular game; many shops won't stock an Adults Only one.
    val piracy = piracyLoss(p.concept, p.protection, popularity(gameForecast(team)?.expectedScore ?: 6.0, p.hype))
    val shelf = if (rated && p.uncut) adultsOnlyShelf(calendarYear) else 1.0
    val (storeLift, pcKeep) = storeLaunchTerms(p)
    val politicsLift = 1.0 + platformPoliticsSalesLift(p.concept.platforms, p.stores)
    val politicsKeep = platformPoliticsKeep(p.concept.platforms, p.stores)
    val report = g.launch(
        context(Lane.GAMES), rng, reach(p.concept.platforms), suited, g.clashFor(p.concept, week), team,
        kept = (1 - piracy) * shelf * storeLift * politicsLift * politicsKeep, pcKeep = pcKeep * politicsKeep, company = companyName,
    )
    g.releases.firstOrNull()?.let { r ->
        r.rating = if (rated) ratingFor(r.concept.audience, p.uncut) else null
        afterPiracy(p, r, 1 - piracy, shelf)
        afterStores(p, r)
    }
    if (exclusive != null) g.releases.firstOrNull()?.let { afterExclusiveLaunch(exclusive, held, it) }
    g.releases.firstOrNull()?.let { r ->
        applyCriticRelations(r)
        resolveAntiqueShip(r)
        applyAccessLocalShip(r, p)
        museumOnShip(r)
        coDevOnLaunch(p, r)
        val sc = String.format(java.util.Locale.US, "%.1f", r.score)
        val outlet = rng.pick(listOf("IGN", "GameSpot", "Kotaku", "Polygon"))
        val words = flavour("${r.title} headline")
        val headline = "${r.title} review: " + words.pick(
            when {
                r.score >= 9 -> listOf("a masterpiece", "an instant classic", "$companyName's finest hour", "believe the hype", "one for the ages")
                r.score >= 8 -> listOf("$companyName has a hit", "a must-play", "easy to love", "the real deal", "a triumph for $companyName")
                r.score >= 6.5 -> listOf("solid, if not spectacular", "good fun with rough edges", "worth a look", "promising, not perfect", "a decent night in")
                r.score >= 5 -> listOf("a mixed bag", "so close, and yet", "uneven but interesting", "wait for a sale", "it never quite clicks")
                else -> listOf("avoid", "a misfire", "not ready", "a costly mistake", "back to the drawing board")
            },
        )
        // A roundup of the critics, and the line everyone will quote.
        val roundup = r.reviews.take(4).joinToString(" · ") { "${it.outlet.title} ${it.formatted}${if (it.outlet.scale.isNotEmpty()) it.outlet.scale else ""}" }
        val body = "$companyName's ${r.concept.size.lower} ${r.concept.topic.title.lowercase()} ${r.concept.genreLower} game averages $sc/10. $roundup." +
            (if (r.userScore > 0) " Players: ${String.format(java.util.Locale.US, "%.1f", r.userScore)}." else "")
        val quoted = r.reviews.maxByOrNull { if (r.score >= 6.5) it.exact else -it.exact }
        val tone = if (r.score >= 8) LogTone.GOOD else if (r.score < 5) LogTone.BAD else LogTone.NEUTRAL
        afterEditions(p, r)
        feedOnLaunch(r)
        for (desk in listOf(NewsDesk.GAMES, NewsDesk.YOU)) {
            publish(
                desk, outlet, headline, body, tone, Lane.GAMES, lead = r.score >= 9,
                tag = NewsTag.REVIEW, quote = quoted?.comment, quoteBy = quoted?.outlet?.title, cover = r.newsCover(),
            )
        }
    }
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
        title = g.suggestTitle(rng, d.topic, d.genre, calendarYear), topic = d.topic, genre = d.genre, platforms = listOf(d.platform), size = d.size,
        engineId = g.engines.maxBy { it.level }.id,
    )
    val wasKnown = d.topic in g.topics
    if (!wasKnown) g.topics += d.topic
    // Check the deal's real platform; the publisher covers licence fees.
    val blocker = conceptBlocker(concept, d, publisherPaysFees = true)
    if (blocker != null) {
        if (!wasKnown) g.topics -= d.topic
        throw notAvailable(blocker)
    }
    g.dealOffers.remove(d)
    val project = GameProject(concept, focus.map { it.toMutableList() }, week, hype = 20.0, deal = d)
    g.project = project
    seedCertSchedule(project)
    cash += d.advance * 0.5
    recordRevenue(d.advance * 0.5, Lane.GAMES)
    addLog(
        "Signed with ${d.publisher}: ${Format.a(d.size.lower)} ${d.topic.title.lowercase()} ${d.genre.lower} game " +
            "for ${d.platform.title}. ${money(d.advance * 0.5)} up front, the rest if it scores ${d.minScore}+.",
        LogTone.GOOD, Lane.GAMES,
    )
}

/**
 * What a new engine with [selected] features (or everything researched) would cost.
 * Cost uses raw feature levels; the finished engine's [Engine.level] applies combo rules.
 */
fun GameState.engineQuote(selected: List<Research>? = null): Triple<List<Research>, Double, Int> {
    val raw = (selected ?: researched.filter { it.kind == Research.Kind.ENGINE }).sortedBy { it.ordinal }
    // Autopilot / one-click build: skip style clashes. Explicit picks keep the soft penalty.
    val features = if (selected == null) pruneEngineStyleClashes(raw) else raw
    val combo = engineCombo(features)
    val cost = 20_000 + 9_000 * combo.rawLevel.toDouble().pow(1.35)
    // Build time tracks raw feature load; clashes still cut the finished effective level.
    return Triple(features, cost, 3 + combo.rawLevel / 3)
}

/** Expected engine standard reviewers use this week (shown in the builder without spoiling feature levels). */
fun GameState.expectedEngineStandard(): Double = engineStandard(week)

fun GameState.buildEngine(selected: List<Research>? = null) {
    val g = studio()
    if (Research.CUSTOM_ENGINES !in researched) throw notAvailable("Research custom engines first.")
    if (g.engineBuild != null) throw busy("An engine is already being built.")
    if (g.project != null || g.contract != null || g.expansion != null) throw busy("The team's busy. Build an engine between games: it takes the whole main team.")
    val (features, cost, weeks) = engineQuote(selected)
    if (features.isEmpty()) throw notAvailable("Research some engine features first.")
    val best = g.engines.maxOfOrNull { it.level } ?: 0
    val bestRaw = g.engines.maxOfOrNull { it.rawLevel } ?: 0
    val combo = engineCombo(features)
    if (combo.level <= best && combo.rawLevel <= bestRaw) {
        if (combo.notes.isNotEmpty()) {
            throw notAvailable("Style clashes keep this pack at level ${combo.level}. Drop a conflicting style, or research more tech.")
        }
        throw notAvailable("You've no new features to build in.")
    }
    if (cash < cost) throw insufficientFunds("A level ${combo.level} engine costs ${money(cost)}.")
    spend(cost, Lane.GAMES)
    val name = "Engine Mk ${g.engines.size}"
    g.engineBuild = EngineBuild(name, features, weeks)
    val note = combo.notes.takeIf { it.isNotEmpty() }?.joinToString(" ")?.let { " $it" } ?: ""
    val risk = if (combo.bugRisk >= 0.04) " Games on it may ship a little buggier." else ""
    addLog(
        "Building $name with ${features.size} features (effective level ${combo.level}" +
            (if (combo.level != combo.rawLevel) ", raw ${combo.rawLevel}" else "") +
            "). Ready in $weeks weeks.$note$risk",
        LogTone.NEUTRAL, Lane.GAMES,
    )
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
        title = g.nextTitle(releaseId, genre, year = calendarYear), sequelOf = latest.id, engineId = g.engines.maxBy { it.level }.id,
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
    if (r.developer != null) return "${r.developer} handles patches for that one."
    if (r.bugs < 2) return "Nothing worth patching."
    if (week - r.week > 52) return "Too long ago. Players have moved on."
    if (cash < r.patchCost()) return "A patch costs ${money(r.patchCost())}."
    return null
}

/**
 * Fix the bugs that shipped. Players raise their score, sales get a bump that scales with how
 * broken it was, and a live game's community comes back a little.
 */
fun GameState.patchGame(id: Int) {
    requireLive()
    patchBlocker(id)?.let { throw notAvailable(it) }
    val g = studio()
    val r = g.release(id)!!
    val cost = r.patchCost()
    spend(cost, Lane.GAMES)
    val fixed = r.bugs
    val severity = (fixed / max(1.0, r.concept.size.work * 0.6)).coerceIn(0.0, 1.0)
    r.bugs = 0.0
    r.patched = true
    val beforeUser = r.userScore
    if (r.userScore > 0) {
        // Recover the bug hit on word of mouth: ~0.8 when bugPenalty was under 0.9, plus severity.
        r.userScore = minOf(10.0, r.userScore + (0.4 + severity * 1.2).coerceIn(0.3, 1.6))
        // First patch settles the early player score into the public number.
        r.userScoreSettled = true
    }
    if (r.weeklySales > 0) r.weeklySales *= (1.1 + severity * 0.35).coerceIn(1.1, 1.5)
    r.live?.takeIf { !it.closed }?.let { live ->
        live.players *= 1 + 0.06 + severity * 0.12
        live.peakPlayers = maxOf(live.peakPlayers, live.players)
    }
    g.fanTrust = minOf(1.2, g.fanTrust + 0.03 + severity * 0.04)
    reputation.adjust(1.0)
    val scoreNote = if (beforeUser > 0 && r.userScore > beforeUser + 0.05) {
        " Player score " + String.format(java.util.Locale.US, "%.1f → %.1f.", beforeUser, r.userScore)
    } else {
        " Players notice."
    }
    addLog("Patched ${r.title}: ${fixed.toInt()} bugs fixed for ${money(cost)}.$scoreNote", LogTone.GOOD, Lane.GAMES)
}

/** Lock in the public player score after the early window (bugs / patches can still move it). */
internal fun GameState.finalizeUserScore(r: GameRelease) {
    if (r.userScoreSettled || r.userScore <= 0) return
    var score = r.userScore
    val work = r.concept.size.work
    if (r.bugs > work * 0.08) score -= 0.35
    else if (r.bugs < work * 0.02) score += 0.15
    if (r.patched) score += 0.25
    score += rng.normal(sd = 0.2)
    r.userScore = kotlin.math.round(score.coerceIn(1.0, 10.0) * 10) / 10.0
    r.userScoreSettled = true
}

fun GameState.expansionBlocker(id: Int): String? {
    val g = games ?: return "You don't run a game studio."
    val r = g.release(id) ?: return "That game is gone."
    if (Research.EXPANSIONS !in researched) return "Research expansion packs first."
    if (r.publisher != null) return "${r.publisher} owns that one."
    if (r.developer != null) return "${r.developer} owns that one."
    if (r.score < 6) return "Only games that scored 6 or more have an audience for more."
    if (r.expansions.size >= 3) return "Three expansions is plenty. Make a sequel."
    if (week - r.week > 104) return "Players have moved on."
    if (g.busy) return "The team is busy."
    return null
}

/** Start an expansion pack for a released game. The team works on it until it's done. */
fun GameState.startExpansion(id: Int, scope: ExpansionScope = ExpansionScope.EXPANSION) {
    requireLive()
    expansionBlocker(id)?.let { throw notAvailable(it) }
    val g = studio()
    val r = g.release(id)!!
    val used = r.expansions.toSet()
    val name = expansionTitles(r.concept.topic, r.id.toLong()).filter { "${r.title}: $it" !in used }.let { rng.pick(it) }
    val x = GameExpansion(r.id, "${r.title}: $name", work = r.concept.size.work * scope.work, weeklyBudget = r.concept.size.weeklyBudget * scope.budget, scope = scope)
    g.expansion = x
    addLog("Work starts on ${x.title}.", LogTone.NEUTRAL, Lane.GAMES)
}
