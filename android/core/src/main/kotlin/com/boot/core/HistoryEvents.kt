package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * History as it happened, for runs that start in the past.
 * - Dated events: the week Space Invaders, Doom, Steam, the financial crisis or ChatGPT arrived,
 *   the news reports it and the market feels it: demand shifts for a while and what gamers want
 *   follows. A few ask you to decide something only that moment offers.
 * - Decades in review: as each decade ends, the papers look back on it: the game of the decade,
 *   your best, what you earned. If yours was the best game of the decade, it's crowned.
 */

/** A real moment in tech and games history: the year and week it happened, and what it does. */
enum class HistoryEvent(val year: Int, val week: Int, val headline: String, val body: String, val outlet: String, val choice: Boolean = false) {
    SPACE_INVADERS(1978, 24, "Space Invaders fever", "Arcades can't get enough of Taito's alien shooter. Japan is said to be running short of coins.", "Electronic Games"),
    PAC_MAN(1980, 21, "Pac-Man arrives", "Namco's maze game is a craze: lunchboxes, cartoons, a hit single. Everyone is playing.", "Electronic Games"),
    IBM_PC(1981, 32, "IBM launches the Personal Computer", "Big Blue's PC runs an operating system from a small company called Microsoft. Business is buying.", "Byte"),
    ET_LANDFILL(1983, 38, "Atari buries its cartridges in the desert", "Trucks full of unsold games, E.T. among them, have been dumped in a New Mexico landfill.", "The New York Times"),
    MACINTOSH(1984, 4, "Apple unveils the Macintosh", "A computer you point at with a mouse. The ad ran once, during the Super Bowl.", "InfoWorld"),
    TETRIS(1984, 24, "A Soviet researcher's falling-blocks game", "Alexey Pajitnov wrote it in Moscow. It's spreading from computer to computer.", "Byte"),
    GAME_BOY(1989, 16, "Game Boy launches with Tetris", "Nintendo's handheld comes with the falling-blocks game in the box. It's selling out.", "Electronic Gaming Monthly"),
    DOOM(1993, 50, "Doom is out, and the first episode is free", "id Software is giving away the first third of Doom as shareware. Offices everywhere have stopped working.", "Computer Gaming World", choice = true),
    NETSCAPE_IPO(1995, 32, "Netscape's shares double on day one", "A sixteen-month-old company with a web browser is worth billions. The web is the place to be.", "The New York Times"),
    WINDOWS_95(1995, 34, "Windows 95 launches", "Queues round the block at midnight, a Rolling Stones song and a Start button.", "InfoWorld"),
    VOODOO(1996, 40, "3D cards arrive", "3dfx's Voodoo card makes games look like nothing before. PC gamers are upgrading.", "PC Gamer"),
    ULTIMA_ONLINE(1997, 39, "Ultima Online opens its world", "Tens of thousands of people in one game at once, paying every month.", "Computer Gaming World"),
    GOOGLE(1998, 36, "Two Stanford students start a search company", "Google says it will organise the world's information. Investors aren't sure.", "Wired"),
    WOW(2004, 47, "World of Warcraft launches", "Blizzard's online world is full on day one. Millions are paying every month.", "IGN"),
    STEAM(2005, 40, "Steam opens to other studios", "Valve's store, until now just for its own games, will sell anyone's. A game can be on sale worldwide without a box.", "Eurogamer", choice = true),
    FACEBOOK_OPEN(2006, 38, "Facebook opens to everyone", "No college email needed any more. Anyone over 13 can sign up.", "TechCrunch"),
    CRISIS(2008, 37, "Lehman Brothers collapses", "The financial crisis is here. Customers are cutting back and banks have stopped lending.", "CNBC"),
    MINECRAFT(2009, 20, "A one-man block game takes off", "Markus Persson's Minecraft is in early alpha and selling thousands a day. Indie games are hot.", "PC Gamer"),
    TWITCH(2011, 24, "Twitch launches", "A site just for watching people play games. Streamers are the new reviewers.", "Kotaku"),
    KICKSTARTER(2012, 6, "Fans fund a game in a day", "Double Fine asked for $400,000 on Kickstarter and raised over a million in a day. Every studio is looking at crowdfunding.", "Kotaku", choice = true),
    ALEXNET(2012, 40, "A neural network wins at image recognition", "A deep learning model trained on graphics cards has crushed the competition. Labs are paying attention.", "Wired"),
    BATTLE_ROYALE(2017, 38, "Battle royale takes over", "A hundred players drop in and one walks out. PUBG and a free mode in Fortnite are the biggest games around.", "Polygon"),
    PANDEMIC(2020, 12, "The world goes into lockdown", "Offices are closing and everyone is at home: playing, scrolling, ordering online.", "The New York Times", choice = true),
    CHATGPT(2022, 48, "ChatGPT launches", "A chatbot that writes essays and code. A million people signed up in five days.", "The Verge", choice = true),
    ;

    /** The week this happens in a run that started in [startYear]. */
    fun weekIn(startYear: Int): Int = (year - startYear) * 52 + week - 1
}

/** A history event that asks you to decide something. */
@Serializable @SerialName("historyChoice")
data class HistoryChoice(val event: HistoryEvent) : EventKind

/** What history has in store, with the weeks they happen: past ones (weeks ≤ now) and those to come. */
fun GameState.historyEvents(): List<Pair<Int, HistoryEvent>> {
    val start = startYear ?: return emptyList()
    return HistoryEvent.entries.map { it.weekIn(start) to it }.filter { it.first > 0 }
}

/** Events that have happened in this run, most recent first. */
fun GameState.historySoFar(): List<Pair<Int, HistoryEvent>> = historyEvents().filter { it.first <= week }.sortedByDescending { it.first }

/** Events still to come, soonest first. */
fun GameState.historyComing(): List<Pair<Int, HistoryEvent>> = historyEvents().filter { it.first > week }.sortedBy { it.first }

/** A lane's demand shifted for a while by history. */
private fun GameState.shift(lane: Lane, title: String, demand: Double, weeks: Int, output: Double = 1.0, cost: Double = 1.0) {
    if (lane in lanes) world.pressures += Pressure(lane, title, demand, weeks, cost = cost, output = output)
}

private fun GameState.shiftAll(title: String, demand: Double, weeks: Int, output: Double = 1.0) {
    for (lane in lanes) shift(lane, title, demand, weeks, output)
}

/** Gamers want this for the next year. */
private fun GameState.trendFor(topic: Topic? = null, genre: Genre? = null) {
    val g = games ?: return
    g.trend = Trend(topic = topic, genre = genre, until = week + 52)
    addLog("Trend: gamers can't get enough of ${g.trend!!.lower} games.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Each week: has history arrived? */
internal fun GameState.historyWeek() {
    decadeWeek()
    val start = startYear ?: return
    for (e in HistoryEvent.entries) {
        if (e.weekIn(start) != week) continue
        publish(NewsDesk.INDUSTRY, e.outlet, e.headline, e.body, LogTone.NEUTRAL, lead = true, tag = NewsTag.NEWS)
        addLog("History: ${e.headline}.", LogTone.MILESTONE)
        happen(e)
        if (e.choice) enqueue(HistoryChoice(e))
    }
}

/** What each event does on its own, before any choice. */
private fun GameState.happen(e: HistoryEvent) {
    when (e) {
        HistoryEvent.SPACE_INVADERS -> {
            shift(Lane.GAMES, "Space Invaders fever", 1.1, 52)
            trendFor(topic = Topic.ALIENS)
        }
        HistoryEvent.PAC_MAN -> {
            shift(Lane.GAMES, "Pac-Man fever", 1.08, 52)
            trendFor(genre = Genre.CASUAL)
        }
        HistoryEvent.IBM_PC -> {
            shift(Lane.HARDWARE, "Business is buying computers", 1.1, 52)
            shift(Lane.OS, "Every PC needs an operating system", 1.12, 52)
        }
        HistoryEvent.ET_LANDFILL -> shift(Lane.GAMES, "Shops don't trust games", 0.95, 26)
        HistoryEvent.MACINTOSH -> shift(Lane.HARDWARE, "The Macintosh buzz", 1.05, 26)
        HistoryEvent.TETRIS -> {}
        HistoryEvent.GAME_BOY -> {
            shift(Lane.GAMES, "Game Boy in every pocket", 1.05, 52)
            trendFor(genre = Genre.PUZZLE)
        }
        HistoryEvent.DOOM -> trendFor(genre = Genre.SHOOTER)
        HistoryEvent.NETSCAPE_IPO -> shift(Lane.WEBSITES, "Everyone wants to be online", 1.15, 52)
        HistoryEvent.WINDOWS_95 -> {
            shift(Lane.OS, "Windows 95 sets the bar", 0.95, 26)
            shift(Lane.HARDWARE, "Upgrading for Windows 95", 1.08, 26)
            shift(Lane.GAMES, "PCs in every home", 1.04, 52)
        }
        HistoryEvent.VOODOO -> shift(Lane.GAMES, "The 3D card boom", 1.05, 52)
        HistoryEvent.ULTIMA_ONLINE -> trendFor(genre = Genre.RPG)
        HistoryEvent.GOOGLE -> {}
        HistoryEvent.WOW -> {
            shift(Lane.GAMES, "Online worlds are everywhere", 1.05, 52)
            trendFor(genre = Genre.RPG)
        }
        HistoryEvent.STEAM -> {}
        HistoryEvent.FACEBOOK_OPEN -> shift(Lane.SOCIAL, "Everyone's joining social networks", 1.12, 52)
        HistoryEvent.CRISIS -> shiftAll("The financial crisis", 0.92, 39)
        HistoryEvent.MINECRAFT -> {
            shift(Lane.GAMES, "Indie games are hot", 1.05, 52)
            trendFor(genre = Genre.SANDBOX)
        }
        HistoryEvent.TWITCH -> shift(Lane.GAMES, "Streamers", 1.04, 104)
        HistoryEvent.KICKSTARTER -> {}
        HistoryEvent.ALEXNET -> shift(Lane.AI, "Deep learning works", 1.15, 52)
        HistoryEvent.BATTLE_ROYALE -> trendFor(genre = Genre.SHOOTER)
        HistoryEvent.PANDEMIC -> {
            shift(Lane.GAMES, "Everyone's at home, playing", 1.2, 52)
            shift(Lane.APPS, "Everyone's on their phone", 1.1, 52)
            shift(Lane.SOCIAL, "Everyone's online", 1.15, 52)
            shift(Lane.WEBSITES, "Every shop needs a website", 1.12, 52)
        }
        HistoryEvent.CHATGPT -> shift(Lane.AI, "The ChatGPT moment", 1.3, 104)
    }
}

internal fun GameState.describeHistory(k: HistoryChoice): Description? {
    val s = momentStake
    val games = games != null
    return when (k.event) {
        HistoryEvent.DOOM -> if (!games) null else Description(
            "Give away part of your next game?",
            "Doom's first episode is free, and everyone who plays it wants the rest. Shareware: copy the disk, pass it on, pay for the full game.",
            listOf(
                choice("Go shareware", "${money(s)} for disks and postage. +12% game sales for six months, and more fans."),
                choice("Keep selling boxes", "Nothing changes."),
            ),
            Lane.GAMES,
        )
        HistoryEvent.STEAM -> if (!games) null else Description(
            "Sell your games on Steam?",
            "Valve will sell your games worldwide, no box needed. Shops won't love it.",
            listOf(
                choice("Sign up", "+8% game sales for three years. Shops stock less of you for a year (−4%)."),
                choice("Stick with shops", "Nothing changes."),
            ),
            Lane.GAMES,
        )
        HistoryEvent.KICKSTARTER -> if (!games) null else Description(
            "Crowdfund your next game?",
            "Your fans would pay now for a game they'll get later. Backers expect updates, and to be listened to.",
            listOf(
                choice("Launch a campaign", "About ${money(crowdfundTarget())} from fans, and hype for what you're making."),
                choice("Fund it yourself", "Nothing changes."),
            ),
            Lane.GAMES,
        )
        HistoryEvent.PANDEMIC -> Description(
            "Close the office?",
            "Lockdown is here. Everyone could work from home: a bit slower at first, and no rent for a while.",
            listOf(
                choice("Everyone works from home", "${money(headquarters.rent * 13)} saved. 8% less done for three months."),
                choice("Keep the office open", "Work carries on. People are nervous: morale drops."),
            ),
        )
        HistoryEvent.CHATGPT -> Description(
            "Put AI in everything?",
            "Every investor wants to hear the word AI. Bolting a chatbot onto what you make would sell, for a while. Some customers will roll their eyes.",
            listOf(
                choice("AI in everything", "+5% demand everywhere for a year. A little reputation lost."),
                choice("Wait and see", "Nothing changes."),
            ),
        )
        else -> null
    }
}

/** What fans would pledge for your next game. */
fun GameState.crowdfundTarget(): Double = max(50_000.0, (games?.fans ?: 0.0) * 15 * pastScale(calendarYear.toDouble()) + momentStake * 2)

internal fun GameState.resolveHistory(k: HistoryChoice, choice: Int) {
    val yes = choice == 0
    val s = momentStake
    when (k.event) {
        HistoryEvent.DOOM -> if (yes) {
            spend(s, Lane.GAMES)
            world.pressures += Pressure(Lane.GAMES, "Shareware", 1.12, 26, mine = true)
            games?.let { it.fans *= 1.08 }
            addLog("Your next game goes out as shareware. Disks are being copied in every school.", LogTone.GOOD, Lane.GAMES)
        }
        HistoryEvent.STEAM -> if (yes) {
            world.pressures += Pressure(Lane.GAMES, "On Steam", 1.08, 156, mine = true)
            world.pressures += Pressure(Lane.GAMES, "Shops sulking about Steam", 0.96, 52)
            addLog("Your games are on Steam.", LogTone.GOOD, Lane.GAMES)
        }
        HistoryEvent.KICKSTARTER -> if (yes) {
            val raised = crowdfundTarget()
            cash += raised
            recordRevenue(raised, Lane.GAMES)
            games?.let { g -> listOfNotNull(g.project, g.project2).forEach { it.hype = min(100.0, it.hype + 15) } }
            games?.let { it.fans *= 1.05 }
            addLog("Your Kickstarter raised ${money(raised)}. The backers are excited.", LogTone.GOOD, Lane.GAMES)
            publish(NewsDesk.GAMES, "Kotaku", "$companyName's fans fund its next game", "The campaign raised ${money(raised)}.", LogTone.GOOD, Lane.GAMES)
        }
        HistoryEvent.PANDEMIC -> if (yes) {
            cash += headquarters.rent * 13
            for (lane in lanes) world.pressures += Pressure(lane, "Working from home", 1.0, 13, output = 0.92, mine = true)
            addLog("The office is closed. Everyone's working from their kitchen table.", LogTone.NEUTRAL)
        } else {
            for (e in staff) e.morale = max(0.0, e.morale - 0.08)
            addLog("The office stays open. People come in, nervously.", LogTone.BAD)
        }
        HistoryEvent.CHATGPT -> if (yes) {
            for (lane in lanes) world.pressures += Pressure(lane, "AI in everything", 1.05, 52, mine = true)
            reputation.adjust(-1.0)
        }
        else -> {}
    }
}

// Decades in review

/** How a decade went: the game of the decade, your best, and what you made of it. */
@Serializable
data class DecadeReview(
    val decade: Int,
    val startWeek: Int,
    val endWeek: Int,
    val revenue: Double,
    val valueStart: Double,
    val valueEnd: Double,
    val topGame: String? = null,
    val topStudio: String? = null,
    val topScore: Double = 0.0,
    val yourBest: String? = null,
    val yourBestScore: Double = 0.0,
    val yourHits: Int = 0,
    /** Your best game was the best of the decade. */
    val crowned: Boolean = false,
) {
    val title: String get() = "The ${decade}s"
}

/** The decade [year] falls in: 1980 for 1987. */
fun decadeOf(year: Int): Int = year / 10 * 10

/** Each week: note where the decade began, and review it when it ends. */
private fun GameState.decadeWeek() {
    val w = world
    if (w.decadeStartWeek < 0) {
        w.decadeStartWeek = week
        w.decadeStartRevenue = stats.totalRevenue
        w.decadeStartValue = valuation
        return
    }
    if (GameClock.weekOfYear(week) != 1 || calendarYear % 10 != 0 || week - w.decadeStartWeek < 26) return
    w.decades += reviewDecade(decadeOf(calendarYear - 1))
    w.decadeStartWeek = week
    w.decadeStartRevenue = stats.totalRevenue
    w.decadeStartValue = valuation
}

/** Looks back on the decade that's just ended. */
private fun GameState.reviewDecade(decade: Int): DecadeReview {
    val w = world
    val from = w.decadeStartWeek
    val g = games
    val theirs = g?.market?.games?.filter { it.week in from until week }.orEmpty().maxByOrNull { it.score }
    val mine = g?.releases?.filter { it.week in from until week && it.publisher == null }.orEmpty()
    val best = mine.maxByOrNull { it.score }
    val crowned = best != null && best.score >= 8 && (theirs == null || best.score >= theirs.score)
    val review = DecadeReview(
        decade, from, week, stats.totalRevenue - w.decadeStartRevenue, w.decadeStartValue, valuation,
        theirs?.title, theirs?.studio, theirs?.score ?: 0.0, best?.title, best?.score ?: 0.0, mine.count { it.score >= 8 }, crowned,
    )
    val top = if (crowned) best!!.title else theirs?.title
    val body = buildString {
        append("$companyName earned ${money(review.revenue)} and went from ${money(review.valueStart)} to ${money(review.valueEnd)}.")
        if (top != null) append(" Game of the decade: $top${if (crowned) ", yours" else " (${theirs!!.studio})"}.")
        if (best != null && !crowned) append(" Your best: ${best.title} (${String.format(java.util.Locale.US, "%.1f", best.score)}).")
    }
    addLog("${review.title} are over. $body", LogTone.MILESTONE)
    publish(NewsDesk.YOU, "The Economist", "${review.title} in review", body, if (review.valueEnd >= review.valueStart) LogTone.GOOD else LogTone.NEUTRAL, lead = true)
    if (crowned) {
        reputation.adjust(4.0)
        g!!.fans *= 1.1
        stats.gamesOfTheDecade += 1
        publish(NewsDesk.GAMES, "Edge", "${best!!.title} is the game of the ${decade}s", "$companyName made the best game of the decade.", LogTone.GOOD, Lane.GAMES, lead = true)
    }
    return review
}
