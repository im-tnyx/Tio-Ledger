package com.tioledger.apps.android.reminders

import androidx.work.Data
import com.tioledger.application.usecase.notification.BudgetReminderStatusView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderWorkDataTest {
    @Test
    fun emiPayloadRoundTripsWithoutMoneyConversion() {
        val payload =
            ReminderWorkPayload(
                identityKey = "emi|loan-1|installment-1|3",
                type = AndroidReminderType.EMI,
                deliveryTimestamp = 123_456L,
                timeZoneId = "Asia/Kolkata",
                destination = AndroidReminderDestination.LoanDetails("loan-1"),
                content =
                    AndroidReminderContent.Emi(
                        loanName = "Home loan",
                        dueDate = 654_321L,
                        payment = AndroidMoneyPayload(12_345L, "INR"),
                    ),
            )

        assertEquals(payload, payload.toWorkData().toReminderWorkPayloadOrNull())
    }

    @Test
    fun eligibleBudgetPayloadRoundTrips() {
        val payload =
            ReminderWorkPayload(
                identityKey = "budget|budget-1|period|WARNING",
                type = AndroidReminderType.BUDGET,
                deliveryTimestamp = 123_456L,
                timeZoneId = "UTC",
                destination = AndroidReminderDestination.Budgets,
                content =
                    AndroidReminderContent.Budget(
                        budgetName = "Food",
                        status = BudgetReminderStatusView.WARNING,
                        target = AndroidMoneyPayload(10_000L, "INR"),
                        spent = AndroidMoneyPayload(8_000L, "INR"),
                        remaining = AndroidMoneyPayload(2_000L, "INR"),
                    ),
            )

        assertEquals(payload, payload.toWorkData().toReminderWorkPayloadOrNull())
    }

    @Test
    fun malformedOrIneligibleDataFailsClosed() {
        val missingIdentity = Data.Builder().putString("type", AndroidReminderType.EMI.name).build()
        val onTrack =
            ReminderWorkPayload(
                identityKey = "budget|budget-1|period|ON_TRACK",
                type = AndroidReminderType.BUDGET,
                deliveryTimestamp = 1L,
                timeZoneId = "UTC",
                destination = AndroidReminderDestination.Budgets,
                content =
                    AndroidReminderContent.Budget(
                        budgetName = "Food",
                        status = BudgetReminderStatusView.ON_TRACK,
                        target = AndroidMoneyPayload(10_000L, "INR"),
                        spent = AndroidMoneyPayload(1_000L, "INR"),
                        remaining = AndroidMoneyPayload(9_000L, "INR"),
                    ),
            ).toWorkData()

        assertNull(missingIdentity.toReminderWorkPayloadOrNull())
        assertNull(onTrack.toReminderWorkPayloadOrNull())
    }
}
