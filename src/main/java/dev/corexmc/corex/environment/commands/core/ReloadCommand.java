package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.scripts.ScriptManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NonNull;

public class ReloadCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "reload";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        long start = System.currentTimeMillis();

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

    @Override public void setSyntax(@NonNull String syntax) {}
    @Override public @NonNull String getSyntax() { return "- reload"; }
    @Override public int getMinArgs() { return 0; }
    @Override public int getMaxArgs() { return 0; }
}