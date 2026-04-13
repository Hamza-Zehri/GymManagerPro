package com.gymmanager.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.gymmanager.data.db.GymDatabase
import com.gymmanager.data.model.BackupLog
import com.gymmanager.data.repository.GymRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DriveBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        private const val DB_NAME = GymDatabase.DATABASE_NAME
    }

    suspend fun createBackupFile(repo: GymRepository): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext Result.failure(Exception("Database not found"))

            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val backupName = "gym_backup_${sdf.format(Date())}.db"
            val backupFile = File(context.cacheDir, backupName)
            
            dbFile.copyTo(backupFile, overwrite = true)

            // Log the backup
            repo.insertBackupLog(
                BackupLog(
                    driveFileId = "local",
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

    fun getShareIntent(file: File): Intent {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/x-sqlite3"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
