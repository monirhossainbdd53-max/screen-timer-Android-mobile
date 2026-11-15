package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.ScreenTimeTrackerApp
import com.example.data.db.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppMonitorAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AppMonitorAccessibilityService? = null
            private set

        private val transientIgnorePackages = setOf(
            "com.android.systemui",
            "android",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        )
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentActiveManagedPackage: String? = null

    @Volatile
    private var latestForegroundPackage: String? = null

    private var pauseJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val coordinator = (application as ScreenTimeTrackerApp).container.sessionCoordinator
        serviceScope.launch {
            coordinator.restoreSessionsOnBootOrRestart()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Privacy guarantee: only inspect the foreground package name, never read window text or content.
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return

        if (isTransientSystemOrIme(pkg)) return

        val container = (application as ScreenTimeTrackerApp).container
        val coordinator = container.sessionCoordinator
        val overlayManager = coordinator.overlayManager

        if (pkg == packageName && overlayManager.isEntryPopupShowing()) {
            return
        }

        latestForegroundPackage = pkg

        val managedApp = coordinator.getManagedAppCached(pkg)
        if (managedApp != null && managedApp.isManaged) {
            pauseJob?.cancel()
            pauseJob = null

            val openSession = coordinator.getOpenSessionCached(pkg)
            val now = System.currentTimeMillis()
            when {
                openSession != null && openSession.status == SessionStatus.ACTIVE -> {
                    if (openSession.endTimeMillis <= now) {
                        currentActiveManagedPackage = null
                        performHomeExit()
                        serviceScope.launch {
                            coordinator.handleSessionExpired(openSession.id, shouldSendHome = true)
                        }
                    } else {
                        currentActiveManagedPackage = pkg
                    }
                }
                openSession != null && openSession.status == SessionStatus.PAUSED -> {
                    if (openSession.remainingMillis <= 0L) {
                        currentActiveManagedPackage = null
                        performHomeExit()
                        serviceScope.launch {
                            coordinator.handleSessionExpired(openSession.id, shouldSendHome = true)
                        }
                    } else {
                        currentActiveManagedPackage = pkg
                        serviceScope.launch {
                            coordinator.resumePausedSession(pkg)
                        }
                    }
                }
                else -> {
                    currentActiveManagedPackage = null
                    overlayManager.showEntryPopup(
                        packageName = pkg,
                        appName = managedApp.appName,
                        onConfirm = { purpose, minutes ->
                            serviceScope.launch {
                                coordinator.startSession(pkg, managedApp.appName, purpose, minutes)
                                currentActiveManagedPackage = pkg
                            }
                        },
                        onCancelToHome = {
                            performHomeExit()
                        }
                    )
                }
            }
        } else {
            if (overlayManager.isEntryPopupShowing()) {
                overlayManager.dismissEntryPopup()
            }
            val prevManaged = currentActiveManagedPackage
            if (prevManaged != null && pkg != prevManaged) {
                val leftAtMillis = System.currentTimeMillis()
                pauseJob?.cancel()
                pauseJob = serviceScope.launch {
                    delay(1500L)
                    if (latestForegroundPackage != prevManaged) {
                        currentActiveManagedPackage = null
                        coordinator.pauseSessionForPackage(prevManaged, leftAtMillis)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        // Required override; no-op
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        pauseJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun performHomeExit() {
        mainHandler.post {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun isTransientSystemOrIme(pkg: String): Boolean {
        if (pkg in transientIgnorePackages) return true
        val lower = pkg.lowercase()
        if (lower.contains("inputmethod") ||
            lower.contains("keyboard") ||
            lower.contains("gboard") ||
            lower.contains("ridmik") ||
            lower.contains("swiftkey")
        ) {
            return true
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val isIme = imm?.enabledInputMethodList?.any { it.packageName == pkg } == true
        return isIme
    }
}
