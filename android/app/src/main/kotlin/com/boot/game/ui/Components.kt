package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.Lane
import com.boot.core.LogEntry

/** A titled section. Every screen is a stack of these. */
@Composable
fun Section(
    title: String?,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null || trailing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) {
                        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    trailing?.invoke(this)
                }
            }
            content()
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null, tint: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Two tiles side by side. Phones are narrow; pairs read better than a long row. */
@Composable
fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

/** A labelled 0...1 bar. */
@Composable
fun Meter(label: String, value: Double, tint: Color = MaterialTheme.colorScheme.primary, caption: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(
                caption ?: Format.percent(value), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = tint,
            trackColor = tint.copy(alpha = 0.15f),
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
    }
}

@Composable
fun KeyValue(key: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth()) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
fun Hint(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
fun Pill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun LaneBadge(lane: Lane, size: Int = 40) {
    Box(
        Modifier
            .size(size.dp)
            .background(gameColors.lane(lane).copy(alpha = 0.16f), RoundedCornerShape((size / 4).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(lane.emoji, style = if (size >= 40) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LaneHeader(lane: Lane) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LaneBadge(lane, 48)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(lane.businessName, style = MaterialTheme.typography.headlineSmall)
            Text(lane.pitch, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LogRow(entry: LogEntry) {
    val colors = gameColors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(colors.tone(entry.tone, MaterialTheme.colorScheme.outline), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(entry.text, style = MaterialTheme.typography.bodyMedium)
            Text(
                listOfNotNull(GameClock.label(entry.week), entry.lane?.title).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A slider that only commits when the finger lifts, so dragging doesn't spam the simulation. */
@Composable
fun CommitSlider(value: Double, label: (Float) -> String, onCommit: (Double) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column {
        Text(label(local), style = MaterialTheme.typography.bodyMedium)
        Slider(value = local, onValueChange = { local = it }, onValueChangeFinished = { onCommit(local.toDouble()) })
    }
}

/** A segmented picker over a short list of options. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/** A text button that drops down a menu of choices. */
@Composable
fun <T> MenuButton(text: String, options: List<T>, label: (T) -> String, enabled: Boolean = true, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, enabled = enabled) { Text(text) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(label(option)) }, onClick = {
                    open = false
                    onPick(option)
                })
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun Double.perWeek() = "${Format.money(this)}/wk"

fun Double.signedPerWeek() = "${Format.signedMoney(this)}/wk"
