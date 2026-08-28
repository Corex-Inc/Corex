plugins {
    java
}

dependencies {
    // Платформа: её даёт прокси, в POM попадать не должна.
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")

    compileOnlyApi("org.java-websocket:Java-WebSocket:1.5.6")
    compileOnlyApi("com.zaxxer:HikariCP:5.1.0")
    compileOnlyApi("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnlyApi(project(":corex"))
}