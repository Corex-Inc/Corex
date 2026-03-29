package dev.corexmc.corex;

import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.environment.EnvironmentLoader;
import dev.corexmc.corex.environment.commands.ExCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Corex extends JavaPlugin {

    private static Corex instance;

    private static boolean isFolia = false;

    private CorexRegistry registry;

    @Override
    public void onEnable() {
        instance = this;

        int scriptCount = 108;

        CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");

        this.registry = new CorexRegistry();
        EnvironmentLoader.registerDefaults(this.registry);

        instance.getServer().getCommandMap().register("corex", new ExCommand());

//        CorexLogger.success("Loaded <aqua>" + scriptCount + "</aqua> scripts.");

//        CorexLogger.warn("Missing dependency 'Vault', economy features disabled.");

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public void onDisable() {
        CorexLogger.info("<#ffaa00>Corex shutting down...</#ffaa00>");
    }

    public static Corex getInstance() {
        return instance;
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public CorexRegistry getRegistry() {
        return registry;
    }
}