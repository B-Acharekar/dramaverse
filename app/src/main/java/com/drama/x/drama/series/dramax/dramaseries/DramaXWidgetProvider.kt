package com.drama.x.drama.series.dramax.dramaseries

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class DramaXWidgetProvider : AppWidgetProvider() {
    companion object {
        fun refresh(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, DramaXWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                DramaXWidgetProvider().onUpdate(context, appWidgetManager, widgetIds)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            refresh(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val showUninstall = WidgetUninstallGate.shouldDisplay(context)
            val layoutRes = if (showUninstall) {
                R.layout.widget_dramaverse_unlimited
            } else {
                R.layout.widget_dramaverse
            }
            val views = RemoteViews(context.packageName, layoutRes).apply {
                setOnClickPendingIntent(R.id.widgetHome, pendingIntent(context, MainActivity.ACTION_WIDGET_HOME, 1))
                if (showUninstall) {
                    setOnClickPendingIntent(R.id.widgetUninstall, pendingIntent(context, MainActivity.ACTION_WIDGET_UNINSTALL, 3))
                }
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
