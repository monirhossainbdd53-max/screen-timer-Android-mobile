package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PermissionSetupScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ScreenTimeTrackerTheme

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    APPS("অ্যাপ", Icons.Rounded.Apps, Icons.Outlined.Apps, "nav_apps"),
    SESSIONS("সেশন", Icons.Rounded.Timer, Icons.Outlined.Timer, "nav_sessions"),
    HISTORY("ইতিহাস", Icons.Rounded.History, Icons.Outlined.History, "nav_history"),
    SETTINGS("সেটিংস", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTrackerTheme {
                ScreenTimeTrackerRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeTrackerRoot(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.refreshInstalledApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.APPS) }

    BackHandler(enabled = uiState.permissions.allGranted && currentTab != MainTab.APPS) {
        currentTab = MainTab.APPS
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.permissions.allGranted) {
                                "অফলাইন • সুরক্ষিত"
                            } else {
                                "অনুমতি সেটআপ আবশ্যক"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState.permissions.allGranted) {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!uiState.permissions.allGranted) {
                PermissionSetupScreen(
                    permissions = uiState.permissions,
                    onRefreshPermissions = { viewModel.refreshPermissions() }
                )
            } else {
                MainTabContent(
                    tab = currentTab,
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.widthIn(max = 720.dp)
                )
            }
        }
    }
}

@Composable
private fun MainTabContent(
    tab: MainTab,
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    when (tab) {
        MainTab.APPS -> AppsScreen(
            uiState = uiState,
            onSearchChange = viewModel::setSearchQuery,
            onFilterManagedChange = viewModel::setShowOnlyManaged,
            onToggleApp = viewModel::toggleManagedApp,
            loadAppIcon = viewModel::loadAppIcon,
            modifier = modifier
        )
        MainTab.SESSIONS -> SessionsScreen(
            uiState = uiState,
            onStopSession = viewModel::stopSession,
            modifier = modifier
        )
        MainTab.HISTORY -> HistoryScreen(
            uiState = uiState,
            onClearHistory = viewModel::clearHistory,
            modifier = modifier
        )
        MainTab.SETTINGS -> SettingsScreen(
            uiState = uiState,
            onWarningSecondsChange = viewModel::setWarningSeconds,
            onStopAllSessions = viewModel::stopAllSessions,
            onClearHistory = viewModel::clearHistory,
            modifier = modifier
        )
    }
}
