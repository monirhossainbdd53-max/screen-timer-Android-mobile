package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.system.PermissionChecker
import com.example.system.PermissionStatusSnapshot
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.OnSuccessGreenContainer
import com.example.util.BengaliUtils

@Composable
fun PermissionSetupScreen(
    permissions: PermissionStatusSnapshot,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notificationDeniedOnce by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            notificationDeniedOnce = true
        }
        onRefreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "শুরু করার আগে অনুমতি সেটআপ",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "অ্যাপটি সঠিকভাবে কাজ করার জন্য নিচের ৫টি অনুমতি নিজে থেকে চালু করা প্রয়োজন। সব অনুমতি সম্পন্ন না হওয়া পর্যন্ত মূল স্ক্রিন লক থাকবে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        LinearProgressIndicator(
                            progress = { permissions.grantedCount / 5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "অনুমতি সম্পন্ন: ${BengaliUtils.formatInt(permissions.grantedCount)} / ৫",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            OutlinedButton(
                                onClick = onRefreshPermissions,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("refresh_permissions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "পুনরায় যাচাই করুন",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("যাচাই করুন")
                            }
                        }
                    }
                }
            }

            item {
                PrivacyStatementCard()
            }

            item {
                PermissionCard(
                    stepNumber = "১",
                    icon = Icons.Rounded.AccessibilityNew,
                    title = "অ্যাক্সেসিবিলিটি সেবা (Accessibility Service)",
                    explanation = "আপনি কোন অ্যাপ খুলছেন তা বুঝতে এবং সময় শেষ হলে স্বয়ংক্রিয়ভাবে হোম স্ক্রিনে ফিরিয়ে আনতে এটি প্রয়োজন। আমরা কোনো অ্যাপের লেখা বা ব্যক্তিগত তথ্য পড়ি না।",
                    deniedHelpText = "অনুমতি না দিলে: সময় শেষ হলেও অ্যাপ স্বয়ংক্রিয়ভাবে বন্ধ হবে না এবং প্রবেশের পপআপ আসবে না। Settings > Accessibility > স্ক্রিন টাইম ট্র্যাকার চালু করুন।",
                    isGranted = permissions.accessibilityGranted,
                    buttonLabel = "অ্যাক্সেসিবিলিটি সেটিংস খুলুন",
                    buttonTestTag = "perm_accessibility_button",
                    onAction = { PermissionChecker.openAccessibilitySettings(context) }
                )
            }

            item {
                PermissionCard(
                    stepNumber = "২",
                    icon = Icons.Rounded.Layers,
                    title = "অন্যান্য অ্যাপের ওপর প্রদর্শন (Display over other apps)",
                    explanation = "নির্বাচিত অ্যাপ খোলার আগে উদ্দেশ্য ও সময় লেখার পপআপ এবং ১ মিনিট আগের সতর্কবার্তা দেখাতে এটি প্রয়োজন।",
                    deniedHelpText = "অনুমতি না দিলে: কোনো অ্যাপ খোলার সময় উদ্দেশ্য ও মিনিট জিজ্ঞাসা করার পপআপ স্ক্রিনে দেখা যাবে না।",
                    isGranted = permissions.overlayGranted,
                    buttonLabel = "ওভারলে অনুমতি দিন",
                    buttonTestTag = "perm_overlay_button",
                    onAction = { PermissionChecker.openOverlayPermissionSettings(context) }
                )
            }

            item {
                PermissionCard(
                    stepNumber = "৩",
                    icon = Icons.Rounded.DataUsage,
                    title = "ব্যবহারের অ্যাক্সেস (Usage Access)",
                    explanation = "ফোনে ইনস্টল করা অ্যাপের তালিকা দেখাতে এবং কোন অ্যাপ বর্তমানে সামনে চলছে তা নিশ্চিত করতে এটি প্রয়োজন।",
                    deniedHelpText = "অনুমতি না দিলে: ফোনের অ্যাপগুলো তালিকাভুক্ত করা বা সঠিকভাবে ট্র্যাক করা সম্ভব হবে না। তালিকা থেকে 'স্ক্রিন টাইম ট্র্যাকার' নির্বাচন করে Allow করুন।",
                    isGranted = permissions.usageAccessGranted,
                    buttonLabel = "Usage Access সেটিংস খুলুন",
                    buttonTestTag = "perm_usage_button",
                    onAction = { PermissionChecker.openUsageAccessSettings(context) }
                )
            }

            item {
                PermissionCard(
                    stepNumber = "৪",
                    icon = Icons.Rounded.Notifications,
                    title = "নোটিফিকেশন অনুমতি (Notifications)",
                    explanation = "চলমান টাইমার ('বাকি আছে: mm:ss') এবং সময় শেষের ১ মিনিট আগে শব্দ ও ভাইব্রেশনসহ সতর্কবার্তা দেখাতে এটি প্রয়োজন।",
                    deniedHelpText = if (notificationDeniedOnce) {
                        "নোটিফিকেশন বন্ধ থাকায় সতর্কবার্তা দেখা যাবে না। নিচের বোতামে চাপ দিয়ে App Info > Notifications থেকে অনুমতি চালু করুন।"
                    } else {
                        "অনুমতি না দিলে: ১ মিনিট আগের সতর্কবার্তা এবং চলমান টাইমার নোটিফিকেশন দেখাবে না।"
                    },
                    isGranted = permissions.notificationsGranted,
                    buttonLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationDeniedOnce) {
                        "নোটিফিকেশন অনুমতি দিন"
                    } else {
                        "নোটিফিকেশন সেটিংস খুলুন"
                    },
                    buttonTestTag = "perm_notifications_button",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationDeniedOnce) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionChecker.openAppNotificationSettings(context)
                        }
                    }
                )
            }

            item {
                PermissionCard(
                    stepNumber = "৫",
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "ব্যাটারি অপ্টিমাইজেশন থেকে অব্যাহতি",
                    explanation = "ফোন ব্যাকগ্রাউন্ডে অ্যাপটি বন্ধ না করে যেন ঠিক সময়ে অ্যালার্ম বাজাতে এবং হোম স্ক্রিনে ফেরাতে পারে, সেজন্য এটি প্রয়োজন।",
                    deniedHelpText = "Xiaomi, Oppo, Vivo, Realme, Symphony ফোনে অতিরিক্ত ধাপ প্রয়োজন হতে পারে: Settings > Apps > স্ক্রিন টাইম ট্র্যাকার > Battery > No restrictions / Unrestricted এবং Autostart চালু করুন।",
                    isGranted = permissions.batteryOptimizationExempt,
                    buttonLabel = "ব্যাটারি সেটিংস খুলুন",
                    buttonTestTag = "perm_battery_button",
                    onAction = { PermissionChecker.openBatteryOptimizationSettings(context) }
                )
            }
        }
    }
}

@Composable
fun PrivacyStatementCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = stringResource(R.string.privacy_statement),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun PermissionCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    explanation: String,
    deniedHelpText: String,
    isGranted: Boolean,
    buttonLabel: String,
    buttonTestTag: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGranted) SuccessGreenContainer else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = "$stepNumber. $title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SuccessGreenContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "অনুমোদিত",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "চালু আছে",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSuccessGreenContainer
                            )
                        }
                    }
                }
            }

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = deniedHelpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag(buttonTestTag)
                ) {
                    Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = deniedHelpText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
