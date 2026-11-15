package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object SessionStatus {
    const val ACTIVE = "ACTIVE"
    const val PAUSED = "PAUSED"
    const val EXPIRED = "EXPIRED"
    const val STOPPED_BY_USER = "STOPPED_BY_USER"
}

@Entity(tableName = "app_sessions")
data class AppSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val purpose: String,
    val plannedMinutes: Int,
    val startTimeMillis: Long,
    val lastResumedAtMillis: Long,
    val endTimeMillis: Long,
    val remainingMillis: Long,
    val usedMillis: Long = 0L,
    val actualEndTimeMillis: Long? = null,
    val warningShown: Boolean = false,
    val status: String = SessionStatus.ACTIVE
) {
    fun currentRemainingMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return when (status) {
            SessionStatus.ACTIVE -> (endTimeMillis - nowMillis).coerceAtLeast(0L)
            SessionStatus.PAUSED -> remainingMillis.coerceAtLeast(0L)
            else -> 0L
        }
    }

    fun currentUsedMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val totalPlanned = plannedMinutes * 60_000L
        return when (status) {
            SessionStatus.ACTIVE -> {
                val activeElapsed = (nowMillis - lastResumedAtMillis).coerceAtLeast(0L)
                (usedMillis + activeElapsed).coerceIn(0L, totalPlanned)
            }
            else -> usedMillis.coerceIn(0L, totalPlanned)
        }
    }

    fun actualUsedMinutes(): Int {
        val totalUsed = currentUsedMillis(actualEndTimeMillis ?: System.currentTimeMillis())
        return if (totalUsed <= 0L) 0 else ((totalUsed + 59_999L) / 60_000L).toInt().coerceAtMost(plannedMinutes)
    }
}
