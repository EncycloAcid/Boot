package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Your own label. Once you've researched publishing, small developers pitch you the games they're
 * making. Each pitch says what it is, what it costs to finish and what the team has done before;
 * how good they really are stays hidden until it ships. Sign one and you pay for the rest of its
 * development, can buy it marketing while it's being made, and release it under your name: critics
 * review it, it goes on the chart, and you keep your share of what it earns. Your fans give your
 * label's games a lift; a hit wins you a few more.
 */

/** A small developer's game, looking for a publisher. */
@Serializable
data class IndiePitch(
    val id: Int,
    val developer: String,
    val title: String,
    val topic: Topic,
    val genre: Genre,
    val size: GameSize,
    val platforms: List<Platform>,
    /** What they need to finish it, paid when you sign. */
    val funding: Double,
    val weeks: Int,
    /** Your cut of what it earns after the platforms take theirs. */
    val labelShare: Double,
    /** Their last game's score, or null when this is their first. */
    val lastScore: Double?,
    /** How the demo plays: 1 rough, 2 promising, 3 polished. A hint, not a promise. */
    val demo: Int,
    /** How good the team really is, 0.3...1. Hidden. */
    internal val talent: Double,
    val expiresWeek: Int,
) {
    val concept: GameConcept get() = GameConcept(title, topic, genre, platforms = platforms, size = size)

    val demoTitle: String
        get() = when (demo) {
            1 -> "Rough demo"
            2 -> "Promising demo"
            else -> "Polished demo"
        }
}

/** A game you've signed, being finished by its developer. */
@Serializable
data class SignedGame(
    val pitch: IndiePitch,
    val signedWeek: Int,
    var weeksLeft: Int,
    var totalWeeks: Int = weeksLeft,
    var hype: Double = 0.0,
    val campaigns: MutableList<Campaign> = mutableListOf(),
    var spent: Double = 0.0,
    var slipped: Boolean = false,
) {
    val title: String get() = pitch.title
    val completion: Double get() = (1 - weeksLeft.toDouble() / totalWeeks).clamped(0.0, 1.0)
}

/** Games your label can have in development at once. */
const val LABEL_SLOTS = 3

// Pitches

private val devFirst = listOf(
    "Moonlit", "Tiny", "Brave", "Quiet", "Velvet", "Paper", "Rusty", "Lucky", "Hollow", "Golden", "Sleepy", "Wild",
    "Neon", "Little", "Salt", "Clever", "Northern", "Odd", "Copper", "Lantern",
)
private val devSecond = listOf(
    "Kettle", "Fern", "Otter", "Comet", "Anchor", "Moth", "Harbor", "Badger", "Orchard", "Foxes", "Pebble", "Raven",
    "Teapot", "Hare", "Thistle", "Lighthouse", "Marmot", "Acorn", "Tide", "Bramble",
)
private val devSuffix = listOf("Games", "Studio", "Interactive", "Works", "Collective", "")

private fun GameState.developerName(): String {
    val g = games!!
    val taken = g.pitches.map { it.developer } + g.signed.map { it.pitch.developer } + companyName
    repeat(20) {
        val name = listOf(rng.pick(devFirst), rng.pick(devSecond), rng.pick(devSuffix)).filter { it.isNotEmpty() }.joinToString(" ")
        if (name !in taken) return name
    }
    return "${rng.pick(devFirst)} ${rng.pick(devSecond)} ${rng.int(2, 99)}"
}

/** The score a team of this talent would get for this game, before the critics' moods. */
private fun pitchQuality(talent: Double, topic: Topic, genre: Genre, platforms: List<Platform>): Double {
    val platformFit = platforms.map {
        when (it.fit(genre)) {
            Fit.GREAT, Fit.GOOD -> 0.3
            Fit.OKAY -> 0.0
            Fit.BAD -> -0.7
        }
    }.average()
    return 3.2 + 6.3 * talent + (topic.fit(genre).multiplier - 1) * 8 + platformFit
}

private fun GameState.newPitch(): IndiePitch {
    val g = games!!
    val sizes = buildList {
        add(GameSize.SMALL to 0.5)
        if (Research.MEDIUM_GAMES in researched) add(GameSize.MEDIUM to 0.35)
        if (Research.LARGE_GAMES in researched && week >= 104) add(GameSize.LARGE to 0.15)
    }
    var roll = rng.range(0.0, sizes.sumOf { it.second })
    val size = sizes.first { (_, w) -> (roll - w).also { roll = it } <= 0 }.first
    // Most teams are middling; a few are brilliant.
    val talent = (0.3 + 0.7 * rng.range(0.0, 1.0).pow(1.3)).clamped(0.3, 1.0)
    val topic = rng.pick(Topic.entries)
    val genre = rng.pick(Genre.entries)
    val onSale = platformsOnSale.filter { it != Platform.OWN_CONSOLE }
    val fitting = onSale.filter { it.fit(genre) != Fit.BAD }.ifEmpty { onSale }
    val platforms = (listOf(rng.pick(fitting)) + if (rng.chance(0.35)) listOf(rng.pick(onSale)) else emptyList()).distinct()
    val (lo, hi) = when (size) {
        GameSize.SMALL -> 30_000.0 to 70_000.0
        GameSize.MEDIUM -> 160_000.0 to 380_000.0
        else -> 1_200_000.0 to 2_400_000.0
    }
    val weeks = when (size) {
        GameSize.SMALL -> rng.int(8, 14)
        GameSize.MEDIUM -> rng.int(18, 30)
        else -> rng.int(36, 52)
    }
    val lastScore = if (rng.chance(0.65)) {
        ((pitchQuality(talent, topic, genre, platforms) + rng.range(-1.0, 1.0)).clamped(2.0, 9.8) * 10).roundToInt() / 10.0
    } else {
        null
    }
    val demoRead = talent + rng.range(-0.18, 0.18)
    val share = rng.range(0.45, 0.65) - if ((lastScore ?: 0.0) >= 8) 0.1 else 0.0
    return IndiePitch(
        id = g.nextId(), developer = developerName(),
        title = gameTitle(topic, genre, rng.int(0, 1_000_000).toLong(), g.releases.map { it.title }.toSet() + g.published.map { it.title }),
        topic = topic, genre = genre, size = size, platforms = platforms,
        funding = (rng.range(lo, hi) * (0.85 + 0.3 * talent) / 1_000).roundToInt() * 1_000.0,
        weeks = weeks, labelShare = (share * 100).roundToInt() / 100.0, lastScore = lastScore,
        demo = if (demoRead < 0.5) 1 else if (demoRead < 0.75) 2 else 3, talent = talent, expiresWeek = week + 6,
    )
}

// Actions

/** Why [pitchId] can't be signed, or null if it can. */
fun GameState.signBlocker(pitchId: Int): String? {
    val g = games ?: return "You don't run a game studio."
    if (Research.PUBLISHING !in researched) return "Research a publishing label first."
    val p = g.pitches.firstOrNull { it.id == pitchId } ?: return "That pitch has gone elsewhere."
    if (g.signed.size >= LABEL_SLOTS) return "Your label can back $LABEL_SLOTS games at a time."
    if (cash < p.funding) return "They need ${money(p.funding)}."
    return null
}

/** Fund a pitch: pay for the rest of its development and publish it when it's done. */
fun GameState.signPitch(pitchId: Int) {
    if (ending != null) throw gameOver()
    signBlocker(pitchId)?.let { throw notAvailable(it) }
    val g = games!!
    val p = g.pitches.first { it.id == pitchId }
    g.pitches.remove(p)
    spend(p.funding, Lane.GAMES)
    g.signed += SignedGame(p, week, p.weeks, hype = minOf(15.0, g.fans / 8_000), spent = p.funding)
    addLog("You signed ${p.title} by ${p.developer} for ${money(p.funding)}. It's due in ${Format.weeks(p.weeks)}.", LogTone.GOOD, Lane.GAMES)
}

/** Turn a pitch down. */
fun GameState.passPitch(pitchId: Int) {
    games?.pitches?.removeAll { it.id == pitchId }
}

/** Why a campaign can't be bought for a signed game, or null if it can. */
fun GameState.labelCampaignBlocker(pitchId: Int, c: Campaign): String? {
    val s = games?.signed?.firstOrNull { it.pitch.id == pitchId } ?: return "That game isn't on your label."
    if (Research.MARKETING !in researched) return "Research marketing campaigns first."
    if (c in s.campaigns) return "Already running."
    if (cash < c.cost) return "Costs ${money(c.cost)}."
    return null
}

fun GameState.buyLabelCampaign(pitchId: Int, c: Campaign) {
    labelCampaignBlocker(pitchId, c)?.let { throw notAvailable(it) }
    val s = games!!.signed.first { it.pitch.id == pitchId }
    spend(c.cost, Lane.GAMES)
    s.campaigns += c
    s.hype = minOf(100.0, s.hype + c.hype)
    s.spent += c.cost
    addLog("${c.title} for ${s.title}. Hype is building.", LogTone.NEUTRAL, Lane.GAMES)
}

// The week

internal fun GameState.publishingWeek() {
    val g = games ?: return
    if (Research.PUBLISHING !in researched) return
    g.pitches.removeAll { it.expiresWeek < week }
    if (week % 3 == 0 && g.pitches.size < 3 && rng.chance(0.7)) {
        val p = newPitch()
        g.pitches += p
        if (g.pitches.size == 1) addLog("${p.developer} is pitching ${p.title}, ${Format.a(p.size.lower)} ${p.topic.title.lowercase()} ${p.genre.lower} game.", LogTone.NEUTRAL, Lane.GAMES)
    }
    for (s in g.signed.toList()) {
        s.weeksLeft -= 1
        s.hype *= 0.99
        if (!s.slipped && s.weeksLeft in 1..4 && rng.chance(0.06)) {
            val late = rng.int(2, 6)
            s.slipped = true
            s.weeksLeft += late
            s.totalWeeks += late
            addLog("${s.pitch.developer} needs ${Format.weeks(late)} more on ${s.title}.", LogTone.NEUTRAL, Lane.GAMES)
        }
        if (s.weeksLeft <= 0) {
            g.signed.remove(s)
            publish(s)
        }
    }
}

/** A signed game is done: the critics have their say and it goes on sale under your label. */
private fun GameState.publish(s: SignedGame) {
    val g = games!!
    val p = s.pitch
    val c = p.concept
    val ctx = context(Lane.GAMES)
    val base = (pitchQuality(p.talent, p.topic, p.genre, p.platforms) + rng.range(-0.7, 0.7)).clamped(1.5, 9.8)
    val onTrend = g.trend?.matches(c) == true
    val verdict = writeReviews(
        ReviewInputs(
            title = p.title, concept = c,
            focus = List(3) { stage -> FocusArea.forStage(stage).map { c.idealShare(it) * p.talent + (1 - p.talent) / 3 } },
            score = base, designRatio = c.idealDesignShare, focusMatch = 0.5 + 0.4 * p.talent,
            bugPenalty = 0.88 + 0.1 * p.talent, polish = 0.25 * p.talent,
            engineLevel = (engineStandard(week) - 2 + 3 * p.talent).toInt(), engineStandard = engineStandard(week),
            hype = s.hype, fanTrust = 1.0, onTrend = onTrend, repeated = false, rushedSequel = false, worthySequel = false,
            stepBack = false, record = false, wrongCrowd = false, awkwardPlatform = p.platforms.firstOrNull { it.fit(p.genre) == Fit.BAD },
            year = ctx.year,
        ),
        rng,
    )
    val avg = verdict.average
    val labelLift = 1 + 0.75 * g.fans / (g.fans + p.size.fanScale)
    val clash = g.clashFor(c, week)
    val gross = p.size.launchSales * (avg / 7).pow(3) * labelLift * ctx.marketDemand * (1 + s.hype / 80) *
        reach(p.platforms).pow(0.75).clamped(0.25, 2.0) * (if (onTrend) 1.12 else 1.0) * (clash?.penalty ?: 1.0)
    val share = Format.percent(p.labelShare)
    val lines = buildList {
        add("Made by ${p.developer} and published by you. You keep $share of what it earns after the platforms' cut.")
        add(
            when (p.demo) {
                1 -> "The demo was rough"
                2 -> "The demo was promising"
                else -> "The demo was polished"
            } + (p.lastScore?.let { " and their last game scored $it" } ?: " and it was their first game") + ". It came out at ${String.format(java.util.Locale.US, "%.1f", avg)}.",
        )
        add("${p.topic.title} + ${p.genre.title}: ${p.topic.fit(p.genre).title.lowercase()} combination.")
        if (s.campaigns.isNotEmpty()) add("Your marketing (${s.campaigns.joinToString { it.title.lowercase() }}) built ${s.hype.toInt()} hype for launch.")
        if (g.fans > 0) add("Your fans gave it a ${((labelLift - 1) * 100).roundToInt()}% lift at launch.")
        if (onTrend) add("Riding the ${g.trend?.title?.lowercase()} trend helped sales.")
        clash?.let { add("It launched next to ${it.game.title} and lost some buyers to it.") }
    }
    val release = GameRelease(
        id = g.nextId(), title = p.title, concept = c, week = week, reviews = verdict.reviews, score = avg, weeklySales = gross,
        netShare = 0.7 * p.labelShare, design = 0.0, tech = 0.0, bugs = (1 - p.talent) * p.size.work * 0.1, hype = s.hype,
        devCost = s.spent, report = lines, userScore = verdict.userScore, aspects = verdict.aspects, pros = verdict.pros,
        cons = verdict.cons, developer = p.developer,
    )
    g.published.add(0, release)
    val text = String.format(java.util.Locale.US, "%.1f", avg)
    when {
        avg >= 8 -> {
            g.fans += gross / c.unitPrice * 0.08
            addLog("Your label's ${p.title} by ${p.developer} is a hit: $text/10. Launch week: ${money(gross)}.", LogTone.MILESTONE, Lane.GAMES)
            publish(NewsDesk.YOU, techOutlet(Lane.GAMES, rng), "${p.title} review: ${p.developer} and $companyName find a gem", "$text/10.", LogTone.GOOD, Lane.GAMES)
        }
        avg >= 5 -> addLog("Your label released ${p.title} by ${p.developer}: $text/10. Launch week: ${money(gross)}.", LogTone.GOOD, Lane.GAMES)
        else -> addLog("${p.title} by ${p.developer} flopped at $text/10. Your label takes the hit.", LogTone.BAD, Lane.GAMES)
    }
}

/** What your label's games earned you last week, after the platforms and developers. */
val GamesStudio.labelWeekly: Double get() = published.sumOf { it.weeklySales * it.netShare }

/** Everything your label's games have earned you, less what you put in. */
val GamesStudio.labelProfit: Double get() = published.sumOf { it.lifetimeSales - it.devCost }
