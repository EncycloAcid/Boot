import Foundation

public enum DeviceCategory: String, Codable, CaseIterable, Identifiable, Sendable {
    case phone, laptop, headphones

    public var id: String { rawValue }

    public var name: String {
        switch self {
        case .phone: "Phone"
        case .laptop: "Laptop"
        case .headphones: "Headphones"
        }
    }

    var designWork: Double {
        switch self {
        case .phone: 260
        case .laptop: 300
        case .headphones: 110
        }
    }

    /// Units a week at launch for a well-reviewed flagship.
    var baseDemand: Double {
        switch self {
        case .phone: 4_000
        case .laptop: 1_600
        case .headphones: 6_000
        }
    }
}

public enum DeviceTier: String, Codable, CaseIterable, Identifiable, Sendable {
    case budget, flagship

    public var id: String { rawValue }
    public var name: String { self == .budget ? "Budget" : "Flagship" }

    var demandMultiplier: Double { self == .budget ? 1.6 : 1 }
    public var weeklyRnD: Double { self == .budget ? 25_000 : 60_000 }
}

public struct DeviceSpec: Codable, Sendable {
    public var category: DeviceCategory
    public var tier: DeviceTier

    public var price: Double {
        switch (category, tier) {
        case (.phone, .budget): 349
        case (.phone, .flagship): 999
        case (.laptop, .budget): 699
        case (.laptop, .flagship): 1_999
        case (.headphones, .budget): 79
        case (.headphones, .flagship): 299
        }
    }

    public var unitCost: Double {
        switch (category, tier) {
        case (.phone, .budget): 210
        case (.phone, .flagship): 420
        case (.laptop, .budget): 480
        case (.laptop, .flagship): 900
        case (.headphones, .budget): 32
        case (.headphones, .flagship): 90
        }
    }
}

public struct DeviceDesign: Codable, Sendable {
    public var name: String
    public var spec: DeviceSpec
    public var progress: Double = 0
    public var polish: Double = 0
    /// Units booked at the factory for this model before launch.
    public var preorderedStock = 0

    public var work: Double { spec.category.designWork }
    public var isReady: Bool { progress >= work }
    public var completion: Double { min(1, progress / work) }
}

public struct Device: Codable, Sendable {
    public var name: String
    public var spec: DeviceSpec
    public var quality: Double
    public var launchWeek: Int
    public var hype: Double
    public var unitsSold = 0
    public var soldOutWeeks = 0
}

public struct ProductionOrder: Codable, Identifiable, Sendable {
    public var id: Int
    public var model: String
    public var units: Int
    public var arrivalWeek: Int
}

public struct HardwareLab: Codable, Sendable {
    public var design: DeviceDesign?
    public var onMarket: Device?
    public var inventory = 0
    public var orders: [ProductionOrder] = []
    public var modelsShipped = 0
    public var lifetimeUnits = 0
    public var lastDemand: Double = 0

    /// 0.2...1. Nobody has heard of your first device. Every unit sold makes the next launch easier.
    public var brandAwareness: Double {
        0.2 + 0.8 * (1 - exp(-Double(lifetimeUnits) / 400_000))
    }
    public var productionHaltedUntil = 0
    private var nextID = 1
    private var usedNames: [String] = []

    public init() {}

    /// Factory lead time. You book capacity months ahead.
    public static let leadTimeWeeks = 12

    mutating func startDesign(_ spec: DeviceSpec, rng: inout SeededRNG) throws {
        guard design == nil else { throw ActionError.busy("A device is already in design.") }
        let base = Names.devices.filter { !usedNames.contains($0) }.randomElement(using: &rng)
            ?? rng.pick(Names.devices)
        usedNames.append(base)
        let suffix = spec.tier == .flagship ? " Pro" : ""
        design = DeviceDesign(name: "\(base) \(spec.category.name)\(suffix)", spec: spec)
    }

    /// Book factory capacity. Paid on order, delivered after the lead time.
    mutating func order(units: Int, week: Int) throws -> (model: String, cost: Double) {
        guard units > 0 else { throw ActionError.invalid("Order at least one unit.") }
        let target = design.map { ($0.name, $0.spec) } ?? onMarket.map { ($0.name, $0.spec) }
        guard let (model, spec) = target else { throw ActionError.invalid("Nothing to manufacture yet.") }
        let arrival = max(week, productionHaltedUntil) + Self.leadTimeWeeks
        orders.append(ProductionOrder(id: nextID, model: model, units: units, arrivalWeek: arrival))
        nextID += 1
        return (model, Double(units) * spec.unitCost)
    }

    mutating func launch(_ ctx: LaneContext, rng: inout SeededRNG) throws -> LaneReport {
        guard let d = design, d.isReady else { throw ActionError.invalid("No finished design to launch.") }
        var report = LaneReport()
        if let old = onMarket, inventory > 0 {
            let clearance = Double(inventory) * old.spec.price * 0.3
            report.revenue += clearance
            report.incidents.append(.stat(.overstock))
            report.log("Stuck with \(inventory) units of the \(old.name). Cleared at 70% off for \(money(clearance)).", .bad)
        }
        let quality = shippedQuality(polish: d.polish, work: d.work,
                                     averageSkill: ctx.staffing.averageSkill, bonus: ctx.mods.quality)
        onMarket = Device(name: d.name, spec: d.spec, quality: quality, launchWeek: ctx.week,
                          hype: 2 + 0.05 * ctx.staffing.marketing)
        inventory = d.preorderedStock
        design = nil
        modelsShipped += 1
        report.incidents.append(.stat(.release))
        report.log("The \(d.name) launches at \(money(d.spec.price)). Reviewers score it \(Int(quality)). \(inventory) units on hand.", .milestone)
        return report
    }

    mutating func tick(_ ctx: LaneContext, rng: inout SeededRNG) -> LaneReport {
        var report = LaneReport()

        if var d = design {
            let work = ctx.workOutput
            let wasReady = d.isReady
            d.progress += work
            d.polish += work * ctx.polishEfficiency
            report.expenses += d.spec.tier.weeklyRnD * ctx.mods.cost
            design = d
            if d.isReady && !wasReady {
                report.log("The \(d.name) design is locked. Make sure stock is booked before you launch.", .good)
            }
        }

        // Deliveries.
        var arrived: [ProductionOrder] = []
        for o in orders where o.arrivalWeek <= ctx.week {
            if ctx.week < productionHaltedUntil { continue }
            arrived.append(o)
            if o.model == onMarket?.name {
                inventory += o.units
            } else if o.model == design?.name {
                design?.preorderedStock += o.units
            }
        }
        orders.removeAll { o in arrived.contains { $0.id == o.id } }

        // Warehousing.
        if let m = onMarket {
            report.expenses += Double(inventory) * m.spec.unitCost * 0.002
        }

        guard var m = onMarket else { return report }
        let age = Double(ctx.week - m.launchWeek)
        let demand = m.spec.category.baseDemand * m.spec.tier.demandMultiplier * pow(m.quality / 70, 2) * brandAwareness
            * ctx.reception * ctx.marketDemand * m.hype * pow(0.97, age) * ctx.marketingBoost
        let sold = min(Int(demand), inventory)
        inventory -= sold
        m.unitsSold += sold
        lifetimeUnits += sold
        m.hype = 1 + (m.hype - 1) * 0.85
        lastDemand = demand
        report.revenue += Double(sold) * m.spec.price

        if Double(sold) < demand * 0.8 && age < 30 {
            m.soldOutWeeks += 1
            if m.soldOutWeeks == 1 {
                report.incidents.append(.stat(.soldOut))
                report.log("The \(m.name) is sold out. Scalpers are listing it at double.", .bad)
                m.hype += 0.1
            }
        }

        // Recalls: the battery issue that costs you a quarter.
        let defectRisk = pow(1 - m.quality / 100, 2) * 0.006
        if m.unitsSold > 1_000 && ctx.week >= productionHaltedUntil && rng.chance(defectRisk) {
            report.incidents.append(.event(.recall(model: m.name, units: m.unitsSold, price: m.spec.price)))
        }
        if !orders.isEmpty && rng.chance(0.012) {
            report.incidents.append(.event(.componentShortage))
        }
        onMarket = m
        return report
    }

    var assetValue: Double {
        Double(inventory) * (onMarket?.spec.unitCost ?? 0) * 0.5 + Double(modelsShipped) * 250_000
    }

    var statusLine: String {
        var parts: [String] = []
        if let d = design { parts.append("Designing \(d.name) \(Int(d.completion * 100))%") }
        if let m = onMarket { parts.append("\(m.name) on sale, \(inventory) in stock") }
        let incoming = orders.reduce(0) { $0 + $1.units }
        if incoming > 0 { parts.append("\(incoming) units inbound") }
        return parts.isEmpty ? "No device in design" : parts.joined(separator: "; ")
    }
}
