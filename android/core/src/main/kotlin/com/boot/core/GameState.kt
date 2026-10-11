package com.boot.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/** Why an action was refused. The message is written for the player. */
class GameException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { INVALID, BUSY, INSUFFICIENT_FUNDS, NOT_AVAILABLE, DECISION_PENDING, GAME_OVER }
}

internal fun invalid(m: String) = GameException(GameException.Kind.INVALID, m)
internal fun busy(m: String) = GameException(GameException.Kind.BUSY, m)
internal fun insufficientFunds(m: String) = GameException(GameException.Kind.INSUFFICIENT_FUNDS, m)
internal fun notAvailable(m: String) = GameException(GameException.Kind.NOT_AVAILABLE, m)
internal fun decisionPending() = GameException(GameException.Kind.DECISION_PENDING, "A decision is waiting on you first.")
internal fun gameOver() = GameException(GameException.Kind.GAME_OVER, "The game is over.")

/** One week of the books. */
@Serializable
class WeekLedger(
    val week: Int,
    val revenue: MutableMap<Lane, Double> = mutableMapOf(),
    val costs: MutableMap<Lane, Double> = mutableMapOf(),
    /** Rent, crossover programs and anything not tied to one lane. */
    var overhead: Double = 0.0,
    var otherRevenue: Double = 0.0,
    var cash: Double = 0.0,
) {
    val totalRevenue: Double get() = revenue.values.sum() + otherRevenue
    val totalCosts: Double get() = costs.values.sum() + overhead
    val profit: Double get() = totalRevenue - totalCosts

    fun profit(lane: Lane): Double = (revenue[lane] ?: 0.0) - (costs[lane] ?: 0.0)
}

data class Headquarters(val name: String, val rent: Double)

@Serializable
class GameState internal constructor(val seed: Long, val startingLane: Lane) {
    companion object {
        /** Fifteen years. */
        const val LENGTH_WEEKS = 780
        const val STARTING_CASH = 25_000.0

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            allowSpecialFloatingPointValues = true
        }

        /** Start a new company. Hardware and AI need a seed round from day one. */
        fun create(
            companyName: String,
            founderName: String,
            lane: Lane,
            seed: Long = Random.nextLong(),
            difficulty: Difficulty = Difficulty.NORMAL,
            legacy: List<LegacyPerk> = emptyList(),
            start: StartPoint = StartPoint.TODAY,
        ): GameState {
            val s = GameState(seed, lane)
            s.startYear = start.year
            GameClock.startYear = start.year
            s.companyName = companyName
            s.founder = Founder(founderName)
            s.difficulty = difficulty
            s.cash = difficulty.startingCash
            s.world.rivals += World.makeRivals().onEach { it.strength = (it.strength + difficulty.rivalStrength).clamped(0.05, 1.0) }
            s.world.eraLength = s.rng.int(70, 110)
            start.year?.let { s.world.startAt(it); s.adoptHistory() }
            s.announceEraResearch()
            s.ledger = listOf(WeekLedger(0, cash = s.cash))
            s.openLane(lane)
            s.refreshTalentPool()
            s.addLog("Day one. A laptop, a cramped apartment, and ${money(s.cash)} in savings.", LogTone.MILESTONE)
            s.applyLegacy(legacy)
            s.ledger = listOf(WeekLedger(0, cash = s.cash))
            if (lane.isCapitalIntensive) {
                s.ventureForLane(lane).getOrNull()?.let { round ->
                    s.takeRound(round)
                    s.addLog("${lane.title} eats money. You raised a ${money(round.amount)} seed round and gained a board.")
                }
            }
            // A run set in the past starts mid-history, with its era's challenge.
            if (s.world.era != Era.BOOTSTRAP_YEARS) s.beginEra()
            return s
        }

        fun decode(text: String): GameState = json.decodeFromString(serializer(), text).apply {
            GameClock.startYear = startYear
            for (r in world.rivals) World.renamed[r.name]?.let { r.name = it }
        }
    }

    fun encode(): String = json.encodeToString(serializer(), this)

    /** An independent copy. Actions on it never touch this state. */
    fun copy(): GameState = decode(encode())

    var companyName = ""
        internal set
    var founder = Founder("")
        internal set
    internal var rng = SeededRNG.fromSeed(seed)
    var week = 0
        internal set

    /** The year a Decades run started in, or null for a run that starts today. */
    var startYear: Int? = null
        internal set
    var lanes: List<Lane> = emptyList()
        internal set

    var games: GamesStudio? = null
        internal set
    var web: WebAgency? = null
        internal set
    var apps: AppStudio? = null
        internal set
    var social: SocialPlatform? = null
        internal set
    var hardware: HardwareLab? = null
        internal set
    var ai: AILab? = null
        internal set

    var staff: List<Employee> = emptyList()
        internal set
    var candidates: List<Candidate> = emptyList()
        internal set
    var cash = STARTING_CASH
        internal set
    var difficulty = Difficulty.NORMAL
        internal set
    var loan: Loan? = null
        internal set

    /** Listed on the stock market and still running. */
    var listing: Listing? = null
        internal set

    /** Playing on past an ending: no year-15 cutoff. */
    var freeplay = false
        internal set
    var crunch = false
        internal set
    val reputation = Reputation()
    val world = World()
    val venture = Venture()
    var crossovers: List<Crossover> = emptyList()
        internal set
    var pivot: PivotRecord? = null
        internal set
    var pendingPivot: PivotOffer? = null
        internal set
    var expansions: List<ExpansionRecord> = emptyList()
        internal set
    var events: List<PendingEvent> = emptyList()
        internal set

    /** Decisions that don't stop the clock. Each has a deadline; ignore it and the last choice is taken for you. */
    var inbox: List<PendingEvent> = emptyList()
        internal set

    /** Consequences waiting to land: a cover-up found out, a lawsuit decided, a burnt-out hire quitting. */
    var followUps: List<FollowUp> = emptyList()
        internal set

    /** The team works from home some days: happier, a little slower. */
    var hybrid = false
        internal set
    var log: List<LogEntry> = emptyList()
        internal set

    /** The press: the world's news, and stories about you. */
    var news: List<NewsStory> = emptyList()
        internal set
    var ledger: List<WeekLedger> = emptyList()
        internal set
    val stats = Stats()
    var ending: EndingReport? = null
        internal set
    var office = Office.APARTMENT
        internal set
    var researchPoints = 0.0
        internal set
    var researched: Set<Research> = emptySet()
        internal set

    /** Roadmap features built, and the ones being built. */
    var features: Set<Feature> = emptySet()
        internal set
    var building: List<FeatureBuild> = emptyList()
        internal set

    /** What's in the office besides desks. */
    var perks: Set<Perk> = emptySet()
        internal set

    /** Achievements earned, with the week each came. */
    var achievements: Map<Achievement, Int> = emptyMap()
        internal set

    /** Goals reached so far, in the order you reached them. */
    var goalsDone: List<Goal> = emptyList()
        internal set

    /** Who levelled up this week, for the office. */
    var levelUps: List<Int> = emptyList()
        internal set
    internal var idCounter = 1

    // Queries

    val isOver: Boolean get() = ending != null
    val needsDecision: Boolean get() = events.isNotEmpty() || pendingPivot != null
    val dateLabel: String get() = GameClock.label(week)
    val headquarters: Headquarters get() = Headquarters(office.title, office.rent)
    val payroll: Double get() = staff.sumOf { it.salary }

    fun crossover(combo: Combo): Crossover? = crossovers.firstOrNull { it.combo == combo }

    /** Combos you could build with your current lanes, built or not. */
    val availableCombos: List<Combo> get() = Combo.available(lanes)

    private fun window(weeks: Int, offset: Int): List<WeekLedger> {
        val end = max(0, ledger.size - offset)
        val start = max(0, end - weeks)
        return ledger.subList(start, end)
    }

    fun trailingRevenue(lane: Lane? = null, weeks: Int, offset: Int = 0): Double =
        window(weeks, offset).sumOf { w -> if (lane == null) w.totalRevenue else w.revenue[lane] ?: 0.0 }

    fun trailingProfit(lane: Lane? = null, weeks: Int): Double =
        window(weeks, 0).sumOf { w -> if (lane == null) w.profit else w.profit(lane) }

    /** Average weekly spend over the last month. */
    val weeklyBurn: Double
        get() {
            val w = window(4, 0)
            if (w.isEmpty()) return payroll + headquarters.rent
            return w.sumOf { it.totalCosts } / w.size
        }

    /** Weeks until the money runs out at the current net burn. Infinite when profitable. */
    val runwayWeeks: Double
        get() {
            // Before the first week closes, assume you're burning payroll and rent.
            val net = if (ledger.size > 1) trailingProfit(weeks = 4) / 4 else -(payroll + headquarters.rent)
            if (net >= 0) return Double.POSITIVE_INFINITY
            return max(0.0, cash) / -net
        }

    fun laneValue(lane: Lane): Double {
        // Blend the trailing year with the latest quarter so one launch spike doesn't dominate.
        val annual = 0.75 * trailingRevenue(lane, 52) + trailingRevenue(lane, 13)
        val assets = when (lane) {
            Lane.GAMES -> games?.assetValue
            Lane.WEBSITES -> web?.assetValue
            Lane.APPS -> apps?.assetValue
            Lane.SOCIAL -> social?.assetValue
            Lane.HARDWARE -> hardware?.assetValue
            Lane.AI -> ai?.assetValue
        } ?: 0.0
        return max(0.0, annual * lane.revenueMultiple * world.investorAppetite) + assets
    }

    /** What the businesses are worth, excluding cash in the bank. */
    val enterpriseValue: Double get() = lanes.sumOf { laneValue(it) } * (if (isEcosystem) 1.3 else 1.0) + subsidiaryValue

    /** Enterprise value plus cash, less what you owe the bank. What a buyer or the market would pay. */
    val valuation: Double get() = max(0.0, enterpriseValue + max(0.0, cash) - (loan?.balance ?: 0.0))

    fun staff(lane: Lane): List<Employee> = staff.filter { it.lane == lane }

    fun staffing(lane: Lane): Staffing {
        val s = Staffing()
        var skillSum = 0.0
        var designSum = 0.0
        var techSum = 0.0
        var weight = 0.0
        for (e in staff) {
            if (e.lane != lane) continue
            if (e.leaveUntil > week) continue
            // Night owls come alive in crunch.
            val owl = if (crunch && e.trait == Trait.NIGHT_OWL) 1.3 else 1.0
            s.output += e.output(lane) * owl
            s.teamOutput += e.output(lane) * owl
            when (e.trait) {
                Trait.PERFECTIONIST -> s.traitQuality += 1.5
                Trait.RAINMAKER -> s.traitDemand = minOf(1.08, s.traitDemand + 0.03)
                else -> {}
            }
            s.headcount += 1
            skillSum += e.skill
            designSum += e.design
            techSum += e.tech
            weight += 1
            val points = e.skill * e.moraleFactor
            when (e.role) {
                Role.MARKETER -> s.marketing += points
                Role.OPERATOR -> s.operations += points
                Role.RESEARCHER -> s.research += points
                else -> {}
            }
        }
        run {
            val share = founder.attentionShare(lane, lanes)
            val bonus = founder.skillBonus(lane)
            val freelance = if (founder.freelancing) 0.35 else 1.0
            s.output += (4 + bonus) * share * freelance
            s.marketing += 1 * share
            if (lane == Lane.AI) s.research += (3 + bonus) * share
            skillSum += (5 + bonus) * share
            designSum += (5 + bonus) * share
            techSum += (5 + bonus * 0.8) * share
            weight += share
        }
        s.averageSkill = if (weight > 0) skillSum / weight else 5.0
        s.avgDesign = if (weight > 0) designSum / weight else 5.0
        s.avgTech = if (weight > 0) techSum / weight else 5.0
        return s
    }

    internal fun context(lane: Lane, effects: ComboEffects? = null): LaneContext {
        val mods = effects?.mods?.get(lane)?.copy() ?: LaneModifiers()
        applyResearch(lane, mods)
        applyRoadmap(lane, mods)
        applyPressures(lane, mods)
        val people = staffing(lane)
        mods.quality += minOf(6.0, people.traitQuality)
        mods.demand *= people.traitDemand
        if (lane == Lane.HARDWARE) mods.cost *= world.hardwareCost
        mods.demand *= difficulty.sales
        mods.cost *= difficulty.costs
        if (isEcosystem) {
            mods.demand *= if (stats.ecosystemOpened) 1.08 else 1.15
            if (lane == Lane.APPS) mods.churn *= 0.9
        }
        return LaneContext(
            week = week, staffing = staffing(lane), mods = mods,
            reception = reputation.launchReception(lane), era = world.era, eraDemand = world.demand(lane), gpuPrice = world.gpuPrice,
            competition = world.competition(lane), crunch = crunch, lane = lane, year = calendarYear,
        )
    }

    /** Weekly compute bill for a training run of this size, at today's GPU prices. */
    fun trainingWeeklyCost(size: TrainingSize): Double =
        ai?.weeklyComputeCost(size.computeUnits, context(Lane.AI)) ?: 0.0

    /** One line per lane for dashboards. */
    fun statusLine(lane: Lane): String = when (lane) {
        Lane.GAMES -> games?.statusLine
        Lane.WEBSITES -> web?.statusLine
        Lane.APPS -> apps?.statusLine
        Lane.SOCIAL -> social?.statusLine
        Lane.HARDWARE -> hardware?.statusLine
        Lane.AI -> ai?.statusLine
    } ?: ""

    // Bookkeeping

    internal fun nextId(): Int = idCounter++

    internal fun addLog(text: String, tone: LogTone = LogTone.NEUTRAL, lane: Lane? = null) {
        val entry = LogEntry(nextId(), week, text, tone, lane)
        log = if (log.size >= 500) log.drop(log.size - 499) + entry else log + entry
    }

    /** Book a one-off cost against this week. */
    internal fun recordCost(amount: Double, lane: Lane?) {
        val w = ledger.lastOrNull() ?: return
        if (lane != null) w.costs[lane] = (w.costs[lane] ?: 0.0) + amount else w.overhead += amount
    }

    internal fun recordRevenue(amount: Double, lane: Lane?) {
        val w = ledger.lastOrNull() ?: return
        if (lane != null) w.revenue[lane] = (w.revenue[lane] ?: 0.0) + amount else w.otherRevenue += amount
        stats.totalRevenue += amount
    }

    internal fun spend(amount: Double, lane: Lane?) {
        cash -= amount
        recordCost(amount, lane)
    }

    internal fun takeRound(offer: RoundOffer) {
        venture.take(offer, week)
        cash += offer.amount
        addLog(
            "Closed a ${money(offer.amount)} ${offer.name}" + (if (offer.lead.isNotEmpty()) " led by ${offer.lead}" else "") +
                " at a ${money(offer.preMoney)} pre-money valuation. " +
                "You now own ${Format.percent(venture.founderEquity)}.",
            LogTone.MILESTONE,
        )
        publish(
            NewsDesk.YOU, "TechCrunch", "$companyName raises ${money(offer.amount)}" + (if (offer.lead.isNotEmpty()) ", led by ${offer.lead}" else ""),
            "A ${offer.name.lowercase()} at a ${money(offer.preMoney)} valuation.", LogTone.GOOD,
        )
    }

    internal fun openLane(lane: Lane) {
        if (lane in lanes) return
        lanes = lanes + lane
        founder.focus[lane] = 1.0
        when (lane) {
            Lane.GAMES -> games = GamesStudio()
            Lane.WEBSITES -> web = WebAgency()
            Lane.APPS -> apps = AppStudio(rng.pick(Names.apps), AppMonetization.ADS)
            Lane.SOCIAL -> social = SocialPlatform(rng.pick(Names.socials), rng.range(0.55, 1.35))
            Lane.HARDWARE -> hardware = HardwareLab()
            Lane.AI -> ai = AILab()
        }
        for (combo in Combo.available(lanes)) {
            if (crossover(combo) != null) continue
            crossovers = crossovers + Crossover(combo, buildWeeksLeft = combo.buildWeeks)
            addLog("New combo unlocked: ${combo.product}. ${combo.pitch}", LogTone.GOOD)
        }
    }

    internal fun closeLane(lane: Lane) {
        lanes = lanes - lane
        founder.focus.remove(lane)
        when (lane) {
            Lane.GAMES -> games = null
            Lane.WEBSITES -> web = null
            Lane.APPS -> apps = null
            Lane.SOCIAL -> social = null
            Lane.HARDWARE -> hardware = null
            Lane.AI -> ai = null
        }
        crossovers = crossovers.filterNot { it.combo.involves(lane) }
    }

    // Talent

    internal fun makeCandidate(role: Role? = null, star: Boolean = false): Candidate {
        val r = role ?: pickRole()
        val appeal = if (has(Perk.CHEF)) 0.5 else 0.0
        val skill = if (star) rng.int(8, 10) else rng.normal(5.0 + appeal + (if (Research.HIRING_PIPELINE in researched) 0.8 else 0.0), 1.8).roundToInt().clamped(1, 10)
        var salary = r.baseSalary * (0.5 + skill * 0.1) * rng.range(0.9, 1.15)
        if (r == Role.RESEARCHER && world.era == Era.AI_GOLD_RUSH) salary *= 1 + 0.6 * world.eraIntensity
        salary *= world.salaries
        var contested: String? = null
        var expires = week + 6
        val rival = rng.pick(world.rivals)
        if (star) {
            expires = week + 8
        } else if (rng.chance(0.15 + 0.3 * rival.aggression * (if (skill >= 7) 1.0 else 0.4))) {
            contested = rival.name
            salary *= 1.2
            expires = week + 2
        }
        // Talents scatter a little around what the role suggests.
        fun jitter(v: Int) = (v + rng.int(-1, 1)).clamped(1, 10)
        return Candidate(
            id = nextId(), name = "${rng.pick(Names.first)} ${rng.pick(Names.last)}", role = r,
            skill = skill, askingSalary = salary.roundToInt().toDouble(), contestedBy = contested, expiresWeek = expires,
            design = jitter(roleDesign(r, skill)), tech = jitter(roleTech(r, skill)), research = jitter(roleResearch(r, skill)),
            // Most people have something about them; a few are just easy to work with.
            trait = if (rng.chance(0.7)) rng.pick(Trait.entries) else null,
        )
    }

    private fun pickRole(): Role {
        // The talent pool leans toward what your lanes need.
        val lane = rng.pick(lanes.ifEmpty { listOf(startingLane) })
        val roles = Role.entries
        var roll = rng.unit() * roles.sumOf { lane.weight(it) }
        for (role in roles) {
            roll -= lane.weight(role)
            if (roll <= 0) return role
        }
        return Role.ENGINEER
    }

    internal fun refreshTalentPool() {
        for (c in candidates) {
            if (c.expiresWeek < week && c.contestedBy != null) addLog("${c.name} signed with ${c.contestedBy}.")
        }
        candidates = candidates.filter { it.expiresWeek >= week }
        if (has(Perk.ROOFTOP) && rng.chance(0.25) && candidates.none { it.skill >= 8 }) candidates = candidates + makeCandidate(star = true)
        while (candidates.size < 5) candidates = candidates + makeCandidate()
    }

    internal fun hireDirect(c: Candidate, lane: Lane? = null): Employee {
        val e = Employee(
            id = c.id, name = c.name, role = c.role, skill = c.skill, salary = c.askingSalary,
            morale = 0.75, lane = lane ?: bestLane(c.role), hiredWeek = week,
            design = c.design, tech = c.tech, research = c.research, trait = c.trait,
        )
        staff = staff + e
        stats.peakHeadcount = max(stats.peakHeadcount, staff.size)
        return e
    }

    internal fun bestLane(role: Role): Lane? = lanes.maxByOrNull { it.weight(role) }
}
