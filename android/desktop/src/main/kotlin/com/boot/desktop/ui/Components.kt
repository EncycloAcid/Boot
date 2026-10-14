package com.boot.desktop.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PixelCorner = RoundedCornerShape(4.dp)

@Composable
fun SpritePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = spriteColors
    Column(
        modifier
            .clip(PixelCorner)
            .background(c.panel)
            .border(BorderStroke(2.dp, c.panelEdge), PixelCorner)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun SpriteButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val c = spriteColors
    val bg = when {
        !enabled -> c.panelEdge.copy(alpha = 0.4f)
        primary -> c.amber
        else -> c.inkRaised
    }
    val fg = when {
        !enabled -> c.mute
        primary -> c.ink
        else -> c.text
    }
    Box(
        modifier
            .clip(PixelCorner)
            .background(bg)
            .border(BorderStroke(2.dp, if (primary) c.amberDim else c.panelEdge), PixelCorner)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HudStat(label: String, value: String, valueColor: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    val c = spriteColors
    Column(
        Modifier
            .clip(PixelCorner)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.mute)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = if (valueColor == Color.Unspecified) c.text else valueColor,
        )
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    val c = spriteColors
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.text)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textDim)
        }
    }
}

@Composable
fun Hint(text: String, color: Color = spriteColors.mute) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = spriteColors
    Box(
        Modifier
            .clip(PixelCorner)
            .background(if (selected) c.navSelected else c.inkRaised)
            .border(BorderStroke(2.dp, if (selected) c.amber else c.panelEdge), PixelCorner)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.amber else c.textDim,
        )
    }
}

@Composable
fun ToastBanner(message: String, onDismiss: () -> Unit) {
    val c = spriteColors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            Modifier
                .widthIn(max = 520.dp)
                .clip(PixelCorner)
                .background(c.amber)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text("OK", style = MaterialTheme.typography.labelLarge, color = c.ink, fontWeight = FontWeight.Bold)
        }
    }
}

val ScreenPad = PaddingValues(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 110.dp)
