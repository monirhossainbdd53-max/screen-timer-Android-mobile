package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Bengali title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("স্ক্রিন টাইম ট্র্যাকার", appName)
    }

    @Test
    fun `manifest has no internet permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
        assertTrue(permissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW))
        assertTrue(permissions.contains(Manifest.permission.RECEIVE_BOOT_COMPLETED))
    }
}
