package com.hamzaasad.gymmanagerpro.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.hamzaasad.gymmanagerpro.data.db.GymDatabase
import com.hamzaasad.gymmanagerpro.data.model.BackupLog
import com.hamzaasad.gymmanagerpro.data.repository.GymRepository
import com.hamzaasad.gymmanagerpro.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
            val imagesDir = File(context.filesDir, "images")
            
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

            // 4. Create new backup ZIP (Data + Photos)
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val backupName = "gym_backup_full_${sdf.format(Date())}.zip"
            val backupFile = File(gymBackupDir, backupName)
            
            FileUtils.zipDirectory(imagesDir, dbFile, backupFile)

            // Log the backup
            repo.insertBackupLog(
                BackupLog(
                    driveFileId = "local_downloads_zip",
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
            val imagesDir = File(context.filesDir, "images")
            
            // Important: Room expects the database to be closed during replacement
            // In a production app, we would normally restart the app or use a trigger.
            // Here we overwrite and the next DB access will use the new one.
            
            FileUtils.unzipToDirectory(uri, context, dbFile, imagesDir)

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
}
