package dev.corexmc.corex.environment.commands.player;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.environment.tags.player.PlayerTag;
import dev.corexmc.corex.environment.tags.core.ListTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

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

        String targets = entry.getPrefix("targets", queue);

        if (targets != null) {
            ListTag targetList = new ListTag(targets);
            List<PlayerTag> players = targetList.filter(PlayerTag.class);


            for (PlayerTag pTag : players) {
                Player player = pTag.getPlayer();
                if (player != null && player.isOnline()) {
                    SchedulerAdapter.runEntity(player, () -> player.sendMessage(message));
                }
            }
        } else if (queue.getPlayer() != null && queue.getPlayer().getOfflinePlayer().isOnline()) {
            SchedulerAdapter.runEntity(queue.getPlayer().getPlayer(),
                    () -> queue.getPlayer().getPlayer().sendMessage(message));
        }
    }

    @Override
    public @NonNull String getSyntax() { return "[<text>] (targets:<player>|...)"; }
    @Override
    public int getMinArgs() { return 1; }
    @Override
    public int getMaxArgs() { return 2; }
}