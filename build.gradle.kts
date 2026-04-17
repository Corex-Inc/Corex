plugins {
    java
}

group = "dev.corexinc.corex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.canvasmc.io/snapshots")
    maven("https://maven.pulsemc.dev/snapshots")
}

dependencies {
    @SuppressWarnings("deprecation")
    compileOnly("io.canvasmc.canvas:canvas-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Other Libs
    implementation("org.java-websocket:Java-WebSocket:1.5.6")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")


    // Tests
    @SuppressWarnings("deprecation")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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