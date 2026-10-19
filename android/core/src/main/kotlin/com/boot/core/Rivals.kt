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
        if (r.absorbed) continue
        if (r.roster && Lane.GAMES !in lanes) continue
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
        // Only the era headline Games rival makes RivalMoves (extras compete via releases / news).
        if (r.roster) {
            val h = world.gamesHeadline
            if (h != null && r.name != h && canonicalStudio(r.name) != canonicalStudio(h)) continue
        }
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
    staff.filter { it.lane == lane && it.skill >= 6 && it.effectiveLoyalty < 0.98 }
        .sortedWith(compareByDescending<Employee> { it.isStar }.thenByDescending { it.reputation }.thenByDescending { it.skill })
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
                    val arcs = people.joinToString("; ") {
                        "${it.name} (${it.chapter.title.lowercase()}, loyalty ${Format.percent(it.effectiveLoyalty)})"
                    }
                    Description(
                        "$r is after your best people",
                        "$r's recruiters have made offers to $names: 30% more money and a big title. $arcs.",
                        listOf(
                            choice("Match their offers", "30% raises all round. Loyalty recovers."),
                            choice("Offer equity", "${Format.percent(0.004 * people.size)} of the company. They stay and they're thrilled."),
                            choice("Let them go", "$r gets stronger. Career arcs walk out the door."),
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
                    Lane.GAMES -> {
                        val holders = platformHoldersIn(calendarYear).ifEmpty { listOf("a platform holder") }
                        "$r signed an exclusive with ${rng.pick(holders)}. Its players will be nudged toward their games for months."
                    }
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
                            e.loyalty = (e.loyalty + 0.12).clamped(0.0, 1.0)
                            e.offerPressure = 0.0
                            e.wantedBy = e.wantedBy.filterNot { it == k.rival }
                        }
                        1 -> {
                            venture.founderEquity = max(0.0, venture.founderEquity - 0.004 * people.size)
                            for (e in people) {
                                e.morale = 1.0
                                e.loyalty = (e.loyalty + 0.22).clamped(0.0, 1.0)
                                e.offerPressure = 0.0
                                e.wantedBy = emptyList()
                            }
                        }
                        else -> {
                            val fame = people.sumOf { it.reputation }
                            staff = staff - people.toSet()
                            r?.let { it.strength = min(1.0, it.strength + 0.04 * people.size + 0.03 * fame) }
                            addLog("${people.joinToString(" and ") { it.name }} left for ${k.rival}.", LogTone.BAD, k.lane)
                            for (gone in people) feedOnRivalBeat(k.rival, "poach_win", gone.name)
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
    CounterMove.POACH_STAR -> poachStarCost(r)
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
            val e = hireRivalStar(r)
            r.strength = max(0.05, r.strength - 0.03 - 0.02 * e.reputation)
            r.heat = min(1.0, r.heat + 0.15 + 0.05 * e.reputation)
            addLog("You hired ${e.name} away from ${r.name} (${e.chapter.title.lowercase()}).", LogTone.GOOD, r.lane)
            publish(
                NewsDesk.INDUSTRY, techOutlet(r.lane, rng),
                "${r.name} loses ${e.name} to $companyName",
                e.careerLine(),
                LogTone.GOOD, r.lane, tag = NewsTag.NEWS,
            )
            feedOnRivalBeat(r.name, "poach_win", e.name)
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
val GameState.yourRivals: List<Rival>
    get() = world.rivals.filter { it.lane in lanes && !it.absorbed }.sortedByDescending { it.heat }

/** Real-world games studios currently competing with you. */
val GameState.gameRivals: List<Rival>
    get() = world.rivals.filter { it.lane == Lane.GAMES && it.roster && !it.absorbed }
        .sortedWith(compareByDescending<Rival> { it.heat }.thenByDescending { it.strength })

/** What's squeezing or lifting a lane right now. */
fun GameState.pressuresOn(lane: Lane): List<Pressure> = world.pressures.filter { it.lane == lane }

/*
 * Real-world games rivals: era-gated studios with personalities. They appear only in the decades
 * they were really active, compete in favoured genres, fade when history closed them (unless they
 * survive this run or you buy them), and drive news / feed story beats when they clash with you.
 */

/** Static personality for a real games publisher. Names match [Studios] / [CompanyMarks] for logos. */
data class RivalProfile(
    val name: String,
    val founded: Int,
    val closed: Int? = null,
    val aggression: Double,
    val strength: Double,
    val reputation: Double,
    val genres: List<Genre>,
    val blurb: String,
)

/** The cast of real studios that fight you in Games. */
val RealWorldRivalProfiles: List<RivalProfile> = listOf(
    RivalProfile("Atari", 1972, 2013, 0.55, 0.55, 0.45, listOf(Genre.ACTION, Genre.CASUAL), "Home of the arcade boom — and the crash."),
    RivalProfile("Nintendo", 1889, null, 0.35, 0.7, 0.9, listOf(Genre.PLATFORMER, Genre.ACTION, Genre.CASUAL), "Cautious, beloved, and very hard to knock off the throne."),
    RivalProfile("Sega", 1960, null, 0.7, 0.55, 0.65, listOf(Genre.ACTION, Genre.PLATFORMER, Genre.SIMULATION), "Flashy, aggressive, forever chasing the next console war."),
    RivalProfile("Activision Blizzard", 1979, null, 0.75, 0.65, 0.7, listOf(Genre.SHOOTER, Genre.ACTION, Genre.CASUAL), "Blockbuster sequels and franchise machines."),
    RivalProfile("Electronic Arts", 1982, null, 0.5, 0.5, 0.55, listOf(Genre.SIMULATION, Genre.ACTION, Genre.CASUAL), "Sports, shooters, and a reputation for milking hits."),
    RivalProfile("Ubisoft", 1986, null, 0.55, 0.5, 0.55, listOf(Genre.ACTION, Genre.ADVENTURE, Genre.RPG), "Open-world factories and annualised brands."),
    RivalProfile("Capcom", 1979, null, 0.5, 0.55, 0.8, listOf(Genre.ACTION, Genre.FIGHTING, Genre.ADVENTURE), "Arcade craft and beloved series they remake forever."),
    RivalProfile("Square Enix", 1986, null, 0.4, 0.5, 0.75, listOf(Genre.RPG, Genre.ADVENTURE), "JRPG prestige — beautiful, ambitious, sometimes late."),
    RivalProfile("Konami", 1969, null, 0.45, 0.45, 0.6, listOf(Genre.ACTION, Genre.ADVENTURE, Genre.CASUAL), "From Metal Gear prestige to pachinko jokes."),
    RivalProfile("Bandai Namco", 1955, null, 0.4, 0.5, 0.65, listOf(Genre.FIGHTING, Genre.RPG, Genre.ACTION), "Anime licences and arcade DNA."),
    RivalProfile("Sierra On-Line", 1979, 2008, 0.45, 0.5, 0.7, listOf(Genre.ADVENTURE, Genre.RPG), "Adventure-game royalty of the PC golden age."),
    RivalProfile("LucasArts", 1982, 2013, 0.35, 0.55, 0.85, listOf(Genre.ADVENTURE, Genre.ACTION), "Witty adventures and Star Wars games."),
    RivalProfile("Acclaim", 1987, 2004, 0.8, 0.45, 0.35, listOf(Genre.ACTION, Genre.CASUAL, Genre.SIMULATION), "Volume shipper — hits and shovelware alike."),
    RivalProfile("Midway", 1988, 2009, 0.65, 0.5, 0.6, listOf(Genre.FIGHTING, Genre.ACTION, Genre.SIMULATION), "Arcade brawlers and Mortal Kombat blood."),
    RivalProfile("Valve", 1996, null, 0.25, 0.7, 0.9, listOf(Genre.SHOOTER, Genre.STRATEGY, Genre.ACTION), "Rare releases, huge influence, never for sale."),
    RivalProfile("Epic Games", 1991, null, 0.65, 0.55, 0.7, listOf(Genre.SHOOTER, Genre.ACTION, Genre.SANDBOX), "Engine kings turned battle-royale giants."),
    RivalProfile("Rockstar Games", 1998, null, 0.55, 0.75, 0.95, listOf(Genre.ACTION, Genre.ADVENTURE, Genre.SANDBOX), "Event releases that own the culture for a year."),
    RivalProfile("Bethesda Softworks", 1986, null, 0.5, 0.55, 0.75, listOf(Genre.RPG, Genre.ADVENTURE, Genre.SANDBOX), "Vast RPGs, mods, and the occasional rough launch."),
    RivalProfile("CD Projekt Red", 2002, null, 0.4, 0.5, 0.8, listOf(Genre.RPG, Genre.ADVENTURE), "Player-friendly RPGs — and one very public stumble."),
    RivalProfile("Take-Two Interactive", 1993, null, 0.55, 0.55, 0.7, listOf(Genre.ACTION, Genre.STRATEGY, Genre.SIMULATION), "Publisher of Rockstar and 2K brands."),
)

private val neverBuyOutright = setOf("Nintendo", "Valve")

/** Is this publisher still shipping in [year] for this run (history, survival, or your ownership)? */
fun GameState.publisherActive(name: String, year: Int = calendarYear): Boolean {
    val key = canonicalStudio(name)
    if (games?.owns(key) == true || games?.owns(name) == true) return true
    val rival = world.rivals.firstOrNull { it.roster && canonicalStudio(it.name) == key }
    if (rival?.absorbed == true) return false
    if (rival?.survived == true) return true
    return studioActive(key, year) || studioActive(name, year)
}

/** Ensure the era-correct games roster exists (new games and old saves). */
internal fun GameState.ensureGameRivalRoster() {
    val year = calendarYear
    val existing = world.rivals.filter { it.roster }.associateBy { canonicalStudio(it.name) }.toMutableMap()
    // Drop the legacy single Games placeholder if a roster is (or will be) present.
    world.rivals.removeAll { it.lane == Lane.GAMES && !it.roster }
    var nextId = (world.rivals.maxOfOrNull { it.id } ?: 10) + 1
    for (p in RealWorldRivalProfiles) {
        val key = canonicalStudio(p.name)
        val owned = games?.owns(p.name) == true
        var r = existing[key]
        if (r == null) {
            // Appear once founded; closed studios only if they survived or you're early enough.
            val historicallyOpen = year >= p.founded && (p.closed == null || year < p.closed)
            val surviveRoll = SeededRNG.fromSeed(seed * 1_009 + p.name.hashCode()).chance(0.12)
            if (!historicallyOpen && !(p.closed != null && year >= p.closed && surviveRoll && !owned)) continue
            r = Rival(
                id = nextId++,
                name = p.name,
                lane = Lane.GAMES,
                strength = (p.strength + difficulty.rivalStrength).clamped(0.05, 1.0),
                aggression = p.aggression,
                genres = p.genres,
                reputation = p.reputation,
                blurb = p.blurb,
                founded = p.founded,
                closedYear = p.closed,
                roster = true,
                survived = !historicallyOpen && surviveRoll,
            )
            world.rivals += r
            existing[key] = r
            if (r.survived && Lane.GAMES in lanes) {
                publish(
                    NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
                    "${p.name} refuses to die",
                    "Against the odds, ${p.name} is still making games in $year.",
                    LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS,
                )
            }
        } else {
            // Backfill personality on old roster rows that somehow lack it.
            if (r.blurb.isEmpty() && p.blurb.isNotEmpty()) {
                // blurb/genres/founded are val — only mutable fields can update on existing.
            }
            if (owned && !r.absorbed) absorbRivalState(r)
            else if (p.closed != null && year >= p.closed && !r.survived && !owned && !r.absorbed &&
                r.quietUntil < Int.MAX_VALUE / 4
            ) {
                // Only announce the fade once (when they first leave the market).
                fadeRival(r, historic = week > 0)
            } else if (year < p.founded && !r.absorbed) {
                r.quietUntil = max(r.quietUntil, week + 52)
            }
        }
    }
    // Era headline giant is the "main" Games rival for UI / news, matching [rivalIn].
    world.gamesHeadline = rivalIn(Lane.GAMES, year)
}

private fun GameState.fadeRival(r: Rival, historic: Boolean) {
    if (r.absorbed) return
    r.strength = max(0.05, r.strength * 0.4)
    r.heat = 0.0
    r.quietUntil = Int.MAX_VALUE / 2
    if (historic && Lane.GAMES in lanes && week > 0) {
        publish(
            NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
            "${r.name} fades from the industry",
            "The studio that once defined ${r.genres.firstOrNull()?.lower ?: "games"} is gone — unless someone buys what's left.",
            LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS,
        )
        addLog("${r.name} has left the stage.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** Mark a rival as absorbed into your empire. */
internal fun GameState.absorbRivalState(r: Rival) {
    r.absorbed = true
    r.strength = 0.05
    r.heat = 0.0
    r.quietUntil = Int.MAX_VALUE / 2
    stats.rivalsBeaten += 1
}

/** Quiet / absorb the rival matching [studio] after you buy them. */
internal fun GameState.absorbRivalNamed(studio: String) {
    val key = canonicalStudio(studio)
    world.rivals.filter { it.lane == Lane.GAMES && (canonicalStudio(it.name) == key || it.name == studio) }
        .forEach { absorbRivalState(it) }
}

/** Story beats when a rival ships something that touches you. */
internal fun GameState.rivalReleaseBeat(studio: String, title: String, genre: Genre, score: Double, size: GameSize) {
    val r = world.rivals.firstOrNull {
        it.roster && !it.absorbed && (canonicalStudio(it.name) == canonicalStudio(studio) || it.name == studio)
    } ?: return
    if (Lane.GAMES !in lanes) return
    val mine = games?.releases.orEmpty()
    val myGenreHit = mine.filter { week - it.week <= 78 && it.score >= 7.5 && it.concept.genre == genre }
        .maxByOrNull { it.score }
    val recentMine = mine.filter { week - it.week <= 52 }
    val sameGenreFight = recentMine.any { it.concept.genre == genre || it.concept.genre2 == genre }

    when {
        score < 5.0 && size >= GameSize.LARGE -> {
            publish(
                NewsDesk.GAMES, techOutlet(Lane.GAMES, rng),
                "${r.name}'s $title is a costly flop",
                "Critics panned the ${genre.lower} release. Rivals smell blood.",
                LogTone.GOOD, Lane.GAMES, tag = NewsTag.REVIEW,
            )
            feedOnRivalBeat(r.name, "flop", title)
            r.strength = max(0.05, r.strength - 0.04)
            r.reputation = max(0.15, r.reputation - 0.04)
        }
        myGenreHit != null && score >= 7.0 -> {
            publish(
                NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
                "${r.name} answers ${myGenreHit.title} with $title",
                "A ${genre.lower} counter-punch aimed squarely at $companyName's hit.",
                LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS, lead = score >= 8.5,
            )
            feedOnRivalBeat(r.name, "copy", title)
            r.heat = min(1.0, r.heat + 0.12)
        }
        sameGenreFight && score >= 7.5 -> {
            publish(
                NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
                "Rivalry heats up: ${r.name} vs $companyName",
                "Both are fighting for the same ${genre.lower} audience this year.",
                LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS,
            )
            feedOnRivalBeat(r.name, "heat", title)
            r.heat = min(1.0, r.heat + 0.08)
        }
    }
    if (r.heat > r.peakHeat + 0.25) {
        r.peakHeat = r.heat
        if (r.heat >= 0.55) {
            publish(
                NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
                "${r.name} has $companyName in its sights",
                r.blurb.ifBlank { "The rivalry is no longer polite." },
                LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS,
            )
            feedOnRivalBeat(r.name, "heat", null)
        }
    }
}

/** Prefer the player's genre and each studio's personality when rivals ship. */
internal fun GameState.pickRivalGenre(studio: Studio, rng: SeededRNG): Genre {
    val open = Genre.entries.filter { !isDecades || calendarYear >= it.year }.ifEmpty { Genre.entries.toList() }
    val profile = RealWorldRivalProfiles.firstOrNull { it.name == studio.name || canonicalStudio(it.name) == canonicalStudio(studio.name) }
    val playerGenres = games?.releases?.takeLast(5)?.map { it.concept.genre }.orEmpty().filter { it in open }
    val favoured = profile?.genres.orEmpty().filter { it in open }
    val pool = mutableListOf<Genre>()
    repeat(3) { favoured.forEach { pool += it } }
    repeat(2) { playerGenres.forEach { pool += it } }
    if (pool.isEmpty()) return rng.pick(open)
    return rng.pick(pool)
}

/** Late-game buyout of a struggling rival — absorbs studio + franchises via existing M&A. */
fun GameState.strugglingRivalPrice(r: Rival): Double {
    val s = studio(canonicalStudio(r.name)) ?: studio(r.name) ?: return 0.0
    val distress = (0.45 + r.strength * 0.4).coerceIn(0.4, 0.85)
    return studioValue(s) * distress
}

fun GameState.strugglingRivalBlocker(r: Rival): String? {
    if (!r.roster || r.lane != Lane.GAMES) return "Only games studios can be bought this way."
    if (r.absorbed) return "You already absorbed ${r.name}."
    val key = canonicalStudio(r.name)
    if (key in neverBuyOutright || r.name in neverBuyOutright) return "${r.name} isn't for sale."
    val g = games ?: return "You need a game studio first."
    if (g.owns(r.name) || g.owns(key)) return "It's already yours."
    g.rivalOwned[key]?.let { return "$it owns ${r.name} now." }
    if (week < 52 * 5) return "Buyouts of rivals open in year 6."
    val struggling = r.strength < 0.28 || r.quietUntil > week || r.reputation < 0.35
    if (!struggling) return "${r.name} isn't struggling enough to sell cheap."
    if (!r.survived && r.closedYear != null && calendarYear >= r.closedYear && r.quietUntil <= week) {
        // Faded: still buyable as remnants.
    }
    val price = strugglingRivalPrice(r)
    if (cash < price) return "Costs ${money(price)}. You have ${money(cash)}."
    if (listing == null && week < 52 * 3) return "The sellers won't talk to a company this young."
    return null
}

/** Buy a struggling rival outright and absorb them. */
fun GameState.buyStrugglingRival(name: String) {
    requireLive()
    val r = world.rivals.firstOrNull { it.name == name || canonicalStudio(it.name) == canonicalStudio(name) }
        ?: throw invalid("No such rival.")
    strugglingRivalBlocker(r)?.let { throw notAvailable(it) }
    val s = studio(canonicalStudio(r.name)) ?: studio(r.name) ?: throw invalid("No such studio.")
    val price = strugglingRivalPrice(r)
    spend(price, Lane.GAMES)
    val g = games!!
    g.rivalOwned.remove(s.name)
    g.rivalOwnedWeek.remove(s.name)
    val held = g.stakePaid.remove(s.name) ?: 0.0
    g.stakes.remove(s.name)
    g.subsidiaries += Subsidiary(s.name, week, price + held, finding = findingAt(s), morale = 0.45)
    learnFrom(g, s)
    absorbRivalState(r)
    stats.acquisitions += 1
    reputation.adjust(2.0)
    addLog("You bought struggling ${s.name} for ${money(price)} and absorbed its studio and franchises.", LogTone.MILESTONE, Lane.GAMES)
    publish(
        NewsDesk.YOU, "Bloomberg", "$companyName scoops up struggling ${s.name}",
        studioFranchises(s.name).take(2).joinToString().let { if (it.isEmpty()) "A distress sale." else "Home of $it, now under $companyName." },
        LogTone.GOOD, Lane.GAMES, lead = true, tag = NewsTag.MONEY,
    )
    feedOnDeal(s.name)
    enqueue(IntegrationChoice(s.name))
}

/** Rare: a rival offers to buy you out while you're deep in the red. Never forced. */
internal fun GameState.maybeDistressBuyout() {
    if (sandbox || ending != null) return
    if (Lane.GAMES !in lanes) return
    if (stats.weeksInRed < 2) return
    if (stats.weeksInRed >= difficulty.redWeeks) return
    if (week - lastDistressOfferWeek < 36) return
    if (needsDecision || events.any { it.kind is EventKind.Buyout }) return
    // ~2.5% each week while struggling — rare, never forced.
    if (!rng.chance(0.025)) return
    val bidder = gameRivals.filter { it.quietUntil <= week && it.strength >= 0.35 }
        .maxByOrNull { it.strength * (0.5 + it.aggression) }
        ?: world.rivals.filter { !it.absorbed && it.quietUntil <= week }.maxByOrNull { it.strength }
        ?: return
    val scores = games?.releases?.map { it.score }.orEmpty()
    val avg = if (scores.isEmpty()) 5.0 else scores.average()
    // Offer scales with how good past games were.
    val mult = (0.35 + (avg / 10.0) * 1.0).coerceIn(0.3, 1.35)
    val price = max(25_000.0, Math.round(valuation * mult / 1_000.0) * 1_000.0)
    lastDistressOfferWeek = week
    enqueue(EventKind.Buyout(bidder.name, price))
    addLog("${bidder.name} floated a buyout while you're in the red. You can take it or keep fighting.", LogTone.NEUTRAL)
}
