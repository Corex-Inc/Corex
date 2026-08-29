package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
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
 * A registered backend is 'server@<name>', where '<name>' is the name it is registered under in
 * velocity.toml, such as 'server@lobby'.
 *
 * A blueprint for a server that does not exist yet is 'server@<name>[address=<host>:<port>]', for
 * example 'server@arena[address=127.0.0.1:25566]'. Nothing is registered by writing one.
 *
 * @Description
 * A ServerTag is one backend server behind the proxy, the thing players are actually sent to.
 * Note the difference from the Paper plugin, where '<server>' means the server the script runs on.
 * Here there is no such thing, the proxy runs no world, so '<server[lobby]>' addresses a backend
 * by name and '<velocity>' covers the proxy itself.
 *
 * A name the proxy does not know gives null, unless an address is written with it. That form is a
 * blueprint: a name and an address, nothing running behind them, which is what
 * <@link mechanism VelocityTag.registerServer> takes to bring a backend up while the proxy runs.
 * It is the same idea as an EntityTag holding a type nobody has spawned yet.
 * Use <@link tag ServerTag.isRegistered> to tell the two apart.
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
 *
 * @Usage
 * // Bring a match server up and send the party to it.
 * - adjust <velocity> registerServer:<server[arena1[address=127.0.0.1:25801]]>
 * - foreach <[party]> as:member:
 *   - adjust <[member]> server:<server[arena1]>
 */
public class ServerTag implements AbstractTag, Flaggable {

    private static final String PREFIX = "server";

    public static final TagProcessor<ServerTag> TAG_PROCESSOR = new TagProcessor<>();

    private final RegisteredServer server;
    private final ServerInfo info;

    public ServerTag(RegisteredServer server) {
        this.server = server;
        this.info = server.getServerInfo();
    }

    public ServerTag(String raw) {
        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;

        String name = clean;
        InetSocketAddress declaredAddress = null;

        int bracketStart = clean.indexOf('[');
        if (bracketStart > 0 && clean.endsWith("]")) {
            name = clean.substring(0, bracketStart);
            declaredAddress = readAddress(clean.substring(bracketStart + 1, clean.length() - 1));
        }

        RegisteredServer resolved = CorexVelocity.getInstance().getServer().getServer(name).orElse(null);
        this.server = resolved;

        if (resolved != null) {
            this.info = resolved.getServerInfo();
        }
        else if (declaredAddress != null && !name.isBlank()) {
            this.info = new ServerInfo(name, declaredAddress);
        }
        else {
            this.info = null;
        }
    }

    private static InetSocketAddress readAddress(String bracketContent) {
        for (String pair : bracketContent.split(";")) {
            int equals = pair.indexOf('=');
            if (equals <= 0) continue;
            if (!pair.substring(0, equals).strip().equalsIgnoreCase("address")) continue;
            return parseAddress(pair.substring(equals + 1).strip());
        }
        return null;
    }

    public static InetSocketAddress parseAddress(String raw) {
        if (raw == null) return null;

        int separator = raw.lastIndexOf(':');
        if (separator <= 0 || separator == raw.length() - 1) return null;

        try {
            int port = Integer.parseInt(raw.substring(separator + 1).strip());
            if (port < 1 || port > 65535) return null;
            return InetSocketAddress.createUnresolved(raw.substring(0, separator).strip(), port);
        }
        catch (NumberFormatException exception) {
            return null;
        }
    }

    public boolean isRegistered() {
        return server != null;
    }

    public RegisteredServer getServer() {
        return server;
    }

    public ServerInfo getInfo() {
        return info;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", (attribute) -> {
            if (!attribute.hasParam()) return null;
            ServerTag tag = new ServerTag(attribute.getParam());
            return tag.info != null ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, (name) -> {
            ServerTag tag = new ServerTag(name);
            return tag.info != null ? tag : null;
        });

        /* @doc tag
         *
         * @Name isRegistered
         * @RawName <ServerTag.isRegistered>
         * @Object ServerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' if the proxy knows this backend, 'false' for a blueprint that was written
         * with an address but never registered. Only a registered backend has players on it, and
         * only a blueprint is worth handing to
         * <@link mechanism VelocityTag.registerServer>.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isRegistered", (attribute, object) ->
                new ElementTag(object.server != null)).setAsyncSafe();

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
                new ElementTag(object.info.getName())).setAsyncSafe();

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
            InetSocketAddress address = object.info.getAddress();
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
                new ElementTag(object.info.getAddress().getHostString())).setAsyncSafe();

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
                new ElementTag(object.info.getAddress().getPort())).setAsyncSafe();

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
         * server nobody is on, including one that is down and one that is still a blueprint, so it
         * says nothing about whether the backend is actually alive.
         *
         * @Usage
         * // Move everyone off a server before restarting it.
         * - foreach <server[minigame].players> as:player:
         *   - adjust <[player]> server:<server[lobby]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attribute, object) -> {
            ListTag list = new ListTag();
            if (object.server == null) return list;
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
        if (server != null) return PREFIX + "@" + info.getName();
        InetSocketAddress address = info.getAddress();
        return PREFIX + "@" + info.getName() + "[address=" + address.getHostString() + ":" + address.getPort() + "]";
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        if (info == null) return null;
        return new SqlFlagTracker(VelocityTag.flagsFile(), PREFIX + "@" + info.getName());
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
