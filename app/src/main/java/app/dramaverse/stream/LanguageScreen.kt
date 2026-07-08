package app.dramaverse.stream

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val showDoneText = selectedLanguage == null || !delayDoneAfterSelection

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616))
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlobeIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Language",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clickable {
                            val chosenLanguage = selectedLanguage ?: "English"
                            saveSelectedLanguage(context, chosenLanguage)
                            onContinue()
                        }
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\u2713",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    )
                    if (showDoneText) {
                        Spacer(modifier = Modifier.width(22.dp))
                        Text(
                            text = "DONE",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
            .height(64.dp)
            .background(Color(0xFF2D2D30), RoundedCornerShape(50))
            .then(
                if (selected) {
                    Modifier.border(0.6.dp, Color.White, RoundedCornerShape(50))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionRing(selected = selected)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SelectionRing(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .aspectRatio(1f)
            .border(
                width = if (selected) 3.4.dp else 3.dp,
                color = if (selected) Color.White else Color(0xFF48B6B8),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(23.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun GlobeIcon() {
    Canvas(modifier = Modifier.size(32.dp)) {
        val stroke = Stroke(width = 3.dp.toPx())
        drawCircle(Color.White, radius = size.minDimension / 2f - 1.dp.toPx(), style = stroke)
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.18f, size.height * 0.5f),
            end = Offset(size.width * 0.82f, size.height * 0.5f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.5f, size.height * 0.1f),
            end = Offset(size.width * 0.5f, size.height * 0.9f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawOval(
            color = Color.White,
            topLeft = Offset(size.width * 0.28f, 1.dp.toPx()),
            size = Size(size.width * 0.44f, size.height - 2.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

private fun saveSelectedLanguage(context: Context, language: String) {
    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SELECTED_LANGUAGE, language)
        .apply()
}
