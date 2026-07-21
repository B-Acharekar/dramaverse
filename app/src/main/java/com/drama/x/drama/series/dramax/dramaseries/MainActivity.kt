package com.drama.x.drama.series.dramax.dramaseries

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        const val ACTION_WIDGET_UNINSTALL = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_UNINSTALL"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
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

    override fun onDestroy() {
        val suppressNativeClear = AdsManager.consumeSuppressNativeClearOnDestroy()
        val preserveNativeAds = isChangingConfigurations || suppressNativeClear
        if (!preserveNativeAds) {
            AdsManager.clearLanguageAds()
        }
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
