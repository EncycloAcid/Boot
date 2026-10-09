import Foundation

/// There's no single win condition. The ending reflects how you got there.
public enum EndingKind: String, Codable, CaseIterable, Sendable {
    case ipo
    case acquired
    case indie
    case burnout
    case bankrupt
    case ousted

    public var name: String {
        switch self {
        case .ipo: "IPO"
        case .acquired: "Acquired"
        case .indie: "Stayed Independent"
        case .burnout: "Burnout"
        case .bankrupt: "Bankrupt"
        case .ousted: "Ousted"
        }
    }
}

public struct EndingReport: Codable, Sendable {
    public var kind: EndingKind
    public var title: String
    public var summary: String
    public var story: [String]
    public var week: Int
    public var valuation: Double
    public var founderTakeHome: Double
    public var ecosystem: Bool
}

public struct Stats: Codable, Sendable {
    public var peakValuation: Double = 0
    public var peakHeadcount = 0
    public var peakCash: Double = 0
    public var totalRevenue: Double = 0
    public var hits = 0
    public var flops = 0
    public var releases = 0
    public var releasesThisQuarter = 0
    public var recalls = 0
    public var scandals = 0
    public var hearings = 0
    public var soldOuts = 0
    public var overstocks = 0
    public var layoffs = 0
    public var buyoutsDeclined = 0
    public var saleProceeds: Double = 0
    public var weeksInRed = 0
    /// Consecutive weeks every lane has been profitable over the trailing quarter.
    public var profitableStreak = 0
    /// Weeks spent as a profitable hardware + software + services company.
    public var ecosystemWeeks = 0
    public var ecosystemPressure: Double = 0
    public var ecosystemOpened = false
    public var vacations = 0
    public var crunchWeeks = 0
    public var warnedBurnout = false
    public var erasSeen: [Era] = [.bootstrapYears]
}

extension GameState {
    /// IPO-ready, or the reason you aren't.
    public var ipoBlocker: String? {
        if week < Self.ipoEarliestWeek { return "Bankers won't take you public before year \(Self.ipoEarliestWeek / 52 + 1)." }
        if enterpriseValue < Self.ipoValuation {
            return "Bankers want the business valued at \(money(Self.ipoValuation)), not counting cash. You're at \(money(enterpriseValue))."
        }
        let annual = trailingRevenue(weeks: 52)
        if annual < Self.ipoAnnualRevenue {
            return "You need \(money(Self.ipoAnnualRevenue)) in annual revenue. You're at \(money(annual))."
        }
        if let b = venture.board, b.confidence < 0.4 { return "The board won't sign off on an IPO right now." }
        return nil
    }

    public mutating func goPublic() throws {
        if let b = ipoBlocker { throw ActionError.notAvailable(b) }
        end(.ipo)
    }

    mutating func end(_ kind: EndingKind, price: Double? = nil, buyer: String? = nil) {
        guard ending == nil else { return }
        let value = price ?? valuation
        stats.peakValuation = max(stats.peakValuation, valuation)
        let takeHome: Double = switch kind {
        case .ipo: value * venture.founderEquity
        case .acquired: value * venture.founderEquity
        case .indie: valuation * venture.founderEquity
        case .burnout: max(0, valuation * venture.founderEquity * 0.5)
        case .bankrupt: 0
        case .ousted: valuation * venture.founderEquity * 0.6
        }
        ending = EndingReport(
            kind: kind, title: endingTitle(kind, buyer: buyer), summary: endingSummary(kind, value: value, buyer: buyer),
            story: endingStory(), week: week, valuation: value, founderTakeHome: takeHome,
            ecosystem: stats.ecosystemWeeks >= 52)
        addLog("THE END: \(ending!.title)", .milestone)
    }

    private var isPredatory: Bool { reputation.has(.predatoryMonetization, atLeast: 0.15) }

    private func endingTitle(_ kind: EndingKind, buyer: String?) -> String {
        let titan = stats.ecosystemWeeks >= 52
        switch kind {
        case .ipo:
            if titan { return "The Ecosystem IPO" }
            if isPredatory { return "IPO, Whale-Funded" }
            if lanes.contains(.ai) { return "The Compute IPO" }
            if venture.rounds.isEmpty { return "Bootstrapped to the Bell" }
            return "Ring the Bell"
        case .acquired:
            if pivot == nil { return "The Quick Exit" }
            if let buyer, world.rivals.contains(where: { $0.name == buyer }) { return "Absorbed by \(buyer)" }
            return "Acquired"
        case .indie:
            if venture.isBacked {
                return valuation < venture.raised ? "Zombie Unicorn" : "Private and Thriving"
            }
            if staff.count <= 3 { return "The Lifestyle Business" }
            return titan ? "The Independent Empire" : "Still Indie"
        case .burnout:
            return valuation > 1_000_000 ? "The Company Outlived You" : "Burnout"
        case .bankrupt:
            return lanes.contains(.ai) ? "Burned the Compute Budget" : "Out of Runway"
        case .ousted:
            return "Fired From Your Own Company"
        }
    }

    private func endingSummary(_ kind: EndingKind, value: Double, buyer: String?) -> String {
        let stake = Format.percent(venture.founderEquity)
        switch kind {
        case .ipo:
            return "\(companyName) went public at a \(money(value)) valuation. Your \(stake) stake is worth \(money(value * venture.founderEquity))."
        case .acquired:
            return "\(buyer ?? "A buyer") acquired \(companyName) for \(money(value)). You walk away with \(money(value * venture.founderEquity))."
        case .indie:
            return "Fifteen years in, \(companyName) is still yours\(venture.isBacked ? ", more or less" : ""). Worth \(money(valuation)) and answering to \(venture.board == nil ? "nobody" : "a board")."
        case .burnout:
            return "Your energy hit zero. You stepped away from \(companyName) with nothing left to give."
        case .bankrupt:
            return "\(companyName) couldn't make payroll for two months straight. The lights went out in \(GameClock.label(week))."
        case .ousted:
            return "The board lost confidence and replaced you as CEO of the company you started."
        }
    }

    private func endingStory() -> [String] {
        var story: [String] = []
        var origin = "You started \(companyName) in a cramped apartment as a one-person \(startingLane.businessName.lowercased())"
        if startingLane.isCapitalIntensive { origin += ", with a seed cheque and a board from day one" }
        story.append(origin + ".")

        if let p = pivot {
            let why: String = switch p.trigger {
            case .buyoutOffer: "a buyout offer"
            case .marketCrash: "a market crash"
            case .burnout: "burnout"
            case .viralAccident: "an accidental viral hit"
            }
            let how = p.mode == .pivot
                ? "sold the \(p.from.businessName.lowercased())\(p.salePrice.map { " for \(money($0))" } ?? "") and went all in on \(p.to.name)"
                : "kept it and expanded into \(p.to.name)"
            story.append("In year \(GameClock.year(of: p.week)), \(why) forced the question. You \(how).")
        } else {
            story.append("You never reached the Pivot Point.")
        }
        for e in expansions {
            story.append("In year \(GameClock.year(of: e.week)) you added \(e.lane.name).")
        }
        if stats.ecosystemWeeks >= 52 {
            story.append("For \(stats.ecosystemWeeks / 52) year\(stats.ecosystemWeeks >= 104 ? "s" : "") you ran a true ecosystem: hardware, software and services under one roof.")
        } else if isEcosystem {
            story.append("You built an ecosystem company, but never held it long enough to make it count.")
        }

        let built = crossovers.filter(\.built).map(\.combo.product)
        if !built.isEmpty {
            story.append("Crossovers: \(built.joined(separator: ", ")).")
        }

        var record: [String] = []
        if stats.releases > 0 { record.append("\(stats.releases) launches") }
        if stats.hits > 0 { record.append("\(stats.hits) hit\(stats.hits == 1 ? "" : "s")") }
        if stats.flops > 0 { record.append("\(stats.flops) flop\(stats.flops == 1 ? "" : "s")") }
        if stats.recalls > 0 { record.append("\(stats.recalls) recall\(stats.recalls == 1 ? "" : "s")") }
        if stats.scandals > 0 { record.append("\(stats.scandals) scandal\(stats.scandals == 1 ? "" : "s")") }
        if stats.hearings > 0 { record.append("\(stats.hearings) hearing\(stats.hearings == 1 ? "" : "s") and lawsuits") }
        if !record.isEmpty { story.append("The record: \(record.joined(separator: ", ")).") }

        let scars = reputation.scars.filter { $0.severity >= 0.15 }.sorted { $0.severity > $1.severity }
        if scars.isEmpty {
            story.append("Your name stayed clean. Reputation finished at \(Int(reputation.score)).")
        } else {
            story.append("People still remember: \(scars.map { $0.kind.name.lowercased() }.joined(separator: ", ")).")
        }
        if isPredatory {
            story.append("Somewhere, a whale is still paying for a loot box you designed.")
        }

        story.append("Peak valuation \(money(stats.peakValuation)). Peak headcount \(stats.peakHeadcount). Lifetime revenue \(money(stats.totalRevenue)).")
        if venture.isBacked {
            story.append("You raised \(money(venture.raised)) over \(venture.rounds.count) round\(venture.rounds.count == 1 ? "" : "s") and kept \(Format.percent(venture.founderEquity)) of the company.")
        } else {
            story.append("You never took a cent of outside money.")
        }
        story.append("You lived through \(stats.erasSeen.map(\.name).joined(separator: ", ")).")
        return story
    }
}
