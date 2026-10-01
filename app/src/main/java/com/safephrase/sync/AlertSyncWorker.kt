package com.safephrase.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.safephrase.domain.usecase.SyncEmergencyEventsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.safephrase.core.util.SafeLogger
import java.util.concurrent.TimeUnit

private const val TAG = "AlertSyncWorker"

@HiltWorker
class AlertSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncUseCase: SyncEmergencyEventsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        SafeLogger.i(TAG, "Sync started (attempt ${runAttemptCount + 1})")
        return try {
            val synced = syncUseCase()
            SafeLogger.i(TAG, "Sync complete — $synced event(s) sent")
            Result.success()
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Sync failed", e)
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_TAG = "AlertSyncWorker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request = OneTimeWorkRequestBuilder<AlertSyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(WORK_TAG)
            .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request)

            SafeLogger.i(TAG, "AlertSyncWorker enqueued")
        }
    }
}
