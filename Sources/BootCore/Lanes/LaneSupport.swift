import Foundation

/// Multipliers applied to a lane this week by eras, combos and the ecosystem bonus.
public struct LaneModifiers: Codable, Sendable {
    public var demand = 1.0
    /// Additive quality points on anything shipped.
    public var quality = 0.0
    public var output = 1.0
    public var cost = 1.0
    /// Multiplier on churn. Below 1 is better.
    public var churn = 1.0
    public var growth = 1.0
    public var computeCost = 1.0
    /// Additive weekly toxicity on a social platform.
    public var toxicity = 0.0

    public init() {}

    mutating func combine(_ o: LaneModifiers) {
        demand *= o.demand
        quality += o.quality
        output *= o.output
        cost *= o.cost
        churn *= o.churn
        growth *= o.growth
        computeCost *= o.computeCost
        toxicity += o.toxicity
    }
}

/// Everything a lane needs to simulate one week.
struct LaneContext {
    var week: Int
    var staffing: Staffing
    var mods: LaneModifiers
    var reception: Double
    var era: Era
    var competition: Double
    var crunch: Bool

    /// Era, rivals and modifiers rolled into one demand number.
    var marketDemand: Double {
        era.demand(lane) * competition * mods.demand
    }

    var lane: Lane

    var workOutput: Double {
        staffing.output * mods.output * (crunch ? 1.4 : 1)
    }

    /// How much of this week's work turns into polish.
    var polishEfficiency: Double { crunch ? 0.6 : 1 }

    var marketingBoost: Double { 1 + 0.03 * staffing.marketing }
}

public enum LogTone: String, Codable, Sendable {
    case neutral, good, bad, milestone
}

public struct LogEntry: Codable, Identifiable, Sendable {
    public var id: Int
    public var week: Int
    public var text: String
    public var tone: LogTone
    public var lane: Lane?
}

/// Side effects a lane reports back to the game.
enum Incident {
    case log(String, LogTone)
    case reputation(Double)
    case scar(ScarKind, Double, String)
    case event(EventKind)
    case stat(StatKey)
}

enum StatKey {
    case hit, flop, recall, scandal, release, soldOut, overstock
}

struct LaneReport {
    var revenue = 0.0
    var expenses = 0.0
    var incidents: [Incident] = []

    mutating func log(_ text: String, _ tone: LogTone = .neutral) {
        incidents.append(.log(text, tone))
    }
}

func shippedQuality(polish: Double, work: Double, averageSkill: Double, bonus: Double) -> Double {
    let craft = 45 * (1 - exp(-1.2 * polish / max(work, 1)))
    return (35 + craft + (averageSkill - 5) * 3 + bonus).clamped(5, 99)
}

func money(_ v: Double) -> String {
    let sign = v < 0 ? "-" : ""
    let a = abs(v)
    switch a {
    case 1_000_000_000...: return "\(sign)$\(String(format: "%.2f", a / 1_000_000_000))B"
    case 1_000_000...: return "\(sign)$\(String(format: "%.2f", a / 1_000_000))M"
    case 10_000...: return "\(sign)$\(String(format: "%.0f", a / 1_000))k"
    case 1_000...: return "\(sign)$\(String(format: "%.1f", a / 1_000))k"
    default: return "\(sign)$\(String(format: "%.0f", a))"
    }
}

func count(_ v: Double) -> String {
    switch v {
    case 1_000_000_000...: return String(format: "%.2fB", v / 1_000_000_000)
    case 1_000_000...: return String(format: "%.2fM", v / 1_000_000)
    case 10_000...: return String(format: "%.0fk", v / 1_000)
    case 1_000...: return String(format: "%.1fk", v / 1_000)
    default: return String(format: "%.0f", v)
    }
}

public enum Format {
    public static func money(_ v: Double) -> String { BootCore.money(v) }
    public static func count(_ v: Double) -> String { BootCore.count(v) }
    public static func percent(_ v: Double, digits: Int = 0) -> String {
        String(format: "%.\(digits)f%%", v * 100)
    }
}
