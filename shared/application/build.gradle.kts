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
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
