package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import java.time.LocalDate

class UnlockedEpisodesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dramaverse_unlocked_episodes", Context.MODE_PRIVATE)
    private val unlockKey = "unlocked_episodes"
    private val dailyUnlocksKey = "daily_unlocks_count"
    private val dailyUnlocksDateKey = "daily_unlocks_date"

    fun getUnlockedEpisodes(): Set<String> {
        return prefs.getStringSet(unlockKey, emptySet()) ?: emptySet()
    }

    fun isEpisodeUnlocked(filmId: Int, episodeNumber: Int): Boolean {
        return getUnlockedEpisodes().contains("$filmId:$episodeNumber")
    }

    fun unlockEpisode(filmId: Int, episodeNumber: Int) {
        val currentUnlocked = getUnlockedEpisodes().toMutableSet()
        currentUnlocked.add("$filmId:$episodeNumber")
        prefs.edit().putStringSet(unlockKey, currentUnlocked).apply()
    }

    fun unlockMultipleEpisodes(filmId: Int, episodeNumbers: List<Int>) {
        val currentUnlocked = getUnlockedEpisodes().toMutableSet()
        episodeNumbers.forEach { ep ->
            currentUnlocked.add("$filmId:$ep")
        }
        prefs.edit().putStringSet(unlockKey, currentUnlocked).apply()
    }

    fun clearUnlockedEpisodes() {
        prefs.edit().remove(unlockKey).apply()
    }

    /**
     * Get the number of daily unlocks used today.
     * Resets automatically if the date has changed.
     */
    fun getDailyUnlocksUsed(): Int {
        val today = LocalDate.now().toString()
        val storedDate = prefs.getString(dailyUnlocksDateKey, "")
        
        // If the stored date is different from today, reset the counter
        if (storedDate != today) {
            prefs.edit()
                .putInt(dailyUnlocksKey, 0)
                .putString(dailyUnlocksDateKey, today)
                .apply()
            return 0
        }
        
        return prefs.getInt(dailyUnlocksKey, 0)
    }

    /**
     * Increment the daily unlock counter for today.
     */
    fun incrementDailyUnlocks() {
        val today = LocalDate.now().toString()
        val currentCount = getDailyUnlocksUsed()
        prefs.edit()
            .putInt(dailyUnlocksKey, currentCount + 1)
            .putString(dailyUnlocksDateKey, today)
            .apply()
    }

    /**
     * Reset daily unlocks (called at midnight or on app restart if date changed).
     */
    fun resetDailyUnlocksIfNeeded() {
        getDailyUnlocksUsed()  // This will automatically reset if needed
    }
}
