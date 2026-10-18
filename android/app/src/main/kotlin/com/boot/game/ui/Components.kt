package com.boot.game.ui

import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.Lane
import com.boot.core.LogEntry
import com.boot.game.audio.Sfx
import com.boot.game.audio.Silent
import com.boot.game.audio.SoundOut

// Sections: a heading, a white card, and a footnote

private val CardShape = RoundedCornerShape(24.dp)
private val CompactCardShape = RoundedCornerShape(16.dp)

/**
 * True on a phone held sideways: the screen is short, so sections, cards, rows and tiles use
 * tighter spacing to show more at once.
 */
val LocalCompact = compositionLocalOf { false }

/**
 * The smaller sideways scale, when it applies. Dialogs and sheets open in windows of their own,
 * which start back at the system scale; [Scaled] puts them on the same one as the game.
 */
val LocalSidewaysDensity = compositionLocalOf<Density?> { null }

/** Draws [content] at the sideways scale, if there is one. Safe to nest. */
@Composable
fun Scaled(content: @Composable () -> Unit) {
    val density = LocalSidewaysDensity.current
    if (density == null) content() else CompositionLocalProvider(LocalDensity provides density, content = content)
}

/** The game's sound effects, for buttons and moments the UI times itself. */
val LocalSound = staticCompositionLocalOf<SoundOut> { Silent }

/** A dialog drawn at the same scale as the rest of the game. */
@Composable
fun GameDialog(onDismissRequest: () -> Unit, properties: DialogProperties = DialogProperties(), content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) { Scaled(content) }
}

/**
 * A section of a screen: a sentence-case heading, a rounded card of rows or content, and an
 * optional footnote under it. The heading sits on the page, not in the card, so cards stay calm.
 */
@Composable
fun GroupedSection(
    header: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compact = LocalCompact.current
    Column(modifier.fillMaxWidth()) {
        if (header != null || action != null) {
            Row(
                Modifier.heightIn(min = if (compact) 28.dp else 32.dp).padding(start = 4.dp, end = 0.dp, bottom = if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    header ?: "",
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
        }
        Surface(shape = if (compact) CompactCardShape else CardShape, color = gameColors.card, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()), content = content)
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = if (compact) 6.dp else 8.dp),
            )
        }
    }
}

/** Free-form content inside a grouped card, with the card's padding. */
@Composable
fun CardBlock(spacing: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalCompact.current
    Column(
        Modifier.fillMaxWidth().padding(if (compact) 16.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) spacing * 0.8f else spacing),
        content = content,
    )
}

/** The hairline between rows, inset to the text. */
@Composable
fun RowDivider(inset: Dp = 20.dp) {
    val edge = if (LocalCompact.current) 16.dp else 20.dp
    HorizontalDivider(Modifier.padding(start = inset.coerceAtLeast(edge), end = edge), thickness = 1.dp, color = gameColors.separator)
}

/** One iOS table cell: optional leading badge, title and subtitle, a value on the right, a chevron if it navigates. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    valueColor: Color = Color.Unspecified,
    valueDetail: String? = null,
    valueDetailColor: Color = Color.Unspecified,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val compact = LocalCompact.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = if (compact) 50.dp else 56.dp)
            .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = if (compact) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else valueColor,
                )
                if (valueDetail != null) {
                    Text(
                        valueDetail,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (valueDetailColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else valueDetailColor,
                    )
                }
            }
        }
        trailing?.invoke(this)
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp).size(22.dp),
            )
        }
    }
}

/** iOS "value" cell: a label on the left, its value in gray on the right. */
@Composable
fun ValueRow(key: String, value: String, valueColor: Color = Color.Unspecified) = ListRow(key, value = value, valueColor = valueColor)

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
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

// Numbers

/**
 * A number with its label: a small label, the figure, an optional line under it. No box of its
 * own; tiles sit straight on the card, separated by space.
 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null, tint: Color = Color.Unspecified) {
    Column(modifier.fillMaxHeight().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FitText(label, MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FitText(
            value,
            MaterialTheme.typography.titleLargeEmphasized,
            color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint,
            min = 12.sp,
        )
        if (detail != null) {
            Text(
                detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tiles side by side. Phones are narrow; pairs and triples read better than a long row. */
@Composable
fun TileRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(if (LocalCompact.current) 16.dp else 20.dp),
        content = content,
    )
}

/** Two pairs of tiles: stacked on a phone held upright, all four on one row sideways. */
@Composable
fun TileGrid(first: @Composable RowScope.() -> Unit, second: @Composable RowScope.() -> Unit) {
    if (LocalCompact.current) {
        TileRow {
            first()
            second()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TileRow(first)
            TileRow(second)
        }
    }
}

/**
 * A labelled 0...1 bar. Static readings (reputation, morale) get a calm rounded bar; work in
 * progress gets Material's expressive wavy indicator, which moves while it fills.
 */
@Composable
fun Meter(
    label: String,
    value: Double,
    tint: Color = MaterialTheme.colorScheme.primary,
    caption: String? = null,
    wavy: Boolean = false,
) {
    val target = value.coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(target, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "meter")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // The label keeps its width; a long caption wraps on the right rather than squeezing it.
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.padding(end = 12.dp))
            Text(
                caption ?: Format.percent(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (wavy) {
            LinearWavyProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth(),
                color = tint,
                trackColor = tint.copy(alpha = 0.18f),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animated)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(tint),
                )
            }
        }
    }
}

// Lane badges: Material expressive shapes, morphing on selection

/** A [Shape] partway between two polygons. Polygons are normalized to a unit square. */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        val matrix = android.graphics.Matrix()
        matrix.setScale(size.width, size.height)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

/**
 * The lane's emoji on its expressive shape. When [morph] is set the badge springs from a circle
 * into the lane's shape (selected) and back (not selected).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LaneBadge(lane: Lane, size: Dp = 40.dp, selected: Boolean = true, morph: Boolean = false) {
    val morphShape = remember(lane) { Morph(MaterialShapes.Circle, lane.polygon) }
    val progress by animateFloatAsState(
        if (selected || !morph) 1f else 0f,
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "badge",
    )
    val color = gameColors.lane(lane)
    Box(
        Modifier
            .size(size)
            .background(color.copy(alpha = if (selected) 0.24f else 0.12f), MorphShape(morphShape, progress)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            lane.emoji,
            style = when {
                size >= 56.dp -> MaterialTheme.typography.headlineMedium
                size >= 40.dp -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.bodyLarge
            },
        )
    }
}

@Composable
fun LogRow(entry: LogEntry) {
    val colors = gameColors
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(8.dp)
                .background(colors.tone(entry.tone, MaterialTheme.colorScheme.outline), CircleShape),
        )
        Spacer(Modifier.width(12.dp))
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

// Controls

/** Primary call to action. Expressive buttons morph their shape when pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptics = LocalHapticFeedback.current
    val sound = LocalSound.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            sound.play(Sfx.CLICK)
            onClick()
        },
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val sound = LocalSound.current
    FilledTonalButton(onClick = { sound.play(Sfx.CLICK); onClick() }, shapes = ButtonDefaults.shapes(), modifier = modifier, enabled = enabled) {
        Text(text, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Color.Unspecified, enabled: Boolean = true) {
    TextButton(onClick = onClick, shapes = ButtonDefaults.shapes(), modifier = modifier, enabled = enabled) {
        Text(text, color = color)
    }
}

/**
 * A picker over a short list: a soft track with the chosen option on a raised white pill that
 * slides between options.
 */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val colors = gameColors
    val motion = MaterialTheme.motionScheme
    val compact = LocalCompact.current
    // One size for every label: the largest at which the longest one fits its segment.
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val base = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val segment = with(density) { ((maxWidth - 6.dp) / options.size.coerceAtLeast(1) - 16.dp).toPx() }
    val widest = options.maxOfOrNull { measurer.measure(label(it), base, maxLines = 1, softWrap = false).size.width } ?: 0
    // When even a slightly smaller label won't fit, the options scroll sideways at full size instead.
    val scrolls = widest > 0 && segment > 0 && widest * 0.85f > segment
    val scale = if (!scrolls && widest > 0 && segment > 0) (segment / widest).coerceIn(0.85f, 1f) else 1f
    val shared = base.copy(fontSize = base.fontSize * scale)
    val scroll = androidx.compose.foundation.rememberScrollState()
    val starts = remember { mutableStateMapOf<Int, Int>() }
    val index = options.indexOf(selected)
    if (scrolls) {
        // Keep the chosen option in view.
        LaunchedEffect(index, starts[index]) {
            starts[index]?.let { x -> scroll.animateScrollTo((x - with(density) { 24.dp.roundToPx() }).coerceAtLeast(0)) }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.fill)
            .then(if (scrolls) Modifier.horizontalScroll(scroll) else Modifier)
            .padding(3.dp),
    ) {
        options.forEachIndexed { i, option ->
            val on = option == selected
            // The chosen option sits on a raised pill: white on the light track, a lighter grey on the dark one.
            val raised = if (colors.card.luminance() < 0.5f) MaterialTheme.colorScheme.surfaceContainerHighest else colors.card
            val bg by androidx.compose.animation.animateColorAsState(if (on) raised else Color.Transparent, motion.defaultEffectsSpec(), label = "segment")
            Surface(
                color = bg,
                shape = RoundedCornerShape(13.dp),
                shadowElevation = if (on) 1.dp else 0.dp,
                modifier = Modifier
                    .then(if (scrolls) Modifier.onGloballyPositioned { starts[i] = it.positionInParent().x.toInt() } else Modifier.weight(1f))
                    .heightIn(min = if (compact) 38.dp else 40.dp)
                    .selectable(selected = on, role = Role.RadioButton) {
                        if (!on) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(option)
                        }
                    },
            ) {
                Box(Modifier.padding(horizontal = if (scrolls) 14.dp else 8.dp, vertical = if (compact) 7.dp else 9.dp), contentAlignment = Alignment.Center) {
                    Text(
                        label(option), style = shared.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    }
}

/** A slider that only commits when the finger lifts, so dragging doesn't spam the simulation. */
@Composable
fun CommitSlider(value: Double, label: (Float) -> String, onCommit: (Double) -> Unit) {
    val state = remember(value) { SliderState(value.toFloat()) }
    Column {
        Text(label(state.value), style = MaterialTheme.typography.bodyMedium)
        Slider(
            state = state,
            onValueChange = { state.value = it },
            onValueChangeFinished = { onCommit(state.value.toDouble()) },
        )
    }
}

/** iOS switch cell. The switch goes iOS green when on. */
@Composable
fun ToggleRow(title: String, checked: Boolean, detail: String? = null, onChange: (Boolean) -> Unit) {
    ListRow(title, subtitle = detail, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = gameColors.good, checkedBorderColor = gameColors.good),
        )
    }
}

/** A cell with one action on the right. */
@Composable
fun ActionRow(title: String, detail: String, action: String, enabled: Boolean = true, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    ListRow(title, subtitle = detail, leading = leading) {
        Spacer(Modifier.width(8.dp))
        TonalButton(action, onClick, enabled = enabled)
    }
}

/** A text button that drops down a menu of choices. */
@Composable
fun <T> MenuButton(text: String, options: List<T>, label: (T) -> String, enabled: Boolean = true, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PlainButton(text, { open = true }, enabled = enabled)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(16.dp)) {
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
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            PlainButton(confirm, {
                onDismiss()
                onConfirm()
            }, color = if (destructive) gameColors.bad else Color.Unspecified)
        },
        dismissButton = { PlainButton("Cancel", onDismiss) },
        shape = RoundedCornerShape(28.dp),
    )
}

fun Double.perWeek() = "${Format.money(this)}/wk"

fun Double.signedPerWeek() = "${Format.signedMoney(this)}/wk"

/**
 * One line that shrinks to fit, down to [min], instead of cutting off. Labels in tight rows
 * (segments, tiles, the top bar) stay whole when the phone's text size is turned up.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    min: TextUnit = 9.sp,
) {
    Text(
        text, modifier, color = color, fontWeight = fontWeight, textAlign = textAlign, style = style,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = min, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}

/** A short explanation that appears on long press: for icons and numbers that don't say what they are. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Tip(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.TooltipBox(
        positionProvider = androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider(
            androidx.compose.material3.TooltipAnchorPosition.Below,
        ),
        tooltip = { PlainTooltip { Text(text) } },
        state = androidx.compose.material3.rememberTooltipState(),
        modifier = modifier,
    ) { content() }
}
