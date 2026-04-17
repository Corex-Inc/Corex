package dev.corexinc.corex.engine.utils;

import dev.corexinc.corex.Corex;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

public class SchedulerAdapter {

    // Runs a task on the global region (main thread equivalent in Folia)
    public static void run(Runnable task) {
        if (Corex.isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(Corex.getInstance(), task);
        } else {
            Bukkit.getScheduler().runTask(Corex.getInstance(), task);
        }
    }

    // Runs a task on the region that owns the given location
    public static void runAt(Location location, Runnable task) {
        if (Corex.isFolia()) {
            Bukkit.getRegionScheduler().execute(Corex.getInstance(), location, task);
        } else {
            Bukkit.getScheduler().runTask(Corex.getInstance(), task);
        }
    }

    // Runs a task after a delay on the region that owns the given location
    public static void runLaterAt(Location location, Runnable task, long delayTicks) {
        if (Corex.isFolia()) {
            Bukkit.getRegionScheduler().runDelayed(
                    Corex.getInstance(),
                    location,
                    scheduledTask -> task.run(),
                    Math.max(1, delayTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskLater(Corex.getInstance(), task, delayTicks);
        }
    }

    // Runs a task after a delay (in ticks)
    public static void runLater(Runnable task, long delayTicks) {
        if (Corex.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(
                    Corex.getInstance(),
                    scheduledTask -> task.run(),
                    Math.max(1, delayTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskLater(Corex.getInstance(), task, delayTicks);
        }
    }

    // Runs a repeating task (in ticks)
    public static void runRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (Corex.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    Corex.getInstance(),
                    scheduledTask -> task.run(),
                    Math.max(1, delayTicks),
                    Math.max(1, periodTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskTimer(Corex.getInstance(), task, delayTicks, periodTicks);
        }
    }

    // Runs a task asynchronously
    public static void runAsync(Runnable task) {
        if (Corex.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(Corex.getInstance(), scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(Corex.getInstance(), task);
        }
    }

    // Runs an async task after a delay (in ticks, converted to ms for Folia)
    public static void runAsyncLater(Runnable task, long delayTicks) {
        if (Corex.isFolia()) {
            long delayMs = delayTicks * 50L;
            Bukkit.getAsyncScheduler().runDelayed(
                    Corex.getInstance(),
                    scheduledTask -> task.run(),
                    Math.max(1, delayMs),
                    TimeUnit.MILLISECONDS
            );
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(Corex.getInstance(), task, delayTicks);
        }
    }

    // Runs a repeating async task (in ticks, converted to ms for Folia)
    public static void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (Corex.isFolia()) {
            long delayMs = Math.max(1, delayTicks * 50L);
            long periodMs = Math.max(1, periodTicks * 50L);
            Bukkit.getAsyncScheduler().runAtFixedRate(
                    Corex.getInstance(),
                    scheduledTask -> task.run(),
                    delayMs,
                    periodMs,
                    TimeUnit.MILLISECONDS
            );
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(Corex.getInstance(), task, delayTicks, periodTicks);
        }
    }

    // Runs a task tied to a specific entity
    public static void runEntity(Entity entity, Runnable task) {
        if (Corex.isFolia()) {
            // Third arg is the retired callback (runs if entity is removed before execution)
            entity.getScheduler().run(Corex.getInstance(), scheduledTask -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(Corex.getInstance(), task);
        }
    }

    // Runs a delayed task tied to a specific entity
    public static void runEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (Corex.isFolia()) {
            entity.getScheduler().runDelayed(
                    Corex.getInstance(),
                    scheduledTask -> task.run(),
                    null,
                    Math.max(1, delayTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskLater(Corex.getInstance(), task, delayTicks);
        }
    }
}