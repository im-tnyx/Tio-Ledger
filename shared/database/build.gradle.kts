import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("tio.kotlin.multiplatform.library")
    id("tio.sqldelight")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            implementation(libs.sqldelight.runtime)
        }

        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

configure<SqlDelightExtension> {
    databases {
        named("TioLedgerDatabase") {
            schemaOutputDirectory.set(
                file("src/commonMain/sqldelight/com/tioledger/database"),
            )
        }
    }
}
