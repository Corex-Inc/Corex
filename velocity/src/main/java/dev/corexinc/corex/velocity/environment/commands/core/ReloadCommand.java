package dev.corexinc.corex.velocity.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.velocity.CorexVelocity;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Reload
 * @Syntax reload
 * @RequiredArgs 0
 * @MaxArgs 0
 * @Modules VELOCITY
 * @ShortDescription Reloads and recompiles all Corex scripts on the proxy.
 *
 * @Implements Reload
 *
 * @Description
 * Rereads config.yml and recompiles every script under the proxy's scripts folder. Meant to be
 * run by hand while writing scripts, as in '/vrun reload'.
 *
 * Running queues are left alone and finish on the bytecode they started with, so a reload does
 * not interrupt anything already in flight.
 *
 * This one is not async safe: it swaps the whole script registry, and doing that off the proxy
 * scheduler while another queue is compiling is how you get half loaded containers.
 *
 * @Usage
 * // Reload after editing a script.
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
            CorexVelocity.getInstance().applyNetworkConfig();

            ScriptManager.reloadScripts();
        } catch (Exception e) {
            Debugger.echoError(queue, "ERROR while reloading scripts. See console logs above this message.");
        }
    }
}