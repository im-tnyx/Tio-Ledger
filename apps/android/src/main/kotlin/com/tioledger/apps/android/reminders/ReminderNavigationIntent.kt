package com.tioledger.apps.android.reminders

import android.content.Context
import android.content.Intent
import com.tioledger.apps.android.MainActivity
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute

object ReminderNavigationIntent {
    fun create(
        context: Context,
        payload: ReminderWorkPayload,
    ): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_REMINDER)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_DESTINATION, payload.destination.destinationValue())
            .apply {
                val destination = payload.destination
                if (destination is AndroidReminderDestination.LoanDetails) {
                    putExtra(EXTRA_LOAN_ID, destination.loanId)
                }
            }

    fun routeOrNull(intent: Intent?): RootRoute? {
        if (intent?.action != ACTION_OPEN_REMINDER) return null
        return when (intent.getStringExtra(EXTRA_DESTINATION)) {
            DESTINATION_LOAN ->
                intent.getStringExtra(EXTRA_LOAN_ID)
                    ?.takeIf(String::isNotBlank)
                    ?.let { loanId -> RootRoute.Main(MainRoute.LoanDetails(loanId)) }

            DESTINATION_BUDGETS -> RootRoute.Main(MainRoute.Budgets)
            else -> null
        }
    }

    private fun AndroidReminderDestination.destinationValue(): String =
        when (this) {
            is AndroidReminderDestination.LoanDetails -> DESTINATION_LOAN
            AndroidReminderDestination.Budgets -> DESTINATION_BUDGETS
        }

    private const val ACTION_OPEN_REMINDER = "com.tioledger.apps.android.action.OPEN_REMINDER"
    private const val EXTRA_DESTINATION = "com.tioledger.apps.android.extra.REMINDER_DESTINATION"
    private const val EXTRA_LOAN_ID = "com.tioledger.apps.android.extra.LOAN_ID"
    private const val DESTINATION_LOAN = "loan"
    private const val DESTINATION_BUDGETS = "budgets"
}
