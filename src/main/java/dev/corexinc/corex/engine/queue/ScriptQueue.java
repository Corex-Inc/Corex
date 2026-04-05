package dev.corexinc.corex.engine.queue;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.engine.utils.debugging.DebugLevel;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

public class ScriptQueue {

    private final String id;
    private final boolean isAsync;
    private final PlayerTag linkedPlayer;
    private final Map<String, AbstractTag> definitions;
    private final List<AbstractTag> returnValues = new ArrayList<>();

    private Instruction[] bytecode;
    private int pointer = 0;
    private Runnable onFinish;
    private BooleanSupplier loopCondition;

    private record QueueFrame(Instruction[] bytecode, int pointer, Runnable onFinish, BooleanSupplier loopCondition) {}
    private final ArrayDeque<QueueFrame> callStack = new ArrayDeque<>();

    private final Map<String, Object> tempData = new HashMap<>();
    private ContextTag context;

    private volatile boolean isPaused = false;
    private boolean isStopped = false;
    private boolean isBroken = false;
    private boolean isCancelled = false;

    private long startNanos;

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
        this.definitions = isAsync ? new ConcurrentHashMap<>() : new HashMap<>();
    }

    public void start() {
        startNanos = System.nanoTime();
        Debugger.queueStart(this);
        executeNext();
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public void executeNext() {
        try {
            while (!isPaused && !isStopped) {

                if (bytecode == null) this.bytecode = new Instruction[0];

                if (pointer < bytecode.length) {
                    Instruction inst = bytecode[pointer++];
                    int depth = callStack.size();

                    try {
                        if (isAsync && !inst.command.isAsyncSafe()) {
                            Debugger.critical(this, "Attempt to execute a sync command '" + inst.command.getName() + "' in an async queue!");
                            stopEntireQueue();
                            return;
                        }

                        boolean skipCommand = false;
                        String skipReason = null;

                        if (inst.globalFlags != null) {
                            for (Map.Entry<AbstractGlobalFlag, CompiledArgument> entry : inst.globalFlags.entrySet()) {
                                if (!entry.getKey().execute(this, inst, entry.getValue())) {
                                    skipCommand = true;
                                    skipReason = entry.getKey().getClass().getSimpleName();
                                    break;
                                }
                            }
                        }

                        if (skipCommand) {
                            Debugger.instructionSkipped(this, inst, skipReason, depth);
                        } else {
                            Debugger.instructionStart(this, inst, depth);

                            long startT = 0;
                            if (Debugger.getGlobalLevel() == DebugLevel.TRACE) startT = System.nanoTime();

                            if (inst.command != null) inst.command.run(this, inst);

                            if (Debugger.getGlobalLevel() == DebugLevel.TRACE) {
                                double elapsedMs = (System.nanoTime() - startT) / 1_000_000.0;
                                Debugger.instructionEnd(this, inst, depth, elapsedMs);
                            }
                        }
                    } catch (Exception e) {
                        String cmdName = (inst != null && inst.command != null) ? inst.command.getName() : "unknown";
                        Debugger.error(this, "Error executing '" + cmdName + "': " + e.getMessage(), e, depth);
                    }
                }
                else if (!isPaused) {
                    if (loopCondition != null && loopCondition.getAsBoolean()) {
                        this.pointer = 0;
                        continue;
                    }

                    Runnable callback = this.onFinish;

                    if (callStack.isEmpty()) {
                        isStopped = true;
                        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
                        Debugger.queueStop(this, elapsedMs);
                        Debugger.releaseQueue(id);
                        if (callback != null) callback.run();
                        break;
                    } else {
                        int depth = callStack.size();
                        QueueFrame frame = callStack.pop();
                        this.bytecode = frame.bytecode();
                        this.pointer = frame.pointer();
                        this.onFinish = frame.onFinish();
                        this.loopCondition = frame.loopCondition();
                        Debugger.frameReturn(this, depth);
                    }

                    if (callback != null) callback.run();
                }
            }
        } catch (Throwable t) {
            Debugger.error(this, "Fatal queue execution crash: " + t.getMessage(), t, callStack.size());
            stopEntireQueue();
        }
    }

    public void pushFrame(String calledScriptName, Instruction[] newBytecode, Runnable newOnFinish) {
        pushFrame(calledScriptName, newBytecode, newOnFinish, null);
    }

    public void pushFrame(String calledScriptName, Instruction[] newBytecode, Runnable newOnFinish, BooleanSupplier loopCondition) {
        int depth = callStack.size();
        callStack.push(new QueueFrame(this.bytecode, this.pointer, this.onFinish, this.loopCondition));
        this.bytecode = newBytecode;
        this.pointer = 0;
        this.onFinish = newOnFinish;
        this.loopCondition = loopCondition;
        Debugger.frameCall(this, calledScriptName, depth);
    }

    public void skipFrame(boolean breakLoop) {
        this.pointer = this.bytecode != null ? this.bytecode.length : 0;
        this.isBroken = breakLoop;
    }

    public boolean isBroken() { return isBroken; }
    public void setBroken(boolean broken) { this.isBroken = broken; }
    public void pause() { this.isPaused = true; }

    public void resume() {
        Debugger.queueResumed(this);
        this.isPaused = false;
        executeNext();
    }

    public void stopEntireQueue() {
        this.isStopped = true;
        this.callStack.clear();
        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        Debugger.queueStop(this, elapsedMs);
        Debugger.releaseQueue(id);
    }

    public void setOnFinish(Runnable onFinish) { this.onFinish = onFinish; }

    public void delay(long ticks) {
        Debugger.queuePaused(this, ticks);
        pause();
        if (isAsync) SchedulerAdapter.runAsyncLater(this::resume, Math.max(1, ticks));
        else SchedulerAdapter.runLater(this::resume, Math.max(1, ticks));
    }

    public void define(String name, AbstractTag value) {
        if (value == null) definitions.remove(name);
        else definitions.put(name, value);
    }

    public AbstractTag getDefinition(String name) {
        return definitions.get(name);
    }

    public PlayerTag getPlayer() {
        AbstractTag def = getDefinition("__player");
        if (def instanceof PlayerTag) return (PlayerTag) def;
        return linkedPlayer;
    }

    public String getId() { return id; }

    public void setTempData(String key, Object value) {
        if (value == null) tempData.remove(key);
        else tempData.put(key, value);
    }

    public Object getTempData(String key) { return tempData.get(key); }

    public void setContext(ContextTag context) { this.context = context; }
    public ContextTag getContext() { return context; }
    public void addReturn(AbstractTag tag) { if (tag != null) returnValues.add(tag); }
    public java.util.List<AbstractTag> getReturns() { return returnValues; }
    public boolean isAsync() { return isAsync; }
    public int getDepth() { return callStack.size(); }
}