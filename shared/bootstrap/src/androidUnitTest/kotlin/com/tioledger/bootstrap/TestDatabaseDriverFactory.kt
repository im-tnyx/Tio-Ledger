package com.tioledger.bootstrap

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tioledger.bootstrap.database.DatabaseDriverFactory
import com.tioledger.database.TioLedgerDatabase

class TestDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TioLedgerDatabase.Schema.create(driver)
        return driver
    }
}
