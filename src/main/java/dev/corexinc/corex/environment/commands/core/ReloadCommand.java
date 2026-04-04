package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.environment.events.EventRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;

/* @[command]
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
        long start = System.currentTimeMillis();

        EventRegistry.resetAll();
        ScriptManager.reloadScripts();

        long end = System.currentTimeMillis() - start;

        Component msg = Component.text("[Corex] on " + end + "ms.")
                .color(NamedTextColor.AQUA);

        if (queue.getPlayer() != null && queue.getPlayer().getOfflinePlayer().isOnline()) {
            queue.getPlayer().getPlayer().sendMessage(msg);
        } else {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(msg);
        }
    }
}