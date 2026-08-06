package com.tioledger.apps.android.reminders

import com.tioledger.application.usecase.notification.ReminderContentView
import com.tioledger.application.usecase.notification.ReminderDestinationView
import com.tioledger.application.usecase.notification.ReminderPlanTypeView
import com.tioledger.application.usecase.notification.ReminderPlanView
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderReconciliationPlannerTest {
    private val planner = ReminderReconciliationPlanner()

    @Test
    fun schedulesMissingAndCancelsStaleRecordsDeterministically() {
        val desiredB = emiPayload(identityKey = "emi|b", deliveryTimestamp = 200L)
        val desiredA = emiPayload(identityKey = "emi|a", deliveryTimestamp = 100L)
        val stale = scheduled(identityKey = "emi|stale", deliveryTimestamp = 50L)

        val result = planner.plan(desired = listOf(desiredB, desiredA), scheduled = listOf(stale))

        assertEquals(
            listOf(
                ReminderReconciliationOperation.Cancel(stale),
                ReminderReconciliationOperation.Schedule(desiredA),
                ReminderReconciliationOperation.Schedule(desiredB),
            ),
            (result as ReminderReconciliationResult.Success).operations,
        )
    }

    @Test
    fun identicalDesiredAndScheduledRecordIsNoOp() {
        val payload = emiPayload(identityKey = "emi|same", deliveryTimestamp = 100L)

        val result = planner.plan(listOf(payload), listOf(payload.toScheduledRecord()))

        assertEquals(
            emptyList<ReminderReconciliationOperation>(),
            (result as ReminderReconciliationResult.Success).operations,
        )
    }

    @Test
    fun changedTimeOrSemanticPayloadReplacesExistingWork() {
        val original = emiPayload(identityKey = "emi|replace", deliveryTimestamp = 100L)
        val changedTime = original.copy(deliveryTimestamp = 200L)
        val changedContent =
            original.copy(
                content =
                    AndroidReminderContent.Emi(
                        loanName = "Renamed loan",
                        dueDate = 300L,
                        payment = AndroidMoneyPayload(12_345L, "INR"),
                    ),
            )

        val timeResult = planner.plan(listOf(changedTime), listOf(original.toScheduledRecord()))
        val contentResult = planner.plan(listOf(changedContent), listOf(original.toScheduledRecord()))

        assertEquals(
            listOf(ReminderReconciliationOperation.Replace(changedTime)),
            (timeResult as ReminderReconciliationResult.Success).operations,
        )
        assertEquals(
            listOf(ReminderReconciliationOperation.Replace(changedContent)),
            (contentResult as ReminderReconciliationResult.Success).operations,
        )
    }

    @Test
    fun duplicateDesiredIdentityFailsClosed() {
        val first = emiPayload(identityKey = "emi|duplicate", deliveryTimestamp = 100L)
        val second = first.copy(deliveryTimestamp = 200L)

        val result = planner.plan(listOf(first, second), emptyList())

        assertEquals(
            ReminderReconciliationResult.DuplicateDesiredIdentity("emi|duplicate"),
            result,
        )
    }

    @Test
    fun applicationPlanMappingPreservesPreciseMoneyAndStablePlatformIdentity() {
        val original =
            ReminderPlanView(
                identityKey = "emi|loan-1|installment-1|3",
                type = ReminderPlanTypeView.EMI,
                deliveryTimestamp = 100L,
                timeZoneId = "Asia/Kolkata",
                destination = ReminderDestinationView.LoanDetails("loan-1"),
                content =
                    ReminderContentView.Emi(
                        loanName = "Home loan",
                        dueDate = 500L,
                        payment = Money(12_345L, CurrencyCode("INR")),
                    ),
            ).toAndroidPayload()
        val revised = original.copy(deliveryTimestamp = 200L)

        assertEquals(
            AndroidMoneyPayload(12_345L, "INR"),
            (original.content as AndroidReminderContent.Emi).payment,
        )
        assertEquals(AndroidReminderDestination.LoanDetails("loan-1"), original.destination)
        assertEquals(original.uniqueWorkName, revised.uniqueWorkName)
        assertEquals(original.notificationId, revised.notificationId)
        assertNotEquals(original.payloadFingerprint, revised.payloadFingerprint)
        assertTrue(original.notificationId > 0)
    }

    private fun emiPayload(
        identityKey: String,
        deliveryTimestamp: Long,
    ): ReminderWorkPayload =
        ReminderWorkPayload(
            identityKey = identityKey,
            type = AndroidReminderType.EMI,
            deliveryTimestamp = deliveryTimestamp,
            timeZoneId = "UTC",
            destination = AndroidReminderDestination.LoanDetails("loan-1"),
            content =
                AndroidReminderContent.Emi(
                    loanName = "Home loan",
                    dueDate = 300L,
                    payment = AndroidMoneyPayload(12_345L, "INR"),
                ),
        )

    private fun scheduled(
        identityKey: String,
        deliveryTimestamp: Long,
    ): ScheduledReminderRecord =
        ScheduledReminderRecord(
            identityKey = identityKey,
            type = AndroidReminderType.EMI,
            deliveryTimestamp = deliveryTimestamp,
            payloadFingerprint = "stale-fingerprint",
        )
}
