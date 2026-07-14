package com.drama.x.drama.series.dramax.dramaseries

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class DramaXWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_dramaverse).apply {
                setOnClickPendingIntent(R.id.widgetHome, pendingIntent(context, MainActivity.ACTION_WIDGET_HOME, 1))
                setOnClickPendingIntent(R.id.widgetDownloads, pendingIntent(context, MainActivity.ACTION_WIDGET_DOWNLOADS, 2))
                setOnClickPendingIntent(R.id.widgetUninstall, pendingIntent(context, MainActivity.ACTION_WIDGET_UNINSTALL, 3))
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
