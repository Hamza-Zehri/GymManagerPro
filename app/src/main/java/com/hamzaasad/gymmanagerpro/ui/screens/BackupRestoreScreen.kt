package com.hamzaasad.gymmanagerpro.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamzaasad.gymmanagerpro.backup.DriveBackupManager
import com.hamzaasad.gymmanagerpro.data.model.BackupLog
import com.hamzaasad.gymmanagerpro.ui.components.*
import com.hamzaasad.gymmanagerpro.ui.theme.*
import com.hamzaasad.gymmanagerpro.utils.DateUtils
import com.hamzaasad.gymmanagerpro.viewmodel.GymViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    vm: GymViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupHistory by vm.backupHistory.collectAsState()

    var isLoading      by remember { mutableStateOf(false) }
    var statusMessage  by remember { mutableStateOf<String?>(null) }
    var isError        by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val driveManager = remember { DriveBackupManager(context) }

    // File picker for restore
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                statusMessage = "Restoring database from file..."
                val res = driveManager.restoreFromUri(it)
                isLoading = false
                if (res.isSuccess) {
                    statusMessage = "✅ Restore successful! Restart the app to see changes."
                    isError = false
                } else {
                    statusMessage = "❌ Restore failed: ${res.exceptionOrNull()?.message}"
                    isError = true
                }
            }
        }
    }

    fun backup() = scope.launch {
        isLoading = true
        statusMessage = "Creating backup file…"
        isError = false
        try {
            val res = driveManager.createBackupFile(vm.repo)
            isLoading = false
            if (res.isSuccess) {
                statusMessage = "✅ Backup saved to Downloads/GymBackup folder! Old backups were removed."
                isError = false
            } else {
                statusMessage = "❌ Backup failed: ${res.exceptionOrNull()?.message}"
                isError = true
            }
        } catch (e: Exception) {
            isLoading = false
            statusMessage = "❌ Error: ${e.message}"
            isError = true
        }
    }

    Column(Modifier.fillMaxSize().background(Zinc950)) {
        GymTopBar(title = "Backup & Restore", subtitle = "Protect your data", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── INFO BANNER ─────────────────────
            item {
                Surface(
                    color = Color(0x1A3B82F6),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Cloud, null, tint = Blue500, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Manual Backup", style = MaterialTheme.typography.titleSmall,
                                color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Create a backup file and save it to your Google Drive, WhatsApp, or Email. " +
                                "To recover data, download the file to your phone and use the Restore option.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF93C5FD)
                            )
                        }
                    }
                }
            }

            // ── STATUS MESSAGE ──────────────────
            statusMessage?.let { msg ->
                item {
                    Surface(
                        color = if (isError) Color(0x1AF43F5E) else Color(0x1A10B981),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                null, tint = if (isError) Rose400 else Emerald400,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(msg, color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ── BACKUP BUTTON ───────────────────
            item {
                Button(
                    onClick = { backup() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text("Create Backup", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Save to Downloads/GymBackup",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // ── RESTORE BUTTON ──────────────────
            item {
                Button(
                    onClick = { showRestoreConfirm = true },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800)
                ) {
                    Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(24.dp), tint = Color(0xFF93C5FD))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Restore from File", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Select a backup file from your phone",
                            style = MaterialTheme.typography.labelSmall, color = Zinc400)
                    }
                }
            }

            // ── WARNING ─────────────────────────
            item {
                Surface(color = Color(0x1AF59E0B), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Amber500, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Restoring will replace all current data. Always backup first before restoring.",
                            style = MaterialTheme.typography.labelSmall, color = Color(0xFFFDE68A)
                        )
                    }
                }
            }

            // ── BACKUP HISTORY ──────────────────
            if (backupHistory.isNotEmpty()) {
                item {
                    Text("BACKUP HISTORY", style = MaterialTheme.typography.labelSmall, color = Zinc400,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(backupHistory, key = { it.id }) { log ->
                    BackupHistoryRow(log)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Restore confirm dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            containerColor = Zinc900,
            title = { Text("Restore Data?", color = Color.White) },
            text = { Text("This will replace all current gym data with the latest backup from Google Drive. This action cannot be undone.", color = Zinc400) },
            confirmButton = {
                TextButton(onClick = { showRestoreConfirm = false; filePickerLauncher.launch("*/*") }) {
                    Text("Restore", color = Amber500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel", color = Zinc400)
                }
            }
        )
    }
}

@Composable
private fun BackupHistoryRow(log: BackupLog) {
    Surface(color = Zinc900, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                null,
                tint = if (log.isSuccess) Emerald400 else Rose400,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(DateUtils.displayDate(log.timestamp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text(DateUtils.displayTime(log.timestamp), color = Zinc400, style = MaterialTheme.typography.labelSmall)
            }
            if (log.sizeBytes > 0) {
                Text("${log.sizeBytes / 1024} KB", color = Zinc400, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

