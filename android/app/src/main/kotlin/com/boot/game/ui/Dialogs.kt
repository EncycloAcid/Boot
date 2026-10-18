package com.boot.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.AwardNight
import com.boot.core.Combo
import com.boot.core.EndingKind
import com.boot.core.EventChoice
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.PivotMode
import com.boot.core.PivotTrigger
import com.boot.core.ShowReport
import com.boot.core.ShowResults
import com.boot.core.acceptPivotBuyout
import com.boot.core.choose
import com.boot.core.keepPlaying
import com.boot.core.pivotBlocker
import com.boot.core.pivotChoices
import com.boot.core.ranked
import com.boot.core.resolvePivot
import com.boot.core.stayFocused
import com.boot.game.GameStore
import com.boot.game.audio.Sfx
import kotlin.math.roundToInt

private val fullScreen = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)

/**
 * A decision that blocks the calendar, as an iOS-style action sheet: it rises from the bottom and
 * can't be swiped away until you pick. The first choice is the filled button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EventSheet(store: GameStore, state: GameState) {
    val e = state.events.first()
    // An award night gets the whole stage.
    if (e.kind is AwardNight) {
        key(e.id) { AwardNightSheet(store, state, e) }
        return
    }
    // A fresh sheet per event, so each one rises on its own.
    key(e.id) {
        val sheet = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            confirmValueChange = { it != SheetValue.Hidden },
        )
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheet,
            sheetGesturesEnabled = false,
            containerColor = gameColors.grouped,
            // Sideways there's width to spare: let the sheet use it rather than squeezing the choices.
            sheetMaxWidth = if (LocalCompact.current) 880.dp else BottomSheetDefaults.SheetMaxWidth,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false, shouldDismissOnClickOutside = false),
        ) {
            Scaled {
                val compact = LocalCompact.current
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = if (compact) 28.dp else 20.dp, end = if (compact) 28.dp else 20.dp, bottom = if (compact) 22.dp else 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // The people it's about, or the stage it's on; otherwise which business it's for.
                    if (!EventArt(state, e.kind)) {
                        e.lane?.let { lane ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LaneBadge(lane, 32.dp)
                                Spacer(Modifier.width(8.dp))
                                Hint(lane.businessName)
                            }
                        }
                    }
                    Text(e.title, style = if (compact) MaterialTheme.typography.headlineSmallEmphasized else MaterialTheme.typography.headlineMediumEmphasized)
                    Text(e.body, style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                    (e.kind as? ShowResults)?.report?.let { StageFigures(it) }
                    // A stage gate: plan the next stage before deciding.
                    val plan = (e.kind as? com.boot.core.MilestoneReview)?.let { rememberStagePlan(state, it) }
                    plan?.let { StagePlanSection(state, it) }
                    Spacer(Modifier.size(4.dp))
                    if (compact) {
                        // Sideways, the choices sit side by side as cards so they can be weighed at a
                        // glance: three to a row, or two rows of two when there are four.
                        val perRow = if (e.choices.size == 4) 2 else minOf(3, e.choices.size)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            e.choices.withIndex().chunked(perRow).forEach { row ->
                                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    for ((i, choice) in row) {
                                        ChoiceCard(choice, primary = i == 0, Modifier.weight(1f).fillMaxHeight()) {
                                            store.perform {
                                                plan?.apply(this)
                                                choose(i)
                                            }
                                        }
                                    }
                                    // Keep a short last row's cards the same width as the rest.
                                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    } else {
                        e.choices.forEachIndexed { i, choice ->
                            ChoiceButton(choice, primary = i == 0) {
                                store.perform {
                                    plan?.apply(this)
                                    choose(i)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A choice as a card: the label over what it means, with room to read both. */
@Composable
internal fun ChoiceCard(choice: EventChoice, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = gameColors
    val sound = LocalSound.current
    val shape = RoundedCornerShape(18.dp)
    val container = if (primary) scheme.primary else colors.card
    val content = if (primary) scheme.onPrimary else scheme.onSurface
    Column(
        modifier
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(container)
            .then(if (primary) Modifier else Modifier.border(1.dp, colors.separator, shape))
            .clickable(role = Role.Button) {
                sound.play(Sfx.CLICK)
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(choice.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = content)
        if (choice.detail.isNotEmpty()) {
            Text(choice.detail, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceButton(choice: EventChoice, primary: Boolean, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(choice.label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(choice.detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
    val sized = modifier.heightIn(min = 60.dp)
    if (primary) {
        Button(onClick = onClick, shapes = ButtonDefaults.shapes(), modifier = sized) { content() }
    } else {
        FilledTonalButton(onClick = onClick, shapes = ButtonDefaults.shapes(), modifier = sized) { content() }
    }
}

/** The mid-game choice, as a full-screen page: pick a second lane and expand or pivot into it, or stay focused. */
@Composable
fun PivotDialog(store: GameStore, state: GameState) {
    val offer = state.pendingPivot ?: return
    val colors = gameColors
    var lane by rememberSaveable(offer.week) { mutableStateOf(offer.suggestedLane) }
    var mode by rememberSaveable(offer.week) { mutableStateOf(PivotMode.EXPAND) }
    var useVenture by rememberSaveable(offer.week) { mutableStateOf(false) }
    var confirmSale by remember { mutableStateOf(false) }

    GameDialog(onDismissRequest = {}, properties = fullScreen) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "The Pivot Point · ${offer.trigger.title}",
                            style = MaterialTheme.typography.labelLarge, color = colors.milestone, fontWeight = FontWeight.Bold,
                        )
                        Text(offer.headline, style = MaterialTheme.typography.headlineLarge)
                        Text(offer.body, style = MaterialTheme.typography.bodyLarge)
                    }
                    GroupedSection("Pick a second lane", footer = "Or stay focused: a second lane is optional, and you can still add one later once you're profitable.") {
                        state.pivotChoices.forEachIndexed { i, l ->
                            if (i > 0) RowDivider(68.dp)
                            PivotLaneRow(state, l, selected = l == lane, suggested = l == offer.suggestedLane) { lane = l }
                        }
                    }
                    GroupedSection(
                        "How do you take it on?",
                        footer = when (mode) {
                            PivotMode.PIVOT ->
                                "${mode.summary} The ${state.startingLane.businessName.lowercase()} sells for " +
                                    "${Format.money(offer.salePrice)}. People whose skills carry over come with you."
                            PivotMode.EXPAND -> "${mode.summary} Setup costs ${Format.money(lane?.setupCost ?: 0.0)}."
                            PivotMode.STAY -> mode.summary
                        },
                    ) {
                        CardBlock { Segmented(PivotMode.intoNewLane, mode, { it.title }) { mode = it } }
                        RowDivider()
                        ToggleRow("Fund it with venture money", useVenture, "Hardware and AI need millions. A round brings a board.") {
                            useVenture = it
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.separator)
                Column(Modifier.background(colors.bar).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val blocker = lane?.let { state.pivotBlocker(it, mode, useVenture) }
                    if (blocker != null) Hint(blocker, colors.bad)
                    PrimaryButton(
                        "${mode.title} into ${lane?.title ?: "…"}",
                        { lane?.let { l -> store.perform { resolvePivot(l, mode, useVenture) } } },
                        Modifier.fillMaxWidth(),
                        enabled = lane != null && blocker == null,
                    )
                    TonalButton(
                        "Stay focused on the ${state.startingLane.businessName.lowercase()}",
                        { store.perform { stayFocused() } },
                        Modifier.fillMaxWidth(),
                    )
                    if (offer.trigger == PivotTrigger.BUYOUT_OFFER) {
                        PlainButton("Sell the whole company and walk away", { confirmSale = true }, Modifier.fillMaxWidth(), colors.bad)
                    }
                }
            }
        }
        if (confirmSale) {
            ConfirmDialog(
                title = "Sell everything?",
                text = "${offer.bidder ?: "The buyer"} takes the whole company. The game ends here.",
                confirm = "Sell",
                destructive = true,
                onConfirm = { store.perform { acceptPivotBuyout() } },
                onDismiss = { confirmSale = false },
            )
        }
    }
}

@Composable
private fun PivotLaneRow(state: GameState, lane: Lane, selected: Boolean, suggested: Boolean, onClick: () -> Unit) {
    val colors = gameColors
    val combo = Combo.between(state.startingLane, lane)
    Box(Modifier.testTag("pivot-lane-${lane.name}")) {
        ListRow(
            lane.title + (if (suggested) " · head start" else "") + (if (lane.isCapitalIntensive) " · VC" else ""),
            subtitle = combo?.let { "Unlocks ${it.product}: ${it.pitch}" },
            leading = { LaneBadge(lane, 40.dp, selected = selected, morph = true) },
            onClick = onClick,
        ) {
            if (selected) Icon(Icons.Filled.CheckCircle, null, tint = colors.lane(lane), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** The story of your run, with an expressive burst behind the verdict. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EndingDialog(store: GameStore, state: GameState) {
    val e = state.ending ?: return
    val colors = gameColors
    val spin = rememberInfiniteTransition(label = "ending")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    val good = e.kind == EndingKind.IPO || e.kind == EndingKind.ACQUIRED || e.kind == EndingKind.INDIE
    val accent = if (good) MaterialTheme.colorScheme.primary else colors.bad
    GameDialog(onDismissRequest = {}, properties = fullScreen) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(132.dp)
                                    .rotate(angle)
                                    .clip((if (good) MaterialShapes.SoftBurst else MaterialShapes.Cookie9Sided).toShape())
                                    .background(accent.copy(alpha = 0.18f)),
                            )
                            Text(e.kind.emoji, style = MaterialTheme.typography.displaySmall)
                        }
                        Spacer(Modifier.size(12.dp))
                        Text(e.kind.title, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.SemiBold)
                        Text(e.title, style = MaterialTheme.typography.displaySmallEmphasized, textAlign = TextAlign.Center)
                        Spacer(Modifier.size(6.dp))
                        Text(e.summary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                        val rank = store.hall.ranked().indexOfFirst { it.seed == state.seed && it.company == state.companyName } + 1
                        if (rank > 0) {
                            Spacer(Modifier.size(10.dp))
                            Pill(
                                if (rank == 1 && store.hall.size > 1) "A new record: #1 in your Hall of Fame" else "#$rank of ${store.hall.size} in your Hall of Fame",
                                colors.milestone,
                            )
                        }
                    }
                    GroupedSection(null) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                            TileRow {
                                StatTile("Final valuation", Format.money(e.valuation), Modifier.weight(1f))
                                StatTile("You walk away with", Format.money(e.founderTakeHome), Modifier.weight(1f))
                            }
                        }
                        RowDivider()
                        ValueRow("Ended", GameClock.label(e.week))
                        RowDivider()
                        ValueRow("Prestige", if (e.ecosystem) "Ecosystem company" else "—", if (e.ecosystem) colors.milestone else Color.Unspecified)
                    }
                    GroupedSection("The story") {
                        e.story.forEachIndexed { i, line ->
                            if (i > 0) RowDivider()
                            Text(line, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                        }
                    }
                }
                Column(Modifier.background(colors.bar).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (e.kind == EndingKind.IPO || e.kind == EndingKind.INDIE) {
                        PrimaryButton(
                            if (e.kind == EndingKind.IPO && state.listing == null) "Keep playing as a public company" else "Keep playing",
                            { store.perform { keepPlaying() } },
                            Modifier.fillMaxWidth(),
                        )
                        TonalButton("New game", { store.abandon() }, Modifier.fillMaxWidth())
                    } else {
                        PrimaryButton("New game", { store.abandon() }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

private val EndingKind.emoji: String
    get() = when (this) {
        EndingKind.IPO -> "🔔"
        EndingKind.ACQUIRED -> "🤝"
        EndingKind.INDIE -> "🏠"
        EndingKind.BANKRUPT -> "💸"
        EndingKind.OUSTED -> "🚪"
    }
