plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
}

allprojects {
    group = "dev.corexinc.corex"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.canvasmc.io/snapshots")
        maven("https://maven.pulsemc.dev/snapshots")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":corex"))
    implementation(project(":v1_21_3"))
    implementation(project(":v1_21_4"))
    implementation(project(":v1_21_11"))
}

tasks {
    shadowJar {
        archiveBaseName.set("Corex")
        archiveClassifier.set("")
        archiveVersion.set("1.0-SNAPSHOT")
    }
    build {
        dependsOn(shadowJar)
    }
}