package com.drama.x.drama.series.dramax.dramaseries

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.drama.x.drama.series.dramax.dramaseries.ads.AdsManager
import com.drama.x.drama.series.dramax.dramaseries.data.DramaNotificationScheduler
import com.drama.x.drama.series.dramax.dramaseries.data.LocaleHelper
import com.drama.x.drama.series.dramax.dramaseries.model.AppViewModel
import com.drama.x.drama.series.dramax.dramaseries.ui.theme.DramaXTheme

class MainActivity : AppCompatActivity() {
    private lateinit var appViewModel: AppViewModel
    
    companion object {
        const val ACTION_WIDGET_HOME = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_HOME"
        const val ACTION_WIDGET_MY_LIST = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_MY_LIST"
        const val ACTION_WIDGET_UNINSTALL = "com.drama.x.drama.series.dramax.dramaseries.action.WIDGET_UNINSTALL"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideNavigationBar()
        DramaNotificationScheduler.ensureChannel(this)
        val initialAction = intent?.action
        intent?.action = null

        // Initialize ViewModel
        appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
        
        // Set up back button handling
        setupBackButtonHandling()

        setContent {
            DramaXTheme {
                DramaXApp(
                    initialAction = initialAction,
                    viewModel = appViewModel
                )
            }
        }
    }
    
    private fun setupBackButtonHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Try to handle back navigation within the app
                val handled = appViewModel.handleBackNavigation()
                if (!handled) {
                    // If not handled by app navigation, exit the app
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
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
    private fun hideNavigationBar() {
        // Keep the notification/status bar visible; screens are designed below system bars.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
