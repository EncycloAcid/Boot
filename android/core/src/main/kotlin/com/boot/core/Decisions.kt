package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * More to decide, week to week. Two kinds of decision now:
 * - Ones that stop the clock (crises, people, rivals' moves), as before.
 * - The inbox: opportunities that wait a few weeks for an answer and don't stop the game. Let one
 *   lapse and it's as if you'd said no.
 * Some choices come back later: a breach patched quietly can still leak, a burnt-out engineer told
 * to push through can still quit, a lawsuit you chose to fight is decided months on.
 */

// People and crises: these stop the clock.

@Serializable
sealed interface DramaEvent : EventKind

@Serializable @SerialName("burnout")
data class Burnout(val employeeId: Int) : DramaEvent

@Serializable @SerialName("staffClash")
data class StaffClash(val a: Int, val b: Int) : DramaEvent

@Serializable @SerialName("equityAsk")
data class EquityAsk(val employeeId: Int) : DramaEvent

@Serializable @SerialName("remoteWork")
data object RemoteWork : DramaEvent

@Serializable @SerialName("sideProject")
data class SideProject(val employeeId: Int) : DramaEvent

@Serializable @SerialName("dataBreach")
data class DataBreach(val lane: Lane) : DramaEvent

@Serializable @SerialName("viralComplaint")
data class ViralComplaint(val lane: Lane) : DramaEvent

@Serializable @SerialName("clientBankrupt")
data class ClientBankrupt(val contractId: Int, val client: String, val owed: Double) : DramaEvent

// Opportunities: these wait in the inbox.

@Serializable
sealed interface Opportunity : EventKind

@Serializable @SerialName("interns")
data object Interns : Opportunity

@Serializable @SerialName("conferenceTalk")
data class ConferenceTalk(val conference: String) : Opportunity

@Serializable @SerialName("partnership")
data class Partnership(val lane: Lane, val partner: String, val pay: Double, val weeks: Int) : Opportunity

@Serializable @SerialName("acquihire")
data class Acquihire(val startup: String, val price: Double, val lane: Lane) : Opportunity

@Serializable @SerialName("angelCheck")
data class AngelCheck(val investor: String, val amount: Double, val equity: Double) : Opportunity

@Serializable @SerialName("interview")
data class Interview(val outlet: String) : Opportunity

// Consequences that land later.

@Serializable
sealed interface Consequence {
    @Serializable @SerialName("burnoutQuit")
    data class BurnoutQuit(val employeeId: Int) : Consequence

    @Serializable @SerialName("leftForRival")
    data class LeftForRival(val employeeId: Int, val rival: String) : Consequence

    @Serializable @SerialName("breachExposed")
    data class BreachExposed(val lane: Lane, val fine: Double) : Consequence

    @Serializable @SerialName("verdict")
    data class Verdict(val rival: String, val lane: Lane, val stake: Double) : Consequence

    @Serializable @SerialName("clientPays")
    data class ClientPays(val client: String, val amount: Double) : Consequence

    @Serializable @SerialName("partnerPays")
    data class PartnerPays(val partner: String, val lane: Lane, val amount: Double) : Consequence

    @Serializable @SerialName("pitchResult")
    data class PitchResult(val brand: String, val value: Double, val chance: Double) : Consequence
}

/** Something that happens [week], with [chance] that it happens at all. */
@Serializable
data class FollowUp(val week: Int, val consequence: Consequence, val chance: Double = 1.0)

/** A sense of scale for costs: a lane's weekly revenue lately, or the company's, never under $20k. */
internal fun GameState.stake(lane: Lane?): Double = max(20_000.0, trailingRevenue(lane, 4) / 4)

private fun Double.roundedMoney(): Double = if (this >= 100_000) Math.round(this / 10_000.0) * 10_000.0 else Math.round(this / 1_000.0) * 1_000.0

/** Put an opportunity in the inbox, open for [weeks]. */
internal fun GameState.offer(kind: EventKind, weeks: Int) {
    if (inbox.size >= 5 || inbox.any { it.kind == kind }) return
    val d = describe(kind) ?: return
    inbox = inbox + PendingEvent(nextId(), week, kind, d.title, d.body, d.choices, d.lane, expiresWeek = week + weeks)
}

/** Schedule a consequence [weeks] from now. */
internal fun GameState.later(weeks: Int, consequence: Consequence, chance: Double = 1.0) {
    followUps = followUps + FollowUp(week + weeks, consequence, chance)
}

/** Answer something in the inbox. */
fun GameState.answer(id: Int, choice: Int) {
    requireLive()
    val item = inbox.firstOrNull { it.id == id } ?: throw notAvailable("That's no longer in your inbox.")
    if (choice !in item.choices.indices) throw invalid("Pick one of the options.")
    inbox = inbox - item
    stats.inboxAnswered += 1
    resolve(item.kind, choice)
}

/** Lapsed inbox items, and consequences that are due. */
internal fun GameState.inboxWeek() {
    val lapsed = inbox.filter { it.expiresWeek < week }
    if (lapsed.isNotEmpty()) {
        inbox = inbox - lapsed.toSet()
        for (item in lapsed) {
            addLog("You let it lapse: ${item.title.replaceFirstChar { it.lowercase() }}.")
            resolve(item.kind, item.choices.lastIndex)
        }
    }
    val due = followUps.filter { it.week <= week }
    if (due.isNotEmpty()) {
        followUps = followUps - due.toSet()
        for (f in due) if (rng.chance(f.chance)) land(f.consequence)
    }
}

/** The weekly chance of new decisions, people trouble and opportunities. */
internal fun GameState.rollDecisions() {
    if (week < 8) return
    val people = staff.filter { it.leaveUntil <= week }
    // People.
    if (crunch || people.any { it.morale < 0.45 }) {
        val tired = people.filter { it.morale < 0.5 && it.skill >= 4 }
        if (tired.isNotEmpty() && rng.chance(if (crunch) 0.03 else 0.008)) enqueue(Burnout(rng.pick(tired).id))
    }
    if (people.size >= 5 && rng.chance(0.004)) {
        val pair = people.shuffledWith(rng).take(2)
        if (pair.size == 2 && pair[0].lane == pair[1].lane) enqueue(StaffClash(pair[0].id, pair[1].id))
    }
    val stars = people.filter { it.skill >= 8 && week - it.hiredWeek >= 52 && it.trait != Trait.LOYAL }
    if (stars.isNotEmpty() && valuation > 5_000_000 && venture.founderEquity > 0.2 && rng.chance(0.004)) enqueue(EquityAsk(rng.pick(stars).id))
    if (!hybrid && people.size >= 8 && rng.chance(0.002)) enqueue(RemoteWork)
    val tinkerers = people.filter { it.skill >= 6 && (it.role == Role.ENGINEER || it.role == Role.DESIGNER) }
    if (tinkerers.isNotEmpty() && rng.chance(0.003)) enqueue(SideProject(rng.pick(tinkerers).id))

    // Crises.
    val userLanes = listOf(Lane.APPS, Lane.SOCIAL, Lane.AI).filter { hasCustomers(it) }
    if (userLanes.isNotEmpty() && rng.chance(0.003)) enqueue(DataBreach(rng.pick(userLanes)))
    val withCustomers = lanes.filter { hasCustomers(it) }
    if (withCustomers.isNotEmpty() && rng.chance(0.004)) enqueue(ViralComplaint(rng.pick(withCustomers)))
    web?.let { w ->
        val big = w.active.filter { it.value >= 15_000 && it.completion > 0.3 }
        if (big.isNotEmpty() && rng.chance(0.004)) {
            val c = rng.pick(big)
            enqueue(ClientBankrupt(c.id, c.client, (c.value * 0.6).roundedMoney()))
        }
    }

    // Opportunities, for the inbox.
    if (freeDesks >= 2 && rng.chance(0.006)) offer(Interns, 6)
    if (week > 26 && rng.chance(0.005)) {
        offer(ConferenceTalk(rng.pick(listOf("Web Summit", "SXSW", "GDC", "TechCrunch Disrupt", "Collision", "Slush", "Google I/O", "WWDC"))), 4)
    }
    val earning = lanes.filter { trailingRevenue(it, 4) > 0 }
    if (earning.isNotEmpty() && rng.chance(0.006)) {
        val lane = rng.pick(earning)
        val weeks = rng.int(6, 12)
        val pay = (stake(lane) * weeks * rng.range(0.35, 0.6)).roundedMoney()
        offer(Partnership(lane, rng.pick(Names.brands), pay, weeks), 5)
    }
    if (week > 52 && rng.chance(0.003)) {
        val lane = lanes.maxByOrNull { trailingRevenue(it, 13) } ?: lanes.first()
        val price = (max(150_000.0, stake(lane) * 6) * rng.range(0.8, 1.2)).roundedMoney()
        offer(Acquihire(rng.pick(Names.startups), price, lane), 6)
    }
    if (venture.board == null && venture.founderEquity >= 0.9 && cash < 400_000 && week > 12 && rng.chance(0.004)) {
        offer(AngelCheck(rng.pick(Names.angels), rng.int(10, 30) * 10_000.0, rng.int(5, 9) / 100.0), 4)
    }
    if (reputation.score >= 40 && stats.releases > 0 && rng.chance(0.005)) {
        offer(Interview(rng.pick(listOf("Bloomberg TV", "The Verge's podcast", "CNBC", "Wired", "a big YouTube channel", "the BBC"))), 3)
    }
}

/** Does this lane have customers who could complain, or whose data could leak? */
private fun GameState.hasCustomers(lane: Lane): Boolean = when (lane) {
    Lane.GAMES -> games?.releases?.isNotEmpty() == true
    Lane.WEBSITES -> (web?.completed ?: 0) > 0
    Lane.APPS -> apps?.isLive == true && (apps?.users ?: 0.0) > 5_000
    Lane.SOCIAL -> social?.isLive == true && (social?.users ?: 0.0) > 10_000
    Lane.HARDWARE -> hardware?.onMarket != null
    Lane.AI -> (ai?.capability ?: 0.0) > 0
}

private fun <T> List<T>.shuffledWith(rng: SeededRNG): List<T> {
    val out = toMutableList()
    for (i in out.indices.reversed()) {
        val j = rng.int(0, i)
        val t = out[i]
        out[i] = out[j]
        out[j] = t
    }
    return out
}

private fun GameState.person(id: Int): Employee? = staff.firstOrNull { it.id == id }

private fun first(name: String) = name.substringBefore(' ')

// Describing

internal fun GameState.describeDrama(kind: DramaEvent): Description? = when (kind) {
    is Burnout -> person(kind.employeeId)?.let { e ->
        Description(
            "${e.name} is burning out",
            "${first(e.name)} has been running on empty: short with people, long hours, small mistakes. " +
                "A proper break now, or push through and hope.",
            listOf(
                choice("Give them four weeks off", "Paid, and they do no work, but they come back fresh."),
                choice("Ask them to push through", "They keep working. Some people break."),
            ),
            e.lane,
        )
    }
    is StaffClash -> {
        val a = person(kind.a)
        val b = person(kind.b)
        if (a == null || b == null) {
            null
        } else {
            Description(
                "${first(a.name)} and ${first(b.name)} can't work together",
                "It started over a code review and now the whole team takes sides. Someone has to decide.",
                listOf(
                    choice("Back ${first(a.name)}", "${first(b.name)} is unhappy and may leave."),
                    choice("Back ${first(b.name)}", "${first(a.name)} is unhappy and may leave."),
                    choice("Bring in a mediator", "${money(mediatorCost)}. Nobody wins, nobody walks."),
                ),
                a.lane,
            )
        }
    }
    is EquityAsk -> person(kind.employeeId)?.let { e ->
        Description(
            "${e.name} wants a stake",
            "Your best ${e.role.title.lowercase()} (skill ${e.skill}) says a salary isn't enough any more: they want a piece of what they're building.",
            listOf(
                choice("Give them 0.5% of the company", "You own a little less. They're with you for good."),
                choice("A raise instead", "15% more pay. They might still look around."),
                choice("No", "They'll be unhappy, and there are people hiring."),
            ),
            e.lane,
        )
    }
    RemoteWork -> Description(
        "The team wants to work from home",
        "A petition: two days a week at home. Most of the office signed it.",
        listOf(
            choice("Go hybrid", "Everyone's happier. A little less gets done."),
            choice("Keep everyone in", "Output stays up. Morale dips."),
        ),
    )
    is SideProject -> person(kind.employeeId)?.let { e ->
        Description(
            "${e.name}'s side project took off",
            "Something ${first(e.name)} built on weekends is getting real attention online. It's good, and it's not ours.",
            listOf(
                choice("Bring it in-house", "It becomes a company project: research points, and a proud ${first(e.name)}."),
                choice("Let them spin it out", "${first(e.name)} leaves to run it, with your blessing and a small stake for you."),
                choice("Ban side projects", "Focus on the day job. Nobody likes this."),
            ),
            e.lane,
        )
    }
    is DataBreach -> {
        val s = stake(kind.lane)
        Description(
            "A security hole in ${productName(kind.lane)}",
            "A researcher found a way into customer data. Nothing's been taken that we know of. Telling everyone is expensive and embarrassing; " +
                "patching it quietly is cheap, if nobody ever finds out.",
            listOf(
                choice("Disclose and fix it", "${money(s * 2)} and a dent in your reputation now."),
                choice("Patch it quietly", "${money(s * 0.3)}. If it comes out later, it's a scandal."),
            ),
            kind.lane,
        )
    }
    is ViralComplaint -> Description(
        "A customer's rant is going viral",
        "Someone's furious post about ${productName(kind.lane)} has millions of views. Some of it's even fair.",
        listOf(
            choice("Apologise and make it right", "${money(stake(kind.lane) * 0.5)} in refunds and goodwill."),
            choice("Let it blow over", "Free. It might not."),
        ),
        kind.lane,
    )
    is ClientBankrupt -> Description(
        "${kind.client} has gone bust",
        "Halfway through the project, the client filed for bankruptcy. They owe you ${money(kind.owed)} you'll probably never see.",
        listOf(
            choice("Write it off", "Move on. The project's cancelled."),
            choice("Take them to court", "${money(legalFee)} in fees for a decent chance of getting most of it back, in a few months."),
        ),
        Lane.WEBSITES,
    )
}

internal fun GameState.describeOpportunity(kind: Opportunity): Description? = when (kind) {
    Interns -> Description(
        "Two interns for the summer",
        "A university wants to place two students with you. Cheap, eager and green.",
        listOf(
            choice("Take them on", "Two juniors at a junior's pay. They need desks."),
            choice("Not this year", "Maybe next summer."),
        ),
    )
    is ConferenceTalk -> Description(
        "Speak at ${kind.conference}",
        "They want you on stage. It costs ${money(talkCost)} to get there and back, and people remember a good talk.",
        listOf(
            choice("Give the talk", "Reputation, and a strong candidate or two come looking."),
            choice("Decline", "Stay home and work."),
        ),
    )
    is Partnership -> Description(
        "${kind.partner} wants a white-label deal",
        "They'll pay ${money(kind.pay)} for your ${kind.lane.businessName.lowercase()} to build something under their name. " +
            "For ${Format.weeks(kind.weeks)} the team gets about a fifth less done on your own work.",
        listOf(
            choice("Sign it", "Half now, half when it's delivered."),
            choice("Pass", "Keep the team on your own products."),
        ),
        kind.lane,
    )
    is Acquihire -> Description(
        "${kind.startup} is selling its team",
        "A small startup ran out of runway. Its three people are good, and they'd join your ${kind.lane.businessName.lowercase()} for ${money(kind.price)}.",
        listOf(
            choice("Buy the team", "Three experienced hires at once." + if (freeDesks < 3) " You're short on desks." else ""),
            choice("Pass", "Someone else will snap them up."),
        ),
        kind.lane,
    )
    is AngelCheck -> Description(
        "${kind.investor} wants to invest",
        "An angel investor offers ${money(kind.amount)} for ${Format.percent(kind.equity)} of the company. No board seat, no strings.",
        listOf(
            choice("Take the money", "Cash now, a little less of the company."),
            choice("No thanks", "Keep it all."),
        ),
    )
    is Interview -> Description(
        "${kind.outlet} wants an interview",
        "Half an hour about you and the company. Most people come across well. Not everyone.",
        listOf(
            choice("Do it", "Usually good press. Sometimes a gaffe."),
            choice("Decline", "No risk, no reward."),
        ),
    )
}

private val GameState.mediatorCost: Double get() = 4_000.0 + 500.0 * staff.size
private val GameState.legalFee: Double get() = 12_000.0
private val GameState.talkCost: Double get() = 6_000.0

/** What customers would call the thing a lane sells. */
internal fun GameState.productName(lane: Lane): String = when (lane) {
    Lane.GAMES -> games?.releases?.lastOrNull()?.title ?: "your game"
    Lane.WEBSITES -> "a client site you built"
    Lane.APPS -> apps?.name ?: "your app"
    Lane.SOCIAL -> social?.name ?: "your platform"
    Lane.HARDWARE -> hardware?.onMarket?.name?.let { "the $it" } ?: "your device"
    Lane.AI -> ai?.modelName ?: "your model"
}

// Resolving

internal fun GameState.resolveDrama(kind: DramaEvent, choice: Int) {
    when (kind) {
        is Burnout -> {
            val e = person(kind.employeeId) ?: return
            if (choice == 0) {
                e.leaveUntil = week + 4
                e.morale = 0.95
                addLog("${e.name} is taking four weeks off.", LogTone.NEUTRAL, e.lane)
            } else {
                e.morale = max(0.0, e.morale - 0.1)
                later(rng.int(6, 16), Consequence.BurnoutQuit(e.id), chance = 0.5)
                addLog("${e.name} is pushing through. For now.", LogTone.NEUTRAL, e.lane)
            }
        }
        is StaffClash -> {
            val a = person(kind.a) ?: return
            val b = person(kind.b) ?: return
            when (choice) {
                0, 1 -> {
                    val (won, lost) = if (choice == 0) a to b else b to a
                    won.morale = min(1.0, won.morale + 0.1)
                    lost.morale = max(0.0, lost.morale - 0.3)
                    later(rng.int(4, 12), Consequence.BurnoutQuit(lost.id), chance = 0.35)
                    addLog("You backed ${first(won.name)}. ${first(lost.name)} is not happy.", LogTone.NEUTRAL, won.lane)
                }
                else -> {
                    spend(mediatorCost, a.lane)
                    a.morale = max(0.0, a.morale - 0.05)
                    b.morale = max(0.0, b.morale - 0.05)
                    addLog("A mediator got ${first(a.name)} and ${first(b.name)} talking again.", LogTone.NEUTRAL, a.lane)
                }
            }
        }
        is EquityAsk -> {
            val e = person(kind.employeeId) ?: return
            when (choice) {
                0 -> {
                    venture.founderEquity = max(0.0, venture.founderEquity - 0.005)
                    e.morale = 1.0
                    e.lastRaiseWeek = week
                    addLog("${e.name} now owns a piece of the company, and acts like it.", LogTone.GOOD, e.lane)
                }
                1 -> {
                    e.salary *= 1.15
                    e.lastRaiseWeek = week
                    e.morale = min(1.0, e.morale + 0.05)
                    later(rng.int(8, 20), Consequence.LeftForRival(e.id, rivalFor(e.lane)), chance = 0.35)
                    addLog("${e.name} took the raise. For now.", LogTone.NEUTRAL, e.lane)
                }
                else -> {
                    e.morale = max(0.0, e.morale - 0.25)
                    later(rng.int(6, 16), Consequence.LeftForRival(e.id, rivalFor(e.lane)), chance = 0.6)
                    addLog("${e.name} didn't take no well.", LogTone.BAD, e.lane)
                }
            }
        }
        RemoteWork -> {
            if (choice == 0) {
                hybrid = true
                for (e in staff) e.morale = min(1.0, e.morale + 0.12)
                addLog("The office is hybrid now. Fridays are quiet.", LogTone.GOOD)
            } else {
                for (e in staff) e.morale = max(0.0, e.morale - 0.08)
                addLog("Everyone stays in the office. There's grumbling.", LogTone.NEUTRAL)
            }
        }
        is SideProject -> {
            val e = person(kind.employeeId) ?: return
            when (choice) {
                0 -> {
                    researchPoints += 15.0
                    e.morale = min(1.0, e.morale + 0.15)
                    addLog("${e.name}'s side project is a company project now. +15 research points.", LogTone.GOOD, e.lane)
                }
                1 -> {
                    staff = staff - e
                    val stakeValue = 20_000.0 + 5_000.0 * e.skill
                    cash += stakeValue
                    recordRevenue(stakeValue, null)
                    reputation.adjust(1.0)
                    addLog("${e.name} left to run their own startup. Your small stake is already worth ${money(stakeValue)}.", LogTone.NEUTRAL, e.lane)
                }
                else -> {
                    e.morale = max(0.0, e.morale - 0.2)
                    for (o in staff) if (o.id != e.id) o.morale = max(0.0, o.morale - 0.03)
                    addLog("No more side projects. ${first(e.name)} took it personally.", LogTone.BAD, e.lane)
                }
            }
        }
        is DataBreach -> {
            val s = stake(kind.lane)
            if (choice == 0) {
                spend(s * 2, kind.lane)
                reputation.scar(ScarKind.PRIVACY, 0.12, week, "Disclosed a security hole in ${productName(kind.lane)}.")
                addLog("You disclosed the security hole and fixed it. Painful, and over.", LogTone.NEUTRAL, kind.lane)
            } else {
                spend(s * 0.3, kind.lane)
                later(rng.int(10, 40), Consequence.BreachExposed(kind.lane, (s * 6).roundedMoney()), chance = 0.45)
                addLog("The hole is patched. Nobody needs to know.", LogTone.NEUTRAL, kind.lane)
            }
        }
        is ViralComplaint -> {
            if (choice == 0) {
                spend(stake(kind.lane) * 0.5, kind.lane)
                reputation.adjust(1.0)
                addLog("You apologised and made it right. The replies are kinder now.", LogTone.GOOD, kind.lane)
            } else if (rng.chance(0.5)) {
                reputation.adjust(-3.0)
                addLog("It didn't blow over. It's still trending.", LogTone.BAD, kind.lane)
            } else {
                addLog("It blew over.", LogTone.NEUTRAL, kind.lane)
            }
        }
        is ClientBankrupt -> {
            web?.active?.removeAll { it.id == kind.contractId }
            if (choice == 1) {
                spend(legalFee, Lane.WEBSITES)
                later(rng.int(12, 26), Consequence.ClientPays(kind.client, (kind.owed * 0.7).roundedMoney()), chance = 0.6)
                addLog("You're taking ${kind.client} to court.", LogTone.NEUTRAL, Lane.WEBSITES)
            } else {
                addLog("${kind.client} went bust owing you ${money(kind.owed)}. Written off.", LogTone.BAD, Lane.WEBSITES)
            }
        }
    }
}

internal fun GameState.resolveOpportunity(kind: Opportunity, choice: Int) {
    if (choice != 0) return
    when (kind) {
        Interns -> {
            repeat(2) {
                val role = rng.pick(listOf(Role.ENGINEER, Role.DESIGNER, Role.MARKETER))
                val c = Candidate(
                    id = nextId(), name = "${rng.pick(Names.first)} ${rng.pick(Names.last)}", role = role, skill = rng.int(2, 3),
                    askingSalary = (role.baseSalary * 0.35 * world.salaries).roundToInt().toDouble(), contestedBy = null, expiresWeek = week,
                )
                hireDirect(c)
            }
            addLog("Two interns started. They brought cookies.", LogTone.GOOD)
        }
        is ConferenceTalk -> {
            spend(talkCost, null)
            reputation.adjust(2.0)
            candidates = candidates + makeCandidate(star = true)
            addLog("Your talk at ${kind.conference} went down well. A strong candidate got in touch.", LogTone.GOOD)
        }
        is Partnership -> {
            val half = kind.pay / 2
            cash += half
            recordRevenue(half, kind.lane)
            world.pressures += Pressure(kind.lane, "Building for ${kind.partner}", demand = 1.0, weeksLeft = kind.weeks, output = 0.8, mine = true)
            later(kind.weeks, Consequence.PartnerPays(kind.partner, kind.lane, half))
            addLog("Signed with ${kind.partner}: ${money(kind.pay)}, half up front.", LogTone.GOOD, kind.lane)
        }
        is Acquihire -> {
            if (cash < kind.price) {
                addLog("You couldn't find ${money(kind.price)} for ${kind.startup}'s team.", LogTone.BAD, kind.lane)
                return
            }
            spend(kind.price, kind.lane)
            repeat(3) {
                val c = makeCandidate(role = pickRoleFor(kind.lane)).copy(skill = rng.int(6, 8))
                hireDirect(c.copy(askingSalary = c.askingSalary * 1.1, contestedBy = null), kind.lane)
            }
            stats.acquisitions += 1
            addLog("${kind.startup}'s team joined the ${kind.lane.businessName.lowercase()}.", LogTone.GOOD, kind.lane)
        }
        is AngelCheck -> {
            cash += kind.amount
            venture.founderEquity = max(0.0, venture.founderEquity * (1 - kind.equity))
            addLog("${kind.investor} invested ${money(kind.amount)} for ${Format.percent(kind.equity)}.", LogTone.GOOD)
        }
        is Interview -> {
            if (rng.chance(0.75)) {
                reputation.adjust(2.5)
                addLog("The ${kind.outlet} interview went well. People liked you.", LogTone.GOOD)
            } else {
                reputation.adjust(-2.0)
                addLog("You said something on ${kind.outlet} you'd like to take back. The clip is everywhere.", LogTone.BAD)
            }
        }
    }
}

/** A role that suits [lane], weighted like the talent pool. */
private fun GameState.pickRoleFor(lane: Lane): Role {
    var roll = rng.unit() * Role.entries.sumOf { lane.weight(it) }
    for (r in Role.entries) {
        roll -= lane.weight(r)
        if (roll <= 0) return r
    }
    return Role.ENGINEER
}

/** The rival most likely to hire from [lane], or any rival. */
private fun GameState.rivalFor(lane: Lane?): String = world.rivals.firstOrNull { it.lane == lane }?.name ?: rng.pick(world.rivals).name

/** A consequence lands. */
private fun GameState.land(c: Consequence) {
    when (c) {
        is Consequence.BurnoutQuit -> person(c.employeeId)?.let { e ->
            staff = staff - e
            addLog("${e.name} quit. They'd had enough.", LogTone.BAD, e.lane)
        }
        is Consequence.LeftForRival -> person(c.employeeId)?.let { e ->
            staff = staff - e
            strengthenRival(c.rival, 0.03)
            addLog("${e.name} left for ${c.rival}.", LogTone.BAD, e.lane)
            publish(NewsDesk.INDUSTRY, techOutlet(e.lane, rng), "${c.rival} hires ${e.name} from $companyName", lane = e.lane)
        }
        is Consequence.BreachExposed -> {
            spend(c.fine, c.lane)
            reputation.scar(ScarKind.PRIVACY, 0.45, week, "Hid a security hole in ${productName(c.lane)}.")
            reputation.scar(ScarKind.SCANDAL, 0.25, week, "The cover-up came out.")
            stats.scandals += 1
            addLog("The security hole you patched quietly came out. Regulators fined you ${money(c.fine)}.", LogTone.BAD, c.lane)
            publish(NewsDesk.YOU, techOutlet(c.lane, rng), "$companyName hid a security hole for months", "Fined ${money(c.fine)}.", LogTone.BAD, c.lane, lead = true)
        }
        is Consequence.Verdict -> settleLawsuit(c)
        is Consequence.ClientPays -> {
            cash += c.amount
            recordRevenue(c.amount, Lane.WEBSITES)
            addLog("The court sided with you: ${c.client}'s administrators paid ${money(c.amount)}.", LogTone.GOOD, Lane.WEBSITES)
        }
        is Consequence.PartnerPays -> {
            cash += c.amount
            recordRevenue(c.amount, c.lane)
            addLog("Delivered the ${c.partner} project. The other ${money(c.amount)} is in.", LogTone.GOOD, c.lane)
        }
        is Consequence.PitchResult -> landPitch(c)
    }
}

/** For tests and tools: put things in front of the player directly. */
object Sandbox {
    fun offer(s: GameState, kind: EventKind, weeks: Int) = s.offer(kind, weeks)
    fun enqueue(s: GameState, kind: EventKind) = s.enqueue(kind)
    fun setCash(s: GameState, cash: Double) {
        s.cash = cash
    }
}
