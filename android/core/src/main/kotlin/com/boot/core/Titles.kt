package com.boot.core

import kotlin.random.Random

/*
 * Game titles that sound like games: built from the topic's vocabulary, shaped by the genre
 * (RPGs get chronicles and legends, strategy games get commands and wars, sims get tycoons) and
 * by the year (arcade names in 1980, quests in the nineties, subtitles in the 2000s, one strange
 * word for an indie in the 2010s), with sequels and expansions named the way real series name them.
 */

private enum class Theme { FANTASY, SCIFI, GRIM, LIGHT, SPORTY, MODERN }

private val Topic.theme: Theme
    get() = when (this) {
        Topic.FANTASY, Topic.MEDIEVAL, Topic.MYTHOLOGY, Topic.VAMPIRES, Topic.PIRATES, Topic.FOLKLORE,
        Topic.STEAMPUNK, Topic.SAMURAI, Topic.VIKINGS, Topic.DRAGONS, Topic.WIZARDS, Topic.MONSTER_TAMING -> Theme.FANTASY
        Topic.SCI_FI, Topic.SPACE, Topic.ALIENS, Topic.MARS, Topic.ROBOTS, Topic.ARTIFICIAL_INTELLIGENCE,
        Topic.CYBERPUNK, Topic.TIME_TRAVEL, Topic.HACKING -> Theme.SCIFI
        Topic.MILITARY, Topic.SPY, Topic.WILD_WEST, Topic.POST_APOCALYPTIC, Topic.ZOMBIES, Topic.HORROR,
        Topic.DETECTIVE, Topic.MYSTERY, Topic.NINJA, Topic.HEIST, Topic.SURVIVAL, Topic.GANGSTERS, Topic.PRISON,
        Topic.GHOSTS, Topic.POLITICS -> Theme.GRIM
        Topic.RACING, Topic.SPORTS, Topic.SKATEBOARDING, Topic.ESPORTS_SCENE, Topic.AVIATION -> Theme.SPORTY
        Topic.BUSINESS, Topic.HOSPITAL, Topic.FASHION, Topic.SCHOOL, Topic.CITY, Topic.MUSIC, Topic.BALLET, Topic.TRAINS -> Theme.MODERN
        else -> Theme.LIGHT
    }

private val Theme.adjectives: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("Shattered", "Forgotten", "Eternal", "Hollow", "Crimson", "Last", "Ancient", "Sunken", "Ashen", "Gilded", "Wandering", "Silver")
        Theme.SCIFI -> listOf("Stellar", "Void", "Neon", "Quantum", "Silent", "Zero", "Distant", "Broken", "Infinite", "Chrome", "Final", "Hidden")
        Theme.GRIM -> listOf("Iron", "Black", "Last", "Silent", "Bitter", "Dead", "Cold", "Burning", "Red", "Grim", "Lost", "Hard")
        Theme.LIGHT -> listOf("Tiny", "Happy", "Super", "Big", "Little", "Cozy", "Wild", "Grand", "Golden", "Lucky", "Sunny", "Merry")
        Theme.SPORTY -> listOf("Turbo", "Extreme", "Pro", "Ultimate", "Max", "Street", "Total", "Rapid", "Hyper", "Wild")
        Theme.MODERN -> listOf("Grand", "Golden", "Big", "Bright", "Prime", "Late Night", "Royal", "Rising", "Hidden", "Perfect")
    }

/** Places stay free of trailing numbers, so a sequel's number is never mistaken for part of the name. */
private val Theme.places: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("Eldermoor", "Vaelheim", "Thornwick", "Ashgard", "Duskhold", "Ironvale", "Mirewood", "Highspire", "Wyrmrest", "Saltmarsh")
        Theme.SCIFI -> listOf("Kepler", "Titan Station", "Nova Prime", "Sector Zero", "Orion Deep", "New Shanghai", "Helios", "Europa", "Halcyon", "Proxima")
        Theme.GRIM -> listOf("Dust Creek", "Ravenport", "Blackwater", "Fort Hollow", "Coldharbor", "Ashfall", "Grey Ridge", "Hollow Pines", "Salt Flats")
        Theme.LIGHT -> listOf("Maple Bay", "Sunny Hills", "Coral Cove", "Starlight City", "Pine Valley", "Harbor Town", "Puddle Lane", "Honey Hollow")
        Theme.SPORTY -> listOf("Neo Tokyo", "Riverside", "Bay City", "Monte Rosso", "Kingsway", "Sunset Strip", "Redwood Ridge")
        Theme.MODERN -> listOf("Metro City", "Westbrook", "Silver Lake", "Ashford", "Kingsbridge", "Lakeview", "Union Square")
    }

/** Endings for made-up one-word titles: Emberfall, Runeborn, Pulsecore. */
private val Theme.suffixes: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("fall", "bound", "born", "guard", "song", "heart", "forge")
        Theme.SCIFI -> listOf("fall", "core", "shift", "point", "drive")
        Theme.GRIM -> listOf("fall", "burn", "shade", "watch")
        Theme.LIGHT -> listOf("land", "ville", "topia")
        Theme.SPORTY -> emptyList()
        Theme.MODERN -> emptyList()
    }

/** What comes after the colon in a sequel. "@" is a place, "#" one of the topic's words. */
private val Theme.subtitles: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("The Ashen Crown", "Wrath of the Old Gods", "The Last Oath", "Embers of @", "Shadow of @", "The Sunken King", "Heart of Winter", "Rise of the #")
        Theme.SCIFI -> listOf("Zero Hour", "Deep Signal", "Event Horizon", "The Long Dark", "First Contact", "Dead Orbit", "Escape from @", "Beyond @")
        Theme.GRIM -> listOf("No Mercy", "Blood Debt", "Ashes to Ashes", "The Long Night", "Judgement Day", "Last Light", "Return to @", "The @ Files")
        Theme.LIGHT -> listOf("Second Helpings", "All Together", "The Big Day", "Party Time", "Summer Holiday", "Back to @", "A New Friend", "Around the World")
        Theme.SPORTY -> listOf("World Tour", "Championship Edition", "All-Stars", "Overdrive", "Unlimited", "Road to @", "Street Kings", "Legends")
        Theme.MODERN -> listOf("Second Term", "The Big Leagues", "Under Pressure", "Rush Hour", "Behind Closed Doors", "Night Shift", "Return to @", "Grand Opening")
    }

private val Theme.expansions: List<String>
    get() = when (this) {
        Theme.FANTASY -> listOf("The Frozen Throne", "Lords of the Deep", "The Lost Isles", "Curse of @", "Heart of Thorns", "The Old Roads")
        Theme.SCIFI -> listOf("Dark Matter", "Outer Rim", "Signal Lost", "The @ Incident", "Cold Storage", "Overclock")
        Theme.GRIM -> listOf("Dead of Winter", "Nightshift", "Scorched Earth", "The @ Job", "Bad Blood", "Undertow")
        Theme.LIGHT -> listOf("Summer Fun", "Winter Wonderland", "Island Getaway", "Seaside Days", "Spooky Season", "Festival Days")
        Theme.SPORTY -> listOf("World Tour", "Legends Pack", "Night Circuit", "Pro Season", "Rivals", "Overtime")
        Theme.MODERN -> listOf("After Hours", "Big City Lights", "Late Shift", "Boom Times", "Hostile Takeover", "The @ Years")
    }

/** The topic as one word for a sim's name: Cooking Tycoon, Hospital Simulator. */
private val Topic.simWord: String
    get() = when (this) {
        Topic.ARTIFICIAL_INTELLIGENCE -> "AI"
        Topic.ESPORTS_SCENE -> "Esports"
        Topic.DESERT_ISLAND -> "Island"
        Topic.POST_APOCALYPTIC -> "Wasteland"
        Topic.MONSTER_TAMING -> "Monster"
        Topic.CARD_BATTLES -> "Card"
        Topic.SCI_FI -> "Starship"
        Topic.TIME_TRAVEL -> "Time Machine"
        Topic.WILD_WEST -> "Frontier"
        Topic.SUPERHEROES -> "Hero"
        Topic.DINOSAURS -> "Dino"
        Topic.PETS -> "Pet Shop"
        Topic.ROBOTS -> "Robot"
        Topic.TRAINS -> "Train"
        Topic.ZOMBIES -> "Zombie"
        Topic.VAMPIRES -> "Vampire"
        Topic.PIRATES -> "Pirate"
        Topic.GHOSTS -> "Ghost"
        Topic.ALIENS -> "Alien"
        Topic.VIKINGS -> "Viking"
        Topic.DRAGONS -> "Dragon"
        Topic.WIZARDS -> "Wizard"
        Topic.GANGSTERS -> "Gangster"
        Topic.NINJA -> "Ninja"
        else -> title.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
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
        Topic.HEIST -> listOf("Score", "Vault", "Crew", "Getaway", "Safehouse", "Take")
        Topic.STEAMPUNK -> listOf("Cog", "Aether", "Brass", "Airship", "Gears", "Clockwork")
        Topic.SURVIVAL -> listOf("Shelter", "Scavenge", "Frost", "Camp", "Rations", "Wilderness")
        Topic.BALLET -> listOf("Stage", "Pointe", "Curtain", "Arabesque", "Encore", "Spotlight")
        Topic.FOLKLORE -> listOf("Tale", "Hearth", "Spirit", "Village", "Legend", "Folk")
        Topic.ESPORTS_SCENE -> listOf("Ladder", "Circuit", "Franchise", "Meta", "Finals", "Draft")
        Topic.SAMURAI -> listOf("Katana", "Shogun", "Daimyo", "Bushido", "Cherry Blossom", "Honor")
        Topic.VIKINGS -> listOf("Longship", "Raid", "Saga", "Fjord", "Jarl", "Rune")
        Topic.DRAGONS -> listOf("Wyrm", "Hoard", "Scale", "Flame", "Wing", "Drake")
        Topic.GANGSTERS -> listOf("Family", "Racket", "Mob", "Tommy Gun", "Speakeasy", "Turf")
        Topic.PRISON -> listOf("Cell Block", "Yard", "Breakout", "Warden", "Lockdown", "Tunnel")
        Topic.CIRCUS -> listOf("Big Top", "Ringmaster", "Trapeze", "Clown", "Carnival", "Spotlight")
        Topic.TRAINS -> listOf("Express", "Junction", "Railway", "Loco", "Signal", "Freight")
        Topic.AVIATION -> listOf("Skies", "Wings", "Runway", "Ace", "Jet Stream", "Altitude")
        Topic.DESERT_ISLAND -> listOf("Castaway", "Shore", "Lagoon", "Raft", "Palm", "Atoll")
        Topic.GHOSTS -> listOf("Haunt", "Phantom", "Spirit", "Specter", "Manor", "Poltergeist")
        Topic.MONSTER_TAMING -> listOf("Tamer", "Critter", "Bond", "Beast", "Arena", "Evolve")
        Topic.CARD_BATTLES -> listOf("Deck", "Hand", "Trump", "Draw", "Duel", "Arcana")
        Topic.POLITICS -> listOf("Ballot", "Senate", "Campaign", "Cabinet", "Mandate", "Office")
        Topic.WIZARDS -> listOf("Spell", "Tome", "Staff", "Arcane", "Tower", "Familiar")
        Topic.JUNGLE -> listOf("Canopy", "Temple", "Vine", "Expedition", "Idol", "Wilds")
        Topic.SKATEBOARDING -> listOf("Kickflip", "Grind", "Halfpipe", "Ollie", "Street", "Session")
    }

private val subtitles = listOf(
    "Reborn", "Origins", "Legacy", "Uprising", "Awakening", "Revelations", "Resurgence", "Ascension",
    "Redemption", "Requiem", "Unleashed", "Endgame", "Aftershock", "Homecoming", "Exodus", "Dominion",
)

private fun single(s: String) = ' ' !in s && '-' !in s

private fun plural(s: String) = when {
    s.endsWith("s") || s.endsWith("x") -> s
    s.endsWith("y") && !s.endsWith("ey") -> s.dropLast(1) + "ies"
    s.endsWith("ch") || s.endsWith("sh") -> s + "es"
    else -> s + "s"
}

private fun words(s: String) = s.lowercase().split(' ', '-').toSet()

/** The look of a title in its day. */
private enum class TitleAge { ARCADE, CLASSIC, MILLENNIUM, INDIE }

private fun ageOf(year: Int) = when {
    year < 1986 -> TitleAge.ARCADE
    year < 2000 -> TitleAge.CLASSIC
    year < 2010 -> TitleAge.MILLENNIUM
    else -> TitleAge.INDIE
}

/** Everything a template needs, picked once per try. */
private class Parts(topic: Topic, r: Random) {
    val theme = topic.theme
    val n: String = topic.nouns.random(r)
    val n2: String = (topic.nouns - n).random(r)
    /** An adjective that doesn't repeat the noun: no "Neon Neon". */
    val a: String = theme.adjectives.filter { (words(it) intersect words(n)).isEmpty() }.random(r)
    val p: String = theme.places.random(r)
    val sim: String = topic.simWord
    val sub: String = fill(theme.subtitles.random(r), p, n)
    /** A made-up word, when the noun takes one well: Emberfall, but not Signalline or Tidesbound. */
    val coined: String? = theme.suffixes.shuffled(r).firstOrNull { s ->
        single(n) && n.length <= 7 && !n.endsWith("s") && !n.endsWith(s) && n.last().lowercaseChar() != s.first()
    }?.let { n + it }
    val one: Boolean = single(n)
}

private fun fill(template: String, place: String, noun: String) = template.replace("@", place).replace("#", noun)

private fun genreTemplates(genre: Genre, x: Parts): List<String?> = with(x) {
    when (genre) {
        Genre.ACTION -> listOf(
            "$a $n", "$n: $sub", if (n != "Protocol") "$n Protocol" else null, "Operation $a $n", "$n Strike", coined,
            "Rogue $n", "$n Unleashed",
        )
        Genre.ADVENTURE -> listOf(
            "The $a $n", "$n of $p", "Journey to $p", "Beyond $p", "Secrets of $p", "The $n of $p", "Lost in $p",
            if (one) "A $n's Tale" else null, coined,
        )
        Genre.RPG -> listOf(
            "$p: $a $n", "Chronicles of $p", "The $a $n", "Tales of $p", "Legend of the $a $n", coined,
            if (one) "$n Saga" else null, if (one) "$a ${plural(n)}" else null,
            if (theme == Theme.FANTASY) "$n & $n2" else null,
        )
        Genre.SIMULATION -> listOf(
            "$sim Tycoon", "$sim Simulator", if (theme == Theme.LIGHT || theme == Theme.MODERN) "$p $sim Story" else null,
            "$n Life", "$n Manager", "$sim Inc.", "$p Days", "My $n", if (sim != "Business") "$sim Empire" else null,
        )
        Genre.STRATEGY -> listOf(
            "$n Command", "$p Tactics", "$n Wars", "Total $n", "$n: Empires", if (one) "Age of ${plural(n)}" else null,
            "$n Lords", "Rise of $p", "$p Conquest",
        )
        Genre.CASUAL -> listOf(
            "$a $n", "$n Pop!", "$n Party", "$n Match", if (one) "Tiny ${plural(n)}" else null, if (one) "$a ${plural(n)}" else null,
            if (one) "$n Saga" else null, "$n Blast", "$n Friends",
        )
        Genre.PUZZLE -> listOf(
            "$n Puzzle", "The $n Riddle", "$a $n Box", "$n Logic", if (one) "${n}doku" else null, "Unlock $p", "The $n Room", "$n Link",
        )
        Genre.PLATFORMER -> listOf(
            "Super $n", "$n Jump", "$n Dash", "$n Land", "$n World", if (one) "$n Bros." else null, "$a $n", "$n Kid", "$n Odyssey",
        )
        Genre.SHOOTER -> listOf(
            "$n Ops", "$a $n: Warfare", "$n Battlefront", "Call of $p", "$n Fire", "Squad $n", "Siege of $p", "$a $n",
        )
        Genre.FIGHTING -> listOf(
            "$n Fighter", "$a $n Combat", "$n Kombat", "$n Clash", "Fist of $p", "$n Tournament", "$a Fist", "$n Brawl",
        )
        Genre.RHYTHM -> listOf(
            "$n Beat", "$n Hero", "$n Revolution", "Dance $n", "$a Rhythm", "$n Groove", "$n Band", "$n Fever",
        )
        Genre.SANDBOX -> listOf(
            "$n Craft", "$p Builder", "Block $n", if (one) "${n}topia" else null, "Make $p", "$n Island", "$n Worlds", coined,
        )
    }
}

/** Names that date a game: the same idea is "Ember Attack" in 1981 and "Ember Survivors" in 2022. */
private fun ageTemplates(age: TitleAge, year: Int, genre: Genre, x: Parts): List<String?> = with(x) {
    when (age) {
        TitleAge.ARCADE -> listOf(
            "$n Attack", "Super $n", "Mega $n", "$n Blaster", if (one && n.length <= 6) "${n}tron" else null,
            "$n Invaders", if (one && n.length <= 6 && !n.endsWith("s")) "$n Man" else null,
            if (theme == Theme.SCIFI) "Space $n" else null, "$n Command",
        )
        TitleAge.CLASSIC -> listOf(
            "$n Quest", "$n Deluxe", "Xtreme $n", "$n Turbo", "$n Gold", "Ultimate $n",
            if (theme == Theme.FANTASY || theme == Theme.LIGHT) "Legend of $p" else null,
            if (genre == Genre.PLATFORMER || genre == Genre.ACTION) "$n Kid" else null,
        )
        TitleAge.MILLENNIUM -> listOf(
            "$n: $sub", "$n Online", "$n Evolution", "$n Unleashed", "$n Legends", "$n Revolution", "$a $n: $sub",
        )
        TitleAge.INDIE -> listOf(
            coined, coined, if (one) n else null, "A $a $n",
            if (genre in setOf(Genre.SIMULATION, Genre.SANDBOX, Genre.ACTION, Genre.CASUAL)) "$sim Simulator" else null,
            if (theme == Theme.LIGHT || theme == Theme.MODERN) "Untitled $n Game" else null,
            "The $n of $p",
            if (year >= 2020) "$n Survivors" else null, if (year >= 2017 && genre == Genre.SHOOTER) "$n Royale" else null,
        )
    }
}

/**
 * A title for a new game. The same salt always gives the same answer, so the UI can reroll cleanly.
 * With a [year], it sounds like a game from then.
 */
fun gameTitle(topic: Topic, genre: Genre, salt: Long, used: Set<String> = emptySet(), year: Int? = null): String {
    val r = Random(salt * 31 + topic.ordinal * 7 + genre.ordinal)
    repeat(30) {
        val x = Parts(topic, r)
        val dated = year?.let { ageTemplates(ageOf(it), it, genre, x).filterNotNull() }.orEmpty()
        val options = genreTemplates(genre, x).filterNotNull()
        // About a third of the time, the year has its say.
        val t = if (dated.isNotEmpty() && r.nextDouble() < 0.35) dated.random(r) else options.random(r)
        if (t !in used && t.length <= 30 && !repeats(t)) return t
    }
    return "${topic.nouns.random(r)} ${(used.size + 1)}"
}

/** "Cipher Cipher" and "Dust: Dust Storm" read like mistakes. */
private fun repeats(title: String): Boolean {
    val w = title.lowercase().split(' ', ':', '-', '&').filter { it.length > 2 }
    return w.size != w.toSet().size
}

private val romans = listOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV")

fun roman(n: Int): String = romans.getOrElse(n) { "$n" }

/** The series name a title belongs to: "Moss Knight II: Reborn" is a "Moss Knight" game. */
fun seriesName(title: String): String =
    title.substringBefore(":").replace(Regex(" (\\d+|[IVX]+)$"), "").trim()

/** Sports games are named for the season they come out before: Kickoff 98, Kickoff 06. */
private val Topic.yearly: Boolean get() = this == Topic.SPORTS || this == Topic.RACING || this == Topic.ESPORTS_SCENE

/**
 * What to call the [n]th game in a series, the way real series do it: numbers, Roman numerals,
 * subtitles in the voice of the topic, and, for sports games with a [year], the season.
 */
fun sequelTitle(series: String, n: Int, topic: Topic, salt: Long, year: Int? = null): String {
    val r = Random(salt * 17 + n)
    if (year != null && topic.yearly && year >= 1990 && r.nextDouble() < 0.6) {
        return "$series ${"%02d".format((year + 1) % 100)}"
    }
    val theme = topic.theme
    val sub = fill((subtitles + theme.subtitles + theme.subtitles).random(r), theme.places.random(r), topic.nouns.random(r))
    return when (r.nextInt(if (n == 2) 4 else 3)) {
        0 -> "$series $n"
        1 -> "$series ${roman(n)}"
        2 -> "$series ${roman(n)}: $sub"
        else -> "$series: $sub"
    }
}

/** Names for an expansion of a game about [topic], in its voice first and then the usual ones. */
fun expansionTitles(topic: Topic, salt: Long): List<String> {
    val theme = topic.theme
    val place = theme.places[Math.floorMod(salt, theme.places.size.toLong()).toInt()]
    return theme.expansions.map { fill(it, place, topic.nouns.first()) } + GameNames.expansions
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
        Genre.PUZZLE -> listOf("Puzzle", "Puzzler", "Logic", "Riddles")
        Genre.PLATFORMER -> listOf("Jump", "Dash", "Land", "Run")
        Genre.SHOOTER -> listOf("Ops", "Warfare", "Squad", "Firefight")
        Genre.FIGHTING -> listOf("Fighter", "Clash", "Brawl", "Tournament")
        Genre.RHYTHM -> listOf("Beat", "Rhythm", "Dance", "Groove")
        Genre.SANDBOX -> listOf("Craft", "Builder", "Worlds", "Sandbox")
    }
    return "$series ${names[Math.floorMod(variant, names.size)]}"
}
