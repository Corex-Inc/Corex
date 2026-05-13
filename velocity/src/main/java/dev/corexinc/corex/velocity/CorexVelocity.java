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
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.utils.scripts.EnvManager;
import dev.corexinc.corex.velocity.utils.VelocitySchedulerAdapter;

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
    private final Path dataDirectory;
    private CorexRegistry registry;

    @Inject
    public CorexVelocity(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        instance = this;

        CorexLogger.setConsole(server.getConsoleCommandSource());
        SchedulerAdapter.set(new VelocitySchedulerAdapter(server, this));

        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");

        FlagManager.init();
        Debugger.updateDebugMode("ALL");

        this.registry = new CorexRegistry();
        VelocityEnvironmentLoader.registerDefaults(this.registry);

        ScriptManager.setDataFolder(dataDirectory);
        ScriptManager.setRegistry(registry);
        ScriptManager.loadScripts();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        CorexLogger.info("<#ffaa00>Corex is shutting down...</#ffaa00>");
        DatabaseManager.closeAll();
    }

    public static CorexVelocity getInstance() { return instance; }
    public CorexRegistry getRegistry() { return registry; }
    public ProxyServer getServer() { return server; }
    public Path getDataDirectory() { return dataDirectory; }

}