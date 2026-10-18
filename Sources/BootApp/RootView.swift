#if os(macOS)
import BootCore
import SwiftUI

/// Which modal is up. Endings beat the Pivot Point, which beats ordinary events.
enum Modal: Identifiable {
    case ending
    case pivot
    case event(Int)

    var id: String {
        switch self {
        case .ending: "ending"
        case .pivot: "pivot"
        case let .event(id): "event-\(id)"
        }
    }
}

struct RootView: View {
    @Environment(GameStore.self) private var store

    var body: some View {
        if let state = store.state {
            GameView(state: state)
        } else {
            NewGameView()
        }
    }
}

struct GameView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    private var modal: Binding<Modal?> {
        Binding(
            get: {
                guard let s = store.state else { return nil }
                if s.isOver { return .ending }
                if s.pendingPivot != nil { return .pivot }
                if let e = s.events.first { return .event(e.id) }
                return nil
            },
            set: { _ in }
        )
    }

    private var errorShown: Binding<Bool> {
        Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })
    }

    var body: some View {
        @Bindable var store = store
        NavigationSplitView {
            List(selection: $store.selection) {
                Label("Dashboard", systemImage: "gauge.with.dots.needle.33percent").tag(Screen.dashboard)
                Section("Lanes") {
                    ForEach(state.lanes) { lane in
                        Label(lane.businessName, systemImage: lane.symbol).tag(Screen.lane(lane))
                    }
                }
                Section("Company") {
                    Label("People", systemImage: "person.3").tag(Screen.staff)
                    Label("Combos", systemImage: "point.3.connected.trianglepath.dotted").tag(Screen.combos)
                    Label("Money & Board", systemImage: "banknote").tag(Screen.funding)
                    Label("Strategy", systemImage: "globe").tag(Screen.strategy)
                    Label("News", systemImage: "newspaper").tag(Screen.news)
                }
            }
            .navigationSplitViewColumnWidth(min: 190, ideal: 210)
        } detail: {
            ScrollView {
                detail
                    .padding(20)
                    .frame(maxWidth: 1100, alignment: .leading)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .navigationTitle(state.companyName)
        .navigationSubtitle("\(state.dateLabel) · \(state.world.era.name)")
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Text(Format.money(state.cash))
                    .monospacedDigit()
                    .foregroundStyle(state.cash < 0 ? Color.red : Color.primary)
                    .help("Cash in the bank")
                Button {
                    store.advance(weeks: 1)
                } label: {
                    Label("Next Week", systemImage: "forward.frame")
                }
                .help("Advance one week (⌘↩)")
                .disabled(!store.canAdvance)
                Button {
                    store.advance(weeks: 4)
                } label: {
                    Label("Next Month", systemImage: "forward")
                }
                .help("Advance four weeks, stopping for decisions (⇧⌘↩)")
                .disabled(!store.canAdvance)
            }
        }
        .sheet(item: modal) { m in
            switch m {
            case .ending: EndingView()
            case .pivot: PivotSheet()
            case .event: EventSheet()
            }
        }
        .alert("Can't do that", isPresented: errorShown) {
            Button("OK") { store.error = nil }
        } message: {
            Text(store.error ?? "")
        }
    }

    @ViewBuilder
    private var detail: some View {
        switch store.selection ?? .dashboard {
        case .dashboard: DashboardView(state: state)
        case let .lane(lane):
            if state.lanes.contains(lane) {
                LaneView(lane: lane, state: state)
            } else {
                DashboardView(state: state)
            }
        case .staff: PeopleView(state: state)
        case .combos: CombosView(state: state)
        case .funding: FundingView(state: state)
        case .strategy: StrategyView(state: state)
        case .news: NewsView(state: state)
        }
    }
}
#endif
