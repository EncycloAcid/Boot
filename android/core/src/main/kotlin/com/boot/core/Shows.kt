package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/*
 * The show season. Around the summer trade show (TradeShow.kt) the industry has a year full of
 * stages: award shows that hand out prizes besides Game of the Year, platform showcases where
 * Nintendo, Sony, Microsoft (and Sega, in the nineties) invite promising games on their consoles,
 * world premieres at the year-end awards and Gamescom, and, once you're big enough, a showcase
 * of your own. Every stage is a chance to reveal what you're making: the first trailer is the
 * announcement and lands hardest; later ones add less. A finished game can be shadow-dropped
 * live on stage.
 */

// Award shows

/** What an award show's jury rewards. */
enum class Vote { DESIGN, CRAFT, ART, FANS }

/** One award: what it's called, what the jury rewards, and whether only small and medium games are up for it. */
data class Prize(val title: String, val vote: Vote, val indie: Boolean = false)

/** The award shows besides Game of the Year, each with prizes of its own. They don't stop the clock. */
enum class AwardShow(val week: Int, internal val from: Int, val prizes: List<Prize>) {
    DICE(7, 1998, listOf(Prize("Outstanding Achievement in Game Design", Vote.DESIGN), Prize("Best Indie Game", Vote.ART, indie = true))),
    GDC(11, 2001, listOf(Prize("Best Technology", Vote.CRAFT), Prize("Audience Award", Vote.FANS))),
    BAFTA(15, 2004, listOf(Prize("Artistic Achievement", Vote.ART), Prize("Best Game", Vote.DESIGN))),

    /** Before 2003 the Golden Joysticks are the year-end show, so this one starts after. */
    GOLDEN_JOYSTICKS(46, 2003, listOf(Prize("Ultimate Game of the Year", Vote.FANS), Prize("Best Indie Game", Vote.ART, indie = true))),
    ;

    /** The headline prize. */
    val prize: String get() = prizes.first().title

    val title: String
        get() = when (this) {
            DICE -> "the D.I.C.E. Awards"
            GDC -> "the Game Developers Choice Awards"
            BAFTA -> "the BAFTA Games Awards"
            GOLDEN_JOYSTICKS -> "the Golden Joystick Awards"
        }

    fun heldIn(year: Int) = year >= from

    internal fun rate(c: Contender): Double = prizes.first().rate(c)
}

/** The year-end show's prize for smaller games, given alongside Game of the Year. */
val YEAR_END_INDIE = Prize("Best Independent Game", Vote.ART, indie = true)

/** The year-end show where Game of the Year is decided, by its name at the time. */
fun yearEndShow(year: Int): String = when {
    year < 1983 -> "the Arkie Awards"
    year < 2003 -> "the Golden Joystick Awards"
    year < 2014 -> "the Spike Video Game Awards"
    else -> "The Game Awards"
}

/** A game in the running for an award. */
internal data class Contender(
    val title: String, val studio: String, val score: Double, val sales: Double, val size: GameSize, val releaseId: Int?,
    val topic: Topic = Topic.CITY, val genre: Genre = Genre.ACTION,
) {
    val mine: Boolean get() = releaseId != null
    val nominee: Nominee get() = Nominee(title, studio, score, mine, topic, genre, size)
}

/** Games out in the last year, yours and everyone else's, worth a jury's time. */
internal fun GameState.contenders(): List<Contender> {
    val g = games ?: return emptyList()
    val rivals = g.market.games.filter { week - it.week in 0 until 52 && it.score >= 7.0 }
        .map { Contender(it.title, it.studio, it.score, it.lifetime + it.weeklySales, it.size, null, it.topic, it.genre) }
    val mine = g.releases.filter { week - it.week in 0 until 52 && it.publisher == null && it.score >= 7.0 }
        .map { Contender(it.title, companyName, it.score, it.lifetimeSales, it.concept.size, it.id, it.concept.topic, it.concept.genre) }
    return rivals + mine
}

/** How a jury rates a game: the score, leaning towards what the prize cares about. */
internal fun Prize.rate(c: Contender): Double = when (vote) {
    Vote.DESIGN -> c.score
    Vote.CRAFT -> c.score + when (c.size) {
        GameSize.AAA -> 0.6
        GameSize.LARGE -> 0.3
        else -> 0.0
    }
    // Juries love a small game with a point of view.
    Vote.ART -> c.score + when (c.size) {
        GameSize.SMALL -> 0.5
        GameSize.MEDIUM -> 0.3
        else -> 0.0
    }
    // The public votes with its wallet.
    Vote.FANS -> c.score * 0.4 + ln(max(c.sales, 1.0)) / ln(10.0) * 0.6
}

internal fun GameState.holdAwardShow(show: AwardShow) {
    val categories = show.prizes.mapNotNull { awardPrize(it, show.title) }
    if (voteDrive == show.title) voteDrive = null
    watchCeremony(show.title, categories)
}

/** One prize: five nominees, and a jury that usually, but not always, picks the best. */
internal fun GameState.awardPrize(prize: Prize, show: String): Category? {
    val g = games ?: return null
    val field = contenders().filter { !prize.indie || it.size == GameSize.SMALL || it.size == GameSize.MEDIUM }
    if (field.isEmpty()) return null
    // Rallied fans count in a public vote.
    val rallied = prize.vote == Vote.FANS && voteDrive == show
    fun rate(c: Contender) = prize.rate(c) + if (rallied && c.mine) 0.6 else 0.0
    val nominees = field.sortedByDescending { rate(it) }.take(5)
    val top = rate(nominees.first())
    val weights = nominees.map { exp((rate(it) - top) * 3) }
    var roll = rng.range(0.0, weights.sum())
    var winner = nominees.first()
    for ((i, w) in weights.withIndex()) {
        roll -= w
        if (roll <= 0) {
            winner = nominees[i]
            break
        }
    }
    g.market.awards += Award(GameClock.year(week), winner.title, winner.studio, winner.score, winner.mine, prize = prize.title, show = show)
    if (winner.mine) {
        stats.honours += 1
        g.fans += max(1_000.0, g.fans * 0.08)
        reputation.adjust(2.0)
        g.release(winner.releaseId ?: -1)?.let { it.weeklySales *= 1.25 }
        addLog("${winner.title} won ${prize.title} at $show.", LogTone.MILESTONE, Lane.GAMES)
        publish(
            NewsDesk.YOU, "IGN", "${winner.title} wins ${prize.title}", "$companyName picks up a prize at $show, beating ${nominees.size - 1} other nominees.", LogTone.GOOD, Lane.GAMES,
            tag = NewsTag.AWARDS, quote = flavour(prize.title).pick(listOf("This one's for the team.", "We made it for the players. This is theirs too.", "We didn't expect this. Thank you.")),
            quoteBy = founder.name, cover = NewsCover(winner.title, winner.topic, winner.genre, winner.size),
        )
    } else {
        val mine = nominees.filter { it.mine }
        if (mine.isNotEmpty()) addLog("${mine.joinToString(" and ") { it.title }} was nominated for ${prize.title} at $show, but ${winner.title} took it.", LogTone.NEUTRAL, Lane.GAMES)
        publish(
            NewsDesk.GAMES, "IGN", "${winner.title} wins ${prize.title}", "${winner.studio} takes the prize at $show.", lane = Lane.GAMES,
            tag = NewsTag.AWARDS, cover = NewsCover(winner.title, winner.topic, winner.genre, winner.size),
        )
    }
    return Category(prize.title, nominees.map { it.nominee }, nominees.indexOf(winner))
}

// Reveals

/** How much a trailer can still surprise anyone: the first one is the announcement. */
internal val GameProject.revealBoost: Double
    get() = when (reveals) {
        0 -> 1.5
        1 -> 1.0
        2 -> 0.75
        else -> 0.55
    }

/** What making a trailer costs, by the size of the game. */
fun GameState.trailerCost(p: GameProject): Double = scaled(
    when (p.concept.size) {
        GameSize.SMALL -> 10_000.0
        GameSize.MEDIUM -> 30_000.0
        GameSize.LARGE -> 80_000.0
        GameSize.AAA -> 200_000.0
    },
)

/** Prices of the day, rounded to the nearest thousand. */
internal fun GameState.scaled(today: Double): Double = max(1_000.0, (today * pastScale(calendarYear.toDouble()) / 1_000).roundToLong() * 1_000.0)

/** Puts a trailer out: the hype it builds, the fans it finds, and the news if it's the first. */
internal fun GameState.reveal(p: GameProject, stage: String, base: Double, appeal: Double, extra: Double = 1.0): Double {
    val g = games ?: return 0.0
    val hype = base * (0.4 + appeal) * p.revealBoost * extra * founderHype
    p.hype = min(100.0, p.hype + hype)
    g.fans += max(200.0, g.fans * 0.015 * (0.5 + appeal) * p.revealBoost)
    revealed(p, stage, hype)
    return hype
}

/** Counts a trailer, with the wishlists it brings, and makes the first one the game's announcement in the news. */
internal fun GameState.revealed(p: GameProject, stage: String, hype: Double) {
    p.wishlists += wishlistsFrom(hype)
    if (p.reveals == 0) {
        stats.announcements += 1
        val said = quotesFor(p.title, reactionFor(appealOf(if (games?.project2 === p) 2 else 1))).first()
        publish(
            NewsDesk.YOU, "IGN", "$companyName announces ${p.title}",
            "The first trailer premiered at $stage: ${Format.a(p.concept.size.lower)} ${p.concept.topic.title.lowercase()} ${p.concept.genreLower} game.",
            LogTone.GOOD, Lane.GAMES, tag = NewsTag.SHOWS, quote = said.line, quoteBy = said.outlet, cover = p.newsCover(),
        )
    }
    p.reveals += 1
}

/** How the crowd will take a particular game, with the trade show's rules. */
internal fun GameState.appealOf(team: Int): Double {
    val g = games ?: return 0.0
    val p = g.projectOf(team) ?: return 0.0
    val expected = gameForecast(team)?.expectedScore ?: 5.0
    var appeal = ((expected - 4.0) / 4.5).clamped(0.1, 1.25) * (0.55 + 0.45 * min(1.0, p.completion / 0.5))
    if (g.trend?.matches(p.concept) == true) appeal *= 1.15
    val original = p.concept.sequelOf?.let { g.release(it) }
    if (original != null && original.score >= 8.0) appeal *= 1.2
    return appeal.clamped(0.1, 1.5)
}

/** Shadow-drops a finished game live on stage: a burst of hype and straight out of the door. */
internal fun GameState.shadowDrop(team: Int, stage: String) {
    val p = games?.projectOf(team) ?: return
    if (!p.isReady) return
    p.hype = min(100.0, p.hype + 20.0)
    stats.shadowDrops += 1
    addLog("${p.title} shadow-dropped during $stage. It's out now.", LogTone.GOOD, Lane.GAMES)
    publish(
        NewsDesk.YOU, "IGN", "${p.title} is out now", "$companyName shadow-dropped it live during $stage.", LogTone.GOOD, Lane.GAMES, lead = true,
        tag = NewsTag.SHOWS, quote = "Nobody saw that coming.", quoteBy = "The chat", cover = p.newsCover(),
    )
    launchGame(team)
}

// Platform showcases

/** The console makers' own showcases, by name and week in a given year. */
enum class PlatformShow(val maker: String) {
    NINTENDO("Nintendo"),
    SONY("Sony"),
    MICROSOFT("Microsoft"),
    SEGA("Sega"),
    ;

    /** This maker's show in [year], and the week it's held, or null if there isn't one. */
    fun inYear(year: Int): Pair<String, Int>? = when (this) {
        NINTENDO -> when {
            year in 1989..2001 -> "Nintendo Space World" to 34
            year >= 2011 -> "a Nintendo Direct" to 37
            else -> null
        }
        SONY -> when {
            year in 1995..2018 -> "Sony's E3 press conference" to 25
            year >= 2019 -> "a PlayStation State of Play" to 22
            else -> null
        }
        MICROSOFT -> when {
            year in 2001..2019 -> "Microsoft's E3 press conference" to 25
            year >= 2020 -> "the Xbox Games Showcase" to 24
            else -> null
        }
        SEGA -> if (year in 1995..2000) "Sega's E3 press conference" to 25 else null
    }
}

/** What a console maker pays to put its logo at the end of your trailer, by the size of the game. */
fun GameState.marketingDeal(p: GameProject): Double = scaled(
    when (p.concept.size) {
        GameSize.SMALL -> 60_000.0
        GameSize.MEDIUM -> 180_000.0
        GameSize.LARGE -> 450_000.0
        GameSize.AAA -> 1_200_000.0
    },
)

/** The appeal a console maker wants before it gives a game stage time. */
const val SHOWCASE_BAR = 0.45

@Serializable @SerialName("platformShow")
data class PlatformInvite(val maker: String, val show: String, val team: Int) : EventKind

/** A world premiere slot at the year-end awards or Gamescom. */
@Serializable @SerialName("premiere")
data class PremiereSlot(val show: String, val fee: Double, val team: Int) : EventKind

/** Your own showcase stream, once you're big enough to hold one. */
@Serializable @SerialName("ownShowcase")
data object OwnShowcase : EventKind

/** Fans needed before your own showcase is worth streaming. */
const val OWN_SHOWCASE_FANS = 200_000.0

/** The week of the year you'd hold your own showcase. */
const val OWN_SHOWCASE_WEEK = 36

/** Gamescom's week: world premieres at the end of the summer. */
const val GAMESCOM_WEEK = 34

private fun gamescomIn(year: Int): String? = when {
    year >= 2019 -> "Gamescom Opening Night Live"
    year >= 2009 -> "Gamescom"
    else -> null
}

/** Year-end world premieres started with the Spike awards. */
private fun premieresAtYearEnd(year: Int) = year >= 2003

/** The games you're making, by team, that could go on a stage. */
private fun GameState.teamsInDevelopment(): List<Int> = listOfNotNull(1.takeIf { games?.project != null }, 2.takeIf { games?.project2 != null })

/** One week of the show season: award shows, invitations and premieres. */
internal fun GameState.showSeasonWeek() {
    val g = games ?: return
    val w = GameClock.weekOfYear(week)
    val year = calendarYear
    for (show in AwardShow.entries) if (show.heldIn(year) && w == show.week) holdAwardShow(show)

    for (maker in PlatformShow.entries) {
        val (name, at) = maker.inYear(year) ?: continue
        if (w != at) continue
        // The maker picks one game of yours on its consoles, if one looks good enough.
        val team = teamsInDevelopment().firstOrNull { t ->
            g.projectOf(t)!!.concept.platforms.any { it.maker == maker.maker }
        } ?: continue
        val p = g.projectOf(team)!!
        if (appealOf(team) >= SHOWCASE_BAR) {
            enqueue(PlatformInvite(maker.maker, name, team))
        } else {
            addLog("${maker.maker} passed on ${p.title} for ${name.removePrefix("a ").removePrefix("the ")}. Make it look better first.", LogTone.NEUTRAL, Lane.GAMES)
        }
    }

    if (w == GAMESCOM_WEEK) gamescomIn(year)?.let { name ->
        teamsInDevelopment().firstOrNull()?.let { enqueue(PremiereSlot(name, scaled(150_000.0), it)) }
    }
    if (w == AWARDS_WEEK - 1 && premieresAtYearEnd(year)) {
        teamsInDevelopment().firstOrNull()?.let { enqueue(PremiereSlot(yearEndShow(year).replaceFirstChar { c -> c.uppercase() }, scaled(250_000.0), it)) }
    }
    if (w == OWN_SHOWCASE_WEEK && g.fans >= OWN_SHOWCASE_FANS && teamsInDevelopment().isNotEmpty()) enqueue(OwnShowcase)
}

/** What it costs to stream your own showcase. */
fun GameState.ownShowcaseCost(): Double = scaled(300_000.0)

internal fun GameState.describeShows(kind: EventKind): Description? {
    val g = games ?: return null
    return when (kind) {
        is PlatformInvite -> {
            val p = g.projectOf(kind.team) ?: return null
            Description(
                "${kind.maker} wants ${p.title} for ${kind.show}",
                "${kind.maker} liked what they saw of ${p.title} and offered it a slot. " +
                    if (p.reveals == 0) "It would be the game's first trailer: the announcement." else "Fans have seen it before, so a new trailer adds a little less.",
                listOfNotNull(
                    choice("Show a trailer", "${money(trailerCost(p))} for the trailer. Hype and fans on ${kind.maker}'s consoles."),
                    if (p.exclusiveWith == null) choice("Take their marketing deal", "They pay ${money(marketingDeal(p))} and give it more stage time. Fans on other platforms grumble.") else null,
                    if (p.isReady) choice("Shadow-drop it", "Release it live during the show, with a burst of hype.") else null,
                    choice("Not this time", "Keep it under wraps."),
                ),
                Lane.GAMES,
            )
        }
        is PremiereSlot -> {
            val p = g.projectOf(kind.team) ?: return null
            Description(
                "A world premiere slot at ${kind.show}",
                "The organisers have a slot for one more trailer, and millions will be watching. " +
                    if (p.reveals == 0) "${p.title} hasn't been announced yet: this would be its reveal." else "${p.title} has had ${p.reveals} trailer${if (p.reveals == 1) "" else "s"} already.",
                listOfNotNull(
                    choice("World premiere", "${money(kind.fee + trailerCost(p))} for the slot and the trailer. Big hype for ${p.title}."),
                    if (p.isReady) choice("Shadow-drop it", "${money(kind.fee)}. Release it live on stage.") else null,
                    choice("Skip it", "Save the money."),
                ),
                Lane.GAMES,
            )
        }
        OwnShowcase -> {
            val shown = teamsInDevelopment().mapNotNull { g.projectOf(it)?.title }
            Description(
                "Host the $companyName Showcase?",
                "You're big enough to hold your own stream: your fans tune in, and the press covers it. You'd show ${shown.joinToString(" and ")}. " +
                    "If there's not much to see, viewers say so.",
                listOf(
                    choice("Go live", "${money(ownShowcaseCost())}. Lots of hype and new fans if it lands."),
                    choice("Not this year", "Save it for when there's more to show."),
                ),
                Lane.GAMES,
            )
        }
        else -> null
    }
}

internal fun GameState.resolveShows(kind: EventKind, choice: Int) {
    val g = games ?: return
    when (kind) {
        is PlatformInvite -> {
            val p = g.projectOf(kind.team) ?: return
            val labels = describeShows(kind)?.choices?.map { it.label } ?: return
            val stage = kind.show
            when (labels.getOrNull(choice)) {
                "Show a trailer" -> {
                    spend(trailerCost(p), Lane.GAMES)
                    onStage(p, stage, appealOf(kind.team), 2_000_000.0) { reveal(p, stage, 14.0, appealOf(kind.team)) }
                }
                "Take their marketing deal" -> {
                    cash += marketingDeal(p)
                    recordRevenue(marketingDeal(p), Lane.GAMES)
                    p.exclusiveWith = kind.maker
                    g.fanTrust = max(0.5, g.fanTrust - 0.03)
                    stats.platformDeals += 1
                    addLog("${kind.maker} paid ${money(marketingDeal(p))} to feature ${p.title} in $stage; fans on other platforms are muttering.", LogTone.GOOD, Lane.GAMES)
                    onStage(p, stage, appealOf(kind.team), 2_500_000.0, listOf("${kind.maker} gave ${p.title} extra stage time.")) {
                        reveal(p, stage, 14.0, appealOf(kind.team), extra = 1.35)
                    }
                }
                "Shadow-drop it" -> shadowDrop(kind.team, stage)
                else -> addLog("You kept ${p.title} out of $stage.", LogTone.NEUTRAL, Lane.GAMES)
            }
        }
        is PremiereSlot -> {
            val p = g.projectOf(kind.team) ?: return
            val labels = describeShows(kind)?.choices?.map { it.label } ?: return
            when (labels.getOrNull(choice)) {
                "World premiere" -> {
                    spend(kind.fee + trailerCost(p), Lane.GAMES)
                    stats.premieres += 1
                    onStage(p, kind.show, appealOf(kind.team), 6_000_000.0, listOf("${p.title} had a world premiere.")) {
                        reveal(p, kind.show, 22.0, appealOf(kind.team))
                    }
                }
                "Shadow-drop it" -> {
                    spend(kind.fee, Lane.GAMES)
                    shadowDrop(kind.team, kind.show)
                }
                else -> Unit
            }
        }
        OwnShowcase -> {
            if (choice != 0) return
            spend(ownShowcaseCost(), Lane.GAMES)
            stats.ownShowcases += 1
            val stage = "the $companyName Showcase"
            val teams = teamsInDevelopment()
            val appeal = teams.maxOf { appealOf(it) }
            if (appeal < SHOWCASE_BAR) {
                // Thin on content: the chat says so.
                g.fanTrust = max(0.5, g.fanTrust - 0.02)
                for (t in teams) g.projectOf(t)?.let { reveal(it, stage, 15.0, appealOf(t), extra = 0.5) }
                addLog("$stage left viewers wanting more. Not much to show this time.", LogTone.BAD, Lane.GAMES)
            } else {
                val best = teams.maxByOrNull { appealOf(it) }!!
                for (t in teams) g.projectOf(t)?.let { p ->
                    if (t == best) {
                        onStage(p, stage, appeal, g.fans * 4, listOf("Your own stream: ${teams.size} game${if (teams.size == 1) "" else "s"} on show.")) {
                            reveal(p, stage, 30.0, appealOf(t)).also { g.fans += g.fans * 0.05 * (0.5 + appeal) }
                        }
                    } else {
                        reveal(p, stage, 30.0, appealOf(t))
                    }
                }
                addLog("$stage went down well. Hype is up and new fans signed up.", LogTone.GOOD, Lane.GAMES)
            }
        }
        else -> Unit
    }
}

/** Runs a trailer on [stage] and reports back with what it did. */
private fun GameState.onStage(p: GameProject, stage: String, appeal: Double, audience: Double, notes: List<String> = emptyList(), show: () -> Double) {
    val g = games ?: return
    val lists = p.wishlists
    val fans = g.fans
    val hype = show()
    addLog("${p.title} was in $stage. Hype +${hype.toInt()}.", LogTone.GOOD, Lane.GAMES)
    stageReport(stage, p, appeal, hype, p.wishlists - lists, g.fans - fans, audience, notes)
}

// The calendar

/**
 * The shows in the next [within] weeks that matter to you, soonest first: the trade show, award
 * shows, Gamescom, the console makers whose machines your games are on, and your own showcase.
 */
fun GameState.showsAhead(within: Int = 8): List<UpcomingShow> {
    val g = games ?: return emptyList()
    val makers = listOfNotNull(g.project, g.project2).flatMap { p -> p.concept.platforms.map { it.maker } }.toSet()
    val out = mutableListOf<UpcomingShow>()
    for (ahead in 0..within) {
        val at = week + ahead
        val w = GameClock.weekOfYear(at)
        val year = yearAt(at)
        val cap = { s: String -> s.replaceFirstChar { it.uppercase() } }
        if (w == SHOW_WEEK) out += UpcomingShow(cap(gamesShowIn(year)), ahead)
        for (show in AwardShow.entries) if (show.heldIn(year) && w == show.week) out += UpcomingShow(cap(show.title), ahead)
        if (w == AWARDS_WEEK) out += UpcomingShow(cap(yearEndShow(year)), ahead)
        if (w == GAMESCOM_WEEK) gamescomIn(year)?.let { out += UpcomingShow(it, ahead) }
        for (maker in PlatformShow.entries) {
            val (name, on) = maker.inYear(year) ?: continue
            if (w == on && maker.maker in makers) out += UpcomingShow(cap(name.removePrefix("a ")), ahead)
        }
        if (w == OWN_SHOWCASE_WEEK && g.fans >= OWN_SHOWCASE_FANS) out += UpcomingShow("The $companyName Showcase", ahead)
    }
    return out
}
