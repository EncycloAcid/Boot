import Foundation

public enum AppMonetization: String, Codable, CaseIterable, Sendable {
    case ads, subscription, paid

    public var name: String {
        switch self {
        case .ads: "Free with ads"
        case .subscription: "Subscription"
        case .paid: "Paid upfront"
        }
    }

    var baseChurn: Double {
        switch self {
        case .ads: 0.05
        case .subscription: 0.07
        case .paid: 0.03
        }
    }

    /// Weekly revenue per active user.
    var arpu: Double {
        switch self {
        case .ads: 0.03
        case .subscription: 0.08
        case .paid: 0
        }
    }

    var installAppeal: Double {
        switch self {
        case .ads: 1
        case .subscription: 0.8
        case .paid: 0.35
        }
    }
}

public enum UpdateStyle: String, Codable, CaseIterable, Sendable {
    /// Ship little and often. Keeps the app fresh.
    case small
    /// Big quarterly drops. More quality per update, longer stale stretches.
    case big

    public var name: String {
        switch self {
        case .small: "Small, frequent updates"
        case .big: "Big, rare updates"
        }
    }

    public var work: Double { self == .small ? 15 : 45 }
    var qualityGain: Double { self == .small ? 1.5 : 5.5 }
}

public struct AppStudio: Codable, Sendable {
    public var name: String
    public var isLive = false
    public var buildProgress: Double = 0
    public var buildWork: Double { 50 }
    public var users: Double = 0
    public var peakUsers: Double = 0
    /// 0...100
    public var quality: Double = 50
    public var rank: Double = 450
    public var weeksSinceUpdate = 0
    public var updateProgress: Double = 0
    public var updatesShipped = 0
    public var monetization: AppMonetization = .ads
    public var updateStyle: UpdateStyle = .small
    public var lastChurn: Double = 0
    public var lastInstalls: Double = 0
    /// Last 26 weeks of churn, for the chart everyone stares at.
    public var churnHistory: [Double] = []
    public var userHistory: [Double] = []

    init(name: String, monetization: AppMonetization) {
        self.name = name
        self.monetization = monetization
    }

    public var rankLabel: String { isLive ? "#\(Int(rank.rounded()))" : "Unreleased" }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()
        let work = ctx.workOutput

        if !isLive {
            buildProgress += work
            report.expenses += 120 * ctx.mods.cost
            if buildProgress >= buildWork {
                isLive = true
                quality = shippedQuality(polish: buildProgress * ctx.polishEfficiency, work: buildWork,
                                         averageSkill: ctx.staffing.averageSkill, bonus: ctx.mods.quality)
                users = 1_500 * ctx.reception * ctx.marketDemand
                report.log("\(name) is live on the store. \(count(users)) installs in launch week.", .milestone)
                report.incidents.append(.stat(.release))
            }
            return report
        }

        updateProgress += work
        weeksSinceUpdate += 1
        if updateProgress >= updateStyle.work {
            updateProgress -= updateStyle.work
            weeksSinceUpdate = 0
            updatesShipped += 1
            quality = min(97, quality + updateStyle.qualityGain * (1 - quality / 110) + ctx.mods.quality * 0.05)
            if updateStyle == .big {
                report.log("\(name) \(updatesShipped + 1).0 ships. Reviews perk up.", .good)
            }
        }

        let stale = 0.003 * Double(max(0, weeksSinceUpdate - 4))
        let churn = ((monetization.baseChurn * (1.6 - quality / 100) + stale) * ctx.mods.churn).clamped(0.006, 0.3)
        let organic = 200_000 / max(rank, 1)
        let appeal = pow(quality / 60, 2) * monetization.installAppeal
        let installs = (organic + ctx.staffing.marketing * 80) * appeal * ctx.reception * ctx.marketDemand

        users = max(0, users * (1 - churn) + installs)
        peakUsers = max(peakUsers, users)
        lastChurn = churn
        lastInstalls = installs

        // Store rank chases install velocity, tempered by churn.
        let score = installs * max(0.2, 1.2 - churn * 4)
        let target = (500 / pow(1 + score / 400, 0.6)).clamped(1, 500)
        rank += (target - rank) * 0.35

        churnHistory.append(churn)
        userHistory.append(users)
        if churnHistory.count > 26 { churnHistory.removeFirst() }
        if userHistory.count > 52 { userHistory.removeFirst() }

        report.revenue += users * monetization.arpu
        if monetization == .paid { report.revenue += installs * 2.99 * 0.7 }
        report.expenses += (users * 0.004 + 150) * ctx.mods.cost

        if rank < 10 && rng.chance(0.05) {
            report.log("\(name) is sitting at #\(Int(rank)) in the store.", .good)
        }
        return report
    }

    var assetValue: Double { users * 1.5 }

    var statusLine: String {
        if !isLive { return "Building \(name): \(Int(min(1, buildProgress / buildWork) * 100))%" }
        return "\(name): \(count(users)) users, \(rankLabel), churn \(Format.percent(lastChurn, digits: 1))/wk"
    }
}
