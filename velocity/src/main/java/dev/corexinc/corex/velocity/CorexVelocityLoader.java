package dev.corexinc.corex.velocity;

import com.velocitypowered.api.plugin.PluginManager;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

public class CorexVelocityLoader {

    private static final String MAVEN_CENTRAL = "https://maven-central.storage-download.googleapis.com/maven2/";

    private static final List<Dependency> DEPENDENCIES = List.of(
            new Dependency("org/java-websocket/Java-WebSocket/1.5.6/Java-WebSocket-1.5.6.jar"),
            new Dependency("com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"),
            new Dependency("org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar")
    );

    private final PluginManager pluginManager;
    private final Object plugin;
    private final Path libsDir;

    public CorexVelocityLoader(PluginManager pluginManager, Object plugin, Path dataFolder) {
        this.pluginManager = pluginManager;
        this.plugin = plugin;
        this.libsDir = dataFolder.resolve(".libs");
    }

    public void download() throws IOException {
        Files.createDirectories(libsDir);

        for (Dependency dep : DEPENDENCIES) {
            Path jar = libsDir.resolve(dep.fileName());

            if (isCorrupted(jar)) {
                if (Files.exists(jar)) {
                    CorexLogger.warn("Corrupted jar detected, re-downloading: " + dep.fileName());
                    Files.delete(jar);
                }
                downloadJar(dep.url(), jar);
            }
        }
    }

    public void inject() {
        for (Dependency dep : DEPENDENCIES) {
            pluginManager.addToClasspath(plugin, libsDir.resolve(dep.fileName()));
        }
    }

    private boolean isCorrupted(Path path) {
        if (!Files.exists(path)) return true;
        try (var zip = new ZipFile(path.toFile())) {
            return !zip.entries().hasMoreElements();
        } catch (Exception e) {
            return true;
        }
    }

    private void downloadJar(String url, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (var in = URI.create(url).toURL().openStream()) {
            Files.copy(in, tmp);
            if (isCorrupted(tmp)) {
                Files.deleteIfExists(tmp);
                throw new IOException("Downloaded jar is corrupted: " + target.getFileName());
            }
            Files.move(tmp, target);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
    }

    private record Dependency(String path) {
        String fileName() { return Path.of(path).getFileName().toString(); }
        String url() { return MAVEN_CENTRAL + path; }
    }
}