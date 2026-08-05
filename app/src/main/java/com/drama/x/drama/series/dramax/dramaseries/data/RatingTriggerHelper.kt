package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.drama.x.drama.series.dramax.dramaseries.screen.AppRatingDialog

private const val TAG = "RatingTriggerHelper"

/**
 * Helper class to trigger rating dialogs at appropriate moments.
 * 
 * Updated Trigger Flows:
 * - Flow 1: Complete watching (exit video player or start next episode)
 * - Flow 2: First favorite/add to My List
 * - Flow 3: Return to Home from another screen (no Interstitial Ad shown)
 * 
 * Rules:
 * - Show only once per session
 * - First eligible trigger shows the dialog
 * - If Interstitial Ad is shown for the trigger, skip rating for that trigger only
 * - Never show if user already rated
 */
class RatingTriggerHelper(private val context: Context) {
    private val ratingManager = RatingManager.getInstance(context)
    
    /**
     * Flow 1: Trigger after completing episode and exiting video player.
     * Called when user exits player or starts next episode.
     * 
     * @param interstitialShown True if an Interstitial Ad was shown for this trigger
     * @return True if rating dialog should be shown
     */
    fun shouldTriggerAfterVideoExit(interstitialShown: Boolean = false): Boolean {
        if (interstitialShown) {
            Log.d(TAG, "Flow 1: Skipped - Interstitial Ad shown")
            return false
        }
        
        if (ratingManager.canShowRatingDialog()) {
            Log.d(TAG, "Flow 1: Triggered after video exit")
            return true
        }
        return false
    }
    
    /**
     * Flow 2: Trigger after adding first item to favorites/My List.
     * 
     * @param interstitialShown True if an Interstitial Ad was shown for this trigger
     * @param isFirstFavorite True if this is the first time user adds to favorites
     * @return True if rating dialog should be shown
     */
    fun shouldTriggerAfterAddToFavorites(interstitialShown: Boolean = false, isFirstFavorite: Boolean = true): Boolean {
        if (interstitialShown) {
            Log.d(TAG, "Flow 2: Skipped - Interstitial Ad shown")
            return false
        }
        
        if (!isFirstFavorite) {
            Log.d(TAG, "Flow 2: Skipped - Not first favorite")
            return false
        }
        
        if (ratingManager.canShowRatingDialog()) {
            Log.d(TAG, "Flow 2: Triggered after add to favorites")
            return true
        }
        return false
    }
    
    /**
     * Flow 3: Trigger when returning to Home from another screen/tab.
     * Only triggers if no Interstitial Ad was shown.
     * 
     * @param interstitialShown True if an Interstitial Ad was shown during navigation
     * @return True if rating dialog should be shown
     */
    fun shouldTriggerOnReturnToHome(interstitialShown: Boolean = false): Boolean {
        if (interstitialShown) {
            Log.d(TAG, "Flow 3: Skipped - Interstitial Ad shown")
            return false
        }
        
        if (ratingManager.canShowRatingDialog()) {
            Log.d(TAG, "Flow 3: Triggered on return to Home")
            return true
        }
        return false
    }
    
    /**
     * Mark that the rating dialog has been shown.
     * Call this when actually displaying the dialog.
     */
    fun markDialogShown() {
        ratingManager.markDialogShown()
    }
    
    companion object {
        @Volatile
        private var instance: RatingTriggerHelper? = null
        
        fun getInstance(context: Context): RatingTriggerHelper {
            return instance ?: synchronized(this) {
                instance ?: RatingTriggerHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Composable state holder for managing rating dialog display.
 * Use this in your screens to conditionally show the rating dialog.
 * 
 * @param shouldTrigger When true, checks if rating dialog should be shown
 * @param onRated Callback when user completes rating
 * @param isManualTrigger True for Settings button trigger, false for automatic triggers
 * 
 * Example usage:
 * ```
 * val ratingState = rememberRatingDialogState(
 *     shouldTrigger = navigatedToHomeFromPlayer,
 *     onRated = { viewModel.onUserRated() }
 * )
 * 
 * RatingDialogHost(ratingState)
 * ```
 */
@Composable
fun rememberRatingDialogState(
    shouldTrigger: Boolean = false,
    onRated: () -> Unit = {},
    isManualTrigger: Boolean = false
): RatingDialogState {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ratingManager = remember { RatingManager.getInstance(context) }
    var showDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(shouldTrigger) {
        if (shouldTrigger && ratingManager.canShowRatingDialog()) {
            showDialog = true
            ratingManager.markDialogShown()
        }
    }
    
    return remember(showDialog) {
        RatingDialogState(
            showDialog = showDialog,
            onDismiss = { showDialog = false },
            onRated = {
                showDialog = false
                onRated()
            },
            isManualTrigger = isManualTrigger
        )
    }
}

/**
 * State holder for rating dialog.
 */
data class RatingDialogState(
    val showDialog: Boolean,
    val onDismiss: () -> Unit,
    val onRated: () -> Unit,
    val isManualTrigger: Boolean = false
)

/**
 * Host composable that displays the rating dialog based on state.
 * 
 * @param state The rating dialog state from rememberRatingDialogState
 */
//@Composable
//fun RatingDialogHost(state: RatingDialogState) {
//    if (state.showDialog) {
//        AppRatingDialog(
//            onDismiss = state.onDismiss,
//            onRated = state.onRated,
//            isManualTrigger = state.isManualTrigger
//        )
//    }
//}

/**
 * Simple extension to check if rating should be triggered for a specific action.
 */
fun Context.shouldShowRatingDialog(): Boolean {
    return RatingManager.getInstance(this).canShowRatingDialog()
}

/**
 * Extension to mark that rating dialog has been shown.
 */
fun Context.markRatingDialogShown() {
    RatingManager.getInstance(this).markDialogShown()
}
