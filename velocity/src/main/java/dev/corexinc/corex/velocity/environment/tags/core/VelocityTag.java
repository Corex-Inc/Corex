package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ServerLink;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import dev.corexinc.corex.velocity.environment.utils.ServerLinkHelper;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* @doc object
 *
 * @Name VelocityTag
 * @Prefix velocity
 * @Modules VELOCITY
 *
 * @Format
 * The basic format for a VelocityTag is simply 'velocity@'. There is only ever one, so the base
 * tag takes no input.
 *
 * @Description
 * The VelocityTag is the proxy itself: the players connected to it, the backends registered on
 * it, and what its config says. It is the proxy counterpart of ServerTag on the Paper plugin.
 *
 * Everything here describes the proxy, never a backend. The proxy runs no world, has no tick
 * loop and holds no blocks, so nothing world shaped lives on this tag.
 *
 * VelocityTag implements Flaggable, so it is where network wide data lives, the proxy counterpart
 * of flags on '<server>' in the Paper plugin. They are kept in 'proxyFlags.db' next to the config
 * and survive a proxy restart. Backends have their own, see <@link tag ServerTag.flag>.
 *
 * @Usage
 * // Report the network population.
 * - narrate "<velocity.players.size> of <velocity.maxPlayers> players online."
 *
 * @Usage
 * // Put the whole network into maintenance until someone lifts it.
 * - flag <velocity> maintenance true
 */
public class VelocityTag implements AbstractTag, Flaggable, Adjustable {

    private static final String PREFIX = "velocity";

    public static final TagProcessor<VelocityTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<VelocityTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(PREFIX, (attribute) -> new VelocityTag());

        /* @doc tag
         *
         * @Name version
         * @RawName <VelocityTag.version>
         * @Object VelocityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the version of the proxy software itself, such as '3.5.0'. This is the Velocity
         * build, not any Minecraft version, for which see <@link tag VelocityTag.supportedVersions>.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "version", (attribute, object) ->
                new ElementTag(proxy().getVersion().getVersion())).setAsyncSafe();

        /* @doc tag
         *
         * @Name players
         * @RawName <VelocityTag.players>
         * @Object VelocityTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Async
         * @Description
         * Returns every player connected to the proxy, across all backends. Players still in the
         * login handshake are not in here yet.
         *
         * @Usage
         * // Announce something to the whole network.
         * - narrate "Server restarting in 5 minutes." targets:<velocity.players>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attribute, object) -> {
            ListTag list = new ListTag();
            proxy().getAllPlayers().forEach(player -> list.addObject(new PlayerTag(player)));
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name servers
         * @RawName <VelocityTag.servers>
         * @Object VelocityTag
         * @ReturnType ListTag(ServerTag)
         * @NoArg
         * @Async
         * @Description
         * Returns every backend registered on the proxy, both the ones from velocity.toml and any
         * a plugin added while running. Registered does not mean reachable, a server that is down
         * still shows up here.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "servers", (attribute, object) -> {
            ListTag list = new ListTag();
            proxy().getAllServers().forEach(server -> list.addObject(new ServerTag(server)));
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name plugins
         * @RawName <VelocityTag.plugins>
         * @Object VelocityTag
         * @ReturnType ListTag(PluginTag)
         * @NoArg
         * @Async
         * @Description
         * Returns every plugin loaded on the proxy, in load order. These are the proxy's own
         * plugins, the ones on the backend servers are not visible from here.
         *
         * @Usage
         * // List what the proxy is running.
         * - foreach <velocity.plugins> as:entry:
         *   - narrate "<[entry].name> v<[entry].version.ifNull[?]>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "plugins", (attribute, object) -> {
            ListTag list = new ListTag();
            proxy().getPluginManager().getPlugins().forEach(container -> list.addObject(new PluginTag(container)));
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name address
         * @RawName <VelocityTag.address>
         * @Object VelocityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the address the proxy listens on as 'host:port'. On the usual setup the host is
         * '0.0.0.0', meaning every interface, so this is not the address players type.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "address", (attribute, object) -> {
            InetSocketAddress address = proxy().getBoundAddress();
            return new ElementTag(address.getHostString() + ":" + address.getPort());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name host
         * @RawName <VelocityTag.host>
         * @Object VelocityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns just the host the proxy listens on, without the port.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "host", (attribute, object) ->
                new ElementTag(proxy().getBoundAddress().getHostString())).setAsyncSafe();

        /* @doc tag
         *
         * @Name port
         * @RawName <VelocityTag.port>
         * @Object VelocityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the port the proxy listens on, 25577 by default.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "port", (attribute, object) ->
                new ElementTag(proxy().getBoundAddress().getPort())).setAsyncSafe();

        /* @doc tag
         *
         * @Name maxPlayers
         * @RawName <VelocityTag.maxPlayers>
         * @Object VelocityTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the player cap shown in the server list. This is a number the proxy displays,
         * not a limit it enforces, so more players than this can be online.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxPlayers", (attribute, object) ->
                new ElementTag(proxy().getConfiguration().getShowMaxPlayers())).setAsyncSafe();

        /* @doc tag
         *
         * @Name matchPlayer
         * @RawName <VelocityTag.matchPlayer[<name>]>
         * @Object VelocityTag
         * @ReturnType PlayerTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns the online player whose name best matches the input.
         * For example, among 'bo', 'bob' and 'bobby': input 'bob' returns 'bob', input 'bobb'
         * returns 'bobby', and input 'b' returns 'bo'. An exact match always wins.
         * Returns null when nothing matches.
         *
         * @Usage
         * // Resolve a partial name typed by a moderator.
         * - narrate "Best match: <velocity.matchPlayer[bob].name>"
         */
        TAG_PROCESSOR.registerTag(PlayerTag.class, "matchPlayer", (attribute, object) -> {
            if (!attribute.hasParam()) return null;
            String input = attribute.getParam().toLowerCase();
            Player best = null;
            for (Player player : proxy().getAllPlayers()) {
                String name = player.getUsername().toLowerCase();
                if (name.equals(input)) {
                    best = player;
                    break;
                }
                if (name.contains(input) && (best == null || name.length() < best.getUsername().length())) {
                    best = player;
                }
            }
            return best == null ? null : new PlayerTag(best);
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name motd
         * @RawName <VelocityTag.motd>
         * @Object VelocityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the message of the day the proxy shows in the server list, straight from
         * velocity.toml. Backend servers have their own, this is only the proxy's.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "motd", (attribute, object) ->
                new ElementTag(proxy().getConfiguration().getMotd())).setAsyncSafe();

        /* @doc tag
         *
         * @Name isOnlineMode
         * @RawName <VelocityTag.isOnlineMode>
         * @Object VelocityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' if the proxy verifies players against Mojang. When this is false, whether
         * any single player is licensed is up to whatever handles authentication, see
         * <@link tag PlayerTag.isOnlineMode>.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnlineMode", (attribute, object) ->
                new ElementTag(proxy().getConfiguration().isOnlineMode())).setAsyncSafe();

        /* @doc tag
         *
         * @Name tryOrder
         * @RawName <VelocityTag.tryOrder>
         * @Object VelocityTag
         * @ReturnType ListTag(ServerTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the 'try' list from velocity.toml, the servers a joining player is sent to in
         * order until one accepts them. The first entry is the default lobby on a normal setup.
         *
         * A name in that list which is not a registered server is left out, so what comes back is
         * always usable.
         *
         * @Usage
         * // Send someone to the network's default lobby, whatever it is called.
         * - adjust <player> server:<velocity.tryOrder.first>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "tryOrder", (attribute, object) -> {
            ListTag list = new ListTag();
            for (String name : proxy().getConfiguration().getAttemptConnectionOrder()) {
                ServerTag server = new ServerTag(name);
                if (server.isRegistered()) list.addObject(server);
            }
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name supportedVersions
         * @RawName <VelocityTag.supportedVersions>
         * @Object VelocityTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
         * @Description
         * Returns every Minecraft version this build of the proxy can accept, oldest first. Versions
         * sharing one protocol appear once, under the oldest of their names.
         * This is what the proxy would let in, not what any backend actually runs.
         * The ends of the range are just '.first' and '.last'.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "supportedVersions", (attribute, object) -> {
            List<ProtocolVersion> versions = new ArrayList<>(ProtocolVersion.SUPPORTED_VERSIONS);
            versions.sort(Comparator.comparingInt(ProtocolVersion::getProtocol));
            ListTag list = new ListTag();
            for (ProtocolVersion version : versions) {
                list.addString(version.getVersionIntroducedIn());
            }
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name forcedHosts
         * @RawName <VelocityTag.forcedHosts>
         * @Object VelocityTag
         * @ReturnType MapTag
         * @NoArg
         * @Async
         * @Description
         * Returns the forced hosts from velocity.toml as a map of hostname to the backends players
         * joining through that hostname are sent to, in the order the proxy tries them.
         * Hostnames are matched by the proxy without case, and are listed here as written in the
         * config. A name in a forced host list that is not a registered server is left out.
         *
         * The host a player actually used is <@link tag PlayerTag.virtualHost>, so the two together
         * are how a script tells 'joined through play.example.com' from 'joined through the IP'.
         *
         * @Usage
         * // Where does this domain send people?
         * - narrate "<velocity.forcedHosts.get[mc.example.com].parse[name]>"
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "forcedHosts", (attribute, object) -> {
            MapTag map = new MapTag();
            proxy().getConfiguration().getForcedHosts().forEach((host, names) -> {
                ListTag servers = new ListTag();
                for (String name : names) {
                    ServerTag server = new ServerTag(name);
                    if (server.isRegistered()) servers.addObject(server);
                }
                map.putObject(host, servers);
            });
            return map;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name favicon
         * @RawName <VelocityTag.favicon>
         * @Object VelocityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the icon the proxy shows in the server list, as the 'data:image/png;base64,...'
         * URL the protocol sends. Returns null when there is no server-icon.png next to the proxy.
         * This is a long string, do not narrate it at a player.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "favicon", (attribute, object) ->
                proxy().getConfiguration().getFavicon()
                        .map(favicon -> new ElementTag(favicon.getBase64Url()))
                        .orElse(null)).setAsyncSafe().ignoreTest();

        /* @doc tag
         *
         * @Name matchServer
         * @RawName <VelocityTag.matchServer[<name>]>
         * @Object VelocityTag
         * @ReturnType ListTag(ServerTag)
         * @ArgRequired
         * @Async
         * @Description
         * Returns every registered backend whose name starts with the input, ignoring case. An exact
         * name gives a one entry list, an input nothing starts with gives an empty one.
         * Unlike <@link tag VelocityTag.matchPlayer> this hands back all matches rather than picking
         * one, because server names are usually prefixed on purpose ('arena1', 'arena2').
         *
         * @Usage
         * // Count how many arenas are up.
         * - narrate "Arenas: <velocity.matchServer[arena].size>"
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "matchServer", (attribute, object) -> {
            if (!attribute.hasParam()) return null;
            ListTag list = new ListTag();
            proxy().matchServer(attribute.getParam()).forEach(server -> list.addObject(new ServerTag(server)));
            return list;
        }).setAsyncSafe();

        /* @doc mechanism
         *
         * @Name shutdown
         * @Object VelocityTag
         * @Input ElementTag
         * @Description
         * Shuts the proxy down. Every player is disconnected with the given message, or with the
         * proxy's configured shutdown message when no input is given.
         * Backends keep running, this only stops the proxy.
         *
         * @Warning
         * This is not a restart. Nothing brings the proxy back up unless a service manager or a
         * start script is watching it, so on a bare setup the network stays down until someone
         * starts it by hand.
         *
         * @Usage
         * // Stop the proxy with a reason players can read.
         * - adjust <velocity> shutdown:"Network maintenance, back in 20 minutes."
         */
        MECHANISM_PROCESSOR.registerMechanism("shutdown", (object, value) -> {
            String reason = value instanceof ElementTag element ? element.asString() : "";

            if (reason.isBlank() || reason.equalsIgnoreCase("true")) {
                proxy().shutdown();
            }
            else {
                proxy().shutdown(value.asComponent());
            }

            return object;
        });

        /* @doc mechanism
         *
         * @Name serverLinks
         * @Object VelocityTag
         * @Input MapTag
         * @Description
         * Sets the pause menu links on every player currently connected, the same map
         * <@link mechanism PlayerTag.serverLinks> takes on one player.
         *
         * Links live on the connection, not on the proxy, so this reaches exactly the players who
         * are online right now. Anyone joining afterwards has none until a script sets theirs, which
         * makes this the mechanism for a reload, and the per player one for a join.
         *
         * The map is parsed once and a bad entry fails before anybody is touched.
         * Players on a client older than 1.21 are skipped without an error, since on a network that
         * lets old clients in that would be an error per player, every time.
         *
         * @Usage
         * // Give the whole network its links back after a reload.
         * - adjust <velocity> serverLinks:<map[WEBSITE=https://example.com;BUG_REPORT=https://example.com/bugs]>
         */
        MECHANISM_PROCESSOR.registerMechanism("serverLinks", (object, value) -> {
            List<ServerLink> links;
            try {
                links = ServerLinkHelper.parse(value);
            }
            catch (IllegalArgumentException exception) {
                Debugger.error("serverLinks: " + exception.getMessage());
                return object;
            }

            for (Player player : proxy().getAllPlayers()) {
                if (ServerLinkHelper.isSupported(player)) player.setServerLinks(links);
            }

            return object;
        });

        /* @doc mechanism
         *
         * @Name registerServer
         * @Object VelocityTag
         * @Input ServerTag
         * @Description
         * Registers a backend on the running proxy, the same thing an entry in the servers block of
         * velocity.toml does at startup. Takes a blueprint ServerTag, a name with an address behind
         * it: 'server@arena1[address=127.0.0.1:25801]'.
         *
         * The registration lives in memory only. It is gone after a proxy restart, and it is not
         * written into velocity.toml. Registering does not check that anything answers at that
         * address, and a name that is already registered is left alone rather than replaced.
         *
         * @Usage
         * // Bring a match server up, then send the party there.
         * - adjust <velocity> registerServer:<server[arena1[address=127.0.0.1:25801]]>
         * - adjust <player> server:<server[arena1]>
         */
        MECHANISM_PROCESSOR.registerMechanism("registerServer", (object, value) -> {
            ServerTag target = asServer(value);

            if (target.getInfo() == null) {
                Debugger.error("registerServer: '" + value.identify()
                        + "' is not a server. Write it as server@name[address=host:port].");
                return object;
            }

            if (target.isRegistered()) {
                return object;
            }

            proxy().registerServer(target.getInfo());
            return object;
        });

        /* @doc mechanism
         *
         * @Name unregisterServer
         * @Object VelocityTag
         * @Input ServerTag
         * @Description
         * Takes a backend off the running proxy. Players already on it stay there, they are simply
         * not sent to it again, and a server from velocity.toml comes back on the next restart.
         *
         * Pass a registered ServerTag, '<server[arena1]>'. A blueprint works only when its address
         * matches the registration exactly, since the proxy removes servers by name and address
         * together.
         *
         * @Usage
         * // Tear the match server down once it is empty.
         * - if <server[arena1].players.isEmpty>:
         *   - adjust <velocity> unregisterServer:<server[arena1]>
         */
        MECHANISM_PROCESSOR.registerMechanism("unregisterServer", (object, value) -> {
            ServerTag target = asServer(value);

            if (target.getInfo() == null) {
                Debugger.error("unregisterServer: '" + value.identify() + "' is not a server.");
                return object;
            }

            proxy().unregisterServer(target.getInfo());
            return object;
        });
    }

    private static ServerTag asServer(AbstractTag value) {
        return value instanceof ServerTag serverTag ? serverTag : new ServerTag(value.identify());
    }

    private static ProxyServer proxy() {
        return CorexVelocity.getInstance().getServer();
    }

    static File flagsFile() {
        return new File(CorexVelocity.getInstance().getDataFolder().toFile(), "proxyFlags.db");
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return new SqlFlagTracker(flagsFile(), identify());
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<VelocityTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "velocity@";
    }

    @Override
    public @NonNull Adjustable duplicate() {
        return new VelocityTag();
    }

    @Override
    public @NonNull AbstractTag applyMechanism(@NonNull String mechanism, @NonNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NonNull MechanismProcessor<VelocityTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }
}