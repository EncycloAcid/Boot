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
    /** List the company and keep running it, rather than ringing the bell and retiring. */
    private val staysPublic: Boolean = false,
) {
    /** Make this week's decisions, then advance one week. */
    fun playWeek(s: GameState) {
        if (s.isOver) return
        var guard = 0
        while (s.needsDecision && !s.isOver && guard < 20) {
            guard += 1
            if (s.pendingPivot != null) resolvePivot(s) else resolveEvent(s)
        }
        // Still stuck: take whatever's left so the week can go on.
        if (s.needsDecision && !s.isOver) {
            if (s.pendingPivot != null) attempt { s.stayFocused() }
            while (s.events.isNotEmpty() && !s.isOver) {
                val before = s.events.size
                attempt { s.choose(s.events[0].choices.lastIndex) }
                if (s.events.size >= before) break
            }
        }
        if (s.isOver) return
        grow(s)
        manage(s)
        attempt { s.advance() }
    }

    /** Spends skill points across the tree, a branch at a time, and takes a second trait when it comes. */
    private fun grow(s: GameState) {
        if (s.founder.level >= SECOND_TRAIT_LEVEL && s.founder.trait2 == null) {
            val pick = listOf(FounderTrait.PEOPLE_PERSON, FounderTrait.SHOWRUNNER, FounderTrait.CODER).firstOrNull { it in s.secondTraitChoices }
            pick?.let { attempt { s.pickSecondTrait(it) } }
        }
        while (s.founder.skillPoints > 0) {
            val next = FounderSkill.entries.sortedBy { it.tier }.firstOrNull { s.skillBlocker(it) == null } ?: break
            s.learnSkill(next)
        }
    }

    /** Play until the game ends. Returns the ending. */
    fun playToEnd(s: GameState): EndingReport? {
        while (!s.isOver && s.week <= s.runLength + 5) playWeek(s)
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
            is RivalMove -> {
                val stake = s.stake(k.lane)
                when (k.move) {
                    RivalMoveKind.COPYCAT -> if (s.cash > stake * 30) 0 else if (s.cash > stake * 10) 1 else 2
                    RivalMoveKind.PRICE_WAR -> 0
                    RivalMoveKind.AD_BLITZ -> if (s.cash > stake * 20) 0 else 1
                    RivalMoveKind.TALENT_RAID -> if (cashy) 0 else 2
                    RivalMoveKind.LAWSUIT -> if (s.cash > stake * 30) 0 else 1
                    RivalMoveKind.EXCLUSIVE_DEAL -> if (s.cash > stake * 40) 0 else 1
                    RivalMoveKind.SMEAR -> if (cashy) 0 else 1
                }
            }
            is Burnout -> 0
            is StaffClash -> if (cashy) 2 else 0
            is EquityAsk -> 1
            RemoteWork -> 0
            is SideProject -> 0
            is DataBreach -> if (cashy) 0 else 1
            is ViralComplaint -> if (cashy) 0 else 1
            is ClientBankrupt -> if (cashy) 1 else 0
            is Opportunity -> 0
            is RetainerOffer -> 0
            is BigPitch -> if (s.cash > k.cost * 10) 0 else 1
            is ExperimentResult -> if (k.better) 0 else 1
            is ModelReady -> 0
            is EraMoment -> if (cashy && s.cash > 200_000) 0 else e.choices.lastIndex
            is ScenarioOver -> 0
            is RaiderStake -> 0
            is TakeoverOffer -> 1
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
            is EventKind.RushJob -> if (s.staff(Lane.WEBSITES).all { it.morale > 0.6 }) 0 else 1
            EventKind.AppFeatured -> if (s.cash > 100_000) 0 else 1
            is EventKind.Celebrity -> if ((s.social?.toxicity ?: 0.0) < 0.3) 0 else 1
            EventKind.OpenSource -> 0
            EventKind.Offsite -> if (s.runwayWeeks > 26) 0 else 1
            EventKind.Convention -> {
                // Go big with a promising game and money to spare; otherwise a stand that fits the budget.
                val open = s.boothsOpen()
                val promising = s.games?.project != null && s.showcase().appeal >= 0.7
                val pick = when {
                    promising && Booth.KEYNOTE in open && s.cash > s.boothCost(Booth.KEYNOTE) * 4 -> Booth.KEYNOTE
                    promising && Booth.LARGE in open && s.cash > s.boothCost(Booth.LARGE) * 4 -> Booth.LARGE
                    s.games?.project != null && s.cash > s.boothCost(Booth.MEDIUM) * 3 -> Booth.MEDIUM
                    s.cash > s.boothCost(Booth.SMALL) * 4 -> Booth.SMALL
                    else -> null
                }
                pick?.let { open.indexOf(it) } ?: e.choices.lastIndex
            }
            is ShowResults -> 0
            is MilestoneReview -> 0
            is ExclusiveOffer -> e.choices.indexOfFirst { it.label == "Timed exclusive" }.takeIf { it >= 0 } ?: e.choices.lastIndex
            is PlatformInvite -> {
                val labels = e.choices.map { it.label }
                val p = s.games?.projectOf(k.team)
                val pick = when {
                    "Shadow-drop it" in labels -> "Shadow-drop it"
                    "Take their marketing deal" in labels && s.cash < 1_000_000 -> "Take their marketing deal"
                    p != null && s.cash > s.trailerCost(p) * 10 -> "Show a trailer"
                    else -> "Not this time"
                }
                labels.indexOf(pick).takeIf { it >= 0 } ?: labels.lastIndex
            }
            is PremiereSlot -> {
                val labels = e.choices.map { it.label }
                val p = s.games?.projectOf(k.team)
                val pick = when {
                    "Shadow-drop it" in labels && s.cash > k.fee * 5 -> "Shadow-drop it"
                    p != null && s.appealOf(k.team) >= 0.6 && s.cash > (k.fee + s.trailerCost(p)) * 5 -> "World premiere"
                    else -> "Skip it"
                }
                labels.indexOf(pick).takeIf { it >= 0 } ?: labels.lastIndex
            }
            is ReleaseSlip -> 0
            // Take the kind option: pair them, credit them, send them home, make it official, give the lead or the break.
            is StoryBeat -> 0
            is AwardNight -> 0
            is IntegrationChoice -> 0
            is DealCounter, is BiddingWar, is RegulatorReview -> 1
            is StreamerPreview -> if ((s.games?.projectOf(k.team)?.bugs ?: 0.0) < 5) 0 else 1
            is FanCampaign -> if (s.cash > s.fanCampaignCost() * 20) 0 else 1
            is PresenterInvite -> 0
            OwnShowcase -> if (s.cash > s.ownShowcaseCost() * 5 && s.appealOf(1) >= 0.5) 0 else 1
        }
        attempt { s.choose(choice) }
    }

    /** Say yes to what's affordable in the inbox; let the rest lapse. */
    private fun answerInbox(s: GameState) {
        for (item in s.inbox.toList()) {
            val yes = when (val k = item.kind) {
                Interns -> s.freeDesks >= 2
                is ConferenceTalk -> s.cash > 100_000
                is Partnership -> true
                is Acquihire -> s.cash > k.price * 3 && s.freeDesks >= 3
                is AngelCheck -> s.cash < 100_000
                is Interview -> true
                is RetainerOffer -> true
                is BigPitch -> s.cash > k.cost * 10
                is ExperimentResult -> k.better
                else -> false
            }
            if (yes) attempt { s.answer(item.id, 0) }
        }
    }

    /** Run app experiments, and hit back at a hostile rival when there's money to spare. */
    private fun useNewLevers(s: GameState) {
        s.apps?.let { a ->
            if (a.isLive && a.experiment == null) {
                ExperimentKind.entries.firstOrNull { s.experimentBlocker(it) == null }
                    ?.let { if (s.cash > s.experimentCost * 10) attempt { s.startExperiment(it) } }
            }
        }
        // Advertise a business that earns ten times what a campaign costs a week.
        for (lane in s.lanes) {
            if (s.campaignFor(lane) != null || (s.marketing.boost[lane] ?: 0.0) > 0.1) continue
            val channel = s.channels().maxByOrNull { it.fit(lane) * it.reach / it.weekly } ?: continue
            val cost = s.campaignCost(channel, AdBudget.MEDIUM)
            if (s.trailingRevenue(lane, 4) / 4 > cost * 10 && s.cash > cost * 8 * 4) attempt { s.startCampaign(lane, channel, AdBudget.MEDIUM, 8) }
        }
        for (r in s.yourRivals) {
            if (r.stance == RivalStance.HOSTILE && s.cash > s.counterCost(r, CounterMove.ATTACK_ADS) * 15 && s.counterBlocker(r, CounterMove.ATTACK_ADS) == null) {
                attempt { s.counter(r.name, CounterMove.ATTACK_ADS) }
            }
        }
    }

    /** Build the cheapest useful roadmap feature for each business once there's money to spare. */
    private fun buildRoadmap(s: GameState) {
        for (lane in s.lanes) {
            val next = Feature.forLane(lane).filter { s.featureBlocker(it) == null }.minByOrNull { it.cost } ?: continue
            if (s.cash > next.cost * 8 && s.runwayWeeks > 26) attempt { s.buildFeature(next) }
        }
    }

    private fun resolvePivot(s: GameState) {
        val offer = s.pendingPivot ?: return
        // Keep a games studio focused until it has a franchise. Expanding into Social/Apps
        // splits the team and Autopilot rarely reaches sequel scores (franchise flow tests).
        val g = s.games
        if (Lane.GAMES in s.lanes && g != null && g.releases.isNotEmpty() && g.franchises.isEmpty()) {
            attempt { s.stayFocused() }
            return
        }
        // Prefer expanding when the first business already shipped — pivoting away wipes franchises,
        // live apps, and other depth progress Autopilot (and flow tests) rely on.
        val shippedSomething = (g?.releases?.isNotEmpty() == true) ||
            s.apps?.isLive == true ||
            s.social?.isLive == true ||
            s.hardware?.onMarket != null ||
            (s.os?.versionsShipped ?: 0) > 0 ||
            ((s.ai?.generation ?: 0) > 0)
        val mode = if (s.trailingProfit(weeks = 13) > 0 || shippedSomething) PivotMode.EXPAND else PivotMode.PIVOT
        val options = lanePreference.filter { it in s.pivotChoices }.toMutableList()
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
        // Fall back: try expand before pivot so a shipping studio isn't sold off as a last resort.
        val modes = if (mode == PivotMode.EXPAND) {
            listOf(PivotMode.EXPAND, PivotMode.PIVOT)
        } else {
            PivotMode.intoNewLane.toList()
        }
        for (lane in s.pivotChoices) {
            for (m in modes) {
                for (venture in listOf(false, true)) {
                    if (s.pivotBlocker(lane, m, venture) == null) {
                        attempt { s.resolvePivot(lane, m, venture) }
                        return
                    }
                }
            }
        }
        // Nothing else works (in a run set in the past there may be only one other business, and no money for it).
        attempt { s.stayFocused() }
    }

    // Running the company

    private fun manage(s: GameState) {
        if (s.crunch) attempt { s.setCrunch(false) }
        // Don't ring the bell while a games studio still hasn't started a series — flow tests and
        // depth loops need sequels before a retire-on-IPO ending.
        val waitingOnFranchise = Lane.GAMES in s.lanes &&
            (s.games?.releases?.isNotEmpty() == true) &&
            (s.games?.franchises?.isEmpty() == true) &&
            Research.SEQUELS in s.researched
        if (s.ipoBlocker == null && !waitingOnFranchise) {
            attempt { s.goPublic(keepRunning = staysPublic) }
            return
        }

        if (s.runwayWeeks < 20 && s.roundOffer().isSuccess) attempt { s.raiseRound() }
        buildRoadmap(s)
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
                Lane.OS -> manageOs(s)
            }
        }

        for (c in s.crossovers) {
            if (c.isIdle && s.cash > c.combo.buildCost * 5 && s.trailingProfit(weeks = 13) > 0) {
                attempt { s.buildCrossover(c.combo) }
                attempt { s.setIntensity(c.combo, comboIntensity) }
            }
        }

        if (s.expansionBlocker == null) {
            lanePreference.firstOrNull { it !in s.lanes && s.laneOpen(it) }?.let { next ->
                if (s.cash > next.selfFundThreshold + next.setupCost) {
                    attempt { s.expand(next) }
                } else if (next.isCapitalIntensive && s.ventureForLane(next).isSuccess) {
                    attempt { s.expand(next, fundWithVenture = true) }
                }
            }
        }

        research(s)
        answerInbox(s)
        useNewLevers(s)
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
        Lane.OS -> 18
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
        // Back the pitches that look good, when there's money to spare.
        for (pitch in g.pitches.toList()) {
            val known = g.knownFit(pitch.topic, pitch.genre)
            val promising = pitch.demo >= 2 && (pitch.lastScore ?: 6.5) >= 6.5 && known != Fit.BAD && known != Fit.OKAY
            if (promising && s.signBlocker(pitch.id) == null && s.cash > pitch.funding * 6 + 100_000) attempt { s.signPitch(pitch.id) }
        }
        // Leagues for the competitive hits, with the prize pool their audience can justify.
        for (r in s.leagueCandidates) if (s.leagueBlocker(r.id) == null && s.cash > r.leagueCost() * 5) attempt { s.startLeague(r.id) }
        for (r in g.leagues) {
            val owners = maxOf(r.unitsSold, r.live?.players ?: 0.0)
            val prize = when {
                owners > 5e6 && s.cash > 5e7 -> PrizePool.RECORD
                owners > 1e6 && s.cash > 1e7 -> PrizePool.MAJOR
                else -> PrizePool.MODEST
            }
            if (r.league!!.prize != prize) attempt { s.setPrizePool(r.id, prize) }
        }
        // Keep live games fresh while they pay for it.
        for (r in g.liveGames) if (s.seasonBlocker(r.id) == null && s.cash > r.seasonCost() * 4) attempt { s.launchSeason(r.id) }
        // The second team makes smaller games alongside the main one.
        g.project2?.let { p2 ->
            if (p2.isReady && (p2.bugs < p2.work * 0.08 || p2.weeksInDev > p2.work / 2 + 30)) attempt { s.launchGame(2) }
        }
        if (Research.SECOND_TEAM in s.researched && g.project2 == null && s.gameTeam >= 16 && s.runwayWeeks > 26) {
            if (g.team2Share != 0.3) attempt { s.setTeamSplit(0.3) }
            bestConcept(s, g)?.let { c ->
                val c2 = c.copy(size = minOf(c.size, GameSize.MEDIUM), live = false)
                if (s.conceptBlocker(c2, team = 2) == null) attempt { s.startGame(c2, idealFocus(c2), 2) }
            }
        }
        val p = g.project
        if (p != null) {
            // Market a game that's worth it, then ship once the bugs are mostly gone.
            if (Research.MARKETING in s.researched) {
                for (c in Campaign.entries) if (c !in p.campaigns && s.cash > c.cost * 25) attempt { s.buyCampaign(c) }
            }
            // Playtest when bugs are biting and cash can cover it; once per stage.
            val playStage = if (p.isReady) 3 else p.stage
            if (playStage !in p.playtestedStages && p.bugs > p.work * 0.05) {
                val cost = p.concept.size.weeklyBudget * (1.5 + playStage * 0.5)
                if (s.cash > cost * 4) attempt { s.playtestGame() }
            }
            // Lean creative direction toward genre needs while content is still in progress.
            if (!p.isReady) {
                val want = when {
                    p.concept.idealDesignShare >= 0.6 -> GameFocus.GAMEPLAY
                    p.bugs > p.work * 0.12 -> GameFocus.PERFORMANCE
                    else -> GameFocus.BALANCED
                }
                if (p.creativeFocus != want) attempt { s.setCreativeFocus(want) }
            } else if (p.creativeFocus != GameFocus.VISUALS && p.polish < p.work * 0.25) {
                attempt { s.setCreativeFocus(GameFocus.VISUALS) }
            }
            val forecast = s.gameForecast()
            val polished = p.bugs < p.work * 0.08 ||
                forecast?.polishPastSweetSpot == true ||
                p.weeksInDev > p.work / 2 + 30
            // Prefer in-season launches when ready; ship off-season only if polish is full or cash is tight.
            val season = GameSeason.forWeek(s.week)
            val inSeason = season.launchMultiplier(p.concept.genre) >= 1.08
            val shouldShip = p.isReady && (
                s.runwayWeeks < 10 ||
                    (polished && inSeason) ||
                    (polished && (forecast?.polishPastSweetSpot == true || p.bugs < p.work * 0.04))
                )
            if (shouldShip) attempt { s.launchGame() }
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
        val fresh = bestConcept(s, g)
        // Follow up a hit once its series has had a year off.
        val hit = if (Research.SEQUELS in s.researched) {
            // Before any franchise exists, follow up the best ship even if it wasn't a critic darling.
            val threshold = if (g.franchises.isEmpty()) 6.0 else 7.5
            val wait = if (g.franchises.isEmpty()) 40 else 52
            g.releases
                .filter {
                    it.score >= threshold && it.publisher == null &&
                        s.week - g.series(g.seriesRoot(it)).last().week >= wait
                }
                .maxByOrNull { it.score }
        } else {
            null
        }
        // Bring back an old hit now and then: half the work.
        val classic = s.remasterable.filter { it.score >= 8 }.maxByOrNull { it.score }
        val concept = when {
            hit != null && fresh != null -> s.sequelConcept(hit.id).copy(size = fresh.size)
            classic != null && fresh != null && s.conceptBlocker(s.remasterConcept(classic.id)) == null -> s.remasterConcept(classic.id)
            else -> fresh
        }
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
            title = g.suggestTitle(s.rng, topic, genre), topic = topic, genre = genre, audience = audience, platforms = platforms,
            size = size, engineId = g.engines.maxBy { it.level }.id,
            live = Research.LIVE_SERVICE in s.researched && size >= GameSize.LARGE,
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
                        Research.STYLE_PIXEL, Research.STYLE_STYLIZED,
                        Research.EXPANSIONS, Research.PUBLISHING, Research.GRAPHICS_3D_V1, Research.REMASTERS,
                        Research.LARGE_GAMES, Research.PHYSICS, Research.MULTIPLAYER, Research.SOUND_SURROUND, Research.GRAPHICS_3D_V2,
                        Research.ADVANCED_AI, Research.PROCEDURAL, Research.ONLINE_SERVICES, Research.LIVE_SERVICE, Research.ESPORTS, Research.OPEN_WORLD,
                        Research.GRAPHICS_3D_V3,
                        Research.AAA_GAMES, Research.SPATIAL_AUDIO, Research.GRAPHICS_3D_V4, Research.CROSS_PLAY, Research.NEURAL_NPCS,
                        Research.PHOTOREAL, Research.SECOND_TEAM,
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
        val focus = when {
            a.lastChurn > 0.09 -> UpdateFocus.STABILITY
            a.weeksSinceUpdate > 10 -> UpdateFocus.FEATURES
            a.quality < 55 -> UpdateFocus.POLISH
            else -> a.updateFocus
        }
        if (focus != a.updateFocus) attempt { s.setUpdateFocus(focus) }
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
                    val cat = if (flagship) DeviceCategory.PHONE else DeviceCategory.HEADPHONES
                    val tier = if (flagship) DeviceTier.FLAGSHIP else DeviceTier.BUDGET
                    // Category-weighted ideals: phones push camera+battery; buds push sound+build.
                    val focus = when (cat) {
                        DeviceCategory.PHONE -> listOf(0.14, 0.22, 0.24, 0.14, 0.12, 0.14)
                        DeviceCategory.HEADPHONES -> listOf(0.08, 0.18, 0.32, 0.22, 0.08, 0.12)
                        else -> DeviceFocus.evenFocus()
                    }
                    s.startDevice(
                        cat, tier,
                        formFactor = FormFactor.defaultFor(cat),
                        focus = focus,
                        bundleOs = s.canBundleOs(),
                        appPack = s.canOfferAppPack() && s.apps?.isLive == true,
                    )
                }
            }
        }
        h.design?.let { d ->
            if (d.completion > 0.55 && d.preorderedStock == 0 && inbound == 0) {
                val forecast = h.forecast(d)
                val affordable = s.cash * 0.45 / d.effectiveUnitCost
                val units = min(forecast.preorderUnits.toDouble(), affordable).toInt()
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

    private fun manageOs(s: GameState) {
        val lab = s.os ?: return
        val p = lab.project
        if (p == null) {
            val target = OsTarget.entries.lastOrNull { s.osTargetOpen(it) } ?: OsTarget.PC
            if (s.cash > target.weeklyBurn * 8) attempt { s.startOsVersion(target) }
            return
        }
        if (p.isReady && !p.inBeta) {
            if (p.bugs > p.work * 0.15) attempt { s.startOsBeta(6) } else attempt { s.shipOs() }
        } else if (p.isReady && p.inBeta && p.betaWeeksLeft <= 0) {
            attempt { s.shipOs() }
        }
    }
}
