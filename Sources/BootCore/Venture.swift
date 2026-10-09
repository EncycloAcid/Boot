import Foundation

public struct FundingRound: Codable, Sendable {
    public var name: String
    public var amount: Double
    public var preMoney: Double
    public var week: Int

    public var postMoney: Double { preMoney + amount }
}

/// Investors who can overrule you, push growth targets, and fire you from your own company.
public struct Board: Codable, Sendable {
    /// 0...1. At zero they replace you.
    public var confidence: Double = 0.7
    /// Quarter-over-quarter revenue growth they expect.
    public var growthTarget: Double = 0.12
    /// Quarters they'll tolerate with no revenue while you build.
    public var graceQuarters = 4
    public var lastReviewWeek = 0
    public var lastGrowth: Double?
    public var demandsMet = 0
    public var demandsDefied = 0
}

public struct Venture: Codable, Sendable {
    public var rounds: [FundingRound] = []
    public var founderEquity: Double = 1
    public var board: Board?

    public var raised: Double { rounds.reduce(0) { $0 + $1.amount } }
    public var isBacked: Bool { !rounds.isEmpty }

    static let roundNames = ["Seed", "Series A", "Series B", "Series C", "Series D", "Series E"]

    mutating func take(_ offer: RoundOffer, week: Int) {
        let dilution = offer.amount / (offer.preMoney + offer.amount)
        founderEquity *= 1 - dilution
        rounds.append(FundingRound(name: offer.name, amount: offer.amount, preMoney: offer.preMoney, week: week))
        if board == nil {
            board = Board(lastReviewWeek: week)
        } else {
            board!.confidence = min(1, board!.confidence + 0.15)
            board!.growthTarget = min(0.25, board!.growthTarget + 0.02)
            board!.graceQuarters = max(board!.graceQuarters, 2)
        }
    }
}

public struct RoundOffer: Sendable {
    public var name: String
    public var amount: Double
    public var preMoney: Double

    public var dilution: Double { amount / (preMoney + amount) }
}

/// Things a nervous board insists on.
public enum BoardDemand: String, Codable, Sendable {
    case layoffs
    case monetize
    case crunch
    case growthHires

    public var title: String {
        switch self {
        case .layoffs: "The board wants a 20% layoff"
        case .monetize: "The board wants you to squeeze harder"
        case .crunch: "The board wants it shipped, now"
        case .growthHires: "The board wants a sales blitz"
        }
    }

    public var body: String {
        switch self {
        case .layoffs: "Growth missed target again and the runway is short. They want headcount cut by a fifth, starting with the weakest performers."
        case .monetize: "They see users, not revenue. They want ad load and monetization turned up across the board."
        case .crunch: "Missed targets mean the next launch has to land sooner. They want the whole company in crunch mode."
        case .growthHires: "They want four marketers hired this week to juice the top line, whatever it does to burn."
        }
    }
}

extension GameState {
    public static let ipoValuation = 500_000_000.0
    public static let ipoAnnualRevenue = 50_000_000.0
    public static let ipoEarliestWeek = 208

    /// What investors would offer right now, or why they won't.
    public func roundOffer() -> Result<RoundOffer, ActionError> {
        guard venture.rounds.count < Venture.roundNames.count else {
            return .failure(.notAvailable("You've raised every round there is. Time to think about going public."))
        }
        if let last = venture.rounds.last, week - last.week < 26 {
            return .failure(.notAvailable("Investors want to see six months of progress since your \(last.name)."))
        }
        if let b = venture.board, b.confidence < 0.35 {
            return .failure(.notAvailable("Your own board won't back a new round while confidence is this low."))
        }
        let name = Venture.roundNames[venture.rounds.count]
        let appetite = world.era.investorAppetite
            * (lanes.contains { world.era.hotLanes.contains($0) } ? 1.3 : 1)
        var pre = enterpriseValue * appetite + max(0, cash)
        if venture.rounds.isEmpty, let heavy = lanes.first(where: \.isCapitalIntensive) {
            pre = max(pre, heavy == .ai ? 15_000_000 : 9_000_000)
        }
        // Angels pay for growth curves, not revenue.
        if let s = social, s.isLive {
            pre = max(pre, s.realUsers * 60 * (1 + 20 * max(0, s.lastGrowth)) * appetite)
        }
        guard pre >= 250_000 else {
            return .failure(.notAvailable("Investors pass. Come back when the company is worth more than \(money(250_000))."))
        }
        return .success(RoundOffer(name: name, amount: (pre / 3).rounded(), preMoney: pre.rounded()))
    }

    /// The board's quarterly verdict.
    mutating func boardReview() {
        guard var b = venture.board, week - b.lastReviewWeek >= GameClock.weeksPerQuarter else { return }
        b.lastReviewWeek = week
        let q = trailingRevenue(weeks: 13)
        let prev = trailingRevenue(weeks: 13, offset: 13)
        var note: String

        if q < 5_000 && b.graceQuarters > 0 {
            b.graceQuarters -= 1
            b.confidence -= stats.releasesThisQuarter > 0 ? -0.03 : 0.04
            note = "The board is patient for now. \(b.graceQuarters) quarter\(b.graceQuarters == 1 ? "" : "s") of grace left."
        } else {
            let growth = prev > 1_000 ? (q - prev) / prev : (q > prev ? 1 : -0.5)
            b.lastGrowth = growth
            if growth >= b.growthTarget {
                b.confidence += 0.08
                note = "Quarterly review: revenue up \(Format.percent(growth)). The board is pleased."
            } else {
                b.confidence -= min(0.25, 0.08 + (b.growthTarget - growth) * 0.3)
                note = "Quarterly review: revenue \(growth >= 0 ? "up" : "down") \(Format.percent(abs(growth))) against a \(Format.percent(b.growthTarget)) target. The board is unhappy."
            }
            if stats.releasesThisQuarter > 0 { b.confidence += 0.04 }
        }
        if runwayWeeks < 13 { b.confidence -= 0.05 }
        b.confidence = b.confidence.clamped(0, 1)
        stats.releasesThisQuarter = 0
        venture.board = b
        addLog(note, b.confidence < 0.4 ? .bad : .neutral)

        if b.confidence <= 0.05 {
            end(.ousted)
        } else if b.confidence < 0.4 {
            enqueue(.boardDemand(pickBoardDemand()))
        }
    }

    private func pickBoardDemand() -> BoardDemand {
        if runwayWeeks < 20 && staff.count >= 5 { return .layoffs }
        if social != nil || apps != nil || crossover(.appsGames)?.built == true { return .monetize }
        if games?.project != nil || hardware?.design != nil { return .crunch }
        return .growthHires
    }

    mutating func applyBoardDemand(_ d: BoardDemand) {
        switch d {
        case .layoffs:
            let cut = max(1, staff.count / 5)
            let leaving = staff.sorted { $0.skill < $1.skill }.prefix(cut).map(\.id)
            staff.removeAll { leaving.contains($0.id) }
            for i in staff.indices { staff[i].morale -= 0.2 }
            reputation.scar(.layoffs, severity: 0.25, week: week, note: "Cut \(cut) jobs at the board's request.")
            stats.layoffs += cut
            addLog("You laid off \(cut) people. The office is quiet.", .bad)
        case .monetize:
            if let s = social { social!.adLoad = max(s.adLoad, 0.8) }
            if apps != nil && apps!.monetization == .paid { apps!.monetization = .ads }
            if let i = crossovers.firstIndex(where: { $0.combo == .appsGames }) {
                crossovers[i].intensity = max(crossovers[i].intensity, 0.9)
            }
            addLog("Monetization turned up on the board's orders.", .neutral)
        case .crunch:
            crunch = true
            addLog("Crunch mode, by order of the board.", .bad)
        case .growthHires:
            for _ in 0..<4 {
                let c = makeCandidate(role: .marketer)
                hireDirect(c)
            }
            addLog("Four new marketers start Monday.", .neutral)
        }
    }
}
