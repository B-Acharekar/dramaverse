package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

private const val TAG = "RatingManager"
private const val PREFS_NAME = "dramaverse_rating"
private const val KEY_HAS_RATED = "has_rated"
private const val KEY_SHOWN_THIS_SESSION = "shown_this_session"
private const val KEY_SESSION_ID = "session_id"

/**
 * Manages app rating state across the application.
 * 
 * Rules:
 * - Rating popup shows only once per session
 * - Once user has rated (via Play Store or feedback), never show again
 * - Triggers: Complete episode, exit player, add to favorites
 */
class RatingManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var currentSessionId: String
    
    init {
        // Generate new session ID on initialization
        currentSessionId = System.currentTimeMillis().toString()
        val lastSessionId = prefs.getString(KEY_SESSION_ID, null)
        
        if (lastSessionId != currentSessionId) {
            // New session - reset the shown flag
            prefs.edit()
                .putString(KEY_SESSION_ID, currentSessionId)
                .putBoolean(KEY_SHOWN_THIS_SESSION, false)
                .apply()
            Log.d(TAG, "New session started: $currentSessionId")
        }
    }
    
    /**
     * Check if the app has been rated already.
     */
    fun hasRated(): Boolean {
        return prefs.getBoolean(KEY_HAS_RATED, false)
    }
    
    /**
     * Check if rating dialog can be shown.
     * Returns true only if:
     * - User has not rated the app yet
     * - Dialog has not been shown in this session
     */
    fun canShowRatingDialog(): Boolean {
        val hasRated = hasRated()
        val shownThisSession = prefs.getBoolean(KEY_SHOWN_THIS_SESSION, false)
        
        Log.d(TAG, "canShowRatingDialog: hasRated=$hasRated, shownThisSession=$shownThisSession")
        return !hasRated && !shownThisSession
    }
    
    /**
     * Mark that the rating dialog has been shown in this session.
     */
    fun markDialogShown() {
        prefs.edit()
            .putBoolean(KEY_SHOWN_THIS_SESSION, true)
            .apply()
        Log.d(TAG, "Rating dialog marked as shown for session $currentSessionId")
    }
    
    /**
     * Mark that the user has rated the app.
     * This prevents the dialog from showing again in the future.
     */
    fun markAsRated() {
        prefs.edit()
            .putBoolean(KEY_HAS_RATED, true)
            .putBoolean(KEY_SHOWN_THIS_SESSION, true)
            .apply()
        Log.d(TAG, "App marked as rated")
    }
    
    /**
     * Reset rating state (for testing purposes only).
     */
    fun resetRatingState() {
        prefs.edit()
            .putBoolean(KEY_HAS_RATED, false)
            .putBoolean(KEY_SHOWN_THIS_SESSION, false)
            .apply()
        Log.d(TAG, "Rating state reset")
    }
    
    companion object {
        @Volatile
        private var instance: RatingManager? = null
        
        fun getInstance(context: Context): RatingManager {
            return instance ?: synchronized(this) {
                instance ?: RatingManager(context).also { instance = it }
            }
        }
    }
}
