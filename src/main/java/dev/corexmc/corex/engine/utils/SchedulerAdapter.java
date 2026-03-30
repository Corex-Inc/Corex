package dev.corexmc.corex.engine.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SchedulerAdapter {

    private final Plugin plugin;
    private final boolean folia;

    public SchedulerAdapter(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    public void run(Runnable task) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAt(Location location, Runnable task) {
        if (folia) {
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runLater(Runnable task, long delayTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public void runRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> task.run(),
                    delayTicks,
                    periodTicks
            );
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public void runEntity(Entity entity, Runnable task) {
        if (folia) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }

    }

}