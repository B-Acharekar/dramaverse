package com.drama.x.drama.series.dramax.dramaseries.screen

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.ADS_TAG
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.NativeAdState

private val UninstallBackground = Color(0xFF111113)
private val UninstallText = Color(0xFFF5EEF1)
private val UninstallMuted = Color(0xFFC1A4A9)
private val UninstallAccent = Color(0xFFFFA4AD)
private val DisabledButton = Color(0xFF2B2024)
private val ButtonBrush = Brush.horizontalGradient(listOf(Color(0xFF86011D), Color(0xFF140105)))

@Composable
fun ConfirmUninstallScreen(
    onBackHome: () -> Unit,
    onStillUninstall: () -> Unit
) {
    BackHandler(onBack = onBackHome)
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var nativeAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Idle) }

    LaunchedEffect(activity) {
        activity?.let { AdsManager.loadNativeUninstall(it) }
    }
    DisposableEffect(Unit) {
        val observer = Observer<NativeAdState> { state ->
            nativeAdState = state
            Log.d(ADS_TAG, "UNINSTALL_NATIVE_CONFIRM observer state=${state.javaClass.simpleName}")
        }
        AdsManager.nativeUninstallAdLive.observeForever(observer)
        onDispose { AdsManager.nativeUninstallAdLive.removeObserver(observer) }
    }

    UninstallBaseScreen(nativeAdState = nativeAdState, onBackHome = onBackHome) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Your watch history and favorite short dramas\nare still waiting for you",
            color = UninstallText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Keep DramaX installed to continue stories,\nrewards, and new episodes anytime.",
            color = UninstallMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(46.dp))
        RoundedActionButton(
            text = "Try Again",
            background = ButtonBrush,
            textColor = Color.White,
            onClick = onBackHome
        )
        Spacer(modifier = Modifier.height(10.dp))
        RoundedActionButton(
            text = "Still want to uninstall",
            background = DisabledButton,
            textColor = UninstallMuted,
            onClick = onStillUninstall
        )
        Spacer(modifier = Modifier.weight(1.2f))
    }
}

@Composable
fun SurveyUninstallScreen(onBackHome: () -> Unit) {
    BackHandler(onBack = onBackHome)
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var selectedReason by remember { mutableStateOf("Too many ads") }
    var nativeAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Idle) }
    val reasons = listOf(
        "Feature does not working",
        "Too many ads",
        "I don't need it",
        "Other"
    )

    LaunchedEffect(activity) {
        activity?.let { AdsManager.loadNativeSurveyUninstall(it) }
    }
    DisposableEffect(Unit) {
        val observer = Observer<NativeAdState> { state ->
            nativeAdState = state
            Log.d(ADS_TAG, "UNINSTALL_NATIVE_SURVEY observer state=${state.javaClass.simpleName}")
        }
        AdsManager.nativeSurveyUninstallAdLive.observeForever(observer)
        onDispose { AdsManager.nativeSurveyUninstallAdLive.removeObserver(observer) }
    }

    UninstallBaseScreen(nativeAdState = nativeAdState, onBackHome = onBackHome) {
        Spacer(modifier = Modifier.height(24.dp))
        AppIconPill()
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "Issues encountered during usage",
            color = UninstallText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(26.dp))
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            reasons.forEach { reason ->
                SurveyReasonRow(
                    text = reason,
                    selected = selectedReason == reason,
                    onClick = { selectedReason = reason }
                )
            }
        }
        Spacer(modifier = Modifier.height(42.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundedActionButton(
                text = "Cancel",
                background = DisabledButton,
                textColor = UninstallMuted,
                onClick = onBackHome,
                modifier = Modifier.weight(1f)
            )
            RoundedActionButton(
                text = "Uninstall",
                background = ButtonBrush,
                textColor = Color.White,
                onClick = {
                    Log.d(ADS_TAG, "UNINSTALL_SURVEY_REASON selected=$selectedReason")
                    context.openSystemUninstall()
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun UninstallBaseScreen(
    nativeAdState: NativeAdState,
    onBackHome: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UninstallBackground)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BackCircle(onClick = onBackHome)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
            LargeNativeAd(state = nativeAdState, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BackCircle(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFF211417), CircleShape)
            .border(0.7.dp, Color(0xFF513039), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SurveyReasonRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = UninstallText,
            fontSize = 13.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.4.dp, if (selected) UninstallAccent else Color(0xFF6B555B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(UninstallAccent, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun RoundedActionButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        letterSpacing = 0.sp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(background, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}

@Composable
private fun RoundedActionButton(
    text: String,
    background: Brush,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        letterSpacing = 0.sp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(background, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}

@Composable
private fun AppIconPill() {
    Row(
        modifier = Modifier
            .background(Color(0xFF211417), RoundedCornerShape(28.dp))
            .border(0.7.dp, Color(0xFF513039), RoundedCornerShape(28.dp))
            .padding(start = 10.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF2B2024), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.icon_2),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = "DramaX",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun LargeNativeAd(
    state: NativeAdState,
    modifier: Modifier = Modifier
) {
    ErainNativeAdHost(
        placementName = "uninstall_native",
        state = state,
        modifier = modifier,
        height = 320.dp
    )
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openSystemUninstall() {
    val uninstallUri = Uri.parse("package:$packageName")
    val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, uninstallUri).apply {
        putExtra(Intent.EXTRA_RETURN_RESULT, false)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uninstallUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        Log.d(ADS_TAG, "UNINSTALL_SYSTEM_INTENT action=${uninstallIntent.action} package=$packageName")
        startActivity(uninstallIntent)
    }.recoverCatching {
        Log.w(ADS_TAG, "UNINSTALL_SYSTEM_INTENT failed; opening app details fallback", it)
        startActivity(fallbackIntent)
    }.onFailure {
        Log.e(ADS_TAG, "UNINSTALL_SYSTEM_INTENT fallback failed", it)
        Toast.makeText(this, "Unable to open uninstall screen", Toast.LENGTH_SHORT).show()
    }
}
