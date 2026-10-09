#if os(macOS)
import BootCore
import SwiftUI

struct EventSheet: View {
    @Environment(GameStore.self) private var store

    var body: some View {
        if let e = store.state?.events.first {
            VStack(alignment: .leading, spacing: 14) {
                if let lane = e.lane {
                    Label(lane.businessName, systemImage: lane.symbol).font(.caption).foregroundStyle(.secondary)
                }
                Text(e.title).font(.title2.bold())
                Text(e.body).fixedSize(horizontal: false, vertical: true)
                ForEach(Array(e.choices.enumerated()), id: \.offset) { i, choice in
                    Button {
                        store.perform { try $0.choose(i) }
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(choice.label).font(.headline)
                            Text(choice.detail).font(.caption).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(8)
                    }
                    .buttonStyle(.bordered)
                }
            }
            .padding(24)
            .frame(width: 480)
            .interactiveDismissDisabled()
        }
    }
}

struct PivotSheet: View {
    @Environment(GameStore.self) private var store
    @State private var lane: Lane?
    @State private var mode: PivotMode = .expand
    @State private var useVenture = false

    var body: some View {
        if let state = store.state, let offer = state.pendingPivot {
            VStack(alignment: .leading, spacing: 14) {
                Text("THE PIVOT POINT · \(offer.trigger.name.uppercased())")
                    .font(.caption.bold()).foregroundStyle(.purple)
                Text(offer.headline).font(.title2.bold())
                Text(offer.body).fixedSize(horizontal: false, vertical: true)

                Text("Pick a second lane").font(.headline)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 200), spacing: 10)], spacing: 10) {
                    ForEach(state.pivotChoices) { l in
                        Button {
                            lane = l
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Image(systemName: l.symbol)
                                    Text(l.name).bold()
                                    Spacer()
                                    if l == offer.suggestedLane { Text("head start").font(.caption).foregroundStyle(.green) }
                                    if l.isCapitalIntensive { Text("VC").font(.caption).foregroundStyle(.orange) }
                                }
                                if let combo = Combo.between(state.startingLane, l) {
                                    Text("Unlocks \(combo.product)").font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(RoundedRectangle(cornerRadius: 8)
                                .strokeBorder(lane == l ? Color.accentColor : Color.primary.opacity(0.1), lineWidth: lane == l ? 2 : 1))
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }

                Picker("How", selection: $mode) {
                    ForEach(PivotMode.allCases, id: \.self) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)
                Text(explanation(state: state, offer: offer))
                    .font(.callout).foregroundStyle(.secondary)
                Toggle("Fund it with venture money", isOn: $useVenture)

                if let lane, let blocker = state.pivotBlocker(lane: lane, mode: mode, fundWithVenture: useVenture) {
                    Label(blocker, systemImage: "xmark.circle").foregroundStyle(.red)
                }

                HStack {
                    if offer.trigger == .buyoutOffer {
                        Button("Sell the whole company and walk away", role: .destructive) {
                            store.perform { try $0.acceptPivotBuyout() }
                        }
                    }
                    Spacer()
                    Button("\(mode.name) into \(lane?.name ?? "…")") {
                        if let lane {
                            store.perform { try $0.resolvePivot(to: lane, mode: mode, fundWithVenture: useVenture) }
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(lane == nil || lane.flatMap { state.pivotBlocker(lane: $0, mode: mode, fundWithVenture: useVenture) } != nil)
                    .keyboardShortcut(.defaultAction)
                }
            }
            .padding(24)
            .frame(width: 640)
            .interactiveDismissDisabled()
            .onAppear { lane = offer.suggestedLane }
        }
    }

    private func explanation(state: GameState, offer: PivotOffer) -> String {
        switch mode {
        case .pivot:
            return "\(mode.summary) The \(state.startingLane.businessName.lowercased()) sells for \(Format.money(offer.salePrice)). People whose skills carry over come with you."
        case .expand:
            return "\(mode.summary) Setup costs \(Format.money(lane?.setupCost ?? 0))."
        }
    }
}

struct EndingView: View {
    @Environment(GameStore.self) private var store

    var body: some View {
        if let e = store.state?.ending {
            VStack(alignment: .leading, spacing: 14) {
                Text(e.kind.name.uppercased()).font(.caption.bold()).foregroundStyle(.secondary)
                Text(e.title).font(.system(size: 34, weight: .heavy))
                Text(e.summary).font(.title3)
                Divider()
                ForEach(Array(e.story.enumerated()), id: \.offset) { _, line in
                    Text(line).fixedSize(horizontal: false, vertical: true)
                }
                Divider()
                HStack {
                    StatTile(label: "Final valuation", value: Format.money(e.valuation))
                    StatTile(label: "You walk away with", value: Format.money(e.founderTakeHome))
                    StatTile(label: "Ended", value: GameClock.label(e.week))
                }
                HStack {
                    Spacer()
                    Button("New game") { store.abandon() }
                        .buttonStyle(.borderedProminent)
                        .keyboardShortcut(.defaultAction)
                }
            }
            .padding(28)
            .frame(width: 620)
            .interactiveDismissDisabled()
        }
    }
}
#endif
