package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DramaItem(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String,
    val rating: String,
    val episodeTotal: Int,
    val genre: String,
    val isPremium: Boolean = false,
    val likeCount: Int = 0
)

data class ContinueWatchingItem(
    val film: DramaItem,
    val episodeNumber: Int,
    val progressSeconds: Int,
    val durationSeconds: Int,
    val completed: Boolean = false
) {
    val progressFraction: Float
        get() = if (durationSeconds > 0) {
            (progressSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

data class HomeFeed(
    val hero: DramaItem,
    val continueWatching: List<ContinueWatchingItem>,
    val trending: List<DramaItem>,       // Popular tab
    val topRated: DramaItem,
    val moreLikeThis: List<DramaItem>,   // Popular tab secondary
    val newReleases: List<DramaItem>,    // New tab
    val ranking: List<DramaItem>,        // Ranking tab (Weekly Top 20)
    val categories: List<DramaItem>,     // Categories tab
    val featured: List<DramaItem>,       // Featured Highlights section
    val hotTags: List<String> = emptyList()
) {
    companion object
}

class HomeRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
    companion object {
        private val _prefetchedFeed = MutableStateFlow<HomeFeed?>(null)
        val prefetchedFeed: StateFlow<HomeFeed?> = _prefetchedFeed.asStateFlow()
    }
    private val cacheStore = HomeCacheStore(context.applicationContext)
    private val savedWatchListStore = SavedWatchListStore(context.applicationContext)
    private val savedWatchHistoryStore = SavedWatchHistoryStore(context.applicationContext)

    fun savedWatchListIds(): Set<Int> = savedWatchListStore.savedIds()

    fun savedWatchListItems(): List<DramaItem> = savedWatchListStore.readItems()

    fun savedWatchHistoryItems(): List<ContinueWatchingItem> = savedWatchHistoryStore.readItems()

    fun cachedCatalogItems(): List<DramaItem> {
        val feed = _prefetchedFeed.value ?: cacheStore.readFeedForCurrentWindow()
        return feed?.let(::withLocalWatchState)?.allCatalogItems().orEmpty()
    }

    suspend fun loadHome(
        backendBaseUrl: String,
        language: String = "en"
    ): Result<HomeFeed?> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. INSTANT: Check in-memory cache (0ms)
            _prefetchedFeed.value?.let { return@runCatching withLocalWatchState(it) }
            
            // 2. FAST: Check disk cache (10-50ms typical)
            cacheStore.readFeedForCurrentWindow()?.let { cached ->
                val merged = withLocalWatchState(cached)
                _prefetchedFeed.value = merged
                // Return cached feed immediately to unblock UI
                // Start fresh fetch in background async (doesn't block UI thread)
                return@runCatching merged
            }
            
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")

            // 3. NETWORK: Fetch with reasonable timeout (reduced from 5000ms to 4000ms)
            // Optimized: Faster timeout for better perceived performance
            withTimeoutOrNull(4000) {
                withContext(Dispatchers.IO) {
                    // Reduced from 3500ms to 3000ms for faster initial load
                    fetchHomeFeed(backendBaseUrl, language, token, timeoutMillis = 3000)
                }
            }?.let { rawFeed ->
                // Cache writes are also on IO to avoid UI blocking
                withContext(Dispatchers.IO) {
                    cacheStore.writeRawFeed(rawFeed)
                }
                withLocalWatchState(cacheStore.displayFeedForToday(rawFeed)).also { displayFeed ->
                    _prefetchedFeed.value = displayFeed
                    withContext(Dispatchers.IO) {
                        cacheStore.writeDisplayFeed(displayFeed)
                    }
                }
            }
        }
    }

    suspend fun refreshHome(
        backendBaseUrl: String,
        language: String = "en"
    ): Result<HomeFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")

            val rawFeed = fetchHomeFeed(backendBaseUrl, language, token, timeoutMillis = 9000)
            cacheStore.writeRawFeed(rawFeed)
            val displayFeed = withLocalWatchState(cacheStore.displayFeedForToday(rawFeed))
            cacheStore.writeDisplayFeed(displayFeed)
            displayFeed.also { _prefetchedFeed.value = it }
        }
    }

    suspend fun refreshContinueWatching(
        backendBaseUrl: String,
        language: String = "en"
    ): Result<List<ContinueWatchingItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")

            val historyJson = getClientJson(
                backendBaseUrl = backendBaseUrl,
                path = "client/history/watch",
                language = language,
                token = token,
                timeoutMillis = 5000,
                page = 1
            )
            parseContinueWatching(historyJson).mergeLocalWatchHistory(savedWatchHistoryStore.readItems())
        }
    }

    suspend fun setReminder(
        backendBaseUrl: String,
        film: DramaItem,
        enabled: Boolean,
        language: String = "en"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Local-only: Save/remove immediately without backend call
            if (enabled) {
                savedWatchListStore.save(film)
            } else {
                savedWatchListStore.remove(film.id)
            }
            // Backend sync removed for instant response
        }
    }

    suspend fun searchMood(
        backendBaseUrl: String,
        mood: String,
        language: String = "en"
    ): Result<HomeFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")
            val query = URLEncoder.encode(mood, "UTF-8")
            val searchJson = getClientJson(
                backendBaseUrl = backendBaseUrl,
                path = "client/search",
                language = language,
                token = token,
                timeoutMillis = 7000,
                page = 1,
                extraQuery = "query=$query"
            )
            val moodItems = collectDramaItems(searchJson)
                .distinctBy { it.stableKey() }
                .filter { it.title.isNotBlank() }
            val current = _prefetchedFeed.value ?: cacheStore.readFeedForCurrentWindow()
            // If no cached feed exists, build a minimal one from the search results.
            val base = current ?: parseHomeFeed(
                filmJsons = listOf(searchJson),
                forYouJsons = listOf(searchJson)
            )
            if (moodItems.isEmpty()) {
                base
            } else {
                base.copy(
                    hero = moodItems.first(),
                    trending = moodItems.drop(1).ifEmpty { moodItems },
                    moreLikeThis = moodItems,
                    newReleases = moodItems,
                    ranking = moodItems,
                    categories = moodItems,
                    featured = moodItems
                )
            }.also { _prefetchedFeed.value = it }
        }
    }

    suspend fun hotSearch(
        backendBaseUrl: String,
        language: String = "en"
    ): Result<HomeFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")
            val searchJson = getClientJson(
                backendBaseUrl = backendBaseUrl,
                path = "client/search/hot",
                language = language,
                token = token,
                timeoutMillis = 7000,
                page = 1
            )
            val hotItems = collectDramaItems(searchJson)
                .distinctBy { it.stableKey() }
                .filter { it.title.isNotBlank() }
            val current = _prefetchedFeed.value ?: cacheStore.readFeedForCurrentWindow()
            val base = current ?: parseHomeFeed(
                filmJsons = listOf(searchJson),
                forYouJsons = listOf(searchJson)
            )
            if (hotItems.isEmpty()) {
                base
            } else {
                base.copy(
                    hero = hotItems.first(),
                    trending = hotItems.drop(1).ifEmpty { hotItems },
                    moreLikeThis = hotItems,
                    newReleases = hotItems,
                    ranking = hotItems,
                    categories = hotItems,
                    featured = hotItems
                )
            }.also { _prefetchedFeed.value = it }
        }
    }

    private fun withLocalWatchState(feed: HomeFeed): HomeFeed {
        return feed.copy(
            continueWatching = feed.continueWatching.mergeLocalWatchHistory(savedWatchHistoryStore.readItems())
        )
    }
}

fun HomeFeed.allCatalogItems(): List<DramaItem> {
    return (listOf(hero, topRated) + trending + moreLikeThis + newReleases + ranking + categories + featured + continueWatching.map { it.film })
        .filter { it.title.isNotBlank() }
        .distinctBy { it.stableKey() }
}

private suspend fun fetchHomeFeed(
    backendBaseUrl: String,
    language: String,
    token: String,
    timeoutMillis: Int
): HomeFeed = coroutineScope {
    // Prioritize: Load page 1 of films immediately, then pages 2-4 in background
    // This ensures we always have some content fast, then enrich with additional pages
    val filmPage1 = async {
        runCatching {
            getClientJson(backendBaseUrl, "client/films", language, token, timeoutMillis, page = 1)
        }.getOrNull()
    }
    val filmPages2to4 = (2..4).map { page ->
        async {
            runCatching {
                getClientJson(backendBaseUrl, "client/films", language, token, timeoutMillis, page = page)
            }.getOrNull()
        }
    }
    // for-you — load only first 2 pages (10 items) instead of 3 to save time
    // Page 3 often contains slow/duplicate personalization, not critical for initial feed
    val forYouPages = (1..2).map { page ->
        async {
            runCatching {
                getClientJson(backendBaseUrl, "client/for-you", language, token, timeoutMillis, page = page)
            }.getOrNull()
        }
    }
    val watchHistory = async {
        runCatching {
            getClientJson(backendBaseUrl, "client/history/watch", language, token, timeoutMillis, page = 1)
        }.getOrNull()
    }
    // Tags are lower priority, can timeout without breaking feed display
    val tags = async {
        runCatching {
            getClientJson(backendBaseUrl, "client/tags", language, token, (timeoutMillis / 2), page = 1)
        }.getOrNull()
    }

    val page1Result = filmPage1.await()
    val page2to4Results = filmPages2to4.mapNotNull { it.await() }
    val filmJsons = listOfNotNull(page1Result) + page2to4Results
    
    parseHomeFeed(
        filmJsons = filmJsons,
        forYouJsons = forYouPages.mapNotNull { it.await() },
        watchHistoryJson = watchHistory.await(),
        tagsJson = tags.await()
    )
}

private fun getClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    timeoutMillis: Int,
    page: Int,
    extraQuery: String? = null
): JSONObject {
    val query = buildString {
        if (extraQuery != null) {
            append(extraQuery)
            append("&")
        }
        append("language=$language&page=$page")
    }
    val url = "${backendBaseUrl.trimEndSlash()}/$path?$query"
    return NetworkUtil.getJsonSync(url, token, timeoutMillis)
}

private fun postClientAction(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String
) {
    val url = "${backendBaseUrl.trimEndSlash()}/$path?language=$language"
    NetworkUtil.postJsonSync(url, body = null, token = token)
}

private fun parseHomeFeed(
    filmJsons: List<JSONObject>,
    forYouJsons: List<JSONObject>,
    watchHistoryJson: JSONObject? = null,
    tagsJson: JSONObject? = null
): HomeFeed {
    // Full catalog from list_films pages 1-4 (~44 unique films)
    val fullCatalog = filmJsons.flatMap { collectDramaItems(it) }
        .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
        .filter { it.title.isNotBlank() }

    // Personalized for-you items (15 total, 5 per page × 3 pages)
    val forYouItems = forYouJsons.flatMap { collectDramaItems(it) }
        .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
        .filter { it.title.isNotBlank() }

    // Combine once, deduplicate once using a HashMap for O(n) performance
    val dedupeMap = mutableMapOf<String, DramaItem>()
    for (item in fullCatalog) {
        val key = item.stableKey()
        dedupeMap[key] = item
    }
    for (item in forYouItems) {
        val key = item.stableKey()
        dedupeMap[key] = item
    }
    val allItems = dedupeMap.values.toList()

    if (allItems.isEmpty()) {
        throw IllegalStateException("Home endpoints returned no films.")
    }

    val continueItems = parseContinueWatching(watchHistoryJson)

    // Popular: for-you first, fill up with catalog
    val forYouIds = forYouItems.map { it.id }.toSet()
    val popularItems = forYouItems + fullCatalog.filter { it.id !in forYouIds }

    // Ranking: sort by likeCount desc (real signal from API)
    val rankingItems = fullCatalog.sortedByDescending { it.likeCount }

    // New: reverse order (last pages = older, first pages = newest on this API)
    val newItems = fullCatalog

    // Categories: full catalog unordered
    val categoryItems = fullCatalog

    // Featured: top-rated by rating field
    val featuredItems = fullCatalog.sortedByDescending { it.rating.toFloatOrNull() ?: 0f }

    return HomeFeed(
        hero             = featuredItems.firstOrNull() ?: allItems.first(),
        continueWatching = continueItems,
        trending         = popularItems,
        topRated         = rankingItems.firstOrNull() ?: allItems.first(),
        moreLikeThis     = popularItems.drop(1),
        newReleases      = newItems,
        ranking          = rankingItems,
        categories       = categoryItems,
        featured         = featuredItems,
        hotTags          = collectHotTags(tagsJson, allItems)
    )
}

private class HomeCacheStore(context: Context) :
    SQLiteOpenHelper(context, "dramaverse_home.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE home_cache (" +
                "cache_key TEXT PRIMARY KEY, " +
                "cache_value TEXT NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS home_cache")
        onCreate(db)
    }

    fun readFeedForCurrentWindow(): HomeFeed? {
        readDisplayFeed()?.let { return it }
        val rawFeed = readRawFeed() ?: return null
        return displayFeedForToday(rawFeed).also { writeDisplayFeed(it) }
    }

    private fun readDisplayFeed(): HomeFeed? {
        val meta = readString(KEY_DISPLAY_META)?.let { JSONObject(it) } ?: return null
        if (meta.optString("day_key") != dayKey() || meta.optString("week_key") != weekKey()) {
            return null
        }
        return readString(KEY_DISPLAY_FEED)?.let { HomeFeed.fromJson(JSONObject(it)) }
    }

    private fun readRawFeed(): HomeFeed? {
        return readString(KEY_RAW_FEED)?.let { HomeFeed.fromJson(JSONObject(it)) }
    }

    fun writeRawFeed(feed: HomeFeed) {
        writeString(KEY_RAW_FEED, feed.toJson().toString())
    }

    fun writeDisplayFeed(feed: HomeFeed) {
        writeString(KEY_DISPLAY_FEED, feed.toJson().toString())
        writeString(
            KEY_DISPLAY_META,
            JSONObject()
                .put("day_key", dayKey())
                .put("week_key", weekKey())
                .toString()
        )
    }

    fun displayFeedForToday(rawFeed: HomeFeed): HomeFeed {
        readDisplayFeed()?.let { return it }
        // Use a stable daily rotation on each section independently so each tab
        // refreshes its order each day without losing any items.
        return rawFeed.copy(
            trending = rawFeed.trending.stableRotated(dayKey()),
            moreLikeThis = rawFeed.moreLikeThis.stableRotated("${dayKey()}-more"),
            newReleases = rawFeed.newReleases.stableRotated("${dayKey()}-new"),
            ranking = rawFeed.ranking,   // ranking order must not be shuffled — it is ranked
            categories = rawFeed.categories.stableRotated("${dayKey()}-cat"),
            featured = rawFeed.featured.stableRotated(weekKey()),  // weekly rotation for featured
            hotTags = rawFeed.hotTags
        )
    }

    private fun readString(key: String): String? {
        readableDatabase.query(
            "home_cache",
            arrayOf("cache_value"),
            "cache_key = ?",
            arrayOf(key),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun writeString(key: String, value: String) {
        val values = ContentValues().apply {
            put("cache_key", key)
            put("cache_value", value)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "home_cache",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}

private const val KEY_RAW_FEED = "raw_feed"
private const val KEY_DISPLAY_FEED = "display_feed"
private const val KEY_DISPLAY_META = "display_meta"

private fun dayKey(): String = LocalDate.now().toString()

private fun weekKey(): String {
    val now = LocalDate.now()
    val week = now.get(WeekFields.ISO.weekOfWeekBasedYear())
    val year = now.get(WeekFields.ISO.weekBasedYear())
    return "$year-W$week"
}

private fun List<DramaItem>.stableRotated(seed: String): List<DramaItem> {
    if (isEmpty()) return this
    val offset = seed.fold(0) { acc, char -> acc + char.code }.floorMod(size)
    return drop(offset) + take(offset)
}

private fun collectDramaItemsFromKeys(value: Any?, keys: Set<String>): List<DramaItem> {
    return when (value) {
        is JSONObject -> {
            val direct = value.keys().asSequence().flatMap { key ->
                val child = value.opt(key)
                if (key in keys) collectDramaItems(child).asSequence() else emptySequence()
            }.toList()
            direct + value.keys().asSequence().flatMap { key ->
                collectDramaItemsFromKeys(value.opt(key), keys).asSequence()
            }.toList()
        }

        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                addAll(collectDramaItemsFromKeys(value.opt(index), keys))
            }
        }

        else -> emptyList()
    }
}

private fun collectDramaItems(value: Any?): List<DramaItem> {
    val results = mutableListOf<DramaItem>()
    val visited = mutableSetOf<Any>()
    
    fun traverse(node: Any?) {
        if (node == null || node in visited) return
        if (node is Any) visited.add(node)
        
        when (node) {
            is JSONObject -> {
                node.toDramaItemOrNull()?.let { results.add(it) }
                val keys = node.keys()
                while (keys.hasNext()) {
                    traverse(node.opt(keys.next()))
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    traverse(node.opt(i))
                }
            }
        }
    }
    
    traverse(value)
    return results
}

private fun collectHotTags(tagsJson: JSONObject?, items: List<DramaItem>): List<String> {
    val backendTags = collectTagLabels(tagsJson)
    val itemTags = items
        .map { it.genre.trim() }
        .filter { it.isUsefulHotTag() }
    return (backendTags + itemTags)
        .map { it.trim() }
        .filter { it.isUsefulHotTag() }
        .distinctBy { it.lowercase(Locale.US) }
        .take(8)
}

private fun collectTagLabels(value: Any?): List<String> {
    return when (value) {
        is JSONObject -> {
            val direct = value.firstString("title", "name", "label", "tag", "genre", "category")
                .takeIf { it.isUsefulHotTag() }
            val children = value.keys().asSequence()
                .filterNot { it.isBlockedHotTagKey() }
                .flatMap { key -> collectTagLabels(value.opt(key)).asSequence() }
                .toList()
            listOfNotNull(direct) + children
        }

        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                addAll(collectTagLabels(value.opt(index)))
            }
        }

        is String -> listOf(value).filter { it.isUsefulHotTag() }
        else -> emptyList()
    }
}

// Tag endpoints also return ids/counts/status fields; only user-facing labels should become chips.
private val blockedHotTagKeys = setOf(
    "id",
    "film_id",
    "movie_id",
    "drama_id",
    "category_id",
    "tag_id",
    "type",
    "order",
    "sort",
    "count",
    "total",
    "response",
    "responses",
    "page",
    "per_page",
    "limit",
    "offset",
    "status",
    "code",
    "value",
    "created_at",
    "updated_at",
    "deleted_at",
    "is_hot",
    "is_active",
    "is_publish"
)

private fun String.isBlockedHotTagKey(): Boolean {
    return trim().lowercase(Locale.US) in blockedHotTagKeys
}

private fun String.isUsefulHotTag(): Boolean {
    val normalized = trim().lowercase(Locale.US)
    return normalized.length in 3..24 &&
        normalized.toDoubleOrNull() == null &&
        normalized.any { it.isLetter() } &&
        !normalized.matches(Regex("""^\d+\s+responses?$""")) &&
        normalized !in setOf(
            "all",
            "more",
            "null",
            "none",
            "drama",
            "movie",
            "film",
            "series",
            "short",
            "home",
            "data",
            "response",
            "responses",
            "trending",
            "new release",
            "new releases",
            "mystery"
        )
}

private fun parseContinueWatching(json: JSONObject?): List<ContinueWatchingItem> {
    val data = json?.opt("data") ?: return emptyList()
    val array = when (data) {
        is JSONArray -> data
        is JSONObject -> data.optJSONArray("items") ?: data.optJSONArray("data") ?: JSONArray()
        else -> JSONArray()
    }
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val filmJson = item.optJSONObject("film") ?: item
            val film = filmJson.toDramaItemFromBackend()
            if (film.title.isBlank()) continue
            add(
                ContinueWatchingItem(
                    film = film,
                    episodeNumber = item.firstInt("episode", "episode_number", "episodeNumber").takeIf { it > 0 } ?: 1,
                    progressSeconds = item.firstInt("progress_seconds", "progressSeconds", "current_time", "time_watching"),
                    durationSeconds = item.firstInt("duration_seconds", "durationSeconds", "episode_duration"),
                    completed = item.firstBoolean("completed", "is_completed")
                )
            )
        }
    }.distinctBy { it.film.id.takeIf { id -> id != 0 } ?: it.film.title }
}

private fun DramaItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("description", description)
    .put("imageUrl", imageUrl)
    .put("rating", rating)
    .put("episodeTotal", episodeTotal)
    .put("genre", genre)
    .put("isPremium", isPremium)
    .put("likeCount", likeCount)

private fun ContinueWatchingItem.toJson(): JSONObject = JSONObject()
    .put("film", film.toJson())
    .put("episodeNumber", episodeNumber)
    .put("progressSeconds", progressSeconds)
    .put("durationSeconds", durationSeconds)
    .put("completed", completed)

private fun HomeFeed.toJson(): JSONObject = JSONObject()
    .put("hero", hero.toJson())
    .put("continueWatching", continueWatching.toContinueJsonArray())
    .put("trending", trending.toJsonArray())
    .put("topRated", topRated.toJson())
    .put("moreLikeThis", moreLikeThis.toJsonArray())
    .put("newReleases", newReleases.toJsonArray())
    .put("ranking", ranking.toJsonArray())
    .put("categories", categories.toJsonArray())
    .put("featured", featured.toJsonArray())
    .put("hotTags", JSONArray().also { array -> hotTags.forEach(array::put) })

private fun List<DramaItem>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun List<ContinueWatchingItem>.toContinueJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { array.put(it.toJson()) }
}

private fun HomeFeed.Companion.fromJson(json: JSONObject): HomeFeed {
    return HomeFeed(
        hero = json.getJSONObject("hero").toDramaItem(),
        continueWatching = json.optJSONArray("continueWatching").toContinueWatchingItems(),
        trending = json.optJSONArray("trending").toDramaItems(),
        topRated = json.getJSONObject("topRated").toDramaItem(),
        moreLikeThis = json.optJSONArray("moreLikeThis").toDramaItems(),
        newReleases = json.optJSONArray("newReleases").toDramaItems(),
        ranking = json.optJSONArray("ranking").toDramaItems(),
        categories = json.optJSONArray("categories").toDramaItems(),
        featured = json.optJSONArray("featured").toDramaItems(),
        hotTags = json.optJSONArray("hotTags").toStringList()
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isUsefulHotTag() }?.let(::add)
        }
    }
}

private fun JSONObject.toDramaItemFromBackend(): DramaItem = DramaItem(
    id = firstInt("id", "film_id", "movie_id"),
    title = firstString("title", "name", "film_name", "movie_name", "filmTitle"),
    description = firstString("description", "desc", "summary", "content"),
    imageUrl = firstString("thumb", "thumb_url", "thumbnail", "thumbnail_url", "image", "image_url", "poster", "poster_url", "cover", "cover_url", "vertical_poster", "vertical_cover", "banner", "banner_url", "photo", "img", "imageUrl"),
    rating = firstString("rating", "rate", "score").ifBlank { "4.8" },
    episodeTotal = firstInt("episode_total", "episodes_count", "total_episodes", "eps", "episodeTotal")
        .takeIf { it > 0 } ?: 1,
    genre = firstString("genre", "category", "tag").ifBlank { "Drama" },
    isPremium = firstBoolean("is_vip", "isVip", "vip", "is_premium", "premium"),
    likeCount = firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
)

private fun JSONObject.toDramaItem(): DramaItem = DramaItem(
    id = optInt("id"),
    title = optString("title"),
    description = optString("description"),
    imageUrl = optString("imageUrl"),
    rating = optString("rating"),
    episodeTotal = optInt("episodeTotal"),
    genre = optString("genre"),
    isPremium = optBoolean("isPremium", false),
    likeCount = optInt("likeCount", 0)
)

private fun JSONArray?.toContinueWatchingItems(): List<ContinueWatchingItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val film = item.optJSONObject("film")?.toDramaItem() ?: continue
            add(
                ContinueWatchingItem(
                    film = film,
                    episodeNumber = item.optInt("episodeNumber", 1),
                    progressSeconds = item.optInt("progressSeconds", 0),
                    durationSeconds = item.optInt("durationSeconds", 0),
                    completed = item.optBoolean("completed", false)
                )
            )
        }
    }
}

private fun JSONArray?.toDramaItems(): List<DramaItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(it.toDramaItem()) }
        }
    }
}

private fun DramaItem.stableKey(): String = id.takeIf { it != 0 }?.toString()
    ?: title.lowercase(Locale.US)

private fun JSONObject.toDramaItemOrNull(): DramaItem? {
    val title = firstString("title", "name", "film_name", "movie_name", "filmTitle")
    val image = firstString("thumb", "thumb_url", "thumbnail", "thumbnail_url", "image", "image_url", "poster", "poster_url", "cover", "cover_url", "vertical_poster", "vertical_cover", "banner", "banner_url", "photo", "img", "imageUrl")
    if (title.isBlank() || image.isBlank()) return null

    return DramaItem(
        id = firstInt("id", "film_id", "movie_id"),
        title = title,
        description = firstString("description", "desc", "summary", "content")
            .ifBlank { "In a world of secrets and ambition, every choice changes the story." },
        imageUrl = image,
        rating = firstString("rating", "rate", "score").ifBlank { "4.8" },
        episodeTotal = firstInt("episode_total", "episodes_count", "total_episodes", "eps").takeIf { it > 0 } ?: 45,
        genre = firstString("genre", "category", "tag", "genres", "genre_name", "category_name", "tags").ifBlank { "Romance" },
        isPremium = firstBoolean("is_vip", "isVip", "vip", "is_premium", "premium") ||
            firstInt("price", "coin_price", "unlock_price") > 0,
        likeCount = firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
    )
}

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        val value = opt(key)
        if (value is String && value.isNotBlank()) return value
        if (value is Number) return value.toString()
        if (value is JSONObject) {
            value.firstString(*keys)
                .takeIf { it.isNotBlank() }
                ?.let { return it }
            value.firstString("url", "src", "path", "thumb", "image", "poster", "cover")
                .takeIf { it.isNotBlank() }
                ?.let { return it }
            value.firstString("name", "title", "label", "tag", "genre", "category", "description")
                .takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        if (value is JSONArray && value.length() > 0) {
            when (val first = value.opt(0)) {
                is String -> if (first.isNotBlank()) return first
                is JSONObject -> {
                    first.firstString(*keys)
                        .takeIf { it.isNotBlank() }
                        ?.let { return it }
                    first.firstString("url", "src", "path", "thumb", "image", "poster", "cover")
                        .takeIf { it.isNotBlank() }
                        ?.let { return it }
                    first.firstString("name", "title", "label", "tag", "genre", "category", "description")
                        .takeIf { it.isNotBlank() }
                        ?.let { return it }
                }
            }
        }
    }
    return ""
}

private fun JSONObject.firstInt(vararg keys: String): Int {
    for (key in keys) {
        when (val value = opt(key)) {
            is Number -> return value.toInt()
            is String -> value.toIntOrNull()?.let { return it }
        }
    }
    return 0
}

private fun JSONObject.firstBoolean(vararg keys: String): Boolean {
    for (key in keys) {
        when (val value = opt(key)) {
            is Boolean -> return value
            is Number -> return value.toInt() != 0
            is String -> {
                if (value.equals("true", ignoreCase = true)) return true
                value.toIntOrNull()?.let { return it != 0 }
            }
        }
    }
    return false
}

private fun String.trimEndSlash(): String = trim().trimEnd('/').ifBlank { "https://drama-verse-backend.vercel.app/" }

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
