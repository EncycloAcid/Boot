package com.boot.core

import kotlinx.serialization.Serializable

/*
 * The news. Besides your own company's log, the world has a press: the era's trends and the
 * rumours of the next one, rivals' launches, the games everyone's playing and how they reviewed,
 * the markets, and, when you do something worth writing about, stories about you. Each story
 * comes from an outlet that would plausibly run it.
 */

/** Sections of the paper. */
@Serializable
enum class NewsDesk(val title: String) {
    YOU("Your company"),
    INDUSTRY("Industry"),
    GAMES("Games"),
    MARKETS("Markets"),
}

/** What kind of story it is, for the kicker above the headline. */
@Serializable
enum class NewsTag(val title: String) {
    NEWS("News"), REVIEW("Review"), AWARDS("Awards"), SHOWS("Shows"), CHARTS("Charts"),
    RUMOUR("Rumour"), MONEY("Markets"), DELAY("Delay"),
}

/** Enough of a game to draw its cover beside a story. */
@Serializable
data class NewsCover(val title: String, val topic: Topic, val genre: Genre, val size: GameSize = GameSize.MEDIUM)

@Serializable
data class NewsStory(
    val id: Int,
    val week: Int,
    val desk: NewsDesk,
    val outlet: String,
    val headline: String,
    val body: String = "",
    val tone: LogTone = LogTone.NEUTRAL,
    val lane: Lane? = null,
    /** Big enough for the front page. */
    val lead: Boolean = false,
    val tag: NewsTag = NewsTag.NEWS,
    /** Someone's words, with who said them. */
    val quote: String? = null,
    val quoteBy: String? = null,
    val cover: NewsCover? = null,
    /** Readers' comments, and the one with the most likes. */
    val comments: Int = 0,
    val topComment: String? = null,
)

internal fun GameState.publish(
    desk: NewsDesk, outlet: String, headline: String, body: String = "",
    tone: LogTone = LogTone.NEUTRAL, lane: Lane? = null, lead: Boolean = false,
    tag: NewsTag? = null, quote: String? = null, quoteBy: String? = null, cover: NewsCover? = null,
) {
    // In a run set in the past, outlets that didn't exist yet are swapped for ones of the day.
    val byline = if (isDecades) outletIn(outlet, calendarYear) else outlet
    val kind = tag ?: if (desk == NewsDesk.MARKETS) NewsTag.MONEY else NewsTag.NEWS
    val (comments, top) = readersOn(desk, kind, tone, lead, flavour(headline))
    val story = NewsStory(nextId(), week, desk, byline, headline, body, tone, lane, lead, kind, quote, quoteBy, cover, comments, top)
    // Cap at 300: prefer dropping oldest MARKETS/INDUSTRY so GAMES and YOU stick around.
    news = if (news.size >= 300) {
        val dropAt = news.indexOfFirst { it.desk == NewsDesk.MARKETS || it.desk == NewsDesk.INDUSTRY }
            .takeIf { it >= 0 } ?: 0
        news.filterIndexed { i, _ -> i != dropAt } + story
    } else {
        news + story
    }
}

/** How many readers comment on a story, and the comment everyone liked. Comment sections came with the web. */
private fun GameState.readersOn(desk: NewsDesk, tag: NewsTag, tone: LogTone, lead: Boolean, rng: SeededRNG): Pair<Int, String?> {
    if (calendarYear < 1997 || desk == NewsDesk.INDUSTRY && tag == NewsTag.NEWS) return 0 to null
    val fans = games?.fans ?: 0.0
    val base = when (desk) {
        NewsDesk.YOU -> 40.0 + kotlin.math.sqrt(fans) * 2
        NewsDesk.GAMES -> 200.0
        else -> 60.0
    }
    val count = (base * (if (lead) 3.0 else 1.0) * rng.range(0.6, 1.6)).toInt()
    val good = tone != LogTone.BAD
    val pool = when (tag) {
        NewsTag.REVIEW -> if (good) listOf("Called it. Day one buy.", "Those scores are deserved.", "Finally, a studio that cares.", "Reviewers are being generous, but I'm in.", "Already 20 hours in. Agree with every word.", "This is why I trust this outlet.", "Bought it on the strength of this review. No regrets.")
        else listOf("Saw this coming from the trailer.", "Wait for a sale.", "How do you get it this wrong?", "Patch it and maybe.", "Glad I didn't pre-order.", "The trailers lied, again.", "Refunded after two hours.")
        NewsTag.AWARDS -> if (good) listOf("Deserved. No contest.", "That speech though.", "Told you all it was GOTY.") else listOf("Robbed.", "The jury didn't even play it.")
        NewsTag.SHOWS -> listOf("Best trailer of the show.", "Need gameplay, not CGI.", "Wishlisted.", "That reveal got me.", "Release date or it didn't happen.", "Hoping it comes to my platform.", "Already rewatched it three times.")
        NewsTag.DELAY -> listOf("A delayed game is eventually good.", "Again?", "Rather that than a broken launch.", "Take your time.")
        NewsTag.CHARTS -> listOf("Deserved.", "People will buy anything.", "Number one, and still no sequel announced?")
        NewsTag.RUMOUR -> listOf("Source: trust me.", "Please be real.", "Calling it now: shadow drop.", "Every year they say this.")
        NewsTag.MONEY -> listOf("Buy the dip.", "Overvalued.", "Wish I'd bought in years ago.")
        NewsTag.NEWS -> if (good) listOf("Big if true.", "Good for them.", "Love to see it.", "About time.", "This is the way.") else listOf("Yikes.", "This won't end well.", "Not a good look.", "Called it.", "Oof.")
    }
    return count to rng.pick(pool)
}

/** A game's cover, for a story about it. */
internal fun GameRelease.newsCover() = NewsCover(title, concept.topic, concept.genre, concept.size)
internal fun RivalGame.newsCover() = NewsCover(title, topic, genre, size)
internal fun GameProject.newsCover() = NewsCover(title, concept.topic, concept.genre, concept.size)

/** Who'd cover a lane. */
internal fun techOutlet(lane: Lane?, rng: SeededRNG): String = when (lane) {
    Lane.GAMES -> rng.pick(listOf("IGN", "Kotaku", "Polygon", "Eurogamer", "PC Gamer"))
    Lane.WEBSITES -> rng.pick(listOf("Smashing Magazine", "TechCrunch", "Fast Company"))
    Lane.APPS -> rng.pick(listOf("TechCrunch", "The Verge", "9to5Mac"))
    Lane.SOCIAL -> rng.pick(listOf("The Verge", "Wired", "Platformer"))
    Lane.HARDWARE -> rng.pick(listOf("The Verge", "Engadget", "Ars Technica"))
    Lane.AI -> rng.pick(listOf("Wired", "MIT Technology Review", "The Information"))
    Lane.OS -> rng.pick(listOf("Ars Technica", "The Verge", "Wired"))
    null -> rng.pick(listOf("TechCrunch", "The Verge", "Wired"))
}

internal fun GameState.businessOutlet(): String = rng.pick(listOf("Bloomberg", "Reuters", "The Wall Street Journal", "Financial Times", "CNBC"))

/** What an era does to business, in a line: who's up, who's down. */
internal fun GameState.eraEffects(era: Era): String {
    val open = Lane.entries.filter { laneOpen(it) }
    fun names(l: List<Lane>) = l.map { if (it == Lane.AI) "AI" else it.title.lowercase() }.let {
        if (it.size <= 1) it.joinToString() else it.dropLast(1).joinToString(", ") + " and " + it.last()
    }
    val up = open.filter { era.demand(it) >= 1.05 }
    val down = open.filter { era.demand(it) <= 0.95 }
    val parts = listOfNotNull(
        up.takeIf { it.isNotEmpty() }?.let { "Good for ${names(it)}" },
        down.takeIf { it.isNotEmpty() }?.let { (if (up.isEmpty()) "Bad" else "bad") + " for ${names(it)}" },
    )
    return if (parts.isEmpty()) era.title else "${era.title}. " + parts.joinToString("; ") + "."
}

/** Rotating industry colour beyond the era headline pool. */
private val industryLedes = listOf(
    "Hiring war heats up for senior engineers" to
        "Studios and labs are poaching each other with equity, remote perks and signing bonuses. Salaries keep climbing.",
    "Platform install base hits a new high" to
        "One more living-room device crosses a hundred million active users. Developers scramble to ship for it first.",
    "Lawsuit rumor swirls around a major studio" to
        "Anonymous filings and carefully worded denials. Lawyers are busy; nobody will go on the record yet.",
)

/** The world's news for the week: the era, the rumours, and the markets. */
internal fun GameState.newsWeek() {
    val w = world
    // The era's mood, a story every few weeks.
    if (w.era != Era.BOOTSTRAP_YEARS && week % 3 == 0 && rng.chance(0.45)) {
        val pool = when (w.phase) {
            EraPhase.DAWN -> w.era.dawn
            EraPhase.PEAK -> w.era.peak
            EraPhase.FADING -> w.era.fade
        }
        val recent = news.takeLast(30).map { it.headline }.toSet()
        pool.filter { it !in recent }.takeIf { it.isNotEmpty() }?.let {
            val headline = rng.pick(it)
            val body = listOfNotNull(eraEffects(w.era).takeIf { b -> b.isNotBlank() }, w.era.blurb.takeIf { b -> b.isNotBlank() })
                .distinct().joinToString(" ")
                .ifBlank { "${w.era.title} is reshaping who gets funded and who gets ignored." }
            publish(
                NewsDesk.INDUSTRY,
                rng.pick(listOf("The Economist", "Wired", "Bloomberg", "The Verge", "Financial Times")),
                headline,
                body,
            )
        }
    }
    // Occasional industry ledes: hiring wars, platform install bases, lawsuit rumors.
    if (week > 0 && week % 5 == 0 && rng.chance(0.35)) {
        val recent = news.takeLast(20).map { it.headline }.toSet()
        industryLedes.filter { it.first !in recent }.takeIf { it.isNotEmpty() }?.let { pool ->
            val (headline, body) = rng.pick(pool)
            publish(
                NewsDesk.INDUSTRY,
                rng.pick(listOf("The Verge", "Bloomberg", "Wired", "Reuters")),
                headline,
                body,
            )
        }
    }
    // Every quarter, the markets.
    if (week % GameClock.WEEKS_PER_QUARTER == 0 && week > 0) {
        val table = rankings()
        val top = table.first()
        publish(
            NewsDesk.MARKETS, businessOutlet(),
            if (top.mine) "$companyName is the most valuable company in the world" else "${top.name} stays the world's most valuable company",
            "Worth ${money(top.value)}. ${table.getOrNull(1)?.let { "${it.name} is second at ${money(it.value)}." } ?: ""}",
            lead = top.mine,
        )
        val hot = w.era.hotLanes.firstOrNull()
        val cold = w.era.coldLanes.firstOrNull()
        if (hot != null || cold != null) {
            publish(
                NewsDesk.MARKETS, businessOutlet(),
                listOfNotNull(
                    hot?.let { "Investors pile into ${it.title.lowercase()}" },
                    cold?.let { "${it.title} stocks slide" },
                ).joinToString("; "),
                "${w.era.title}: ${w.era.blurb}",
            )
        }
        val me = table.indexOfFirst { it.mine } + 1
        if (me in 1..20 && !top.mine) {
            publish(NewsDesk.MARKETS, businessOutlet(), "$companyName is now #$me among tech companies", "Worth ${money(table[me - 1].value)}.", LogTone.GOOD)
        }
    }
}

/** Every four weeks, the games charts, with where you stand. */
internal fun GameState.chartsStory() {
    val g = games ?: return
    if (week % 4 != 0 || week == 0) return
    val chart = g.market.chart
    val top = chart.firstOrNull() ?: return
    val mine = chart.indexOfFirst { it.mine } + 1
    val rng = flavour("charts")
    val topGame = g.market.games.firstOrNull { it.title == top.title }?.newsCover() ?: g.releases.firstOrNull { it.title == top.title }?.newsCover()
    publish(
        NewsDesk.GAMES, techOutlet(Lane.GAMES, rng),
        if (top.mine) "$companyName's ${top.title} is No. 1 in the charts" else "The charts: ${top.title} is No. 1",
        chart.take(3).mapIndexed { i, e -> "${i + 1}. ${e.title} (${e.studio})" }.joinToString(" · ") +
            if (mine > 3) ". Your ${chart[mine - 1].title} is at No. $mine." else "",
        if (top.mine) LogTone.GOOD else LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.CHARTS, cover = topGame,
    )
}

/** Stories newer than [seenId], for the unread count. */
fun GameState.unreadNews(seenId: Int): Int = news.count { it.id > seenId }
