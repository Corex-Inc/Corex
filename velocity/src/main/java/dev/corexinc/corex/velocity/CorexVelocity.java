package dev.corexinc.corex.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonResolver;
import dev.corexinc.corex.velocity.environment.addons.VelocityAddonResolver;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.network.NetworkSecret;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.environment.commands.core.WhileCommand;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.environment.utils.ServerVersion;
import dev.corexinc.corex.environment.utils.scripts.EnvManager;
import dev.corexinc.corex.environment.utils.scripts.WebSocketManager;
import dev.corexinc.corex.velocity.environment.utils.ConfigManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.Modules;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.velocity.environment.VelocityEnvironmentLoader;
import dev.corexinc.corex.velocity.environment.network.ProxyRelay;
import dev.corexinc.corex.velocity.environment.network.ProxySecretResolver;
import org.jspecify.annotations.Nullable;
import dev.corexinc.corex.velocity.environment.network.VelocityNetworkExecutor;
import dev.corexinc.corex.velocity.environment.utils.VelocitySchedulerAdapter;
import dev.corexinc.corex.velocity.environment.utils.commands.impl.VRunCommand;
import dev.corexinc.corex.velocity.environment.utils.commands.impl.VRunsCommand;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    private ProxyRelay proxyRelay;

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

    /**
     * Builds the engine before anything else on the proxy gets to see the initialize event, so an
     * addon subscribing to it at normal priority finds the registries ready.
     *
     * <p>The ordering is stated twice because Velocity moved from {@code order} to {@code priority}
     * and honours whichever of the two its build understands.</p>
     */
    @SuppressWarnings("deprecation")
    @Subscribe(order = PostOrder.FIRST, priority = Short.MAX_VALUE)
    public void onInit(ProxyInitializeEvent event) {
        instance = this;

        Modules.setCurrent(Modules.VELOCITY);
        SchedulerAdapter.set(new VelocitySchedulerAdapter(server, this));

        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");
        ServerVersion.setCurrent(server.getVersion().getVersion().split("-")[0]);

        loader.inject();

        this.config = new ConfigManager(dataFolder, "config.yml");
        this.config.load();

        FlagManager.init();
        EnvManager.load(dataFolder.toFile());
        Debugger.updateDebugMode(config.getString("logger.debug-mode", "default"));
        SqlFlagTracker.setCacheSize(config.getInt("flags.sql-cache-size", 0));
        WhileCommand.setMaxIterations(config.getInt("scripts.while-max-iterations", 0));

        AddonManager.reset();
        AddonResolver.set(new VelocityAddonResolver(server, this));

        this.registry = new CorexRegistry();
        ScriptManager.setDataFolder(dataFolder);
        ScriptManager.setRegistry(registry);

        AddonManager.openScope(AddonOwner.CORE);
        try {
            VelocityEnvironmentLoader.registerDefaults(this.registry);
        }
        finally {
            AddonManager.closeScope(AddonOwner.CORE);
        }

        setupNetwork();
        registerCommands();
    }

    /**
     * Compiles the scripts once every other plugin has had its turn at the initialize event, which
     * is the proxy's equivalent of compiling after the last {@code onLoad()}. Addons register in
     * between the two handlers.
     */
    @SuppressWarnings("deprecation")
    @Subscribe(order = PostOrder.LAST, priority = Short.MIN_VALUE)
    public void onAddonsRegistered(ProxyInitializeEvent event) {
        AddonManager.seal();
        ScriptManager.loadScripts();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        CorexLogger.info("<#ffaa00>Corex is shutting down...</#ffaa00>");
        try {
            WebSocketManager.disconnectAll();
        } catch (Throwable ignored) {}

        if (proxyRelay != null) {
            proxyRelay.shutdown();
            proxyRelay = null;
        }
        NetworkManager.shutdown();
        FlagManager.shutdown();
        DatabaseManager.closeAll();
    }

    /**
     * Brings up the relay that forwards Corex packets between backends, and lets scripts running
     * on the proxy send their own.
     *
     * <p>The relay is also the only thing standing between a modded client and a backend's packet
     * listener, since Velocity forwards a client sent plugin message by default.</p>
     */
    public @Nullable NetworkSecret applyNetworkConfig() {
        if (!config.getBoolean("network.enabled", true)) {
            return null;
        }

        NetworkSecret secret = ProxySecretResolver.resolve(dataFolder,
                config.getBoolean("network.use-proxy-secret", true));

        NetworkManager.configure(secret != null ? secret.value() : null,
                config.getBoolean("network.allow-remote-execution", false),
                config.getInt("network.replay-window-seconds", 30) * 1000L,
                config.getInt("network.unsigned-rate-limit", NetworkManager.DEFAULT_UNSIGNED_RATE_LIMIT));
        return secret;
    }

    private void setupNetwork() {
        if (!config.getBoolean("network.enabled", true)) {
            return;
        }

        NetworkSecret secret = applyNetworkConfig();

        if (secret != null) {
            CorexLogger.info("Corex network is signing with " + secret.source() + ".");
        }

        NetworkManager.setExecutionHandler(new VelocityNetworkExecutor(server));

        proxyRelay = new ProxyRelay(server);
        proxyRelay.init();
        server.getEventManager().register(this, proxyRelay);

        if (config.getBoolean("network.websocket.enabled", false)) {
            proxyRelay.startWebSocket(new InetSocketAddress(
                    config.getString("network.websocket.bind", "127.0.0.1"),
                    config.getInt("network.websocket.port", 25599)));
        }

        if (!NetworkManager.hasSecret()) {
            CorexLogger.warn("Corex found no shared secret. It will relay plain messages, but remote "
                    + "scripts and the websocket stay refused. Switch on modern forwarding, or set "
                    + "CX_NETWORK_SECRET in secrets.env on every server.");
        }
    }

    public void registerCommands() {
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("vrun")
                        .plugin(this)
                        .build(),
                new VRunCommand()
        );

        VRunsCommand runsCommand = new VRunsCommand();

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("vruns")
                        .plugin(this)
                        .build(),
                runsCommand
        );

        server.getEventManager().register(this, runsCommand);
    }

    public static CorexVelocity getInstance() { return instance; }
    public CorexRegistry getRegistry() { return registry; }
    public ProxyServer getServer() { return server; }
    public Path getDataFolder() { return dataFolder; }
    public ConfigManager getConfig() { return config; }
}