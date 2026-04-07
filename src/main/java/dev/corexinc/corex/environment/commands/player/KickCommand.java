package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.*;

/* @doc command
 *
 * @Name kick
 * @Syntax kick [<player>|...] (reason:<text>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Kicks a player from the server.
 *
 * @Implements Kick
 *
 * @Description
 * Kick a player or a list of players from the server and optionally specify a reason.
 * If no reason is specified it defaults to "Kicked."
 *
 * @Usage
 * // Use to kick the player with the default reason.
 * - kick <player>
 *
 * @Usage
 * // Use to kick the player with a reason.
 * - kick <player> "reason:Because I can."
 *
 * @Usage
 * // Use to kick another player with a reason.
 * - kick <server.onlinePlayers.exclude[<player>]> "reason:I.. AM.. GOOOOD!!"
 */
public class KickCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "kick";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<player>|...] (reason:<text>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction entry) {
        String firstArg = entry.getLinear(0, queue);
        String secondArg = entry.getPrefix("reason", queue);

        final Component reason = (secondArg == null ? null : MiniMessage.miniMessage().deserialize(secondArg));

        ListTag targetList = new ListTag(firstArg);
        List<PlayerTag> players = targetList.filter(PlayerTag.class);

        if (players.isEmpty()) {
            Debugger.error(queue, getName() + ": no players found in '" + firstArg + "'", 0);
            return;
        }

        for (PlayerTag pTag : players) {
            Player player = pTag.getPlayer();
            if (player != null && player.isOnline()) {
                SchedulerAdapter.runEntity(player, () -> player.kick(reason));
            } else {
                Debugger.error(queue, getName() + ": player '" + pTag.getPlayer().getName() + "' is offline or not found", 0);
            }
        }
    }
}