package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Try
 * @Syntax try: [<commands>]
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription Runs a block of commands, trapping any errors so they can be handled.
 *
 * @Description
 * Runs the sub-block of commands while trapping errors instead of printing them.
 * If a command in the block errors, the rest of the block is skipped and execution continues at the following catch command.
 * Works together with the catch command (handles the error) and the finally command (always runs).
 *
 * Only soft errors (the kind printed by failing commands) are trapped, a fatal queue stop cannot be caught.
 *
 * @Usage
 * // Handle a command that might fail without spamming the console.
 * - try:
 *     - flag <player> score:+:not_a_number
 * - catch as:err:
 *     - narrate "Something went wrong: <[err]>"
 * - finally:
 *     - narrate "Done."
 */
public class TryCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "try";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<commands>]";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        queue.setTempData("corex_try_caught", null);

        if (instruction.innerBlock == null) return;

        queue.setErrorTrapped(true);
        queue.pushFrame(getName(), instruction.innerBlock, () -> {
            queue.setErrorTrapped(false);
            queue.setTempData("corex_try_caught", queue.getAndClearTrappedErrors());
        });
    }
}
