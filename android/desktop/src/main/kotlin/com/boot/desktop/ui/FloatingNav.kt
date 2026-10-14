package com.boot.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boot.desktop.DesktopStore
import com.boot.desktop.Tab

private data class NavItem(val tab: Tab, val label: String, val glyph: String)

private val NavItems = listOf(
    NavItem(Tab.OFFICE, "Office", "⌂"),
    NavItem(Tab.LANES, "Lanes", "▦"),
    NavItem(Tab.RESEARCH, "Research", "⚗"),
    NavItem(Tab.TEAM, "Team", "☺"),
    NavItem(Tab.COMPANY, "Company", "⌂"),
)

/** macOS/GDT floating nav — white translucent pill with soft shadow, not a dark Material bar. */
@Composable
fun FloatingNav(store: DesktopStore, modifier: Modifier = Modifier) {
    val h = hudColors
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 36.dp, end = 36.dp, top = 12.dp, bottom = 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(h.glass)
                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in NavItems) {
                FloatingNavTab(
                    item = item,
                    selected = store.tab == item.tab,
                    onClick = { store.tab = item.tab },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FloatingNavTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val h = hudColors
    val tint by animateColorAsState(
        if (selected) h.accent else h.textDim,
        tween(160),
        label = "navTint",
    )
    val bg by animateColorAsState(
        if (selected) h.accent.copy(alpha = 0.12f) else Color.Transparent,
        tween(160),
        label = "navBg",
    )
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) h.accent.copy(alpha = 0.16f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.glyph, fontSize = 14.sp, color = tint)
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
