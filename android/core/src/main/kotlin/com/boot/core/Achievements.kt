package com.boot.core

import kotlinx.serialization.Serializable

/*
 * Achievements: the run's trophy cabinet. Unlike goals they pay nothing; they're there to be
 * collected, from shipping a first game to owning Nintendo. Each one is checked every week and
 * remembered with the week you earned it.
 */

@Serializable
enum class Achievement(val title: String, val detail: String, val tier: Tier, internal val met: GameState.() -> Boolean) {
    // Making games
    HELLO_WORLD("Hello, World", "Ship your first game.", Tier.BRONZE, { (games?.releases?.size ?: 0) >= 1 }),
    BREAKOUT("Breakout Hit", "Ship a game that scores 8 or more.", Tier.BRONZE, { games?.releases?.any { it.score >= 8 } == true }),
    MASTERPIECE("Masterpiece", "Ship a game that scores 9.5 or more.", Tier.GOLD, { games?.releases?.any { it.score >= 9.5 } == true }),
    BLOCKBUSTER("Blockbuster", "Ship an AAA game.", Tier.SILVER, { games?.releases?.any { it.concept.size == GameSize.AAA } == true }),
    NUMBER_ONE("Number One", "Top the weekly sales chart.", Tier.SILVER, { stats.chartToppers > 0 }),
    GOTY("Game of the Year", "Win Game of the Year.", Tier.GOLD, { (games?.market?.wins ?: 0) >= 1 }),
    BEST_IN_SHOW("Best in Show", "Win Best in Show at the year's big games show.", Tier.SILVER, { stats.bestInShow > 0 }),
    WORLD_PREMIERE("World Premiere", "Premiere a trailer at the year-end awards or Gamescom.", Tier.BRONZE, { stats.premieres > 0 }),
    SHADOW_DROP("Out Now", "Shadow-drop a game live during a show.", Tier.BRONZE, { stats.shadowDrops > 0 }),
    TROPHY_CABINET("Trophy Cabinet", "Win five industry awards of any kind.", Tier.GOLD, { stats.honours + stats.awards >= 5 }),
    DYNASTY("Dynasty", "Win Game of the Year three times.", Tier.PLATINUM, { (games?.market?.wins ?: 0) >= 3 }),
    PLATINUM_SALES("Platinum Seller", "Sell a million copies of one game.", Tier.SILVER, { games?.releases?.any { it.unitsSold >= 1e6 } == true }),
    DIAMOND_SALES("Diamond Seller", "Sell ten million copies of one game.", Tier.GOLD, { games?.releases?.any { it.unitsSold >= 1e7 } == true }),
    FAN_FAVORITE("Fan Favorite", "Reach a million fans.", Tier.SILVER, { (games?.fans ?: 0.0) >= 1e6 }),
    TRILOGY("Trilogy", "Make a series of three games.", Tier.SILVER, { games?.franchises?.any { it.entries.size >= 3 } == true }),
    BELOVED("Beloved Series", "Make a series of three or more that averages 8.", Tier.GOLD, {
        games?.franchises?.any { it.entries.size >= 3 && it.averageScore >= 8 } == true
    }),
    SPIN_DOCTOR("Spin Doctor", "Ship a spin-off in a new genre.", Tier.BRONZE, {
        games?.let { g -> g.releases.any { r -> r.concept.sequelOf?.let { g.release(it) }?.concept?.genre?.let { it != r.concept.genre } == true } } == true
    }),
    EXPANDED("More of a Good Thing", "Release an expansion pack.", Tier.BRONZE, { games?.releases?.any { it.expansions.isNotEmpty() } == true }),
    DOUBLE_FEATURE("Double Feature", "Have two games in development at once.", Tier.SILVER, { games?.twoGames == true }),
    ENGINEER("Engine Builder", "Build your own game engine.", Tier.BRONZE, { (games?.engines?.size ?: 0) >= 2 }),
    MIDDLEWARE("Middleware", "License an engine to other studios.", Tier.SILVER, { games?.engines?.any { it.licensed } == true }),
    REMASTERED("Back by Popular Demand", "Release a remaster.", Tier.BRONZE, { games?.releases?.any { it.concept.remasterOf != null } == true }),

    // Your label
    LABEL("Record Label", "Publish another developer's game.", Tier.BRONZE, { (games?.published?.size ?: 0) >= 1 }),
    INDIE_DARLING("Indie Darling", "Publish a game that scores 8 or more.", Tier.SILVER, { games?.published?.any { it.score >= 8 } == true }),
    TALENT_SCOUT("Talent Scout", "Publish five games that score 7 or more.", Tier.GOLD, { (games?.published?.count { it.score >= 7 } ?: 0) >= 5 }),

    // Live games
    LIVE("Games as a Service", "Launch a live game.", Tier.BRONZE, { games?.releases?.any { it.live != null } == true }),
    SEASON_PASS("Season Pass", "Launch five seasons.", Tier.SILVER, { stats.seasons >= 5 }),
    MILLION_PLAYERS("Live and Thriving", "Have a million players in a live game.", Tier.GOLD, { games?.releases?.any { (it.live?.peakPlayers ?: 0.0) >= 1e6 } == true }),
    GRAND_FINAL("Grand Final", "Hold an esports championship.", Tier.SILVER, { stats.finals >= 1 }),
    PRIME_TIME("Prime Time", "Have a million people watching your league.", Tier.GOLD, { games?.releases?.any { (it.league?.peakViewers ?: 0.0) >= 1e6 } == true }),

    // Buying studios
    FIRST_STUDIO("Shopping Spree", "Buy a studio.", Tier.SILVER, { (games?.subsidiaries?.size ?: 0) >= 1 }),
    GIANT_SLAYER("Giant Slayer", "Buy one of the big publishers.", Tier.GOLD, { games?.subsidiaries?.any { studio(it.name)?.indie == false } == true }),
    HOLDING_COMPANY("Holding Company", "Own five studios at once.", Tier.GOLD, { (games?.subsidiaries?.size ?: 0) >= 5 }),
    ITS_A_ME("It's-a Me", "Buy Nintendo.", Tier.PLATINUM, { games?.owns("Nintendo") == true }),

    // The company
    FIRST_HIRE("Not Alone", "Hire your first employee.", Tier.BRONZE, { staff.isNotEmpty() }),
    FIFTY("Fifty Strong", "Have 50 people on staff.", Tier.SILVER, { staff.size >= 50 }),
    CAMPUS("Campus Life", "Move onto a campus.", Tier.SILVER, { office == Office.CAMPUS }),
    BEST_PLACE("Best Place to Work", "Have every office perk.", Tier.GOLD, { perks.size >= Perk.entries.size }),
    KNOW_IT_ALL("Know-It-All", "Research 40 things.", Tier.SILVER, { researched.size >= 40 }),
    TWO_LANES("Diversified", "Run two businesses at once.", Tier.BRONZE, { lanes.size >= 2 }),
    ECOSYSTEM("Ecosystem", "Run hardware, software and services together.", Tier.GOLD, { isEcosystem }),

    // Money
    MILLIONAIRE("Seven Figures", "Have a million in the bank.", Tier.BRONZE, { cash >= 1e6 }),
    UNICORN("Unicorn", "Be worth a billion.", Tier.SILVER, { valuation >= 1e9 }),
    BOOTSTRAPPED("Bootstrapped", "Be worth $50M without raising a cent.", Tier.GOLD, { valuation >= 5e7 && !venture.isBacked }),
    RING_THE_BELL("Ring the Bell", "Take the company public.", Tier.GOLD, { listing != null || ending?.kind == EndingKind.IPO }),
    TOP_TEN("Top Ten", "Make the industry's top ten.", Tier.GOLD, { valuation >= 1e10 && rank in 1..10 }),
    BIGGEST("Biggest in the World", "Be the most valuable company in the industry.", Tier.PLATINUM, { valuation >= 1e11 && rank == 1 }),
    TRILLION("Trillion-Dollar Club", "Be worth a trillion.", Tier.PLATINUM, { valuation >= 1e12 }),

    // Grit
    HARD_HIT("Hard Mode Hero", "Ship a hit on hard.", Tier.GOLD, { difficulty == Difficulty.HARD && games?.releases?.any { it.score >= 8 } == true }),
    LONG_HAUL("The Long Haul", "Play into year fifteen.", Tier.SILVER, { week >= 52 * 14 }),
    ERA_RIDER("Sign of the Times", "Meet an era's challenge.", Tier.BRONZE, { stats.eraChallengesWon >= 1 }),
    DECADE_CROWN("Game of the Decade", "Make the best game of a decade.", Tier.GOLD, { stats.gamesOfTheDecade >= 1 }),
    ERA_MASTER("Master of Eras", "Meet three eras' challenges in one run.", Tier.GOLD, { stats.eraChallengesWon >= 3 }),
    ;

    enum class Tier { BRONZE, SILVER, GOLD, PLATINUM }
}

internal fun GameState.checkAchievements() {
    // Sandbox money doesn't earn trophies.
    if (sandbox) return
    for (a in Achievement.entries) {
        if (a in achievements) continue
        if (!a.met(this)) continue
        achievements = achievements + (a to week)
        addLog("Achievement unlocked: ${a.title}. ${a.detail}", LogTone.MILESTONE)
    }
}

/** Earned achievements, newest first. */
val GameState.earnedAchievements: List<Achievement>
    get() = achievements.entries.sortedByDescending { it.value }.map { it.key }
