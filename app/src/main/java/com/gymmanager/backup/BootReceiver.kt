package com.gymmanager.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gymmanager.DataStoreKeys
import com.gymmanager.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val autoBackup = context.dataStore.data.map { it[DataStoreKeys.AUTO_BACKUP] ?: true }.first()
                if (autoBackup) {
                    AutoBackupWorker.schedule(context)
                }
            }
        }
    }
}
