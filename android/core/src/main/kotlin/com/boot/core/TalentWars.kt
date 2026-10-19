package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Talent wars: stars with visible career arcs, loyalty, and poaching that costs something.
 * Specialists, hire/fire/train and rival poach events stay the hooks — this layer gives people
 * a reputation, a chapter (rising / peak / fading / legend), past titles and hits, and "wanted by"
 * pressure that shows on the Team screen, not only as a one-line news beat.
 */

/** Where someone sits in their career. Old saves default to Rising. */
@Serializable
enum class CareerChapter(val title: String, val detail: String) {
    RISING("Rising", "Still building a name."),
    PEAK("Peak", "At the top of their game."),
    FADING("Fading", "Past their brightest years — still skilled."),
    LEGEND("Legend", "A name everyone in the industry knows."),
}

/** A star is someone rivals bother to fight over: high skill, notable hire, or a known name. */
val Employee.isStar: Boolean
    get() = notable || skill >= 8 || reputation >= 0.4 || (specialty != null && skill >= 7)

/** Loyalty used for poaches: the Loyal trait never listens; everyone else uses the 0…1 meter. */
val Employee.effectiveLoyalty: Double
    get() = if (trait == Trait.LOYAL) 1.0 else loyalty.clamped(0.0, 1.0)

/** How hard rivals are circling them right now. */
fun Employee.offerLine(): String? = when {
    offerPressure <= 0.05 && wantedBy.isEmpty() -> null
    wantedBy.isNotEmpty() && offerPressure >= 0.45 -> "Wanted by ${wantedBy.joinToString(" · ")} — heavy offer pressure"
    wantedBy.isNotEmpty() -> "Wanted by ${wantedBy.joinToString(" · ")}"
    offerPressure >= 0.45 -> "Heavy offer pressure"
    else -> "Quiet interest from other studios"
}

/** One-line career summary for Team / person card. */
fun Employee.careerLine(): String {
    val bits = mutableListOf(chapter.title)
    if (specialty != null) bits += specialty!!.person
    if (pastHits.isNotEmpty()) bits += pastHits.take(2).joinToString(", ")
    else if (pastTitles.isNotEmpty()) bits += pastTitles.take(2).joinToString(", ")
    return bits.joinToString(" · ")
}

/** Stars currently on the payroll, hottest first. */
fun GameState.stars(lane: Lane? = null): List<Employee> =
    staff.filter { it.isStar && it.leaveUntil < week && (lane == null || it.lane == lane) }
        .sortedWith(compareByDescending<Employee> { it.reputation }.thenByDescending { it.skill })

/**
 * Fixed-term / contract labour on the team. Empty until Industry Pressure (or similar)
 * sets [Employee.contractUntil]. Team screen lists these above permanent staff when present.
 */
fun GameState.temps(lane: Lane? = null): List<Employee> =
    staff.filter { it.isTemp(week) && (lane == null || it.lane == lane) }
        .sortedBy { it.contractUntil }

/** Permanent staff — everyone not on an active contract. */
fun GameState.permanents(lane: Lane? = null): List<Employee> =
    staff.filter { !it.isTemp(week) && (lane == null || it.lane == lane) }

/** Market candidates rivals would fight over. */
fun GameState.marketStars(): List<Candidate> =
    candidates.filter { it.notable || it.skill >= 8 || it.reputation >= 0.4 }

internal fun careerChapterForHire(skill: Int, reputation: Double, hits: Int, notable: Boolean): CareerChapter = when {
    reputation >= 0.85 || hits >= 3 -> CareerChapter.LEGEND
    notable || skill >= 8 || reputation >= 0.45 -> CareerChapter.PEAK
    skill <= 5 && reputation < 0.2 -> CareerChapter.RISING
    else -> if (skill >= 7) CareerChapter.PEAK else CareerChapter.RISING
}

/** Seed career fields for a candidate — stars get past titles/hits so they feel real.
 *  Uses a private RNG from the candidate id so hiring does not perturb the week's seed stream. */
internal fun GameState.seedCareer(c: Candidate, star: Boolean): Candidate {
    val dice = SeededRNG.fromSeed(seed * 1_009L + c.id * 97_331L + if (star) 17L else 0L)
    if (!star && c.skill < 8 && c.specialty == null) {
        return c.copy(
            reputation = (c.skill / 20.0).clamped(0.0, 0.35),
            loyalty = dice.range(0.4, 0.7),
            chapter = CareerChapter.RISING,
            notable = false,
        )
    }
    val notable = star || c.skill >= 8
    val hits = if (notable) dice.int(0, if (star) 3 else 1) else 0
    val titles = buildList {
        if (c.specialty != null) add(c.specialty.person)
        if (notable && dice.chance(0.55)) add(
            when (c.role) {
                Role.DESIGNER -> dice.pick(listOf("Lead designer", "Creative lead", "Design director"))
                Role.ENGINEER -> dice.pick(listOf("Lead engineer", "Principal engineer", "Tech lead"))
                Role.RESEARCHER -> "Principal researcher"
                Role.MARKETER -> "Brand lead"
                Role.OPERATOR -> "Studio ops lead"
            },
        )
        if (hits >= 2 && dice.chance(0.4)) add("Studio veteran")
    }.distinct()
    val pastHits = if (hits <= 0) emptyList() else (1..hits).map {
        val topic = dice.pick(Topic.entries)
        val genre = dice.pick(Genre.entries)
        gameTitle(topic, genre, dice.int(0, 1_000_000).toLong(), emptySet(), calendarYear)
    }
    val reputation = when {
        hits >= 3 -> dice.range(0.75, 0.95)
        hits >= 1 || star -> dice.range(0.45, 0.8)
        c.skill >= 8 -> dice.range(0.35, 0.6)
        else -> dice.range(0.15, 0.4)
    }
    // Poached / contested talent arrives less loyal.
    val loyalty = when {
        c.contestedBy != null -> dice.range(0.25, 0.5)
        star -> dice.range(0.3, 0.55)
        else -> dice.range(0.45, 0.75)
    }
    return c.copy(
        reputation = reputation,
        loyalty = loyalty,
        chapter = careerChapterForHire(c.skill, reputation, hits, notable),
        pastTitles = titles,
        pastHits = pastHits,
        notable = notable,
    )
}

/** What it costs to counter a poach offer on [e], before the usual 40% raise. */
fun GameState.poachRetainCost(e: Employee): Double =
    e.salary * (1.35 + 0.25 * e.reputation + when (e.chapter) {
        CareerChapter.LEGEND -> 0.35
        CareerChapter.PEAK -> 0.2
        CareerChapter.FADING -> 0.05
        CareerChapter.RISING -> 0.1
    })

/** Signing bonus to lure a market or rival star. */
fun GameState.poachSigningCost(skill: Int, reputation: Double, chapter: CareerChapter, lane: Lane? = null): Double {
    val inLane = lane ?: lanes.firstOrNull() ?: Lane.GAMES
    val base = max(40_000.0, stake(inLane) * 2)
    val fame = 1.0 + reputation * 1.4 + when (chapter) {
        CareerChapter.LEGEND -> 0.8
        CareerChapter.PEAK -> 0.35
        CareerChapter.FADING -> 0.15
        CareerChapter.RISING -> 0.1
    } + (skill - 7).coerceAtLeast(0) * 0.12
    return base * fame
}

/** Why you can't hire this contested/market star right now. */
fun GameState.poachMarketBlocker(candidateId: Int): String? {
    val c = candidates.firstOrNull { it.id == candidateId } ?: return "They're gone."
    if (!(c.notable || c.skill >= 8 || c.reputation >= 0.4)) return "Not a star hire."
    if (freeDesks <= 0) return "No free desk for a new hire."
    val into = c.role.let { bestLane(it) }
    val cost = poachSigningCost(c.skill, c.reputation, c.chapter, into) + c.askingSalary * 2
    if (cash < cost) return "Costs ${money(cost)} (signing + fee)."
    return null
}

/**
 * Hire a market star (or contested candidate) with a proper signing bonus.
 * Extends [hire] — contested names become a poach from that rival.
 */
fun GameState.poachMarketStar(candidateId: Int, lane: Lane? = null) {
    requireLive()
    poachMarketBlocker(candidateId)?.let { throw notAvailable(it) }
    val c = candidates.firstOrNull { it.id == candidateId } ?: throw invalid("They're gone.")
    val into = lane ?: bestLane(c.role)
    val signing = poachSigningCost(c.skill, c.reputation, c.chapter, into)
    spend(signing + c.askingSalary * 2, into)
    candidates = candidates.filterNot { it.id == candidateId }
    val e = hireDirect(c.copy(contestedBy = null), into)
    e.loyalty = (e.loyalty * 0.85).clamped(0.15, 0.65)
    e.offerPressure = 0.0
    e.wantedBy = emptyList()
    val from = c.contestedBy
    if (from != null) {
        world.rivals.firstOrNull { it.name == from }?.let {
            it.strength = max(0.05, it.strength - 0.025)
            it.heat = min(1.0, it.heat + 0.12)
        }
        addLog("You poached ${e.name} from $from's orbit.", LogTone.GOOD, into)
        publish(
            NewsDesk.INDUSTRY, techOutlet(into ?: Lane.GAMES, rng),
            "$companyName signs ${e.name} away from $from",
            "${e.chapter.title} ${e.role.title.lowercase()} · ${e.careerLine()}",
            LogTone.GOOD, into, tag = NewsTag.NEWS,
        )
        feedOnRivalBeat(from, "poach_fail", e.name)
    } else {
        addLog("You signed star hire ${e.name}.", LogTone.GOOD, into)
        publish(
            NewsDesk.INDUSTRY, techOutlet(into ?: Lane.GAMES, rng),
            "$companyName lands ${e.name}",
            "${e.chapter.title} talent joins the team.",
            LogTone.GOOD, into, tag = NewsTag.NEWS,
        )
    }
}

/** Build a star who "comes from" rival [r] — used by CounterMove.POACH_STAR. */
internal fun GameState.hireRivalStar(r: Rival): Employee {
    val c0 = makeCandidate(star = true)
    val dice = SeededRNG.fromSeed(seed * 4_241L + c0.id * 13L + r.name.hashCode())
    val c = c0.copy(
        askingSalary = c0.askingSalary * 1.3,
        contestedBy = null,
        pastTitles = (c0.pastTitles + "${r.name} alum").distinct(),
        loyalty = dice.range(0.2, 0.45),
    )
    val e = hireDirect(c, r.lane)
    e.wantedBy = emptyList()
    e.offerPressure = 0.0
    return e
}

/** Cost of CounterMove.POACH_STAR against [r], scaled by how famous the lure is. */
fun GameState.poachStarCost(r: Rival): Double {
    // Don't consume IDs / RNG from the live game when pricing the button — use a fixed fame band.
    return poachSigningCost(9, 0.55, CareerChapter.PEAK, r.lane).coerceAtLeast(max(40_000.0, stake(r.lane) * 2))
}

/** Credit the games team when a release ships / hits — grows arcs and past hits. */
internal fun GameState.creditCareerOnShip(title: String, score: Double) {
    val team = staff(Lane.GAMES).filter { it.leaveUntil < week }
    if (team.isEmpty()) return
    val hit = score >= 8.0
    for (e in team) {
        val bump = when {
            hit && e.isStar -> 0.08
            hit -> 0.05
            score >= 6.5 -> 0.02
            else -> 0.005
        }
        e.reputation = (e.reputation + bump).clamped(0.0, 1.0)
        if (hit) {
            if (title !in e.pastHits) e.pastHits = (listOf(title) + e.pastHits).take(6)
            e.loyalty = (e.loyalty + 0.03).clamped(0.0, 1.0)
            if (e.notable || e.skill >= 7) maybeAddTitle(e, when (e.role) {
                Role.DESIGNER -> "Hit designer"
                Role.ENGINEER -> "Hit engineer"
                else -> "Hit maker"
            })
        }
        advanceChapter(e, announce = hit && e.isStar)
    }
}

private fun maybeAddTitle(e: Employee, title: String) {
    if (title in e.pastTitles) return
    e.pastTitles = (e.pastTitles + title).take(8)
}

/** Recompute chapter from reputation / hits / skill; optionally news when it changes. */
internal fun GameState.advanceChapter(e: Employee, announce: Boolean) {
    val next = when {
        e.chapter == CareerChapter.LEGEND -> CareerChapter.LEGEND
        e.reputation >= 0.88 || e.pastHits.size >= 3 && e.skill >= 8 -> CareerChapter.LEGEND
        e.chapter == CareerChapter.FADING && (e.reputation >= 0.55 || e.pastHits.isNotEmpty() && e.skill >= 8) ->
            if (e.reputation >= 0.7) CareerChapter.PEAK else CareerChapter.FADING
        e.chapter == CareerChapter.PEAK && e.reputation < 0.28 && e.skill <= 7 && week - e.hiredWeek > 156 ->
            CareerChapter.FADING
        e.chapter == CareerChapter.RISING && (e.skill >= 8 || e.reputation >= 0.4 || e.notable && e.level >= 3) ->
            CareerChapter.PEAK
        e.chapter == CareerChapter.PEAK -> CareerChapter.PEAK
        e.chapter == CareerChapter.FADING -> CareerChapter.FADING
        else -> CareerChapter.RISING
    }
    if (next == e.chapter) return
    val from = e.chapter
    e.chapter = next
    if (!announce) return
    addLog("${e.name} enters a new chapter: ${next.title}.", LogTone.MILESTONE, e.lane)
    publish(
        NewsDesk.INDUSTRY, techOutlet(e.lane ?: Lane.GAMES, rng),
        "${e.name}: from ${from.title.lowercase()} to ${next.title.lowercase()}",
        e.careerLine(),
        if (next == CareerChapter.LEGEND || next == CareerChapter.PEAK) LogTone.GOOD else LogTone.NEUTRAL,
        e.lane, tag = NewsTag.NEWS,
    )
    feedOnCareerChapter(e.name, next)
}

/** Weekly loyalty drift, offer pressure, chapter ticks, and rival circling on stars. */
internal fun GameState.talentWeek() {
    for (e in staff) {
        // Loyal trait floors the meter; everyone else drifts toward morale and raises.
        if (e.trait == Trait.LOYAL) {
            e.loyalty = 1.0
        } else {
            val toward = (0.35 + e.morale * 0.45 + if (week - e.lastRaiseWeek < 26) 0.1 else 0.0).clamped(0.15, 0.9)
            e.loyalty = (e.loyalty + (toward - e.loyalty) * 0.04).clamped(0.0, 1.0)
        }
        e.offerPressure = (e.offerPressure * 0.92).clamped(0.0, 1.0)
        if (e.offerPressure < 0.08) e.wantedBy = emptyList()
        else if (e.wantedBy.size > 3) e.wantedBy = e.wantedBy.take(3)
        // Slow fade for long-tenured peak people who aren't shipping.
        if (e.chapter == CareerChapter.PEAK && week - e.hiredWeek > 200 && e.pastHits.isEmpty() && e.reputation < 0.5) {
            e.reputation = (e.reputation - 0.002).clamped(0.0, 1.0)
        }
        if (week % 13 == 0) advanceChapter(e, announce = true)
    }
    // Rivals quietly mark your stars — pressure before a full poach event.
    for (r in world.rivals) {
        if (r.absorbed || r.quietUntil > week) continue
        if (r.lane !in lanes) continue
        if (r.roster && Lane.GAMES !in lanes) continue
        val mark = stars(r.lane).filter { it.effectiveLoyalty < 0.92 }.maxByOrNull { it.reputation * 2 + it.skill }
            ?: continue
        val chance = r.aggression * 0.01 * (1.0 - mark.effectiveLoyalty) * (0.5 + mark.reputation)
        if (!rng.chance(chance)) continue
        if (r.name !in mark.wantedBy) mark.wantedBy = mark.wantedBy + r.name
        mark.offerPressure = (mark.offerPressure + 0.12 + 0.1 * r.aggression).clamped(0.0, 1.0)
    }
}

/** Soft weekly poach roll — prefers stars under pressure; loyalty resists. */
internal fun GameState.tryRivalPoach(r: Rival) {
    val pool = staff.filter {
        it.lane == r.lane && it.skill >= 6 && it.effectiveLoyalty < 0.98 && it.leaveUntil < week
    }
    if (pool.isEmpty()) return
    val best = pool.maxByOrNull { e ->
        val star = if (e.isStar) 3.0 else 1.0
        val pressure = 1.0 + e.offerPressure
        (e.skill + e.reputation * 6) * star * pressure * (1.0 - e.effectiveLoyalty * 0.7)
    } ?: return
    val stock = if (Research.STOCK_OPTIONS in researched) 0.4 else 1.0
    val poachChance = r.aggression * 0.006 * stock * (1.0 - best.effectiveLoyalty * 0.85) *
        (if (best.isStar) 1.35 else 0.85) * (1.0 + best.offerPressure * 0.5)
    if (!rng.chance(poachChance)) return
    if (r.name !in best.wantedBy) best.wantedBy = best.wantedBy + r.name
    best.offerPressure = (best.offerPressure + 0.25).clamped(0.0, 1.0)
    enqueue(EventKind.Poach(best.id, r.name))
    if (r.lane == Lane.GAMES || best.isStar) {
        publish(
            NewsDesk.INDUSTRY, techOutlet(r.lane, rng),
            "${r.name} tries to poach ${best.name}",
            "${best.chapter.title} ${best.role.title.lowercase()} · loyalty ${Format.percent(best.effectiveLoyalty)}",
            LogTone.BAD, r.lane, tag = NewsTag.NEWS,
        )
        feedOnRivalBeat(r.name, "poach", best.name)
    }
}

/** Resolve keeping or losing someone in a poach — loyalty and news. */
internal fun GameState.resolvePoachOutcome(e: Employee, rival: String, kept: Boolean) {
    if (kept) {
        e.loyalty = (e.loyalty + 0.18).clamped(0.0, 1.0)
        e.morale = min(1.0, e.morale + 0.15)
        e.offerPressure = 0.0
        e.wantedBy = e.wantedBy.filterNot { it == rival }
        addLog("${e.name} stays, at a price.", LogTone.GOOD, e.lane)
        publish(
            NewsDesk.INDUSTRY, techOutlet(e.lane ?: Lane.GAMES, rng),
            "${e.name} turns down $rival",
            "A counter-offer keeps the ${e.chapter.title.lowercase()} ${e.role.title.lowercase()}.",
            LogTone.GOOD, e.lane, tag = NewsTag.NEWS,
        )
        feedOnRivalBeat(rival, "poach_fail", e.name)
    } else {
        staff = staff - e
        strengthenRival(rival, 0.03 + 0.02 * e.reputation)
        // Remaining stars feel it.
        for (o in staff.filter { it.lane == e.lane && it.isStar }) {
            o.loyalty = (o.loyalty - 0.06).clamped(0.0, 1.0)
            o.morale = (o.morale - 0.04).clamped(0.0, 1.0)
        }
        addLog("${e.name} left for $rival.", LogTone.BAD, e.lane)
        publish(
            NewsDesk.INDUSTRY, techOutlet(e.lane ?: Lane.GAMES, rng),
            "$rival poaches ${e.name} from $companyName",
            "${e.chapter.title} · ${e.careerLine()}",
            LogTone.BAD, e.lane, tag = NewsTag.NEWS,
        )
        feedOnRivalBeat(rival, "poach_win", e.name)
    }
}

/** When someone specialises, that title joins their arc. */
internal fun GameState.noteSpecialistTitle(e: Employee) {
    val title = e.specialty?.person ?: return
    maybeAddTitle(e, title)
    e.reputation = (e.reputation + 0.04).clamped(0.0, 1.0)
    advanceChapter(e, announce = true)
}
