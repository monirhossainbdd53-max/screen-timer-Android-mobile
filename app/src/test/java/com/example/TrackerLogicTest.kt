package com.example

import android.content.pm.ApplicationInfo
import com.example.data.db.AppSessionEntity
import com.example.data.db.SessionStatus
import com.example.system.BlockedAppsPolicy
import com.example.util.BengaliUtils
import com.example.util.EntryValidationResult
import com.example.util.EntryValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerLogicTest {

    @Test
    fun `entry validator rejects empty purpose and out of range minutes`() {
        val emptyBoth = EntryValidator.validate("   ", "")
        assertTrue(emptyBoth is EntryValidationResult.Invalid)
        val invalid1 = emptyBoth as EntryValidationResult.Invalid
        assertTrue(invalid1.purposeError != null)
        assertTrue(invalid1.minutesError != null)

        val zeroMin = EntryValidator.validate("মেসেজ দেখা", "0")
        assertTrue(zeroMin is EntryValidationResult.Invalid)

        val overMax = EntryValidator.validate("মেসেজ দেখা", "181")
        assertTrue(overMax is EntryValidationResult.Invalid)
    }

    @Test
    fun `entry validator accepts valid English and Bengali digits`() {
        val res1 = EntryValidator.validate("কাজের মেইল দেখা", "15")
        assertTrue(res1 is EntryValidationResult.Valid)
        assertEquals(15, (res1 as EntryValidationResult.Valid).minutes)

        val res2 = EntryValidator.validate("ভিডিও দেখা", "১৮০")
        assertTrue(res2 is EntryValidationResult.Valid)
        assertEquals(180, (res2 as EntryValidationResult.Valid).minutes)
    }

    @Test
    fun `countdown formats mm ss accurately`() {
        assertEquals("01:00", BengaliUtils.formatCountdownMmSs(60_000L))
        assertEquals("00:59", BengaliUtils.formatCountdownMmSs(59_000L))
        assertEquals("00:00", BengaliUtils.formatCountdownMmSs(0L))
        assertEquals("০১:০০", BengaliUtils.formatCountdownBengaliMmSs(60_000L))
    }

    @Test
    fun `paused session preserves remaining time and resumes accurately`() {
        val start = 1_000_000L
        val tenMinMillis = 10 * 60_000L
        val twoMinMillis = 2 * 60_000L

        val pausedSession = AppSessionEntity(
            id = 1L,
            packageName = "com.facebook.katana",
            appName = "Facebook",
            purpose = "মেসেজ দেখা",
            plannedMinutes = 10,
            startTimeMillis = start,
            lastResumedAtMillis = start,
            endTimeMillis = start + tenMinMillis,
            remainingMillis = tenMinMillis - twoMinMillis,
            usedMillis = twoMinMillis,
            status = SessionStatus.PAUSED
        )

        // Even 30 minutes later while outside the app, remaining time stays 8 minutes!
        val thirtyMinsLater = start + 30 * 60_000L
        assertEquals(8 * 60_000L, pausedSession.currentRemainingMillis(thirtyMinsLater))
        assertEquals(2, pausedSession.actualUsedMinutes())

        // Resuming counts down from the remaining 8 minutes
        val resumedSession = pausedSession.copy(
            status = SessionStatus.ACTIVE,
            lastResumedAtMillis = thirtyMinsLater,
            endTimeMillis = thirtyMinsLater + pausedSession.remainingMillis
        )
        val afterThreeMoreMinutes = thirtyMinsLater + 3 * 60_000L
        assertEquals(5 * 60_000L, resumedSession.currentRemainingMillis(afterThreeMoreMinutes))
        assertEquals(5 * 60_000L, resumedSession.currentUsedMillis(afterThreeMoreMinutes))
    }

    @Test
    fun `blocked apps policy excludes system blocked apps and non-launchable apps`() {
        val blocked = setOf("com.android.settings", "com.android.systemui", "com.example.self")
        val regularApp = ApplicationInfo()
        val systemApp = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }

        assertFalse(
            BlockedAppsPolicy.isPackageSelectable("com.android.settings", systemApp, true, blocked)
        )
        assertFalse(
            BlockedAppsPolicy.isPackageSelectable("com.facebook.katana", regularApp, false, blocked)
        )
        assertTrue(
            BlockedAppsPolicy.isPackageSelectable("com.facebook.katana", regularApp, true, blocked)
        )
        assertTrue(
            BlockedAppsPolicy.isPackageSelectable("com.google.android.youtube", systemApp, true, blocked)
        )
    }
}
