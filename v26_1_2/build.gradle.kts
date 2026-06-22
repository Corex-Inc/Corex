plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

dependencies {
    compileOnly(project(":corex"))

    paperweight.paperDevBundle("26.1.2.build.+")
}