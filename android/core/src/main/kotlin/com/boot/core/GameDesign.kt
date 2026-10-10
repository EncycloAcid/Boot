package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

// The building blocks of a game, Game Dev Tycoon style: topic, genre, audience, platform, size,
// and where the team spends its effort in each of the three development stages.

@Serializable
enum class Genre(val code: Char) {
    ACTION('A'), ADVENTURE('V'), RPG('R'), SIMULATION('S'), STRATEGY('T'), CASUAL('C');

    val title: String
        get() = when (this) {
            ACTION -> "Action"
            ADVENTURE -> "Adventure"
            RPG -> "RPG"
            SIMULATION -> "Simulation"
            STRATEGY -> "Strategy"
            CASUAL -> "Casual"
        }

    /** For use mid-sentence: "rpg" reads wrong, "RPG" stays. */
    val lower: String get() = if (this == RPG) title else title.lowercase()

    /** The share of points this genre wants to be design rather than tech. */
    val idealDesignShare: Double
        get() = when (this) {
            ACTION -> 0.38
            ADVENTURE -> 0.68
            RPG -> 0.66
            SIMULATION -> 0.36
            STRATEGY -> 0.58
            CASUAL -> 0.55
        }

    /** How much each development area matters to this genre: 0 low, 1 medium, 2 high. */
    fun weight(area: FocusArea): Int = when (this) {
        ACTION -> intArrayOf(2, 2, 0, 0, 2, 2, 0, 2, 1)
        ADVENTURE -> intArrayOf(0, 1, 2, 2, 1, 0, 2, 1, 1)
        RPG -> intArrayOf(0, 2, 2, 2, 1, 0, 2, 1, 0)
        SIMULATION -> intArrayOf(2, 2, 0, 0, 1, 2, 1, 2, 0)
        STRATEGY -> intArrayOf(2, 2, 0, 0, 1, 2, 2, 0, 1)
        CASUAL -> intArrayOf(0, 2, 0, 0, 2, 0, 1, 2, 2)
    }[area.ordinal]

    companion object {
        fun of(code: Char): Genre = entries.first { it.code == code }
    }
}

/** The nine areas a team splits its time between, three per development stage. */
@Serializable
enum class FocusArea(val stage: Int, val designShare: Double) {
    ENGINE(0, 0.0), GAMEPLAY(0, 0.8), STORY(0, 1.0),
    DIALOGUES(1, 0.9), LEVEL_DESIGN(1, 0.6), AI(1, 0.1),
    WORLD_DESIGN(2, 0.8), GRAPHICS(2, 0.2), SOUND(2, 0.5);

    val title: String
        get() = when (this) {
            ENGINE -> "Engine"
            GAMEPLAY -> "Gameplay"
            STORY -> "Story & quests"
            DIALOGUES -> "Dialogues"
            LEVEL_DESIGN -> "Level design"
            AI -> "AI"
            WORLD_DESIGN -> "World design"
            GRAPHICS -> "Graphics"
            SOUND -> "Sound"
        }

    companion object {
        val STAGES = listOf("Stage 1 · Foundations", "Stage 2 · Content", "Stage 3 · Presentation")
        fun forStage(stage: Int): List<FocusArea> = entries.filter { it.stage == stage }
    }
}

@Serializable
enum class Audience(val code: Char) {
    YOUNG('Y'), EVERYONE('E'), MATURE('M');

    val title: String
        get() = when (this) {
            YOUNG -> "Young"
            EVERYONE -> "Everyone"
            MATURE -> "Mature"
        }

    /** For use mid-sentence: "for young players", "for everyone". */
    val phrase: String
        get() = when (this) {
            YOUNG -> "young players"
            EVERYONE -> "everyone"
            MATURE -> "mature players"
        }
}

/** How well two choices go together. Hidden until a game report reveals it. */
@Serializable
enum class Fit(val multiplier: Double) {
    GREAT(1.15), GOOD(1.0), OKAY(0.9), BAD(0.7);

    val title: String
        get() = when (this) {
            GREAT -> "Great"
            GOOD -> "Good"
            OKAY -> "Okay"
            BAD -> "Bad"
        }
}

/**
 * Game topics. The strings list genre codes (A action, V adventure, R RPG, S simulation, T strategy,
 * C casual) that are a great, good or bad match, and audience codes (Y, E, M) that suit or don't.
 */
@Serializable
enum class Topic(
    private val great: String,
    private val good: String,
    private val bad: String,
    private val audienceGood: String = "E",
    private val audienceBad: String = "",
    val starter: Boolean = false,
) {
    FANTASY("RV", "AT", "S", "EM", starter = true),
    SCI_FI("AT", "RV", "C", "EM", starter = true),
    RACING("SA", "C", "RVT", "YE", starter = true),
    SPORTS("SC", "AT", "RV", "YE", starter = true),
    MILITARY("TA", "S", "CV", "M", "Y", starter = true),
    SPACE("TS", "AR", "C", "EM", starter = true),
    PIRATES("VA", "TR", "S", "YE", starter = true),
    DETECTIVE("V", "RC", "AS", "EM", starter = true),
    CITY("ST", "C", "AVR", "E", starter = true),
    NINJA("A", "VR", "S", "YE", starter = true),
    ZOMBIES("A", "VT", "CS", "M", "Y"),
    HORROR("VA", "R", "CST", "M", "Y"),
    CYBERPUNK("RA", "VT", "CS", "M", "Y"),
    MEDIEVAL("TR", "ASV", "C", "EM"),
    FARMING("SC", "T", "A", "YE"),
    COOKING("CS", "V", "ART", "YE"),
    SCHOOL("CV", "SR", "AT", "Y", "M"),
    DINOSAURS("AS", "VC", "T", "Y"),
    SUPERHEROES("A", "VR", "ST", "YE"),
    POST_APOCALYPTIC("RA", "TV", "C", "M", "Y"),
    MYSTERY("V", "CR", "AS", "EM"),
    MUSIC("C", "S", "ART", "YE"),
    MYTHOLOGY("RT", "AV", "SC", "EM"),
    WILD_WEST("AV", "STR", "C", "EM"),
    ALIENS("AT", "VS", "C", "E"),
    ROBOTS("AS", "TC", "V", "YE"),
    TIME_TRAVEL("VR", "A", "S", "E"),
    PETS("CS", "V", "ATR", "Y", "M"),
    BUSINESS("ST", "C", "AVR", "EM", "Y"),
    HACKING("TS", "VA", "C", "M", "Y"),
    VAMPIRES("RV", "A", "CS", "M", "Y"),
    OCEAN("SV", "C", "T", "YE"),
    SPY("AV", "T", "SC", "EM"),
    MARS("ST", "VA", "C", "E"),
    FASHION("CS", "V", "ATR", "YE", "M"),
    HOSPITAL("S", "CT", "AR", "EM", "Y"),
    ARTIFICIAL_INTELLIGENCE("TS", "VR", "C", "EM");

    val title: String
        get() = when (this) {
            SCI_FI -> "Sci-Fi"
            POST_APOCALYPTIC -> "Post-apocalyptic"
            WILD_WEST -> "Wild West"
            TIME_TRAVEL -> "Time travel"
            ARTIFICIAL_INTELLIGENCE -> "Artificial intelligence"
            else -> name.lowercase().replaceFirstChar { it.uppercase() }
        }

    /** Research points to unlock a topic you don't start with. */
    val researchCost: Int get() = if (starter) 0 else 6 + (ordinal % 5) * 3

    fun fit(genre: Genre): Fit = when (genre.code) {
        in great -> Fit.GREAT
        in good -> Fit.GOOD
        in bad -> Fit.BAD
        else -> Fit.OKAY
    }

    fun fit(audience: Audience): Fit = when (audience.code) {
        in audienceGood -> Fit.GOOD
        in audienceBad -> Fit.BAD
        else -> Fit.OKAY
    }
}

@Serializable
enum class GameSize {
    SMALL, MEDIUM, LARGE, AAA;

    val title: String
        get() = when (this) {
            SMALL -> "Small"
            MEDIUM -> "Medium"
            LARGE -> "Large"
            AAA -> "AAA"
        }

    val lower: String get() = if (this == AAA) title else title.lowercase()

    /** Work points to finish. */
    val work: Double
        get() = when (this) {
            SMALL -> 60.0
            MEDIUM -> 500.0
            LARGE -> 2_400.0
            AAA -> 6_000.0
        }

    /** Art, licences and marketing while in development. */
    val weeklyBudget: Double
        get() = when (this) {
            SMALL -> 150.0
            MEDIUM -> 3_000.0
            LARGE -> 22_000.0
            AAA -> 60_000.0
        }

    /** Launch-week revenue for a 7/10 game on an average spread of platforms, before fans and hype. */
    internal val launchSales: Double
        get() = when (this) {
            SMALL -> 30_000.0
            MEDIUM -> 150_000.0
            LARGE -> 800_000.0
            AAA -> 2_000_000.0
        }

    val price: Double
        get() = when (this) {
            SMALL -> 12.0
            MEDIUM -> 25.0
            LARGE -> 50.0
            AAA -> 70.0
        }

    internal val fanScale: Double
        get() = when (this) {
            SMALL -> 5_000.0
            MEDIUM -> 40_000.0
            LARGE -> 200_000.0
            AAA -> 500_000.0
        }

    /** Research you need before you can make games this big. */
    val requires: Research?
        get() = when (this) {
            SMALL -> null
            MEDIUM -> Research.MEDIUM_GAMES
            LARGE -> Research.LARGE_GAMES
            AAA -> Research.AAA_GAMES
        }

    /** Staff on the game team (not counting you) before the size is allowed. */
    val minTeam: Int
        get() = when (this) {
            SMALL, MEDIUM -> 0
            LARGE -> 4
            AAA -> 10
        }
}

/**
 * Parody platforms with a life cycle: each launches, ramps up, plateaus and is retired. Weeks are
 * game weeks; negative launch weeks were already on sale when you started.
 */
@Serializable
enum class Platform(
    val title: String,
    val maker: String,
    val launch: Int,
    val retire: Int,
    /** Millions of active players at peak. */
    val peakPlayers: Double,
    /** How much a player spends compared with a console owner. */
    val spend: Double,
    val fee: Double,
    private val good: String,
    private val bad: String,
    private val audiences: String,
) {
    PC("Personal Computer", "Everyone", -2_000, 100_000, 55.0, 1.0, 0.0, "TSR", "", "EM"),
    PLAYSYSTEM_5("Playsystem 5", "Sunny", -150, 330, 50.0, 1.0, 80_000.0, "AVR", "T", "EM"),
    MBOX_Z("mBox Series Z", "Megasoft", -150, 330, 28.0, 1.0, 80_000.0, "AS", "C", "M"),
    SWITCHEROO("Nuu Switcheroo", "Ninvento", -300, 260, 42.0, 0.9, 50_000.0, "CVA", "ST", "YE"),
    GRPHONE("grPhone & friends", "Fruit Co", -2_000, 100_000, 400.0, 0.08, 2_000.0, "C", "TS", "YE"),
    VAPOR_DECK("Vapor Deck", "Vapor", 30, 560, 9.0, 1.1, 0.0, "RTA", "", "EM"),
    STRATOS("Stratos Cloud", "Googol", 90, 230, 6.0, 0.7, 20_000.0, "A", "T", "E"),
    VISOR_VR("Visor VR", "Metaverse Inc", 120, 620, 14.0, 1.1, 30_000.0, "AS", "TR", "EM"),
    SWITCHEROO_2("Nuu Switcheroo 2", "Ninvento", 230, 100_000, 60.0, 0.9, 70_000.0, "CVA", "ST", "YE"),
    PLAYSYSTEM_6("Playsystem 6", "Sunny", 320, 100_000, 65.0, 1.0, 120_000.0, "AVR", "T", "EM"),
    MBOX_INFINITY("mBox Infinity", "Megasoft", 335, 100_000, 34.0, 1.0, 120_000.0, "AS", "C", "M"),
    SPECTACLE_AR("Spectacle AR", "Fruit Co", 470, 100_000, 30.0, 0.8, 50_000.0, "CA", "T", "YE"),

    /** Your own handheld, once Games and Hardware have built the Handheld Console crossover. */
    OWN_CONSOLE("Your handheld", "You", -2_000, 100_000, 0.0, 1.2, 0.0, "AVRSTC", "", "YEM");

    fun fit(genre: Genre): Fit = when (genre.code) {
        in good -> Fit.GOOD
        in bad -> Fit.BAD
        else -> Fit.OKAY
    }

    fun suits(audience: Audience): Boolean = audience.code in audiences

    /** Millions of active players this week. */
    fun players(week: Int, ownConsoleUnits: Int = 0): Double {
        if (this == OWN_CONSOLE) return ownConsoleUnits / 1_000_000.0 * 2.5
        if (week < launch || week >= retire) return 0.0
        val ramp = min(1.0, (week - launch) / 52.0 + 0.15)
        val decline = min(1.0, (retire - week) / 104.0)
        val growth = if (this == PC || this == GRPHONE) 1 + week / 780.0 * 0.6 else 1.0
        return peakPlayers * ramp * decline * growth
    }

    fun isAvailable(week: Int, ownConsoleUnits: Int = 0): Boolean = players(week, ownConsoleUnits) >= 0.5
}

/** What gamers are into right now. */
@Serializable
data class Trend(val topic: Topic? = null, val genre: Genre? = null, val until: Int) {
    val title: String get() = topic?.title ?: genre?.title ?: ""

    fun matches(c: GameConcept): Boolean = (topic != null && c.topic == topic) || (genre != null && (c.genre == genre || c.genre2 == genre))
}

/** Parody review outlets. Each has its own taste. */
@Serializable
enum class Outlet {
    PIXEL_GAZETTE, GAMESPOTTY, CRITICAL_HIT, LOADING_SCREEN;

    val title: String
        get() = when (this) {
            PIXEL_GAZETTE -> "Pixel Gazette"
            GAMESPOTTY -> "GameSpotty"
            CRITICAL_HIT -> "Critical Hit"
            LOADING_SCREEN -> "The Loading Screen"
        }

    /** Score nudge from this outlet's taste. */
    internal fun bias(concept: GameConcept, techShare: Double): Double = when (this) {
        PIXEL_GAZETTE -> if (techShare > 0.55) 0.35 else -0.1
        GAMESPOTTY -> -0.3
        CRITICAL_HIT -> if (concept.genre == Genre.RPG || concept.genre == Genre.ADVENTURE) 0.35 else 0.0
        LOADING_SCREEN -> 0.2
    }
}

@Serializable
data class Review(val outlet: Outlet, val score: Int, val comment: String)

/** Everything decided before development starts. */
@Serializable
data class GameConcept(
    val title: String,
    val topic: Topic,
    val genre: Genre,
    val genre2: Genre? = null,
    val audience: Audience = Audience.EVERYONE,
    val platforms: List<Platform>,
    val size: GameSize,
    val engineId: Int = 0,
    /** The release this is a sequel to. */
    val sequelOf: Int? = null,
) {
    val genreTitle: String get() = if (genre2 == null) genre.title else "${genre.title}-${genre2.title}"
    val genreLower: String get() = if (genre2 == null) genre.lower else "${genre.lower}-${genre2.lower}"

    val comboFit: Double
        get() = if (genre2 == null) topic.fit(genre).multiplier else (topic.fit(genre).multiplier + topic.fit(genre2).multiplier) / 2

    val idealDesignShare: Double
        get() = if (genre2 == null) genre.idealDesignShare else genre.idealDesignShare * 0.6 + genre2.idealDesignShare * 0.4

    /** Ideal share of a stage's effort for an area, from the genre(s). */
    fun idealShare(area: FocusArea): Double {
        val areas = FocusArea.forStage(area.stage)
        fun w(a: FocusArea) = if (genre2 == null) weightValue(genre.weight(a)) else weightValue(genre.weight(a)) * 0.6 + weightValue(genre2.weight(a)) * 0.4
        return w(area) / areas.sumOf { w(it) }
    }

    private fun weightValue(level: Int) = when (level) {
        0 -> 0.2
        1 -> 0.6
        else -> 1.0
    }
}

/** How close an allocation is to a genre's ideal, 0...1. */
internal fun focusMatch(concept: GameConcept, stage: Int, allocation: List<Double>): Double {
    val areas = FocusArea.forStage(stage)
    val total = allocation.sum().takeIf { it > 0 } ?: return 0.5
    val distance = areas.indices.sumOf { i -> kotlin.math.abs(allocation[i] / total - concept.idealShare(areas[i])) }
    return max(0.0, 1 - distance / 1.2)
}

internal object GameNames {
    val first = listOf(
        "Neon", "Moss", "Iron", "Velvet", "Hollow", "Crimson", "Tiny", "Deep", "Paper", "Glitch", "Cinder", "Echo",
        "Quiet", "Star", "Rust", "Pocket", "Last", "Sky", "Mythic", "Boss", "Salt", "Crab", "Lunar", "Pixel",
    )
    val second = listOf(
        "Knight", "Courier", "Tactics", "Lantern", "Kingdom", "Saga", "Garden", "Drift", "Raiders", "Empire",
        "Laundry", "Commute", "Rush", "Orchard", "Planet", "Frontier", "Legends", "Heist", "Odyssey", "Protocol",
    )
    val clients = listOf("Gigaplay", "a cereal brand", "a bank", "a theme park", "a sneaker company", "a soda brand", "a car maker", "a museum")
    val expansions = listOf(
        "Rising Tide", "The Lost Isles", "Frozen Throne", "Second Wind", "Afterglow", "Echoes", "The Deep End",
        "Dark Harvest", "New Horizons", "Overtime", "Reloaded", "Wild Card",
    )
    val publishers = listOf("Megapublish", "Blue Comet Games", "Overdrive Media", "Lantern & Co", "Northstar Interactive")
}
