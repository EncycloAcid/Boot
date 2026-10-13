package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Rivals that fight back. Each lane's main rival keeps an eye on you: the more of their market you
 * take, the hotter they run (calm, then wary, then hostile), and a hostile rival makes moves.
 * Copycat products, price wars, ad blitzes, raids on your best people, lawsuits, exclusive deals and
 * smear campaigns, each one a decision with a real cost either way. Most leave a pressure on your
 * market for a few months. You can strike first too: undercut them, poach their stars or run attack
 * ads, all of which make them angrier. Push a rival's strength low enough and they pull back from
 * the market for a year.
 */

/** Something squeezing (or, when it's yours, boosting) a lane for a while. */
@Serializable
data class Pressure(
    val lane: Lane,
    val title: String,
    val demand: Double,
    var weeksLeft: Int,
    val cost: Double = 1.0,
    val output: Double = 1.0,
    /** Your own campaign or deal, not a rival's. */
    val mine: Boolean = false,
    val rival: String? = null,
)

/** How a rival feels about you. */
enum class RivalStance(val title: String, val detail: String) {
    CALM("Calm", "Not paying you much attention."),
    WARY("Wary", "Watching you. A move could come."),
    HOSTILE("Hostile", "Out to get you. Expect a move soon."),
}

val Rival.stance: RivalStance
    get() = when {
        heat < 0.3 -> RivalStance.CALM
        heat < 0.6 -> RivalStance.WARY
        else -> RivalStance.HOSTILE
    }

/** The moves a rival can make against you. */
@Serializable
enum class RivalMoveKind(val title: String) {
    COPYCAT("Copycat"),
    PRICE_WAR("Price war"),
    AD_BLITZ("Ad blitz"),
    TALENT_RAID("Talent raid"),
    LAWSUIT("Lawsuit"),
    EXCLUSIVE_DEAL("Exclusive deal"),
    SMEAR("Smear campaign"),
}

@Serializable
sealed interface RivalEvent : EventKind

@Serializable @SerialName("rivalMove")
data class RivalMove(val rival: String, val lane: Lane, val move: RivalMoveKind, val targets: List<Int> = emptyList()) : RivalEvent

/** The moves you can make against a rival. */
@Serializable
enum class CounterMove(val title: String, val detail: String) {
    UNDERCUT("Undercut them", "Cut your prices for three months: 15% more demand, thinner margins. They lose ground and get angrier."),
    POACH_STAR("Poach a star", "Lure one of their best people to your team with a signing bonus. Needs a free desk."),
    ATTACK_ADS("Attack ads", "A campaign aimed squarely at them. They lose ground and you win a little demand. It looks a bit petty."),
}

/** A weekly revenue that makes you a real player in a lane, as far as its rival is concerned. */
private fun laneScale(lane: Lane): Double = when (lane) {
    Lane.GAMES -> 300_000.0
    Lane.WEBSITES -> 60_000.0
    Lane.APPS -> 100_000.0
    Lane.SOCIAL -> 150_000.0
    Lane.HARDWARE -> 600_000.0
    Lane.AI -> 400_000.0
    Lane.OS -> 500_000.0
}

/** Weeks between a rival's moves, at least. */
private const val MOVE_COOLDOWN = 16

/** Weeks between your counter-moves against the same rival. */
const val COUNTER_COOLDOWN = 13

/** Pressures squeeze or lift a lane's demand, costs and output. */
internal fun GameState.applyPressures(lane: Lane, mods: LaneModifiers) {
    for (p in world.pressures) {
        if (p.lane != lane) continue
        mods.demand *= p.demand
        mods.cost *= p.cost
        mods.output *= p.output
    }
    if (hybrid) mods.output *= 0.97
}

/** Rivals watch, warm up, and make their moves. Pressures run down. */
internal fun GameState.rivalWeek() {
    val ended = world.pressures.filter { it.weeksLeft <= 1 }
    for (p in world.pressures) p.weeksLeft -= 1
    world.pressures.removeAll { it.weeksLeft <= 0 }
    for (p in ended) {
        if (p.lane in lanes) addLog(if (p.mine) "${p.title}: that's done." else "${p.title} is over.", LogTone.NEUTRAL, p.lane)
    }
    for (r in world.rivals) {
        if (r.lane !in lanes) {
            r.heat *= 0.98
            continue
        }
        val threat = (trailingRevenue(r.lane, 4) / 4 / laneScale(r.lane)).clamped(0.0, 2.0)
        r.heat = (r.heat * 0.99 + 0.0015 + 0.006 * threat).clamped(0.0, 1.0)

        // Knocked out of the market for a while.
        if (r.strength < 0.12 && r.quietUntil <= week) {
            r.quietUntil = week + 52
            r.heat = 0.0
            stats.rivalsBeaten += 1
            addLog("${r.name} is pulling back from ${r.lane.industry}. You won this round.", LogTone.MILESTONE, r.lane)
            publish(NewsDesk.INDUSTRY, techOutlet(r.lane, rng), "${r.name} retreats from ${r.lane.industry}", "$companyName is the one to beat now.", LogTone.GOOD, r.lane, lead = true)
        }
        if (week < 30 || r.quietUntil > week || week - r.lastMoveWeek < MOVE_COOLDOWN) continue
        if (events.any { it.kind is RivalEvent } || !hasProduct(r.lane)) continue
        val chance = 0.004 + 0.04 * r.heat * (0.5 + r.aggression) * (0.5 + r.strength)
        if (!rng.chance(chance)) continue
        val move = pickMove(r)
        val targets = if (move == RivalMoveKind.TALENT_RAID) raidTargets(r.lane) else emptyList()
        r.lastMoveWeek = week
        r.heat = max(0.0, r.heat - 0.15)
        stats.rivalMoves += 1
        enqueue(RivalMove(r.name, r.lane, move, targets))
    }
}

private fun GameState.hasProduct(lane: Lane): Boolean = when (lane) {
    Lane.GAMES -> games?.releases?.isNotEmpty() == true
    Lane.WEBSITES -> (web?.completed ?: 0) > 0
    Lane.APPS -> apps?.isLive == true
    Lane.SOCIAL -> social?.isLive == true
    Lane.HARDWARE -> hardware?.onMarket != null
    Lane.AI -> (ai?.capability ?: 0.0) > 0
    Lane.OS -> os?.releases?.isNotEmpty() == true
}

private fun GameState.raidTargets(lane: Lane): List<Int> =
    staff.filter { it.lane == lane && it.skill >= 6 && it.trait != Trait.LOYAL }.sortedByDescending { it.skill }
        .take(if (Research.STOCK_OPTIONS in researched) 1 else 2).map { it.id }

/** What a rival does, weighted by what suits the market. */
private fun GameState.pickMove(r: Rival): RivalMoveKind {
    val w = mutableMapOf(
        RivalMoveKind.COPYCAT to 2.0,
        RivalMoveKind.PRICE_WAR to 2.0,
        RivalMoveKind.AD_BLITZ to 2.0,
        RivalMoveKind.TALENT_RAID to if (raidTargets(r.lane).isNotEmpty()) 1.5 else 0.0,
        RivalMoveKind.LAWSUIT to 1.0,
        RivalMoveKind.EXCLUSIVE_DEAL to 1.0,
        RivalMoveKind.SMEAR to 1.0,
    )
    when (r.lane) {
        Lane.GAMES -> { w[RivalMoveKind.EXCLUSIVE_DEAL] = 2.0; w[RivalMoveKind.PRICE_WAR] = 1.0 }
        Lane.WEBSITES -> { w[RivalMoveKind.PRICE_WAR] = 3.0; w[RivalMoveKind.COPYCAT] = 1.0 }
        Lane.APPS -> w[RivalMoveKind.COPYCAT] = 3.0
        Lane.SOCIAL -> { w[RivalMoveKind.COPYCAT] = 3.0; w[RivalMoveKind.SMEAR] = 2.0; w[RivalMoveKind.PRICE_WAR] = 0.5 }
        Lane.HARDWARE -> { w[RivalMoveKind.LAWSUIT] = 2.5; w[RivalMoveKind.PRICE_WAR] = 2.5 }
        Lane.AI -> { w[RivalMoveKind.LAWSUIT] = 2.0; w[RivalMoveKind.TALENT_RAID] = (w[RivalMoveKind.TALENT_RAID] ?: 0.0) * 2 }
        Lane.OS -> { w[RivalMoveKind.EXCLUSIVE_DEAL] = 2.5; w[RivalMoveKind.LAWSUIT] = 2.0; w[RivalMoveKind.COPYCAT] = 1.5 }
    }
    var roll = rng.unit() * w.values.sum()
    for ((k, v) in w) {
        roll -= v
        if (roll <= 0) return k
    }
    return RivalMoveKind.AD_BLITZ
}

private fun GameState.squeeze(r: RivalMove, title: String, demand: Double, weeks: Int, cost: Double = 1.0) {
    world.pressures.removeAll { it.rival == r.rival && it.lane == r.lane && !it.mine }
    world.pressures += Pressure(r.lane, title, demand, weeks, cost = cost, rival = r.rival)
}

private fun GameState.rivalNamed(name: String): Rival? = world.rivals.firstOrNull { it.name == name }

// Describing and resolving a rival's move.

internal fun GameState.describeRival(k: RivalEvent): Description? = when (k) {
    is RivalMove -> {
        val s = stake(k.lane)
        val product = productName(k.lane)
        val r = k.rival
        when (k.move) {
            RivalMoveKind.COPYCAT -> Description(
                "$r copied ${product.replaceFirstChar { it.lowercase() }}",
                "$r just launched something that looks an awful lot like $product, and they have the money to push it. " +
                    "Left alone it'll take a slice of your customers for half a year.",
                listOf(
                    choice("Rush out something better", "${money(s * 3)}. You'll keep most of your customers."),
                    choice("Outspend them on marketing", "${money(s * 1.5)}. You'll keep some."),
                    choice("Ignore it", "About 15% less demand for six months."),
                ),
                k.lane,
            )
            RivalMoveKind.PRICE_WAR -> Description(
                "$r started a price war",
                "$r slashed its prices across ${k.lane.industry}. Customers are noticing.",
                listOf(
                    choice("Match their prices", "Thinner margins for three months. They bleed too."),
                    choice("Hold your prices", "About 20% less demand for four months."),
                    choice("Go upmarket", "Lean into quality: some demand lost, a little reputation won."),
                ),
                k.lane,
            )
            RivalMoveKind.AD_BLITZ -> Description(
                "$r is running ads everywhere",
                "Billboards, feeds, podcasts: $r is buying attention in ${k.lane.industry}, and some of it was yours.",
                listOf(
                    choice("Counter-campaign", "${money(s * 2)}. Barely a dent."),
                    choice("Ride it out", "About 12% less demand for ten weeks."),
                ),
                k.lane,
            )
            RivalMoveKind.TALENT_RAID -> {
                val people = k.targets.mapNotNull { id -> staff.firstOrNull { it.id == id } }
                if (people.isEmpty()) {
                    null
                } else {
                    val names = people.joinToString(" and ") { it.name }
                    Description(
                        "$r is after your best people",
                        "$r's recruiters have made offers to $names: 30% more money and a big title.",
                        listOf(
                            choice("Match their offers", "30% raises all round."),
                            choice("Offer equity", "${Format.percent(0.004 * people.size)} of the company. They stay and they're thrilled."),
                            choice("Let them go", "$r gets stronger."),
                        ),
                        k.lane,
                    )
                }
            }
            RivalMoveKind.LAWSUIT -> Description(
                "$r is suing you",
                "A patent claim over ${product.replaceFirstChar { it.lowercase() }}. It's probably nonsense; courts are unpredictable.",
                listOf(
                    choice("Settle", "${money(s * 6)} and it goes away."),
                    choice("Fight it", "${money(s * 1.5)} in legal fees now. Win and they look foolish; lose and it costs far more."),
                ),
                k.lane,
            )
            RivalMoveKind.EXCLUSIVE_DEAL -> Description(
                "$r locked up an exclusive deal",
                when (k.lane) {
                    Lane.GAMES -> "$r signed an exclusive with a platform holder. Its players will be nudged toward their games for months."
                    Lane.WEBSITES -> "$r signed an exclusive with a big agency network. Fewer briefs will reach you."
                    Lane.APPS -> "$r has a featuring deal with the app stores. Their app is front and centre; yours isn't."
                    Lane.SOCIAL -> "$r signed the biggest creators to exclusive deals."
                    Lane.HARDWARE -> "$r locked up the big retailers' best shelf space."
                    Lane.AI -> "$r signed an exclusive cloud deal: the best GPUs and the biggest enterprise customers."
                    Lane.OS -> "$r locked three OEMs into exclusive licensing. Your partners are getting cold feet."
                },
                listOf(
                    choice("Outbid them", "${money(s * 4)}. You keep your place."),
                    choice("Find other partners", "About 10% less demand for six months."),
                ),
                k.lane,
            )
            RivalMoveKind.SMEAR -> Description(
                "$r is badmouthing you",
                "Anonymous briefings, a well-placed op-ed, a few influencers with talking points: $r wants people to think you're cutting corners.",
                listOf(
                    choice("Answer it publicly", "${money(s * 0.8)} on PR. Usually works."),
                    choice("Stay above it", "Free. Some of it sticks."),
                ),
                k.lane,
            )
        }
    }
}

internal fun GameState.resolveRival(k: RivalEvent, choice: Int) {
    when (k) {
        is RivalMove -> {
            val s = stake(k.lane)
            val r = rivalNamed(k.rival)
            val headline: String
            when (k.move) {
                RivalMoveKind.COPYCAT -> {
                    headline = "${k.rival} launches a rival to ${productName(k.lane)}"
                    when (choice) {
                        0 -> { spend(s * 3, k.lane); squeeze(k, "${k.rival}'s copycat", 0.95, 12); r?.let { it.strength = max(0.05, it.strength - 0.03) } }
                        1 -> { spend(s * 1.5, k.lane); squeeze(k, "${k.rival}'s copycat", 0.92, 16) }
                        else -> squeeze(k, "${k.rival}'s copycat", 0.85, 26)
                    }
                }
                RivalMoveKind.PRICE_WAR -> {
                    headline = "${k.rival} slashes prices"
                    when (choice) {
                        0 -> {
                            squeeze(k, "Price war with ${k.rival}", 0.9, 13)
                            r?.let { it.strength = max(0.05, it.strength - 0.06) }
                        }
                        1 -> squeeze(k, "${k.rival}'s price cuts", 0.8, 17)
                        else -> {
                            squeeze(k, "${k.rival}'s price cuts", 0.88, 17)
                            reputation.adjust(2.0)
                        }
                    }
                }
                RivalMoveKind.AD_BLITZ -> {
                    headline = "${k.rival} launches a huge ad campaign"
                    if (choice == 0) {
                        spend(s * 2, k.lane)
                        squeeze(k, "${k.rival}'s ad blitz", 0.97, 6)
                    } else {
                        squeeze(k, "${k.rival}'s ad blitz", 0.88, 10)
                        // Their ads drown yours out.
                        marketing.boost[k.lane]?.let { marketing.boost[k.lane] = it * 0.7 }
                    }
                }
                RivalMoveKind.TALENT_RAID -> {
                    headline = "${k.rival} goes on a hiring spree"
                    val people = k.targets.mapNotNull { id -> staff.firstOrNull { it.id == id } }
                    when (choice) {
                        0 -> for (e in people) {
                            e.salary *= 1.3
                            e.lastRaiseWeek = week
                        }
                        1 -> {
                            venture.founderEquity = max(0.0, venture.founderEquity - 0.004 * people.size)
                            for (e in people) e.morale = 1.0
                        }
                        else -> {
                            staff = staff - people.toSet()
                            r?.let { it.strength = min(1.0, it.strength + 0.04 * people.size) }
                            addLog("${people.joinToString(" and ") { it.name }} left for ${k.rival}.", LogTone.BAD, k.lane)
                        }
                    }
                }
                RivalMoveKind.LAWSUIT -> {
                    headline = "${k.rival} sues $companyName"
                    if (choice == 0) {
                        spend(s * 6, k.lane)
                        addLog("You settled with ${k.rival}.", LogTone.NEUTRAL, k.lane)
                    } else {
                        spend(s * 1.5, k.lane)
                        later(rng.int(13, 26), Consequence.Verdict(k.rival, k.lane, s))
                        addLog("You're fighting ${k.rival} in court. A verdict in a few months.", LogTone.NEUTRAL, k.lane)
                    }
                }
                RivalMoveKind.EXCLUSIVE_DEAL -> {
                    headline = "${k.rival} signs an exclusive deal"
                    if (choice == 0) {
                        spend(s * 4, k.lane)
                        r?.let { it.heat = min(1.0, it.heat + 0.1) }
                    } else {
                        squeeze(k, "${k.rival}'s exclusive", 0.9, 26)
                    }
                }
                RivalMoveKind.SMEAR -> {
                    headline = "Questions grow about $companyName"
                    if (choice == 0) {
                        spend(s * 0.8, k.lane)
                        if (rng.chance(0.75)) {
                            reputation.adjust(1.0)
                            addLog("Your answer to ${k.rival}'s smears landed. They look petty now.", LogTone.GOOD, k.lane)
                        } else {
                            reputation.adjust(-2.0)
                            addLog("Your response only kept the story going.", LogTone.BAD, k.lane)
                        }
                    } else {
                        reputation.adjust(-4.0)
                    }
                }
            }
            r?.lastMove = headline
            publish(NewsDesk.INDUSTRY, techOutlet(k.lane, rng), headline, "A shot at $companyName.", LogTone.BAD, k.lane)
        }
    }
}

/** A lawsuit you chose to fight is decided. */
internal fun GameState.settleLawsuit(c: Consequence.Verdict) {
    if (rng.chance(0.55)) {
        rivalNamed(c.rival)?.let { it.strength = max(0.05, it.strength - 0.1) }
        reputation.adjust(3.0)
        val costs = c.stake * 1.5
        cash += costs
        recordRevenue(costs, c.lane)
        addLog("You won the lawsuit against ${c.rival}, and they pay your costs.", LogTone.GOOD, c.lane)
        publish(NewsDesk.INDUSTRY, techOutlet(c.lane, rng), "Court throws out ${c.rival}'s case against $companyName", lane = c.lane)
    } else {
        spend(c.stake * 9, c.lane)
        world.pressures += Pressure(c.lane, "The ${c.rival} injunction", 0.92, 13, rival = c.rival)
        addLog("You lost the lawsuit against ${c.rival}: ${money(c.stake * 9)} in damages, and an injunction.", LogTone.BAD, c.lane)
        publish(NewsDesk.INDUSTRY, techOutlet(c.lane, rng), "${c.rival} wins its case against $companyName", lane = c.lane)
    }
}

// Your moves.

/** What a counter-move against [r] costs. */
fun GameState.counterCost(r: Rival, move: CounterMove): Double = when (move) {
    CounterMove.UNDERCUT -> 0.0
    CounterMove.POACH_STAR -> max(40_000.0, stake(r.lane) * 2)
    CounterMove.ATTACK_ADS -> max(50_000.0, stake(r.lane) * 3)
}

/** Why you can't make [move] against [r] right now, or null if you can. */
fun GameState.counterBlocker(r: Rival, move: CounterMove): String? {
    if (r.lane !in lanes) return "You don't compete with ${r.name}."
    if (r.quietUntil > week) return "${r.name} has pulled back from the market."
    if (r.counterReadyWeek > week) return "Wait ${Format.weeks(r.counterReadyWeek - week)} before another move on ${r.name}."
    if (move == CounterMove.POACH_STAR && freeDesks <= 0) return "No free desk for a new hire."
    if (move == CounterMove.UNDERCUT && world.pressures.any { it.mine && it.lane == r.lane && it.rival == r.name }) return "You're already undercutting them."
    val cost = counterCost(r, move)
    if (cash < cost) return "Costs ${money(cost)}."
    return null
}

/** Strike at a rival. */
fun GameState.counter(rivalName: String, move: CounterMove) {
    requireLive()
    val r = rivalNamed(rivalName) ?: throw invalid("No such rival.")
    counterBlocker(r, move)?.let { throw notAvailable(it) }
    spend(counterCost(r, move), r.lane)
    r.counterReadyWeek = week + COUNTER_COOLDOWN
    stats.counterMoves += 1
    when (move) {
        CounterMove.UNDERCUT -> {
            world.pressures += Pressure(r.lane, "Undercutting ${r.name}", 1.15, 13, cost = 1.12, mine = true, rival = r.name)
            r.strength = max(0.05, r.strength - 0.06)
            r.heat = min(1.0, r.heat + 0.2)
            addLog("You cut prices to take customers from ${r.name}.", LogTone.NEUTRAL, r.lane)
            publish(NewsDesk.INDUSTRY, techOutlet(r.lane, rng), "$companyName undercuts ${r.name}", lane = r.lane)
        }
        CounterMove.POACH_STAR -> {
            val c = makeCandidate(star = true)
            val e = hireDirect(c.copy(askingSalary = c.askingSalary * 1.25, contestedBy = null), r.lane)
            r.strength = max(0.05, r.strength - 0.03)
            r.heat = min(1.0, r.heat + 0.15)
            addLog("You hired ${e.name} away from ${r.name}.", LogTone.GOOD, r.lane)
            publish(NewsDesk.INDUSTRY, techOutlet(r.lane, rng), "${r.name} loses ${e.name} to $companyName", lane = r.lane)
        }
        CounterMove.ATTACK_ADS -> {
            world.pressures += Pressure(r.lane, "Your campaign against ${r.name}", 1.05, 8, mine = true, rival = r.name)
            r.strength = max(0.05, r.strength - 0.08)
            r.heat = min(1.0, r.heat + 0.25)
            reputation.adjust(-1.0)
            addLog("Your attack ads against ${r.name} are everywhere.", LogTone.NEUTRAL, r.lane)
            publish(NewsDesk.INDUSTRY, techOutlet(r.lane, rng), "$companyName takes aim at ${r.name} in new ads", lane = r.lane)
        }
    }
}

/** Rivals in your markets, the angriest first. */
val GameState.yourRivals: List<Rival> get() = world.rivals.filter { it.lane in lanes }.sortedByDescending { it.heat }

/** What's squeezing or lifting a lane right now. */
fun GameState.pressuresOn(lane: Lane): List<Pressure> = world.pressures.filter { it.lane == lane }
