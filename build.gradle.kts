plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.intellij.platform") apply false
}

allprojects {
    group = "ai.korasafe"
    version = providers.gradleProperty("pluginVersion")
        .orElse(providers.environmentVariable("PLUGIN_VERSION"))
        .orElse("0.2.0")
        .get()
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
