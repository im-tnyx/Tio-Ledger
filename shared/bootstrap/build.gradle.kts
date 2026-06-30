plugins {
    id("tio.kotlin.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(project(":shared:database"))
            implementation(project(":shared:application"))
            implementation(project(":shared:data"))
            implementation(project(":shared:domain"))
            implementation(project(":shared:finance-engine"))
            implementation(libs.koin.core)
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
            implementation(libs.koin.test)
        }

        androidUnitTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}
