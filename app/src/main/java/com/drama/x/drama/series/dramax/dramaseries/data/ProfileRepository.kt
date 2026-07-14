package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ProfileSummary(
    val userName: String,
    val guestId: String,
    val deviceId: String,
    val avatarUrl: String?,
    val coins: Int,
    val hoursWatched: Int,
    val minutesWatched: Int,
    val episodesWatched: Int
)

class ProfileRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val appContext = context.applicationContext

    suspend fun loadProfile(
        backendBaseUrl: String,
        language: String = LocaleHelper.persistedLanguageCode(appContext)
    ): Result<ProfileSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepository.clientToken(backendBaseUrl, language)
            coroutineScope {
                val profile = async {
                    getClientJson(backendBaseUrl, "client/me", language, token)
                }
                val rewards = async {
                    runCatching { getClientJson(backendBaseUrl, "client/rewards", language, token) }.getOrNull()
                }
                val history = async {
                    runCatching { getClientJson(backendBaseUrl, "client/history/watch", language, token) }.getOrNull()
                }
                buildProfileSummary(
                    profileJson = profile.await(),
                    rewardsJson = rewards.await(),
                    historyJson = history.await(),
                    localDeviceId = authRepository.deviceId()
                )
            }
        }
    }
}

private fun buildProfileSummary(
    profileJson: JSONObject,
    rewardsJson: JSONObject?,
    historyJson: JSONObject?,
    localDeviceId: String
): ProfileSummary {
    val profile = profileJson.optJSONObject("data") ?: profileJson
    val guestId = profile.firstString("guest_id", "guestId", "id", "user_id", "uid").ifBlank { localDeviceId }
    val userName = profile.firstString("display_name", "displayName", "username", "name", "nick_name", "nickname")
        .ifBlank { "Guest ${guestId.takeLast(6)}" }
    val rewardData = rewardsJson?.optJSONObject("data") ?: rewardsJson
    val historyItems = historyJson.historyItems()
    val watchedSeconds = historyItems.sumOf { item ->
        item.firstInt("progress_seconds", "progressSeconds", "current_time", "time_watching")
            .coerceAtLeast(0)
    }
    val watchedMinutes = (watchedSeconds / 60).coerceAtLeast(0)
    return ProfileSummary(
        userName = userName,
        guestId = guestId,
        deviceId = profile.firstString("device_id", "deviceId").ifBlank { localDeviceId },
        avatarUrl = profile.firstString("profile_pic_png", "avatar_url", "avatar", "profile_pic", "photo", "image")
            .takeIf { it.isNotBlank() },
        coins = rewardData?.firstInt("coins", "balance", "coin") ?: 0,
        hoursWatched = (watchedSeconds / 3600).coerceAtLeast(0),
        minutesWatched = watchedMinutes,
        episodesWatched = historyItems.count()
    )
}

private fun JSONObject?.historyItems(): List<JSONObject> {
    val data = this?.opt("data") ?: return emptyList()
    val array = when (data) {
        is JSONArray -> data
        is JSONObject -> data.optJSONArray("items") ?: data.optJSONArray("data") ?: JSONArray()
        else -> JSONArray()
    }
    return buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let(::add)
        }
    }
}

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
