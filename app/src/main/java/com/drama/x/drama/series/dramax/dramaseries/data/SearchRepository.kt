package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class SearchRepository(
    private val authRepository: AuthRepository
) {
    suspend fun searchFilms(
        backendBaseUrl: String,
        query: String,
        language: String = "en"
    ): Result<List<DramaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.authToken()
                ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
                ?: throw IllegalStateException("Device auth did not return a bearer token.")
            val trimmed = query.trim()
            val json = if (trimmed.equals("hot", ignoreCase = true)) {
                getClientJson(backendBaseUrl, "client/search/hot", language, token, null)
            } else {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                getClientJson(backendBaseUrl, "client/search", language, token, "query=$encoded")
            }
            collectDramaItems(json)
                .filter { it.title.isNotBlank() }
                .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
        }
    }
}

fun SearchRepository(context: Context): SearchRepository =
    SearchRepository(AuthRepository(context.applicationContext))

private fun getClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    extraQuery: String?
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
        connectTimeout = 9000
        readTimeout = 9000
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
        imageUrl = film.firstString("thumb", "thumb_url", "thumbnail", "thumbnail_url", "image", "image_url", "poster", "poster_url", "cover", "cover_url", "vertical_poster", "vertical_cover", "banner", "banner_url", "photo", "img", "imageUrl"),
        rating = film.firstString("rating", "rate", "score").ifBlank { "4.8" },
        episodeTotal = film.firstInt("episode_total", "episodes_count", "total_episodes", "eps", "episodeTotal").takeIf { it > 0 } ?: 1,
        genre = film.firstString("genre", "category", "tag").ifBlank { "Drama" },
        isPremium = film.firstBoolean("is_vip", "isVip", "vip", "is_premium", "premium"),
        likeCount = film.firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
    )
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
            is String -> if (value.equals("true", true) || value == "1") return true
        }
    }
    return false
}

private fun String.trimEndSlash(): String = trim().trimEnd('/').ifBlank { "https://dramaverse-backend-lbq5.onrender.com" }
