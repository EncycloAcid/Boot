import Foundation

/// What forces the mid-game choice.
public enum PivotTrigger: String, Codable, CaseIterable, Sendable {
    case buyoutOffer
    case marketCrash
    case burnout
    case viralAccident

    public var name: String {
        switch self {
        case .buyoutOffer: "A buyout offer"
        case .marketCrash: "Your market crashed"
        case .burnout: "Burnout"
        case .viralAccident: "Viral by accident"
        }
    }

    /// Multiplier on what the first business sells for if you pivot.
    var saleMultiplier: Double {
        switch self {
        case .buyoutOffer: 1.3
        case .marketCrash: 0.65
        case .burnout: 0.9
        case .viralAccident: 1.1
        }
    }
}

public struct PivotOffer: Codable, Sendable {
    public var trigger: PivotTrigger
    public var week: Int
    public var headline: String
    public var body: String
    /// What the first business would sell for if you pivot.
    public var salePrice: Double
    public var bidder: String?
    /// The lane the accident points you toward. Picking it comes with a head start.
    public var suggestedLane: Lane?
}

public struct PivotRecord: Codable, Sendable {
    public var trigger: PivotTrigger
    public var week: Int
    public var from: Lane
    public var to: Lane
    public var mode: PivotMode
    public var salePrice: Double?
}

/// A lane opened after the Pivot Point, beyond the second.
public struct ExpansionRecord: Codable, Sendable {
    public var week: Int
    public var lane: Lane
}

extension GameState {
    static let pivotWindow = 78...156

    mutating func checkPivotPoint() {
        guard pivot == nil, pendingPivot == nil, week >= Self.pivotWindow.lowerBound else { return }
        var trigger: PivotTrigger?
        if founder.energy < 30 {
            trigger = .burnout
        } else if world.era.demand(startingLane) <= 0.85 {
            trigger = .marketCrash
        } else if valuation >= 300_000 && rng.chance(0.03) {
            trigger = .buyoutOffer
        } else if rng.chance(0.02) {
            trigger = .viralAccident
        }
        if trigger == nil && week >= Self.pivotWindow.upperBound {
            trigger = valuation >= 150_000 ? .buyoutOffer : .viralAccident
        }
        guard let trigger else { return }
        pendingPivot = makePivotOffer(trigger)
        addLog("The Pivot Point: \(pendingPivot!.headline)", .milestone)
    }

    private mutating func makePivotOffer(_ t: PivotTrigger) -> PivotOffer {
        let lane = startingLane
        let base = laneValue(lane)
        let price = max(base * t.saleMultiplier, 20_000).rounded()
        switch t {
        case .buyoutOffer:
            let bidder = world.rival(in: lane)?.name ?? "MegaCorp Holdings"
            return PivotOffer(
                trigger: t, week: week,
                headline: "\(bidder) wants to buy your \(lane.businessName.lowercased()).",
                body: "\(bidder) has offered \(money(price)) for the \(lane.businessName.lowercased()). You can take the money and go all in on something new, keep it and expand into a second lane, or sell everything and walk away.",
                salePrice: price, bidder: bidder)
        case .marketCrash:
            return PivotOffer(
                trigger: t, week: week,
                headline: "\(world.era.name) has gutted the \(lane.name.lowercased()) market.",
                body: "Demand in your lane has collapsed. The business would only fetch about \(money(price)) right now, but staying put means riding out the slump. Time to pick a second lane.",
                salePrice: price)
        case .burnout:
            return PivotOffer(
                trigger: t, week: week,
                headline: "You can't keep doing this.",
                body: "Your energy is at \(Int(founder.energy)). Something has to change: sell the \(lane.businessName.lowercased()) (about \(money(price))) and start fresh, or expand and hope new work rekindles the spark. Pivoting gives you a second wind; expanding asks for more of what you don't have.",
                salePrice: price)
        case .viralAccident:
            let suggestion: Lane = switch lane {
            case .games, .apps, .ai: .social
            case .websites, .hardware, .social: .apps
            }
            let story = switch lane {
            case .games: "A clip from your game exploded online and the fans need somewhere to gather."
            case .websites: "A little tool you built for a client went viral, and strangers want to download it."
            case .apps: "People started using your app to talk to each other instead of what it was built for."
            case .social: "A mini-app someone built inside your platform is suddenly everywhere."
            case .hardware: "Someone's DIY companion app for your device went viral."
            case .ai: "Screenshots of your chatbot's best answers are all over the internet."
            }
            return PivotOffer(
                trigger: t, week: week,
                headline: "Something went viral by accident.",
                body: "\(story) Lean into \(suggestion.name.lowercased()) and you start with a head start. Or take the attention somewhere else entirely.",
                salePrice: price, suggestedLane: suggestion)
        }
    }

    /// Lanes you can move into at the Pivot Point.
    public var pivotChoices: [Lane] {
        Lane.allCases.filter { !lanes.contains($0) }
    }

    /// Why a choice can't be taken, or nil if it can.
    public func pivotBlocker(lane: Lane, mode: PivotMode, fundWithVenture: Bool) -> String? {
        guard let offer = pendingPivot else { return "There's no decision to make right now." }
        guard !lanes.contains(lane) else { return "You already run that lane." }
        let available = cash + (mode == .pivot ? offer.salePrice : 0) - (mode == .expand ? lane.setupCost : 0)
        if lane.isCapitalIntensive && !fundWithVenture && available < lane.selfFundThreshold {
            return "\(lane.name) needs about \(money(lane.selfFundThreshold)) in the bank. Raise venture money to do it."
        }
        if mode == .expand && !fundWithVenture && cash < lane.setupCost {
            return "Setting up costs \(money(lane.setupCost)); you have \(money(cash))."
        }
        if fundWithVenture, case .failure(let e) = ventureForLane(lane) {
            return e.message
        }
        return nil
    }

    /// The round you would raise to fund a move into `lane`.
    public func ventureForLane(_ lane: Lane) -> Result<RoundOffer, ActionError> {
        switch roundOffer() {
        case .success(var offer):
            let floor = lane == .ai ? 5_000_000.0 : lane == .hardware ? 3_000_000 : 500_000
            if offer.amount < floor {
                offer.amount = floor
                offer.preMoney = max(offer.preMoney, floor * 3)
            }
            return .success(offer)
        case .failure(let e):
            return .failure(e)
        }
    }

    public mutating func resolvePivot(to lane: Lane, mode: PivotMode, fundWithVenture: Bool = false) throws {
        guard let offer = pendingPivot else { throw ActionError.notAvailable("There's no Pivot Point to resolve.") }
        if let blocker = pivotBlocker(lane: lane, mode: mode, fundWithVenture: fundWithVenture) {
            throw ActionError.notAvailable(blocker)
        }
        let from = startingLane
        if fundWithVenture, case .success(let round) = ventureForLane(lane) {
            takeRound(round)
        }

        switch mode {
        case .pivot:
            cash += offer.salePrice
            stats.saleProceeds += offer.salePrice
            // People whose craft carries over follow you; the rest stay with the buyer.
            let followers = staff.filter { $0.lane == from && lane.weight(for: $0.role) >= 0.8 }
            let stayed = staff.filter { $0.lane == from }.count - followers.count
            staff.removeAll { $0.lane == from && lane.weight(for: $0.role) < 0.8 }
            closeLane(from)
            openLane(lane)
            for i in staff.indices where staff[i].lane == from || staff[i].lane == nil {
                staff[i].lane = lane
            }
            if offer.trigger == .burnout { founder.energy = min(100, founder.energy + 40) }
            let detail = followers.isEmpty ? "" : " \(followers.count) of your people came with you."
            addLog("You sold the \(from.businessName.lowercased()) for \(money(offer.salePrice)) and went all in on \(lane.name).\(detail)", .milestone)
            if stayed > 0 { addLog("\(stayed) staff stayed on with the buyer.", .neutral) }
        case .expand:
            spend(lane.setupCost, lane: lane)
            openLane(lane)
            if offer.trigger == .burnout { founder.energy = max(5, founder.energy - 10) }
            addLog("You kept the \(from.businessName.lowercased()) and expanded into \(lane.name). Two lanes, one you.", .milestone)
        }

        if lane == offer.suggestedLane {
            applyViralHeadStart(lane)
        }
        stats.profitableStreak = 0
        pivot = PivotRecord(trigger: offer.trigger, week: week, from: from, to: lane, mode: mode,
                            salePrice: mode == .pivot ? offer.salePrice : nil)
        pendingPivot = nil
    }

    /// Only for a buyout: sell the whole company and walk away.
    public mutating func acceptPivotBuyout() throws {
        guard let offer = pendingPivot, offer.trigger == .buyoutOffer else {
            throw ActionError.notAvailable("Nobody is offering to buy the whole company.")
        }
        let price = max(offer.salePrice, valuation * 1.1)
        pendingPivot = nil
        end(.acquired, price: price, buyer: offer.bidder)
    }

    private mutating func applyViralHeadStart(_ lane: Lane) {
        reputation.adjust(5)
        switch lane {
        case .social: social?.spark += 0.35
        case .apps: apps?.quality += 10
        case .games: games?.fans += 8_000
        case .websites: web?.standing += 0.2
        case .hardware: break
        case .ai: ai?.capability += 5
        }
        addLog("The viral moment gives \(lane.name) a head start.", .good)
    }

    // MARK: Later expansion and the ecosystem

    /// Third lane (or a second after a clean pivot) unlocks once every lane you run is profitable.
    public var expansionBlocker: String? {
        if pivot == nil { return "Expansion opens up after the Pivot Point." }
        if lanes.count >= 3 { return "Three lanes is the limit. You're running an empire already." }
        if pendingPivot != nil { return "Resolve the Pivot Point first." }
        let need = Self.expansionStreakNeeded
        if stats.profitableStreak < need {
            return "Every lane you run has to be profitable for \(need) straight weeks (\(stats.profitableStreak)/\(need))."
        }
        return nil
    }

    public static let expansionStreakNeeded = 12

    public mutating func expand(into lane: Lane, fundWithVenture: Bool = false) throws {
        if let b = expansionBlocker { throw ActionError.notAvailable(b) }
        guard !lanes.contains(lane) else { throw ActionError.invalid("You already run \(lane.name).") }
        if fundWithVenture {
            switch ventureForLane(lane) {
            case .success(let r): takeRound(r)
            case .failure(let e): throw e
            }
        } else if lane.isCapitalIntensive && cash < lane.selfFundThreshold {
            throw ActionError.insufficientFunds("\(lane.name) needs about \(money(lane.selfFundThreshold)) in the bank. Raise venture money to do it.")
        }
        guard cash >= lane.setupCost else {
            throw ActionError.insufficientFunds("Setting up costs \(money(lane.setupCost)).")
        }
        cash -= lane.setupCost
        recordCost(lane.setupCost, lane: lane)
        openLane(lane)
        expansions.append(ExpansionRecord(week: week, lane: lane))
        stats.profitableStreak = 0
        addLog("You expanded into \(lane.name). \(lanes.count) lanes under one roof.", .milestone)
        if isEcosystem {
            addLog("Hardware, software and services together. You're an ecosystem company now. Hold on to it.", .milestone)
        }
    }

    /// The prestige goal: hardware, software and services together.
    public var isEcosystem: Bool {
        lanes.contains(.hardware)
            && (lanes.contains(.apps) || lanes.contains(.games))
            && (lanes.contains(.ai) || lanes.contains(.social) || lanes.contains(.websites))
    }
}
