package com.example.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String
)

class InstalledAppsProvider(private val context: Context) {
    private val iconCache = LruCache<String, ImageBitmap>(120)

    suspend fun getSelectableApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val blockedPackages = BlockedAppsPolicy.resolveBlockedPackages(context)

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val seenPackages = HashSet<String>()
        val result = ArrayList<InstalledAppItem>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkg = activityInfo.packageName ?: continue
            if (!seenPackages.add(pkg)) continue

            val appInfo = activityInfo.applicationInfo ?: continue
            val hasLaunchIntent = pm.getLaunchIntentForPackage(pkg) != null
            if (!BlockedAppsPolicy.isPackageSelectable(pkg, appInfo, hasLaunchIntent, blockedPackages)) {
                continue
            }

            val label = resolveInfo.loadLabel(pm)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: pm.getApplicationLabel(appInfo).toString()

            result.add(InstalledAppItem(packageName = pkg, appName = label))
        }

        result.sortedBy { it.appName.lowercase() }
    }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawableToBitmap(drawable, 96)
                bitmap.asImageBitmap().also { iconCache.put(packageName, it) }
            }.getOrNull()
        }
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
