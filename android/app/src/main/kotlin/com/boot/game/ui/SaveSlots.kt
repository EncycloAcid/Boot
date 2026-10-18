package com.boot.game.ui

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.game.GameStore
import com.boot.game.SLOT_COUNT
import com.boot.game.SlotInfo

/*
 * Save slots on the title screen: carry on with any of them, or pick an empty one for a new game.
 * Each slot saves as you play, and "Save slots" in the menu comes back here.
 */

@Composable
fun SaveSlots(store: GameStore) {
    var deleting by remember { mutableStateOf<SlotInfo?>(null) }
    GroupedSection(
        "Save slots",
        footer = "Continue a game, or pick an empty slot for a new one. Every slot saves as you play; \"Save slots\" in the game's menu brings you back here.",
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val wide = maxWidth > 520.dp
            val card: @Composable (Int, Modifier) -> Unit = { n, modifier ->
                SlotCard(store, n, store.slots.getOrNull(n - 1), selected = store.slot == n, modifier) { deleting = it }
            }
            if (wide) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (n in 1..SLOT_COUNT) card(n, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (n in 1..SLOT_COUNT) card(n, Modifier.fillMaxWidth())
                }
            }
        }
    }
    deleting?.let { info ->
        ConfirmDialog(
            title = "Delete ${info.company}?",
            text = "The game in slot ${info.number} is gone for good. Its Hall of Fame entry, if it has one, stays.",
            confirm = "Delete",
            destructive = true,
            onConfirm = { store.deleteSlot(info.number) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SlotCard(store: GameStore, n: Int, info: SlotInfo?, selected: Boolean, modifier: Modifier, onDelete: (SlotInfo) -> Unit) {
    val colors = gameColors
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .heightIn(min = 112.dp)
            .clip(shape)
            .background(if (selected && info == null) scheme.primaryContainer.copy(alpha = 0.5f) else colors.card)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else colors.separator), shape)
            .then(if (info == null) Modifier.clickable(role = Role.RadioButton) { store.chooseSlot(n) } else Modifier)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Slot $n", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = if (selected) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            if (info != null) {
                IconButton(onClick = { onDelete(info) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete slot $n", tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (info == null) {
            Text("Empty", style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
            Text(
                if (selected) "Your new game goes here." else "Tap to start your new game here.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
            )
        } else {
            Text(info.company, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${info.mode} · ${info.date}", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                (if (info.over) "Finished · " else "") + "${Format.money(info.cash)} cash · worth ${Format.money(info.valuation)}",
                style = MaterialTheme.typography.bodySmall, color = if (info.cash < 0) colors.bad else scheme.onSurfaceVariant,
            )
            Text(
                if (System.currentTimeMillis() - info.savedAt < DateUtils.MINUTE_IN_MILLIS) {
                    "Played just now"
                } else {
                    "Played " + DateUtils.getRelativeTimeSpanString(info.savedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                },
                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            TonalButton(if (info.over) "See how it ended" else "Continue", { store.loadSlot(n) }, Modifier.fillMaxWidth().padding(end = 8.dp, top = 6.dp))
        }
    }
}
