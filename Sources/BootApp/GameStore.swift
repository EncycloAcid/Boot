#if os(macOS)
import BootCore
import Foundation
import Observation

enum Screen: Hashable {
    case dashboard
    case lane(Lane)
    case staff
    case combos
    case funding
    case news
}

/// Owns the running game, routes player actions into it, and autosaves.
@MainActor
@Observable
final class GameStore {
    var state: GameState?
    var error: String?
    var selection: Screen? = .dashboard

    private let saveURL: URL = {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("Boot", isDirectory: true).appendingPathComponent("autosave.json")
    }()

    init() {
        if let data = try? Data(contentsOf: saveURL),
           let saved = try? JSONDecoder().decode(GameState.self, from: data),
           !saved.isOver {
            state = saved
        }
    }

    var canAdvance: Bool {
        guard let s = state else { return false }
        return !s.isOver && !s.needsDecision
    }

    func newGame(company: String, founder: String, lane: Lane) {
        state = GameState(companyName: company, founderName: founder, lane: lane)
        selection = .dashboard
        save()
    }

    func abandon() {
        state = nil
        try? FileManager.default.removeItem(at: saveURL)
    }

    /// Run an action against the game. On failure the game is left untouched and the reason is shown.
    func perform(_ action: (inout GameState) throws -> Void) {
        guard var s = state else { return }
        do {
            try action(&s)
            state = s
            save()
        } catch let e as ActionError {
            error = e.message
        } catch {
            self.error = error.localizedDescription
        }
    }

    func advance(weeks: Int) {
        perform { _ = try $0.advance(weeks: weeks) }
    }

    private func save() {
        guard let s = state else { return }
        do {
            try FileManager.default.createDirectory(at: saveURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(s).write(to: saveURL, options: .atomic)
        } catch {
            // Autosave is best effort; the game keeps running.
        }
    }
}
#endif
