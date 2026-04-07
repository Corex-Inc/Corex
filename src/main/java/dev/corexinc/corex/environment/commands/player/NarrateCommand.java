package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Narrate
 * @Syntax narrate [<text>] (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Shows some text to the player.
 *
 * @Implements Narrate
 *
 * @Description
 * Prints some text into the target's chat area. If no target is specified it will default to the attached player or the console.
 *
 * @Usage
 * // Use to narrate text to the player.
 * - narrate "Hello World!"
 *
 * @Usage
 * // Use to narrate text to a list of players.
 * - narrate "Hello there." targets:<[player]>|<[somePlayer]>|<[thatPlayer]>
 */
public class NarrateCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "narrate";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<text>] (targets:<player>|...)";
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
        AbstractTag text = entry.getLinearObject(0, queue);
        if (text == null) return;

        Component message = text.asComponent();

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
        else {
            Bukkit.getServer().getConsoleSender().sendMessage(message);
        }
    }
}