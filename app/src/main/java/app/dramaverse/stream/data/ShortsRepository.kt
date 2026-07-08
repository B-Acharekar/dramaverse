package app.dramaverse.stream.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ShortsItem(
    val film: DramaItem,
    val episodeNumber: Int = 1,
    val playUrl: String = "",
    val subtitleUrl: String = "",
    val isLocked: Boolean = false
)

class ShortsRepository(
    private val authRepository: AuthRepository
) {
    suspend fun loadFilms(
        backendBaseUrl: String,
        language: String = "en",
        page: Int
    ): Result<List<ShortsItem>> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        val json = getJson(backendBaseUrl, "client/films", language, token, page)
        collectShortDramaItems(json)
            .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
            .filter { it.title.isNotBlank() }
            .map { ShortsItem(film = it) }
    }

    suspend fun loadPlayback(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int = 1,
        language: String = "en"
    ): Result<ShortsItem> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        val json = getJson(
            backendBaseUrl = backendBaseUrl,
            path = "client/films/$filmId/episodes/$episodeNumber/play",
            language = language,
            token = token,
            page = null
        )
        val filmJson = json.optJSONObject("film") ?: JSONObject()
        val episodeJson = json.optJSONObject("episode") ?: JSONObject()
        val playback = episodeJson.optJSONObject("playback") ?: JSONObject()
        val film = DramaItem(
            id = filmJson.optInt("id", filmId),
            title = filmJson.optString("title", "Drama"),
            description = filmJson.optString("description", ""),
            imageUrl = filmJson.optString("thumb"),
            rating = filmJson.optString("rating", "4.8"),
            episodeTotal = filmJson.optInt("episode_total", 1),
            genre = filmJson.optString("genre", "Drama")
        )
        ShortsItem(
            film = film,
            episodeNumber = episodeJson.optInt("episode", episodeNumber),
            playUrl = playback.optString("hls_url").ifBlank { playback.optString("backup_hls_url") },
            subtitleUrl = playback.optJSONObject("subtitles")?.firstValue().orEmpty(),
            isLocked = json.optBoolean("unlock_required", false)
        )
    }
}

private fun getJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    page: Int?
): JSONObject {
    val query = buildString {
        append("language=$language")
        if (page != null) append("&page=$page")
    }
    val url = URL("${backendBaseUrl.trimEndSlash()}/$path?$query")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 6500
        readTimeout = 6500
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

private fun collectShortDramaItems(value: Any?): List<DramaItem> {
    return when (value) {
        is JSONObject -> {
            val own = value.toShortDramaItemOrNull()
            val children = value.keys().asSequence().flatMap { key ->
                collectShortDramaItems(value.opt(key)).asSequence()
            }.toList()
            if (own != null) listOf(own) + children else children
        }

        is org.json.JSONArray -> buildList {
            for (index in 0 until value.length()) addAll(collectShortDramaItems(value.opt(index)))
        }

        else -> emptyList()
    }
}

private fun JSONObject.toShortDramaItemOrNull(): DramaItem? {
    val title = firstString("title", "name", "film_name", "movie_name", "filmTitle")
    val image = firstString("thumb", "thumbnail", "image", "poster", "cover", "vertical_poster", "banner")
    if (title.isBlank() || image.isBlank()) return null
    return DramaItem(
        id = firstInt("id", "film_id", "movie_id"),
        title = title,
        description = firstString("description", "desc", "summary", "content")
            .ifBlank { "A short drama packed with secrets, romance, and revenge." },
        imageUrl = image,
        rating = firstString("rating", "rate", "score").ifBlank { "4.8" },
        episodeTotal = firstInt("episode_total", "episodes_count", "total_episodes", "eps").takeIf { it > 0 } ?: 1,
        genre = firstString("genre", "category", "tag").ifBlank { "Drama" },
        isPremium = firstBoolean("is_vip", "isVip", "vip", "is_premium", "premium") ||
            firstInt("price", "coin_price", "unlock_price") > 0
    )
}

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        val value = opt(key)
        if (value is String && value.isNotBlank()) return value
        if (value is Number) return value.toString()
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

private fun JSONObject.firstValue(): String? {
    val keys = keys()
    while (keys.hasNext()) {
        val value = opt(keys.next())
        if (value is String && value.isNotBlank()) return value
    }
    return null
}

private fun String.trimEndSlash(): String =
    trim().trimEnd('/').ifBlank { "https://dramaverse-backend-lbq5.onrender.com" }
