package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameProject
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.Platform
import com.boot.core.STORE_BUILD_COST
import com.boot.core.STORE_BUILD_WEEKS
import com.boot.core.StoreCuts
import com.boot.core.Storefront
import com.boot.core.defaultStores
import com.boot.core.epicGuarantee
import com.boot.core.exclusiveBlocker
import com.boot.core.exclusiveFee
import com.boot.core.exclusiveTargets
import com.boot.core.giveawayCost
import com.boot.core.openStore
import com.boot.core.openStoreBlocker
import com.boot.core.ownStoreReach
import com.boot.core.pcPlayers
import com.boot.core.sellStore
import com.boot.core.setGiveaways
import com.boot.core.setStore
import com.boot.core.setStoreCut
import com.boot.core.signStoreExclusive
import com.boot.core.storeBlocker
import com.boot.core.storeName
import com.boot.core.storeTerms
import com.boot.core.storeValue
import com.boot.core.storesFor
import com.boot.core.storesOpen
import com.boot.game.GameStore

/*
 * Game stores: where a PC game is sold, and your own store.
 */

/** Which stores a PC game in development will be sold on. */
@Composable
fun StoreControls(store: GameStore, state: GameState, p: GameProject, team: Int) {
    if (p.deal != null || Platform.PC !in p.concept.platforms) return
    val open = state.storesOpen()
    if (open.size < 2) return
    val chosen = state.storesFor(p)
    val terms = state.storeTerms(chosen, remaster = p.concept.remasterOf != null)
    Column(Modifier.testTag("stores-$team"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Sold on (PC)", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            open.forEach { s ->
                val on = s in chosen
                val blocker = state.storeBlocker(p, s)
                FilterChip(
                    selected = on,
                    onClick = { store.perform { setStore(team, s, !on) } },
                    enabled = on || blocker == null,
                    label = { Text("${state.storeName(s)} · ${Format.percent(s.cut)}") },
                )
            }
        }
        val usual = chosen == state.defaultStores()
        Hint(
            "Reaches ${Format.percent(terms.reach)} of PC players" +
                (if (usual) ", as usual" else " (${Format.percent(terms.lift)} of the usual)") +
                ", and you keep ${Format.percent(terms.keep)} of each sale." +
                (state.epicGuarantee(p)?.let { " Epic pays ${Format.money(it)} at launch to have it alone; some players won't like it." } ?: ""),
            if (terms.lift < 0.7) gameColors.warn else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Say why a store is greyed out.
        open.filter { it !in chosen }.mapNotNull { state.storeBlocker(p, it) }.distinct().forEach { Hint(it) }
    }
}

/** Your own game store: build it, run it, sell it. */
@Composable
fun OwnStoreSection(store: GameStore, state: GameState, g: GamesStudio) {
    Sections { OwnStoreSections(store, state, g) }
}

@Composable
private fun OwnStoreSections(store: GameStore, state: GameState, g: GamesStudio) {
    val colors = gameColors
    val s = g.store
    if (s == null) {
        val blocker = state.openStoreBlocker()
        GroupedSection(
            "A store of your own",
            Modifier.testTag("own-store"),
            footer = "Take on Steam. Players who sign up buy other studios' games and you take the cut. Your own PC games sold there keep every penny.",
        ) {
            CardBlock {
                Text(
                    "Build a PC game store: ${Format.money(STORE_BUILD_COST)} and half a year's work. Then set its cut, give games away to pull players in, and pay for exclusives.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PrimaryButton("Build a store · ${Format.money(STORE_BUILD_COST)}", { store.perform { openStore() } }, Modifier.fillMaxWidth(), enabled = blocker == null)
                if (blocker != null) Hint(blocker)
            }
        }
        return
    }
    if (!s.isOpen(state.week)) {
        GroupedSection(s.name, Modifier.testTag("own-store")) {
            CardBlock {
                Meter("Building", (state.week - s.startedWeek).toDouble() / STORE_BUILD_WEEKS, colors.milestone)
                Hint("Opens ${GameClock.label(s.openWeek)}.")
            }
        }
        return
    }
    val profit = s.lastRevenue - s.lastCost
    GroupedSection(
        "${s.name} · ${Format.signedMoney(profit)}/wk",
        Modifier.testTag("own-store"),
        footer = "A lower cut brings studios and their games, and more games bring players; a higher one earns more on each sale. Giveaways pull players in fast, at a price. When your store gets big, Valve answers.",
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Players", Format.count(s.users * 1_000_000), Modifier.weight(1f), "${Format.percent(state.ownStoreReach())} of PC")
            StatTile("Games", Format.count(s.catalogue.toDouble()), Modifier.weight(1f))
            StatTile("Takes in", Format.money(s.lastRevenue), Modifier.weight(1f), "a week", tint = colors.good)
        }
        RowDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your cut", style = MaterialTheme.typography.labelLarge)
            Segmented(StoreCuts, StoreCuts.minBy { kotlin.math.abs(it - s.cut) }, { Format.percent(it) }) { cut -> store.perform { setStoreCut(cut) } }
        }
        RowDivider()
        ToggleRow("Free games every week", s.giveaways, "${Format.money(giveawayCost(s))} a week. Players sign up for the free games and stay for the rest.") { on ->
            store.perform { setGiveaways(on) }
        }
        if (state.week < s.squeezedUntil) {
            RowDivider()
            CardBlock { Hint("Valve is fighting back: growth is slower until ${GameClock.label(s.squeezedUntil)}.", colors.warn) }
        }
        RowDivider()
        CardBlock {
            Hint("Earned ${Format.money(s.earned)} so far, against ${Format.money(s.spent)} spent. ${Format.count(state.pcPlayers() * 1_000_000)} people play on PC.")
            TonalButton("Sell ${s.name} · ${Format.money(state.storeValue)}", { store.perform { sellStore() } }, Modifier.fillMaxWidth())
        }
    }
    val targets = state.exclusiveTargets().take(4)
    val signed = s.exclusives.filter { !it.landed }
    if (targets.isNotEmpty() || signed.isNotEmpty()) {
        GroupedSection(
            "Exclusives",
            Modifier.testTag("store-exclusives"),
            footer = "Pay a studio to sell its next game only on ${s.name} for a year. When it comes out, its players sign up. The biggest games only come to a store with players.",
        ) {
            signed.forEachIndexed { i, e ->
                if (i > 0) RowDivider()
                ListRow(e.title, subtitle = "${e.studio} · out ${GameClock.label(e.week)}", trailing = { Pill("Signed", colors.good) })
            }
            targets.forEachIndexed { i, a ->
                if (i > 0 || signed.isNotEmpty()) RowDivider()
                val blocker = state.exclusiveBlocker(a)
                ListRow(
                    a.title,
                    subtitle = "${a.studio} · out ${GameClock.label(a.week)}" + (blocker?.let { "\n$it" } ?: ""),
                    trailing = { PlainButton(Format.money(state.exclusiveFee(a)), { store.perform { signStoreExclusive(a.title) } }, enabled = blocker == null) },
                )
            }
        }
    }
}
