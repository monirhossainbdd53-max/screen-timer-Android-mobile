package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppDao {
    @Query("SELECT * FROM managed_apps WHERE isManaged = 1 ORDER BY appName ASC")
    fun observeManagedApps(): Flow<List<ManagedAppEntity>>

    @Query("SELECT * FROM managed_apps WHERE isManaged = 1")
    suspend fun getManagedAppsSync(): List<ManagedAppEntity>

    @Query("SELECT * FROM managed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): ManagedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ManagedAppEntity)

    @Query("DELETE FROM managed_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE managed_apps SET status = :status, activeSessionId = :sessionId, updatedAtMillis = :updatedAt WHERE packageName = :packageName")
    suspend fun updateStatus(
        packageName: String,
        status: String,
        sessionId: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE managed_apps SET status = 'DISABLED', activeSessionId = NULL, updatedAtMillis = :updatedAt")
    suspend fun disableAll(updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface AppSessionDao {
    @Query("SELECT * FROM app_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY lastResumedAtMillis DESC")
    fun observeOpenSessions(): Flow<List<AppSessionEntity>>

    @Query("SELECT * FROM app_sessions WHERE status = 'ACTIVE' ORDER BY endTimeMillis ASC")
    suspend fun getActiveSessionsSync(): List<AppSessionEntity>

    @Query("SELECT * FROM app_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY lastResumedAtMillis DESC")
    suspend fun getOpenSessionsSync(): List<AppSessionEntity>

    @Query("SELECT * FROM app_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AppSessionEntity?

    @Query("SELECT * FROM app_sessions WHERE packageName = :packageName AND status IN ('ACTIVE', 'PAUSED') ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getOpenSessionForPackage(packageName: String): AppSessionEntity?

    @Query("SELECT * FROM app_sessions ORDER BY startTimeMillis DESC")
    fun observeAllSessions(): Flow<List<AppSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: AppSessionEntity): Long

    @Update
    suspend fun update(session: AppSessionEntity)

    @Query("UPDATE app_sessions SET warningShown = 1 WHERE id = :sessionId")
    suspend fun markWarningShown(sessionId: Long)

    @Query("UPDATE app_sessions SET status = :status, remainingMillis = :remainingMillis, usedMillis = :usedMillis, actualEndTimeMillis = :actualEndMillis WHERE id = :sessionId")
    suspend fun closeSession(
        sessionId: Long,
        status: String,
        remainingMillis: Long,
        usedMillis: Long,
        actualEndMillis: Long
    )

    @Query("UPDATE app_sessions SET status = 'STOPPED_BY_USER', remainingMillis = 0, actualEndTimeMillis = :actualEndMillis WHERE status IN ('ACTIVE', 'PAUSED')")
    suspend fun stopAllOpenSessions(actualEndMillis: Long)

    @Query("DELETE FROM app_sessions WHERE status NOT IN ('ACTIVE', 'PAUSED')")
    suspend fun clearCompletedHistory()
}
