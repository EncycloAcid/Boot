#if os(macOS)
import BootCore
import SwiftUI

struct NewGameView: View {
    @Environment(GameStore.self) private var store
    @State private var company = "Garage Labs"
    @State private var founder = ""
    @State private var lane: Lane = .games

    private let columns = [GridItem(.adaptive(minimum: 280), spacing: 14)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Boot").font(.system(size: 44, weight: .heavy))
                    Text("You start solo in a cramped apartment with $25k in savings. Pick a lane.")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }

                HStack(spacing: 14) {
                    TextField("Company name", text: $company)
                    TextField("Your name", text: $founder)
                }
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 560)

                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(Lane.allCases) { l in
                        LaneCard(lane: l, selected: l == lane)
                            .onTapGesture { lane = l }
                    }
                }

                HStack {
                    if lane.isCapitalIntensive {
                        Label("Hard mode: \(lane.name) starts with a seed round and a board that can overrule you.",
                              systemImage: "exclamationmark.triangle")
                            .foregroundStyle(.orange)
                    }
                    Spacer()
                    Button {
                        store.newGame(company: company.isEmpty ? "Garage Labs" : company,
                                      founder: founder.isEmpty ? "You" : founder, lane: lane)
                    } label: {
                        Text("Start \(lane.businessName)").padding(.horizontal, 12)
                    }
                    .controlSize(.large)
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                }
            }
            .padding(32)
            .frame(maxWidth: 1000)
        }
    }
}

private struct LaneCard: View {
    var lane: Lane
    var selected: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: lane.symbol).font(.title2)
                Text(lane.name).font(.title3.bold())
                Spacer()
                if lane.isCapitalIntensive {
                    Text("VC").font(.caption.bold()).padding(.horizontal, 6).padding(.vertical, 2)
                        .background(Capsule().fill(Color.orange.opacity(0.2)))
                }
            }
            Text(lane.pitch).font(.callout).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .frame(maxWidth: .infinity, minHeight: 130, alignment: .topLeading)
        .background(RoundedRectangle(cornerRadius: 12).fill(Color(nsColor: .controlBackgroundColor)))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(selected ? Color.accentColor : Color.primary.opacity(0.08), lineWidth: selected ? 2 : 1)
        )
        .contentShape(Rectangle())
    }
}
#endif
