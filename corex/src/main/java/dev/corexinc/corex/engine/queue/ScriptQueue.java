package dev.corexinc.corex.engine.queue;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.utils.exceptions.RegionRelocateException;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.Location;

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
    private boolean silent = false;

    private boolean keepAlive = false;
    private boolean isWaitingForInstructions = false;

    private long startNanos;

    private final List<String> currentErrors = new ArrayList<>();
    private boolean errorHeaderPrinted = false;
    public boolean isErrorHeaderPrinted() { return errorHeaderPrinted; }
    public void setErrorHeaderPrinted(boolean value) { this.errorHeaderPrinted = value; }

    private static final Map<String, ScriptQueue> activeQueues = new ConcurrentHashMap<>();
    private Location anchorLocation = null;

    private Location targetRegionLocation = null;

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer, Location anchorLocation) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
        this.definitions = isAsync ? new ConcurrentHashMap<>() : new HashMap<>();
        this.anchorLocation = anchorLocation;
    }

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
        this.definitions = isAsync ? new ConcurrentHashMap<>() : new HashMap<>();
    }

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer, boolean silent) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
        this.definitions = isAsync ? new ConcurrentHashMap<>() : new HashMap<>();
        this.silent = silent;
    }

    public void start() {
        startNanos = System.nanoTime();
        Debugger.queueStart(this);
        activeQueues.put(id, this);
        executeNext();
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void injectInstructions(Instruction... insts) {
        if (this.bytecode == null || this.pointer >= this.bytecode.length) {
            this.bytecode = insts;
        } else {
            int remaining = this.bytecode.length - this.pointer;
            Instruction[] combined = new Instruction[remaining + insts.length];
            System.arraycopy(this.bytecode, this.pointer, combined, 0, remaining);
            System.arraycopy(insts, 0, combined, remaining, insts.length);
            this.bytecode = combined;
        }
        this.pointer = 0;

        if (isWaitingForInstructions) {
            isWaitingForInstructions = false;
            executeNext();
        }
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

                if (targetRegionLocation != null && !SchedulerAdapter.isRegionOwner(targetRegionLocation)) {
                    isPaused = true;
                    Location target = targetRegionLocation;
                    SchedulerAdapter.runAt(target, () -> {
                        isPaused = false;
                        executeNext();
                    });
                    return;
                }

                if (bytecode == null) this.bytecode = new Instruction[0];

                if (pointer < bytecode.length) {
                    Instruction inst = bytecode[pointer++];
                    int depth = callStack.size();

                    try {
                        if (isAsync && !inst.command.isAsyncSafe()) {
                            Debugger.error(this, "Attempt to execute a sync command '" + inst.command.getName() + "' in an async queue!", depth);
                            stopEntireQueue();
                            return;
                        }

                        boolean skipCommand = false;

                        if (inst.globalFlags != null) {
                            for (Map.Entry<AbstractGlobalFlag, CompiledArgument> entry : inst.globalFlags.entrySet()) {
                                if (!entry.getKey().execute(this, inst, entry.getValue())) {
                                    skipCommand = true;
                                    break;
                                }
                            }
                        }

                        if (!skipCommand) {
                            this.setErrorHeaderPrinted(false);
                            if (inst.command != null) {
                                try {
                                    inst.command.run(this, inst);
                                } catch (RegionRelocateException rre) {
                                    throw rre;
                                } catch (Exception e) {
                                    this.addError("Internal Java Exception: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            Debugger.flushErrors(this, inst);
                        }
                    } catch (RegionRelocateException rre) {
                        throw rre;
                    } catch (Exception e) {
                        String cmdName = (inst != null && inst.command != null) ? inst.command.getName() : "unknown";
                        Debugger.error(this, "Error executing '" + cmdName + "': " + e.getMessage(), e, depth);
                    }
                } else if (!isPaused) {
                    if (loopCondition != null && loopCondition.getAsBoolean()) {
                        this.pointer = 0;
                        continue;
                    }

                    Runnable callback = this.onFinish;

                    if (callStack.isEmpty()) {

                        if (keepAlive) {
                            isWaitingForInstructions = true;
                            if (callback != null) {
                                callback.run();
                                this.onFinish = null;
                            }
                            break;
                        }

                        isStopped = true;
                        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
                        Debugger.queueStop(this, elapsedMs);
                        Debugger.releaseQueue(this);
                        if (callback != null) callback.run();
                        break;
                    } else {
                        QueueFrame frame = callStack.pop();
                        this.bytecode = frame.bytecode();
                        this.pointer = frame.pointer();
                        this.onFinish = frame.onFinish();
                        this.loopCondition = frame.loopCondition();
                    }

                    if (callback != null) callback.run();
                }
            }
        } catch (RegionRelocateException rre) {
            pointer--;
            isPaused = true;
            SchedulerAdapter.runAt(rre.getLocation(), () -> {
                isPaused = false;
                executeNext();
            });
        } catch (Throwable t) {
            Debugger.error(this, "Fatal queue execution crash: " + t.getMessage(), t, callStack.size());
            stopEntireQueue();
        }
    }

    public void addError(String message) {
        currentErrors.add(message);
    }

    public boolean hasErrors() {
        return !currentErrors.isEmpty();
    }

    public List<String> getAndClearErrors() {
        List<String> copy = new ArrayList<>(currentErrors);
        currentErrors.clear();
        return copy;
    }

    public void pushFrame(String calledScriptName, Instruction[] newBytecode, Runnable newOnFinish) {
        pushFrame(calledScriptName, newBytecode, newOnFinish, null);
    }

    public void pushFrame(String calledScriptName, Instruction[] newBytecode, Runnable newOnFinish, BooleanSupplier loopCondition) {
        callStack.push(new QueueFrame(this.bytecode, this.pointer, this.onFinish, this.loopCondition));
        this.bytecode = newBytecode;
        this.pointer = 0;
        this.onFinish = newOnFinish;
        this.loopCondition = loopCondition;
    }

    public void skipFrame(boolean breakLoop) {
        this.pointer = this.bytecode != null ? this.bytecode.length : 0;
        this.isBroken = breakLoop;
    }

    public boolean isBroken() {
        return isBroken;
    }

    public void setBroken(boolean broken) {
        this.isBroken = broken;
    }

    public void pause() {
        this.isPaused = true;
    }

    public void resume() {
        this.isPaused = false;
        executeNext();
    }

    public void stopEntireQueue() {
        this.isStopped = true;
        this.callStack.clear();
        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        activeQueues.remove(id);
        Debugger.queueStop(this, elapsedMs);
        Debugger.releaseQueue(this);
    }

    public void setOnFinish(Runnable onFinish) {
        this.onFinish = onFinish;
    }

    public void delay(long ticks) {
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

    public String getId() {
        return id;
    }

    public void setTempData(String key, Object value) {
        if (value == null) tempData.remove(key);
        else tempData.put(key, value);
    }

    public Object getTempData(String key) {
        return tempData.get(key);
    }

    public void setContext(ContextTag context) {
        this.context = context;
    }

    public ContextTag getContext() {
        return context;
    }

    public void addReturn(AbstractTag tag) {
        if (tag != null) returnValues.add(tag);
    }

    public List<AbstractTag> getReturns() {
        return returnValues;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public int getDepth() {
        return callStack.size();
    }

    public static ScriptQueue getQueueById(String id) {
        return activeQueues.get(id);
    }

    public static Collection<ScriptQueue> getAllQueues() {
        return activeQueues.values();
    }

    public Map<String, AbstractTag> getDefinitionsMap() {
        return definitions;
    }

    public org.bukkit.Location getAnchorLocation() {
        if (linkedPlayer != null && linkedPlayer.getPlayer() != null && linkedPlayer.getPlayer().isOnline()) {
            return linkedPlayer.getPlayer().getLocation();
        }
        return anchorLocation;
    }

    public void setTargetRegion(Location location) {
        this.targetRegionLocation = location;
    }

    public Location getTargetRegion() {
        return targetRegionLocation;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }
}