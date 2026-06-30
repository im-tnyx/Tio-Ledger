plugins {
    id("tio.kotlin.ios.framework")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core"))
            api(project(":shared:domain"))
            api(project(":shared:bootstrap"))
            api(project(":shared:data"))
            api(project(":shared:database"))
            api(project(":shared:finance-engine"))
            api(project(":shared:loan-engine"))
            api(project(":shared:budget-engine"))
            api(project(":shared:analytics"))
            api(project(":shared:notifications"))
            api(project(":shared:ui"))
        }
    }
}
