import Foundation

public struct SocialPlatform: Codable, Sendable {
    public var name: String
    public var isLive = false
    public var buildProgress: Double = 0
    public var buildWork: Double { 70 }
    public var users: Double = 0
    public var peakUsers: Double = 0
    /// Hidden product-market fit. Feature work nudges it up.
    public var spark: Double
    /// 0...0.6 of accounts that aren't people.
    public var botShare: Double = 0
    /// 0...1
    public var toxicity: Double = 0.05
    /// 0...1 player slider. Costs money, keeps the place livable.
    public var moderation: Double = 0.3
    /// 0...1 player slider. Revenue now versus growth.
    public var adLoad: Double = 0.2
    public var weeksLive = 0
    public var caughtOn = false
    public var lastGrowth: Double = 0
    public var userHistory: [Double] = []

    init(name: String, spark: Double) {
        self.name = name
        self.spark = spark
    }

    public var realUsers: Double { users * (1 - botShare) }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()
        let work = ctx.workOutput

        if !isLive {
            buildProgress += work
            report.expenses += 200 * ctx.mods.cost
            if buildProgress >= buildWork {
                isLive = true
                users = 1_000 * ctx.reception * ctx.marketDemand
                report.log("\(name) opens its doors. \(count(users)) people sign up in week one.", .milestone)
                report.incidents.append(.stat(.release))
            }
            return report
        }

        weeksLive += 1
        spark = min(1.8, spark + work * 0.0004)

        let modPower = moderation * (0.5 + min(ctx.staffing.operations, 20) * 0.05)
        let cap = 400_000_000.0
        let network = 1 + 4 * users / (users + 30_000)
        let growth = 0.05 * spark * network * max(0, 1 - users / cap) * (1 - adLoad * 0.4)
            * ctx.marketDemand * ctx.reception * ctx.mods.growth * ctx.marketingBoost
        let churn = 0.015 + toxicity * 0.05 + botShare * 0.02
        lastGrowth = growth - churn
        users = max(50, users * (1 + lastGrowth) + ctx.staffing.marketing * 25)
        peakUsers = max(peakUsers, users)
        userHistory.append(users)
        if userHistory.count > 52 { userHistory.removeFirst() }

        if !caughtOn && users > 250_000 {
            caughtOn = true
            report.log("\(name) is catching on. Growth just went vertical.", .milestone)
        }

        let scale = log10(users + 1)
        botShare = (botShare + 0.0025 * scale / 7 * ctx.era.demand(.ai) - modPower * 0.01).clamped(0, 0.6)
        toxicity = (toxicity + 0.004 * scale / 7 + ctx.mods.toxicity - modPower * 0.012).clamped(0, 1)

        let maturity = min(1, users / 2_000_000)
        let advertiserTrust = 1 - toxicity * 0.5
        report.revenue += realUsers * adLoad * 0.02 * maturity * advertiserTrust * ctx.mods.demand
        report.expenses += (100 + users * 0.002 + users * moderation * 0.002) * ctx.mods.cost

        if users > 100_000 && rng.chance(toxicity * 0.03) {
            report.incidents.append(.event(.moderationFire))
        }
        if users > 500_000 && rng.chance(0.004 * scale / 7 * (1.2 - moderation)) {
            report.incidents.append(.event(.pressScandal))
        }
        if botShare > 0.25 && rng.chance(0.02) {
            report.incidents.append(.event(.botWave))
        }
        return report
    }

    var assetValue: Double { realUsers * 2.5 }

    var statusLine: String {
        if !isLive { return "Building \(name): \(Int(min(1, buildProgress / buildWork) * 100))%" }
        return "\(name): \(count(users)) users (\(Format.percent(botShare)) bots), growth \(Format.percent(lastGrowth, digits: 1))/wk"
    }
}
