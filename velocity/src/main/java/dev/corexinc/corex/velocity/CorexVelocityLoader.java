package dev.corexinc.corex.velocity;

import com.velocitypowered.api.plugin.PluginManager;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * Downloads the runtime libraries the proxy module needs and adds them to the plugin classpath.
 *
 * <p>Every jar is pinned to a SHA-256 taken from Maven Central at the time its version was
 * chosen. A jar on disk is used only when it hashes to that value, and a download that does not
 * is deleted and reported rather than loaded, so a mirror or a man in the middle cannot hand the
 * proxy code it never asked for. Bumping a version here means bumping its hash in the same
 * change.</p>
 */
public class CorexVelocityLoader {

    private static final List<String> MIRRORS = List.of(
            "https://repo1.maven.org/maven2/",
            "https://maven-central.storage-download.googleapis.com/maven2/"
    );

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final List<Dependency> DEPENDENCIES = List.of(
            new Dependency("org/java-websocket/Java-WebSocket/1.5.6/Java-WebSocket-1.5.6.jar",
                    "ba2c5b646e115c6a9aa923139a154cbcdbf136b2b5c82bf423b1433639e0d83b"),
            new Dependency("com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar",
                    "a47a6ee62379694ee52c30036f0931b72f9aee2a801d590341ed82bd839e2134"),
            new Dependency("org/xerial/sqlite-jdbc/3.45.1.0/sqlite-jdbc-3.45.1.0.jar",
                    "f5f5404fa5a60f9e0b15e7bea2ea2d137e255f01babd0bfcb9dafcd2e3bf9cd2")
    );

    private final PluginManager pluginManager;
    private final Object plugin;
    private final Path libsDir;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    public CorexVelocityLoader(PluginManager pluginManager, Object plugin, Path dataFolder) {
        this.pluginManager = pluginManager;
        this.plugin = plugin;
        this.libsDir = dataFolder.resolve(".libs");
    }

    public void download() throws IOException {
        Files.createDirectories(libsDir);

        for (Dependency dependency : DEPENDENCIES) {
            Path jar = libsDir.resolve(dependency.fileName());

            if (Files.exists(jar) && dependency.matches(jar)) {
                continue;
            }
            if (Files.exists(jar)) {
                CorexLogger.warn("Library " + dependency.fileName() + " does not match its pinned hash, re-downloading.");
                Files.delete(jar);
            }
            fetch(dependency, jar);
        }
    }

    public void inject() {
        for (Dependency dependency : DEPENDENCIES) {
            Path jar = libsDir.resolve(dependency.fileName());
            if (!Files.exists(jar)) {
                CorexLogger.error("Library " + dependency.fileName() + " is missing, Corex will not work on this proxy.");
                continue;
            }
            pluginManager.addToClasspath(plugin, jar);
        }
    }

    private void fetch(Dependency dependency, Path target) throws IOException {
        IOException last = null;
        for (String mirror : MIRRORS) {
            try {
                downloadJar(mirror + dependency.path(), dependency, target);
                CorexLogger.info("Downloaded " + dependency.fileName() + " from " + mirror);
                return;
            }
            catch (IOException e) {
                last = e;
                CorexLogger.warn("Could not fetch " + dependency.fileName() + " from " + mirror + ": " + e.getMessage());
            }
        }
        throw last != null ? last : new IOException("No mirror configured for " + dependency.fileName());
    }

    private void downloadJar(String url, Dependency dependency, Path target) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            try (InputStream body = response.body()) {
                Files.copy(body, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!dependency.matches(temporary)) {
                throw new IOException("downloaded file does not match the pinned SHA-256");
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while downloading", e);
        }
        finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record Dependency(String path, String sha256) {

        String fileName() {
            return Path.of(path).getFileName().toString();
        }

        boolean matches(Path file) {
            try (InputStream in = Files.newInputStream(file);
                 DigestInputStream digestStream = new DigestInputStream(in, MessageDigest.getInstance("SHA-256"))) {
                digestStream.transferTo(OutputStream.nullOutputStream());
                return HexFormat.of().formatHex(digestStream.getMessageDigest().digest()).equalsIgnoreCase(sha256);
            }
            catch (IOException | NoSuchAlgorithmException e) {
                return false;
            }
        }
    }
}
