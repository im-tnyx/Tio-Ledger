package com.tioledger.apps.android.reminders

import android.content.Context
import android.content.SharedPreferences

data class AndroidReminderPreferences(
    val emiRemindersEnabled: Boolean,
    val budgetRemindersEnabled: Boolean,
)

data class NotificationPermissionHistory(
    val requestAttempted: Boolean,
    val grantObserved: Boolean,
)

interface ReminderPlatformStateStore {
    fun reminderPreferences(): AndroidReminderPreferences

    fun setEmiRemindersEnabled(enabled: Boolean): Boolean

    fun setBudgetRemindersEnabled(enabled: Boolean): Boolean

    fun permissionHistory(): NotificationPermissionHistory

    fun markPermissionRequestAttempted(): Boolean

    fun recordPermissionGranted(): Boolean
}

interface BudgetReminderReceiptStore {
    fun deliveredIdentityKeys(): Set<String>

    fun recordDelivered(
        identityKey: String,
        deliveredAt: Long,
    ): Boolean
}

interface ScheduledReminderStore {
    fun records(): List<ScheduledReminderRecord>

    fun put(record: ScheduledReminderRecord): Boolean

    fun remove(identityKey: String): Boolean
}

class SharedPreferencesReminderPlatformStateStore(
    context: Context,
) : ReminderPlatformStateStore {
    private val preferences = context.reminderSharedPreferences()

    override fun reminderPreferences(): AndroidReminderPreferences =
        AndroidReminderPreferences(
            emiRemindersEnabled = preferences.getBoolean(KEY_EMI_ENABLED, false),
            budgetRemindersEnabled = preferences.getBoolean(KEY_BUDGET_ENABLED, false),
        )

    override fun setEmiRemindersEnabled(enabled: Boolean): Boolean {
        return preferences.edit().putBoolean(KEY_EMI_ENABLED, enabled).commit()
    }

    override fun setBudgetRemindersEnabled(enabled: Boolean): Boolean {
        return preferences.edit().putBoolean(KEY_BUDGET_ENABLED, enabled).commit()
    }

    override fun permissionHistory(): NotificationPermissionHistory =
        NotificationPermissionHistory(
            requestAttempted = preferences.getBoolean(KEY_PERMISSION_REQUESTED, false),
            grantObserved = preferences.getBoolean(KEY_PERMISSION_GRANTED_OBSERVED, false),
        )

    override fun markPermissionRequestAttempted(): Boolean {
        return preferences.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).commit()
    }

    override fun recordPermissionGranted(): Boolean {
        return preferences.edit().putBoolean(KEY_PERMISSION_GRANTED_OBSERVED, true).commit()
    }
}

class SharedPreferencesBudgetReminderReceiptStore(
    context: Context,
    private val maxEntries: Int = DEFAULT_MAX_BUDGET_RECEIPTS,
) : BudgetReminderReceiptStore {
    private val preferences = context.reminderSharedPreferences()

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero" }
    }

    override fun deliveredIdentityKeys(): Set<String> =
        preferences.all.keys
            .asSequence()
            .filter { key -> key.startsWith(BUDGET_RECEIPT_PREFIX) }
            .map { key -> key.removePrefix(BUDGET_RECEIPT_PREFIX) }
            .toSet()

    override fun recordDelivered(
        identityKey: String,
        deliveredAt: Long,
    ): Boolean {
        require(identityKey.isNotBlank()) { "identityKey must not be blank" }
        require(deliveredAt >= 0L) { "deliveredAt must be zero or greater" }
        val stored =
            preferences
                .edit()
                .putLong(BUDGET_RECEIPT_PREFIX + identityKey, deliveredAt)
                .commit()
        return stored && pruneOldestReceipts()
    }

    private fun pruneOldestReceipts(): Boolean {
        val receiptTimestamps =
            preferences.all
                .asSequence()
                .mapNotNull { (key, value) ->
                    if (key.startsWith(BUDGET_RECEIPT_PREFIX) && value is Long) {
                        key to value
                    } else {
                        null
                    }
                }.toMap()
        val staleKeys = budgetReceiptKeysToPrune(receiptTimestamps, maxEntries)
        if (staleKeys.isEmpty()) return true

        val editor = preferences.edit()
        staleKeys.forEach(editor::remove)
        return editor.commit()
    }
}

class SharedPreferencesScheduledReminderStore(
    context: Context,
) : ScheduledReminderStore {
    private val preferences = context.reminderSharedPreferences()

    override fun records(): List<ScheduledReminderRecord> =
        preferences.all.keys
            .asSequence()
            .filter { key -> key.startsWith(SCHEDULED_REMINDER_PREFIX) }
            .mapNotNull { key ->
                val identityKey = key.removePrefix(SCHEDULED_REMINDER_PREFIX)
                preferences.getString(key, null)?.toScheduledRecordOrNull(identityKey)
            }.sortedBy(ScheduledReminderRecord::identityKey)
            .toList()

    override fun put(record: ScheduledReminderRecord): Boolean {
        require(record.identityKey.isNotBlank()) { "identityKey must not be blank" }
        require(record.deliveryTimestamp >= 0L) { "deliveryTimestamp must be zero or greater" }
        val encoded = "${record.type.name}|${record.deliveryTimestamp}|${record.payloadFingerprint}"
        return preferences
            .edit()
            .putString(SCHEDULED_REMINDER_PREFIX + record.identityKey, encoded)
            .commit()
    }

    override fun remove(identityKey: String): Boolean {
        return preferences.edit().remove(SCHEDULED_REMINDER_PREFIX + identityKey).commit()
    }

    @Suppress("ReturnCount")
    private fun String.toScheduledRecordOrNull(identityKey: String): ScheduledReminderRecord? {
        val parts = split(SCHEDULED_RECORD_DELIMITER, limit = SCHEDULED_RECORD_PART_COUNT)
        if (parts.size != SCHEDULED_RECORD_PART_COUNT) return null
        val type =
            runCatching { AndroidReminderType.valueOf(parts[0]) }
                .getOrNull()
                ?: return null
        val deliveryTimestamp = parts[1].toLongOrNull() ?: return null
        if (deliveryTimestamp < 0L || parts[2].isBlank()) return null
        return ScheduledReminderRecord(
            identityKey = identityKey,
            type = type,
            deliveryTimestamp = deliveryTimestamp,
            payloadFingerprint = parts[2],
        )
    }
}

internal fun budgetReceiptKeysToPrune(
    receiptTimestamps: Map<String, Long>,
    maxEntries: Int,
): List<String> {
    require(maxEntries > 0) { "maxEntries must be greater than zero" }
    return receiptTimestamps.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Long>> { entry -> entry.value }
                .thenByDescending { entry -> entry.key },
        ).drop(maxEntries)
        .map { entry -> entry.key }
}

private fun Context.reminderSharedPreferences(): SharedPreferences {
    return getSharedPreferences(REMINDER_PREFERENCES_FILE, Context.MODE_PRIVATE)
}

private const val REMINDER_PREFERENCES_FILE = "tio_reminders_v1"
private const val KEY_EMI_ENABLED = "emi_enabled"
private const val KEY_BUDGET_ENABLED = "budget_enabled"
private const val KEY_PERMISSION_REQUESTED = "notification_permission_requested"
private const val KEY_PERMISSION_GRANTED_OBSERVED = "notification_permission_granted_observed"
private const val BUDGET_RECEIPT_PREFIX = "budget_receipt:"
private const val SCHEDULED_REMINDER_PREFIX = "scheduled_reminder:"
private const val DEFAULT_MAX_BUDGET_RECEIPTS = 256
private const val SCHEDULED_RECORD_DELIMITER = '|'
private const val SCHEDULED_RECORD_PART_COUNT = 3
