package com.boot.core

import kotlin.math.max
import kotlin.math.min

/** Simulate one week. */
fun GameState.advance() {
    if (ending != null) throw gameOver()
    if (needsDecision) throw decisionPending()

    week += 1
    ledger = (if (ledger.size >= GameState.LENGTH_WEEKS + 10) ledger.drop(1) else ledger) + WeekLedger(week)

    updateEra()
    eraWeek()
    historyWeek()
    updateHistoricRivals()
    updateRivals()
    rivalWeek()
    inboxWeek()

    // Combos first: they shape every lane's week.
    val fx = ComboEffects()
    for (c in crossovers) c.applyTo(fx, this)
    web?.let { it.clientWariness = max(fx.webWariness, it.clientWariness * 0.98) }

    for (lane in lanes) {
        val ctx = context(lane, fx)
        val report = when (lane) {
            Lane.GAMES -> {
                prepareLeads()
                games!!.tick(ctx, rng)
            }
            Lane.WEBSITES -> web!!.tick(ctx, rng)
            Lane.APPS -> apps!!.tick(ctx, rng)
            Lane.SOCIAL -> social!!.tick(ctx, rng)
            Lane.HARDWARE -> hardware!!.tick(ctx, rng)
            Lane.AI -> ai!!.tick(ctx, rng)
            Lane.OS -> os!!.tick(ctx, rng)
        }
        applyReport(report, lane)
        val lanePayroll = staff(lane).sumOf { it.salary }
        recordCost(lanePayroll, lane)
        cash -= lanePayroll
    }

    studioWeek()
    roadmapWeek()
    marketingWeek()
    strategyWeek()
    newsWeek()
    researchPoints += weeklyResearch(games?.lastWork ?: 0.0)

    // Unassigned staff still get paid.
    val idle = staff.filter { it.lane == null }.sumOf { it.salary }
    spend(idle + headquarters.rent + perkUpkeep, null)
    payLoan()

    updateCrossovers(fx)

    if (founder.freelancing) {
        val gig = founder.freelanceIncome(week)
        cash += gig
        recordRevenue(gig, null)
    }

    stats.peakCash = max(stats.peakCash, cash)
    stats.peakValuation = max(stats.peakValuation, valuation)
    tradeWeek()
    stockMarketWeek()
    if (listing == null) quarterlyResults()
    ledger.last().cash = cash

    updateFounder()
    updateStaff(fx)
    talentWeek()
    reputation.weeklyDrift()
    if (week % 4 == 0) refreshTalentPool()
    candidates = candidates.filterNot { it.contestedBy != null && it.expiresWeek < week }

    rollWorldEvents()
    scenarioWeek()
    tutorialWeek()
    boardReview()
    checkPivotPoint()
    updateStreaks()
    checkGoals()
    checkAchievements()
    checkEndings()
}

/** Advance until a decision is needed, the game ends, or [weeks] pass. Returns weeks simulated. */
fun GameState.advance(weeks: Int): Int {
    var n = 0
    while (n < weeks && !needsDecision && ending == null) {
        advance()
        n += 1
    }
    return n
}

private fun GameState.applyReport(report: LaneReport, lane: Lane) {
    cash += report.revenue - report.expenses
    recordRevenue(report.revenue, lane)
    recordCost(report.expenses, lane)
    handle(report.incidents, lane)
}

internal fun GameState.handle(incidents: List<Incident>, lane: Lane?) {
    for (incident in incidents) {
        when (incident) {
            is Incident.Log -> {
                addLog(incident.text, incident.tone, lane)
                if (incident.tone == LogTone.MILESTONE && lane != null && lane != Lane.GAMES) {
                    publish(NewsDesk.YOU, techOutlet(lane, rng), incident.text.substringBefore(". ").removeSuffix("."), "$companyName · ${lane.businessName}", LogTone.GOOD, lane)
                }
            }
            is Incident.Reputation -> reputation.adjust(incident.delta)
            is Incident.Scar -> {
                reputation.scar(incident.kind, incident.severity, week, incident.note)
                museumOnScar(incident.kind, incident.note)
            }
            is Incident.Event -> enqueue(incident.kind)
            is Incident.Inbox -> offer(incident.kind, incident.weeks)
            is Incident.Research -> researchPoints += incident.points
            is Incident.Stat -> when (incident.key) {
                StatKey.HIT -> {
                    stats.hits += 1
                    founderXp(15.0)
                }
                StatKey.FLOP -> {
                    stats.flops += 1
                    founderXp(4.0)
                }
                StatKey.RECALL -> stats.recalls += 1
                StatKey.SCANDAL -> stats.scandals += 1
                StatKey.RELEASE -> {
                    stats.releases += 1
                    stats.releasesThisQuarter += 1
                    founderXp(10.0)
                    if (lane == Lane.GAMES) {
                        games?.releases?.firstOrNull()?.let { creditCareerOnShip(it.title, it.score) }
                    }
                }
                StatKey.SOLD_OUT -> stats.soldOuts += 1
                StatKey.OVERSTOCK -> stats.overstocks += 1
            }
        }
    }
}

// World

private fun GameState.updateEra() {
    val w = world
    val progress = (week - w.eraStartWeek).toDouble() / w.eraLength
    if (progress >= 1.0) {
        startNextEra()
        return
    }
    val phase = when {
        w.era == Era.BOOTSTRAP_YEARS -> EraPhase.PEAK
        progress < 0.2 -> EraPhase.DAWN
        progress > 0.75 -> EraPhase.FADING
        else -> EraPhase.PEAK
    }
    w.eraIntensity = when (phase) {
        EraPhase.DAWN -> 0.35 + 0.65 * progress / 0.2
        EraPhase.PEAK -> 1.0
        EraPhase.FADING -> 1 - 0.5 * (progress - 0.75) / 0.25
    }
    if (phase != w.phase) {
        w.phase = phase
        when (phase) {
            EraPhase.PEAK -> if (w.era != Era.BOOTSTRAP_YEARS) {
                publish(NewsDesk.INDUSTRY, "The Economist", "${w.era.title} is at its height", w.era.blurb, lead = true)
            }
            EraPhase.FADING -> {
                val next = pickNextEra()
                w.nextEra = next
                publish(NewsDesk.INDUSTRY, "The Economist", "Is ${w.era.inSentence} over?", "What people are talking about now: ${next.rumor}")
                addLog("${w.era.title} is fading. The talk is of something new: ${next.rumor}", LogTone.NEUTRAL)
            }
            EraPhase.DAWN -> {}
        }
    }
    // While an era fades, the rumours of the next get louder.
    val next = w.nextEra
    if (phase == EraPhase.FADING && next != null && rng.chance(0.05)) {
        publish(NewsDesk.INDUSTRY, techOutlet(null, rng), "What comes after ${w.era.inSentence}?", next.rumor)
    }
}

private fun GameState.pickNextEra(): Era {
    // A run that started in the past follows history until the present.
    historicalEraAfter()?.let { return it.first }
    val pool = Era.entries.filter { it != Era.BOOTSTRAP_YEARS && !it.historical && it != world.era }
    val unseen = pool.filter { it !in world.pastEras }
    return rng.pick(unseen.ifEmpty { pool })
}

private fun GameState.startNextEra() {
    val w = world
    val next = historicalEraAfter()?.first ?: w.nextEra ?: pickNextEra()
    w.history += closeEra()
    w.pastEras += w.era
    w.era = next
    w.eraStartWeek = week
    w.eraLength = historicalEraAfter(now = true)?.let { (_, end) -> (end - calendarYear) * 52 - week.mod(52) } ?: rng.int(78, 130)
    w.phase = EraPhase.DAWN
    w.eraIntensity = 0.35
    w.nextEra = null
    if (next !in stats.erasSeen) stats.erasSeen += next
    val mine = lanes.map { next.demand(it) }
    val tone = when {
        mine.any { it > 1.05 } -> LogTone.GOOD
        mine.any { it < 0.95 } -> LogTone.BAD
        else -> LogTone.NEUTRAL
    }
    addLog("A new era begins: ${next.title}. ${next.blurb}", tone)
    announceEraResearch()
    publish(NewsDesk.INDUSTRY, "The Economist", "${next.emoji} ${next.title} begins", next.blurb, tone, lead = true)
    beginEra()
}

private fun GameState.updateRivals() {
    val active = world.rivals.filter { !it.absorbed && it.quietUntil <= week }
    val headlineKey = world.gamesHeadline?.let { canonicalStudio(it) }
    for (r in active) {
        // Games roster only when you make games; non-headline roster drifts on private dice.
        if (r.roster && Lane.GAMES !in lanes) continue
        val isExtra = r.roster && headlineKey != null && canonicalStudio(r.name) != headlineKey
        if (isExtra) {
            val dice = flavour("rival-tick ${r.id}")
            val target = (0.5 * world.demand(r.lane)).clamped(0.2, 0.9)
            r.strength = (r.strength + (target - r.strength) * 0.02 + dice.normal(sd = 0.01)).clamped(0.05, 1.0)
            continue
        }
        val target = (0.5 * world.demand(r.lane)).clamped(0.2, 0.9)
        r.strength = (r.strength + (target - r.strength) * 0.02 + rng.normal(sd = 0.01)).clamped(0.05, 1.0)

        if (rng.chance(0.008)) {
            val who = if (rng.chance(0.5)) r.name else rng.pick(if (isDecades) peersIn(r.lane, calendarYear) else World.peers(r.lane))
            val headline = rivalMove(who, r.lane)
            if (r.lane in lanes) {
                r.strength = min(1.0, r.strength + 0.1)
                val move = "$headline. Your market just got tougher."
                r.lastMove = move
                addLog(move, LogTone.BAD, r.lane)
            }
            publish(NewsDesk.INDUSTRY, techOutlet(r.lane, rng), headline, if (r.lane in lanes) "A rival in your market." else "", lane = r.lane)
        }

        // Rivals compete for the same talent — stars under pressure first; loyalty resists.
        tryRivalPoach(r)
    }
}

/** Trends, the yearly convention and new offers for a game studio. */
private fun GameState.studioWeek() {
    val g = games ?: return
    val t = g.trend
    if (t == null || week >= t.until) {
        // Most of the time the era decides what's hot; now and then something comes out of nowhere.
        val next = (eraTrend()?.takeIf { rng.chance(0.75) }
            ?: if (rng.chance(0.6)) Trend(topic = rng.pick(Topic.entries), until = 0) else Trend(genre = rng.pick(Genre.entries), until = 0))
            .copy(until = week + rng.int(40, 70))
        if (next.title != t?.title) addLog("Trend: gamers can't get enough of ${next.lower} games.", LogTone.NEUTRAL, Lane.GAMES)
        g.trend = next
    }
    if (GameClock.weekOfYear(week) == SHOW_WEEK) enqueue(EventKind.Convention)
    showSeasonWeek()
    wishlistWeek(g)
    releaseDayWeek(g)
    showMomentsWeek(g)
    dealWeek(g)
    milestoneWeek(g)
    exclusiveWeek(g)
    storeWeek(g)
    modWeek(g)
    transparencyWeek(g)
    screenWeek(g)
    ratingsWeek(g)
    bombWeek(g)
    criticStandingWeek(g)
    collectionWeek(g)
    teamWeek(g)
    portWeek(g)
    chartsStory()
    feedWeek()
    marketWeek()
    subsidiaryWeek()
    stakesWeek(g)
    earnOutWeek(g)
    studioMarketWeek(g)
    ownedStudioWeek(g)
    publishingWeek()
    esportsWeek()
    // Pushy stores get noticed.
    if (week % 13 == 0 && g.liveGames.any { it.live!!.monetization == Monetization.AGGRESSIVE && it.live!!.players > 10_000 }) {
        reputation.scar(ScarKind.PREDATORY_MONETIZATION, 0.03, week, "Pay-to-win store")
    }
    // Early player scores settle after three weeks of word of mouth (or sooner via patch).
    for (r in g.releases) {
        if (!r.userScoreSettled && r.userScore > 0 && week - r.week >= 3) {
            finalizeUserScore(r)
        }
    }
    // Merchandise for the hits, fading over a couple of years.
    if (Research.MERCHANDISE in researched) {
        val merch = g.releases.filter { it.score >= 8 && it.publisher == null }
            .sumOf { it.unitsSold * 0.006 * kotlin.math.exp(-(week - it.week) / 104.0) }
        g.lastMerch = merch
        if (merch > 0) {
            cash += merch
            recordRevenue(merch, Lane.GAMES)
        }
    }
    val output = staffing(Lane.GAMES).output
    g.refreshOffers(
        week, calendarYear, output, rng, unlockedSizes,
        Platform.entries.filter { availablePlatform(it) && it != Platform.OWN_CONSOLE },
    )
}

private fun GameState.rollWorldEvents() {
    for (lane in lanes) {
        if (rng.chance(0.0025) && hasLiveProduct(lane)) enqueue(EventKind.ViralMoment(lane))
    }
    rollSmallEvents()
    rollDecisions()
    storyWeek()
    if (pivot != null && valuation >= 2_000_000 && rng.chance(0.005)) {
        val bidder = if (rng.chance(0.5)) {
            rng.pick(world.rivals).name
        } else {
            rng.pick(listOf("Microsoft", "Tencent", "Amazon", "Blackstone", "Sony"))
        }
        enqueue(EventKind.Buyout(bidder, Math.round(valuation * rng.range(1.1, 1.6)).toDouble()))
    }
}

/** The everyday surprises: people, fans, leaks, refunds and outages. */
private fun GameState.rollSmallEvents() {
    if (staff.size >= 6 && rng.chance(0.003)) enqueue(EventKind.Offsite)
    if (web != null && staff(Lane.WEBSITES).isNotEmpty() && rng.chance(0.006)) {
        val pay = max(15_000.0, trailingRevenue(Lane.WEBSITES, 4) / 4 * 3)
        enqueue(EventKind.RushJob(rng.pick(brandsIn(calendarYear) + Names.clients), Math.round(pay / 1_000.0) * 1_000.0))
    }
    if (apps?.isLive == true && rng.chance(0.004)) enqueue(EventKind.AppFeatured)
    if (social?.isLive == true && social?.caughtOn == true && rng.chance(0.004)) {
        enqueue(EventKind.Celebrity(rng.pick(listOf("Taylor Swift", "MrBeast", "Zendaya", "Keanu Reeves", "Rihanna", "Lionel Messi", "Bad Bunny"))))
    }
    if ((ai?.capability ?: 0.0) > 10 && rng.chance(0.004)) enqueue(EventKind.OpenSource)
    if (staff.isNotEmpty() && rng.chance(0.006 + 0.0015 * min(staff.size, 10))) {
        staff.filter { week - maxOf(it.hiredWeek, it.lastRaiseWeek) >= 52 && it.skill >= 5 }
            .takeIf { it.isNotEmpty() }
            ?.let { enqueue(EventKind.RaiseRequest(rng.pick(it).id)) }
    }
    games?.let { g ->
        val hot = g.releases.filter { it.weeklySales > 0 && it.score >= 6.5 }
        // Streamers came along with YouTube and Twitch.
        if (hot.isNotEmpty() && calendarYear >= 2007 && rng.chance(0.01)) {
            enqueue(EventKind.Streamer(rng.pick(hot).id, rng.pick(listOf("xXPixelQueenXx", "Big Dave Plays", "Mira Streams", "TheLagWizard", "Cozy Kat"))))
        }
        val p = g.project
        if (p != null && !p.isReady && p.stage >= 1 && rng.chance(0.004)) enqueue(EventKind.LeakedBuild)
        if (Research.SEQUELS in researched && g.promisedSequel == null) {
            val loved = g.releases.filter {
                it.score >= 8 && it.id !in g.petitions && week - it.week in 8..104 &&
                    g.releases.none { other -> other.concept.sequelOf == it.id }
            }
            if (loved.isNotEmpty() && rng.chance(0.015)) {
                val r = rng.pick(loved)
                g.petitions += r.id
                enqueue(EventKind.FanPetition(r.id))
            }
        }
    }
    if (rng.chance(0.003)) enqueue(EventKind.GameJam)
    if (GameClock.weekOfYear(week) == 14 && week > 52) {
        val credit = Math.round(payroll * 52 * 0.05 / 100) * 100.0
        if (credit >= 1_000 && rng.chance(0.6)) enqueue(EventKind.TaxCredit(credit))
    }
    for (lane in listOf(Lane.APPS, Lane.SOCIAL)) {
        val live = (lane == Lane.APPS && apps?.isLive == true) || (lane == Lane.SOCIAL && social?.isLive == true)
        if (live && rng.chance(0.004)) enqueue(EventKind.ServerOutage(lane))
    }
    if (freeDesks > 0 && week > 26 && rng.chance(0.004)) enqueue(EventKind.Headhunter)
}

private fun GameState.hasLiveProduct(lane: Lane): Boolean = when (lane) {
    Lane.GAMES -> games?.releases?.isNotEmpty() == true
    Lane.WEBSITES -> (web?.completed ?: 0) > 0
    Lane.APPS -> apps?.isLive == true
    Lane.SOCIAL -> social?.isLive == true
    Lane.HARDWARE -> hardware?.onMarket != null
    Lane.AI -> (ai?.capability ?: 0.0) > 0
    Lane.OS -> os?.releases?.isNotEmpty() == true
}

// Combos

private fun GameState.updateCrossovers(fx: ComboEffects) {
    recordRevenue(fx.revenue, null)
    cash += fx.revenue
    spend(fx.cost, null)

    for (c in crossovers) {
        if (c.isBuilding) {
            c.buildWeeksLeft -= 1
            if (c.buildWeeksLeft <= 0) {
                c.built = true
                addLog("${c.combo.product} is live. ${c.combo.pitch}", LogTone.MILESTONE)
            }
        }
        c.pressure += c.pressureRate(this)
        if (c.pressure >= 1) {
            c.pressure = 0.5
            enqueue(EventKind.ComboProblem(c.combo))
        }
    }

    if (isEcosystem) {
        stats.ecosystemPressure += if (stats.ecosystemOpened) 0.002 else 0.006
        if (stats.ecosystemPressure >= 1) enqueue(EventKind.EcosystemAntitrust)
    }
}

// People

private fun GameState.updateFounder() {
    if (crunch) stats.crunchWeeks += 1
    founderXp(1.0)
    for (lane in lanes) {
        founder.experience[lane] = (founder.experience[lane] ?: 0.0) + founder.attentionShare(lane, lanes)
    }
}

private fun GameState.updateStaff(fx: ComboEffects) {
    val quitters = mutableListOf<Employee>()
    for (e in staff) {
        var m = e.morale
        // Team players lift everyone they work with.
        val lift = minOf(0.08, 0.04 * staff.count { it.lane == e.lane && it.id != e.id && it.trait == Trait.TEAM_PLAYER })
        val boss = (if (founderIs(FounderTrait.PEOPLE_PERSON)) 0.08 else 0.0) + (if (founderHas(FounderSkill.STEADY_HAND)) 0.04 else 0.0)
        val settle = minOf(0.97, 0.7 + perks.sumOf { it.morale } + (e.trait?.settle ?: 0.0) + lift + boss)
        m += (settle - m) * 0.05
        if (crunch && e.trait != Trait.NIGHT_OWL) m -= if (has(Perk.NAP_PODS)) 0.0125 else 0.025
        if (cash < 0) m -= 0.03
        if (e.lane == Lane.WEBSITES) m -= fx.moraleDrain
        if (staff.size > office.desks) m -= 0.02
        e.morale = m.clamped(0.0, 1.0)
        val quitChance = (if (has(Perk.DAYCARE)) 0.02 else 0.04) * (if (e.trait == Trait.DIVA) 2.0 else 1.0) *
            (if (founderHas(FounderSkill.INSPIRING)) 0.5 else 1.0)
        if (e.morale < 0.3 && e.trait != Trait.LOYAL && rng.chance(quitChance)) quitters += e
    }
    gainExperience()
    for (q in quitters) {
        staff = staff.filterNot { it.id == q.id }
        addLog("${q.name} quit. Morale had been low for a while.", LogTone.BAD, q.lane)
    }
}

/** At the end of each quarter, the numbers. (Listed companies report earnings instead.) */
private fun GameState.quarterlyResults() {
    if (week % GameClock.WEEKS_PER_QUARTER != 0 || week < GameClock.WEEKS_PER_QUARTER) return
    val revenue = trailingRevenue(weeks = 13)
    if (revenue <= 0) return
    val previous = trailingRevenue(weeks = 13, offset = 13)
    val profit = trailingProfit(weeks = 13)
    val quarter = (GameClock.weekOfYear(week - 1) - 1) / 13 + 1
    val growth = if (previous > 0) " (${if (revenue >= previous) "+" else ""}${((revenue / previous - 1) * 100).toInt()}%)" else ""
    addLog(
        "Q$quarter results: revenue ${money(revenue)}$growth, ${if (profit >= 0) "profit" else "loss"} ${money(kotlin.math.abs(profit))}.",
        if (profit >= 0) LogTone.GOOD else LogTone.NEUTRAL,
    )
}

/** People get better by doing the work. */
private fun GameState.gainExperience() {
    val ups = mutableListOf<Int>()
    val shipping = games?.busy == true
    for (e in staff) {
        val lane = e.lane ?: continue
        if (e.maxLevel) continue
        var gain = 0.6 + 0.4 * e.morale
        if (crunch) gain *= 1.3
        if (lane == Lane.GAMES && shipping) gain += 0.3
        if (lane == Lane.GAMES && has(Perk.ARCADE)) gain *= 1.2
        if (has(Perk.GYM)) gain *= 1.1
        if (e.trait == Trait.FAST_LEARNER) gain *= 2
        if (staff.any { it.lane == lane && it.id != e.id && it.trait == Trait.MENTOR }) gain *= 1.25
        if (founderHas(FounderSkill.MENTOR)) gain *= 1.2
        gain *= storyXp(e)
        e.xp += gain
        if (e.xp < e.xpToNext) continue
        e.xp -= e.xpToNext
        e.level += 1
        if (e.level % 2 == 0) e.skill = minOf(10, e.skill + 1)
        when (e.role) {
            Role.DESIGNER -> e.design = minOf(10, e.design + 1)
            Role.ENGINEER -> e.tech = minOf(10, e.tech + 1)
            Role.RESEARCHER -> e.research = minOf(10, e.research + 1)
            Role.MARKETER, Role.OPERATOR -> {
                if (e.design <= e.tech) e.design = minOf(10, e.design + 1) else e.tech = minOf(10, e.tech + 1)
            }
        }
        e.salary *= if (e.trait == Trait.LOYAL) 1.03 else 1.06
        e.morale = minOf(1.0, e.morale + 0.1)
        ups += e.id
    }
    levelUps = ups
    val leveled = staff.filter { it.id in ups }
    when (leveled.size) {
        0 -> {}
        1 -> leveled[0].let { e ->
            addLog("${e.name} reached level ${e.level}. Skill ${e.skill}, and a raise to ${money(e.salary)}/week.", LogTone.GOOD, e.lane)
        }
        2 -> addLog("${leveled[0].name} and ${leveled[1].name} levelled up.", LogTone.GOOD)
        else -> addLog("${leveled[0].name}, ${leveled[1].name} and ${leveled.size - 2} others levelled up.", LogTone.GOOD)
    }
}

// Progress and endings

private fun GameState.updateStreaks() {
    val allProfitable = lanes.isNotEmpty() && lanes.all { trailingProfit(it, 13) > 0 }
    stats.profitableStreak = if (allProfitable) stats.profitableStreak + 1 else 0
    if (pivot != null && lanes.size < 3 && stats.profitableStreak == EXPANSION_STREAK_NEEDED) {
        addLog("Every lane is profitable. You can expand into another lane.", LogTone.MILESTONE)
    }
    if (isEcosystem && allProfitable) {
        stats.ecosystemWeeks += 1
        if (stats.ecosystemWeeks == 52) {
            addLog("A full year as a profitable ecosystem company. The prestige goal is yours, if you can hold it.", LogTone.MILESTONE)
        }
    }
}

private fun GameState.checkEndings() {
    if (ending != null) return
    if (cash < 0 && !sandbox) {
        stats.weeksInRed += 1
        if (stats.weeksInRed == 1) addLog("You're overdrawn. ${difficulty.redWeeks} weeks in the red and it's over.", LogTone.BAD)
        maybeDistressBuyout()
        if (stats.weeksInRed >= difficulty.redWeeks) {
            end(EndingKind.BANKRUPT)
            return
        }
    } else {
        stats.weeksInRed = 0
    }
    if (week >= runLength && !freeplay) end(if (listing != null) EndingKind.IPO else EndingKind.INDIE)
}

/** What a rival's big move looks like in the headlines. */
private fun GameState.rivalMove(who: String, lane: Lane): String = rng.pick(
    when (lane) {
        Lane.GAMES -> listOf("$who announces its biggest game yet", "$who signs a blockbuster exclusive", "$who buys a hot indie studio")
        Lane.WEBSITES -> listOf("$who launches an AI site builder", "$who cuts its prices in half", "$who signs a deal with every bank in the country")
        Lane.APPS -> listOf("$who ships a redesign everyone's talking about", "$who's new app tops the store in a day", "$who adds a feature everyone else will copy")
        Lane.SOCIAL -> listOf("$who copies a rival's best feature", "$who's new app passes 100 million users", "$who pays creators to move over")
        Lane.HARDWARE -> listOf("$who unveils a new flagship", "$who's new device sells out in minutes", "$who undercuts everyone on price")
        Lane.AI -> listOf("$who releases a new frontier model", "$who's model tops every benchmark", "$who raises billions for a new data centre")
        Lane.OS -> listOf("$who ships a new platform version", "$who locks in three more OEM partners", "$who's developer conference steals the week")
    },
)
