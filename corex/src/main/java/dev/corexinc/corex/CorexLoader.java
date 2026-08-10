package dev.corexinc.corex;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class CorexLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder builder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addDependency(new Dependency(new DefaultArtifact("org.java-websocket:Java-WebSocket:1.5.6"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:5.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.xerial:sqlite-jdbc:3.45.1.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.retrooper:packetevents-spigot:2.13.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("io.github.tofaa2:spigot:3.3.7-SNAPSHOT"), null));

        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", "https://maven-central.storage-download.googleapis.com/maven2"
        ).build());

        resolver.addRepository(new RemoteRepository.Builder(
                "codemc-releases", "default", "https://repo.codemc.io/repository/maven-releases/"
        ).build());

        resolver.addRepository(new RemoteRepository.Builder(
                "codemc-snapshots", "default", "https://repo.codemc.io/repository/maven-snapshots/"
        ).build());

        resolver.addRepository(new RemoteRepository.Builder(
                "tofaa", "default", "https://maven.pvphub.me/tofaa"
        ).build());

        builder.addLibrary(resolver);
    }
}