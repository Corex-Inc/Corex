package dev.corexinc.corex.velocity.environment.commands.player;

import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Narrate
 * @Syntax narrate [<text>] (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @Modules VELOCITY
 * @ShortDescription Sends a message to players through the proxy.
 *
 * @Implements Narrate
 *
 * @Description
 * Sends a chat message. With no 'targets:' it goes to the queue's linked player, and to the proxy
 * console when the queue has no player, which is what a scheduled task gets.
 *
 * The proxy sends this straight to the client, so it arrives whatever the player's backend is
 * doing, and no backend plugin can see or cancel it.
 *
 * @Usage
 * // Message the linked player.
 * - narrate "<green>Welcome back!"
 *
 * @Usage
 * // Announce to the whole network.
 * - narrate "<gray>Restarting in 5 minutes." targets:<velocity.players>
 *
 * @Usage
 * // Message everyone on one backend.
 * - narrate "The arena is opening." targets:<server[arena].players>
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
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        AbstractTag text = instruction.getLinearObject(0, queue);
        if (text == null) {
            Debugger.echoError(queue, "Empty text argument are not allowed");
            return;
        }

        text.asComponent();
        Component message = text.asComponent();

        String targets = instruction.getPrefix("targets", queue);

        Debugger.report(queue, instruction,
                "Narrating", text.identify(),
                "Targets", targets
        );

        if (targets != null) {
            List<PlayerTag> playerTags = new ListTag(targets).filter(PlayerTag.class, queue);
            List<Player> online = new ArrayList<>(playerTags.size());
            for (PlayerTag tag : playerTags) {
                tag.getPlayer().ifPresent(online::add);
            }
            for (Player player : online) {
                player.sendMessage(message);
            }
        } else {
            PlayerTag queuePlayer = (PlayerTag) queue.getPlayer();
            if (queuePlayer != null) {
                queuePlayer.getPlayer().ifPresent(p -> p.sendMessage(message));
            } else {
                CorexVelocity.getInstance().getServer().getConsoleCommandSource().sendMessage(message);
            }
        }
    }
}