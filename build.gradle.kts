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
        // NVD API key intentionally not set in the DSL — the Kotlin DSL
        // `nvd.apiKey` path didn't actually wire the value to the plugin
        // at runtime. Use `-Dnvd.api.key=$NVD_API_KEY` on the Gradle
        // command line OR set `DEPENDENCYCHECK_NVD_API_KEY` in the env
        // (the cve-audit.yml workflow does both as belt-and-suspenders).
    }
}
