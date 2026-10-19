package com.boot.core

import kotlinx.serialization.Serializable

/**
 * The industry's moods. Each era lifts some businesses and squeezes others, moves salaries, GPU
 * prices, hardware costs and how freely investors write cheques. Eras arrive gradually (a dawn),
 * peak, and fade; while one fades, the next is already being talked about.
 */
@Serializable
enum class Era {
    BOOTSTRAP_YEARS,
    AI_GOLD_RUSH,
    VR_HYPE_CYCLE,
    CRYPTO_WINTER,
    REMOTE_WORK_BOOM,
    PRIVACY_PANIC,
    CREATOR_ECONOMY,
    RETRO_REVIVAL,
    METAVERSE_HANGOVER,
    CHIP_SHORTAGE,
    CLOUD_GAMING,
    SUPER_APPS,

    // History, for runs that start in the past.
    HOME_COMPUTERS,
    VIDEO_GAME_CRASH,
    CONSOLE_WARS,
    MULTIMEDIA,
    DOT_COM_BOOM,
    DOT_COM_BUST,
    WEB_2_0,
    SMARTPHONES,
    FREE_TO_PLAY;

    /** An era of the past: it only comes round in a run that starts before it. */
    val historical: Boolean get() = ordinal >= HOME_COMPUTERS.ordinal

    val title: String
        get() = when (this) {
            BOOTSTRAP_YEARS -> "The Bootstrap Years"
            AI_GOLD_RUSH -> "The AI Gold Rush"
            VR_HYPE_CYCLE -> "The VR Hype Cycle"
            CRYPTO_WINTER -> "Crypto Winter"
            REMOTE_WORK_BOOM -> "The Remote Work Boom"
            PRIVACY_PANIC -> "The Privacy Panic"
            CREATOR_ECONOMY -> "The Creator Economy"
            RETRO_REVIVAL -> "The Retro Revival"
            METAVERSE_HANGOVER -> "The Metaverse Hangover"
            CHIP_SHORTAGE -> "The Chip Shortage"
            CLOUD_GAMING -> "The Cloud Gaming Push"
            SUPER_APPS -> "The Super-App Era"
            HOME_COMPUTERS -> "The Home Computer Revolution"
            VIDEO_GAME_CRASH -> "The Video Game Crash"
            CONSOLE_WARS -> "The 8-Bit Console Wars"
            MULTIMEDIA -> "The Multimedia Age"
            DOT_COM_BOOM -> "The Dot-com Boom"
            DOT_COM_BUST -> "The Dot-com Bust"
            WEB_2_0 -> "Web 2.0"
            SMARTPHONES -> "The Smartphone Revolution"
            FREE_TO_PLAY -> "The Free-to-Play Boom"
        }

    val emoji: String
        get() = when (this) {
            BOOTSTRAP_YEARS -> "🏚️"
            AI_GOLD_RUSH -> "🤖"
            VR_HYPE_CYCLE -> "🥽"
            CRYPTO_WINTER -> "🥶"
            REMOTE_WORK_BOOM -> "🏡"
            PRIVACY_PANIC -> "🕵️"
            CREATOR_ECONOMY -> "🎥"
            RETRO_REVIVAL -> "👾"
            METAVERSE_HANGOVER -> "🤕"
            CHIP_SHORTAGE -> "🔌"
            CLOUD_GAMING -> "☁️"
            SUPER_APPS -> "📲"
            HOME_COMPUTERS -> "🖥️"
            VIDEO_GAME_CRASH -> "📉"
            CONSOLE_WARS -> "🕹️"
            MULTIMEDIA -> "💿"
            DOT_COM_BOOM -> "🌐"
            DOT_COM_BUST -> "💥"
            WEB_2_0 -> "🧩"
            SMARTPHONES -> "📱"
            FREE_TO_PLAY -> "🎁"
        }

    val blurb: String
        get() = when (this) {
            BOOTSTRAP_YEARS -> "Nobody's watching yet. Rent is due."
            AI_GOLD_RUSH -> "Every pitch deck says 'agentic'. GPUs cost more than houses. Clients want chatbots on their bakery sites."
            VR_HYPE_CYCLE -> "Headsets are the future, again. Gamers and gadget buyers are throwing money around."
            CRYPTO_WINTER -> "The coins crashed. Investors are skittish, ad budgets shrank, and miners are dumping cheap GPUs."
            REMOTE_WORK_BOOM -> "Everyone needs a website, a productivity app and a better laptop."
            PRIVACY_PANIC -> "Regulators and journalists are hunting for data scandals. Social and AI feel the heat."
            CREATOR_ECONOMY -> "Everyone's a creator. Platforms and storefronts boom."
            RETRO_REVIVAL -> "Pixel art is back. Small games sell; shiny gadgets less so."
            METAVERSE_HANGOVER -> "Remember the metaverse? Investors do, unfortunately."
            CHIP_SHORTAGE -> "Factories can't make chips fast enough. Devices cost more to build and GPUs are scarce."
            CLOUD_GAMING -> "Games stream from the cloud to anything with a screen. Consoles feel old; game studios are in demand."
            SUPER_APPS -> "One app to chat, pay, shop and order dinner. Apps and social boom; websites feel quaint."
            HOME_COMPUTERS -> "Computers are coming home: the Apple II, the TRS-80, the Commodore PET. Hobbyists type in games from magazines."
            VIDEO_GAME_CRASH -> "Too many consoles and too many bad games. Shops can't give cartridges away, and nobody wants to fund a game studio."
            CONSOLE_WARS -> "A console from Japan brought games back from the dead. Plumbers and hedgehogs fight for the living room."
            MULTIMEDIA -> "CD-ROMs, sound cards, 16 bits and the first 3D worlds. Every family wants a multimedia PC."
            DOT_COM_BOOM -> "Everyone needs a website, and anything ending in .com can go public. Investors have never been so generous."
            DOT_COM_BUST -> "The bubble burst. Investors want profits now, and websites are a dirty word."
            WEB_2_0 -> "Blogs, wikis and everyone sharing everything. Social networks spring up in dorm rooms."
            SMARTPHONES -> "A phone with no keyboard changed everything. There's an app for that."
            FREE_TO_PLAY -> "Games are free and the gems cost money. Mobile games out-earn consoles, and everyone has a phone."
        }

    /** What people say about it before it arrives. */
    val rumor: String
        get() = when (this) {
            BOOTSTRAP_YEARS -> "Things are quiet."
            AI_GOLD_RUSH -> "Every demo at the last conference had a chatbot in it. VCs are asking about GPUs."
            VR_HYPE_CYCLE -> "Three big companies are rumoured to be working on headsets."
            CRYPTO_WINTER -> "Coin prices are wobbling, and some investors are quietly selling."
            REMOTE_WORK_BOOM -> "Offices are emptying out on Fridays, and then Thursdays."
            PRIVACY_PANIC -> "A journalist is said to be sitting on a big data-leak story."
            CREATOR_ECONOMY -> "Teenagers say they want to be creators when they grow up."
            RETRO_REVIVAL -> "Pixel art keeps topping the charts, and nobody knows why."
            METAVERSE_HANGOVER -> "Analysts are asking where all the metaverse money went."
            CHIP_SHORTAGE -> "A fab fire and a busy shipping season have chip buyers worried."
            CLOUD_GAMING -> "Every big tech company seems to be building data centres for games."
            SUPER_APPS -> "Messaging apps keep adding payments, shops and taxis."
            HOME_COMPUTERS -> "Garage tinkerers say there'll be a computer in every home."
            VIDEO_GAME_CRASH -> "Shops are piling up unsold cartridges, and the console makers are cutting prices."
            CONSOLE_WARS -> "A toy company from Kyoto is test-marketing a console in New York."
            MULTIMEDIA -> "Developers are excited about a shiny disc that holds 650 megabytes."
            DOT_COM_BOOM -> "Something called the World Wide Web is spreading out of the universities."
            DOT_COM_BUST -> "Analysts are starting to ask how the dot-coms will ever make money."
            WEB_2_0 -> "College students are signing up for a network called TheFacebook."
            SMARTPHONES -> "Apple is rumoured to be making a phone."
            FREE_TO_PLAY -> "A game about matching candies is making millions a day, and it's free."
        }

    /** Headlines while it arrives, at its height, and as it fades. */
    val dawn: List<String>
        get() = when (this) {
            BOOTSTRAP_YEARS -> listOf("Small startups find it hard to get noticed", "Coffee shops fill with founders and laptops")
            AI_GOLD_RUSH -> listOf("Startups add \"AI\" to their names and double their valuations", "GPU prices tick up as labs place huge orders", "Every keynote this week was about AI")
            VR_HYPE_CYCLE -> listOf("A new headset sells out in hours", "Game studios quietly start VR projects", "\"This time VR is for real\", say the people selling it")
            CRYPTO_WINTER -> listOf("Coins fall for a third straight week", "Crypto exchanges announce layoffs", "Investors get choosier about who they fund")
            REMOTE_WORK_BOOM -> listOf("Half of office workers now work from home some days", "Laptop sales jump", "Small businesses rush to get online")
            PRIVACY_PANIC -> listOf("A major data leak exposes millions of accounts", "Regulators promise a crackdown on data brokers", "Users start reading privacy policies, briefly")
            CREATOR_ECONOMY -> listOf("A teenager's channel passes 50 million subscribers", "Platforms launch creator funds", "Brands move ad money to influencers")
            RETRO_REVIVAL -> listOf("A pixel-art indie outsells a blockbuster", "Old consoles fetch record prices online", "Chiptune albums chart")
            METAVERSE_HANGOVER -> listOf("A big tech company writes off its metaverse unit", "Virtual land prices collapse", "Investors want profits, not visions")
            CHIP_SHORTAGE -> listOf("Chipmakers warn of months-long waits", "Car makers halt lines for lack of chips", "Device makers raise prices")
            CLOUD_GAMING -> listOf("A tech giant launches a game-streaming service", "Studios sign exclusive streaming deals", "\"The last console generation\", says one CEO")
            SUPER_APPS -> listOf("A messaging app adds payments and shopping", "Banks rush to get inside chat apps", "\"Why open a website?\" asks a viral post")
            HOME_COMPUTERS -> listOf("The Apple II goes on sale for $1,298", "Computer clubs pack out garages every weekend", "Magazines print games readers can type in")
            VIDEO_GAME_CRASH -> listOf("Console makers slash prices to clear stock", "A famous flop is buried in a New Mexico landfill", "Retailers pull games off their shelves")
            CONSOLE_WARS -> listOf("A Japanese console sells out its first Christmas", "Shops bring back games, cautiously", "Kids line up for a game about a plumber")
            MULTIMEDIA -> listOf("CD-ROM drives become a must-have", "A shooter on a disc sells millions", "Sound cards outsell graphics cards")
            DOT_COM_BOOM -> listOf("A browser company's IPO doubles on day one", "Every business wants a home page", "Investors ask startups for their URL, not their revenue")
            DOT_COM_BUST -> listOf("The Nasdaq falls for a fifth straight week", "A famous pet-supplies site shuts down", "Startups lay off half their staff")
            WEB_2_0 -> listOf("A social network launches at Harvard", "Blogs are the new newspapers", "Everyone's uploading videos to a site called YouTube")
            SMARTPHONES -> listOf("Apple unveils the iPhone", "The App Store opens with 500 apps", "Keyboard phones are suddenly old-fashioned")
            FREE_TO_PLAY -> listOf("A free mobile game passes $1 billion in sales", "Studios bolt in-game stores onto everything", "Whales: the players who pay for everyone")
        }

    val peak: List<String>
        get() = when (this) {
            BOOTSTRAP_YEARS -> listOf("The industry waits for its next big thing")
            AI_GOLD_RUSH -> listOf("A two-month-old AI startup raises at a $10B valuation", "Clients want chatbots on their bakery websites", "AI researchers now earn more than footballers", "NVIDIA becomes the world's most valuable company, again")
            VR_HYPE_CYCLE -> listOf("VR headsets are this year's must-have gift", "Every big game gets a VR mode", "Gadget sales hit a record")
            CRYPTO_WINTER -> listOf("Funding for startups falls to a five-year low", "Miners dump cheap GPUs on the market", "Ad budgets shrink across the industry")
            REMOTE_WORK_BOOM -> listOf("Productivity apps top the download charts", "Agencies are booked for months", "Office rents fall in every big city")
            PRIVACY_PANIC -> listOf("Social networks face hearings over teen safety", "AI labs asked to explain their training data", "Hardware makers pitch privacy as a feature")
            CREATOR_ECONOMY -> listOf("The creator economy is now bigger than Hollywood", "Every app adds a video feed", "Game streamers outdraw the Super Bowl")
            RETRO_REVIVAL -> listOf("Indie games dominate the sales chart", "\"Graphics don't matter\", say gamers, again", "Gadget sales slump as people play old consoles")
            METAVERSE_HANGOVER -> listOf("VR headsets gather dust", "Investors pile into anything that makes money now", "AI labs pick up the metaverse's leftover engineers")
            CHIP_SHORTAGE -> listOf("Device makers ration their best models", "GPUs resell for twice their price", "Factories fight over the last wafers")
            CLOUD_GAMING -> listOf("Game streaming passes console sales", "Studios are the hottest acquisition targets", "Console makers cut prices to keep up")
            SUPER_APPS -> listOf("A super-app passes a billion daily users", "Websites lose traffic to apps", "Social apps become shops")
            HOME_COMPUTERS -> listOf("Computer sales triple in a year", "A spreadsheet program sells computers by itself", "Arcade hits come home on cassette")
            VIDEO_GAME_CRASH -> listOf("Game sales fall by 97%", "Investors call video games a fad", "Home computers win the living room, for now")
            CONSOLE_WARS -> listOf("The console war hits the playground", "Mascots get their own cartoons", "Game sales pass the movies")
            MULTIMEDIA -> listOf("3D graphics cards arrive", "The first online games connect over phone lines", "Every PC ships with a CD-ROM drive")
            DOT_COM_BOOM -> listOf("A company with no revenue is worth $10 billion", "Web designers earn more than doctors", "Investors pour billions into anything online")
            DOT_COM_BUST -> listOf("Funding dries up across the industry", "Former dot-com staff look for real jobs", "Only the profitable survive")
            WEB_2_0 -> listOf("A social network passes 100 million users", "User-generated content is the future", "Websites add share buttons to everything")
            SMARTPHONES -> listOf("App developers become millionaires overnight", "Everyone has a smartphone now", "A bird game is the biggest game in the world")
            FREE_TO_PLAY -> listOf("Mobile games out-earn consoles", "A battle royale is free and everywhere", "Loot boxes draw regulators' attention")
        }

    val fade: List<String>
        get() = when (this) {
            BOOTSTRAP_YEARS -> listOf("Something is about to change")
            AI_GOLD_RUSH -> listOf("Is the AI bubble about to burst?", "Investors ask AI startups about revenue", "GPU prices finally ease")
            VR_HYPE_CYCLE -> listOf("Headset sales slow after the holidays", "VR studios report disappointing numbers")
            CRYPTO_WINTER -> listOf("Coins steady after months of losses", "Investors return, cautiously")
            REMOTE_WORK_BOOM -> listOf("Bosses call workers back to the office", "Laptop sales cool")
            PRIVACY_PANIC -> listOf("The privacy hearings wind down", "Regulators turn to other things")
            CREATOR_ECONOMY -> listOf("Creators complain about burnout", "Platforms trim their creator funds")
            RETRO_REVIVAL -> listOf("Gamers want shiny graphics again", "Old console prices come back down")
            METAVERSE_HANGOVER -> listOf("Investors start dreaming again", "Someone mentions the metaverse without laughing")
            CHIP_SHORTAGE -> listOf("New fabs come online", "Chip prices fall back")
            CLOUD_GAMING -> listOf("Streaming services fight over the same players", "Gamers complain about lag")
            SUPER_APPS -> listOf("Regulators look at super-app monopolies", "Users say they miss simple apps")
            HOME_COMPUTERS -> listOf("Too many consoles, too many games, say retailers", "Game prices start to fall")
            VIDEO_GAME_CRASH -> listOf("A new console is coming from Japan", "Shops wonder if games can come back")
            CONSOLE_WARS -> listOf("16-bit consoles are on the way", "Kids want better graphics")
            MULTIMEDIA -> listOf("Everyone's talking about the internet", "Modems get faster")
            DOT_COM_BOOM -> listOf("Some say the dot-com stocks are overvalued", "Interest rates go up")
            DOT_COM_BUST -> listOf("A few web companies are profitable after all", "Investors start looking again")
            WEB_2_0 -> listOf("Phones that browse the web are coming", "Social networks look for ways to make money")
            SMARTPHONES -> listOf("App stores are crowded", "Free apps push out paid ones")
            FREE_TO_PLAY -> listOf("Headsets are coming back", "Players tire of paywalls")
        }

    /** Demand multiplier per lane. */
    fun demand(lane: Lane): Double = when (this) {
        BOOTSTRAP_YEARS -> 1.0
        AI_GOLD_RUSH -> when (lane) {
            Lane.AI -> 1.7; Lane.APPS -> 1.1; Lane.WEBSITES -> 0.9; else -> 1.0
        }
        VR_HYPE_CYCLE -> when (lane) {
            Lane.HARDWARE -> 1.35; Lane.GAMES -> 1.2; else -> 1.0
        }
        CRYPTO_WINTER -> when (lane) {
            Lane.SOCIAL -> 0.8; Lane.APPS -> 0.85; Lane.AI -> 0.9; else -> 0.95
        }
        REMOTE_WORK_BOOM -> when (lane) {
            Lane.WEBSITES -> 1.3; Lane.APPS -> 1.2; Lane.HARDWARE -> 1.2; else -> 1.0
        }
        PRIVACY_PANIC -> when (lane) {
            Lane.SOCIAL -> 0.75; Lane.AI -> 0.85; Lane.HARDWARE -> 1.1; else -> 1.0
        }
        CREATOR_ECONOMY -> when (lane) {
            Lane.SOCIAL -> 1.35; Lane.WEBSITES -> 1.1; Lane.GAMES -> 1.1; else -> 1.0
        }
        RETRO_REVIVAL -> when (lane) {
            Lane.GAMES -> 1.3; Lane.HARDWARE -> 0.9; else -> 1.0
        }
        METAVERSE_HANGOVER -> when (lane) {
            Lane.HARDWARE -> 0.8; Lane.SOCIAL -> 0.9; Lane.AI -> 1.1; else -> 0.95
        }
        CHIP_SHORTAGE -> when (lane) {
            Lane.HARDWARE -> 0.9; Lane.AI -> 0.9; Lane.WEBSITES -> 1.1; Lane.APPS -> 1.05; else -> 1.0
        }
        CLOUD_GAMING -> when (lane) {
            Lane.GAMES -> 1.3; Lane.HARDWARE -> 0.85; Lane.AI -> 1.05; else -> 1.0
        }
        SUPER_APPS -> when (lane) {
            Lane.APPS -> 1.3; Lane.SOCIAL -> 1.2; Lane.WEBSITES -> 0.8; else -> 1.0
        }
        HOME_COMPUTERS -> when (lane) {
            Lane.HARDWARE -> 1.4; Lane.GAMES -> 1.2; else -> 1.0
        }
        VIDEO_GAME_CRASH -> when (lane) {
            Lane.GAMES -> 0.6; Lane.HARDWARE -> 0.85; else -> 1.0
        }
        CONSOLE_WARS -> when (lane) {
            Lane.GAMES -> 1.35; Lane.HARDWARE -> 1.1; else -> 1.0
        }
        MULTIMEDIA -> when (lane) {
            Lane.GAMES -> 1.2; Lane.HARDWARE -> 1.2; else -> 1.0
        }
        DOT_COM_BOOM -> when (lane) {
            Lane.WEBSITES -> 1.5; Lane.HARDWARE -> 1.15; Lane.GAMES -> 1.1; else -> 1.0
        }
        DOT_COM_BUST -> when (lane) {
            Lane.WEBSITES -> 0.6; Lane.HARDWARE -> 0.85; Lane.GAMES -> 1.05; else -> 0.9
        }
        WEB_2_0 -> when (lane) {
            Lane.SOCIAL -> 1.4; Lane.WEBSITES -> 1.2; Lane.GAMES -> 1.1; else -> 1.0
        }
        SMARTPHONES -> when (lane) {
            Lane.APPS -> 1.35; Lane.HARDWARE -> 1.15; Lane.SOCIAL -> 1.0; Lane.WEBSITES -> 0.95; else -> 1.0
        }
        FREE_TO_PLAY -> when (lane) {
            Lane.APPS -> 1.25; Lane.GAMES -> 1.2; Lane.SOCIAL -> 1.15; Lane.AI -> 0.9; else -> 1.0
        }
    }

    /** Multiplier on what people ask to be paid. */
    val salaries: Double
        get() = when (this) {
            AI_GOLD_RUSH -> 1.12
            REMOTE_WORK_BOOM -> 1.08
            DOT_COM_BOOM -> 1.15
            DOT_COM_BUST -> 0.88
            VIDEO_GAME_CRASH -> 0.9
            CRYPTO_WINTER -> 0.9
            METAVERSE_HANGOVER -> 0.92
            else -> 1.0
        }

    /** Multiplier on what devices cost to build. */
    val hardwareCost: Double
        get() = when (this) {
            CHIP_SHORTAGE -> 1.3
            VR_HYPE_CYCLE -> 1.05
            HOME_COMPUTERS -> 1.1
            CRYPTO_WINTER -> 0.95
            else -> 1.0
        }

    /** Multiplier on what GPUs cost an AI lab. */
    val gpuPrice: Double
        get() = when (this) {
            AI_GOLD_RUSH -> 1.8
            CRYPTO_WINTER -> 0.6
            VR_HYPE_CYCLE -> 1.1
            CHIP_SHORTAGE -> 1.6
            else -> 1.0
        }

    /** How willing investors are to write cheques. */
    val investorAppetite: Double
        get() = when (this) {
            AI_GOLD_RUSH -> 1.5
            VR_HYPE_CYCLE -> 1.2
            CRYPTO_WINTER -> 0.6
            METAVERSE_HANGOVER -> 0.7
            PRIVACY_PANIC -> 0.9
            CLOUD_GAMING -> 1.1
            HOME_COMPUTERS -> 1.2
            VIDEO_GAME_CRASH -> 0.7
            DOT_COM_BOOM -> 1.8
            DOT_COM_BUST -> 0.45
            WEB_2_0 -> 1.1
            SMARTPHONES -> 1.1
            else -> 1.0
        }

    /** Lanes investors are especially hot on this era. */
    val hotLanes: List<Lane> get() = Lane.entries.filter { demand(it) >= 1.2 }

    val coldLanes: List<Lane> get() = Lane.entries.filter { demand(it) <= 0.85 }
}

@Serializable
data class Rival(
    val id: Int,
    var name: String,
    val lane: Lane,
    /** 0...1 market muscle in their lane. */
    var strength: Double,
    /** 0...1 how often they poach and bid for talent. */
    val aggression: Double,
    var lastMove: String? = null,
    /** 0...1 how much they want to hurt you. Rises as you take their market. */
    var heat: Double = 0.0,
    var lastMoveWeek: Int = -100,
    /** When you can next make a move against them. */
    var counterReadyWeek: Int = 0,
    /** Beaten back: they make no moves until this week. */
    var quietUntil: Int = 0,
    /** Favoured genres (games roster rivals). Empty = no preference. Defaults keep old saves loading. */
    val genres: List<Genre> = emptyList(),
    /** 0...1 how well their games usually land. */
    var reputation: Double = 0.5,
    /** Short story of who they are. */
    val blurb: String = "",
    val founded: Int = 0,
    /** Historical close year, if any. */
    val closedYear: Int? = null,
    /** Real-world games-studio rival from the era roster (not the one-per-lane placeholder). */
    val roster: Boolean = false,
    /** Closed historically but stayed alive this run. */
    var survived: Boolean = false,
    /** Bought by the player; no longer competes. */
    var absorbed: Boolean = false,
    /** Highest heat seen, for "rivalry heating up" story beats. */
    var peakHeat: Double = 0.0,
)

/** Where an era is in its life: arriving, at its height, or fading. */
@Serializable
enum class EraPhase(val title: String) { DAWN("Dawn"), PEAK("Peak"), FADING("Fading") }

/** An era that has been and gone, for the timeline. */
@Serializable
data class EraSpan(
    val era: Era,
    val startWeek: Int,
    val endWeek: Int,
    /** For eras you lived through: what you earned, and your value at the start and the end. */
    val revenue: Double = 0.0,
    val valueStart: Double = 0.0,
    val valueEnd: Double = 0.0,
    val challenge: String? = null,
    val challengeWon: Boolean? = null,
) {
    /** Did the company live through this era (rather than it being history before the run began)? */
    val lived: Boolean get() = endWeek > 0 && (valueStart > 0 || valueEnd > 0 || revenue > 0)
}

@Serializable
class World {
    var era = Era.BOOTSTRAP_YEARS
    var eraStartWeek = 0
    var eraLength = 104
    val pastEras = mutableListOf<Era>()
    val history = mutableListOf<EraSpan>()

    /** How strongly the era is felt right now, 0.35...1: it builds at dawn and eases as it fades. */
    var eraIntensity = 1.0
    var phase = EraPhase.PEAK

    /** The era that comes next, picked as this one starts to fade. Only rumours say what it is. */
    var nextEra: Era? = null

    /** This era's goal, and where you stood when it began. */
    var challenge: EraChallenge? = null
    var eraStartValue = 0.0
    var eraStartRevenue = 0.0

    /** The era's one-off moment has come round. */
    var momentDone = false

    /** Decades looked back on, and where the current one began (-1 until the first week). */
    val decades = mutableListOf<DecadeReview>()
    var decadeStartWeek = -1
    var decadeStartRevenue = 0.0
    var decadeStartValue = 0.0

    private fun scaled(x: Double): Double = 1 + (x - 1) * eraIntensity

    /** Demand for a lane this week, from the era as strongly as it's felt. */
    fun demand(lane: Lane): Double = scaled(era.demand(lane))
    val gpuPrice: Double get() = scaled(era.gpuPrice)
    val investorAppetite: Double get() = scaled(era.investorAppetite)
    val salaries: Double get() = scaled(era.salaries)
    val hardwareCost: Double get() = scaled(era.hardwareCost)
    val rivals = mutableListOf<Rival>()

    /** Era headline Games rival (from [rivalIn]); preferred by [rival]. Old saves default null. */
    var gamesHeadline: String? = null

    /** Rivals' campaigns against you, and yours against them. */
    val pressures = mutableListOf<Pressure>()

    companion object {
        /** One rival per non-games lane. Games rivals come from [ensureGameRivalRoster]. */
        internal fun makeRivals() = listOf(
            Rival(2, "Squarespace", Lane.WEBSITES, strength = 0.4, aggression = 0.3),
            Rival(3, "Google", Lane.APPS, strength = 0.5, aggression = 0.4),
            Rival(4, "Meta", Lane.SOCIAL, strength = 0.6, aggression = 0.5),
            Rival(5, "Apple", Lane.HARDWARE, strength = 0.7, aggression = 0.4),
            Rival(6, "OpenAI", Lane.AI, strength = 0.6, aggression = 0.8),
            Rival(7, "Microsoft", Lane.OS, strength = 0.65, aggression = 0.45),
        )

        /** The other big names in each lane, for the news. The main rival is one of them. */
        fun peers(lane: Lane): List<String> = when (lane) {
            Lane.GAMES -> listOf(
                "Electronic Arts", "Ubisoft", "Activision Blizzard", "Take-Two", "Nintendo", "Sony Interactive",
                "Sega", "Capcom", "Square Enix", "Valve", "Epic Games", "Rockstar Games", "Bethesda Softworks",
                "CD Projekt Red", "Bandai Namco",
            )
            Lane.WEBSITES -> listOf("Squarespace", "Wix", "Webflow", "WordPress.com", "Shopify")
            Lane.APPS -> listOf("Google", "Duolingo", "Spotify", "Calm", "Strava", "Notion")
            Lane.SOCIAL -> listOf("Meta", "TikTok", "X", "Snap", "Reddit", "Discord")
            Lane.HARDWARE -> listOf("Apple", "Samsung", "Google", "Sony", "Nothing", "Microsoft")
            Lane.AI -> listOf("OpenAI", "Google DeepMind", "Anthropic", "Meta AI", "xAI", "Mistral AI")
            Lane.OS -> listOf("Microsoft", "Apple", "Google", "Canonical", "Red Hat", "Valve")
        }

        /** Earlier versions used made-up rivals; saves from then get the real ones. */
        internal val renamed = mapOf(
            "Gigaplay Interactive" to "Electronic Arts",
            "Pixelsmith Collective" to "Squarespace",
            "Appetite Inc" to "Google",
            "Fritter" to "Meta",
            "Pear Computing" to "Apple",
            "Hyperion Labs" to "OpenAI",
        )
    }

    /** Demand multiplier from rival competition in a lane. */
    fun competition(lane: Lane): Double {
        val s = rivals.filter { it.lane == lane && !it.absorbed }.maxOfOrNull { it.strength } ?: 0.0
        return 1 - 0.2 * s
    }

    /** The lane's headline rival: era giant for Games, otherwise hottest active studio. */
    fun rival(lane: Lane): Rival? {
        if (lane == Lane.GAMES) {
            val h = gamesHeadline
            if (h != null) {
                rivals.firstOrNull {
                    it.lane == Lane.GAMES && !it.absorbed &&
                        (it.name == h || canonicalStudio(it.name) == canonicalStudio(h))
                }?.let { return it }
            }
        }
        return rivals.filter { it.lane == lane && !it.absorbed }.maxByOrNull { it.heat * 2 + it.strength }
            ?: rivals.firstOrNull { it.lane == lane }
    }

}
