package com.tioledger.data.repository

import app.cash.sqldelight.db.SqlDriver

expect fun createTestSqlDriver(): SqlDriver
