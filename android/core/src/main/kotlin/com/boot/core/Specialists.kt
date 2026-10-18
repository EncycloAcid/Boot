package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Specialists and team chemistry.
 *
 * Each specialist is best at the areas of the stage sliders their craft covers:
 * - writers: story and dialogues
 * - game designers: gameplay and level design
 * - engine programmers: engine and AI
 * - artists: world design and graphics
 * - composers: sound
 *
 * An area with a specialist on the game team gets more out of every week spent on it, and a game
 * whose genre's key areas had specialists on them reviews better. Bigger games need more than one.
 *
 * Some pairs of people work well together and some clash. You find out by working with them;
 * either way it changes what the whole game team gets done.
 */

/** What someone's best at. */
@Serializable
enum class Specialty(val title: String, val person: String, val areas: Set<FocusArea>) {
    WRITER("Writing", "Writer", setOf(FocusArea.STORY, FocusArea.DIALOGUES)),
    GAME_DESIGNER("Game design", "Game designer", setOf(FocusArea.GAMEPLAY, FocusArea.LEVEL_DESIGN)),
    ENGINE_PROGRAMMER("Engine programming", "Engine programmer", setOf(FocusArea.ENGINE, FocusArea.AI)),
    ARTIST("Art", "Artist", setOf(FocusArea.WORLD_DESIGN, FocusArea.GRAPHICS)),
    COMPOSER("Audio", "Composer", setOf(FocusArea.SOUND)),
    ;

    companion object {
        /** The specialty that covers [area]. */
        fun of(area: FocusArea): Specialty = entries.first { area in it.areas }
    }
}

/** How much more an area gets done with enough specialists on it. */
const val SPECIALIST_BOOST = 0.25

/** How many specialists an area wants, by the size of the game. */
fun specialistsNeeded(size: GameSize): Int = when (size) {
    GameSize.SMALL, GameSize.MEDIUM -> 1
    GameSize.LARGE -> 2
    GameSize.AAA -> 3
}

/** The game team's specialists who are in this week. */
fun GameState.specialists(): List<Employee> = staff(Lane.GAMES).filter { it.specialty != null && it.leaveUntil < week }

/** Whether [e] works on [area]: they do if they're put on it, or if they've been left to go where they're needed. */
fun worksOn(p: GameProject?, e: Employee, area: FocusArea): Boolean =
    p == null || (p.crew[crewKey(area.stage, e.id)]?.let { it == area } ?: true)

/** How well [area] is covered for a game of [size]: 0 (nobody) to 1 (enough people). */
fun GameState.coverage(area: FocusArea, size: GameSize, team: List<Employee> = specialists(), p: GameProject? = null): Double {
    val n = team.count { it.specialty?.areas?.contains(area) == true && worksOn(p, it, area) }
    return min(1.0, n.toDouble() / specialistsNeeded(size))
}

/** How much more a stage gets done from its specialists, weighted by where the effort goes. */
fun GameState.specialistBoost(concept: GameConcept, stage: Int, alloc: List<Double>, team: List<Employee> = specialists(), p: GameProject? = null): Double {
    val areas = FocusArea.forStage(stage)
    val total = alloc.sum().takeIf { it > 0 } ?: return 1.0
    return areas.indices.sumOf { i -> alloc[i] / total * (1 + SPECIALIST_BOOST * coverage(areas[i], concept.size, team, p)) }
}

// Putting people on areas

/** The key for someone's area in a stage. */
fun crewKey(stage: Int, id: Int): String = "$stage:$id"

/** The area [e] is on in [stage] of [p], or null if they go where they're needed. */
fun GameProject.areaOf(stage: Int, e: Employee): FocusArea? = crew[crewKey(stage, e.id)]

/** How someone's talent suits [area]: design for the design-heavy areas, tech for the rest. */
private fun talent(e: Employee, area: FocusArea): Double = (if (area.designShare >= 0.5) e.design else e.tech).toDouble()

/**
 * How much better (or worse) [stage] goes for who's on which area. People nobody placed spread
 * themselves where the effort goes, as before. Putting the right people on an area makes it
 * better; leaving an area short, or piling people where little effort goes, wastes them.
 */
fun GameState.crewBoost(p: GameProject, stage: Int): Double {
    val people = staff(Lane.GAMES).filter { it.leaveUntil < week }
    if (people.isEmpty()) return 1.0
    val picks = people.mapNotNull { e -> p.areaOf(stage, e)?.let { e to it } }
    if (picks.isEmpty()) return 1.0
    val areas = FocusArea.forStage(stage)
    val alloc = p.focus[stage]
    val total = alloc.sum().takeIf { it > 0 } ?: return 1.0
    val free = people.size - picks.size
    var quality = 0.0
    var mismatch = 0.0
    areas.forEachIndexed { i, area ->
        val share = alloc[i] / total
        val mine = picks.filter { it.second == area }.map { it.first }
        val everyone = people.map { talent(it, area) }.average().coerceAtLeast(1.0)
        val fit = if (mine.isEmpty()) 1.0 else (mine.map { talent(it, area) }.average() / everyone).coerceIn(0.6, 1.6)
        val placed = mine.size + free * share
        quality += share * (if (placed <= 0) 0.6 else (mine.size * fit + free * share) / placed)
        mismatch += kotlin.math.abs(placed / people.size - share)
    }
    // Mismatch runs 0 (people where the effort is) to 2 (all of them somewhere else).
    return (quality * (1 - 0.25 * mismatch)).coerceIn(0.6, 1.4)
}

/** Why [employeeId] can't be put on [area] in [stage] of [team]'s game, or null if they can. */
fun GameState.crewBlocker(team: Int, stage: Int, employeeId: Int, area: FocusArea?): String? {
    val p = games?.projectOf(team) ?: return "No game in development."
    if (p.isReady) return "It's content complete."
    if (stage < p.stage || stage > 2) return "That stage is done."
    if (area != null && area.stage != stage) return "${area.title} isn't in that stage."
    if (staff.none { it.id == employeeId && it.lane == Lane.GAMES }) return "They're not on the game team."
    return null
}

/** Put someone on an area of a stage, or (with null) let them go where they're needed. */
fun GameState.assignCrew(team: Int, stage: Int, employeeId: Int, area: FocusArea?) {
    requireLive()
    crewBlocker(team, stage, employeeId, area)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    if (area == null) p.crew.remove(crewKey(stage, employeeId)) else p.crew[crewKey(stage, employeeId)] = area
}

// Features that need a specialist

/** Engine features too advanced to build in without the right specialist. */
val SPECIALIST_FEATURES = setOf(
    Research.MOTION_CAPTURE, Research.FACIAL_ANIMATION, Research.NEURAL_NPCS, Research.WORLD_STREAMING,
    Research.GRAPHICS_3D_V4, Research.PHOTOREAL, Research.SPATIAL_AUDIO,
)

/** The specialist [r] needs, if any. */
fun Research.needs(): Specialty? = if (this in SPECIALIST_FEATURES) FeatureGroup.of(this)?.area?.let { Specialty.of(it) } else null

/** Why [r] can't go into a game, or null if it can. */
fun GameState.featureBlocker(r: Research): String? {
    val s = r.needs() ?: return null
    return if (specialists().none { it.specialty == s }) "Needs a ${s.person.lowercase()} on the game team." else null
}

/** [features] with any the team can't do stepped down a rung, or left out. */
fun GameState.fitFeatures(features: Set<Research>?, engine: Engine): Set<Research>? {
    if (features == null) return null
    return features.mapNotNull { r ->
        if (featureBlocker(r) == null) return@mapNotNull r
        val group = FeatureGroup.of(r) ?: return@mapNotNull null
        if (!group.ladder) return@mapNotNull null
        group.members.take(group.members.indexOf(r)).lastOrNull { it in engine.features && featureBlocker(it) == null }
    }.toSet()
}

/** The areas [concept]'s genre cares most about. */
fun keyAreas(concept: GameConcept): List<FocusArea> = FocusArea.entries.filter { concept.genre.weight(it) == 2 }

// Chemistry

/** How two people get on. */
@Serializable
enum class Chemistry(val title: String) {
    GOOD("Work well together"),
    NEUTRAL("Get along"),
    BAD("Clash"),
}

/** How [a] and [b] get on: fixed for a run, shaped a little by who they are. */
fun GameState.chemistry(a: Employee, b: Employee): Chemistry {
    if (a.id == b.id) return Chemistry.NEUTRAL
    val lo = min(a.id, b.id)
    val hi = max(a.id, b.id)
    val roll = mix(seed * 7_919L + lo * 104_729L + hi)
    val traits = setOf(a.trait, b.trait)
    // Two divas never get on; a team player gets on with nearly anyone; mentors make friends.
    if (a.trait == Trait.DIVA && b.trait == Trait.DIVA) return Chemistry.BAD
    val good = 0.12 + (if (Trait.MENTOR in traits) 0.1 else 0.0) + (if (Trait.TEAM_PLAYER in traits) 0.08 else 0.0)
    val bad = if (Trait.TEAM_PLAYER in traits) 0.0 else 0.08 + (if (Trait.DIVA in traits) 0.1 else 0.0) + (if (Trait.PERFECTIONIST in traits && Trait.HUSTLER in traits) 0.2 else 0.0)
    return when {
        roll < good -> Chemistry.GOOD
        roll > 1 - bad -> Chemistry.BAD
        else -> Chemistry.NEUTRAL
    }
}

/** A quick, well-spread number in 0...1 from [x] (SplitMix64): there can be a lot of pairs. */
private fun mix(x: Long): Double {
    var z = x + -0x61c8864680b583ebL
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1af7L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    z = z xor (z ushr 31)
    return (z ushr 11).toDouble() / (1L shl 53)
}

/** The most people whose pairings count: in a big team, people only really work with those near them. */
const val CHEMISTRY_TEAM = 40

/** The key for a pair, the same whichever way round. */
fun pairKey(a: Int, b: Int): String = "${min(a, b)}:${max(a, b)}"

/** Two people on the game team who don't just get along, and whether you know about it yet. */
data class TeamPair(val a: Employee, val b: Employee, val chemistry: Chemistry, val known: Boolean)

/** Every such pair on the game team. */
fun GameState.teamPairs(): List<TeamPair> {
    val team = staff(Lane.GAMES).take(CHEMISTRY_TEAM)
    val known = games?.knownPairs.orEmpty()
    return buildList {
        for (i in team.indices) for (j in i + 1 until team.size) {
            val c = chemistry(team[i], team[j])
            if (c != Chemistry.NEUTRAL) add(TeamPair(team[i], team[j], c, pairKey(team[i].id, team[j].id) in known))
        }
    }
}

/** What the game team's chemistry does to its output: good pairs lift it, clashes drag it down. */
fun GameState.chemistryFactor(pairs: List<TeamPair> = teamPairs()): Double {
    val good = pairs.count { it.chemistry == Chemistry.GOOD }
    val bad = pairs.count { it.chemistry == Chemistry.BAD }
    // Each pair counts for less in a bigger team.
    val people = min(CHEMISTRY_TEAM, staff(Lane.GAMES).size)
    val weight = min(1.0, 8.0 / max(1, people))
    return (1 + (0.04 * good - 0.06 * bad) * weight).coerceIn(0.8, 1.2)
}

/** Notes which areas of each game's current stage have enough specialists on them. */
internal fun GameState.noteCoverage(g: GamesStudio, weekStart: Boolean = false) {
    val team = specialists()
    for (p in listOfNotNull(g.project, g.project2)) {
        // Every stage worked this week counts: a small game can get through more than one.
        val from = if (weekStart) p.stage else p.weekStartStage
        val to = if (p.isReady) 2 else p.stage
        if (weekStart) p.weekStartStage = p.stage
        if (weekStart && p.isReady) continue
        for (stage in from..to) for (area in FocusArea.forStage(stage)) {
            if (coverage(area, p.concept.size, team, p) >= 1.0) p.covered += area
        }
    }
}

/** Each week of development: specialists' areas are noted, pairs find out how they get on, clashes wear people down. */
internal fun GameState.teamWeek(g: GamesStudio) {
    val pairs = teamPairs()
    for (pair in pairs) if (pair.chemistry == Chemistry.BAD) {
        pair.a.morale = max(0.0, pair.a.morale - 0.004)
        pair.b.morale = max(0.0, pair.b.morale - 0.004)
    }
    val projects = listOfNotNull(g.project, g.project2)
    if (projects.isEmpty()) return
    noteCoverage(g)
    // Working side by side, people find out how they get on.
    for (pair in pairs.filter { !it.known }) {
        if (!flavour("pair ${pairKey(pair.a.id, pair.b.id)}").chance(0.1)) continue
        g.knownPairs += pairKey(pair.a.id, pair.b.id)
        if (pair.chemistry == Chemistry.GOOD) {
            addLog("${pair.a.name} and ${pair.b.name} work really well together. The whole team's quicker for it.", LogTone.GOOD, Lane.GAMES)
        } else {
            addLog("${pair.a.name} and ${pair.b.name} keep clashing. It's slowing everyone down.", LogTone.BAD, Lane.GAMES)
        }
        break
    }
}

// Becoming a specialist

/** What a specialist course costs: research points and cash. */
fun GameState.specialiseCost(e: Employee): Pair<Int, Double> = 6 to scaled(3_000.0 + 800.0 * e.skill)

/** How long the course takes. */
const val SPECIALISE_WEEKS = 3

/** Why [e] can't become a [specialty] specialist, or null if they can. */
fun GameState.specialiseBlocker(e: Employee, specialty: Specialty): String? {
    if (games == null) return "You don't make games."
    if (e.role != Role.DESIGNER && e.role != Role.ENGINEER) return "Only designers and engineers make games."
    if (e.specialty == specialty) return "${e.name} is already a ${specialty.person.lowercase()}."
    if (e.leaveUntil >= week) return "${e.name} is away."
    if (week - e.specializedWeek < 52) return "${e.name} changed specialty this year."
    val (rp, cost) = specialiseCost(e)
    if (researchPoints < rp) return "Needs $rp RP."
    if (cash < cost) return "Costs ${money(cost)}."
    return null
}

/** Send [employeeId] on a course to become a [specialty] specialist. They're away for a few weeks. */
fun GameState.specialise(employeeId: Int, specialty: Specialty) {
    requireLive()
    val e = staff.firstOrNull { it.id == employeeId } ?: throw invalid("No such employee.")
    specialiseBlocker(e, specialty)?.let { throw notAvailable(it) }
    val (rp, cost) = specialiseCost(e)
    researchPoints -= rp
    spend(cost, e.lane)
    e.specialty = specialty
    e.specializedWeek = week
    e.leaveUntil = week + SPECIALISE_WEEKS
    addLog("${e.name} is off on a ${specialty.title.lowercase()} course: back in ${Format.weeks(SPECIALISE_WEEKS)} as a ${specialty.person.lowercase()}.", LogTone.NEUTRAL, e.lane)
}

/** A specialty for someone joining: about half of designers and engineers have one. */
internal fun GameState.specialtyFor(id: Int, role: Role): Specialty? {
    val dice = SeededRNG.fromSeed(seed * 31L + id * 7_877L)
    if (role != Role.DESIGNER && role != Role.ENGINEER) return null
    if (!dice.chance(0.55)) return null
    return if (role == Role.ENGINEER) {
        if (dice.chance(0.75)) Specialty.ENGINE_PROGRAMMER else dice.pick(listOf(Specialty.ARTIST, Specialty.COMPOSER))
    } else {
        dice.pick(listOf(Specialty.WRITER, Specialty.GAME_DESIGNER, Specialty.ARTIST, Specialty.COMPOSER))
    }
}

/** What the genre's key areas got from specialists, for the review: +0.15 each, up to +0.45. */
fun specialistPoints(concept: GameConcept, covered: Set<FocusArea>): Double =
    min(0.45, keyAreas(concept).count { it in covered } * 0.15)
