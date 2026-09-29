package com.tioledger.apps.android.reminders

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import com.tioledger.bootstrap.database.DatabaseDriverFactory

// SQLDelight table keys whose committed writes can change reminder eligibility or reminder content.
internal val REMINDER_RELEVANT_TABLE_KEYS: List<String> =
    listOf(
        "loans",
        "emi_schedules",
        "budgets",
        "transactions",
        "transaction_splits",
        "ledger_entries",
    )

// Read-only observer: reports committed writes so the Android adapter can enqueue reconciliation.
class ReminderDataChangeDriverFactory(
    private val delegate: DatabaseDriverFactory,
    private val onRelevantDataChanged: () -> Unit,
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        delegate.createDriver().also { driver ->
            val listener = Query.Listener { onRelevantDataChanged() }
            REMINDER_RELEVANT_TABLE_KEYS.forEach { key -> driver.addListener(key, listener = listener) }
        }
}
