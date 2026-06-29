import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
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

configure<KotlinMultiplatformExtension> {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

configure<LibraryExtension> {
    namespace = tioNamespace()
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
