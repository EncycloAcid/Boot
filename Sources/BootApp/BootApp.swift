#if os(macOS)
import AppKit
import SwiftUI

@main
struct BootApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var store = GameStore()

    var body: some Scene {
        WindowGroup("Boot") {
            RootView()
                .environment(store)
                .frame(minWidth: 1000, minHeight: 680)
        }
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("New Game…") { store.abandon() }
                    .keyboardShortcut("n", modifiers: [.command, .shift])
            }
            CommandMenu("Time") {
                Button("Next Week") { store.advance(weeks: 1) }
                    .keyboardShortcut(.return, modifiers: .command)
                    .disabled(!store.canAdvance)
                Button("Next Month") { store.advance(weeks: 4) }
                    .keyboardShortcut(.return, modifiers: [.command, .shift])
                    .disabled(!store.canAdvance)
                Button("Next Quarter") { store.advance(weeks: 13) }
                    .keyboardShortcut(.return, modifiers: [.command, .option])
                    .disabled(!store.canAdvance)
            }
        }
    }
}

/// Lets the game run as a normal foreground app straight from `swift run`.
final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApplication.shared.setActivationPolicy(.regular)
        NSApplication.shared.activate(ignoringOtherApps: true)
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }
}

#else

@main
enum BootAppUnavailable {
    static func main() {
        print("The Boot game window needs macOS. On this platform, play in the terminal with `swift run boot-cli`.")
    }
}

#endif
