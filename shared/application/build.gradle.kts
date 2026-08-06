plugins {
    id("tio.kotlin.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(project(":shared:domain"))
            api(project(":shared:finance-engine"))
            api(project(":shared:budget-engine"))
            api(project(":shared:loan-engine"))
            implementation(project(":shared:analytics"))
            implementation(project(":shared:notifications"))
            api(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
