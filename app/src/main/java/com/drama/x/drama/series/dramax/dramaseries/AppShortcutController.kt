package com.drama.x.drama.series.dramax.dramaseries

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

object AppShortcutController {
    private const val UNINSTALL_SHORTCUT_ID = "shortcut_uninstall"

    fun syncUninstallShortcut(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val shortcutManager = appContext.getSystemService(ShortcutManager::class.java) ?: return
        if (enabled) {
            shortcutManager.enableShortcuts(listOf(UNINSTALL_SHORTCUT_ID))
            shortcutManager.addDynamicShortcuts(listOf(appContext.uninstallShortcut()))
        } else {
            shortcutManager.removeDynamicShortcuts(listOf(UNINSTALL_SHORTCUT_ID))
            shortcutManager.disableShortcuts(listOf(UNINSTALL_SHORTCUT_ID))
        }
    }

    private fun Context.uninstallShortcut(): ShortcutInfo =
        ShortcutInfo.Builder(this, UNINSTALL_SHORTCUT_ID)
            .setShortLabel(getString(R.string.widget_uninstall))
            .setLongLabel(getString(R.string.widget_uninstall))
            .setDisabledMessage(getString(R.string.widget_uninstall))
            .setIcon(Icon.createWithResource(this, R.drawable.widget_delete))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_WIDGET_UNINSTALL
                }
            )
            .build()
}
