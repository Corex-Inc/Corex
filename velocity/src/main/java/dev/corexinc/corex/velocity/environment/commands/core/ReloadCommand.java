package dev.corexinc.corex.velocity.environment.commands.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.velocity.CorexVelocity;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Reload
 * @Syntax reload
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription Reloads and recompile all Corex scripts. Primarily for use as an in-game command.
 *
 * @Implements Reload
 *
 * @Description
 * Reloads and recompile all Corex scripts.
 * Primarily for use as an in-game command, like "/run reload".
 *
 * Reloads and recompile scripts in a way that may delay a few ticks to avoid interrupting the server on large reloads.
 *
 * @Usage
 * // Use to reload scripts automatically
 * - reload
 */
public class ReloadCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "reload";
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
        try {
            Debugger.report(queue, instruction);
            CorexVelocity.getInstance().getConfig().reload();
            Debugger.updateDebugMode(CorexVelocity.getInstance().getConfig().getString("logger.debug-mode", "default"));
            EventRegistry.resetAll();

            ScriptManager.reloadScripts();
        } catch (Exception e) {
            Debugger.echoError(queue, "ERROR while reloading scripts. See console logs above this message.");
        }
    }
}