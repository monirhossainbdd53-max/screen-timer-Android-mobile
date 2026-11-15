package com.example

import android.app.Application
import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.prefs.SettingsDataStore
import com.example.service.OverlayManager
import com.example.system.InstalledAppsProvider
import com.example.system.NotificationHelper
import com.example.system.SessionCoordinator
import com.example.system.SessionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
    val installedAppsProvider: InstalledAppsProvider = InstalledAppsProvider(context)
    val sessionScheduler: SessionScheduler = SessionScheduler(context)
    val overlayManager: OverlayManager = OverlayManager(context)
    val sessionCoordinator: SessionCoordinator = SessionCoordinator(
        context = context,
        database = database,
        settingsDataStore = settingsDataStore,
        scheduler = sessionScheduler,
        overlayManager = overlayManager
    )
}

class ScreenTimeTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        container = AppContainer(this)
        appScope.launch {
            container.sessionCoordinator.restoreSessionsOnBootOrRestart()
        }
    }
}
