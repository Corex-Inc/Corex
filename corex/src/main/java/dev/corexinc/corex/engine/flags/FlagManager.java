package dev.corexinc.corex.engine.flags;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.tags.core.DurationTag;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FlagManager {

    private static final PriorityQueue<FlagTask> queue = new PriorityQueue<>();
    private static final Object monitor = new Object();
    private static Thread sleeperThread;

    private static final ConcurrentHashMap<String, AtomicLong> versions = new ConcurrentHashMap<>();

    private static FlagExpirationHandler expirationHandler;

    public static void setExpirationHandler(FlagExpirationHandler handler) {
        expirationHandler = handler;
    }

    public static void init() {
        if (sleeperThread != null) return;

        sleeperThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                FlagTask nextTask;

                synchronized (monitor) {
                    if (queue.isEmpty()) {
                        try {
                            monitor.wait();
                        } catch (InterruptedException e) { break; }
                        continue;
                    }

                    nextTask = queue.peek();
                    long now = System.currentTimeMillis();
                    long timeToSleep = nextTask.expireTime - now;

                    if (timeToSleep > 0) {
                        try {
                            monitor.wait(timeToSleep);
                        } catch (InterruptedException e) { break; }
                        continue;
                    }

                    queue.poll();
                }

                handleExpiration(nextTask);
            }
        });

        sleeperThread.setName("Corex-Flag-Expiration-Thread");
        sleeperThread.setDaemon(true);
        sleeperThread.start();
    }

    public static void scheduleExpiration(AbstractFlagTracker tracker, String flagPath, long durationMs) {
        long expireTime = System.currentTimeMillis() + durationMs;
        long[] version = new long[1];
        versions.compute(versionKey(tracker.getTrackerId(), flagPath), (key, current) -> {
            AtomicLong counter = current != null ? current : new AtomicLong();
            version[0] = counter.incrementAndGet();
            return counter;
        });
        FlagTask task = new FlagTask(tracker, flagPath, expireTime, version[0]);

        synchronized (monitor) {
            queue.add(task);
            if (queue.peek() == task) {
                monitor.notify();
            }
        }
    }

    public static void cancelExpiration(String trackerId, String flagPath) {
        versions.computeIfPresent(versionKey(trackerId, flagPath), (key, version) -> {
            version.incrementAndGet();
            return version;
        });
    }

    private static String versionKey(String trackerId, String flagPath) {
        return trackerId + " " + flagPath;
    }

    private static boolean isCurrent(FlagTask task) {
        AtomicLong version = versions.get(versionKey(task.trackerId, task.flagPath));
        return version != null && version.get() == task.version;
    }

    private static void forget(FlagTask task) {
        versions.computeIfPresent(versionKey(task.trackerId, task.flagPath),
                (key, version) -> version.get() == task.version ? null : version);
    }

    private static void handleExpiration(FlagTask task) {
        if (!isCurrent(task)) return;

        Runnable handling = () -> {
            if (!isCurrent(task)) return;

            AbstractFlagTracker tracker = task.tracker;
            AbstractTag value = tracker.getFlag(task.flagPath);
            if (value == null) {
                forget(task);
                return;
            }

            AbstractTag decision = null;
            if (expirationHandler != null) {
                decision = expirationHandler.onExpired(task.trackerId, task.flagPath, value);
            }

            if (decision != null) {
                String result = decision.identify().toLowerCase();

                if (result.equals("true")) {
                    tracker.setFlag(task.flagPath, value, 0);
                    return;
                }

                if (decision instanceof DurationTag dt) {
                    tracker.setFlag(task.flagPath, value, dt.getMilliseconds());
                    return;
                }
            }

            if (tracker.isAsyncSafeCleanup()) {
                SchedulerAdapter.get().runAsync(() -> {
                    try {
                        tracker.deleteFlagPhysically(task.flagPath);
                    } catch (Exception e) {
                        CorexLogger.error("Error clearing flag in background " + task.flagPath + ": " + e.getMessage());
                    }
                    forget(task);
                });
            } else {
                tracker.deleteFlagPhysically(task.flagPath);
                forget(task);
            }
        };

        task.tracker.getSchedulerPosition().ifPresentOrElse(
                pos -> SchedulerAdapter.get().runAt(pos, handling),
                () -> SchedulerAdapter.get().run(handling)
        );
    }

    private static class FlagTask implements Comparable<FlagTask> {
        final AbstractFlagTracker tracker;
        final String trackerId;
        final String flagPath;
        final long expireTime;
        final long version;

        FlagTask(AbstractFlagTracker tracker, String flagPath, long expireTime, long version) {
            this.tracker = tracker;
            this.trackerId = tracker.getTrackerId();
            this.flagPath = flagPath;
            this.expireTime = expireTime;
            this.version = version;
        }

        @Override
        public int compareTo(FlagTask o) { return Long.compare(this.expireTime, o.expireTime); }
    }
}
