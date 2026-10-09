package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
sealed interface EventKind {
    @Serializable @SerialName("poach")
    data class Poach(val employeeId: Int, val rival: String) : EventKind

    @Serializable @SerialName("buyout")
    data class Buyout(val bidder: String, val price: Double) : EventKind

    @Serializable @SerialName("boardDemand")
    data class BoardDemandEvent(val demand: BoardDemand) : EventKind

    @Serializable @SerialName("recall")
    data class Recall(val model: String, val units: Int, val price: Double) : EventKind

    @Serializable @SerialName("componentShortage")
    data object ComponentShortage : EventKind

    @Serializable @SerialName("moderationFire")
    data object ModerationFire : EventKind

    @Serializable @SerialName("pressScandal")
    data object PressScandal : EventKind

    @Serializable @SerialName("botWave")
    data object BotWave : EventKind

    @Serializable @SerialName("regulatorInquiry")
    data object RegulatorInquiry : EventKind

    @Serializable @SerialName("viralMoment")
    data class ViralMoment(val lane: Lane) : EventKind


    @Serializable @SerialName("comboProblem")
    data class ComboProblem(val combo: Combo) : EventKind

    @Serializable @SerialName("ecosystemAntitrust")
    data object EcosystemAntitrust : EventKind

    @Serializable @SerialName("convention")
    data object Convention : EventKind
}

@Serializable
data class EventChoice(val label: String, val detail: String)

/** A decision that blocks the calendar until you make it. */
@Serializable
data class PendingEvent(
    val id: Int,
    val week: Int,
    val kind: EventKind,
    val title: String,
    val body: String,
    val choices: List<EventChoice>,
    val lane: Lane? = null,
)

private data class Description(val title: String, val body: String, val choices: List<EventChoice>, val lane: Lane? = null)

internal fun GameState.enqueue(kind: EventKind) {
    if (events.any { it.kind == kind }) return
    val d = describe(kind) ?: return
    events = events + PendingEvent(nextId(), week, kind, d.title, d.body, d.choices, d.lane)
}

private fun choice(label: String, detail: String) = EventChoice(label, detail)

private fun GameState.describe(kind: EventKind): Description? = when (kind) {
    is EventKind.Poach -> {
        val e = staff.firstOrNull { it.id == kind.employeeId }
        if (e == null) {
            null
        } else {
            Description(
                "${kind.rival} is poaching ${e.name}",
                "They've offered your ${e.role.title.lowercase()} ${e.name} (skill ${e.skill}) about 40% more than you pay.",
                listOf(
                    choice("Counter-offer", "Raise to ${money(e.salary * 1.4)}/week."),
                    choice("Let them go", "${kind.rival} gets stronger."),
                ),
                e.lane,
            )
        }
    }
    is EventKind.Buyout -> Description(
        "${kind.bidder} wants to buy you",
        "${money(kind.price)} for the whole company. Your stake would be worth ${money(kind.price * venture.founderEquity)}.",
        listOf(
            choice("Sell", "Take the money. The game ends here."),
            choice("Not for sale", if (venture.board == null) "Keep building." else "The board won't love it."),
        ),
    )
    is EventKind.BoardDemandEvent -> Description(
        kind.demand.title, kind.demand.body,
        listOf(
            choice("Comply", "Confidence recovers."),
            choice("Push back", "Confidence drops. Too low and they replace you."),
        ),
    )
    is EventKind.Recall -> {
        val cost = recallCost(kind.units, kind.price)
        Description(
            "Battery issue in the ${kind.model}",
            "Reports of swelling batteries are piling up. A full recall costs about ${money(cost)} and halts production " +
                "for a quarter. A quiet fix is cheaper, if nobody finds out.",
            listOf(
                choice("Full recall", "${money(cost)}, production halted 13 weeks."),
                choice("Quiet fix", "${money(cost * 0.15)} now. It might leak."),
            ),
            Lane.HARDWARE,
        )
    }
    EventKind.ComponentShortage -> Description(
        "Component shortage",
        "Your supplier can't get enough chips. Your factory orders are stuck.",
        listOf(
            choice("Pay the spot premium", "${money(shortagePremium)} to keep deliveries on time."),
            choice("Accept the delay", "Every pending order slips 8 weeks."),
        ),
        Lane.HARDWARE,
    )
    EventKind.ModerationFire -> social?.let { s ->
        Description(
            "Moderation fire on ${s.name}",
            "A harassment campaign is trending on the platform and the screenshots are everywhere.",
            listOf(
                choice("Emergency moderation sprint", "${money(moderationSprintCost(s))}, toxicity drops."),
                choice("Let it burn out", "Reputation takes the hit."),
            ),
            Lane.SOCIAL,
        )
    }
    EventKind.PressScandal -> social?.let { s ->
        Description(
            "Press scandal",
            "A major outlet is running a story on what really happens on ${s.name}.",
            listOf(
                choice("Apologise and overhaul policy", "Smaller hit, permanently more moderation."),
                choice("Deny everything", "Might blow over. Might not."),
            ),
            Lane.SOCIAL,
        )
    }
    EventKind.BotWave -> social?.let { s ->
        Description(
            "Advertisers want to know who's real",
            "${Format.percent(s.botShare)} of accounts on ${s.name} look like bots. Advertisers are asking questions.",
            listOf(
                choice("Purge the bots", "User count drops, trust recovers."),
                choice("Keep the numbers", "Growth charts stay pretty. For now."),
            ),
            Lane.SOCIAL,
        )
    }
    EventKind.RegulatorInquiry -> Description(
        "Regulators come knocking",
        "Your AI lab is big enough to have a file now. They want to see your safety process.",
        listOf(
            choice("Cooperate and fund a safety team", "${money(600_000.0)}, heat drops a lot."),
            choice("Lawyer up", "${money(200_000.0)} in fees. They may come back angrier."),
        ),
        Lane.AI,
    )
    is EventKind.ViralMoment -> Description(
        "Your ${kind.lane.businessName.lowercase()} went viral",
        "Nobody planned it, but everybody's talking about it.",
        listOf(choice("Ride the wave", "A free boost.")),
        kind.lane,
    )
    is EventKind.ComboProblem -> {
        val combo = kind.combo
        Description(
            "${combo.product}: ${combo.headline}",
            combo.problem,
            listOf(
                choice("Dial it back", "${money(comboFixCost(combo))} and lower ${combo.dial.lowercase()}."),
                choice("Ride it out", rideOutDetail(combo)),
            ),
        )
    }
    EventKind.Convention -> {
        val project = games?.project
        val about = project?.let { "Show off ${it.title} and build hype before launch." } ?: "You've nothing in development, but fans still want to meet you."
        val keynote = (games?.fans ?: 0.0) >= 100_000
        Description(
            "BootCon is next week",
            "The biggest games convention of the year. $about",
            listOfNotNull(
                choice("Small booth", "${money(15_000.0)}. A little hype, a few new fans."),
                choice("Big booth", "${money(120_000.0)}. Real hype, lots of new fans."),
                if (keynote) choice("Keynote stage", "${money(600_000.0)}. Everyone's watching.") else null,
                choice("Skip it", "Save the money."),
            ),
            Lane.GAMES,
        )
    }
    EventKind.EcosystemAntitrust -> Description(
        "Antitrust regulators circle your ecosystem",
        "Hardware, software and services under one roof looks a lot like a monopoly to some people.",
        listOf(
            choice("Open up the platform", "Lose some lock-in. Regulators back off."),
            choice("Fight it in court", "${money(2_000_000.0)} in legal fees and a coin flip."),
        ),
    )
}

/** Answer the decision at the front of the queue. */
fun GameState.choose(index: Int) {
    val event = events.firstOrNull() ?: throw notAvailable("Nothing needs a decision.")
    if (index !in event.choices.indices) throw invalid("Pick one of the options.")
    events = events.drop(1)
    resolve(event.kind, index)
}

private fun GameState.resolve(kind: EventKind, choice: Int) {
    when (kind) {
        is EventKind.Poach -> {
            val e = staff.firstOrNull { it.id == kind.employeeId } ?: return
            if (choice == 0) {
                e.salary *= 1.4
                e.morale = min(1.0, e.morale + 0.15)
                addLog("${e.name} stays, at a price.")
            } else {
                staff = staff - e
                strengthenRival(kind.rival, 0.03)
                addLog("${e.name} left for ${kind.rival}.", LogTone.BAD)
            }
        }

        is EventKind.Buyout -> {
            if (choice == 0) {
                end(EndingKind.ACQUIRED, kind.price, kind.bidder)
            } else {
                stats.buyoutsDeclined += 1
                venture.board?.let { it.confidence -= 0.08 }
                addLog("You turned down ${kind.bidder}.")
            }
        }

        is EventKind.BoardDemandEvent -> {
            val b = venture.board
            if (choice == 0) {
                applyBoardDemand(kind.demand)
                b?.let {
                    it.confidence += 0.15
                    it.demandsMet += 1
                }
            } else {
                b?.let {
                    it.confidence -= 0.15
                    it.demandsDefied += 1
                }
                addLog("You told the board no.", LogTone.BAD)
                if (b != null && b.confidence <= 0.05) end(EndingKind.OUSTED)
            }
        }

        is EventKind.Recall -> {
            val cost = recallCost(kind.units, kind.price)
            stats.recalls += 1
            if (choice == 0) {
                spend(cost, Lane.HARDWARE)
                hardware?.productionHaltedUntil = week + 13
                reputation.scar(ScarKind.RECALL, 0.25, week, "Recalled the ${kind.model}.")
                addLog("Full recall of the ${kind.model}. It costs you a quarter.", LogTone.BAD)
            } else {
                spend(cost * 0.15, Lane.HARDWARE)
                if (rng.chance(0.45)) {
                    reputation.scar(ScarKind.RECALL, 0.5, week, "Covered up the ${kind.model} defect.")
                    reputation.scar(ScarKind.SCANDAL, 0.35, week, "The ${kind.model} cover-up leaked.")
                    stats.scandals += 1
                    addLog("The quiet fix leaked. The headlines say 'cover-up'.", LogTone.BAD)
                } else {
                    addLog("The quiet fix held. Nobody noticed.")
                }
            }
        }

        EventKind.ComponentShortage -> {
            if (choice == 0) {
                spend(shortagePremium, Lane.HARDWARE)
                addLog("Paid the spot premium. Deliveries stay on schedule.")
            } else {
                hardware?.let { h ->
                    for (o in h.orders) o.arrivalWeek += 8
                    addLog("Factory orders slip 8 weeks.", LogTone.BAD)
                }
            }
        }

        EventKind.ModerationFire -> {
            val s = social ?: return
            if (choice == 0) {
                spend(moderationSprintCost(s), Lane.SOCIAL)
                s.toxicity = max(0.0, s.toxicity - 0.15)
                s.moderation = min(1.0, s.moderation + 0.1)
                addLog("The moderation sprint works. Mostly.")
            } else {
                reputation.adjust(-4.0)
                s.toxicity = min(1.0, s.toxicity + 0.05)
                s.users *= 0.97
                addLog("The fire burns itself out, and takes some users with it.", LogTone.BAD)
            }
        }

        EventKind.PressScandal -> {
            stats.scandals += 1
            if (choice == 0) {
                reputation.adjust(-3.0)
                social?.let {
                    it.moderation = it.moderation.nudged(0.15)
                    it.toxicity = it.toxicity.nudged(-0.1)
                }
                addLog("You apologised and rewrote the rules.")
            } else if (rng.chance(0.5)) {
                addLog("The denial worked. The story died.")
            } else {
                reputation.scar(ScarKind.SCANDAL, 0.45, week, "Denied a story that turned out true.")
                addLog("The denial made it worse. Follow-up stories everywhere.", LogTone.BAD)
            }
        }

        EventKind.BotWave -> {
            val s = social ?: return
            if (choice == 0) {
                s.users *= 1 - s.botShare * 0.8
                s.botShare *= 0.2
                reputation.adjust(2.0)
                addLog("Purged the bots. The user count looks smaller and more honest.")
            } else {
                s.botShare = min(0.6, s.botShare + 0.05)
                reputation.scar(ScarKind.SCANDAL, 0.15, week, "Inflated user numbers with bots.")
                addLog("Kept the bots. Advertisers are quietly discounting your numbers.", LogTone.BAD)
            }
        }

        EventKind.RegulatorInquiry -> {
            if (choice == 0) {
                spend(600_000.0, Lane.AI)
                ai?.let { it.regulatoryHeat = it.regulatoryHeat.nudged(-0.5) }
                addLog("A new safety team keeps the regulators satisfied.")
            } else {
                spend(200_000.0, Lane.AI)
                ai?.let { it.regulatoryHeat = it.regulatoryHeat.nudged(-0.1) }
                if (rng.chance(0.4)) {
                    val fine = max(500_000.0, trailingRevenue(Lane.AI, 52) * 0.04)
                    spend(fine, Lane.AI)
                    reputation.scar(ScarKind.SCANDAL, 0.3, week, "Fined by AI regulators.")
                    stats.hearings += 1
                    addLog("Regulators come back with a ${money(fine)} fine.", LogTone.BAD)
                }
            }
        }

        is EventKind.ViralMoment -> {
            reputation.adjust(3.0)
            when (kind.lane) {
                Lane.GAMES -> games?.let { g ->
                    g.releases.firstOrNull()?.let { it.weeklySales *= 2.5 }
                    g.fans += 5_000
                }
                Lane.APPS -> apps?.let { it.users *= 1.3 }
                Lane.SOCIAL -> social?.let { it.users *= 1.4 }
                Lane.WEBSITES -> web?.let { it.standing = it.standing.nudged(0.1) }
                Lane.HARDWARE -> hardware?.onMarket?.let { it.hype += 0.6 }
                Lane.AI -> ai?.let { it.subscribers *= 1.3 }
            }
            addLog("You rode the viral wave in ${kind.lane.title}.", LogTone.GOOD)
        }

        is EventKind.ComboProblem -> {
            val c = crossover(kind.combo) ?: return
            c.incidents += 1
            if (choice == 0) {
                spend(comboFixCost(kind.combo), null)
                c.intensity = max(0.0, c.intensity - 0.3)
                c.pressure = 0.0
                addLog("You dialled back ${kind.combo.product}.")
            } else {
                c.pressure = 0.2
                rideOut(kind.combo)
            }
        }

        EventKind.Convention -> {
            val g = games ?: return
            val keynote = g.fans >= 100_000
            val options = listOfNotNull(Triple(15_000.0, 8.0, 0.02), Triple(120_000.0, 20.0, 0.05), if (keynote) Triple(600_000.0, 35.0, 0.1) else null)
            val pick = options.getOrNull(choice) ?: run {
                addLog("You skipped BootCon this year.", LogTone.NEUTRAL, Lane.GAMES)
                return
            }
            val (cost, hype, fanGain) = pick
            spend(cost, Lane.GAMES)
            g.project?.let { it.hype = minOf(100.0, it.hype + hype) }
            g.fans += max(500.0, g.fans * fanGain)
            if (choice == 2) reputation.adjust(2.0)
            addLog(
                "BootCon went well. " + (g.project?.let { "Hype for ${it.title} is up. " } ?: "") + "New fans signed up.",
                LogTone.GOOD, Lane.GAMES,
            )
        }

        EventKind.EcosystemAntitrust -> {
            stats.hearings += 1
            stats.ecosystemPressure = 0.0
            if (choice == 0) {
                crossover(Combo.HARDWARE_APPS)?.let { it.intensity = max(0.0, it.intensity - 0.4) }
                stats.ecosystemOpened = true
                addLog("You opened the platform to rivals. Regulators back off.")
            } else {
                spend(2_000_000.0, null)
                if (rng.chance(0.5)) {
                    addLog("You won in court. The ecosystem stays closed.", LogTone.GOOD)
                } else {
                    reputation.scar(ScarKind.ANTITRUST, 0.5, week, "Lost an antitrust case.")
                    val fine = trailingRevenue(weeks = 52) * 0.05
                    spend(fine, null)
                    addLog("You lost in court. A ${money(fine)} fine and a consent decree.", LogTone.BAD)
                }
            }
        }
    }
}

// Helpers

private fun recallCost(units: Int, price: Double): Double = min(units, 60_000) * price * 0.25

private fun moderationSprintCost(s: SocialPlatform): Double = min(2_000_000.0, max(20_000.0, s.users * 0.01))

private val GameState.shortagePremium: Double
    get() {
        val h = hardware ?: return 0.0
        val spec = h.design?.spec ?: h.onMarket?.spec
        val units = h.orders.sumOf { it.units }
        return units * (spec?.unitCost ?: 100.0) * 0.25
    }

fun GameState.comboFixCost(combo: Combo): Double = max(10_000.0, combo.buildCost * 0.2)

private fun rideOutDetail(combo: Combo): String = when (combo) {
    Combo.APPS_GAMES -> "Keep squeezing. Your reputation tanks everywhere."
    Combo.HARDWARE_APPS -> "Fight the lawsuit. Fines and an antitrust mark."
    Combo.AI_SOCIAL -> "Testify at the hearings."
    Combo.WEBSITES_APPS -> "Lose clients who feel betrayed."
    Combo.WEBSITES_AI -> "Some of the team walks out."
    Combo.GAMES_WEBSITES -> "You personally pull the all-nighters."
    Combo.GAMES_HARDWARE -> "Fans grumble and remember."
    Combo.HARDWARE_AI, Combo.APPS_AI, Combo.WEBSITES_HARDWARE -> "Eat the cost overrun."
    else -> "Take the reputation hit."
}

private fun GameState.rideOut(combo: Combo) {
    combo.scar?.let { scar ->
        val severity = when (combo) {
            Combo.APPS_GAMES -> 0.5
            Combo.APPS_SOCIAL -> 0.45
            Combo.HARDWARE_APPS, Combo.AI_SOCIAL, Combo.SOCIAL_HARDWARE -> 0.35
            else -> 0.2
        }
        reputation.scar(scar, severity, week, "${combo.product}: ${combo.problem}")
    }
    when (combo) {
        Combo.GAMES_SOCIAL -> {
            social?.let {
                it.toxicity = it.toxicity.nudged(0.2)
                it.users *= 0.95
            }
            addLog("Gamers and everyone else are at war on your platform.", LogTone.BAD)
        }
        Combo.WEBSITES_APPS -> {
            web?.let {
                it.standing = it.standing.nudged(-0.15)
                it.clientWariness = it.clientWariness.nudged(0.3)
            }
            addLog("Two big clients pulled their contracts. They don't trust you anymore.", LogTone.BAD)
        }
        Combo.APPS_GAMES -> addLog("'Predatory' is now the first word in every article about you.", LogTone.BAD)
        Combo.HARDWARE_APPS -> {
            val fine = max(250_000.0, trailingRevenue(weeks = 52) * 0.03)
            spend(fine, null)
            stats.hearings += 1
            addLog("Antitrust ruling: a ${money(fine)} fine.", LogTone.BAD)
        }
        Combo.HARDWARE_AI -> {
            spend(1_000_000.0, Lane.AI)
            addLog("The chip tape-out slipped. Another million into the fab.", LogTone.BAD)
        }
        Combo.AI_SOCIAL -> {
            stats.hearings += 1
            addLog("You spent a day testifying about your algorithm. The clips went everywhere.", LogTone.BAD)
        }
        Combo.AI_GAMES -> {
            stats.scandals += 1
            addLog("A player got one of your NPCs to say something unforgivable. It's the top post everywhere.", LogTone.BAD)
        }
        Combo.GAMES_WEBSITES -> {
            for (e in staff) e.morale = max(0.0, e.morale - 0.1)
            addLog("Agency deadline and launch week landed on the same night. Everyone covered both, and everyone's fried.", LogTone.BAD)
        }
        Combo.GAMES_HARDWARE -> {
            games?.let { it.fanTrust = it.fanTrust.nudged(-0.15, 0.3, 1.2) }
            addLog("Fans are furious about the exclusives.", LogTone.BAD)
        }
        Combo.WEBSITES_SOCIAL -> {
            social?.let { it.toxicity = it.toxicity.nudged(0.1) }
            addLog("Creators are accusing you of rigging the platform.", LogTone.BAD)
        }
        Combo.WEBSITES_HARDWARE -> {
            spend(150_000.0, Lane.WEBSITES)
            addLog("The kiosk support backlog cost you a fortune.", LogTone.BAD)
        }
        Combo.WEBSITES_AI -> {
            val quitters = staff.filter { it.lane == Lane.WEBSITES }.sortedBy { it.morale }.take(2).map { it.id }.toSet()
            staff = staff.filterNot { it.id in quitters }
            for (e in staff) if (e.lane == Lane.WEBSITES) e.morale -= 0.2
            addLog("${quitters.size} people quit rather than train their replacement.", LogTone.BAD)
        }
        Combo.APPS_SOCIAL -> {
            stats.scandals += 1
            addLog("A data-sharing leak made the front page.", LogTone.BAD)
        }
        Combo.APPS_AI -> {
            spend(300_000.0, Lane.APPS)
            addLog("A surprise inference bill: ${money(300_000.0)}.", LogTone.BAD)
        }
        Combo.SOCIAL_HARDWARE -> {
            stats.scandals += 1
            addLog("Venues are banning your glasses.", LogTone.BAD)
        }
    }
}

private fun GameState.strengthenRival(name: String, amount: Double) {
    world.rivals.firstOrNull { it.name == name }?.let { it.strength = min(1.0, it.strength + amount) }
}
