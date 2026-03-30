package dev.corexmc.corex.environment.commands.player;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.compiler.Instruction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class NarrateCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "narrate";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction entry) {
        String text = entry.getLinear(0, queue);
        if (text == null) return;

        Component message = MiniMessage.miniMessage().deserialize(text);

        String targetsRaw = entry.getPrefix("targets", queue);

        if (targetsRaw != null) {
            Player target = Bukkit.getPlayer(targetsRaw.replace("p@", ""));
            if (target != null) {
                target.sendMessage(message);
            }
        } else {
            if (queue.getPlayer() != null && queue.getPlayer().getOfflinePlayer().isOnline()) {
                queue.getPlayer().getPlayer().sendMessage(message);
            } else {
                Bukkit.getConsoleSender().sendMessage(message);
            }
        }
    }

    @Override
    public void setSyntax(@NonNull String syntax) {}
    @Override
    public @NonNull String getSyntax() { return "- narrate [<text>] (targets:<player>|...)"; }
    @Override
    public int getMinArgs() { return 1; }
    @Override
    public int getMaxArgs() { return 2; }
}