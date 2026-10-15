package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft GDT float — white panel, soft shadow, light rim. */
private val PanelShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(12.dp)
private val DiscShape = CircleShape

@Composable
fun Modifier.hudSurface(elevated: Boolean = true): Modifier {
    val h = hudColors
    return this
        .then(
            if (elevated) {
                Modifier.shadow(8.dp, PanelShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            } else {
                Modifier
            },
        )
        .clip(PanelShape)
        .background(h.panel)
        .border(1.dp, h.panelBorder, PanelShape)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.hudSurface().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun StatusPill(
    iconTint: Color,
    glyph: HudGlyph,
    label: String,
    valueColor: Color = hudColors.ink,
    onClick: (() -> Unit)? = null,
) {
    val h = hudColors
    Row(
        Modifier
            .shadow(6.dp, ChipShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(ChipShape)
            .background(h.panel)
            .border(1.dp, h.panelBorder, ChipShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 36.dp)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            HudIcon(glyph, iconTint, 13.dp)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Bright GDT resource disc — saturated fill, soft rim, silhouette glyph + value. */
@Composable
fun ResourceBubble(
    value: String,
    fill: Color,
    glyph: HudGlyph,
    size: Dp = 56.dp,
    caption: String? = null,
) {
    val h = hudColors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(size)
                .shadow(7.dp, DiscShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
                .clip(DiscShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            fill,
                            fill.copy(red = fill.red * 0.9f, green = fill.green * 0.9f, blue = fill.blue * 0.9f),
                        ),
                    ),
                )
                .border(2.dp, Color.White.copy(alpha = 0.45f), DiscShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-1).dp),
            ) {
                HudIcon(glyph, Color.White, size * 0.38f)
                Text(
                    value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.26f).sp,
                    lineHeight = (size.value * 0.28f).sp,
                    maxLines = 1,
                )
            }
        }
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = h.inkMute,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun MacButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val h = hudColors
    val bg = when {
        !enabled -> Color(0xFFE8E4DC)
        primary -> h.accent
        else -> h.panel
    }
    val fg = when {
        !enabled -> h.inkMute
        primary -> Color.White
        else -> h.ink
    }
    Box(
        modifier
            .shadow(if (primary && enabled) 4.dp else 0.dp, ChipShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(ChipShape)
            .background(bg)
            .then(if (!primary && enabled) Modifier.border(1.dp, Color(0x22000000), ChipShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val h = hudColors
    Box(
        Modifier
            .clip(ChipShape)
            .background(if (selected) h.accentSoft else h.panel)
            .border(1.dp, if (selected) h.accent.copy(alpha = 0.4f) else Color(0x22000000), ChipShape)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) h.accent else h.inkMute,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    val h = hudColors
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = h.ink)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = h.inkMute)
        }
    }
}

@Composable
fun Hint(text: String, color: Color = hudColors.inkMute) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}

/** Scannable empty / next-step block inside a panel. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val h = hudColors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(h.accentSoft.copy(alpha = 0.55f))
            .border(1.dp, h.accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = h.ink, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = h.inkMute)
        if (actionLabel != null && onAction != null) {
            MacButton(actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ToastBanner(message: String, onDismiss: () -> Unit) {
    val h = hudColors
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .widthIn(max = 480.dp)
                .hudSurface()
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(28.dp).clip(RoundedCornerShape(1.dp)).background(h.accent))
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = h.ink, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text("Dismiss", style = MaterialTheme.typography.labelLarge, color = h.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WorldBackdrop(modifier: Modifier = Modifier) {
    val h = hudColors
    Box(
        modifier.background(Brush.verticalGradient(listOf(h.world, h.worldDeep, Color(0xFFD2CBBF)))),
    )
}

private val WindowShape = RoundedCornerShape(16.dp)

/**
 * Floating window over the office — title bar with drag grip, clear Close, focused chrome.
 * [onDrag] reports pointer delta in dp so the shell can offset the window.
 */
@Composable
fun FloatingWindow(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    focused: Boolean = true,
    onDrag: ((DpOffset) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val h = hudColors
    val border = if (focused) h.accent.copy(alpha = 0.35f) else h.panelBorder
    Column(
        modifier
            .shadow(if (focused) 18.dp else 10.dp, WindowShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(WindowShape)
            .background(h.panel)
            .border(1.5.dp, border, WindowShape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) Color(0xFFEAF2FA) else Color(0xFFF4F6F8))
                .then(
                    if (onDrag != null) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(DpOffset(dragAmount.x.toDp(), dragAmount.y.toDp()))
                            }
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Drag affordance — six-dot grip.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) {
                            Box(
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(h.inkMute.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = h.ink,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = h.inkMute,
                        maxLines = 1,
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(h.accent)
                    .clickable(onClick = onClose)
                    .defaultMinSize(minWidth = 72.dp, minHeight = 36.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Close", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

val ScreenPad = PaddingValues(start = 32.dp, end = 32.dp, top = 128.dp, bottom = 124.dp)

val WindowPad = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
