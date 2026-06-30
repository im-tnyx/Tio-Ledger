plugins {
    id("tio.android.application")
}

dependencies {
    implementation(project(":shared:bootstrap"))
    implementation(project(":shared:ui"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:data"))
    implementation(project(":shared:notifications"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)
}
