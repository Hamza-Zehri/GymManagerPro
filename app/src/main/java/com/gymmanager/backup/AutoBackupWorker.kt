package com.gymmanager.backup

import android.content.Context
import androidx.work.*
import com.gymmanager.data.db.GymDatabase
import com.gymmanager.data.repository.GymRepository
import com.gymmanager.DataStoreKeys
import com.gymmanager.dataStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val appSettings = applicationContext.dataStore.data.first()
        val autoBackupEnabled = appSettings[DataStoreKeys.AUTO_BACKUP] ?: false

        if (!autoBackupEnabled) return Result.success()

        val database = GymDatabase.getInstance(applicationContext)
        val repository = GymRepository(
            database.gymInfoDao(),
            database.subscriptionPlanDao(),
            database.memberDao(),
            database.attendanceDao(),
            database.paymentDao(),
            database.expenseDao(),
            database.backupLogDao()
        )
        val backupManager = DriveBackupManager(applicationContext)

        val result = backupManager.createBackupFile(repository)
        
        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_auto_backup"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
