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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.core)

    testImplementation(libs.junit)
}
