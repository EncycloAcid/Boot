import Foundation

/// Deterministic, Codable random source. Every run is reproducible from its seed,
/// which keeps saves, tests and balance runs stable.
public struct SeededRNG: RandomNumberGenerator, Codable, Sendable {
    private var state: UInt64

    public init(seed: UInt64) {
        state = seed &+ 0x9E37_79B9_7F4A_7C15
    }

    // SplitMix64
    public mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }

    /// Uniform in [0, 1).
    public mutating func unit() -> Double {
        Double(next() >> 11) * 0x1.0p-53
    }

    public mutating func chance(_ p: Double) -> Bool {
        unit() < p
    }

    public mutating func range(_ lo: Double, _ hi: Double) -> Double {
        lo + (hi - lo) * unit()
    }

    public mutating func int(_ range: ClosedRange<Int>) -> Int {
        Int.random(in: range, using: &self)
    }

    /// Box-Muller normal sample.
    public mutating func normal(mean: Double = 0, sd: Double = 1) -> Double {
        let u1 = max(unit(), 1e-12)
        let u2 = unit()
        return mean + sd * sqrt(-2 * log(u1)) * cos(2 * .pi * u2)
    }

    public mutating func pick<T>(_ items: [T]) -> T {
        items[Int.random(in: 0..<items.count, using: &self)]
    }

    /// Knuth's Poisson sampler; fine for the small means the sim uses.
    public mutating func poisson(_ mean: Double) -> Int {
        guard mean > 0 else { return 0 }
        let l = exp(-min(mean, 30))
        var k = 0
        var p = 1.0
        repeat {
            k += 1
            p *= unit()
        } while p > l
        return k - 1
    }
}

extension Double {
    func clamped(_ lo: Double, _ hi: Double) -> Double {
        Swift.min(Swift.max(self, lo), hi)
    }

    mutating func nudge(_ delta: Double, within range: ClosedRange<Double> = 0...1) {
        self = (self + delta).clamped(range.lowerBound, range.upperBound)
    }

    /// Guards against NaN/inf leaking into saved state.
    var finite: Double { isFinite ? self : 0 }
}

extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

public enum GameClock {
    public static let weeksPerYear = 52
    public static let weeksPerQuarter = 13

    public static func year(of week: Int) -> Int { week / weeksPerYear + 1 }
    public static func weekOfYear(_ week: Int) -> Int { week % weeksPerYear + 1 }

    public static func label(_ week: Int) -> String {
        "Y\(year(of: week)) W\(weekOfYear(week))"
    }
}
