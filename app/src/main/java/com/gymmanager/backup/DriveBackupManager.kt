package com.gymmanager.backup

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.gymmanager.data.db.GymDatabase
import com.gymmanager.data.model.BackupLog
import com.gymmanager.data.repository.GymRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────
//  GOOGLE SIGN-IN HELPER
// ─────────────────────────────────────────────
object GoogleSignInHelper {
    private const val WEB_CLIENT_ID = "403580158347-0fjshabb6h6aguqumtmoetrg2l4pnm7r.apps.googleusercontent.com"

    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(WEB_CLIENT_ID)
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }

    fun getSignInClient(context: Context): GoogleSignInClient {
        return GoogleSignIn.getClient(context, getSignInOptions())
    }

    fun isSignedIn(context: Context): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    fun getSignInIntent(context: Context): Intent {
        return getSignInClient(context).signInIntent
    }

    suspend fun signOut(context: Context) = withContext(Dispatchers.IO) {
        getSignInClient(context).signOut()
    }
}

// ─────────────────────────────────────────────
//  DRIVE BACKUP MANAGER
// ─────────────────────────────────────────────
class DriveBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "DriveBackup"
        private const val BACKUP_FOLDER_NAME = "GymManagerPro_Backups"
        private const val DB_NAME = GymDatabase.DATABASE_NAME
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account ?: return null
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("GymManagerPro").build()
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun uploadBackup(repo: GymRepository): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext Result.failure(Exception("Database not found"))

            // Copy DB to backup file
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val backupName = "gym_backup_${sdf.format(Date())}.db"
            val backupFile = File(context.cacheDir, backupName)
            dbFile.copyTo(backupFile, overwrite = true)

            val drive = getDriveService()
                ?: return@withContext Result.failure(Exception("Not signed in to Google"))

            // Get or create AppData folder
            val folderId = getOrCreateFolder(drive)

            // Upload
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = backupName
                parents = listOf(folderId)
            }
            val mediaContent = FileContent("application/x-sqlite3", backupFile)
            val driveFile = drive.files().create(fileMetadata, mediaContent)
                .setFields("id,size,name")
                .execute()

            // Keep only last 5 backups
            pruneOldBackups(drive, folderId)

            // Log success
            repo.insertBackupLog(
                BackupLog(
                    driveFileId = driveFile.id,
                    sizeBytes = backupFile.length(),
                    isSuccess = true
                )
            )
            backupFile.delete()
            Log.d(TAG, "Backup uploaded: ${driveFile.id}")
            Result.success(driveFile.id)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun downloadLatestBackup(repo: GymRepository): Result<File> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService()
                ?: return@withContext Result.failure(Exception("Not signed in to Google"))

            val folderId = getOrCreateFolder(drive)
            val files = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setOrderBy("createdTime desc")
                .setPageSize(1)
                .setFields("files(id,name,size)")
                .execute().files

            if (files.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No backups found in Drive"))
            }

            val latestFile = files[0]
            val destFile = File(context.cacheDir, latestFile.name)
            FileOutputStream(destFile).use { out ->
                drive.files().get(latestFile.id).executeMediaAndDownloadTo(out)
            }
            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }

    fun restoreDatabase(backupFile: File): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            // Close existing connections handled by caller
            backupFile.copyTo(dbFile, overwrite = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restore copy failed", e)
            false
        }
    }

    private fun getOrCreateFolder(drive: Drive): String {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME' and trashed=false"
        val result = drive.files().list().setQ(query).setFields("files(id)").execute()
        if (result.files.isNotEmpty()) return result.files[0].id

        val folderMeta = com.google.api.services.drive.model.File().apply {
            name = BACKUP_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        return drive.files().create(folderMeta).setFields("id").execute().id
    }

    private fun pruneOldBackups(drive: Drive, folderId: String) {
        try {
            val files = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setOrderBy("createdTime desc")
                .setFields("files(id,name)")
                .execute().files ?: return
            files.drop(5).forEach { drive.files().delete(it.id).execute() }
        } catch (e: Exception) {
            Log.w(TAG, "Prune failed", e)
        }
    }
}

// ─────────────────────────────────────────────
//  WORKMANAGER – PERIODIC BACKUP
// ─────────────────────────────────────────────
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = GymDatabase.getInstance(applicationContext)
        val repo = GymRepository(
            db.gymInfoDao(), db.subscriptionPlanDao(), db.memberDao(),
            db.attendanceDao(), db.paymentDao(), db.expenseDao(), db.backupLogDao()
        )
        val manager = DriveBackupManager(applicationContext)
        if (!manager.isNetworkAvailable()) return Result.retry()
        if (!GoogleSignInHelper.isSignedIn(applicationContext)) return Result.failure()

        return when (val res = manager.uploadBackup(repo)) {
            is kotlin.Result -> if (res.isSuccess) Result.success() else Result.retry()
            else -> Result.retry()
        }
    }

    companion object {
        private const val WORK_TAG = "gym_auto_backup"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}

// ─────────────────────────────────────────────
//  BOOT RECEIVER
// ─────────────────────────────────────────────
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            AutoBackupWorker.schedule(context)
        }
    }
}

// ─────────────────────────────────────────────
//  BACKUP SERVICE (foreground for large DBs)
// ─────────────────────────────────────────────
class BackupService : android.app.Service() {
    override fun onBind(intent: android.content.Intent?) = null
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
}
