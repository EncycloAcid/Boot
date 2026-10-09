import Foundation

/// Parody industry eras that boost or weaken different lanes.
public enum Era: String, Codable, CaseIterable, Sendable {
    case bootstrapYears
    case aiGoldRush
    case vrHypeCycle
    case cryptoWinter
    case remoteWorkBoom
    case privacyPanic
    case creatorEconomy
    case retroRevival
    case metaverseHangover

    public var name: String {
        switch self {
        case .bootstrapYears: "The Bootstrap Years"
        case .aiGoldRush: "The AI Gold Rush"
        case .vrHypeCycle: "The VR Hype Cycle"
        case .cryptoWinter: "Crypto Winter"
        case .remoteWorkBoom: "The Remote Work Boom"
        case .privacyPanic: "The Privacy Panic"
        case .creatorEconomy: "The Creator Economy"
        case .retroRevival: "The Retro Revival"
        case .metaverseHangover: "The Metaverse Hangover"
        }
    }

    public var blurb: String {
        switch self {
        case .bootstrapYears: "Nobody's watching yet. Rent is due."
        case .aiGoldRush: "Every pitch deck says 'agentic'. GPUs cost more than houses. Clients want chatbots on their bakery sites."
        case .vrHypeCycle: "Headsets are the future, again. Gamers and gadget buyers are throwing money around."
        case .cryptoWinter: "The coins crashed. Investors are skittish, ad budgets shrank, and miners are dumping cheap GPUs."
        case .remoteWorkBoom: "Everyone needs a website, a productivity app and a better laptop."
        case .privacyPanic: "Regulators and journalists are hunting for data scandals. Social and AI feel the heat."
        case .creatorEconomy: "Everyone's a creator. Platforms and storefronts boom."
        case .retroRevival: "Pixel art is back. Small games sell; shiny gadgets less so."
        case .metaverseHangover: "Remember the metaverse? Investors do, unfortunately."
        }
    }

    /// Demand multiplier per lane.
    public func demand(_ lane: Lane) -> Double {
        switch (self, lane) {
        case (.bootstrapYears, _): 1.0

        case (.aiGoldRush, .ai): 1.7
        case (.aiGoldRush, .apps): 1.1
        case (.aiGoldRush, .websites): 0.9
        case (.aiGoldRush, _): 1.0

        case (.vrHypeCycle, .hardware): 1.35
        case (.vrHypeCycle, .games): 1.2
        case (.vrHypeCycle, _): 1.0

        case (.cryptoWinter, .social): 0.8
        case (.cryptoWinter, .apps): 0.85
        case (.cryptoWinter, .ai): 0.9
        case (.cryptoWinter, _): 0.95

        case (.remoteWorkBoom, .websites): 1.3
        case (.remoteWorkBoom, .apps): 1.2
        case (.remoteWorkBoom, .hardware): 1.2
        case (.remoteWorkBoom, _): 1.0

        case (.privacyPanic, .social): 0.75
        case (.privacyPanic, .ai): 0.85
        case (.privacyPanic, .hardware): 1.1
        case (.privacyPanic, _): 1.0

        case (.creatorEconomy, .social): 1.35
        case (.creatorEconomy, .websites): 1.1
        case (.creatorEconomy, .games): 1.1
        case (.creatorEconomy, _): 1.0

        case (.retroRevival, .games): 1.3
        case (.retroRevival, .hardware): 0.9
        case (.retroRevival, _): 1.0

        case (.metaverseHangover, .hardware): 0.8
        case (.metaverseHangover, .social): 0.9
        case (.metaverseHangover, .ai): 1.1
        case (.metaverseHangover, _): 0.95
        }
    }

    /// Multiplier on what GPUs cost an AI lab.
    public var gpuPrice: Double {
        switch self {
        case .aiGoldRush: 1.8
        case .cryptoWinter: 0.6
        case .vrHypeCycle: 1.1
        default: 1.0
        }
    }

    /// How willing investors are to write cheques.
    public var investorAppetite: Double {
        switch self {
        case .aiGoldRush: 1.5
        case .vrHypeCycle: 1.2
        case .cryptoWinter: 0.6
        case .metaverseHangover: 0.7
        case .privacyPanic: 0.9
        default: 1.0
        }
    }

    /// Lanes investors are especially hot on this era.
    public var hotLanes: [Lane] {
        Lane.allCases.filter { demand($0) >= 1.2 }
    }

    public var coldLanes: [Lane] {
        Lane.allCases.filter { demand($0) <= 0.85 }
    }
}

public struct Rival: Codable, Identifiable, Sendable {
    public var id: Int
    public var name: String
    public var lane: Lane
    /// 0...1 market muscle in their lane.
    public var strength: Double
    /// 0...1 how often they poach and bid for talent.
    public var aggression: Double
    public var lastMove: String?
}

public struct World: Codable, Sendable {
    public var era: Era = .bootstrapYears
    public var eraStartWeek = 0
    public var eraLength = 104
    public var pastEras: [Era] = []
    public var rivals: [Rival] = []

    static func makeRivals() -> [Rival] {
        [
            Rival(id: 1, name: "Gigaplay Interactive", lane: .games, strength: 0.5, aggression: 0.5),
            Rival(id: 2, name: "Pixelsmith Collective", lane: .websites, strength: 0.4, aggression: 0.3),
            Rival(id: 3, name: "Appetite Inc", lane: .apps, strength: 0.5, aggression: 0.4),
            Rival(id: 4, name: "Fritter", lane: .social, strength: 0.6, aggression: 0.5),
            Rival(id: 5, name: "Pear Computing", lane: .hardware, strength: 0.7, aggression: 0.4),
            Rival(id: 6, name: "Hyperion Labs", lane: .ai, strength: 0.6, aggression: 0.8),
        ]
    }

    /// Demand multiplier from rival competition in a lane.
    public func competition(in lane: Lane) -> Double {
        let s = rivals.filter { $0.lane == lane }.map(\.strength).max() ?? 0
        return 1 - 0.2 * s
    }

    public func rival(in lane: Lane) -> Rival? {
        rivals.first { $0.lane == lane }
    }
}
