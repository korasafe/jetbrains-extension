plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    implementation(project(":core"))

    intellijPlatform {
        intellijIdeaCommunity("2024.2.5")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        id = "ai.korasafe.jetbrains"
        name = "KoraSafe Governance"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { "243.*" }
        }

        vendor {
            name = "KoraSafe"
            email = "Contact-us@korasafe.ai"
            url = "https://korasafe.ai"
        }
    }
}
