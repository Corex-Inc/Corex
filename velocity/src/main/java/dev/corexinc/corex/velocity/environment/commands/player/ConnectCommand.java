package dev.corexinc.corex.velocity.environment.commands.player;

import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.scripts.CommandHelper;
import dev.corexinc.corex.velocity.environment.tags.core.ServerTag;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Connect
 * @Syntax connect [<server>] (target:<player>) (save:<name>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @Waitable
 * @Modules VELOCITY
 * @ShortDescription Moves one player to a backend server and reports how it went.
 *
 * @Description
 * Sends a player to a backend server and, unlike the 'server' mechanism, tells you what happened.
 * With no 'target:' it moves the queue's linked player.
 *
 * The move takes a round trip to the backend, so run this waitable with '~' if the next line
 * depends on the player having arrived.
 *
 * The result is a map, saved under 'save:'. 'successful' is the boolean to branch on, 'status' is
 * one of SUCCESS, ALREADY_CONNECTED, CONNECTION_IN_PROGRESS, CONNECTION_CANCELLED or
 * SERVER_DISCONNECTED, 'server' is the ServerTag that was attempted, and 'reason' carries the backend's
 * message when there is one. A connection that threw outright reports status ERROR.
 *
 * CONNECTION_CANCELLED means another plugin refused the move. SERVER_DISCONNECTED means the
 * backend dropped the player, which is what a server that is down looks like from here.
 *
 * This handles one player on purpose. To move a crowd, where per player results are noise, use
 * '- adjust <player> server:<server[name]>', which fires and forgets.
 *
 * @Usage
 * // Move the player and react if the server refused.
 * - ~connect <server[arena]> save:move
 * - if !<[move].get[successful]>:
 *   - narrate "<red>Could not send you: <[move].get[status]>"
 *
 * @Usage
 * // Move someone else, without waiting.
 * - connect <server[lobby]> target:<velocity.matchPlayer[Steve]>
 */
public class ConnectCommand implements AbstractCommand {

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ServerTag.class)
            .optionalPrefix("target", PlayerTag.class)
            .build();

    @Override
    public @NonNull String getName() {
        return "connect";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<server>] (target:<player>)";
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
    public boolean setCanBeWaitable() {
        return true;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        ServerTag destination = args.linear(0);
        if (!destination.isRegistered()) {
            Debugger.echoError(queue, "No server registered on this proxy under that name.");
            return;
        }

        PlayerTag target = args.prefix("target");
        if (target == null && queue.getPlayer() instanceof PlayerTag linked) {
            target = linked;
        }

        if (target == null || !target.isOnline()) {
            Debugger.echoError(queue, "No online player to connect.");
            return;
        }

        Player player = target.getPlayer().orElse(null);
        if (player == null) {
            Debugger.echoError(queue, "No online player to connect.");
            return;
        }

        Debugger.report(queue, instruction,
                "Server", destination.identify(),
                "Target", target.getName(),
                "Waitable", instruction.isWaitable
        );

        if (instruction.isWaitable) {
            queue.pause();
        }

        player.createConnectionRequest(destination.getServer()).connect().whenComplete((result, throwable) -> {
            SchedulerAdapter.get().runLater(() -> {
                MapTag saved = new MapTag();
                saved.putObject("server", destination);

                if (throwable != null) {
                    saved.putObject("successful", new ElementTag(false));
                    saved.putObject("status", new ElementTag("ERROR"));
                    saved.putObject("reason", new ElementTag(describe(throwable)));
                }
                else {
                    saved.putObject("successful", new ElementTag(result.isSuccessful()));
                    saved.putObject("status", new ElementTag(result.getStatus().name()));
                    result.getReasonComponent().ifPresent(reason ->
                            saved.putObject("reason", new ElementTag(reason)));
                }

                CommandHelper.saveResult(queue, instruction, saved);

                if (instruction.isWaitable) {
                    if (queue.isAsync()) {
                        SchedulerAdapter.get().runAsync(queue::resume);
                    }
                    else {
                        queue.resume();
                    }
                }
            }, 1L);
        });
    }

    private String describe(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String message = cause.getMessage();
        return message != null ? message : cause.getClass().getSimpleName();
    }
}
