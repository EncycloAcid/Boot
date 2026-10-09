import Foundation

/// Every pairing of two lanes unlocks a crossover product only that combination gets,
/// along with a problem unique to it.
public enum Combo: String, Codable, CaseIterable, Identifiable, Sendable {
    case gamesSocial
    case websitesApps
    case appsGames
    case hardwareApps
    case hardwareAI
    case aiSocial
    case aiGames
    case gamesWebsites
    case gamesHardware
    case websitesSocial
    case websitesHardware
    case websitesAI
    case appsSocial
    case appsAI
    case socialHardware

    public var id: String { rawValue }

    public var lanes: (Lane, Lane) {
        switch self {
        case .gamesSocial: (.games, .social)
        case .websitesApps: (.websites, .apps)
        case .appsGames: (.apps, .games)
        case .hardwareApps: (.hardware, .apps)
        case .hardwareAI: (.hardware, .ai)
        case .aiSocial: (.ai, .social)
        case .aiGames: (.ai, .games)
        case .gamesWebsites: (.games, .websites)
        case .gamesHardware: (.games, .hardware)
        case .websitesSocial: (.websites, .social)
        case .websitesHardware: (.websites, .hardware)
        case .websitesAI: (.websites, .ai)
        case .appsSocial: (.apps, .social)
        case .appsAI: (.apps, .ai)
        case .socialHardware: (.social, .hardware)
        }
    }

    public static func between(_ a: Lane, _ b: Lane) -> Combo? {
        allCases.first { ($0.lanes.0 == a && $0.lanes.1 == b) || ($0.lanes.0 == b && $0.lanes.1 == a) }
    }

    public static func available(for lanes: [Lane]) -> [Combo] {
        allCases.filter { lanes.contains($0.lanes.0) && lanes.contains($0.lanes.1) }
    }

    public var product: String {
        switch self {
        case .gamesSocial: "Trending Hub"
        case .websitesApps: "Client Tools Suite"
        case .appsGames: "Free-to-Play Mobile"
        case .hardwareApps: "Walled Garden"
        case .hardwareAI: "Custom Silicon"
        case .aiSocial: "The Algorithm"
        case .aiGames: "Living NPCs"
        case .gamesWebsites: "Co-Dev Studio"
        case .gamesHardware: "Handheld Console"
        case .websitesSocial: "Creator Storefronts"
        case .websitesHardware: "Smart Retail Installs"
        case .websitesAI: "Autopilot Agency"
        case .appsSocial: "Super App"
        case .appsAI: "Assistant App"
        case .socialHardware: "Smart Glasses"
        }
    }

    public var pitch: String {
        switch self {
        case .gamesSocial: "Your platform becomes where your games trend. Launches ride the feed."
        case .websitesApps: "Turn the tools you built for clients into your own software product."
        case .appsGames: "Mobile money is huge. How hard you squeeze players is up to you."
        case .hardwareApps: "Your apps run best on your devices. Customers get locked in."
        case .hardwareAI: "Design your own chips. No more GPU shortage, and your devices get smarter."
        case .aiSocial: "A recommendation engine that makes the feed irresistible."
        case .aiGames: "NPCs that actually talk back. A huge selling point."
        case .gamesWebsites: "Your agency takes on game marketing sites and co-development work at premium rates."
        case .gamesHardware: "Your own handheld. Your games sell devices; your devices sell games."
        case .websitesSocial: "Sell storefront sites to the creators on your platform."
        case .websitesHardware: "Bundle your devices into kiosk and retail installs for agency clients."
        case .websitesAI: "Let the model build the sites. Margins soar."
        case .appsSocial: "Log in with your social account, share everything, never leave."
        case .appsAI: "An assistant baked into your app. People stop leaving."
        case .socialHardware: "Glasses that post what you see. Social, hands-free."
        }
    }

    public var problem: String {
        switch self {
        case .gamesSocial: "Your gamer audience floods the site and clashes with everyone else."
        case .websitesApps: "Your clients notice you're now competing with them."
        case .appsGames: "Squeeze players too hard and your reputation tanks across every lane."
        case .hardwareApps: "Too much lock-in brings antitrust lawsuits."
        case .hardwareAI: "The chip R&D bill is brutal."
        case .aiSocial: "Engagement soars, and so do the congressional hearings."
        case .aiGames: "Sooner or later a player gets an NPC to say something that goes viral for the wrong reasons."
        case .gamesWebsites: "Agency deadlines collide with launch crunch."
        case .gamesHardware: "Fans resent the exclusives."
        case .websitesSocial: "Creators accuse you of rigging the platform for paying customers."
        case .websitesHardware: "Every install is a support ticket waiting to happen."
        case .websitesAI: "You're automating your own staff, and they know it."
        case .appsSocial: "All that sharing is one leak away from a privacy scandal."
        case .appsAI: "Inference costs scale with every user."
        case .socialHardware: "People don't love being recorded by strangers' glasses."
        }
    }

    /// Headline when the problem lands.
    public var headline: String {
        switch self {
        case .gamesSocial: "Gamers have flooded the platform"
        case .websitesApps: "Your clients noticed"
        case .appsGames: "Players say you're squeezing them"
        case .hardwareApps: "Antitrust lawsuit filed"
        case .hardwareAI: "The chip program is over budget"
        case .aiSocial: "Called to testify about the algorithm"
        case .aiGames: "An NPC said what?"
        case .gamesWebsites: "Deadlines collide"
        case .gamesHardware: "Exclusivity backlash"
        case .websitesSocial: "Creators cry favoritism"
        case .websitesHardware: "Install support is drowning"
        case .websitesAI: "The team knows what Autopilot means"
        case .appsSocial: "Data-sharing leak"
        case .appsAI: "Inference bill shock"
        case .socialHardware: "Your glasses are getting banned"
        }
    }

    /// What the intensity dial controls for this combo.
    public var dial: String {
        switch self {
        case .gamesSocial: "Cross-promotion"
        case .websitesApps: "Product push"
        case .appsGames: "Monetization pressure"
        case .hardwareApps: "Lock-in"
        case .hardwareAI: "Chip program scale"
        case .aiSocial: "Algorithm aggressiveness"
        case .aiGames: "NPC freedom"
        case .gamesWebsites: "Co-dev workload"
        case .gamesHardware: "Exclusivity"
        case .websitesSocial: "Storefront promotion"
        case .websitesHardware: "Install volume"
        case .websitesAI: "Automation"
        case .appsSocial: "Data sharing"
        case .appsAI: "Assistant reach"
        case .socialHardware: "Always-on capture"
        }
    }

    public var buildCost: Double {
        switch self {
        case .hardwareAI: 2_000_000
        case .gamesHardware, .socialHardware, .hardwareApps: 600_000
        case .aiSocial, .aiGames, .appsAI, .websitesAI: 250_000
        default: 60_000
        }
    }

    public var buildWeeks: Int {
        switch self {
        case .hardwareAI: 26
        case .gamesHardware, .socialHardware: 20
        case .hardwareApps, .aiSocial, .aiGames, .appsAI: 12
        default: 8
        }
    }

    /// Scar left if you ride out this combo's problem instead of fixing it.
    var scar: ScarKind? {
        switch self {
        case .appsGames: .predatoryMonetization
        case .hardwareApps: .antitrust
        case .aiSocial, .aiGames, .gamesSocial, .websitesSocial: .scandal
        case .appsSocial, .socialHardware: .privacy
        case .websitesAI: .layoffs
        default: nil
        }
    }
}

public struct Crossover: Codable, Identifiable, Sendable {
    public var combo: Combo
    public var built = false
    public var buildWeeksLeft: Int
    /// 0...1, player controlled. More upside, more trouble.
    public var intensity: Double = 0.5
    /// Builds toward 1, then the combo's problem fires.
    public var pressure: Double = 0
    public var incidents = 0

    public var id: Combo { combo }
}

/// What the active combos do to the company this week.
struct ComboEffects {
    var mods: [Lane: LaneModifiers] = [:]
    var revenue = 0.0
    var cost = 0.0
    var energyDrain = 0.0
    var moraleDrain = 0.0
    var webWariness = 0.0

    mutating func mod(_ lane: Lane, _ change: (inout LaneModifiers) -> Void) {
        var m = mods[lane] ?? LaneModifiers()
        change(&m)
        mods[lane] = m
    }
}

extension Crossover {
    /// Weekly effect of a built crossover at its current intensity.
    func apply(to fx: inout ComboEffects, state: GameState) {
        guard built else { return }
        let i = intensity
        switch combo {
        case .gamesSocial:
            let reach = min(1, (state.social?.realUsers ?? 0) / 5_000_000)
            fx.mod(.games) { $0.demand *= 1 + i * (0.2 + 0.8 * reach) }
            fx.mod(.social) { $0.growth *= 1 + 0.25 * i; $0.toxicity += 0.004 * i }
        case .websitesApps:
            let clients = Double(state.web?.completed ?? 0)
            fx.revenue += clients * 60 * i
            fx.mod(.apps) { $0.demand *= 1 + 0.3 * i }
            fx.webWariness = 0.6 * i
        case .appsGames:
            fx.mod(.apps) { $0.demand *= 1 + 0.4 * i; $0.churn *= 1 + 0.4 * i * i }
            fx.mod(.games) { $0.demand *= 1 + 0.6 * i }
            fx.revenue += ((state.apps?.users ?? 0) * 0.05 + (state.games?.fans ?? 0) * 0.4) * i * (1 + i)
        case .hardwareApps:
            fx.mod(.hardware) { $0.demand *= 1 + 0.4 * i }
            fx.mod(.apps) { $0.churn *= 1 - 0.35 * i }
        case .hardwareAI:
            fx.mod(.ai) { $0.computeCost *= 1 - 0.6 * i }
            fx.mod(.hardware) { $0.quality += 10 * i; $0.demand *= 1 + 0.15 * i }
            fx.cost += 150_000 * i
        case .aiSocial:
            fx.mod(.social) { $0.growth *= 1 + 0.8 * i; $0.demand *= 1 + 0.3 * i; $0.toxicity += 0.003 * i }
        case .aiGames:
            fx.mod(.games) { $0.quality += 8 * i; $0.demand *= 1 + 0.2 * i }
        case .gamesWebsites:
            fx.mod(.websites) { $0.demand *= 1 + 0.5 * i }
            fx.energyDrain += 1.0 * i
        case .gamesHardware:
            let fans = min(1, (state.games?.fans ?? 0) / 200_000)
            fx.mod(.hardware) { $0.demand *= 1 + i * (0.1 + 0.4 * fans) }
            fx.mod(.games) { $0.demand *= 1 + 0.3 * i }
        case .websitesSocial:
            fx.revenue += (state.social?.realUsers ?? 0) * 0.0006 * i
            fx.mod(.websites) { $0.demand *= 1 + 0.2 * i }
        case .websitesHardware:
            fx.mod(.websites) { $0.demand *= 1 + 0.6 * i }
            fx.mod(.hardware) { $0.demand *= 1 + 0.1 * i }
            fx.cost += 4_000 * i
        case .websitesAI:
            fx.mod(.websites) { $0.output *= 1 + 1.2 * i }
            fx.moraleDrain += 0.012 * i
        case .appsSocial:
            let reach = min(1, (state.social?.realUsers ?? 0) / 10_000_000)
            fx.mod(.apps) { $0.demand *= 1 + i * (0.2 + 0.6 * reach) }
            fx.mod(.social) { $0.growth *= 1 + 0.15 * i }
        case .appsAI:
            fx.mod(.apps) { $0.churn *= 1 - 0.35 * i; $0.quality += 4 * i }
            fx.cost += (state.apps?.users ?? 0) * 0.01 * i
        case .socialHardware:
            fx.mod(.hardware) { $0.demand *= 1 + 0.3 * i }
            fx.mod(.social) { $0.growth *= 1 + 0.2 * i }
        }
    }

    /// Weekly pressure toward this combo's problem.
    func pressureRate(state: GameState) -> Double {
        let base = 0.004
        // Gamers flood your platform whether or not you built the hub. Other problems need the crossover.
        guard built else { return combo == .gamesSocial ? base : 0 }
        let i = intensity
        switch combo {
        case .appsGames: return base + 0.06 * pow(i, 3)
        case .hardwareApps: return base + 0.02 * i * i
        case .aiSocial: return base + 0.03 * i * min(1, (state.social?.users ?? 0) / 20_000_000 + 0.2)
        case .hardwareAI: return base + 0.006 * i
        case .websitesAI: return base + 0.03 * i
        default: return base + 0.02 * i * i
        }
    }
}
