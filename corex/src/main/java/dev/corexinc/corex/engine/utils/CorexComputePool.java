package dev.corexinc.corex.engine.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinTask;
import java.util.function.IntConsumer;

/**
 * Shared worker pool for data-parallel numeric work inside tags and commands.
 * <p>
 * This is not a replacement for {@link SchedulerAdapter}. The scheduler decides which thread a
 * script runs on; this pool splits a single heavy operation (a matrix multiply, an activation
 * over a large vector) across cores and joins before the tag returns. The calling queue stays on
 * its own thread and observes the operation as ordinary blocking work, so nothing about queue
 * ordering, region safety, or async rules changes.
 * <p>
 * Splitting is not free: handing a chunk to another thread and joining it back costs roughly as
 * much as several thousand multiplications. {@link #parallelFor} therefore estimates the total
 * work first and runs inline when it is below {@link #threshold()}, which keeps small lists on
 * the fast path instead of paying dispatch for nothing.
 *
 * @since 1.0.0
 */
public final class CorexComputePool {

    /** Work units below which an operation always runs inline on the calling thread. */
    public static final long DEFAULT_THRESHOLD = 65_536L;

    private static final Object POOL_LOCK = new Object();

    private static volatile ForkJoinPool pool;
    private static volatile int parallelism = defaultParallelism();
    private static volatile long threshold = DEFAULT_THRESHOLD;

    private CorexComputePool() {}

    private static int defaultParallelism() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    }

    private static ForkJoinWorkerThread createWorker(ForkJoinPool owner) {
        ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(owner);
        worker.setName("Corex-Compute-" + worker.getPoolIndex());
        worker.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
        worker.setDaemon(true);
        return worker;
    }

    /**
     * Applies the configured thread count and split threshold.
     *
     * @param requestedThreads   worker threads, or {@code 0} for half the available cores
     * @param requestedThreshold work units below which an operation runs inline, or {@code 0} for the default
     */
    public static void configure(int requestedThreads, long requestedThreshold) {
        int resolvedThreads = requestedThreads > 0 ? requestedThreads : defaultParallelism();
        long resolvedThreshold = requestedThreshold > 0 ? requestedThreshold : DEFAULT_THRESHOLD;

        synchronized (POOL_LOCK) {
            if (resolvedThreads != parallelism && pool != null) {
                pool.shutdown();
                pool = null;
            }
            parallelism = resolvedThreads;
            threshold = resolvedThreshold;
        }
    }

    /**
     * Returns how many worker threads heavy numeric operations may spread across.
     */
    public static int parallelism() {
        return parallelism;
    }

    /**
     * Returns the work-unit count below which an operation runs inline.
     */
    public static long threshold() {
        return threshold;
    }

    /**
     * Releases the worker threads. Safe to call when the pool was never created.
     */
    public static void shutdown() {
        synchronized (POOL_LOCK) {
            if (pool != null) {
                pool.shutdown();
                pool = null;
            }
        }
    }

    /**
     * Tells whether the caller is already a worker of this pool, in which case splitting again
     * would only pile tasks onto threads that are themselves busy.
     * <p>
     * This deliberately does not use {@code ForkJoinTask.inForkJoinPool()}: that is true for a
     * worker of <em>any</em> pool, and server schedulers hand script work to their own ForkJoin
     * threads. Treating those as "already parallel" silently collapsed every matrix multiply onto
     * a single core.
     */
    private static boolean insideOwnPool() {
        ForkJoinPool current = pool;
        return current != null
                && Thread.currentThread() instanceof ForkJoinWorkerThread worker
                && worker.getPool() == current;
    }

    private static ForkJoinPool pool() {
        ForkJoinPool current = pool;
        if (current != null) return current;

        synchronized (POOL_LOCK) {
            if (pool == null) {
                pool = new ForkJoinPool(parallelism, CorexComputePool::createWorker, null, false);
            }
            return pool;
        }
    }

    /**
     * Runs {@code body} for every index in the range, spreading contiguous chunks across workers
     * when the estimated work justifies it and running inline otherwise.
     * <p>
     * {@code body} must only write to distinct per-index destinations. It is called from several
     * threads at once, so it must not touch queue state, live entities, or anything else that is
     * not thread-safe. The call blocks until every index has been processed; an exception thrown
     * by any chunk propagates to the caller.
     *
     * @param fromInclusive first index, inclusive
     * @param toExclusive   last index, exclusive
     * @param workPerIndex  rough cost of one index in multiplications, used to decide on splitting
     * @param body          the per-index operation
     */
    public static void parallelFor(int fromInclusive, int toExclusive, long workPerIndex, IntConsumer body) {
        int length = toExclusive - fromInclusive;
        if (length <= 0) return;

        int workers = parallelism;
        boolean worthSplitting = workers > 1
                && length > 1
                && (long) length * Math.max(1L, workPerIndex) >= threshold
                && !insideOwnPool();

        if (!worthSplitting) {
            for (int index = fromInclusive; index < toExclusive; index++) {
                body.accept(index);
            }
            return;
        }

        int chunkCount = Math.min(workers, length);
        int chunkSize = (length + chunkCount - 1) / chunkCount;
        ForkJoinPool activePool = pool();
        List<ForkJoinTask<?>> tasks = new ArrayList<>(chunkCount);

        for (int start = fromInclusive; start < toExclusive; start += chunkSize) {
            int chunkStart = start;
            int chunkEnd = Math.min(start + chunkSize, toExclusive);
            tasks.add(activePool.submit(() -> {
                for (int index = chunkStart; index < chunkEnd; index++) {
                    body.accept(index);
                }
            }));
        }

        for (ForkJoinTask<?> task : tasks) {
            task.join();
        }
    }
}
