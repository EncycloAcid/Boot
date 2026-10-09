import Foundation

/// A lasting mark on your name. Scars follow you into every lane: ship a predatory mobile
/// game and your social app launches to suspicion.
public enum ScarKind: String, Codable, CaseIterable, Sendable {
    case predatoryMonetization
    case flop
    case privacy
    case scandal
    case recall
    case antitrust
    case layoffs

    public var name: String {
        switch self {
        case .predatoryMonetization: "Predatory monetization"
        case .flop: "Shipped a flop"
        case .privacy: "Privacy breach"
        case .scandal: "Press scandal"
        case .recall: "Product recall"
        case .antitrust: "Antitrust case"
        case .layoffs: "Mass layoffs"
        }
    }

    /// Weekly decay of severity. Players never quite forget some things.
    var decay: Double {
        switch self {
        case .predatoryMonetization: 0.002
        case .flop: 0.006
        case .privacy: 0.004
        case .scandal: 0.008
        case .recall: 0.005
        case .antitrust: 0.003
        case .layoffs: 0.006
        }
    }

    /// How strongly this scar makes a lane's audience suspicious, 0...1.
    func suspicion(for lane: Lane) -> Double {
        switch (self, lane) {
        case (.predatoryMonetization, .games), (.predatoryMonetization, .apps): 0.7
        case (.predatoryMonetization, .social): 0.6
        case (.predatoryMonetization, _): 0.25
        case (.flop, .games): 0.6
        case (.flop, _): 0.1
        case (.privacy, .social), (.privacy, .ai), (.privacy, .hardware): 0.7
        case (.privacy, _): 0.3
        case (.scandal, .social), (.scandal, .ai): 0.5
        case (.scandal, _): 0.25
        case (.recall, .hardware): 0.8
        case (.recall, _): 0.15
        case (.antitrust, _): 0.2
        case (.layoffs, _): 0.15
        }
    }
}

public struct Scar: Codable, Identifiable, Sendable {
    public var id: Int
    public var kind: ScarKind
    /// 0...1
    public var severity: Double
    public var week: Int
    public var note: String
}

public struct Reputation: Codable, Sendable {
    /// 0...100, shared across every lane.
    public var score: Double = 50
    public var scars: [Scar] = []
    private var nextScarID = 1

    public init() {}

    /// 0.6...1.4 general demand multiplier.
    public var factor: Double { 0.6 + 0.8 * score / 100 }

    /// Multiplier on a launch in `lane`, after old scars are taken into account.
    public func launchReception(for lane: Lane) -> Double {
        let drag = scars.reduce(0) { $0 + $1.severity * $1.kind.suspicion(for: lane) }
        return factor * max(0.35, 1 - drag * 0.6)
    }

    public func worstScar(for lane: Lane) -> Scar? {
        scars.max { $0.severity * $0.kind.suspicion(for: lane) < $1.severity * $1.kind.suspicion(for: lane) }
    }

    public func has(_ kind: ScarKind, atLeast severity: Double = 0.2) -> Bool {
        scars.contains { $0.kind == kind && $0.severity >= severity }
    }

    mutating func adjust(_ delta: Double) {
        score = (score + delta).clamped(0, 100)
    }

    mutating func scar(_ kind: ScarKind, severity: Double, week: Int, note: String) {
        if let i = scars.firstIndex(where: { $0.kind == kind }) {
            scars[i].severity = min(1, scars[i].severity + severity)
            scars[i].week = week
            scars[i].note = note
        } else {
            scars.append(Scar(id: nextScarID, kind: kind, severity: min(1, severity), week: week, note: note))
            nextScarID += 1
        }
        adjust(-severity * 18)
    }

    mutating func weeklyDrift() {
        // Reputation slowly regresses toward 50; scars heal at their own pace.
        score += (50 - score) * 0.004
        for i in scars.indices {
            scars[i].severity -= scars[i].kind.decay
        }
        scars.removeAll { $0.severity <= 0.02 }
    }
}
