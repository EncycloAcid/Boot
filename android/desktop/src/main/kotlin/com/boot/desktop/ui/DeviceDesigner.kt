package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.DeviceCategory
import com.boot.core.DeviceFocus
import com.boot.core.DeviceTier
import com.boot.core.FormFactor
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.canBundleOs
import com.boot.core.canOfferAppPack
import com.boot.core.startDevice
import com.boot.desktop.DesktopStore

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceDesigner(store: DesktopStore, state: GameState) {
    if (state.hardware == null) {
        store.closeDeviceDesigner()
        return
    }
    val h = hudColors
    var step by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf(DeviceCategory.PHONE) }
    var tier by remember { mutableStateOf(DeviceTier.BUDGET) }
    var form by remember { mutableStateOf(FormFactor.defaultFor(DeviceCategory.PHONE)) }
    val focus = remember { mutableStateListOf<Double>().also { it.addAll(DeviceFocus.evenFocus()) } }
    var bundleOs by remember { mutableStateOf(false) }
    var appPack by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { store.closeDeviceDesigner() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 400.dp, max = 560.dp)
                    .fillMaxWidth(0.58f)
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp)),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Design a device", style = MaterialTheme.typography.titleLarge, color = h.ink)
                        Hint("Step ${step + 1} of 3")
                    }
                    MacButton("Close", onClick = { store.closeDeviceDesigner() }, primary = false)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (step) {
                        0 -> {
                            GlassPanel {
                                Text("Category", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DeviceCategory.entries.forEach { c ->
                                        SelectChip(c.title, category == c) {
                                            category = c
                                            form = FormFactor.defaultFor(c)
                                        }
                                    }
                                }
                                Hint("Tier")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DeviceTier.entries.forEach { t ->
                                        SelectChip(t.title, tier == t) { tier = t }
                                    }
                                }
                                Hint("R&D ${Format.money(tier.weeklyRnD)}/wk while designing")
                            }
                        }
                        1 -> {
                            GlassPanel {
                                Text("Form factor", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FormFactor.options(category).forEach { f ->
                                        SelectChip(f.title, form == f) { form = f }
                                    }
                                }
                            }
                            GlassPanel {
                                Text("Component mix", style = MaterialTheme.typography.labelLarge, color = h.textDim)
                                Hint("Drag, then release — commits once.")
                                DeviceFocus.ALL.forEachIndexed { i, area ->
                                    CommitSlider(
                                        focus[i],
                                        { v -> "${area.title(category)} · ${Format.percent(v.toDouble())}" },
                                    ) { v -> focus[i] = v }
                                }
                            }
                        }
                        else -> {
                            GlassPanel {
                                Text("${tier.title} ${form.title} ${category.title}", style = MaterialTheme.typography.titleMedium, color = h.text)
                                Hint("Focus sum ${Format.percent(focus.sum())} — scoring normalizes.")
                                if (state.canBundleOs()) {
                                    SelectChip(if (bundleOs) "Bundle OS · on" else "Bundle OS · off", bundleOs) {
                                        bundleOs = !bundleOs
                                    }
                                }
                                if (state.canOfferAppPack()) {
                                    SelectChip(if (appPack) "App pack · on" else "App pack · off", appPack) {
                                        appPack = !appPack
                                    }
                                }
                                store.error?.let { Hint(it, gameColors.bad) }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFF4F6F8)).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (step > 0) MacButton("Back", onClick = { step -= 1 }, primary = false)
                    Spacer(Modifier.weight(1f))
                    if (step < 2) {
                        MacButton("Next", onClick = { step += 1 })
                    } else {
                        MacButton(
                            "Start design",
                            onClick = {
                                store.perform {
                                    startDevice(category, tier, form, focus.toList(), bundleOs, appPack)
                                }
                                if (store.state?.hardware?.design != null) store.closeDeviceDesigner()
                            },
                        )
                    }
                }
            }
        }
    }
}
