#if os(macOS)
import BootCore
import SwiftUI

/*
 * Minimal Strategy surface for macOS.
 *
 * Full regional offices, alliances, and Venture Arm mechanics live in the Android Kotlin core
 * (`StrategyBook` / Company → Strategy). Swift BootCore does not yet model those types, and this
 * branch is Android-focused — wiring a parallel Swift simulation would diverge saves and collide
 * with pre-existing SwiftUI actor-isolation CI failures on `store.perform`.
 *
 * This panel still gives macOS players a Strategy home: market-cycle context from the shared era
 * model, open lanes you could seed on Android, and the research ladder to unlock the Android arm.
 */

struct StrategyView: View {
    var state: GameState

    private var appetite: Double { state.world.era.investorAppetite }

    private var cycleLabel: String {
        switch appetite {
        case 1.25...: return "Hot"
        case 1.05..<1.25: return "Warm"
        case 0.9..<1.05: return "Steady"
        case 0.7..<0.9: return "Cool"
        default: return "Cold"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Strategy").font(.title2.bold())
            Text("Regional offices, alliances, and the Venture Arm ship in the Android build. This panel mirrors the cycle context until Swift core parity lands.")
                .font(.callout)
                .foregroundStyle(.secondary)

            HStack(alignment: .top, spacing: 16) {
                cycleCard
                lanesCard
            }
            HStack(alignment: .top, spacing: 16) {
                ventureArmCard
                researchCard
            }
        }
    }

    private var cycleCard: some View {
        Card("Market cycle") {
            KeyValue(key: "Mood", value: cycleLabel)
            KeyValue(key: "Investor appetite", value: String(format: "%.0f%%", appetite * 100))
            KeyValue(key: "Era", value: state.world.era.name)
            Text("On Android, Venture Arm marks and exits lean on this cycle.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var lanesCard: some View {
        Card("Active lanes") {
            if state.lanes.isEmpty {
                Text("No lanes open yet.").foregroundStyle(.secondary)
            } else {
                ForEach(state.lanes) { lane in
                    HStack {
                        Label(lane.businessName, systemImage: lane.symbol)
                        Spacer()
                        Text("seedable on Android")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            Text("The Venture Arm can seed a minority stake in any open lane, then follow on Seed → Series A.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var ventureArmCard: some View {
        Card("Venture Arm") {
            Text("Android Company → Strategy")
                .font(.headline)
            Text("Seed minority investments with ownership, conviction, basis, and valuation. Follow on after six months. Exit turns marked value into cash.")
                .font(.callout)
            Divider()
            KeyValue(key: "Holdings here", value: "— (Kotlin core)")
            KeyValue(key: "Follow-on", value: "Seed → Series A")
            KeyValue(key: "Exit", value: "Cash from marks")
            Text("Swift BootCore has no StrategyBook yet; play the full arm on Android.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var researchCard: some View {
        Card("Unlock ladder (Android research)") {
            KeyValue(key: "1. Regional expansion", value: "More offices")
            KeyValue(key: "2. Global alliances", value: "2-year deals")
            KeyValue(key: "3. Venture Arm", value: "Seed / exit")
            Text("Research these on Android to open the Strategy page actions.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}
#endif
