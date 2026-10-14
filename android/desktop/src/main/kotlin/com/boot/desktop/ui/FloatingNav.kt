package com.boot.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.desktop.DesktopStore
import com.boot.desktop.Tab

private data class NavItem(val tab: Tab, val label: String)

private val NavItems = listOf(
    NavItem(Tab.OFFICE, "Office"),
    NavItem(Tab.LANES, "Lanes"),
    NavItem(Tab.RESEARCH, "Research"),
    NavItem(Tab.TEAM, "Team"),
    NavItem(Tab.COMPANY, "Company"),
)

/**
 * Floating navigation pill — overlays the scene, not glued to screen edges like a Material bar.
 * Sits above a soft fade so content can scroll underneath.
 */
@Composable
fun FloatingNav(store: DesktopStore, modifier: Modifier = Modifier) {
    val c = spriteColors
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, c.ink.copy(alpha = 0.55f), c.ink.copy(alpha = 0.88f)),
                ),
            )
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 22.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(c.navPill)
                .border(BorderStroke(2.dp, c.panelEdge), RoundedCornerShape(28.dp))
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
    val c = spriteColors
    val tint by animateColorAsState(
        if (selected) c.amber else c.textDim,
        tween(160),
        label = "navTint",
    )
    val bg by animateColorAsState(
        if (selected) c.navSelected else Color.Transparent,
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
        NavSprite(item.tab, tint, selected)
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
