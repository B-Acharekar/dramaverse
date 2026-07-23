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
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
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
    val trending: List<DramaItem>,
    val topRated: DramaItem,
    val moreLikeThis: List<DramaItem>,
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

    fun savedWatchListIds(): Set<Int> = savedWatchListStore.savedIds()

    suspend fun loadHome(
        backendBaseUrl: String,
        language: String = "en"
    ): Result<HomeFeed?> = withContext(Dispatchers.IO) {
        runCatching {
            _prefetchedFeed.value?.let { return@runCatching it }
            cacheStore.readFeedForCurrentWindow()?.let { cached ->
                _prefetchedFeed.value = cached
                return@runCatching cached
            }
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")

            withTimeoutOrNull(2800) {
                fetchHomeFeed(backendBaseUrl, language, token, timeoutMillis = 2200)
            }?.let { rawFeed ->
                cacheStore.writeRawFeed(rawFeed)
                cacheStore.displayFeedForToday(rawFeed).also { displayFeed ->
                    _prefetchedFeed.value = displayFeed
                    cacheStore.writeDisplayFeed(displayFeed)
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
            val displayFeed = cacheStore.displayFeedForToday(rawFeed)
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
            parseContinueWatching(historyJson)
        }
    }

    suspend fun setReminder(
        backendBaseUrl: String,
        film: DramaItem,
        enabled: Boolean,
        language: String = "en"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (enabled) {
                savedWatchListStore.save(film)
            } else {
                savedWatchListStore.remove(film.id)
            }
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")
            postClientAction(
                backendBaseUrl = backendBaseUrl,
                path = "client/films/${film.id}/${if (enabled) "reminder" else "unreminder"}",
                language = language,
                token = token
            )
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
            val base = current ?: parseHomeFeed(null, emptyList(), listOf(searchJson))
            if (moodItems.isEmpty()) {
                base
            } else {
                base.copy(
                    hero = moodItems.first(),
                    trending = moodItems.drop(1).take(8).ifEmpty { moodItems.take(8) },
                    moreLikeThis = moodItems.take(8)
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
            val base = current ?: parseHomeFeed(null, emptyList(), listOf(searchJson))
            if (hotItems.isEmpty()) {
                base
            } else {
                base.copy(
                    hero = hotItems.first(),
                    trending = hotItems.drop(1).take(10).ifEmpty { hotItems.take(10) },
                    moreLikeThis = hotItems.take(12)
                )
            }.also { _prefetchedFeed.value = it }
        }
    }
}

private suspend fun fetchHomeFeed(
    backendBaseUrl: String,
    language: String,
    token: String,
    timeoutMillis: Int
): HomeFeed = coroutineScope {
    val home = async {
        runCatching {
            getClientJson(backendBaseUrl, "client/home", language, token, timeoutMillis, page = 1)
        }
    }
    val filmPages = (1..3).map { page ->
        async {
            runCatching {
                getClientJson(backendBaseUrl, "client/films", language, token, timeoutMillis, page = page)
            }.getOrNull()
        }
    }
    val forYouPages = (1..3).map { page ->
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

    val tags = async {
        runCatching {
            getClientJson(backendBaseUrl, "client/tags", language, token, timeoutMillis, page = 1)
        }.getOrNull()
    }

    parseHomeFeed(
        homeJson = home.await().getOrNull(),
        filmsJsons = filmPages.mapNotNull { it.await() },
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
    val url = URL("${backendBaseUrl.trimEndSlash()}/$path?$query")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = timeoutMillis
        readTimeout = timeoutMillis
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
    }

    val responseText = if (connection.responseCode in 200..299) {
        connection.inputStream.bufferedReader().use { it.readText() }
    } else {
        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        throw IllegalStateException("$path failed: ${connection.responseCode} $error")
    }
    return JSONObject(responseText)
}

private fun postClientAction(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String
) {
    val url = URL("${backendBaseUrl.trimEndSlash()}/$path?language=$language")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 6500
        readTimeout = 6500
        doOutput = true
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
        outputStream.use { it.write(ByteArray(0)) }
    }
    if (connection.responseCode !in 200..299) {
        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        throw IllegalStateException("$path failed: ${connection.responseCode} $error")
    }
    connection.inputStream?.close()
}

private fun parseHomeFeed(
    homeJson: JSONObject?,
    filmsJsons: List<JSONObject>,
    forYouJsons: List<JSONObject>,
    watchHistoryJson: JSONObject? = null,
    tagsJson: JSONObject? = null
): HomeFeed {
    val homeItems = collectDramaItems(homeJson)
        .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
        .filter { it.title.isNotBlank() }
    val forYouItems = forYouJsons.flatMap { collectDramaItems(it) }
        .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
        .filter { it.title.isNotBlank() }
    val filmItems = filmsJsons.flatMap { collectDramaItems(it) }
        .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
        .filter { it.title.isNotBlank() }
    val continueKeys = setOf(
        "continue_watching",
        "continueWatching",
        "watching",
        "history",
        "history_watching",
        "watch_history"
    )
    val continueItems = parseContinueWatching(watchHistoryJson).ifEmpty {
        (
            collectDramaItemsFromKeys(homeJson, continueKeys) +
                filmsJsons.flatMap { collectDramaItemsFromKeys(it, continueKeys) }
            )
            .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
            .map { film ->
                ContinueWatchingItem(
                    film = film,
                    episodeNumber = 1,
                    progressSeconds = 0,
                    durationSeconds = 0
                )
            }
    }

    val items = (homeItems + forYouItems + filmItems)
        .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
    if (items.isEmpty()) {
        throw IllegalStateException("Home endpoints returned no films.")
    }
    val feedItems = if (items.size >= 5) items else fallbackItems()
    return HomeFeed(
        hero = homeItems.firstOrNull() ?: feedItems[0],
        continueWatching = continueItems,
        trending = filmItems.take(12).ifEmpty { feedItems.drop(2).take(8).ifEmpty { feedItems.take(4) } },
        topRated = homeItems.getOrNull(1) ?: feedItems.getOrElse(3) { feedItems.first() },
        moreLikeThis = (forYouItems + filmItems).distinctBy { it.stableKey() }.take(12)
            .ifEmpty { feedItems.drop(4).take(8).ifEmpty { feedItems.take(4) } },
        hotTags = collectHotTags(tagsJson, items)
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
        val pool = (listOf(rawFeed.hero) + rawFeed.trending + rawFeed.moreLikeThis)
            .distinctBy { it.stableKey() }
        val daily = pool.stableRotated(dayKey())
        val weekly = pool.stableRotated(weekKey())
        val hero = daily.firstOrNull() ?: rawFeed.hero
        val trending = rawFeed.trending
            .filterNot { it.stableKey() == hero.stableKey() }
            .stableRotated(dayKey())
            .take(10)
            .ifEmpty { daily.drop(1).take(10) }
        val topRated = weekly.firstOrNull() ?: rawFeed.topRated
        val moreLikeThis = pool.filterNot { it.stableKey() == hero.stableKey() }
            .stableRotated("${dayKey()}-more")
            .take(8)
            .ifEmpty { rawFeed.moreLikeThis }
        return rawFeed.copy(
            hero = hero,
            trending = trending,
            topRated = topRated,
            moreLikeThis = moreLikeThis,
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
    return when (value) {
        is JSONObject -> {
            val own = value.toDramaItemOrNull()
            val children = value.keys().asSequence().flatMap { key ->
                collectDramaItems(value.opt(key)).asSequence()
            }.toList()
            if (own != null) listOf(own) + children else children
        }

        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                addAll(collectDramaItems(value.opt(index)))
            }
        }

        else -> emptyList()
    }
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
        genre = firstString("genre", "category", "tag").ifBlank { "Romance" },
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
            value.firstString("url", "src", "path", "thumb", "image", "poster", "cover").takeIf { it.isNotBlank() }?.let { return it }
        }
        if (value is JSONArray && value.length() > 0) {
            when (val first = value.opt(0)) {
                is String -> if (first.isNotBlank()) return first
                is JSONObject -> first.firstString("url", "src", "path", "thumb", "image", "poster", "cover").takeIf { it.isNotBlank() }?.let { return it }
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

private fun fallbackItems(): List<DramaItem> = listOf(
    DramaItem(1, "Empire of Deceit", "In the cutthroat world of global finance, one woman risks everything to expose the corruption that built an empire.", "", "4.8", 96, "Romance"),
    DramaItem(2, "The Secret Vow", "A hidden promise changes two lives forever.", "", "4.7", 42, "Drama"),
    DramaItem(3, "Midnight Pulse", "Neon streets, dangerous secrets, and a chase after midnight.", "", "4.6", 65, "Thriller"),
    DramaItem(4, "CEO's Hidden Heir", "A powerful family secret returns to claim everything.", "", "4.9", 96, "Romance", true),
    DramaItem(5, "Love in Autumn Rain", "A second chance arrives with the season's first storm.", "", "4.5", 48, "Romance"),
    DramaItem(6, "The Billionaire's Secret Heir", "Power, revenge, and family collide in the city.", "", "4.8", 45, "Revenge"),
    DramaItem(7, "The Glass Mansion", "Behind every perfect wall is a dangerous lie.", "", "4.9", 72, "Suspense", true),
    DramaItem(8, "Vengeance Sweetness", "A masked betrayal becomes a dangerous romance.", "", "4.7", 58, "Thriller")
)

private fun String.trimEndSlash(): String = trim().trimEnd('/').ifBlank { "https://drama-verse-backend.vercel.app/" }

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
