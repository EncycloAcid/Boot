import Foundation

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

    /// Cash cost to ship a day-one patch for a released game of this scope.
    public var patchCost: Double { weeklyBudget * 4 }
}

/// Snapshot of how a project in development is likely to land if launched now.
public struct GameLaunchForecast: Sendable {
    public var expectedReview: Double
    /// Rough critical range (±1 SD of the review roll).
    public var reviewLow: Int
    public var reviewHigh: Int
    public var expectedFirstWeekSales: Double
    /// Nil when already content-complete.
    public var weeksToReady: Int?
    /// polish / required work. Above ~1.4, further weeks buy little.
    public var polishRatio: Double

    public var polishPastSweetSpot: Bool { polishRatio >= 1.4 }
}

public struct GameProject: Codable, Sendable {
    public var title: String
    public var scope: GameScope
    public var progress: Double = 0
    public var polish: Double = 0
    public var weeksInDev = 0

    public var isReady: Bool { progress >= scope.work }
    public var completion: Double { min(1, progress / scope.work) }

    /// Whole weeks of current output needed to hit content-complete. Nil if ready or idle.
    public func weeksRemaining(workOutput: Double) -> Int? {
        guard !isReady else { return nil }
        guard workOutput > 0.01 else { return nil }
        return Int(ceil((scope.work - progress) / workOutput))
    }
}

public struct GameRelease: Codable, Identifiable, Sendable {
    public var id: Int
    public var title: String
    public var scope: GameScope
    public var week: Int
    public var review: Int
    public var weeklySales: Double
    public var lifetimeSales: Double
    /// Day-one patch already shipped. Optional in saves older than this field.
    public var patched: Bool

    public init(id: Int, title: String, scope: GameScope, week: Int, review: Int,
                weeklySales: Double, lifetimeSales: Double, patched: Bool = false) {
        self.id = id
        self.title = title
        self.scope = scope
        self.week = week
        self.review = review
        self.weeklySales = weeklySales
        self.lifetimeSales = lifetimeSales
        self.patched = patched
    }

    public var verdict: String {
        switch review {
        case 90...: "Masterpiece"
        case 85..<90: "Hit"
        case 70..<85: "Solid"
        case 55..<70: "Mixed"
        default: "Flop"
        }
    }

    enum CodingKeys: String, CodingKey {
        case id, title, scope, week, review, weeklySales, lifetimeSales, patched
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        title = try c.decode(String.self, forKey: .title)
        scope = try c.decode(GameScope.self, forKey: .scope)
        week = try c.decode(Int.self, forKey: .week)
        review = try c.decode(Int.self, forKey: .review)
        weeklySales = try c.decode(Double.self, forKey: .weeklySales)
        lifetimeSales = try c.decode(Double.self, forKey: .lifetimeSales)
        patched = try c.decodeIfPresent(Bool.self, forKey: .patched) ?? false
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

    /// Weeks after launch during which a day-one patch can still move the needle.
    public static let patchWindowWeeks = 12

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

    /// Expected review and launch-week sales if you shipped the current project today.
    func forecast(ctx: LaneContext) -> GameLaunchForecast? {
        guard let p = project else { return nil }
        return forecast(for: p, ctx: ctx)
    }

    func forecast(for p: GameProject, ctx: LaneContext) -> GameLaunchForecast {
        let quality = shippedQuality(polish: p.polish, work: p.scope.work,
                                     averageSkill: ctx.staffing.averageSkill, bonus: ctx.mods.quality)
        let fanLift = 1 + 1.5 * fans / (fans + p.scope.fanScale)
        let sales = p.scope.launchSales * pow(quality / 70, 3) * fanLift * fanTrust
            * ctx.reception * ctx.marketDemand * ctx.marketingBoost
        return GameLaunchForecast(
            expectedReview: quality,
            reviewLow: Int((quality - 9).clamped(5, 98).rounded()),
            reviewHigh: Int((quality + 9).clamped(5, 98).rounded()),
            expectedFirstWeekSales: sales,
            weeksToReady: p.weeksRemaining(workOutput: ctx.workOutput),
            polishRatio: p.polish / max(p.scope.work, 1)
        )
    }

    public func canPatch(_ release: GameRelease, atWeek week: Int) -> Bool {
        !release.patched
            && release.review < 92
            && week - release.week <= Self.patchWindowWeeks
            && release.weeklySales > 0
    }

    public func patchCost(for release: GameRelease) -> Double { release.scope.patchCost }

    /// Launches the current project. Returns what happened for the log.
    mutating func launch(_ ctx: LaneContext, rng: inout SeededRNG) throws -> LaneReport {
        guard let p = project else { throw ActionError.invalid("No game in development.") }
        guard p.isReady else { throw ActionError.invalid("\(p.title) isn't finished yet.") }
        var report = LaneReport()
        let quality = shippedQuality(polish: p.polish, work: p.scope.work,
                                     averageSkill: ctx.staffing.averageSkill, bonus: ctx.mods.quality)
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

    /// Ship a day-one patch: spend cash, bump the score, and refresh fading sales.
    mutating func patch(releaseID: Int, ctx: LaneContext, rng: inout SeededRNG) throws -> LaneReport {
        guard let i = releases.firstIndex(where: { $0.id == releaseID }) else {
            throw ActionError.invalid("No release with that id.")
        }
        let release = releases[i]
        guard canPatch(release, atWeek: ctx.week) else {
            if release.patched {
                throw ActionError.invalid("\(release.title) already got its day-one patch.")
            }
            if release.review >= 92 {
                throw ActionError.invalid("Critics already love \(release.title).")
            }
            if ctx.week - release.week > Self.patchWindowWeeks {
                throw ActionError.invalid("The patch window for \(release.title) has closed.")
            }
            throw ActionError.invalid("\(release.title) isn't selling enough to patch.")
        }

        var report = LaneReport()
        let cost = patchCost(for: release)
        report.expenses += cost

        let skillBoost = (ctx.staffing.averageSkill - 5) * 0.7
        let meanBump = 4.5 + skillBoost + (release.review < 55 ? 1.5 : 0)
        let bump = Int(rng.normal(mean: meanBump, sd: 1.8).clamped(2, 11).rounded())
        let oldReview = release.review
        let newReview = min(98, oldReview + bump)
        releases[i].review = newReview
        releases[i].patched = true

        // Sales wake up with the score: flops recover harder than near-hits.
        let lift = pow(Double(newReview) / Double(max(oldReview, 1)), 1.8)
        releases[i].weeklySales *= min(2.2, max(1.08, lift))

        if oldReview < 85 && newReview >= 85 {
            hits += 1
            if oldReview < 55 { flops = max(0, flops - 1) }
            fanTrust = min(1.2, fanTrust + 0.08)
            fans += release.scope.fanScale * 0.08
            report.incidents.append(.stat(.hit))
            report.incidents.append(.reputation(2))
            report.log("The \(release.title) patch lands. Reviews climb \(oldReview) → \(newReview). It's a late hit.", .milestone)
        } else if oldReview < 55 && newReview >= 55 {
            fanTrust = min(1.2, fanTrust + 0.05)
            fans += release.scope.fanScale * 0.03
            report.log("The \(release.title) patch steadies the ship. Reviews \(oldReview) → \(newReview).", .good)
        } else {
            report.log("Day-one patch for \(release.title): reviews \(oldReview) → \(newReview). Sales tick up.", .good)
        }
        return report
    }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()
        if var p = project {
            let work = ctx.workOutput
            let wasReady = p.isReady
            p.progress += work
            p.polish += work * ctx.polishEfficiency
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
