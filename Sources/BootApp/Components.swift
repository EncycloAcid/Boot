#if os(macOS)
import BootCore
import SwiftUI

struct Card<Content: View>: View {
    var title: String?
    var content: Content

    init(_ title: String? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let title {
                Text(title).font(.headline)
            }
            content
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .controlBackgroundColor)))
        .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color.primary.opacity(0.08)))
    }
}

struct StatTile: View {
    var label: String
    var value: String
    var detail: String?
    var tint: Color = .primary

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            Text(value).font(.title2.weight(.semibold)).monospacedDigit().foregroundStyle(tint)
            if let detail {
                Text(detail).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 10).fill(Color(nsColor: .controlBackgroundColor)))
        .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color.primary.opacity(0.08)))
    }
}

/// A labelled 0...1 bar.
struct Meter: View {
    var label: String
    var value: Double
    var tint: Color = .accentColor
    var caption: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack {
                Text(label).font(.caption)
                Spacer()
                Text(caption ?? Format.percent(value)).font(.caption).monospacedDigit().foregroundStyle(.secondary)
            }
            ProgressView(value: min(max(value, 0), 1)).tint(tint)
        }
    }
}

struct KeyValue: View {
    var key: String
    var value: String

    var body: some View {
        HStack {
            Text(key).foregroundStyle(.secondary)
            Spacer()
            Text(value).monospacedDigit()
        }
        .font(.callout)
    }
}

struct LaneHeader: View {
    var lane: Lane
    var subtitle: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: lane.symbol)
                .font(.title)
                .foregroundStyle(Color.accentColor)
                .frame(width: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(lane.businessName).font(.title2.bold())
                Text(subtitle).font(.callout).foregroundStyle(.secondary)
            }
        }
    }
}

extension LogTone {
    var symbol: String {
        switch self {
        case .good: "arrow.up.circle.fill"
        case .bad: "exclamationmark.triangle.fill"
        case .milestone: "star.circle.fill"
        case .neutral: "circle.fill"
        }
    }

    var color: Color {
        switch self {
        case .good: .green
        case .bad: .orange
        case .milestone: .purple
        case .neutral: .secondary
        }
    }
}

struct LogRow: View {
    var entry: LogEntry

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Image(systemName: entry.tone.symbol)
                .font(entry.tone == .neutral ? Font.system(size: 5) : Font.caption)
                .foregroundStyle(entry.tone.color)
                .frame(width: 14)
            Text(GameClock.label(entry.week))
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(width: 64, alignment: .leading)
            Text(entry.text).font(.callout)
        }
    }
}

extension Double {
    var signedMoney: String { (self >= 0 ? "+" : "") + Format.money(self) }
}
#endif
