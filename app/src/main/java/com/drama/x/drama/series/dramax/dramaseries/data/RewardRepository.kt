package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CheckInReward(
    val day: Int,
    val reward: Int,
    val current: Boolean,
    val claimed: Boolean,
    val status: String
)

data class DailyRewardTask(
    val id: String,
    val title: String,
    val targetMinutes: Int,
    val reward: Int,
    val progressMinutes: Int,
    val completed: Boolean,
    val claimed: Boolean
)

data class SpinReward(
    val available: Boolean,
    val weekKey: String,
    val segments: List<Int>,
    val selectedIndex: Int?,
    val lastReward: Int?
)

data class RewardFeed(
    val coins: Int,
    val checkInDay: Int,
    val canCheckIn: Boolean,
    val checkInRewards: List<CheckInReward>,
    val dailyTasks: List<DailyRewardTask>,
    val spin: SpinReward,
    val rules: List<String>
)

fun fallbackRewardFeed(): RewardFeed = RewardFeed(
    coins = 0,
    checkInDay = 1,
    canCheckIn = true,
    checkInRewards = defaultCheckIns(),
    dailyTasks = defaultTasks(),
    spin = SpinReward(available = true, weekKey = "", segments = listOf(0, 10, 15, 20, 30, 40, 60, 100), selectedIndex = null, lastReward = null),
    rules = listOf(
        "Balance starts at 0 coins.",
        "Daily check-in starts at +20 and resets after day 7.",
        "Daily tasks unlock after real watch time.",
        "Spin wheel can be used once per week."
    )
)

class RewardRepository(
    context: Context,
    private val authRepository: AuthRepository
) {
    private val appContext = context.applicationContext

    suspend fun loadRewards(backendBaseUrl: String): Result<RewardFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            val json = getClientJson(backendBaseUrl, "client/rewards", language, token)
            parseRewardFeed(json.optJSONObject("data") ?: json)
        }
    }

    suspend fun claimAction(
        backendBaseUrl: String,
        action: String,
        metadata: JSONObject = JSONObject()
    ): Result<RewardFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val language = LocaleHelper.persistedLanguageCode(appContext)
            val token = authRepository.clientToken(backendBaseUrl, language)
            val json = postClientJson(
                backendBaseUrl = backendBaseUrl,
                path = "client/rewards/action",
                language = language,
                token = token,
                body = JSONObject()
                    .put("action", action)
                    .put("amount", 0)
                    .put("metadata", metadata)
            )
            parseRewardFeed(json.optJSONObject("data") ?: json)
        }
    }
}

private fun parseRewardFeed(data: JSONObject): RewardFeed = RewardFeed(
    coins = data.optInt("coins", 0),
    checkInDay = data.optInt("check_in_day", 1).coerceIn(1, 7),
    canCheckIn = data.optBoolean("can_check_in", true),
    checkInRewards = data.optJSONArray("check_in_rewards").toCheckIns(),
    dailyTasks = data.optJSONArray("daily_tasks").toDailyTasks(),
    spin = data.optJSONObject("spin").toSpinReward(),
    rules = data.optJSONArray("rules").toStringList()
)

private fun JSONArray?.toCheckIns(): List<CheckInReward> {
    if (this == null) return defaultCheckIns()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                CheckInReward(
                    day = item.optInt("day", index + 1),
                    reward = item.optInt("reward", 20 + index * 5),
                    current = item.optBoolean("current", index == 0),
                    claimed = item.optBoolean("claimed", false),
                    status = item.optString("status", if (item.optBoolean("current", false)) "today" else if (item.optBoolean("claimed", false)) "claimed" else "locked")
                )
            )
        }
    }.ifEmpty { defaultCheckIns() }
}

private fun JSONArray?.toDailyTasks(): List<DailyRewardTask> {
    if (this == null) return defaultTasks()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                DailyRewardTask(
                    id = item.optString("id"),
                    title = item.optString("title"),
                    targetMinutes = item.optInt("target_minutes", 5),
                    reward = item.optInt("reward", 15),
                    progressMinutes = item.optInt("progress_minutes", 0),
                    completed = item.optBoolean("completed", false),
                    claimed = item.optBoolean("claimed", false)
                )
            )
        }
    }.ifEmpty { defaultTasks() }
}

private fun JSONObject?.toSpinReward(): SpinReward {
    if (this == null) return fallbackRewardFeed().spin
    return SpinReward(
        available = optBoolean("available", true),
        weekKey = optString("week_key"),
        segments = optJSONArray("segments").toIntList().ifEmpty { fallbackRewardFeed().spin.segments },
        selectedIndex = if (has("selected_index") && !isNull("selected_index")) optInt("selected_index") else null,
        lastReward = if (has("last_reward") && !isNull("last_reward")) optInt("last_reward") else null
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return fallbackRewardFeed().rules
    return buildList {
        for (index in 0 until length()) optString(index).takeIf { it.isNotBlank() }?.let(::add)
    }.ifEmpty { fallbackRewardFeed().rules }
}

private fun JSONArray?.toIntList(): List<Int> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) add(optInt(index))
    }.filter { it >= 0 }
}

private fun defaultCheckIns(): List<CheckInReward> =
    listOf(20, 25, 30, 35, 40, 45, 60).mapIndexed { index, reward ->
        CheckInReward(day = index + 1, reward = reward, current = index == 0, claimed = false, status = if (index == 0) "today" else "locked")
    }

private fun defaultTasks(): List<DailyRewardTask> = listOf(
    DailyRewardTask("watch_5", "Watch 5 minutes", 5, 15, 0, false, false),
    DailyRewardTask("watch_10", "Watch 10 minutes", 10, 20, 0, false, false),
    DailyRewardTask("watch_15", "Watch 15 minutes", 15, 30, 0, false, false)
)
