package com.gymmanager

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymmanager.backup.AutoBackupWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "gym_settings")

object DataStoreKeys {
    val WHATSAPP_ENABLED = booleanPreferencesKey("whatsapp_enabled")
    val SMS_ENABLED      = booleanPreferencesKey("sms_enabled")
    val APP_LOCK         = booleanPreferencesKey("app_lock")
    val APP_PIN          = stringPreferencesKey("app_pin")
    val AUTO_BACKUP      = booleanPreferencesKey("auto_backup")
}

class GymApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AutoBackup if enabled
        runBlocking {
            val autoBackup = dataStore.data.map { it[DataStoreKeys.AUTO_BACKUP] ?: true }.first()
            if (autoBackup) {
                AutoBackupWorker.schedule(applicationContext)
            }
        }
    }
}
