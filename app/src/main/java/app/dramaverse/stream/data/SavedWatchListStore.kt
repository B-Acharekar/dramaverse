package app.dramaverse.stream.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SavedWatchListStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dramaverse_saved_watchlist", Context.MODE_PRIVATE)

    fun readItems(): List<DramaItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        DramaItem(
                            id = item.optInt("id"),
                            title = item.optString("title"),
                            description = item.optString("description"),
                            imageUrl = item.optString("imageUrl"),
                            rating = item.optString("rating", "4.8"),
                            episodeTotal = item.optInt("episodeTotal", 1).coerceAtLeast(1),
                            genre = item.optString("genre", "Drama"),
                            isPremium = item.optBoolean("isPremium"),
                            likeCount = item.optInt("likeCount")
                        )
                    )
                }
            }.filterNot { it.isInvalidSavedFilm() }
        }.getOrDefault(emptyList())
    }

    fun savedIds(): Set<Int> = readItems().map { it.id }.filter { it != 0 }.toSet()

    fun save(item: DramaItem) {
        if (item.isInvalidSavedFilm()) return
        writeItems((listOf(item) + readItems()).distinctBy { it.id })
    }

    fun remove(filmId: Int) {
        if (filmId == 0) return
        writeItems(readItems().filterNot { it.id == filmId })
    }

    private fun writeItems(items: List<DramaItem>) {
        val array = JSONArray()
        items.filterNot { it.isInvalidSavedFilm() }.take(MAX_ITEMS).forEach { item ->
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
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val MAX_ITEMS = 80
    }
}

private fun DramaItem.isInvalidSavedFilm(): Boolean {
    val normalizedTitle = title.trim().lowercase()
    val normalizedGenre = genre.trim().lowercase()
    val blockedTitles = setOf(
        "drama",
        "romance",
        "melodrama",
        "thriller",
        "mystery",
        "historical drama",
        "toxic love"
    )
    return title.isBlank() ||
        normalizedTitle in blockedTitles ||
        (id == 0 && imageUrl.isBlank()) ||
        (id == 0 && normalizedTitle == normalizedGenre)
}
