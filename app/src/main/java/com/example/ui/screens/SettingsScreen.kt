package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.prefs.SettingsDataStore
import com.example.system.PermissionChecker
import com.example.ui.MainUiState
import com.example.ui.theme.SuccessGreen
import com.example.util.BengaliUtils
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onWarningSecondsChange: (Int) -> Unit,
    onStopAllSessions: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showStopAllDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showStopAllDialog) {
        AlertDialog(
            onDismissRequest = { showStopAllDialog = false },
            title = { Text("সব সেশন নিষ্ক্রিয় করবেন?") },
            text = { Text("সকল চলমান সেশন এখনই বন্ধ হয়ে যাবে এবং সব নির্বাচিত অ্যাপ 'নিষ্ক্রিয় (Disabled)' অবস্থায় চলে যাবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopAllDialog = false
                        onStopAllSessions()
                    }
                ) {
                    Text("নিষ্ক্রিয় করুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopAllDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("ইতিহাস মুছবেন?") },
            text = { Text("সব সংরক্ষিত সেশন ইতিহাস মুছে যাবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text("মুছুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "warning_slider_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "সতর্কবার্তার সময়",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সময় শেষ হওয়ার ${BengaliUtils.formatInt(uiState.warningSeconds)} সেকেন্ড আগে নোটিফিকেশন ও স্ক্রিন সতর্কবার্তা দেখাবে (৩০ থেকে ১২০ সেকেন্ড)।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = uiState.warningSeconds.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 5f).roundToInt() * 5)
                                .coerceIn(
                                    SettingsDataStore.MIN_WARNING_SECONDS,
                                    SettingsDataStore.MAX_WARNING_SECONDS
                                )
                            onWarningSecondsChange(snapped)
                        },
                        valueRange = SettingsDataStore.MIN_WARNING_SECONDS.toFloat()..
                            SettingsDataStore.MAX_WARNING_SECONDS.toFloat(),
                        steps = 17,
                        modifier = Modifier.testTag("warning_seconds_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("৩০ সে.", style = MaterialTheme.typography.labelSmall)
                        Text("৬০ সে. (ডিফল্ট)", style = MaterialTheme.typography.labelSmall)
                        Text("১২০ সে.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item(key = "control_buttons_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "সেশন ও ডেটা নিয়ন্ত্রণ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showStopAllDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("disable_all_sessions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নিষ্ক্রিয় করুন (সব সেশন বন্ধ)", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("settings_clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ইতিহাস মুছুন", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item(key = "permissions_status_card") {
            val perms = uiState.permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "অনুমতির অবস্থা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PermissionStatusRow(
                        label = "অ্যাক্সেসিবিলিটি সেবা",
                        granted = perms.accessibilityGranted,
                        onOpenSettings = { PermissionChecker.openAccessibilitySettings(context) }
                    )
                    HorizontalDivider()
                    PermissionStatusRow(
                        label = "অন্যান্য অ্যাপের ওপর প্রদর্শন",
                        granted = perms.overlayGranted,
                        onOpenSettings = { PermissionChecker.openOverlayPermissionSettings(context) }
                    )
                    HorizontalDivider()
                    PermissionStatusRow(
                        label = "ব্যবহারের অ্যাক্সেস (Usage Access)",
                        granted = perms.usageAccessGranted,
                        onOpenSettings = { PermissionChecker.openUsageAccessSettings(context) }
                    )
                    HorizontalDivider()
                    PermissionStatusRow(
                        label = "নোটিফিকেশন",
                        granted = perms.notificationsGranted,
                        onOpenSettings = { PermissionChecker.openAppNotificationSettings(context) }
                    )
                    HorizontalDivider()
                    PermissionStatusRow(
                        label = "ব্যাটারি অপ্টিমাইজেশন অব্যাহতি",
                        granted = perms.batteryOptimizationExempt,
                        onOpenSettings = { PermissionChecker.openBatteryOptimizationSettings(context) }
                    )
                }
            }
        }

        item(key = "symphony_tips_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Symphony Z42 (Android 11) এবং গোপনীয়তা",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = "• Symphony Z42 (Android 11)-এ ব্যাকগ্রাউন্ড টাইমার সচল রাখতে: Settings > Apps & notifications > স্ক্রিন টাইম ট্র্যাকার > Battery > Battery optimization > Don't optimize নির্বাচন করুন।\n• Recents স্ক্রিনে অ্যাপটি লক করে রাখলে সিস্টেম ক্লিনার অ্যাপটি বন্ধ করবে না।\n• ${stringResource(R.string.privacy_statement)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (granted) SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(if (granted) "সেটিংস" else "চালু করুন")
        }
    }
}
