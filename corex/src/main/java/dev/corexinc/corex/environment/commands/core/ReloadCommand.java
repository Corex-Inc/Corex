package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.containers.ItemContainer;
import dev.corexinc.corex.environment.containers.commands.CommandContainer;
import dev.corexinc.corex.environment.containers.commands.CommandManager;
import dev.corexinc.corex.environment.events.EventRegistry;
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
 * Reloads config.yml, resets every script event and recompiles the whole scriptpack, then
 * re-registers scripted commands. Meant for use as an in-game command, like "/run reload".
 *
 * The reload runs on the thread that called it and blocks until every script is compiled, so a
 * large scriptpack will hold the server for the duration. Queues already running keep their old
 * bytecode until they finish.
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
            Corex.getInstance().reloadConfig();
            Debugger.updateDebugMode(Corex.getInstance().getConfig().getString("logger.debug-mode", "default"));
            Corex.getInstance().applyNetworkConfig();
            EventRegistry.resetAll();

            ScriptManager.reloadScripts();
            ItemContainer.ItemCache.clear();

            List<CommandContainer> after = ScriptManager.getContainersByType(CommandContainer.class);
            CommandManager.INSTANCE.updateContainers(after);

            CommandManager.INSTANCE.reinjectAll(after);

        } catch (Exception e) {
            Debugger.echoError(queue, "ERROR while reloading scripts. See console logs above this message.");
        }
    }
}