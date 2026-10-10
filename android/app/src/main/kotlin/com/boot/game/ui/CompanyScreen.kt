package com.boot.game.ui

import androidx.compose.runtime.Composable
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.game.GameStore

/** The company as a whole: an overview, money and the board, and the combos. */
@Composable
fun CompanyScreen(store: GameStore, state: GameState) {
    val pages = listOf("Overview", "Money", "Combos", "Goals")
    ScreenColumn {
        item { ScreenHeader("Company", "You own ${Format.percent(state.venture.founderEquity)} of ${state.companyName}") }
        item { Segmented(pages.indices.toList(), store.companyPage, { pages[it] }) { store.companyPage = it } }
        when (store.companyPage) {
            0 -> overviewItems(store, state)
            1 -> moneyItems(store, state)
            2 -> combosItems(store, state)
            else -> item { GoalsList(state) }
        }
    }
}
