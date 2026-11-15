package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.AppSessionEntity
import com.example.data.db.ManagedAppEntity
import com.example.data.db.SessionStatus
import com.example.system.BlockedAppsPolicy
import com.example.system.InstalledAppItem
import com.example.ui.MainUiState
import com.example.ui.theme.OnSuccessGreenContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.util.BengaliUtils

@Composable
fun AppsScreen(
    uiState: MainUiState,
    onSearchChange: (String) -> Unit,
    onFilterManagedChange: (Boolean) -> Unit,
    onToggleApp: (InstalledAppItem, Boolean) -> Unit,
    loadAppIcon: suspend (String) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val openSessionsMap = remember(uiState.activeSessions) {
        uiState.activeSessions.associateBy { it.packageName }
    }

    val filteredApps = remember(
        uiState.installedApps,
        uiState.managedAppsMap,
        uiState.searchQuery,
        uiState.showOnlyManaged
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        uiState.installedApps.filter { app ->
            val matchesQuery = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            val isManaged = uiState.managedAppsMap.containsKey(app.packageName)
            matchesQuery && (!uiState.showOnlyManaged || isManaged)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "blocked_apps_card") {
            BlockedAppsNoticeCard()
        }

        item(key = "search_and_filter") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("অ্যাপ খুঁজুন (যেমন: Facebook, YouTube)...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "সার্চ মুছুন"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = !uiState.showOnlyManaged,
                        onClick = { onFilterManagedChange(false) },
                        label = {
                            Text("সব অ্যাপ (${BengaliUtils.formatInt(uiState.installedApps.size)})")
                        }
                    )
                    FilterChip(
                        selected = uiState.showOnlyManaged,
                        onClick = { onFilterManagedChange(true) },
                        label = {
                            Text("নির্বাচিত (${BengaliUtils.formatInt(uiState.managedAppsMap.size)})")
                        }
                    )
                }
            }
        }

        if (uiState.isLoadingApps) {
            item(key = "loading_apps") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ইনস্টল করা অ্যাপগুলো লোড হচ্ছে...")
                    }
                }
            }
        } else if (filteredApps.isEmpty()) {
            item(key = "empty_apps") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "কোনো অ্যাপ পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "অনুগ্রহ করে অন্য নাম দিয়ে খুঁজুন অথবা 'সব অ্যাপ' নির্বাচন করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->
                val managed = uiState.managedAppsMap[app.packageName]
                val openSession = openSessionsMap[app.packageName]
                SelectableAppRow(
                    app = app,
                    managedEntity = managed,
                    openSession = openSession,
                    nowMillis = uiState.nowMillis,
                    onToggle = { enabled -> onToggleApp(app, enabled) },
                    loadAppIcon = loadAppIcon
                )
            }
        }
    }
}

@Composable
private fun BlockedAppsNoticeCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blocked_apps_notice_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "এই অ্যাপগুলো ব্যবহার করা যাবে না",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            BlockedAppsPolicy.blockedInfoItemsBengali.forEach { line ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableAppRow(
    app: InstalledAppItem,
    managedEntity: ManagedAppEntity?,
    openSession: AppSessionEntity?,
    nowMillis: Long,
    onToggle: (Boolean) -> Unit,
    loadAppIcon: suspend (String) -> ImageBitmap?
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(app.packageName) {
        iconBitmap = loadAppIcon(app.packageName)
    }

    val isManaged = managedEntity != null
    val remainingMillis = openSession?.currentRemainingMillis(nowMillis) ?: 0L
    val isActiveNow = openSession != null && openSession.status == SessionStatus.ACTIVE && remainingMillis > 0L
    val isPausedNow = openSession != null && openSession.status == SessionStatus.PAUSED && remainingMillis > 0L
    val isDisabledState = isManaged && !isActiveNow && !isPausedNow

    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isManaged) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isManaged) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap!!,
                        contentDescription = app.appName,
                        colorFilter = if (isDisabledState) grayscaleFilter else null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .alpha(if (isDisabledState) 0.42f else 1f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .alpha(if (isDisabledState) 0.45f else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = app.appName,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isDisabledState) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "নিষ্ক্রিয়",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDisabledState) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isManaged) {
                    when {
                        isActiveNow -> {
                            val mmSs = BengaliUtils.formatCountdownBengaliMmSs(remainingMillis)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SuccessGreenContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "সক্রিয় (Active) • $mmSs বাকি",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSuccessGreenContainer
                                    )
                                }
                            }
                        }
                        isPausedNow -> {
                            val mmSs = BengaliUtils.formatCountdownBengaliMmSs(remainingMillis)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PauseCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "বিরতিতে (Paused) • $mmSs বাকি",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "নিষ্ক্রিয় (Disabled)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "ট্র্যাকিং চালু করতে সুইচ অন করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isManaged,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("toggle_app_${app.packageName}")
            )
        }
    }
}
