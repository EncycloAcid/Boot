import Foundation

public enum Role: String, Codable, CaseIterable, Identifiable, Sendable {
    case engineer
    case designer
    case researcher
    case marketer
    case `operator`

    public var id: String { rawValue }

    public var name: String {
        switch self {
        case .engineer: "Engineer"
        case .designer: "Designer"
        case .researcher: "Researcher"
        case .marketer: "Marketer"
        case .operator: "Operations"
        }
    }

    /// Weekly salary at skill 5, before market adjustments.
    var baseSalary: Double {
        switch self {
        case .engineer: 2_200
        case .designer: 1_900
        case .researcher: 3_400
        case .marketer: 1_700
        case .operator: 1_400
        }
    }
}

public struct Employee: Codable, Identifiable, Hashable, Sendable {
    public var id: Int
    public var name: String
    public var role: Role
    /// 1...10
    public var skill: Int
    /// Weekly.
    public var salary: Double
    /// 0...1. Low morale cuts output and makes people quit.
    public var morale: Double
    public var lane: Lane?
    public var hiredWeek: Int

    var moraleFactor: Double { 0.5 + 0.5 * morale }

    func output(in lane: Lane) -> Double {
        Double(skill) * lane.weight(for: role) * moraleFactor
    }
}

public struct Candidate: Codable, Identifiable, Hashable, Sendable {
    public var id: Int
    public var name: String
    public var role: Role
    public var skill: Int
    public var askingSalary: Double
    /// A rival studio is circling. If you don't hire soon, they're gone.
    public var contestedBy: String?
    public var expiresWeek: Int
}

public struct Founder: Codable, Sendable {
    public var name: String
    /// 0...100. Hits zero and the game ends in burnout.
    public var energy: Double = 100
    /// Weeks of hands-on experience per lane. This is the "skills" you keep when you pivot.
    public var experience: [Lane: Double] = [:]
    /// Share of attention per lane. Normalised when used.
    public var focus: [Lane: Double] = [:]
    /// Taking outside gigs to pay rent. Steady money, but the company gets a fraction of you.
    public var freelancing: Bool = false
    public var vacationWeeksLeft: Int = 0

    public var isOnVacation: Bool { vacationWeeksLeft > 0 }

    /// Weekly income from freelance gigs. Grows with experience.
    public func freelanceIncome(week: Int) -> Double {
        700 + 120 * Double(min(10, week / 52))
    }

    /// Experience bonus caps at +4 output after ~3 years in a lane.
    public func skillBonus(in lane: Lane) -> Double {
        let weeks = experience[lane] ?? 0
        return 4 * (1 - exp(-weeks / 120))
    }

    public func attentionShare(for lane: Lane, among lanes: [Lane]) -> Double {
        guard lanes.contains(lane) else { return 0 }
        let weights = lanes.map { max(focus[$0] ?? 1, 0) }
        let total = weights.reduce(0, +)
        guard total > 0 else { return 1 / Double(lanes.count) }
        return max(focus[lane] ?? 1, 0) / total
    }
}

/// What a lane gets from the people pointed at it this week.
public struct Staffing: Sendable {
    public var output: Double = 0
    public var marketing: Double = 0
    public var operations: Double = 0
    public var research: Double = 0
    public var headcount: Int = 0
    public var averageSkill: Double = 5
}

enum Names {
    static let first = [
        "Ada", "Bo", "Cass", "Dev", "Eli", "Fern", "Gus", "Hana", "Ines", "Jules", "Kai", "Lena",
        "Milo", "Nia", "Omar", "Pia", "Quinn", "Rafa", "Sol", "Tess", "Umi", "Vik", "Wren", "Xan",
        "Yara", "Zed", "Ari", "Bex", "Cy", "Dana", "Emeka", "Femi", "Gio", "Hal", "Ivy", "Jin",
    ]
    static let last = [
        "Okafor", "Lindqvist", "Moreau", "Tanaka", "Reyes", "Novak", "Haddad", "Kowalski", "Osei",
        "Park", "Silva", "Ivanova", "Byrne", "Nakamura", "Petrov", "Mensah", "Dubois", "Castillo",
        "Yilmaz", "Akhtar", "Fischer", "Rossi", "Nguyen", "Adeyemi", "Larsen", "Cohen",
    ]
    static let clients = [
        "Bramble & Co", "Cask Brewery", "Dentally", "Eastside Gym", "Fernhill Florist", "Gourmand Bistro",
        "Harbor Realty", "Inkwell Press", "Juniper Yoga", "Kettle Cafe", "Loom Textiles", "Mason Law",
        "Northwind Travel", "Orchard Clinic", "Pine & Peak Outfitters", "Quarry Climbing", "Riverbend Vet",
        "Saltwater Surf", "Tidy Home Cleaning", "Umber Gallery", "Vela Bikes", "Wick Candles", "Yonder Tours",
        "Zest Juicery", "Acme Logistics", "Bolt Couriers", "Civic Credit Union", "Driftwood Hotels",
    ]
    static let games = [
        "Moss Knight", "Neon Courier", "Tiny Tactics", "Hollow Lantern", "Pocket Kingdom", "Starfold",
        "Rust & Rhyme", "Deep Shift", "Paper Moon Racing", "Last Orchard", "Glitch Garden", "Cinder Saga",
        "Overclocked", "Salt Lake Raiders", "Velvet Dungeon", "Echo Drift", "Bramblewood", "Hyperlane",
        "Iron Picnic", "Quiet Planet", "Crab Empire", "Sky Laundry", "Mythic Commute", "Boss Rush Hour",
    ]
    static let apps = [
        "Tally", "Nudge", "Sprout", "Pocketlog", "Habitat", "Driftnote", "Snapcart", "Quill", "Loop",
        "Mealwise", "Stride", "Budgie", "Focusly", "Glimmer", "Plotter", "Waypoint",
    ]
    static let socials = ["Chirp", "Hive", "Murmur", "Campfire", "Loopd", "Echo", "Plaza", "Huddle"]
    static let devices = ["Arc", "Pebble", "Halo", "Nova", "Slate", "Ember", "Prism", "Orbit", "Volt", "Aura"]
    static let models = ["Sage", "Atlas", "Muse", "Lumen", "Oracle", "Scribe", "Echo", "Vector"]
}
