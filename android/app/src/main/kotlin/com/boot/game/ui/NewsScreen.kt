package com.boot.game.ui

import androidx.compose.ui.draw.clip
import com.boot.core.trending
import com.boot.core.feedName
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontStyle
import com.boot.core.Platform
import com.boot.core.GameConcept
import com.boot.core.NewsCover
import com.boot.core.NewsTag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.LogTone
import com.boot.core.NewsDesk
import com.boot.core.NewsStory
import com.boot.core.rankings
import com.boot.game.GameStore

/** What the paper shows: the front page, one desk, or your own company's log. */
private enum class Page(val title: String) { FRONT("Front page"), SOCIAL("Social"), YOU("Your company"), INDUSTRY("Industry"), GAMES("Games"), MARKETS("Markets"), LOG("Log") }

@Composable
fun NewsScreen(store: GameStore, state: GameState) {
    var page by rememberSaveable { mutableStateOf(Page.FRONT) }
    var filter by rememberSaveable { mutableStateOf<Lane?>(null) }
    // Opening the paper marks everything as read.
    LaunchedEffect(state.news.lastOrNull()?.id) { state.news.lastOrNull()?.let { store.newsSeen = maxOf(store.newsSeen, it.id) } }
    val stories = state.news.asReversed()
    ScreenColumn {
        item { Masthead(state) }
        item {
            ChipRow(Page.entries.filter { (it != Page.GAMES && it != Page.SOCIAL) || state.games != null }, page, { if (it == Page.SOCIAL) state.feedName else it.title }, tag = "desks") { page = it }
        }
        when (page) {
            Page.FRONT -> {
                item { EraCard(state) }
                // The lead: the biggest recent story, but not the era's own, which the card above already tells.
                val eraStory = { s: NewsStory -> s.desk == NewsDesk.INDUSTRY && s.body == state.world.era.blurb }
                val lead = stories.firstOrNull { it.lead && state.week - it.week <= 26 && !eraStory(it) }
                    ?: stories.firstOrNull { !eraStory(it) }
                // The lead, with what people are saying beside it when there's room (under it when there isn't).
                val posts = state.feed.asReversed().filter { it.likes >= 20 || it.kind == com.boot.core.Poster.YOU }.take(3)
                if (lead != null || posts.isNotEmpty()) item {
                    androidx.compose.foundation.layout.BoxWithConstraints {
                        val feed: @Composable (Modifier) -> Unit = { m ->
                            if (posts.isNotEmpty()) Column(m) { FeedSection("On ${state.feedName}", state, posts, onMore = { page = Page.SOCIAL }) }
                        }
                        if (maxWidth >= 640.dp && lead != null && posts.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1.3f)) { LeadStory(state, lead) }
                                feed(Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                lead?.let { LeadStory(state, it) }
                                feed(Modifier)
                            }
                        }
                    }
                }
                val rest = stories.filter { it.id != lead?.id && !eraStory(it) }.take(12)
                if (rest.isNotEmpty()) item { StoryList("Latest", state, rest) }
                if (stories.isEmpty()) item { GroupedSection(null) { CardBlock { Hint("No news yet. The press hasn't noticed you.") } } }
            }
            Page.SOCIAL -> {
                val tags = state.trending()
                if (tags.isNotEmpty()) item { Trending(tags) }
                val posts = state.feed.asReversed().take(60)
                if (posts.isEmpty()) {
                    item { GroupedSection(null) { CardBlock { Hint("Nobody's talking about you yet. Announce something.") } } }
                } else {
                    item { FeedSection(null, state, posts) }
                }
            }
            Page.YOU -> desk(state, stories.filter { it.desk == NewsDesk.YOU }, "Nobody's written about you yet. Ship something, raise money, make a splash.")
            Page.INDUSTRY -> desk(state, stories.filter { it.desk == NewsDesk.INDUSTRY }, "The era's trends, rivals' launches and the rumours.")
            Page.GAMES -> desk(state, stories.filter { it.desk == NewsDesk.GAMES }, "Big releases, reviews, esports and the awards.")
            Page.MARKETS -> {
                item { MarketsBoard(state) }
                desk(state, stories.filter { it.desk == NewsDesk.MARKETS }, "Every quarter: who's worth what.")
            }
            Page.LOG -> {
                item {
                    ChipRow(listOf<Lane?>(null) + Lane.entries.filter { it in state.lanes }, filter, { it?.let { l -> "${l.emoji} ${l.short}" } ?: "Everything" }) { filter = it }
                }
                item {
                    val entries = state.log.asReversed().filter { filter == null || it.lane == filter }.take(200)
                    GroupedSection("Company log", footer = "Everything that's happened to you, week by week.") {
                        if (entries.isEmpty()) CardBlock { Hint("Nothing yet.") }
                        entries.forEachIndexed { i, entry ->
                            if (i > 0) RowDivider(36.dp)
                            LogRow(entry)
                        }
                    }
                }
            }
        }
    }
}

/** A row of chips that scrolls edge to edge, past the screen's side margins. */
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, tag: String = "", onPick: (T) -> Unit) {
    LazyRow(
        modifier = Modifier.bleed(16.dp).testTag(tag),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { o -> FilterChip(selected = o == selected, onClick = { onPick(o) }, label = { Text(label(o), maxLines = 1) }) }
    }
}

/** Widen past the parent's horizontal padding, so a scrolling row runs to the screen's edges. */
private fun Modifier.bleed(by: Dp): Modifier = layout { measurable, constraints ->
    val px = by.roundToPx()
    val placeable = measurable.measure(constraints.copy(minWidth = constraints.minWidth + 2 * px, maxWidth = constraints.maxWidth + 2 * px))
    layout(placeable.width - 2 * px, placeable.height) { placeable.place(-px, 0) }
}

/** How long ago, in plain words, for grouping a desk's stories. */
private fun ageGroup(weeksAgo: Int): String = when {
    weeksAgo <= 0 -> "This week"
    weeksAgo <= 4 -> "This month"
    weeksAgo <= 13 -> "The last three months"
    weeksAgo <= 52 -> "This past year"
    else -> "Older"
}

/** One desk: its stories in sections by age, newest first. */
private fun LazyListScope.desk(state: GameState, stories: List<NewsStory>, about: String) {
    if (stories.isEmpty()) {
        item { GroupedSection(null) { CardBlock { Hint("Nothing yet. $about") } } }
        return
    }
    val groups = stories.take(60).groupBy { ageGroup(state.week - it.week) }.entries.toList()
    groups.forEachIndexed { i, (title, list) ->
        item(key = "desk-$title") { StoryList(title, state, list, footer = if (i == groups.lastIndex) about else null) }
    }
}

/** The paper's name, the date and the era. */
@Composable
private fun Masthead(state: GameState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "The Daily Byte", style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface)
        FitText(
            "${state.dateLabel.uppercase()} · ${state.world.era.title.uppercase()}",
            MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** The front page's big story. */
@Composable
private fun LeadStory(state: GameState, s: NewsStory) {
    GroupedSection(null) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Kicker(s)
                Byline(state, s)
                Text(s.headline, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                if (s.body.isNotEmpty()) Text(s.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                QuoteBlock(s)
                Comments(s)
            }
            s.cover?.let { StoryCover(it, 104.dp) }
        }
    }
}

@Composable
private fun StoryList(title: String, state: GameState, stories: List<NewsStory>, footer: String? = null) {
    GroupedSection(title, footer = footer) {
        stories.forEachIndexed { i, s ->
            if (i > 0) RowDivider(16.dp)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Kicker(s)
                    Byline(state, s)
                    Text(s.headline, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                    // A run of stories with the same standfirst (the era's, say) only needs it once.
                    if (s.body.isNotEmpty() && s.body != stories.getOrNull(i - 1)?.body) Text(s.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    QuoteBlock(s, small = true)
                    Comments(s)
                }
                s.cover?.let { StoryCover(it, 52.dp) }
            }
        }
    }
}

/** What kind of story it is, above the byline: a review, the awards, a rumour. Plain news has none. */
@Composable
private fun Kicker(s: NewsStory) {
    if (s.tag == NewsTag.NEWS) return
    val colors = gameColors
    val tint = when (s.tag) {
        NewsTag.REVIEW -> MaterialTheme.colorScheme.primary
        NewsTag.AWARDS -> colors.milestone
        NewsTag.SHOWS -> colors.warn
        NewsTag.CHARTS, NewsTag.MONEY -> colors.good
        NewsTag.DELAY -> colors.bad
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        s.tag.title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = tint,
        fontStyle = if (s.tag == NewsTag.RUMOUR) FontStyle.Italic else FontStyle.Normal,
    )
}

/** Someone's words, set off with a rule. */
@Composable
private fun QuoteBlock(s: NewsStory, small: Boolean = false) {
    val quote = s.quote ?: return
    Row(Modifier.padding(top = 4.dp).height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(gameColors.milestone, RoundedCornerShape(2.dp)))
        Column(Modifier.padding(start = 10.dp)) {
            Text(
                "“$quote”", style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
            )
            s.quoteBy?.let { Text("— $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** How many readers had something to say, and the comment with the most likes. */
@Composable
private fun Comments(s: NewsStory) {
    if (s.comments <= 0) return
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${Format.count(s.comments.toDouble())} comments", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        s.topComment?.let {
            Text(
                " · “$it”", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StoryCover(c: NewsCover, width: Dp) {
    GameCover(GameConcept(c.title, c.topic, c.genre, platforms = listOf(Platform.PC), size = c.size, collects = c.collects), width)
}

/** Outlet and when, coloured by the desk. The date only shows once it's more than a year ago. */
@Composable
private fun Byline(state: GameState, s: NewsStory) {
    val colors = gameColors
    val deskColor = when (s.desk) {
        NewsDesk.YOU -> MaterialTheme.colorScheme.primary
        NewsDesk.INDUSTRY -> colors.milestone
        NewsDesk.GAMES -> colors.lane(Lane.GAMES)
        NewsDesk.MARKETS -> colors.good
    }
    val ago = state.week - s.week
    val time = when {
        ago <= 0 -> "This week"
        ago == 1 -> "Last week"
        ago <= 52 -> "${Format.weeks(ago)} ago"
        else -> GameClock.label(s.week)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (s.tone == LogTone.BAD) {
            Box(Modifier.size(6.dp).background(colors.bad, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        CompanyLogo(s.outlet, size = 18.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            s.outlet.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = deskColor,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            " · $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        )
    }
}

/** The biggest companies right now, you included if you're there. */
@Composable
private fun MarketsBoard(state: GameState) {
    val table = state.rankings()
    val me = table.indexOfFirst { it.mine }
    GroupedSection("The biggest companies") {
        table.take(5).forEachIndexed { i, c ->
            if (i > 0) RowDivider(48.dp)
            ListRow(
                c.name, subtitle = c.lane?.title,
                leading = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp))
                        if (!c.mine) {
                            Spacer(Modifier.width(6.dp))
                            CompanyLogo(c.name, size = 28.dp)
                        }
                    }
                },
                value = Format.money(c.value),
                valueColor = if (c.mine) MaterialTheme.colorScheme.primary else Color.Unspecified,
            )
        }
        if (me >= 5) {
            RowDivider(48.dp)
            ListRow(
                table[me].name, subtitle = "You",
                leading = { Text("${me + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp)) },
                value = Format.money(table[me].value), valueColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The latest three stories, for the office. */
@Composable
fun HeadlinesCard(store: GameStore, state: GameState) {
    val stories = state.news.asReversed().take(3)
    if (stories.isEmpty()) return
    GroupedSection("Headlines", action = { PlainButton("Read the paper", { store.tab = com.boot.game.Tab.NEWS }) }) {
        stories.forEachIndexed { i, s ->
            if (i > 0) RowDivider(16.dp)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Byline(state, s)
                Text(s.headline, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


/** Posts, newest first, the way the feed shows them. */
@Composable
private fun FeedSection(title: String?, state: GameState, posts: List<com.boot.core.Post>, onMore: (() -> Unit)? = null) {
    GroupedSection(title, action = onMore?.let { { PlainButton("See all", it) } }) {
        posts.forEachIndexed { i, p ->
            if (i > 0) RowDivider(68.dp)
            PostRow(state, p)
        }
    }
}

/** One post: who, when, what they said, and how far it went. */
@Composable
private fun PostRow(state: GameState, p: com.boot.core.Post) {
    val colors = gameColors
    val tint = when (p.kind) {
        com.boot.core.Poster.YOU -> MaterialTheme.colorScheme.primary
        com.boot.core.Poster.STREAMER -> Color(0xFF9146FF)
        com.boot.core.Poster.PRESS -> colors.milestone
        com.boot.core.Poster.INSIDER -> colors.warn
        com.boot.core.Poster.RIVAL -> colors.lane(Lane.GAMES)
        com.boot.core.Poster.MEME -> Color(0xFFE5484D)
        else -> Color(0xFF6B7A90).let { base -> listOf(base, Color(0xFF2E9E8F), Color(0xFFC7772E), Color(0xFF5B7BFF), Color(0xFFB04BFF))[Math.floorMod(p.handle.hashCode(), 5)] }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (p.kind == com.boot.core.Poster.YOU) {
            CompanyLogo(state.companyName, size = 40.dp)
        } else {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                Text(p.author.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "?" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (p.kind in setOf(com.boot.core.Poster.YOU, com.boot.core.Poster.STREAMER, com.boot.core.Poster.PRESS)) {
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(14.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                        Text("✓", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
            Text(
                "${p.handle} · ${ago(state, p.week)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(p.text, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${Format.count(p.replies.toDouble())} replies · ${Format.count(p.reposts.toDouble())} reposts · ${Format.count(p.likes.toDouble())} likes",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The tags everyone's using this month. */
@Composable
private fun Trending(tags: List<String>) {
    GroupedSection("Trending") {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEachIndexed { i, t ->
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(gameColors.fill).padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("${i + 1} · Trending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(t, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun ago(state: GameState, week: Int): String {
    val n = state.week - week
    return when {
        n <= 0 -> "this week"
        n == 1 -> "1w"
        n < 52 -> "${n}w"
        else -> "${n / 52}y"
    }
}
