package com.tioledger.bootstrap.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tioledger.database.TioLedgerDatabase

class AndroidDatabaseDriverFactory(
    private val context: Context,
    private val databaseName: String = "tio-ledger.db",
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = TioLedgerDatabase.Schema,
            context = context,
            name = databaseName,
        )
    }
}
