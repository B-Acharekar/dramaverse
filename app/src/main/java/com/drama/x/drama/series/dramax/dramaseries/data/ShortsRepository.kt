package com.drama.x.drama.series.dramax.dramaseries.data

import org.json.JSONObject

data class SubtitleTrack(
    val label: String,
    val language: String,
    val url: String
)

data class ShortsItem(
    val film: DramaItem,
    val episodeNumber: Int = 1,
    val playUrl: String = "",
    val subtitleUrl: String = "",
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val isLocked: Boolean = false,
    val likeCount: Int = 0
)

data class EpisodeInfo(
    val episodeNumber: Int,
    val title: String = "",
    val isLocked: Boolean = false,
    val isWatched: Boolean = false
)

private const val FREE_SHORTS_PREVIEW_EPISODES = 7

class ShortsRepository(
    private val authRepository: AuthRepository,
    context: android.content.Context
) {
    private val savedWatchListStore = SavedWatchListStore(context.applicationContext)
    suspend fun loadFilms(
        backendBaseUrl: String,
        language: String = "en",
        page: Int
    ): Result<List<ShortsItem>> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        val json = getJson(backendBaseUrl, "client/films", language, token, page)
        val items = collectShortDramaItems(json)
        
        // Deduplicate once using HashMap for O(n) performance
        val dedupeMap = mutableMapOf<String, DramaItem>()
        for (item in items) {
            val key = item.id.takeIf { it != 0 }?.toString() ?: item.title
            dedupeMap[key] = item
        }
        
        dedupeMap.values
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
        val subtitleTracks = playback.opt("subtitles").subtitleTracks(backendBaseUrl)
        val likeCount = episodeJson.firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
            .takeIf { it > 0 }
            ?: filmJson.firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
        val film = DramaItem(
            id = filmJson.optInt("id", filmId),
            title = filmJson.optString("title", "Drama"),
            description = filmJson.firstString("description", "desc", "summary", "content"),
            imageUrl = filmJson.optString("thumb"),
            rating = filmJson.firstString("rating", "rate", "score").ifBlank { "4.8" },
            episodeTotal = filmJson.optInt("episode_total", 1),
            genre = filmJson.firstString("genre", "category", "tag").ifBlank { "Drama" },
            likeCount = likeCount
        )
        ShortsItem(
            film = film,
            episodeNumber = episodeJson.optInt("episode", episodeNumber),
            playUrl = playback.optString("hls_url").ifBlank { playback.optString("backup_hls_url") },
            subtitleUrl = subtitleTracks.firstOrNull()?.url.orEmpty(),
            subtitleTracks = subtitleTracks,
            isLocked = episodeJson.optInt("episode", episodeNumber) > FREE_SHORTS_PREVIEW_EPISODES,
            likeCount = likeCount
        )
    }

    suspend fun loadEpisodes(
        backendBaseUrl: String,
        filmId: Int,
        language: String = "en"
    ): Result<List<EpisodeInfo>> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        val json = getJson(
            backendBaseUrl = backendBaseUrl,
            path = "client/films/$filmId/episodes",
            language = language,
            token = token,
            page = null
        )
        val episodes = json.optJSONArray("episodes") ?: org.json.JSONArray()
        buildList {
            for (index in 0 until episodes.length()) {
                val episode = episodes.optJSONObject(index) ?: continue
                val number = episode.firstInt("episode", "episode_number", "number").takeIf { it > 0 }
                    ?: (index + 1)
                add(
                    EpisodeInfo(
                        episodeNumber = number,
                        title = episode.firstString("title", "name"),
                        isLocked = number > FREE_SHORTS_PREVIEW_EPISODES,
                        isWatched = episode.optBoolean("watched", false) ||
                            episode.optBoolean("is_watched", false) ||
                            episode.optBoolean("completed", false) ||
                            episode.firstInt("is_watched", "watched", "completed") == 1
                    )
                )
            }
        }
    }

    suspend fun setEpisodeLike(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        liked: Boolean,
        language: String = "en"
    ): Result<Unit> = postClientAction(
        backendBaseUrl = backendBaseUrl,
        path = "client/films/$filmId/episodes/$episodeNumber/${if (liked) "like" else "unlike"}",
        language = language
    )

    suspend fun setReminder(
        backendBaseUrl: String,
        filmId: Int,
        film: DramaItem?,
        enabled: Boolean,
        language: String = "en"
    ): Result<Unit> = runCatching {
        // Local-only: Save/remove immediately without backend call
        if (enabled && film != null) {
            savedWatchListStore.save(film)
        } else {
            savedWatchListStore.remove(filmId)
        }
        // Backend sync removed for instant response
    }

    suspend fun unlockEpisode(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        language: String = "en"
    ): Result<Unit> = postClientAction(
        backendBaseUrl = backendBaseUrl,
        path = "client/films/$filmId/episodes/$episodeNumber/unlock",
        language = language
    )

    suspend fun saveWatchProgress(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        progressSeconds: Int,
        durationSeconds: Int?,
        completed: Boolean,
        language: String = "en"
    ): Result<Unit> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        postJson(
            backendBaseUrl = backendBaseUrl,
            path = "client/films/$filmId/episodes/$episodeNumber/watch",
            language = language,
            token = token,
            body = JSONObject()
                .put("progress_seconds", progressSeconds)
                .put("duration_seconds", durationSeconds)
                .put("completed", completed)
        )
        Unit
    }

    suspend fun sendFeedback(
        backendBaseUrl: String,
        filmId: Int,
        episodeNumber: Int,
        message: String,
        language: String = "en"
    ): Result<Unit> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        postJson(
            backendBaseUrl = backendBaseUrl,
            path = "client/feedback",
            language = language,
            token = token,
            body = JSONObject()
                .put("film_id", filmId)
                .put("episode_number", episodeNumber)
                .put("message", message)
                .put("source", "shorts")
        )
        Unit
    }

    private suspend fun postClientAction(
        backendBaseUrl: String,
        path: String,
        language: String
    ): Result<Unit> = runCatching {
        val token = authRepository.authToken()
            ?: authRepository.registerDevice(backendBaseUrl, language).getOrThrow().token
            ?: throw IllegalStateException("Device auth did not return a bearer token.")
        postJson(backendBaseUrl, path, language, token)
        Unit
    }
}

private fun postJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    body: JSONObject? = null
): JSONObject {
    val url = "${backendBaseUrl.trimEndSlash()}/$path?language=$language"
    return NetworkUtil.postJsonSync(url, body?.toString(), token)
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
    val url = "${backendBaseUrl.trimEndSlash()}/$path?$query"
    return NetworkUtil.getJsonSync(url, token)
}

private fun collectShortDramaItems(value: Any?): List<DramaItem> {
    val results = mutableListOf<DramaItem>()
    val visited = mutableSetOf<Any>()
    
    fun traverse(node: Any?) {
        if (node == null || node in visited) return
        if (node is Any) visited.add(node)
        
        when (node) {
            is JSONObject -> {
                node.toShortDramaItemOrNull()?.let { results.add(it) }
                val keys = node.keys()
                while (keys.hasNext()) {
                    traverse(node.opt(keys.next()))
                }
            }
            is org.json.JSONArray -> {
                for (i in 0 until node.length()) {
                    traverse(node.opt(i))
                }
            }
        }
    }
    
    traverse(value)
    return results
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
            firstInt("price", "coin_price", "unlock_price") > 0,
        likeCount = firstInt("like_count", "likes", "likes_count", "likeCount", "favorite_count")
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

private fun Any?.subtitleTracks(backendBaseUrl: String): List<SubtitleTrack> {
    return when (this) {
        is String -> takeIf { it.isNotBlank() }
            ?.let { listOf(SubtitleTrack("English", "en", it.normalizeMediaUrl(backendBaseUrl))) }
            .orEmpty()

        is JSONObject -> buildList {
            val direct = firstString("url", "src", "file", "vtt", "default")
            if (direct.isNotBlank()) {
                val label = firstString("label", "name", "language", "lang")
                val language = firstString("language", "lang").ifBlank { label.toSubtitleLanguageCode() }
                add(
                    SubtitleTrack(
                        label = label.toSubtitleLabel(),
                        language = language,
                        url = direct.normalizeMediaUrl(backendBaseUrl)
                    )
                )
                return@buildList
            }

            val keys = keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val nested = opt(key).subtitleTracks(backendBaseUrl)
                addAll(
                    nested.map { track ->
                        if (key.isSubtitleContainerKey() || (track.label.isNotBlank() && track.label != "English")) {
                            track
                        } else {
                            track.copy(label = key.toSubtitleLabel(), language = key)
                        }
                    }
                )
            }
        }

        is org.json.JSONArray -> buildList {
            for (index in 0 until length()) {
                addAll(opt(index).subtitleTracks(backendBaseUrl))
            }
        }

        else -> emptyList()
    }.distinctBy { it.url }
        .sortedWith(compareBy<SubtitleTrack> { it.language.subtitleSortRank() }.thenBy { it.label })
}

private fun String.toSubtitleLabel(): String = when (lowercase()) {
    "", "en", "eng", "english" -> "English"
    "hi", "hin", "hindi" -> "Hindi"
    "ms", "msa", "may", "malay" -> "Malay"
    "vi", "vie", "vietnamese" -> "Vietnamese"
    "ko", "kor", "korean" -> "Korean"
    "ja", "jpn", "japanese" -> "Japanese"
    "zh", "zho", "chi", "chinese", "cn" -> "Chinese"
    "th", "tha", "thai" -> "Thai"
    "ar", "ara", "arabic" -> "Arabic"
    "de", "ger", "deu", "german" -> "German"
    "it", "ita", "italian" -> "Italian"
    "ru", "rus", "russian" -> "Russian"
    "tr", "tur", "turkish" -> "Turkish"
    "bn", "ben", "bengali" -> "Bengali"
    "ta", "tam", "tamil" -> "Tamil"
    "te", "tel", "telugu" -> "Telugu"
    "mr", "mar", "marathi" -> "Marathi"
    "es", "spa", "spanish" -> "Spanish"
    "fr", "fra", "fre", "french" -> "French"
    "pt", "por", "portuguese" -> "Portuguese"
    "id", "indonesian" -> "Indonesian"
    else -> replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

private fun String.toSubtitleLanguageCode(): String = when (lowercase()) {
    "english", "eng", "en" -> "en"
    "hindi", "hin", "hi" -> "hi"
    "malay", "msa", "may", "ms" -> "ms"
    "vietnamese", "vie", "vi" -> "vi"
    "korean", "kor", "ko" -> "ko"
    "japanese", "jpn", "ja" -> "ja"
    "chinese", "zho", "chi", "zh", "cn" -> "zh"
    "thai", "tha", "th" -> "th"
    "arabic", "ara", "ar" -> "ar"
    "german", "deu", "ger", "de" -> "de"
    "italian", "ita", "it" -> "it"
    "russian", "rus", "ru" -> "ru"
    "turkish", "tur", "tr" -> "tr"
    "bengali", "ben", "bn" -> "bn"
    "tamil", "tam", "ta" -> "ta"
    "telugu", "tel", "te" -> "te"
    "marathi", "mar", "mr" -> "mr"
    "spanish", "spa", "es" -> "es"
    "french", "fra", "fre", "fr" -> "fr"
    "portuguese", "por", "pt" -> "pt"
    "indonesian", "id" -> "id"
    else -> "en"
}

private fun String.subtitleSortRank(): Int = when (lowercase()) {
    "en", "eng", "english" -> 0
    "hi", "hin", "hindi" -> 1
    "ms", "msa", "may", "malay" -> 2
    "vi", "vie", "vietnamese" -> 3
    "ko", "kor", "korean" -> 4
    "ja", "jpn", "japanese" -> 5
    "zh", "zho", "chi", "chinese", "cn" -> 6
    "th", "tha", "thai" -> 7
    "es", "spa", "spanish" -> 8
    "fr", "fra", "fre", "french" -> 9
    "pt", "por", "portuguese" -> 10
    else -> 99
}

private fun String.isSubtitleContainerKey(): Boolean =
    lowercase() in setOf("subtitles", "subtitle", "tracks", "captions", "items", "data")

private fun String.trimEndSlash(): String =
    trim().trimEnd('/').ifBlank { "https://drama-verse-backend.vercel.app/" }

private fun String.normalizeMediaUrl(backendBaseUrl: String): String {
    if (isBlank()) return ""
    return if (startsWith("/")) "${backendBaseUrl.trimEndSlash()}$this" else this
}
