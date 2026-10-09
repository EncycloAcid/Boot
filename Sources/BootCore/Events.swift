import Foundation

public enum EventKind: Codable, Sendable, Equatable {
    case poach(employeeID: Int, rival: String)
    case buyout(bidder: String, price: Double)
    case boardDemand(BoardDemand)
    case recall(model: String, units: Int, price: Double)
    case componentShortage
    case moderationFire
    case pressScandal
    case botWave
    case regulatorInquiry
    case viralMoment(Lane)
    case burnoutWarning
    case comboProblem(Combo)
    case ecosystemAntitrust
}

public struct EventChoice: Codable, Sendable {
    public var label: String
    public var detail: String
}

/// A decision that blocks the calendar until you make it.
public struct PendingEvent: Codable, Identifiable, Sendable {
    public var id: Int
    public var week: Int
    public var kind: EventKind
    public var title: String
    public var body: String
    public var choices: [EventChoice]
    public var lane: Lane?
}

extension GameState {
    mutating func enqueue(_ kind: EventKind) {
        guard !events.contains(where: { $0.kind == kind }) else { return }
        guard let (title, body, choices, lane) = describe(kind) else { return }
        events.append(PendingEvent(id: nextID(), week: week, kind: kind, title: title, body: body,
                                   choices: choices, lane: lane))
    }

    private func describe(_ kind: EventKind) -> (String, String, [EventChoice], Lane?)? {
        switch kind {
        case let .poach(id, rival):
            guard let e = staff.first(where: { $0.id == id }) else { return nil }
            let counter = e.salary * 1.4
            return ("\(rival) is poaching \(e.name)",
                    "They've offered your \(e.role.name.lowercased()) \(e.name) (skill \(e.skill)) about 40% more than you pay.",
                    [EventChoice(label: "Counter-offer", detail: "Raise to \(money(counter))/week."),
                     EventChoice(label: "Let them go", detail: "\(rival) gets stronger.")],
                    e.lane)
        case let .buyout(bidder, price):
            return ("\(bidder) wants to buy you",
                    "\(money(price)) for the whole company. Your stake would be worth \(money(price * venture.founderEquity)).",
                    [EventChoice(label: "Sell", detail: "Take the money. The game ends here."),
                     EventChoice(label: "Not for sale", detail: venture.board == nil ? "Keep building." : "The board won't love it.")],
                    nil)
        case let .boardDemand(d):
            return (d.title, d.body,
                    [EventChoice(label: "Comply", detail: "Confidence recovers."),
                     EventChoice(label: "Push back", detail: "Confidence drops. Too low and they replace you.")],
                    nil)
        case let .recall(model, units, price):
            let cost = recallCost(units: units, price: price)
            return ("Battery issue in the \(model)",
                    "Reports of swelling batteries are piling up. A full recall costs about \(money(cost)) and halts production for a quarter. A quiet fix is cheaper, if nobody finds out.",
                    [EventChoice(label: "Full recall", detail: "\(money(cost)), production halted 13 weeks."),
                     EventChoice(label: "Quiet fix", detail: "\(money(cost * 0.15)) now. It might leak.")],
                    .hardware)
        case .componentShortage:
            let premium = shortagePremium
            return ("Component shortage",
                    "Your supplier can't get enough chips. Your factory orders are stuck.",
                    [EventChoice(label: "Pay the spot premium", detail: "\(money(premium)) to keep deliveries on time."),
                     EventChoice(label: "Accept the delay", detail: "Every pending order slips 8 weeks.")],
                    .hardware)
        case .moderationFire:
            guard let s = social else { return nil }
            let cost = min(2_000_000, max(20_000, s.users * 0.01))
            return ("Moderation fire on \(s.name)",
                    "A harassment campaign is trending on the platform and the screenshots are everywhere.",
                    [EventChoice(label: "Emergency moderation sprint", detail: "\(money(cost)), toxicity drops."),
                     EventChoice(label: "Let it burn out", detail: "Reputation takes the hit.")],
                    .social)
        case .pressScandal:
            guard let s = social else { return nil }
            return ("Press scandal",
                    "A major outlet is running a story on what really happens on \(s.name).",
                    [EventChoice(label: "Apologise and overhaul policy", detail: "Smaller hit, permanently more moderation."),
                     EventChoice(label: "Deny everything", detail: "Might blow over. Might not.")],
                    .social)
        case .botWave:
            guard let s = social else { return nil }
            return ("Advertisers want to know who's real",
                    "\(Format.percent(s.botShare)) of accounts on \(s.name) look like bots. Advertisers are asking questions.",
                    [EventChoice(label: "Purge the bots", detail: "User count drops, trust recovers."),
                     EventChoice(label: "Keep the numbers", detail: "Growth charts stay pretty. For now.")],
                    .social)
        case .regulatorInquiry:
            return ("Regulators come knocking",
                    "Your AI lab is big enough to have a file now. They want to see your safety process.",
                    [EventChoice(label: "Cooperate and fund a safety team", detail: "\(money(600_000)), heat drops a lot."),
                     EventChoice(label: "Lawyer up", detail: "\(money(200_000)) in fees. They may come back angrier.")],
                    .ai)
        case let .viralMoment(lane):
            return ("Your \(lane.businessName.lowercased()) went viral",
                    "Nobody planned it, but everybody's talking about it.",
                    [EventChoice(label: "Ride the wave", detail: "A free boost.")],
                    lane)
        case .burnoutWarning:
            return ("You're running on fumes",
                    "Your energy is at \(Int(founder.energy)). If it hits zero, it's over.",
                    [EventChoice(label: "Take two weeks off", detail: "Nothing moves without you, but you'll recover."),
                     EventChoice(label: "Push through", detail: "Keep going.")],
                    nil)
        case let .comboProblem(combo):
            let fix = comboFixCost(combo)
            return ("\(combo.product): \(combo.headline)",
                    combo.problem,
                    [EventChoice(label: "Dial it back", detail: "\(money(fix)) and lower \(combo.dial.lowercased())."),
                     EventChoice(label: "Ride it out", detail: rideOutDetail(combo))],
                    nil)
        case .ecosystemAntitrust:
            return ("Antitrust regulators circle your ecosystem",
                    "Hardware, software and services under one roof looks a lot like a monopoly to some people.",
                    [EventChoice(label: "Open up the platform", detail: "Lose some lock-in. Regulators back off."),
                     EventChoice(label: "Fight it in court", detail: "\(money(2_000_000)) in legal fees and a coin flip.")],
                    nil)
        }
    }

    public mutating func choose(_ index: Int) throws {
        guard let event = events.first else { throw ActionError.notAvailable("Nothing needs a decision.") }
        guard event.choices.indices.contains(index) else { throw ActionError.invalid("Pick one of the options.") }
        events.removeFirst()
        resolve(event.kind, choice: index)
    }

    private mutating func resolve(_ kind: EventKind, choice: Int) {
        switch kind {
        case let .poach(id, rival):
            guard let i = staff.firstIndex(where: { $0.id == id }) else { return }
            if choice == 0 {
                staff[i].salary *= 1.4
                staff[i].morale = min(1, staff[i].morale + 0.15)
                addLog("\(staff[i].name) stays, at a price.", .neutral)
            } else {
                let e = staff.remove(at: i)
                strengthenRival(named: rival, by: 0.03)
                addLog("\(e.name) left for \(rival).", .bad)
            }

        case let .buyout(bidder, price):
            if choice == 0 {
                end(.acquired, price: price, buyer: bidder)
            } else {
                stats.buyoutsDeclined += 1
                venture.board?.confidence -= 0.08
                addLog("You turned down \(bidder).", .neutral)
            }

        case let .boardDemand(d):
            if choice == 0 {
                applyBoardDemand(d)
                venture.board?.confidence += 0.15
                venture.board?.demandsMet += 1
            } else {
                venture.board?.confidence -= 0.15
                venture.board?.demandsDefied += 1
                addLog("You told the board no.", .bad)
                if let c = venture.board?.confidence, c <= 0.05 { end(.ousted) }
            }

        case let .recall(model, units, price):
            let cost = recallCost(units: units, price: price)
            stats.recalls += 1
            if choice == 0 {
                spend(cost, lane: .hardware)
                hardware?.productionHaltedUntil = week + 13
                reputation.scar(.recall, severity: 0.25, week: week, note: "Recalled the \(model).")
                addLog("Full recall of the \(model). It costs you a quarter.", .bad)
            } else {
                spend(cost * 0.15, lane: .hardware)
                if rng.chance(0.45) {
                    reputation.scar(.recall, severity: 0.5, week: week, note: "Covered up the \(model) defect.")
                    reputation.scar(.scandal, severity: 0.35, week: week, note: "The \(model) cover-up leaked.")
                    stats.scandals += 1
                    addLog("The quiet fix leaked. The headlines say 'cover-up'.", .bad)
                } else {
                    addLog("The quiet fix held. Nobody noticed.", .neutral)
                }
            }

        case .componentShortage:
            if choice == 0 {
                spend(shortagePremium, lane: .hardware)
                addLog("Paid the spot premium. Deliveries stay on schedule.", .neutral)
            } else if hardware != nil {
                for i in hardware!.orders.indices { hardware!.orders[i].arrivalWeek += 8 }
                addLog("Factory orders slip 8 weeks.", .bad)
            }

        case .moderationFire:
            guard let s = social else { return }
            if choice == 0 {
                spend(min(2_000_000, max(20_000, s.users * 0.01)), lane: .social)
                social!.toxicity = max(0, s.toxicity - 0.15)
                social!.moderation = min(1, s.moderation + 0.1)
                addLog("The moderation sprint works. Mostly.", .neutral)
            } else {
                reputation.adjust(-4)
                social!.toxicity = min(1, s.toxicity + 0.05)
                social!.users *= 0.97
                addLog("The fire burns itself out, and takes some users with it.", .bad)
            }

        case .pressScandal:
            stats.scandals += 1
            if choice == 0 {
                reputation.adjust(-3)
                social?.moderation.nudge(0.15)
                social?.toxicity.nudge(-0.1)
                addLog("You apologised and rewrote the rules.", .neutral)
            } else if rng.chance(0.5) {
                addLog("The denial worked. The story died.", .neutral)
            } else {
                reputation.scar(.scandal, severity: 0.45, week: week, note: "Denied a story that turned out true.")
                addLog("The denial made it worse. Follow-up stories everywhere.", .bad)
            }

        case .botWave:
            guard let s = social else { return }
            if choice == 0 {
                social!.users *= 1 - s.botShare * 0.8
                social!.botShare *= 0.2
                reputation.adjust(2)
                addLog("Purged the bots. The user count looks smaller and more honest.", .neutral)
            } else {
                social!.botShare = min(0.6, s.botShare + 0.05)
                reputation.scar(.scandal, severity: 0.15, week: week, note: "Inflated user numbers with bots.")
                addLog("Kept the bots. Advertisers are quietly discounting your numbers.", .bad)
            }

        case .regulatorInquiry:
            if choice == 0 {
                spend(600_000, lane: .ai)
                ai?.regulatoryHeat.nudge(-0.5)
                addLog("A new safety team keeps the regulators satisfied.", .neutral)
            } else {
                spend(200_000, lane: .ai)
                ai?.regulatoryHeat.nudge(-0.1)
                if rng.chance(0.4) {
                    let fine = max(500_000, trailingRevenue(lane: .ai, weeks: 52) * 0.04)
                    spend(fine, lane: .ai)
                    reputation.scar(.scandal, severity: 0.3, week: week, note: "Fined by AI regulators.")
                    stats.hearings += 1
                    addLog("Regulators come back with a \(money(fine)) fine.", .bad)
                }
            }

        case let .viralMoment(lane):
            reputation.adjust(3)
            switch lane {
            case .games:
                if games != nil, !games!.releases.isEmpty { games!.releases[0].weeklySales *= 2.5 }
                games?.fans += 5_000
            case .apps: apps?.users *= 1.3
            case .social: social?.users *= 1.4
            case .websites: web?.standing.nudge(0.1)
            case .hardware: hardware?.onMarket?.hype += 0.6
            case .ai: ai?.subscribers *= 1.3
            }
            addLog("You rode the viral wave in \(lane.name).", .good)

        case .burnoutWarning:
            if choice == 0 { takeVacation() }

        case let .comboProblem(combo):
            guard let i = crossovers.firstIndex(where: { $0.combo == combo }) else { return }
            crossovers[i].incidents += 1
            if choice == 0 {
                spend(comboFixCost(combo), lane: nil)
                crossovers[i].intensity = max(0, crossovers[i].intensity - 0.3)
                crossovers[i].pressure = 0
                addLog("You dialled back \(combo.product).", .neutral)
            } else {
                crossovers[i].pressure = 0.2
                rideOut(combo)
            }

        case .ecosystemAntitrust:
            stats.hearings += 1
            stats.ecosystemPressure = 0
            if choice == 0 {
                if let i = crossovers.firstIndex(where: { $0.combo == .hardwareApps }) {
                    crossovers[i].intensity = max(0, crossovers[i].intensity - 0.4)
                }
                stats.ecosystemOpened = true
                addLog("You opened the platform to rivals. Regulators back off.", .neutral)
            } else {
                spend(2_000_000, lane: nil)
                if rng.chance(0.5) {
                    addLog("You won in court. The ecosystem stays closed.", .good)
                } else {
                    reputation.scar(.antitrust, severity: 0.5, week: week, note: "Lost an antitrust case.")
                    let fine = trailingRevenue(weeks: 52) * 0.05
                    spend(fine, lane: nil)
                    addLog("You lost in court. A \(money(fine)) fine and a consent decree.", .bad)
                }
            }
        }
    }

    // MARK: Helpers

    private func recallCost(units: Int, price: Double) -> Double {
        Double(min(units, 60_000)) * price * 0.25
    }

    private var shortagePremium: Double {
        guard let h = hardware else { return 0 }
        let spec = h.design?.spec ?? h.onMarket?.spec
        let units = h.orders.reduce(0) { $0 + $1.units }
        return Double(units) * (spec?.unitCost ?? 100) * 0.25
    }

    public func comboFixCost(_ combo: Combo) -> Double {
        max(10_000, combo.buildCost * 0.2)
    }

    private func rideOutDetail(_ combo: Combo) -> String {
        switch combo {
        case .appsGames: "Keep squeezing. Your reputation tanks everywhere."
        case .hardwareApps: "Fight the lawsuit. Fines and an antitrust mark."
        case .aiSocial: "Testify at the hearings."
        case .websitesApps: "Lose clients who feel betrayed."
        case .websitesAI: "Some of the team walks out."
        case .gamesWebsites: "You personally pull the all-nighters."
        case .gamesHardware: "Fans grumble and remember."
        case .hardwareAI, .appsAI, .websitesHardware: "Eat the cost overrun."
        default: "Take the reputation hit."
        }
    }

    private mutating func rideOut(_ combo: Combo) {
        if let scar = combo.scar {
            let severity: Double = switch combo {
            case .appsGames: 0.5
            case .appsSocial: 0.45
            case .hardwareApps, .aiSocial, .socialHardware: 0.35
            default: 0.2
            }
            reputation.scar(scar, severity: severity, week: week, note: "\(combo.product): \(combo.problem)")
        }
        switch combo {
        case .gamesSocial:
            social?.toxicity.nudge(0.2)
            social?.users *= 0.95
            addLog("Gamers and everyone else are at war on your platform.", .bad)
        case .websitesApps:
            web?.standing.nudge(-0.15)
            web?.clientWariness.nudge(0.3)
            addLog("Two big clients pulled their contracts. They don't trust you anymore.", .bad)
        case .appsGames:
            addLog("'Predatory' is now the first word in every article about you.", .bad)
        case .hardwareApps:
            let fine = max(250_000, trailingRevenue(weeks: 52) * 0.03)
            spend(fine, lane: nil)
            stats.hearings += 1
            addLog("Antitrust ruling: a \(money(fine)) fine.", .bad)
        case .hardwareAI:
            spend(1_000_000, lane: .ai)
            addLog("The chip tape-out slipped. Another million into the fab.", .bad)
        case .aiSocial:
            stats.hearings += 1
            addLog("You spent a day testifying about your algorithm. The clips went everywhere.", .bad)
        case .aiGames:
            stats.scandals += 1
            addLog("A player got one of your NPCs to say something unforgivable. It's the top post everywhere.", .bad)
        case .gamesWebsites:
            founder.energy = max(0, founder.energy - 15)
            addLog("Agency deadline and launch week landed on the same night. You covered both.", .bad)
        case .gamesHardware:
            games?.fanTrust.nudge(-0.15, within: 0.3...1.2)
            addLog("Fans are furious about the exclusives.", .bad)
        case .websitesSocial:
            social?.toxicity.nudge(0.1)
            addLog("Creators are accusing you of rigging the platform.", .bad)
        case .websitesHardware:
            spend(150_000, lane: .websites)
            addLog("The kiosk support backlog cost you a fortune.", .bad)
        case .websitesAI:
            let quitters = staff.filter { $0.lane == .websites }.sorted { $0.morale < $1.morale }.prefix(2).map(\.id)
            staff.removeAll { quitters.contains($0.id) }
            for i in staff.indices where staff[i].lane == .websites { staff[i].morale -= 0.2 }
            addLog("\(quitters.count) people quit rather than train their replacement.", .bad)
        case .appsSocial:
            stats.scandals += 1
            addLog("A data-sharing leak made the front page.", .bad)
        case .appsAI:
            spend(300_000, lane: .apps)
            addLog("A surprise inference bill: \(money(300_000)).", .bad)
        case .socialHardware:
            stats.scandals += 1
            addLog("Venues are banning your glasses.", .bad)
        }
    }

    private mutating func strengthenRival(named name: String, by amount: Double) {
        if let i = world.rivals.firstIndex(where: { $0.name == name }) {
            world.rivals[i].strength = min(1, world.rivals[i].strength + amount)
        }
    }
}
