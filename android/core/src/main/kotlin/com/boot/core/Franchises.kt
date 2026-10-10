package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.pow

/*
 * Real game franchises. Their studios release the next entries on something like their real
 * schedules: sports games every autumn, Call of Duty every November, GTA once in a long while.
 * Big releases are announced months ahead, so you can plan around them, and the biggest ones
 * take everyone's attention for a few weeks whatever genre you're in.
 */

/** Year one of a run is 2027. */
fun realYear(week: Int): Int = 2027 + week / 52

internal class RealFranchise(
    val name: String,
    val studio: String,
    val topic: Topic,
    val genre: Genre,
    val size: GameSize,
    /** 0...1: how good its games usually are. 0.98 is a GTA. */
    val quality: Double,
    /** Weeks between entries. */
    val cadence: Int,
    /** Annual series land in the same week every year. */
    val annualWeek: Int? = null,
    /** The next entries, in order. Annual series name themselves after the year instead. */
    val titles: List<String> = emptyList(),
    val yearly: ((Int) -> String)? = null,
) {
    fun title(entry: Int, week: Int): String =
        yearly?.invoke(realYear(week)) ?: titles.getOrNull(entry) ?: "$name ${entry + 2}"
}

private fun yy(year: Int) = String.format(java.util.Locale.US, "%02d", (year + 1) % 100)

internal val RealFranchises = listOf(
    // Electronic Arts
    RealFranchise("EA Sports FC", "Electronic Arts", Topic.SPORTS, Genre.SIMULATION, GameSize.AAA, 0.5, 52, annualWeek = 38, yearly = { "EA Sports FC ${yy(it)}" }),
    RealFranchise("Madden NFL", "Electronic Arts", Topic.SPORTS, Genre.SIMULATION, GameSize.LARGE, 0.42, 52, annualWeek = 33, yearly = { "Madden NFL ${yy(it)}" }),
    RealFranchise("Battlefield", "Electronic Arts", Topic.MILITARY, Genre.ACTION, GameSize.AAA, 0.68, 160, titles = listOf("Battlefield 7", "Battlefield 8", "Battlefield 9")),
    RealFranchise("The Sims", "Electronic Arts", Topic.CITY, Genre.SIMULATION, GameSize.AAA, 0.62, 420, titles = listOf("The Sims 5", "The Sims 6")),
    RealFranchise("Need for Speed", "Electronic Arts", Topic.RACING, Genre.ACTION, GameSize.LARGE, 0.5, 156, titles = listOf("Need for Speed: Underground 3", "Need for Speed: Most Wanted 2", "Need for Speed: Velocity")),
    RealFranchise("Mass Effect", "Electronic Arts", Topic.SCI_FI, Genre.RPG, GameSize.AAA, 0.78, 420, titles = listOf("Mass Effect 5", "Mass Effect 6")),
    // Ubisoft
    RealFranchise("Assassin's Creed", "Ubisoft", Topic.MEDIEVAL, Genre.ACTION, GameSize.AAA, 0.6, 104, titles = listOf("Assassin's Creed Hexe", "Assassin's Creed Invictus", "Assassin's Creed Jade", "Assassin's Creed Empire", "Assassin's Creed Dynasty")),
    RealFranchise("Far Cry", "Ubisoft", Topic.MILITARY, Genre.ACTION, GameSize.AAA, 0.55, 160, titles = listOf("Far Cry 7", "Far Cry 8", "Far Cry 9")),
    RealFranchise("Just Dance", "Ubisoft", Topic.MUSIC, Genre.CASUAL, GameSize.MEDIUM, 0.45, 52, annualWeek = 42, yearly = { "Just Dance ${it + 1}" }),
    // Activision Blizzard
    RealFranchise("Call of Duty", "Activision Blizzard", Topic.MILITARY, Genre.ACTION, GameSize.AAA, 0.6, 52, annualWeek = 44, titles = listOf(
        "Call of Duty: Modern Warfare 4", "Call of Duty: Black Ops 8", "Call of Duty: Vanguard 2", "Call of Duty: Modern Warfare 5",
        "Call of Duty: Black Ops 9", "Call of Duty: Ghosts 2", "Call of Duty: Advanced Warfare 2", "Call of Duty: Modern Warfare 6",
        "Call of Duty: Black Ops 10", "Call of Duty: World at War 2", "Call of Duty: Modern Warfare 7", "Call of Duty: Black Ops 11",
        "Call of Duty: Infinite Warfare 2", "Call of Duty: Modern Warfare 8", "Call of Duty: Black Ops 12",
    )),
    RealFranchise("Diablo", "Activision Blizzard", Topic.FANTASY, Genre.RPG, GameSize.AAA, 0.78, 300, titles = listOf("Diablo V", "Diablo VI")),
    RealFranchise("Overwatch", "Activision Blizzard", Topic.SCI_FI, Genre.ACTION, GameSize.LARGE, 0.68, 320, titles = listOf("Overwatch 3")),
    RealFranchise("World of Warcraft", "Activision Blizzard", Topic.FANTASY, Genre.RPG, GameSize.LARGE, 0.75, 104, titles = listOf(
        "World of Warcraft: Midnight", "World of Warcraft: The Last Titan", "World of Warcraft: Eclipse", "World of Warcraft: Dragonfall",
        "World of Warcraft: Shadows of Azeroth", "World of Warcraft: Tides of War",
    )),
    // Rockstar
    RealFranchise("Grand Theft Auto", "Rockstar Games", Topic.CITY, Genre.ACTION, GameSize.AAA, 0.98, 560, titles = listOf("Grand Theft Auto VII", "Grand Theft Auto VIII")),
    RealFranchise("Red Dead Redemption", "Rockstar Games", Topic.WILD_WEST, Genre.ADVENTURE, GameSize.AAA, 0.96, 480, titles = listOf("Red Dead Redemption 3")),
    // Nintendo
    RealFranchise("Super Mario", "Nintendo", Topic.FANTASY, Genre.ACTION, GameSize.LARGE, 0.9, 156, titles = listOf("Super Mario Odyssey 2", "Super Mario Galaxy 3", "Super Mario Bros. Wonder 2", "Super Mario Universe")),
    RealFranchise("The Legend of Zelda", "Nintendo", Topic.FANTASY, Genre.ADVENTURE, GameSize.AAA, 0.96, 300, titles = listOf("The Legend of Zelda: Echoes of Hyrule", "The Legend of Zelda: Skyward Crown", "The Legend of Zelda: Twilight Reborn")),
    RealFranchise("Pokémon", "Nintendo", Topic.PETS, Genre.RPG, GameSize.LARGE, 0.58, 104, titles = listOf(
        "Pokémon Wind and Waves", "Pokémon Legends: Johto", "Pokémon Crystal Dawn", "Pokémon Legends: Unova", "Pokémon Solar and Lunar", "Pokémon Legends: Galar",
    )),
    RealFranchise("Mario Kart", "Nintendo", Topic.RACING, Genre.CASUAL, GameSize.LARGE, 0.86, 400, titles = listOf("Mario Kart 10", "Mario Kart 11")),
    RealFranchise("Animal Crossing", "Nintendo", Topic.FARMING, Genre.SIMULATION, GameSize.LARGE, 0.86, 400, titles = listOf("Animal Crossing: New Shores", "Animal Crossing: Happy Harbor")),
    RealFranchise("Super Smash Bros.", "Nintendo", Topic.SUPERHEROES, Genre.ACTION, GameSize.LARGE, 0.9, 420, titles = listOf("Super Smash Bros. Infinity")),
    // Sony
    RealFranchise("God of War", "PlayStation Studios", Topic.MYTHOLOGY, Genre.ACTION, GameSize.AAA, 0.93, 300, titles = listOf("God of War: Odin's Reckoning", "God of War: Dawn of Gods")),
    RealFranchise("The Last of Us", "PlayStation Studios", Topic.ZOMBIES, Genre.ADVENTURE, GameSize.AAA, 0.94, 420, titles = listOf("The Last of Us Part III")),
    RealFranchise("Marvel's Spider-Man", "PlayStation Studios", Topic.SUPERHEROES, Genre.ACTION, GameSize.AAA, 0.86, 260, titles = listOf("Marvel's Spider-Man 3", "Marvel's Spider-Man 4")),
    RealFranchise("Horizon", "PlayStation Studios", Topic.ROBOTS, Genre.RPG, GameSize.AAA, 0.82, 300, titles = listOf("Horizon III", "Horizon IV")),
    RealFranchise("Gran Turismo", "PlayStation Studios", Topic.RACING, Genre.SIMULATION, GameSize.LARGE, 0.8, 300, titles = listOf("Gran Turismo 8", "Gran Turismo 9")),
    // Microsoft
    RealFranchise("Halo", "Xbox Game Studios", Topic.SCI_FI, Genre.ACTION, GameSize.AAA, 0.76, 300, titles = listOf("Halo 7", "Halo 8")),
    RealFranchise("Forza Horizon", "Xbox Game Studios", Topic.RACING, Genre.ACTION, GameSize.AAA, 0.88, 200, titles = listOf("Forza Horizon 7", "Forza Horizon 8", "Forza Horizon 9")),
    RealFranchise("Gears of War", "Xbox Game Studios", Topic.MILITARY, Genre.ACTION, GameSize.AAA, 0.8, 300, titles = listOf("Gears of War: E-Day", "Gears 7")),
    RealFranchise("The Elder Scrolls", "Xbox Game Studios", Topic.FANTASY, Genre.RPG, GameSize.AAA, 0.9, 700, titles = listOf("The Elder Scrolls VI", "The Elder Scrolls VII")),
    RealFranchise("Fallout", "Xbox Game Studios", Topic.POST_APOCALYPTIC, Genre.RPG, GameSize.AAA, 0.82, 420, titles = listOf("Fallout 5", "Fallout: New Vegas 2")),
    // Capcom
    RealFranchise("Resident Evil", "Capcom", Topic.ZOMBIES, Genre.ACTION, GameSize.AAA, 0.86, 104, titles = listOf(
        "Resident Evil Code: Veronica", "Resident Evil 10", "Resident Evil Zero", "Resident Evil 11", "Resident Evil: Outbreak", "Resident Evil 12",
    )),
    RealFranchise("Monster Hunter", "Capcom", Topic.DINOSAURS, Genre.RPG, GameSize.AAA, 0.82, 260, titles = listOf("Monster Hunter Horizons", "Monster Hunter 7")),
    RealFranchise("Street Fighter", "Capcom", Topic.NINJA, Genre.ACTION, GameSize.LARGE, 0.82, 400, titles = listOf("Street Fighter 7")),
    // FromSoftware
    RealFranchise("Elden Ring", "FromSoftware", Topic.FANTASY, Genre.RPG, GameSize.AAA, 0.95, 320, titles = listOf("Elden Ring II", "Elden Ring III")),
    RealFranchise("Armored Core", "FromSoftware", Topic.ROBOTS, Genre.ACTION, GameSize.LARGE, 0.8, 420, titles = listOf("Armored Core VII")),
    // CD Projekt Red
    RealFranchise("The Witcher", "CD Projekt Red", Topic.FANTASY, Genre.RPG, GameSize.AAA, 0.94, 480, titles = listOf("The Witcher 4", "The Witcher 5")),
    RealFranchise("Cyberpunk", "CD Projekt Red", Topic.CYBERPUNK, Genre.RPG, GameSize.AAA, 0.86, 520, titles = listOf("Cyberpunk 2")),
    // Square Enix
    RealFranchise("Final Fantasy", "Square Enix", Topic.FANTASY, Genre.RPG, GameSize.AAA, 0.8, 160, titles = listOf(
        "Final Fantasy VII Remake Part 3", "Final Fantasy XVII", "Final Fantasy IX Remake", "Final Fantasy XVIII",
    )),
    RealFranchise("Kingdom Hearts", "Square Enix", Topic.FANTASY, Genre.ACTION, GameSize.AAA, 0.74, 400, titles = listOf("Kingdom Hearts IV")),
    RealFranchise("Dragon Quest", "Square Enix", Topic.FANTASY, Genre.RPG, GameSize.LARGE, 0.78, 300, titles = listOf("Dragon Quest XII", "Dragon Quest XIII")),
    // Epic, Bandai Namco, Supercell, Valve
    RealFranchise("Fortnite", "Epic Games", Topic.SUPERHEROES, Genre.ACTION, GameSize.LARGE, 0.6, 52, annualWeek = 49, yearly = { "Fortnite Chapter ${it - 2019}" }),
    RealFranchise("Rocket League", "Epic Games", Topic.SPORTS, Genre.ACTION, GameSize.MEDIUM, 0.75, 400, titles = listOf("Rocket League 2")),
    RealFranchise("Tekken", "Bandai Namco", Topic.NINJA, Genre.ACTION, GameSize.LARGE, 0.76, 400, titles = listOf("Tekken 9")),
    RealFranchise("Dragon Ball", "Bandai Namco", Topic.SUPERHEROES, Genre.ACTION, GameSize.LARGE, 0.62, 156, titles = listOf("Dragon Ball Xenoverse 3", "Dragon Ball: Sparking! Two", "Dragon Ball FighterZ 2")),
    RealFranchise("Clash", "Supercell", Topic.MEDIEVAL, Genre.STRATEGY, GameSize.MEDIUM, 0.62, 156, titles = listOf("Clash of Clans 2", "Clash Royale 2", "Clash Heroes")),
    RealFranchise("Half-Life", "Valve", Topic.SCI_FI, Genre.ACTION, GameSize.AAA, 0.97, 900, titles = listOf("Half-Life 3")),
)

/** A release a rival has announced, with its date. */
@Serializable
data class Announcement(
    val title: String,
    val studio: String,
    val franchise: String,
    val week: Int,
    val topic: Topic,
    val genre: Genre,
    val size: GameSize,
    /** The kind of release everyone stops to play. */
    val mega: Boolean,
)

internal val RealFranchise.mega: Boolean get() = size == GameSize.AAA && quality >= 0.9

/** Spread the first entries out over the first years of the run. */
internal fun GameState.seedFranchises(m: GameMarket) {
    for (f in RealFranchises) {
        val first = when {
            f.annualWeek != null -> f.annualWeek - 1
            f.name == "Half-Life" -> rng.int(300, 900)
            else -> rng.int(8, minOf(f.cadence, 260))
        }
        m.franchiseNext[f.name] = first
    }
}

/** Announce what's coming and release what's due. */
internal fun GameState.franchiseWeek(m: GameMarket) {
    if (m.franchiseNext.isEmpty()) seedFranchises(m)
    for (f in RealFranchises) {
        val next = m.franchiseNext[f.name] ?: continue
        val entry = m.franchiseEntries[f.name] ?: 0
        val lead = if (f.size == GameSize.AAA) 26 else 12
        if (next - week in 0..lead && m.upcoming.none { it.franchise == f.name }) {
            val a = Announcement(f.title(entry, next), f.studio, f.name, next, f.topic, f.genre, f.size, f.mega)
            m.upcoming += a
            if (f.mega || f.size == GameSize.AAA) {
                addLog("${f.studio} announced ${a.title}, out ${GameClock.label(a.week)}.", LogTone.NEUTRAL, Lane.GAMES)
            }
        }
        if (week < next) continue
        val a = m.upcoming.firstOrNull { it.franchise == f.name }
        m.upcoming.removeAll { it.franchise == f.name }
        val title = a?.title ?: f.title(entry, week)
        val score = (rng.normal(5.0 + 4.6 * f.quality, 0.6) + (entry % 3 - 1) * 0.1).clamped(3.0, 9.9)
        val power = 1 + 2 * f.quality
        val sales = f.size.launchSales * (score / 7).pow(3) * power * rng.range(0.85, 1.15) * world.era.demand(Lane.GAMES)
        m.games += RivalGame(m.nextId++, title, f.studio, f.topic, f.genre, f.size, score, week, sales, franchise = f.name)
        m.franchiseEntries[f.name] = entry + 1
        val ours = games?.owns(f.studio) == true
        if (score >= 9 && f.size == GameSize.AAA && !ours) {
            // A giant launch pulls players away from whatever else just came out, yours included.
            val fresh = games?.releases?.filter { it.weeklySales > 0 && week - it.week in 0..5 }.orEmpty()
            fresh.forEach { it.weeklySales *= 0.8 }
            fresh.firstOrNull()?.let { addLog("${it.title} lost players to $title this week.", LogTone.BAD, Lane.GAMES) }
        }
        val s = String.format(java.util.Locale.US, "%.1f", score)
        addLog(
            when {
                ours -> "Your studio ${f.studio} released $title: $s/10."
                score >= 9 && f.size == GameSize.AAA -> "$title is out, and it's everywhere: $s/10."
                else -> "${f.studio} released $title: $s/10."
            },
            if (score >= 9 || ours && score >= 8) LogTone.MILESTONE else LogTone.NEUTRAL,
            Lane.GAMES,
        )
        m.franchiseNext[f.name] = if (f.annualWeek != null) {
            week + 52
        } else {
            week + (f.cadence * rng.range(0.85, 1.25)).toInt()
        }
    }
    m.upcoming.removeAll { it.week < week }
    m.upcoming.sortBy { it.week }
}
