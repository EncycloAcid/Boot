package com.boot.core

import kotlinx.serialization.Serializable

/** What finishing a goal pays out. */
data class GoalReward(val cash: Double = 0.0, val rp: Double = 0.0, val fans: Double = 0.0, val reputation: Double = 0.0) {
    val label: String
        get() = listOfNotNull(
            if (cash > 0) money(cash) else null,
            if (rp > 0) "${rp.toInt()} RP" else null,
            if (fans > 0) "${Format.count(fans)} fans" else null,
            if (reputation > 0) "+${reputation.toInt()} reputation" else null,
        ).joinToString(" · ")
}

private fun cash(v: Double) = GoalReward(cash = v)
private fun rp(v: Double) = GoalReward(rp = v)
private fun fans(v: Double) = GoalReward(fans = v)
private fun rep(v: Double) = GoalReward(reputation = v)

/**
 * Goals to work toward, roughly in the order a run meets them. Each one is a number to reach;
 * lane goals only count while you run that lane. The first few unfinished ones are always on show.
 */
@Serializable
enum class Goal(
    val title: String,
    val lane: Lane?,
    val reward: GoalReward,
    val target: Double,
    internal val measure: GameState.() -> Double,
) {
    // Getting started
    G_SHIP("Ship your first game", Lane.GAMES, GoalReward(rp = 5.0, fans = 500.0), 1.0, { games?.releases?.size?.toDouble() ?: 0.0 }),
    W_FIRST("Deliver your first website", Lane.WEBSITES, cash(5_000.0), 1.0, { web?.completed?.toDouble() ?: 0.0 }),
    A_LIVE("Launch your app", Lane.APPS, rp(5.0), 1.0, { if (apps?.isLive == true) 1.0 else 0.0 }),
    S_LIVE("Launch your platform", Lane.SOCIAL, rp(5.0), 1.0, { if (social?.isLive == true) 1.0 else 0.0 }),
    H_FIRST("Ship your first device", Lane.HARDWARE, rp(6.0), 1.0, { hardware?.modelsShipped?.toDouble() ?: 0.0 }),
    AI_FIRST("Ship your first model", Lane.AI, rp(6.0), 1.0, { ai?.generation?.toDouble() ?: 0.0 }),
    HIRE_FIRST("Hire your first employee", null, rp(3.0), 1.0, { staff.size.toDouble() }),
    EARN_25K("Earn $25k in revenue", null, cash(5_000.0), 25_000.0, { stats.totalRevenue }),
    RESEARCH_3("Research three things", null, cash(10_000.0), 3.0, { researched.size.toDouble() }),

    // Finding your feet
    G_SEVEN("Get a review of 7 or more", Lane.GAMES, cash(15_000.0), 7.0, { games?.topScore ?: 0.0 }),
    W_TEN("Deliver 10 websites", Lane.WEBSITES, rp(8.0), 10.0, { web?.completed?.toDouble() ?: 0.0 }),
    A_10K("Reach 10,000 users", Lane.APPS, cash(20_000.0), 10_000.0, { apps?.users ?: 0.0 }),
    S_100K("Reach 100,000 accounts", Lane.SOCIAL, rp(8.0), 100_000.0, { social?.users ?: 0.0 }),
    AI_SUBS("Reach 10,000 subscribers", Lane.AI, cash(100_000.0), 10_000.0, { ai?.subscribers ?: 0.0 }),
    PROFIT_QUARTER("Turn a profit over a quarter", null, GoalReward(rp = 5.0, reputation = 1.0), 1.0, {
        if (week >= 13 && trailingProfit(weeks = 13) > 0) 1.0 else 0.0
    }),
    MOVE_UP("Move out of the apartment", null, rp(6.0), 1.0, { office.ordinal.toDouble() }),
    TEAM_5("Grow the team to 5", null, cash(25_000.0), 5.0, { staff.size.toDouble() }),
    G_FANS_10K("Reach 10,000 fans", Lane.GAMES, rp(8.0), 10_000.0, { games?.fans ?: 0.0 }),

    // Making a name
    G_HIT("Make a hit: a review of 8 or more", Lane.GAMES, fans(5_000.0), 8.0, { games?.topScore ?: 0.0 }),
    G_ENGINE("Build your own engine", Lane.GAMES, rp(10.0), 2.0, { games?.engines?.size?.toDouble() ?: 0.0 }),
    W_STANDING("Reach 60% standing", Lane.WEBSITES, cash(50_000.0), 0.6, { web?.standing ?: 0.0 }),
    H_SELLOUT("Sell out a launch", Lane.HARDWARE, rep(2.0), 1.0, { stats.soldOuts.toDouble() }),
    BANK_250K("Have $250k in the bank", null, rp(10.0), 250_000.0, { cash }),
    WORTH_1M("Be worth $1M", null, rep(2.0), 1_000_000.0, { valuation }),
    SECOND_LANE("Run a second lane", null, rp(12.0), 2.0, { lanes.size.toDouble() }),
    H_THREE("Ship three devices", Lane.HARDWARE, cash(250_000.0), 3.0, { hardware?.modelsShipped?.toDouble() ?: 0.0 }),

    // Big time
    G_CHART("Top the sales chart", Lane.GAMES, cash(100_000.0), 1.0, { stats.chartToppers.toDouble() }),
    G_EXPANSION("Release an expansion pack", Lane.GAMES, fans(10_000.0), 1.0, {
        games?.releases?.sumOf { it.expansions.size }?.toDouble() ?: 0.0
    }),
    COMBO("Build a crossover product", null, cash(100_000.0), 1.0, { crossovers.count { it.built }.toDouble() }),
    REVENUE_1M("Make $1M in a year", null, rp(15.0), 1_000_000.0, { trailingRevenue(weeks = 52) }),
    TEAM_20("Grow the team to 20", null, cash(250_000.0), 20.0, { staff.size.toDouble() }),
    W_FIFTY("Deliver 50 websites", Lane.WEBSITES, rp(20.0), 50.0, { web?.completed?.toDouble() ?: 0.0 }),
    A_1M("Reach a million users", Lane.APPS, rep(3.0), 1_000_000.0, { apps?.users ?: 0.0 }),
    AI_CAP("Reach capability 60", Lane.AI, rp(20.0), 60.0, { ai?.capability ?: 0.0 }),
    WORTH_10M("Be worth $10M", null, rep(3.0), 10_000_000.0, { valuation }),

    // Legend
    G_NINE("Get a review of 9 or more", Lane.GAMES, rp(20.0), 9.0, { games?.topScore ?: 0.0 }),
    G_GOTY("Win Game of the Year", Lane.GAMES, cash(500_000.0), 1.0, { games?.market?.wins?.toDouble() ?: 0.0 }),
    G_FANS_1M("Reach a million fans", Lane.GAMES, rep(3.0), 1_000_000.0, { games?.fans ?: 0.0 }),
    G_AAA("Ship an AAA game", Lane.GAMES, rp(30.0), 1.0, {
        games?.releases?.count { it.concept.size == GameSize.AAA }?.toDouble() ?: 0.0
    }),
    S_10M("Reach 10 million accounts", Lane.SOCIAL, rep(3.0), 10_000_000.0, { social?.users ?: 0.0 }),
    WORTH_100M("Be worth $100M", null, rp(30.0), 100_000_000.0, { valuation }),
    ECOSYSTEM("Become an ecosystem company", null, rep(4.0), 1.0, { if (isEcosystem) 1.0 else 0.0 }),
    UNICORN("Be worth $1B", null, rep(5.0), 1_000_000_000.0, { valuation });

    /** 0...1 */
    fun progress(state: GameState): Double = (state.measure() / target).coerceIn(0.0, 1.0)

    fun appliesTo(state: GameState): Boolean = lane == null || lane in state.lanes
}

/** Unfinished goals for the lanes you run, in order. */
val GameState.openGoals: List<Goal> get() = Goal.entries.filter { it !in goalsDone && it.appliesTo(this) }

/** What's on show right now. */
val GameState.currentGoals: List<Goal> get() = openGoals.take(3)

internal fun GameState.checkGoals() {
    for (goal in openGoals) {
        if (goal.measure(this) < goal.target) continue
        goalsDone = goalsDone + goal
        val r = goal.reward
        if (r.cash > 0) {
            cash += r.cash
            recordRevenue(r.cash, null)
        }
        researchPoints += r.rp
        games?.let { it.fans += r.fans }
        reputation.adjust(r.reputation)
        addLog("Goal reached: ${goal.title}. Reward: ${r.label}.", LogTone.MILESTONE, goal.lane)
    }
}
