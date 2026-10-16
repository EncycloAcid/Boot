package com.boot.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.DeviceCategory
import com.boot.core.DeviceFocus
import com.boot.core.DeviceSpec
import com.boot.core.DeviceTier
import com.boot.core.FormFactor
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.bomMultiplier
import com.boot.core.canBundleOs
import com.boot.core.canOfferAppPack
import com.boot.core.startDevice
import com.boot.game.GameStore
import java.util.Locale

private val steps = listOf("Category & tier", "Form factor", "Components", "Summary")

/**
 * Hardware brief, mirrored on the game designer: category → chassis → component mix → summary.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceDesigner(store: GameStore, state: GameState) {
    if (state.hardware == null) return
    val colors = gameColors
    var step by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf(DeviceCategory.PHONE) }
    var tier by remember { mutableStateOf(DeviceTier.BUDGET) }
    var formFactor by remember { mutableStateOf(FormFactor.defaultFor(DeviceCategory.PHONE)) }
    val focus = remember { mutableStateListOf<Double>().apply { addAll(DeviceFocus.evenFocus()) } }
    var bundleOs by remember { mutableStateOf(false) }
    var appPack by remember { mutableStateOf(false) }

    fun close() {
        store.designingDevice = false
    }

    fun onCategory(c: DeviceCategory) {
        category = c
        val opts = FormFactor.options(c)
        if (formFactor !in opts) formFactor = FormFactor.defaultFor(c)
    }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { close() }) { Icon(Icons.Filled.Close, "Close") }
                    Column(Modifier.weight(1f)) {
                        Text("Design a device", style = MaterialTheme.typography.titleMedium)
                        Hint("Step ${step + 1} of ${steps.size} · ${steps[step]}")
                    }
                }
                LinearWavyProgressIndicator(
                    progress = { (step + 1f) / steps.size },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val motion = MaterialTheme.motionScheme
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(motion.defaultSpatialSpec()) { it / 6 * dir } + fadeIn()) togetherWith
                            (slideOutHorizontally(motion.fastSpatialSpec()) { -it / 6 * dir } + fadeOut())
                    },
                    modifier = Modifier.weight(1f),
                    label = "device-step",
                ) { s ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        when (s) {
                            0 -> DeviceCategoryStep(category, { onCategory(it) }, tier, { tier = it })
                            1 -> DeviceFormStep(category, formFactor) { formFactor = it }
                            2 -> DeviceFocusStep(category, focus)
                            else -> DeviceSummaryStep(
                                state, category, tier, formFactor, focus.toList(),
                                bundleOs, { bundleOs = it },
                                appPack, { appPack = it },
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.bar)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (step > 0) PlainButton("Back", { step -= 1 })
                        Spacer(Modifier.weight(1f))
                        if (step < steps.lastIndex) {
                            PrimaryButton("Next", { step += 1 })
                        } else {
                            PrimaryButton("Start design", {
                                store.perform {
                                    startDevice(
                                        category, tier,
                                        formFactor = formFactor,
                                        focus = focus.toList(),
                                        bundleOs = bundleOs,
                                        appPack = appPack,
                                    )
                                }
                                if (store.state?.hardware?.design != null) close()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCategoryStep(
    category: DeviceCategory,
    onCategory: (DeviceCategory) -> Unit,
    tier: DeviceTier,
    onTier: (DeviceTier) -> Unit,
) {
    val spec = DeviceSpec(category, tier)
    GroupedSection("Category", footer = "Phones move volume; laptops earn more per unit; headphones are the quick project.") {
        CardBlock { Segmented(DeviceCategory.entries, category, { it.title }, onCategory) }
    }
    GroupedSection("Tier", footer = "Budget sells more units; flagship earns more per unit and costs more to design.") {
        CardBlock { Segmented(DeviceTier.entries, tier, { it.title }, onTier) }
    }
    GroupedSection("List price") {
        ValueRow("Retail", Format.money(spec.price))
        RowDivider()
        ValueRow("Base BOM", Format.money(spec.unitCost))
        RowDivider()
        ValueRow("Weekly R&D", tier.weeklyRnD.perWeek())
    }
}

@Composable
private fun DeviceFormStep(category: DeviceCategory, formFactor: FormFactor, onForm: (FormFactor) -> Unit) {
    val opts = FormFactor.options(category)
    GroupedSection(
        "Form factor",
        footer = when (category) {
            DeviceCategory.PHONE -> "Compact is pocketable; Pro leans camera and silicon."
            DeviceCategory.LAPTOP -> "Ultraportable prioritises battery; Creator pushes performance."
            DeviceCategory.HEADPHONES -> "Buds travel light; over-ear wins on sound and build."
        },
    ) {
        CardBlock { Segmented(opts, formFactor.takeIf { it in opts } ?: opts.first(), { it.title }, onForm) }
    }
}

@Composable
private fun DeviceFocusStep(category: DeviceCategory, focus: MutableList<Double>) {
    Text(
        "Allocate engineering effort across the six component areas. Reviewers weight them differently by category.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    GroupedSection("Component mix") {
        CardBlock {
            DeviceFocus.ALL.forEachIndexed { i, area ->
                val total = focus.sum().coerceAtLeast(0.01)
                val slider = remember(category, i) { SliderState(focus[i].toFloat()) }
                Column {
                    Row {
                        Text(area.title(category), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(Format.percent(focus[i] / total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(state = slider, onValueChange = {
                        slider.value = it
                        focus[i] = it.toDouble()
                    })
                }
            }
        }
    }
}

@Composable
private fun DeviceSummaryStep(
    state: GameState,
    category: DeviceCategory,
    tier: DeviceTier,
    formFactor: FormFactor,
    focus: List<Double>,
    bundleOs: Boolean,
    onBundle: (Boolean) -> Unit,
    appPack: Boolean,
    onAppPack: (Boolean) -> Unit,
) {
    val spec = DeviceSpec(category, tier)
    val bomMult = bomMultiplier(focus)
    val unit = spec.unitCost * bomMult
    val lean = focus.mapIndexed { i, v -> DeviceFocus.ALL[i] to v }
        .sortedByDescending { it.second }
        .take(2)
        .joinToString(" & ") { it.first.title(category).lowercase() }
    GroupedSection("Brief") {
        ValueRow("Product", "${tier.title} ${category.title}")
        RowDivider()
        ValueRow("Form", formFactor.title)
        RowDivider()
        ValueRow("Review lean", lean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() })
    }
    GroupedSection(
        "Economics",
        footer = "High display and specialty ambition raises BOM and can squeeze margin.",
    ) {
        ValueRow("Est. BOM", Format.money(unit))
        RowDivider()
        ValueRow("List price", Format.money(spec.price))
        RowDivider()
        ValueRow("Margin", Format.money(spec.price - unit))
        RowDivider()
        ValueRow("Price band", "${Format.money(spec.price * 0.92)} – ${Format.money(spec.price * 1.08)}")
    }
    if (state.canBundleOs()) {
        GroupedSection(
            "Bundled OS",
            footer = "Your Platform Lab shipped recently. Bundling gives an OEM-style review bump and a Strategy ecosystem nudge.",
        ) {
            ToggleRow("Ship with your OS", bundleOs, "Reviewers score integration higher.") { onBundle(it) }
        }
    }
    if (state.canOfferAppPack()) {
        GroupedSection(
            "App pack",
            footer = "A small first-party app bundle for launch week hype. Once per model.",
        ) {
            ToggleRow("Include first-party apps", appPack, "Extra launch buzz when Apps is open.") { onAppPack(it) }
        }
    }
}
