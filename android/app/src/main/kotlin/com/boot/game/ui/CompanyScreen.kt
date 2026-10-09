package com.boot.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.GameState
import com.boot.game.GameStore

/** Money, the board and the combos, behind one segmented control. */
@Composable
fun CompanyScreen(store: GameStore, state: GameState) {
    Column {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Segmented(listOf(0, 1), store.companyPage, { if (it == 0) "Money & Board" else "Combos" }) { store.companyPage = it }
        }
        if (store.companyPage == 0) MoneyScreen(store, state) else CombosScreen(store, state)
    }
}
