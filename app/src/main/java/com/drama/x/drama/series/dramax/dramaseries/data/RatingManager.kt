package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

private const val TAG = "RatingManager"
private const val PREFS_NAME = "dramaverse_rating"
private const val KEY_HAS_RATED = "has_rated"

/**
 * Manages app rating state across the application.
 *
 * Rules:
 * - Rating popup shows only once per session
 * - Once user has rated (via Play Store, In-App Review, or feedback), never show again
 * - Triggers: Complete episode, exit player, add first favorite, return to Home, manual Settings entry
 */
class RatingManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Process-lifetime only. Resets automatically when the app is fully closed
    // and reopened (new process = new session), no persistence needed.
    @Volatile
    private var shownThisSession: Boolean = false

    /** Check if the app has already been rated (permanent, persisted). */
    fun hasRated(): Boolean = prefs.getBoolean(KEY_HAS_RATED, false)

    /** True only if not yet rated AND dialog hasn't been shown this session. */
    fun canShowRatingDialog(): Boolean {
        val result = !hasRated() && !shownThisSession
        Log.d(TAG, "canShowRatingDialog: hasRated=${hasRated()}, shownThisSession=$shownThisSession -> $result")
        return result
    }

    /** Call the instant the dialog actually becomes visible to the user. */
    fun markDialogShown() {
        shownThisSession = true
        Log.d(TAG, "Rating dialog marked as shown for this session")
    }

    /** Call once the user has made a rating decision (1-3 or 4-5 stars selected). */
    fun markAsRated() {
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
        shownThisSession = true
        Log.d(TAG, "App marked as rated")
    }

    /** Testing only. */
    fun resetRatingState() {
        prefs.edit().putBoolean(KEY_HAS_RATED, false).apply()
        shownThisSession = false
        Log.d(TAG, "Rating state reset")
    }

    companion object {
        @Volatile
        private var instance: RatingManager? = null

        fun getInstance(context: Context): RatingManager =
            instance ?: synchronized(this) {
                instance ?: RatingManager(context.applicationContext).also { instance = it }
            }
    }
}