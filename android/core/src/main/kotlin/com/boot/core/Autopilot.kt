package com.boot.core

import kotlin.math.max
import kotlin.math.min

/**
 * A simple scripted player. It plays sensibly rather than well, which makes it a
 * good yardstick for balance runs and a smoke test for the whole simulation.
 */
class Autopilot(
    /** Lanes to move into at the Pivot Point and later expansions, in order of preference. */
    private val lanePreference: List<Lane> = listOf(Lane.APPS, Lane.HARDWARE, Lane.AI, Lane.SOCIAL, Lane.GAMES, Lane.WEBSITES),
    /** Sell outright when a buyout arrives after the Pivot Point. */
    private val sellsOnOffer: Boolean = false,
    /** How hard to push combo dials. */
    private val comboIntensity: Double = 0.5,
) {
    /** Make this week's decisions, then advance one week. */
    fun playWeek(s: GameState) {
        if (s.isOver) return
        var guard = 0
        while (s.needsDecision && !s.isOver && guard < 20) {
            guard += 1
            if (s.pendingPivot != null) resolvePivot(s) else resolveEvent(s)
        }
        if (s.isOver) return
        manage(s)
        attempt { s.advance() }
    }

    /** Play until the game ends. Returns the ending. */
    fun playToEnd(s: GameState): EndingReport? {
        while (!s.isOver && s.week <= GameState.LENGTH_WEEKS + 5) playWeek(s)
        return s.ending
    }

    private inline fun attempt(block: () -> Unit) {
        try {
            block()
        } catch (_: GameException) {
        }
    }

    // Decisions

    private fun resolveEvent(s: GameState) {
        val e = s.events.firstOrNull() ?: return
        val cashy = s.cash > 26 * s.weeklyBurn
        val choice = when (val k = e.kind) {
            is EventKind.Poach -> if (cashy) 0 else 1
            is EventKind.Buyout -> if (sellsOnOffer) 0 else 1
            is EventKind.BoardDemandEvent -> 0
            is EventKind.Recall -> 0
            EventKind.ComponentShortage -> if (cashy) 0 else 1
            EventKind.ModerationFire -> if (cashy) 0 else 1
            EventKind.PressScandal -> 0
            EventKind.BotWave -> 0
            EventKind.RegulatorInquiry -> if (s.cash > 3_000_000) 0 else 1
            is EventKind.ViralMoment -> 0
            is EventKind.ComboProblem -> if (s.cash > s.comboFixCost(k.combo) * 4) 0 else 1
            EventKind.EcosystemAntitrust -> 0
            EventKind.GameAwards -> if (s.cash > 200_000) 0 else 1
            is EventKind.RaiseRequest -> if (s.runwayWeeks > 26) 0 else 1
            is EventKind.Streamer -> if (s.cash > 50_000) 0 else 1
            EventKind.LeakedBuild -> 0
            EventKind.GameJam -> if (s.cash > 100_000) 0 else 1
            is EventKind.TaxCredit -> 0
            is EventKind.ServerOutage -> if (s.cash > 200_000) 0 else 1
            is EventKind.FanPetition -> 0
            EventKind.Headhunter -> if (s.cash > 500_000 && s.freeDesks > 0) 0 else 1
            EventKind.Convention -> when {
                s.games?.project != null && s.cash > 2_000_000 -> 1
                s.cash > 200_000 -> 0
                else -> e.choices.lastIndex
            }
        }
        attempt { s.choose(choice) }
    }

    private fun resolvePivot(s: GameState) {
        val offer = s.pendingPivot ?: return
        val mode = if (s.trailingProfit(weeks = 13) > 0) PivotMode.EXPAND else PivotMode.PIVOT
        val options = lanePreference.filter { it !in s.lanes }.toMutableList()
        offer.suggestedLane?.let { suggested ->
            if (options.remove(suggested)) options.add(0, suggested)
        }
        for (lane in options) {
            for (venture in listOf(false, true)) {
                if (s.pivotBlocker(lane, mode, venture) == null) {
                    attempt { s.resolvePivot(lane, mode, venture) }
                    return
                }
            }
        }
        // Fall back to whatever works.
        for (lane in s.pivotChoices) {
            for (m in PivotMode.intoNewLane) {
                for (venture in listOf(false, true)) {
                    if (s.pivotBlocker(lane, m, venture) == null) {
                        attempt { s.resolvePivot(lane, m, venture) }
                        return
                    }
                }
            }
        }
    }

    // Running the company

    private fun manage(s: GameState) {
        if (s.crunch) attempt { s.setCrunch(false) }
        if (s.ipoBlocker == null) {
            attempt { s.goPublic() }
            return
        }

        if (s.runwayWeeks < 20 && s.roundOffer().isSuccess) attempt { s.raiseRound() }
        // Perks once the business can carry them.
        val weeklyRevenue = s.trailingRevenue(weeks = 13) / 13
        for (perk in Perk.entries) {
            if (s.perkBlocker(perk) == null && s.cash > perk.cost * 20 && s.perkUpkeep + perk.weeklyCost(s.staff.size) < weeklyRevenue * 0.02) {
                attempt { s.buyPerk(perk) }
            }
        }
        // A bank loan as a last resort, and paid off early once the money's there.
        if (s.cash < s.weeklyBurn * 3 && s.loanBlocker == null) attempt { s.takeLoan() }
        s.loanPayoff?.let { if (s.cash > it * 4 && s.runwayWeeks > 52) attempt { s.repayLoan() } }
        val lean = s.runwayWeeks < 16 && s.staff.isEmpty()
        if (lean != s.founder.freelancing) attempt { s.setFreelancing(lean) }

        for (lane in s.lanes) {
            when (lane) {
                Lane.GAMES -> manageGames(s)
                Lane.WEBSITES -> manageWeb(s)
                Lane.APPS -> manageApps(s)
                Lane.SOCIAL -> manageSocial(s)
                Lane.HARDWARE -> manageHardware(s)
                Lane.AI -> manageAI(s)
            }
        }

        for (c in s.crossovers) {
            if (c.isIdle && s.cash > c.combo.buildCost * 5 && s.trailingProfit(weeks = 13) > 0) {
                attempt { s.buildCrossover(c.combo) }
                attempt { s.setIntensity(c.combo, comboIntensity) }
            }
        }

        if (s.expansionBlocker == null) {
            lanePreference.firstOrNull { it !in s.lanes }?.let { next ->
                if (s.cash > next.selfFundThreshold + next.setupCost) {
                    attempt { s.expand(next) }
                } else if (next.isCapitalIntensive && s.ventureForLane(next).isSuccess) {
                    attempt { s.expand(next, fundWithVenture = true) }
                }
            }
        }

        research(s)
        if (s.freeDesks <= 0) s.office.next?.let { if (s.cash > it.moveCost * 4 + 26 * s.weeklyBurn) attempt { s.moveOffice() } }
        hire(s)
    }

    /** Rough headcount where each lane stops benefiting from more people. */
    private fun usefulHeadcount(lane: Lane, s: GameState): Int = when (lane) {
        Lane.GAMES -> if (s.cash > 5_000_000) 35 else 12
        Lane.WEBSITES -> 3 + (s.web?.active?.size ?: 0) * 2
        Lane.APPS -> 3 + ((s.apps?.users ?: 0.0) / 150_000).toInt()
        Lane.SOCIAL -> 3 + ((s.social?.users ?: 0.0) / 1_000_000).toInt()
        Lane.HARDWARE -> 25
        Lane.AI -> 20
    }

    private fun hire(s: GameState) {
        if (s.week % 2 != 0) return
        val profit = s.trailingProfit(weeks = 13) / 13
        val budget = max(profit * 0.5, if (s.venture.isBacked) s.cash / 300 else 0.0)
        if (budget <= 0 || s.cash <= 40 * (s.payroll + 2_000)) return
        val lane = s.lanes.filter { s.staff(it).size < usefulHeadcount(it, s) }.minByOrNull { s.staff(it).size } ?: return
        val fit = s.candidates
            .filter { lane.weight(it.role) >= 0.8 && it.askingSalary <= budget }
            .maxByOrNull { it.skill / it.askingSalary }
        if (fit != null) attempt { s.hire(fit.id, lane) }
    }

    private fun manageGames(s: GameState) {
        val g = s.games ?: return
        g.unseenReview?.let { s.acknowledgeReview() }
        val p = g.project
        if (p != null) {
            // Market a game that's worth it, then ship once the bugs are mostly gone.
            if (Research.MARKETING in s.researched) {
                for (c in Campaign.entries) if (c !in p.campaigns && s.cash > c.cost * 25) attempt { s.buyCampaign(c) }
            }
            val polished = p.bugs < p.work * 0.08 || p.weeksInDev > p.work / 2 + 30
            if (p.isReady && (polished || s.runwayWeeks < 10)) attempt { s.launchGame() }
            return
        }
        // License every engine but the newest.
        val newest = g.engines.maxBy { it.level }
        for (e in g.engines) if (e !== newest && e.level > 0 && !e.licensed) attempt { s.setEngineLicensed(e.id, true) }
        if (g.contract != null || g.expansion != null) return
        // Patch what shipped broken, and expand what sold well.
        for (r in g.releases.take(3)) if (s.patchBlocker(r.id) == null && s.cash > r.patchCost() * 5) attempt { s.patchGame(r.id) }
        g.releases.firstOrNull { it.score >= 7.5 && it.expansions.isEmpty() && s.expansionBlocker(it.id) == null }
            ?.let { if (s.cash > it.concept.size.weeklyBudget * 40) attempt { s.startExpansion(it.id) } }
        if (g.busy) return
        if (Research.CUSTOM_ENGINES in s.researched && g.engineBuild == null) {
            val (features, cost, _) = s.engineQuote()
            if (features.sumOf { it.engineLevel } >= g.engines.maxOf { it.level } + 4 && s.cash > cost * 6) attempt { s.buildEngine() }
        }
        val concept = bestConcept(s, g)
        if (concept == null || s.conceptBlocker(concept) != null || (s.runwayWeeks < 12 && g.contractOffers.isNotEmpty())) {
            g.contractOffers.firstOrNull()?.let { attempt { s.takeGameContract(it.id) } }
            return
        }
        attempt { s.startGame(concept, idealFocus(concept)) }
    }

    /** A decent concept: the best known combo, the biggest affordable size, the widest reach. */
    private fun bestConcept(s: GameState, g: GamesStudio): GameConcept? {
        val genre = Genre.entries.maxBy { genre ->
            g.topics.maxOf { t -> (g.knownFit(t, genre)?.multiplier ?: 0.98) - if (g.releases.firstOrNull()?.concept?.genre == genre) 0.2 else 0.0 }
        }
        val last = g.releases.firstOrNull()?.concept
        val topic = g.topics.filter { last == null || it != last.topic }.ifEmpty { g.topics }
            .maxBy { (g.knownFit(it, genre)?.multiplier ?: 0.98) + it.ordinal * 0.0001 }
        val sizes = s.unlockedSizes.filter { s.gameTeam >= it.minTeam }
        val size = sizes.lastOrNull { s.cash > it.weeklyBudget * s.estimatedWeeks(it) * 2.5 + 20_000 } ?: GameSize.SMALL
        val onSale = s.platformsOnSale.filter { it.fit(genre) != Fit.BAD || it == Platform.PC }
        val maxPlatforms = if (Research.MULTI_PLATFORM in s.researched) 3 else 1
        val platforms = onSale.sortedByDescending { s.platformPlayers(it) * it.spend }
            .filter { it.fee < s.cash * 0.05 || it.fee == 0.0 }
            .take(maxPlatforms).ifEmpty { listOf(Platform.PC) }
        val audience = if (Research.TARGET_AUDIENCE in s.researched) {
            Audience.entries.maxBy { topic.fit(it).multiplier }
        } else {
            Audience.EVERYONE
        }
        return GameConcept(
            title = g.suggestTitle(s.rng), topic = topic, genre = genre, audience = audience, platforms = platforms,
            size = size, engineId = g.engines.maxBy { it.level }.id,
        )
    }

    /** An experienced designer's instinct: close to the genre's ideal, not perfect. */
    private fun idealFocus(c: GameConcept): List<MutableList<Double>> = List(3) { stage ->
        FocusArea.forStage(stage).map { c.idealShare(it) * 0.8 + 0.2 / 3 }.toMutableList()
    }

    private fun research(s: GameState) {
        val g = s.games
        if (g != null) {
            // A new topic now and then keeps games fresh.
            val topic = Topic.entries.filter { it !in g.topics }.minByOrNull { it.researchCost }
            if (topic != null && g.topics.size < 14 && s.researchPoints >= topic.researchCost + 10) attempt { s.researchTopic(topic) }
        }
        val priority = buildList {
            if (g != null) {
                addAll(
                    listOf(
                        Research.MEDIUM_GAMES, Research.SAVE_SYSTEM, Research.GRAPHICS_2D_V2, Research.SOUND_STEREO, Research.CUSTOM_ENGINES,
                        Research.MARKETING, Research.TARGET_AUDIENCE, Research.BASIC_AI, Research.MULTI_PLATFORM, Research.SEQUELS, Research.MERCHANDISE,
                        Research.EXPANSIONS, Research.GRAPHICS_3D_V1,
                        Research.LARGE_GAMES, Research.PHYSICS, Research.MULTIPLAYER, Research.SOUND_SURROUND, Research.GRAPHICS_3D_V2,
                        Research.ADVANCED_AI, Research.PROCEDURAL, Research.ONLINE_SERVICES, Research.OPEN_WORLD, Research.GRAPHICS_3D_V3,
                        Research.AAA_GAMES, Research.SPATIAL_AUDIO, Research.GRAPHICS_3D_V4, Research.CROSS_PLAY, Research.NEURAL_NPCS,
                        Research.PHOTOREAL,
                    ),
                )
            }
            for (lane in s.lanes) addAll(Research.entries.filter { it.lane == lane })
            add(Research.TRAINING)
            add(Research.RD_LAB)
        }
        priority.firstOrNull { it !in s.researched }?.let { next ->
            if (s.researchBlocker(next) == null && s.cash > next.cash * 5) attempt { s.research(next) }
        }
    }

    private fun manageWeb(s: GameState) {
        val w = s.web ?: return
        val rate = if (w.standing > 0.6) AgencyRate.PREMIUM else AgencyRate.STANDARD
        if (w.rate != rate) attempt { s.setAgencyRate(rate) }
        val capacity = max(1, (s.staffing(Lane.WEBSITES).output / 5).toInt())
        for (offer in w.offers.sortedByDescending { it.value / it.work }) {
            if ((s.web?.active?.size ?: 0) >= capacity) break
            attempt { s.acceptContract(offer.id) }
        }
    }

    private fun manageApps(s: GameState) {
        val a = s.apps ?: return
        if (!a.isLive) return
        if (a.quality > 72 && a.monetization == AppMonetization.ADS && a.users > 20_000) {
            attempt { s.setMonetization(AppMonetization.SUBSCRIPTION) }
        }
    }

    private fun manageSocial(s: GameState) {
        val so = s.social ?: return
        attempt { s.setModeration((0.25 + so.toxicity + so.botShare).clamped(0.2, 0.9)) }
        val ads = when {
            so.users > 2_000_000 -> 0.5
            so.users > 200_000 -> 0.25
            else -> 0.1
        }
        attempt { s.setAdLoad(ads) }
    }

    private fun manageHardware(s: GameState) {
        val h = s.hardware ?: return
        val inbound = h.orders.sumOf { it.units }
        if (h.design == null) {
            val age = h.onMarket?.let { s.week - it.launchWeek } ?: 999
            if (age > 40) {
                val flagship = s.cash > 4_000_000
                attempt {
                    s.startDevice(
                        if (flagship) DeviceCategory.PHONE else DeviceCategory.HEADPHONES,
                        if (flagship) DeviceTier.FLAGSHIP else DeviceTier.BUDGET,
                    )
                }
            }
        }
        h.design?.let { d ->
            if (d.completion > 0.55 && d.preorderedStock == 0 && inbound == 0) {
                val spec = d.spec
                val estimate = spec.category.baseDemand * spec.tier.demandMultiplier * h.brandAwareness * 14
                val affordable = s.cash * 0.45 / spec.unitCost
                val units = min(estimate, affordable).toInt()
                if (units > 100) attempt { s.orderUnits(units) }
            }
            if (d.isReady && (d.preorderedStock > 0 || inbound == 0)) attempt { s.launchDevice() }
        }
        val m = h.onMarket
        if (m != null && h.design == null && h.orders.isEmpty() && h.inventory < h.lastDemand * 3 && s.week - m.launchWeek < 30) {
            val units = min(h.lastDemand * 10, s.cash * 0.35 / m.spec.unitCost).toInt()
            if (units > 100) attempt { s.orderUnits(units) }
        }
    }

    private fun manageAI(s: GameState) {
        val lab = s.ai ?: return
        if (lab.training == null) {
            val wanted = TrainingSize.entries.firstOrNull { lab.capability < it.ceiling * 0.8 } ?: TrainingSize.FRONTIER
            val budget = when (wanted) {
                TrainingSize.SMALL -> 400_000.0
                TrainingSize.MEDIUM -> 2_500_000.0
                TrainingSize.FRONTIER -> 14_000_000.0
            }
            if (s.cash > budget) attempt { s.startTraining(wanted) }
        }
        if (s.cash > 30_000_000 && lab.gpusOwned < 16) attempt { s.buyGPUs(4) }
    }
}
