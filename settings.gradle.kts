pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ Add JitPack repository for GitHub dependencies
        maven { url = uri("https://jitpack.io") }
        // ✅ Add Cloudinary repository
        maven { url = uri("https://cloudinary.bintray.com/cloudinary") }
    }
}

rootProject.name = "TradeUp"
include(":app")