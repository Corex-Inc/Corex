package dev.corexmc.corex;

import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.scripts.ScriptManager;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.engine.utils.EnvManager;
import dev.corexmc.corex.environment.EnvironmentLoader;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.environment.utils.commands.RunCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Corex extends JavaPlugin {

    private static Corex instance;

    private CorexRegistry registry;

    @Override
    public void onEnable() {
        instance = this;
        if(!getDataFolder().exists()) CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        this.registry = new CorexRegistry();
        EnvironmentLoader.registerDefaults(this.registry);
        EnvManager.load(getDataFolder());

        try {
            registerCommand("run", new RunCommand());
        }
        catch (NoClassDefFoundError | NoSuchMethodError e) {
            CorexLogger.warn("Не удалось зарегистрировать Brigadier команды. Возможно, старая версия Paper?");
        }


        ScriptManager.loadScripts(new File(getDataFolder().toURI()));
    }

    @Override
    public void onDisable() {
        CorexLogger.info("<#ffaa00>Corex shutting down...</#ffaa00>");
    }

    public static Corex getInstance() {
        return instance;
    }

    public CorexRegistry getRegistry() {
        return registry;
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isTest() {
        return Bukkit.getName().equalsIgnoreCase("MockBukkit");
    }
}