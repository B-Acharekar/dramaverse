package app.dramaverse.stream

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.dramaverse.stream.data.DramaNotificationScheduler
import app.dramaverse.stream.data.LocaleHelper
import app.dramaverse.stream.ui.theme.DramaVerseTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        useDefaultSystemBars()
        DramaNotificationScheduler.ensureChannel(this)

        setContent {
            DramaVerseTheme {
                DramaVerseApp()
            }
        }
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
