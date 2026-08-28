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
     * Executes the command logic, reading arguments off the raw instruction.
     * <p>
     * Note: This method is called by the ScriptQueue engine.
     * Do not call this method manually.
     * <p>
     * <b>You may instead declare {@code run} with one parameter per argument</b>, and let the
     * engine resolve, convert and report them for you. The parameter list is matched against
     * {@link #getSyntax()} once when the plugin loads, so a mismatch is reported at startup
     * rather than the first time a script uses the command:
     * <pre>{@code
     * // syntax: "[<entity>] [<location>] (cause:<TeleportCause>=PLUGIN) (silent)"
     * public void run(ScriptQueue queue, EntityTag entity, LocationTag location,
     *                 ElementTag cause, boolean silent) { ... }
     * }</pre>
     * A leading {@link ScriptQueue} is optional; everything after it lines up with the syntax
     * one for one. Parameters may be any tag type, {@link String}, a number, or {@code boolean}
     * for a flag.
     * <p>
     * <b>Nullability.</b> An argument written {@code [required]} is never {@code null}: if the
     * script omitted it, the engine reports the error and your method is never called. An
     * argument written {@code (optional)} <i>is</i> {@code null} when the script omitted it,
     * unless the syntax supplies a default with {@code =}. Optional numbers and booleans get
     * {@code 0} and {@code false} rather than null, since primitives cannot hold one.
     * <p>
     * Note that non-null does not mean valid: Corex tag constructors are forgiving, so a
     * required {@code LocationTag} given nonsense text arrives as a LocationTag that simply
     * describes nowhere. Check the contents when it matters.
     * <p>
     * <b>Order.</b> Positional arguments are matched by type, not by position, so a script may
     * write them in either order when the types differ. Arguments the engine cannot tell apart
     * keep the order the script wrote them in.
     * <p>
     * Arguments are reported to the debug log automatically before the body runs - see
     * {@link #getReportLabels()} to rename them and {@link NoDebug} to hide one. To add a value
     * the engine could not know in advance, call {@code Debugger.detail(queue, key, value)}
     * from the body, the same way you would call {@code Debugger.echoError}.
     * <p>
     * Implement exactly one of the two forms. Overriding neither fails loudly on first use.
     *
     * @param queue       The {@link ScriptQueue} instance currently executing this command.
     * @param instruction The pre-compiled {@link Instruction} containing parsed arguments and prefixes.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    default void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
        throw new CommandExecutionException("Command '" + getName() + "' ("
                + getClass().getSimpleName() + ") does not implement run(...) in either form");
    }

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
    /**
     * Renames arguments in this command's debug report.
     *
     * <p>By default a bound command labels each argument with its syntax name, so
     * {@code [<text>]} prints as {@code Text='hello'}. Override this when a nicer word
     * reads better in the log, without having to distort either the syntax or the Java
     * parameter to get it:</p>
     *
     * <pre>{@code
     * // syntax: "[<text>] (targets:<list>)"
     * // run:    void run(ElementTag text, ListTag targets)
     * @Override
     * public Map<String, String> getReportLabels() {
     *     return Map.of("text", "Narrating");
     * }
     * // prints: Executing 'NARRATE': Narrating='hello' Targets='li@p@bob'
     * }</pre>
     *
     * <p>Keys are syntax argument names; anything left out keeps its default label. A key
     * that matches no argument is reported when the plugin loads, so a typo here surfaces
     * at startup rather than silently doing nothing.</p>
     *
     * @return argument name to display label; empty by default
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    default Map<String, String> getReportLabels() {
        return Map.of();
    }

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