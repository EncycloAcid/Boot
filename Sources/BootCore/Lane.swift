import Foundation

/// The six kinds of business you can run. Each one plays like a different management game.
public enum Lane: String, Codable, CodingKeyRepresentable, CaseIterable, Identifiable, Sendable, Comparable {
    case games
    case websites
    case apps
    case social
    case hardware
    case ai

    public var id: String { rawValue }

    public static func < (a: Lane, b: Lane) -> Bool {
        allCases.firstIndex(of: a)! < allCases.firstIndex(of: b)!
    }

    public var name: String {
        switch self {
        case .games: "Games"
        case .websites: "Websites"
        case .apps: "Apps"
        case .social: "Social Media"
        case .hardware: "Hardware"
        case .ai: "AI"
        }
    }

    public var businessName: String {
        switch self {
        case .games: "Game Studio"
        case .websites: "Web Agency"
        case .apps: "App Studio"
        case .social: "Social Platform"
        case .hardware: "Device Lab"
        case .ai: "AI Lab"
        }
    }

    public var pitch: String {
        switch self {
        case .games:
            "Hit-driven and volatile. Long dev cycles, launch-week spikes, review scores, and a fanbase that never forgets a flop."
        case .websites:
            "An agency sim. Steady contract money, clients who change the brief at 11pm, and scope creep. Low risk, low ceiling."
        case .apps:
            "A retention game. Launches are small, then you live or die by churn charts, store rankings, and how often you ship updates."
        case .social:
            "A slow start that turns exponential if it catches on. Server costs outrun revenue early; success brings bots, moderation fires, and press scandals."
        case .hardware:
            "Supply chains and timing. Book factory capacity months ahead and guess how many units to make. Huge margins if you nail it."
        case .ai:
            "Compute and burn rate. Training runs cost a fortune and you don't know how good the model is until it finishes. Highest ceiling, fastest way to go broke."
        }
    }

    public var symbol: String {
        switch self {
        case .games: "gamecontroller"
        case .websites: "globe"
        case .apps: "apps.iphone"
        case .social: "bubble.left.and.bubble.right"
        case .hardware: "laptopcomputer.and.iphone"
        case .ai: "cpu"
        }
    }

    /// Hardware and AI need big money up front. Starting in them means taking venture capital.
    public var isCapitalIntensive: Bool { self == .hardware || self == .ai }

    /// Cash you need on hand to open this lane without raising a round.
    public var selfFundThreshold: Double {
        switch self {
        case .games, .websites, .apps: 15_000
        case .social: 40_000
        case .hardware: 1_500_000
        case .ai: 3_000_000
        }
    }

    /// One-off cost to stand the business up when you add it as a second or third lane.
    public var setupCost: Double {
        switch self {
        case .games: 10_000
        case .websites: 3_000
        case .apps: 8_000
        case .social: 25_000
        case .hardware: 600_000
        case .ai: 1_200_000
        }
    }

    /// Revenue multiple used when valuing the business.
    public var revenueMultiple: Double {
        switch self {
        case .games: 2.0
        case .websites: 1.0
        case .apps: 4.0
        case .social: 6.0
        case .hardware: 3.0
        case .ai: 12.0
        }
    }

    /// How much each role contributes to this lane's output.
    public func weight(for role: Role) -> Double {
        switch (self, role) {
        case (.games, .engineer): 1.0
        case (.games, .designer): 1.2
        case (.games, .researcher): 0.3
        case (.games, .marketer): 0.4
        case (.games, .operator): 0.3

        case (.websites, .engineer): 1.0
        case (.websites, .designer): 1.0
        case (.websites, .researcher): 0.2
        case (.websites, .marketer): 0.5
        case (.websites, .operator): 0.4

        case (.apps, .engineer): 1.2
        case (.apps, .designer): 0.9
        case (.apps, .researcher): 0.3
        case (.apps, .marketer): 0.4
        case (.apps, .operator): 0.4

        case (.social, .engineer): 1.0
        case (.social, .designer): 0.6
        case (.social, .researcher): 0.3
        case (.social, .marketer): 0.5
        case (.social, .operator): 0.8

        case (.hardware, .engineer): 1.3
        case (.hardware, .designer): 1.0
        case (.hardware, .researcher): 0.6
        case (.hardware, .marketer): 0.4
        case (.hardware, .operator): 0.6

        case (.ai, .engineer): 0.7
        case (.ai, .designer): 0.3
        case (.ai, .researcher): 1.5
        case (.ai, .marketer): 0.4
        case (.ai, .operator): 0.4
        }
    }
}

/// How the second (or third) lane is taken on at the Pivot Point.
public enum PivotMode: String, Codable, CaseIterable, Sendable {
    /// Run both lanes with shared staff and cash, attention split between them.
    case expand
    /// Sell the first business, take the lump sum and your skills, go all in on the new one.
    case pivot

    public var name: String {
        switch self {
        case .expand: "Expand"
        case .pivot: "Pivot"
        }
    }

    public var summary: String {
        switch self {
        case .expand: "Run both lanes with shared staff and cash. Your attention is split between them."
        case .pivot: "Sell the first business, take the lump sum and your skills, and go all in on the new one."
        }
    }
}
