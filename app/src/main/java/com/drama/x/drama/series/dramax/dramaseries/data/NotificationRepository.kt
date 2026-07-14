package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val read: Boolean,
    val createdAt: String
)

data class NotificationFeed(
    val unreadCount: Int,
    val items: List<AppNotification>
)

class NotificationRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val appContext = context.applicationContext

    suspend fun loadNotifications(backendBaseUrl: String): Result<NotificationFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            val json = getClientJson(backendBaseUrl, "client/notifications", language, token)
            parseNotifications(json)
        }
    }

    suspend fun trackNotification(
        backendBaseUrl: String,
        title: String,
        body: String,
        type: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            postClientJson(
                backendBaseUrl = backendBaseUrl,
                path = "client/notifications",
                language = language,
                token = token,
                body = JSONObject()
                    .put("title", title)
                    .put("body", body)
                    .put("type", type)
                    .put("metadata", JSONObject())
            )
            Unit
        }
    }

    suspend fun clearNotifications(backendBaseUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            deleteClientJson(backendBaseUrl, "client/notifications", language, token)
            Unit
        }
    }
}

private fun parseNotifications(json: JSONObject): NotificationFeed {
    val data = json.opt("data")
    val array = when (data) {
        is JSONArray -> data
        is JSONObject -> data.optJSONArray("items") ?: JSONArray().put(data)
        else -> JSONArray()
    }
    val items = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            add(
                AppNotification(
                    id = item.optString("id"),
                    title = item.optString("title", "DramaX"),
                    body = item.optString("body", ""),
                    type = item.optString("type", "general"),
                    read = item.optBoolean("read", false),
                    createdAt = item.optString("created_at", item.optString("updated_at"))
                )
            )
        }
    }
    return NotificationFeed(
        unreadCount = json.optInt("unread_count", items.count { !it.read }),
        items = items
    )
}
