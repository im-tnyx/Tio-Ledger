plugins {
    id("tio.kotlin.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
