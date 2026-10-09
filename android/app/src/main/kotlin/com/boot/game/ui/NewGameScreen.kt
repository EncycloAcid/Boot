package com.boot.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.game.GameStore

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewGameScreen(store: GameStore) {
    var company by rememberSaveable { mutableStateOf("Garage Labs") }
    var founder by rememberSaveable { mutableStateOf("") }
    var lane by rememberSaveable { mutableStateOf(Lane.GAMES) }
    val colors = gameColors

    Scaffold(
        containerColor = colors.grouped,
        bottomBar = {
            Surface(color = colors.bar) {
                Column {
                    HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (lane.isCapitalIntensive) {
                            Hint(
                                "Hard mode: ${lane.title} starts with a seed round and a board that can overrule you, or fire you.",
                                colors.warn,
                            )
                        }
                        PrimaryButton(
                            "Start your ${lane.businessName.lowercase()}",
                            { store.newGame(company.trim().ifEmpty { "Garage Labs" }, founder.trim().ifEmpty { "You" }, lane) },
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 28.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Boot", style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "You start solo in a cramped apartment with ${Format.money(GameState.STARTING_CASH)} in savings. Pick a lane.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Hint(
                        "Mid-game, the Pivot Point forces a second lane. Every pairing unlocks its own crossover. " +
                            "Hardware, software and services together make an ecosystem company. " +
                            "IPO, sell, stay indie or burn out: the ending reflects how you got there.",
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GroupedSection("Your company") {
                    FormRow("Company", company, "Garage Labs", ImeAction.Next) { company = it }
                    RowDivider()
                    FormRow("Founder", founder, "You", ImeAction.Done) { founder = it }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "PICK A LANE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                )
            }
            items(Lane.entries) { l -> LaneTile(l, selected = l == lane) { lane = l } }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = lane,
                    transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut() },
                    label = "pitch",
                ) { l ->
                    GroupedSection(null) {
                        CardBlock(spacing = 6.dp) {
                            Text(l.businessName, style = MaterialTheme.typography.titleMedium)
                            Text(l.pitch, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** An iOS form cell: label on the left, an inline text field on the right. */
@Composable
private fun FormRow(label: String, value: String, placeholder: String, ime: ImeAction, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(96.dp))
        val style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ime),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline))
                    inner()
                }
            },
        )
    }
}

@Composable
private fun LaneTile(lane: Lane, selected: Boolean, onClick: () -> Unit) {
    val colors = gameColors
    val accent = colors.lane(lane)
    val border by animateColorAsState(if (selected) accent else colors.card, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "border")
    val width by animateDpAsState(if (selected) 2.5.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "width")
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = BorderStroke(width, border),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                LaneBadge(lane, 56.dp, selected = selected, morph = true)
                Spacer(Modifier.weight(1f))
                if (lane.isCapitalIntensive) Pill("VC", colors.warn)
            }
            Spacer(Modifier.height(12.dp))
            Text(lane.title, style = MaterialTheme.typography.titleMedium)
            Text(
                lane.businessName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
