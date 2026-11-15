package com.example.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.BengaliUtils

object NotificationHelper {
    const val CHANNEL_TIMER = "timer_ongoing_channel"
    const val CHANNEL_WARNING = "warning_alert_channel"
    const val CHANNEL_ENDED = "session_ended_channel"

    const val FOREGROUND_NOTIFICATION_ID = 1001
    private const val WARNING_NOTIFICATION_BASE_ID = 2000
    private const val ENDED_NOTIFICATION_BASE_ID = 3000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            context.getString(R.string.notification_channel_timer_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_timer_desc)
            setShowBadge(false)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val warningChannel = NotificationChannel(
            CHANNEL_WARNING,
            context.getString(R.string.notification_channel_warning_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_warning_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 350L, 180L, 350L)
            setSound(soundUri, audioAttrs)
        }

        val endedChannel = NotificationChannel(
            CHANNEL_ENDED,
            context.getString(R.string.notification_channel_ended_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_ended_desc)
            enableVibration(true)
        }

        nm.createNotificationChannels(listOf(timerChannel, warningChannel, endedChannel))
    }

    fun buildOngoingTimerNotification(
        context: Context,
        appName: String,
        purpose: String,
        remainingMillis: Long,
        activeCount: Int
    ): Notification {
        val countdownText = BengaliUtils.formatCountdownMmSs(remainingMillis)
        val title = if (activeCount > 1) {
            "$appName (+আরও ${BengaliUtils.formatInt(activeCount - 1)}টি)"
        } else {
            appName
        }
        val content = "বাকি আছে: $countdownText • উদ্দেশ্য: $purpose"

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun postWarningNotification(
        context: Context,
        sessionId: Long,
        appName: String,
        warningSeconds: Int
    ) {
        if (!PermissionChecker.isNotificationPermissionGranted(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (warningSeconds == 60) {
            "১ মিনিট বাকি আছে"
        } else {
            "${BengaliUtils.formatInt(warningSeconds)} সেকেন্ড বাকি আছে"
        }
        val body = "⚠️ $appName থেকে বের হওয়ার প্রস্তুতি নিন।"

        val notification = NotificationCompat.Builder(context, CHANNEL_WARNING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        nm.notify(WARNING_NOTIFICATION_BASE_ID + (sessionId % 900).toInt(), notification)
    }

    fun postSessionEndedNotification(context: Context, sessionId: Long, appName: String) {
        if (!PermissionChecker.isNotificationPermissionGranted(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = "সময় শেষ। $appName এখন নিষ্ক্রিয়।"

        val notification = NotificationCompat.Builder(context, CHANNEL_ENDED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("সময় শেষ")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        nm.notify(ENDED_NOTIFICATION_BASE_ID + (sessionId % 900).toInt(), notification)
    }
}
