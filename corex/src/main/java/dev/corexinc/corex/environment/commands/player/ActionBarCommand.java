package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name ActionBar
 * @Syntax actionbar [<text>] (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Displays a message above the player's hotbar.
 *
 * @Implements ActionBar
 *
 * @Description
 * Sends a message to the target's action bar area (the space right above the inventory hotbar).
 * If no target is explicitly specified, it defaults to the player attached to the current script queue.
 *
 * Note: If you want to show personalized messages (like inserting the specific player's name in a mass-broadcast),
 * do not use a global actionbar. Instead, loop through the players using the `foreach` command and send an actionbar individually.
 *
 * @Usage
 * // Send a simple colored message to the attached player.
 * - actionbar "<&a>Welcome to the safe zone!"
 *
 * @Usage
 * // Broadcast an actionbar to multiple specific players.
 * - actionbar "<&l><&c>The boss has spawned!" targets:<server.onlinePlayers>
 *
 * @Usage
 * // Send a personalized message to everyone on the server using a loop.
 * - foreach <server.onlinePlayers> as:target:
 *     - actionbar "<&7>Hello, <&b><[target].name><gray>! Your ping is <[target].ping>ms." targets:<[target]>
 */
public class ActionBarCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "actionbar";
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
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        AbstractTag textTag = instruction.getLinearObject(0, queue);
        if (textTag == null) {
            Debugger.echoError(queue, "Cannot send an empty actionbar! Text is required.");
            return;
        }

        final Component message = buildComponent(textTag);
        String targetsRaw = instruction.getPrefix("targets", queue);

        List<Player> targetPlayers = new ArrayList<>();

        if (targetsRaw != null) {
            List<PlayerTag> playerTags = new ListTag(targetsRaw).filter(PlayerTag.class, queue);
            for (PlayerTag pTag : playerTags) {
                Player player = pTag.getPlayer();
                if (player != null && player.isOnline()) {
                    targetPlayers.add(player);
                }
            }
        } else {
            PlayerTag queuePlayer = queue.getPlayer();
            if (queuePlayer != null && queuePlayer.getPlayer() != null && queuePlayer.getPlayer().isOnline()) {
                targetPlayers.add(queuePlayer.getPlayer());
            }
        }

        Debugger.report(queue, instruction,
                "Message", textTag.identify(),
                "Targets", targetsRaw != null ? targetsRaw : "Attached Player",
                "Targets_Count", targetPlayers.size()
        );

        if (targetPlayers.isEmpty()) {
            if (targetsRaw == null) {
                Debugger.echoError(queue, "No valid targets found and no player attached to the queue.");
            }
            return;
        }

        for (Player player : targetPlayers) {
            SchedulerAdapter.runEntity(player, () -> player.sendActionBar(message));
        }
    }

    private Component buildComponent(@NonNull AbstractTag text) {
        Component component = text.asComponent();
        return component != null ? component : Component.text(text.identify());
    }
}