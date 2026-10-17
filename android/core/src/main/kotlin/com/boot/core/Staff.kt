package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

@Serializable
enum class Role {
    ENGINEER, DESIGNER, RESEARCHER, MARKETER, OPERATOR;

    val title: String
        get() = when (this) {
            ENGINEER -> "Engineer"
            DESIGNER -> "Designer"
            RESEARCHER -> "Researcher"
            MARKETER -> "Marketer"
            OPERATOR -> "Operations"
        }

    /** Weekly salary at skill 5, before market adjustments. */
    internal val baseSalary: Double
        get() = when (this) {
            ENGINEER -> 2_200.0
            DESIGNER -> 1_900.0
            RESEARCHER -> 3_400.0
            MARKETER -> 1_700.0
            OPERATOR -> 1_400.0
        }
}

/** A personality: what someone's like to work with, for better and worse. Old hires have none. */
@Serializable
enum class Trait(val title: String, val detail: String) {
    PERFECTIONIST("Perfectionist", "Better quality, a little slower."),
    HUSTLER("Hustler", "Works fast, but needs more to stay happy."),
    NIGHT_OWL("Night owl", "Thrives in crunch: more output, no morale hit."),
    MENTOR("Mentor", "Everyone else on the team learns faster."),
    DIVA("Diva", "Brilliant and productive, hard to keep happy."),
    TEAM_PLAYER("Team player", "Lifts the morale of everyone on the team."),
    RAINMAKER("Rainmaker", "Brings in business: more demand."),
    LOYAL("Loyal", "Never quits or listens to rivals, and asks for smaller raises."),
    FAST_LEARNER("Fast learner", "Levels up twice as fast."),
    ;

    /** Multiplier on what they get done. */
    internal val output: Double
        get() = when (this) {
            PERFECTIONIST -> 0.9
            HUSTLER -> 1.15
            DIVA -> 1.25
            else -> 1.0
        }

    /** Where their morale settles, against everyone else's. */
    internal val settle: Double
        get() = when (this) {
            HUSTLER -> -0.06
            DIVA -> -0.12
            else -> 0.0
        }
}

/** Design, tech and research talent follow from a role: designers design, engineers build. */
internal fun roleDesign(role: Role, skill: Int) = (skill + when (role) {
    Role.DESIGNER -> 2; Role.MARKETER -> 0; Role.ENGINEER -> -2; Role.RESEARCHER -> -1; Role.OPERATOR -> -2
}).clamped(1, 10)

internal fun roleTech(role: Role, skill: Int) = (skill + when (role) {
    Role.ENGINEER -> 2; Role.RESEARCHER -> 1; Role.DESIGNER -> -2; Role.MARKETER -> -3; Role.OPERATOR -> -1
}).clamped(1, 10)

internal fun roleResearch(role: Role, skill: Int) = (skill + when (role) {
    Role.RESEARCHER -> 3; Role.ENGINEER -> 0; Role.DESIGNER -> -1; Role.MARKETER -> -3; Role.OPERATOR -> -3
}).clamped(1, 10)

@Serializable
data class Employee(
    val id: Int,
    val name: String,
    val role: Role,
    /** 1...10 */
    var skill: Int,
    /** Weekly. */
    var salary: Double,
    /** 0...1. Low morale cuts output and makes people quit. */
    var morale: Double,
    var lane: Lane?,
    val hiredWeek: Int,
    /** Game Dev Tycoon style talents, 1...10. */
    var design: Int = roleDesign(role, skill),
    var tech: Int = roleTech(role, skill),
    var research: Int = roleResearch(role, skill),
    var lastTrainedWeek: Int = -1_000,
    /** Experience from working. Enough of it and they level up. */
    var xp: Double = 0.0,
    var level: Int = 1,
    var lastRaiseWeek: Int = -1_000,
    val trait: Trait? = null,
    /** Away (a break, a course) until this week. They're paid but do no work. */
    var leaveUntil: Int = -1,
    /** What they're best at making, if anything. */
    var specialty: Specialty? = null,
    var specializedWeek: Int = -1_000,
) {
    internal val moraleFactor: Double get() = 0.5 + 0.5 * morale

    /** Experience needed for the next level. */
    val xpToNext: Double get() = 30.0 + 15.0 * level

    val maxLevel: Boolean get() = level >= MAX_LEVEL

    companion object {
        const val MAX_LEVEL = 10
    }

    internal fun output(lane: Lane): Double = skill * lane.weight(role) * moraleFactor * (trait?.output ?: 1.0)
}

@Serializable
data class Candidate(
    val id: Int,
    val name: String,
    val role: Role,
    val skill: Int,
    val askingSalary: Double,
    /** A rival studio is circling. If you don't hire soon, they're gone. */
    val contestedBy: String?,
    val expiresWeek: Int,
    val design: Int = roleDesign(role, skill),
    val tech: Int = roleTech(role, skill),
    val research: Int = roleResearch(role, skill),
    val trait: Trait? = null,
    val specialty: Specialty? = null,
)

@Serializable
class Founder(val name: String) {
    /** How you look, if you chose; otherwise a look of your own from your name. */
    var appearance: Appearance? = null

    /** Your special trait. */
    var trait: FounderTrait? = null

    /** A second trait, picked at level 5. */
    var trait2: FounderTrait? = null

    /** Experience toward the next level, from running the company. */
    var xp: Double = 0.0
    var level: Int = 1

    /** The skills you've learned with your skill points. */
    var skills: Set<FounderSkill> = emptySet()

    /** Weeks of hands-on experience per lane. This is the "skills" you keep when you pivot. */
    val experience: MutableMap<Lane, Double> = mutableMapOf()

    /** Share of attention per lane. Normalised when used. */
    val focus: MutableMap<Lane, Double> = mutableMapOf()

    /** Taking outside gigs to pay rent. Steady money, but the company gets a fraction of you. */
    var freelancing = false

    /** Weekly income from freelance gigs. Grows with experience. */
    fun freelanceIncome(week: Int): Double = 700.0 + 120.0 * min(10, week / 52)

    /** Experience bonus caps at +4 output after ~3 years in a lane. */
    fun skillBonus(lane: Lane): Double = 4 * (1 - exp(-(experience[lane] ?: 0.0) / 120))

    fun attentionShare(lane: Lane, lanes: List<Lane>): Double {
        if (lane !in lanes) return 0.0
        val total = lanes.sumOf { max(focus[it] ?: 1.0, 0.0) }
        if (total <= 0) return 1.0 / lanes.size
        return max(focus[lane] ?: 1.0, 0.0) / total
    }
}

/** What a lane gets from the people pointed at it this week. */
data class Staffing(
    var output: Double = 0.0,
    var marketing: Double = 0.0,
    var operations: Double = 0.0,
    var research: Double = 0.0,
    var headcount: Int = 0,
    var averageSkill: Double = 5.0,
    /** Output from employees alone, the part crunch can stretch. */
    var teamOutput: Double = 0.0,
    /** Team design and tech talent, 1...10, for making games. */
    var avgDesign: Double = 5.0,
    var avgTech: Double = 5.0,
    /** What the team's personalities add: quality points, and demand from rainmakers. */
    var traitQuality: Double = 0.0,
    var traitDemand: Double = 1.0,
)

internal object Names {
    val first = listOf(
        "Ada", "Bo", "Cass", "Dev", "Eli", "Fern", "Gus", "Hana", "Ines", "Jules", "Kai", "Lena",
        "Milo", "Nia", "Omar", "Pia", "Quinn", "Rafa", "Sol", "Tess", "Umi", "Vik", "Wren", "Xan",
        "Yara", "Zed", "Ari", "Bex", "Cy", "Dana", "Emeka", "Femi", "Gio", "Hal", "Ivy", "Jin",
    )
    val last = listOf(
        "Okafor", "Lindqvist", "Moreau", "Tanaka", "Reyes", "Novak", "Haddad", "Kowalski", "Osei",
        "Park", "Silva", "Ivanova", "Byrne", "Nakamura", "Petrov", "Mensah", "Dubois", "Castillo",
        "Yilmaz", "Akhtar", "Fischer", "Rossi", "Nguyen", "Adeyemi", "Larsen", "Cohen",
    )
    val clients = listOf(
        "Bramble & Co", "Cask Brewery", "Dentally", "Eastside Gym", "Fernhill Florist", "Gourmand Bistro",
        "Harbor Realty", "Inkwell Press", "Juniper Yoga", "Kettle Cafe", "Loom Textiles", "Mason Law",
        "Northwind Travel", "Orchard Clinic", "Pine & Peak Outfitters", "Quarry Climbing", "Riverbend Vet",
        "Saltwater Surf", "Tidy Home Cleaning", "Umber Gallery", "Vela Bikes", "Wick Candles", "Yonder Tours",
        "Zest Juicery", "Acme Logistics", "Bolt Couriers", "Civic Credit Union", "Driftwood Hotels",
    )
    /** Big brands, for the agency's biggest briefs. */
    val brands = listOf(
        "Nike", "Spotify", "Airbnb", "Tesla", "Coca-Cola", "IKEA", "Starbucks", "Netflix", "Adidas", "Red Bull",
        "LEGO", "Shopify", "Patagonia", "Uber", "Duolingo", "Peloton", "Sephora", "Delta Air Lines",
    )
    val games = listOf(
        "Moss Knight", "Neon Courier", "Tiny Tactics", "Hollow Lantern", "Pocket Kingdom", "Starfold",
        "Rust & Rhyme", "Deep Shift", "Paper Moon Racing", "Last Orchard", "Glitch Garden", "Cinder Saga",
        "Overclocked", "Salt Lake Raiders", "Velvet Dungeon", "Echo Drift", "Bramblewood", "Hyperlane",
        "Iron Picnic", "Quiet Planet", "Crab Empire", "Sky Laundry", "Mythic Commute", "Boss Rush Hour",
    )
    val apps = listOf(
        "Tally", "Nudge", "Sprout", "Pocketlog", "Habitat", "Driftnote", "Snapcart", "Quill", "Loop",
        "Mealwise", "Stride", "Budgie", "Focusly", "Glimmer", "Plotter", "Waypoint",
    )
    val socials = listOf("Chirp", "Hive", "Murmur", "Campfire", "Loopd", "Echo", "Plaza", "Huddle")
    val devices = listOf("Arc", "Pebble", "Halo", "Nova", "Slate", "Ember", "Prism", "Orbit", "Volt", "Aura")
    val models = listOf("Sage", "Atlas", "Muse", "Lumen", "Oracle", "Scribe", "Echo", "Vector")
    val startups = listOf("Fieldnote", "Brightwire", "Tinker Labs", "Northstar Bits", "Paperplane", "Quokka", "Kiln", "Lantern & Co", "Bitwise", "Moonshot Garage")
    val angels = listOf("Elena Marsh", "Raj Patel", "Sofia Lindgren", "Marcus Bell", "Yuki Sato", "Amara Okoye", "Tom Hadley")
}
