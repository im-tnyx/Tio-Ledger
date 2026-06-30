package com.tioledger.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.tioledger.database.TioLedgerDatabase

actual fun createTestSqlDriver(): SqlDriver {
    return NativeSqliteDriver(TioLedgerDatabase.Schema, "test.db")
}
