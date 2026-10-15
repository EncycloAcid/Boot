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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.boot.desktop.DesktopStore
import com.boot.desktop.Tab

private data class NavItem(val tab: Tab, val label: String, val glyph: HudGlyph, val hint: String)

private val NavItems = listOf(
    NavItem(Tab.OFFICE, "Office", HudGlyph.OFFICE, "Studio floor"),
    NavItem(Tab.LANES, "Lanes", HudGlyph.LANES, "Businesses"),
    NavItem(Tab.RESEARCH, "Research", HudGlyph.RESEARCH, "Unlocks"),
    NavItem(Tab.TEAM, "Team", HudGlyph.TEAM, "People"),
    NavItem(Tab.COMPANY, "Company", HudGlyph.COMPANY, "Money"),
    NavItem(Tab.NEWS, "News", HudGlyph.CALENDAR, "Press"),
)

private val DockShape = RoundedCornerShape(20.dp)
private val TabShape = RoundedCornerShape(14.dp)

/** Soft white floating dock — larger targets, unread News badge, clear selected state. */
@Composable
fun FloatingNav(store: DesktopStore, modifier: Modifier = Modifier) {
    val h = hudColors
    val unreadNews = (store.state?.news?.count { it.id > store.newsSeen } ?: 0).coerceAtLeast(0)
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .shadow(14.dp, DockShape, clip = false, ambientColor = h.shadow, spotColor = h.shadow)
                .clip(DockShape)
                .background(h.panel)
                .border(1.dp, h.panelBorder, DockShape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in NavItems) {
                FloatingNavTab(
                    item = item,
                    selected = store.tab == item.tab,
                    badge = if (item.tab == Tab.NEWS && unreadNews > 0) unreadNews else 0,
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
    badge: Int,
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
            .defaultMinSize(minHeight = 52.dp)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            HudIcon(item.glyph, tint, 18.dp)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(h.bugs),
                )
            }
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
        // Hint only when selected — keeps dock short enough for 800px windowed height.
        if (selected) {
            Text(
                item.hint,
                style = MaterialTheme.typography.labelSmall,
                color = h.accent.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .width(if (selected) 18.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(if (selected) h.accent else Color.Transparent),
        )
    }
}
