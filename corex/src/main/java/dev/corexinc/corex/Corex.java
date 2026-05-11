package dev.corexinc.corex;

import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.EnvManager;
import dev.corexinc.corex.engine.utils.Metrics;
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
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class Corex extends JavaPlugin {

    private static Corex instance;

    private CorexRegistry registry;

    private static boolean IS_FOLIA  = false;
    private static boolean IS_CANVAS = false;
    private static boolean IS_TEST   = false;

    public static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    @Override
    public void onEnable() {
        instance = this;
        silenceHikariLogs();
        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");

        getConfig().options().copyDefaults();
        saveDefaultConfig();
        FlagManager.init();
        Debugger.updateDebugMode();

        setupRuntimeFlags();

        this.registry = new CorexRegistry();
        EnvironmentLoader.registerDefaults(this.registry);
        EnvManager.load();

        int pluginId = 30505;
        new Metrics(this, pluginId);

        registerCommands();

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

        DatabaseManager.closeAll();
    }

    public static Corex getInstance() { return instance; }

    public CorexRegistry getRegistry() { return registry; }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands() {
        if (!isTest()) {
            try {
                getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    event.registrar().register("run",  new RunCommand());
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

    public static boolean isFolia()  { return IS_FOLIA; }
    public static boolean isTest()   { return IS_TEST; }
    public static boolean isCanvas() { return IS_CANVAS; }
}