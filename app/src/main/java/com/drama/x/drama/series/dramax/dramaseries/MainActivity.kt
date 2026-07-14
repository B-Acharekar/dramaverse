package com.drama.x.drama.series.dramax.dramaseries

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.data.DramaNotificationScheduler
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
import com.drama.x.drama.series.dramax.dramaseries.ui.theme.DramaXTheme

class MainActivity : AppCompatActivity() {
    companion object {
        const val ACTION_WIDGET_HOME = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_HOME"
        const val ACTION_WIDGET_DOWNLOADS = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_DOWNLOADS"
        const val ACTION_WIDGET_UNINSTALL = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_UNINSTALL"
    }

    private var hasResumedOnce = false
    private var returnedFromBackground = false
    private var backgroundedAtMs = 0L


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        useDefaultSystemBars()
        DramaNotificationScheduler.ensureChannel(this)

        setContent {
            DramaXTheme {
                DramaXApp(initialAction = intent?.action)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        val suppressResumeAd = AdsManager.consumeSuppressNextResumeInterstitial()
        val backgroundElapsedMs =
            if (backgroundedAtMs > 0L) SystemClock.elapsedRealtime() - backgroundedAtMs else 0L
        val eligibleBackgroundReturn = returnedFromBackground || backgroundElapsedMs >= 700L
        Log.d(
            "DramaXAds",
            "MAIN_ON_RESUME hasResumedOnce=$hasResumedOnce returnedFromBackground=$returnedFromBackground " +
                "backgroundElapsedMs=$backgroundElapsedMs suppressResumeAd=$suppressResumeAd"
        )
        if (hasResumedOnce && eligibleBackgroundReturn && !suppressResumeAd) {
            AdsManager.showWelcomeBackInterstitial(this)
        }
        hasResumedOnce = true
        returnedFromBackground = false
        backgroundedAtMs = 0L
    }

    override fun onPause() {
        backgroundedAtMs = SystemClock.elapsedRealtime()
        Log.d("DramaXAds", "MAIN_ON_PAUSE backgroundedAtMs=$backgroundedAtMs")
        super.onPause()
    }

    override fun onStop() {
        Log.d("DramaXAds", "MAIN_ON_STOP markReturnedFromBackground=true")
        returnedFromBackground = true
        super.onStop()
    }

    override fun onDestroy() {
        AdsManager.clearLanguageAds()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun useDefaultSystemBars() {
        // Keep the notification/status bar visible; screens are designed below system bars.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}
