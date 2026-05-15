package dev.corexinc.corex.api.flags;

import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jspecify.annotations.NonNull;

/**
 * Represents a global flag within the Corex scripting engine.
 * <p>
 * Global flags are per-instruction prefixes (e.g., {@code if:}, {@code save:}, {@code player:})
 * that are evaluated <b>before</b> a command executes. They can be used to alter the execution context,
 * intercept data, or conditionally prevent the command from running.
 * <p>
 * If the execution of any global flag attached to an instruction returns {@code false},
 * the entire command is skipped.
 */
public interface AbstractGlobalFlag {

    /**
     * Gets the identifier name of this global flag.
     * <p>
     * This is the exact string matched before the colon in a script line.
     * For example, returning {@code "if"} will match {@code if:<player.isOnline>}
     * in the script.
     *
     * @return a non-null string representing the name of the global flag.
     */
    @NonNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getName();

    /**
     * Executes the logic for this global flag.
     * <p>
     * This method is invoked immediately before the associated command runs.
     * If this method returns {@code false}, the instruction will be entirely skipped,
     * and the {@link ScriptQueue} will proceed to the next instruction.
     * <p>
     * <b>Note:</b> The {@code value} parameter is provided as a {@link CompiledArgument}.
     * To get the actual runtime value, you must evaluate it within the current context
     * using {@link CompiledArgument#evaluate(ScriptQueue)}.
     *
     * @param queue       the current script queue execution context.
     * @param instruction the instruction that this global flag is attached to.
     * @param value       the compiled argument passed to the flag (the data after the colon).
     * @return {@code true} to allow the instruction to continue execution, or {@code false} to skip it.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    boolean execute(
            @NonNull ScriptQueue queue,
            @NonNull Instruction instruction,
            @NonNull CompiledArgument value
    );
}