plugins {
    id("tio.android.application")
}

dependencies {
    implementation(project(":shared:application"))
    implementation(project(":shared:bootstrap"))
    implementation(project(":shared:ui"))
    implementation(project(":shared:data"))
    implementation(project(":shared:database"))
    implementation(project(":shared:notifications"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.components.uiToolingPreview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.core)
    implementation(libs.sqldelight.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.sqldelight.sqlite.driver)
}
