package com.porthub.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the catalog, caches it on disk, and never leaves the UI without something to show:
 * network -> disk cache -> bundled seed.
 */
class CatalogRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("porthub", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val cacheFile = File(context.filesDir, "catalog.cache.json")

    var feedUrl: String
        get() = prefs.getString(KEY_FEED, DEFAULT_FEED) ?: DEFAULT_FEED
        set(value) {
            prefs.edit().putString(KEY_FEED, value.ifBlank { DEFAULT_FEED }).apply()
        }

    suspend fun load(forceRefresh: Boolean = false): Result<Pair<List<Game>, Source>> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                readCache()?.let { return@withContext Result.success(it to Source.CACHE) }
            }

            runCatching { fetchRemote() }
                .onSuccess { games ->
                    if (games.isNotEmpty()) {
                        cacheFile.writeText(json.encodeToString(Catalog.serializer(), Catalog(games = games)))
                        return@withContext Result.success(games to Source.NETWORK)
                    }
                }

            readCache()?.let { return@withContext Result.success(it to Source.CACHE) }

            runCatching { readSeed() }
                .map { it to Source.BUNDLED }
        }

    private fun readCache(): List<Game>? = runCatching {
        if (!cacheFile.exists()) return null
        val catalog = json.decodeFromString(Catalog.serializer(), cacheFile.readText())
        catalog.games.ifEmpty { null }
    }.getOrNull()

    private fun readSeed(): List<Game> {
        val text = context.assets.open("seed.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(Catalog.serializer(), text).games
    }

    private fun fetchRemote(): List<Game> {
        val url = URL(feedUrl.trim())
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "PortHub-Android/0.1")
            instanceFollowRedirects = true
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Feed returned HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val catalog = json.decodeFromString(Catalog.serializer(), body)
            return catalog.games
        } finally {
            conn.disconnect()
        }
    }

    enum class Source { NETWORK, CACHE, BUNDLED }

    companion object {
        private const val KEY_FEED = "feed_url"

        const val DEFAULT_FEED =
            "https://raw.githubusercontent.com/orev7s/porthub-catalog/main/games.json"
    }
}
