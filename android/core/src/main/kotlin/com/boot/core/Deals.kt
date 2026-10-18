package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Buying a company is a deal, not a button. You look a studio over first: its recent games, its
 * series, and how its owners feel about selling. Due diligence costs a fee and tells you what's
 * under the hood: a gem in development, people planning to walk, a team worn out by crunch, or
 * a studio whose best years are behind it. Whatever's there comes with the studio either way.
 *
 * Then you make an offer: the premium is up to you, and the more you offer the likelier a yes.
 * A listed company can pay half in shares. The owners answer in a couple of weeks: yes, a
 * counter-offer, or no (and they won't take your calls for six months). A big studio can draw a
 * rival bidder, and the biggest deals go before the regulators. Once it's yours, you decide how
 * to run it: leave it be, fold its best people into your own team, or cut costs.
 */

/** How a studio's owners feel about selling. */
enum class Stance(val title: String, val detail: String) {
    OPEN("Open to offers", "The owners would sell for a fair price."),
    WARY("Needs convincing", "The owners would want a generous offer."),
    CLOSED("Not for sale", "Only a very big offer has any chance."),
}

/** What due diligence turns up. Every studio has one, found or not. */
enum class Finding(val title: String, val detail: String, val good: Boolean = false) {
    CLEAN("Nothing worrying", "The books are clean and the team is steady."),
    HIDDEN_GEM("A gem in development", "They're working on something special that nobody's seen yet.", good = true),
    TALENT_FLIGHT("Key people may walk", "Several leads have said they'd leave after a sale."),
    BURNT_OUT("A worn-out team", "Years of crunch: morale is low and output is down."),
    PAST_ITS_BEST("Past its best", "Their best games are behind them, and the value will slide."),
}

/** How you run a studio you've bought. */
enum class Plan(val title: String, val detail: String) {
    INDEPENDENT("Leave them be", "They keep their name and their ways. All of their profit comes to you."),
    INTEGRATED("Fold them in", "Their three best people join your own team. Less profit from what's left of them."),
    SQUEEZED("Cut costs", "Much more profit for now, but their games get worse, and the press notices."),
}

/** An offer on the table. */
@Serializable
data class Deal(
    val studio: String,
    val premium: Double,
    val price: Double,
    /** Paid half in your own shares. */
    val inShares: Boolean,
    /** When the owners answer. */
    val answerWeek: Int,
    /** Part paid later, if the studio hits its targets. */
    val earnOut: Boolean = false,
)

@Serializable @SerialName("dealCounter")
data class DealCounter(val studio: String, val price: Double, val inShares: Boolean, val earnOut: Boolean = false) : EventKind

@Serializable @SerialName("biddingWar")
data class BiddingWar(val studio: String, val rival: String, val price: Double, val inShares: Boolean, val earnOut: Boolean = false) : EventKind

@Serializable @SerialName("regulatorReview")
data class RegulatorReview(val studio: String, val price: Double, val inShares: Boolean, val earnOut: Boolean = false) : EventKind

@Serializable @SerialName("integration")
data class IntegrationChoice(val studio: String) : EventKind

/** The smallest and largest premiums you can offer. */
const val MIN_PREMIUM = -0.1
const val MAX_PREMIUM = 0.8

/** Deals this big go before the regulators. */
const val REGULATOR_THRESHOLD = 20e9

/** The two studios nobody can buy outright, whatever's offered. */
private val neverForSale = setOf("Nintendo", "Valve")

/** How a studio's owners feel today: settled per studio and year, and swayed by how they're doing and how you're seen. */
fun GameState.stanceOf(s: Studio): Stance {
    if (s.name in neverForSale) return Stance.CLOSED
    val roll = SeededRNG.fromSeed(seed * 7_919 + s.name.hashCode() + calendarYear).unit()
    val g = games
    val recent = g?.market?.games?.filter { canonicalStudio(it.studio) == s.name && week - it.week <= 104 }.orEmpty()
    val struggling = recent.count { it.score < 6 } >= 2 || (recent.isNotEmpty() && recent.map { it.score }.average() < 6.5)
    var open = (if (s.indie) 0.45 else 0.25) + (if (struggling) 0.25 else 0.0) + (reputation.score - 50) / 250
    val closed = (if (s.indie) 0.12 else 0.25) - (if (struggling) 0.1 else 0.0)
    open = open.clamped(0.05, 0.85)
    return when {
        roll < open -> Stance.OPEN
        roll > 1 - closed -> Stance.CLOSED
        else -> Stance.WARY
    }
}

/** What's really going on at a studio, found or not. */
fun GameState.findingAt(s: Studio): Finding {
    val r = SeededRNG.fromSeed(seed * 31 + s.name.hashCode())
    return when (r.int(0, 9)) {
        0, 1 -> Finding.HIDDEN_GEM
        2, 3 -> Finding.TALENT_FLIGHT
        4 -> Finding.BURNT_OUT
        5 -> Finding.PAST_ITS_BEST
        else -> Finding.CLEAN
    }
}

/** What due diligence costs: half a percent of the studio's value, never under $50k in today's money. */
fun GameState.diligenceCost(s: Studio): Double = max(scaled(50_000.0), studioValue(s) * 0.005)

/** Pays for a look at the books. */
fun GameState.dueDiligence(name: String) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio first.")
    val s = studio(name) ?: throw invalid("No such studio.")
    if (name in g.diligence) throw invalid("You've already looked at ${s.name}'s books.")
    val cost = diligenceCost(s)
    if (cash < cost) throw notAvailable("Due diligence costs ${money(cost)}.")
    spend(cost, Lane.GAMES)
    g.diligence += name
    val f = findingAt(s)
    addLog("Due diligence on ${s.name}: ${f.title.lowercase()}. ${f.detail}", if (f.good) LogTone.GOOD else LogTone.NEUTRAL, Lane.GAMES)
}

/** What a given premium would cost. */
fun GameState.offerPrice(s: Studio, premium: Double): Double = studioValue(s) * (1 + premium)

/** The chance the owners say yes to a premium, as far as anyone can tell. */
fun GameState.acceptOdds(s: Studio, premium: Double, earnOut: Boolean = false): Double {
    if (s.name in neverForSale) return 0.0
    if (games?.rivalOwned?.containsKey(s.name) == true) return 0.0
    val (base, fair) = when (stanceOf(s)) {
        Stance.OPEN -> 0.75 to 0.1
        Stance.WARY -> 0.45 to 0.25
        Stance.CLOSED -> 0.15 to 0.5
    }
    // A stake makes them friendlier; an earn-out, warier: they'd rather have the money now.
    return (base + (premium - fair) * 2.0 + (reputation.score - 50) / 200 + stakeGoodwill(s) - (if (earnOut) 0.08 else 0.0)).clamped(0.02, 0.95)
}

/** Why you can't make this offer, or null if you can. */
fun GameState.offerBlocker(s: Studio, premium: Double, inShares: Boolean, earnOut: Boolean = false): String? {
    val g = games ?: return "You need a game studio to buy one."
    if (g.owns(s.name)) return "It's already yours."
    g.rivalOwned[s.name]?.let { return "$it owns ${s.name} now." }
    if (inShares && earnOut) return "Pick shares or an earn-out, not both."
    if (s.name in neverForSale) return "${s.name} isn't for sale at any price."
    if (!s.indie && listing == null && week < 52 * 3) return "The big publishers won't talk to a company this young."
    g.deal?.let { return "You're waiting to hear back from ${it.studio}." }
    g.cooloff[s.name]?.let { if (it > week) return "${s.name} won't take your calls until ${GameClock.label(it)}." }
    if (inShares && listing == null) return "Only a listed company can pay in shares."
    val cash = offerCash(s, premium, inShares, earnOut)
    if (this.cash < cash) return "That needs ${money(cash)} in cash. You have ${money(this.cash)}."
    return null
}

/** The cash an offer needs now: less what your stake already covers, and less again for shares or an earn-out. */
fun GameState.offerCash(s: Studio, premium: Double, inShares: Boolean, earnOut: Boolean = false): Double =
    offerPrice(s, premium) * (1 - stakeIn(s.name)) * when {
        inShares -> 0.5
        earnOut -> EARN_OUT_NOW
        else -> 1.0
    }

/** Puts an offer on the table. The owners answer in two weeks. */
fun GameState.makeOffer(name: String, premium: Double, inShares: Boolean = false, earnOut: Boolean = false) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio first.")
    val s = studio(name) ?: throw invalid("No such studio.")
    val p = premium.clamped(MIN_PREMIUM, MAX_PREMIUM)
    offerBlocker(s, p, inShares, earnOut)?.let { throw notAvailable(it) }
    g.deal = Deal(s.name, p, offerPrice(s, p), inShares, week + 2, earnOut)
    stats.offers += 1
    addLog("You offered ${money(offerPrice(s, p))} for ${s.name}. They'll answer in two weeks.", LogTone.NEUTRAL, Lane.GAMES)
}

/** Withdraws the offer on the table. */
fun GameState.withdrawOffer() {
    val g = games ?: return
    val d = g.deal ?: return
    g.deal = null
    addLog("You withdrew your offer for ${d.studio}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** The owners answer, when it's time. */
internal fun GameState.dealWeek(g: GamesStudio) {
    val d = g.deal ?: return
    if (week < d.answerWeek) return
    g.deal = null
    val s = studio(d.studio) ?: return
    val odds = acceptOdds(s, d.premium, d.earnOut)
    val roll = rng.unit()
    when {
        roll < odds -> {
            // A big studio for sale draws other buyers.
            if (!s.indie && rng.chance(0.3)) {
                val rival = rng.pick(listOf("Tencent", "Sony", "Microsoft", "Embracer Group", "Savvy Games Group").filter { it != s.name })
                enqueue(BiddingWar(s.name, rival, d.price * rng.range(1.08, 1.2), d.inShares, d.earnOut))
            } else {
                close(s, d.price, d.inShares, d.earnOut)
            }
        }
        roll < odds + 0.25 -> enqueue(DealCounter(s.name, d.price * rng.range(1.1, 1.25), d.inShares, d.earnOut))
        else -> {
            g.cooloff[s.name] = week + 26
            addLog("${s.name} turned down your offer. They won't hear another for six months.", LogTone.BAD, Lane.GAMES)
            publish(NewsDesk.GAMES, "Bloomberg", "${s.name} rejects $companyName bid", "The ${money(d.price)} offer wasn't enough.", LogTone.BAD, Lane.GAMES, tag = NewsTag.MONEY)
        }
    }
}

/** Agreed: through the regulators if it's big enough, then it's yours. */
private fun GameState.close(s: Studio, price: Double, inShares: Boolean, earnOut: Boolean = false) {
    if (price >= REGULATOR_THRESHOLD && !s.indie && rng.chance(0.4)) {
        enqueue(RegulatorReview(s.name, price, inShares, earnOut))
        return
    }
    complete(s, price, inShares, earnOut)
}

/** Pays, takes over, and asks how you'll run it. */
private fun GameState.complete(s: Studio, price: Double, inShares: Boolean, earnOut: Boolean = false) {
    val g = games ?: return
    // What your stake already covers comes off the price.
    val stake = stakeIn(s.name)
    val rest = price * (1 - stake)
    val cashPart = rest * when {
        inShares -> 0.5
        earnOut -> EARN_OUT_NOW
        else -> 1.0
    }
    if (cash < cashPart) {
        addLog("The ${s.name} deal fell through: you couldn't find ${money(cashPart)}.", LogTone.BAD, Lane.GAMES)
        return
    }
    spend(cashPart, Lane.GAMES)
    if (inShares) {
        // New shares for the sellers: you own a little less of a bigger company.
        val after = valuation + rest * 0.5
        venture.founderEquity = max(0.01, venture.founderEquity * (1 - rest * 0.5 / max(after, 1.0)))
    }
    val heldFor = g.stakePaid.remove(s.name) ?: 0.0
    g.stakes.remove(s.name)
    g.subsidiaries += Subsidiary(
        s.name, week, (if (inShares) rest else cashPart) + heldFor, finding = findingAt(s),
        earnOutDue = if (earnOut) rest * EARN_OUT_LATER else 0.0, earnOutWeek = if (earnOut) week + EARN_OUT_WEEKS else 0,
    )
    val learned = learnFrom(g, s)
    if (learned > 0) addLog("${s.name}'s people showed you what they know: $learned topic and genre pairings you hadn't tried.", LogTone.GOOD, Lane.GAMES)
    stats.acquisitions += 1
    reputation.adjust(if (s.indie) 1.0 else 3.0)
    addLog(
        "You bought ${s.name} for ${money(price)}" + when {
            inShares -> ", half of it in shares"
            earnOut -> ": ${money(cashPart)} now, ${money(rest * EARN_OUT_LATER)} more in two years if it hits its targets"
            else -> ""
        } + (if (stake > 0) ", with your ${Format.percent(stake)} stake counted" else "") + ".",
        LogTone.MILESTONE, Lane.GAMES,
    )
    publish(
        NewsDesk.YOU, "Bloomberg", "$companyName buys ${s.name} for ${money(price)}",
        studioFranchises(s.name).take(2).joinToString().let { if (it.isEmpty()) "The deal closed this week." else "The maker of $it changes hands." },
        LogTone.GOOD, Lane.GAMES, lead = !s.indie, tag = NewsTag.MONEY,
        quote = "We've admired ${s.name} for years. Nothing changes for their players.", quoteBy = founder.name,
    )
    feedOnDeal(s.name)
    enqueue(IntegrationChoice(s.name))
}

private fun terms(inShares: Boolean, earnOut: Boolean) = when {
    inShares -> ", half in shares."
    earnOut -> ", part of it later if they hit their targets."
    else -> "."
}

internal fun GameState.describeDeal(k: EventKind): Description? = when (k) {
    is DealCounter -> Description(
        "${k.studio} wants more",
        "The owners like you, but not the price. They'd sell for ${money(k.price)}.",
        listOf(choice("Accept their price", money(k.price) + terms(k.inShares, k.earnOut)), choice("Walk away", "No deal, and no hard feelings.")),
        Lane.GAMES,
    )
    is BiddingWar -> Description(
        "${k.rival} is bidding for ${k.studio}",
        "${k.studio} said yes, then ${k.rival} came in higher. To win it you'd have to pay ${money(k.price)}.",
        listOf(choice("Outbid them", money(k.price) + terms(k.inShares, k.earnOut)), choice("Let them have it", "${k.rival} gets the studio.")),
        Lane.GAMES,
    )
    is RegulatorReview -> Description(
        "Regulators look at the ${k.studio} deal",
        "A deal this size worries the competition authorities. They'll approve it if you accept conditions: a fee, and a promise to keep its games on every platform.",
        listOf(choice("Accept the conditions", "${money(k.price * 0.03)} on top, and the deal closes."), choice("Walk away", "The deal is off.")),
        Lane.GAMES,
    )
    is IntegrationChoice -> Description(
        "How will you run ${k.studio}?",
        "The deal's done. Now decide what ${k.studio} becomes. You can change your mind once a year.",
        Plan.entries.map { choice(it.title, it.detail) },
        Lane.GAMES,
    )
    else -> null
}

internal fun GameState.resolveDeal(k: EventKind, choice: Int) {
    val g = games ?: return
    when (k) {
        is DealCounter -> if (choice == 0) studio(k.studio)?.let { close(it, k.price, k.inShares, k.earnOut) } else addLog("You walked away from ${k.studio}.", LogTone.NEUTRAL, Lane.GAMES)
        is BiddingWar -> if (choice == 0) {
            studio(k.studio)?.let { close(it, k.price, k.inShares, k.earnOut) }
        } else {
            addLog("${k.rival} bought ${k.studio}.", LogTone.NEUTRAL, Lane.GAMES)
            publish(NewsDesk.GAMES, "Bloomberg", "${k.rival} buys ${k.studio}", "It outbid $companyName.", lane = Lane.GAMES, tag = NewsTag.MONEY)
            g.rivalOwned[k.studio] = k.rival
            g.rivalOwnedWeek[k.studio] = week
            // Your stake goes with it, at the price they paid.
            g.stakes.remove(k.studio)?.let { share ->
                g.stakePaid.remove(k.studio)
                val payout = k.price * share
                cash += payout
                recordRevenue(payout, Lane.GAMES)
                addLog("${k.rival} bought your ${Format.percent(share)} of ${k.studio} too: ${money(payout)}.", LogTone.NEUTRAL, Lane.GAMES)
            }
        }
        is RegulatorReview -> if (choice == 0) {
            spend(k.price * 0.03, Lane.GAMES)
            studio(k.studio)?.let { complete(it, k.price, k.inShares, k.earnOut) }
        } else {
            addLog("You walked away from ${k.studio} rather than accept the regulators' terms.", LogTone.NEUTRAL, Lane.GAMES)
        }
        is IntegrationChoice -> {
            val sub = g.subsidiaries.firstOrNull { it.name == k.studio } ?: return
            setPlan(sub, Plan.entries[choice.coerceIn(0, Plan.entries.lastIndex)])
        }
        else -> Unit
    }
}

/** Changes how you run a studio you own: once a year. */
fun GameState.runStudio(name: String, plan: Plan) {
    requireLive()
    val sub = games?.subsidiaries?.firstOrNull { it.name == name } ?: throw invalid("You don't own $name.")
    if (sub.plan == plan) return
    if (week - sub.planWeek < 52) throw notAvailable("You can change how ${sub.name} is run in ${GameClock.label(sub.planWeek + 52)}.")
    if (plan == Plan.INTEGRATED && sub.folded) throw notAvailable("Their best people already work for you.")
    setPlan(sub, plan)
}

private fun GameState.setPlan(sub: Subsidiary, plan: Plan) {
    sub.plan = plan
    sub.planWeek = week
    when (plan) {
        Plan.INTEGRATED -> if (!sub.folded) {
            sub.folded = true
            // Their three best people, one of each kind where we can.
            val q = studio(sub.name)?.quality ?: 0.6
            val roles = listOf(Role.DESIGNER, Role.ENGINEER, if (sub.finding == Finding.TALENT_FLIGHT) Role.MARKETER else Role.RESEARCHER)
            // The designer is a specialist in what their games need most; the engineer builds engines.
            val genre = studio(sub.name)?.let { mainGenre(it) }
            val key = genre?.let { gn -> FocusArea.entries.firstOrNull { gn.weight(it) == 2 } } ?: FocusArea.GAMEPLAY
            val joined = roles.map { role ->
                val c = makeCandidate(role)
                val skill = (6 + q * 4).toInt().coerceIn(6, 10)
                val specialty = when (role) {
                    Role.DESIGNER -> Specialty.of(key).takeIf { it != Specialty.ENGINE_PROGRAMMER } ?: Specialty.GAME_DESIGNER
                    Role.ENGINEER -> Specialty.ENGINE_PROGRAMMER
                    else -> null
                }
                hireDirect(c.copy(skill = skill, design = max(c.design, skill - 1), tech = max(c.tech, skill - 1), askingSalary = c.askingSalary * 1.2, specialty = specialty ?: c.specialty), Lane.GAMES)
            }
            addLog("${joined.joinToString { it.name }} joined your team from ${sub.name}.", LogTone.GOOD, Lane.GAMES)
        }
        Plan.SQUEEZED -> {
            reputation.adjust(-2.0)
            publish(NewsDesk.GAMES, "Kotaku", "Layoffs at ${sub.name} after $companyName takeover", "Staff say the studio is being run for cash.", LogTone.BAD, Lane.GAMES)
        }
        Plan.INDEPENDENT -> Unit
    }
    addLog("${sub.name}: ${plan.title.lowercase()}.", LogTone.NEUTRAL, Lane.GAMES)
}

/** How a plan and what was found change a studio's weekly profit. */
internal fun Subsidiary.profitFactor(week: Int): Double {
    val plan = when (plan) {
        Plan.INDEPENDENT -> 1.0
        Plan.INTEGRATED -> 0.6
        Plan.SQUEEZED -> 1.4
    }
    val firstYear = week - boughtWeek < 52
    val found = when (finding) {
        Finding.BURNT_OUT -> if (firstYear) 0.8 else 1.0
        // People walk unless they're left alone.
        Finding.TALENT_FLIGHT -> if (this.plan == Plan.INDEPENDENT) 1.0 else 0.8
        Finding.HIDDEN_GEM -> if (firstYear) 1.3 else 1.1
        else -> 1.0
    }
    return plan * found
}

/** How what was found changes what a studio is worth. */
internal fun Subsidiary.valueFactor(week: Int): Double = max(0.6, 1 - 0.08 * talentLost) * when (finding) {
    Finding.PAST_ITS_BEST -> max(0.7, 1 - 0.05 * ((week - boughtWeek) / 26))
    Finding.HIDDEN_GEM -> if (week - boughtWeek >= 26) 1.15 else 1.0
    Finding.TALENT_FLIGHT -> if (plan != Plan.INDEPENDENT) 0.85 else 1.0
    else -> 1.0
} * (if (plan == Plan.SQUEEZED) max(0.75, 1 - 0.04 * ((week - planWeek) / 13)) else 1.0)

/** Owned studios run for cash make worse games. */
internal fun GameState.ownedQualityPenalty(studio: String): Double {
    val sub = games?.subsidiaries?.firstOrNull { it.name == canonicalStudio(studio) } ?: return 0.0
    return if (sub.plan == Plan.SQUEEZED) 0.8 else 0.0
}

/** A studio's recent games, newest first. */
fun GameState.recentGamesBy(s: Studio, limit: Int = 4): List<RivalGame> =
    games?.market?.games?.filter { canonicalStudio(it.studio) == s.name }?.sortedByDescending { it.week }?.take(limit).orEmpty()
