package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.api.commands.DataBlockCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.network.CorexPacket;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.network.NetworkTarget;
import dev.corexinc.corex.engine.network.SendResult;
import dev.corexinc.corex.engine.network.packets.NetworkMessagePacket;
import dev.corexinc.corex.engine.network.packets.RoutedPacket;
import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/* @doc command
 *
 * @Name Proxy
 * @Syntax proxy [send/script] (channel:<name>) (data:<object>) (to:<server>)
 * @RequiredArgs 1
 * @MaxArgs 4
 * @Aliases velocity
 * @ShortDescription Sends a message or a script to the proxy, or to another server behind it.
 *
 * @Description
 * Talks to the Velocity proxy your server sits behind, and through it to the other backends.
 *
 * "send" delivers a payload on a channel. "script" runs a block of Corex lines somewhere else,
 * written inline under the command:
 *
 * - proxy script:
 *     - narrate "Hello from another server."
 *
 * The block is sent uncompiled and compiled on the far side, against that server's registry, so
 * it can use commands and tags this one does not have. Nested blocks work, an "if:" or a
 * "repeat:" inside travels the same as a flat line.
 *
 * "to:" picks the destination. Leave it off and the packet stays on the proxy and runs there,
 * which is the point of the whole thing: the proxy has commands no backend does. Use "to:*" for
 * every other backend, or "to:lobby" for one by name. A "send" needs a real destination, since
 * the proxy has no script events to deliver to.
 *
 * A ServerTag works in place of the name, "to:<server[lobby]>", and "to:<proxy>" says the proxy
 * out loud rather than by leaving the argument off. Both are the same destinations written as
 * objects, so a script that already holds one does not have to dig the name back out of it.
 *
 * The channel decides who hears a "send". A plain word like "bossDown" is a Corex channel and
 * fires the "proxy message" event on the far side. A word with a namespace, like
 * "myplugin:data", is a real plugin messaging channel, and the payload lands there for an
 * unrelated plugin to pick up. That second form writes the object's text as a single UTF string,
 * the shape a plugin reading a Bungee style message with readUTF() expects.
 *
 * "script" is refused unless every server shares a secret: put the same CX_NETWORK_SECRET line in
 * secrets.env on the proxy and on each backend. Without it a modded client could send the same
 * packet, so Corex signs them and drops anything unsigned. The receiving server also needs
 * allow-remote-execution turned on in the network section of its config.yml.
 *
 * To run the far side script as a player, use the "player:" global flag on the command. There is
 * no "player:" argument of its own, since the global flag already owns that word.
 *
 * Delivery rides a player connection, so nothing goes out while the sending server is empty and
 * nothing arrives while the receiving one is. That is reported as an error rather than dropped
 * quietly, so wrap the command in a try block if a miss is survivable.
 *
 * @Usage
 * // Tell every other backend the boss died.
 * - proxy send channel:bossDown data:<map[boss=wither;world=<player.location.world.name>]> to:*
 *
 * @Usage
 * // Hand a payload to an unrelated plugin listening on its own channel.
 * - proxy send channel:myplugin:data data:<[payload]> to:lobby
 *
 * @Usage
 * // Run something on the proxy itself, where the proxy only commands live.
 * - proxy script:
 *     - connect <player> lobby
 *
 * @Usage
 * // Run a block on another backend as the player who triggered this. Note where the colon goes:
 * // the whole command line is the key, so any prefix sits before it.
 * - proxy script to:lobby player:<player>:
 *     - narrate "Welcome back."
 *     - flag <player> visits:++
 */
public class ProxyCommand implements AbstractCommand, DataBlockCommand {

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ElementTag.class)
            .optionalPrefix("channel", ElementTag.class, "default")
            .optionalPrefix("data", AbstractTag.class)
            .optionalPrefix("to", AbstractTag.class, RoutedPacket.PROXY)
            .build();

    @Override
    public @NonNull String getName() {
        return "proxy";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("velocity");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[send/script] (channel:<name>) (data:<object>) (to:<server>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        ElementTag action = args.linear(0);
        ElementTag channel = args.prefix("channel");
        AbstractTag data = args.prefix("data");
        String target = targetOf(args.prefix("to"));

        Debugger.report(queue, instruction,
                "Action", action.asString(),
                "Channel", channel.asString(),
                "Data", data != null ? data.identify() : null,
                "To", target.isEmpty() ? "proxy" : target
        );

        if (!NetworkManager.isAvailable()) {
            Debugger.echoError(queue, "The Corex network layer is not running. "
                    + "Enable it in the network section of config.yml.");
            return;
        }

        CorexPacket packet = switch (action.asString().toLowerCase()) {
            case "send" -> buildMessage(queue, channel.asString(), data, target);
            case "script" -> buildScript(queue, instruction, target);
            default -> {
                Debugger.echoError(queue, "Unknown proxy action: '<red>" + action.asString() + "</red>'.");
                Debugger.echoError(queue, "Use send or script.");
                yield null;
            }
        };

        if (packet == null) return;

        SendResult result = NetworkManager.send(packet);
        if (!result.delivered()) {
            Debugger.echoError(queue, "Could not send the proxy " + action.asString()
                    + ": " + result.reason() + ".");
        }
    }

    private @Nullable CorexPacket buildMessage(@NonNull ScriptQueue queue,
                                               @NonNull String channel,
                                               @Nullable AbstractTag data,
                                               @NonNull String target) {
        if (data == null) {
            Debugger.echoError(queue, "proxy send needs a data: argument to deliver.");
            return null;
        }
        if (channel.isBlank()) {
            Debugger.echoError(queue, "The channel name cannot be empty.");
            return null;
        }
        if (RoutedPacket.PROXY.equals(target)) {
            Debugger.echoError(queue, "proxy send needs a to: destination. The proxy has no script "
                    + "events to deliver a message to, so use to:* or to:<server>.");
            return null;
        }
        return new NetworkMessagePacket(target, channel, data);
    }

    private @Nullable CorexPacket buildScript(@NonNull ScriptQueue queue,
                                              @NonNull Instruction instruction,
                                              @NonNull String target) {
        if (!(instruction.customData instanceof List<?> block) || block.isEmpty()) {
            Debugger.echoError(queue, "proxy script needs a block of lines written under it, like:");
            Debugger.echoError(queue, "<gray>- proxy script:</gray>");
            Debugger.echoError(queue, "<gray>    - narrate \"Hello.\"</gray>");
            return null;
        }
        return new ScriptPacket(target, block, resolvePlayer(queue));
    }

    private static @NonNull String targetOf(@NonNull AbstractTag target) {
        return target instanceof NetworkTarget named ? named.networkTarget() : target.identify();
    }

    private @Nullable UUID resolvePlayer(@NonNull ScriptQueue queue) {
        PlayerIdentity player = queue.getPlayer();
        return player != null ? player.getUniqueId() : null;
    }
}
