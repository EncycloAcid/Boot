package com.boot.game.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.Combo
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.IPO_ANNUAL_REVENUE
import com.boot.core.IPO_VALUATION
import com.boot.core.Lane
import com.boot.core.expand
import com.boot.core.expansionBlocker
import com.boot.core.goPublic
import com.boot.core.ipoBlocker
import com.boot.core.isEcosystem
import com.boot.core.raiseRound
import com.boot.core.roundOffer
import com.boot.core.LOAN_TERM_WEEKS
import com.boot.core.loanBlocker
import com.boot.core.loanLimit
import com.boot.core.loanOffer
import com.boot.core.loanPayoff
import com.boot.core.repayLoan
import com.boot.core.takeLoan
import com.boot.game.GameStore
import androidx.compose.foundation.lazy.LazyListScope

fun LazyListScope.moneyItems(store: GameStore, state: GameState) {
    run {
        item { OwnershipSection(store, state) }
        item { LoanSection(store, state) }
        item { BoardSection(state) }
        item { ExpansionSection(store, state) }
        item { ExitSection(store, state) }
    }
}

@Composable
private fun OwnershipSection(store: GameStore, state: GameState) {
    val v = state.venture
    val offer = state.roundOffer()
    GroupedSection(
        "Ownership",
        footer = offer.fold({ "Money comes with a board that sets growth targets and can overrule or fire you." }, { it.message }),
    ) {
        ValueRow("You own", Format.percent(v.founderEquity))
        RowDivider()
        ValueRow("Raised", Format.money(v.raised))
        v.rounds.forEach { r ->
            RowDivider()
            ListRow(r.name, subtitle = GameClock.label(r.week), value = Format.money(r.amount), valueDetail = "at ${Format.money(r.preMoney)} pre")
        }
        offer.onSuccess { o ->
            RowDivider()
            CardBlock(spacing = 10.dp) {
                Text(
                    "Investors would put in ${Format.money(o.amount)} for ${Format.percent(o.dilution)} of the company.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PrimaryButton("Raise a ${o.name}", { store.perform { raiseRound() } }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BoardSection(state: GameState) {
    val colors = gameColors
    val b = state.venture.board
    GroupedSection(
        "Board",
        footer = if (b == null) "No board. Nobody can overrule you." else "Below 40% they start overruling you. At zero they replace you.",
    ) {
        if (b == null) {
            ValueRow("Board seats", "None")
        } else {
            CardBlock { Meter("Confidence", b.confidence, if (b.confidence < 0.4) colors.bad else colors.good) }
            RowDivider()
            ValueRow("Growth target", "${Format.percent(b.growthTarget)} per quarter")
            b.lastGrowth?.let {
                RowDivider()
                ValueRow("Last quarter", Format.percent(it), if (it >= b.growthTarget) colors.good else colors.bad)
            }
            RowDivider()
            ValueRow("Grace quarters left", "${b.graceQuarters}")
            RowDivider()
            ValueRow("Demands met / defied", "${b.demandsMet} / ${b.demandsDefied}")
        }
    }
}

@Composable
private fun ExpansionSection(store: GameStore, state: GameState) {
    var useVenture by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Lane?>(null) }
    val colors = gameColors
    val blocker = state.expansionBlocker
    GroupedSection(
        "Expansion",
        footer = "Ecosystem company: Hardware + software (Apps or Games) + services (AI, Social or Websites). Hold it for a year.",
    ) {
        if (blocker != null) {
            CardBlock { Hint(blocker) }
        } else {
            ToggleRow("Fund it with venture money", useVenture, "A round brings a board that can overrule you.") { useVenture = it }
            Lane.entries.filter { it !in state.lanes }.forEach { lane ->
                RowDivider(68.dp)
                val combos = state.lanes.mapNotNull { Combo.between(it, lane)?.product }
                ActionRow(
                    title = lane.title,
                    detail = "Setup ${Format.money(lane.setupCost)}" +
                        (if (lane.isCapitalIntensive) " · needs ${Format.money(lane.selfFundThreshold)} or VC" else "") +
                        (if (combos.isEmpty()) "" else " · unlocks ${combos.joinToString(", ")}"),
                    action = "Expand",
                    leading = { LaneBadge(lane, 40.dp) },
                ) { confirm = lane }
            }
        }
        if (state.isEcosystem) {
            RowDivider()
            ListRow(
                "Ecosystem company",
                subtitle = "Antitrust pressure builds the whole time you hold it.",
                leading = { Icon(Icons.Filled.EmojiEvents, null, tint = colors.milestone, modifier = Modifier.size(28.dp)) },
                value = "${state.stats.ecosystemWeeks}/52",
                valueColor = colors.milestone,
            )
        }
    }
    confirm?.let { lane ->
        ConfirmDialog(
            title = "Expand into ${lane.title}?",
            text = "Setup costs ${Format.money(lane.setupCost)}" +
                (if (useVenture) ", funded by a new venture round." else ".") +
                " Another lane splits your attention.",
            confirm = "Expand",
            onConfirm = { store.perform { expand(lane, useVenture) } },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun ExitSection(store: GameStore, state: GameState) {
    var confirm by remember { mutableStateOf(false) }
    val blocker = state.ipoBlocker
    GroupedSection("Exit", footer = blocker ?: "Going public ends the game. Or stay private: reach year 15 and you stay independent.") {
        CardBlock {
            Meter("Toward an IPO valuation", state.enterpriseValue / IPO_VALUATION, caption = "${Format.money(state.enterpriseValue)} of ${Format.money(IPO_VALUATION)}")
            val annual = state.trailingRevenue(weeks = 52)
            Meter("Toward IPO revenue", annual / IPO_ANNUAL_REVENUE, caption = "${Format.money(annual)} of ${Format.money(IPO_ANNUAL_REVENUE)}")
            if (blocker == null) PrimaryButton("Go public", { confirm = true }, Modifier.fillMaxWidth())
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = "Ring the bell?",
            text = "${state.companyName} goes public and the game ends.",
            confirm = "Go public",
            onConfirm = { store.perform { goPublic() } },
            onDismiss = { confirm = false },
        )
    }
}

/** The bank: cash now, paid back weekly over a year. No board, no equity. */
@Composable
private fun LoanSection(store: GameStore, state: GameState) {
    val colors = gameColors
    val loan = state.loan
    if (loan != null) {
        val payoff = state.loanPayoff ?: loan.balance
        GroupedSection(
            "Bank loan",
            footer = "Repayments come out every week. Paying early saves some of the interest.",
        ) {
            CardBlock {
                Meter(
                    "Paid back", 1 - loan.balance / (loan.amount * (1 + loan.rate)), colors.good,
                    caption = "${Format.money(loan.balance)} left",
                )
            }
            RowDivider()
            ValueRow("Weekly repayment", loan.weeklyPayment.perWeek(), colors.bad)
            RowDivider()
            ActionRow("Pay it off now", "Clears the loan for ${Format.money(payoff)}.", "Repay", enabled = state.cash >= payoff) {
                store.perform { repayLoan() }
            }
        }
    } else {
        val offer = state.loanOffer()
        val blocker = state.loanBlocker
        GroupedSection(
            "Bank loan",
            footer = "The bank lends against your revenue and what the business is worth. " +
                "${(offer.rate * 100).toInt()}% interest, repaid over $LOAN_TERM_WEEKS weeks. Debt counts against your valuation.",
        ) {
            ActionRow(
                "Borrow ${Format.money(state.loanLimit)}",
                blocker ?: "Then ${offer.weeklyPayment.perWeek()} for a year.",
                "Borrow",
                enabled = blocker == null,
            ) { store.perform { takeLoan() } }
        }
    }
}
