package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft studio sheet — 10dp corners, ink border, single light lift (not frosted glass). */
private val PanelShape = RoundedCornerShape(10.dp)
private val ChipShape = RoundedCornerShape(8.dp)
private val DiscShape = CircleShape

@Composable
fun Modifier.hudSurface(elevated: Boolean = true): Modifier {
    val h = hudColors
    return this
        .then(if (elevated) Modifier.shadow(4.dp, PanelShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow) else Modifier)
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
        modifier.hudSurface().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

@Composable
fun StatusPill(
    iconTint: Color,
    icon: String,
    label: String,
    valueColor: Color = hudColors.ink,
    onClick: (() -> Unit)? = null,
) {
    val h = hudColors
    Row(
        Modifier
            .shadow(3.dp, ChipShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(ChipShape)
            .background(h.panel)
            .border(1.dp, h.panelBorder, ChipShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Colored tick instead of emoji badge.
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(iconTint),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Matte GDT-style resource disc — solid fill, thin ink rim, no white glow. */
@Composable
fun ResourceBubble(
    value: String,
    fill: Color,
    glyph: String,
    size: Dp = 56.dp,
) {
    val h = hudColors
    Box(
        Modifier
            .size(size)
            .shadow(5.dp, DiscShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(DiscShape)
            .background(
                Brush.verticalGradient(
                    listOf(fill.copy(alpha = 1f), fill.copy(red = fill.red * 0.88f, green = fill.green * 0.88f, blue = fill.blue * 0.88f)),
                ),
            )
            .border(1.5.dp, Color.Black.copy(alpha = 0.18f), DiscShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-1).dp),
        ) {
            Text(
                glyph,
                fontSize = (size.value * 0.2f).sp,
                color = Color.White.copy(alpha = 0.92f),
                lineHeight = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.3f).sp,
                lineHeight = (size.value * 0.32f).sp,
                maxLines = 1,
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
        !enabled -> Color(0xFFE2E8F0)
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
            .shadow(if (primary && enabled) 3.dp else 0.dp, ChipShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(ChipShape)
            .background(bg)
            .then(if (!primary && enabled) Modifier.border(1.dp, h.panelBorder, ChipShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            .border(1.dp, if (selected) h.accent.copy(alpha = 0.55f) else h.panelBorder, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
            Text("OK", style = MaterialTheme.typography.labelLarge, color = h.accent, fontWeight = FontWeight.Bold)
        }
    }
}

/** Cool mist paper wash for new game / empty worlds. */
@Composable
fun WorldBackdrop(modifier: Modifier = Modifier) {
    val h = hudColors
    Box(
        modifier
            .background(
                Brush.verticalGradient(listOf(h.world, h.worldDeep, Color(0xFFCBD8E2))),
            ),
    )
}

private val WindowShape = RoundedCornerShape(12.dp)

/**
 * Floating studio window over the office — title bar + close, scrollable body.
 * Does not cover the whole screen; the isometric room stays visible around it.
 */
@Composable
fun FloatingWindow(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val h = hudColors
    Column(
        modifier
            .shadow(10.dp, WindowShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
            .clip(WindowShape)
            .background(h.panel)
            .border(1.dp, h.panelBorder, WindowShape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(h.accentSoft)
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = h.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Close", style = MaterialTheme.typography.labelLarge, color = h.accent, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

/** Full-page padding (new game only). Tab content uses [WindowPad] inside floating windows. */
val ScreenPad = PaddingValues(start = 32.dp, end = 32.dp, top = 128.dp, bottom = 124.dp)

/** Compact padding for content inside [FloatingWindow]. */
val WindowPad = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
