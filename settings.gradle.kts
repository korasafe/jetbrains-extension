import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.1.21"
        id("org.jetbrains.intellij.platform") version "2.5.0"
        id("org.jetbrains.intellij.platform.settings") version "2.5.0"
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

rootProject.name = "korasafe-jetbrains-extension"

include("core", "plugin")
