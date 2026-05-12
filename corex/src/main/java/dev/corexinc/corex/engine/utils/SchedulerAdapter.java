package dev.corexinc.corex.engine.utils;

/**
 * Platform-agnostic scheduling abstraction for the Corex engine.
 * <p>
 * Register the platform implementation once at startup via {@link #set(SchedulerAdapter)}.
 * All engine code calls {@link #get()} to retrieve the active instance — no Bukkit,
 * Paper, or Velocity imports leak into platform-neutral modules.
 * <p>
 * Bukkit/Folia: register {@code BukkitSchedulerAdapter}.<br>
 * Velocity: register your own implementation extending this class.
 */
public abstract class SchedulerAdapter {

    private static SchedulerAdapter instance;

    /** Returns the active platform adapter. Throws if not yet registered. */
    public static SchedulerAdapter get() {
        if (instance == null) throw new IllegalStateException("SchedulerAdapter not initialised");
        return instance;
    }

    /** Registers the platform adapter. Call once during plugin/proxy startup. */
    public static void set(SchedulerAdapter adapter) {
        instance = adapter;
    }

    /** Runs a task on the main/global thread. */
    public abstract void run(Runnable task);

    /**
     * Runs a task on the region that owns {@code position}.
     * On Velocity (no regions) this is equivalent to {@link #run(Runnable)}.
     */
    public abstract void runAt(Position position, Runnable task);

    /**
     * Runs a task after {@code delayTicks} ticks on the region that owns {@code position}.
     * On Velocity this is equivalent to {@link #runLater(Runnable, long)}.
     */
    public abstract void runLaterAt(Position position, Runnable task, long delayTicks);

    /** Runs a task after {@code delayTicks} ticks on the main/global thread. */
    public abstract void runLater(Runnable task, long delayTicks);

    /** Runs a repeating task on the main/global thread. */
    public abstract void runRepeating(Runnable task, long delayTicks, long periodTicks);

    /** Runs a task asynchronously. */
    public abstract void runAsync(Runnable task);

    /** Runs an async task after a delay. */
    public abstract void runAsyncLater(Runnable task, long delayTicks);

    /** Runs a repeating async task. */
    public abstract void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);

    /**
     * Returns {@code true} if the current thread does NOT own the region for {@code position}
     * and a relocation is required. On Velocity always returns {@code false}.
     */
    public abstract boolean needsRegionRelocation(Position position);
}