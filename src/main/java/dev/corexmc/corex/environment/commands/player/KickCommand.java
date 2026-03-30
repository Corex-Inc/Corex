package dev.corexmc.corex.environment.commands.player;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.ListTag;
import dev.corexmc.corex.environment.tags.PlayerTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class KickCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "kick";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction entry) {
        String firstArg = entry.getLinear(0, queue);
        String secondArg = entry.getPrefix("reason", queue);

        // TODO нужна проверка количества аргументов, желательно автоматически

        Component reason = (secondArg == null ? null : MiniMessage.miniMessage().deserialize(secondArg));

        ListTag targetList = new ListTag(firstArg);

        List<PlayerTag> players = targetList.filter(PlayerTag.class);

        for (PlayerTag pTag : players) {
            Player p = pTag.getPlayer();
            if (p != null && p.isOnline()) {
                p.kick(reason);
            }
        }
    }

    @Override
    public void setSyntax(@NonNull String syntax) {}
    @Override
    public @NonNull String getSyntax() { return "- kick [<player>|...] (reason:<text>)"; }
    @Override
    public int getMinArgs() { return 1; }
    @Override
    public int getMaxArgs() { return 2; }
}
