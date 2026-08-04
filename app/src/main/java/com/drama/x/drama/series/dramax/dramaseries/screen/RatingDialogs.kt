package com.drama.x.drama.series.dramax.dramaseries.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.data.RatingManager
import com.google.android.play.core.review.ReviewManagerFactory

private const val TAG = "RatingDialogs"

/**
 * Enhanced rating dialog with emoji feedback and star selection.
 * Supports:
 * - 1-3 stars: Show feedback dialog
 * - 4-5 stars: Launch In-App Review or redirect to Play Store
 */
@Composable
fun AppRatingDialog(
    onDismiss: () -> Unit,
    onRated: () -> Unit
) {
    val context = LocalContext.current
    val ratingManager = remember { RatingManager.getInstance(context) }
    var selectedRating by remember { mutableStateOf(0) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    // Get emoji based on rating
    val emoji = when (selectedRating) {
        1 -> "😞"
        2 -> "😕"
        3 -> "😐"
        4 -> "😊"
        5 -> "😍"
        else -> "🤔"
    }
    
    val title = when (selectedRating) {
        0 -> stringResource(R.string.rateus_dialog_title)
        in 1..2 -> stringResource(R.string.rate_dialog_sad_title)
        3 -> stringResource(R.string.rate_dialog_neutral_title)
        in 4..5 -> stringResource(R.string.rate_dialog_happy_title)
        else -> stringResource(R.string.rateus_dialog_title)
    }
    
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = {
                showFeedbackDialog = false
                onDismiss()
            },
            onFeedbackSubmitted = {
                ratingManager.markAsRated()
                onRated()
            }
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB8000000))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF16121A))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                        .clickable(onClick = {})
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Emoji display
                    Text(
                        text = emoji,
                        fontSize = 80.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Title
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    // Subtitle
                    Text(
                        text = stringResource(R.string.rateus_dialog_subtitle),
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Star rating
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (index < selectedRating) Color(0xFFFFC533) else Color(0xFFC8CBCC),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { selectedRating = index + 1 }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Rate button
                    Button(
                        onClick = {
                            when {
                                selectedRating == 0 -> {
                                    // No rating selected
                                    Toast.makeText(context, context.getString(R.string.please_select_rating), Toast.LENGTH_SHORT).show()
                                }
                                selectedRating in 1..3 -> {
                                    // Low rating - show feedback dialog
                                    showFeedbackDialog = true
                                }
                                selectedRating in 4..5 -> {
                                    // High rating - launch In-App Review or Play Store
                                    ratingManager.markAsRated()
                                    context.launchInAppReview(onComplete = {
                                        onRated()
                                        onDismiss()
                                    })
                                }
                            }
                        },
                        enabled = selectedRating > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRating > 0) Color(0xFFFF3F59) else Color(0xFF555555)
                        )
                    ) {
                        Text(
                            text = when {
                                selectedRating == 0 -> stringResource(R.string.select_rating)
                                selectedRating in 1..3 -> stringResource(R.string.send_feedback)
                                else -> stringResource(R.string.rate_on_play_store)
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Not now button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0))
                    ) {
                        Text(
                            stringResource(R.string.not_now),
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Feedback dialog for low ratings (1-3 stars).
 * Allows user to provide text feedback and sends via email.
 * Design matches app dark theme with close button and orange submit button.
 */
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onFeedbackSubmitted: () -> Unit
) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB8000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16121A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                    .clickable(onClick = {})
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.thanks_for_feedback),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    // Subtitle
                    Text(
                        text = stringResource(R.string.feedback_dialog_subtitle),
                        color = Color(0xFFB0B0B0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )
                    
                    // Feedback text field with dark theme
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = {
                            Text(
                                stringResource(R.string.feedback_hint),
                                color = Color(0xFF666666),
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1F1F1F),
                            unfocusedContainerColor = Color(0xFF1F1F1F),
                            focusedBorderColor = Color(0xFFFF6B35),
                            unfocusedBorderColor = Color(0xFF3A3A3A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Submit button - Orange color matching reference
                    Button(
                        onClick = {
                            if (feedbackText.isNotBlank()) {
                                context.sendFeedbackEmail(feedbackText)
                                onFeedbackSubmitted()
                                onDismiss()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.thanks_for_feedback),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.please_enter_feedback),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35), // Orange color
                            disabledContainerColor = Color(0xFF3A3A3A)
                        ),
                        enabled = feedbackText.isNotBlank()
                    ) {
                        Text(
                            stringResource(R.string.submit_feedback),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        )
                    }
                }
                
                // Close button (X) at top-right corner
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFB0B0B0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Launch In-App Review flow (Google Play).
 * Falls back to opening Play Store if In-App Review is not available.
 */
private fun Context.launchInAppReview(onComplete: () -> Unit) {
    try {
        val reviewManager = ReviewManagerFactory.create(this)
        val requestReviewFlow = reviewManager.requestReviewFlow()
        
        requestReviewFlow.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                // Launch the in-app review flow
                if (this is android.app.Activity) {
                    reviewManager.launchReviewFlow(this, reviewInfo).addOnCompleteListener {
                        Log.d(TAG, "In-App Review flow completed")
                        onComplete()
                    }
                } else {
                    // Fallback to Play Store if not an Activity context
                    openPlayStoreRating()
                    onComplete()
                }
            } else {
                // Fallback to Play Store if request fails
                Log.w(TAG, "In-App Review request failed, opening Play Store")
                openPlayStoreRating()
                onComplete()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error launching In-App Review", e)
        // Fallback to Play Store
        openPlayStoreRating()
        onComplete()
    }
}

/**
 * Open Play Store rating page for the app.
 */
private fun Context.openPlayStoreRating() {
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(marketIntent)
    } catch (e: ActivityNotFoundException) {
        // Play Store not installed, open in browser
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(webIntent)
    }
}

/**
 * Send feedback via email with app name and version in subject.
 */
private fun Context.sendFeedbackEmail(feedback: String) {
    try {
        val appName = applicationInfo.loadLabel(packageManager).toString()
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName
        
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@dramaverse.com")) // Replace with your email
            putExtra(Intent.EXTRA_SUBJECT, "[$appName] v$appVersion Feedback")
            putExtra(Intent.EXTRA_TEXT, feedback)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        Log.e(TAG, "Error sending feedback email", e)
        Toast.makeText(this, getString(R.string.error_sending_feedback), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Simple "Thanks for rating" dialog shown when user has already rated.
 */
@Composable
fun AlreadyRatedDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB8000000))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF16121A))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                    .clickable(onClick = {})
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success emoji
                Text(
                    text = "🎉",
                    fontSize = 80.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Title
                Text(
                    text = stringResource(R.string.thanks_for_rating),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // OK button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3F59))
                ) {
                    Text(
                        stringResource(R.string.ok),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
