package dev.corexinc.corex.velocity.environment.commands.player;

import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name kick
 * @Syntax kick [<player>|...] (reason:<text>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @Modules VELOCITY
 * @ShortDescription Kicks a player from the network.
 *
 * @Implements Kick
 *
 * @Description
 * Kick a player or a list of players from the proxy and optionally specify a reason.
 * If no reason is specified the disconnect screen is left blank.
 *
 * Kicking here drops the player out of the network entirely rather than off one backend, and it
 * works while their backend is down or still loading.
 *
 * @Usage
 * // Use to kick the player with no reason.
 * - kick <player>
 *
 * @Usage
 * // Use to kick the player with a reason.
 * - kick <player> "reason:Because I can."
 *
 * @Usage
 * // Use to kick everyone else with a reason.
 * - kick <velocity.players.exclude[<player>]> "reason:I.. AM.. GOOOOD!!"
 */
public class KickCommand implements AbstractCommand {

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ListTag.class)
            .optionalPrefix("reason", ElementTag.class)
            .build();

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
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        ListTag targetList = args.linear(0);
        ElementTag reasonRaw = args.prefix("reason");
        boolean failed = false;

        final Component reason = (reasonRaw == null ? Component.empty() : reasonRaw.asComponent());
        List<PlayerTag> players = targetList.filter(PlayerTag.class, queue);

        if (players.isEmpty()) {
            Debugger.echoError(queue, getName() + ": no players found in '" + targetList.identify() + "'");
            failed = true;
        }

        Debugger.report(queue, instruction,
            "Players", targetList.identify(),
                "Reason", reasonRaw
        );

        if (failed) return;

        for (PlayerTag pTag : players) {
            Player player = pTag.getPlayer().orElse(null);
            if (player != null && player.isActive()) {
                player.disconnect(reason);
            } else {
                Debugger.echoError(queue, getName() + ": player '" + targetList.identify() + "' is offline or not found");
            }
        }
    }
}
