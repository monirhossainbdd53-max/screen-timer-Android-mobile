package com.example.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings

object BlockedAppsPolicy {

    val blockedInfoItemsBengali: List<String> = listOf(
        "ফোনের সিস্টেম সেটিংস (Settings)",
        "এই টাইমার অ্যাপ নিজে",
        "প্যাকেজ ইনস্টলার ও অ্যাপ ম্যানেজার",
        "ফোন ডায়ালার ও জরুরি সেবা (Emergency)",
        "সিস্টেম UI (Status bar, Launcher/Home) — এগুলো বন্ধ করা Android অনুমতি দেয় না",
        "ব্যাংকিং ও পেমেন্ট অ্যাপের কিছু অংশ (নিরাপত্তার কারণে সীমিত)"
    )

    private val staticBlockedPackages: Set<String> = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.systemui",
        "com.android.emergency",
        "com.android.incallui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    fun resolveBlockedPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val blocked = HashSet<String>(staticBlockedPackages)
        blocked.add(context.packageName)

        // Default launcher(s)
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY).forEach {
            it.activityInfo?.packageName?.let(blocked::add)
        }
        pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
            blocked.add(it)
        }

        // System settings handler
        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
        pm.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
            blocked.add(it)
        }

        // Default dialer
        val dialIntent = Intent(Intent.ACTION_DIAL)
        pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let {
            blocked.add(it)
        }

        return blocked
    }

    fun isPackageSelectable(
        packageName: String,
        applicationInfo: ApplicationInfo,
        hasLaunchIntent: Boolean,
        blockedPackages: Set<String>
    ): Boolean {
        if (!hasLaunchIntent) return false
        if (packageName in blockedPackages) return false

        // Exclude system apps unless they have a launch intent and are not in the blocked list
        val isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        return if (isSystem) {
            hasLaunchIntent && packageName !in blockedPackages
        } else {
            true
        }
    }
}
