package com.hamzaasad.gymmanagerpro.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.hamzaasad.gymmanagerpro.data.db.GymDatabase
import com.hamzaasad.gymmanagerpro.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SyncManager(private val context: Context, private val db: GymDatabase) {
    private val syncServer = SyncServer(context, db)
    private val syncClient = SyncClient(context, db)

    companion object {
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    val lastSyncTime: Flow<Long> = context.dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }

    fun startServer(port: Int = 8080): Boolean {
        return syncServer.start(port)
    }

    fun stopServer() {
        syncServer.stop()
    }

    suspend fun syncWithServer(serverIp: String): SyncResult {
        val currentTime = lastSyncTime.first()
        val result = syncClient.syncWithServer(serverIp, currentTime)
        if (result is SyncResult.Success) {
            updateLastSyncTime(result.serverTime)
        }
        return result
    }

    private suspend fun updateLastSyncTime(timestamp: Long) {
        context.dataStore.edit { it[LAST_SYNC_TIME] = timestamp }
    }
}
