import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

private fun Project.tioNamespace(): String {
    val suffix = path
        .split(":")
        .filter(String::isNotBlank)
        .joinToString(".") { segment -> segment.replace("-", "") }
    return "com.tioledger.$suffix"
}

configure<ApplicationExtension> {
    namespace = tioNamespace()
    compileSdk = 35

    defaultConfig {
        applicationId = tioNamespace()
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
