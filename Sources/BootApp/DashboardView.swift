#if os(macOS)
import BootCore
import Charts
import SwiftUI

struct DashboardView: View {
    @Environment(GameStore.self) private var store
    var state: GameState

    private struct RevenuePoint: Identifiable {
        var id: String { "\(week)-\(lane)" }
        var week: Int
        var lane: String
        var amount: Double
    }

    private var revenuePoints: [RevenuePoint] {
        state.ledger.suffix(52).flatMap { w in
            var points = w.revenue.map { RevenuePoint(week: w.week, lane: $0.key.name, amount: $0.value) }
            if w.otherRevenue > 0 { points.append(RevenuePoint(week: w.week, lane: "Other", amount: w.otherRevenue)) }
            return points
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            tiles
            HStack(alignment: .top, spacing: 16) {
                Card("Revenue and costs, last 52 weeks") {
                    Chart {
                        ForEach(revenuePoints) { p in
                            BarMark(x: .value("Week", p.week), y: .value("Revenue", p.amount))
                                .foregroundStyle(by: .value("Source", p.lane))
                        }
                        ForEach(state.ledger.suffix(52), id: \.week) { w in
                            LineMark(x: .value("Week", w.week), y: .value("Costs", w.totalCosts))
                                .foregroundStyle(Color.red.opacity(0.8))
                                .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [4, 3]))
                        }
                    }
                    .chartYAxis {
                        AxisMarks { value in
                            AxisGridLine()
                            AxisValueLabel {
                                if let v = value.as(Double.self) { Text(Format.money(v)) }
                            }
                        }
                    }
                    .frame(height: 220)
                    Text("Dashed line: weekly costs").font(.caption).foregroundStyle(.secondary)
                }
                eraCard.frame(width: 300)
            }
            lanesCard
            HStack(alignment: .top, spacing: 16) {
                reputationCard
                newsCard
            }
        }
    }

    private var tiles: some View {
        let runway = state.runwayWeeks
        return HStack(spacing: 12) {
            StatTile(label: "Cash", value: Format.money(state.cash),
                     detail: runway.isFinite ? "\(Int(runway)) weeks of runway" : "Profitable",
                     tint: state.cash < 0 ? .red : .primary)
            StatTile(label: "Valuation", value: Format.money(state.valuation),
                     detail: "You own \(Format.percent(state.venture.founderEquity))")
            StatTile(label: "Weekly profit", value: (state.trailingProfit(weeks: 4) / 4).signedMoney,
                     detail: "4-week average",
                     tint: state.trailingProfit(weeks: 4) >= 0 ? .green : .red)
            StatTile(label: "Reputation", value: "\(Int(state.reputation.score))",
                     detail: state.reputation.scars.isEmpty ? "Clean" : "\(state.reputation.scars.count) scar(s)")
            StatTile(label: "Energy", value: "\(Int(state.founder.energy))",
                     detail: state.founder.isOnVacation ? "On vacation" : state.crunch ? "Crunching" : "Founder",
                     tint: state.founder.energy < 25 ? .red : .primary)
            StatTile(label: "Team", value: "\(state.staff.count + 1)", detail: state.headquarters.name)
        }
    }

    private var eraCard: some View {
        Card("Era") {
            Text(state.world.era.name).font(.title3.bold())
            Text(state.world.era.blurb).font(.callout).foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            Divider()
            ForEach(Lane.allCases) { lane in
                let d = state.world.era.demand(lane)
                HStack {
                    Text(lane.name).font(.caption)
                        .fontWeight(state.lanes.contains(lane) ? .bold : .regular)
                    Spacer()
                    Text(d == 1 ? "—" : (d > 1 ? "+" : "") + Format.percent(d - 1))
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(d > 1 ? Color.green : d < 1 ? Color.orange : Color.secondary)
                }
            }
            if state.world.era.gpuPrice != 1 {
                Text("GPU prices \(state.world.era.gpuPrice > 1 ? "up" : "down") \(Format.percent(abs(state.world.era.gpuPrice - 1)))")
                    .font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private var lanesCard: some View {
        Card("Your lanes") {
            ForEach(state.lanes) { lane in
                Button {
                    store.selection = .lane(lane)
                } label: {
                    HStack {
                        Image(systemName: lane.symbol).frame(width: 24)
                        VStack(alignment: .leading) {
                            Text(lane.businessName).font(.headline)
                            Text(state.statusLine(for: lane)).font(.callout).foregroundStyle(.secondary)
                        }
                        Spacer()
                        VStack(alignment: .trailing) {
                            Text("\(Format.money(state.trailingRevenue(lane: lane, weeks: 4) / 4))/wk")
                                .monospacedDigit()
                            let profit = state.trailingProfit(lane: lane, weeks: 4) / 4
                            Text("\(profit.signedMoney)/wk profit")
                                .font(.caption.monospacedDigit())
                                .foregroundStyle(profit >= 0 ? Color.green : Color.red)
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                if lane != state.lanes.last { Divider() }
            }
            if state.isEcosystem {
                Label("Ecosystem company: \(state.stats.ecosystemWeeks) profitable weeks. Hold it for 52.",
                      systemImage: "crown")
                    .foregroundStyle(.purple)
            }
            if state.pivot == nil {
                Text("The Pivot Point arrives between year 2 and year 4.").font(.caption).foregroundStyle(.secondary)
            } else if state.expansionBlocker == nil {
                Label("Every lane is profitable. You can expand from Money & Board.", systemImage: "plus.circle")
                    .foregroundStyle(.green)
            }
        }
    }

    private var reputationCard: some View {
        Card("Reputation") {
            Meter(label: "Score", value: state.reputation.score / 100, tint: .blue,
                  caption: "\(Int(state.reputation.score))/100")
            if state.reputation.scars.isEmpty {
                Text("No scars. People trust you, for now.").font(.callout).foregroundStyle(.secondary)
            }
            ForEach(state.reputation.scars) { scar in
                Meter(label: scar.kind.name, value: scar.severity, tint: .orange)
                Text(scar.note).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private var newsCard: some View {
        Card("Latest") {
            ForEach(state.log.suffix(8).reversed()) { entry in
                LogRow(entry: entry)
            }
        }
    }
}
#endif
