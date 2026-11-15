package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.ScreenTimeTrackerApp
import com.example.system.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START_OR_UPDATE = "com.example.action.START_OR_UPDATE_TIMER"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val placeholder = NotificationHelper.buildOngoingTimerNotification(
            context = this,
            appName = "স্ক্রিন টাইম ট্র্যাকার",
            purpose = "সেশন চলছে",
            remainingMillis = 0L,
            activeCount = 1
        )
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.FOREGROUND_NOTIFICATION_ID,
            placeholder,
            fgType
        )
        startTickerIfNeeded()
        return START_STICKY
    }

    private fun startTickerIfNeeded() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            val container = (application as ScreenTimeTrackerApp).container
            val sessionDao = container.database.appSessionDao()
            val coordinator = container.sessionCoordinator
            val settingsStore = container.settingsDataStore
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            while (isActive) {
                val activeList = sessionDao.getActiveSessionsSync()
                if (activeList.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val now = System.currentTimeMillis()
                val warningSeconds = settingsStore.getWarningSeconds()

                for (session in activeList) {
                    if (now >= session.endTimeMillis) {
                        coordinator.handleSessionExpired(session.id, shouldSendHome = true)
                    } else if (!session.warningShown &&
                        now >= session.endTimeMillis - warningSeconds * 1000L
                    ) {
                        coordinator.handleWarning(session.id)
                    }
                }

                val refreshed = sessionDao.getActiveSessionsSync()
                if (refreshed.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                val primary = refreshed.minByOrNull { it.endTimeMillis }!!
                val remaining = (primary.endTimeMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                val updatedNotification = NotificationHelper.buildOngoingTimerNotification(
                    context = this@TimerForegroundService,
                    appName = primary.appName,
                    purpose = primary.purpose,
                    remainingMillis = remaining,
                    activeCount = refreshed.size
                )
                nm.notify(NotificationHelper.FOREGROUND_NOTIFICATION_ID, updatedNotification)

                delay(1000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
