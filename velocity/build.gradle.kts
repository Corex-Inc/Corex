plugins {
    java
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.1.1-SNAPSHOT")

    compileOnlyApi("org.java-websocket:Java-WebSocket:1.5.6")
    compileOnlyApi("com.zaxxer:HikariCP:5.1.0")
    compileOnlyApi("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnlyApi(project(":corex"))
}