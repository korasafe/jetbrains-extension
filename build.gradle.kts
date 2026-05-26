plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.intellij.platform") apply false
    id("org.owasp.dependencycheck") version "10.0.4"
}

allprojects {
    group = "ai.korasafe"
    version = providers.gradleProperty("pluginVersion")
        .orElse(providers.environmentVariable("PLUGIN_VERSION"))
        .orElse("0.2.0")
        .get()
}

subprojects {
    apply(plugin = "org.owasp.dependencycheck")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    extensions.configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension>("dependencyCheck") {
        failBuildOnCVSS = 7.0f
        formats = listOf("HTML", "JSON")
        suppressionFile = rootProject.layout.projectDirectory.file("gradle/dependency-check-suppressions.xml").asFile.absolutePath
        analyzers.assemblyEnabled = false
        analyzers.nodeAuditEnabled = false
        nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
    }
}
