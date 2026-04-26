package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
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
        if (text == null) {
            Debugger.echoError(queue, "Empty text argument are not allowed");
            return;
        }

        Component message = buildComponent(text);

        String targets = entry.getPrefix("targets", queue);

        Debugger.report(queue, entry,
                "Narrating", text.identify(),
                "Targets", targets
        );

        if (targets != null) {
            sendToTargets(queue, entry, targets, message);
        } else {
            sendToQueuePlayerOrConsole(queue, message);
        }
    }

    private Component buildComponent(@NonNull AbstractTag text) {
        Component component = text.asComponent();
        return component != null ? component : Component.text(text.identify());
    }

    private void sendToTargets(@NonNull ScriptQueue queue, @NonNull Instruction entry,
                               @NonNull String targets, @NonNull Component message) {
        List<PlayerTag> playerTags = new ListTag(targets).filter(PlayerTag.class, queue);

        List<Player> onlinePlayers = new ArrayList<>(playerTags.size());
        for (PlayerTag pTag : playerTags) {
            Player player = pTag.getPlayer();
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }

        if (onlinePlayers.isEmpty()) return;

        SchedulerAdapter.run(() -> {
            for (Player player : onlinePlayers) {
                player.sendMessage(message);
            }
        });
    }

    private void sendToQueuePlayerOrConsole(@NonNull ScriptQueue queue, @NonNull Component message) {
        PlayerTag queuePlayer = queue.getPlayer();
        if (queuePlayer != null && queuePlayer.getOfflinePlayer().isOnline()) {
            Player player = queuePlayer.getPlayer();
            SchedulerAdapter.runEntity(player, () -> player.sendMessage(message));
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(message);
        }
    }
}