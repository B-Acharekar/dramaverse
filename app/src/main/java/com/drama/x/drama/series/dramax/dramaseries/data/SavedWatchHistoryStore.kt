package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SavedWatchHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dramaverse_watch_history", Context.MODE_PRIVATE)

    fun readItems(): List<ContinueWatchingItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val filmJson = item.optJSONObject("film") ?: continue
                    val film = DramaItem(
                        id = filmJson.optInt("id"),
                        title = filmJson.optString("title"),
                        description = filmJson.optString("description"),
                        imageUrl = filmJson.optString("imageUrl"),
                        rating = filmJson.optString("rating", "4.8"),
                        episodeTotal = filmJson.optInt("episodeTotal", 1).coerceAtLeast(1),
                        genre = filmJson.optString("genre", "Drama"),
                        isPremium = filmJson.optBoolean("isPremium"),
                        likeCount = filmJson.optInt("likeCount")
                    )
                    if (film.isInvalidHistoryFilm()) continue
                    add(
                        ContinueWatchingItem(
                            film = film,
                            episodeNumber = item.optInt("episodeNumber", 1).coerceAtLeast(1),
                            progressSeconds = item.optInt("progressSeconds", 0).coerceAtLeast(0),
                            durationSeconds = item.optInt("durationSeconds", 0).coerceAtLeast(0),
                            completed = item.optBoolean("completed", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(
        film: DramaItem,
        episodeNumber: Int,
        progressSeconds: Int,
        durationSeconds: Int?,
        completed: Boolean
    ) {
        if (film.isInvalidHistoryFilm()) return
        val item = ContinueWatchingItem(
            film = film,
            episodeNumber = episodeNumber.coerceAtLeast(1),
            progressSeconds = progressSeconds.coerceAtLeast(0),
            durationSeconds = durationSeconds?.coerceAtLeast(0) ?: 0,
            completed = completed
        )
        writeItems((listOf(item) + readItems()).distinctBy { it.film.stableHistoryKey() })
    }

    fun clearAll() {
        writeItems(emptyList())
    }

    private fun writeItems(items: List<ContinueWatchingItem>) {
        val array = JSONArray()
        items.take(MAX_ITEMS).forEach { item ->
            array.put(
                JSONObject()
                    .put(
                        "film",
                        JSONObject()
                            .put("id", item.film.id)
                            .put("title", item.film.title)
                            .put("description", item.film.description)
                            .put("imageUrl", item.film.imageUrl)
                            .put("rating", item.film.rating)
                            .put("episodeTotal", item.film.episodeTotal)
                            .put("genre", item.film.genre)
                            .put("isPremium", item.film.isPremium)
                            .put("likeCount", item.film.likeCount)
                    )
                    .put("episodeNumber", item.episodeNumber)
                    .put("progressSeconds", item.progressSeconds)
                    .put("durationSeconds", item.durationSeconds)
                    .put("completed", item.completed)
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val MAX_ITEMS = 100
    }
}

fun List<ContinueWatchingItem>.mergeLocalWatchHistory(localItems: List<ContinueWatchingItem>): List<ContinueWatchingItem> {
    return (localItems + this)
        .filterNot { it.film.isInvalidHistoryFilm() }
        .distinctBy { it.film.stableHistoryKey() }
}

private fun DramaItem.stableHistoryKey(): Any = id.takeIf { it != 0 } ?: title.trim().lowercase()

private fun DramaItem.isInvalidHistoryFilm(): Boolean {
    val normalizedTitle = title.trim().lowercase()
    return title.isBlank() ||
        normalizedTitle in setOf("drama", "romance", "thriller", "melodrama", "historical drama") ||
        (id == 0 && imageUrl.isBlank())
}
