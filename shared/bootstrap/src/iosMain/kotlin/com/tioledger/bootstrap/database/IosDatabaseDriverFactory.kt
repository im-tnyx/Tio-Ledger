package com.tioledger.bootstrap.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.tioledger.database.TioLedgerDatabase

class IosDatabaseDriverFactory(
    private val databaseName: String = "tio-ledger.db",
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = TioLedgerDatabase.Schema,
            name = databaseName,
        )
    }
}
