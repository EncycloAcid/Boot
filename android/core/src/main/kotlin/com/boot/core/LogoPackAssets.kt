package com.boot.core

/*
 * Maps CompanyMark / LogoId → licensed pack PNG basename (no extension).
 * Filenames match the user-supplied logos.zip under media/logos/.
 * UI layers load the PNG when present and fall back to BrandLogoDraw silhouettes.
 */

/** Basename under `logos/` for [mark], or null when the pack has no asset. */
fun logoPackBasename(mark: CompanyMark): String? =
    LogoPackById[mark.logo]?.takeIf { mark.logo != LogoId.NONE }
        ?: LogoPackByKey[companyKey(mark.name)]
        ?: LogoPackByKey[mark.name]

/** Basename for a [LogoId], ignoring company-specific overrides. */
fun logoPackBasename(logo: LogoId): String? =
    if (logo == LogoId.NONE) null else LogoPackById[logo]

/** How many catalog marks resolve to a pack PNG. */
fun logoPackCoverageCount(): Int = Marks.count { logoPackBasename(it) != null }

/** Distinct pack basenames referenced by the catalog. */
fun logoPackBasenamesInUse(): Set<String> =
    Marks.mapNotNull { logoPackBasename(it) }.toSet()

private val LogoPackById: Map<LogoId, String> = mapOf(
    LogoId.APPLE to "apple",
    LogoId.MICROSOFT to "microsoft",
    LogoId.GOOGLE to "google",
    LogoId.NINTENDO to "nintendo",
    LogoId.SONY to "sony",
    LogoId.PLAYSTATION to "playstation",
    LogoId.XBOX to "xbox",
    LogoId.SEGA to "sega",
    LogoId.ATARI to "atari",
    LogoId.EA to "ea",
    LogoId.UBISOFT to "ubisoft",
    LogoId.ACTIVISION to "activision",
    LogoId.STEAM to "steam",
    LogoId.VALVE to "valve",
    LogoId.EPIC to "epic-games",
    LogoId.NVIDIA to "nvidia",
    LogoId.AMAZON to "amazon",
    LogoId.META to "meta",
    LogoId.FACEBOOK to "meta",
    LogoId.OPENAI to "openai",
    LogoId.NETFLIX to "netflix",
    LogoId.TESLA to "tesla",
    LogoId.SPOTIFY to "spotify",
    LogoId.DISCORD to "discord",
    LogoId.REDDIT to "reddit",
    LogoId.TIKTOK to "tiktok",
    LogoId.SNAP to "snap",
    LogoId.SEQUOIA to "sequoia-capital",
    LogoId.Y_COMBINATOR to "y-combinator",
    LogoId.A16Z to "andreessen-horowitz",
    LogoId.SOFTBANK to "softbank",
    LogoId.KLEINER_PERKINS to "kleiner-perkins",
    LogoId.IGN to "ign",
    LogoId.TECHCRUNCH to "techcrunch",
    LogoId.THE_VERGE to "the-verge",
    LogoId.WIRED to "wired",
    LogoId.BLOOMBERG to "bloomberg",
    LogoId.REUTERS to "reuters",
    LogoId.CNBC to "cnbc",
    LogoId.IBM to "ibm",
    LogoId.COMMODORE to "commodore",
    LogoId.CAPCOM to "capcom",
    LogoId.KONAMI to "konami",
    LogoId.SQUARE_ENIX to "square-enix",
    LogoId.BANDAI_NAMCO to "bandai-namco",
    LogoId.ROCKSTAR to "rockstar-games",
    LogoId.TAKE_TWO to "take-two",
    LogoId.BETHESDA to "bethesda",
    LogoId.DEVOLVER to "devolver-digital",
    LogoId.CDPR to "cd-projekt-red",
    LogoId.SUPERCELL to "supercell",
    LogoId.CANONICAL to "canonical",
    LogoId.SAMSUNG to "samsung",
    LogoId.DELL to "dell",
    LogoId.NOKIA to "nokia",
    LogoId.ROBLOX to "roblox",
    LogoId.SHOPIFY to "shopify",
    LogoId.DUOLINGO to "duolingo",
    LogoId.WORDPRESS to "wordpress",
    LogoId.SQUARESPACE to "squarespace",
    LogoId.WIX to "wix",
    LogoId.TENCENT to "tencent",
    LogoId.ANTHROPIC to "anthropic",
    LogoId.MYSPACE to "myspace",
    LogoId.AOL to "aol",
    LogoId.YAHOO to "yahoo",
    LogoId.GAMESPOT to "gamespot",
    LogoId.EUROGAMER to "eurogamer",
    LogoId.PC_GAMER to "pc-gamer",
    LogoId.EDGE to "edge",
    LogoId.KOTAKU to "kotaku",
    LogoId.POLYGON to "polygon",
    LogoId.ARS to "ars-technica",
    LogoId.ENGADGET to "engadget",
    LogoId.NYT to "the-new-york-times",
    LogoId.WSJ to "the-wall-street-journal",
    LogoId.FT to "financial-times",
)

/** Name/key overrides for LogoId.NONE firms (and aliases) that still have pack art. */
private val LogoPackByKey: Map<String, String> = mapOf(
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
