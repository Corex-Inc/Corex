package dev.corexmc.corex.engine.queue;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.environment.tags.player.PlayerTag;

import java.util.concurrent.ConcurrentHashMap;

public class ScriptQueue {

    private final String id;

    private final Instruction[] bytecode;
    private int pointer = 0;

    private volatile boolean isPaused = false;

    private final boolean isAsync;

    private PlayerTag linkedPlayer;

    private final ConcurrentHashMap<String, AbstractTag> definitions = new ConcurrentHashMap<>();

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void start() {
        executeNext();
    }

    public void executeNext() {
        while (!isPaused && pointer < bytecode.length) {
            Instruction inst = bytecode[pointer];
            pointer++;

            try {
                inst.command.run(this, inst);
            } catch (Exception e) {
                CorexLogger.error("ERROR while executing script command in " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Pauses the queue and resumes it after {@code ticks} server ticks.
     * Works on both Paper (Bukkit scheduler) and Folia (region/async scheduler)
     * via {@link SchedulerAdapter}.
     *
     * <p>Async queues use {@link SchedulerAdapter#runAsyncLater} so they stay
     * off the main thread. Sync queues use {@link SchedulerAdapter#runLater}
     * and resume on the global region (main-thread equivalent on Folia).</p>
     */
    public void delay(long ticks) {
        this.isPaused = true;

        if (isAsync) {
            SchedulerAdapter.runAsyncLater(this::resume, Math.max(1, ticks));
        } else {
            SchedulerAdapter.runLater(this::resume, Math.max(1, ticks));
        }
    }

    public void resume() {
        this.isPaused = false;
        executeNext();
    }

    public void define(String name, AbstractTag value) {
        if (value == null) {
            definitions.remove(name.toLowerCase());
        } else {
            definitions.put(name.toLowerCase(), value);
        }
    }

    public AbstractTag getDefinition(String name) {
        return definitions.get(name.toLowerCase());
    }

    public PlayerTag getPlayer() {
        AbstractTag definedPlayer = getDefinition("__player");
        if (definedPlayer instanceof PlayerTag) {
            return (PlayerTag) definedPlayer;
        }
        return linkedPlayer;
    }

    public String getId() {
        return id;
    }
}