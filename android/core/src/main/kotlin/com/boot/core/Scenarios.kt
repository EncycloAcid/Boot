package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Scenarios and sandbox, the two things every tycoon game has.
 * - A scenario is a handcrafted challenge: a starting situation, an objective and a deadline.
 *   Beat it in the first half of the time for gold, three quarters for silver, or by the deadline
 *   for bronze. When it's decided you can keep playing or call it a day.
 * - Sandbox gives you a billion dollars and no bankruptcy, and keeps out of the Hall of Fame.
 */

@Serializable
enum class Medal(val title: String) { BRONZE("Bronze"), SILVER("Silver"), GOLD("Gold") }

/** What a scenario asks you to reach. */
@Serializable
enum class Objective {
    VALUATION, YEARLY_REVENUE, BEST_SCORE, APP_USERS, SOCIAL_USERS, DEVICES_SOLD, CAPABILITY, BEAT_RIVAL, PROFIT_STREAK, FANS;

    /** How the target reads: "$500M", "5M users". */
    fun describe(target: Double): String = when (this) {
        VALUATION -> "Be worth ${money(target)}"
        YEARLY_REVENUE -> "Earn ${money(target)} in a year"
        BEST_SCORE -> "Ship a game that scores ${String.format(java.util.Locale.US, "%.0f", target)} or more"
        APP_USERS -> "Reach ${Format.count(target)} app users"
        SOCIAL_USERS -> "Reach ${Format.count(target)} people on your platform"
        DEVICES_SOLD -> "Sell ${Format.count(target)} devices"
        CAPABILITY -> "Train a model to capability ${target.toInt()}"
        BEAT_RIVAL -> "Drive your rival out of the market"
        PROFIT_STREAK -> "Stay profitable ${target.toInt()} weeks in a row"
        FANS -> "Win ${Format.count(target)} fans"
    }
}

@Serializable
enum class Scenario(
    val title: String,
    val blurb: String,
    val lane: Lane,
    /** The year it starts, or null for today. */
    val year: Int?,
    val objective: Objective,
    val target: Double,
    val years: Int,
    val cash: Double? = null,
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** People already on the payroll on day one. */
    val staff: Int = 0,
    /** An era to start in, for a run started today. */
    val era: Era? = null,
) {
    GARAGE_TO_GLORY(
        "Garage to Glory", "Just you, a laptop and a dream of making a masterpiece.",
        Lane.GAMES, null, Objective.BEST_SCORE, 9.0, 5,
    ),
    CULT_FOLLOWING(
        "Cult Following", "1977. Computers are coming home, and so are games. Build a following that lasts.",
        Lane.GAMES, 1977, Objective.FANS, 500_000.0, 10,
    ),
    CRASH_OF_83(
        "The Crash of '83", "Nobody wants video games any more. Prove them wrong.",
        Lane.GAMES, 1983, Objective.VALUATION, 50_000_000.0, 6, cash = 60_000.0,
    ),
    CONSOLE_KINGS(
        "Console Kings", "1985: the living room is up for grabs. Build the machines everyone wants.",
        Lane.HARDWARE, 1985, Objective.DEVICES_SOLD, 1_000_000.0, 8,
    ),
    DOT_COM_GOLD_RUSH(
        "Dot-com Gold Rush", "1995. Everyone needs a website and investors are throwing money around. Get rich before it ends.",
        Lane.WEBSITES, 1995, Objective.VALUATION, 200_000_000.0, 5,
    ),
    SURVIVE_THE_BUST(
        "Survive the Bust", "2000. The money's gone, the payroll isn't. You still have a name and two loyal clients: make them count.",
        Lane.WEBSITES, 2000, Objective.PROFIT_STREAK, 26.0, 3, cash = 300_000.0, staff = 3,
    ),
    SOCIAL_NETWORK(
        "The Social Network", "2004. Everyone's about to be online all the time. Be where they gather.",
        Lane.SOCIAL, 2004, Objective.SOCIAL_USERS, 20_000_000.0, 6,
    ),
    APP_FOR_THAT(
        "There's an App for That", "2008. The App Store just opened. Get on every home screen.",
        Lane.APPS, 2008, Objective.APP_USERS, 500_000.0, 4,
    ),
    FREE_TO_PLAY(
        "Whale Watching", "2012. Games are free and the gems cost money. Make a fortune.",
        Lane.GAMES, 2012, Objective.YEARLY_REVENUE, 20_000_000.0, 6,
    ),
    FRONTIER_LAB(
        "Frontier Lab", "The race to build the smartest machine is on, and compute is expensive.",
        Lane.AI, null, Objective.CAPABILITY, 100.0, 8,
    ),
    GIANT_SLAYER(
        "Giant Slayer", "Electronic Arts owns your market. Make them leave it.",
        Lane.GAMES, null, Objective.BEAT_RIVAL, 1.0, 8,
    ),
    WINTER_IS_HERE(
        "Winter Is Here", "Crypto winter: investors won't return your calls. Make money the old-fashioned way.",
        Lane.WEBSITES, null, Objective.PROFIT_STREAK, 52.0, 3, era = Era.CRYPTO_WINTER,
    ),
    ;

    val goal: String get() = objective.describe(target)
}

/** A scenario in play: its deadline and how it went. */
@Serializable
data class ScenarioRun(val scenario: Scenario, val deadlineWeek: Int, var medal: Medal? = null, var failed: Boolean = false) {
    val decided: Boolean get() = medal != null || failed
}

/** The scenario's outcome, as a decision: keep playing or stop here. */
@Serializable @SerialName("scenarioOver")
data class ScenarioOver(val won: Boolean) : EventKind

/** Start a scenario. */
fun GameState.Companion.scenario(
    scenario: Scenario,
    companyName: String,
    founderName: String,
    seed: Long = kotlin.random.Random.nextLong(),
    legacy: List<LegacyPerk> = emptyList(),
): GameState {
    val s = create(companyName, founderName, scenario.lane, seed, scenario.difficulty, legacy, StartPoint.TODAY, year = scenario.year)
    scenario.cash?.let { cash ->
        s.cash = cash
        s.ledger = listOf(WeekLedger(0, cash = cash))
    }
    repeat(scenario.staff) { s.hireDirect(s.makeCandidate(), scenario.lane) }
    if (scenario == Scenario.SURVIVE_THE_BUST) s.web?.let { web ->
        // A survivor of the boom: known around town, with clients who stayed.
        web.standing = 0.6
        web.retainers += Retainer(s.rng.pick(Names.brands), 4_000.0, 3.0, 0)
        web.retainers += Retainer(s.rng.pick(Names.clients), 2_500.0, 2.0, 0)
    }
    scenario.era?.let { era ->
        s.world.era = era
        s.world.eraStartWeek = 0
        s.world.eraLength = 104
        s.world.phase = EraPhase.PEAK
        s.world.eraIntensity = 1.0
        s.beginEra()
    }
    s.scenario = ScenarioRun(scenario, scenario.years * 52)
    s.addLog("Scenario: ${scenario.title}. ${scenario.goal} by ${GameClock.label(scenario.years * 52)}.", LogTone.MILESTONE)
    return s
}

/** How far along the scenario's objective is, 0...1. */
fun GameState.scenarioProgress(): Double {
    val run = scenario ?: return 0.0
    if (run.medal != null) return 1.0
    val t = run.scenario.target
    val now = when (run.scenario.objective) {
        Objective.VALUATION -> valuation
        Objective.YEARLY_REVENUE -> trailingRevenue(null, 52)
        Objective.BEST_SCORE -> games?.releases?.filter { it.publisher == null }?.maxOfOrNull { it.score } ?: 0.0
        Objective.APP_USERS -> apps?.users ?: 0.0
        Objective.SOCIAL_USERS -> social?.realUsers ?: 0.0
        Objective.DEVICES_SOLD -> (hardware?.lifetimeUnits ?: 0).toDouble()
        Objective.CAPABILITY -> ai?.capability ?: 0.0
        Objective.BEAT_RIVAL -> stats.rivalsBeaten.toDouble()
        Objective.PROFIT_STREAK -> stats.profitableStreak.toDouble()
        Objective.FANS -> games?.fans ?: 0.0
    }
    return (now / t).coerceIn(0.0, 1.0)
}

/** Each week: is the scenario won, or out of time? */
internal fun GameState.scenarioWeek() {
    val run = scenario ?: return
    if (run.decided) return
    if (scenarioProgress() >= 1.0) {
        val used = week.toDouble() / run.deadlineWeek
        run.medal = when {
            used <= 0.5 -> Medal.GOLD
            used <= 0.75 -> Medal.SILVER
            else -> Medal.BRONZE
        }
        addLog("Scenario complete: ${run.scenario.title}. ${run.medal!!.title} medal!", LogTone.MILESTONE)
        publish(NewsDesk.YOU, "The Economist", "$companyName does it", "${run.scenario.goal}: done, for a ${run.medal!!.title.lowercase()} medal.", LogTone.GOOD, lead = true)
        enqueue(ScenarioOver(won = true))
    } else if (week >= run.deadlineWeek) {
        run.failed = true
        addLog("Out of time: ${run.scenario.title} wasn't won.", LogTone.BAD)
        enqueue(ScenarioOver(won = false))
    }
}

internal fun GameState.describeScenario(k: ScenarioOver): Description? {
    val run = scenario ?: return null
    return if (k.won) {
        Description(
            "${run.medal!!.title} medal: ${run.scenario.title}",
            "${run.scenario.goal}: done, in ${Format.weeks(week)}. " + when (run.medal) {
                Medal.GOLD -> "In half the time or less. Remarkable."
                Medal.SILVER -> "With time to spare."
                else -> "Just in time."
            },
            listOf(choice("Keep playing", "See how far it goes."), choice("Retire on top", "End the run here.")),
        )
    } else {
        Description(
            "Out of time: ${run.scenario.title}",
            "${run.scenario.goal} by ${GameClock.label(run.deadlineWeek)} didn't happen. The company's still standing, though.",
            listOf(choice("Keep playing anyway", "No medal, but it's your company."), choice("Call it a day", "End the run here.")),
        )
    }
}

internal fun GameState.resolveScenario(k: ScenarioOver, choice: Int) {
    if (choice == 1) end(if (listing != null) EndingKind.IPO else EndingKind.INDIE)
}
