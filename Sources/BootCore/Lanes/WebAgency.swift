import Foundation

public enum ScopePolicy: String, Codable, CaseIterable, Sendable {
    /// Eat the extra work. Clients love you; margins don't.
    case absorb
    /// Send a change order. Paid, but clients bristle and some walk.
    case billChanges

    public var name: String {
        switch self {
        case .absorb: "Absorb scope creep"
        case .billChanges: "Bill for changes"
        }
    }
}

public struct Contract: Codable, Identifiable, Sendable {
    public var id: Int
    public var client: String
    public var value: Double
    public var work: Double
    public var done: Double = 0
    public var weeksAllowed: Int
    public var weeksElapsed = 0
    /// 0...1
    public var satisfaction: Double = 0.7
    public var changeOrders: Double = 0
    public var creepCount = 0
    public var expiresWeek: Int

    public var completion: Double { min(1, done / max(work, 1)) }
    public var isOverdue: Bool { weeksElapsed > weeksAllowed }
}

public struct WebAgency: Codable, Sendable {
    public var offers: [Contract] = []
    public var active: [Contract] = []
    /// 0...1 word of mouth. Bigger, better clients come to agencies with standing.
    public var standing: Double = 0.25
    public var policy: ScopePolicy = .absorb
    public var completed = 0
    /// Clients pull back once they notice you sell competing software (Web + Apps).
    public var clientWariness: Double = 0
    private var nextID = 1

    public init() {}

    static let ratePerPoint = 400.0

    mutating func accept(_ id: Int, week: Int) throws -> Double {
        guard let i = offers.firstIndex(where: { $0.id == id }) else {
            throw ActionError.invalid("That offer is no longer on the table.")
        }
        var c = offers.remove(at: i)
        c.weeksElapsed = 0
        active.append(c)
        return c.value * 0.4
    }

    mutating func decline(_ id: Int) {
        offers.removeAll { $0.id == id }
    }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()
        offers.removeAll { $0.expiresWeek < ctx.week }

        // New briefs.
        let mean = (0.5 + standing * 1.5 + ctx.staffing.marketing * 0.06) * ctx.marketDemand * (1 - clientWariness * 0.6)
        for _ in 0..<min(rng.poisson(mean), 3) where offers.count < 5 {
            let size = (1 + standing * 2) * (1 + 0.4 * log2(1 + ctx.staffing.output / 6))
            let value = min(rng.range(2_500, 8_000) * size * ctx.mods.demand, 60_000)
            let work = value / Self.ratePerPoint * rng.range(0.8, 1.2)
            let pace = max(3, ctx.staffing.output * 0.8)
            offers.append(Contract(
                id: nextID, client: rng.pick(Names.clients), value: value.rounded(), work: work,
                weeksAllowed: Int((work / pace).rounded(.up)) + 2, expiresWeek: ctx.week + 2))
            nextID += 1
        }

        report.expenses += (80 + 40 * Double(active.count)) * ctx.mods.cost
        guard !active.isEmpty else { return report }

        // Work is spread evenly; overcommit and everything slips.
        let share = ctx.workOutput / Double(active.count)
        var finished: [Contract] = []
        for i in active.indices {
            active[i].done += share
            active[i].weeksElapsed += 1
            if active[i].isOverdue { active[i].satisfaction -= 0.05 }

            // The 11pm email.
            if rng.chance(0.10) {
                let extra = active[i].work * rng.range(0.15, 0.4)
                active[i].work += extra
                active[i].creepCount += 1
                switch policy {
                case .absorb:
                    active[i].satisfaction += 0.02
                    active[i].weeksAllowed += 1
                case .billChanges:
                    active[i].changeOrders += extra * Self.ratePerPoint * 0.8
                    active[i].satisfaction -= 0.12
                    active[i].weeksAllowed += Int((extra / max(share, 1)).rounded(.up))
                }
                if rng.chance(0.3) {
                    report.log("\(active[i].client) changed the brief at 11pm. Again.")
                }
            }
            active[i].satisfaction = active[i].satisfaction.clamped(0, 1)

            if policy == .billChanges && active[i].satisfaction < 0.35 && rng.chance(0.08) {
                report.log("\(active[i].client) walked away mid-project over the change orders.", .bad)
                standing = max(0, standing - 0.04)
                active[i].work = -1
                continue
            }
            if active[i].done >= active[i].work { finished.append(active[i]) }
        }

        for c in finished where c.work > 0 {
            let withheld = c.satisfaction < 0.3 ? 0.7 : 1
            report.revenue += (c.value * 0.6 + c.changeOrders) * withheld
            standing = (standing + (c.satisfaction - 0.5) * 0.08).clamped(0, 1)
            completed += 1
            if c.satisfaction > 0.8 && rng.chance(0.4) {
                report.log("\(c.client) loved the site and is telling everyone.", .good)
            }
        }
        active.removeAll { c in c.work < 0 || finished.contains { $0.id == c.id } }
        return report
    }

    var assetValue: Double { standing * 150_000 + Double(completed) * 2_000 }

    var statusLine: String {
        "\(active.count) active, \(offers.count) offers, standing \(Int(standing * 100))"
    }
}
