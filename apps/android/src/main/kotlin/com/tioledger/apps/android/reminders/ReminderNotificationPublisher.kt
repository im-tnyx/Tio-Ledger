package com.tioledger.apps.android.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.tioledger.application.usecase.notification.BudgetReminderStatusView
import com.tioledger.apps.android.R
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ReminderNotificationChannels {
    fun create(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    EMI_CHANNEL_ID,
                    context.getString(R.string.notification_channel_emi_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_emi_description)
                },
                NotificationChannel(
                    BUDGET_CHANNEL_ID,
                    context.getString(R.string.notification_channel_budget_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_budget_description)
                },
            ),
        )
    }
}

class AndroidReminderNotificationPublisher(
    private val context: Context,
) {
    fun publish(payload: ReminderWorkPayload): Boolean =
        runCatching {
            ReminderNotificationChannels.create(context)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val contentIntent =
                PendingIntent.getActivity(
                    context,
                    payload.notificationId,
                    ReminderNavigationIntent.create(context, payload),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val presentation = payload.presentation()
            val notification =
                Notification.Builder(context, payload.type.channelId())
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(presentation.title)
                    .setContentText(presentation.body)
                    .setStyle(Notification.BigTextStyle().bigText(presentation.body))
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setShowWhen(true)
                    .build()
            notificationManager.notify(payload.notificationId, notification)
        }.isSuccess

    private fun ReminderWorkPayload.presentation(): NotificationPresentation =
        when (val semanticContent = content) {
            is AndroidReminderContent.Emi ->
                NotificationPresentation(
                    title = context.getString(R.string.notification_emi_title),
                    body =
                        context.getString(
                            R.string.notification_emi_body,
                            semanticContent.loanName,
                            semanticContent.payment.toDisplayAmount(),
                            semanticContent.dueDate.toDisplayDate(),
                        ),
                )

            is AndroidReminderContent.Budget ->
                NotificationPresentation(
                    title = context.getString(semanticContent.status.titleResource()),
                    body =
                        context.getString(
                            R.string.notification_budget_body,
                            semanticContent.budgetName,
                            semanticContent.spent.toDisplayAmount(),
                            semanticContent.target.toDisplayAmount(),
                        ),
                )
        }

    private fun AndroidMoneyPayload.toDisplayAmount(): String {
        return "$currencyCode ${BigDecimal.valueOf(amount, MONEY_SCALE).toPlainString()}"
    }

    private fun Long.toDisplayDate(): String =
        DateFormat
            .getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(this))

    private fun BudgetReminderStatusView.titleResource(): Int =
        when (this) {
            BudgetReminderStatusView.WARNING -> R.string.notification_budget_warning_title
            BudgetReminderStatusView.REACHED -> R.string.notification_budget_reached_title
            BudgetReminderStatusView.EXCEEDED -> R.string.notification_budget_exceeded_title
            BudgetReminderStatusView.ON_TRACK -> R.string.notification_budget_title
        }

    private fun AndroidReminderType.channelId(): String =
        when (this) {
            AndroidReminderType.EMI -> EMI_CHANNEL_ID
            AndroidReminderType.BUDGET -> BUDGET_CHANNEL_ID
        }

    private data class NotificationPresentation(
        val title: String,
        val body: String,
    )
}

private const val EMI_CHANNEL_ID = "tio_emi_reminders_v1"
private const val BUDGET_CHANNEL_ID = "tio_budget_reminders_v1"
private const val MONEY_SCALE = 2
