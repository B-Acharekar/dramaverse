package app.dramaverse.stream

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PREFS_NAME = "dramaverse_onboarding"
private const val KEY_SELECTED_LANGUAGE = "selected_language"

@Composable
fun LanguageScreen(
    delayDoneAfterSelection: Boolean,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val languages = remember {
        listOf(
            "English",
            "Ti\u1ebfng Vi\u1ec7t",
            "Espa\u00f1ol",
            "Fran\u00e7ais",
            "Deutsch",
            "Italiano",
            "Portugu\u00eas",
            "T\u00fcrk\u00e7e",
            "\u0627\u0644\u0639\u0631\u0628\u064a\u0629",
            "\u0939\u093f\u0928\u094d\u0926\u0940",
            "\ud55c\uad6d\uc5b4",
            "\u4e2d\u6587"
        )
    }
    val showActionButton = !delayDoneAfterSelection || selectedLanguage != null
    val showDoneText = !delayDoneAfterSelection

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        LanguageHeader(
            showActionButton = showActionButton,
            showDoneText = showDoneText,
            onActionClick = {
                val chosenLanguage = selectedLanguage ?: "English"
                saveSelectedLanguage(context, chosenLanguage)
                onContinue()
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.5.dp),
            contentPadding = PaddingValues(top = 7.5.dp, bottom = 7.5.dp),
            verticalArrangement = Arrangement.spacedBy(7.5.dp)
        ) {
            items(languages) { language ->
                LanguageItem(
                    name = language,
                    selected = selectedLanguage == language,
                    onClick = {
                        selectedLanguage = language
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageHeader(
    showActionButton: Boolean,
    showDoneText: Boolean,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .statusBarsPadding()
            .padding(start = 13.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Language",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showActionButton) {
            Row(
                modifier = Modifier.clickable(onClick = onActionClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2713",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                }
                if (showDoneText) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "DONE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    )
                }
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
            .padding(horizontal = 13.dp, vertical = 10.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionRing(selected = selected)
        Spacer(modifier = Modifier.width(5.dp))
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
                color = if (selected) Color.White else Color(0xFF4D7C3C),
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

private fun saveSelectedLanguage(context: Context, language: String) {
    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SELECTED_LANGUAGE, language)
        .apply()
}
