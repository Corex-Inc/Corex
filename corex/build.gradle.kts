plugins {
    java
}

configurations.testRuntimeOnly {
    extendsFrom(configurations.compileOnlyApi.get())
}

@Suppress("UNCHECKED_CAST")
val corexLibraries = rootProject.extra["corexLibraries"] as List<String>

dependencies {
    @SuppressWarnings("deprecation")
    compileOnly("io.canvasmc.canvas:canvas-api:26.2.build.937-stable")

    compileOnlyApi("org.jetbrains:annotations:24.1.0")
    corexLibraries.forEach { compileOnlyApi(it) }


    testImplementation(project(":corex-test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testRuntimeOnly("io.netty:netty-all:4.1.72.Final")
}

tasks.withType<Test>().configureEach {
    jvmArgs(
        "--sun-misc-unsafe-memory-access=allow",
        "--enable-native-access=ALL-UNNAMED",
    )

    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = providers.gradleProperty("corexVerboseTests").isPresent
    }
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