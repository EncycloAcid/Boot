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
    updateRivals()

    // Combos first: they shape every lane's week.
    val fx = ComboEffects()
    for (c in crossovers) c.applyTo(fx, this)
    web?.let { it.clientWariness = max(fx.webWariness, it.clientWariness * 0.98) }

    for (lane in lanes) {
        val ctx = context(lane, fx)
        val report = when (lane) {
            Lane.GAMES -> games!!.tick(ctx, rng)
            Lane.WEBSITES -> web!!.tick(ctx, rng)
            Lane.APPS -> apps!!.tick(ctx, rng)
            Lane.SOCIAL -> social!!.tick(ctx, rng)
            Lane.HARDWARE -> hardware!!.tick(ctx, rng)
            Lane.AI -> ai!!.tick(ctx, rng)
        }
        applyReport(report, lane)
        val lanePayroll = staff(lane).sumOf { it.salary }
        recordCost(lanePayroll, lane)
        cash -= lanePayroll
    }

    studioWeek()
    researchPoints += weeklyResearch(games?.lastWork ?: 0.0)

    // Unassigned staff still get paid.
    val idle = staff.filter { it.lane == null }.sumOf { it.salary }
    spend(idle + headquarters.rent, null)
    payLoan()

    updateCrossovers(fx)

    if (founder.freelancing) {
        val gig = founder.freelanceIncome(week)
        cash += gig
        recordRevenue(gig, null)
    }

    stats.peakCash = max(stats.peakCash, cash)
    stats.peakValuation = max(stats.peakValuation, valuation)
    ledger.last().cash = cash

    updateFounder()
    updateStaff(fx)
    reputation.weeklyDrift()
    if (week % 4 == 0) refreshTalentPool()
    candidates = candidates.filterNot { it.contestedBy != null && it.expiresWeek < week }

    rollWorldEvents()
    boardReview()
    checkPivotPoint()
    updateStreaks()
    checkGoals()
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
            is Incident.Log -> addLog(incident.text, incident.tone, lane)
            is Incident.Reputation -> reputation.adjust(incident.delta)
            is Incident.Scar -> reputation.scar(incident.kind, incident.severity, week, incident.note)
            is Incident.Event -> enqueue(incident.kind)
            is Incident.Research -> researchPoints += incident.points
            is Incident.Stat -> when (incident.key) {
                StatKey.HIT -> stats.hits += 1
                StatKey.FLOP -> stats.flops += 1
                StatKey.RECALL -> stats.recalls += 1
                StatKey.SCANDAL -> stats.scandals += 1
                StatKey.RELEASE -> {
                    stats.releases += 1
                    stats.releasesThisQuarter += 1
                }
                StatKey.SOLD_OUT -> stats.soldOuts += 1
                StatKey.OVERSTOCK -> stats.overstocks += 1
            }
        }
    }
}

// World

private fun GameState.updateEra() {
    if (week - world.eraStartWeek < world.eraLength) return
    val unseen = Era.entries.filter { it != Era.BOOTSTRAP_YEARS && it !in world.pastEras && it != world.era }
    val next = if (unseen.isEmpty()) {
        rng.pick(Era.entries.filter { it != world.era && it != Era.BOOTSTRAP_YEARS })
    } else {
        rng.pick(unseen)
    }
    world.pastEras += world.era
    world.era = next
    world.eraStartWeek = week
    world.eraLength = rng.int(78, 130)
    if (next !in stats.erasSeen) stats.erasSeen += next
    val mine = lanes.map { next.demand(it) }
    val tone = when {
        mine.any { it > 1.05 } -> LogTone.GOOD
        mine.any { it < 0.95 } -> LogTone.BAD
        else -> LogTone.NEUTRAL
    }
    addLog("A new era begins: ${next.title}. ${next.blurb}", tone)
}

private fun GameState.updateRivals() {
    for (r in world.rivals) {
        val target = (0.5 * world.era.demand(r.lane)).clamped(0.2, 0.9)
        r.strength = (r.strength + (target - r.strength) * 0.02 + rng.normal(sd = 0.01)).clamped(0.05, 1.0)

        if (r.lane in lanes && rng.chance(0.008)) {
            r.strength = min(1.0, r.strength + 0.1)
            val move = "${r.name} launched something big in ${r.lane.title.lowercase()}. Your market just got tougher."
            r.lastMove = move
            addLog(move, LogTone.BAD, r.lane)
        }

        // Rivals compete for the same talent.
        val best = staff.filter { it.lane == r.lane && it.skill >= 6 }.maxByOrNull { it.skill }
        if (best != null && rng.chance(r.aggression * 0.006)) {
            enqueue(EventKind.Poach(best.id, r.name))
        }
    }
}

/** Trends, the yearly convention and new offers for a game studio. */
private fun GameState.studioWeek() {
    val g = games ?: return
    val t = g.trend
    if (t == null || week >= t.until) {
        val next = when (world.era) {
            Era.AI_GOLD_RUSH -> Trend(topic = rng.pick(listOf(Topic.ARTIFICIAL_INTELLIGENCE, Topic.ROBOTS, Topic.CYBERPUNK)), until = 0)
            Era.VR_HYPE_CYCLE -> Trend(genre = Genre.ACTION, until = 0)
            Era.RETRO_REVIVAL -> Trend(genre = Genre.CASUAL, until = 0)
            Era.CREATOR_ECONOMY -> Trend(topic = rng.pick(listOf(Topic.MUSIC, Topic.FASHION, Topic.COOKING)), until = 0)
            Era.PRIVACY_PANIC -> Trend(topic = rng.pick(listOf(Topic.HACKING, Topic.SPY)), until = 0)
            Era.CRYPTO_WINTER -> Trend(topic = Topic.BUSINESS, until = 0)
            else -> if (rng.chance(0.6)) Trend(topic = rng.pick(Topic.entries), until = 0) else Trend(genre = rng.pick(Genre.entries), until = 0)
        }.copy(until = week + rng.int(40, 70))
        if (next.title != t?.title) addLog("Trend: gamers can't get enough of ${next.title.lowercase()} games.", LogTone.NEUTRAL, Lane.GAMES)
        g.trend = next
    }
    if (GameClock.weekOfYear(week) == 26) enqueue(EventKind.Convention)
    marketWeek()
    val output = staffing(Lane.GAMES).output
    g.refreshOffers(week, output, rng, unlockedSizes, Platform.entries.filter { availablePlatform(it) && it != Platform.OWN_CONSOLE })
}

private fun GameState.rollWorldEvents() {
    for (lane in lanes) {
        if (rng.chance(0.0025) && hasLiveProduct(lane)) enqueue(EventKind.ViralMoment(lane))
    }
    rollSmallEvents()
    if (pivot != null && valuation >= 2_000_000 && rng.chance(0.005)) {
        val bidder = if (rng.chance(0.5)) {
            rng.pick(world.rivals).name
        } else {
            rng.pick(listOf("MegaCorp Holdings", "Blackstone-ish Capital", "Conglomo"))
        }
        enqueue(EventKind.Buyout(bidder, Math.round(valuation * rng.range(1.1, 1.6)).toDouble()))
    }
}

/** The everyday surprises: people, fans, leaks, refunds and outages. */
private fun GameState.rollSmallEvents() {
    if (staff.isNotEmpty() && rng.chance(0.006 + 0.0015 * min(staff.size, 10))) {
        staff.filter { week - maxOf(it.hiredWeek, it.lastRaiseWeek) >= 52 && it.skill >= 5 }
            .takeIf { it.isNotEmpty() }
            ?.let { enqueue(EventKind.RaiseRequest(rng.pick(it).id)) }
    }
    games?.let { g ->
        val hot = g.releases.filter { it.weeklySales > 0 && it.score >= 6.5 }
        if (hot.isNotEmpty() && rng.chance(0.01)) {
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
    for (lane in lanes) {
        founder.experience[lane] = (founder.experience[lane] ?: 0.0) + founder.attentionShare(lane, lanes)
    }
}

private fun GameState.updateStaff(fx: ComboEffects) {
    val quitters = mutableListOf<Employee>()
    for (e in staff) {
        var m = e.morale
        m += (0.7 - m) * 0.05
        if (crunch) m -= 0.025
        if (cash < 0) m -= 0.03
        if (e.lane == Lane.WEBSITES) m -= fx.moraleDrain
        if (staff.size > office.desks) m -= 0.02
        e.morale = m.clamped(0.0, 1.0)
        if (e.morale < 0.3 && rng.chance(0.04)) quitters += e
    }
    gainExperience()
    for (q in quitters) {
        staff = staff.filterNot { it.id == q.id }
        addLog("${q.name} quit. Morale had been low for a while.", LogTone.BAD, q.lane)
    }
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
        e.salary *= 1.06
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
    if (cash < 0) {
        stats.weeksInRed += 1
        if (stats.weeksInRed == 1) addLog("You're overdrawn. ${difficulty.redWeeks} weeks in the red and it's over.", LogTone.BAD)
        if (stats.weeksInRed >= difficulty.redWeeks) {
            end(EndingKind.BANKRUPT)
            return
        }
    } else {
        stats.weeksInRed = 0
    }
    if (week >= GameState.LENGTH_WEEKS) end(EndingKind.INDIE)
}
