package com.boot.core

import kotlinx.serialization.Serializable

/**
 * The research tree, spent with research points (RP). Studio items unlock game-making options,
 * engine items become parts of custom engines, company items help everyone, and lane items
 * improve one business.
 */
@Serializable
enum class Research(
    val title: String,
    val detail: String,
    val kind: Kind,
    val rp: Int,
    val cash: Double = 0.0,
    val requires: Research? = null,
    /** Contribution to a custom engine's tech level. */
    val engineLevel: Int = 0,
    val lane: Lane? = null,
) {
    // Studio
    TARGET_AUDIENCE("Target audience", "Pick who a game is for: young, everyone or mature.", Kind.STUDIO, 8),
    MARKETING("Marketing campaigns", "Buy hype before launch: press releases, ads, TV.", Kind.STUDIO, 10),
    SEQUELS("Sequels", "Follow up a game with the same topic and genre. Fans come back.", Kind.STUDIO, 12),
    EXPANSIONS("Expansion packs", "New content for a game that sold well. Everyone who bought it is a customer.", Kind.STUDIO, 18, requires = SEQUELS),
    CUSTOM_ENGINES("Custom engines", "Build engines from the features you've researched.", Kind.STUDIO, 15),
    MEDIUM_GAMES("Medium games", "Bigger games, bigger launches.", Kind.STUDIO, 20),
    MULTI_GENRE("Multi-genre games", "Mix two genres in one game.", Kind.STUDIO, 35),
    MULTI_PLATFORM("Multi-platform releases", "Ship on up to three platforms at once.", Kind.STUDIO, 35),
    LARGE_GAMES("Large games", "Needs a team of four on the game.", Kind.STUDIO, 70, requires = MEDIUM_GAMES),
    AAA_GAMES("AAA games", "Needs a team of ten on the game.", Kind.STUDIO, 180, requires = LARGE_GAMES),

    // Engine features
    GRAPHICS_2D_V2("2D graphics V2", "Crisper sprites.", Kind.ENGINE, 8, engineLevel = 1),
    GRAPHICS_3D_V1("3D graphics V1", "Polygons!", Kind.ENGINE, 25, requires = GRAPHICS_2D_V2, engineLevel = 2),
    GRAPHICS_3D_V2("3D graphics V2", "Real lighting.", Kind.ENGINE, 50, requires = GRAPHICS_3D_V1, engineLevel = 2),
    GRAPHICS_3D_V3("3D graphics V3", "Shaders everywhere.", Kind.ENGINE, 90, requires = GRAPHICS_3D_V2, engineLevel = 3),
    GRAPHICS_3D_V4("3D graphics V4", "Ray tracing.", Kind.ENGINE, 150, requires = GRAPHICS_3D_V3, engineLevel = 3),
    PHOTOREAL("Photoreal rendering", "Is it a game or a film?", Kind.ENGINE, 240, requires = GRAPHICS_3D_V4, engineLevel = 4),
    SOUND_STEREO("Stereo sound", "Left and right, finally.", Kind.ENGINE, 8, engineLevel = 1),
    SOUND_SURROUND("Surround sound", "Footsteps behind you.", Kind.ENGINE, 40, requires = SOUND_STEREO, engineLevel = 2),
    SPATIAL_AUDIO("Spatial audio", "You can hear the room.", Kind.ENGINE, 110, requires = SOUND_SURROUND, engineLevel = 3),
    SAVE_SYSTEM("Save anywhere", "Players stop throwing controllers.", Kind.ENGINE, 6, engineLevel = 1),
    BASIC_AI("Basic AI", "Enemies that take cover.", Kind.ENGINE, 12, engineLevel = 1),
    ADVANCED_AI("Advanced AI", "Enemies that flank.", Kind.ENGINE, 55, requires = BASIC_AI, engineLevel = 2),
    NEURAL_NPCS("Neural NPCs", "Characters that improvise.", Kind.ENGINE, 180, requires = ADVANCED_AI, engineLevel = 3),
    PHYSICS("Physics engine", "Everything can fall over.", Kind.ENGINE, 30, engineLevel = 2),
    MULTIPLAYER("Multiplayer", "Play with friends.", Kind.ENGINE, 35, engineLevel = 2),
    ONLINE_SERVICES("Online services", "Accounts, leaderboards, patches.", Kind.ENGINE, 80, requires = MULTIPLAYER, engineLevel = 2),
    CROSS_PLAY("Cross-play", "Everyone in one lobby.", Kind.ENGINE, 140, requires = ONLINE_SERVICES, engineLevel = 3),
    PROCEDURAL("Procedural generation", "Infinite worlds, finite budget.", Kind.ENGINE, 65, engineLevel = 2),
    OPEN_WORLD("Open world", "Go anywhere.", Kind.ENGINE, 85, requires = PHYSICS, engineLevel = 3),

    // Company
    TRAINING("Training program", "Send people on courses to raise their skills.", Kind.COMPANY, 15),
    RD_LAB("R&D lab", "Half again as many research points. Needs a studio floor or bigger.", Kind.COMPANY, 40, cash = 150_000.0),

    // Lanes
    SITE_TEMPLATES("Site templates", "Agency work goes 10% faster.", Kind.LANE, 12, lane = Lane.WEBSITES),
    CMS_PLATFORM("Your own CMS", "Faster builds, more repeat clients.", Kind.LANE, 30, requires = SITE_TEMPLATES, lane = Lane.WEBSITES),
    AI_SITE_BUILDER("AI site builder", "The machine does the boring half.", Kind.LANE, 90, requires = CMS_PLATFORM, lane = Lane.WEBSITES),
    PUSH_NOTIFICATIONS("Push notifications", "5% less churn.", Kind.LANE, 12, lane = Lane.APPS),
    AB_TESTING("A/B testing", "8% more installs from every listing.", Kind.LANE, 30, requires = PUSH_NOTIFICATIONS, lane = Lane.APPS),
    HOME_WIDGETS("Home screen widgets", "Less churn and better reviews.", Kind.LANE, 60, requires = AB_TESTING, lane = Lane.APPS),
    SPAM_FILTERS("Spam filters", "Toxicity falls faster.", Kind.LANE, 15, lane = Lane.SOCIAL),
    RECOMMENDATIONS("Recommendations v1", "8% more growth.", Kind.LANE, 40, requires = SPAM_FILTERS, lane = Lane.SOCIAL),
    CREATOR_TOOLS("Creator tools", "Creators bring their audiences: more demand and ad revenue.", Kind.LANE, 80, requires = RECOMMENDATIONS, lane = Lane.SOCIAL),
    SUPPLY_CHAIN("Supply chain deals", "Factory lead time drops from 12 to 8 weeks.", Kind.LANE, 25, lane = Lane.HARDWARE),
    BATTERY_SAFETY("Battery safety lab", "Halves the chance of a recall.", Kind.LANE, 30, lane = Lane.HARDWARE),
    OLED_DISPLAYS("OLED displays", "Better reviews and a little more demand.", Kind.LANE, 70, requires = SUPPLY_CHAIN, lane = Lane.HARDWARE),
    EFFICIENT_TRAINING("Efficient training", "Compute bills drop by 15%.", Kind.LANE, 30, lane = Lane.AI),
    SAFETY_EVALS("Safety evals", "Regulators take longer to notice you.", Kind.LANE, 40, lane = Lane.AI),
    MIXTURE_OF_EXPERTS("Mixture of experts", "Training runs gain 15% more capability.", Kind.LANE, 90, requires = EFFICIENT_TRAINING, lane = Lane.AI);

    enum class Kind { STUDIO, ENGINE, COMPANY, LANE }
}

/** A game engine: the features it was built with set its tech level. */
@Serializable
data class Engine(val id: Int, val name: String, val features: List<Research>, val builtWeek: Int) {
    val level: Int get() = features.sumOf { it.engineLevel }

    /** Points multiplier for games built on this engine. */
    val multiplier: Double get() = 1 + 0.05 * level
}

/** An engine being built. */
@Serializable
data class EngineBuild(val name: String, val features: List<Research>, val weeksLeft: Int)

// Research effects on lanes, folded into the weekly context.

internal fun GameState.applyResearch(lane: Lane, mods: LaneModifiers) {
    fun has(r: Research) = r in researched
    when (lane) {
        Lane.WEBSITES -> {
            if (has(Research.SITE_TEMPLATES)) mods.output *= 1.1
            if (has(Research.CMS_PLATFORM)) {
                mods.output *= 1.05
                mods.demand *= 1.05
            }
            if (has(Research.AI_SITE_BUILDER)) mods.output *= 1.2
        }
        Lane.APPS -> {
            if (has(Research.PUSH_NOTIFICATIONS)) mods.churn *= 0.95
            if (has(Research.AB_TESTING)) mods.demand *= 1.08
            if (has(Research.HOME_WIDGETS)) {
                mods.churn *= 0.95
                mods.quality += 2
            }
        }
        Lane.SOCIAL -> {
            if (has(Research.SPAM_FILTERS)) mods.toxicity -= 0.0015
            if (has(Research.RECOMMENDATIONS)) mods.growth *= 1.08
            if (has(Research.CREATOR_TOOLS)) mods.demand *= 1.1
        }
        Lane.HARDWARE -> {
            if (has(Research.BATTERY_SAFETY)) mods.risk *= 0.5
            if (has(Research.OLED_DISPLAYS)) {
                mods.quality += 4
                mods.demand *= 1.03
            }
        }
        Lane.AI -> {
            if (has(Research.EFFICIENT_TRAINING)) mods.computeCost *= 0.85
            if (has(Research.SAFETY_EVALS)) mods.risk *= 0.6
            if (has(Research.MIXTURE_OF_EXPERTS)) mods.gain *= 1.15
        }
        Lane.GAMES -> {}
    }
}

/** Why a research item can't be bought right now, or null if it can. */
fun GameState.researchBlocker(r: Research): String? {
    if (r in researched) return "Already researched."
    r.requires?.let { if (it !in researched) return "Needs ${it.title} first." }
    if (r == Research.RD_LAB && office < Office.STUDIO) return "Needs a studio floor or bigger office."
    if (researchPoints < r.rp) return "Needs ${r.rp} RP. You have ${researchPoints.toInt()}."
    if (cash < r.cash) return "Costs ${money(r.cash)}."
    return null
}

fun GameState.research(r: Research) {
    if (ending != null) throw gameOver()
    researchBlocker(r)?.let { throw notAvailable(it) }
    researchPoints -= r.rp
    if (r.cash > 0) spend(r.cash, null)
    researched = researched + r
    addLog("Research complete: ${r.title}. ${r.detail}", LogTone.GOOD, r.lane)
}

fun GameState.researchTopic(topic: Topic) {
    if (ending != null) throw gameOver()
    val g = games ?: throw notAvailable("Topics are for game studios.")
    if (topic in g.topics) throw invalid("You already know ${topic.title}.")
    if (researchPoints < topic.researchCost) throw notAvailable("Needs ${topic.researchCost} RP.")
    researchPoints -= topic.researchCost
    g.topics += topic
    addLog("New topic researched: ${topic.title}.", LogTone.GOOD, Lane.GAMES)
}

/** Research points earned this week. */
internal fun GameState.weeklyResearch(gameWork: Double): Double {
    var rp = 0.5 + founder.skillBonus(lanes.firstOrNull() ?: startingLane) * 0.1
    for (e in staff) rp += e.research * 0.04 * e.moraleFactor
    rp += gameWork * 0.07
    if (Research.RD_LAB in researched) rp *= 1.5
    return rp
}

/** Research points the company is earning per week right now. */
val GameState.researchPerWeek: Double get() = weeklyResearch(games?.lastWork ?: 0.0)
