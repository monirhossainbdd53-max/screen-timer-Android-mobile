package com.example.system

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.db.AppDatabase
import com.example.data.db.AppSessionEntity
import com.example.data.db.ManagedAppEntity
import com.example.data.db.ManagedAppStatus
import com.example.data.db.SessionStatus
import com.example.data.prefs.SettingsDataStore
import com.example.service.AppMonitorAccessibilityService
import com.example.service.OverlayManager
import com.example.service.TimerForegroundService
import com.example.util.BengaliUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class SessionCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val scheduler: SessionScheduler,
    val overlayManager: OverlayManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val managedAppDao = database.managedAppDao()
    private val sessionDao = database.appSessionDao()

    private val managedCache = ConcurrentHashMap<String, ManagedAppEntity>()
    private val openSessionByPackageCache = ConcurrentHashMap<String, AppSessionEntity>()

    init {
        scope.launch {
            managedAppDao.observeManagedApps().collectLatest { list ->
                val map = list.associateBy { it.packageName }
                managedCache.clear()
                managedCache.putAll(map)
            }
        }
        scope.launch {
            sessionDao.observeOpenSessions().collectLatest { list ->
                val map = list.associateBy { it.packageName }
                openSessionByPackageCache.clear()
                openSessionByPackageCache.putAll(map)
            }
        }
    }

    fun getManagedAppCached(packageName: String): ManagedAppEntity? = managedCache[packageName]

    fun getOpenSessionCached(packageName: String): AppSessionEntity? =
        openSessionByPackageCache[packageName]

    suspend fun refreshCachesBlocking() {
        val managed = managedAppDao.getManagedAppsSync()
        managedCache.clear()
        managed.forEach { managedCache[it.packageName] = it }

        val open = sessionDao.getOpenSessionsSync()
        openSessionByPackageCache.clear()
        open.forEach { openSessionByPackageCache[it.packageName] = it }
    }

    suspend fun toggleManagedApp(packageName: String, appName: String, enabled: Boolean) {
        mutex.withLock {
            if (enabled) {
                val existing = managedAppDao.getByPackage(packageName)
                val entity = existing?.copy(
                    appName = appName,
                    isManaged = true,
                    updatedAtMillis = System.currentTimeMillis()
                ) ?: ManagedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isManaged = true,
                    status = ManagedAppStatus.DISABLED
                )
                managedAppDao.upsert(entity)
                managedCache[packageName] = entity
            } else {
                val open = sessionDao.getOpenSessionForPackage(packageName)
                if (open != null) {
                    val now = System.currentTimeMillis()
                    closeSessionLocked(
                        session = open,
                        newStatus = SessionStatus.STOPPED_BY_USER,
                        closedAtMillis = now,
                        remainingMillis = 0L,
                        usedMillis = open.currentUsedMillis(now)
                    )
                }
                managedAppDao.deleteByPackage(packageName)
                managedCache.remove(packageName)
                ensureForegroundServiceStateLocked()
            }
        }
    }

    suspend fun startSession(
        packageName: String,
        appName: String,
        purpose: String,
        minutes: Int
    ) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val totalMillis = minutes * 60_000L
            val end = now + totalMillis

            sessionDao.getOpenSessionForPackage(packageName)?.let { stale ->
                closeSessionLocked(
                    session = stale,
                    newStatus = SessionStatus.STOPPED_BY_USER,
                    closedAtMillis = now,
                    remainingMillis = 0L,
                    usedMillis = stale.currentUsedMillis(now)
                )
            }

            val insertedId = sessionDao.insert(
                AppSessionEntity(
                    packageName = packageName,
                    appName = appName,
                    purpose = purpose,
                    plannedMinutes = minutes,
                    startTimeMillis = now,
                    lastResumedAtMillis = now,
                    endTimeMillis = end,
                    remainingMillis = totalMillis,
                    usedMillis = 0L,
                    status = SessionStatus.ACTIVE
                )
            )
            val createdSession = AppSessionEntity(
                id = insertedId,
                packageName = packageName,
                appName = appName,
                purpose = purpose,
                plannedMinutes = minutes,
                startTimeMillis = now,
                lastResumedAtMillis = now,
                endTimeMillis = end,
                remainingMillis = totalMillis,
                usedMillis = 0L,
                status = SessionStatus.ACTIVE
            )

            val updatedApp = ManagedAppEntity(
                packageName = packageName,
                appName = appName,
                isManaged = true,
                status = ManagedAppStatus.ACTIVE,
                activeSessionId = insertedId,
                updatedAtMillis = now
            )
            managedAppDao.upsert(updatedApp)
            managedCache[packageName] = updatedApp
            openSessionByPackageCache[packageName] = createdSession

            val warningSeconds = settingsDataStore.getWarningSeconds()
            scheduler.scheduleSessionAlarms(createdSession, warningSeconds)
            startTimerForegroundService()
        }
    }

    suspend fun resumePausedSession(packageName: String) {
        var triggerImmediateWarningId: Long? = null
        mutex.withLock {
            val current = sessionDao.getOpenSessionForPackage(packageName) ?: return@withLock
            if (current.status != SessionStatus.PAUSED) return@withLock

            val now = System.currentTimeMillis()
            val remaining = current.remainingMillis.coerceAtLeast(0L)
            if (remaining <= 0L) {
                closeSessionLocked(
                    session = current,
                    newStatus = SessionStatus.EXPIRED,
                    closedAtMillis = now,
                    remainingMillis = 0L,
                    usedMillis = current.plannedMinutes * 60_000L
                )
                ensureForegroundServiceStateLocked()
                return@withLock
            }

            val newEndTime = now + remaining
            val resumed = current.copy(
                status = SessionStatus.ACTIVE,
                lastResumedAtMillis = now,
                endTimeMillis = newEndTime
            )
            sessionDao.update(resumed)

            managedAppDao.updateStatus(
                packageName = packageName,
                status = ManagedAppStatus.ACTIVE,
                sessionId = resumed.id,
                updatedAt = now
            )
            managedCache[packageName]?.let { cached ->
                managedCache[packageName] = cached.copy(
                    status = ManagedAppStatus.ACTIVE,
                    activeSessionId = resumed.id,
                    updatedAtMillis = now
                )
            }
            openSessionByPackageCache[packageName] = resumed

            val warningSeconds = settingsDataStore.getWarningSeconds()
            if (!resumed.warningShown && remaining <= warningSeconds * 1000L) {
                triggerImmediateWarningId = resumed.id
            }
            scheduler.scheduleSessionAlarms(resumed, warningSeconds)
            startTimerForegroundService()
        }

        triggerImmediateWarningId?.let { handleWarning(it) }
    }

    suspend fun pauseSessionForPackage(packageName: String, leftAtMillis: Long) {
        var expiredSession: AppSessionEntity? = null
        mutex.withLock {
            val current = sessionDao.getOpenSessionForPackage(packageName) ?: return@withLock
            if (current.status != SessionStatus.ACTIVE) return@withLock

            scheduler.cancelSessionAlarms(current.id, current.packageName, current.appName)

            val totalPlannedMillis = current.plannedMinutes * 60_000L
            val activeElapsed = (leftAtMillis - current.lastResumedAtMillis).coerceAtLeast(0L)
            val newUsed = (current.usedMillis + activeElapsed).coerceIn(0L, totalPlannedMillis)
            val newRemaining = (totalPlannedMillis - newUsed).coerceAtLeast(0L)

            if (newRemaining < 1000L) {
                closeSessionLocked(
                    session = current,
                    newStatus = SessionStatus.EXPIRED,
                    closedAtMillis = leftAtMillis,
                    remainingMillis = 0L,
                    usedMillis = totalPlannedMillis
                )
                expiredSession = current
            } else {
                val paused = current.copy(
                    status = SessionStatus.PAUSED,
                    remainingMillis = newRemaining,
                    usedMillis = newUsed,
                    endTimeMillis = leftAtMillis + newRemaining
                )
                sessionDao.update(paused)
                managedAppDao.updateStatus(
                    packageName = packageName,
                    status = ManagedAppStatus.PAUSED,
                    sessionId = paused.id,
                    updatedAt = leftAtMillis
                )
                managedCache[packageName]?.let { cached ->
                    managedCache[packageName] = cached.copy(
                        status = ManagedAppStatus.PAUSED,
                        activeSessionId = paused.id,
                        updatedAtMillis = leftAtMillis
                    )
                }
                openSessionByPackageCache[packageName] = paused
            }

            ensureForegroundServiceStateLocked()
        }

        expiredSession?.let {
            val msg = "সময় শেষ। ${it.appName} এখন নিষ্ক্রিয়।"
            NotificationHelper.postSessionEndedNotification(context, it.id, it.appName)
            overlayManager.showBannerOverlay(msg, isWarning = false, durationMillis = 4000L)
        }
    }

    suspend fun handleWarning(sessionId: Long) {
        val session = mutex.withLock {
            val current = sessionDao.getById(sessionId) ?: return@withLock null
            if (current.status != SessionStatus.ACTIVE || current.warningShown) {
                return@withLock null
            }
            sessionDao.markWarningShown(sessionId)
            val updated = current.copy(warningShown = true)
            openSessionByPackageCache[updated.packageName] = updated
            updated
        } ?: return

        val warningSeconds = settingsDataStore.getWarningSeconds()
        NotificationHelper.postWarningNotification(context, session.id, session.appName, warningSeconds)
        val bannerText = if (warningSeconds == 60) {
            "⚠️ আর ১ মিনিট বাকি। ${session.appName} থেকে বের হওয়ার প্রস্তুতি নিন।"
        } else {
            "⚠️ আর ${BengaliUtils.formatInt(warningSeconds)} সেকেন্ড বাকি। ${session.appName} থেকে বের হওয়ার প্রস্তুতি নিন।"
        }
        overlayManager.showBannerOverlay(bannerText, isWarning = true, durationMillis = 5000L)
    }

    suspend fun handleSessionExpired(sessionId: Long, shouldSendHome: Boolean = true) {
        val expiredSession = mutex.withLock {
            val current = sessionDao.getById(sessionId) ?: return@withLock null
            if (current.status != SessionStatus.ACTIVE && current.status != SessionStatus.PAUSED) {
                return@withLock null
            }
            val now = System.currentTimeMillis()
            val totalPlanned = current.plannedMinutes * 60_000L
            closeSessionLocked(
                session = current,
                newStatus = SessionStatus.EXPIRED,
                closedAtMillis = now,
                remainingMillis = 0L,
                usedMillis = totalPlanned
            )
            ensureForegroundServiceStateLocked()
            current
        } ?: return

        overlayManager.dismissEntryPopup()
        if (shouldSendHome) {
            AppMonitorAccessibilityService.instance?.performHomeExit()
        }
        val msg = "সময় শেষ। ${expiredSession.appName} এখন নিষ্ক্রিয়।"
        NotificationHelper.postSessionEndedNotification(context, expiredSession.id, expiredSession.appName)
        overlayManager.showBannerOverlay(msg, isWarning = false, durationMillis = 4000L)
    }

    suspend fun stopSessionById(sessionId: Long) {
        mutex.withLock {
            val open = sessionDao.getById(sessionId) ?: return@withLock
            if (open.status == SessionStatus.ACTIVE || open.status == SessionStatus.PAUSED) {
                val now = System.currentTimeMillis()
                closeSessionLocked(
                    session = open,
                    newStatus = SessionStatus.STOPPED_BY_USER,
                    closedAtMillis = now,
                    remainingMillis = 0L,
                    usedMillis = open.currentUsedMillis(now)
                )
                ensureForegroundServiceStateLocked()
            }
        }
    }

    suspend fun stopAllActiveSessions() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val openList = sessionDao.getOpenSessionsSync()
            openList.forEach { session ->
                scheduler.cancelSessionAlarms(session.id, session.packageName, session.appName)
                sessionDao.closeSession(
                    sessionId = session.id,
                    status = SessionStatus.STOPPED_BY_USER,
                    remainingMillis = 0L,
                    usedMillis = session.currentUsedMillis(now),
                    actualEndMillis = now
                )
            }
            managedAppDao.disableAll(now)
            openSessionByPackageCache.clear()
            managedCache.keys.forEach { key ->
                managedCache[key]?.let {
                    managedCache[key] = it.copy(
                        status = ManagedAppStatus.DISABLED,
                        activeSessionId = null,
                        updatedAtMillis = now
                    )
                }
            }
            stopTimerForegroundService()
        }
    }

    suspend fun clearHistory() {
        sessionDao.clearCompletedHistory()
    }

    suspend fun restoreSessionsOnBootOrRestart() {
        mutex.withLock {
            refreshCachesBlocking()
            val now = System.currentTimeMillis()
            val warningSeconds = settingsDataStore.getWarningSeconds()
            val activeList = sessionDao.getActiveSessionsSync()

            for (session in activeList) {
                if (session.endTimeMillis <= now) {
                    closeSessionLocked(
                        session = session,
                        newStatus = SessionStatus.EXPIRED,
                        closedAtMillis = now,
                        remainingMillis = 0L,
                        usedMillis = session.plannedMinutes * 60_000L
                    )
                } else {
                    scheduler.scheduleSessionAlarms(session, warningSeconds)
                }
            }
            ensureForegroundServiceStateLocked()
        }
    }

    private suspend fun closeSessionLocked(
        session: AppSessionEntity,
        newStatus: String,
        closedAtMillis: Long,
        remainingMillis: Long,
        usedMillis: Long
    ) {
        scheduler.cancelSessionAlarms(session.id, session.packageName, session.appName)
        sessionDao.closeSession(
            sessionId = session.id,
            status = newStatus,
            remainingMillis = remainingMillis,
            usedMillis = usedMillis,
            actualEndMillis = closedAtMillis
        )
        managedAppDao.updateStatus(
            packageName = session.packageName,
            status = ManagedAppStatus.DISABLED,
            sessionId = null,
            updatedAt = closedAtMillis
        )
        openSessionByPackageCache.remove(session.packageName)
        managedCache[session.packageName]?.let { cached ->
            managedCache[session.packageName] = cached.copy(
                status = ManagedAppStatus.DISABLED,
                activeSessionId = null,
                updatedAtMillis = closedAtMillis
            )
        }
    }

    private suspend fun ensureForegroundServiceStateLocked() {
        val remainingActive = sessionDao.getActiveSessionsSync()
        if (remainingActive.isEmpty()) {
            stopTimerForegroundService()
        } else {
            startTimerForegroundService()
        }
    }

    private fun startTimerForegroundService() {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START_OR_UPDATE
        }
        runCatching {
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private fun stopTimerForegroundService() {
        val intent = Intent(context, TimerForegroundService::class.java)
        runCatching {
            context.stopService(intent)
        }
    }
}
