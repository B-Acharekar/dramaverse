package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "dramaverse_rating"
private const val KEY_HAS_ADDED_FIRST_FAVORITE = "has_added_first_favorite"

/**
 * Tracks whether the user has added their first favorite/watchlist item.
 * Used to determine if Flow 2 rating trigger should fire.
 */
class FirstFavoriteTracker(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if user has added at least one favorite before.
     */
    fun hasAddedFirstFavorite(): Boolean {
        return prefs.getBoolean(KEY_HAS_ADDED_FIRST_FAVORITE, false)
    }
    
    /**
     * Mark that user has added their first favorite.
     * Returns true if this was the FIRST time (for trigger logic).
     */
    fun markFavoriteAdded(): Boolean {
        val wasFirst = !hasAddedFirstFavorite()
        if (wasFirst) {
            prefs.edit()
                .putBoolean(KEY_HAS_ADDED_FIRST_FAVORITE, true)
                .apply()
        }
        return wasFirst
    }
    
    /**
     * Reset state (for testing only).
     */
    fun reset() {
        prefs.edit()
            .putBoolean(KEY_HAS_ADDED_FIRST_FAVORITE, false)
            .apply()
    }
    
    companion object {
        @Volatile
        private var instance: FirstFavoriteTracker? = null
        
        fun getInstance(context: Context): FirstFavoriteTracker {
            return instance ?: synchronized(this) {
                instance ?: FirstFavoriteTracker(context).also { instance = it }
            }
        }
    }
}
