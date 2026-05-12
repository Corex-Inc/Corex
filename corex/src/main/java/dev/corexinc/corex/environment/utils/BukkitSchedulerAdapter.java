package dev.corexinc.corex.environment.utils;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.exceptions.RegionRelocateException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.UUID;

import java.util.concurrent.TimeUnit;

public class BukkitSchedulerAdapter extends SchedulerAdapter {

    public static Location toLocation(Position position) {
        World world = position.world() != null ? Bukkit.getWorld(position.world()) : null;
        return new Location(world, position.x(), position.y(), position.z());
    }

    public static Position toPosition(Location location) {
        UUID worldId = location.getWorld() != null ? location.getWorld().getUID() : null;
        return Position.of(worldId, location.getX(), location.getY(), location.getZ());
    }

    public static void requireRegion(Location location) {
        Position pos = toPosition(location);
        if (SchedulerAdapter.get().needsRegionRelocation(pos))
            throw new RegionRelocateException(pos);
    }

    @Override
    public void run(Runnable task) {
        if (Corex.isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(Corex.getInstance(), task);
        } else {
            Bukkit.getScheduler().runTask(Corex.getInstance(), task);
        }
    }

    @Override
    public void runAt(Position position, Runnable task) {
        if (Corex.isFolia()) {
            Bukkit.getRegionScheduler().execute(Corex.getInstance(), toLocation(position), task);
        } else {
            Bukkit.getScheduler().runTask(Corex.getInstance(), task);
        }
    }

    @Override
    public void runLaterAt(Position position, Runnable task, long delayTicks) {
        if (Corex.isFolia()) {
            Bukkit.getRegionScheduler().runDelayed(
                    Corex.getInstance(),
                    toLocation(position),
                    scheduledTask -> task.run(),
                    Math.max(1, delayTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskLater(Corex.getInstance(), task, delayTicks);
        }
    }

    @Override
    public void runLater(Runnable task, long delayTicks) {
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

    @Override
    public void runRepeating(Runnable task, long delayTicks, long periodTicks) {
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

    @Override
    public void runAsync(Runnable task) {
        if (Corex.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(Corex.getInstance(), scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(Corex.getInstance(), task);
        }
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
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

    @Override
    public void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
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

    @Override
    public boolean needsRegionRelocation(Position position) {
        if (!Corex.isFolia()) return false;
        Location loc = toLocation(position);
        if (loc.getWorld() == null) return false;
        return !Bukkit.isOwnedByCurrentRegion(loc);
    }

    public void runEntity(Entity entity, Runnable task) {
        if (Corex.isFolia()) {
            entity.getScheduler().run(Corex.getInstance(), scheduledTask -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(Corex.getInstance(), task);
        }
    }

    public void runEntityLater(Entity entity, Runnable task, long delayTicks) {
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