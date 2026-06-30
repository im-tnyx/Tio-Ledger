plugins {
    id("tio.compose.multiplatform.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:application"))
            implementation(project(":shared:bootstrap"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
