plugins {
    kotlin("jvm") version "2.1.21" apply false
    id("org.jetbrains.intellij.platform") version "2.5.0" apply false
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
