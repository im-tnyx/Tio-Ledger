import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("app.cash.sqldelight")
}

configure<SqlDelightExtension> {
    databases {
        create("TioLedgerDatabase") {
            packageName.set("com.tioledger.database")
            verifyMigrations.set(true)
        }
    }
}

val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
val isCi = System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null

if (isWindows && !isCi) {
    tasks.withType<app.cash.sqldelight.gradle.VerifyMigrationTask>().configureEach {
        enabled = false
    }
}

