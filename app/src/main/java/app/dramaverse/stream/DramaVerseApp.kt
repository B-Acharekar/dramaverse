package app.dramaverse.stream

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.onesignal.OneSignal

private const val TAG = "DramaVerse"
private const val RC_DELAY_DONE_LANGUAGE = "delay_button_done_language"
private const val RC_ONESIGNAL_APP_ID = "onesignal_app_id"

private enum class OnboardingStep {
    Splash,
    Language,
    Home
}

@Composable
fun DramaVerseApp() {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(OnboardingStep.Splash) }
    var delayDoneLanguage by remember { mutableStateOf(false) }
    var oneSignalInitializedAppId by remember { mutableStateOf<String?>(null) }
    val remoteConfig = remember(context) { configureRemoteConfig(context.applicationContext) }

    LaunchedEffect(remoteConfig) {
        if (remoteConfig == null) return@LaunchedEffect

        delayDoneLanguage = remoteConfig.getBoolean(RC_DELAY_DONE_LANGUAGE)
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            delayDoneLanguage = remoteConfig.getBoolean(RC_DELAY_DONE_LANGUAGE)
            val oneSignalAppId = remoteConfig.getString(RC_ONESIGNAL_APP_ID)
            if (oneSignalAppId.isNotBlank() && oneSignalInitializedAppId != oneSignalAppId) {
                runCatching {
                    OneSignal.initWithContext(context, oneSignalAppId)
                    oneSignalInitializedAppId = oneSignalAppId
                }.onFailure {
                    Log.w(TAG, "OneSignal initialization skipped.", it)
                }
            }
        }
    }

    when (currentStep) {
        OnboardingStep.Splash -> CustomSplashScreen(
            onFinished = {
                currentStep = OnboardingStep.Language
            }
        )

        OnboardingStep.Language -> {
            LanguageScreen(
                delayDoneAfterSelection = delayDoneLanguage,
                onContinue = {
                    currentStep = OnboardingStep.Home
                }
            )
            NotificationPermissionRequester()
        }

        OnboardingStep.Home -> HomeScreen()
    }
}

private fun configureRemoteConfig(context: Context): FirebaseRemoteConfig? {
    return runCatching {
        FirebaseApp.initializeApp(context)
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }.onFailure {
        Log.w(TAG, "Firebase Remote Config is using local defaults until Firebase config is added.", it)
    }.getOrNull()
}

@Composable
private fun NotificationPermissionRequester() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@LaunchedEffect
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val isGranted =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            permissionLauncher.launch(permission)
        }
    }
}

@Composable
private fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "DramaVerse",
            color = Color(0xFFFFA8AE),
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.sp
        )
    }
}
