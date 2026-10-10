# Boot

A company management game for Android and macOS. You start solo in a cramped apartment and build a company in one of six lanes: **Games, Websites, Apps, Social Media, Hardware or AI**. Each lane plays like a different management game. Mid-game, the **Pivot Point** offers you a second lane: expand into it, pivot into it, or (on Android) turn it down and stay focused. Lane pairings unlock crossover products and their own problems. Eras, rivals and a reputation that follows you everywhere shape the run, and the ending (IPO, acquisition, staying indie, bankruptcy or getting fired by your own board) reflects how you got there.

The full design is in [docs/DESIGN.md](docs/DESIGN.md).

**New in 1.3 (Android):** esports. Research esports and your competitive hits (action or strategy games that scored 7.5+, on an engine with multiplayer) can get a pro league. Sponsors pay for every viewer, watching keeps people buying and playing, and twice a year there's a championship with a prize pool you choose: big pools win big audiences, but only pay off on big games. Watch for match-fixing. Two new achievements, and stat tiles now shrink long numbers to fit instead of cutting them off.

**New in 2.3 (Android):** Decades. When you start a company you can now pick when: today, as before, or at the start of one of tech's revolutions (home computers in 1977, the console wars in 1985, the web in 1995 or the smartphone in 2007) and play all the way through to the present. The calendar shows real dates ("Mar 1988"). History's eras arrive when they really did: the video game crash of 1983, the 8-bit console wars, the multimedia age, the dot-com boom and bust, Web 2.0, the smartphone, free-to-play, and on to the AI gold rush, and the company page lists what's still to come. Businesses open up when they became possible: websites from 1994, social media from 2004, apps from 2008 and AI from 2012. The consoles are the ones of the day, from the Atari 2600, Commodore 64, NES and Game Boy through the PlayStation, PS2, Wii and Xbox 360 to the PS4. The big series debut in their real years with their real titles (Super Mario Bros. in 1985, Grand Theft Auto III in 2001, Call of Duty 4: Modern Warfare in 2007, FIFA before it became EA Sports FC), and no studio makes a AAA game before the 2000s. Studios appear in the years they were making games, including Atari, Sierra, Infocom, Origin, Westwood and LucasArts, under the names of the day (Square before Square Enix). Your games are reviewed by the magazines of the day: Creative Computing, Byte, Electronic Games, Computer Gaming World, Zzap!64, Nintendo Power and Electronic Gaming Monthly. Each lane's biggest rival changes with the years (Atari, then Nintendo, then EA; MySpace, then Facebook), the news runs in the outlets of the time, and the league table has IBM, Commodore, AOL, Yahoo, Nokia and BlackBerry at what they were really worth, with today's giants far smaller back then.

**New in 2.2 (Android):** game ratings, reworked. Scores used to be measured against the best game you'd ever made, on a curve so steep that anything short of it looked like a flop, while what reviewers expected rose every year. By year eight the average game scored 1.4. Now every game starts at 5.0 and each part of how it was made adds or takes off points: craft (your team's points for the work against what a decent team makes that year, rising 10% a year), the topic and genre, the audience and platforms, the focus split, the design and tech balance, bugs and polish, the engine, trends, retreads and rushed sequels, and a small nudge for a drop well below your recent games or a new best. Above 8 each extra point counts half, so a 10 needs everything right. The review screen shows the biggest reasons, and the game report shows every part, so you always know why a game scored what it did. Strong play now averages around 7, with 9s for the best work.

**New in 2.1 (Android):** in-depth eras and a real newspaper.
- **Eras have a life.** Each one dawns (its effects building up from a third of their strength), peaks, and fades (easing to half). As one fades, the next is already chosen and the rumours start: the news and the era card hint at what's coming, so you can get ready. Eras now move salaries, GPU prices, hardware costs and investor appetite as well as demand, and there are three new ones: the Chip Shortage, the Cloud Gaming Push and the Super-App Era. The company page shows the era, its phase, every effect at today's strength and a timeline of the eras so far.
- **The Daily Byte.** The news is a newspaper now, with a front page (the era, a lead story and the latest), and sections for your company, the industry, games and the markets, plus your company log. Real outlets cover the era's trends and rumours, every rival's big launches, the big game releases and their reviews, esports finals, Game of the Year, the quarterly markets with a league table, and you: your game reviews, funding rounds, IPO, acquisitions and big milestones. The office shows the latest headlines, and the menu counts unread stories.

**New in 2.0 (Android):** the everything update.
- **Personalities.** Most people you hire have a trait: perfectionists raise quality but work slower, hustlers are fast but harder to keep happy, night owls thrive in crunch, mentors make the whole team learn faster, divas are brilliant and moody, team players lift everyone's morale, rainmakers bring in demand, loyal people never quit (or listen to rivals) and ask for smaller raises, and fast learners level up twice as fast. Candidates show theirs before you hire.
- **Product roadmaps** for every business but the game studio: four features each (case studies and offshore partners for the agency, onboarding and offline mode for apps, stories, an algorithmic feed and community notes for social, a drop-test lab and your own chip for hardware, a red team and an enterprise API for AI, and more). Each costs money and a few weeks of the team's time, then helps for good; some come with a catch.
- **Hall of Fame and legacy.** Every finished run is remembered and ranked. What you've done before gives new companies a head start: cash after your first run, a star hire once you've had an IPO, experience in your lane after going bust, research points for an ecosystem, the espresso machine after a billion-dollar run. The ending tells you where the run placed.
- **New events:** rush jobs for the agency, an App Store feature, a celebrity joining your social network, researchers who want to open-source a model, and the team asking for an offsite.

**New in 1.4 (Android):** a friendlier UI.
- A **Next step** card on the office suggests the one thing most worth doing now (money running out, a first hire, too few desks, research points to spend, pitches for your label) with a button that takes you there. Tap Not now to hide it until something else comes up.
- The game studio is split into four pages: **Make** (what the teams are building, offers, remasters), **Games** (your catalogue, live games, esports), **Market** (the chart, what's coming, your label, studios for sale) and **Tech** (engines, platforms, your notebook). A count shows where something is waiting for you.
- The top bar says what its numbers are ("41 RP", "7.3k fans"), and every top-bar control explains itself on a long press.
- Cancelling a game in development asks first.

**1.3.1:** the tab bar floats. It's a pill over the content with a soft shadow, the screen scrolls underneath it and fades out just behind it, and the selected tab's icon sits on a pill that slides between tabs.

**New in 1.2 (Android):** your own label, and remasters.
- Research a publishing label and small developers pitch you their games. Each pitch shows what it is, what it costs and what share you'd keep. You can see the demo and their last game, but not how good the team really is. A topic and genre you know works is the best sign. Fund up to three at a time, market them while they're made, and release them under your name with full reviews and a place on the chart.
- Remaster an old hit: a game that scored 7+ and is three years old comes back for today's platforms at half the work, and its old fans buy it again.
- Four new achievements, and your label's best game makes the story at the end of a run.

**New in 1.1 (Android):** two development teams. Research a second team once you're on a studio floor and make two games at once, Game Dev Tycoon style.
- While both teams are busy, a slider splits your game staff between them; a team working alone gets everyone. Team 1 can still take contracts and expansions while Team 2 makes a game.
- Team 2 can make small, medium and large games; AAA needs the main team, and size limits count each team's own people.
- Each team has its own development, focus and hype panels, its own release button, and its own card on the office screen, and points float up for both.
- There's a new "Double Feature" achievement.

**1.1.1 polish:** focus sliders start in the middle of the track, not full; long game titles wrap on the office card instead of getting cut off; business descriptions show in full; stat tile notes wrap instead of ending in "…"; tiles in a row line up; the top bar shows the era without "The"; the live-service options fit ("Cosmetic", "Fair", "Aggressive"); and $0 incomes show grey instead of green.

**1.1.2 polish:** the UI holds up with big system text and in dark mode. Short labels (segmented tabs, stage tabs, stat tiles, the top bar, the office chips) shrink to fit instead of cutting off; a meter's label can no longer be squeezed into a column of single letters by a long note (the office card in polish did this); the stage pill sits above the game title instead of squeezing it; Team 2's Cancel button no longer disappears beside a long Release button; and the company card's pills wrap onto a new line. `./gradlew :app:testDebugUnitTest -PfontScale=1.3 -Pnight` redraws every screenshot with big text and/or in dark mode.

**New in 1.0 (Android):** the empire update.
- **Buy studios.** Every studio on the games market is for sale, from indies like Team Cherry, ConcernedApe and Larian to the giants: Rockstar, Nintendo, EA, PlayStation Studios, Valve.
  - A studio you own keeps making games under its own name. Its real series (GTA, Mario, Zelda, Call of Duty, FIFA and the rest) count as yours on the chart and stop competing with you.
  - About 6% of its value comes home every year as profit, more when it has hits out.
  - What it's worth counts towards your company's value, it leaves the industry league table, and you can sell it on.
- **Live-service games.** Researched after online services, a medium or bigger game can ship as a live game.
  - Players spend every week, but drift away, faster once the game goes stale.
  - Launch seasons of new content to bring them back. Choose a cosmetics-only, fair or aggressive store (aggressive pays more, bleeds players and gets you called pay-to-win), and pay for the servers.
  - A server rack blinks in the office while you run one.
- **39 achievements**, bronze to platinum, from shipping your first game to buying Nintendo or joining the trillion-dollar club. They pop up as you earn them, the newest show in the office, and the full cabinet is under Company › Goals.

**New in 0.9 (Android):** better graphics and a cleaner UI. The office is now an isometric diorama: back-to-back desks where you can see faces on one side and screens on the other, everyone's screen showing their lane's work (code and level editors, web pages, apps, feeds, CAD, training curves), each office tier with its own floor, windows and view, the perks you've bought standing along the walls, Game of the Year trophies on a shelf, and the night shift lit by monitors and desk lamps in dark mode. Everyone has a face: the same drawn person sits at their desk, appears in the team list and stays the same from candidate to hire, and you're in the top bar with your crown. Every game gets generated box art from its topic, genre, platform and rating, shown in the designer, while it's in development, in the reviews, the game report, your releases and your franchises. The Team tab previews the next office before you move, and staff rows are tidier with one menu for lanes, training and letting people go.

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
