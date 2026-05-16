package dev.corexinc.corex.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.environment.utils.ServerVersion;
import dev.corexinc.corex.velocity.environment.utils.ConfigManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.velocity.environment.utils.VelocityEnvironmentLoader;
import dev.corexinc.corex.velocity.environment.utils.VelocitySchedulerAdapter;
import dev.corexinc.corex.velocity.environment.utils.commands.impl.VRunCommand;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "corex",
        name = "Corex",
        version = "1.0",
        description = "Modern, Paper-exclusive compiled scripting engine",
        authors = { "tizis0", "Nybik_YT" }
)
public class CorexVelocity {

    private static CorexVelocity instance;
    private final ProxyServer server;
    private final Path dataFolder;
    private final CorexVelocityLoader loader;
    private CorexRegistry registry;
    private ConfigManager config;

    @Inject
    public CorexVelocity(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataFolder = dataDirectory;
        this.loader = new CorexVelocityLoader(server.getPluginManager(), this, dataFolder);

        CorexLogger.setConsole(server.getConsoleCommandSource());
        try {
            this.loader.download();
        } catch (IOException e) {
            CorexLogger.error("Failed to download libraries: " + e.getMessage());
        }
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        instance = this;

        SchedulerAdapter.set(new VelocitySchedulerAdapter(server, this));

        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");
        ServerVersion.setCurrent(server.getVersion().getVersion().split("-")[0]);

        loader.inject();

        this.config = new ConfigManager(dataFolder, "config.yml");
        this.config.load();

        FlagManager.init();
        Debugger.updateDebugMode(config.getString("logger.debug-mode", "default"));

        this.registry = new CorexRegistry();
        VelocityEnvironmentLoader.registerDefaults(this.registry);

        ScriptManager.setDataFolder(dataFolder);
        ScriptManager.setRegistry(registry);
        ScriptManager.loadScripts();

        registerCommands();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        CorexLogger.info("<#ffaa00>Corex is shutting down...</#ffaa00>");
        DatabaseManager.closeAll();
    }

    public void registerCommands() {
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("vrun")
                        .plugin(this)
                        .build(),
                new VRunCommand()
        );
    }

    public static CorexVelocity getInstance() { return instance; }
    public CorexRegistry getRegistry() { return registry; }
    public ProxyServer getServer() { return server; }
    public Path getDataFolder() { return dataFolder; }
    public ConfigManager getConfig() { return config; }
}