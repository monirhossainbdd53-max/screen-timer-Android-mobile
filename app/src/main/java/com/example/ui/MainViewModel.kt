package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScreenTimeTrackerApp
import com.example.data.db.AppSessionEntity
import com.example.data.db.ManagedAppEntity
import com.example.data.db.SessionStatus
import com.example.system.InstalledAppItem
import com.example.system.PermissionChecker
import com.example.system.PermissionStatusSnapshot
import com.example.util.BengaliUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUsageSummary(
    val packageName: String,
    val appName: String,
    val totalMinutes: Int
)

data class HistoryGroup(
    val dateHeader: String,
    val sessions: List<AppSessionEntity>
)

data class MainUiState(
    val permissions: PermissionStatusSnapshot = PermissionStatusSnapshot(),
    val installedApps: List<InstalledAppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val managedAppsMap: Map<String, ManagedAppEntity> = emptyMap(),
    val activeSessions: List<AppSessionEntity> = emptyList(),
    val historyGroups: List<HistoryGroup> = emptyList(),
    val todayTotalsByApp: List<AppUsageSummary> = emptyList(),
    val weekTotalsByApp: List<AppUsageSummary> = emptyList(),
    val todayTotalMinutes: Int = 0,
    val weekTotalMinutes: Int = 0,
    val warningSeconds: Int = 60,
    val searchQuery: String = "",
    val showOnlyManaged: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ScreenTimeTrackerApp).container
    private val managedAppDao = container.database.managedAppDao()
    private val sessionDao = container.database.appSessionDao()
    private val coordinator = container.sessionCoordinator
    private val settingsStore = container.settingsDataStore
    private val installedAppsProvider = container.installedAppsProvider

    private val permissionsFlow = MutableStateFlow(PermissionChecker.checkAll(application))
    private val installedAppsFlow = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val isLoadingAppsFlow = MutableStateFlow(true)
    private val searchQueryFlow = MutableStateFlow("")
    private val showOnlyManagedFlow = MutableStateFlow(false)

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private data class CoreSnapshot(
        val permissions: PermissionStatusSnapshot,
        val installedApps: List<InstalledAppItem>,
        val isLoadingApps: Boolean,
        val managedList: List<ManagedAppEntity>,
        val openList: List<AppSessionEntity>,
        val allSessions: List<AppSessionEntity>,
        val warningSeconds: Int
    )

    private val coreSnapshotFlow = combine(
        permissionsFlow,
        installedAppsFlow,
        isLoadingAppsFlow,
        managedAppDao.observeManagedApps(),
        sessionDao.observeOpenSessions(),
        sessionDao.observeAllSessions(),
        settingsStore.warningSecondsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        CoreSnapshot(
            permissions = values[0] as PermissionStatusSnapshot,
            installedApps = values[1] as List<InstalledAppItem>,
            isLoadingApps = values[2] as Boolean,
            managedList = values[3] as List<ManagedAppEntity>,
            openList = values[4] as List<AppSessionEntity>,
            allSessions = values[5] as List<AppSessionEntity>,
            warningSeconds = values[6] as Int
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreSnapshotFlow,
        searchQueryFlow,
        showOnlyManagedFlow,
        tickerFlow
    ) { core, query, onlyManaged, now ->
        val completedSessions = core.allSessions.filter {
            it.status != SessionStatus.ACTIVE && it.status != SessionStatus.PAUSED
        }
        val todayStart = BengaliUtils.dayStartMillis(now)
        val weekStart = BengaliUtils.weekStartMillis(now)

        val todaySessions = completedSessions.filter { it.startTimeMillis >= todayStart }
        val weekSessions = completedSessions.filter { it.startTimeMillis >= weekStart }

        val todayTotals = todaySessions
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                AppUsageSummary(pkg, list.first().appName, list.sumOf { it.actualUsedMinutes() })
            }
            .sortedByDescending { it.totalMinutes }

        val weekTotals = weekSessions
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                AppUsageSummary(pkg, list.first().appName, list.sumOf { it.actualUsedMinutes() })
            }
            .sortedByDescending { it.totalMinutes }

        val groupedHistory = completedSessions
            .groupBy { BengaliUtils.formatDateHeader(it.startTimeMillis, now) }
            .map { (header, sessions) -> HistoryGroup(header, sessions) }

        MainUiState(
            permissions = core.permissions,
            installedApps = core.installedApps,
            isLoadingApps = core.isLoadingApps,
            managedAppsMap = core.managedList.associateBy { it.packageName },
            activeSessions = core.openList,
            historyGroups = groupedHistory,
            todayTotalsByApp = todayTotals,
            weekTotalsByApp = weekTotals,
            todayTotalMinutes = todayTotals.sumOf { it.totalMinutes },
            weekTotalMinutes = weekTotals.sumOf { it.totalMinutes },
            warningSeconds = core.warningSeconds,
            searchQuery = query,
            showOnlyManaged = onlyManaged,
            nowMillis = now
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(permissions = PermissionChecker.checkAll(application))
    )

    init {
        refreshInstalledApps()
    }

    fun refreshPermissions() {
        permissionsFlow.value = PermissionChecker.checkAll(getApplication())
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            isLoadingAppsFlow.value = true
            installedAppsFlow.value = installedAppsProvider.getSelectableApps()
            isLoadingAppsFlow.value = false
        }
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setShowOnlyManaged(onlyManaged: Boolean) {
        showOnlyManagedFlow.value = onlyManaged
    }

    fun toggleManagedApp(app: InstalledAppItem, enabled: Boolean) {
        viewModelScope.launch {
            coordinator.toggleManagedApp(app.packageName, app.appName, enabled)
        }
    }

    fun stopSession(sessionId: Long) {
        viewModelScope.launch {
            coordinator.stopSessionById(sessionId)
        }
    }

    fun stopAllSessions() {
        viewModelScope.launch {
            coordinator.stopAllActiveSessions()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            coordinator.clearHistory()
        }
    }

    fun setWarningSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsStore.setWarningSeconds(seconds)
        }
    }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? {
        return installedAppsProvider.loadAppIcon(packageName)
    }
}
