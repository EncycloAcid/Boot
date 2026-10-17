package com.boot.core

import kotlin.math.max

/*
 * Making a game the way Game Dev Tycoon does it: stage by stage, you choose which of your engine's
 * features go into each part of the game and who leads each part.
 *
 * - Features come from the engine the game is built on. Each belongs to one area (graphics, sound,
 *   AI...). Some are ladders (stereo, then surround, then spatial audio: pick how far up to go);
 *   some are a choice of one (an art style); the rest are on or off. Every feature costs money
 *   when its stage starts and brings a few bugs, and makes that stage's work count for more. A
 *   feature counts for more in an area that matters to the genre, and for little in one that
 *   doesn't: voice acting in a puzzle game is money down the drain.
 * - A lead for an area makes that area's work count for more, by how good they are at it: design
 *   for the design-heavy areas, tech for the rest. Nobody leads two areas in the same stage.
 * - A game that never had its features chosen (an old save, or one started outside the designer)
 *   is built the old way: the engine's whole kit, free, with no boost.
 */

/** A set of engine features that go in the same place in a game. */
enum class FeatureGroup(val title: String, val area: FocusArea, val members: List<Research>, val ladder: Boolean = false) {
    // Stage 1
    SAVING("Saving", FocusArea.ENGINE, listOf(Research.SAVE_SYSTEM)),
    PHYSICS("Physics", FocusArea.ENGINE, listOf(Research.PHYSICS)),
    ONLINE("Online", FocusArea.ENGINE, listOf(Research.MULTIPLAYER, Research.ONLINE_SERVICES, Research.CROSS_PLAY), ladder = true),
    CLOUD("Cloud", FocusArea.ENGINE, listOf(Research.CLOUD_SAVES, Research.CLOUD_STREAMING)),
    VR("VR", FocusArea.ENGINE, listOf(Research.VR_SUPPORT)),
    ANTI_CHEAT("Anti-cheat", FocusArea.ENGINE, listOf(Research.ANTI_CHEAT)),
    ACHIEVEMENTS("Achievements", FocusArea.GAMEPLAY, listOf(Research.ACHIEVEMENTS)),
    PHOTO_MODE("Photo mode", FocusArea.GAMEPLAY, listOf(Research.PHOTO_MODE)),
    HAPTICS("Haptics", FocusArea.GAMEPLAY, listOf(Research.HAPTICS)),
    BRANCHING("Branching story", FocusArea.STORY, listOf(Research.BRANCHING_STORY)),
    CUTSCENES("Cutscenes", FocusArea.STORY, listOf(Research.CINEMATIC_CUTSCENES)),
    DESTRUCTION("Destruction", FocusArea.GAMEPLAY, listOf(Research.DESTRUCTION_PHYSICS)),
    FMV("Full-motion video", FocusArea.STORY, listOf(Research.FMV)),
    MOCAP("Motion capture", FocusArea.STORY, listOf(Research.MOTION_CAPTURE)),

    // Stage 2
    DIALOGUE_TREES("Dialogue trees", FocusArea.DIALOGUES, listOf(Research.DIALOGUE_TREES)),
    VOICE("Voice acting", FocusArea.DIALOGUES, listOf(Research.VOICE_ACTING)),
    FACES("Facial animation", FocusArea.DIALOGUES, listOf(Research.FACIAL_ANIMATION)),
    PROCEDURAL("Procedural levels", FocusArea.LEVEL_DESIGN, listOf(Research.PROCEDURAL)),
    LEVEL_TOOLS("Level tools", FocusArea.LEVEL_DESIGN, listOf(Research.LEVEL_EDITOR, Research.MOD_TOOLS), ladder = true),
    AI("AI", FocusArea.AI, listOf(Research.BASIC_AI, Research.ADVANCED_AI, Research.NEURAL_NPCS), ladder = true),
    COMPANIONS("Companions", FocusArea.AI, listOf(Research.COMPANION_AI)),

    // Stage 3
    OPEN_WORLD("Open world", FocusArea.WORLD_DESIGN, listOf(Research.OPEN_WORLD, Research.WORLD_STREAMING), ladder = true),
    WORLD_SIM("World simulation", FocusArea.WORLD_DESIGN, listOf(Research.DAY_NIGHT, Research.DYNAMIC_WEATHER), ladder = true),
    GRAPHICS(
        "Graphics", FocusArea.GRAPHICS,
        listOf(Research.GRAPHICS_2D_V2, Research.GRAPHICS_3D_V1, Research.GRAPHICS_3D_V2, Research.GRAPHICS_3D_V3, Research.GRAPHICS_3D_V4, Research.PHOTOREAL),
        ladder = true,
    ),
    ART_STYLE(
        "Art style", FocusArea.GRAPHICS,
        listOf(Research.STYLE_PIXEL, Research.STYLE_HAND_PAINTED, Research.STYLE_STYLIZED, Research.STYLE_REALISTIC, Research.STYLE_ANIME, Research.STYLE_VOXEL),
    ),
    LIGHTING("Lighting", FocusArea.GRAPHICS, listOf(Research.DYNAMIC_LIGHTING)),
    EFFECTS("Effects", FocusArea.GRAPHICS, listOf(Research.PARTICLES)),
    SOUND("Sound", FocusArea.SOUND, listOf(Research.SOUND_STEREO, Research.SOUND_SURROUND, Research.SPATIAL_AUDIO), ladder = true),
    MUSIC("Music", FocusArea.SOUND, listOf(Research.ORCHESTRAL_SCORE, Research.ADAPTIVE_MUSIC), ladder = true),
    ;

    /** What picking [r] is worth: up a ladder, every rung below counts too. */
    fun points(r: Research): Int = if (ladder) members.take(members.indexOf(r) + 1).sumOf { it.engineLevel } else r.engineLevel

    companion object {
        fun of(r: Research): FeatureGroup? = entries.firstOrNull { r in it.members }
        fun forStage(stage: Int): List<FeatureGroup> = entries.filter { it.area.stage == stage }
    }
}

/** The groups a game on [engine] can use in [stage], with the members the engine has. */
fun engineGroups(engine: Engine, stage: Int): List<Pair<FeatureGroup, List<Research>>> =
    FeatureGroup.forStage(stage).mapNotNull { g -> g.members.filter { it in engine.features }.takeIf { it.isNotEmpty() }?.let { g to it } }

/** The best of every group an engine offers: what the designer starts with. */
fun defaultFeatures(engine: Engine): Set<Research> =
    (0..2).flatMap { stage -> engineGroups(engine, stage).map { (g, have) -> if (g.ladder) have.last() else have.first() } }.toSet()

/** What putting [r] into a game of [size] costs. */
fun featureCost(r: Research, size: GameSize, year: Int = MODERN_YEAR): Double {
    val perPoint = when (size) {
        GameSize.SMALL -> 30.0
        GameSize.MEDIUM -> 200.0
        GameSize.LARGE -> 1_000.0
        GameSize.AAA -> 3_000.0
    }
    return max(100.0, r.rp * perPoint * pastScale(year.toDouble()))
}

/** What the features chosen for [stage] cost. */
fun stageFeatureCost(features: Set<Research>, stage: Int, size: GameSize, year: Int = MODERN_YEAR): Double =
    features.filter { FeatureGroup.of(it)?.area?.stage == stage }.sumOf { featureCost(it, size, year) }

/** How much more a stage's work counts for, from the features chosen for it. 1 when none or never chosen. */
fun featureBoost(concept: GameConcept, features: Set<Research>?, stage: Int): Double {
    if (features == null) return 1.0
    val weighted = features.sumOf { r ->
        val g = FeatureGroup.of(r) ?: return@sumOf 0.0
        if (g.area.stage != stage) 0.0 else g.points(r) * concept.idealShare(g.area) * 3
    }
    return 1 + 0.025 * weighted
}

/** Bugs a stage's features bring when it starts: more moving parts. */
fun featureBugs(features: Set<Research>?, stage: Int): Double =
    features.orEmpty().filter { FeatureGroup.of(it)?.area?.stage == stage }.sumOf { 1.5 * it.engineLevel }

/** How much better a stage's work gets from its leads: up to a third more in each area they lead. */
fun leadBoost(concept: GameConcept, leads: Map<FocusArea, Employee>, stage: Int, alloc: List<Double>): Double {
    val areas = FocusArea.forStage(stage)
    val total = alloc.sum().takeIf { it > 0 } ?: return 1.0
    return areas.indices.sumOf { i ->
        val lead = leads[areas[i]]
        val bonus = if (lead == null) 0.0 else 0.33 * leadFit(lead, areas[i])
        alloc[i] / total * (1 + bonus)
    }
}

/** How good someone is at leading [area], 0...1: design for the design-heavy areas, tech for the rest. */
fun leadFit(e: Employee, area: FocusArea): Double =
    ((if (area.designShare >= 0.5) e.design else e.tech) / 10.0 * e.moraleFactor + (if (e.specialty?.areas?.contains(area) == true) 0.2 else 0.0)).coerceIn(0.0, 1.0)

/** The game staff who could lead an area of [team]'s game. */
fun GameState.leadCandidates(): List<Employee> = staff(Lane.GAMES).filter { it.leaveUntil < week }

/** Who leads each area of [p], as people. */
fun GameState.leadsOf(p: GameProject): Map<FocusArea, Employee> =
    p.leads.mapNotNull { (area, id) -> staff.firstOrNull { it.id == id && it.lane == Lane.GAMES }?.let { area to it } }.toMap()

/** Why [team]'s plan for [stage] can't be set, or null if it can. */
fun GameState.stagePlanBlocker(team: Int, stage: Int, features: Set<Research>?, leads: Map<FocusArea, Int>): String? {
    val g = games ?: return "You need a game studio."
    val p = g.projectOf(team) ?: return "No game in development."
    if (stage !in 0..2) return "No such stage."
    if (p.isReady) return "It's content complete."
    if (stage < p.stage) return "That stage is done."
    val engine = g.engine(p.concept.engineId)
    if (features != null) {
        if (stage in p.paidStages && features.filter { FeatureGroup.of(it)?.area?.stage == stage }.toSet() != p.features.orEmpty().filter { FeatureGroup.of(it)?.area?.stage == stage }.toSet()) {
            return "This stage's features are already in."
        }
        if (features.any { it !in engine.features }) return "${engine.name} doesn't have that."
        if (features.groupBy { FeatureGroup.of(it) }.any { (k, v) -> k == null || v.size > 1 }) return "Pick one from each group."
    }
    val areas = FocusArea.forStage(stage)
    if (leads.isNotEmpty() && Research.LEAD_ROLES !in researched) return "Research lead roles first."
    if (leads.keys.any { it !in areas }) return "Those areas aren't in this stage."
    if (leads.values.toSet().size < leads.size) return "Nobody can lead two areas at once."
    val people = leadCandidates().map { it.id }.toSet()
    if (leads.values.any { it !in people }) return "Leads have to be on the game team."
    return null
}

/**
 * Sets [team]'s plan for [stage]: the effort split, the features (null keeps what's chosen) and
 * who leads each of its areas. Features can change until the stage starts.
 */
fun GameState.planStage(team: Int, stage: Int, focus: List<Double>? = null, features: Set<Research>? = null, leads: Map<FocusArea, Int>? = null) {
    requireLive()
    stagePlanBlocker(team, stage, features, leads.orEmpty())?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    if (focus != null) {
        require(focus.size == 3 && focus.all { it >= 0 })
        for (i in 0..2) p.focus[stage][i] = focus[i]
    }
    if (features != null) {
        val others = p.features.orEmpty().filter { FeatureGroup.of(it)?.area?.stage != stage }
        p.features = (others + features).toMutableSet()
    }
    if (leads != null) {
        val areas = FocusArea.forStage(stage)
        areas.forEach { p.leads.remove(it) }
        p.leads.putAll(leads)
    }
}

/** What a game's features look like to a reviewer: the areas that matter, and whether they got anything. */
internal fun bareAreas(concept: GameConcept, features: Set<Research>?, engine: Engine): List<FocusArea> {
    if (features == null) return emptyList()
    return FocusArea.entries.filter { area ->
        concept.genre.weight(area) == 2 &&
            FeatureGroup.entries.any { it.area == area && it.members.any { m -> m in engine.features } } &&
            features.none { FeatureGroup.of(it)?.area == area }
    }
}

/** Works out each project's lead boost for the week ahead. */
internal fun GameState.prepareLeads() {
    val g = games ?: return
    // Note who's on each area before the week's work moves a stage on.
    noteCoverage(g, weekStart = true)
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        // Leads who've left the game team stop leading.
        p.leads.entries.removeAll { (_, id) -> staff.none { it.id == id && it.lane == Lane.GAMES } }
        p.leadFactor = if (p.isReady) {
            1.0
        } else {
            leadBoost(p.concept, leadsOf(p), p.stage, p.focus[p.stage]) * specialistBoost(p.concept, p.stage, p.focus[p.stage]) * chemistryFactor()
        }
    }
}
