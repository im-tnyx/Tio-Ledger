package com.tioledger.apps.android.reminders

sealed interface ReminderReconciliationOperation {
    val identityKey: String

    data class Schedule(
        val payload: ReminderWorkPayload,
    ) : ReminderReconciliationOperation {
        override val identityKey: String = payload.identityKey
    }

    data class Replace(
        val payload: ReminderWorkPayload,
    ) : ReminderReconciliationOperation {
        override val identityKey: String = payload.identityKey
    }

    data class Cancel(
        val record: ScheduledReminderRecord,
    ) : ReminderReconciliationOperation {
        override val identityKey: String = record.identityKey
    }
}

sealed interface ReminderReconciliationResult {
    data class Success(
        val operations: List<ReminderReconciliationOperation>,
    ) : ReminderReconciliationResult

    data class DuplicateDesiredIdentity(
        val identityKey: String,
    ) : ReminderReconciliationResult

    data class DuplicateScheduledIdentity(
        val identityKey: String,
    ) : ReminderReconciliationResult
}

class ReminderReconciliationPlanner {
    @Suppress("ReturnCount")
    fun plan(
        desired: List<ReminderWorkPayload>,
        scheduled: List<ScheduledReminderRecord>,
    ): ReminderReconciliationResult {
        duplicateIdentity(desired.map(ReminderWorkPayload::identityKey))?.let { identityKey ->
            return ReminderReconciliationResult.DuplicateDesiredIdentity(identityKey)
        }
        duplicateIdentity(scheduled.map(ScheduledReminderRecord::identityKey))?.let { identityKey ->
            return ReminderReconciliationResult.DuplicateScheduledIdentity(identityKey)
        }

        val desiredByIdentity = desired.associateBy(ReminderWorkPayload::identityKey)
        val scheduledByIdentity = scheduled.associateBy(ScheduledReminderRecord::identityKey)
        val operations = mutableListOf<ReminderReconciliationOperation>()

        scheduledByIdentity.values
            .asSequence()
            .filter { record -> record.identityKey !in desiredByIdentity }
            .sortedBy(ScheduledReminderRecord::identityKey)
            .forEach { record -> operations += ReminderReconciliationOperation.Cancel(record) }

        desiredByIdentity.values
            .sortedBy(ReminderWorkPayload::identityKey)
            .forEach { payload ->
                val current = scheduledByIdentity[payload.identityKey]
                when {
                    current == null -> operations += ReminderReconciliationOperation.Schedule(payload)
                    current.requiresReplacement(payload) -> {
                        operations += ReminderReconciliationOperation.Replace(payload)
                    }
                }
            }

        return ReminderReconciliationResult.Success(operations)
    }

    private fun ScheduledReminderRecord.requiresReplacement(payload: ReminderWorkPayload): Boolean =
        type != payload.type ||
            deliveryTimestamp != payload.deliveryTimestamp ||
            payloadFingerprint != payload.payloadFingerprint

    private fun duplicateIdentity(identityKeys: List<String>): String? {
        val seen = mutableSetOf<String>()
        return identityKeys.firstOrNull { identityKey -> !seen.add(identityKey) }
    }
}
