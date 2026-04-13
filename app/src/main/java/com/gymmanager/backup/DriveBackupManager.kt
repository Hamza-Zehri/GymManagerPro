package com.gymmanager.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.gymmanager.data.db.GymDatabase
import com.gymmanager.data.model.BackupLog
import com.gymmanager.data.repository.GymRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DriveBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        private const val DB_NAME = GymDatabase.DATABASE_NAME
        private const val BACKUP_DIR_NAME = "GymBackup"
    }

    suspend fun createBackupFile(repo: GymRepository): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext Result.failure(Exception("Database not found"))

            // 1. Get Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val gymBackupDir = File(downloadsDir, BACKUP_DIR_NAME)
            
            // 2. Create directory if not exists
            if (!gymBackupDir.exists()) {
                gymBackupDir.mkdirs()
            }

            // 3. Delete old backups (keep only the latest)
            gymBackupDir.listFiles()?.forEach { it.delete() }

            // 4. Create new backup
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val backupName = "gym_backup_${sdf.format(Date())}.db"
            val backupFile = File(gymBackupDir, backupName)
            
            dbFile.copyTo(backupFile, overwrite = true)

            // Log the backup
            repo.insertBackupLog(
                BackupLog(
                    driveFileId = "local_downloads",
                    sizeBytes = backupFile.length(),
                    isSuccess = true
                )
            )
            
            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Backup creation failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreFromUri(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
}
