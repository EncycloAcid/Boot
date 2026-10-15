package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.pow
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
    ARTIFICIAL_INTELLIGENCE("TS", "VR", "C", "EM"),
    HEIST("AV", "T", "CS", "EM", "Y"),
    STEAMPUNK("RA", "VS", "C", "EM"),
    SURVIVAL("SA", "TV", "C", "EM", "Y"),
    BALLET("C", "VS", "ART", "YE", "M"),
    FOLKLORE("RV", "AT", "CS", "EM"),
    ESPORTS_SCENE("AC", "S", "VRT", "YE", "M");

    val title: String
        get() = when (this) {
            SCI_FI -> "Sci-Fi"
            POST_APOCALYPTIC -> "Post-apocalyptic"
            WILD_WEST -> "Wild West"
            TIME_TRAVEL -> "Time travel"
            ARTIFICIAL_INTELLIGENCE -> "Artificial intelligence"
            ESPORTS_SCENE -> "Esports"
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
    // Enum names are kept from earlier versions so old saves still load. Weeks are on the shared
    // timeline, where week 0 is the start of MODERN_YEAR; a run started today begins there.
    PC("PC", "Valve (Steam)", -2_600, 100_000, 55.0, 1.0, 0.0, "TSR", "", "EM"),
    PLAYSYSTEM_5("PlayStation 5", "Sony", -322, 330, 50.0, 1.0, 80_000.0, "AVR", "T", "EM"),
    MBOX_Z("Xbox Series X|S", "Microsoft", -322, 330, 28.0, 1.0, 80_000.0, "AS", "C", "M"),
    SWITCHEROO("Nintendo Switch", "Nintendo", -514, 160, 42.0, 0.9, 50_000.0, "CVA", "ST", "YE"),
    GRPHONE("iPhone & Android", "Apple and Google", -988, 100_000, 400.0, 0.08, 2_000.0, "C", "TS", "YE"),
    VAPOR_DECK("Steam Deck", "Valve", -255, 560, 9.0, 1.1, 0.0, "RTA", "", "EM"),
    STRATOS("Amazon Luna", "Amazon", -220, 230, 6.0, 0.7, 20_000.0, "A", "T", "E"),
    VISOR_VR("Meta Quest", "Meta", -376, 620, 14.0, 1.1, 30_000.0, "AS", "TR", "EM"),
    SWITCHEROO_2("Nintendo Switch 2", "Nintendo", -82, 100_000, 60.0, 0.9, 70_000.0, "CVA", "ST", "YE"),
    PLAYSYSTEM_6("PlayStation 6", "Sony", 110, 100_000, 65.0, 1.0, 120_000.0, "AVR", "T", "EM"),
    MBOX_INFINITY("Next-gen Xbox", "Microsoft", 100, 100_000, 34.0, 1.0, 120_000.0, "AS", "C", "M"),
    SPECTACLE_AR("Apple Vision", "Apple", 300, 100_000, 30.0, 0.8, 50_000.0, "CA", "T", "YE"),

    // The consoles of the past, for runs that start there. They're long retired by today.
    ATARI_2600("Atari 2600", "Atari", -2_584, -1_820, 18.0, 1.0, 0.0, "AC", "RS", "YE"),
    C64("Commodore 64", "Commodore", -2_324, -1_700, 14.0, 0.9, 0.0, "AVS", "", "YEM"),
    NES("NES", "Nintendo", -2_174, -1_664, 34.0, 1.0, 20_000.0, "AVC", "S", "YE"),
    GAME_BOY("Game Boy", "Nintendo", -1_957, -1_248, 40.0, 0.7, 15_000.0, "CV", "ST", "YE"),
    GENESIS("Sega Genesis", "Sega", -1_950, -1_508, 22.0, 1.0, 20_000.0, "A", "", "YEM"),
    SNES("Super Nintendo", "Nintendo", -1_836, -1_456, 30.0, 1.0, 25_000.0, "RVA", "", "YE"),
    PLAYSTATION("PlayStation", "Sony", -1_628, -1_144, 60.0, 1.0, 30_000.0, "AVR", "", "EM"),
    N64("Nintendo 64", "Nintendo", -1_576, -1_300, 22.0, 1.0, 30_000.0, "AV", "T", "YE"),
    PS2("PlayStation 2", "Sony", -1_362, -728, 90.0, 1.0, 40_000.0, "AVR", "", "EM"),
    XBOX("Xbox", "Microsoft", -1_304, -1_092, 18.0, 1.0, 40_000.0, "AS", "C", "M"),
    GAMECUBE("GameCube", "Nintendo", -1_304, -1_040, 15.0, 1.0, 35_000.0, "CVA", "ST", "YE"),
    NINTENDO_DS("Nintendo DS", "Nintendo", -1_144, -676, 80.0, 0.8, 25_000.0, "CV", "ST", "YE"),
    XBOX_360("Xbox 360", "Microsoft", -1_092, -572, 60.0, 1.0, 50_000.0, "AS", "C", "M"),
    WII("Wii", "Nintendo", -1_040, -676, 70.0, 0.8, 40_000.0, "CS", "TR", "YE"),
    PS3("PlayStation 3", "Sony", -1_040, -520, 60.0, 1.0, 50_000.0, "AVR", "", "EM"),
    PS4("PlayStation 4", "Sony", -684, -156, 85.0, 1.0, 60_000.0, "AVR", "T", "EM"),
    XBOX_ONE("Xbox One", "Microsoft", -684, -260, 40.0, 1.0, 60_000.0, "AS", "C", "M"),

    /** Your own handheld, once Games and Hardware have built the Handheld Console crossover. */
    OWN_CONSOLE("Your handheld", "You", -2_000, 100_000, 0.0, 1.2, 0.0, "AVRSTC", "", "YEM");

    fun fit(genre: Genre): Fit = when (genre.code) {
        in good -> Fit.GOOD
        in bad -> Fit.BAD
        else -> Fit.OKAY
    }

    fun suits(audience: Audience): Boolean = audience.code in audiences

    /** Who made it in [year]: before Steam, PC games came on disks from the shops. */
    fun makerIn(year: Int): String = when {
        this == PC && year < 1982 -> "Apple, Commodore and Tandy"
        this == PC && year < 2004 -> "IBM PC and clones"
        this == GRPHONE && year < 2009 -> "Apple"
        else -> maker
    }

    /** Millions of active players in this week of the timeline, where week 0 is the start of MODERN_YEAR. */
    fun players(week: Int, ownConsoleUnits: Int = 0): Double {
        if (this == OWN_CONSOLE) return ownConsoleUnits / 1_000_000.0 * 2.5
        if (week < launch || week >= retire) return 0.0
        val ramp = min(1.0, (week - launch) / 52.0 + 0.15)
        val decline = min(1.0, (retire - week) / 104.0)
        // Computers and phones keep growing: from launch to today, then on from there.
        val growth = when {
            this != PC && this != GRPHONE -> 1.0
            week >= 0 -> 1 + week / 780.0 * 0.6
            this == PC -> 0.3 + 0.7 * (week - launch).toDouble() / -launch
            else -> 0.25 + 0.75 * (week - launch).toDouble() / -launch
        }
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

/** What you charge for a game. Great games can charge more; weak ones sell better cheap. */
@Serializable
enum class PriceTier(val title: String, val multiplier: Double) {
    BUDGET("Budget", 0.6),
    STANDARD("Standard", 1.0),
    PREMIUM("Premium", 1.4);

    /**
     * How the price changes launch revenue for a game of this score. Buyers are price sensitive,
     * less so for great games: a 9 sells nearly as many copies at a premium; a 5 loses a lot.
     */
    fun revenueFactor(score: Double): Double {
        val elasticity = (2.2 - 0.18 * score).coerceIn(0.4, 1.9)
        return multiplier.pow(1 - elasticity)
    }
}

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
    val price: PriceTier = PriceTier.STANDARD,
    /** A live game: seasons, in-game purchases and servers. */
    val live: Boolean = false,
    /** The release this remasters: the same game rebuilt for today, at half the work. */
    val remasterOf: Int? = null,
) {
    /** What a copy sells for. */
    val unitPrice: Double get() = size.price * price.multiplier

    /** Work points to finish: a remaster starts from a finished game. */
    val work: Double get() = size.work * (if (remasterOf != null) REMASTER_WORK else 1.0)

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
    val clients = listOf("Electronic Arts", "Kellogg's", "JPMorgan Chase", "Disney", "Nike", "Coca-Cola", "Toyota", "the Smithsonian", "LEGO", "McDonald's")
    val expansions = listOf(
        "Rising Tide", "The Lost Isles", "Frozen Throne", "Second Wind", "Afterglow", "Echoes", "The Deep End",
        "Dark Harvest", "New Horizons", "Overtime", "Reloaded", "Wild Card",
    )
    /** Modern-day default list; prefer [publishersIn] so decades runs stay historically accurate. */
    val publishers: List<String> get() = publishersIn(MODERN_YEAR)
}
