package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/*
 * The feed: what people are saying about you. Players, fans, streamers, journalists, insiders,
 * rival developers and your own account post about every moment worth talking about (an
 * announcement, a trailer, a release date, launch day, a delay, an award, a buyout, a chart
 * topper) and keep chattering in between. Before the web it's the message boards; from 2006 it's
 * Chirp, the network everyone's on.
 *
 * Like all the game's writing, posts roll their own dice: reading the feed never changes what
 * happens.
 */

/** Who's posting. */
enum class Poster(val title: String) {
    PLAYER("Player"), FAN("Fan"), STREAMER("Streamer"), PRESS("Journalist"), INSIDER("Insider"),
    RIVAL("Developer"), YOU("Your account"), MEME("Meme account"),
}

@Serializable
data class Post(
    val id: Int,
    val week: Int,
    val author: String,
    val handle: String,
    val kind: Poster,
    val text: String,
    val likes: Int,
    val reposts: Int,
    val replies: Int,
    val tone: LogTone = LogTone.NEUTRAL,
    /** The game it's about, for its tag. */
    val about: String? = null,
)

/** The name of the place people post, in its day. */
val GameState.feedName: String get() = if (calendarYear < 2006) "The forums" else "Chirp"

/** A game's hashtag. */
fun hashtag(title: String): String = "#" + title.filter { it.isLetterOrDigit() }

/** What everyone's talking about this month, most talked-about first. */
fun GameState.trending(limit: Int = 4): List<String> =
    feed.filter { week - it.week <= 4 && it.about != null }.groupBy { it.about!! }
        .entries.sortedByDescending { e -> e.value.sumOf { it.likes } }.take(limit).map { hashtag(it.key) }

private const val FEED_CAP = 250

// Who posts

private val first = listOf("pixel", "night", "retro", "crit", "lag", "loot", "boss", "speed", "cozy", "chaos", "noob", "mana", "glitch", "turbo", "quiet", "salty", "frame", "combo", "save", "rogue")
private val second = listOf("pilgrim", "owl", "wizard", "goblin", "knight", "runner", "fox", "gremlin", "bard", "panda", "ghost", "potato", "dragon", "raccoon", "ronin", "otter", "golem", "witch", "pigeon", "moth")
private val streamers = listOf("LunaLive", "GrimTV", "ToastyPlays", "Captain Crit", "MossyMae", "Big Lag Energy", "NovaStreams", "Pixel Pete", "QuestQueen", "The Couch Co-op")
private val journalists = listOf("Dana Ito", "Sam Reyes", "Priya Banerjee", "Tom Okafor", "Mia Lindqvist", "Jules Moreau", "Ravi Chandran", "Elena Kowalski")
private val insiders = listOf("Leaky Pipe", "The Rumour Mill", "Dev Whispers", "Patch Notes Pete")
private val memers = listOf("Gaming Memes Daily", "Clip It", "Respawn Jokes", "Hold A To Laugh")

private fun SeededRNG.player(): Pair<String, String> {
    val a = pick(first)
    val b = pick(second)
    val n = if (chance(0.5)) "${int(1, 99)}" else ""
    val name = a.replaceFirstChar { it.uppercase() } + b.replaceFirstChar { it.uppercase() } + n
    return name to "@$a${if (chance(0.5)) "_" else ""}$b$n"
}

private fun handleOf(name: String) = "@" + name.lowercase().filter { it.isLetterOrDigit() }

/** Who's behind a post of a given kind. */
private fun GameState.author(kind: Poster, rng: SeededRNG): Pair<String, String> = when (kind) {
    Poster.PLAYER, Poster.FAN -> rng.player()
    Poster.STREAMER -> rng.pick(streamers).let { it to handleOf(it) }
    Poster.PRESS -> rng.pick(journalists).let { "$it" to handleOf(it) }
    Poster.INSIDER -> rng.pick(insiders).let { it to handleOf(it) }
    Poster.MEME -> rng.pick(memers).let { it to handleOf(it) }
    Poster.RIVAL -> {
        val studio = rng.pick(Studios.filter { studioActive(it.name, calendarYear) }.ifEmpty { Studios }).name
        "A developer at $studio" to handleOf(studio) + "_dev"
    }
    Poster.YOU -> companyName to handleOf(companyName)
}

/** How far a post travels: more for loud kinds of poster and bigger moments, more as you grow. */
private fun GameState.spread(kind: Poster, size: Double, rng: SeededRNG): Int {
    val base = when (kind) {
        Poster.STREAMER, Poster.MEME -> 4_000.0
        Poster.PRESS, Poster.INSIDER -> 1_500.0
        Poster.YOU -> 600.0 + sqrt(games?.fans ?: 0.0) * 6
        Poster.RIVAL -> 400.0
        Poster.FAN -> 250.0
        Poster.PLAYER -> 60.0
    }
    // Fewer people online in the old days.
    val era = if (calendarYear < 2006) 0.05 else if (calendarYear < 2012) 0.4 else 1.0
    return max(1.0, base * size * era * rng.range(0.3, 2.5).pow(1.5)).toInt()
}

/** Adds a post, written with its own dice. */
private fun GameState.post(kind: Poster, text: String, rng: SeededRNG, about: String? = null, tone: LogTone = LogTone.NEUTRAL, size: Double = 1.0) {
    val (name, handle) = author(kind, rng)
    val likes = spread(kind, size, rng)
    // Posts count on their own, so they never shift anyone else's ids.
    val p = Post((feed.lastOrNull()?.id ?: 0) + 1, week, name, handle, kind, text, likes, (likes * rng.range(0.05, 0.3)).toInt(), (likes * rng.range(0.02, 0.15)).toInt(), tone, about)
    feed = (feed + p).takeLast(FEED_CAP)
}

/** Fills in a template: {t} the title, {tag} its hashtag, {s} your company, {g} the genre, {x} the topic. */
private fun fill(s: String, title: String?, company: String, genre: String = "", topic: String = "") = s
    .replace("{t}", title ?: "")
    .replace("{tag}", title?.let { hashtag(it) } ?: "")
    .replace("{s}", company)
    .replace("{g}", genre)
    .replace("{x}", topic)

private fun GameState.say(rng: SeededRNG, kind: Poster, pool: List<String>, title: String?, tone: LogTone = LogTone.NEUTRAL, size: Double = 1.0, genre: String = "", topic: String = "") =
    post(kind, fill(rng.pick(pool), title, companyName, genre, topic), rng, title, tone, size)

// Moments

/** A game is announced or shown again: the feed reacts to how the trailer landed. */
internal fun GameState.feedOnTrailer(p: GameProject, stage: String, reaction: Double) {
    val rng = flavour("${p.title} trailer $stage")
    val g = p.concept.genreLower
    val x = p.concept.topic.title.lowercase()
    if (p.reveals <= 1) {
        say(rng, Poster.YOU, listOf("Say hello to {t}. {tag}", "We've been working on something. Meet {t}. {tag}", "{t}. Finally, we can talk about it. Wishlist now. {tag}", "Our next game is {t}, a {x} {g} game. More soon. {tag}"), p.title, LogTone.GOOD, 1.5, g, x)
    }
    when {
        reaction >= 0.75 -> {
            say(rng, Poster.FAN, listOf("{t} just won the whole show. I'm not ok", "THE END OF THAT {t} TRAILER", "Wishlisted {t} so fast I pulled something", "{s} cooked with {t}", "Didn't know I needed a {x} {g} game until {t}", "Rewatched the {t} trailer six times. Seven.", "{t} is the only thing anyone's talking about in my group chat"), p.title, LogTone.GOOD, 2.0, g, x)
            say(rng, Poster.STREAMER, listOf("Reacting to the {t} trailer live and I lost my voice", "{t} looks unreal. We're playing it day one on stream", "Chat voted {t} trailer of the year. Chat is right."), p.title, LogTone.GOOD, 1.5)
            say(rng, Poster.MEME, listOf("Me pretending to work after the {t} trailer dropped", "Nobody:\nAbsolutely nobody:\nMe: {tag}", "Every other trailer vs {t}"), p.title, LogTone.GOOD, 1.5)
        }
        reaction >= 0.45 -> {
            say(rng, Poster.PLAYER, listOf("{t} looks pretty good actually", "Ok {t} has my attention", "Cautiously optimistic about {t}", "{t} looks fun but I need to see gameplay", "Is {t} coming to every platform?", "That {x} setting in {t} is a vibe"), p.title, LogTone.NEUTRAL, 1.0, g, x)
            say(rng, Poster.PRESS, listOf("{t} was one of the more confident showings at $stage.", "Saw {t} at $stage. Solid, and it knows what it is.", "{s}'s {t} is worth keeping an eye on."), p.title, LogTone.NEUTRAL, 1.0)
        }
        else -> {
            say(rng, Poster.PLAYER, listOf("{t}? Pass", "Was that it? {t} looked unfinished", "Another {g} game. Wake me up when it's out.", "Who is {t} for", "{t} trailer was mid, sorry", "Graphics look a decade old"), p.title, LogTone.BAD, 1.0, g, x)
            say(rng, Poster.MEME, listOf("The audience during the {t} trailer: *crickets*", "{t} trailer brought to you by: sleep"), p.title, LogTone.BAD, 1.0)
        }
    }
}

/** A release date is set. */
internal fun GameState.feedOnDate(p: GameProject, label: String) {
    val rng = flavour("${p.title} date $label")
    say(rng, Poster.YOU, listOf("{t} launches $label. Mark your calendars. {tag}", "It's official: {t} is out $label. {tag}"), p.title, LogTone.GOOD, 1.5)
    say(rng, Poster.FAN, listOf("Taking the week off for {t}. Boss doesn't know yet", "$label can't come soon enough {tag}", "Countdown app installed. {t}, $label."), p.title, LogTone.GOOD, 1.0)
}

/** A game slips. */
internal fun GameState.feedOnDelay(p: GameProject, indefinite: Boolean) {
    val rng = flavour("${p.title} delay ${p.delays}")
    say(rng, Poster.YOU, if (indefinite) listOf("An update on {t}: we need more time, and it'll be out when it's ready. Thank you for your patience.") else listOf("{t} needs a few more weeks. It's going to be worth it. Sorry for the wait."), p.title, LogTone.BAD, 1.5)
    say(rng, Poster.PLAYER, listOf("A delayed game is eventually good. Take your time on {t}", "Again?? {tag}", "Rather they fix {t} than ship it broken", "This is the {d} delay for {t}. I'm tired".replace("{d}", if (p.delays > 1) "${p.delays}th" else "first"), "Not mad, just disappointed. {t}"), p.title, LogTone.BAD, 1.0)
    if (p.delays >= 2) say(rng, Poster.MEME, listOf("{t} release date speedrun (any%)", "Scientists confirm {t} is coming out after the heat death of the universe"), p.title, LogTone.BAD, 1.5)
}

/** Launch week: the feed follows the reviews. */
internal fun GameState.feedOnLaunch(r: GameRelease) {
    val rng = flavour("${r.title} launch")
    val sc = String.format(java.util.Locale.US, "%.1f", r.score)
    val g = r.concept.genreLower
    say(rng, Poster.YOU, listOf("{t} is out now! Thank you to everyone who helped make it. {tag}", "Launch day. {t} is yours now. Go play. {tag}", "It's here. {t}, out now everywhere. {tag}"), r.title, LogTone.GOOD, 2.0)
    val top = r.reviews.maxByOrNull { it.exact }
    if (top != null && r.score >= 7) say(rng, Poster.PRESS, listOf("Our {t} review is up: ${top.headline.lowercase()}. ${top.outlet.title} gave it ${top.formatted}${top.outlet.scale}.", "{t} reviews are in and they're good. Averaging $sc.", "Played {t} for the review. Believe the hype."), r.title, LogTone.GOOD, 1.5)
    when {
        r.score >= 8.5 -> {
            say(rng, Poster.FAN, listOf("{t} is a masterpiece and I will not be taking questions", "Called in sick. {tag}", "40 hours into {t} and I've just finished the tutorial area. no regrets", "{s} doesn't miss", "Game of the year. I said what I said. {tag}", "My partner hasn't seen me in four days. {t} is that good"), r.title, LogTone.GOOD, 2.0)
            say(rng, Poster.STREAMER, listOf("Day one {t} marathon starts NOW", "Chat has not let me stop playing {t} for 9 hours", "{t} broke our viewer record"), r.title, LogTone.GOOD, 2.0)
            say(rng, Poster.RIVAL, listOf("Congrats to everyone at {s} on {t}. Absolute craft.", "Played {t} all weekend. Back to the drawing board for us."), r.title, LogTone.GOOD, 1.0)
        }
        r.score >= 6.5 -> {
            say(rng, Poster.PLAYER, listOf("{t} is good! Not perfect, but good", "Having fun with {t}. Some rough edges", "{t} is a solid 7 for me", "Wish {t} was a bit longer but I enjoyed it", "The {g} side of {t} is great, the rest is fine"), r.title, LogTone.NEUTRAL, 1.0, g)
            say(rng, Poster.STREAMER, listOf("{t} first impressions: pretty fun!", "Playing more {t} tonight, chat's into it"), r.title, LogTone.NEUTRAL, 1.0)
        }
        else -> {
            say(rng, Poster.PLAYER, listOf("Refunded {t}. Sorry {s}", "{t} needed another year", "How did {t} ship like this", "The reviews were generous for {t}", "Waiting for a sale. {t} isn't worth full price"), r.title, LogTone.BAD, 1.0)
            say(rng, Poster.MEME, listOf("{t} physics engine, a thread", "Expectation vs reality: {t}", "{t} launch going great (it's not)"), r.title, LogTone.BAD, 2.0)
        }
    }
    if (r.bugs >= 8) say(rng, Poster.PLAYER, listOf("Fell through the floor in {t}. Twice.", "{t} crashed on me four times tonight", "Patch when {s}?"), r.title, LogTone.BAD, 1.5)
}

/** An award, won or lost. */
internal fun GameState.feedOnAward(title: String, prize: String, show: String, mine: Boolean, won: Boolean) {
    val rng = flavour("$title $prize $show")
    when {
        mine && won -> {
            say(rng, Poster.YOU, listOf("{t} won $prize at $show. We don't have words. Thank you. {tag}", "WE WON. $prize for {t}. This is for the team and for you. {tag}"), title, LogTone.GOOD, 2.0)
            say(rng, Poster.FAN, listOf("{t} winning $prize is the only correct outcome", "Deserved. {tag}", "Crying at {s}'s speech ngl"), title, LogTone.GOOD, 1.5)
        }
        mine -> say(rng, Poster.FAN, listOf("{t} was robbed at $show", "Can't believe {t} lost $prize", "{t} is still the winner in our hearts"), title, LogTone.BAD, 1.0)
        else -> if (rng.chance(0.4)) say(rng, Poster.PLAYER, listOf("{t} winning $prize, good call", "$prize to {t}? Hot take but no", "Fair play to {t}"), title, LogTone.NEUTRAL, 0.8)
    }
}

/** You buy a studio. */
internal fun GameState.feedOnDeal(studio: String) {
    val rng = flavour("deal $studio")
    say(rng, Poster.PRESS, listOf("$companyName has bought $studio. Big week.", "Confirmed: $studio is now part of $companyName.", "$companyName buying $studio makes a lot of sense, honestly."), null, LogTone.NEUTRAL, 1.5)
    say(rng, Poster.PLAYER, listOf("Please don't ruin $studio", "$studio fans, hold on to something", "Hope $companyName leaves $studio alone", "This could be good for $studio actually"), null, LogTone.NEUTRAL, 1.0)
}

/** What people say when a game goes exclusive. */
internal fun GameState.feedOnExclusive(title: String, maker: String, timed: Boolean) {
    val rng = flavour("exclusive $title")
    say(rng, Poster.PRESS, listOf("Confirmed: {t} is ${if (timed) "a timed" else "an"} exclusive for $maker.", "$maker has locked down {t}.", "{s} signs {t} with $maker"), title, LogTone.NEUTRAL, 1.5)
    say(
        rng, Poster.PLAYER,
        if (timed) listOf("So I just wait six months. Fine.", "Timed exclusive. Of course it is.", "I'll still be here when it comes out for everyone, {s}") else listOf("Guess I'm not playing {t} then", "Money talks. {tag}", "Another game I can't play. Thanks {s}", "Exclusives are bad for players and you know it"),
        title, LogTone.BAD, 1.4,
    )
    say(rng, Poster.FAN, listOf("Day one on my $maker machine {tag}", "This is why I picked a $maker console", "W for $maker {tag}"), title, LogTone.GOOD, 1.0)
}

/** A game of yours tops the charts. */
internal fun GameState.feedOnChart(title: String) {
    val rng = flavour("$title charts")
    say(rng, Poster.YOU, listOf("{t} is the best-selling game in the world this week. Thank you! {tag}", "Number one. {tag}"), title, LogTone.GOOD, 1.5)
    say(rng, Poster.PRESS, listOf("{t} goes to No. 1.", "{t} tops the charts in its launch week."), title, LogTone.GOOD, 1.0)
}

/** Critic personality beats: grudge, pet, spat, or rave. */
internal fun GameState.feedOnCriticBeat(outlet: String, kind: String, about: String?) {
    val rng = flavour("critic $outlet $kind ${about ?: ""} $week")
    when (kind) {
        "grudge" -> {
            say(rng, Poster.PRESS, listOf("$outlet has soured on $companyName.", "Industry chatter: $outlet vs $companyName is getting personal."), about, LogTone.BAD, 1.2)
            say(rng, Poster.FAN, listOf("$outlet really doesn't like $companyName anymore", "Drama at $outlet over {t}"), about, LogTone.BAD, 0.9)
        }
        "pet" -> {
            say(rng, Poster.PRESS, listOf("$outlet is openly rooting for $companyName.", "$outlet's desk can't stop talking about $companyName."), about, LogTone.GOOD, 1.1)
            say(rng, Poster.FAN, listOf("$outlet loves {t}. Same", "$companyName and $outlet sitting in a tree"), about, LogTone.GOOD, 0.9)
        }
        "spat" -> {
            say(rng, Poster.PLAYER, listOf("$outlet went scorched earth on {t}", "Did $outlet even play {t}? That score is personal"), about, LogTone.BAD, 1.1)
            say(rng, Poster.PRESS, listOf("$outlet's {t} review is making the rounds.", "A notably cold take on {t} from $outlet."), about, LogTone.NEUTRAL, 1.0)
        }
        "rave" -> {
            say(rng, Poster.PRESS, listOf("$outlet crowns {t}.", "{t} is $outlet's game of the moment."), about, LogTone.GOOD, 1.1)
            say(rng, Poster.FAN, listOf("$outlet gets it. {t} rules", "Pull quote from $outlet on {t} goes hard"), about, LogTone.GOOD, 0.9)
        }
    }
}

/** Mod / community scene beats on the feed. */
internal fun GameState.feedOnMod(title: String, kind: String) {
    val rng = flavour("mod feed $title $kind $week")
    when (kind) {
        "form" -> say(rng, Poster.FAN, listOf("People are already modding {t}", "The {t} workshop is waking up {tag}", "Unofficial {t} patches already? Love this community"), title, LogTone.GOOD, 0.9)
        "support" -> say(rng, Poster.PLAYER, listOf("{s} actually posted mod docs for {t}", "Modding {t} just got easier", "Shout out {s} for the {t} samples"), title, LogTone.GOOD, 0.8)
        "kit" -> {
            say(rng, Poster.PRESS, listOf("{s} ships an official mod kit for {t}.", "{t} opens up properly to creators."), title, LogTone.GOOD, 1.2)
            say(rng, Poster.FAN, listOf("OFFICIAL {t} KIT {tag}", "Total conversion season for {t} starts now"), title, LogTone.GOOD, 1.1)
        }
        "clamp" -> {
            say(rng, Poster.PLAYER, listOf("{s} just banned a pile of {t} mods", "Workshop purge on {t}. Gross.", "So much for the {t} community"), title, LogTone.BAD, 1.2)
            say(rng, Poster.MEME, listOf("{s} discovering modders have feelings", "Another day of the {t} mod ban"), title, LogTone.BAD, 1.0)
        }
        "partner" -> say(rng, Poster.FAN, listOf("{s} partnered with a big {t} mod team — W", "Semi-official {t} mods? Take my money", "The {t} scene just leveled up"), title, LogTone.GOOD, 1.0)
        "moty" -> {
            say(rng, Poster.PRESS, listOf("{t} takes Mod of the Year.", "The {t} workshop is the story of the year."), title, LogTone.GOOD, 1.5)
            say(rng, Poster.STREAMER, listOf("Playing the Mod of the Year build of {t} all week {tag}", "{t} mods ate my schedule"), title, LogTone.GOOD, 1.3)
        }
        else -> say(rng, Poster.PLAYER, listOf("Still installing {t} mods", "{t} workshop hits different"), title, LogTone.NEUTRAL, 0.6)
    }
}

/** Co-dev partner beats: propose, glory, blame, disputes. */
internal fun GameState.feedOnCoDev(kind: String, partner: String?, title: String?) {
    val rng = flavour("codev $kind ${partner ?: ""} ${title ?: ""} $week")
    when (kind) {
        "propose" -> say(rng, Poster.INSIDER, listOf("$companyName pitched a co-dev to $partner on {t}", "Co-dev talks: $companyName × $partner"), title, LogTone.NEUTRAL, 0.8)
        "accept" -> {
            say(rng, Poster.PRESS, listOf("$companyName and $partner lock a co-dev on {t}.", "Split studio: {t} is a joint project."), title, LogTone.GOOD, 1.2)
            say(rng, Poster.FAN, listOf("$partner on {t}? Take my money", "Co-dev arc for {t} {tag}"), title, LogTone.GOOD, 1.0)
        }
        "decline" -> say(rng, Poster.INSIDER, listOf("$partner passed on co-dev with $companyName", "No deal: $partner / $companyName"), title, LogTone.NEUTRAL, 0.6)
        "glory" -> {
            say(rng, Poster.PRESS, listOf("{t} is a shared win for $companyName and $partner.", "Both logos on the victory lap for {t}."), title, LogTone.GOOD, 1.4)
            say(rng, Poster.FAN, listOf("{t} co-dev W {tag}", "$partner and $companyName ate on {t}"), title, LogTone.GOOD, 1.2)
        }
        "blame", "delay" -> {
            say(rng, Poster.PRESS, listOf("{t} mess sticks to $companyName and $partner.", "Shared blame on the {t} co-dev."), title, LogTone.BAD, 1.2)
            say(rng, Poster.MEME, listOf("co-dev of pain: {t}", "$partner discovering $companyName calendars"), title, LogTone.BAD, 1.0)
        }
        "ship" -> say(rng, Poster.INSIDER, listOf("$companyName × $partner ships {t}", "{t} lands with both logos"), title, LogTone.NEUTRAL, 0.9)
        "quit" -> say(rng, Poster.PRESS, listOf("$partner walks from the {t} co-dev.", "Partnership fracture on {t}."), title, LogTone.BAD, 1.3)
        "sue" -> say(rng, Poster.PRESS, listOf("$partner takes $companyName to court over {t}.", "Co-dev lawsuit: {t}."), title, LogTone.BAD, 1.4)
        "smooth" -> say(rng, Poster.INSIDER, listOf("$companyName papered over the $partner co-dev spat", "Peace talks on {t}"), title, LogTone.NEUTRAL, 0.7)
        "credit_fight" -> say(rng, Poster.PLAYER, listOf("$partner tried to soft-poach {t} credit", "Credit war on {t}: $companyName clapped back"), title, LogTone.NEUTRAL, 1.0)
        "cancel" -> say(rng, Poster.INSIDER, listOf("{t} cancelled — $partner co-dev dies with it", "Joint project {t} is dead"), title, LogTone.BAD, 0.9)
        else -> {}
    }
}

/** Industry pressure beats: temps, streamers, platform politics. */
internal fun GameState.feedOnIndustry(kind: String, about: String?, title: String? = null) {
    val rng = flavour("industry $kind ${about ?: ""} ${title ?: ""} $week")
    when (kind) {
        "temp_hire" -> say(rng, Poster.INSIDER, listOf("$companyName just brought in a wave of contractors", "Crunch contractors spotted at $companyName"), about, LogTone.NEUTRAL, 0.7)
        "temp_fair" -> say(rng, Poster.PLAYER, listOf("At least $companyName paid out the contractors", "Rare W: fair contract end"), about, LogTone.GOOD, 0.6)
        "temp_quiet", "temp_cut" -> say(rng, Poster.PLAYER, listOf("$companyName treating contractors like furniture again", "Another contractor story out of $companyName"), about, LogTone.BAD, 0.9)
        "temp_expire" -> {}
        "temp_scandal" -> {
            say(rng, Poster.PRESS, listOf("Contract labour backlash hits $companyName.", "Industry press digs into $companyName's temp pipeline."), about, LogTone.BAD, 1.3)
            say(rng, Poster.MEME, listOf("$companyName discovering labour law", "Temp-to-scandal speedrun"), about, LogTone.BAD, 1.0)
        }
        "stream_court" -> say(rng, Poster.STREAMER, listOf("Keys for {t} landed — going live soon {tag}", "Chat voted yes on {t}. See you tonight"), title, LogTone.GOOD, 1.1)
        "stream_snub" -> say(rng, Poster.STREAMER, listOf("Not touching {t} after that pitch", "Chat said skip {t} and chat was right"), title, LogTone.BAD, 1.0)
        "cert_chase" -> say(rng, Poster.INSIDER, listOf("$companyName is burning favors to clear cert on {t}", "Cert office: {t} got pulled forward"), title, LogTone.NEUTRAL, 0.8)
        "featuring" -> say(rng, Poster.PRESS, listOf("$companyName buys a storefront slot at $about.", "Discovery deal: $about featuring $companyName."), null, LogTone.GOOD, 1.0)
        "logo_deal" -> say(rng, Poster.PRESS, listOf("$companyName puts the $about logo on the box.", "First-party-friendly deal: $companyName × $about."), null, LogTone.GOOD, 1.2)
        else -> {}
    }
}

/** Rivalry story beats on the feed: heat, copycat, flop, or poach. */
internal fun GameState.feedOnRivalBeat(studio: String, kind: String, about: String?) {
    val rng = flavour("rival $studio $kind ${about ?: ""} $week")
    when (kind) {
        "heat" -> {
            say(rng, Poster.PRESS, listOf("$studio and $companyName are properly at war now.", "Industry watchers: the $studio vs $companyName rivalry just got personal."), about, LogTone.BAD, 1.2)
            say(rng, Poster.FAN, listOf("Popcorn ready. $studio vs $companyName.", "This rivalry is better than most games."), about, LogTone.NEUTRAL, 0.9)
        }
        "copy" -> {
            say(rng, Poster.PLAYER, listOf("$studio saw what worked and made {t}. Classic.", "Is {t} just a clone of what $companyName did?", "$studio copying homework and still shipping."), about, LogTone.BAD, 1.1)
            say(rng, Poster.RIVAL, listOf("Inspiration, not imitation. Our {t} stands on its own.", "We ship what players want. Coincidence."), about, LogTone.NEUTRAL, 0.8)
        }
        "flop" -> {
            say(rng, Poster.MEME, listOf("$studio's {t}: how it started vs how it's going", "Nobody:\n$studio: ship {t} anyway"), about, LogTone.GOOD, 1.2)
            say(rng, Poster.PRESS, listOf("{t} is a rare miss for $studio.", "$studio stumbles with {t}."), about, LogTone.NEUTRAL, 1.0)
        }
        "poach" -> {
            say(rng, Poster.INSIDER, listOf("$studio is circling $companyName talent again.", "Heard $studio made an offer. Names withheld."), about, LogTone.BAD, 1.0)
        }
        "poach_win" -> {
            say(rng, Poster.INSIDER, listOf("$studio landed {t} — big get.", "Talent war: {t} walks to $studio."), about, LogTone.BAD, 1.1)
            say(rng, Poster.PRESS, listOf("{t} joins $studio.", "$studio wins a hiring fight over {t}."), about, LogTone.NEUTRAL, 0.9)
        }
        "poach_fail" -> {
            say(rng, Poster.INSIDER, listOf("$studio's offer for {t} didn't stick.", "{t} stays put. $studio walks away empty."), about, LogTone.GOOD, 1.0)
        }
    }
}

/** Antique IP buy / remake / reboot / meme / hit beats. */
internal fun GameState.feedOnAntique(brand: String, kind: String) {
    val rng = flavour("antique $brand $kind $week")
    when (kind) {
        "buy" -> say(rng, Poster.PRESS, listOf("$companyName just bought the $brand rights.", "Who's reviving $brand? Apparently $companyName."), brand, LogTone.GOOD, 1.1)
        "remake" -> {
            say(rng, Poster.FAN, listOf("$brand remake?????? TAKE MY MONEY", "Please don't mess up $brand", "{tag} $brand remake announced"), brand, LogTone.GOOD, 1.2)
            say(rng, Poster.MEME, listOf("Day 1 $brand remake discourse", "Nostalgia bait detected: $brand"), brand, LogTone.NEUTRAL, 1.0)
        }
        "reboot" -> {
            say(rng, Poster.FAN, listOf("$brand reboot. Bold.", "If they reboot $brand wrong I'm riotting", "New $brand era let's go"), brand, LogTone.NEUTRAL, 1.2)
            say(rng, Poster.MEME, listOf("$brand reboot already has a meme page", "Ah yes, the cursed $brand reboot cycle"), brand, LogTone.BAD, 1.1)
        }
        "meme" -> say(rng, Poster.MEME, listOf("$brand will never recover from this", "The $brand revival is a whole genre of meme now", "Clip it: $brand flop compilation"), brand, LogTone.BAD, 1.4)
        "hit" -> say(rng, Poster.PRESS, listOf("$brand is back — and it works.", "$companyName actually revived $brand properly."), brand, LogTone.GOOD, 1.3)
    }
}

/** Dev diary / silence / roadmap transparency beats. */
internal fun GameState.feedOnTransparency(title: String?, kind: String) {
    val rng = flavour("transparency $kind ${title ?: ""} $week")
    when (kind) {
        "diary" -> say(rng, Poster.FAN, listOf("{s} posted a {t} diary — love to see it", "Open development for {t}? Based", "More studios should diary like {s}"), title, LogTone.GOOD, 0.9)
        "roadmap" -> {
            say(rng, Poster.PLAYER, listOf("{s} dumped the whole {t} roadmap lol", "That's… a lot of promises for {t}", "Wishlisted {t} after that roadmap. Nervous."), title, LogTone.NEUTRAL, 1.2)
            say(rng, Poster.MEME, listOf("Roadmap so long it needs a sequel", "{t} roadmap: how it started vs how it'll end"), title, LogTone.NEUTRAL, 1.0)
        }
        "silence_crisis" -> say(rng, Poster.PLAYER, listOf("{s} radio silence during THIS? Cowardly", "Say something. Anything.", "Transparency metre in the toilet"), null, LogTone.BAD, 1.1)
        "silence" -> say(rng, Poster.INSIDER, listOf("{s} has gone quiet again.", "No updates from {s}. Classic."), null, LogTone.NEUTRAL, 0.6)
    }
}

/** Accessibility / localisation goodwill beats. */
internal fun GameState.feedOnAccessLocal(title: String, kind: String) {
    val rng = flavour("access $kind $title $week")
    when (kind) {
        "access" -> say(rng, Poster.PRESS, listOf("{s} putting real money into {t} accessibility.", "{t} accessibility pass — quiet but important."), title, LogTone.GOOD, 0.9)
        "local" -> say(rng, Poster.FAN, listOf("{t} getting proper localisation? Finally", "New language support for {t} — welcome", "{s} investing in regional {t} markets"), title, LogTone.GOOD, 0.85)
    }
}

/** Tech demo flex on the feed. */
internal fun GameState.feedOnTechDemo(title: String, stage: String) {
    val rng = flavour("techdemo $title $stage $week")
    say(rng, Poster.STREAMER, listOf("That {t} tech demo at $stage was INSANE", "Rewatching the {t} demo. Graphics how?"), title, LogTone.GOOD, 1.4)
    say(rng, Poster.PLAYER, listOf("If {t} looks like that demo I'm sold", "Demo looked CGI. Hope it's real-time.", "Wishlisted {t} purely off the tech demo"), title, LogTone.NEUTRAL, 1.2)
    say(rng, Poster.MEME, listOf("Tech demo ≠ final game challenge (impossible)", "{t} demo: how it started"), title, LogTone.NEUTRAL, 1.0)
}

/** A star enters a new career chapter — secondary to the Team card, but worth a feed beat. */
internal fun GameState.feedOnCareerChapter(name: String, chapter: CareerChapter) {
    val rng = flavour("career $name ${chapter.name} $week")
    val lines = when (chapter) {
        CareerChapter.RISING -> listOf("{t} is one to watch.", "Quiet buzz around {t}.")
        CareerChapter.PEAK -> listOf("{t} is at the top of their game.", "Everyone wants a piece of {t} right now.")
        CareerChapter.FADING -> listOf("Is {t}'s best work behind them?", "{t} isn't the hot hire they used to be.")
        CareerChapter.LEGEND -> listOf("{t} is a legend. Full stop.", "They'll be teaching {t}'s games in schools.")
    }
    say(rng, Poster.PRESS, lines, name, if (chapter == CareerChapter.FADING) LogTone.NEUTRAL else LogTone.GOOD, 1.0)
}

/** Week to week: chatter about what you're making and what's out. */
internal fun GameState.feedWeek() {
    val g = games ?: return
    val rng = flavour("feed week")
    // Hype for what's announced.
    for (team in 1..2) {
        val p = g.projectOf(team) ?: continue
        if (p.reveals == 0 || !rng.chance(0.25 + p.hype / 200)) continue
        say(
            rng, if (rng.chance(0.2)) Poster.INSIDER else Poster.FAN,
            if (p.releaseWeek == null) {
                listOf("Any news on {t}?", "Still thinking about that {t} trailer", "{s}, give us a {t} release date please", "Rumour: {t} has a new trailer coming soon", "My most wanted game right now is {t}")
            } else {
                listOf("{w} weeks until {t}".replace("{w}", "${p.releaseWeek!! - week}"), "Counting down to {t} {tag}", "{t} preload when?", "Pre-ordered {t}. No regrets yet")
            },
            p.title, LogTone.NEUTRAL, 0.8 + p.hype / 60,
        )
    }
    // Players still playing what's out.
    g.releases.firstOrNull { week - it.week in 1..12 }?.let { r ->
        if (rng.chance(0.4)) {
            say(
                rng, Poster.PLAYER,
                if (r.score >= 7) listOf("Still playing {t}. Can't stop.", "Hot take: {t} is underrated", "Just finished {t}. What a ride", "{t} patch when? Not complaining, just want more", "Anyone else replaying {t}?")
                else listOf("Gave {t} another chance. Still no.", "{t} is better after the patch, I'll admit", "Uninstalled {t}"),
                r.title, if (r.score >= 7) LogTone.GOOD else LogTone.BAD, 0.6,
            )
        }
    }
    // Workshop chatter for titles with a mod scene.
    g.releases.firstOrNull { it.hasModScene && it.modBuzz >= 0.3 && week - it.week in 8..200 }?.let { r ->
        if (rng.chance(0.2 + r.modBuzz * 0.25)) {
            val tone = if (r.modHealth >= 0.45) LogTone.GOOD else LogTone.BAD
            say(
                rng, if (rng.chance(0.35)) Poster.FAN else Poster.PLAYER,
                if (r.modHealth >= 0.45) listOf("New {t} total conversion dropped {tag}", "The {t} workshop is stacked this week", "Installing another {t} fix pack", "{t} modders are carrying")
                else listOf("{t} workshop is a mess right now", "Half the {t} mods are broken", "Toxicity in the {t} mod discord is unreal"),
                r.title, tone, 0.7 + r.modBuzz * 0.5,
            )
        }
    }
    // The rest of the industry.
    if (rng.chance(0.3)) {
        g.market.games.lastOrNull { week - it.week <= 2 }?.let { rival ->
            say(
                rng, if (rival.mega) Poster.STREAMER else Poster.PLAYER,
                when {
                    rival.mega -> listOf("Everyone's playing {t} this week and honestly fair", "{t} is going to eat every other game alive", "Taking a week off for {t}")
                    rival.score >= 8 -> listOf("{t} is really good, you should play it", "Didn't expect much from {t}. It's great.")
                    rival.score < 5.5 -> listOf("{t} is rough", "Skip {t}", "How did {t} get made")
                    else -> listOf("{t} is fine", "Playing a bit of {t}. It's ok")
                },
                rival.title, if (rival.score >= 7) LogTone.GOOD else LogTone.NEUTRAL, if (rival.mega) 2.0 else 0.8,
            )
        }
    }
}
