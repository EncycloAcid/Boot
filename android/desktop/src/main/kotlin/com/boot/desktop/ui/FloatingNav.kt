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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.desktop.DesktopStore
import com.boot.desktop.Tab

private data class NavItem(val tab: Tab, val label: String, val glyph: HudGlyph)

private val NavItems = listOf(
    NavItem(Tab.OFFICE, "Office", HudGlyph.OFFICE),
    NavItem(Tab.LANES, "Lanes", HudGlyph.LANES),
    NavItem(Tab.RESEARCH, "Research", HudGlyph.RESEARCH),
    NavItem(Tab.TEAM, "Team", HudGlyph.TEAM),
    NavItem(Tab.COMPANY, "Company", HudGlyph.COMPANY),
)

private val DockShape = RoundedCornerShape(18.dp)
private val TabShape = RoundedCornerShape(12.dp)

/** Soft white floating dock — same five tabs, GDT-light materials. */
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
                .shadow(12.dp, DockShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
                .clip(DockShape)
                .background(h.panel)
                .border(1.dp, h.panelBorder, DockShape)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in NavItems) {
                FloatingNavTab(
                    item = item,
                    selected = store.tab == item.tab,
                    onClick = {
                        store.tab = when {
                            item.tab == Tab.OFFICE -> Tab.OFFICE
                            store.tab == item.tab -> Tab.OFFICE
                            else -> item.tab
                        }
                    },
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
        if (selected) h.accent else h.inkMute,
        tween(160),
        label = "navTint",
    )
    val bg by animateColorAsState(
        if (selected) h.accentSoft else Color.Transparent,
        tween(160),
        label = "navBg",
    )
    Column(
        modifier
            .clip(TabShape)
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
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            HudIcon(item.glyph, tint, 18.dp)
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            Modifier
                .width(if (selected) 16.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) h.accent else Color.Transparent),
        )
    }
}
