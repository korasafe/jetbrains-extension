plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.intellij.platform") apply false
}

allprojects {
    group = "ai.korasafe"
    version = "0.1.0"
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
