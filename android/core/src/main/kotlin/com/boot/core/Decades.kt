package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.pow

/*
 * Decades. A run can start today, or at the start of one of tech's revolutions (home computers in
 * 1977, the console wars in 1985, the web in 1995, the smartphone in 2007) and play through to the
 * present. The calendar shows real dates; history's eras arrive when they did; businesses open up
 * when they became possible; the consoles, the studios, the big series, the review outlets, your
 * rivals and the biggest companies are the ones of the day. Once the present is reached the world
 * is the same as a run started today, and the eras after that are anyone's guess.
 */

/** The year a run started today begins in. Week 0 of a modern run is this year. */
const val MODERN_YEAR = 2027

/** Where a run can start. */
@Serializable
enum class StartPoint(val year: Int?, val title: String, val blurb: String) {
    TODAY(null, "Today", "The world as it is: consoles, phones, social networks and AI labs. Fifteen years to make your mark."),
    HOME_COMPUTERS(1977, "1977: Home computers", "The Apple II has just come out. Computers are coming home, and so are games. Play through to today."),
    CONSOLE_WARS(1985, "1985: The console wars", "Games crashed, and a console from Kyoto is bringing them back. Play through to today."),
    THE_WEB(1995, "1995: The web", "Everyone wants a website and investors are throwing money at anything with .com in the name. Play through to today."),
    SMARTPHONES(2007, "2007: The smartphone", "A phone with no keyboard is about to change everything. Play through to today."),
}

/** The era a start point opens in, or null for today. */
val StartPoint.era: Era? get() = year?.let { historicalEra(it)?.first }

/** How long a run from here lasts, in years. */
val StartPoint.years: Int get() = year?.let { MODERN_YEAR - it } ?: GameState.LENGTH_WEEKS / 52

/** Can [lane] be started at this point in history? */
fun StartPoint.laneOpen(lane: Lane): Boolean = (year ?: MODERN_YEAR) >= lane.arrives

/** The year a business first became possible. */
val Lane.arrives: Int
    get() = when (this) {
        Lane.GAMES -> 1970
        Lane.HARDWARE -> 1975
        Lane.OS -> 1981
        Lane.WEBSITES -> 1994
        Lane.SOCIAL -> 2004
        Lane.APPS -> 2008
        Lane.AI -> 2012
    }

/** What makes a business possible, and when. */
val Lane.arrival: String
    get() = when (this) {
        Lane.GAMES -> "Arcades and the first consoles, 1970."
        Lane.HARDWARE -> "The first home computer kits, 1975."
        Lane.OS -> "Personal computers need platforms, 1981."
        Lane.WEBSITES -> "The web goes public in 1994."
        Lane.SOCIAL -> "Social networks take off in 2004."
        Lane.APPS -> "The App Store opens in 2008."
        Lane.AI -> "Deep learning takes off in 2012."
    }

/** The industry a lane is in, for headlines. */
internal val Lane.industry: String
    get() = when (this) {
        Lane.GAMES -> "games"
        Lane.WEBSITES -> "the web"
        Lane.APPS -> "apps"
        Lane.SOCIAL -> "social media"
        Lane.HARDWARE -> "computers"
        Lane.AI -> "AI"
        Lane.OS -> "operating systems"
    }

/** Research only this era offers. */
val Era.research: List<Research> get() = Research.entries.filter { it.era == this }

/** Tell the player what this era lets them research, while it lasts. */
internal fun GameState.announceEraResearch() {
    val items = world.era.research.filter { it !in researched }
    if (items.isEmpty()) return
    addLog("New research, only while ${world.era.inSentence} lasts: ${items.joinToString(" and ") { it.inSentence }}.", LogTone.MILESTONE)
}

/** A research item's name mid-sentence: "copy protection", but "AI copilots" and "fire-sale GPUs" keep their capitals. */
val Research.inSentence: String
    get() = when {
        title.startsWith("A ") || title.startsWith("An ") -> title.replaceFirstChar { it.lowercase() }
        title.length > 1 && title[1].isLowerCase() -> title.replaceFirstChar { it.lowercase() }
        else -> title
    }

/** An era's name mid-sentence: "the home computer revolution", "Web 2.0". */
val Era.inSentence: String get() = if (title.startsWith("The ")) "the " + title.removePrefix("The ").lowercase() else title

/** What the big summer games show was called in [year]. */
internal fun gamesShowIn(year: Int): String = when {
    year < 1995 -> "the Consumer Electronics Show"
    year < 2020 -> "E3"
    else -> "Summer Game Fest"
}

/** Is this a run that started in the past? */
val GameState.isDecades: Boolean get() = startYear != null

/** The calendar year this week falls in. A modern run starts in [MODERN_YEAR]. */
val GameState.calendarYear: Int get() = (startYear ?: MODERN_YEAR) + week / 52

/** This week on the shared timeline platforms and real series run on, where week 0 is the start of [MODERN_YEAR]. */
val GameState.timelineWeek: Int get() = week + ((startYear ?: MODERN_YEAR) - MODERN_YEAR) * 52

/** Weeks until the run ends on its own: fifteen years, or until the present for a run started in the past. */
val GameState.runLength: Int get() = startYear?.let { (MODERN_YEAR - it) * 52 } ?: GameState.LENGTH_WEEKS

/** Can this business be started yet? */
fun GameState.laneOpen(lane: Lane): Boolean = calendarYear >= lane.arrives

/** Why [lane] can't be started yet, or null if it can. */
fun GameState.laneClosed(lane: Lane): String? =
    if (laneOpen(lane)) null else "${lane.title} isn't a business yet. ${lane.arrival}"

// The eras, as they happened.

/** History's eras and the years they began, for runs that start in the past. They end at [MODERN_YEAR]. */
internal val History = listOf(
    1977 to Era.HOME_COMPUTERS,
    1983 to Era.VIDEO_GAME_CRASH,
    1985 to Era.CONSOLE_WARS,
    1990 to Era.MULTIMEDIA,
    1995 to Era.DOT_COM_BOOM,
    2000 to Era.DOT_COM_BUST,
    2004 to Era.WEB_2_0,
    2007 to Era.SMARTPHONES,
    2012 to Era.FREE_TO_PLAY,
    2016 to Era.VR_HYPE_CYCLE,
    2018 to Era.CREATOR_ECONOMY,
    2020 to Era.REMOTE_WORK_BOOM,
    2022 to Era.CRYPTO_WINTER,
    2023 to Era.AI_GOLD_RUSH,
)

/** In a run set in the past, the eras history still has in store, with the years they begin. */
val GameState.historyAhead: List<Pair<Int, Era>>
    get() = if (!isDecades) emptyList() else History.filter { it.first > calendarYear && it.second != world.era }

/** The historical era in [year] and the year it ends, or null from the present on. */
internal fun historicalEra(year: Int): Pair<Era, Int>? {
    if (year >= MODERN_YEAR) return null
    val i = History.indexOfLast { it.first <= year }.coerceAtLeast(0)
    val end = History.getOrNull(i + 1)?.first ?: MODERN_YEAR
    return History[i].second to end
}

/** Set the world's era for a run starting in [year]. */
internal fun World.startAt(year: Int) {
    val (era, end) = historicalEra(year) ?: return
    val begin = History.first { it.second == era }.first
    this.era = era
    eraStartWeek = (begin - year) * 52
    eraLength = (end - begin) * 52
    pastEras += History.takeWhile { it.first < begin }.map { it.second }
    history += History.takeWhile { it.first < begin }.mapIndexed { i, (y, e) ->
        EraSpan(e, (y - year) * 52, ((History.getOrNull(i + 1)?.first ?: begin) - year) * 52)
    }
}

/**
 * In a run that started in the past, the era history has next: the one that begins when the current
 * one ends (or, with [now], the one this week is in) and the year it ends. Null from the present on.
 */
internal fun GameState.historicalEraAfter(now: Boolean = false): Pair<Era, Int>? {
    val start = startYear ?: return null
    val at = if (now) week else world.eraStartWeek + world.eraLength
    return historicalEra(start + at.floorDiv(52))
}

/** Give the world the rivals of the start year. */
internal fun GameState.adoptHistory() {
    for (r in world.rivals) r.name = rivalIn(r.lane, calendarYear)
}

/** As the years pass, the giants change: a lane's main rival hands over to the next one. */
internal fun GameState.updateHistoricRivals() {
    if (!isDecades || week % 52 != 0) return
    for (r in world.rivals) {
        val now = rivalIn(r.lane, calendarYear)
        if (now == r.name) continue
        val was = r.name
        r.name = now
        r.strength = r.strength.coerceAtLeast(0.5)
        if (r.lane in lanes) {
            publish(NewsDesk.INDUSTRY, techOutlet(r.lane, rng), "$now is the new giant of ${r.lane.industry}", "$was used to be the one to beat. Not any more.", lane = r.lane)
            addLog("$now has taken over from $was as the biggest name in ${r.lane.industry}.", LogTone.NEUTRAL, r.lane)
        }
    }
}

// Who existed when.

/** The year each real series debuted. */
internal val FranchiseDebut = mapOf(
    "EA Sports FC" to 1993, "Madden NFL" to 1988, "Battlefield" to 2002, "The Sims" to 2000, "Need for Speed" to 1994,
    "Mass Effect" to 2007, "Assassin's Creed" to 2007, "Far Cry" to 2004, "Just Dance" to 2009, "Call of Duty" to 2003,
    "Diablo" to 1996, "Overwatch" to 2016, "World of Warcraft" to 2004, "Grand Theft Auto" to 1997, "Red Dead Redemption" to 2004,
    "Super Mario" to 1985, "The Legend of Zelda" to 1986, "Pokémon" to 1996, "Mario Kart" to 1992, "Animal Crossing" to 2001,
    "Super Smash Bros." to 1999, "God of War" to 2005, "The Last of Us" to 2013, "Marvel's Spider-Man" to 2018, "Horizon" to 2017,
    "Gran Turismo" to 1997, "Halo" to 2001, "Forza Horizon" to 2012, "Gears of War" to 2006, "The Elder Scrolls" to 1994,
    "Fallout" to 1997, "Resident Evil" to 1996, "Monster Hunter" to 2004, "Street Fighter" to 1987, "Elden Ring" to 2022,
    "Armored Core" to 1997, "The Witcher" to 2007, "Cyberpunk" to 2020, "Final Fantasy" to 1987, "Kingdom Hearts" to 2002,
    "Dragon Quest" to 1986, "Fortnite" to 2017, "Rocket League" to 2015, "Tekken" to 1994, "Dragon Ball" to 1986,
    "Clash" to 2012, "Half-Life" to 1998,
)

/** What a series was called in [year], when it was called something else. */
internal fun seriesNameIn(name: String, year: Int): String = when {
    name == "EA Sports FC" && year < 2023 -> "FIFA"
    else -> name
}

/** The year each studio opened, and the year it closed if it did. */
internal val StudioYears = mapOf(
    "Electronic Arts" to 1982, "Ubisoft" to 1986, "Activision Blizzard" to 1979, "Rockstar Games" to 1998, "Nintendo" to 1889,
    "Capcom" to 1979, "FromSoftware" to 1986, "CD Projekt Red" to 2002, "Square Enix" to 1986, "Epic Games" to 1991,
    "Bandai Namco" to 1955, "Supercell" to 2010, "PlayStation Studios" to 1993, "Xbox Game Studios" to 2000, "Valve" to 1996,
    "ConcernedApe" to 2011, "Team Cherry" to 2014, "Mega Crit" to 2013, "Playdead" to 2006, "Innersloth" to 2015,
    "Supergiant Games" to 2009, "Thatgamecompany" to 2006, "Hello Games" to 2008, "Re-Logic" to 2011, "Klei Entertainment" to 2005,
    "Annapurna Interactive" to 2016, "Devolver Digital" to 2009, "Remedy Entertainment" to 1995, "Larian Studios" to 1996,
    "Atari" to 1972, "Sierra On-Line" to 1979, "Infocom" to 1979, "Origin Systems" to 1983, "Westwood Studios" to 1985,
    "LucasArts" to 1982, "Sega" to 1960, "Konami" to 1969, "id Software" to 1991,
)

/** Studios that closed. */
internal val StudioClosed = mapOf(
    "Atari" to 2013, "Sierra On-Line" to 2008, "Infocom" to 1989, "Origin Systems" to 2004, "Westwood Studios" to 2003, "LucasArts" to 2013,
)

/** Was [studio] making games in [year]? */
fun studioActive(studio: String, year: Int): Boolean =
    year >= (StudioYears[studio] ?: 0) && year < (StudioClosed[studio] ?: Int.MAX_VALUE)

/** Each lane's main rival through the years. The last one is today's. */
internal val RivalsThroughTime = mapOf(
    Lane.GAMES to listOf(1972 to "Atari", 1985 to "Nintendo", 2000 to "Electronic Arts"),
    Lane.WEBSITES to listOf(1994 to "Razorfish", 2004 to "Squarespace"),
    Lane.APPS to listOf(2008 to "Google"),
    Lane.SOCIAL to listOf(2003 to "MySpace", 2009 to "Facebook", 2021 to "Meta"),
    Lane.HARDWARE to listOf(1975 to "Commodore", 1986 to "IBM", 1996 to "Dell", 2007 to "Apple"),
    Lane.AI to listOf(2012 to "Google DeepMind", 2020 to "OpenAI"),
    Lane.OS to listOf(1981 to "Digital Research", 1985 to "Microsoft", 2007 to "Apple"),
)

/** [lane]'s main rival in [year]. */
fun rivalIn(lane: Lane, year: Int): String =
    RivalsThroughTime.getValue(lane).lastOrNull { it.first <= year }?.second ?: RivalsThroughTime.getValue(lane).first().second

/** When the big names in each lane were founded, for the news. */
internal val PeerYears = mapOf(
    "Electronic Arts" to 1982, "Ubisoft" to 1986, "Activision Blizzard" to 1979, "Take-Two" to 1993, "Nintendo" to 1889, "Sony Interactive" to 1993,
    "Squarespace" to 2003, "Wix" to 2006, "Webflow" to 2013, "WordPress.com" to 2005, "Shopify" to 2006,
    "Google" to 1998, "Duolingo" to 2011, "Spotify" to 2006, "Calm" to 2012, "Strava" to 2009, "Notion" to 2016,
    "Meta" to 2004, "TikTok" to 2016, "X" to 2006, "Snap" to 2011, "Reddit" to 2005, "Discord" to 2015,
    "Apple" to 1976, "Samsung" to 1969, "Sony" to 1946, "Nothing" to 2020, "Microsoft" to 1975,
    "OpenAI" to 2015, "Google DeepMind" to 2010, "Anthropic" to 2021, "Meta AI" to 2013, "xAI" to 2023, "Mistral AI" to 2023,
    "Canonical" to 2004, "Red Hat" to 1993, "Valve" to 1996, "Digital Research" to 1976,
)

/** The big names in [lane] that existed in [year]: today's, plus the giants of the past. */
internal fun peersIn(lane: Lane, year: Int): List<String> {
    // The giants of the past, with the years they mattered.
    val past = when (lane) {
        Lane.GAMES -> listOf(Triple("Atari", 1972, 1996), Triple("Sega", 1983, 2001), Triple("Activision", 1979, 2008), Triple("Sierra On-Line", 1979, 2004))
        Lane.WEBSITES -> listOf(Triple("Razorfish", 1995, 2009), Triple("Netscape", 1994, 2003), Triple("GeoCities", 1994, 2009), Triple("Yahoo", 1995, 2012))
        Lane.APPS -> listOf(Triple("Rovio", 2009, 2016), Triple("King", 2012, 2016), Triple("Zynga", 2009, 2014))
        Lane.SOCIAL -> listOf(Triple("MySpace", 2003, 2010), Triple("Friendster", 2002, 2008), Triple("Facebook", 2004, 2021))
        Lane.HARDWARE -> listOf(Triple("Commodore", 1977, 1994), Triple("IBM", 1977, 2005), Triple("Compaq", 1982, 2002), Triple("Dell", 1984, 2015), Triple("Palm", 1996, 2010), Triple("Nokia", 1992, 2013))
        Lane.AI -> listOf(Triple("IBM Watson", 2011, 2020))
        Lane.OS -> listOf(Triple("Digital Research", 1976, 1991), Triple("BeOS", 1991, 2001), Triple("Palm OS", 1996, 2010), Triple("Symbian", 1998, 2012))
    }
    val today = World.peers(lane).filter { (PeerYears[it] ?: 0) <= year && (lane != Lane.SOCIAL || it != "Meta" || year >= 2021) }
    return (today + past.filter { year in it.second until it.third }.map { it.first }).ifEmpty { listOf(rivalIn(lane, year)) }
}

/** When news and review outlets started. Ones not listed have always been around. */
internal val OutletYears = mapOf(
    "TechCrunch" to 2005, "The Verge" to 2011, "Wired" to 1993, "Kotaku" to 2004, "Polygon" to 2012, "Eurogamer" to 1999,
    "PC Gamer" to 1993, "IGN" to 1996, "GameSpot" to 1996, "Smashing Magazine" to 2006, "Fast Company" to 1995, "9to5Mac" to 2007,
    "Platformer" to 2020, "The Information" to 2013, "Engadget" to 2004, "Ars Technica" to 1998, "Dexerto" to 2016,
    "ESPN Esports" to 2016, "The Esports Observer" to 2015, "CNBC" to 1989, "Game Informer" to 1991, "Edge" to 1993,
)

/** Outlets that closed. */
internal val OutletClosed = mapOf("Byte" to 1998, "Computer Gaming World" to 2006, "Nintendo Power" to 2012, "Electronic Games" to 1985)

/** [outlet] if it existed in [year], or one of the day's that would have run the same story. */
internal fun outletIn(outlet: String, year: Int): String {
    if (year >= (OutletYears[outlet] ?: 0)) return outlet
    val games = outlet in listOf("IGN", "Kotaku", "Polygon", "Eurogamer", "PC Gamer", "GameSpot", "Dexerto", "ESPN Esports", "The Esports Observer", "Game Informer", "Edge")
    val pool = if (games) {
        listOf("Electronic Games" to (1981 to 1985), "Computer Gaming World" to (1981 to 2006), "Nintendo Power" to (1988 to 2012), "Electronic Gaming Monthly" to (1989 to 9999))
    } else {
        listOf("Byte" to (1975 to 1998), "InfoWorld" to (1978 to 9999), "Computerworld" to (1967 to 9999), "The New York Times" to (0 to 9999))
    }
    val open = pool.filter { (_, span) -> year >= span.first && year < span.second }.map { it.first }
    return if (open.isEmpty()) "The New York Times" else open[year % open.size]
}

/** The biggest rival game a studio would make in [year]: no AAA blockbusters before the 2000s. */
internal fun biggestRivalGame(year: Int): GameSize = when {
    year < 1990 -> GameSize.SMALL
    year < 1998 -> GameSize.MEDIUM
    year < 2005 -> GameSize.LARGE
    else -> GameSize.AAA
}

/** Companies of the past for the league table, with their value at a few points in time. */
internal val HistoricCompanies = listOf(
    Triple("IBM", Lane.HARDWARE, listOf(1977 to 3e10, 1987 to 1e11, 1993 to 3e10, 2000 to 2e11, 2026 to 2.2e11)),
    Triple("Atari", Lane.GAMES, listOf(1977 to 3e8, 1982 to 2e9, 1985 to 2e8, 1996 to 1e8)),
    Triple("Commodore", Lane.HARDWARE, listOf(1977 to 1e8, 1983 to 1.5e9, 1994 to 5e7)),
    Triple("AOL", Lane.WEBSITES, listOf(1992 to 1e8, 1999 to 1.6e11, 2002 to 3e10, 2015 to 4e9)),
    Triple("Yahoo", Lane.WEBSITES, listOf(1996 to 8e8, 2000 to 1.2e11, 2002 to 1e10, 2017 to 5e10)),
    Triple("Nokia", Lane.HARDWARE, listOf(1990 to 3e9, 2000 to 2.5e11, 2007 to 1.5e11, 2013 to 1.5e10, 2026 to 3e10)),
    Triple("BlackBerry", Lane.HARDWARE, listOf(1999 to 1e9, 2008 to 8e10, 2013 to 5e9, 2026 to 3e9)),
)

/** A historic company's value in [year], between its known points, or null if it wasn't around. */
internal fun historicValue(points: List<Pair<Int, Double>>, year: Double): Double? {
    if (year < points.first().first || year > points.last().first) return null
    val i = points.indexOfLast { it.first <= year }.coerceIn(0, points.size - 2)
    val (y0, v0) = points[i]
    val (y1, v1) = points[i + 1]
    val t = ((year - y0) / (y1 - y0)).coerceIn(0.0, 1.0)
    return exp(ln(v0) + (ln(v1) - ln(v0)) * t)
}

/** The year [week] of this run falls in. */
internal fun GameState.yearAt(week: Int): Int = (startYear ?: MODERN_YEAR) + week.floorDiv(52)

/** The real entries of the big series before today, with their years, for runs set in the past. */
internal val SeriesHistory: Map<String, List<Pair<Int, String>>> = mapOf(
    "Super Mario" to listOf(
        1985 to "Super Mario Bros.", 1988 to "Super Mario Bros. 2", 1990 to "Super Mario Bros. 3", 1991 to "Super Mario World",
        1996 to "Super Mario 64", 2002 to "Super Mario Sunshine", 2007 to "Super Mario Galaxy", 2010 to "Super Mario Galaxy 2",
        2013 to "Super Mario 3D World", 2017 to "Super Mario Odyssey", 2023 to "Super Mario Bros. Wonder",
    ),
    "The Legend of Zelda" to listOf(
        1986 to "The Legend of Zelda", 1987 to "Zelda II: The Adventure of Link", 1991 to "The Legend of Zelda: A Link to the Past",
        1993 to "The Legend of Zelda: Link's Awakening", 1998 to "The Legend of Zelda: Ocarina of Time", 2000 to "The Legend of Zelda: Majora's Mask",
        2002 to "The Legend of Zelda: The Wind Waker", 2006 to "The Legend of Zelda: Twilight Princess", 2011 to "The Legend of Zelda: Skyward Sword",
        2017 to "The Legend of Zelda: Breath of the Wild", 2023 to "The Legend of Zelda: Tears of the Kingdom",
    ),
    "Grand Theft Auto" to listOf(
        1997 to "Grand Theft Auto", 1999 to "Grand Theft Auto 2", 2001 to "Grand Theft Auto III", 2002 to "Grand Theft Auto: Vice City",
        2004 to "Grand Theft Auto: San Andreas", 2008 to "Grand Theft Auto IV", 2013 to "Grand Theft Auto V", 2026 to "Grand Theft Auto VI",
    ),
    "Halo" to listOf(
        2001 to "Halo: Combat Evolved", 2004 to "Halo 2", 2007 to "Halo 3", 2010 to "Halo: Reach", 2012 to "Halo 4",
        2015 to "Halo 5: Guardians", 2021 to "Halo Infinite",
    ),
    "Call of Duty" to listOf(
        2003 to "Call of Duty", 2005 to "Call of Duty 2", 2006 to "Call of Duty 3", 2007 to "Call of Duty 4: Modern Warfare",
        2008 to "Call of Duty: World at War", 2009 to "Call of Duty: Modern Warfare 2", 2010 to "Call of Duty: Black Ops",
        2011 to "Call of Duty: Modern Warfare 3", 2012 to "Call of Duty: Black Ops II", 2013 to "Call of Duty: Ghosts",
        2014 to "Call of Duty: Advanced Warfare", 2015 to "Call of Duty: Black Ops III", 2016 to "Call of Duty: Infinite Warfare",
        2017 to "Call of Duty: WWII", 2018 to "Call of Duty: Black Ops 4", 2019 to "Call of Duty: Modern Warfare",
        2020 to "Call of Duty: Black Ops Cold War", 2021 to "Call of Duty: Vanguard", 2022 to "Call of Duty: Modern Warfare II",
        2023 to "Call of Duty: Modern Warfare III", 2024 to "Call of Duty: Black Ops 6", 2025 to "Call of Duty: Black Ops 7",
    ),
    "Final Fantasy" to listOf(
        1987 to "Final Fantasy", 1988 to "Final Fantasy II", 1990 to "Final Fantasy III", 1991 to "Final Fantasy IV", 1992 to "Final Fantasy V",
        1994 to "Final Fantasy VI", 1997 to "Final Fantasy VII", 1999 to "Final Fantasy VIII", 2000 to "Final Fantasy IX", 2001 to "Final Fantasy X",
        2002 to "Final Fantasy XI", 2006 to "Final Fantasy XII", 2009 to "Final Fantasy XIII", 2010 to "Final Fantasy XIV", 2016 to "Final Fantasy XV",
        2020 to "Final Fantasy VII Remake", 2023 to "Final Fantasy XVI", 2024 to "Final Fantasy VII Rebirth",
    ),
    "Pokémon" to listOf(
        1996 to "Pokémon Red and Blue", 1999 to "Pokémon Gold and Silver", 2002 to "Pokémon Ruby and Sapphire", 2006 to "Pokémon Diamond and Pearl",
        2010 to "Pokémon Black and White", 2013 to "Pokémon X and Y", 2016 to "Pokémon Sun and Moon", 2019 to "Pokémon Sword and Shield",
        2022 to "Pokémon Scarlet and Violet", 2025 to "Pokémon Legends: Z-A",
    ),
    "Mario Kart" to listOf(
        1992 to "Super Mario Kart", 1996 to "Mario Kart 64", 2001 to "Mario Kart: Super Circuit", 2003 to "Mario Kart: Double Dash!!",
        2005 to "Mario Kart DS", 2008 to "Mario Kart Wii", 2011 to "Mario Kart 7", 2014 to "Mario Kart 8", 2025 to "Mario Kart World",
    ),
    "The Elder Scrolls" to listOf(
        1994 to "The Elder Scrolls: Arena", 1996 to "The Elder Scrolls II: Daggerfall", 2002 to "The Elder Scrolls III: Morrowind",
        2006 to "The Elder Scrolls IV: Oblivion", 2011 to "The Elder Scrolls V: Skyrim",
    ),
    "Fallout" to listOf(1997 to "Fallout", 1998 to "Fallout 2", 2008 to "Fallout 3", 2010 to "Fallout: New Vegas", 2015 to "Fallout 4", 2018 to "Fallout 76"),
    "Resident Evil" to listOf(
        1996 to "Resident Evil", 1998 to "Resident Evil 2", 1999 to "Resident Evil 3: Nemesis", 2005 to "Resident Evil 4", 2009 to "Resident Evil 5",
        2012 to "Resident Evil 6", 2017 to "Resident Evil 7: Biohazard", 2021 to "Resident Evil Village", 2026 to "Resident Evil Requiem",
    ),
    "Half-Life" to listOf(1998 to "Half-Life", 2004 to "Half-Life 2", 2020 to "Half-Life: Alyx"),
    "Diablo" to listOf(1996 to "Diablo", 2000 to "Diablo II", 2012 to "Diablo III", 2023 to "Diablo IV"),
    "Street Fighter" to listOf(
        1987 to "Street Fighter", 1991 to "Street Fighter II", 1995 to "Street Fighter Alpha", 1997 to "Street Fighter III",
        2008 to "Street Fighter IV", 2016 to "Street Fighter V", 2023 to "Street Fighter 6",
    ),
    "Gran Turismo" to listOf(
        1997 to "Gran Turismo", 1999 to "Gran Turismo 2", 2001 to "Gran Turismo 3: A-Spec", 2004 to "Gran Turismo 4", 2010 to "Gran Turismo 5",
        2013 to "Gran Turismo 6", 2017 to "Gran Turismo Sport", 2022 to "Gran Turismo 7",
    ),
    "The Sims" to listOf(2000 to "The Sims", 2004 to "The Sims 2", 2009 to "The Sims 3", 2014 to "The Sims 4"),
    "Battlefield" to listOf(
        2002 to "Battlefield 1942", 2005 to "Battlefield 2", 2011 to "Battlefield 3", 2013 to "Battlefield 4", 2016 to "Battlefield 1",
        2018 to "Battlefield V", 2021 to "Battlefield 2042", 2025 to "Battlefield 6",
    ),
    "Mass Effect" to listOf(2007 to "Mass Effect", 2010 to "Mass Effect 2", 2012 to "Mass Effect 3", 2017 to "Mass Effect: Andromeda"),
    "Assassin's Creed" to listOf(
        2007 to "Assassin's Creed", 2009 to "Assassin's Creed II", 2010 to "Assassin's Creed Brotherhood", 2012 to "Assassin's Creed III",
        2013 to "Assassin's Creed IV: Black Flag", 2014 to "Assassin's Creed Unity", 2017 to "Assassin's Creed Origins", 2018 to "Assassin's Creed Odyssey",
        2020 to "Assassin's Creed Valhalla", 2023 to "Assassin's Creed Mirage", 2025 to "Assassin's Creed Shadows",
    ),
    "God of War" to listOf(2005 to "God of War", 2007 to "God of War II", 2010 to "God of War III", 2018 to "God of War", 2022 to "God of War Ragnarök"),
    "The Witcher" to listOf(2007 to "The Witcher", 2011 to "The Witcher 2: Assassins of Kings", 2015 to "The Witcher 3: Wild Hunt"),
    "Gears of War" to listOf(2006 to "Gears of War", 2008 to "Gears of War 2", 2011 to "Gears of War 3", 2016 to "Gears of War 4", 2019 to "Gears 5"),
    "World of Warcraft" to listOf(
        2004 to "World of Warcraft", 2007 to "World of Warcraft: The Burning Crusade", 2008 to "World of Warcraft: Wrath of the Lich King",
        2010 to "World of Warcraft: Cataclysm", 2012 to "World of Warcraft: Mists of Pandaria", 2016 to "World of Warcraft: Legion",
        2020 to "World of Warcraft: Shadowlands", 2024 to "World of Warcraft: The War Within",
    ),
    "The Last of Us" to listOf(2013 to "The Last of Us", 2020 to "The Last of Us Part II"),
    "Red Dead Redemption" to listOf(2004 to "Red Dead Revolver", 2010 to "Red Dead Redemption", 2018 to "Red Dead Redemption 2"),
    "Super Smash Bros." to listOf(1999 to "Super Smash Bros.", 2001 to "Super Smash Bros. Melee", 2008 to "Super Smash Bros. Brawl", 2014 to "Super Smash Bros. for Wii U", 2018 to "Super Smash Bros. Ultimate"),
    "Animal Crossing" to listOf(2001 to "Animal Crossing", 2005 to "Animal Crossing: Wild World", 2008 to "Animal Crossing: City Folk", 2012 to "Animal Crossing: New Leaf", 2020 to "Animal Crossing: New Horizons"),
    "Overwatch" to listOf(2016 to "Overwatch", 2022 to "Overwatch 2"),
    "Elden Ring" to listOf(2022 to "Elden Ring"),
    "Cyberpunk" to listOf(2020 to "Cyberpunk 2077"),
    "Horizon" to listOf(2017 to "Horizon Zero Dawn", 2022 to "Horizon Forbidden West"),
    "Marvel's Spider-Man" to listOf(2018 to "Marvel's Spider-Man", 2020 to "Marvel's Spider-Man: Miles Morales", 2023 to "Marvel's Spider-Man 2"),
    "Monster Hunter" to listOf(2004 to "Monster Hunter", 2018 to "Monster Hunter: World", 2021 to "Monster Hunter Rise", 2025 to "Monster Hunter Wilds"),
    "Dragon Quest" to listOf(1986 to "Dragon Quest", 1987 to "Dragon Quest II", 1988 to "Dragon Quest III", 1990 to "Dragon Quest IV", 1992 to "Dragon Quest V", 1995 to "Dragon Quest VI", 2000 to "Dragon Quest VII", 2004 to "Dragon Quest VIII", 2009 to "Dragon Quest IX", 2017 to "Dragon Quest XI"),
    "Tekken" to listOf(1994 to "Tekken", 1995 to "Tekken 2", 1997 to "Tekken 3", 2001 to "Tekken 4", 2004 to "Tekken 5", 2007 to "Tekken 6", 2015 to "Tekken 7", 2024 to "Tekken 8"),
    "Kingdom Hearts" to listOf(2002 to "Kingdom Hearts", 2005 to "Kingdom Hearts II", 2019 to "Kingdom Hearts III"),
    "Need for Speed" to listOf(1994 to "The Need for Speed", 2002 to "Need for Speed: Hot Pursuit 2", 2003 to "Need for Speed: Underground", 2005 to "Need for Speed: Most Wanted", 2010 to "Need for Speed: Hot Pursuit", 2015 to "Need for Speed", 2022 to "Need for Speed Unbound"),
    "Far Cry" to listOf(2004 to "Far Cry", 2008 to "Far Cry 2", 2012 to "Far Cry 3", 2014 to "Far Cry 4", 2018 to "Far Cry 5", 2021 to "Far Cry 6"),
    "Armored Core" to listOf(1997 to "Armored Core", 2002 to "Armored Core 3", 2006 to "Armored Core 4", 2012 to "Armored Core V", 2023 to "Armored Core VI: Fires of Rubicon"),
    "Rocket League" to listOf(2015 to "Rocket League"),
    "Forza Horizon" to listOf(2012 to "Forza Horizon", 2014 to "Forza Horizon 2", 2016 to "Forza Horizon 3", 2018 to "Forza Horizon 4", 2021 to "Forza Horizon 5"),
    "Clash" to listOf(2012 to "Clash of Clans", 2016 to "Clash Royale"),
    "Dragon Ball" to listOf(1986 to "Dragon Ball: Shenron no Nazo", 2002 to "Dragon Ball Z: Budokai", 2015 to "Dragon Ball Xenoverse", 2018 to "Dragon Ball FighterZ", 2024 to "Dragon Ball: Sparking! Zero"),
)

/** What a studio was called in [year], for the ones that merged or renamed. */
internal fun studioNameIn(studio: String, year: Int): String = when {
    studio == "Activision Blizzard" && year < 2008 -> "Activision"
    studio == "Square Enix" && year < 2003 -> "Square"
    studio == "Bandai Namco" && year < 2006 -> "Namco"
    studio == "PlayStation Studios" && year < 2019 -> "Sony Computer Entertainment"
    studio == "Xbox Game Studios" && year < 2019 -> "Microsoft Game Studios"
    studio == "Epic Games" && year < 1999 -> "Epic MegaGames"
    else -> studio
}

/** Today's name for a studio that was called something else back then. */
internal fun canonicalStudio(name: String): String = when (name) {
    "Activision" -> "Activision Blizzard"
    "Square" -> "Square Enix"
    "Namco" -> "Bandai Namco"
    "Sony Computer Entertainment" -> "PlayStation Studios"
    "Microsoft Game Studios" -> "Xbox Game Studios"
    "Epic MegaGames" -> "Epic Games"
    else -> name
}

/** Where a real series stands when a run starts in [year]: its entries so far and when the next one lands. */
internal fun GameState.seedHistoricSeries(m: GameMarket, f: RealFranchise, year: Int) {
    val past = SeriesHistory[f.name].orEmpty()
    val debut = FranchiseDebut[f.name] ?: past.firstOrNull()?.first ?: 0
    m.franchiseEntries[f.name] = past.count { it.first < year }
    val nextYear = past.firstOrNull { it.first >= year }?.first ?: if (debut >= year) debut else null
    val inYear = f.annualWeek?.minus(1) ?: rng.int(8, 46)
    m.franchiseNext[f.name] = when {
        nextYear != null -> (nextYear - year) * 52 + inYear
        f.annualWeek != null -> inYear
        else -> rng.int(8, minOf(f.cadence, 260))
    }
}

/** When a historic series' next entry comes out, or null once it's caught up with today. */
internal fun GameState.historicNext(f: RealFranchise, entry: Int): Int? {
    val start = startYear ?: return null
    val next = SeriesHistory[f.name]?.getOrNull(entry) ?: return null
    if (next.first >= MODERN_YEAR) return null
    return (next.first - start) * 52 + (f.annualWeek?.minus(1) ?: rng.int(8, 46))
}

/** A series' entry title in a run set in the past: the real one while history has it. */
internal fun historicTitle(f: RealFranchise, entry: Int, year: Int): String? {
    if (f.name == "Fortnite" && year < 2020) return "Fortnite"
    if (f.yearly != null) return if (f.name == "EA Sports FC" && year < 2023) "FIFA ${String.format(java.util.Locale.US, "%02d", (year + 1) % 100)}" else null
    val past = SeriesHistory[f.name] ?: return if (entry == 0) f.name else null
    return past.getOrNull(entry)?.second
}

/** How much smaller a company was in [year] than today: tech has grown about 16% a year for decades. */
internal fun pastScale(year: Double): Double = if (year >= MODERN_YEAR) 1.0 else 0.86.pow(MODERN_YEAR - year)

/**
 * How company values have grown: about 6% a year from today on, and in a run set in the past,
 * from what they were worth back then.
 */
internal val GameState.industryGrowth: Double
    get() {
        val year = (startYear ?: MODERN_YEAR) + week / 52.0
        return if (year < MODERN_YEAR) pastScale(year) else 1.06.pow(year - MODERN_YEAR)
    }

/** The years today's big companies were founded, for the league table. */
internal val IndustryFounded = mapOf(
    "NVIDIA" to 1993, "Apple" to 1976, "Microsoft" to 1975, "Alphabet" to 1998, "Amazon" to 1994, "Meta" to 2004, "Tesla" to 2003,
    "TSMC" to 1987, "Tencent" to 1998, "OpenAI" to 2015, "Netflix" to 1997, "Samsung" to 1969, "xAI" to 2023, "Anthropic" to 2021,
    "Sony" to 1946, "Shopify" to 2006, "Spotify" to 2006, "Nintendo" to 1889, "Roblox" to 2004, "Take-Two Interactive" to 1993,
    "Electronic Arts" to 1982, "Reddit" to 2005, "Epic Games" to 1991, "Pinterest" to 2010, "Duolingo" to 2011, "Snap" to 2011,
    "Discord" to 2015, "Mistral AI" to 2023, "Capcom" to 1979, "Supercell" to 2010, "Wix" to 2006, "Squarespace" to 2003,
    "CD Projekt" to 1994, "Ubisoft" to 1986,
)

/** What a company was called in [year]. */
internal fun companyNameIn(name: String, year: Int): String = when {
    name == "Alphabet" && year < 2015 -> "Google"
    name == "Meta" && year < 2021 -> "Facebook"
    name == "Snap" && year < 2016 -> "Snapchat"
    else -> name
}
