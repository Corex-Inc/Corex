package dev.corexinc.corex;

import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.EnvManager;
import dev.corexinc.corex.engine.utils.Metrics;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.EnvironmentLoader;
import dev.corexinc.corex.environment.utils.commands.RunCommand;
import dev.corexinc.corex.environment.utils.WebSocketManager;
import dev.corexinc.corex.environment.utils.commands.RunsCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Corex extends JavaPlugin {

    private static Corex instance;

    private CorexRegistry registry;

    private static boolean IS_FOLIA = false;
    private static boolean IS_TEST = false;

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
    }

    @Override
    public void onDisable() {
        CorexLogger.info("<#ffaa00>Corex is shutting down...</#ffaa00>");
        try {
            WebSocketManager.disconnectAll();
        } catch (Throwable ignored) {}

        DatabaseManager.closeAll();
    }

    public static Corex getInstance() {
        return instance;
    }

    public CorexRegistry getRegistry() {
        return registry;
    }

    public void registerCommands() {
        if (!isTest()) {
            try {
                getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    event.registrar().register(
                            "run",
                            new RunCommand()
                    );
                });
                getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
                    event.registrar().register(
                            "runs",
                            new RunsCommand()
                    );
                });
            }
            catch (NoClassDefFoundError | Exception | NoSuchMethodError e) {
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

        IS_TEST = Bukkit.getName().equalsIgnoreCase("ServerMock");
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static boolean isTest() {
        return IS_TEST;
    }
}