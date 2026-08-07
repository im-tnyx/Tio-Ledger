package com.tioledger.apps.android.reminders

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.notification.PlanRemindersCommand
import com.tioledger.application.usecase.notification.PlanRemindersUseCase
import java.time.ZoneId

sealed interface AndroidReminderReconcileResult {
    data class Success(
        val operationCount: Int,
    ) : AndroidReminderReconcileResult

    data class RetryableFailure(
        val error: ApplicationError?,
        val reason: String,
    ) : AndroidReminderReconcileResult

    data class InvalidState(
        val reason: String,
    ) : AndroidReminderReconcileResult
}

class AndroidReminderReconciler(
    private val planRemindersUseCase: PlanRemindersUseCase,
    private val stateStore: ReminderPlatformStateStore,
    private val receiptStore: BudgetReminderReceiptStore,
    private val scheduledStore: ScheduledReminderStore,
    private val permissionController: AndroidNotificationPermissionController,
    private val reconciliationPlanner: ReminderReconciliationPlanner,
    private val workOperationApplier: ReminderWorkOperationApplier,
    private val clock: AndroidReminderClock,
) {
    fun reconcile(): AndroidReminderReconcileResult {
        val desiredResult = desiredPayloads(stateStore.reminderPreferences())
        val desired =
            when (desiredResult) {
                is DesiredPayloadResult.Success -> desiredResult.payloads
                is DesiredPayloadResult.Failure -> return desiredResult.result
            }
        val reconciliation = reconciliationPlanner.plan(desired, scheduledStore.records())
        val operations =
            when (reconciliation) {
                is ReminderReconciliationResult.Success -> reconciliation.operations
                is ReminderReconciliationResult.DuplicateDesiredIdentity -> {
                    return AndroidReminderReconcileResult.InvalidState(
                        "Duplicate desired reminder identity: ${reconciliation.identityKey}",
                    )
                }

                is ReminderReconciliationResult.DuplicateScheduledIdentity -> {
                    return AndroidReminderReconcileResult.InvalidState(
                        "Duplicate scheduled reminder identity: ${reconciliation.identityKey}",
                    )
                }
            }

        return when (val result = workOperationApplier.apply(operations)) {
            ReminderScheduleApplicationResult.Success ->
                AndroidReminderReconcileResult.Success(operations.size)

            is ReminderScheduleApplicationResult.Failure ->
                AndroidReminderReconcileResult.RetryableFailure(
                    error = null,
                    reason =
                        "Unable to apply ${result.operation} for reminder ${result.identityKey}",
                )
        }
    }

    private fun desiredPayloads(preferences: AndroidReminderPreferences): DesiredPayloadResult {
        if (!preferences.anyEnabled() || !permissionController.canPostNotifications()) {
            return DesiredPayloadResult.Success(emptyList())
        }
        return when (
            val result =
                planRemindersUseCase(
                    PlanRemindersCommand(
                        currentTimestamp = clock.currentTimeMillis(),
                        timeZoneId = ZoneId.systemDefault().id,
                        emiRemindersEnabled = preferences.emiRemindersEnabled,
                        budgetRemindersEnabled = preferences.budgetRemindersEnabled,
                        deliveredBudgetIdentityKeys = receiptStore.deliveredIdentityKeys(),
                    ),
                )
        ) {
            is ApplicationResult.Success ->
                DesiredPayloadResult.Success(
                    result.outcome.value.map { plan -> plan.toAndroidPayload() },
                )

            is ApplicationResult.Failure ->
                DesiredPayloadResult.Failure(
                    AndroidReminderReconcileResult.RetryableFailure(
                        error = result.error,
                        reason = "Unable to load desired reminder plans",
                    ),
                )
        }
    }

    private fun AndroidReminderPreferences.anyEnabled(): Boolean =
        emiRemindersEnabled || budgetRemindersEnabled

    private sealed interface DesiredPayloadResult {
        data class Success(
            val payloads: List<ReminderWorkPayload>,
        ) : DesiredPayloadResult

        data class Failure(
            val result: AndroidReminderReconcileResult,
        ) : DesiredPayloadResult
    }
}
