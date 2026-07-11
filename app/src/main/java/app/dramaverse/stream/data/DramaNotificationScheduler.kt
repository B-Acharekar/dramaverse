package app.dramaverse.stream.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.dramaverse.stream.MainActivity
import app.dramaverse.stream.R
import java.time.OffsetDateTime

private const val CHANNEL_ID = "dramaverse_planner"
private const val EXTRA_TITLE = "title"
private const val EXTRA_BODY = "body"

class DramaReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DramaNotificationScheduler.ensureChannel(context)
        if (!DramaNotificationScheduler.canShowNotifications(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_2)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE) ?: "Drama reminder")
            .setContentText(intent.getStringExtra(EXTRA_BODY) ?: "Your planned drama starts soon.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(intent.getStringExtra(EXTRA_BODY)))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}

object DramaNotificationScheduler {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Drama planner",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for planned dramas and reward updates."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canShowNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun schedulePlannerReminder(
        context: Context,
        plannerItem: PlannerItem
    ) {
        val triggerAt = runCatching {
            OffsetDateTime.parse(plannerItem.scheduledAt)
                .minusMinutes(plannerItem.remindBeforeMinutes.toLong())
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(System.currentTimeMillis() + 60_000)
            .coerceAtLeast(System.currentTimeMillis() + 5_000)

        val body = buildString {
            append(plannerItem.title)
            plannerItem.episode?.let { append(" Episode ").append(it) }
            append(" is on your watchlist.")
        }
        val intent = Intent(context, DramaReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, "Drama Planner")
            putExtra(EXTRA_BODY, body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plannerItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact alarms avoid special exact-alarm policy while still surfacing reminders reliably.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
