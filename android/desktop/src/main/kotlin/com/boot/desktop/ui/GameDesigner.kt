package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Audience
import com.boot.core.FocusArea
import com.boot.core.Format
import com.boot.core.GameConcept
import com.boot.core.GameSeason
import com.boot.core.GameSize
import com.boot.core.GameState
import com.boot.core.Genre
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.conceptBlocker
import com.boot.core.estimatedWeeks
import com.boot.core.gameTitle
import com.boot.core.platformPlayers
import com.boot.core.platformsOnSale
import com.boot.core.startGame
import com.boot.core.unlockedSizes
import com.boot.desktop.DesktopStore

private val steps = listOf("Concept", "Platform & size", "Focus", "Summary")

/**
 * Slim Game Dev Tycoon–style designer: topic/genre → platform/size → stage focus → start.
 * Same core [startGame] path as Android; Desktop chrome only.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDesigner(store: DesktopStore, state: GameState) {
    val g = state.games
    if (g == null) {
        store.closeDesigner()
        return
    }
    val seed = store.draft
    val team = store.designTeam
    val h = hudColors
    val colors = gameColors
    var step by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf(seed?.topic ?: g.topics.first()) }
    var genre by remember { mutableStateOf(seed?.genre ?: Genre.ACTION) }
    val salt = remember { state.week * 13L + g.releases.size }
    var reroll by remember { mutableIntStateOf(0) }
    var typed by remember { mutableStateOf(false) }
    fun suggested(): String = gameTitle(topic, genre, salt + reroll, g.releases.map { it.title }.toSet())
    var title by remember { mutableStateOf(seed?.title ?: suggested()) }
    var genre2 by remember { mutableStateOf(seed?.genre2) }
    var audience by remember { mutableStateOf(seed?.audience ?: Audience.EVERYONE) }
    val platforms = remember {
        mutableStateListOf<Platform>().apply { addAll(seed?.platforms ?: listOf(Platform.PC)) }
    }
    var size by remember { mutableStateOf(seed?.size ?: GameSize.SMALL) }
    var engineId by remember { mutableIntStateOf(seed?.engineId ?: g.engines.maxBy { it.level }.id) }
    val focus = remember { List(3) { mutableStateListOf(0.5, 0.5, 0.5) } }

    val concept = GameConcept(
        title = title.trim().ifEmpty { "Untitled" },
        topic = topic,
        genre = genre,
        genre2 = genre2,
        audience = audience,
        platforms = platforms.toList(),
        size = size,
        engineId = engineId,
        sequelOf = seed?.sequelOf,
        remasterOf = seed?.remasterOf,
        price = seed?.price ?: com.boot.core.PriceTier.STANDARD,
        live = seed?.live == true && size != GameSize.SMALL,
    )
    val season = GameSeason.forWeek(state.week)
    val seasonLift = season.launchMultiplier(genre)
    val blocker = state.conceptBlocker(concept, team = team)

    fun close() = store.closeDesigner()

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 420.dp, max = 640.dp)
                    .fillMaxWidth(0.62f)
                    .heightIn(min = 360.dp, max = 720.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (team == 2) "Design a game for Team 2" else "Design a game",
                            style = MaterialTheme.typography.titleLarge,
                            color = h.ink,
                        )
                        Hint("Step ${step + 1} of ${steps.size} · ${steps[step]}")
                    }
                    MacButton("Close", onClick = { close() }, primary = false)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (step) {
                        0 -> {
                            GlassPanel {
                                Text("Title", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                BasicTextField(
                                    value = title,
                                    onValueChange = { title = it; typed = true },
                                    textStyle = MaterialTheme.typography.titleMedium.copy(color = h.ink),
                                    cursorBrush = SolidColor(h.accent),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                        .border(1.dp, h.panelBorder, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                )
                                MacButton(
                                    "Another name",
                                    onClick = { reroll += 1; typed = false; title = suggested() },
                                    primary = false,
                                )
                            }
                            GlassPanel {
                                Text("Topic", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    g.topics.sortedBy { it.title }.forEach { t ->
                                        SelectChip(t.title, t == topic) {
                                            topic = t
                                            if (!typed) title = suggested()
                                        }
                                    }
                                }
                            }
                            GlassPanel {
                                Text("Genre", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                val fitHint = g.knownFit(topic, genre)?.let {
                                    "${topic.title} + ${genre.title}: ${it.title.lowercase()}."
                                }
                                val seasonHint = when {
                                    seasonLift > 1.0 -> "${season.title}: ${genre.title} launches get a demand boost."
                                    seasonLift < 1.0 -> "${season.title}: ${genre.title} launches are slightly soft."
                                    else -> null
                                }
                                Hint(listOfNotNull(fitHint, seasonHint).joinToString(" ").ifBlank { season.marketTip })
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Genre.entries.forEach { gn ->
                                        SelectChip(gn.title, gn == genre) {
                                            genre = gn
                                            if (genre2 == gn) genre2 = null
                                            if (!typed) title = suggested()
                                        }
                                    }
                                }
                            }
                            if (Research.MULTI_GENRE in state.researched) {
                                GlassPanel {
                                    Text("Second genre", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        SelectChip("None", genre2 == null) { genre2 = null }
                                        Genre.entries.filter { it != genre }.forEach { gn ->
                                            SelectChip(gn.title, gn == genre2) { genre2 = gn }
                                        }
                                    }
                                }
                            }
                            if (Research.TARGET_AUDIENCE in state.researched) {
                                GlassPanel {
                                    Text("Audience", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Audience.entries.forEach { a ->
                                            SelectChip(a.title, audience == a) { audience = a }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            GlassPanel {
                                Text("Platforms", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    state.platformsOnSale.forEach { p ->
                                        val on = p in platforms
                                        SelectChip(
                                            "${p.title} · ${Format.count(state.platformPlayers(p))}",
                                            on,
                                        ) {
                                            if (on) {
                                                if (platforms.size > 1) platforms.remove(p)
                                            } else if (platforms.size < 3) {
                                                platforms.add(p)
                                            }
                                        }
                                    }
                                }
                            }
                            GlassPanel {
                                Text("Size", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.unlockedSizes.forEach { s ->
                                        SelectChip(
                                            "${s.title} · ~${state.estimatedWeeks(s)}w",
                                            size == s,
                                        ) { size = s }
                                    }
                                }
                                Hint("Budget about ${Format.money(size.weeklyBudget)}/week while in development.")
                            }
                            if (g.engines.size > 1) {
                                GlassPanel {
                                    Text("Engine", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        g.engines.forEach { eng ->
                                            SelectChip("Lv ${eng.level} · ${eng.name}", eng.id == engineId) {
                                                engineId = eng.id
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Hint("Where the hours go in each stage. You can rebalance later in Lanes.")
                            for (stage in 0..2) {
                                val areas = FocusArea.forStage(stage)
                                GlassPanel {
                                    Text(FocusArea.STAGES[stage], style = MaterialTheme.typography.titleMedium, color = h.accent)
                                    areas.forEachIndexed { i, area ->
                                        val alloc = focus[stage]
                                        val total = alloc.sum().takeIf { it > 0 } ?: 1.0
                                        CommitSlider(
                                            alloc[i],
                                            { v ->
                                                val share = v.toDouble() / (total - alloc[i] + v.toDouble()).coerceAtLeast(0.01)
                                                "${area.title} · ${Format.percent(share)}"
                                            },
                                        ) { v -> alloc[i] = v.coerceIn(0.05, 1.0) }
                                    }
                                }
                            }
                        }
                        else -> {
                            GlassPanel {
                                Text(concept.title, style = MaterialTheme.typography.headlineMedium, color = h.ink)
                                Hint(
                                    "${concept.size.title} ${concept.topic.title.lowercase()} ${concept.genreLower} · " +
                                        concept.platforms.joinToString { it.title },
                                )
                                Hint("~${state.estimatedWeeks(concept)} weeks with today's team")
                                when {
                                    seasonLift > 1.0 -> Hint("${season.title} favors ${genre.title} launches.", colors.good)
                                    seasonLift < 1.0 -> Hint("${season.title} is soft for ${genre.title}.", colors.warn)
                                    else -> Hint(season.marketTip)
                                }
                            }
                        }
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F6F8))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if ((step == 1 || step == steps.lastIndex) && blocker != null) {
                        Hint(blocker, colors.bad)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (step > 0) {
                            MacButton("Back", onClick = { step -= 1 }, primary = false)
                        }
                        Spacer(Modifier.weight(1f))
                        if (step < steps.lastIndex) {
                            val nextOk = when (step) {
                                1 -> platforms.isNotEmpty() && blocker == null
                                else -> true
                            }
                            MacButton("Next", onClick = { step += 1 }, enabled = nextOk)
                        } else {
                            store.error?.let { Hint(it, colors.bad) }
                            MacButton(
                                "Start development",
                                onClick = {
                                    store.dismissError()
                                    val split = focus.map { it.toMutableList() }
                                    store.perform { startGame(concept, split, team) }
                                    if (store.state?.games?.projectOf(team) != null) close()
                                },
                                enabled = blocker == null,
                            )
                        }
                    }
                }
            }
        }
    }
}
