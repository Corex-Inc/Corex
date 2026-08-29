package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

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
 * @Usage
 * // Report the network population.
 * - narrate "<velocity.players.size> of <velocity.maxPlayers> players online."
 */
public class VelocityTag implements AbstractTag {

    private static final String PREFIX = "velocity";

    public static final TagProcessor<VelocityTag> TAG_PROCESSOR = new TagProcessor<>();

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
         * @ReturnType ListTag
         * @NoArg
         * @Async
         * @Description
         * Returns the 'try' list from velocity.toml, the servers a joining player is sent to in
         * order until one accepts them. The first entry is the default lobby on a normal setup.
         *
         * @Usage
         * // Send someone to the network's default lobby, whatever it is called.
         * - adjust <player> server:<server[<velocity.tryOrder.first>]>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "tryOrder", (attribute, object) -> {
            ListTag list = new ListTag();
            for (String name : proxy().getConfiguration().getAttemptConnectionOrder()) {
                list.addString(name);
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
    }

    private static ProxyServer proxy() {
        return CorexVelocity.getInstance().getServer();
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
}