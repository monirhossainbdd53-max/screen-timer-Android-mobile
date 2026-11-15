package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

object ManagedAppStatus {
    const val ACTIVE = "ACTIVE"
    const val PAUSED = "PAUSED"
    const val DISABLED = "DISABLED"
}

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isManaged: Boolean = true,
    val status: String = ManagedAppStatus.DISABLED,
    val activeSessionId: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
