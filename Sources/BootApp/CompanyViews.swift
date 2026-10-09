#if os(macOS)
import BootCore
import SwiftUI

// MARK: People

struct PeopleView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("People").font(.title2.bold())
            HStack(alignment: .top, spacing: 16) {
                founderCard
                hiringCard
            }
            teamCard
        }
    }

    private var founderCard: some View {
        Card("You") {
            Meter(label: "Energy", value: state.founder.energy / 100,
                  tint: state.founder.energy < 25 ? .red : .green, caption: "\(Int(state.founder.energy))/100")
            if state.lanes.count > 1 {
                Text("Attention").font(.caption).foregroundStyle(.secondary)
                ForEach(state.lanes) { lane in
                    HStack {
                        Text(lane.name).frame(width: 90, alignment: .leading)
                        Slider(value: Binding(
                            get: { state.founder.focus[lane] ?? 1 },
                            set: { v in store.perform { $0.setFocus(lane, v) } }), in: 0...1)
                        Text(Format.percent(state.founder.attentionShare(for: lane, among: state.lanes)))
                            .monospacedDigit().frame(width: 44, alignment: .trailing)
                    }
                }
                Text("Running more than one lane drains your energy every week.").font(.caption).foregroundStyle(.secondary)
            }
            ForEach(state.lanes) { lane in
                KeyValue(key: "\(lane.name) experience", value: String(format: "+%.1f output", state.founder.skillBonus(in: lane)))
            }
            Divider()
            Toggle("Crunch mode (company-wide)", isOn: Binding(
                get: { state.crunch },
                set: { on in store.perform { try $0.setCrunch(on) } }))
            Text("+40% output, worse polish, morale and energy fall fast.").font(.caption).foregroundStyle(.secondary)
            Toggle("Take freelance gigs", isOn: Binding(
                get: { state.founder.freelancing },
                set: { on in store.perform { try $0.setFreelancing(on) } }))
            Text("\(Format.money(state.founder.freelanceIncome(week: state.week)))/wk to cover rent. The company gets a third of you.")
                .font(.caption).foregroundStyle(.secondary)
            Button(state.founder.isOnVacation ? "On vacation (\(state.founder.vacationWeeksLeft) weeks left)" : "Take two weeks off") {
                store.perform { $0.takeVacation() }
            }
            .disabled(state.founder.isOnVacation)
        }
        .frame(width: 340)
    }

    private var hiringCard: some View {
        Card("Hiring") {
            Text("Rivals compete for the same people. Contested candidates vanish fast. Recruiting costs two weeks' salary.")
                .font(.caption).foregroundStyle(.secondary)
            ForEach(state.candidates) { c in
                HStack {
                    VStack(alignment: .leading) {
                        Text(c.name).font(.headline)
                        Text("\(c.role.name) · skill \(c.skill)").font(.caption)
                        if let rival = c.contestedBy {
                            Text("\(rival) is circling").font(.caption).foregroundStyle(.orange)
                        }
                    }
                    Spacer()
                    Text("\(Format.money(c.askingSalary))/wk").monospacedDigit()
                    Menu("Hire") {
                        ForEach(state.lanes) { lane in
                            Button("Into \(lane.businessName)") { store.perform { try $0.hire(c.id, lane: lane) } }
                        }
                    }
                    .fixedSize()
                }
            }
        }
    }

    private var teamCard: some View {
        Card("Team · \(Format.money(state.payroll))/wk payroll · \(state.headquarters.name) (\(Format.money(state.headquarters.rent))/wk)") {
            if state.staff.isEmpty {
                Text("It's just you in the apartment.").foregroundStyle(.secondary)
            }
            ForEach(state.staff) { e in
                HStack {
                    Text(e.name).frame(width: 150, alignment: .leading)
                    Text(e.role.name).foregroundStyle(.secondary).frame(width: 90, alignment: .leading)
                    Text("skill \(e.skill)").monospacedDigit().frame(width: 60, alignment: .leading)
                    ProgressView(value: e.morale).tint(e.morale < 0.35 ? Color.red : Color.green).frame(width: 80)
                    Text("\(Format.money(e.salary))/wk").monospacedDigit().frame(width: 90, alignment: .trailing)
                    Spacer()
                    Picker("", selection: Binding(
                        get: { e.lane },
                        set: { lane in store.perform { try $0.assign(e.id, to: lane) } })) {
                        Text("Unassigned").tag(Lane?.none)
                        ForEach(state.lanes) { Text($0.name).tag(Lane?.some($0)) }
                    }
                    .labelsHidden()
                    .frame(width: 140)
                    Button(role: .destructive) {
                        store.perform { try $0.fire(e.id) }
                    } label: {
                        Image(systemName: "person.badge.minus")
                    }
                    .help("Let go with four weeks' severance")
                }
            }
        }
    }
}

// MARK: Combos

struct CombosView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Combos").font(.title2.bold())
            Text("Every pairing of lanes unlocks a crossover product only that combination gets, along with a problem unique to it.")
                .foregroundStyle(.secondary)
            ForEach(state.crossovers) { c in
                crossoverCard(c)
            }
            Card("All crossovers") {
                ForEach(Combo.allCases) { combo in
                    let unlocked = state.crossover(combo) != nil
                    HStack(alignment: .firstTextBaseline) {
                        Image(systemName: unlocked ? "lock.open" : "lock").foregroundStyle(.secondary)
                        Text("\(combo.lanes.0.name) + \(combo.lanes.1.name)").frame(width: 170, alignment: .leading)
                        Text(combo.product).bold().frame(width: 170, alignment: .leading)
                        Text(combo.pitch).font(.caption).foregroundStyle(.secondary)
                    }
                    .opacity(unlocked ? 1 : 0.6)
                }
            }
        }
    }

    private func crossoverCard(_ c: Crossover) -> some View {
        Card(c.combo.product) {
            Text(c.combo.pitch)
            Label(c.combo.problem, systemImage: "exclamationmark.triangle").font(.callout).foregroundStyle(.orange)
            if c.built {
                Text("\(c.combo.dial): \(Format.percent(c.intensity))").font(.callout)
                Slider(value: Binding(
                    get: { c.intensity },
                    set: { v in store.perform { try $0.setIntensity(c.combo, v) } }), in: 0...1)
            } else if c.buildWeeksLeft < c.combo.buildWeeks {
                Meter(label: "Building", value: 1 - Double(c.buildWeeksLeft) / Double(c.combo.buildWeeks),
                      caption: "\(c.buildWeeksLeft) weeks left")
            } else {
                HStack {
                    Button("Build for \(Format.money(c.combo.buildCost))") {
                        store.perform { try $0.buildCrossover(c.combo) }
                    }
                    Text("About \(c.combo.buildWeeks) weeks").font(.caption).foregroundStyle(.secondary)
                }
            }
            Meter(label: "Trouble brewing", value: c.pressure, tint: .red,
                  caption: c.incidents > 0 ? "\(c.incidents) incident\(c.incidents == 1 ? "" : "s") so far" : nil)
        }
    }
}

// MARK: Money and the board

struct FundingView: View {
    @Environment(GameStore.self) private var store
    var state: GameState
    @State private var useVenture = false

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Money & Board").font(.title2.bold())
            HStack(alignment: .top, spacing: 16) {
                roundsCard
                boardCard
            }
            HStack(alignment: .top, spacing: 16) {
                expansionCard
                ipoCard
            }
        }
    }

    private var roundsCard: some View {
        Card("Ownership") {
            KeyValue(key: "You own", value: Format.percent(state.venture.founderEquity))
            KeyValue(key: "Raised", value: Format.money(state.venture.raised))
            ForEach(Array(state.venture.rounds.enumerated()), id: \.offset) { _, r in
                KeyValue(key: "\(r.name), \(GameClock.label(r.week))", value: "\(Format.money(r.amount)) at \(Format.money(r.preMoney)) pre")
            }
            Divider()
            switch state.roundOffer() {
            case .success(let offer):
                Text("Investors would put in \(Format.money(offer.amount)) for \(Format.percent(offer.dilution)) of the company.")
                Button("Raise a \(offer.name)") { store.perform { try $0.raiseRound() } }
                Text("Money comes with a board that sets growth targets and can overrule or fire you.")
                    .font(.caption).foregroundStyle(.secondary)
            case .failure(let e):
                Text(e.message).foregroundStyle(.secondary)
            }
        }
    }

    private var boardCard: some View {
        Card("Board") {
            if let b = state.venture.board {
                Meter(label: "Confidence", value: b.confidence, tint: b.confidence < 0.4 ? .red : .green)
                KeyValue(key: "Growth target", value: "\(Format.percent(b.growthTarget)) per quarter")
                if let g = b.lastGrowth { KeyValue(key: "Last quarter", value: Format.percent(g)) }
                KeyValue(key: "Grace quarters left", value: "\(b.graceQuarters)")
                KeyValue(key: "Demands met / defied", value: "\(b.demandsMet) / \(b.demandsDefied)")
                Text("Below 40% they start overruling you. At zero they replace you.").font(.caption).foregroundStyle(.secondary)
            } else {
                Text("No board. Nobody can overrule you.").foregroundStyle(.secondary)
            }
        }
    }

    private var expansionCard: some View {
        Card("Expansion") {
            if let blocker = state.expansionBlocker {
                Text(blocker).foregroundStyle(.secondary)
            } else {
                Text("Pick another lane. Hardware, software and services together make an ecosystem company.")
                Toggle("Fund it with venture money", isOn: $useVenture)
                ForEach(Lane.allCases.filter { !state.lanes.contains($0) }) { lane in
                    HStack {
                        Label(lane.name, systemImage: lane.symbol)
                        Spacer()
                        Text("setup \(Format.money(lane.setupCost))").font(.caption).foregroundStyle(.secondary)
                        Button("Expand") { store.perform { try $0.expand(into: lane, fundWithVenture: useVenture) } }
                    }
                }
            }
            if state.isEcosystem {
                Divider()
                Label("Ecosystem company", systemImage: "crown").foregroundStyle(.purple)
                KeyValue(key: "Profitable ecosystem weeks", value: "\(state.stats.ecosystemWeeks)/52")
            }
        }
    }

    private var ipoCard: some View {
        Card("Exit") {
            KeyValue(key: "Business value (ex-cash)", value: Format.money(state.enterpriseValue))
            KeyValue(key: "Annual revenue", value: Format.money(state.trailingRevenue(weeks: 52)))
            if let blocker = state.ipoBlocker {
                Text(blocker).foregroundStyle(.secondary)
            } else {
                Button("Go public") { store.perform { try $0.goPublic() } }
                    .buttonStyle(.borderedProminent)
                Text("This ends the game.").font(.caption).foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: News

struct NewsView: View {
    var state: GameState
    @State private var filter: Lane?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("News").font(.title2.bold())
                Spacer()
                Picker("Lane", selection: $filter) {
                    Text("Everything").tag(Lane?.none)
                    ForEach(Lane.allCases) { Text($0.name).tag(Lane?.some($0)) }
                }
                .frame(width: 200)
            }
            ForEach(state.log.reversed().filter { filter == nil || $0.lane == filter }) { entry in
                LogRow(entry: entry)
            }
        }
    }
}
#endif
