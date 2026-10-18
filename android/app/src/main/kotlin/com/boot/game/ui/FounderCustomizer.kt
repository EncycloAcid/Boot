package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Appearance
import com.boot.core.FounderTrait
import kotlin.random.Random

/*
 * The founder customizer on the new game screen: how you look, with a live portrait, and the
 * special trait you start with.
 */

private val styleNames = listOf("Short", "Swept", "Long", "Bun", "Curly", "Bald", "Ponytail", "Bob")
private val outfitNames = listOf("Hoodie", "T-shirt", "Shirt", "Sweater", "Blazer")

/** Your look: a portrait that changes as you pick, and a row of choices for each part. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FounderLook(look: Appearance, onChange: (Appearance) -> Unit) {
    GroupedSection("You, the founder", footer = "This is you in the office and on every screen. Change it any time before you start.") {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(lookOf(look), MaterialTheme.colorScheme.primary, 96.dp, crown = true)
                TonalButton("Shuffle", { onChange(randomLook()) })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Choice("Skin") {
                    SkinTones.forEachIndexed { i, c -> Swatch(c, look.skin == i, "Skin tone ${i + 1}") { onChange(look.copy(skin = i)) } }
                }
                Choice("Hair") {
                    HairColours.forEachIndexed { n, (i, c) -> Swatch(c, look.hair == i, "Hair colour ${n + 1}") { onChange(look.copy(hair = i)) } }
                }
                Choice("Style") {
                    styleNames.forEachIndexed { i, name -> Tag(name, look.style == i) { onChange(look.copy(style = i)) } }
                }
                Choice("Clothes") {
                    outfitNames.forEachIndexed { i, name -> Tag(name, look.outfit == i) { onChange(look.copy(outfit = i)) } }
                }
                Choice("Colour") {
                    ClothesColours.forEachIndexed { i, c -> Swatch(c, look.top == i, "Clothes colour ${i + 1}") { onChange(look.copy(top = i)) } }
                }
                Choice("Extras") {
                    Tag("Glasses", look.glasses) { onChange(look.copy(glasses = !look.glasses)) }
                    Tag("Beard", look.beard) { onChange(look.copy(beard = !look.beard)) }
                }
            }
        }
    }
}

/** Your special trait, or none. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FounderTraitPicker(trait: FounderTrait?, onChange: (FounderTrait?) -> Unit) {
    GroupedSection("Your special trait", footer = trait?.detail ?: "No trait: you play it straight.") {
        // A grid that fills the width: as many columns as fit, every card in a row the same size.
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val cols = ((maxWidth + 10.dp) / (170.dp + 10.dp)).toInt().coerceIn(1, 4)
            val cards = listOf<Pair<FounderTrait?, Pair<String, String>>>(null to ("None" to "Play it straight.")) +
                FounderTrait.entries.map { it to (it.title to it.detail.substringAfter(": ").replaceFirstChar { c -> c.uppercase() }) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in cards.chunked(cols)) {
                    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((t, text) in row) {
                            TraitCard(text.first, text.second, trait == t, Modifier.weight(1f).fillMaxHeight()) { onChange(t) }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choice(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, description: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(if (selected) 3.dp else 1.dp, if (selected) scheme.primary else gameColors.separator, CircleShape)
            .padding(if (selected) 5.dp else 3.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
    ) {}
}

@Composable
private fun Tag(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .border(1.dp, if (selected) scheme.primary else gameColors.separator, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun TraitCard(title: String, detail: String, selected: Boolean, modifier: Modifier = Modifier.width(196.dp), onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) scheme.primaryContainer.copy(alpha = 0.6f) else gameColors.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else gameColors.separator, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (selected) scheme.primary else scheme.onSurface)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

/** A look at random, for the shuffle button. */
internal fun randomLook(): Appearance = appearanceFor(Random.nextInt())
