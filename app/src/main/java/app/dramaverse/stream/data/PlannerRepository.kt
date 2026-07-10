package app.dramaverse.stream.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class PlannerItem(
    val id: String,
    val filmId: Int,
    val title: String,
    val episode: Int?,
    val scheduledAt: String,
    val note: String,
    val imageUrl: String,
    val remindBeforeMinutes: Int
)

class PlannerRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val appContext = context.applicationContext

    suspend fun loadPlanner(backendBaseUrl: String): Result<List<PlannerItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            parsePlannerItems(getClientJson(backendBaseUrl, "client/planner", language, token))
        }
    }

    suspend fun savePlannerItem(
        backendBaseUrl: String,
        film: DramaItem,
        episode: Int,
        scheduledAt: OffsetDateTime,
        remindBeforeMinutes: Int
    ): Result<PlannerItem> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            val json = postClientJson(
                backendBaseUrl = backendBaseUrl,
                path = "client/planner",
                language = language,
                token = token,
                body = JSONObject()
                    .put("film_id", film.id)
                    .put("title", film.title)
                    .put("episode", episode)
                    .put("scheduled_at", scheduledAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .put("note", "Weekly watchlist reminder")
                    .put("image_url", film.imageUrl)
                    .put("remind_before_minutes", remindBeforeMinutes)
            )
            parsePlannerItem(json.optJSONObject("data") ?: json)
        }
    }

    suspend fun loadSuggestions(backendBaseUrl: String): Result<List<DramaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            val json = getClientJson(backendBaseUrl, "client/films", language, token)
            collectDramaItems(json)
                .distinctBy { it.id.takeIf { id -> id != 0 } ?: it.title }
                .filter { it.title.isNotBlank() }
                .take(8)
                .ifEmpty { fallbackPlannerFilms() }
        }
    }
}

private fun parsePlannerItems(json: JSONObject): List<PlannerItem> {
    val data = json.opt("data")
    val array = when (data) {
        is JSONArray -> data
        is JSONObject -> data.optJSONArray("items") ?: JSONArray().put(data)
        else -> JSONArray()
    }
    return buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let { add(parsePlannerItem(it)) }
        }
    }
}

private fun parsePlannerItem(item: JSONObject): PlannerItem = PlannerItem(
    id = item.optString("id"),
    filmId = item.firstInt("film_id", "filmId", "id"),
    title = item.firstString("title", "name"),
    episode = item.firstInt("episode", "episode_number").takeIf { it > 0 },
    scheduledAt = item.firstString("scheduled_at", "scheduledAt"),
    note = item.firstString("note"),
    imageUrl = item.firstString("image_url", "imageUrl", "thumb"),
    remindBeforeMinutes = item.firstInt("remind_before_minutes", "remindBeforeMinutes").takeIf { it >= 0 } ?: 15
)

private fun collectDramaItems(value: Any?): List<DramaItem> = when (value) {
    is JSONObject -> {
        val film = value.toPlannerDramaItemOrNull()
        val children = value.keys().asSequence().flatMap { collectDramaItems(value.opt(it)).asSequence() }.toList()
        listOfNotNull(film) + children
    }
    is JSONArray -> buildList {
        for (index in 0 until value.length()) addAll(collectDramaItems(value.opt(index)))
    }
    else -> emptyList()
}

private fun JSONObject.toPlannerDramaItemOrNull(): DramaItem? {
    val title = firstString("title", "name", "film_name", "movie_name")
    val image = firstString("thumb", "thumbnail", "image", "poster", "cover")
    if (title.isBlank()) return null
    return DramaItem(
        id = firstInt("id", "film_id", "movie_id"),
        title = title,
        description = firstString("description", "desc", "summary").ifBlank { "Plan this drama for later." },
        imageUrl = image,
        rating = firstString("rating", "rate", "score").ifBlank { "4.8" },
        episodeTotal = firstInt("episode_total", "episodes_count", "total_episodes").takeIf { it > 0 } ?: 1,
        genre = firstString("genre", "category", "tag").ifBlank { "Drama" }
    )
}

private fun fallbackPlannerFilms(): List<DramaItem> = listOf(
    DramaItem(1, "The Secret Vow", "A hidden promise changes two lives forever.", "", "4.8", 42, "Drama"),
    DramaItem(2, "Midnight Pulse", "A midnight cliffhanger worth planning.", "", "4.7", 64, "Thriller")
)

private fun JSONObject.firstString(vararg keys: String): String {
    for (key in keys) {
        when (val value = opt(key)) {
            is String -> if (value.isNotBlank()) return value
            is Number -> return value.toString()
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
