plugins {
    java
}

@Suppress("UNCHECKED_CAST")
val corexLibraries = rootProject.extra["corexLibraries"] as List<String>

dependencies {
    api(project(":corex"))

    @Suppress("DEPRECATION")
    api("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.+")
    api("io.papermc.paper:paper-api:26.2.build.119-stable")

    corexLibraries.forEach { runtimeOnly(it) }
    runtimeOnly("io.netty:netty-all:4.1.72.Final")
}
