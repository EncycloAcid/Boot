# Boot

A macOS management game. You start solo in a cramped apartment and build a company in one of six lanes: **Games, Websites, Apps, Social Media, Hardware or AI**. Each lane plays like a different management game. Mid-game, the **Pivot Point** forces you to expand into a second lane or pivot into it. Lane pairings unlock crossover products and their own problems. Eras, rivals and a reputation that follows you everywhere shape the run, and the ending (IPO, acquisition, staying indie, burnout, bankruptcy or getting fired by your own board) reflects how you got there.

The full design is in [docs/DESIGN.md](docs/DESIGN.md).

## Play it

### On a Mac (the game)

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

The simulation is deterministic per seed and the whole game state is `Codable`, so saves, tests and balance runs are reproducible.

## Develop

```sh
swift build
swift test
```

CI builds and tests on macOS (including the SwiftUI app and the `.app` bundle) and Linux.
