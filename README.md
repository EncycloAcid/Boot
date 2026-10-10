# Boot

A company management game for Android and macOS. You start solo in a cramped apartment and build a company in one of six lanes: **Games, Websites, Apps, Social Media, Hardware or AI**. Each lane plays like a different management game. Mid-game, the **Pivot Point** offers you a second lane: expand into it, pivot into it, or (on Android) turn it down and stay focused. Lane pairings unlock crossover products and their own problems. Eras, rivals and a reputation that follows you everywhere shape the run, and the ending (IPO, acquisition, staying indie, bankruptcy or getting fired by your own board) reflects how you got there.

The full design is in [docs/DESIGN.md](docs/DESIGN.md).

**New in 0.8 (Android):** real franchises, your own series and better names. 47 real series (Call of Duty, EA Sports FC, Grand Theft Auto, Zelda, Mario, Pokémon, God of War, Halo, Elden Ring, Final Fantasy, Fortnite and more) release on their real schedules: yearly games every autumn, the big ones every few years, each announced months ahead in a new Coming soon list. The giants pull players away from anything that launches next to them, and the designer warns you before you ship into one. Your sequels and spin-offs now form franchises: a series that averages 7 or better sells more with every entry, a slipping one wears its fans out, and spin-offs take a series into a new genre. Sequels get real-style names ("Moss Knight II: Reborn", "Moss Knight Tactics"), new games get names that fit their topic and genre, and there's a button to roll another.

**New in 0.7 (Android):** more of a real tycoon. Price your games (budget, standard or premium: great games can charge more, weak ones sell better cheap) and set your agency's rates. Buy office perks (espresso machine, arcade, free lunch, nap pods, research library, gym, rooftop bar, daycare) that lift morale, learning and hiring, cost upkeep, and show up in the office. Going public no longer has to end the run: list the company and keep running it with a share price, quarterly earnings, secondary offerings and buybacks, and you can play on past year 15. Climb an industry league table of real companies, read quarterly results, license your engines to other studios and sell merchandise for your hits.

**New in 0.6 (Android):** real companies and better reviews. Platforms are PlayStation, Xbox, Nintendo Switch, Steam Deck, Meta Quest and friends; rivals are Electronic Arts, Rockstar, Nintendo, Apple, Meta, OpenAI and the rest; IGN, GameSpot, Game Informer, Eurogamer, PC Gamer and Edge review your games, each in their own format (stars, percentages, quarter points), with a Metascore, a separate player score that decides how long a game keeps selling, a breakdown by gameplay, story, visuals, sound and polish, and the good and the bad. The Pivot Point no longer forces a second lane: you can stay focused and add one later, or never. (This build uses real company names for personal play.)

**New in 0.5 (Android):** a big gameplay update. Pick Easy, Normal or Hard at the start. A chain of 43 **goals** (lane goals only count for lanes you run) always shows you what to aim for next and pays out cash, research points, fans or reputation. The games market comes alive: rival studios ship their own games, a weekly **top-10 sales chart** ranks theirs against yours, a strong rival release in your genre takes buyers from your launch, and every December the industry votes on **Game of the Year**. After launch you can **patch** a buggy game and build **expansion packs** for the ones that sold. Staff gain **experience and level up** (watch for "LV UP" over their desks). The bank offers **loans** as a lifeline, and eight new decision events turn up: raise requests, streamers, leaked builds, game jams, tax credits, server outages, sequel petitions and headhunters.

**New in 0.4 (Android):** a new UI built around your office. The home screen is an illustrated office that grows from the apartment to the campus, with everyone at their desks and design, tech and bug points floating off them each week. Time now runs by itself, Game Dev Tycoon style: press play, pick 1×, 2× or 3×, and the clock stops on its own when something needs you (an event, reviews, a game ready to ship, an overdraft). Founder energy, vacations and the burnout ending are gone; crunch now stretches your team rather than you.

**New in 0.3 (Android):** the Games lane is now a full game studio sim in the spirit of Game Dev Tycoon. Pick a topic, genre, audience, platforms and size; split each of the three development stages across focus areas; watch design and tech points, bugs and hype build up; then read four outlets' reviews and a game report that teaches you what works. A research tree (spent with research points your team generates) unlocks bigger games, sequels, marketing, custom engines and company and lane upgrades. Offices have desks, staff can be trained, and contracts, publisher deals, trends and a yearly convention fill the calendar. The macOS and terminal versions don't have this update yet.

## Play it

### On Android

The Android game lives in [`android/`](android): a Kotlin port of the simulation plus a Jetpack Compose phone UI. It needs Android 8.0 (API 26) or later.

The look is iOS structure with Material 3 Expressive character. From iOS it takes grouped backgrounds, inset grouped lists with hairline separators, the iOS system colors, form rows and bottom sheets for decisions. From [Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive) it takes the expressive motion scheme (springs everywhere), lane badges that morph into each lane's Material shape when picked, buttons that change shape when pressed (the play button morphs as time runs), connected button groups as segmented controls, wavy progress for work in flight, a shape-morphing loading indicator for AI training runs, and emphasized type for the big numbers. Light and dark themes are both supported; in dark mode the office is on the night shift.

Open the `android/` folder in Android Studio and press Run, or from a terminal with the Android SDK installed (JDK 21):

```sh
cd android
./gradlew :app:installDebug          # build and install on a connected phone or emulator
./gradlew :app:assembleRelease       # app/build/outputs/apk/release/app-release.apk
./gradlew :core:test                 # simulation tests (plain JVM)
./gradlew :app:testDebugUnitTest     # UI tests on Robolectric, no emulator; screenshots land in app/build/screenshots
./gradlew :core:balance -Pruns=20    # autopilot balance report
```

The build uses Android Gradle Plugin 9, compileSdk 37 and Material 3 1.5 (beta), which is where the Expressive components ship. The release build is signed with the debug key so it installs out of the box; swap in your own signing config before publishing to Google Play. The game autosaves after every action and picks up where you left off.

The status bar at the top shows your company, the date, cash and weekly profit, research points, and fans (or reputation), next to the clock: speed, a one-week step and play/pause. The five tabs along the bottom are Office, Lanes, Research, Team and Company (overview, money and loans, combos, goals); News is in the ⋯ menu and under the office. Leaving the app pauses the game. Saves from 0.1 and 0.2 don't load in 0.3 or later; 0.3 and 0.4 saves carry over (they play on Normal).

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
