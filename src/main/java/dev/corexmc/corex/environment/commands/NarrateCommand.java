package dev.corexmc.corex.environment.commands;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.queue.CommandEntry;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NarrateCommand implements AbstractCommand {

    @Override
    public String getName() {
        return "narrate";
    }

    @Override
    public void run(ScriptQueue queue, CommandEntry entry) {
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
    public void setSyntax(String syntax) {}
    @Override
    public String getSyntax() { return "- narrate [<text>] (targets:<player>|...)"; }
    @Override
    public int getMinArgs() { return 1; }
    @Override
    public int getMaxArgs() { return 2; }
}