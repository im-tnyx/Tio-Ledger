import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("app.cash.sqldelight")
}

configure<SqlDelightExtension> {
    databases {
        create("TioLedgerDatabase") {
            packageName.set("com.tioledger.database")
        }
    }
}
