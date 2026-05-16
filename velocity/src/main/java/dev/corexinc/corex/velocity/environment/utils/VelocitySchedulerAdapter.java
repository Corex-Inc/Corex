package dev.corexinc.corex.velocity.environment.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;

import java.util.concurrent.TimeUnit;

public class VelocitySchedulerAdapter extends SchedulerAdapter {

    private static final long MILLIS_PER_TICK = 50L;

    private final ProxyServer server;
    private final Object plugin;

    public VelocitySchedulerAdapter(ProxyServer server, Object plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void run(Runnable task) {
        server.getScheduler()
                .buildTask(plugin, task)
                .schedule();
    }

    @Override
    public void runAt(Position position, Runnable task) {
        run(task);
    }

    @Override
    public void runLaterAt(Position position, Runnable task, long delayTicks) {
        runLater(task, delayTicks);
    }

    @Override
    public void runLater(Runnable task, long delayTicks) {
        runLaterMillis(task, delayTicks * MILLIS_PER_TICK);
    }

    @Override
    public void runRepeating(Runnable task, long delayTicks, long periodTicks) {
        runRepeatingMillis(task, delayTicks * MILLIS_PER_TICK, periodTicks * MILLIS_PER_TICK);
    }

    @Override
    public void runAsync(Runnable task) {
        run(task);
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        runLater(task, delayTicks);
    }

    @Override
    public void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        runRepeating(task, delayTicks, periodTicks);
    }

    @Override
    public boolean needsRegionRelocation(Position position) {
        return false;
    }

    public void runLaterMillis(Runnable task, long millis) {
        server.getScheduler()
                .buildTask(plugin, task)
                .delay(millis, TimeUnit.MILLISECONDS)
                .schedule();
    }


    public void runRepeatingMillis(Runnable task, long delayMillis, long periodMillis) {
        server.getScheduler()
                .buildTask(plugin, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .repeat(periodMillis, TimeUnit.MILLISECONDS)
                .schedule();
    }
}