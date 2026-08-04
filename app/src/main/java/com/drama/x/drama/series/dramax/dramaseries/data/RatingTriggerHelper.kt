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
 * Manages the logic for when to show rating prompts based on user actions.
 */
class RatingTriggerHelper(private val context: Context) {
    private val ratingManager = RatingManager.getInstance(context)
    
    /**
     * Trigger rating after completing an episode and returning to home.
     */
    fun triggerAfterEpisodeComplete() {
        if (ratingManager.canShowRatingDialog()) {
            Log.d(TAG, "Triggered rating after episode complete")
            ratingManager.markDialogShown()
        }
    }
    
    /**
     * Trigger rating after exiting player.
     */
    fun triggerAfterExitPlayer() {
        if (ratingManager.canShowRatingDialog()) {
            Log.d(TAG, "Triggered rating after exit player")
            ratingManager.markDialogShown()
        }
    }
    
    /**
     * Trigger rating after adding to favorites/watchlist.
     */
    fun triggerAfterAddToFavorites() {
        if (ratingManager.canShowRatingDialog()) {
            Log.d(TAG, "Triggered rating after add to favorites")
            ratingManager.markDialogShown()
        }
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
    onRated: () -> Unit = {}
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
            }
        )
    }
}

/**
 * State holder for rating dialog.
 */
data class RatingDialogState(
    val showDialog: Boolean,
    val onDismiss: () -> Unit,
    val onRated: () -> Unit
)

/**
 * Host composable that displays the rating dialog based on state.
 * 
 * @param state The rating dialog state from rememberRatingDialogState
 */
@Composable
fun RatingDialogHost(state: RatingDialogState) {
    if (state.showDialog) {
        AppRatingDialog(
            onDismiss = state.onDismiss,
            onRated = state.onRated
        )
    }
}

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
