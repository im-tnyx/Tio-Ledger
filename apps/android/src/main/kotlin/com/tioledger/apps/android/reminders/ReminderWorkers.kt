package com.tioledger.apps.android.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tioledger.apps.android.TioAndroidApplication
import java.util.concurrent.TimeUnit

enum class ReminderReconciliationReason {
    APP_START,
    BOOT_COMPLETED,
    TIME_ZONE_CHANGED,
    PACKAGE_REPLACED,
    PREFERENCES_CHANGED,
    PERMISSION_CHANGED,
    RELEVANT_DATA_CHANGED,
}

object ReminderReconciliationEnqueuer {
    fun enqueue(
        context: Context,
        reason: ReminderReconciliationReason,
    ) {
        val request =
            OneTimeWorkRequestBuilder<ReminderReconciliationWorker>()
                .setInputData(workDataOf(KEY_RECONCILIATION_REASON to reason.name))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MINIMUM_BACKOFF_SECONDS, TimeUnit.SECONDS)
                .addTag(RECONCILIATION_TAG)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RECONCILIATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

class ReminderReconciliationWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val application = applicationContext as? TioAndroidApplication ?: return Result.failure()
        return when (application.koinApplication.koin.get<AndroidReminderReconciler>().reconcile()) {
            is AndroidReminderReconcileResult.Success -> Result.success()
            is AndroidReminderReconcileResult.RetryableFailure -> Result.retry()
            is AndroidReminderReconcileResult.InvalidState -> Result.failure()
        }
    }
}

class ReminderDeliveryWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val application = applicationContext as? TioAndroidApplication ?: return Result.failure()
        val payload = inputData.toReminderWorkPayloadOrNull() ?: return Result.failure()
        val koin = application.koinApplication.koin
        val scheduledStore = koin.get<ScheduledReminderStore>()
        val permissionController = koin.get<AndroidNotificationPermissionController>()
        if (!permissionController.canPostNotifications()) {
            return removeScheduledOrRetry(scheduledStore, payload.identityKey)
        }

        val receiptStore = koin.get<BudgetReminderReceiptStore>()
        if (payload.isDeliveredBudget(receiptStore)) {
            return removeScheduledOrRetry(scheduledStore, payload.identityKey)
        }

        val publisher = koin.get<AndroidReminderNotificationPublisher>()
        if (!publisher.publish(payload)) return Result.retry()
        if (
            payload.type == AndroidReminderType.BUDGET &&
            !receiptStore.recordDelivered(
                identityKey = payload.identityKey,
                deliveredAt = koin.get<AndroidReminderClock>().currentTimeMillis(),
            )
        ) {
            return Result.retry()
        }
        return removeScheduledOrRetry(scheduledStore, payload.identityKey)
    }

    private fun ReminderWorkPayload.isDeliveredBudget(receiptStore: BudgetReminderReceiptStore): Boolean =
        type == AndroidReminderType.BUDGET && identityKey in receiptStore.deliveredIdentityKeys()

    private fun removeScheduledOrRetry(
        scheduledStore: ScheduledReminderStore,
        identityKey: String,
    ): Result =
        if (scheduledStore.remove(identityKey)) {
            Result.success()
        } else {
            Result.retry()
        }
}

class ReminderReconciliationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val reason =
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> ReminderReconciliationReason.BOOT_COMPLETED
                Intent.ACTION_TIMEZONE_CHANGED -> ReminderReconciliationReason.TIME_ZONE_CHANGED
                Intent.ACTION_MY_PACKAGE_REPLACED -> ReminderReconciliationReason.PACKAGE_REPLACED
                else -> return
            }
        ReminderReconciliationEnqueuer.enqueue(context, reason)
    }
}

private const val RECONCILIATION_WORK_NAME = "tio-reminder-reconciliation-v1"
private const val RECONCILIATION_TAG = "tio-reminder-reconciliation-v1"
private const val KEY_RECONCILIATION_REASON = "reconciliation_reason"
private const val MINIMUM_BACKOFF_SECONDS = 30L
