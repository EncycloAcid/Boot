package com.boot.game.ui

import androidx.compose.runtime.Composable
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.game.GameStore

/** The company as a whole: overview, money, markets, strategy, combos, and goals. */
@Composable
fun CompanyScreen(store: GameStore, state: GameState) {
    val pages = listOf("Overview", "Money", "Markets", "Strategy", "Combos", "Goals")
    ScreenColumn {
        item { ScreenHeader("Company", "You own ${Format.percent(state.venture.founderEquity)} of ${state.companyName}") }
        item { Segmented(pages.indices.toList(), store.companyPage, { pages[it] }) { store.companyPage = it } }
        when (store.companyPage) {
            0 -> overviewItems(store, state)
            1 -> moneyItems(store, state)
            2 -> marketsItems(store, state)
            3 -> strategyItems(store, state)
            4 -> combosItems(store, state)
            else -> {
                item { Sections { GoalsList(state) } }
                item { AchievementsSection(state) }
            }
        }
    }
}
