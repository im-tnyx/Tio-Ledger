plugins {
    id("tio.android.application")
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(project(":shared:data"))
    implementation(project(":shared:database"))
    implementation(project(":shared:notifications"))
}
