package com.boot.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.AIProduct
import com.boot.core.AppCategory
import com.boot.core.AppMonetization
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.OsTarget
import com.boot.core.TrainingSize
import com.boot.core.UpdateFocus
import com.boot.core.UpdateStyle
import com.boot.core.answer
import com.boot.core.buyGPUs
import com.boot.core.osTargetOpen
import com.boot.core.setAIFocus
import com.boot.core.setAdLoad
import com.boot.core.setAppCategory
import com.boot.core.setModeration
import com.boot.core.setMonetization
import com.boot.core.setUpdateFocus
import com.boot.core.setUpdateStyle
import com.boot.core.shipOs
import com.boot.core.startOsBeta
import com.boot.core.startOsVersion
import com.boot.core.startTraining
import com.boot.desktop.DesktopStore

@Composable
internal fun AppsLanePanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val a = state.apps
    GlassPanel {
        Text("App studio", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (a == null) {
            Hint("Apps lane not open.")
            return@GlassPanel
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LaneStat("Users", Format.count(a.users))
            LaneStat("Quality", "${a.quality.toInt()}")
            LaneStat("Status", if (a.isLive) "Live" else "Building")
        }
        Hint(a.name)
        if (!a.isLive) {
            Hint("Category (locked after ship)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppCategory.entries.forEach { c ->
                    SelectChip(c.title, a.category == c) { store.perform { setAppCategory(c) } }
                }
            }
        } else {
            Hint("${a.category.title} · ${a.monetization.title}")
        }
        Hint("Monetization")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppMonetization.entries.forEach { m ->
                SelectChip(m.title, a.monetization == m) { store.perform { setMonetization(m) } }
            }
        }
        Hint("Update style")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UpdateStyle.entries.forEach { s ->
                SelectChip(s.title, a.updateStyle == s) { store.perform { setUpdateStyle(s) } }
            }
        }
        Hint("Next update focus")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UpdateFocus.entries.forEach { f ->
                SelectChip(f.title, a.updateFocus == f) { store.perform { setUpdateFocus(f) } }
            }
        }
        Hint(a.updateFocus.detail)
    }
}

@Composable
internal fun SocialLanePanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val s = state.social
    GlassPanel {
        Text("Social platform", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (s == null) {
            Hint("Social lane not open.")
            return@GlassPanel
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LaneStat("People", Format.count(s.realUsers))
            LaneStat("Bots", Format.percent(s.botShare))
            LaneStat("Status", if (s.isLive) "Live" else "Building")
        }
        Hint("Moderation ${Format.percent(s.moderation)}")
        Slider(
            value = s.moderation.toFloat(),
            onValueChange = { v -> store.perform { setModeration(v.toDouble()) } },
            valueRange = 0f..1f,
        )
        Hint("Ad load ${Format.percent(s.adLoad)}")
        Slider(
            value = s.adLoad.toFloat(),
            onValueChange = { v -> store.perform { setAdLoad(v.toDouble()) } },
            valueRange = 0f..1f,
        )
        Hint("Higher ads earn more and grow slower; moderation fights scandals and bots.")
    }
}

@Composable
internal fun AiLanePanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val lab = state.ai
    GlassPanel {
        Text("AI lab", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (lab == null) {
            Hint("AI lane not open.")
            return@GlassPanel
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LaneStat("Capability", "${lab.capability.toInt()}")
            LaneStat("GPUs", "${lab.gpusOwned}")
            LaneStat("Subs", Format.count(lab.subscribers))
        }
        lab.modelName?.let { Hint("Live model · $it", colors.good) }
        Hint("Product focus · ${lab.focus.title}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AIProduct.entries.forEach { p ->
                SelectChip(p.title, lab.focus == p) { store.perform { setAIFocus(p) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacButton("Buy 1 GPU", onClick = { store.perform { buyGPUs(1) } }, primary = false)
            MacButton("Buy 4 GPUs", onClick = { store.perform { buyGPUs(4) } }, primary = false)
        }
        lab.training?.let { t ->
            Hint("Training ${t.codename} · ${t.size.title} · ${Format.percent(t.completion)}")
        } ?: run {
            Hint("Start a training run — burns compute every week.")
            TrainingSize.entries.forEach { size ->
                MacButton(
                    "${size.title} (~${size.weeks}w · ${size.computeUnits} GPU-units)",
                    onClick = { store.perform { startTraining(size) } },
                    primary = size == TrainingSize.SMALL,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        lab.pendingModel?.let {
            Hint("Model ready: ${it.codename} — answer it from the Office inbox.", colors.warn)
        }
    }
}

@Composable
internal fun OsLanePanel(store: DesktopStore, state: GameState) {
    val h = hudColors
    val colors = gameColors
    val lab = state.os
    GlassPanel {
        Text("Platform Lab", style = MaterialTheme.typography.titleLarge, color = h.accent)
        if (lab == null) {
            Hint("OS lane not open.")
            return@GlassPanel
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LaneStat("Ecosystem", Format.percent(lab.ecosystem))
            LaneStat("Versions", "${lab.versionsShipped}")
            LaneStat("OEM trust", Format.percent(lab.oemTrust))
        }
        val p = lab.project
        if (p != null) {
            Text(p.codename, style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint("v${p.version} · ${p.target.title} · ${if (p.isReady) "ready" else Format.percent(p.completion)}")
            Hint("Design ${p.design.toInt()} · tech ${p.tech.toInt()} · bugs ${p.bugs.toInt()}")
            if (p.inBeta) {
                Hint("In beta · ${p.betaWeeksLeft}w left", colors.milestone)
            } else if (!p.isReady) {
                Hint("Advance weeks to build; beta cuts bugs before ship.")
            }
            if (!p.inBeta && p.completion > 0.55) {
                MacButton("Start 6-week beta", onClick = { store.perform { startOsBeta(6) } }, primary = false)
            }
            MacButton(
                "Ship ${p.codename}",
                onClick = { store.perform { shipOs() } },
                enabled = p.isReady,
                primary = true,
            )
        } else {
            Hint("Start a version — capital-intensive, OEM licensing after ship.")
            var target by remember { mutableStateOf(OsTarget.PC) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OsTarget.entries.forEach { t ->
                    val open = state.osTargetOpen(t)
                    SelectChip(
                        if (open) t.title else "${t.title} · ${t.arrives}",
                        target == t,
                    ) { if (open) target = t }
                }
            }
            Hint("~${Format.money(target.weeklyBurn)}/wk burn while in development")
            MacButton(
                "Start ${target.title} version",
                onClick = { store.perform { startOsVersion(target) } },
                primary = true,
            )
        }
    }
}

/** Soft inbox decisions that don't stop the calendar — same answer() path as Android. */
@Composable
fun InboxPanel(store: DesktopStore, state: GameState) {
    if (state.inbox.isEmpty()) return
    val h = hudColors
    val colors = gameColors
    GlassPanel {
        Text("Inbox · ${state.inbox.size}", style = MaterialTheme.typography.titleLarge, color = h.accent)
        Hint("Optional — the clock keeps running. Ignore one too long and it counts as a no.")
        state.inbox.forEach { item ->
            Spacer(Modifier.height(6.dp))
            val left = item.expiresWeek - state.week
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = h.text)
            Hint(item.body)
            Hint(if (left <= 0) "Last week" else "${Format.weeks(left)} left", if (left <= 1) colors.warn else h.inkMute)
            item.choices.forEachIndexed { c, choice ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(choice.label, style = MaterialTheme.typography.labelLarge, color = h.text)
                        Hint(choice.detail)
                    }
                    MacButton(
                        if (c == 0) "Yes" else "Reply",
                        onClick = { store.perform { answer(item.id, c) } },
                        primary = c == 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun LaneStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = hudColors.textDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = hudColors.text)
    }
}
