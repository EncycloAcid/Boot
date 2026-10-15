package com.boot.core

/*
 * Recognizable firms and logo ids for the UI. Names are historically year-gated so a 1977 run
 * never surfaces modern-only publishers, VCs or outlets. UI prefers licensed pack PNGs when
 * present (see logoPackSlug / companyLogoPackSlug); otherwise BrandLogoDraw paints hand-drawn
 * Compose silhouettes. Obscure indie names still fall back to a monogram tile.
 */

/** What kind of firm a mark stands for. */
enum class MarkKind { RIVAL, PUBLISHER, INVESTOR, OUTLET, PLATFORM, TECH, STUDIO }

/** Which hand-drawn logo silhouette to paint. [NONE] falls back to a plain monogram. */
enum class LogoId {
    NONE,
    APPLE, MICROSOFT, GOOGLE, NINTENDO, SONY, PLAYSTATION, XBOX,
    SEGA, ATARI, EA, UBISOFT, ACTIVISION, STEAM, VALVE, EPIC,
    NVIDIA, AMAZON, META, FACEBOOK, OPENAI, NETFLIX, TESLA,
    SPOTIFY, DISCORD, REDDIT, TIKTOK, TWITTER_X, SNAP,
    SEQUOIA, Y_COMBINATOR, A16Z, SOFTBANK, KLEINER_PERKINS,
    IGN, TECHCRUNCH, THE_VERGE, WIRED, BLOOMBERG, REUTERS, CNBC,
    IBM, COMMODORE, CAPCOM, KONAMI, SQUARE_ENIX, BANDAI_NAMCO,
    ROCKSTAR, TAKE_TWO, BETHESDA, DEVOLVER, CDPR, SUPERCELL,
    CANONICAL, SAMSUNG, DELL, NOKIA, ROBLOX, PINTEREST,
    SHOPIFY, DUOLINGO, WORDPRESS, SQUARESPACE, WIX,
    TENCENT, ANTHROPIC, XAI, MYSPACE, AOL, YAHOO,
    GAMESPOT, EUROGAMER, PC_GAMER, EDGE, KOTAKU, POLYGON,
    ARS, ENGADGET, NYT, WSJ, FT, BYTE, CGW,
}

/**
 * Visual identity for a company name. [bg]/[fg] are ARGB longs for Compose Color(…) without
 * pulling Android graphics into :core. [logo] selects the path recreation to draw.
 */
data class CompanyMark(
    val name: String,
    val founded: Int,
    val kind: MarkKind,
    val initials: String,
    val bg: Long,
    val fg: Long = 0xFFFFFFFF,
    val logo: LogoId = LogoId.NONE,
    val closed: Int? = null,
) {
    fun activeIn(year: Int): Boolean = year >= founded && (closed == null || year < closed)
}

/** Canonical lookup key: strip Inc./Games/etc. noise and normalize aliases. */
fun companyKey(name: String): String {
    val trimmed = name.trim()
    return when (canonicalStudio(trimmed)) {
        "Activision Blizzard" -> "Activision Blizzard"
        "Square Enix" -> "Square Enix"
        "Bandai Namco" -> "Bandai Namco"
        "PlayStation Studios" -> "PlayStation Studios"
        "Xbox Game Studios" -> "Xbox Game Studios"
        "Epic Games" -> "Epic Games"
        else -> when (trimmed) {
            "Take-Two", "Take-Two Interactive" -> "Take-Two Interactive"
            "Sony Interactive", "Sony Computer Entertainment" -> "PlayStation Studios"
            "Microsoft Game Studios" -> "Xbox Game Studios"
            "Facebook" -> "Meta"
            "Snapchat" -> "Snap"
            "FIFA" -> "Electronic Arts"
            "Steam" -> "Steam"
            else -> trimmed
        }
    }
}

/** Mark for [name], or a neutral fallback monogram when unknown. */
fun companyMark(name: String): CompanyMark {
    val key = companyKey(name)
    MarksByKey[key]?.let { return it.copy(name = name) }
    MarksByKey[name]?.let { return it }
    Marks.firstOrNull { it.name.equals(name, ignoreCase = true) || it.name.equals(key, ignoreCase = true) }
        ?.let { return it.copy(name = name) }
    return fallbackMark(name)
}

/** Was this named firm around in [year]? Unknown names are treated as always-active (indie labels). */
fun companyActive(name: String, year: Int): Boolean {
    val mark = MarksByKey[companyKey(name)] ?: Marks.firstOrNull { it.name.equals(name, ignoreCase = true) }
    return mark?.activeIn(year) ?: true
}

/** Games publishers that would offer deals in [year]. */
fun publishersIn(year: Int): List<String> =
    PublisherRoster.filter { companyActive(it, year) }.map { studioNameIn(it, year) }.distinct()

/** Venture leads for funding stage [stage] (0 = seed) that existed in [year]. */
fun investorsIn(year: Int, stage: Int): List<String> {
    val pool = InvestorStages[stage.coerceIn(0, InvestorStages.lastIndex)]
    val open = pool.filter { companyActive(it, year) }
    return open.ifEmpty { listOf("Local angels", "Family office", "Regional bank") }
}

/** Big consumer brands for agency briefs that existed in [year]. */
fun brandsIn(year: Int): List<String> =
    BrandYears.filter { it.value <= year }.keys.toList().ifEmpty { listOf("Acme Logistics", "Civic Credit Union") }

/** Console / storefront holders active in [year]. */
fun platformHoldersIn(year: Int): List<String> =
    PlatformHolders.filter { companyActive(it, year) }.map { studioNameIn(it, year) }.distinct()

/** Review / tech outlets with marks that existed in [year]. */
fun outletsWithMarksIn(year: Int): List<String> =
    Marks.filter { it.kind == MarkKind.OUTLET && it.activeIn(year) }.map { it.name }

/**
 * Licensed pack filename stem (kebab-case, no extension) for [logo], or null when the pack
 * has no matching PNG and the UI should use the vector silhouette / monogram.
 */
fun logoPackSlug(logo: LogoId): String? = when (logo) {
    LogoId.NONE, LogoId.FACEBOOK, LogoId.PINTEREST, LogoId.TWITTER_X, LogoId.XAI,
    LogoId.BYTE, LogoId.CGW,
    -> null
    LogoId.APPLE -> "apple"
    LogoId.MICROSOFT -> "microsoft"
    LogoId.GOOGLE -> "google"
    LogoId.NINTENDO -> "nintendo"
    LogoId.SONY -> "sony"
    LogoId.PLAYSTATION -> "playstation"
    LogoId.XBOX -> "xbox"
    LogoId.SEGA -> "sega"
    LogoId.ATARI -> "atari"
    LogoId.EA -> "ea"
    LogoId.UBISOFT -> "ubisoft"
    LogoId.ACTIVISION -> "activision"
    LogoId.STEAM -> "steam"
    LogoId.VALVE -> "valve"
    LogoId.EPIC -> "epic-games"
    LogoId.NVIDIA -> "nvidia"
    LogoId.AMAZON -> "amazon"
    LogoId.META -> "meta"
    LogoId.OPENAI -> "openai"
    LogoId.NETFLIX -> "netflix"
    LogoId.TESLA -> "tesla"
    LogoId.SPOTIFY -> "spotify"
    LogoId.DISCORD -> "discord"
    LogoId.REDDIT -> "reddit"
    LogoId.TIKTOK -> "tiktok"
    LogoId.SNAP -> "snap"
    LogoId.SEQUOIA -> "sequoia-capital"
    LogoId.Y_COMBINATOR -> "y-combinator"
    LogoId.A16Z -> "andreessen-horowitz"
    LogoId.SOFTBANK -> "softbank"
    LogoId.KLEINER_PERKINS -> "kleiner-perkins"
    LogoId.IGN -> "ign"
    LogoId.TECHCRUNCH -> "techcrunch"
    LogoId.THE_VERGE -> "the-verge"
    LogoId.WIRED -> "wired"
    LogoId.BLOOMBERG -> "bloomberg"
    LogoId.REUTERS -> "reuters"
    LogoId.CNBC -> "cnbc"
    LogoId.IBM -> "ibm"
    LogoId.COMMODORE -> "commodore"
    LogoId.CAPCOM -> "capcom"
    LogoId.KONAMI -> "konami"
    LogoId.SQUARE_ENIX -> "square-enix"
    LogoId.BANDAI_NAMCO -> "bandai-namco"
    LogoId.ROCKSTAR -> "rockstar-games"
    LogoId.TAKE_TWO -> "take-two"
    LogoId.BETHESDA -> "bethesda"
    LogoId.DEVOLVER -> "devolver-digital"
    LogoId.CDPR -> "cd-projekt-red"
    LogoId.SUPERCELL -> "supercell"
    LogoId.CANONICAL -> "canonical"
    LogoId.SAMSUNG -> "samsung"
    LogoId.DELL -> "dell"
    LogoId.NOKIA -> "nokia"
    LogoId.ROBLOX -> "roblox"
    LogoId.SHOPIFY -> "shopify"
    LogoId.DUOLINGO -> "duolingo"
    LogoId.WORDPRESS -> "wordpress"
    LogoId.SQUARESPACE -> "squarespace"
    LogoId.WIX -> "wix"
    LogoId.TENCENT -> "tencent"
    LogoId.ANTHROPIC -> "anthropic"
    LogoId.MYSPACE -> "myspace"
    LogoId.AOL -> "aol"
    LogoId.YAHOO -> "yahoo"
    LogoId.GAMESPOT -> "gamespot"
    LogoId.EUROGAMER -> "eurogamer"
    LogoId.PC_GAMER -> "pc-gamer"
    LogoId.EDGE -> "edge"
    LogoId.KOTAKU -> "kotaku"
    LogoId.POLYGON -> "polygon"
    LogoId.ARS -> "ars-technica"
    LogoId.ENGADGET -> "engadget"
    LogoId.NYT -> "the-new-york-times"
    LogoId.WSJ -> "the-wall-street-journal"
    LogoId.FT -> "financial-times"
}

/**
 * Pack stem for [mark]: LogoId mapping first, then name overrides for firms that still use
 * [LogoId.NONE] but have a licensed PNG in the pack.
 */
fun companyLogoPackSlug(mark: CompanyMark): String? {
    logoPackSlug(mark.logo)?.let { return it }
    return NamePackSlugs[companyKey(mark.name)]
        ?: NamePackSlugs[mark.name]
        ?: NamePackSlugs.entries.firstOrNull { it.key.equals(mark.name, ignoreCase = true) }?.value
}

/** Pack stem for a display [name], or null when no licensed PNG is wired. */
fun companyLogoPackSlug(name: String): String? = companyLogoPackSlug(companyMark(name))

/** How many distinct [LogoId] values (excluding [LogoId.NONE]) resolve to a pack PNG. */
fun logoIdsWithPackAssets(): Int =
    LogoId.entries.count { it != LogoId.NONE && logoPackSlug(it) != null }

/** How many [LogoId] values still rely on silhouette / monogram only. */
fun logoIdsWithoutPackAssets(): Int =
    LogoId.entries.count { it != LogoId.NONE && logoPackSlug(it) == null }

/** Name → pack stem for marks that keep [LogoId.NONE] but ship a PNG. */
private val NamePackSlugs = mapOf(
    "Annapurna Interactive" to "annapurna-interactive",
    "THQ" to "thq",
    "THQ Nordic" to "thq-nordic",
    "Sierra On-Line" to "sierra-on-line",
    "LucasArts" to "lucasarts",
    "Infocom" to "infocom",
    "Broderbund" to "broderbund",
    "Eidos" to "eidos-interactive",
    "Acclaim" to "acclaim-entertainment",
    "Midway" to "midway-games",
    "FromSoftware" to "fromsoftware",
    "Notion" to "notion",
    "Netscape" to "netscape",
    "Compaq" to "compaq",
    "Palm" to "palm",
    "Palm OS" to "palm",
    "Symbian" to "symbian",
)

private fun fallbackMark(name: String): CompanyMark {
    val words = name.split(' ', '-', '&', '.').filter { it.isNotBlank() && it.lowercase() !in SkipWords }
    val initials = when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.isNotEmpty() -> words[0].take(2).uppercase()
        else -> "?"
    }
    val hue = (name.hashCode().toLong() and 0xFFFFFF)
    val bg = 0xFF000000 or hue
    return CompanyMark(name, 0, MarkKind.STUDIO, initials, bg, logo = LogoId.NONE)
}

private val SkipWords = setOf(
    "the", "of", "and", "inc", "llc", "ltd", "corp", "co", "games", "studios", "studio",
    "interactive", "entertainment", "capital", "partners", "ventures", "fund",
)

private fun m(
    name: String, founded: Int, kind: MarkKind, initials: String, bg: Long,
    logo: LogoId, fg: Long = 0xFFFFFFFF, closed: Int? = null,
) = CompanyMark(name, founded, kind, initials, bg, fg, logo, closed)

// Catalog

private val Marks: List<CompanyMark> = listOf(
    m("Atari", 1972, MarkKind.PUBLISHER, "A", 0xFFE53935, LogoId.ATARI, closed = 2013),
    m("Activision", 1979, MarkKind.PUBLISHER, "AV", 0xFF1565C0, LogoId.ACTIVISION),
    m("Activision Blizzard", 1979, MarkKind.PUBLISHER, "AB", 0xFF0D47A1, LogoId.ACTIVISION),
    m("Electronic Arts", 1982, MarkKind.PUBLISHER, "EA", 0xFF000000, LogoId.EA),
    m("Ubisoft", 1986, MarkKind.PUBLISHER, "Ubi", 0xFF1A237E, LogoId.UBISOFT),
    m("Nintendo", 1889, MarkKind.PUBLISHER, "N", 0xFFE60012, LogoId.NINTENDO),
    m("Sega", 1960, MarkKind.PUBLISHER, "SE", 0xFF0050A0, LogoId.SEGA),
    m("Capcom", 1979, MarkKind.PUBLISHER, "C", 0xFFC62828, LogoId.CAPCOM),
    m("Konami", 1969, MarkKind.PUBLISHER, "K", 0xFFB71C1C, LogoId.KONAMI),
    m("Square", 1986, MarkKind.PUBLISHER, "SQ", 0xFF212121, LogoId.SQUARE_ENIX),
    m("Square Enix", 1986, MarkKind.PUBLISHER, "SE", 0xFF212121, LogoId.SQUARE_ENIX),
    m("Bandai Namco", 1955, MarkKind.PUBLISHER, "BN", 0xFFF9A825, LogoId.BANDAI_NAMCO, fg = 0xFF212121),
    m("Namco", 1955, MarkKind.PUBLISHER, "N", 0xFFF9A825, LogoId.BANDAI_NAMCO, fg = 0xFF212121),
    m("Take-Two Interactive", 1993, MarkKind.PUBLISHER, "T2", 0xFF6A1B9A, LogoId.TAKE_TWO),
    m("Rockstar Games", 1998, MarkKind.PUBLISHER, "R", 0xFFFFD600, LogoId.ROCKSTAR, fg = 0xFF212121),
    m("Bethesda Softworks", 1986, MarkKind.PUBLISHER, "B", 0xFF37474F, LogoId.BETHESDA),
    m("Devolver Digital", 2009, MarkKind.PUBLISHER, "DD", 0xFFE65100, LogoId.DEVOLVER),
    m("Annapurna Interactive", 2016, MarkKind.PUBLISHER, "AI", 0xFF4E342E, LogoId.NONE),
    m("THQ", 1990, MarkKind.PUBLISHER, "THQ", 0xFF00695C, LogoId.NONE, closed = 2013),
    m("THQ Nordic", 2016, MarkKind.PUBLISHER, "TN", 0xFF00695C, LogoId.NONE),
    m("Sierra On-Line", 1979, MarkKind.PUBLISHER, "Si", 0xFF2E7D32, LogoId.NONE, closed = 2008),
    m("LucasArts", 1982, MarkKind.PUBLISHER, "LA", 0xFF455A64, LogoId.NONE, closed = 2013),
    m("Origin Systems", 1983, MarkKind.PUBLISHER, "OS", 0xFF5D4037, LogoId.NONE, closed = 2004),
    m("Infocom", 1979, MarkKind.PUBLISHER, "In", 0xFF455A64, LogoId.NONE, closed = 1989),
    m("Broderbund", 1980, MarkKind.PUBLISHER, "Br", 0xFF1565C0, LogoId.NONE, closed = 1998),
    m("Eidos", 1990, MarkKind.PUBLISHER, "Ei", 0xFF263238, LogoId.NONE, closed = 2009),
    m("Acclaim", 1987, MarkKind.PUBLISHER, "Ac", 0xFFE65100, LogoId.NONE, closed = 2004),
    m("Midway", 1988, MarkKind.PUBLISHER, "M", 0xFF212121, LogoId.NONE, closed = 2009),
    m("Valve", 1996, MarkKind.PUBLISHER, "V", 0xFF171A21, LogoId.VALVE),
    m("Epic Games", 1991, MarkKind.PUBLISHER, "EG", 0xFF212121, LogoId.EPIC),
    m("CD Projekt Red", 2002, MarkKind.STUDIO, "CD", 0xFFE53935, LogoId.CDPR),
    m("CD Projekt", 1994, MarkKind.PUBLISHER, "CD", 0xFFE53935, LogoId.CDPR),
    m("FromSoftware", 1986, MarkKind.STUDIO, "FS", 0xFF212121, LogoId.NONE),
    m("Supercell", 2010, MarkKind.PUBLISHER, "SC", 0xFFFFEB3B, LogoId.SUPERCELL, fg = 0xFF212121),
    m("PlayStation Studios", 1993, MarkKind.PUBLISHER, "PS", 0xFF003087, LogoId.PLAYSTATION),
    m("Xbox Game Studios", 2000, MarkKind.PUBLISHER, "X", 0xFF107C10, LogoId.XBOX),
    m("Sony", 1946, MarkKind.PLATFORM, "S", 0xFF212121, LogoId.SONY),
    m("Sony Interactive", 1993, MarkKind.PUBLISHER, "SI", 0xFF003087, LogoId.PLAYSTATION),
    m("Microsoft", 1975, MarkKind.TECH, "MS", 0xFF00A4EF, LogoId.MICROSOFT),
    m("Tencent", 1998, MarkKind.PUBLISHER, "T", 0xFF1565C0, LogoId.TENCENT),

    m("Apple", 1976, MarkKind.PLATFORM, "A", 0xFF212121, LogoId.APPLE),
    m("Google", 1998, MarkKind.PLATFORM, "G", 0xFF4285F4, LogoId.GOOGLE),
    m("Alphabet", 1998, MarkKind.TECH, "A", 0xFF4285F4, LogoId.GOOGLE),
    m("Steam", 2003, MarkKind.PLATFORM, "S", 0xFF171A21, LogoId.STEAM),
    m("Commodore", 1976, MarkKind.PLATFORM, "C=", 0xFF1565C0, LogoId.COMMODORE, closed = 1994),
    m("IBM", 1911, MarkKind.TECH, "IBM", 0xFF0D47A1, LogoId.IBM),
    m("Dell", 1984, MarkKind.TECH, "D", 0xFF007DB8, LogoId.DELL),
    m("Nokia", 1865, MarkKind.TECH, "N", 0xFF124191, LogoId.NOKIA),
    m("Samsung", 1969, MarkKind.TECH, "S", 0xFF1428A0, LogoId.SAMSUNG),
    m("Nothing", 2020, MarkKind.TECH, "Nt", 0xFF212121, LogoId.NONE),

    m("Meta", 2004, MarkKind.RIVAL, "Me", 0xFF0866FF, LogoId.META),
    m("Facebook", 2004, MarkKind.RIVAL, "f", 0xFF1877F2, LogoId.FACEBOOK),
    m("MySpace", 2003, MarkKind.RIVAL, "My", 0xFF003399, LogoId.MYSPACE, closed = 2010),
    m("TikTok", 2016, MarkKind.RIVAL, "TT", 0xFF010101, LogoId.TIKTOK),
    m("X", 2006, MarkKind.RIVAL, "X", 0xFF000000, LogoId.TWITTER_X),
    m("Snap", 2011, MarkKind.RIVAL, "Sn", 0xFFFFFC00, LogoId.SNAP, fg = 0xFF212121),
    m("Reddit", 2005, MarkKind.RIVAL, "r/", 0xFFFF4500, LogoId.REDDIT),
    m("Discord", 2015, MarkKind.RIVAL, "D", 0xFF5865F2, LogoId.DISCORD),
    m("OpenAI", 2015, MarkKind.RIVAL, "AI", 0xFF10A37F, LogoId.OPENAI),
    m("Google DeepMind", 2010, MarkKind.RIVAL, "DM", 0xFF4285F4, LogoId.GOOGLE),
    m("Anthropic", 2021, MarkKind.RIVAL, "An", 0xFFD4A27F, LogoId.ANTHROPIC, fg = 0xFF212121),
    m("xAI", 2023, MarkKind.RIVAL, "x", 0xFF212121, LogoId.XAI),
    m("Mistral AI", 2023, MarkKind.RIVAL, "M", 0xFFFF7000, LogoId.NONE),
    m("Meta AI", 2013, MarkKind.RIVAL, "Me", 0xFF0866FF, LogoId.META),
    m("Digital Research", 1976, MarkKind.RIVAL, "DR", 0xFF5D4037, LogoId.NONE, closed = 1991),
    m("Canonical", 2004, MarkKind.TECH, "Ub", 0xFFE95420, LogoId.CANONICAL),
    m("Red Hat", 1993, MarkKind.TECH, "RH", 0xFFEE0000, LogoId.NONE),
    m("Razorfish", 1995, MarkKind.RIVAL, "Rf", 0xFFE91E63, LogoId.NONE, closed = 2009),
    m("Squarespace", 2003, MarkKind.RIVAL, "Sq", 0xFF000000, LogoId.SQUARESPACE),
    m("Wix", 2006, MarkKind.RIVAL, "Wix", 0xFFFAAD4D, LogoId.WIX, fg = 0xFF212121),
    m("Webflow", 2013, MarkKind.RIVAL, "Wf", 0xFF4353FF, LogoId.NONE),
    m("WordPress.com", 2005, MarkKind.RIVAL, "Wp", 0xFF21759B, LogoId.WORDPRESS),
    m("Shopify", 2006, MarkKind.RIVAL, "Sh", 0xFF96BF48, LogoId.SHOPIFY, fg = 0xFF212121),
    m("Duolingo", 2011, MarkKind.RIVAL, "Duo", 0xFF58CC02, LogoId.DUOLINGO, fg = 0xFF212121),
    m("Spotify", 2006, MarkKind.RIVAL, "Sp", 0xFF1DB954, LogoId.SPOTIFY),
    m("Calm", 2012, MarkKind.RIVAL, "Ca", 0xFF2180D8, LogoId.NONE),
    m("Strava", 2009, MarkKind.RIVAL, "St", 0xFFFC4C02, LogoId.NONE),
    m("Notion", 2016, MarkKind.RIVAL, "No", 0xFF212121, LogoId.NONE),
    m("NVIDIA", 1993, MarkKind.TECH, "N", 0xFF76B900, LogoId.NVIDIA, fg = 0xFF212121),
    m("Amazon", 1994, MarkKind.TECH, "a", 0xFFFF9900, LogoId.AMAZON, fg = 0xFF212121),
    m("Netflix", 1997, MarkKind.TECH, "N", 0xFFE50914, LogoId.NETFLIX),
    m("Tesla", 2003, MarkKind.TECH, "T", 0xFFE31937, LogoId.TESLA),
    m("TSMC", 1987, MarkKind.TECH, "TSMC", 0xFF1A237E, LogoId.NONE),
    m("Roblox", 2004, MarkKind.TECH, "R", 0xFFE2231A, LogoId.ROBLOX),
    m("AOL", 1985, MarkKind.TECH, "AOL", 0xFF00A4E4, LogoId.AOL, closed = 2015),
    m("Yahoo", 1995, MarkKind.TECH, "Y!", 0xFF6001D2, LogoId.YAHOO),
    m("BlackBerry", 1984, MarkKind.TECH, "BB", 0xFF212121, LogoId.NONE),
    m("Netscape", 1994, MarkKind.TECH, "N", 0xFF1A237E, LogoId.NONE, closed = 2003),
    m("GeoCities", 1994, MarkKind.TECH, "GC", 0xFF00897B, LogoId.NONE, closed = 2009),
    m("Compaq", 1982, MarkKind.TECH, "Cq", 0xFFB71C1C, LogoId.NONE, closed = 2002),
    m("Palm", 1992, MarkKind.TECH, "P", 0xFF1565C0, LogoId.NONE, closed = 2010),
    m("IBM Watson", 2011, MarkKind.RIVAL, "W", 0xFF0D47A1, LogoId.IBM),
    m("BeOS", 1991, MarkKind.RIVAL, "Be", 0xFFE65100, LogoId.NONE, closed = 2001),
    m("Palm OS", 1996, MarkKind.RIVAL, "P", 0xFF1565C0, LogoId.NONE, closed = 2010),
    m("Symbian", 1998, MarkKind.RIVAL, "Sy", 0xFF00695C, LogoId.NONE, closed = 2012),
    m("Rovio", 2003, MarkKind.RIVAL, "Rv", 0xFFD32F2F, LogoId.NONE),
    m("King", 2003, MarkKind.RIVAL, "K", 0xFF7B1FA2, LogoId.NONE),
    m("Zynga", 2007, MarkKind.RIVAL, "Z", 0xFF6D4C41, LogoId.NONE),
    m("Friendster", 2002, MarkKind.RIVAL, "Fr", 0xFFFF6F00, LogoId.NONE, closed = 2008),
    m("Pinterest", 2010, MarkKind.TECH, "P", 0xFFE60023, LogoId.PINTEREST),

    m("IGN", 1996, MarkKind.OUTLET, "IGN", 0xFFD32F2F, LogoId.IGN),
    m("GameSpot", 1996, MarkKind.OUTLET, "GS", 0xFFF9A825, LogoId.GAMESPOT, fg = 0xFF212121),
    m("Game Informer", 1991, MarkKind.OUTLET, "GI", 0xFF1565C0, LogoId.NONE),
    m("Eurogamer", 1999, MarkKind.OUTLET, "EG", 0xFF2E7D32, LogoId.EUROGAMER),
    m("PC Gamer", 1993, MarkKind.OUTLET, "PCG", 0xFF00695C, LogoId.PC_GAMER),
    m("Edge", 1993, MarkKind.OUTLET, "E", 0xFF212121, LogoId.EDGE),
    m("Kotaku", 2004, MarkKind.OUTLET, "Ko", 0xFFC2185B, LogoId.KOTAKU),
    m("Polygon", 2012, MarkKind.OUTLET, "Pg", 0xFFE040FB, LogoId.POLYGON, fg = 0xFF212121),
    m("TechCrunch", 2005, MarkKind.OUTLET, "TC", 0xFF0A9E01, LogoId.TECHCRUNCH),
    m("The Verge", 2011, MarkKind.OUTLET, "V", 0xFFE01A22, LogoId.THE_VERGE),
    m("Wired", 1993, MarkKind.OUTLET, "W", 0xFF212121, LogoId.WIRED),
    m("Ars Technica", 1998, MarkKind.OUTLET, "Ars", 0xFFFF4E00, LogoId.ARS, fg = 0xFF212121),
    m("Engadget", 2004, MarkKind.OUTLET, "En", 0xFF6B2D5C, LogoId.ENGADGET),
    m("Bloomberg", 1981, MarkKind.OUTLET, "B", 0xFF2800D7, LogoId.BLOOMBERG),
    m("Reuters", 1851, MarkKind.OUTLET, "R", 0xFFFF8000, LogoId.REUTERS, fg = 0xFF212121),
    m("The Wall Street Journal", 1889, MarkKind.OUTLET, "WSJ", 0xFF0274B6, LogoId.WSJ),
    m("Financial Times", 1888, MarkKind.OUTLET, "FT", 0xFFFFF1E5, LogoId.FT, fg = 0xFF212121),
    m("CNBC", 1989, MarkKind.OUTLET, "CNBC", 0xFF005594, LogoId.CNBC),
    m("Byte", 1975, MarkKind.OUTLET, "By", 0xFF1565C0, LogoId.BYTE, closed = 1998),
    m("Computer Gaming World", 1981, MarkKind.OUTLET, "CGW", 0xFF2E7D32, LogoId.CGW, closed = 2006),
    m("Nintendo Power", 1988, MarkKind.OUTLET, "NP", 0xFFE53935, LogoId.NINTENDO, closed = 2012),
    m("Electronic Games", 1981, MarkKind.OUTLET, "EG", 0xFFE65100, LogoId.NONE, closed = 1985),
    m("Electronic Gaming Monthly", 1989, MarkKind.OUTLET, "EGM", 0xFF6A1B9A, LogoId.NONE),
    m("InfoWorld", 1978, MarkKind.OUTLET, "IW", 0xFF1565C0, LogoId.NONE),
    m("Computerworld", 1967, MarkKind.OUTLET, "CW", 0xFF0D47A1, LogoId.NONE),
    m("The New York Times", 1851, MarkKind.OUTLET, "NYT", 0xFF212121, LogoId.NYT),
    m("Creative Computing", 1974, MarkKind.OUTLET, "CC", 0xFF00695C, LogoId.NONE, closed = 1985),
    m("Zzap!64", 1985, MarkKind.OUTLET, "Z!", 0xFFE53935, LogoId.NONE, closed = 1992),
    m("Smashing Magazine", 2006, MarkKind.OUTLET, "SM", 0xFFE95C33, LogoId.NONE),
    m("Fast Company", 1995, MarkKind.OUTLET, "FC", 0xFFE31C23, LogoId.NONE),
    m("9to5Mac", 2007, MarkKind.OUTLET, "9", 0xFF212121, LogoId.APPLE),
    m("Platformer", 2020, MarkKind.OUTLET, "Pl", 0xFF5C6BC0, LogoId.NONE),
    m("The Information", 2013, MarkKind.OUTLET, "TI", 0xFF212121, LogoId.NONE),
    m("MIT Technology Review", 1899, MarkKind.OUTLET, "TR", 0xFFE53935, LogoId.NONE),
    m("Dexerto", 2016, MarkKind.OUTLET, "Dx", 0xFF7C4DFF, LogoId.NONE),
    m("ESPN Esports", 2016, MarkKind.OUTLET, "ESPN", 0xFFD32F2F, LogoId.NONE),
    m("The Esports Observer", 2015, MarkKind.OUTLET, "EO", 0xFF37474F, LogoId.NONE),

    m("Sequoia Capital", 1972, MarkKind.INVESTOR, "Sq", 0xFF0B3D2E, LogoId.SEQUOIA),
    m("Kleiner Perkins", 1972, MarkKind.INVESTOR, "KP", 0xFF1B5E20, LogoId.KLEINER_PERKINS),
    m("Venrock", 1969, MarkKind.INVESTOR, "Vr", 0xFF37474F, LogoId.NONE),
    m("Greylock Partners", 1965, MarkKind.INVESTOR, "GL", 0xFF263238, LogoId.NONE),
    m("NEA", 1977, MarkKind.INVESTOR, "NEA", 0xFF0D47A1, LogoId.NONE),
    m("Accel", 1983, MarkKind.INVESTOR, "Ac", 0xFF1565C0, LogoId.NONE),
    m("Benchmark", 1995, MarkKind.INVESTOR, "Bm", 0xFF212121, LogoId.NONE),
    m("Index Ventures", 1996, MarkKind.INVESTOR, "Ix", 0xFFE53935, LogoId.NONE),
    m("Insight Partners", 1995, MarkKind.INVESTOR, "In", 0xFF0D47A1, LogoId.NONE),
    m("Coatue", 1999, MarkKind.INVESTOR, "Co", 0xFF1A237E, LogoId.NONE),
    m("Lightspeed Venture Partners", 2000, MarkKind.INVESTOR, "LS", 0xFF6A1B9A, LogoId.NONE),
    m("General Catalyst", 2000, MarkKind.INVESTOR, "GC", 0xFF00695C, LogoId.NONE),
    m("Tiger Global", 2001, MarkKind.INVESTOR, "TG", 0xFFFF6F00, LogoId.NONE),
    m("First Round Capital", 2004, MarkKind.INVESTOR, "FR", 0xFF1565C0, LogoId.NONE),
    m("Y Combinator", 2005, MarkKind.INVESTOR, "YC", 0xFFF26522, LogoId.Y_COMBINATOR, fg = 0xFF212121),
    m("Andreessen Horowitz", 2009, MarkKind.INVESTOR, "a16z", 0xFF212121, LogoId.A16Z),
    m("SV Angel", 2009, MarkKind.INVESTOR, "SV", 0xFF455A64, LogoId.NONE),
    m("Initialized Capital", 2010, MarkKind.INVESTOR, "Ic", 0xFF5E35B1, LogoId.NONE),
    m("Thrive Capital", 2010, MarkKind.INVESTOR, "Th", 0xFF004D40, LogoId.NONE),
    m("SoftBank Vision Fund", 2017, MarkKind.INVESTOR, "SB", 0xFFA6CE39, LogoId.SOFTBANK, fg = 0xFF212121),
    m("Fidelity", 1946, MarkKind.INVESTOR, "F", 0xFF28A745, LogoId.NONE),
    m("T. Rowe Price", 1937, MarkKind.INVESTOR, "TRP", 0xFF003366, LogoId.NONE),
    m("BlackRock", 1988, MarkKind.INVESTOR, "BR", 0xFF212121, LogoId.NONE),
    m("Mubadala", 2002, MarkKind.INVESTOR, "Mu", 0xFF8B1538, LogoId.NONE),
    m("Bessemer Venture Partners", 1911, MarkKind.INVESTOR, "BV", 0xFF0D47A1, LogoId.NONE),
    m("Mayfield Fund", 1969, MarkKind.INVESTOR, "Mf", 0xFF4E342E, LogoId.NONE),
)

private val MarksByKey: Map<String, CompanyMark> = Marks.associateBy { companyKey(it.name) }

/** Publishers that can appear on deal offers, year-filtered via [publishersIn]. */
internal val PublisherRoster = listOf(
    "Atari", "Activision Blizzard", "Electronic Arts", "Ubisoft", "Nintendo", "Sega", "Capcom", "Konami",
    "Square Enix", "Bandai Namco", "Take-Two Interactive", "Rockstar Games", "Bethesda Softworks",
    "Sierra On-Line", "LucasArts", "Origin Systems", "Broderbund", "Eidos", "Acclaim", "THQ", "THQ Nordic",
    "Devolver Digital", "Annapurna Interactive", "Xbox Game Studios", "PlayStation Studios", "Epic Games",
    "Valve", "CD Projekt Red", "Supercell",
)

private val InvestorStages = listOf(
    listOf("Y Combinator", "First Round Capital", "SV Angel", "Initialized Capital", "NEA", "Venrock", "Mayfield Fund", "Bessemer Venture Partners"),
    listOf("Sequoia Capital", "Andreessen Horowitz", "Benchmark", "Accel", "Kleiner Perkins", "Greylock Partners"),
    listOf("Lightspeed Venture Partners", "Kleiner Perkins", "Index Ventures", "General Catalyst"),
    listOf("Tiger Global", "Coatue", "Thrive Capital", "Insight Partners"),
    listOf("SoftBank Vision Fund", "Fidelity", "T. Rowe Price"),
    listOf("SoftBank Vision Fund", "BlackRock", "Mubadala"),
)

internal val BrandYears = mapOf(
    "Nike" to 1971, "Coca-Cola" to 1892, "IKEA" to 1943, "Adidas" to 1949, "LEGO" to 1932,
    "Starbucks" to 1971, "McDonald's" to 1955, "Toyota" to 1937, "Delta Air Lines" to 1929,
    "Patagonia" to 1973, "Red Bull" to 1987, "Sephora" to 1969, "Disney" to 1923,
    "Kellogg's" to 1906, "JPMorgan Chase" to 2000, "Spotify" to 2006, "Airbnb" to 2008,
    "Tesla" to 2003, "Netflix" to 1997, "Shopify" to 2006, "Uber" to 2009, "Duolingo" to 2011,
    "Peloton" to 2012, "the Smithsonian" to 1846,
)

private val PlatformHolders = listOf(
    "Atari", "Nintendo", "Sega", "Sony", "Microsoft", "Valve", "Apple", "Google", "Commodore",
)
