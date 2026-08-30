pluginManagement {
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

rootProject.name = "selah"

// Standalone FanStaF app (com.fanstaf.selah) — a sibling of woodshed, NOT under the N5SLN ham
// brand and deliberately self-contained (own theme, no groundplane composite build).
include(":app")
