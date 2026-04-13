package com.gymmanager.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.FragmentActivity
import com.gymmanager.ui.Screen
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.viewmodel.GymViewModel
import com.gymmanager.dataStore
import com.gymmanager.DataStoreKeys
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: GymViewModel, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val gymInfo by vm.gymInfo.collectAsState()

    val whatsappEnabled by context.dataStore.data.map { it[DataStoreKeys.WHATSAPP_ENABLED] ?: true }.collectAsState(true)
    val smsEnabled      by context.dataStore.data.map { it[DataStoreKeys.SMS_ENABLED] ?: false }.collectAsState(false)
    val appLock         by context.dataStore.data.map { it[DataStoreKeys.APP_LOCK] ?: false }.collectAsState(false)
    val autoBackup      by context.dataStore.data.map { it[DataStoreKeys.AUTO_BACKUP] ?: true }.collectAsState(true)
    val savedPin        by context.dataStore.data.map { it[DataStoreKeys.APP_PIN] ?: "" }.collectAsState("")

    var showPinDialog   by remember { mutableStateOf(false) }
    var newPin          by remember { mutableStateOf("") }
    var confirmPin      by remember { mutableStateOf("") }
    var pinError        by remember { mutableStateOf("") }

    fun setWhatsapp(v: Boolean)   = scope.launch { context.dataStore.edit { it[DataStoreKeys.WHATSAPP_ENABLED] = v } }
    fun setSms(v: Boolean)        = scope.launch { context.dataStore.edit { it[DataStoreKeys.SMS_ENABLED] = v } }
    fun setAutoBackup(v: Boolean) = scope.launch { context.dataStore.edit { it[DataStoreKeys.AUTO_BACKUP] = v } }
    fun setAppLock(v: Boolean)    = scope.launch {
        if (!v) {
            context.dataStore.edit { it[DataStoreKeys.APP_LOCK] = false }
        } else {
            if (savedPin.isNotEmpty()) {
                context.dataStore.edit { it[DataStoreKeys.APP_LOCK] = true }
            } else {
                showPinDialog = true
            }
        }
    }

    Column(Modifier.fillMaxSize().background(GymBgDark).verticalScroll(rememberScrollState())) {
        // Header
        Box(Modifier.fillMaxWidth().background(GymBgCard)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GymBgElevated)) {
                        Icon(Icons.Default.ArrowBack, null, tint = GymYellow)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Settings", style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(GymYellow))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Gym Info Card
        gymInfo?.let { info ->
            Surface(color = GymBgCard, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x1AFFD600)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FitnessCenter, null, tint = GymYellow, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(info.gymName, color = TextPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text(info.ownerName, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(info.phone, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // MESSAGING
        SettingsSectionHeader("💬 MESSAGING")
        Surface(color = GymBgCard, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Column {
                GymSettingsToggle("WhatsApp Reminders", "Send fee reminders via WhatsApp",
                    Icons.Default.Message, Color(0xFF25D366), whatsappEnabled) { setWhatsapp(it) }
                HorizontalDivider(color = GymBgBorder, modifier = Modifier.padding(horizontal = 16.dp))
                GymSettingsToggle("SMS Reminders", "Send fee reminders via SMS",
                    Icons.Default.Sms, Blue500, smsEnabled) { setSms(it) }
            }
        }

        // SUBSCRIPTION
        SettingsSectionHeader("💳 SUBSCRIPTION PLANS")
        Surface(color = GymBgCard, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            GymSettingsNavRow("Manage Plans", "Add or remove subscription plans",
                Icons.Default.CardMembership, GymGreen) { onNavigate(Screen.SubscriptionPlans.route) }
        }

        // BACKUP
        SettingsSectionHeader("☁️ BACKUP & DATA")
        Surface(color = GymBgCard, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Column {
                GymSettingsToggle("Auto Backup to Drive", "Daily backup when internet available",
                    Icons.Default.CloudSync, Cyan500, autoBackup) { setAutoBackup(it) }
                HorizontalDivider(color = GymBgBorder, modifier = Modifier.padding(horizontal = 16.dp))
                GymSettingsNavRow("Backup & Restore", "Manual backup or restore from Google Drive",
                    Icons.Default.CloudUpload, Color(0xFF0277BD)) { onNavigate(Screen.BackupRestore.route) }
            }
        }

        // SECURITY
        SettingsSectionHeader("🔒 SECURITY")
        Surface(color = GymBgCard, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Column {
                GymSettingsToggle("App Lock", "Require PIN or fingerprint to open app",
                    Icons.Default.Lock, GymYellow, appLock) { setAppLock(it) }
                if (appLock) {
                    HorizontalDivider(color = GymBgBorder, modifier = Modifier.padding(horizontal = 16.dp))
                    GymSettingsNavRow("Change PIN", "Update your 4-digit app lock PIN",
                        Icons.Default.Pin, GymGrey) { showPinDialog = true }
                }
            }
        }

        // ABOUT
        SettingsSectionHeader("ℹ️ ABOUT")
        Surface(color = GymBgCard, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(GymYellow),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.FitnessCenter, null, tint = GymBgDark, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Gym Manager Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Text("Version 1.0.0", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = GymBgBorder)
                Spacer(Modifier.height(12.dp))
                Text("Developed by", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                Text("Engr. Hamza Asad", color = GymYellow, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Built for Pakistani Gym Owners 🇵🇰", fontSize = 11.sp, color = TextMuted)
            }
        }
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; newPin = ""; confirmPin = ""; pinError = "" },
            containerColor = GymBgCard,
            title = { Text("Set App PIN", color = GymYellow, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a 4-digit PIN to lock your app.", color = TextSecondary)
                    OutlinedTextField(
                        value = newPin, onValueChange = { if (it.length <= 4) newPin = it },
                        label = { Text("New PIN", color = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GymBgElevated, unfocusedContainerColor = GymBgElevated,
                            focusedBorderColor = GymYellow, unfocusedBorderColor = GymBgBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it },
                        label = { Text("Confirm PIN", color = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GymBgElevated, unfocusedContainerColor = GymBgElevated,
                            focusedBorderColor = GymYellow, unfocusedBorderColor = GymBgBorder),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError.isNotEmpty()) {
                        Text(pinError, color = StatusUnpaid, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        newPin.length < 4     -> pinError = "PIN must be 4 digits"
                        newPin != confirmPin  -> pinError = "PINs do not match"
                        else -> {
                            scope.launch {
                                context.dataStore.edit {
                                    it[DataStoreKeys.APP_PIN] = newPin
                                    it[DataStoreKeys.APP_LOCK] = true
                                }
                            }
                            showPinDialog = false; newPin = ""; confirmPin = ""; pinError = ""
                        }
                    }
                }) { Text("Save PIN", color = GymYellow, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; newPin = ""; confirmPin = ""; pinError = "" }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextMuted, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}

@Composable
fun GymSettingsToggle(title: String, subtitle: String, icon: ImageVector, iconBg: Color,
                      checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconBg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GymBgDark, checkedTrackColor = GymYellow,
                uncheckedThumbColor = TextSecondary, uncheckedTrackColor = GymBgBorder))
    }
}

@Composable
fun GymSettingsNavRow(title: String, subtitle: String, icon: ImageVector, iconBg: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconBg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
    }
}
