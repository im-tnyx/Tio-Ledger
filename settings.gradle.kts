pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TioLedger"

include(":apps:android")
include(":apps:wear")
include(":apps:ios")

include(":shared:core")
include(":shared:domain")
include(":shared:application")
include(":shared:bootstrap")
include(":shared:data")
include(":shared:database")
include(":shared:finance-engine")
include(":shared:loan-engine")
include(":shared:budget-engine")
include(":shared:analytics")
include(":shared:notifications")
include(":shared:ui")
