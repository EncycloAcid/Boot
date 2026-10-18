package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

/*
 * Eras that play differently, not just shift the numbers.
 * - A challenge: each era sets a goal in its own spirit (survive the crash, triple your value in
 *   the boom, ship a hit in the console wars). Beat it before the era ends for a reward.
 * - A moment: at the era's height, a one-off decision that only happens then.
 * - Trends: what gamers want follows the era.
 * - A review: when an era ends, the paper looks back on how you did, and the timeline keeps it.
 */

/** What an era's challenge asks for. */
@Serializable
enum class ChallengeKind { EARN, VALUE_GROWTH, PROFIT_STREAK, HIT, ERA_TECH, HEADCOUNT }

/** An era's goal: beat it before the era ends. */
@Serializable
data class EraChallenge(
    val era: Era,
    val kind: ChallengeKind,
    val target: Double,
    val title: String,
    val detail: String,
    val reward: Double,
    var done: Boolean = false,
)

/** The challenges an era prefers, best first. The first that suits your company is the one you get. */
private fun Era.challengeKinds(): List<ChallengeKind> = when (this) {
    Era.BOOTSTRAP_YEARS -> emptyList()
    Era.HOME_COMPUTERS, Era.CONSOLE_WARS, Era.RETRO_REVIVAL -> listOf(ChallengeKind.HIT, ChallengeKind.EARN)
    Era.VIDEO_GAME_CRASH, Era.DOT_COM_BUST, Era.CRYPTO_WINTER, Era.METAVERSE_HANGOVER -> listOf(ChallengeKind.PROFIT_STREAK)
    Era.MULTIMEDIA, Era.SMARTPHONES, Era.VR_HYPE_CYCLE, Era.CLOUD_GAMING, Era.CHIP_SHORTAGE -> listOf(ChallengeKind.ERA_TECH, ChallengeKind.EARN)
    Era.DOT_COM_BOOM, Era.AI_GOLD_RUSH -> listOf(ChallengeKind.VALUE_GROWTH)
    Era.REMOTE_WORK_BOOM -> listOf(ChallengeKind.HEADCOUNT)
    else -> listOf(ChallengeKind.EARN)
}

private fun Double.roundMoney(): Double = if (this >= 100_000) Math.round(this / 10_000.0) * 10_000.0 else Math.round(this / 1_000.0) * 1_000.0

/** This era's challenge for this company, or null if there's none. */
private fun GameState.makeChallenge(): EraChallenge? {
    val era = world.era
    val years = max(1.0, world.eraLength / 52.0)
    val reward = max(50_000.0, stake(null) * 6).roundMoney()
    val techable = era.research.any { it.kind != Research.Kind.ENGINE || games != null }
    val kind = era.challengeKinds().firstOrNull { k ->
        when (k) {
            ChallengeKind.HIT -> games != null
            ChallengeKind.ERA_TECH -> techable
            else -> true
        }
    } ?: return null
    return when (kind) {
        ChallengeKind.EARN -> {
            val target = max(150_000.0, trailingRevenue(null, 52) * years * 1.2).roundMoney()
            EraChallenge(era, kind, target, "Earn ${money(target)} this era", "Make the most of ${era.inSentence}: revenue from everything you run counts.", reward)
        }
        ChallengeKind.VALUE_GROWTH -> {
            val target = max(2_000_000.0, valuation * 2.5).roundMoney()
            EraChallenge(era, kind, target, "Be worth ${money(target)}", "Money is cheap and investors are wild. Ride ${era.inSentence} to a far bigger valuation.", reward)
        }
        ChallengeKind.PROFIT_STREAK -> EraChallenge(
            era, kind, 26.0, "Stay profitable for six months straight", "Plenty of companies won't survive ${era.inSentence}. Be one that does.", reward,
        )
        ChallengeKind.HIT -> EraChallenge(era, kind, 1.0, "Ship a hit", "Release a game that scores 8 or more while ${era.inSentence} lasts.", reward)
        ChallengeKind.ERA_TECH -> EraChallenge(
            era, kind, 1.0, "Master the new tech", "Research ${era.research.joinToString(" or ") { it.inSentence }} before ${era.inSentence} ends.", reward,
        )
        ChallengeKind.HEADCOUNT -> {
            val target = (staff.size + max(3, staff.size / 2)).toDouble()
            EraChallenge(era, kind, target, "Grow the team to ${target.toInt()}", "Everyone can work from anywhere now. Hire the people you couldn't before.", reward)
        }
    }
}

/** How far along the era's challenge is, 0...1. */
fun GameState.challengeProgress(c: EraChallenge): Double {
    if (c.done || c.target <= 0) return 1.0
    val p = when (c.kind) {
        ChallengeKind.EARN -> (stats.totalRevenue - world.eraStartRevenue) / c.target
        ChallengeKind.VALUE_GROWTH -> valuation / c.target
        ChallengeKind.PROFIT_STREAK -> stats.profitableStreak / c.target
        ChallengeKind.HIT -> (games?.releases?.count { it.week >= world.eraStartWeek && it.score >= 8 && it.publisher == null } ?: 0) / c.target
        ChallengeKind.ERA_TECH -> if (c.era.research.any { it in researched }) 1.0 else 0.0
        ChallengeKind.HEADCOUNT -> staff.size / c.target
    }
    return p.coerceIn(0.0, 1.0)
}

/** A new era starts: note where you stand, and set its challenge. */
internal fun GameState.beginEra() {
    world.eraStartValue = valuation
    world.eraStartRevenue = stats.totalRevenue
    world.momentDone = false
    world.challenge = makeChallenge()
    world.challenge?.let { addLog("${world.era.title} challenge: ${it.title.replaceFirstChar { c -> c.lowercase() }}. ${it.detail}", LogTone.MILESTONE) }
}

/** The era ends: how did you do? Returns its place in the timeline. */
internal fun GameState.closeEra(): EraSpan {
    val w = world
    val earned = stats.totalRevenue - w.eraStartRevenue
    val c = w.challenge
    val span = EraSpan(
        w.era, w.eraStartWeek, week, revenue = earned, valueStart = w.eraStartValue, valueEnd = valuation,
        challenge = c?.title, challengeWon = c?.done,
    )
    if (w.era != Era.BOOTSTRAP_YEARS) {
        val verdict = when {
            c?.done == true -> "You met its challenge."
            c != null -> "You missed its challenge: ${c.title.replaceFirstChar { it.lowercase() }}."
            else -> ""
        }
        val body = "$companyName earned ${money(earned)} and went from ${money(w.eraStartValue)} to ${money(valuation)}. $verdict".trim()
        addLog("${w.era.title} is over. $body", if (valuation >= w.eraStartValue) LogTone.GOOD else LogTone.BAD)
        publish(NewsDesk.YOU, "The Economist", "${w.era.title} in review: how $companyName did", body, if (valuation >= w.eraStartValue) LogTone.GOOD else LogTone.BAD)
    }
    w.challenge = null
    return span
}

/** Each week: has the challenge been met, and is it time for the era's moment? */
internal fun GameState.eraWeek() {
    val w = world
    w.challenge?.let { c ->
        if (!c.done && challengeProgress(c) >= 1.0) {
            c.done = true
            stats.eraChallengesWon += 1
            cash += c.reward
            recordRevenue(c.reward, null)
            researchPoints += 25.0
            reputation.adjust(3.0)
            addLog("Era challenge met: ${c.title.replaceFirstChar { it.lowercase() }}. ${money(c.reward)}, 25 research points and a reputation boost.", LogTone.MILESTONE)
            publish(NewsDesk.YOU, techOutlet(null, rng), "$companyName rides ${w.era.inSentence}", c.title + ".", LogTone.GOOD)
        }
    }
    if (!w.momentDone && w.phase == EraPhase.PEAK && w.era != Era.BOOTSTRAP_YEARS && week > 26) {
        w.momentDone = true
        enqueue(EraMoment(w.era))
    }
}

/** What gamers want in an era, or null to let the market wander. */
internal fun GameState.eraTrend(): Trend? = when (world.era) {
    Era.HOME_COMPUTERS -> if (rng.chance(0.5)) Trend(topic = rng.pick(listOf(Topic.SPACE, Topic.ALIENS, Topic.FANTASY)), until = 0) else Trend(genre = Genre.ADVENTURE, until = 0)
    Era.VIDEO_GAME_CRASH -> Trend(genre = rng.pick(listOf(Genre.STRATEGY, Genre.ADVENTURE)), until = 0)
    Era.CONSOLE_WARS -> if (rng.chance(0.5)) Trend(topic = rng.pick(listOf(Topic.NINJA, Topic.FANTASY, Topic.SPORTS)), until = 0) else Trend(genre = Genre.ACTION, until = 0)
    Era.MULTIMEDIA -> Trend(topic = rng.pick(listOf(Topic.HORROR, Topic.DETECTIVE, Topic.SCI_FI, Topic.MILITARY)), until = 0)
    Era.DOT_COM_BOOM -> if (rng.chance(0.5)) Trend(genre = Genre.SIMULATION, until = 0) else Trend(topic = rng.pick(listOf(Topic.CITY, Topic.HACKING)), until = 0)
    Era.DOT_COM_BUST -> Trend(topic = rng.pick(listOf(Topic.MILITARY, Topic.POST_APOCALYPTIC)), until = 0)
    Era.WEB_2_0 -> if (rng.chance(0.5)) Trend(topic = rng.pick(listOf(Topic.FARMING, Topic.PETS, Topic.MUSIC)), until = 0) else Trend(genre = Genre.CASUAL, until = 0)
    Era.SMARTPHONES -> if (rng.chance(0.6)) Trend(genre = Genre.CASUAL, until = 0) else Trend(topic = rng.pick(listOf(Topic.PETS, Topic.COOKING)), until = 0)
    Era.FREE_TO_PLAY -> if (rng.chance(0.5)) Trend(genre = Genre.STRATEGY, until = 0) else Trend(topic = rng.pick(listOf(Topic.MEDIEVAL, Topic.SUPERHEROES, Topic.ZOMBIES)), until = 0)
    Era.AI_GOLD_RUSH -> Trend(topic = rng.pick(listOf(Topic.ARTIFICIAL_INTELLIGENCE, Topic.ROBOTS, Topic.CYBERPUNK)), until = 0)
    Era.VR_HYPE_CYCLE -> Trend(genre = Genre.ACTION, until = 0)
    Era.RETRO_REVIVAL -> Trend(genre = Genre.CASUAL, until = 0)
    Era.CREATOR_ECONOMY -> Trend(topic = rng.pick(listOf(Topic.MUSIC, Topic.FASHION, Topic.COOKING)), until = 0)
    Era.PRIVACY_PANIC -> Trend(topic = rng.pick(listOf(Topic.HACKING, Topic.SPY)), until = 0)
    Era.CRYPTO_WINTER -> Trend(topic = Topic.BUSINESS, until = 0)
    Era.REMOTE_WORK_BOOM -> Trend(topic = rng.pick(listOf(Topic.FARMING, Topic.COOKING, Topic.PETS)), until = 0)
    Era.METAVERSE_HANGOVER -> Trend(topic = rng.pick(listOf(Topic.MYSTERY, Topic.TIME_TRAVEL)), until = 0)
    Era.CLOUD_GAMING -> Trend(genre = rng.pick(listOf(Genre.ACTION, Genre.RPG)), until = 0)
    Era.CHIP_SHORTAGE -> Trend(topic = rng.pick(listOf(Topic.ROBOTS, Topic.MARS, Topic.SPACE)), until = 0)
    Era.SUPER_APPS -> Trend(topic = Topic.BUSINESS, until = 0)
    else -> null
}

// The era's moment.

/** What a moment's deals cost and pay: a few weeks of revenue, small for a small company. */
internal val GameState.momentStake: Double get() = max(5_000.0, trailingRevenue(null, 4) / 4)

@Serializable @SerialName("eraMoment")
data class EraMoment(val era: Era) : EventKind

internal fun GameState.describeMoment(k: EraMoment): Description? {
    val s = momentStake
    val games = games != null
    return when (k.era) {
        Era.HOME_COMPUTERS -> Description(
            "A magazine wants to print your code",
            "A computer magazine wants to print one of your programs as a type-in listing. Thousands of kids would type it in by hand.",
            listOf(choice("Let them print it", "Fame and goodwill: reputation and research points."), choice("Sell them the rights", "${money(s * 2)}.")),
        )
        Era.VIDEO_GAME_CRASH -> if (!games) null else Description(
            "Shops are sending games back",
            "Retailers are drowning in unsold cartridges and want to return yours for a refund.",
            listOf(choice("Take the returns", "${money(s * 3)}. Shops will remember you played fair."), choice("Refuse", "Keep the money. Retailers won't forget.")),
            Lane.GAMES,
        )
        Era.CONSOLE_WARS -> if (!games) null else Description(
            "A console maker wants you exclusive",
            "One of the console makers offers marketing money if your next games are theirs alone.",
            listOf(choice("Sign the exclusive", "+8% game sales for a year, and ${money(s * 2)} up front."), choice("Stay on every platform", "Keep your options open.")),
            Lane.GAMES,
        )
        Era.MULTIMEDIA -> Description(
            "Go all-in on CD-ROM?",
            "Discs hold a thousand times more than floppies. Retooling everything costs money and makes the team more productive for a year.",
            listOf(choice("Go all-in", "${money(s * 3)}. 10% more done for a year."), choice("Wait and see", "Nothing changes.")),
        )
        Era.DOT_COM_BOOM -> Description(
            "A banker says you could raise on a napkin",
            "An investment bank will put in ${money(valuation * 0.25)} for a fifth of the company, no questions asked. It's mad money.",
            listOf(choice("Take it", "${money(valuation * 0.25)} for 20% of the company."), choice("Pass", "Keep your company.")),
        )
        Era.DOT_COM_BUST -> Description(
            "A failed dot-com's team is going cheap",
            "A startup that raised millions just shut down. Three of its best people would join you for a song.",
            listOf(choice("Hire them", "${money(s)}. Three experienced hires." + if (freeDesks < 3) " You're short on desks." else ""), choice("Pass", "Not now.")),
        )
        Era.WEB_2_0 -> Description(
            "Put a share button on everything?",
            "Everyone's adding share buttons and sign-in-with-everything. It brings in people, and data you'd rather not be holding.",
            listOf(choice("Share everything", "+5% demand everywhere for six months. A small privacy risk."), choice("Keep it simple", "Nothing changes.")),
        )
        Era.SMARTPHONES -> if (!games && apps == null) null else Description(
            "You're invited to the App Store launch",
            "Apple wants a handful of developers on stage for the App Store's first day. Building for it means dropping everything for a few weeks.",
            listOf(choice("Be there on day one", "${money(s * 2)} and a busy month. +10% sales for a year."), choice("Wait for version two", "Nothing changes.")),
        )
        Era.FREE_TO_PLAY -> if (!games) null else Description(
            "Make your games free?",
            "Free-to-play is printing money. Give the game away and sell gems: more players, more money, and some people hate it.",
            listOf(choice("Go free-to-play", "+10% game sales for a year. Some reputation lost."), choice("Keep charging for games", "Nothing changes.")),
            Lane.GAMES,
        )
        Era.AI_GOLD_RUSH -> Description(
            "An AI lab wants to license your data",
            "One of the big labs will pay ${money(s * 5)} for everything you've ever collected, to train on.",
            listOf(choice("License it", "${money(s * 5)}. People may not love it."), choice("Decline", "Keep your data.")),
        )
        Era.VR_HYPE_CYCLE -> Description(
            "A headset maker will pay you to build for VR",
            "${money(s * 3)} to make something for their headset. It'll tie up part of the team for three months.",
            listOf(choice("Take the money", "${money(s * 3)}, 10% less done for three months."), choice("Pass", "Nothing changes.")),
        )
        Era.CRYPTO_WINTER -> Description(
            "A client wants to pay in tokens",
            "A crypto client owes you ${money(s * 2)} and wants to pay in its own tokens. They might recover. They might not.",
            listOf(choice("Take the tokens", "A coin flip: double or nothing."), choice("Insist on cash", "${money(s)} now, and they'll grumble.")),
        )
        Era.REMOTE_WORK_BOOM -> Description(
            "Give up part of the office?",
            "Half the desks are empty most days. Your landlord would take a chunk of the lease back.",
            listOf(choice("Hand it back", "${money(headquarters.rent * 20)} saved. The office feels emptier."), choice("Keep it", "Nothing changes.")),
        )
        Era.PRIVACY_PANIC -> Description(
            "Regulators want an audit",
            "A new privacy law means an audit of everything you store. Do it properly, or stall and hope they're busy.",
            listOf(choice("Full audit", "${money(s * 2)}. Much less risk for a year."), choice("Stall", "Free. They might come back angrier.")),
        )
        Era.CREATOR_ECONOMY -> Description(
            "A top creator wants a sponsorship",
            "One of the biggest creators online offers to feature you for ${money(s * 1.5)}.",
            listOf(choice("Sponsor them", "${money(s * 1.5)}. +6% demand for six months."), choice("Pass", "Nothing changes.")),
        )
        Era.RETRO_REVIVAL -> Description(
            "Collectors want your early work",
            "Retro collectors are paying silly money for prototypes and first editions. Yours included.",
            listOf(choice("Auction the old prototypes", "${money(s * 2)}."), choice("Keep them", "Some things aren't for sale. The team likes that.")),
        )
        Era.METAVERSE_HANGOVER -> Description(
            "A metaverse studio is selling up",
            "A virtual-world startup is selling its gear and research for pennies on the dollar.",
            listOf(choice("Buy it", "${money(s)}. 40 research points."), choice("Pass", "Nothing changes.")),
        )
        Era.CHIP_SHORTAGE -> Description(
            "A broker has chips, at a price",
            "A broker can get you the parts everyone's short of, for three times the going rate.",
            listOf(choice("Pay up", "${money(s * 2)}. No shortages for you this year."), choice("Wait it out", "Costs stay high.")),
        )
        Era.CLOUD_GAMING -> if (!games) null else Description(
            "A cloud service wants your games",
            "A streaming service wants your catalogue on day one, for a cut of the subscription money.",
            listOf(choice("Sign up", "${money(s * 2)} up front and +5% sales for a year."), choice("Keep them off", "Nothing changes.")),
            Lane.GAMES,
        )
        Era.SUPER_APPS -> Description(
            "Join a super-app",
            "One of the super-apps wants you inside it as a mini-app: their billion users, their rules.",
            listOf(choice("Join", "+8% demand for a year. They take a cut."), choice("Stay independent", "Nothing changes.")),
        )
        else -> null
    }
}

/** Boost every lane you run for a while: your own campaign or deal. */
private fun GameState.boostAll(title: String, demand: Double, weeks: Int, output: Double = 1.0, cost: Double = 1.0) {
    for (lane in lanes) world.pressures += Pressure(lane, title, demand, weeks, cost = cost, output = output, mine = true)
}

internal fun GameState.resolveMoment(k: EraMoment, choice: Int) {
    val s = momentStake
    val yes = choice == 0
    when (k.era) {
        Era.HOME_COMPUTERS -> if (yes) {
            reputation.adjust(3.0)
            researchPoints += 15.0
            addLog("Your code is in the magazine. Kids everywhere are typing it in.", LogTone.GOOD)
        } else {
            cash += s * 2
            recordRevenue(s * 2, null)
        }
        Era.VIDEO_GAME_CRASH -> if (yes) {
            spend(s * 3, Lane.GAMES)
            reputation.adjust(3.0)
            addLog("You took the returns. Shops will remember who stood by them.", LogTone.NEUTRAL, Lane.GAMES)
        } else {
            reputation.adjust(-4.0)
            world.pressures += Pressure(Lane.GAMES, "Shops are wary of you", 0.92, 26)
        }
        Era.CONSOLE_WARS -> if (yes) {
            cash += s * 2
            recordRevenue(s * 2, Lane.GAMES)
            world.pressures += Pressure(Lane.GAMES, "Console exclusive", 1.08, 52, mine = true)
        }
        Era.MULTIMEDIA -> if (yes) {
            spend(s * 3, null)
            boostAll("All-in on CD-ROM", 1.0, 52, output = 1.1)
        }
        Era.DOT_COM_BOOM -> if (yes) {
            val amount = valuation * 0.25
            cash += amount
            venture.founderEquity *= 0.8
            addLog("The bank put in ${money(amount)} for a fifth of the company. Mad money.", LogTone.GOOD)
        }
        Era.DOT_COM_BUST -> if (yes) {
            spend(s, null)
            repeat(3) {
                val c = makeCandidate().let { it.copy(skill = rng.int(6, 8)) }
                hireDirect(c.copy(contestedBy = null))
            }
            addLog("Three people from a failed dot-com joined you.", LogTone.GOOD)
        }
        Era.WEB_2_0 -> if (yes) {
            boostAll("Share buttons everywhere", 1.05, 26)
            if (rng.chance(0.25)) reputation.scar(ScarKind.PRIVACY, 0.1, week, "Shared more data than people knew.")
        }
        Era.SMARTPHONES -> if (yes) {
            spend(s * 2, null)
            for (lane in lanes.filter { it == Lane.GAMES || it == Lane.APPS }) world.pressures += Pressure(lane, "On the App Store from day one", 1.1, 52, mine = true)
            world.pressures += Pressure(lanes.first(), "Building for the App Store", 1.0, 4, output = 0.8, mine = true)
        }
        Era.FREE_TO_PLAY -> if (yes) {
            world.pressures += Pressure(Lane.GAMES, "Free-to-play", 1.1, 52, mine = true)
            reputation.adjust(-2.0)
        }
        Era.AI_GOLD_RUSH -> if (yes) {
            cash += s * 5
            recordRevenue(s * 5, null)
            if (rng.chance(0.35)) reputation.scar(ScarKind.PRIVACY, 0.2, week, "Sold customer data to train an AI.")
        }
        Era.VR_HYPE_CYCLE -> if (yes) {
            cash += s * 3
            recordRevenue(s * 3, null)
            boostAll("Building for VR", 1.0, 13, output = 0.9)
        }
        Era.CRYPTO_WINTER -> if (yes) {
            if (rng.chance(0.5)) {
                cash += s * 4
                recordRevenue(s * 4, null)
                addLog("The tokens recovered. You cashed out at double.", LogTone.GOOD)
            } else {
                addLog("The tokens are worthless.", LogTone.BAD)
            }
        } else {
            cash += s
            recordRevenue(s, null)
        }
        Era.REMOTE_WORK_BOOM -> if (yes) {
            cash += headquarters.rent * 20
            for (e in staff) e.morale = max(0.0, e.morale - 0.03)
        }
        Era.PRIVACY_PANIC -> if (yes) {
            spend(s * 2, null)
            boostAll("Privacy audit done", 1.02, 52)
        } else if (rng.chance(0.4)) {
            later(rng.int(8, 20), Consequence.BreachExposed(lanes.first(), (s * 4).roundMoney()))
        }
        Era.CREATOR_ECONOMY -> if (yes) {
            spend(s * 1.5, null)
            boostAll("Creator sponsorship", 1.06, 26)
        }
        Era.RETRO_REVIVAL -> if (yes) {
            cash += s * 2
            recordRevenue(s * 2, null)
        } else {
            for (e in staff) e.morale = minOf(1.0, e.morale + 0.05)
        }
        Era.METAVERSE_HANGOVER -> if (yes) {
            spend(s, null)
            researchPoints += 40.0
        }
        Era.CHIP_SHORTAGE -> if (yes) {
            spend(s * 2, null)
            boostAll("Chips secured", 1.0, 52, cost = 0.9)
        }
        Era.CLOUD_GAMING -> if (yes) {
            cash += s * 2
            recordRevenue(s * 2, Lane.GAMES)
            world.pressures += Pressure(Lane.GAMES, "On the cloud service", 1.05, 52, mine = true)
        }
        Era.SUPER_APPS -> if (yes) boostAll("Inside a super-app", 1.08, 52, cost = 1.03)
        else -> {}
    }
}
