package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class LibraryFeed(
    val watchList: List<DramaItem>,
    val watchHistory: List<ContinueWatchingItem>,
    val topStars: List<TopStar>,
    val recommended: List<DramaItem>,
    val similarFilms: List<DramaItem>
) {
    companion object
}

data class TopStar(
    val name: String,
    val imageUrl: String,
    val filmId: Int
)

class LibraryRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val cacheStore = LibraryCacheStore(context.applicationContext)
    private val savedWatchListStore = SavedWatchListStore(context.applicationContext)

    suspend fun loadCachedLibrary(): LibraryFeed? = withContext(Dispatchers.IO) {
        withLocalWatchList(cacheStore.readFeed())
    }

    private fun withLocalWatchList(feed: LibraryFeed?): LibraryFeed? {
        val localWatchListItems = savedWatchListStore.readItems()
            .filterNot { it.looksLikePlaceholder() }
        if (feed == null) {
            return localWatchListItems.takeIf { it.isNotEmpty() }?.let {
                LibraryFeed(
                    watchList = it.distinctFilms(),
                    watchHistory = emptyList(),
                    topStars = emptyList(),
                    recommended = emptyList(),
                    similarFilms = emptyList()
                )
            }
        }
        // Local saves are merged into cached data so Library reflects new saves before backend refresh finishes.
        return feed.copy(watchList = (localWatchListItems + feed.watchList).distinctFilms())
    }

    suspend fun loadLibrary(
        backendBaseUrl: String,
        language: String = "en"
    ): Result<LibraryFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")

            coroutineScope {
                val watchList = async {
                    runCatching {
                        getClientJson(backendBaseUrl, "client/reminders", language, token, 7000)
                    }.getOrNull()
                }
                val history = async {
                    runCatching {
                        getClientJson(backendBaseUrl, "client/history/watch", language, token, 7000)
                    }.getOrNull()
                }
                val recommendedPages = (1..4).map { page ->
                    async {
                        runCatching {
                            getClientJson(backendBaseUrl, "client/for-you", language, token, 7000, page = page)
                        }.getOrNull()
                    }
                }

                val historyItems = parseContinueWatching(history.await())
                val watchListJson = watchList.await()
                val localWatchListItems = savedWatchListStore.readItems()
                val watchListItems = (localWatchListItems + collectDramaItems(watchListJson))
                    .filterNot { it.looksLikePlaceholder() }
                    .distinctFilms()
                val recommendedJsons = recommendedPages.mapNotNull { it.await() }
                val recommendedItems = recommendedJsons
                    .flatMap { collectDramaItems(it) }
                    .filterNot { it.looksLikePlaceholder() }
                    .distinctFilms()
                val displayedRecommendedItems = recommendedItems.take(12)
                val similarQuery = historyItems.firstOrNull()?.film?.title?.takeIf { it.isUsefulSimilarQuery() }
                    ?: historyItems.firstOrNull()?.film?.genre?.takeIf { it.isUsefulSimilarQuery() }
                    ?: watchListItems.firstOrNull()?.title?.takeIf { it.isUsefulSimilarQuery() }
                    ?: watchListItems.firstOrNull()?.genre?.takeIf { it.isUsefulSimilarQuery() }
                val similarJson = runCatching {
                    val querySeed = similarQuery ?: return@runCatching null
                    val query = URLEncoder.encode(querySeed, "UTF-8")
                    getClientJson(backendBaseUrl, "client/search", language, token, 7000, "query=$query")
                }.getOrNull()
                val watchedIds = historyItems.map { it.film.id }.toSet()
                val watchListIds = watchListItems.map { it.id }.toSet()
                val displayedRecommendedIds = displayedRecommendedItems.map { it.id }.toSet()
                val excludedIds = watchedIds + watchListIds + displayedRecommendedIds
                val excludedTitles = (historyItems.map { it.film.title } + watchListItems.map { it.title } + displayedRecommendedItems.map { it.title })
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()
                val similarItems = collectDramaItems(similarJson)
                    .filterNot { it.id != 0 && it.id in excludedIds }
                    .filterNot { it.title.trim().lowercase() in excludedTitles }
                    .filterNot { it.looksLikePlaceholder() }
                    .distinctFilms()
                // If search returns nothing, use real backend recommendations after the displayed slice.
                val similarFallbackItems = recommendedItems
                    .drop(displayedRecommendedItems.size)
                    .filterNot { it.id != 0 && it.id in (watchedIds + watchListIds) }
                    .filterNot { it.title.trim().lowercase() in excludedTitles }
                    .filterNot { it.looksLikePlaceholder() }

                LibraryFeed(
                    watchList = watchListItems,
                    watchHistory = historyItems,
                    topStars = collectTopStars(listOf(watchListJson, similarJson) + recommendedJsons).take(12),
                    recommended = displayedRecommendedItems,
                    similarFilms = similarItems.ifEmpty { similarFallbackItems }
                        .filterNot { it.looksLikePlaceholder() }
                        .take(12)
                ).also { cacheStore.writeFeed(it) }
            }
        }
    }
}

private class LibraryCacheStore(context: Context) :
    SQLiteOpenHelper(context, "dramaverse_library.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE library_cache (" +
                "cache_key TEXT PRIMARY KEY, " +
                "cache_value TEXT NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS library_cache")
        onCreate(db)
    }

    fun readFeed(): LibraryFeed? {
        readableDatabase.query(
            "library_cache",
            arrayOf("cache_value"),
            "cache_key = ?",
            arrayOf("library_feed"),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) LibraryFeed.fromJson(JSONObject(cursor.getString(0))) else null
        }
    }

    fun writeFeed(feed: LibraryFeed) {
        val values = ContentValues().apply {
            put("cache_key", "library_feed")
            put("cache_value", feed.toJson().toString())
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "library_cache",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }
}

private fun getClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    timeoutMillis: Int,
    extraQuery: String? = null,
    page: Int = 1
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
            val film = filmJson.toDramaItem()
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

private fun collectDramaItems(value: Any?): List<DramaItem> {
    return when (value) {
        is JSONObject -> {
            val direct = value.toDramaItem().takeIf { it.title.isNotBlank() }
            val children = value.keys().asSequence().flatMap { key ->
                collectDramaItems(value.opt(key)).asSequence()
            }.toList()
            listOfNotNull(direct) + children
        }
        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                addAll(collectDramaItems(value.opt(index)))
            }
        }
        else -> emptyList()
    }
}

private fun collectTopStars(values: List<Any?>): List<TopStar> =
    values.flatMap { collectTopStars(it) }
        .filter { it.name.isNotBlank() }
        .distinctBy { it.name.lowercase() }

private fun collectTopStars(value: Any?): List<TopStar> {
    return when (value) {
        is JSONObject -> {
            val film = value.optJSONObject("film") ?: value
            val filmId = film.firstInt("id", "film_id", "movie_id")
            val direct = buildList {
                val name = film.firstString("actor", "actors", "cast", "star", "stars", "performer", "artist")
                if (name.isNotBlank()) {
                    name.split(",", "/", "|")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach {
                            add(TopStar(it, film.firstString("actor_thumb", "actor_image", "avatar", "photo"), filmId))
                        }
                }
            }
            val children = value.keys().asSequence().flatMap { key ->
                collectTopStars(value.opt(key)).asSequence()
            }.toList()
            direct + children
        }
        is JSONArray -> buildList {
            for (index in 0 until value.length()) {
                addAll(collectTopStars(value.opt(index)))
            }
        }
        else -> emptyList()
    }
}

private fun JSONObject.toDramaItem(): DramaItem {
    val film = optJSONObject("film") ?: this
    return DramaItem(
        id = film.firstInt("id", "film_id", "movie_id"),
        title = film.firstString("title", "name", "film_name", "movie_name", "filmTitle"),
        description = film.firstString("description", "desc", "summary", "content"),
        imageUrl = film.firstString("thumb", "thumb_url", "thumbnail", "thumbnail_url", "image", "image_url", "poster", "poster_url", "cover", "cover_url", "vertical_poster", "vertical_cover", "banner", "banner_url", "photo", "img", "imageUrl"),
        rating = film.firstString("rating", "rate", "score").ifBlank { "4.8" },
        episodeTotal = film.firstInt("episode_total", "episodes_count", "total_episodes", "eps", "episodeTotal")
            .takeIf { it > 0 } ?: 1,
        genre = film.firstString("genre", "category", "tag").ifBlank { "Drama" },
        isPremium = film.firstBoolean("is_vip", "isVip", "vip", "is_premium", "premium"),
        likeCount = film.firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
    )
}

private fun List<DramaItem>.distinctFilms(): List<DramaItem> =
    filter { it.title.isNotBlank() }.distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }

private fun DramaItem.looksLikePlaceholder(): Boolean {
    val normalizedTitle = title.trim().lowercase()
    val normalizedGenre = genre.trim().lowercase()
    val blockedTitles = setOf(
        "love after marriage",
        "toxic love",
        "historical drama",
        "drama",
        "romance",
        "melodrama"
    )
    return title.isBlank() ||
        normalizedTitle in blockedTitles ||
        (id == 0 && imageUrl.isBlank()) ||
        (id == 0 && normalizedGenre in setOf("drama", "romance") && imageUrl.isBlank())
}

private fun String.isUsefulSimilarQuery(): Boolean {
    val normalized = trim().lowercase()
    return normalized.isNotBlank() &&
        normalized !in setOf("drama", "romance", "movie", "film", "short", "series")
}

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        when (val raw = opt(key)) {
            is String -> {
                val value = raw.trim()
                if (value.isNotBlank() && value != "null") return value
            }
            is Number -> return raw.toString()
            is JSONObject -> raw.firstString("url", "src", "path", "thumb", "image", "poster", "cover").takeIf { it.isNotBlank() }?.let { return it }
            is JSONArray -> if (raw.length() > 0) {
                when (val first = raw.opt(0)) {
                    is String -> if (first.isNotBlank()) return first
                    is JSONObject -> first.firstString("url", "src", "path", "thumb", "image", "poster", "cover").takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
    }
    return ""
}

private fun JSONObject.firstInt(vararg keys: String): Int {
    for (key in keys) {
        if (!has(key)) continue
        val value = opt(key)
        when (value) {
            is Number -> return value.toInt()
            is String -> value.toIntOrNull()?.let { return it }
        }
    }
    return 0
}

private fun JSONObject.firstBoolean(vararg keys: String): Boolean {
    for (key in keys) {
        if (!has(key)) continue
        val value = opt(key)
        when (value) {
            is Boolean -> return value
            is Number -> return value.toInt() != 0
            is String -> if (value.equals("true", true) || value == "1") return true
        }
    }
    return false
}

private fun String.trimEndSlash(): String = trim().trimEnd('/').ifBlank { "https://drama-verse-backend.vercel.app/" }

private fun LibraryFeed.toJson(): JSONObject = JSONObject()
    .put("watchList", watchList.toDramaJsonArray())
    .put("watchHistory", watchHistory.toContinueJsonArray())
    .put("topStars", topStars.toTopStarJsonArray())
    .put("recommended", recommended.toDramaJsonArray())
    .put("similarFilms", similarFilms.toDramaJsonArray())

private fun LibraryFeed.Companion.fromJson(json: JSONObject): LibraryFeed = LibraryFeed(
    watchList = json.optJSONArray("watchList").toDramaItems(),
    watchHistory = json.optJSONArray("watchHistory").toContinueItems(),
    topStars = json.optJSONArray("topStars").toTopStars(),
    recommended = json.optJSONArray("recommended").toDramaItems(),
    similarFilms = json.optJSONArray("similarFilms").toDramaItems()
)

private fun List<DramaItem>.toDramaJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("description", item.description)
                .put("imageUrl", item.imageUrl)
                .put("rating", item.rating)
                .put("episodeTotal", item.episodeTotal)
                .put("genre", item.genre)
                .put("isPremium", item.isPremium)
                .put("likeCount", item.likeCount)
        )
    }
}

private fun List<ContinueWatchingItem>.toContinueJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { item ->
        array.put(
            JSONObject()
                .put("film", listOf(item.film).toDramaJsonArray().getJSONObject(0))
                .put("episodeNumber", item.episodeNumber)
                .put("progressSeconds", item.progressSeconds)
                .put("durationSeconds", item.durationSeconds)
                .put("completed", item.completed)
        )
    }
}

private fun List<TopStar>.toTopStarJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { item ->
        array.put(
            JSONObject()
                .put("name", item.name)
                .put("imageUrl", item.imageUrl)
                .put("filmId", item.filmId)
        )
    }
}

private fun JSONArray?.toDramaItems(): List<DramaItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(item.toCachedDramaItem())
        }
    }
}

private fun JSONArray?.toContinueItems(): List<ContinueWatchingItem> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val film = item.optJSONObject("film") ?: continue
            val drama = film.toCachedDramaItem()
            add(
                ContinueWatchingItem(
                    film = drama,
                    episodeNumber = item.optInt("episodeNumber", 1),
                    progressSeconds = item.optInt("progressSeconds", 0),
                    durationSeconds = item.optInt("durationSeconds", 0),
                    completed = item.optBoolean("completed", false)
                )
            )
        }
    }
}

private fun JSONObject.toCachedDramaItem(): DramaItem = DramaItem(
    id = optInt("id"),
    title = optString("title"),
    description = optString("description"),
    imageUrl = optString("imageUrl"),
    rating = optString("rating"),
    episodeTotal = optInt("episodeTotal", 1),
    genre = optString("genre", "Drama"),
    isPremium = optBoolean("isPremium", false),
    likeCount = optInt("likeCount", 0)
)

private fun JSONArray?.toTopStars(): List<TopStar> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                TopStar(
                    name = item.optString("name"),
                    imageUrl = item.optString("imageUrl"),
                    filmId = item.optInt("filmId")
                )
            )
        }
    }
}
