pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

// In settings.gradle.kts
buildCache {
    local {
        isEnabled = true
    }
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "EcoPets"

// Core
include(":eco-core")
include(":eco-core:core-plugin")
