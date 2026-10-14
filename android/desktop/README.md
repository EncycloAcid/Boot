# Boot Game Tycoon — Windows Desktop

This module is the Windows desktop port of the Kotlin simulation in `:core`. It is built with Compose Multiplatform and is intended to be the desktop-first companion to the Android app.

## Build on Windows

Requirements: JDK 21 and the Gradle wrapper included in `android/`.

```powershell
cd android
.\gradlew.bat :core:test
.\gradlew.bat :desktop:run
.\gradlew.bat :desktop:packageDistributionForCurrentOS
```

The installer task is configured to produce Windows MSI and EXE packages. CI uploads successful packages as the `boot-windows-desktop` artifact.

## Current desktop port

This branch is based on the repository's Android `cursor/tycoon-depth-629b` branch, which declares `versionName = "3.4.1"`. The port also carries the newer paid-playtesting and seasonal-demand systems from the gameplay feature branch.

The desktop interface is being brought up to parity with the Android simulation. It currently includes:

- Shared serialized game state and desktop save/load.
- Studio overview, calendar, finances, research points, achievements, news, and activity feed.
- Game concept design, topic/genre/audience/size/price selection, platform targeting, optional second genre, and live-service concepts.
- Development stages and focus allocation, launch forecast, paid playtesting, marketing campaigns, launch, and cancellation.
- Contract work, publisher advances, reviews, release library, patches, expansions, sequels, franchise history, publishing pitches, and seasonal launch demand.
- Live-service season launches and monetization choices.
- Esports leagues and prize-pool management.
- Research unlocks, topic research, custom engine building, staff hiring/training, and company settings.

This is an active port, not a final release. The CI build must pass before an installer should be treated as ready. The remaining parity work includes the full depth of each Android business screen, the richer hardware/device designer, remaining Android dialogs, and final save compatibility checks.
