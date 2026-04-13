package com.gymmanager.ui.screens

import android.app.Activity
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.gymmanager.backup.DriveBackupManager
import com.gymmanager.backup.GoogleSignInHelper
import com.gymmanager.data.model.BackupLog
import com.gymmanager.ui.components.*
import com.gymmanager.ui.theme.*
import com.gymmanager.utils.DateUtils
import com.gymmanager.viewmodel.GymViewModel
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

    var isSignedIn     by remember { mutableStateOf(GoogleSignInHelper.isSignedIn(context)) }
    var isLoading      by remember { mutableStateOf(false) }
    var statusMessage  by remember { mutableStateOf<String?>(null) }
    var isError        by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val driveManager = remember { DriveBackupManager(context) }

    // Google sign-in launcher
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                isSignedIn = true
                statusMessage = "Signed in to Google successfully"
                isError = false
            } catch (e: ApiException) {
                statusMessage = "Sign-in failed: ${e.message}"
                isError = true
            }
        }
    }

    fun signIn() { signInLauncher.launch(GoogleSignInHelper.getSignInIntent(context)) }

    fun backup() = scope.launch {
        isLoading = true
        statusMessage = "Preparing database for backup…"
        isError = false
        try {
            val res = driveManager.uploadBackup(vm.repo)
            isLoading = false
            if (res.isSuccess) {
                statusMessage = "✅ Backup uploaded successfully to Google Drive"
                isError = false
            } else {
                val error = res.exceptionOrNull()
                statusMessage = "❌ Backup failed: ${error?.message ?: "Unknown error"}"
                isError = true
                error?.printStackTrace()
            }
        } catch (e: Exception) {
            isLoading = false
            statusMessage = "❌ Critical failure: ${e.message}"
            isError = true
            e.printStackTrace()
        }
    }

    fun restore() = scope.launch {
        isLoading = true
        statusMessage = "Searching for latest backup on Drive…"
        isError = false
        try {
            val res = driveManager.downloadLatestBackup(vm.repo)
            if (res.isSuccess) {
                statusMessage = "📥 Download complete. Applying changes…"
                val file = res.getOrThrow()
                val ok = driveManager.restoreDatabase(file)
                isLoading = false
                if (ok) {
                    statusMessage = "✅ Restore successful! Please close and REOPEN the app to see your data."
                    isError = false
                } else {
                    statusMessage = "❌ Failed to overwrite database file. Try manually."
                    isError = true
                }
            } else {
                isLoading = false
                val error = res.exceptionOrNull()
                statusMessage = "❌ Download failed: ${error?.message ?: "No backups found"}"
                isError = true
            }
        } catch (e: Exception) {
            isLoading = false
            statusMessage = "❌ Restore error: ${e.message}"
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
                            Text("Google Drive Backup", style = MaterialTheme.typography.titleSmall,
                                color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Your gym data is automatically backed up to Google Drive when internet is available. " +
                                "If you uninstall and reinstall the app, sign in with the same Google account and restore your data.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF93C5FD)
                            )
                        }
                    }
                }
            }

            // ── GOOGLE ACCOUNT ──────────────────
            item {
                Surface(color = Zinc900, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("GOOGLE ACCOUNT", style = MaterialTheme.typography.labelSmall, color = Zinc400,
                            modifier = Modifier.padding(bottom = 12.dp))
                        if (isSignedIn) {
                            val account = GoogleSignIn.getLastSignedInAccount(context)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1A10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountCircle, null, tint = Emerald400, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(account?.displayName ?: "Google Account",
                                        color = Color.White, style = MaterialTheme.typography.titleSmall)
                                    Text(account?.email ?: "", color = Zinc400, style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(color = Color(0x1A10B981), shape = RoundedCornerShape(8.dp)) {
                                    Text("Connected", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall, color = Emerald400)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        GoogleSignInHelper.signOut(context)
                                        isSignedIn = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Zinc700)
                            ) {
                                Icon(Icons.Default.Logout, null, tint = Zinc400, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sign Out", color = Zinc400)
                            }
                        } else {
                            PrimaryButton(
                                text = "Sign in with Google",
                                onClick = { signIn() },
                                icon = Icons.Default.AccountCircle,
                                color = Blue500
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
                    onClick = { if (isSignedIn) backup() else signIn() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text("Backup to Google Drive", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(if (isSignedIn) "Upload current data to Drive" else "Sign in first",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // ── RESTORE BUTTON ──────────────────
            item {
                Button(
                    onClick = { if (isSignedIn) showRestoreConfirm = true else signIn() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Zinc800)
                ) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(24.dp), tint = Color(0xFF93C5FD))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Restore from Google Drive", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Recover data from latest backup",
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
                TextButton(onClick = { showRestoreConfirm = false; restore() }) {
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

