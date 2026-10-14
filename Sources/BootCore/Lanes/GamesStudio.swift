import Foundation

/// A weekly creative direction for the current game. Each choice trades speed against shipped quality.
public enum GameFocus: String, Codable, CaseIterable, Identifiable, Sendable {
    case balanced, gameplay, visuals, performance, content

    public var id: String { rawValue }

    public var name: String {
        switch self {
        case .balanced: "Balanced"
        case .gameplay: "Gameplay first"
        case .visuals: "Visual polish"
        case .performance: "Performance first"
        case .content: "More content"
        }
    }

    public var description: String {
        switch self {
        case .balanced: "Steady progress and polish."
        case .gameplay: "Slower production, stronger core experience."
        case .visuals: "More polish, slower content progress."
        case .performance: "A safer, smoother release with a small pace cost."
        case .content: "Ship content faster, but leave less time for polish."
        }
    }

    var workMultiplier: Double {
        switch self {
        case .balanced: 1
        case .gameplay: 0.94
        case .visuals: 0.90
        case .performance: 0.96
        case .content: 1.14
        }
    }

    var polishMultiplier: Double {
        switch self {
        case .balanced: 1
        case .gameplay: 1.08
        case .visuals: 1.28
        case .performance: 1.12
        case .content: 0.72
        }
    }

    var qualityBonus: Double {
        switch self {
        case .balanced: 0
        case .gameplay: 4
        case .visuals: 3
        case .performance: 3
        case .content: -3
        }
    }
}

public enum GameScope: String, Codable, CaseIterable, Identifiable, Sendable {
    case small, medium, large

    public var id: String { rawValue }

    public var name: String {
        switch self {
        case .small: "Small indie"
        case .medium: "Mid-size"
        case .large: "Blockbuster"
        }
    }

    /// Work points to finish.
    public var work: Double {
        switch self {
        case .small: 60
        case .medium: 600
        case .large: 4_000
        }
    }

    /// Art, licences and marketing while in development.
    public var weeklyBudget: Double {
        switch self {
        case .small: 150
        case .medium: 3_000
        case .large: 25_000
        }
    }

    var launchSales: Double {
        switch self {
        case .small: 25_000
        case .medium: 150_000
        case .large: 1_100_000
        }
    }

    var price: Double {
        switch self {
        case .small: 15
        case .medium: 25
        case .large: 60
        }
    }

    var fanScale: Double {
        switch self {
        case .small: 5_000
        case .medium: 40_000
        case .large: 250_000
        }
    }
}

public struct GameProject: Codable, Sendable {
    public var title: String
    public var scope: GameScope
    public var progress: Double = 0
    public var polish: Double = 0
    /// Optional to keep older saved games decodable; nil behaves as balanced.
    public var focus: GameFocus? = .balanced
    public var weeksInDev = 0

    public var activeFocus: GameFocus { focus ?? .balanced }
    public var isReady: Bool { progress >= scope.work }
    public var completion: Double { min(1, progress / scope.work) }
}

public struct GameRelease: Codable, Identifiable, Sendable {
    public var id: Int
    public var title: String
    public var scope: GameScope
    public var week: Int
    public var review: Int
    public var weeklySales: Double
    public var lifetimeSales: Double

    public var verdict: String {
        switch review {
        case 90...: "Masterpiece"
        case 85..<90: "Hit"
        case 70..<85: "Solid"
        case 55..<70: "Mixed"
        default: "Flop"
        }
    }
}

public struct GamesStudio: Codable, Sendable {
    public var project: GameProject?
    public var releases: [GameRelease] = []
    public var fans: Double = 0
    /// 0.3...1.2. Flops knock it down and only hits bring it back. The fanbase never forgets.
    public var fanTrust: Double = 1
    public var hits = 0
    public var flops = 0
    private var nextID = 1
    private var usedTitles: [String] = []

    public init() {}

    public var weeklySales: Double { releases.reduce(0) { $0 + $1.weeklySales } }

    mutating func startProject(scope: GameScope, rng: inout SeededRNG) throws {
        guard project == nil else { throw ActionError.busy("A game is already in development.") }
        let title = Names.games.filter { !usedTitles.contains($0) }.randomElement(using: &rng)
            ?? "Untitled \(releases.count + 1)"
        usedTitles.append(title)
        project = GameProject(title: title, scope: scope)
    }

    mutating func cancelProject() {
        project = nil
    }

    /// Launches the current project. Returns what happened for the log.
    mutating func launch(_ ctx: LaneContext, rng: inout SeededRNG) throws -> LaneReport {
        guard let p = project else { throw ActionError.invalid("No game in development.") }
        guard p.isReady else { throw ActionError.invalid("\(p.title) isn't finished yet.") }
        var report = LaneReport()
        let quality = shippedQuality(polish: p.polish, work: p.scope.work,
                                     averageSkill: ctx.staffing.averageSkill, bonus: ctx.mods.quality + p.activeFocus.qualityBonus)
        let review = Int(rng.normal(mean: quality, sd: 9).clamped(5, 98).rounded())
        let fanLift = 1 + 1.5 * fans / (fans + p.scope.fanScale)
        let firstWeek = p.scope.launchSales * pow(Double(review) / 70, 3) * fanLift * fanTrust
            * ctx.reception * ctx.marketDemand * ctx.marketingBoost

        let release = GameRelease(id: nextID, title: p.title, scope: p.scope, week: ctx.week,
                                  review: review, weeklySales: firstWeek, lifetimeSales: 0)
        nextID += 1
        releases.insert(release, at: 0)
        project = nil
        report.incidents.append(.stat(.release))

        let buyers = firstWeek / p.scope.price
        switch review {
        case 85...:
            hits += 1
            fanTrust = min(1.2, fanTrust + 0.05)
            fans += buyers * 0.35
            report.incidents.append(.stat(.hit))
            report.incidents.append(.reputation(4))
            report.log("\(p.title) launches to a \(review) review score. Launch week: \(money(firstWeek)). It's a hit.", .milestone)
        case 55..<85:
            fans += buyers * 0.15
            report.log("\(p.title) launches with a \(review). Launch week: \(money(firstWeek)).", .good)
        default:
            flops += 1
            fanTrust = max(0.3, fanTrust - 0.2)
            fans *= 0.85
            report.incidents.append(.stat(.flop))
            let severity = p.scope == .small ? 0.15 : 0.35
            report.incidents.append(.scar(.flop, severity, "\(p.title) flopped with a \(review)."))
            report.log("\(p.title) flops with a \(review). The fans will remember this one.", .bad)
        }
        return report
    }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()
        if var p = project {
            let work = ctx.workOutput
            let wasReady = p.isReady
            p.progress += work * p.activeFocus.workMultiplier
            p.polish += work * ctx.polishEfficiency * p.activeFocus.polishMultiplier
            p.weeksInDev += 1
            project = p
            report.expenses += p.scope.weeklyBudget * ctx.mods.cost
            if p.isReady && !wasReady {
                report.log("\(p.title) is content complete. Launch it now, or keep polishing.", .good)
            }
        }
        for i in releases.indices {
            let age = ctx.week - releases[i].week
            report.revenue += releases[i].weeklySales
            releases[i].lifetimeSales += releases[i].weeklySales
            releases[i].weeklySales *= age < 6 ? 0.75 : 0.95
            if releases[i].weeklySales < 40 { releases[i].weeklySales = 0 }
        }
        return report
    }

    /// Back catalogue and fans count toward valuation.
    var assetValue: Double { fans * 4 * fanTrust }

    var statusLine: String {
        if let p = project {
            return "\(p.title) (\(p.scope.name)) \(Int(p.completion * 100))%"
                + (p.isReady ? ", ready to launch" : "")
        }
        return releases.isEmpty ? "No game in development" : "Between projects"
    }
}
