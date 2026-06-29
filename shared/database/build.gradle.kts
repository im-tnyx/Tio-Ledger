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
