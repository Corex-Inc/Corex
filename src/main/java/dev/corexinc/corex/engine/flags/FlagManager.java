package dev.corexinc.corex.engine.flags;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.LocationPdcFlagTracker;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.tags.core.DurationTag;

import java.util.PriorityQueue;

public class FlagManager {

    private static final PriorityQueue<FlagTask> queue = new PriorityQueue<>();
    private static final Object monitor = new Object();
    private static Thread sleeperThread;

    private static FlagExpirationHandler expirationHandler;

    public static void setExpirationHandler(FlagExpirationHandler handler) {
        expirationHandler = handler;
    }

    public static void init() {
        if (sleeperThread != null) return;

        sleeperThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                FlagTask nextTask = null;

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

    public static void scheduleExpiration(String trackerId, String flagPath, long durationMs) {
        long expireTime = System.currentTimeMillis() + durationMs;
        FlagTask task = new FlagTask(trackerId, flagPath, expireTime);

        synchronized (monitor) {
            queue.add(task);
            if (queue.peek() == task) {
                monitor.notify();
            }
        }
    }

    private static void handleExpiration(FlagTask task) {
        AbstractFlagTracker tracker = AbstractFlagTracker.getTracker(task.trackerId);
        if (tracker == null) return;

        AbstractTag value = tracker.getFlag(task.flagPath);
        if (value == null) return;

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
            SchedulerAdapter.runAsync(() -> {
                try {
                    tracker.deleteFlagPhysically(task.flagPath);
                } catch (Exception e) {
                    CorexLogger.error("Error clearing flag in background " + task.flagPath + ": " + e.getMessage());
                }
            });
        } else if (tracker instanceof LocationPdcFlagTracker locTracker) {
            SchedulerAdapter.runAt(locTracker.getLocation(), () -> {
                tracker.deleteFlagPhysically(task.flagPath);
            });
        } else {
            SchedulerAdapter.run(() -> {
                tracker.deleteFlagPhysically(task.flagPath);
            });
        }
    }

    private static class FlagTask implements Comparable<FlagTask> {
        final String trackerId;
        final String flagPath;
        final long expireTime;

        FlagTask(String t, String f, long e) { trackerId = t; flagPath = f; expireTime = e; }
        @Override public int compareTo(FlagTask o) { return Long.compare(this.expireTime, o.expireTime); }
    }
}