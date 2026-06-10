package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Finally
 * @Syntax finally: [<commands>]
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription Runs a block of commands after a try, whether or not it errored.
 *
 * @Description
 * Always runs its sub-block, regardless of whether the preceding try succeeded or its catch handled an error.
 * Clears the trapped error state, so it should be the last part of a try/catch/finally sequence.
 *
 * @Usage
 * // Always clean up after a risky block.
 * - try:
 *     - edit something risky
 * - finally:
 *     - narrate "Cleanup done."
 */
public class FinallyCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "finally";
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
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        queue.setTempData("corex_try_caught", null);

        if (instruction.innerBlock != null) {
            queue.pushFrame(getName(), instruction.innerBlock, null);
        }
    }
}
