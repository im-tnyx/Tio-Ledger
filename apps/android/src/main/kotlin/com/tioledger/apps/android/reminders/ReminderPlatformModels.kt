package com.tioledger.apps.android.reminders

import com.tioledger.application.usecase.notification.BudgetReminderStatusView
import com.tioledger.application.usecase.notification.ReminderContentView
import com.tioledger.application.usecase.notification.ReminderDestinationView
import com.tioledger.application.usecase.notification.ReminderPlanTypeView
import com.tioledger.application.usecase.notification.ReminderPlanView
import java.security.MessageDigest

enum class AndroidReminderType {
    EMI,
    BUDGET,
}

sealed interface AndroidReminderDestination {
    data class LoanDetails(
        val loanId: String,
    ) : AndroidReminderDestination

    data object Budgets : AndroidReminderDestination
}

data class AndroidMoneyPayload(
    val amount: Long,
    val currencyCode: String,
)

sealed interface AndroidReminderContent {
    data class Emi(
        val loanName: String,
        val dueDate: Long,
        val payment: AndroidMoneyPayload,
    ) : AndroidReminderContent

    data class Budget(
        val budgetName: String,
        val status: BudgetReminderStatusView,
        val target: AndroidMoneyPayload,
        val spent: AndroidMoneyPayload,
        val remaining: AndroidMoneyPayload,
    ) : AndroidReminderContent
}

data class ReminderWorkPayload(
    val identityKey: String,
    val type: AndroidReminderType,
    val deliveryTimestamp: Long,
    val timeZoneId: String,
    val destination: AndroidReminderDestination,
    val content: AndroidReminderContent,
) {
    val payloadFingerprint: String = stableSha256(canonicalValue())
    val uniqueWorkName: String = reminderUniqueWorkName(identityKey)
    val notificationId: Int = stableNotificationId(identityKey)

    private fun canonicalValue(): String =
        buildString {
            appendField(identityKey)
            appendField(type.name)
            appendField(deliveryTimestamp.toString())
            appendField(timeZoneId)
            when (val target = destination) {
                is AndroidReminderDestination.LoanDetails -> {
                    appendField("loan")
                    appendField(target.loanId)
                }

                AndroidReminderDestination.Budgets -> appendField("budgets")
            }
            when (val semanticContent = content) {
                is AndroidReminderContent.Emi -> {
                    appendField("emi")
                    appendField(semanticContent.loanName)
                    appendField(semanticContent.dueDate.toString())
                    appendMoney(semanticContent.payment)
                }

                is AndroidReminderContent.Budget -> {
                    appendField("budget")
                    appendField(semanticContent.budgetName)
                    appendField(semanticContent.status.name)
                    appendMoney(semanticContent.target)
                    appendMoney(semanticContent.spent)
                    appendMoney(semanticContent.remaining)
                }
            }
        }

    private fun StringBuilder.appendMoney(money: AndroidMoneyPayload) {
        appendField(money.amount.toString())
        appendField(money.currencyCode)
    }

    private fun StringBuilder.appendField(value: String) {
        append(value.length)
        append(':')
        append(value)
        append('|')
    }
}

data class ScheduledReminderRecord(
    val identityKey: String,
    val type: AndroidReminderType,
    val deliveryTimestamp: Long,
    val payloadFingerprint: String,
)

fun ReminderPlanView.toAndroidPayload(): ReminderWorkPayload =
    ReminderWorkPayload(
        identityKey = identityKey,
        type =
            when (type) {
                ReminderPlanTypeView.EMI -> AndroidReminderType.EMI
                ReminderPlanTypeView.BUDGET -> AndroidReminderType.BUDGET
            },
        deliveryTimestamp = deliveryTimestamp,
        timeZoneId = timeZoneId,
        destination = destination.toAndroidDestination(),
        content = content.toAndroidContent(),
    )

fun ReminderWorkPayload.toScheduledRecord(): ScheduledReminderRecord =
    ScheduledReminderRecord(
        identityKey = identityKey,
        type = type,
        deliveryTimestamp = deliveryTimestamp,
        payloadFingerprint = payloadFingerprint,
    )

internal fun reminderUniqueWorkName(identityKey: String): String {
    return "tio-reminder-${stableSha256(identityKey)}"
}

private fun ReminderDestinationView.toAndroidDestination(): AndroidReminderDestination =
    when (this) {
        is ReminderDestinationView.LoanDetails -> AndroidReminderDestination.LoanDetails(loanId)
        ReminderDestinationView.Budgets -> AndroidReminderDestination.Budgets
    }

private fun ReminderContentView.toAndroidContent(): AndroidReminderContent =
    when (this) {
        is ReminderContentView.Emi ->
            AndroidReminderContent.Emi(
                loanName = loanName,
                dueDate = dueDate,
                payment = AndroidMoneyPayload(payment.amount, payment.currency.toString()),
            )

        is ReminderContentView.Budget ->
            AndroidReminderContent.Budget(
                budgetName = budgetName,
                status = status,
                target = AndroidMoneyPayload(target.amount, target.currency.toString()),
                spent = AndroidMoneyPayload(spent.amount, spent.currency.toString()),
                remaining = AndroidMoneyPayload(remaining.amount, remaining.currency.toString()),
            )
    }

private fun stableSha256(value: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

private fun stableNotificationId(identityKey: String): Int {
    val candidate =
        stableSha256(identityKey)
            .take(STABLE_NOTIFICATION_ID_HEX_LENGTH)
            .toLong(radix = HEX_RADIX)
            .toInt() and Int.MAX_VALUE
    return candidate.takeUnless { it == 0 } ?: 1
}

private const val STABLE_NOTIFICATION_ID_HEX_LENGTH = 8
private const val HEX_RADIX = 16
