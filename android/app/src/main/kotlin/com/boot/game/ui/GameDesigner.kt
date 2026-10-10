package com.boot.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Audience
import com.boot.core.FocusArea
import com.boot.core.Format
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.GameState
import com.boot.core.Genre
import com.boot.core.Platform
import com.boot.core.PriceTier
import com.boot.core.Research
import com.boot.core.conceptBlocker
import com.boot.core.estimatedWeeks
import com.boot.core.gameTitle
import com.boot.core.platformPlayers
import com.boot.core.platformsOnSale
import com.boot.core.startGame
import com.boot.core.unlockedSizes
import com.boot.game.GameStore
import java.util.Locale

private val steps = listOf("Concept", "Platform & size", "Focus", "Summary")

/**
 * Designing a game, Game Dev Tycoon style: concept, then platform and size, then how the team
 * splits its effort in each stage, then a last look before development starts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameDesigner(store: GameStore, state: GameState) {
    val g = state.games ?: return
    val seed = store.draft
    val colors = gameColors
    var step by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf(seed?.topic ?: g.topics.first()) }
    var genre by remember { mutableStateOf(seed?.genre ?: Genre.ACTION) }
    val sequel = seed?.sequelOf
    val seriesGenre = sequel?.let { g.release(it)?.concept?.genre }
    val salt = remember { state.week * 13L + g.releases.size }
    var reroll by remember { mutableIntStateOf(0) }
    // A name the player typed stays put; a suggested one follows the topic and genre.
    var typed by remember { mutableStateOf(false) }
    fun suggested(): String = if (sequel != null) {
        g.nextTitle(sequel, genre, reroll)
    } else {
        gameTitle(topic, genre, salt + reroll, g.releases.map { it.title }.toSet())
    }
    var title by remember { mutableStateOf(seed?.title ?: suggested()) }
    var genre2 by remember { mutableStateOf(seed?.genre2) }
    var audience by remember { mutableStateOf(seed?.audience ?: Audience.EVERYONE) }
    val platforms = remember { mutableStateListOf<Platform>().apply { addAll(seed?.platforms ?: listOf(Platform.PC)) } }
    var size by remember { mutableStateOf(seed?.size ?: GameSize.SMALL) }
    var engineId by remember { mutableIntStateOf(seed?.engineId ?: g.engines.maxBy { it.level }.id) }
    var price by remember { mutableStateOf(seed?.price ?: PriceTier.STANDARD) }
    val focus = remember { List(3) { mutableStateListOf(1.0, 1.0, 1.0) } }

    val concept = GameConcept(
        title = title.trim().ifEmpty { "Untitled" }, topic = topic, genre = genre, genre2 = genre2, audience = audience,
        platforms = platforms.toList(), size = size, engineId = engineId, sequelOf = sequel, price = price,
    )
    fun close() {
        store.designing = false
        store.draft = null
    }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { close() }) { Icon(Icons.Filled.Close, "Close") }
                    Column(Modifier.weight(1f)) {
                        val heading = when {
                            sequel == null -> "Design a game"
                            genre != seriesGenre -> "Design a spin-off"
                            else -> "Design a sequel"
                        }
                        Text(heading, style = MaterialTheme.typography.titleMedium)
                        Hint("Step ${step + 1} of ${steps.size} · ${steps[step]}")
                    }
                }
                LinearWavyProgressIndicator(
                    progress = { (step + 1f) / steps.size },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(motion.defaultSpatialSpec()) { it / 6 * dir } + fadeIn()) togetherWith
                            (slideOutHorizontally(motion.fastSpatialSpec()) { -it / 6 * dir } + fadeOut())
                    },
                    modifier = Modifier.weight(1f),
                    label = "step",
                ) { s ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        when (s) {
                            0 -> ConceptStep(
                                state,
                                title, { title = it; typed = true }, { reroll += 1; typed = false; title = suggested() },
                                topic, { topic = it; if (!typed) title = suggested() },
                                genre, { genre = it; if (genre2 == it) genre2 = null; if (!typed) title = suggested() },
                                genre2, { genre2 = it }, audience, { audience = it }, seriesGenre,
                            )
                            1 -> PlatformStep(state, genre, audience, platforms, size, { size = it }, engineId, { engineId = it })
                            2 -> FocusStep(state, concept, focus)
                            else -> SummaryStep(state, concept) { price = it }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.bar)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (step > 0) PlainButton("Back", { step -= 1 })
                    Spacer(Modifier.weight(1f))
                    if (step < steps.lastIndex) {
                        PrimaryButton("Next", { step += 1 }, enabled = platforms.isNotEmpty())
                    } else {
                        val blocker = state.conceptBlocker(concept)
                        PrimaryButton("Start development", {
                            val split = focus.map { it.toMutableList() }
                            store.perform { startGame(concept, split) }
                            if (store.state?.games?.project != null) close()
                        }, enabled = blocker == null)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConceptStep(
    state: GameState,
    title: String, onTitle: (String) -> Unit, onReroll: () -> Unit,
    topic: com.boot.core.Topic, onTopic: (com.boot.core.Topic) -> Unit,
    genre: Genre, onGenre: (Genre) -> Unit,
    genre2: Genre?, onGenre2: (Genre?) -> Unit,
    audience: Audience, onAudience: (Audience) -> Unit,
    seriesGenre: Genre?,
) {
    val locked = seriesGenre != null
    val g = state.games!!
    val colors = gameColors
    GroupedSection("Name") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Title", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(72.dp))
            BasicTextField(
                value = title,
                onValueChange = onTitle,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onReroll) { Icon(Icons.Filled.Casino, "Another name", tint = MaterialTheme.colorScheme.primary) }
        }
    }
    GroupedSection(
        "Topic",
        footer = if (locked) "A series keeps its topic. Pick another genre to make it a spin-off." else "Colors show combos you've learned from game reports. Research more topics in the Research tab.",
    ) {
        CardBlock {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                g.topics.sortedBy { it.title }.forEach { t ->
                    val fit = g.knownFit(t, genre)
                    val trending = g.trend?.topic == t
                    FilterChip(
                        selected = t == topic,
                        onClick = { onTopic(t) },
                        enabled = !locked || t == topic,
                        label = { Text(t.title) },
                        leadingIcon = {
                            if (trending) {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = colors.warn, modifier = Modifier.size(16.dp))
                            } else {
                                Box(Modifier.size(8.dp).background(fitColor(fit, colors), CircleShape))
                            }
                        },
                    )
                }
            }
        }
    }
    val genreFooter = when {
        seriesGenre != null && genre != seriesGenre ->
            "A spin-off: the series in a new genre. Fans follow it, but only half as keenly as a true sequel."
        else -> g.knownFit(topic, genre)?.let { "${topic.title} + ${genre.title}: ${it.title.lowercase()} combination." }
    }
    GroupedSection("Genre", footer = genreFooter) {
        CardBlock {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Genre.entries.forEach { gn ->
                    val fit = g.knownFit(topic, gn)
                    FilterChip(
                        selected = gn == genre,
                        onClick = { onGenre(gn) },
                        label = { Text(gn.title) },
                        leadingIcon = { Box(Modifier.size(8.dp).background(fitColor(fit, colors), CircleShape)) },
                    )
                }
            }
        }
    }
    if (Research.MULTI_GENRE in state.researched) {
        GroupedSection("Second genre") {
            CardBlock {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FilterChip(selected = genre2 == null, onClick = { onGenre2(null) }, label = { Text("None") })
                    Genre.entries.filter { it != genre }.forEach { gn ->
                        FilterChip(selected = gn == genre2, onClick = { onGenre2(gn) }, label = { Text(gn.title) })
                    }
                }
            }
        }
    }
    GroupedSection(
        "Audience",
        footer = if (Research.TARGET_AUDIENCE !in state.researched) "Research target audience to aim at young or mature players." else g.knownFit(topic, audience)?.let { "${topic.title} for ${audience.phrase}: ${it.title.lowercase()}." },
    ) {
        CardBlock {
            if (Research.TARGET_AUDIENCE in state.researched) {
                Segmented(Audience.entries, audience, { it.title }, onAudience)
            } else {
                Text("Everyone", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PlatformStep(
    state: GameState,
    genre: Genre,
    audience: Audience,
    platforms: MutableList<Platform>,
    size: GameSize, onSize: (GameSize) -> Unit,
    engineId: Int, onEngine: (Int) -> Unit,
) {
    val g = state.games!!
    val colors = gameColors
    val multi = Research.MULTI_PLATFORM in state.researched
    GroupedSection("Platforms", footer = if (multi) "Up to three. Each one charges its licence up front." else "Research multi-platform releases to ship on more than one.") {
        state.platformsOnSale.sortedByDescending { state.platformPlayers(it) * it.spend }.forEachIndexed { i, p ->
            if (i > 0) RowDivider(56.dp)
            val selected = p in platforms
            val fit = g.knownFit(p, genre)
            ListRow(
                p.title,
                subtitle = "${String.format(Locale.US, "%.0f", state.platformPlayers(p))}M players · " +
                    (if (p.fee > 0) "${Format.money(p.fee)} licence" else "no licence") +
                    (fit?.let { " · ${genre.lower} fit: ${it.title.lowercase()}" } ?: "") +
                    (if (!p.suits(audience)) " · not for ${audience.phrase}" else ""),
                leading = {
                    Icon(
                        if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                },
                onClick = {
                    when {
                        selected && platforms.size > 1 -> platforms.remove(p)
                        selected -> {}
                        multi && platforms.size < 3 -> platforms.add(p)
                        else -> {
                            platforms.clear()
                            platforms.add(p)
                        }
                    }
                },
            )
        }
    }
    val sizes = state.unlockedSizes
    GroupedSection(
        "Size",
        footer = "${size.title}: about ${state.estimatedWeeks(size)} weeks with your team, ${size.weeklyBudget.perWeek()} budget. " +
            (GameSize.entries.firstOrNull { it !in sizes }?.let { "Research ${it.requires?.title?.lowercase()} for bigger games." } ?: ""),
    ) {
        CardBlock { Segmented(sizes, size.takeIf { it in sizes } ?: sizes.first(), { it.title }, onSize) }
    }
    GroupedSection("Engine", footer = "Better engines turn the same work into more points. Build new ones from your research.") {
        g.engines.sortedByDescending { it.level }.forEachIndexed { i, e ->
            if (i > 0) RowDivider(56.dp)
            ListRow(
                e.name,
                subtitle = "Level ${e.level}",
                leading = {
                    Icon(
                        if (e.id == engineId) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                        tint = if (e.id == engineId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                },
                onClick = { onEngine(e.id) },
            )
        }
    }
}

@Composable
private fun FocusStep(state: GameState, concept: GameConcept, focus: List<MutableList<Double>>) {
    val g = state.games!!
    Text(
        "Split each stage's effort between its three areas. Every ${concept.genreLower} game rewards the right split. " +
            "Game reports tell you what matters.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    for (stage in 0..2) {
        val areas = FocusArea.forStage(stage)
        val alloc = focus[stage]
        GroupedSection(FocusArea.STAGES[stage]) {
            CardBlock {
                areas.forEachIndexed { i, area ->
                    val total = alloc.sum().coerceAtLeast(0.01)
                    val lesson = insight(g, concept.genre, area)
                    val slider = remember(stage, i) { SliderState(alloc[i].toFloat()) }
                    Column {
                        Row {
                            Text(area.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            if (lesson != null) Hint(lesson, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(Format.percent(alloc[i] / total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Slider(state = slider, onValueChange = {
                            slider.value = it
                            alloc[i] = it.toDouble()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStep(state: GameState, concept: GameConcept, onPrice: (PriceTier) -> Unit) {
    val g = state.games!!
    val colors = gameColors
    val fees = concept.platforms.sumOf { it.fee }
    val blocker = state.conceptBlocker(concept)
    Column(Modifier.padding(horizontal = 4.dp)) {
        Text(concept.title, style = MaterialTheme.typography.headlineMediumEmphasized)
        Text(
            "${concept.size.title} ${concept.topic.title.lowercase()} ${concept.genreLower} game " +
                "for ${concept.audience.phrase} on ${concept.platforms.joinToString { it.title }}.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    GroupedSection("What you know", footer = "Unknown combos are a gamble. That's how you learn.") {
        val tg = g.knownFit(concept.topic, concept.genre)
        ValueRow("${concept.topic.title} + ${concept.genre.title}", tg?.title ?: "Unknown", fitColor(tg, colors))
        concept.genre2?.let {
            RowDivider()
            val f = g.knownFit(concept.topic, it)
            ValueRow("${concept.topic.title} + ${it.title}", f?.title ?: "Unknown", fitColor(f, colors))
        }
        RowDivider()
        val ta = g.knownFit(concept.topic, concept.audience)
        ValueRow("For ${concept.audience.phrase}", ta?.title ?: "Unknown", fitColor(ta, colors))
        concept.platforms.forEach { p ->
            RowDivider()
            val f = g.knownFit(p, concept.genre)
            ValueRow("${concept.genre.title} on ${p.title}", f?.title ?: "Unknown", fitColor(f, colors))
        }
        if (g.trend?.matches(concept) == true) {
            RowDivider()
            ValueRow("Trend", "On trend", colors.good)
        }
    }
    GroupedSection("Budget", footer = blocker) {
        ValueRow("Development", "about ${state.estimatedWeeks(concept.size)} weeks")
        RowDivider()
        ValueRow("Budget", concept.size.weeklyBudget.perWeek())
        RowDivider()
        ValueRow("Licence fees", if (fees > 0) Format.money(fees) else "None")
    }
    GroupedSection(
        "Price · ${Format.money(concept.unitPrice)} a copy",
        footer = when (concept.price) {
            PriceTier.BUDGET -> "Cheap sells more copies and players love a bargain, but every copy earns less. Best for games you're not sure about."
            PriceTier.STANDARD -> "The going rate for a ${concept.size.title.lowercase()} game."
            PriceTier.PREMIUM -> "Only great games hold up at a premium. A weak one sells far fewer copies, and critics call it overpriced."
        },
    ) {
        CardBlock { Segmented(PriceTier.entries, concept.price, { it.title }) { onPrice(it) } }
    }
}
