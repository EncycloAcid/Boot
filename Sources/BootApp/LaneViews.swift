#if os(macOS)
import BootCore
import Charts
import SwiftUI

struct LaneView: View {
    var lane: Lane
    var state: GameState

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            LaneHeader(lane: lane, subtitle: lane.pitch)
            HStack(spacing: 12) {
                StatTile(label: "Revenue", value: "\(Format.money(state.trailingRevenue(lane: lane, weeks: 4) / 4))/wk",
                         detail: "4-week average")
                let profit = state.trailingProfit(lane: lane, weeks: 4) / 4
                StatTile(label: "Profit", value: "\(profit.signedMoney)/wk", detail: "after this lane's payroll",
                         tint: profit >= 0 ? .green : .red)
                StatTile(label: "Team", value: "\(state.staff(in: lane).count)",
                         detail: "plus \(Format.percent(state.founder.attentionShare(for: lane, among: state.lanes))) of you")
                StatTile(label: "Market", value: Format.percent(state.world.era.demand(lane) * state.world.competition(in: lane)),
                         detail: state.world.rival(in: lane).map { "vs \($0.name)" })
            }
            switch lane {
            case .games: GamesView(state: state)
            case .websites: WebView(state: state)
            case .apps: AppsView(state: state)
            case .social: SocialView(state: state)
            case .hardware: HardwareView(state: state)
            case .ai: AIView(state: state)
            }
        }
    }
}

// MARK: Games

struct GamesView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        if let g = state.games {
            HStack(alignment: .top, spacing: 16) {
                Card("In development") {
                    if let p = g.project {
                        Text(p.title).font(.title3.bold())
                        Text("\(p.scope.name) · \(p.weeksInDev) weeks in · \(Format.money(p.scope.weeklyBudget))/wk budget")
                            .font(.callout).foregroundStyle(.secondary)
                        Picker("Development focus", selection: Binding(
                            get: { p.activeFocus },
                            set: { focus in store.perform { try $0.setGameFocus(focus) } })) {
                            ForEach(GameFocus.allCases) { focus in
                                Text(focus.name).tag(focus)
                            }
                        }
                        .pickerStyle(.menu)
                        Text(p.activeFocus.description).font(.caption).foregroundStyle(.secondary)
                        Meter(label: "Content", value: p.completion)
                        Meter(label: "Polish", value: min(1, p.polish / (p.scope.work * 1.5)), tint: .purple,
                              caption: p.isReady ? "Keep polishing for a better review" : nil)
                        HStack {
                            Button("Launch \(p.title)") { store.perform { try $0.launchGame() } }
                                .buttonStyle(.borderedProminent)
                                .disabled(!p.isReady)
                            Button("Cancel project", role: .destructive) { store.perform { try $0.cancelGame() } }
                        }
                    } else {
                        Text("Nothing in development. What's next?").foregroundStyle(.secondary)
                        ForEach(GameScope.allCases) { scope in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(scope.name).font(.headline)
                                    Text("\(Int(scope.work)) work · \(Format.money(scope.weeklyBudget))/wk budget")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Button("Start") { store.perform { try $0.startGame(scope) } }
                            }
                        }
                    }
                }
                Card("Fanbase") {
                    KeyValue(key: "Fans", value: Format.count(g.fans))
                    Meter(label: "Fan trust", value: g.fanTrust / 1.2, tint: g.fanTrust < 0.7 ? .orange : .green,
                          caption: String(format: "%.2f", g.fanTrust))
                    KeyValue(key: "Hits", value: "\(g.hits)")
                    KeyValue(key: "Flops", value: "\(g.flops)")
                    Text("Flops knock fan trust down and only hits bring it back. The fanbase never forgets.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                .frame(width: 300)
            }
            Card("Releases") {
                if g.releases.isEmpty {
                    Text("No games shipped yet.").foregroundStyle(.secondary)
                }
                ForEach(g.releases) { r in
                    HStack {
                        Text(r.title).font(.headline)
                        Text(r.verdict).font(.caption).padding(.horizontal, 6).padding(.vertical, 2)
                            .background(Capsule().fill((r.review >= 85 ? Color.green : r.review < 55 ? Color.red : Color.gray).opacity(0.2)))
                        Spacer()
                        Text("Review \(r.review)").monospacedDigit()
                        Text("\(Format.money(r.weeklySales))/wk").monospacedDigit().frame(width: 100, alignment: .trailing)
                        Text("\(Format.money(r.lifetimeSales)) total").monospacedDigit().foregroundStyle(.secondary)
                            .frame(width: 120, alignment: .trailing)
                    }
                }
            }
        }
    }
}

// MARK: Websites

struct WebView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        if let w = state.web {
            HStack(alignment: .top, spacing: 16) {
                Card("New briefs") {
                    if w.offers.isEmpty {
                        Text("No offers this week. Standing and marketers bring more.").foregroundStyle(.secondary)
                    }
                    ForEach(w.offers) { o in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(o.client).font(.headline)
                                Text("\(Int(o.work)) work · due in \(o.weeksAllowed) weeks · 40% upfront")
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(Format.money(o.value)).monospacedDigit()
                            Button("Accept") { store.perform { try $0.acceptContract(o.id) } }
                            Button("Pass") { store.perform { try $0.declineContract(o.id) } }
                        }
                    }
                }
                Card("Agency") {
                    Meter(label: "Standing", value: w.standing, tint: .green)
                    if w.clientWariness > 0.05 {
                        Meter(label: "Client wariness", value: w.clientWariness, tint: .orange)
                    }
                    KeyValue(key: "Projects delivered", value: "\(w.completed)")
                    Picker("When the brief changes at 11pm", selection: Binding(
                        get: { w.policy },
                        set: { p in store.perform { try $0.setScopePolicy(p) } })) {
                        ForEach(ScopePolicy.allCases, id: \.self) { Text($0.name).tag($0) }
                    }
                    .pickerStyle(.radioGroup)
                }
                .frame(width: 300)
            }
            Card("Active projects") {
                if w.active.isEmpty {
                    Text("Nothing on the go. Accept a brief.").foregroundStyle(.secondary)
                }
                ForEach(w.active) { c in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(c.client).font(.headline)
                            if c.isOverdue { Text("OVERDUE").font(.caption.bold()).foregroundStyle(.red) }
                            if c.creepCount > 0 {
                                Text("\(c.creepCount) brief change\(c.creepCount == 1 ? "" : "s")").font(.caption).foregroundStyle(.orange)
                            }
                            Spacer()
                            Text("week \(c.weeksElapsed)/\(c.weeksAllowed)").font(.caption.monospacedDigit())
                        }
                        Meter(label: "Done", value: c.completion)
                        Meter(label: "Client happiness", value: c.satisfaction, tint: c.satisfaction < 0.35 ? .red : .green)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }
}

// MARK: Apps

struct AppsView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        if let a = state.apps {
            HStack(alignment: .top, spacing: 16) {
                Card(a.name) {
                    if !a.isLive {
                        Text("Building version 1.0").foregroundStyle(.secondary)
                        Meter(label: "Progress", value: min(1, a.buildProgress / a.buildWork))
                    } else {
                        HStack(spacing: 12) {
                            StatTile(label: "Users", value: Format.count(a.users))
                            StatTile(label: "Store rank", value: a.rankLabel)
                            StatTile(label: "Churn", value: Format.percent(a.lastChurn, digits: 1),
                                     detail: "per week", tint: a.lastChurn > 0.08 ? .red : .primary)
                            StatTile(label: "Installs", value: Format.count(a.lastInstalls), detail: "this week")
                        }
                        Meter(label: "Quality", value: a.quality / 100, tint: .purple, caption: "\(Int(a.quality))")
                        Meter(label: "Next update", value: min(1, a.updateProgress / a.updateStyle.work),
                              caption: "\(a.weeksSinceUpdate) weeks since last update")
                    }
                }
                Card("Strategy") {
                    Picker("Monetization", selection: Binding(
                        get: { a.monetization },
                        set: { m in store.perform { try $0.setMonetization(m) } })) {
                        ForEach(AppMonetization.allCases, id: \.self) { Text($0.name).tag($0) }
                    }
                    Picker("Updates", selection: Binding(
                        get: { a.updateStyle },
                        set: { u in store.perform { try $0.setUpdateStyle(u) } })) {
                        ForEach(UpdateStyle.allCases, id: \.self) { Text($0.name).tag($0) }
                    }
                    Text("Stale apps churn. Ship often to stay fresh, or ship big to raise quality.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                .frame(width: 300)
            }
            if a.isLive {
                HStack(spacing: 16) {
                    Card("Users") {
                        Chart(Array(a.userHistory.enumerated()), id: \.offset) { item in
                            AreaMark(x: .value("Week", item.offset), y: .value("Users", item.element))
                                .foregroundStyle(Color.accentColor.opacity(0.4))
                        }
                        .frame(height: 160)
                    }
                    Card("Weekly churn") {
                        Chart(Array(a.churnHistory.enumerated()), id: \.offset) { item in
                            LineMark(x: .value("Week", item.offset), y: .value("Churn %", item.element * 100))
                                .foregroundStyle(Color.red)
                        }
                        .frame(height: 160)
                    }
                }
            }
        }
    }
}

// MARK: Social

struct SocialView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        if let s = state.social {
            HStack(alignment: .top, spacing: 16) {
                Card(s.name) {
                    if !s.isLive {
                        Text("Building the platform").foregroundStyle(.secondary)
                        Meter(label: "Progress", value: min(1, s.buildProgress / s.buildWork))
                    } else {
                        HStack(spacing: 12) {
                            StatTile(label: "Accounts", value: Format.count(s.users))
                            StatTile(label: "Real people", value: Format.count(s.realUsers))
                            StatTile(label: "Growth", value: Format.percent(s.lastGrowth, digits: 1), detail: "per week",
                                     tint: s.lastGrowth > 0 ? .green : .red)
                        }
                        if s.caughtOn {
                            Label("It caught on.", systemImage: "flame").foregroundStyle(.orange)
                        } else {
                            Text("Slow start. If it catches on (around 250k users), growth goes exponential.")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        Meter(label: "Bots", value: s.botShare / 0.6, tint: .orange, caption: Format.percent(s.botShare))
                        Meter(label: "Toxicity", value: s.toxicity, tint: .red)
                    }
                }
                Card("Policy") {
                    Text("Moderation \(Format.percent(s.moderation))").font(.callout)
                    Slider(value: Binding(get: { s.moderation },
                                          set: { v in store.perform { try $0.setModeration(v) } }), in: 0...1)
                    Text("Costs money per user. Keeps bots, fires and scandals down.").font(.caption).foregroundStyle(.secondary)
                    Divider()
                    Text("Ad load \(Format.percent(s.adLoad))").font(.callout)
                    Slider(value: Binding(get: { s.adLoad },
                                          set: { v in store.perform { try $0.setAdLoad(v) } }), in: 0...1)
                    Text("Revenue now versus growth. Ads pay little until you're big.").font(.caption).foregroundStyle(.secondary)
                }
                .frame(width: 300)
            }
            if s.isLive {
                Card("Accounts") {
                    Chart(Array(s.userHistory.enumerated()), id: \.offset) { item in
                        AreaMark(x: .value("Week", item.offset), y: .value("Users", item.element))
                            .foregroundStyle(Color.accentColor.opacity(0.4))
                    }
                    .frame(height: 180)
                }
            }
        }
    }
}

// MARK: Hardware

struct HardwareView: View {
    @Environment(GameStore.self) private var store
    var state: GameState
    @State private var category: DeviceCategory = .headphones
    @State private var tier: DeviceTier = .budget
    @State private var units = 5_000

    var body: some View {
        if let h = state.hardware {
            HStack(alignment: .top, spacing: 16) {
                Card("Design") {
                    if let d = h.design {
                        Text(d.name).font(.title3.bold())
                        Text("\(Format.money(d.spec.price)) retail · \(Format.money(d.spec.unitCost)) to build · \(Format.money(d.spec.tier.weeklyRnD))/wk R&D")
                            .font(.callout).foregroundStyle(.secondary)
                        Meter(label: "Design", value: d.completion)
                        KeyValue(key: "Stock booked for launch", value: "\(d.preorderedStock) units")
                        Button("Launch the \(d.name)") { store.perform { try $0.launchDevice() } }
                            .buttonStyle(.borderedProminent)
                            .disabled(!d.isReady)
                    } else {
                        Picker("Category", selection: $category) {
                            ForEach(DeviceCategory.allCases) { Text($0.name).tag($0) }
                        }
                        Picker("Tier", selection: $tier) {
                            ForEach(DeviceTier.allCases) { Text($0.name).tag($0) }
                        }
                        .pickerStyle(.segmented)
                        let spec = DeviceSpec(category: category, tier: tier)
                        Text("\(Format.money(spec.price)) retail, \(Format.money(spec.unitCost)) per unit, \(Format.money(tier.weeklyRnD))/wk R&D")
                            .font(.caption).foregroundStyle(.secondary)
                        Button("Start design") { store.perform { try $0.startDevice(category, tier) } }
                    }
                }
                Card("Factory") {
                    Text("Lead time \(HardwareLab.leadTimeWeeks) weeks. Guess low and scalpers profit; guess high and you're stuck with last year's model.")
                        .font(.caption).foregroundStyle(.secondary)
                    HStack {
                        TextField("Units", value: $units, format: .number)
                            .textFieldStyle(.roundedBorder)
                            .frame(width: 100)
                        Stepper("", value: $units, in: 100...5_000_000, step: 1_000).labelsHidden()
                        Button("Book") { store.perform { try $0.orderUnits(units) } }
                            .disabled(h.design == nil && h.onMarket == nil)
                    }
                    if let spec = h.design?.spec ?? h.onMarket?.spec {
                        Text("Costs \(Format.money(Double(units) * spec.unitCost)) up front").font(.caption).foregroundStyle(.secondary)
                    }
                    ForEach(h.orders) { o in
                        KeyValue(key: "\(o.units) × \(o.model)", value: "arrives \(GameClock.label(o.arrivalWeek))")
                    }
                    if state.week < h.productionHaltedUntil {
                        Label("Production halted until \(GameClock.label(h.productionHaltedUntil))", systemImage: "xmark.octagon")
                            .foregroundStyle(.red)
                    }
                }
                .frame(width: 320)
            }
            Card("On the market") {
                if let m = h.onMarket {
                    HStack(spacing: 12) {
                        StatTile(label: m.name, value: Format.money(m.spec.price), detail: "Review \(Int(m.quality))")
                        StatTile(label: "Demand", value: "\(Format.count(h.lastDemand))/wk")
                        StatTile(label: "In stock", value: Format.count(Double(h.inventory)),
                                 tint: Double(h.inventory) < h.lastDemand ? .orange : .primary)
                        StatTile(label: "Sold", value: Format.count(Double(m.unitsSold)))
                    }
                } else {
                    Text("Nothing on sale yet.").foregroundStyle(.secondary)
                }
                Meter(label: "Brand awareness", value: h.brandAwareness, tint: .blue)
                KeyValue(key: "Models shipped", value: "\(h.modelsShipped)")
            }
        }
    }
}

// MARK: AI

struct AIView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        if let lab = state.ai {
            HStack(alignment: .top, spacing: 16) {
                Card("Models") {
                    HStack(spacing: 12) {
                        StatTile(label: "Best model", value: lab.modelName ?? "None")
                        StatTile(label: "Capability", value: "\(Int(lab.capability))")
                        StatTile(label: "Failed runs", value: "\(lab.failedRuns)")
                    }
                    if let t = lab.training {
                        Text("Training \(t.codename) (\(t.size.name.lowercased()))").font(.headline)
                        Meter(label: "Run", value: t.completion,
                              caption: "\(t.weeksDone)/\(t.size.weeks) weeks · \(Format.money(state.trainingWeeklyCost(t.size)))/wk")
                        Text("You won't know how good it is until it finishes.").font(.caption).foregroundStyle(.secondary)
                    } else {
                        ForEach(TrainingSize.allCases) { size in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(size.name).font(.headline)
                                    Text("\(size.weeks) weeks · \(Format.money(state.trainingWeeklyCost(size)))/wk · tops out near \(Int(size.ceiling))")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Button("Train") { store.perform { try $0.startTraining(size) } }
                            }
                        }
                    }
                }
                Card("Business") {
                    Picker("Focus", selection: Binding(
                        get: { lab.focus },
                        set: { p in store.perform { try $0.setAIFocus(p) } })) {
                        ForEach(AIProduct.allCases) { Text($0.name).tag($0) }
                    }
                    KeyValue(key: "Chatbot subscribers", value: Format.count(lab.subscribers))
                    KeyValue(key: "API volume", value: "\(Format.count(lab.apiVolume))k calls/wk")
                    KeyValue(key: "Enterprise clients", value: "\(lab.enterpriseClients)")
                    Divider()
                    KeyValue(key: "GPU units owned", value: "\(lab.gpusOwned)")
                    Button("Buy 4 GPU units (\(Format.money(4 * AILab.gpuUnitPrice * state.world.era.gpuPrice)))") {
                        store.perform { try $0.buyGPUs(4) }
                    }
                    Meter(label: "Regulatory heat", value: lab.regulatoryHeat, tint: .red)
                }
                .frame(width: 320)
            }
        }
    }
}
#endif
