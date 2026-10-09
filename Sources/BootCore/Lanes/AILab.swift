import Foundation

public enum TrainingSize: String, Codable, CaseIterable, Identifiable, Sendable {
    case small, medium, frontier

    public var id: String { rawValue }

    public var name: String {
        switch self {
        case .small: "Small run"
        case .medium: "Medium run"
        case .frontier: "Frontier run"
        }
    }

    public var weeks: Int {
        switch self {
        case .small: 8
        case .medium: 16
        case .frontier: 30
        }
    }

    /// GPU cluster units rented per week.
    public var computeUnits: Int {
        switch self {
        case .small: 1
        case .medium: 4
        case .frontier: 16
        }
    }

    /// A run this size can't push a model much past this capability. Progress means bigger runs.
    public var ceiling: Double {
        switch self {
        case .small: 30
        case .medium: 65
        case .frontier: 140
        }
    }

    var expectedGain: Double {
        switch self {
        case .small: 4
        case .medium: 9
        case .frontier: 18
        }
    }
}

public enum AIProduct: String, Codable, CaseIterable, Identifiable, Sendable {
    case chatbot, api, enterprise

    public var id: String { rawValue }

    public var name: String {
        switch self {
        case .chatbot: "Consumer chatbot"
        case .api: "Developer API"
        case .enterprise: "Enterprise tools"
        }
    }
}

public struct TrainingRun: Codable, Sendable {
    public var codename: String
    public var size: TrainingSize
    public var weeksDone = 0

    public var completion: Double { Double(weeksDone) / Double(size.weeks) }
}

public struct AILab: Codable, Sendable {
    public var training: TrainingRun?
    /// Capability of the best shipped model. 0 until the first run lands.
    public var capability: Double = 0
    public var modelName: String?
    public var generation = 0
    public var focus: AIProduct = .chatbot
    public var subscribers: Double = 0
    /// Thousands of API calls a week.
    public var apiVolume: Double = 0
    public var enterpriseClients = 0
    public var gpusOwned = 0
    /// 0...1 regulator attention.
    public var regulatoryHeat: Double = 0
    public var failedRuns = 0

    public init() {}

    public static let rentPerUnit = 15_000.0
    public static let gpuUnitPrice = 400_000.0

    func weeklyComputeCost(units: Int, ctx: LaneContext) -> Double {
        let rented = max(0, units - gpusOwned)
        let owned = min(units, gpusOwned)
        return (Double(rented) * Self.rentPerUnit * ctx.era.gpuPrice + Double(owned) * 1_000) * ctx.mods.computeCost
    }

    mutating func startTraining(_ size: TrainingSize, rng: inout SeededRNG) throws {
        guard training == nil else { throw ActionError.busy("A training run is already going.") }
        let name = "\(rng.pick(Names.models))-\(generation + 1)"
        training = TrainingRun(codename: name, size: size)
    }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()

        if var run = training {
            run.weeksDone += 1
            report.expenses += weeklyComputeCost(units: run.size.computeUnits, ctx: ctx)
            training = run
            if run.weeksDone >= run.size.weeks {
                training = nil
                finish(run, ctx: ctx, rng: &rng, report: &report)
            }
        }

        // Owned GPUs draw power whether or not they're training.
        if training == nil { report.expenses += Double(gpusOwned) * 1_000 }

        guard capability > 0 else { return report }
        let reach = ctx.reception * ctx.marketDemand * ctx.marketingBoost
        let c = capability / 60

        // Every product grows toward its potential; the focused one gets the push.
        let focus = self.focus
        let push = { (p: AIProduct) in p == focus ? 1.0 : 0.25 }
        let subTarget = pow(c, 2.5) * 120_000 * reach * push(.chatbot)
        subscribers += (subTarget - subscribers) * 0.08
        let apiTarget = pow(c, 2.7) * 12_000 * reach * push(.api)
        apiVolume += (apiTarget - apiVolume) * 0.10
        let entTarget = pow(c, 2) * (2 + ctx.staffing.marketing * 0.5) * ctx.marketDemand * push(.enterprise)
        if Double(enterpriseClients) < entTarget.rounded(.down) && rng.chance(0.15) {
            enterpriseClients += 1
            report.log("Signed a new enterprise client.", .good)
        } else if Double(enterpriseClients) > entTarget + 1 && rng.chance(0.1) {
            enterpriseClients -= 1
        }

        report.revenue += subscribers * 4.6 + apiVolume * 2.5 + Double(enterpriseClients) * 40_000
        report.expenses += (subscribers * 2.0 + apiVolume * 1.1) * ctx.mods.computeCost
            + Double(enterpriseClients) * 6_000

        let scale = subscribers / 1_000_000 + apiVolume / 100_000 + Double(enterpriseClients) / 40
        regulatoryHeat = (regulatoryHeat + scale * 0.01 - 0.004).clamped(0, 1)
        if regulatoryHeat > 0.5 && rng.chance(0.02 * regulatoryHeat) {
            report.incidents.append(.event(.regulatorInquiry))
        }
        return report
    }

    private mutating func finish(_ run: TrainingRun, ctx: LaneContext, rng: inout SeededRNG, report: inout LaneReport) {
        let researchFactor = (0.5 + ctx.staffing.research * 0.025).clamped(0.5, 2)
        let headroom = max(0.03, 1 - capability / run.size.ceiling)
        let roll = rng.normal(mean: 1, sd: 0.55)
        let gain = run.size.expectedGain * researchFactor * headroom * roll
        if roll < 0.15 || gain < 0.3 {
            failedRuns += 1
            report.log("\(run.codename) finished training and it's... worse. The run was a write-off.", .bad)
            return
        }
        capability += gain
        generation += 1
        modelName = run.codename
        report.incidents.append(.stat(.release))
        let verdict = roll > 1.3 ? "It's far better than anyone expected." : roll < 0.6 ? "It's underwhelming." : "Solid gains."
        report.log("\(run.codename) is done. Capability now \(Int(capability)). \(verdict)", roll < 0.6 ? .neutral : .milestone)
    }

    var assetValue: Double { pow(capability, 2) * 8_000 + Double(gpusOwned) * Self.gpuUnitPrice * 0.6 }

    var statusLine: String {
        var parts: [String] = []
        if let t = training { parts.append("Training \(t.codename) \(Int(t.completion * 100))%") }
        if let m = modelName { parts.append("\(m) live, capability \(Int(capability))") }
        return parts.isEmpty ? "No model yet" : parts.joined(separator: "; ")
    }
}
