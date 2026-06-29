plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("app.cash.sqldelight:gradle-plugin:2.0.2")
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.7.3")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
}
