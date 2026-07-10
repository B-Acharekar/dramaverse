package app.dramaverse.stream.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RewardPackage(
    val title: String,
    val amount: Int,
    val price: String,
    val bestValue: Boolean = false
)

data class RewardAchievement(
    val title: String,
    val subtitle: String,
    val unlocked: Boolean
)

data class RewardFeed(
    val coins: Int,
    val vip: Boolean,
    val checkInDay: Int,
    val spinAvailable: Int,
    val watchMinutesToday: Int,
    val coinPackages: List<RewardPackage>,
    val subscriptionPackages: List<RewardPackage>,
    val achievements: List<RewardAchievement>
)

fun fallbackRewardFeed(): RewardFeed = RewardFeed(
    coins = 1240,
    vip = false,
    checkInDay = 4,
    spinAvailable = 1,
    watchMinutesToday = 22,
    coinPackages = listOf(
        RewardPackage("50 Coins", 50, "$0.99"),
        RewardPackage("100 Coins", 100, "$1.99"),
        RewardPackage("250 Coins", 250, "$4.99"),
        RewardPackage("500 Coins", 500, "$8.99"),
        RewardPackage("1000 Coins Pack", 1000, "$14.99", true)
    ),
    subscriptionPackages = listOf(
        RewardPackage("Monthly", 0, "$10.99"),
        RewardPackage("Yearly", 0, "$100.99", true),
        RewardPackage("Weekly", 0, "$4.99")
    ),
    achievements = listOf(
        RewardAchievement("Drama King", "Watch 100 episodes", true),
        RewardAchievement("Night Owl", "Watch after 12 AM", false),
        RewardAchievement("Top Fan", "Favorite 5 series", true),
        RewardAchievement("Socialite", "Invite 3 friends", false)
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
        amount: Int
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
                    .put("amount", amount)
                    .put("metadata", JSONObject().put("checked_at", System.currentTimeMillis()))
            )
            parseRewardFeed(json.optJSONObject("data") ?: json)
        }
    }
}

private fun parseRewardFeed(data: JSONObject): RewardFeed {
    val unlocked = data.optJSONArray("achievements").stringSet()
    return RewardFeed(
        coins = data.optInt("coins", 1240),
        vip = data.optBoolean("vip", false),
        checkInDay = data.optInt("check_in_day", 4).coerceIn(1, 7),
        spinAvailable = data.optInt("spin_available", 1),
        watchMinutesToday = data.optInt("watch_minutes_today", 22).coerceAtLeast(0),
        coinPackages = data.opt("coin_packages").toRewardPackages(
            fallback = listOf(
                RewardPackage("50 Coins", 50, "$0.99"),
                RewardPackage("100 Coins", 100, "$1.99"),
                RewardPackage("250 Coins", 250, "$4.99"),
                RewardPackage("500 Coins", 500, "$8.99"),
                RewardPackage("1000 Coins Pack", 1000, "$14.99", true)
            )
        ),
        subscriptionPackages = data.opt("subscription_packages").toRewardPackages(
            fallback = listOf(
                RewardPackage("Monthly", 0, "$10.99"),
                RewardPackage("Yearly", 0, "$100.99", true),
                RewardPackage("Weekly", 0, "$4.99")
            )
        ),
        achievements = listOf(
            RewardAchievement("Drama King", "Watch 100 episodes", "drama_king" in unlocked),
            RewardAchievement("Night Owl", "Watch after 12 AM", "night_owl" in unlocked),
            RewardAchievement("Top Fan", "Favorite 5 series", "top_fan" in unlocked),
            RewardAchievement("Socialite", "Invite 3 friends", "socialite" in unlocked)
        )
    )
}

private fun Any?.toRewardPackages(fallback: List<RewardPackage>): List<RewardPackage> {
    val items = when (this) {
        is JSONArray -> collectPackages(this)
        is JSONObject -> collectPackages(this.optJSONArray("items") ?: this.optJSONArray("data") ?: JSONArray().put(this))
        else -> emptyList()
    }
    return items.ifEmpty { fallback }
}

private fun collectPackages(array: JSONArray): List<RewardPackage> = buildList {
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val amount = item.firstInt("coins", "coin", "amount", "value")
        val title = item.firstString("title", "name", "label").ifBlank {
            if (amount > 0) "$amount Coins" else "VIP Plan"
        }
        val price = item.firstString("price", "amount_text", "display_price", "currency_price").ifBlank {
            item.firstInt("price_cents", "cents").takeIf { it > 0 }?.let { "$${"%.2f".format(it / 100f)}" }.orEmpty()
        }.ifBlank { "$0.99" }
        add(
            RewardPackage(
                title = title,
                amount = amount,
                price = price,
                bestValue = item.optBoolean("best_value", false) || index == array.length() - 1
            )
        )
    }
}

private fun JSONArray?.stringSet(): Set<String> {
    if (this == null) return emptySet()
    return buildSet {
        for (index in 0 until length()) optString(index).takeIf { it.isNotBlank() }?.let(::add)
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
