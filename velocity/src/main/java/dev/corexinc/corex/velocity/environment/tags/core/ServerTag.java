package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;

/* @doc object
 *
 * @Name ServerTag
 * @Prefix server
 * @Modules VELOCITY
 *
 * @Format
 * The identity format for ServerTags is 'server@<name>', where '<name>' is the name the backend
 * is registered under in velocity.toml, such as 'server@lobby'.
 *
 * @Description
 * A ServerTag is one backend server behind the proxy, the thing players are actually sent to.
 * Note the difference from the Paper plugin, where '<server>' means the server the script runs on.
 * Here there is no such thing, the proxy runs no world, so '<server[lobby]>' addresses a backend
 * by name and '<velocity>' covers the proxy itself.
 *
 * Fetching a name that is not registered gives null, so a script cannot accidentally hold a
 * ServerTag pointing at nothing.
 *
 * ServerTag implements Flaggable, so per backend data is stored on the tag itself. The flags live
 * on the proxy in 'proxyFlags.db', keyed by the server name from velocity.toml, and survive both a
 * proxy restart and the backend going down. Nothing of this reaches the backend itself, a Paper
 * server running Corex keeps its own separate '<server>' flags.
 *
 * @Usage
 * // Announce how busy the lobby is.
 * - narrate "The lobby has <server[lobby].players.size> players."
 *
 * @Usage
 * // Close one backend for an hour, then let it open again on its own.
 * - flag <server[minigame]> closed true expire:1h
 */
public class ServerTag implements AbstractTag, Flaggable {

    private static final String PREFIX = "server";

    public static final TagProcessor<ServerTag> TAG_PROCESSOR = new TagProcessor<>();

    private final RegisteredServer server;

    public ServerTag(RegisteredServer server) {
        this.server = server;
    }

    public ServerTag(String raw) {
        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;
        this.server = CorexVelocity.getInstance().getServer().getServer(clean).orElse(null);
    }

    public boolean isRegistered() {
        return server != null;
    }

    public RegisteredServer getServer() {
        return server;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", (attribute) -> {
            if (!attribute.hasParam()) return null;
            ServerTag tag = new ServerTag(attribute.getParam());
            return tag.isRegistered() ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, (name) -> {
            ServerTag tag = new ServerTag(name);
            return tag.isRegistered() ? tag : null;
        });

        /* @doc tag
         *
         * @Name name
         * @RawName <ServerTag.name>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the name the backend is registered under, the key from the servers block in
         * velocity.toml. This is the name every other tag and command takes.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) ->
                new ElementTag(object.server.getServerInfo().getName())).setAsyncSafe();

        /* @doc tag
         *
         * @Name address
         * @RawName <ServerTag.address>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the backend address as 'host:port'. This is where the proxy connects, not
         * anything a player can see or reach.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "address", (attribute, object) -> {
            InetSocketAddress address = object.server.getServerInfo().getAddress();
            return new ElementTag(address.getHostString() + ":" + address.getPort());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name host
         * @RawName <ServerTag.host>
         * @Object ServerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns just the host part of the backend address, without the port.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "host", (attribute, object) ->
                new ElementTag(object.server.getServerInfo().getAddress().getHostString())).setAsyncSafe();

        /* @doc tag
         *
         * @Name port
         * @RawName <ServerTag.port>
         * @Object ServerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns just the port of the backend address.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "port", (attribute, object) ->
                new ElementTag(object.server.getServerInfo().getAddress().getPort())).setAsyncSafe();

        /* @doc tag
         *
         * @Name players
         * @RawName <ServerTag.players>
         * @Object ServerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Async
         * @Description
         * Returns every player currently connected to this backend. The list is empty for a
         * server nobody is on, including one that is down, so it says nothing about whether the
         * backend is actually alive.
         *
         * @Usage
         * // Move everyone off a server before restarting it.
         * - foreach <server[minigame].players> as:player:
         *   - adjust <[player]> server:<server[lobby]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attribute, object) -> {
            ListTag list = new ListTag();
            object.server.getPlayersConnected().forEach(player -> list.addObject(new PlayerTag(player)));
            return list;
        }).setAsyncSafe();
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + server.getServerInfo().getName();
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        if (server == null) return null;
        return new SqlFlagTracker(VelocityTag.flagsFile(), identify());
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<ServerTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "server@lobby";
    }
}
