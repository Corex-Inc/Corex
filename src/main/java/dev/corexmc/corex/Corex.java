package dev.corexmc.corex;

import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.scripts.ScriptManager;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.environment.EnvironmentLoader;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.environment.utils.commands.RunCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Corex extends JavaPlugin {

    private static Corex instance;

    private static SchedulerAdapter schedulerAdapter;

    private CorexRegistry registry;



    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        instance = this;
        schedulerAdapter = new SchedulerAdapter(this, isFolia());
        if(!getDataFolder().exists()) CorexLogger.info("<#8ce6ff>Welcome to Corex<white>!");
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        this.registry = new CorexRegistry();
        EnvironmentLoader.registerDefaults(this.registry);

        registerCommand("run", new RunCommand());

        this.scriptManager = new ScriptManager();
        this.scriptManager.loadScripts(new File(getDataFolder(), "scripts"));

//        CorexLogger.success("Loaded <aqua>" + scriptCount + "</aqua> scripts.");

//        CorexLogger.warn("Missing dependency 'Vault', economy features disabled.");

    }

    @Override
    public void onDisable() {
        CorexLogger.info("<#ffaa00>Corex shutting down...</#ffaa00>");
    }

    public static Corex getInstance() {
        return instance;
    }
    public static SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }


    public CorexRegistry getRegistry() {
        return registry;
    }

    public ScriptManager getScriptManager() { return scriptManager; }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}