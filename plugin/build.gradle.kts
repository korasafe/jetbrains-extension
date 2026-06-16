import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

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
    buildSearchableOptions = false

    pluginConfiguration {
        id = "ai.korasafe.jetbrains"
        name = "KoraSafe Governance"
        version = project.version.toString()
        description = """
            <p>KoraSafe Governance brings local AI governance checks into JetBrains IDEs for agent code, LLM usage, policy files, and cloud-check workflows.</p>
            <ul>
              <li>Scan project files for governance findings and regulation references.</li>
              <li>Load workspace rules manifests and <code>.korasafe/policy.yaml</code> configuration.</li>
              <li>Gate cloud checks on project trust, API-key configuration, and telemetry opt-in.</li>
            </ul>
        """.trimIndent()
        changeNotes = """
            <ul>
              <li>Initial JetBrains v1.0 compatibility build for IntelliJ Platform 2024.2 IDEs.</li>
              <li>Adds analyzer, policy, rules manifest, cloud-check, and MCP client foundations.</li>
            </ul>
        """.trimIndent()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { "253.*" }
        }

        vendor {
            name = "KoraSafe"
            email = "Contact-us@korasafe.ai"
            url = "https://korasafe.ai"
        }
    }

    publishing {
        token = providers.environmentVariable("INTELLIJ_PUBLISH_TOKEN")
            .orElse(providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN"))
            .orElse(providers.environmentVariable("PUBLISH_TOKEN"))
        channels = providers.environmentVariable("JETBRAINS_MARKETPLACE_CHANNEL")
            .map { it.split(',').map(String::trim).filter(String::isNotBlank) }
            .orElse(listOf("default"))
    }

    signing {
        certificateChain = providers.environmentVariable("JETBRAINS_CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("JETBRAINS_PRIVATE_KEY")
        password = providers.environmentVariable("JETBRAINS_PRIVATE_KEY_PASSWORD")
    }

    pluginVerification {
        ides {
            when (providers.gradleProperty("korasafeVerifyIde").orNull) {
                "idea-community" -> ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.5")
                "idea-ultimate" -> ide(IntelliJPlatformType.IntellijIdeaUltimate, "2024.2.5")
                "pycharm-community" -> ide(IntelliJPlatformType.PyCharmCommunity, "2024.2.5")
                "pycharm-professional" -> ide(IntelliJPlatformType.PyCharmProfessional, "2024.2.5")
                "webstorm" -> ide(IntelliJPlatformType.WebStorm, "2024.2.5")
                "goland" -> ide(IntelliJPlatformType.GoLand, "2024.2.5")
                "rubymine" -> ide(IntelliJPlatformType.RubyMine, "2024.2.5")
                else -> {
                    ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.5")
                    ide(IntelliJPlatformType.IntellijIdeaUltimate, "2024.2.5")
                    ide(IntelliJPlatformType.PyCharmCommunity, "2024.2.5")
                    ide(IntelliJPlatformType.PyCharmProfessional, "2024.2.5")
                    ide(IntelliJPlatformType.WebStorm, "2024.2.5")
                    ide(IntelliJPlatformType.GoLand, "2024.2.5")
                    ide(IntelliJPlatformType.RubyMine, "2024.2.5")
                }
            }
        }
    }
}
