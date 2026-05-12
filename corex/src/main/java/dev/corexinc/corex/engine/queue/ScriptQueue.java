package dev.corexinc.corex.engine.queue;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.engine.utils.exceptions.RegionRelocateException;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.jetbrains.annotations.ApiStatus.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Represents an isolated execution thread within the Corex Virtual Machine (CVM).
 * <p>
 * The ScriptQueue manages the sequential execution of {@link Instruction}s,
 * handles nested logic blocks via a Call Stack, and provides lexical scoping
 * for variables (definitions).
 * <p>
 * This class is designed to be thread-aware, supporting both standard Paper
 * and multithreaded Folia/Canvas environments through region-based scheduling.
 * <p>
 * <b>Platform note:</b> this class is Bukkit-free. Position data is represented
 * as {@link Position}. On Velocity (proxy), region fields are unused and
 * {@link RegionRelocateException} is never thrown.
 *
 * @since 1.0.0
 */
public class ScriptQueue {

    /**
     * The unique identifier of this queue.
     */
    private final String id;

    /**
     * Whether this queue is running asynchronously.
     */
    private final boolean isAsync;

    /**
     * The player context associated with this queue.
     */
    private final PlayerTag linkedPlayer;

    /**
     * Local variables (definitions) stored in this queue.
     */
    private final Map<String, AbstractTag> definitions;

    /**
     * List of values returned by the queue via 'return' or 'determine' commands.
     */
    private final List<AbstractTag> returnValues = new ArrayList<>();

    /**
     * The current block of instructions being executed.
     */
    private Instruction[] bytecode;

    /**
     * The instruction pointer (Program Counter) for the current block.
     */
    private int pointer = 0;

    /**
     * A callback executed when the current stack frame or the entire queue finishes.
     */
    private Runnable onFinish;

    /**
     * A condition used by loops (e.g., 'while' or 'repeat') to determine if the block should restart.
     */
    private BooleanSupplier loopCondition;

    /**
     * Represents a saved state of execution (instruction set + pointer)
     * when entering a nested block.
     */
    private record QueueFrame(Instruction[] bytecode, int pointer, Runnable onFinish, BooleanSupplier loopCondition) {}

    /**
     * The internal Call Stack used to handle nested blocks (if, repeat, try-catch).
     */
    private final ArrayDeque<QueueFrame> callStack = new ArrayDeque<>();

    /**
     * Temporary data storage for internal command communication.
     */
    private final Map<String, Object> tempData = new HashMap<>();

    /**
     * The contextual data object provided to this queue (e.g., event data).
     */
    private AbstractTag context;

    private volatile boolean isPaused = false;
    private boolean isStopped = false;
    private boolean isBroken = false;
    private boolean isCancelled = false;
    private boolean silent = false;

    /**
     * If true, the queue will not be destroyed after finishing its bytecode,
     * waiting for more instructions to be injected.
     */
    private boolean keepAlive = false;
    private boolean isWaitingForInstructions = false;

    /**
     * Nanosecond timestamp of when the queue started.
     */
    private long startNanos;

    /**
     * Tracks errors encountered during the execution of the current instruction.
     */
    private final List<String> currentErrors = new ArrayList<>();
    private boolean errorHeaderPrinted = false;

    public boolean isErrorHeaderPrinted() {
        return errorHeaderPrinted;
    }

    public void setErrorHeaderPrinted(boolean value) {
        this.errorHeaderPrinted = value;
    }

    /**
     * Global registry of all active queues currently managed by the CVM.
     */
    private static final Map<String, ScriptQueue> activeQueues = new ConcurrentHashMap<>();

    /**
     * The position this queue is "anchored" to (used for Folia region matching).
     * <p>
     * {@code null} on Velocity and in queue contexts that have no world position.
     */
    @Nullable
    private Position anchorPosition = null;

    /**
     * The target position of a region shift requested by a command.
     * <p>
     * {@code null} when no region shift is pending.
     */
    @Nullable
    private Position targetRegionPosition = null;

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer, Position anchorPosition) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
        this.definitions = isAsync ? new ConcurrentHashMap<>() : new HashMap<>();
        this.anchorPosition = anchorPosition;
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

    /**
     * Starts the queue execution.
     * <p>
     * Initializes the start timer, logs the start event, and begins the instruction loop.
     */
    @AvailableSince("1.0.0")
    public void start() {
        startNanos = System.nanoTime();
        Debugger.queueStart(this);
        activeQueues.put(id, this);
        executeNext();
    }

    /**
     * The main execution loop of the CVM.
     * <p>
     * Iterates through the {@code bytecode} array, processes global flags,
     * executes commands, and manages stack frame transitions.
     * Supports Folia region relocation via {@link RegionRelocateException}.
     */
    @AvailableSince("1.0.0")
    public void executeNext() {
        try {
            while (!isPaused && !isStopped) {

                if (targetRegionPosition != null && SchedulerAdapter.get().needsRegionRelocation(targetRegionPosition)) {
                    isPaused = true;
                    Position target = targetRegionPosition;
                    SchedulerAdapter.get().runAt(target, () -> {
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

                        for (Map.Entry<AbstractGlobalFlag, CompiledArgument> entry : inst.globalFlags.entrySet()) {
                            if (!entry.getKey().execute(this, inst, entry.getValue())) {
                                skipCommand = true;
                                break;
                            }
                        }

                        if (!skipCommand) {
                            this.setErrorHeaderPrinted(false);
                            try {
                                inst.command.run(this, inst);
                            } catch (RegionRelocateException rre) {
                                throw rre;
                            } catch (Exception e) {
                                this.addError("Internal Java Exception: " + e.getMessage());
                                e.printStackTrace();
                            }
                            Debugger.flushErrors(this, inst);
                        }
                    } catch (RegionRelocateException rre) {
                        throw rre;
                    } catch (Exception e) {
                        String cmdName = inst != null ? inst.command.getName() : "unknown";
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
            SchedulerAdapter.get().runAt(rre.getPosition(), () -> {
                isPaused = false;
                executeNext();
            });
        } catch (Throwable t) {
            Debugger.error(this, "Fatal queue execution crash: " + t.getMessage(), t, callStack.size());
            stopEntireQueue();
        }
    }

    /**
     * Enters a nested instruction block (e.g., a loop body or an 'if' branch).
     *
     * @param calledScriptName Name of the script/block for debugging purposes.
     * @param newBytecode      The block of instructions to execute.
     * @param newOnFinish      Callback to run when this block finishes.
     */
    @AvailableSince("1.0.0")
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

    /**
     * Forces the queue to jump to the end of the current instruction block.
     * Useful for 'stop' or 'next' commands within loops.
     *
     * @param breakLoop If true, indicates that a loop should not restart.
     */
    @AvailableSince("1.0.0")
    public void skipFrame(boolean breakLoop) {
        this.pointer = this.bytecode != null ? this.bytecode.length : 0;
        this.isBroken = breakLoop;
    }


    /**
     * Pauses the queue, preventing further instructions from being executed
     * until {@link #resume()} is called.
     */
    @AvailableSince("1.0.0")
    public void pause() {
        this.isPaused = true;
    }

    /**
     * Resumes execution of a paused queue.
     */
    @AvailableSince("1.0.0")
    public void resume() {
        this.isPaused = false;
        executeNext();
    }

    /**
     * Fully terminates the queue, clearing the call stack and releasing resources.
     */
    @AvailableSince("1.0.0")
    public void stopEntireQueue() {
        this.isStopped = true;
        this.callStack.clear();
        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        activeQueues.remove(id);
        Debugger.queueStop(this, elapsedMs);
        Debugger.releaseQueue(this);
    }

    /**
     * Pauses the queue and schedules a resume after the specified duration.
     *
     * @param ticks Time to wait in server ticks (1 tick = 50ms).
     */
    @AvailableSince("1.0.0")
    public void delay(long ticks) {
        pause();
        if (isAsync) SchedulerAdapter.get().runAsyncLater(this::resume, Math.max(1, ticks));
        else SchedulerAdapter.get().runLater(this::resume, Math.max(1, ticks));
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


    /**
     * Defines a local variable within this queue's scope.
     *
     * @param name  The name of the definition.
     * @param value The value to store. If {@code null}, the definition is removed.
     */
    @AvailableSince("1.0.0")
    public void define(String name, AbstractTag value) {
        if (value == null) definitions.remove(name);
        else definitions.put(name, value);
    }

    /**
     * Retrieves a stored local variable.
     *
     * @param name The name of the definition.
     * @return The tag value, or {@code null} if not found.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public AbstractTag getDefinition(String name) {
        return definitions.get(name);
    }

    /**
     * Gets the primary player linked to this queue.
     * Checks for the special {@code __player} definition before falling back to the queue's default.
     *
     * @return the linked {@link PlayerTag}.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public PlayerTag getPlayer() {
        AbstractTag def = getDefinition("__player");
        if (def instanceof PlayerTag) return (PlayerTag) def;
        return linkedPlayer;
    }

    /**
     * Gets the current evaluation position "anchor" of this queue.
     * Used by the {@code <region>} tag to determine region-local performance metrics.
     * <p>
     * On Bukkit/Folia the environment should override the anchor by calling
     * {@link #setAnchorPosition(Position)} with the player's current position
     * at queue creation time. On Velocity this always returns {@code null}.
     * <p>
     * <b>Migration note:</b> the previous implementation called
     * {@code linkedPlayer.getPlayer().getLocation()} directly here, which required
     * a Bukkit import. Player position retrieval is now a platform concern —
     * populate {@code anchorPosition} from the platform layer instead.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public Position getAnchorPosition() {
        return anchorPosition;
    }

    /** Sets (or clears) the anchor position for this queue. */
    public void setAnchorPosition(@Nullable Position position) {
        this.anchorPosition = position;
    }

    /**
     * Requests a region shift to the given position.
     * The shift is applied at the start of the next {@link #executeNext()} tick.
     */
    public void setTargetRegion(@Nullable Position position) {
        this.targetRegionPosition = position;
    }

    @Nullable
    public Position getTargetRegion() {
        return targetRegionPosition;
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

    /**
     * Associates a contextual object with this queue.
     */
    @AvailableSince("1.0.0")
    public void setContext(@Nullable AbstractTag context) {
        this.context = context;
    }

    @Nullable
    @AvailableSince("1.0.0")
    public AbstractTag getContext() {
        return context;
    }

    @AvailableSince("1.0.0")
    public void addReturn(AbstractTag tag) {
        if (tag != null) returnValues.add(tag);
    }

    @NotNull
    @AvailableSince("1.0.0")
    public List<AbstractTag> getReturns() {
        return returnValues;
    }

    @AvailableSince("1.0.0")
    public void setTempData(String key, Object value) {
        if (value == null) tempData.remove(key);
        else tempData.put(key, value);
    }

    @Nullable
    @AvailableSince("1.0.0")
    public Object getTempData(String key) {
        return tempData.get(key);
    }

    public String getId() { return id; }
    public boolean isAsync() { return isAsync; }
    public boolean isStopped() { return isStopped; }
    public boolean isCancelled() { return isCancelled; }
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }
    public boolean isBroken() { return isBroken; }
    public void setBroken(boolean broken) { isBroken = broken; }
    public boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }
    public void setOnFinish(Runnable onFinish) { this.onFinish = onFinish; }
    public void setKeepAlive(boolean keepAlive) { this.keepAlive = keepAlive; }
    public boolean isKeepAlive() { return keepAlive; }
    public int getDepth() { return callStack.size(); }
    public Map<String, AbstractTag> getDefinitionsMap() { return definitions; }
    public static ScriptQueue getQueueById(String id) { return activeQueues.get(id); }
    public static Collection<ScriptQueue> getAllQueues() { return activeQueues.values(); }
}