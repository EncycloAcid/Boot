# Boot

A company management game for Android and macOS. You start solo in a cramped apartment and build a company in one of six lanes: **Games, Websites, Apps, Social Media, Hardware or AI**. Each lane plays like a different management game. Mid-game, the **Pivot Point** forces you to expand into a second lane or pivot into it. Lane pairings unlock crossover products and their own problems. Eras, rivals and a reputation that follows you everywhere shape the run, and the ending (IPO, acquisition, staying indie, burnout, bankruptcy or getting fired by your own board) reflects how you got there.

The full design is in [docs/DESIGN.md](docs/DESIGN.md).

## Play it

### On Android

The Android game lives in [`android/`](android): a Kotlin port of the simulation plus a Jetpack Compose phone UI. It needs Android 8.0 (API 26) or later.

Open the `android/` folder in Android Studio and press Run, or from a terminal with the Android SDK installed (JDK 17+, JDK 21 for the UI tests):

```sh
cd android
./gradlew :app:installDebug          # build and install on a connected phone or emulator
./gradlew :app:assembleRelease       # app/build/outputs/apk/release/app-release.apk
./gradlew :core:test                 # simulation tests (plain JVM)
./gradlew :app:testDebugUnitTest     # UI tests on Robolectric, no emulator; screenshots land in app/build/screenshots
./gradlew :core:balance -Pruns=20    # autopilot balance report
```

The release build is signed with the debug key so it installs out of the box; swap in your own signing config before publishing to Google Play. The game autosaves after every action and picks up where you left off.

On a phone the five tabs along the bottom are Home, Lanes, People, Combos and Money; News sits in the top bar. The bar above the tabs shows your cash and advances time by a week, a month or a quarter, stopping whenever a decision needs you.

### On a Mac

Requires macOS 14 and Xcode 15 or later.

```sh
swift run Boot              # launches the game window
scripts/make-app.sh         # or build dist/Boot.app to double-click
```

You can also open `Package.swift` in Xcode, pick the **Boot** scheme and press Run.

The game autosaves to `~/Library/Application Support/Boot/autosave.json`. Use **⌘↩** to advance a week, **⇧⌘↩** for a month, **⌥⌘↩** for a quarter.

### In a terminal (any platform)

```sh
swift run boot-cli                  # interactive game; type `help` for commands
swift run boot-cli auto games 7     # watch the autopilot play a Games run with seed 7
swift run boot-cli balance 20       # 20 autopilot games per starting lane, with outcome stats
```

## Project layout

```
android/
  core/             The simulation in Kotlin (a port of BootCore). Plain JVM; tests and balance runs need no device.
  app/              The Android game: Jetpack Compose, Material 3, autosave, Robolectric UI tests
Sources/
  BootCore/         The whole simulation. Foundation only; builds and tests on macOS and Linux.
    Lanes/          One file per lane: GamesStudio, WebAgency, AppStudio, SocialPlatform, HardwareLab, AILab
    GameState.swift The company: lanes, staff, cash, ledger, valuation
    Simulation.swift The weekly tick
    Actions.swift   Everything the player can do
    Pivot.swift     The Pivot Point, later expansion, the ecosystem company
    Combos.swift    The 15 crossover products and their problems
    Events.swift    Decisions that interrupt the calendar
    Venture.swift   Funding rounds and the board
    World.swift     Eras and rival studios
    Reputation.swift Scars that follow you across lanes
    Endings.swift   Endings and the story of your run
    Autopilot.swift A scripted player for balance runs and tests
  BootApp/          The SwiftUI macOS game
  BootCLI/          Terminal harness
Tests/BootCoreTests/
```

The simulation is deterministic per seed and the whole game state is serializable (`Codable` in Swift, kotlinx.serialization on Android), so saves, tests and balance runs are reproducible.

The Swift `BootCore` and the Kotlin `android/core` are two implementations of the same design and the same numbers. A rule change belongs in both; each side has its own test suite and autopilot balance run to check it.

## Develop

```sh
swift build
swift test
```

CI builds and tests on macOS (including the SwiftUI app and the `.app` bundle) and Linux, and builds, lints and tests the Android game.
