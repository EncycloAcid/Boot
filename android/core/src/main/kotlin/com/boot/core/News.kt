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
)

internal fun GameState.publish(
    desk: NewsDesk, outlet: String, headline: String, body: String = "",
    tone: LogTone = LogTone.NEUTRAL, lane: Lane? = null, lead: Boolean = false,
) {
    // In a run set in the past, outlets that didn't exist yet are swapped for ones of the day.
    val byline = if (isDecades) outletIn(outlet, calendarYear) else outlet
    val story = NewsStory(nextId(), week, desk, byline, headline, body, tone, lane, lead)
    news = (if (news.size >= 300) news.drop(news.size - 299) else news) + story
}

/** Who'd cover a lane. */
internal fun techOutlet(lane: Lane?, rng: SeededRNG): String = when (lane) {
    Lane.GAMES -> rng.pick(listOf("IGN", "Kotaku", "Polygon", "Eurogamer", "PC Gamer"))
    Lane.WEBSITES -> rng.pick(listOf("Smashing Magazine", "TechCrunch", "Fast Company"))
    Lane.APPS -> rng.pick(listOf("TechCrunch", "The Verge", "9to5Mac"))
    Lane.SOCIAL -> rng.pick(listOf("The Verge", "Wired", "Platformer"))
    Lane.HARDWARE -> rng.pick(listOf("The Verge", "Engadget", "Ars Technica"))
    Lane.AI -> rng.pick(listOf("Wired", "MIT Technology Review", "The Information"))
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
            publish(NewsDesk.INDUSTRY, rng.pick(listOf("The Economist", "Wired", "Bloomberg", "The Verge", "Financial Times")), rng.pick(it), eraEffects(w.era))
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

/** Stories newer than [seenId], for the unread count. */
fun GameState.unreadNews(seenId: Int): Int = news.count { it.id > seenId }
