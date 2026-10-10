package com.boot.core

import kotlin.random.Random

/*
 * Game titles that sound like games: built from the topic's vocabulary and shaped by the genre
 * (RPGs get chronicles and legends, strategy games get commands and wars, sims get tycoons), and
 * sequels named the way real series name them.
 */

private enum class Theme { FANTASY, SCIFI, GRIM, LIGHT }

private val Topic.theme: Theme
    get() = when (this) {
        Topic.FANTASY, Topic.MEDIEVAL, Topic.MYTHOLOGY, Topic.VAMPIRES, Topic.PIRATES -> Theme.FANTASY
        Topic.SCI_FI, Topic.SPACE, Topic.ALIENS, Topic.MARS, Topic.ROBOTS, Topic.ARTIFICIAL_INTELLIGENCE,
        Topic.CYBERPUNK, Topic.TIME_TRAVEL, Topic.HACKING -> Theme.SCIFI
        Topic.MILITARY, Topic.SPY, Topic.WILD_WEST, Topic.POST_APOCALYPTIC, Topic.ZOMBIES, Topic.HORROR,
        Topic.DETECTIVE, Topic.MYSTERY, Topic.NINJA -> Theme.GRIM
        else -> Theme.LIGHT
    }

private val Theme.adjectives: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("Shattered", "Forgotten", "Eternal", "Hollow", "Crimson", "Last", "Ancient", "Sunken", "Ashen", "Gilded")
        Theme.SCIFI -> listOf("Stellar", "Void", "Neon", "Quantum", "Silent", "Zero", "Distant", "Broken", "Infinite", "Chrome")
        Theme.GRIM -> listOf("Iron", "Black", "Last", "Silent", "Bitter", "Dead", "Cold", "Burning", "Red", "Grim")
        Theme.LIGHT -> listOf("Tiny", "Happy", "Super", "Big", "Little", "Cozy", "Wild", "Grand", "Golden", "Lucky")
    }

private val Theme.places: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("Eldermoor", "Vaelheim", "Thornwick", "Ashgard", "Duskhold", "Ironvale", "Mirewood", "Highspire")
        Theme.SCIFI -> listOf("Kepler-9", "Titan Station", "Nova Prime", "Sector 7", "Orion Deep", "New Shanghai", "Helios")
        Theme.GRIM -> listOf("Dust Creek", "Ravenport", "Blackwater", "Fort Hollow", "Coldharbor", "Ashfall", "Grey Ridge")
        Theme.LIGHT -> listOf("Maple Bay", "Sunny Hills", "Coral Cove", "Starlight City", "Pine Valley", "Harbor Town")
    }

private val Topic.nouns: List<String>
    get() = when (this) {
        Topic.FANTASY -> listOf("Crown", "Ember", "Rune", "Oath", "Dragon", "Throne")
        Topic.SCI_FI -> listOf("Horizon", "Protocol", "Signal", "Frontier", "Vector", "Pulse")
        Topic.RACING -> listOf("Apex", "Drift", "Velocity", "Nitro", "Circuit", "Redline")
        Topic.SPORTS -> listOf("Striker", "Slam", "Champions", "Kickoff", "Rally", "Showdown")
        Topic.MILITARY -> listOf("Front", "Vanguard", "Nightfall", "Warzone", "Outpost", "Strike")
        Topic.SPACE -> listOf("Orbit", "Nebula", "Starfall", "Voyager", "Eclipse", "Odyssey")
        Topic.PIRATES -> listOf("Tides", "Plunder", "Corsair", "Black Flag", "Brine", "Doubloon")
        Topic.DETECTIVE -> listOf("Case", "Alibi", "Verdict", "Shadow", "Witness", "Cold Trail")
        Topic.CITY -> listOf("Skyline", "Avenue", "Metro", "District", "Block", "Downtown")
        Topic.NINJA -> listOf("Shadow", "Shuriken", "Clan", "Ronin", "Blade", "Lotus")
        Topic.ZOMBIES -> listOf("Outbreak", "Dead Zone", "Horde", "Infection", "Last Stand", "Quarantine")
        Topic.HORROR -> listOf("Dread", "Hollow", "Whisper", "Asylum", "Below", "Silence")
        Topic.CYBERPUNK -> listOf("Neon", "Chrome", "Grid", "Ghost", "Circuit", "Static")
        Topic.MEDIEVAL -> listOf("Keep", "Banner", "Siege", "Crown", "Knight", "Realm")
        Topic.FARMING -> listOf("Harvest", "Meadow", "Orchard", "Seasons", "Barn", "Fields")
        Topic.COOKING -> listOf("Kitchen", "Feast", "Recipe", "Diner", "Chef", "Bites")
        Topic.SCHOOL -> listOf("Academy", "Hall Pass", "Detention", "Class", "Campus", "Prom")
        Topic.DINOSAURS -> listOf("Raptor", "Primal", "Fossil", "Rex", "Jurassic", "Tar Pit")
        Topic.SUPERHEROES -> listOf("Justice", "Cape", "Vigilante", "Hero", "Guardian", "Sentinel")
        Topic.POST_APOCALYPTIC -> listOf("Wasteland", "Ruins", "Ashes", "Scavenger", "Aftermath", "Dust")
        Topic.MYSTERY -> listOf("Riddle", "Secret", "Cipher", "Enigma", "Key", "Locked Room")
        Topic.MUSIC -> listOf("Rhythm", "Beat", "Encore", "Harmony", "Tempo", "Bassline")
        Topic.MYTHOLOGY -> listOf("Olympus", "Titan", "Oracle", "Valhalla", "God", "Labyrinth")
        Topic.WILD_WEST -> listOf("Outlaw", "Frontier", "Sheriff", "Gold Rush", "Six-Shooter", "Badlands")
        Topic.ALIENS -> listOf("Invasion", "Contact", "Abduction", "Signal", "Mothership", "Hive")
        Topic.ROBOTS -> listOf("Mech", "Gear", "Circuit", "Automaton", "Steel", "Rust")
        Topic.TIME_TRAVEL -> listOf("Paradox", "Chrono", "Loop", "Rewind", "Epoch", "Timeline")
        Topic.PETS -> listOf("Paws", "Whiskers", "Pals", "Tails", "Puppy", "Purr")
        Topic.BUSINESS -> listOf("Tycoon", "Empire", "Startup", "Capital", "Corp", "Venture")
        Topic.HACKING -> listOf("Exploit", "Firewall", "Zero Day", "Root", "Backdoor", "Payload")
        Topic.VAMPIRES -> listOf("Blood", "Night", "Crypt", "Fang", "Moon", "Coven")
        Topic.OCEAN -> listOf("Abyss", "Reef", "Tide", "Depths", "Coral", "Current")
        Topic.SPY -> listOf("Cipher", "Agent", "Covert", "Dossier", "Double Cross", "Black Ops")
        Topic.MARS -> listOf("Red Planet", "Colony", "Dust Storm", "Ares", "Outpost", "Olympus Mons")
        Topic.FASHION -> listOf("Runway", "Couture", "Style", "Glam", "Catwalk", "Vogue")
        Topic.HOSPITAL -> listOf("Ward", "Code Blue", "Surgeon", "Clinic", "ER", "Pulse")
        Topic.ARTIFICIAL_INTELLIGENCE -> listOf("Singularity", "Sentience", "Neural", "Mind", "Algorithm", "Turing")
    }

private val subtitles = listOf(
    "Reborn", "Origins", "Legacy", "Uprising", "Awakening", "Revelations", "Resurgence", "Ascension",
    "Redemption", "Requiem", "Unleashed", "Endgame", "Aftershock", "Homecoming", "Exodus", "Dominion",
)

private fun single(s: String) = ' ' !in s && '-' !in s

private fun plural(s: String) = when {
    s.endsWith("s") -> s
    s.endsWith("y") && !s.endsWith("ey") -> s.dropLast(1) + "ies"
    else -> s + "s"
}

private fun genreTemplates(genre: Genre, topic: Topic, r: Random): String {
    val t = topic.theme
    val n = topic.nouns.random(r)
    val n2 = (topic.nouns - n).random(r)
    val a = t.adjectives.random(r)
    val p = t.places.random(r)
    val options = when (genre) {
        Genre.ACTION -> listOfNotNull(
            "$a $n", "$n: $a $n2", "$n Protocol", "Operation $a $n", "$n Strike",
            if (single(n) && !n.endsWith("fall")) "${n}fall" else null,
        )
        Genre.ADVENTURE -> listOf("The $a $n", "$n of $p", "Journey to $p", "Beyond $p", "Secrets of $p", "The $n of $p")
        Genre.RPG -> listOfNotNull(
            "$p: $a $n", "Chronicles of $p", "The $a $n", "Tales of $p", "Legend of the $a $n",
            if (single(n)) "${n}bound" else null,
        )
        Genre.SIMULATION -> listOf("${topic.title} Tycoon", "${topic.title} Simulator", "$p ${topic.title} Story", "$a $n Life", "$n Manager")
        Genre.STRATEGY -> listOfNotNull(
            "$n Command", "$p Tactics", "$n Wars", "Total $n", "$a $n: Empire",
            if (single(n)) "Age of ${plural(n)}" else null,
        )
        Genre.CASUAL -> listOfNotNull(
            "$a $n", "$n Pop!", "$n Party", "$n Match", if (single(n)) "Tiny ${plural(n)}" else null, "$a ${plural(n)}",
        )
    }
    return options.random(r)
}

/** A title for a new game. The same salt always gives the same answer, so the UI can reroll cleanly. */
fun gameTitle(topic: Topic, genre: Genre, salt: Long, used: Set<String> = emptySet()): String {
    val r = Random(salt * 31 + topic.ordinal * 7 + genre.ordinal)
    repeat(30) {
        val t = genreTemplates(genre, topic, r)
        if (t !in used) return t
    }
    return "${topic.nouns.random(r)} ${(used.size + 1)}"
}

private val romans = listOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV")

fun roman(n: Int): String = romans.getOrElse(n) { "$n" }

/** The series name a title belongs to: "Moss Knight II: Reborn" is a "Moss Knight" game. */
fun seriesName(title: String): String =
    title.substringBefore(":").replace(Regex(" (\\d+|[IVX]+)$"), "").trim()

/** What to call the [n]th game in a series, the way real series do it. */
fun sequelTitle(series: String, n: Int, topic: Topic, salt: Long): String {
    val r = Random(salt * 17 + n)
    val sub = (subtitles + listOf("Rise of the ${topic.nouns.random(r)}", "Return to ${topic.theme.places.random(r)}")).random(r)
    return when (r.nextInt(if (n == 2) 4 else 3)) {
        0 -> "$series $n"
        1 -> "$series ${roman(n)}"
        2 -> "$series ${roman(n)}: $sub"
        else -> "$series: $sub"
    }
}

/** What to call a spin-off in another genre. [variant] picks another of the genre's usual names. */
fun spinOffTitle(series: String, genre: Genre, variant: Int = 0): String {
    val names = when (genre) {
        Genre.ACTION -> listOf("Arena", "Strike", "Rising", "Unleashed")
        Genre.ADVENTURE -> listOf("Adventures", "Journeys", "Tales", "Quest")
        Genre.RPG -> listOf("Legends", "Chronicles", "Saga", "Odyssey")
        Genre.SIMULATION -> listOf("Life", "Tycoon", "World", "Manager")
        Genre.STRATEGY -> listOf("Tactics", "Command", "Wars", "Conquest")
        Genre.CASUAL -> listOf("Party", "Pop!", "Puzzle", "Go")
    }
    return "$series ${names[Math.floorMod(variant, names.size)]}"
}
