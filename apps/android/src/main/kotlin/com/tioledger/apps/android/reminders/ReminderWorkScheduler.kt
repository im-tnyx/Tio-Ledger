package com.tioledger.apps.android.reminders

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun interface AndroidReminderClock {
    fun currentTimeMillis(): Long
}

sealed interface ReminderScheduleApplicationResult {
    data object Success : ReminderScheduleApplicationResult

    data class Failure(
        val identityKey: String,
        val operation: String,
    ) : ReminderScheduleApplicationResult
}

interface ReminderWorkOperationApplier {
    fun apply(operations: List<ReminderReconciliationOperation>): ReminderScheduleApplicationResult

    fun cancelType(type: AndroidReminderType): ReminderScheduleApplicationResult
}

class AndroidReminderWorkScheduler(
    private val workManager: WorkManager,
    private val scheduledStore: ScheduledReminderStore,
    private val clock: AndroidReminderClock,
) : ReminderWorkOperationApplier {
    override fun apply(operations: List<ReminderReconciliationOperation>): ReminderScheduleApplicationResult {
        operations.forEach { operation ->
            val applied =
                when (operation) {
                    is ReminderReconciliationOperation.Schedule -> schedule(operation.payload)
                    is ReminderReconciliationOperation.Replace -> schedule(operation.payload)
                    is ReminderReconciliationOperation.Cancel -> cancel(operation.record)
                }
            if (!applied) {
                return ReminderScheduleApplicationResult.Failure(
                    identityKey = operation.identityKey,
                    operation = operation.operationName(),
                )
            }
        }
        return ReminderScheduleApplicationResult.Success
    }

    override fun cancelType(type: AndroidReminderType): ReminderScheduleApplicationResult =
        apply(
            scheduledStore.records()
                .filter { record -> record.type == type }
                .map(ReminderReconciliationOperation::Cancel),
        )

    private fun schedule(payload: ReminderWorkPayload): Boolean =
        runCatching {
            val delayMillis = (payload.deliveryTimestamp - clock.currentTimeMillis()).coerceAtLeast(0L)
            val request =
                OneTimeWorkRequestBuilder<ReminderDeliveryWorker>()
                    .setInputData(payload.toWorkData())
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .addTag(ALL_REMINDERS_TAG)
                    .addTag(payload.type.workTag())
                    .build()
            workManager
                .enqueueUniqueWork(payload.uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
                .result
                .get()
            check(scheduledStore.put(payload.toScheduledRecord())) {
                "Unable to persist scheduled reminder snapshot"
            }
        }.isSuccess

    private fun cancel(record: ScheduledReminderRecord): Boolean =
        runCatching {
            workManager
                .cancelUniqueWork(reminderUniqueWorkName(record.identityKey))
                .result
                .get()
            check(scheduledStore.remove(record.identityKey)) {
                "Unable to remove scheduled reminder snapshot"
            }
        }.isSuccess

    private fun ReminderReconciliationOperation.operationName(): String =
        when (this) {
            is ReminderReconciliationOperation.Schedule -> "schedule"
            is ReminderReconciliationOperation.Replace -> "replace"
            is ReminderReconciliationOperation.Cancel -> "cancel"
        }
}

private fun AndroidReminderType.workTag(): String =
    when (this) {
        AndroidReminderType.EMI -> EMI_REMINDERS_TAG
        AndroidReminderType.BUDGET -> BUDGET_REMINDERS_TAG
    }

private const val ALL_REMINDERS_TAG = "tio-reminders-v1"
private const val EMI_REMINDERS_TAG = "tio-reminders-v1-emi"
private const val BUDGET_REMINDERS_TAG = "tio-reminders-v1-budget"
