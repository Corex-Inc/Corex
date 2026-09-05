package dev.corexinc.corex;

import com.github.retrooper.packetevents.PacketEvents;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonResolver;
import dev.corexinc.corex.environment.addons.BukkitAddonResolver;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.network.NetworkSecret;
import dev.corexinc.corex.engine.network.WebSocketTransport;
import dev.corexinc.corex.engine.utils.CorexComputePool;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.environment.commands.core.FileCommand;
import dev.corexinc.corex.environment.commands.core.WhileCommand;
import dev.corexinc.corex.environment.network.BackendSecretResolver;
import dev.corexinc.corex.environment.network.BukkitNetworkExecutor;
import dev.corexinc.corex.environment.network.PluginMessageTransport;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.Modules;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import dev.corexinc.corex.environment.utils.ServerVersion;
import dev.corexinc.corex.environment.utils.scripts.EnvManager;
import dev.corexinc.corex.environment.utils.Metrics;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.EnvironmentLoader;
import dev.corexinc.corex.environment.containers.GeneratorContainer;
import dev.corexinc.corex.environment.containers.commands.CommandContainer;
import dev.corexinc.corex.environment.containers.commands.CommandManager;
import dev.corexinc.corex.environment.generators.ScriptedChunkGenerator;
import dev.corexinc.corex.environment.generators.VoidGenerator;
import dev.corexinc.corex.environment.utils.commands.impl.RunCommand;
import dev.corexinc.corex.environment.utils.scripts.WebSocketManager;
import dev.corexinc.corex.environment.utils.commands.impl.RunsCommand;
import dev.corexinc.corex.environment.tags.core.MapTag;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class Corex extends JavaPlugin {

    private static Corex instance;

    private CorexRegistry registry;

    private PluginMessageTransport pluginMessageTransport;

    private WebSocketTransport webSocketTransport;

    private static boolean IS_FOLIA = false;
    private static boolean IS_CANVAS = false;
    private static boolean IS_TEST = false;

    @Override
    public void onLoad() {
        instance = this;
        this.registry = new CorexRegistry();
        ScriptManager.setRegistry(registry);

        AddonManager.reset();
        AddonResolver.set(new BukkitAddonResolver());

        ServerVersion.setCurrent(Bukkit.getBukkitVersion().split("-")[0]);
        setupRuntimeFlags();

        if (isCanvas()) {
            Modules.setCurrent(Modules.CANVAS);
        }
        else if (isFolia()) {
            Modules.setCurrent(Modules.FOLIA);
        }
        else {
            Modules.setCurrent(Modules.PAPER);
        }

        if (!isTest()) {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
        }

        AddonManager.openScope(AddonOwner.CORE);
        try {
            EnvironmentLoader.registerDefaults(this.registry);
        }
        finally {
            AddonManager.closeScope(AddonOwner.CORE);
        }

        if (!new File(getDataFolder(), "secrets.env").exists()) {
            try {
                saveResource("secrets.env", false);
            } catch (IllegalArgumentException ignored) {}
        }
        EnvManager.load(getDataFolder());
    }

    @Override
    public void onEnable() {
        SchedulerAdapter.set(new BukkitSchedulerAdapter());
        CorexLogger.setConsole(Bukkit.getConsoleSender());

        if (!isTest()) {
            SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
            APIConfig settings = new APIConfig(PacketEvents.getAPI())
                    .tickTickables()
                    .usePlatformLogger();

            EntityLib.init(platform, settings);
        }

        silenceHikariLogs();
        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        FlagManager.init();
        Debugger.updateDebugMode(getConfig().getString("logger.debug-mode", "default"));
        CorexComputePool.configure(
                getConfig().getInt("compute.threads", 0),
                getConfig().getLong("compute.split-threshold", CorexComputePool.DEFAULT_THRESHOLD));
        FileCommand.setRoot(getDataFolder().toPath().resolve(getConfig().getString("file.folder", "files")));
        SqlFlagTracker.setCacheSize(getConfig().getInt("flags.sql-cache-size", 0));
        WhileCommand.setMaxIterations(getConfig().getInt("scripts.while-max-iterations", 0));
        CommandManager.INSTANCE.setCacheTtls(
                getConfig().getLong("commands.requires-cache-ms", 0L),
                getConfig().getLong("commands.suggests-cache-ms", 0L));

        setupNetwork();

        int pluginId = 30505;
        new Metrics(this, pluginId);

        registerCommands();

        ScriptManager.setDataFolder(getDataFolder().toPath());
        AddonManager.seal();
        ScriptManager.loadScripts();

        CommandManager.INSTANCE.updateContainers(
                ScriptManager.getContainersByType(CommandContainer.class)
        );
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if ("void".equals(id)) {
            return new VoidGenerator();
        }
        if (id != null && !id.isEmpty()) {
            String containerId = id;
            MapTag instanceDefs = new MapTag();

            int bracket = id.indexOf('[');
            if (bracket != -1 && id.endsWith("]")) {
                containerId = id.substring(0, bracket);
                instanceDefs = new MapTag(id.substring(bracket + 1, id.length() - 1));
            }

            AbstractContainer container = ScriptManager.getContainer(containerId);
            if (container instanceof GeneratorContainer) {
                return new ScriptedChunkGenerator(containerId, instanceDefs);
            }
            CorexLogger.warn("Unknown generator id '" + containerId + "'");
        }
        return null;
    }

    @Override
    public void onDisable() {
        CorexLogger.info("<#ffaa00>Corex is shutting down...</#ffaa00>");
        try {
            WebSocketManager.disconnectAll();
        } catch (Throwable ignored) {}

        if (webSocketTransport != null) {
            webSocketTransport.shutdown();
            webSocketTransport = null;
        }
        if (pluginMessageTransport != null) {
            pluginMessageTransport.shutdown();
            pluginMessageTransport = null;
        }
        NetworkManager.shutdown();

        FlagManager.shutdown();
        DatabaseManager.closeAll();
        CorexComputePool.shutdown();
        FileCommand.clearCache();

        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
    }

    public static Corex getInstance() {
        return instance;
    }

    public CorexRegistry getRegistry() {
        return registry;
    }

    public @Nullable NetworkSecret applyNetworkConfig() {
        if (isTest() || !getConfig().getBoolean("network.enabled", true)) {
            return null;
        }

        NetworkSecret secret = BackendSecretResolver.resolve(this,
                getConfig().getBoolean("network.use-proxy-secret", true));

        NetworkManager.configure(secret != null ? secret.value() : null,
                getConfig().getBoolean("network.allow-remote-execution", false),
                getConfig().getLong("network.replay-window-seconds", 30L) * 1000L,
                getConfig().getInt("network.unsigned-rate-limit", NetworkManager.DEFAULT_UNSIGNED_RATE_LIMIT));
        return secret;
    }

    private void setupNetwork() {
        if (isTest() || !getConfig().getBoolean("network.enabled", true)) {
            return;
        }

        boolean allowRemoteExecution = getConfig().getBoolean("network.allow-remote-execution", false);
        NetworkSecret secret = applyNetworkConfig();

        NetworkManager.setExecutionHandler(new BukkitNetworkExecutor());

        if (secret != null) {
            CorexLogger.info("Corex network is signing with " + secret.source() + ".");
        }

        if (!getConfig().getBoolean("network.websocket.enabled", false) || !setupWebSocket()) {
            pluginMessageTransport = new PluginMessageTransport(this);
            pluginMessageTransport.init();
        }

        if (allowRemoteExecution && !NetworkManager.hasSecret()) {
            CorexLogger.warn("network.allow-remote-execution is on but Corex found no shared secret, "
                    + "so remote scripts will be refused. Either switch on Velocity forwarding, "
                    + "or set CX_NETWORK_SECRET in secrets.env on every server.");
        }
    }

    /**
     * Opens the socket to the proxy, or reports why it cannot and leaves the caller to fall back
     * to plugin messaging.
     *
     * <p>A misconfigured socket drops the server back onto the transport it had before rather than
     * leaving it with none: the alternative is a server that looks fine and silently talks to
     * nobody.</p>
     *
     * @return true when the socket transport was installed.
     */
    private boolean setupWebSocket() {
        if (!NetworkManager.hasSecret()) {
            CorexLogger.error("network.websocket needs a shared secret and Corex found none. "
                    + "Switch on Velocity forwarding, or set CX_NETWORK_SECRET in secrets.env. "
                    + "Falling back to plugin messaging.");
            return false;
        }

        String url = getConfig().getString("network.websocket.url", "");
        String serverName = getConfig().getString("network.websocket.server-name", "");

        if (url == null || url.isBlank() || serverName == null || serverName.isBlank()) {
            CorexLogger.error("network.websocket needs both url and server-name set. "
                    + "Falling back to plugin messaging.");
            return false;
        }

        URI uri;
        try {
            uri = new URI(url);
        }
        catch (URISyntaxException e) {
            CorexLogger.error("network.websocket.url is not a valid URI: " + e.getMessage()
                    + ". Falling back to plugin messaging.");
            return false;
        }

        webSocketTransport = new WebSocketTransport(uri, serverName,
                getConfig().getLong("network.websocket.reconnect-seconds", 10L));
        webSocketTransport.init();
        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands() {
        if (!isTest()) {
            try {
                getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    event.registrar().register("run", new RunCommand());
                    event.registrar().register("runs", new RunsCommand());
                    CommandManager.INSTANCE.syncAll(event.registrar());
                });
            } catch (NoClassDefFoundError | Exception | NoSuchMethodError e) {
                CorexLogger.warn("Failed to register Brigadier commands. Possibly an outdated version of Paper?");
            }
        }
    }

    private void silenceHikariLogs() {
        try {
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object warnLevel = levelClass.getField("WARN").get(null);

            Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            java.lang.reflect.Method setLevel = configuratorClass.getMethod("setLevel", String.class, levelClass);

            setLevel.invoke(null, "com.zaxxer.hikari", warnLevel);
            setLevel.invoke(null, "com.zaxxer.hikari.pool.HikariPool", warnLevel);
            setLevel.invoke(null, "com.zaxxer.hikari.HikariDataSource", warnLevel);
        } catch (Exception ignored) {}
    }

    public void setupRuntimeFlags() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            IS_FOLIA = true;
        } catch (ClassNotFoundException e) {
            IS_FOLIA = false;
        }

        try {
            Class.forName("io.canvasmc.canvas.region.WorldRegionizer");
            IS_CANVAS = true;
        } catch (ClassNotFoundException e) {
            IS_CANVAS = false;
        }

        IS_TEST = Bukkit.getName().equalsIgnoreCase("ServerMock");
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static boolean isTest() {
        return IS_TEST;
    }

    public static boolean isCanvas() {
        return IS_CANVAS;
    }
}