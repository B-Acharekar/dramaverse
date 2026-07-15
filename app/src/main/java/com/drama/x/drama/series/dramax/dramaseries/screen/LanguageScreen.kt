package com.drama.x.drama.series.dramax.dramaseries.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drama.x.drama.series.dramax.dramaseries.R
import com.drama.x.drama.series.dramax.dramaseries.ads.ADS_TAG
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.ads.NativeAdState
import com.drama.x.drama.series.dramax.dramaseries.model.LanguageViewModel

@Composable
fun LanguageScreen(
    delayDoneAfterSelection: Boolean,
    onContinue: (String) -> Unit,
    viewModel: LanguageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val showActionButton = !delayDoneAfterSelection || uiState.selectedLanguage != null
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val firstVisit = remember { viewModel.isFirstLanguageVisit }
    var selectedOnce by remember { mutableStateOf(false) }
    var nativeAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.Idle) }

    LaunchedEffect(activity, firstVisit) {
        activity?.let { AdsManager.loadNativeLanguage(it, firstVisit) }
    }

    DisposableEffect(Unit) {
        val observer = Observer<NativeAdState> { state ->
            nativeAdState = state
            Log.d(ADS_TAG, "Language observer received native state=${state.javaClass.simpleName}")
        }
        AdsManager.nativeLanguageAdLive.observeForever(observer)
        onDispose { AdsManager.nativeLanguageAdLive.removeObserver(observer) }
    }

    DisposableEffect(selectedOnce) {
        if (!selectedOnce) return@DisposableEffect onDispose {}
        val observer = Observer<NativeAdState> { state ->
            if (state is NativeAdState.Loaded || state is NativeAdState.Loading) {
                nativeAdState = state
            }
            Log.d(ADS_TAG, "Language click observer received native state=${state.javaClass.simpleName}")
        }
        AdsManager.nativeLanguageClickAdLive.observeForever(observer)
        onDispose { AdsManager.nativeLanguageClickAdLive.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
            .statusBarsPadding()
    ) {
        LanguageHeader(
            showActionButton = showActionButton,
            onActionClick = {
                onContinue(viewModel.confirmLanguage())
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161616))
                .padding(top = 10.dp)
        ) {
            HorizontalDivider(
                color = Color.Gray.copy(alpha = 0.3f)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 11.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.languages) { language ->
                LanguageItem(
                    name = language,
                    selected = uiState.selectedLanguage == language,
                    onClick = {
                        viewModel.selectLanguage(language)
                        if (!selectedOnce) {
                            selectedOnce = true
                            activity?.let { AdsManager.loadNativeLanguageClick(it, firstVisit) }
                        }
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 11.dp)
                .padding(top = 8.dp, bottom = 10.dp)
        ) {
            ErainNativeAdHost(
                placementName = "language_native",
                state = nativeAdState,
                modifier = Modifier.fillMaxWidth(),
                height = 340.dp
            )
        }
    }
}

@Composable
private fun LanguageHeader(
    showActionButton: Boolean,
    onActionClick: () -> Unit
) {
    val bgBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF86011D),
            Color(0xFF140105)

        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 13.dp, end = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = stringResource(R.string.language_title),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showActionButton) {
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .background(brush = bgBrush, RoundedCornerShape(50))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.done),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF3A3A3C), RoundedCornerShape(50))
            .then(
                if (selected) {
                    Modifier.border(0.6.dp, Color.White, RoundedCornerShape(50))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionRing(selected = selected)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = flagForLanguage(name),
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SelectionRing(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .aspectRatio(1f)
            .border(
                width = 1.dp,
                color = if (selected) Color.White else Color(0xFF4DC7C3),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

private fun flagForLanguage(name: String): String = when (name) {
    "English" -> "\uD83C\uDDFA\uD83C\uDDF8"
    "Tiếng Việt" -> "\uD83C\uDDFB\uD83C\uDDF3"
    "Español" -> "\uD83C\uDDEA\uD83C\uDDF8"
    "Français" -> "\uD83C\uDDEB\uD83C\uDDF7"
    "Deutsch" -> "\uD83C\uDDE9\uD83C\uDDEA"
    "Italiano" -> "\uD83C\uDDEE\uD83C\uDDF9"
    "Português" -> "\uD83C\uDDE7\uD83C\uDDF7"
    "Türkçe" -> "\uD83C\uDDF9\uD83C\uDDF7"
    "العربية" -> "\uD83C\uDDF8\uD83C\uDDE6"
    "हिन्दी" -> "\uD83C\uDDEE\uD83C\uDDF3"
    "한국어" -> "\uD83C\uDDF0\uD83C\uDDF7"
    "日本語" -> "\uD83C\uDDEF\uD83C\uDDF5"
    "中文" -> "\uD83C\uDDE8\uD83C\uDDF3"
    else -> "\uD83C\uDF10"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
