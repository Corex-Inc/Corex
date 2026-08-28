plugins {
    java
}

group = "dev.corexinc.corex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.canvasmc.io/releases")
    maven("https://maven.pulsemc.dev/snapshots")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.pvphub.me/tofaa")
}

dependencies {
    @SuppressWarnings("deprecation")
    compileOnly("io.canvasmc.canvas:canvas-api:26.2.build.937-stable")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Other Libs
    compileOnly("org.java-websocket:Java-WebSocket:1.5.6")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
    compileOnly("io.github.tofaa2:spigot:3.3.7-SNAPSHOT")


    // Tests
    @SuppressWarnings("deprecation")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.+")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:26.2.build.119-stable")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.java-websocket:Java-WebSocket:1.5.6")
    testRuntimeOnly("com.zaxxer:HikariCP:5.1.0")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    testRuntimeOnly("com.github.retrooper:packetevents-spigot:2.13.0")
    testRuntimeOnly("io.github.tofaa2:spigot:3.3.7-SNAPSHOT")
    testRuntimeOnly("io.netty:netty-all:4.1.72.Final")
}

tasks.test {
    useJUnitPlatform()

    dependsOn(tasks.jar)
    classpath = files(tasks.jar) + (classpath - sourceSets.main.get().output)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.register<Test>("ObjectTagPreTest") {
    group = "verification"
    description = "Runs automated testing of all ObjectTags and properties."

    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath

    useJUnitPlatform {
        includeTags("ObjectTagTest")
    }

    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("FormatterTagPreTest") {
    group = "verification"
    description = "Runs automated testing of all FormatterTags."

    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath

    useJUnitPlatform {
        includeTags("FormatterTest")
    }

    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}