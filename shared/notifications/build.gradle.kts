plugins {
    id("tio.kotlin.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:loan-engine"))
            implementation(project(":shared:budget-engine"))
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
