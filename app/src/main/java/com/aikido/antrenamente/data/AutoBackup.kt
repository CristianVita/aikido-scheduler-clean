package com.aikido.antrenamente.data

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration

/**
 * Once a week, overwrites a user-chosen file (typically on Google Drive) with the current data. The
 * file has the same format as a manual backup, so it restores the same way.
 */
object AutoBackup {
    private const val WORK_NAME = "auto-backup-weekly"
    private val INTERVAL: Duration = Duration.ofDays(7)

    /** Starts the weekly schedule. WorkManager runs the first copy as soon as there is a network. */
    fun start(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(INTERVAL)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class BackupWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = Store.get(applicationContext)
        val uri = store.backupStatus.value.uri ?: return Result.success()
        return try {
            applicationContext.contentResolver.openOutputStream(Uri.parse(uri), "wt")!!
                .bufferedWriter()
                .use { it.write(store.exportBackup()) }
            store.recordBackupResult(success = true)
            Result.success()
        } catch (_: Exception) {
            store.recordBackupResult(success = false)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
