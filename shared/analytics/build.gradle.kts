plugins {
    id("tio.kotlin.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:finance-engine"))
            implementation(project(":shared:loan-engine"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
