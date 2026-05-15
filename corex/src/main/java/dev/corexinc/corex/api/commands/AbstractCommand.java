package dev.corexinc.corex.api.commands;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;

/**
 * Represents a script command that can be executed within a Corex ScriptQueue.
 * Implementations should be thread-safe if {@link #isAsyncSafe()} returns true.
 */
public interface AbstractCommand {

    /**
     * Gets the primary name of the command.
     * This is what users will type in scripts (e.g., "narrate", "teleport").
     *
     * @return the unique command name in lowercase.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getName();

    /**
     * Returns a list of alternative names (aliases) for this command.
     * By default, returns a list containing only the primary name.
     *
     * @return a list of lowercase aliases.
     */
    @NotNull
    @OverrideOnly
    @Unmodifiable
    @AvailableSince("1.0.0")
    default List<String> getAlias() {
        return List.of(getName());
    }

    /**
     * Executes the command logic.
     * <p>
     * Note: This method is called by the ScriptQueue engine.
     * Do not call this method manually.
     *
     * @param queue       The {@link ScriptQueue} instance currently executing this command.
     * @param instruction The pre-compiled {@link Instruction} containing parsed arguments and prefixes.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction);

    /**
     * Gets the syntax pattern for this command.
     * This string is used by the Compiler to distinguish between prefixes, flags, and linear arguments.
     * <p>
     * Format example: "- narrate [<text>] (targets:<player>|...) (per_player)"
     *
     * @return the command syntax string.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getSyntax();

    /**
     * The minimum number of linear (positional) arguments required for this command to run.
     *
     * @return min required linear arguments.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    int getMinArgs();

    /**
     * The maximum number of linear (positional) arguments allowed for this command.
     *
     * @return max allowed linear arguments.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    int getMaxArgs();

    /**
     * Determines if this command can be waited for using the '~' prefix (Holdable).
     * If true, the queue will pause until the command explicitly signals it has finished.
     *
     * @return true if the command supports synchronous waiting.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    default boolean setCanBeWaitable() {
        return false;
    }

    /**
     * Determines if this command is thread-safe and can be executed in an asynchronous queue.
     * <p>
     * <b>Warning:</b> If this returns true, the command implementation must not call
     * non-thread-safe Bukkit API methods without proper synchronization.
     *
     * @return true if the command can run off-thread.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    default boolean isAsyncSafe() {
        return false;
    }

    default void report(ScriptQueue queue, Instruction instruction, Map<String, Object> report) {}
}