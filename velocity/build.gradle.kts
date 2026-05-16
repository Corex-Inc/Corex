plugins {
    java
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    compileOnly("org.java-websocket:Java-WebSocket:1.5.6")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnly(project(":corex"))
}