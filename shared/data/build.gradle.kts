plugins {
    id("tio.kotlin.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:database"))
            implementation(project(":shared:finance-engine"))
            implementation(project(":shared:loan-engine"))
            implementation(project(":shared:budget-engine"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
