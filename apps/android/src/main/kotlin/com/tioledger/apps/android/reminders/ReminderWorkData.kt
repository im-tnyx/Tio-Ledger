package com.tioledger.apps.android.reminders

import androidx.work.Data
import com.tioledger.application.usecase.notification.BudgetReminderStatusView

fun ReminderWorkPayload.toWorkData(): Data {
    val builder =
        Data.Builder()
            .putString(KEY_IDENTITY, identityKey)
            .putString(KEY_TYPE, type.name)
            .putLong(KEY_DELIVERY_TIMESTAMP, deliveryTimestamp)
            .putString(KEY_TIME_ZONE, timeZoneId)
            .putString(KEY_DESTINATION, destination.kind())

    when (val target = destination) {
        is AndroidReminderDestination.LoanDetails -> builder.putString(KEY_LOAN_ID, target.loanId)
        AndroidReminderDestination.Budgets -> Unit
    }
    when (val semanticContent = content) {
        is AndroidReminderContent.Emi ->
            builder
                .putString(KEY_LOAN_NAME, semanticContent.loanName)
                .putLong(KEY_DUE_DATE, semanticContent.dueDate)
                .putMoney(PREFIX_PAYMENT, semanticContent.payment)

        is AndroidReminderContent.Budget ->
            builder
                .putString(KEY_BUDGET_NAME, semanticContent.budgetName)
                .putString(KEY_BUDGET_STATUS, semanticContent.status.name)
                .putMoney(PREFIX_TARGET, semanticContent.target)
                .putMoney(PREFIX_SPENT, semanticContent.spent)
                .putMoney(PREFIX_REMAINING, semanticContent.remaining)
    }
    return builder.build()
}

@Suppress("ReturnCount")
fun Data.toReminderWorkPayloadOrNull(): ReminderWorkPayload? {
    val identityKey = getString(KEY_IDENTITY)?.takeIf(String::isNotBlank) ?: return null
    val type = getString(KEY_TYPE)?.enumValueOrNull<AndroidReminderType>() ?: return null
    val deliveryTimestamp = requiredLong(KEY_DELIVERY_TIMESTAMP)?.takeIf { it >= 0L } ?: return null
    val timeZoneId = getString(KEY_TIME_ZONE)?.takeIf(String::isNotBlank) ?: return null
    val destination = destinationOrNull() ?: return null
    val content = contentOrNull(type) ?: return null
    return ReminderWorkPayload(
        identityKey = identityKey,
        type = type,
        deliveryTimestamp = deliveryTimestamp,
        timeZoneId = timeZoneId,
        destination = destination,
        content = content,
    )
}

private fun Data.destinationOrNull(): AndroidReminderDestination? =
    when (getString(KEY_DESTINATION)) {
        DESTINATION_LOAN ->
            getString(KEY_LOAN_ID)
                ?.takeIf(String::isNotBlank)
                ?.let { loanId -> AndroidReminderDestination.LoanDetails(loanId) }

        DESTINATION_BUDGETS -> AndroidReminderDestination.Budgets
        else -> null
    }

private fun Data.contentOrNull(type: AndroidReminderType): AndroidReminderContent? =
    when (type) {
        AndroidReminderType.EMI ->
            AndroidReminderContent.Emi(
                loanName = getString(KEY_LOAN_NAME)?.takeIf(String::isNotBlank) ?: return null,
                dueDate = requiredLong(KEY_DUE_DATE)?.takeIf { it >= 0L } ?: return null,
                payment = moneyOrNull(PREFIX_PAYMENT) ?: return null,
            )

        AndroidReminderType.BUDGET -> {
            val status =
                getString(KEY_BUDGET_STATUS)
                    ?.enumValueOrNull<BudgetReminderStatusView>()
                    ?.takeUnless { value -> value == BudgetReminderStatusView.ON_TRACK }
                    ?: return null
            AndroidReminderContent.Budget(
                budgetName = getString(KEY_BUDGET_NAME)?.takeIf(String::isNotBlank) ?: return null,
                status = status,
                target = moneyOrNull(PREFIX_TARGET) ?: return null,
                spent = moneyOrNull(PREFIX_SPENT) ?: return null,
                remaining = moneyOrNull(PREFIX_REMAINING) ?: return null,
            )
        }
    }

@Suppress("ReturnCount")
private fun Data.moneyOrNull(prefix: String): AndroidMoneyPayload? {
    val amount = requiredLong("${prefix}_amount") ?: return null
    val currencyCode =
        getString("${prefix}_currency")
            ?.takeIf { value -> value.length == CURRENCY_CODE_LENGTH }
            ?: return null
    return AndroidMoneyPayload(amount, currencyCode)
}

private fun Data.requiredLong(key: String): Long? = getLong(key, 0L).takeIf { key in keyValueMap }

private fun Data.Builder.putMoney(
    prefix: String,
    money: AndroidMoneyPayload,
): Data.Builder {
    return putLong("${prefix}_amount", money.amount)
        .putString("${prefix}_currency", money.currencyCode)
}

private fun AndroidReminderDestination.kind(): String =
    when (this) {
        is AndroidReminderDestination.LoanDetails -> DESTINATION_LOAN
        AndroidReminderDestination.Budgets -> DESTINATION_BUDGETS
    }

private inline fun <reified T : Enum<T>> String.enumValueOrNull(): T? {
    return runCatching { enumValueOf<T>(this) }.getOrNull()
}

private const val KEY_IDENTITY = "identity"
private const val KEY_TYPE = "type"
private const val KEY_DELIVERY_TIMESTAMP = "delivery_timestamp"
private const val KEY_TIME_ZONE = "time_zone"
private const val KEY_DESTINATION = "destination"
private const val KEY_LOAN_ID = "loan_id"
private const val KEY_LOAN_NAME = "loan_name"
private const val KEY_DUE_DATE = "due_date"
private const val KEY_BUDGET_NAME = "budget_name"
private const val KEY_BUDGET_STATUS = "budget_status"
private const val PREFIX_PAYMENT = "payment"
private const val PREFIX_TARGET = "target"
private const val PREFIX_SPENT = "spent"
private const val PREFIX_REMAINING = "remaining"
private const val DESTINATION_LOAN = "loan"
private const val DESTINATION_BUDGETS = "budgets"
private const val CURRENCY_CODE_LENGTH = 3
