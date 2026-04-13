package com.gymmanager

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "gym_settings")

object DataStoreKeys {
    val WHATSAPP_ENABLED = booleanPreferencesKey("whatsapp_enabled")
    val SMS_ENABLED      = booleanPreferencesKey("sms_enabled")
    val APP_LOCK         = booleanPreferencesKey("app_lock")
    val APP_PIN          = stringPreferencesKey("app_pin")
    val AUTO_BACKUP      = booleanPreferencesKey("auto_backup")
}

class GymApp : Application()
