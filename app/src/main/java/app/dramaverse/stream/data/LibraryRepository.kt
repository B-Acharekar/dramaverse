package app.dramaverse.stream.data

import android.content.Context
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
    val followedFilms: List<DramaItem>,
    val recommended: List<DramaItem>,
    val similarFilms: List<DramaItem>
)

class LibraryRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
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
                val followed = async {
                    runCatching {
                        getClientJson(backendBaseUrl, "client/history/follow", language, token, 7000)
                    }.getOrNull()
                }
                val recommended = async {
                    runCatching {
                        getClientJson(backendBaseUrl, "client/for-you", language, token, 7000)
                    }.getOrNull()
                }

                val historyItems = parseContinueWatching(history.await())
                val recommendItems = collectDramaItems(recommended.await())
                val similarQuery = historyItems.firstOrNull()?.film?.genre?.takeIf { it.isNotBlank() }
                    ?: historyItems.firstOrNull()?.film?.title
                    ?: "romance"
                val similarJson = runCatching {
                    val query = URLEncoder.encode(similarQuery, "UTF-8")
                    getClientJson(backendBaseUrl, "client/search", language, token, 7000, "query=$query")
                }.getOrNull()
                val watchedIds = historyItems.map { it.film.id }.toSet()

                LibraryFeed(
                    watchList = collectDramaItems(watchList.await()).distinctFilms(),
                    watchHistory = historyItems,
                    followedFilms = collectDramaItems(followed.await()).distinctFilms(),
                    recommended = recommendItems.distinctFilms().take(12),
                    similarFilms = collectDramaItems(similarJson)
                        .filterNot { it.id != 0 && it.id in watchedIds }
                        .distinctFilms()
                        .ifEmpty { recommendItems.distinctFilms() }
                        .take(12)
                )
            }
        }
    }
}

private fun getClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    timeoutMillis: Int,
    extraQuery: String? = null
): JSONObject {
    val query = buildString {
        if (extraQuery != null) {
            append(extraQuery)
            append("&")
        }
        append("language=$language&page=1")
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

private fun JSONObject.toDramaItem(): DramaItem {
    val film = optJSONObject("film") ?: this
    return DramaItem(
        id = film.firstInt("id", "film_id", "movie_id"),
        title = film.firstString("title", "name", "film_name", "movie_name", "filmTitle"),
        description = film.firstString("description", "desc", "summary", "content"),
        imageUrl = film.firstString("thumb", "thumbnail", "image", "poster", "cover", "vertical_poster", "banner", "imageUrl"),
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

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        val value = optString(key).trim()
        if (value.isNotBlank() && value != "null") return value
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

private fun String.trimEndSlash(): String = trim().trimEnd('/').ifBlank { "https://dramaverse-backend-lbq5.onrender.com" }
