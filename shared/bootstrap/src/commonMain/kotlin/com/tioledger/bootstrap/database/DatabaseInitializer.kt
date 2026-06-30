package com.tioledger.bootstrap.database

import com.tioledger.database.TioLedgerDatabase

class DatabaseInitializer(
    private val driverFactory: DatabaseDriverFactory,
) {
    fun initialize(): TioLedgerDatabase {
        val driver = driverFactory.createDriver()
        return TioLedgerDatabase(driver)
    }
}
