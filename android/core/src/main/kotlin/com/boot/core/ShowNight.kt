package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Show night. The shows and award ceremonies play out in front of you instead of in a line of
 * the log:
 *
 * - Award nights. When a game of yours is nominated, you watch the ceremony: each category's
 *   nominees, then the envelope. Win, and you get to choose what to say on stage.
 * - Stage reports. Every stage your game goes on (the trade show, a console maker's showcase, a
 *   world premiere, your own stream) reports back with the crowd's reaction, what the press said,
 *   what the chat said, and the hype, wishlists and fans it brought.
 * - New moments around the shows: a streamer wants a preview build, fans can be rallied for the
 *   prizes the public votes on, and a well-known founder gets asked to present an award.
 */

// Award nights

/** One game up for a prize, with enough to draw its cover. */
@Serializable
data class Nominee(
    val title: String,
    val studio: String,
    val score: Double,
    val mine: Boolean,
    val topic: Topic = Topic.CITY,
    val genre: Genre = Genre.ACTION,
    val size: GameSize = GameSize.MEDIUM,
)

/** One prize on the night: the nominees, and which of them won. */
@Serializable
data class Category(val prize: String, val nominees: List<Nominee>, val winner: Int) {
    val won: Boolean get() = nominees.getOrNull(winner)?.mine == true
    val nominated: Boolean get() = nominees.any { it.mine }
}

/** A ceremony you're part of: the categories in the order they're read out. */
@Serializable
data class Ceremony(val show: String, val categories: List<Category>) {
    val wins: Int get() = categories.count { it.won }
}

@Serializable @SerialName("awardNight")
data class AwardNight(val ceremony: Ceremony) : EventKind

/** Shows the night if a game of yours is up for anything; otherwise it's just news. */
internal fun GameState.watchCeremony(show: String, categories: List<Category>) {
    val mine = categories.filter { it.nominated }
    if (mine.isEmpty()) return
    // Your categories, with the biggest prize last.
    enqueue(AwardNight(Ceremony(show.replaceFirstChar { it.uppercase() }, mine)))
}

internal fun GameState.describeAwardNight(k: AwardNight): Description {
    val c = k.ceremony
    val nominated = c.categories.flatMap { cat -> cat.nominees.filter { it.mine }.map { it.title } }.distinct()
    val wins = c.categories.filter { it.won }
    val p = games?.project
    return Description(
        when {
            wins.size > 1 -> "${wins.size} wins at ${c.show}!"
            wins.size == 1 -> "${wins.first().nominees[wins.first().winner].title} wins ${wins.first().prize}!"
            else -> "No luck at ${c.show} this time"
        },
        when {
            wins.isNotEmpty() -> "You're called up on stage. A few seconds at the microphone, and the whole industry is watching. What do you say?"
            else -> "${nominated.joinToString(" and ")} ${if (nominated.size == 1) "was" else "were"} up for ${c.categories.size} prize${if (c.categories.size == 1) "" else "s"}. " +
                "Being nominated is still worth something: people noticed."
        },
        if (wins.isEmpty()) {
            listOf(choice("Back to work", "There's always next year."))
        } else {
            listOfNotNull(
                choice("Thank the team", "Name everyone. The team is walking on air."),
                if (p != null) choice("Tease what's next", "A few words about ${p.title}. Hype and wishlists go up.") else null,
                choice("Thank the fans", "The fans who bought it. More of them sign up."),
            )
        },
        Lane.GAMES,
    )
}

internal fun GameState.resolveAwardNight(k: AwardNight, choice: Int) {
    val g = games ?: return
    stats.ceremonies += 1
    if (k.ceremony.wins == 0) return
    val label = describeAwardNight(k).choices.getOrNull(choice)?.label
    when (label) {
        "Thank the team" -> {
            for (e in staff) {
                e.morale = min(1.0, e.morale + 0.08)
                e.xp += 5.0
            }
            addLog("You thanked the team by name on stage. They won't forget it.", LogTone.GOOD, Lane.GAMES)
        }
        "Tease what's next" -> g.project?.let { p ->
            val hype = 8.0 * founderHype
            p.hype = min(100.0, p.hype + hype)
            if (p.reveals > 0) p.wishlists += wishlistsFrom(hype) else revealed(p, k.ceremony.show, hype)
            addLog("You teased ${p.title} from the stage. Hype +${hype.toInt()}.", LogTone.GOOD, Lane.GAMES)
        }
        else -> {
            g.fans += max(1_000.0, g.fans * 0.03)
            g.fanTrust = min(1.2, g.fanTrust + 0.02)
            addLog("You thanked the fans. They loved it.", LogTone.GOOD, Lane.GAMES)
        }
    }
}

// Stage reports

/** What one outlet said. */
@Serializable
data class PressQuote(val outlet: String, val line: String)

/** How the crowd took it, from 0 (silence) to 1 (on fire), in a word. */
fun reactionWord(reaction: Double): String = when {
    reaction >= 0.8 -> "On fire"
    reaction >= 0.55 -> "Hot"
    reaction >= 0.3 -> "Warm"
    else -> "Cold"
}

/** The crowd's reaction for a game with this [appeal]. */
internal fun reactionFor(appeal: Double): Double = (appeal / 1.3).clamped(0.05, 1.0)

private val outlets = listOf("IGN", "GameSpot", "Polygon", "Kotaku", "Eurogamer", "PC Gamer", "Edge")

/** Two outlets' verdicts on [title] after a stage show. */
internal fun GameState.quotesFor(title: String, reaction: Double): List<PressQuote> {
    val rng = flavour("$title quotes")
    val lines = when {
        reaction >= 0.75 -> listOf("$title stole the show.", "The best thing we saw all week.", "A genuine showstopper.", "Day one. No question.")
        reaction >= 0.45 -> listOf("$title looks solid, if a little safe.", "Promising. We want to play more.", "Not the loudest trailer, but one of the most confident.")
        else -> listOf("$title failed to make much of an impression.", "We've seen this before.", "Hard to get excited about yet.")
    }
    val picks = outlets.shuffledBy(rng).take(2)
    val said = lines.shuffledBy(rng)
    return picks.mapIndexed { i, o -> PressQuote(if (isDecades) outletIn(o, calendarYear) else o, said[i % said.size]) }
}

/** What the chat (or the crowd, before there was a chat) shouted. */
internal fun GameState.chatFor(reaction: Double): List<String> {
    val rng = flavour("chat $reaction")
    val pool = when {
        reaction >= 0.75 -> listOf("DAY ONE", "take my money", "GOTY already", "they cooked", "wishlisted instantly", "this is the one")
        reaction >= 0.45 -> listOf("looks decent", "wishlisted", "need to see gameplay", "ok that's cool", "price?", "hope it runs well")
        else -> listOf("skip", "next", "who asked", "looks dated", "is that it?", "wake me up when it's out")
    }
    return pool.shuffledBy(rng).take(4)
}

private fun <T> List<T>.shuffledBy(rng: SeededRNG): List<T> {
    val out = toMutableList()
    for (i in out.indices.reversed()) {
        val j = rng.int(0, i)
        val t = out[i]
        out[i] = out[j]
        out[j] = t
    }
    return out
}

/** How many people watch a stream of this size: fewer before everyone streamed everything. */
private fun GameState.audience(base: Double, reaction: Double): Int =
    (base * (0.5 + reaction) * (if (calendarYear < 2005) 0.08 else 1.0) * rng.normal(1.0, 0.08).clamped(0.8, 1.2)).toInt()

/** Reports back after a trailer on someone else's stage or your own. */
internal fun GameState.stageReport(stage: String, p: GameProject, appeal: Double, hype: Double, wishlists: Double, fans: Double, base: Double, extra: List<String> = emptyList()) {
    val reaction = reactionFor(appeal)
    val press = extra.toMutableList()
    press += when {
        reaction >= 0.75 -> "${p.title} was the moment everyone was talking about after $stage."
        reaction >= 0.45 -> "${p.title} went down well at $stage."
        reaction >= 0.3 -> "${p.title} got a polite reception at $stage."
        else -> "${p.title} barely registered at $stage."
    }
    enqueue(
        ShowResults(
            ShowReport(
                stage, null, p.title, audience(base, reaction), hype, fans, false, press,
                wishlists = wishlists, reaction = reaction, quotes = quotesFor(p.title, reaction), chat = chatFor(reaction), stream = true,
            ),
        ),
    )
}

// New moments around the shows

/** A big streamer wants to play your announced game before anyone else. */
@Serializable @SerialName("streamerPreview")
data class StreamerPreview(val team: Int, val streamer: String) : EventKind

/** A public vote is coming up, and fans could be rallied for your game. */
@Serializable @SerialName("fanCampaign")
data class FanCampaign(val show: String) : EventKind

/** You're asked to present an award. */
@Serializable @SerialName("presenterInvite")
data class PresenterInvite(val show: String) : EventKind

/** What rallying the fans costs. */
fun GameState.fanCampaignCost(): Double = scaled(40_000.0)

private fun GameState.streamers(): List<String> = when {
    calendarYear < 2007 -> listOf("a games magazine's cover team", "a TV games show")
    calendarYear < 2014 -> listOf("TotalBiscuit", "the Angry Joe Show", "a big YouTube channel")
    else -> listOf("Pokimane", "Shroud", "Jacksepticeye", "Markiplier", "a huge Twitch streamer")
}

/** The weekly chance of the new show moments. */
internal fun GameState.showMomentsWeek(g: GamesStudio) {
    val w = GameClock.weekOfYear(week)
    // A preview build, for an announced game that's well along.
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        if (p.reveals > 0 && p.completion >= 0.5 && p.hype >= 15 && rng.chance(0.008)) enqueue(StreamerPreview(team, rng.pick(streamers())))
    }
    // The public votes at the GDC Audience Award and the Golden Joysticks.
    for (show in AwardShow.entries) {
        if (!show.heldIn(calendarYear) || w != show.week - 1 || show.prizes.none { it.vote == Vote.FANS }) continue
        val fansPrize = show.prizes.first { it.vote == Vote.FANS }
        val field = contenders().sortedByDescending { fansPrize.rate(it) }.take(8)
        if (field.any { it.mine } && voteDrive != show.title) enqueue(FanCampaign(show.title))
    }
    // The rumour mill, a week before each show.
    for (show in showsAhead(1).filter { it.weeks == 1 }) {
        val rng = flavour(show.name)
        val p = g.project?.takeIf { it.reveals > 0 || appealOf(1) >= SHOWCASE_BAR }
        val line = when {
            p != null && rng.chance(0.5) -> "Insiders say $companyName's ${p.title} could be there."
            else -> rng.pick(listOf("Insiders promise at least one big surprise.", "Expect a shadow drop, if the leaks are right.", "A long-rumoured sequel is said to be on the running order."))
        }
        publish(NewsDesk.GAMES, rng.pick(listOf("VGC", "Kotaku", "Eurogamer")), "What to expect at ${show.name} next week", line, lane = Lane.GAMES, tag = NewsTag.RUMOUR)
    }
    // A known face gets asked to hand out a prize.
    if (reputation.score >= 60 && g.releases.isNotEmpty() && w == AWARDS_WEEK - 1 && rng.chance(0.5)) enqueue(PresenterInvite(yearEndShow(calendarYear)))
}

internal fun GameState.describeShowMoment(k: EventKind): Description? {
    val g = games ?: return null
    return when (k) {
        is StreamerPreview -> {
            val p = g.projectOf(k.team) ?: return null
            Description(
                "${k.streamer.replaceFirstChar { it.uppercase() }} wants to play ${p.title}",
                "An early build, played live to a huge audience before anyone else gets a look. " +
                    (if (p.bugs >= 5) "There are still ${p.bugs.toInt()} known bugs in it." else "The build is in decent shape."),
                listOf(
                    choice("Send a preview build", "Hype and wishlists, if it holds up. A buggy build could embarrass you live."),
                    choice("Not yet", "Keep it under wraps until it's ready."),
                ),
                Lane.GAMES,
            )
        }
        is FanCampaign -> Description(
            "The public votes at ${k.show}",
            "Fans pick one of the prizes, and your game is in with a shout. A campaign (videos, a hashtag, a few giveaways) could tip it.",
            listOf(
                choice("Rally the fans", "${money(fanCampaignCost())}. Your game does better in the public vote."),
                choice("Let the game speak", "Free. May the best game win."),
            ),
            Lane.GAMES,
        )
        is PresenterInvite -> Description(
            "Present an award at ${k.show}?",
            "The organisers want a familiar face to hand out a prize. A minute on stage in front of millions.",
            listOfNotNull(
                choice("Accept", "Good for your reputation, and the fans see you." + (g.project?.let { " You might sneak in a word about ${it.title}." } ?: "")),
                choice("Decline", "Stay home and work."),
            ),
            Lane.GAMES,
        )
        else -> null
    }
}

internal fun GameState.resolveShowMoment(k: EventKind, choice: Int) {
    val g = games ?: return
    when (k) {
        is StreamerPreview -> {
            val p = g.projectOf(k.team) ?: return
            if (choice != 0) return
            val buggy = p.bugs >= 5 && rng.chance(min(0.7, p.bugs / 20.0))
            if (buggy) {
                p.hype = max(0.0, p.hype - 5.0)
                reputation.adjust(-1.0)
                addLog("The ${p.title} preview crashed live on ${k.streamer}'s stream. The clip went round.", LogTone.BAD, Lane.GAMES)
                publish(NewsDesk.GAMES, "Kotaku", "${p.title} preview build crashes live on stream", "$companyName's game has some way to go.", LogTone.BAD, Lane.GAMES)
            } else {
                val hype = 8.0 * founderHype
                p.hype = min(100.0, p.hype + hype)
                p.wishlists *= 1.15
                p.wishlists += wishlistsFrom(hype)
                stats.previews += 1
                addLog("${k.streamer.replaceFirstChar { it.uppercase() }} played ${p.title} live. Chat loved it: hype +${hype.toInt()}, wishlists up.", LogTone.GOOD, Lane.GAMES)
            }
        }
        is FanCampaign -> if (choice == 0) {
            spend(fanCampaignCost(), Lane.GAMES)
            voteDrive = k.show
            addLog("You rallied the fans for ${k.show}.", LogTone.NEUTRAL, Lane.GAMES)
        }
        is PresenterInvite -> if (choice == 0) {
            reputation.adjust(2.0)
            g.fans += max(500.0, g.fans * 0.01)
            stats.presented += 1
            g.project?.let { p -> if (p.reveals > 0) p.hype = min(100.0, p.hype + 4.0) }
            addLog("You presented a prize at ${k.show}. The press liked you.", LogTone.GOOD, Lane.GAMES)
        }
        else -> Unit
    }
}
