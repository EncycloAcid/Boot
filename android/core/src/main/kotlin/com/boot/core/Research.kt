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
    MERCHANDISE("Merchandise", "T-shirts, figures and plushies for your hits. Steady money from games that scored 8+.", Kind.STUDIO, 20, requires = MARKETING),
    EXPANSIONS("Expansion packs", "New content for a game that sold well. Everyone who bought it is a customer.", Kind.STUDIO, 18, requires = SEQUELS),
    REMASTERS("Remasters", "Rebuild an old hit for today's platforms, at half the work of a new game.", Kind.STUDIO, 15, requires = SEQUELS),
    PUBLISHING(
        "Publishing label", "Small developers pitch you their games. Fund the ones you believe in and take a share of the sales.",
        Kind.STUDIO, 40, requires = MARKETING,
    ),
    CUSTOM_ENGINES("Custom engines", "Build engines from the features you've researched.", Kind.STUDIO, 15),
    LOCALIZATION("Localization", "Translate your games for Europe and Japan: 6% more sales.", Kind.STUDIO, 25),
    PLAYTESTING("Playtesting lab", "Strangers play your builds and you watch: better games, every time.", Kind.STUDIO, 30),
    DIGITAL_STORES("Digital storefronts", "Sell direct to players' machines: 5% more sales.", Kind.STUDIO, 40, requires = MARKETING),
    MEDIUM_GAMES("Medium games", "Bigger games, bigger launches.", Kind.STUDIO, 20),
    MULTI_GENRE("Multi-genre games", "Mix two genres in one game.", Kind.STUDIO, 35),
    MULTI_PLATFORM("Multi-platform releases", "Ship on up to three platforms at once.", Kind.STUDIO, 35),
    LARGE_GAMES("Large games", "Needs a team of four on the game.", Kind.STUDIO, 70, requires = MEDIUM_GAMES),
    AAA_GAMES("AAA games", "Needs a team of ten on the game.", Kind.STUDIO, 180, requires = LARGE_GAMES),
    SECOND_TEAM(
        "Second team", "Make two games at once: split your game staff into two teams. Needs a studio floor or bigger.",
        Kind.STUDIO, 110, requires = LARGE_GAMES,
    ),

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
    VOICE_ACTING("Voice acting", "Real actors, real lines.", Kind.ENGINE, 35, requires = SOUND_STEREO, engineLevel = 1),
    MOTION_CAPTURE("Motion capture", "Actors in dotted suits. Characters move like people.", Kind.ENGINE, 60, requires = GRAPHICS_3D_V1, engineLevel = 2),
    MULTIPLAYER("Multiplayer", "Play with friends.", Kind.ENGINE, 35, engineLevel = 2),
    ONLINE_SERVICES("Online services", "Accounts, leaderboards, patches.", Kind.ENGINE, 80, requires = MULTIPLAYER, engineLevel = 2),
    CROSS_PLAY("Cross-play", "Everyone in one lobby.", Kind.ENGINE, 140, requires = ONLINE_SERVICES, engineLevel = 3),
    PROCEDURAL("Procedural generation", "Infinite worlds, finite budget.", Kind.ENGINE, 65, engineLevel = 2),
    OPEN_WORLD("Open world", "Go anywhere.", Kind.ENGINE, 85, requires = PHYSICS, engineLevel = 3),
    LIVE_SERVICE(
        "Live service games", "Ship medium and bigger games as live games: seasons of new content, in-game purchases, servers to run.",
        Kind.STUDIO, 70, requires = ONLINE_SERVICES,
    ),
    ESPORTS(
        "Esports", "Run a pro league for a competitive hit: sponsors pay for viewers, and a championship twice a year grows the audience.",
        Kind.STUDIO, 60, requires = ONLINE_SERVICES,
    ),

    // Company
    TRAINING("Training program", "Send people on courses to raise their skills.", Kind.COMPANY, 15),
    RD_LAB("R&D lab", "Half again as many research points. Needs a studio floor or bigger.", Kind.COMPANY, 40, cash = 150_000.0),
    HIRING_PIPELINE("Recruiting pipeline", "University ties and referral bonuses: better candidates turn up.", Kind.COMPANY, 25),
    STOCK_OPTIONS("Stock options", "Everyone owns a sliver. Rivals find your people much harder to poach.", Kind.COMPANY, 35),
    OPS_AUTOMATION("Back-office automation", "Payroll, invoices and IT run themselves: every business costs 5% less to run.", Kind.COMPANY, 60),

    // Lanes
    SITE_TEMPLATES("Site templates", "Agency work goes 10% faster.", Kind.LANE, 12, lane = Lane.WEBSITES),
    CMS_PLATFORM("Your own CMS", "Faster builds, more repeat clients.", Kind.LANE, 30, requires = SITE_TEMPLATES, lane = Lane.WEBSITES),
    AI_SITE_BUILDER("AI site builder", "The machine does the boring half.", Kind.LANE, 90, requires = CMS_PLATFORM, lane = Lane.WEBSITES),
    ACCESSIBILITY("Accessibility practice", "Sites everyone can use. Big clients require it: 5% more briefs.", Kind.LANE, 20, lane = Lane.WEBSITES),
    HEADLESS_COMMERCE("Headless commerce", "Shops built on APIs: faster builds and bigger clients.", Kind.LANE, 60, requires = CMS_PLATFORM, lane = Lane.WEBSITES),
    PUSH_NOTIFICATIONS("Push notifications", "5% less churn.", Kind.LANE, 12, lane = Lane.APPS),
    AB_TESTING("A/B testing", "8% more installs from every listing.", Kind.LANE, 30, requires = PUSH_NOTIFICATIONS, lane = Lane.APPS),
    HOME_WIDGETS("Home screen widgets", "Less churn and better reviews.", Kind.LANE, 60, requires = AB_TESTING, lane = Lane.APPS),
    SUBSCRIPTION_ANALYTICS("Subscription analytics", "See who's about to leave before they do: 5% less churn.", Kind.LANE, 35, lane = Lane.APPS),
    INSTANT_APPS("Instant apps", "Try the app without installing it: 6% more installs.", Kind.LANE, 50, requires = PUSH_NOTIFICATIONS, lane = Lane.APPS),
    SPAM_FILTERS("Spam filters", "Toxicity falls faster.", Kind.LANE, 15, lane = Lane.SOCIAL),
    RECOMMENDATIONS("Recommendations v1", "8% more growth.", Kind.LANE, 40, requires = SPAM_FILTERS, lane = Lane.SOCIAL),
    CREATOR_TOOLS("Creator tools", "Creators bring their audiences: more demand and ad revenue.", Kind.LANE, 80, requires = RECOMMENDATIONS, lane = Lane.SOCIAL),
    GROUPS("Groups", "Communities inside the platform: 5% more growth.", Kind.LANE, 30, lane = Lane.SOCIAL),
    FACT_CHECKING("Fact-checking partners", "Misleading posts get labelled. Toxicity falls, advertisers relax.", Kind.LANE, 45, requires = SPAM_FILTERS, lane = Lane.SOCIAL),
    SUPPLY_CHAIN("Supply chain deals", "Factory lead time drops from 12 to 8 weeks.", Kind.LANE, 25, lane = Lane.HARDWARE),
    BATTERY_SAFETY("Battery safety lab", "Halves the chance of a recall.", Kind.LANE, 30, lane = Lane.HARDWARE),
    OLED_DISPLAYS("OLED displays", "Better reviews and a little more demand.", Kind.LANE, 70, requires = SUPPLY_CHAIN, lane = Lane.HARDWARE),
    MODULAR_DESIGN("Modular design", "Parts that swap out: better devices, fewer recalls.", Kind.LANE, 40, lane = Lane.HARDWARE),
    FAST_CHARGING("Fast charging", "Half an hour to full: 4% more demand.", Kind.LANE, 50, requires = BATTERY_SAFETY, lane = Lane.HARDWARE),
    EFFICIENT_TRAINING("Efficient training", "Compute bills drop by 15%.", Kind.LANE, 30, lane = Lane.AI),
    SAFETY_EVALS("Safety evals", "Regulators take longer to notice you.", Kind.LANE, 40, lane = Lane.AI),
    MIXTURE_OF_EXPERTS("Mixture of experts", "Training runs gain 15% more capability.", Kind.LANE, 90, requires = EFFICIENT_TRAINING, lane = Lane.AI),
    DISTILLATION("Distillation", "Small models taught by big ones: compute bills drop 10%.", Kind.LANE, 50, lane = Lane.AI),
    RLHF("Human feedback", "People rate the answers and the model learns: training gains 8% more.", Kind.LANE, 70, requires = SAFETY_EVALS, lane = Lane.AI),

    // Era research: only while its era lasts. Once you have it, it's yours for good.
    COPY_PROTECTION("Copy protection", "Stop the bootleg tapes going round school: 6% more game sales.", Kind.ERA, 15),
    QUALITY_SEAL("Seal of quality", "Promise every game is worth buying, and mean it: better reviews and more trust.", Kind.ERA, 20),
    MASCOT("A mascot", "A character on every box and lunchbox: more game and device sales.", Kind.ERA, 25),
    FMV("Full-motion video", "Real actors on a CD-ROM. Cheesy and thrilling.", Kind.ENGINE, 30, engineLevel = 2),
    ECOMMERCE("E-commerce checkout", "Everyone wants a shop online: 12% more agency briefs.", Kind.ERA, 25),
    LEAN_OPS("Lean operations", "What the bust taught everyone: every business costs 6% less to run.", Kind.ERA, 30),
    AJAX("Rich web apps", "Pages that update without reloading: faster agency builds, a livelier platform.", Kind.ERA, 30),
    MOBILE_SDK("Mobile SDKs", "Build for the new phones first: 10% more app installs and a few more game sales.", Kind.ERA, 30),
    GACHA("Gacha mechanics", "Random rewards for real money. It works, and people know it works.", Kind.ERA, 35),
    COPILOTS("AI copilots", "Everyone works with an AI assistant: every team gets 6% more done.", Kind.ERA, 40),
    VR_SUPPORT("VR support", "Your engine renders for headsets.", Kind.ENGINE, 45, engineLevel = 2),
    GPU_FIRE_SALE("Fire-sale GPUs", "The miners are selling: compute costs 10% less, for good.", Kind.ERA, 30),
    REMOTE_TOOLING("Remote tooling", "Work from anywhere, properly: a little more done for a little less.", Kind.ERA, 30),
    PRIVACY_BY_DESIGN("Privacy by design", "Collect less, protect more: less risk, and users stay.", Kind.ERA, 35),
    CREATOR_PROGRAM("Creator program", "Pay creators to make things with your products: more social growth and game sales.", Kind.ERA, 35),
    PIXEL_ART("Pixel-art pipeline", "Retro is in, and it's cheap to make: games cost 10% less.", Kind.ERA, 25),
    SPATIAL_DESIGN("Spatial design talent", "The metaverse crowd is hiring cheap: better games and devices.", Kind.ERA, 30),
    CHIP_STOCKPILE("Chip stockpile", "Buy every chip you can find: devices cost 8% less to make.", Kind.ERA, 35),
    CLOUD_STREAMING("Cloud streaming", "Your engine streams games from the cloud.", Kind.ENGINE, 45, engineLevel = 2),
    MINI_APPS("Mini-apps", "Your app runs inside the super-apps: more installs, more platform use.", Kind.ERA, 35);

    enum class Kind { STUDIO, ENGINE, COMPANY, LANE, ERA }

    /** The era this can only be researched in, or null if any time will do. */
    val era: Era?
        get() = when (this) {
            COPY_PROTECTION -> Era.HOME_COMPUTERS
            QUALITY_SEAL -> Era.VIDEO_GAME_CRASH
            MASCOT -> Era.CONSOLE_WARS
            FMV -> Era.MULTIMEDIA
            ECOMMERCE -> Era.DOT_COM_BOOM
            LEAN_OPS -> Era.DOT_COM_BUST
            AJAX -> Era.WEB_2_0
            MOBILE_SDK -> Era.SMARTPHONES
            GACHA -> Era.FREE_TO_PLAY
            COPILOTS -> Era.AI_GOLD_RUSH
            VR_SUPPORT -> Era.VR_HYPE_CYCLE
            GPU_FIRE_SALE -> Era.CRYPTO_WINTER
            REMOTE_TOOLING -> Era.REMOTE_WORK_BOOM
            PRIVACY_BY_DESIGN -> Era.PRIVACY_PANIC
            CREATOR_PROGRAM -> Era.CREATOR_ECONOMY
            PIXEL_ART -> Era.RETRO_REVIVAL
            SPATIAL_DESIGN -> Era.METAVERSE_HANGOVER
            CHIP_STOCKPILE -> Era.CHIP_SHORTAGE
            CLOUD_STREAMING -> Era.CLOUD_GAMING
            MINI_APPS -> Era.SUPER_APPS
            else -> null
        }

    /** The year it became possible. In a run set in the past, nothing can be researched before its time. */
    val year: Int
        get() = when (this) {
            SEQUELS, PROCEDURAL, BASIC_AI, MULTIPLAYER -> 1978
            MERCHANDISE, PUBLISHING, CUSTOM_ENGINES, STOCK_OPTIONS -> 1980
            MEDIUM_GAMES -> 1983
            MULTI_PLATFORM -> 1984
            MULTI_GENRE, LOCALIZATION, SOUND_STEREO, OPS_AUTOMATION -> 1985
            SAVE_SYSTEM -> 1986
            PLAYTESTING, EXPANSIONS, SECOND_TEAM, BATTERY_SAFETY -> 1990
            GRAPHICS_3D_V1 -> 1992
            LARGE_GAMES, VOICE_ACTING -> 1993
            MOTION_CAPTURE -> 1994
            SITE_TEMPLATES -> 1996
            ONLINE_SERVICES -> 1997
            GRAPHICS_3D_V2, SOUND_SURROUND, ADVANCED_AI, PHYSICS -> 1998
            ESPORTS, ACCESSIBILITY -> 2000
            OPEN_WORLD -> 2001
            CMS_PLATFORM -> 2003
            AAA_GAMES, GRAPHICS_3D_V3, DIGITAL_STORES, SPAM_FILTERS -> 2004
            REMASTERS, MODULAR_DESIGN -> 2005
            GROUPS, OLED_DISPLAYS -> 2008
            PUSH_NOTIFICATIONS, RECOMMENDATIONS -> 2009
            AB_TESTING, LIVE_SERVICE -> 2010
            SUBSCRIPTION_ANALYTICS -> 2012
            HOME_WIDGETS, EFFICIENT_TRAINING, FAST_CHARGING -> 2014
            CREATOR_TOOLS, SPATIAL_AUDIO, FACT_CHECKING -> 2016
            HEADLESS_COMMERCE -> 2017
            GRAPHICS_3D_V4, CROSS_PLAY, INSTANT_APPS -> 2018
            SAFETY_EVALS -> 2019
            DISTILLATION -> 2020
            MIXTURE_OF_EXPERTS -> 2021
            RLHF -> 2022
            PHOTOREAL, NEURAL_NPCS, AI_SITE_BUILDER -> 2023
            else -> 0
        }
}

/** A game engine: the features it was built with set its tech level. */
@Serializable
data class Engine(val id: Int, val name: String, val features: List<Research>, val builtWeek: Int, var licensed: Boolean = false) {
    val level: Int get() = features.sumOf { it.engineLevel }

    /** What other studios pay a week to build on it, before the market's mood. */
    fun royalty(week: Int): Double = level * 900.0 * (1 + 0.1 * week / 52)

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
            if (has(Research.ACCESSIBILITY)) mods.demand *= 1.05
            if (has(Research.HEADLESS_COMMERCE)) {
                mods.output *= 1.05
                mods.demand *= 1.04
            }
            if (has(Research.ECOMMERCE)) mods.demand *= 1.12
            if (has(Research.AJAX)) mods.output *= 1.08
            if (has(Research.SITE_TEMPLATES)) mods.output *= 1.1
            if (has(Research.CMS_PLATFORM)) {
                mods.output *= 1.05
                mods.demand *= 1.05
            }
            if (has(Research.AI_SITE_BUILDER)) mods.output *= 1.2
        }
        Lane.APPS -> {
            if (has(Research.SUBSCRIPTION_ANALYTICS)) mods.churn *= 0.95
            if (has(Research.INSTANT_APPS)) mods.demand *= 1.06
            if (has(Research.MOBILE_SDK)) mods.demand *= 1.1
            if (has(Research.GACHA)) mods.demand *= 1.06
            if (has(Research.PRIVACY_BY_DESIGN)) mods.churn *= 0.97
            if (has(Research.MINI_APPS)) mods.demand *= 1.08
            if (has(Research.PUSH_NOTIFICATIONS)) mods.churn *= 0.95
            if (has(Research.AB_TESTING)) mods.demand *= 1.08
            if (has(Research.HOME_WIDGETS)) {
                mods.churn *= 0.95
                mods.quality += 2
            }
        }
        Lane.SOCIAL -> {
            if (has(Research.GROUPS)) mods.growth *= 1.05
            if (has(Research.FACT_CHECKING)) {
                mods.toxicity -= 0.001
                mods.demand *= 1.03
            }
            if (has(Research.AJAX)) mods.growth *= 1.04
            if (has(Research.PRIVACY_BY_DESIGN)) mods.toxicity -= 0.0005
            if (has(Research.CREATOR_PROGRAM)) mods.demand *= 1.08
            if (has(Research.MINI_APPS)) mods.demand *= 1.04
            if (has(Research.SPAM_FILTERS)) mods.toxicity -= 0.0015
            if (has(Research.RECOMMENDATIONS)) mods.growth *= 1.08
            if (has(Research.CREATOR_TOOLS)) mods.demand *= 1.1
        }
        Lane.HARDWARE -> {
            if (has(Research.MODULAR_DESIGN)) {
                mods.quality += 2
                mods.risk *= 0.85
            }
            if (has(Research.FAST_CHARGING)) mods.demand *= 1.04
            if (has(Research.MASCOT)) mods.demand *= 1.05
            if (has(Research.SPATIAL_DESIGN)) mods.quality += 2
            if (has(Research.CHIP_STOCKPILE)) mods.cost *= 0.92
            if (has(Research.BATTERY_SAFETY)) mods.risk *= 0.5
            if (has(Research.OLED_DISPLAYS)) {
                mods.quality += 4
                mods.demand *= 1.03
            }
        }
        Lane.AI -> {
            if (has(Research.DISTILLATION)) mods.computeCost *= 0.9
            if (has(Research.RLHF)) mods.gain *= 1.08
            if (has(Research.GPU_FIRE_SALE)) mods.computeCost *= 0.9
            if (has(Research.EFFICIENT_TRAINING)) mods.computeCost *= 0.85
            if (has(Research.SAFETY_EVALS)) mods.risk *= 0.6
            if (has(Research.MIXTURE_OF_EXPERTS)) mods.gain *= 1.15
        }
        Lane.GAMES -> {
            if (has(Research.LOCALIZATION)) mods.demand *= 1.06
            if (has(Research.DIGITAL_STORES)) mods.demand *= 1.05
            if (has(Research.PLAYTESTING)) mods.quality += 3
            if (has(Research.COPY_PROTECTION)) mods.demand *= 1.06
            if (has(Research.QUALITY_SEAL)) {
                mods.quality += 4
                mods.demand *= 1.04
            }
            if (has(Research.MASCOT)) mods.demand *= 1.05
            if (has(Research.MOBILE_SDK)) mods.demand *= 1.03
            if (has(Research.GACHA)) mods.demand *= 1.04
            if (has(Research.CREATOR_PROGRAM)) mods.demand *= 1.03
            if (has(Research.PIXEL_ART)) mods.cost *= 0.9
            if (has(Research.SPATIAL_DESIGN)) mods.quality += 2
        }
    }
    // Company-wide.
    if (has(Research.OPS_AUTOMATION)) mods.cost *= 0.95
    if (has(Research.LEAN_OPS)) mods.cost *= 0.94
    if (has(Research.COPILOTS)) mods.output *= 1.06
    if (has(Research.REMOTE_TOOLING)) {
        mods.output *= 1.03
        mods.cost *= 0.97
    }
    if (has(Research.PRIVACY_BY_DESIGN)) mods.risk *= 0.8
}

/** Why a research item can't be bought right now, or null if it can. */
fun GameState.researchBlocker(r: Research): String? {
    if (r in researched) return "Already researched."
    if (r.year > calendarYear) return "Not invented until ${r.year}."
    r.era?.let { if (world.era != it) return "Only during ${it.title.replaceFirst("The ", "the ")}." }
    r.requires?.let { if (it !in researched) return "Needs ${it.title} first." }
    if (r == Research.RD_LAB && office < Office.STUDIO) return "Needs a studio floor or bigger office."
    if (r == Research.SECOND_TEAM && office < Office.STUDIO) return "Needs a studio floor or bigger office."
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
    if (has(Perk.LIBRARY)) rp *= 1.1
    return rp
}

/** Research points the company is earning per week right now. */
val GameState.researchPerWeek: Double get() = weeklyResearch(games?.lastWork ?: 0.0)
