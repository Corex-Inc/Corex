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

        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", "https://maven-central.storage-download.googleapis.com/maven2"
        ).build());

        builder.addLibrary(resolver);
    }
}