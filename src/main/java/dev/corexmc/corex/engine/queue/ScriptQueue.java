package dev.corexmc.corex.engine.queue;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.environment.tags.player.PlayerTag;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptQueue {

    private final String id;
    private final boolean isAsync;
    private final PlayerTag linkedPlayer;
    private final ConcurrentHashMap<String, AbstractTag> definitions = new ConcurrentHashMap<>();

    private Instruction[] bytecode;
    private int pointer = 0;
    private Runnable onFinish;

    private record QueueFrame(Instruction[] bytecode, int pointer, Runnable onFinish) {}
    private final Stack<QueueFrame> callStack = new Stack<>();

    private volatile boolean isPaused = false;
    private boolean isStopped = false;
    private boolean isBroken = false;

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer) {
        this.id = id; this.bytecode = bytecode; this.isAsync = isAsync; this.linkedPlayer = linkedPlayer;
    }

    public void start() { executeNext(); }

    public void executeNext() {
        while (!isPaused && !isStopped) {

            if (pointer < bytecode.length) {
                Instruction inst = bytecode[pointer++];
                try {
                    inst.command.run(this, inst);
                } catch (Exception e) {
                    CorexLogger.error("ERROR in " + id + ": " + e.getMessage());
                }
            } else {
                Runnable callback = this.onFinish;

                if (callStack.isEmpty()) {
                    isStopped = true;
                    if (callback != null) callback.run();
                    break;
                } else {
                    QueueFrame frame = callStack.pop();
                    this.bytecode = frame.bytecode;
                    this.pointer = frame.pointer;
                    this.onFinish = frame.onFinish;
                }

                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    public void pushFrame(Instruction[] newBytecode, Runnable newOnFinish) {
        callStack.push(new QueueFrame(this.bytecode, this.pointer, this.onFinish));
        this.bytecode = newBytecode;
        this.pointer = 0;
        this.onFinish = newOnFinish;
    }

    public void skipFrame(boolean breakLoop) {
        this.pointer = this.bytecode.length;
        this.isBroken = breakLoop;
    }

    public boolean isBroken() { return isBroken; }
    public void setBroken(boolean broken) { this.isBroken = broken; }

    public void pause() { this.isPaused = true; }
    public void resume() { this.isPaused = false; executeNext(); }

    public void delay(long ticks) {
        pause();
        if (isAsync) SchedulerAdapter.runAsyncLater(this::resume, Math.max(1, ticks));
        else SchedulerAdapter.runLater(this::resume, Math.max(1, ticks));
    }

    public void define(String name, AbstractTag value) {
        if (value == null) definitions.remove(name.toLowerCase());
        else definitions.put(name.toLowerCase(), value);
    }

    public AbstractTag getDefinition(String name) {
        return definitions.get(name.toLowerCase());
    }

    public PlayerTag getPlayer() {
        AbstractTag def = getDefinition("__player");
        if (def instanceof PlayerTag) return (PlayerTag) def;
        return linkedPlayer;
    }

    public String getId() { return id; }
}