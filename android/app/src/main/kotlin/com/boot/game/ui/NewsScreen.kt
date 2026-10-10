package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
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
private enum class Page(val title: String) { FRONT("Front page"), YOU("Your company"), INDUSTRY("Industry"), GAMES("Games"), MARKETS("Markets"), LOG("Log") }

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
            LazyRow(contentPadding = PaddingValues(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Page.entries.filter { it != Page.GAMES || state.games != null }) { p ->
                    FilterChip(selected = page == p, onClick = { page = p }, label = { Text(p.title) })
                }
            }
        }
        when (page) {
            Page.FRONT -> {
                item { EraCard(state) }
                val lead = stories.firstOrNull { it.lead && state.week - it.week <= 26 } ?: stories.firstOrNull()
                if (lead != null) item { LeadStory(state, lead) }
                val rest = stories.filter { it.id != lead?.id }.take(12)
                if (rest.isNotEmpty()) item { StoryList("Latest", state, rest) }
                if (stories.isEmpty()) item { GroupedSection(null) { CardBlock { Hint("No news yet. The press hasn't noticed you.") } } }
            }
            Page.YOU -> item {
                val mine = stories.filter { it.desk == NewsDesk.YOU }
                if (mine.isEmpty()) {
                    GroupedSection("Your company") { CardBlock { Hint("Nobody's written about you yet. Ship something, raise money, make a splash.") } }
                } else {
                    StoryList("Your company in the press", state, mine.take(40))
                }
            }
            Page.INDUSTRY -> item { DeskList("Industry", state, stories.filter { it.desk == NewsDesk.INDUSTRY }, "The era's trends, rivals' launches and the rumours.") }
            Page.GAMES -> item { DeskList("Games", state, stories.filter { it.desk == NewsDesk.GAMES }, "Big releases, reviews, esports and the awards.") }
            Page.MARKETS -> {
                item { DeskList("Markets", state, stories.filter { it.desk == NewsDesk.MARKETS }, "Every quarter: who's worth what.") }
                item { MarketsBoard(state) }
            }
            Page.LOG -> {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Everything") }) }
                        items(Lane.entries.filter { it in state.lanes }) { lane ->
                            FilterChip(selected = filter == lane, onClick = { filter = lane }, label = { Text("${lane.emoji} ${lane.short}") })
                        }
                    }
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
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Byline(state, s)
            Text(s.headline, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            if (s.body.isNotEmpty()) Text(s.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeskList(title: String, state: GameState, stories: List<NewsStory>, empty: String) {
    if (stories.isEmpty()) {
        GroupedSection(title) { CardBlock { Hint("Nothing yet. $empty") } }
    } else {
        StoryList(title, state, stories.take(40), footer = empty)
    }
}

@Composable
private fun StoryList(title: String, state: GameState, stories: List<NewsStory>, footer: String? = null) {
    GroupedSection(title, footer = footer) {
        stories.forEachIndexed { i, s ->
            if (i > 0) RowDivider(16.dp)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Byline(state, s)
                Text(s.headline, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                if (s.body.isNotEmpty()) Text(s.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Outlet and when, coloured by the desk. */
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.outlet.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = deskColor)
        Spacer(Modifier.width(6.dp))
        Text(
            (if (ago <= 0) "this week" else "${Format.weeks(ago)} ago") + " · ${GameClock.label(s.week)}",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (s.tone == LogTone.BAD) {
            Spacer(Modifier.width(6.dp))
            Pill("Bad news", colors.bad)
        }
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
                leading = { Text("${i + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp)) },
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
