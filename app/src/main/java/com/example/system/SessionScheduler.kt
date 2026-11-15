package com.example.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.db.AppSessionEntity
import com.example.receiver.AlarmReceiver

class SessionScheduler(private val context: Context) {

    fun scheduleSessionAlarms(session: AppSessionEntity, warningSeconds: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        val warningTriggerAt = session.endTimeMillis - (warningSeconds * 1000L)
        if (!session.warningShown && warningTriggerAt > now + 1000L) {
            val warningIntent = buildAlarmPendingIntent(
                sessionId = session.id,
                packageName = session.packageName,
                appName = session.appName,
                action = AlarmReceiver.ACTION_WARNING,
                requestCodeOffset = 100_000
            )
            scheduleExact(alarmManager, warningTriggerAt, warningIntent)
        }

        if (session.endTimeMillis > now) {
            val exitIntent = buildAlarmPendingIntent(
                sessionId = session.id,
                packageName = session.packageName,
                appName = session.appName,
                action = AlarmReceiver.ACTION_EXIT,
                requestCodeOffset = 200_000
            )
            scheduleExact(alarmManager, session.endTimeMillis, exitIntent)
        }
    }

    fun cancelSessionAlarms(sessionId: Long, packageName: String, appName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val warningIntent = buildAlarmPendingIntent(
            sessionId = sessionId,
            packageName = packageName,
            appName = appName,
            action = AlarmReceiver.ACTION_WARNING,
            requestCodeOffset = 100_000
        )
        val exitIntent = buildAlarmPendingIntent(
            sessionId = sessionId,
            packageName = packageName,
            appName = appName,
            action = AlarmReceiver.ACTION_EXIT,
            requestCodeOffset = 200_000
        )
        alarmManager.cancel(warningIntent)
        alarmManager.cancel(exitIntent)
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, operation: PendingIntent) {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    private fun buildAlarmPendingIntent(
        sessionId: Long,
        packageName: String,
        appName: String,
        action: String,
        requestCodeOffset: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_SESSION_ID, sessionId)
            putExtra(AlarmReceiver.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AlarmReceiver.EXTRA_APP_NAME, appName)
        }
        val reqCode = requestCodeOffset + (sessionId % 90_000).toInt()
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
