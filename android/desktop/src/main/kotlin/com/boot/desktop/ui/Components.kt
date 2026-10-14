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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassShape = RoundedCornerShape(18.dp)
private val PillShape = RoundedCornerShape(50)

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val h = hudColors
    Column(
        modifier
            .shadow(10.dp, GlassShape, clip = false)
            .clip(GlassShape)
            .background(h.glass)
            .border(1.dp, Color.White.copy(alpha = 0.65f), GlassShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun StatusPill(
    iconTint: Color,
    icon: String,
    label: String,
    valueColor: Color = hudColors.text,
    onClick: (() -> Unit)? = null,
) {
    val h = hudColors
    Row(
        Modifier
            .shadow(6.dp, PillShape, clip = false)
            .clip(PillShape)
            .background(h.glass)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 12.sp, color = iconTint)
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

@Composable
fun ResourceBubble(
    value: String,
    fill: Color,
    glyph: String,
    size: Dp = 56.dp,
) {
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Compact stack so glyph + value never clip inside the circle.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(glyph, fontSize = (size.value * 0.22f).sp, color = Color.White.copy(alpha = 0.95f), lineHeight = (size.value * 0.24f).sp)
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.28f).sp,
                lineHeight = (size.value * 0.3f).sp,
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
        !enabled -> Color(0xFFE5E5EA)
        primary -> h.accent
        else -> h.glass
    }
    val fg = when {
        !enabled -> h.textDim
        primary -> Color.White
        else -> h.text
    }
    Box(
        modifier
            .shadow(if (primary) 4.dp else 2.dp, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
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
            .clip(PillShape)
            .background(if (selected) h.accent.copy(alpha = 0.14f) else h.glass)
            .border(1.dp, if (selected) h.accent.copy(alpha = 0.45f) else Color(0x22000000), PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) h.accent else h.textDim,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    val h = hudColors
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = h.text)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = h.textDim)
        }
    }
}

@Composable
fun Hint(text: String, color: Color = hudColors.textDim) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
fun ToastBanner(message: String, onDismiss: () -> Unit) {
    val h = hudColors
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .widthIn(max = 480.dp)
                .shadow(10.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(h.glass)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = h.text, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text("OK", style = MaterialTheme.typography.labelLarge, color = h.accent, fontWeight = FontWeight.Bold)
        }
    }
}

val ScreenPad = PaddingValues(start = 32.dp, end = 32.dp, top = 128.dp, bottom = 124.dp)
