package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Default
 * @Syntax default
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription The fallback branch of a 'switch' block.
 *
 * @Implements Default
 *
 * @Description
 * The branch a 'switch' falls into when no 'case' matched. At most one per switch,
 * conventionally written last. Like 'case', it only means something inside a
 * switch block; anywhere else it logs an error.
 *
 * @Usage
 * // Catch unrecognized input.
 * - switch <[command]>:
 *   - case start:
 *     - narrate "Starting..."
 *   - default:
 *     - narrate "Unknown subcommand '<[command]>'."
 */
public class SwitchDefaultCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "default";
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
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        Debugger.echoError(queue, "Command '" + getName() + "' can ONLY be used inside 'switch'!");
    }
}