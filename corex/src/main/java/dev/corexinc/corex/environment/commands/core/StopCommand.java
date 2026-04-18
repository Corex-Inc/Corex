package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Stop
 * @Syntax stop
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription Stops the current script queue immediately.
 *
 * @Implements Stop
 *
 * @Description
 * Halts the execution of the current script queue instantly.
 * Any upcoming commands in the current script (or its call stack) will be skipped and the queue will be destroyed.
 *
 * @Usage
 * // Use to stop the queue immediately
 * - stop
 *
 * @Usage
 * // Can be used in if statements to abort execution
 * - if <[player].name> == "Notch":
 *   - narrate "Hello creator!"
 *   - stop
 * - narrate "You are not Notch!"
 */
public class StopCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "stop";
    }

    @Override
    public @NonNull String getSyntax() {
        return "";
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
        Debugger.report(queue, instruction);
        queue.stopEntireQueue();
        // WHOAH! Very difficult command!!! 💀
    }
}