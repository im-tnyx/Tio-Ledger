plugins {
    id("tio.android.application")
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:data"))
    implementation(project(":shared:notifications"))
}
