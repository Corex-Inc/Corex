package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Async
 * @Syntax async: [<commands>]
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription Runs a block of commands in a single asynchronous queue.
 *
 * @Description
 * Runs the entire sub-block in one new asynchronous queue, off the main server thread.
 * Because all nested commands share that one queue, waitable (~) commands inside it behave normally.
 * The block inherits a snapshot of the current definitions, linked player and context, but changes do not flow back.
 *
 * Only async-safe commands may run inside (the engine rejects sync commands in an async queue).
 * Use the '@' prefix on a single command to spin just that command into its own async queue instead.
 * If used inside an already-async queue, the block simply runs inline.
 *
 * @Usage
 * // Do blocking work off-thread, then come back to the main thread to show the result.
 * - async:
 *     - ~fetch https://example.com/data save:result
 *     - narrate "<[result].get[result]>"
 */
public class AsyncCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "async";
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
        if (instruction.innerBlock == null) return;

        if (queue.isAsync()) {
            queue.pushFrame(getName(), instruction.innerBlock, null);
        } else {
            queue.runAsyncChild(instruction.innerBlock, instruction.isWaitable);
        }
    }
}
