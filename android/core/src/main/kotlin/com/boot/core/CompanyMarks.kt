package com.boot.core

/*
 * Recognizable firms and geometric monogram marks for the UI. Names are historically year-gated so
 * a 1977 run never surfaces modern-only publishers, VCs or outlets. Marks are Compose-drawn
 * initials on simple shapes (inspired by brand colors) — not scraped trademark artwork.
 */

/** What kind of firm a mark stands for. */
enum class MarkKind { RIVAL, PUBLISHER, INVESTOR, OUTLET, PLATFORM, TECH, STUDIO }

/** Simple geometric badge behind the monogram. */
enum class MarkShape { DISC, ROUNDED_SQUARE, DIAMOND, HEX, PILL }

/**
 * Visual identity for a company name. [bg]/[fg] are ARGB longs for Compose Color(…) without
 * pulling Android graphics into :core.
 */
data class CompanyMark(
    val name: String,
    val founded: Int,
    val kind: MarkKind,
    val initials: String,
    val bg: Long,
    val fg: Long = 0xFFFFFFFF,
    val shape: MarkShape = MarkShape.ROUNDED_SQUARE,
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
            "Google" -> "Alphabet"
            "Facebook" -> "Meta"
            "Snapchat" -> "Snap"
            "FIFA" -> "Electronic Arts"
            else -> trimmed
        }
    }
}

/** Mark for [name], or a neutral fallback monogram when unknown. */
fun companyMark(name: String): CompanyMark {
    val key = companyKey(name)
    MarksByKey[key]?.let { return it.copy(name = name) }
    MarksByKey[name]?.let { return it }
    // Alias table covers historic outlet / rival spellings.
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

private fun fallbackMark(name: String): CompanyMark {
    val words = name.split(' ', '-', '&', '.').filter { it.isNotBlank() && it.lowercase() !in SkipWords }
    val initials = when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.isNotEmpty() -> words[0].take(2).uppercase()
        else -> "?"
    }
    val hue = (name.hashCode().toLong() and 0xFFFFFF)
    val bg = 0xFF000000 or hue
    return CompanyMark(name, 0, MarkKind.STUDIO, initials, bg, shape = MarkShape.DISC)
}

private val SkipWords = setOf("the", "of", "and", "inc", "llc", "ltd", "corp", "co", "games", "studios", "studio", "interactive", "entertainment", "capital", "partners", "ventures", "fund")

// Catalog

private val Marks: List<CompanyMark> = listOf(
    // Publishers / studios
    CompanyMark("Atari", 1972, MarkKind.PUBLISHER, "A", 0xFFE53935, closed = 2013),
    CompanyMark("Activision", 1979, MarkKind.PUBLISHER, "AV", 0xFF1565C0),
    CompanyMark("Activision Blizzard", 1979, MarkKind.PUBLISHER, "AB", 0xFF0D47A1),
    CompanyMark("Electronic Arts", 1982, MarkKind.PUBLISHER, "EA", 0xFF000000),
    CompanyMark("Ubisoft", 1986, MarkKind.PUBLISHER, "Ubi", 0xFF1A237E, shape = MarkShape.PILL),
    CompanyMark("Nintendo", 1889, MarkKind.PUBLISHER, "N", 0xFFE53935, shape = MarkShape.DISC),
    CompanyMark("Sega", 1960, MarkKind.PUBLISHER, "SE", 0xFF1565C0),
    CompanyMark("Capcom", 1979, MarkKind.PUBLISHER, "C", 0xFFC62828),
    CompanyMark("Konami", 1969, MarkKind.PUBLISHER, "K", 0xFFB71C1C),
    CompanyMark("Square", 1986, MarkKind.PUBLISHER, "SQ", 0xFF212121),
    CompanyMark("Square Enix", 1986, MarkKind.PUBLISHER, "SE", 0xFF212121),
    CompanyMark("Bandai Namco", 1955, MarkKind.PUBLISHER, "BN", 0xFFF9A825, fg = 0xFF212121),
    CompanyMark("Namco", 1955, MarkKind.PUBLISHER, "N", 0xFFF9A825, fg = 0xFF212121),
    CompanyMark("Take-Two Interactive", 1993, MarkKind.PUBLISHER, "T2", 0xFF6A1B9A),
    CompanyMark("Rockstar Games", 1998, MarkKind.PUBLISHER, "R", 0xFFFFD600, fg = 0xFF212121, shape = MarkShape.DIAMOND),
    CompanyMark("Bethesda Softworks", 1986, MarkKind.PUBLISHER, "B", 0xFF37474F),
    CompanyMark("Devolver Digital", 2009, MarkKind.PUBLISHER, "DD", 0xFFE65100),
    CompanyMark("Annapurna Interactive", 2016, MarkKind.PUBLISHER, "AI", 0xFF4E342E),
    CompanyMark("THQ", 1990, MarkKind.PUBLISHER, "THQ", 0xFF00695C, closed = 2013),
    CompanyMark("THQ Nordic", 2016, MarkKind.PUBLISHER, "TN", 0xFF00695C),
    CompanyMark("Sierra On-Line", 1979, MarkKind.PUBLISHER, "Si", 0xFF2E7D32, closed = 2008),
    CompanyMark("LucasArts", 1982, MarkKind.PUBLISHER, "LA", 0xFF455A64, closed = 2013),
    CompanyMark("Origin Systems", 1983, MarkKind.PUBLISHER, "OS", 0xFF5D4037, closed = 2004),
    CompanyMark("Infocom", 1979, MarkKind.PUBLISHER, "In", 0xFF455A64, closed = 1989),
    CompanyMark("Broderbund", 1980, MarkKind.PUBLISHER, "Br", 0xFF1565C0, closed = 1998),
    CompanyMark("Eidos", 1990, MarkKind.PUBLISHER, "Ei", 0xFF263238, closed = 2009),
    CompanyMark("Acclaim", 1987, MarkKind.PUBLISHER, "Ac", 0xFFE65100, closed = 2004),
    CompanyMark("Midway", 1988, MarkKind.PUBLISHER, "M", 0xFF212121, closed = 2009),
    CompanyMark("Valve", 1996, MarkKind.PUBLISHER, "V", 0xFF212121),
    CompanyMark("Epic Games", 1991, MarkKind.PUBLISHER, "EG", 0xFF212121),
    CompanyMark("CD Projekt Red", 2002, MarkKind.STUDIO, "CD", 0xFFE53935),
    CompanyMark("CD Projekt", 1994, MarkKind.PUBLISHER, "CD", 0xFFE53935),
    CompanyMark("FromSoftware", 1986, MarkKind.STUDIO, "FS", 0xFF212121),
    CompanyMark("Supercell", 2010, MarkKind.PUBLISHER, "SC", 0xFFFFEB3B, fg = 0xFF212121),
    CompanyMark("PlayStation Studios", 1993, MarkKind.PUBLISHER, "PS", 0xFF1565C0, shape = MarkShape.DISC),
    CompanyMark("Xbox Game Studios", 2000, MarkKind.PUBLISHER, "X", 0xFF2E7D32, shape = MarkShape.DISC),
    CompanyMark("Sony", 1946, MarkKind.PLATFORM, "S", 0xFF212121),
    CompanyMark("Sony Interactive", 1993, MarkKind.PUBLISHER, "SI", 0xFF1565C0),
    CompanyMark("Microsoft", 1975, MarkKind.TECH, "MS", 0xFF1565C0),
    CompanyMark("Tencent", 1998, MarkKind.PUBLISHER, "T", 0xFF1565C0),

    // Platform holders
    CompanyMark("Apple", 1976, MarkKind.PLATFORM, "A", 0xFF212121, shape = MarkShape.DISC),
    CompanyMark("Google", 1998, MarkKind.PLATFORM, "G", 0xFF4285F4),
    CompanyMark("Alphabet", 1998, MarkKind.TECH, "A", 0xFF4285F4),
    CompanyMark("Steam", 2003, MarkKind.PLATFORM, "S", 0xFF171A21),
    CompanyMark("Commodore", 1976, MarkKind.PLATFORM, "C=", 0xFF1565C0, closed = 1994),
    CompanyMark("IBM", 1911, MarkKind.TECH, "IBM", 0xFF0D47A1, shape = MarkShape.PILL),
    CompanyMark("Dell", 1984, MarkKind.TECH, "D", 0xFF007DB8),
    CompanyMark("Nokia", 1865, MarkKind.TECH, "N", 0xFF124191),
    CompanyMark("Samsung", 1969, MarkKind.TECH, "S", 0xFF1565C0),
    CompanyMark("Nothing", 2020, MarkKind.TECH, "Nt", 0xFF212121),

    // Rivals / tech peers
    CompanyMark("Meta", 2004, MarkKind.RIVAL, "Me", 0xFF0866FF, shape = MarkShape.DISC),
    CompanyMark("Facebook", 2004, MarkKind.RIVAL, "f", 0xFF1877F2, shape = MarkShape.DISC),
    CompanyMark("MySpace", 2003, MarkKind.RIVAL, "My", 0xFF003399, closed = 2010),
    CompanyMark("TikTok", 2016, MarkKind.RIVAL, "TT", 0xFF010101),
    CompanyMark("X", 2006, MarkKind.RIVAL, "X", 0xFF000000),
    CompanyMark("Snap", 2011, MarkKind.RIVAL, "Sn", 0xFFFFFC00, fg = 0xFF212121, shape = MarkShape.DISC),
    CompanyMark("Reddit", 2005, MarkKind.RIVAL, "r/", 0xFFFF4500),
    CompanyMark("Discord", 2015, MarkKind.RIVAL, "D", 0xFF5865F2),
    CompanyMark("OpenAI", 2015, MarkKind.RIVAL, "AI", 0xFF10A37F, shape = MarkShape.DISC),
    CompanyMark("Google DeepMind", 2010, MarkKind.RIVAL, "DM", 0xFF4285F4),
    CompanyMark("Anthropic", 2021, MarkKind.RIVAL, "An", 0xFFD4A27F, fg = 0xFF212121),
    CompanyMark("xAI", 2023, MarkKind.RIVAL, "x", 0xFF212121),
    CompanyMark("Mistral AI", 2023, MarkKind.RIVAL, "M", 0xFFFF7000),
    CompanyMark("Meta AI", 2013, MarkKind.RIVAL, "Me", 0xFF0866FF),
    CompanyMark("Digital Research", 1976, MarkKind.RIVAL, "DR", 0xFF5D4037, closed = 1991),
    CompanyMark("Canonical", 2004, MarkKind.TECH, "Ub", 0xFFE95420),
    CompanyMark("Red Hat", 1993, MarkKind.TECH, "RH", 0xFFEE0000),
    CompanyMark("Razorfish", 1995, MarkKind.RIVAL, "Rf", 0xFFE91E63, closed = 2009),
    CompanyMark("Squarespace", 2003, MarkKind.RIVAL, "Sq", 0xFF000000),
    CompanyMark("Wix", 2006, MarkKind.RIVAL, "Wix", 0xFFFAAD4D, fg = 0xFF212121),
    CompanyMark("Webflow", 2013, MarkKind.RIVAL, "Wf", 0xFF4353FF),
    CompanyMark("WordPress.com", 2005, MarkKind.RIVAL, "Wp", 0xFF21759B),
    CompanyMark("Shopify", 2006, MarkKind.RIVAL, "Sh", 0xFF96BF48, fg = 0xFF212121),
    CompanyMark("Duolingo", 2011, MarkKind.RIVAL, "Duo", 0xFF58CC02, fg = 0xFF212121),
    CompanyMark("Spotify", 2006, MarkKind.RIVAL, "Sp", 0xFF1DB954, shape = MarkShape.DISC),
    CompanyMark("Calm", 2012, MarkKind.RIVAL, "Ca", 0xFF2180D8),
    CompanyMark("Strava", 2009, MarkKind.RIVAL, "St", 0xFFFC4C02),
    CompanyMark("Notion", 2016, MarkKind.RIVAL, "No", 0xFF212121),
    CompanyMark("NVIDIA", 1993, MarkKind.TECH, "N", 0xFF76B900, fg = 0xFF212121),
    CompanyMark("Amazon", 1994, MarkKind.TECH, "a", 0xFFFF9900, fg = 0xFF212121),
    CompanyMark("Netflix", 1997, MarkKind.TECH, "N", 0xFFE50914),
    CompanyMark("Tesla", 2003, MarkKind.TECH, "T", 0xFFCC0000),
    CompanyMark("TSMC", 1987, MarkKind.TECH, "TSMC", 0xFF1A237E, shape = MarkShape.PILL),
    CompanyMark("Roblox", 2004, MarkKind.TECH, "R", 0xFFE2231A),
    CompanyMark("AOL", 1985, MarkKind.TECH, "AOL", 0xFF00A4E4, closed = 2015),
    CompanyMark("Yahoo", 1995, MarkKind.TECH, "Y!", 0xFF6001D2),
    CompanyMark("BlackBerry", 1984, MarkKind.TECH, "BB", 0xFF212121),
    CompanyMark("Netscape", 1994, MarkKind.TECH, "N", 0xFF1A237E, closed = 2003),
    CompanyMark("GeoCities", 1994, MarkKind.TECH, "GC", 0xFF00897B, closed = 2009),
    CompanyMark("Compaq", 1982, MarkKind.TECH, "Cq", 0xFFB71C1C, closed = 2002),
    CompanyMark("Palm", 1992, MarkKind.TECH, "P", 0xFF1565C0, closed = 2010),
    CompanyMark("IBM Watson", 2011, MarkKind.RIVAL, "W", 0xFF0D47A1),
    CompanyMark("BeOS", 1991, MarkKind.RIVAL, "Be", 0xFFE65100, closed = 2001),
    CompanyMark("Palm OS", 1996, MarkKind.RIVAL, "P", 0xFF1565C0, closed = 2010),
    CompanyMark("Symbian", 1998, MarkKind.RIVAL, "Sy", 0xFF00695C, closed = 2012),
    CompanyMark("Rovio", 2003, MarkKind.RIVAL, "Rv", 0xFFD32F2F),
    CompanyMark("King", 2003, MarkKind.RIVAL, "K", 0xFF7B1FA2),
    CompanyMark("Zynga", 2007, MarkKind.RIVAL, "Z", 0xFF6D4C41),
    CompanyMark("Friendster", 2002, MarkKind.RIVAL, "Fr", 0xFFFF6F00, closed = 2008),
    CompanyMark("Pinterest", 2010, MarkKind.TECH, "P", 0xFFE60023),

    // Outlets
    CompanyMark("IGN", 1996, MarkKind.OUTLET, "IGN", 0xFFD32F2F, shape = MarkShape.PILL),
    CompanyMark("GameSpot", 1996, MarkKind.OUTLET, "GS", 0xFFF9A825, fg = 0xFF212121),
    CompanyMark("Game Informer", 1991, MarkKind.OUTLET, "GI", 0xFF1565C0),
    CompanyMark("Eurogamer", 1999, MarkKind.OUTLET, "EG", 0xFF2E7D32),
    CompanyMark("PC Gamer", 1993, MarkKind.OUTLET, "PCG", 0xFF00695C, shape = MarkShape.PILL),
    CompanyMark("Edge", 1993, MarkKind.OUTLET, "E", 0xFF212121),
    CompanyMark("Kotaku", 2004, MarkKind.OUTLET, "Ko", 0xFFC2185B),
    CompanyMark("Polygon", 2012, MarkKind.OUTLET, "Pg", 0xFFE040FB, fg = 0xFF212121),
    CompanyMark("TechCrunch", 2005, MarkKind.OUTLET, "TC", 0xFF0A9E01),
    CompanyMark("The Verge", 2011, MarkKind.OUTLET, "V", 0xFFE01A22),
    CompanyMark("Wired", 1993, MarkKind.OUTLET, "W", 0xFF212121),
    CompanyMark("Ars Technica", 1998, MarkKind.OUTLET, "Ars", 0xFFFF4E00, fg = 0xFF212121),
    CompanyMark("Engadget", 2004, MarkKind.OUTLET, "En", 0xFF6B2D5C),
    CompanyMark("Bloomberg", 1981, MarkKind.OUTLET, "B", 0xFF2800D7),
    CompanyMark("Reuters", 1851, MarkKind.OUTLET, "R", 0xFFFF8000, fg = 0xFF212121),
    CompanyMark("The Wall Street Journal", 1889, MarkKind.OUTLET, "WSJ", 0xFF0274B6, shape = MarkShape.PILL),
    CompanyMark("Financial Times", 1888, MarkKind.OUTLET, "FT", 0xFFFFF1E5, fg = 0xFF212121),
    CompanyMark("CNBC", 1989, MarkKind.OUTLET, "CNBC", 0xFF005594, shape = MarkShape.PILL),
    CompanyMark("Byte", 1975, MarkKind.OUTLET, "By", 0xFF1565C0, closed = 1998),
    CompanyMark("Computer Gaming World", 1981, MarkKind.OUTLET, "CGW", 0xFF2E7D32, closed = 2006),
    CompanyMark("Nintendo Power", 1988, MarkKind.OUTLET, "NP", 0xFFE53935, closed = 2012),
    CompanyMark("Electronic Games", 1981, MarkKind.OUTLET, "EG", 0xFFE65100, closed = 1985),
    CompanyMark("Electronic Gaming Monthly", 1989, MarkKind.OUTLET, "EGM", 0xFF6A1B9A),
    CompanyMark("InfoWorld", 1978, MarkKind.OUTLET, "IW", 0xFF1565C0),
    CompanyMark("Computerworld", 1967, MarkKind.OUTLET, "CW", 0xFF0D47A1),
    CompanyMark("The New York Times", 1851, MarkKind.OUTLET, "NYT", 0xFF212121, shape = MarkShape.PILL),
    CompanyMark("Creative Computing", 1974, MarkKind.OUTLET, "CC", 0xFF00695C, closed = 1985),
    CompanyMark("Zzap!64", 1985, MarkKind.OUTLET, "Z!", 0xFFE53935, closed = 1992),
    CompanyMark("Smashing Magazine", 2006, MarkKind.OUTLET, "SM", 0xFFE95C33),
    CompanyMark("Fast Company", 1995, MarkKind.OUTLET, "FC", 0xFFE31C23),
    CompanyMark("9to5Mac", 2007, MarkKind.OUTLET, "9", 0xFF212121),
    CompanyMark("Platformer", 2020, MarkKind.OUTLET, "Pl", 0xFF5C6BC0),
    CompanyMark("The Information", 2013, MarkKind.OUTLET, "TI", 0xFF212121),
    CompanyMark("MIT Technology Review", 1899, MarkKind.OUTLET, "TR", 0xFFE53935),
    CompanyMark("Dexerto", 2016, MarkKind.OUTLET, "Dx", 0xFF7C4DFF),
    CompanyMark("ESPN Esports", 2016, MarkKind.OUTLET, "ESPN", 0xFFD32F2F, shape = MarkShape.PILL),
    CompanyMark("The Esports Observer", 2015, MarkKind.OUTLET, "EO", 0xFF37474F),

    // Investors
    CompanyMark("Sequoia Capital", 1972, MarkKind.INVESTOR, "Sq", 0xFF0B3D2E),
    CompanyMark("Kleiner Perkins", 1972, MarkKind.INVESTOR, "KP", 0xFF1B5E20),
    CompanyMark("Venrock", 1969, MarkKind.INVESTOR, "Vr", 0xFF37474F),
    CompanyMark("Greylock Partners", 1965, MarkKind.INVESTOR, "GL", 0xFF263238),
    CompanyMark("NEA", 1977, MarkKind.INVESTOR, "NEA", 0xFF0D47A1, shape = MarkShape.PILL),
    CompanyMark("Accel", 1983, MarkKind.INVESTOR, "Ac", 0xFF1565C0),
    CompanyMark("Benchmark", 1995, MarkKind.INVESTOR, "Bm", 0xFF212121),
    CompanyMark("Index Ventures", 1996, MarkKind.INVESTOR, "Ix", 0xFFE53935),
    CompanyMark("Insight Partners", 1995, MarkKind.INVESTOR, "In", 0xFF0D47A1),
    CompanyMark("Coatue", 1999, MarkKind.INVESTOR, "Co", 0xFF1A237E),
    CompanyMark("Lightspeed Venture Partners", 2000, MarkKind.INVESTOR, "LS", 0xFF6A1B9A),
    CompanyMark("General Catalyst", 2000, MarkKind.INVESTOR, "GC", 0xFF00695C),
    CompanyMark("Tiger Global", 2001, MarkKind.INVESTOR, "TG", 0xFFFF6F00),
    CompanyMark("First Round Capital", 2004, MarkKind.INVESTOR, "FR", 0xFF1565C0),
    CompanyMark("Y Combinator", 2005, MarkKind.INVESTOR, "YC", 0xFFF26522, fg = 0xFF212121),
    CompanyMark("Andreessen Horowitz", 2009, MarkKind.INVESTOR, "a16z", 0xFF212121, shape = MarkShape.PILL),
    CompanyMark("SV Angel", 2009, MarkKind.INVESTOR, "SV", 0xFF455A64),
    CompanyMark("Initialized Capital", 2010, MarkKind.INVESTOR, "Ic", 0xFF5E35B1),
    CompanyMark("Thrive Capital", 2010, MarkKind.INVESTOR, "Th", 0xFF004D40),
    CompanyMark("SoftBank Vision Fund", 2017, MarkKind.INVESTOR, "SB", 0xFFA6CE39, fg = 0xFF212121),
    CompanyMark("Fidelity", 1946, MarkKind.INVESTOR, "F", 0xFF28A745),
    CompanyMark("T. Rowe Price", 1937, MarkKind.INVESTOR, "TRP", 0xFF003366, shape = MarkShape.PILL),
    CompanyMark("BlackRock", 1988, MarkKind.INVESTOR, "BR", 0xFF212121),
    CompanyMark("Mubadala", 2002, MarkKind.INVESTOR, "Mu", 0xFF8B1538),
    CompanyMark("Bessemer Venture Partners", 1911, MarkKind.INVESTOR, "BV", 0xFF0D47A1),
    CompanyMark("Mayfield Fund", 1969, MarkKind.INVESTOR, "Mf", 0xFF4E342E),
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

/** VC leads by round stage; each name is filtered by founded year in [investorsIn]. */
private val InvestorStages = listOf(
    listOf("Y Combinator", "First Round Capital", "SV Angel", "Initialized Capital", "NEA", "Venrock", "Mayfield Fund", "Bessemer Venture Partners"),
    listOf("Sequoia Capital", "Andreessen Horowitz", "Benchmark", "Accel", "Kleiner Perkins", "Greylock Partners"),
    listOf("Lightspeed Venture Partners", "Kleiner Perkins", "Index Ventures", "General Catalyst"),
    listOf("Tiger Global", "Coatue", "Thrive Capital", "Insight Partners"),
    listOf("SoftBank Vision Fund", "Fidelity", "T. Rowe Price"),
    listOf("SoftBank Vision Fund", "BlackRock", "Mubadala"),
)

/** Consumer brands for agency briefs, with founding years. */
internal val BrandYears = mapOf(
    "Nike" to 1971, "Coca-Cola" to 1892, "IKEA" to 1943, "Adidas" to 1949, "LEGO" to 1932,
    "Starbucks" to 1971, "McDonald's" to 1955, "Toyota" to 1937, "Delta Air Lines" to 1929,
    "Patagonia" to 1973, "Red Bull" to 1987, "Sephora" to 1969, "Disney" to 1923,
    "Kellogg's" to 1906, "JPMorgan Chase" to 2000, "Spotify" to 2006, "Airbnb" to 2008,
    "Tesla" to 2003, "Netflix" to 1997, "Shopify" to 2006, "Uber" to 2009, "Duolingo" to 2011,
    "Peloton" to 2012, "the Smithsonian" to 1846,
)

/** Storefront / console holders. */
private val PlatformHolders = listOf(
    "Atari", "Nintendo", "Sega", "Sony", "Microsoft", "Valve", "Apple", "Google", "Commodore",
)
