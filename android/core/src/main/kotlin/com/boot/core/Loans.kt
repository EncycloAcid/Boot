package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.min

/** A bank loan: the cash now, a fixed weekly repayment for a year. */
@Serializable
data class Loan(
    val amount: Double,
    var balance: Double,
    val weeklyPayment: Double,
    val rate: Double,
    val takenWeek: Int,
)

data class LoanOffer(val amount: Double, val weeklyPayment: Double, val rate: Double)

const val LOAN_TERM_WEEKS = 52

/** What the bank will lend: some of a year's revenue plus a slice of what the business is worth. */
val GameState.loanLimit: Double
    get() {
        val raw = trailingRevenue(weeks = 52) * 0.4 + enterpriseValue * 0.15
        val capped = raw.clamped(15_000.0, 5_000_000.0) * (if (difficulty == Difficulty.EASY) 1.3 else 1.0)
        return floor(capped / 1_000) * 1_000
    }

fun GameState.loanOffer(amount: Double = loanLimit): LoanOffer {
    val rate = difficulty.loanRate
    return LoanOffer(amount, amount * (1 + rate) / LOAN_TERM_WEEKS, rate)
}

val GameState.loanBlocker: String?
    get() {
        if (ending != null) return "The game is over."
        if (loan != null) return "Pay off your current loan first."
        return null
    }

fun GameState.takeLoan(amount: Double = loanLimit) {
    requireLive()
    loanBlocker?.let { throw notAvailable(it) }
    if (amount <= 0 || amount > loanLimit) throw invalid("The bank will lend up to ${money(loanLimit)}.")
    val offer = loanOffer(amount)
    loan = Loan(amount, amount * (1 + offer.rate), offer.weeklyPayment, offer.rate, week)
    cash += amount
    stats.loans += 1
    addLog(
        "The bank lent you ${money(amount)}. You'll repay ${money(offer.weeklyPayment)} a week for a year.",
        LogTone.NEUTRAL,
    )
}

/** Clear the loan early. You still owe the interest already built into the balance, minus a discount. */
fun GameState.repayLoan() {
    requireLive()
    val l = loan ?: throw notAvailable("You don't have a loan.")
    val payoff = l.balance * (1 - 0.5 * l.rate / (1 + l.rate))
    if (cash < payoff) throw insufficientFunds("Paying it off takes ${money(payoff)}.")
    cash -= payoff
    recordCost(payoff - payoff / (1 + l.rate), null)
    loan = null
    addLog("Loan paid off early: ${money(payoff)}.", LogTone.GOOD)
}

/** What it costs to clear the loan today. */
val GameState.loanPayoff: Double? get() = loan?.let { it.balance * (1 - 0.5 * it.rate / (1 + it.rate)) }

internal fun GameState.payLoan() {
    val l = loan ?: return
    val pay = min(l.balance, l.weeklyPayment)
    cash -= pay
    l.balance -= pay
    // Only the interest is a cost; the rest is paying back what you borrowed.
    recordCost(pay * l.rate / (1 + l.rate), null)
    if (l.balance <= 0.5) {
        loan = null
        addLog("The bank loan is paid off.", LogTone.GOOD)
    }
}
