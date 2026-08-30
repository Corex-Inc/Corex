package dev.corexinc.corex.velocity.environment.commands.core;

import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.utils.scripts.CommandHelper;
import dev.corexinc.corex.velocity.environment.tags.core.ServerTag;
import org.jspecify.annotations.NonNull;

import java.time.Duration;

/* @doc command
 *
 * @Name Ping
 * @Syntax ping [<server>] (timeout:<duration>) (save:<name>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @Waitable
 * @Modules VELOCITY
 * @ShortDescription Checks whether a backend server is alive and reads its status.
 *
 * @Description
 * Asks a backend server for the same status a client sees in its server list, and reports whether
 * it answered at all. This is the only way the proxy can tell a live server from a dead one:
 * a server being registered means it is in the config, and an empty player list means nobody is
 * on it, neither says anything about it running.
 *
 * The answer comes back over the network, so run this waitable with '~' if the next line needs
 * the result. Without '~' the script carries on and the saved result lands a moment later.
 *
 * The result is a map, saved under 'save:'. It always has 'reachable'. When that is true it also
 * has 'latency' (the round trip in milliseconds, measured here), 'motd', 'versionName',
 * 'versionProtocol', and, for a server that reports them, 'onlinePlayers', 'maxPlayers' and
 * 'sample'. When it is false the only other key is 'error'.
 *
 * 'sample' is the short list a client shows on hover over the player count. Treat it as
 * decoration, not data: a vanilla server puts a random dozen names in there whatever the real
 * count is, plenty of servers replace it with advertising lines, and it can be turned off
 * entirely. Count players with 'onlinePlayers'.
 *
 * 'timeout:' defaults to 5s. A server that is down usually refuses the connection long before
 * that, the timeout only matters for a machine that accepts the connection and then goes quiet.
 *
 * There is deliberately no tag version of this. A tag has to answer immediately, and there is no
 * way to know whether a server is up without waiting for the network.
 *
 * @Usage
 * // Check one server before sending anyone there.
 * - ~ping <server[arena]> save:status
 * - if <[status].get[reachable]>:
 *   - adjust <player> server:<server[arena]>
 *   - else:
 *     - narrate "<red>The arena is down, try again in a minute."
 *
 * @Usage
 * // Report the state of the whole network to an admin.
 * - foreach <proxy.servers> as:backend:
 *   - ~ping <[backend]> timeout:2s save:status
 *   - if <[status].get[reachable]>:
 *     - narrate "<green><[backend].name>: up, <[status].get[latency]>ms, <[status].get[onlinePlayers]> online"
 *     - else:
 *       - narrate "<red><[backend].name>: down (<[status].get[error]>)"
 */
public class PingCommand implements AbstractCommand {

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ServerTag.class)
            .optionalPrefix("timeout", DurationTag.class, "5s")
            .build();

    @Override
    public @NonNull String getName() {
        return "ping";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<server>] (timeout:<duration>)";
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

        ServerTag target = args.linear(0);
        if (!target.isRegistered()) {
            Debugger.echoError(queue, "No server registered on this proxy under that name.");
            return;
        }

        DurationTag timeout = args.prefix("timeout");
        PingOptions options = PingOptions.builder()
                .timeout(Duration.ofMillis(timeout.getMilliseconds()))
                .build();

        Debugger.report(queue, instruction,
                "Server", target.identify(),
                "Timeout", timeout.identify(),
                "Waitable", instruction.isWaitable
        );

        if (instruction.isWaitable) {
            queue.pause();
        }

        long startedAt = System.nanoTime();

        target.getServer().ping(options).whenComplete((ping, throwable) -> {
            SchedulerAdapter.get().runLater(() -> {
                MapTag result = new MapTag();

                if (throwable != null) {
                    result.putObject("reachable", new ElementTag(false));
                    result.putObject("error", new ElementTag(describe(throwable)));
                }
                else {
                    result.putObject("reachable", new ElementTag(true));
                    result.putObject("latency", new ElementTag((System.nanoTime() - startedAt) / 1_000_000L));
                    result.putObject("motd", new ElementTag(ping.getDescriptionComponent()));
                    result.putObject("versionName", new ElementTag(ping.getVersion().getName()));
                    result.putObject("versionProtocol", new ElementTag(ping.getVersion().getProtocol()));

                    ping.getPlayers().ifPresent(players -> {
                        result.putObject("onlinePlayers", new ElementTag(players.getOnline()));
                        result.putObject("maxPlayers", new ElementTag(players.getMax()));
                        ListTag sample = new ListTag();
                        for (ServerPing.SamplePlayer player : players.getSample()) {
                            sample.addString(player.getName());
                        }
                        result.putObject("sample", sample);
                    });
                }

                CommandHelper.saveResult(queue, instruction, result);

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
