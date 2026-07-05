package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Case
 * @Syntax case [<value>|...]
 * @RequiredArgs 1
 * @MaxArgs -1
 * @ShortDescription One branch inside a 'switch' block.
 *
 * @Implements Case
 *
 * @Description
 * Marks a branch inside 'switch'. The branch runs when the switched value equals
 * any of the pipe-separated values (case-insensitive). The switch command reads
 * these branches itself; writing 'case' outside a switch just logs an error.
 *
 * @Usage
 * // Accept several spellings for one branch.
 * - switch <[answer]>:
 *   - case yes|y|yeah:
 *     - narrate "Deal."
 *   - case no|n:
 *     - narrate "Maybe next time."
 *   - default:
 *     - narrate "That's not a yes or a no."
 */
public class SwitchCaseCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "case";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<text>|...]";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return -1;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        Debugger.echoError(queue, "Command '" + getName() + "' can ONLY be used inside 'switch'!");
    }
}