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
 * New Rules (Updated):
 * - Show rating dialog only once per app session
 * - First eligible trigger can display the rate dialog
 * - Once shown, don't show again during same session
 * - If user is already Rated, never show again
 * - New session starts after app completely closed and reopened
 * - Monetization: If Interstitial Ad displayed for current trigger, skip Rate dialog only for that trigger
 * 
 * Triggers:
 * - Flow 1: Complete watching (exit player or start next episode) - pauses video if shown
 * - Flow 2: First favorite/add to My List - pauses video if shown during playback
 * - Flow 3: Return to Home from another screen/tab (no Interstitial Ad shown)
 * - Manual: Rate App in Settings
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
     * Check if rating dialog can be shown (for automatic triggers).
     * Returns true only if:
     * - User has not rated the app yet
     * - Dialog has not been shown in this session
     * 
     * For automatic triggers like Flow 1, 2, 3.
     */
    fun canShowRatingDialog(): Boolean {
        val hasRated = hasRated()
        val shownThisSession = prefs.getBoolean(KEY_SHOWN_THIS_SESSION, false)
        
        Log.d(TAG, "canShowRatingDialog: hasRated=$hasRated, shownThisSession=$shownThisSession")
        return !hasRated && !shownThisSession
    }
    
    /**
     * Check if rating dialog can be shown from Settings (manual trigger).
     * For manual trigger: Show dialog if not shown in current session.
     * If already rated, caller should show "Thanks for your rating!" message.
     * 
     * Returns true only if:
     * - User has not rated the app yet
     * - Dialog has not been shown in this session
     */
    fun canShowRatingDialogFromSettings(): Boolean {
        val hasRated = hasRated()
        val shownThisSession = prefs.getBoolean(KEY_SHOWN_THIS_SESSION, false)
        
        Log.d(TAG, "canShowRatingDialogFromSettings: hasRated=$hasRated, shownThisSession=$shownThisSession")
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
