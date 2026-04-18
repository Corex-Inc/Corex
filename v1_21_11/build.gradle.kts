plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

dependencies {
    compileOnly(project(":corex"))

    paperweight.foliaDevBundle("1.21.11-R0.1-SNAPSHOT")
}