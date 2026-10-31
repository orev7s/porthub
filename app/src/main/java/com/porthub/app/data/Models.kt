package com.porthub.app.data

import kotlinx.serialization.Serializable

/**
 * Root feed document. One file, every port we know about.
 * Published to GitHub raw; consumed by the app and by anyone else who wants it.
 */
@Serializable
data class Catalog(
    val version: Int = 1,
    val generatedAt: String? = null,
    val games: List<Game> = emptyList(),
)

@Serializable
data class Game(
    /** Stable slug, e.g. "ship-of-harkinian". Used for deep links + favourites. */
    val id: String,
    val title: String,
    /** The retail game this port reconstructs, e.g. "The Legend of Zelda: Ocarina of Time". */
    val originalTitle: String? = null,
    val year: Int? = null,
    /** Original platforms: N64, PS1, PC ... (not Android). */
    val platforms: List<String> = emptyList(),
    val developer: String? = null,
    /** recompilation | decompilation | engine | source-port */
    val portType: String = "unknown",
    /** stable | beta | alpha | wip | archived */
    val status: String = "unknown",
    val engine: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    /** Steam appid of the original game, when it exists on Steam. Gives us free official artwork. */
    val steamAppId: Int? = null,
    val android: AndroidInfo = AndroidInfo(),
    val artwork: Artwork = Artwork(),
    val links: Links = Links(),
    val downloads: List<Download> = emptyList(),
    val license: String? = null,
    val lastUpdated: String? = null,
    val stars: Int? = null,
) {
    /** Everything the search box matches against. */
    val searchBlob: String
        get() = buildString {
            append(title).append(' ')
            originalTitle?.let { append(it).append(' ') }
            append(description).append(' ')
            append(tags.joinToString(" ")).append(' ')
            append(platforms.joinToString(" ")).append(' ')
            engine?.let { append(it) }
        }.lowercase()

    /** Feed artwork wins; otherwise Steam's public CDN; otherwise the app draws a placeholder. */
    val coverUrl: String?
        get() = artwork.cover ?: SteamArt.cover(steamAppId)

    val heroUrl: String?
        get() = artwork.hero ?: artwork.cover ?: SteamArt.hero(steamAppId)

    val monogram: String
        get() = title.split(" ")
            .filter { it.isNotBlank() && it.first().isLetterOrDigit() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
}

@Serializable
data class AndroidInfo(
    val supported: Boolean = true,
    val minAndroid: String? = null,
    /** arm64-v8a, armeabi-v7a, x86_64 */
    val abis: List<String> = emptyList(),
    /** none | owned | required — does the user have to supply their own copy of the game? */
    val requiresGameFiles: String = "unknown",
    val notes: String? = null,
)

@Serializable
data class Artwork(
    val cover: String? = null,
    val hero: String? = null,
    val screenshots: List<String> = emptyList(),
    /** Hex accent used when there is no artwork, keeps every card looking intentional. */
    val accent: String? = null,
)

@Serializable
data class Links(
    val repo: String? = null,
    val releases: String? = null,
    val website: String? = null,
    val discord: String? = null,
    val wiki: String? = null,
)

@Serializable
data class Download(
    val label: String,
    /** apk | zip | store | source */
    val kind: String = "apk",
    val url: String,
    val version: String? = null,
    val sizeMb: Double? = null,
    val abi: String? = null,
)

/**
 * Steam's public CDN exposes official capsule art for any appid — no key, no scraping.
 * It only covers games that are actually on Steam, which is why every lookup is nullable
 * and the UI falls back to a generated tile.
 */
object SteamArt {
    private const val BASE = "https://cdn.cloudflare.steamstatic.com/steam/apps"

    /** Portrait capsule, 600x900 — the one that matches a phone card. */
    fun cover(appId: Int?): String? = appId?.let { "$BASE/$it/library_600x900.jpg" }

    /** Wide hero, 1920x620 — detail screen header. */
    fun hero(appId: Int?): String? = appId?.let { "$BASE/$it/library_hero.jpg" }

    /** 460x215 header, cheapest option when a small thumbnail is all we need. */
    fun header(appId: Int?): String? = appId?.let { "$BASE/$it/header.jpg" }
}

/** UI-facing label helpers, kept out of the data classes so serialization stays dumb. */
object PortType {
    const val RECOMP = "recompilation"
    const val DECOMP = "decompilation"
    const val ENGINE = "engine"
    const val SOURCE_PORT = "source-port"

    fun label(raw: String): String = when (raw) {
        RECOMP -> "Recomp"
        DECOMP -> "Decomp"
        ENGINE -> "Engine"
        SOURCE_PORT -> "Source port"
        else -> "Port"
    }
}

object PortStatus {
    const val STABLE = "stable"
    const val BETA = "beta"
    const val ALPHA = "alpha"
    const val WIP = "wip"

    fun label(raw: String): String = when (raw) {
        STABLE -> "Stable"
        BETA -> "Beta"
        ALPHA -> "Alpha"
        WIP -> "In progress"
        else -> "Unknown"
    }
}

object GameFiles {
    const val NONE = "none"
    const val OWNED = "owned"
    const val REQUIRED = "required"

    fun label(raw: String): String = when (raw) {
        NONE -> "Standalone — no game files needed"
        OWNED, REQUIRED -> "Bring your own copy of the original game"
        else -> "Game file requirement unknown"
    }
}
