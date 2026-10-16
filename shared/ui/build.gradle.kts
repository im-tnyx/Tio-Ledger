plugins {
    id("tio.compose.multiplatform.library")
}

android {
    defaultConfig {
        testInstrumentationRunner = "com.tioledger.ui.validation.AccountsValidationInstrumentation"
    }
    testOptions {
        targetSdk = 35
    }
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
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonTest.dependencies {
            implementation(project(":shared:analytics"))
            implementation(libs.kotlin.test)
        }
    }
}
