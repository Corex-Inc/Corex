plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

val corexVersion = project.property("corexVersion") as String
val corexRepoUrl = project.property("corexRepoUrl") as String
val isSnapshot = corexVersion.endsWith("SNAPSHOT")

allprojects {
    group = "dev.corexinc.corex"
    version = corexVersion

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.canvasmc.io/releases")
        maven("https://maven.canvasmc.io/snapshots")
        maven("https://maven.pulsemc.dev/snapshots")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://maven.pvphub.me/tofaa")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Keeps parameter names in the class file, so a bound command's debug report can
        // label arguments the way the author named them instead of by syntax placeholder.
        options.compilerArgs.add("-parameters")
    }
}

val corexLibraries = listOf(
    "org.java-websocket:Java-WebSocket:1.5.6",
    "com.zaxxer:HikariCP:5.1.0",
    "org.xerial:sqlite-jdbc:3.45.1.0",
    "com.github.retrooper:packetevents-spigot:2.13.0",
    "io.github.tofaa2:spigot:3.3.7-SNAPSHOT",
)
extra["corexLibraries"] = corexLibraries

val publishedArtifacts = mapOf(
    "corex" to "corex",
    "velocity" to "corex-velocity",
    "corex-test" to "corex-test",
)

configure(subprojects.filter { it.name in publishedArtifacts.keys }) {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Javadoc>().configureEach {
        isFailOnError = false

        include(
            "dev/corexinc/corex/api/**",
            "dev/corexinc/corex/engine/**",
            "dev/corexinc/corex/environment/events/**",
            "dev/corexinc/corex/velocity/**",
            "dev/corexinc/corex/testing/**",
        )

        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("Xmaxwarns", "1000")
        }
    }

    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(":corex:test")
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                artifactId = publishedArtifacts.getValue(project.name)
                from(components["java"])

                pom {
                    name.set(artifactId)
                    description.set(when (project.name) {
                        "velocity" -> "Corex for Velocity — proxy-side commands and tags"
                        "corex-test" -> "Corex test harness — MockBukkit bootstrap for addon tag and formatter tests"
                        else -> "Corex — Denizen-inspired scripting engine for Paper/Folia/Canvas/Velocity"
                    })
                    url.set("https://github.com/corexinc/Corex")
                    licenses {
                        license {
                            name.set("See LICENSE")
                            url.set("https://github.com/corexinc/Corex/blob/main/LICENSE")
                        }
                    }
                }
            }
        }

        repositories {
            maven {
                name = "reposilite"
                url = uri("$corexRepoUrl/${if (isSnapshot) "snapshots" else "releases"}")
                credentials {
                    username = providers.gradleProperty("reposiliteUsername").orNull
                        ?: System.getenv("REPOSILITE_USER")
                    password = providers.gradleProperty("reposilitePassword").orNull
                        ?: System.getenv("REPOSILITE_TOKEN")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":corex"))
    implementation(project(":velocity"))
    implementation(project(":v1_21"))
    implementation(project(":v1_21_3"))
    implementation(project(":v1_21_4"))
    implementation(project(":v1_21_5"))
    implementation(project(":v1_21_6"))
    implementation(project(":v1_21_7"))
    implementation(project(":v1_21_9"))
    implementation(project(":v1_21_11"))
    implementation(project(":v26_1_2"))
    implementation(project(":v26_2"))
}

tasks {
    shadowJar {
        archiveBaseName.set("Corex")
        archiveClassifier.set("")
        archiveVersion.set(corexVersion)

        dependsOn(":corex:test")
    }
    build {
        dependsOn(shadowJar)
    }
}
