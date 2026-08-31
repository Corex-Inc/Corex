package dev.corexinc.corex.velocity.environment.tags.player;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.player.SkinParts;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
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
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.engine.utils.Modules;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.core.QueueTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.core.ServerTag;
import dev.corexinc.corex.velocity.environment.utils.ServerLinkHelper;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/* @doc object
 *
 * @Name PlayerTag
 * @Prefix p
 * @Modules VELOCITY
 *
 * @Format
 * The identity format for PlayerTags is 'p@<uuid>'. The base tag also accepts a name,
 * as in '<player[Steve]>', but it always identifies back as the UUID.
 *
 * @Description
 * A PlayerTag is one player connected to the proxy. Fetching an offline player gives null, so a
 * script never holds a PlayerTag for someone who left.
 *
 * This is the proxy's view of a player, which is a much thinner thing than the Paper one: the
 * proxy knows the connection, the client, the profile and which backend the player is on, and
 * nothing at all about health, inventory or position. For those, run the script on the backend.
 *
 * Flags are stored in playerFlags.db next to the proxy config and are shared by every backend,
 * which makes them the simple way to carry data across a server switch.
 *
 * @Usage
 * // Greet a player with where they are.
 * - narrate "Hello <player.name>, you are on <player.server.name>."
 */
public class PlayerTag implements AbstractTag, Adjustable, Flaggable, PlayerIdentity {

    private static final String PREFIX = "p";

    public static final TagProcessor<PlayerTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<PlayerTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private final Player player;

    public PlayerTag(Player player) {
        this.player = player;
    }

    public PlayerTag(UUID uuid) {
        this.player = CorexVelocity.getInstance().getServer().getPlayer(uuid).orElse(null);
    }

    public PlayerTag(String raw) {
        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(2) : raw;
        Player resolved;
        try {
            resolved = CorexVelocity.getInstance().getServer().getPlayer(UUID.fromString(clean)).orElse(null);
        } catch (IllegalArgumentException e) {
            resolved = CorexVelocity.getInstance().getServer().getPlayer(clean).orElse(null);
        }
        this.player = resolved;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("player", (attribute) -> {
            if (attribute.hasParam()) {
                PlayerTag tag = new PlayerTag(attribute.getParam());
                return tag.isOnline() ? tag : null;
            }
            return (AbstractTag) attribute.getQueue().getPlayer();
        });

        ObjectFetcher.registerFetcher(PREFIX, (uuidStr) -> {
            PlayerTag tag = new PlayerTag(UUID.fromString(uuidStr));
            return tag.isOnline() ? tag : null;
        });

        /* @doc tag
         *
         * @Name name
         * @RawName <PlayerTag.name>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the player's username. On an offline mode proxy this is whatever they typed
         * into the launcher, so it is not a stable identity, use the UUID for that.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) ->
                new ElementTag(object.player.getUsername())).setAsyncSafe();

        /* @doc tag
         *
         * @Name uuid
         * @RawName <PlayerTag.uuid>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the player's unique ID. This is the same one the backends see, which is what
         * makes it safe to key data on across a server switch.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) ->
                new ElementTag(object.player.getUniqueId().toString())).setAsyncSafe();

        /* @doc tag
         *
         * @Name isOnline
         * @RawName <PlayerTag.isOnline>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' while the player's connection to the proxy is still live. A PlayerTag
         * held in a definition across a wait can go stale, so check this before acting on one.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnline", (attribute, object) ->
                new ElementTag(object.isOnline())).setAsyncSafe();

        /* @doc tag
         *
         * @Name ping
         * @RawName <PlayerTag.ping>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the player's latency to the proxy in milliseconds, measured on the keepalive
         * round trip. This is the proxy leg only, the backend measures its own and the two do not
         * have to agree. Returns -1 while the ping is unknown, which is the case right after login,
         * before the first round trip has come back.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ping", (attribute, object) ->
                new ElementTag(object.player.getPing())).setAsyncSafe();

        /* @doc tag
         *
         * @Name server
         * @RawName <PlayerTag.server>
         * @Object PlayerTag
         * @ReturnType ServerTag
         * @NoArg
         * @Async
         * @Description
         * Returns the backend the player is on. Returns null while they are between servers, which
         * includes the moment right after login and every switch.
         * For just the name, use '<player.server.name>'.
         */
        TAG_PROCESSOR.registerTag(ServerTag.class, "server", (attribute, object) ->
                object.player.getCurrentServer()
                        .map(connection -> new ServerTag(connection.getServer()))
                        .orElse(null)).setAsyncSafe();

        /* @doc tag
         *
         * @Name address
         * @RawName <PlayerTag.address>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the player's IP address, without the port.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "address", (attribute, object) ->
                new ElementTag(object.player.getRemoteAddress().getAddress().getHostAddress())).setAsyncSafe();

        /* @doc tag
         *
         * @Name hasPermission
         * @RawName <PlayerTag.hasPermission[<permission>]>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Async
         * @Description
         * Returns 'true' if the player has the given permission on the proxy. Proxy permissions
         * are a separate set from the backends', a permission plugin on the lobby says nothing
         * about this unless it also runs here.
         *
         * @Usage
         * // Gate a network wide announcement.
         * - if <player.hasPermission[network.announce]>:
         *   - narrate <[message]> targets:<proxy.players>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasPermission", (attribute, object) -> {
            if (!attribute.hasParam()) return null;
            return new ElementTag(object.player.hasPermission(attribute.getParam()));
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name protocolVersion
         * @RawName <PlayerTag.protocolVersion>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the raw protocol number of the player's client, for example 773 for 1.21.11.
         * Use it to compare clients, the numbers only ever go up. For something to show a human,
         * use <@link tag PlayerTag.clientVersion> instead.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "protocolVersion", (attribute, object) ->
                new ElementTag(object.player.getProtocolVersion().getProtocol())).setAsyncSafe();

        /* @doc tag
         *
         * @Name clientVersion
         * @RawName <PlayerTag.clientVersion>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the readable version name of the player's client, for example '1.21.11'.
         * Several Minecraft versions can share one protocol number, and the protocol is all the
         * client tells us, so this returns the oldest of them. A player on 1.21.11 reads as
         * '1.21.9', because those two are the same protocol and nothing can tell them apart.
         * Compare clients with <@link tag PlayerTag.protocolVersion> instead, this is for showing.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "clientVersion", (attribute, object) ->
                new ElementTag(object.player.getProtocolVersion().getVersionIntroducedIn())).setAsyncSafe();

        /* @doc tag
         *
         * @Name clientBrand
         * @RawName <PlayerTag.clientBrand>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the brand the client reports, 'vanilla' for the normal game, 'fabric' or 'forge'
         * for modded ones. The brand arrives as a plugin message a moment after login, so this is
         * null until it lands, and stays null for a client that never sends one.
         * A client can put anything it likes in here, so do not gate anything important on it.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "clientBrand", (attribute, object) -> {
            String brand = object.player.getClientBrand();
            return brand == null ? null : new ElementTag(brand);
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name locale
         * @RawName <PlayerTag.locale>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the language the player picked in their client, like 'en_us' or 'ru_ru'.
         * This is what translatable text is rendered in when sent to them.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "locale", (attribute, object) -> {
            Locale locale = object.player.getEffectiveLocale();
            return locale == null ? null : new ElementTag(locale.toString().toLowerCase(Locale.ROOT));
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name virtualHost
         * @RawName <PlayerTag.virtualHost>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the hostname the player typed into their client to reach the proxy, without the
         * port. This is what forced-hosts route on, so 'pvp.example.com' and 'mc.example.com' can
         * be told apart even though both land here. Returns null for clients that sent nothing.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "virtualHost", (attribute, object) ->
                object.player.getVirtualHost()
                        .map(address -> new ElementTag(address.getHostString()))
                        .orElse(null)).setAsyncSafe();

        /* @doc tag
         *
         * @Name isOnlineMode
         * @RawName <PlayerTag.isOnlineMode>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' if this player was verified against Mojang, that is, a licensed account.
         * Only meaningful on a proxy running in offline mode next to something that lets both
         * kinds in, since in online mode every player is verified.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnlineMode", (attribute, object) ->
                new ElementTag(object.player.isOnlineMode())).setAsyncSafe();

        /* @doc tag
         *
         * @Name protocolState
         * @RawName <PlayerTag.protocolState>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the protocol phase the connection is in: HANDSHAKE, STATUS, LOGIN, CONFIGURATION
         * or PLAY. A player in the game is in PLAY, except during the moment they are being moved
         * to another backend, where they pass through CONFIGURATION.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "protocolState", (attribute, object) ->
                new ElementTag(object.player.getProtocolState().name())).setAsyncSafe();

        /* @doc tag
         *
         * @Name previousServer
         * @RawName <PlayerTag.previousServer>
         * @Object PlayerTag
         * @ReturnType ServerTag
         * @NoArg
         * @Async
         * @Description
         * Returns the backend server the player was on before the current one, or null if this is
         * the first server they joined. Handy for sending someone back where they came from.
         */
        TAG_PROCESSOR.registerTag(ServerTag.class, "previousServer", (attribute, object) ->
                object.player.getCurrentServer()
                        .flatMap(ServerConnection::getPreviousServer)
                        .map(ServerTag::new)
                        .orElse(null)).setAsyncSafe();

        /* @doc tag
         *
         * @Name hasSentOptions
         * @RawName <PlayerTag.hasSentOptions>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' once the client has sent its settings packet. The packet arrives a moment
         * after login, and until it does <@link tag PlayerTag.options> reports defaults rather than
         * what the player actually chose. Check this before trusting those values on join.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasSentOptions", (attribute, object) ->
                new ElementTag(object.player.hasSentPlayerSettings())).setAsyncSafe();

        /* @doc tag
         *
         * @Name options
         * @RawName <PlayerTag.options>
         * @Object PlayerTag
         * @ReturnType MapTag
         * @NoArg
         * @Async
         * @Warning
         * Until the client sends its settings packet, shortly after login, this returns protocol
         * defaults instead of the player's real choices. Guard reads on join with
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns every client setting the player sent, as a map with the keys 'viewDistance',
         * 'chatMode', 'mainHand', 'hasChatColors', 'clientListing', 'textFiltering' and
         * 'particleStatus'. Each one is also available as its own tag.
         *
         * @Usage
         * // Warn players rendering almost nothing.
         * - if <player.options.get[viewDistance]> < 4:
         *   - narrate "Your render distance is very low, the arena may not load in."
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "options", (attribute, object) -> {
            PlayerSettings settings = object.player.getPlayerSettings();
            MapTag map = new MapTag();
            map.putObject("viewDistance", new ElementTag(settings.getViewDistance()));
            map.putObject("chatMode", new ElementTag(settings.getChatMode().name()));
            map.putObject("mainHand", new ElementTag(settings.getMainHand().name()));
            map.putObject("hasChatColors", new ElementTag(settings.hasChatColors()));
            map.putObject("clientListing", new ElementTag(settings.isClientListingAllowed()));
            map.putObject("textFiltering", new ElementTag(settings.isTextFilteringEnabled()));
            map.putObject("particleStatus", new ElementTag(settings.getParticleStatus().name()));
            return map;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name viewDistance
         * @RawName <PlayerTag.viewDistance>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns the render distance in chunks the player set in their video options.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "viewDistance", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().getViewDistance())).setAsyncSafe();

        /* @doc tag
         *
         * @Name chatMode
         * @RawName <PlayerTag.chatMode>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns how the player set their chat: SHOWN, COMMANDS_ONLY or HIDDEN.
         * A player on HIDDEN will never see anything you narrate to them.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "chatMode", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().getChatMode().name())).setAsyncSafe();

        /* @doc tag
         *
         * @Name mainHand
         * @RawName <PlayerTag.mainHand>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns the player's main hand, LEFT or RIGHT.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mainHand", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().getMainHand().name())).setAsyncSafe();

        /* @doc tag
         *
         * @Name hasChatColors
         * @RawName <PlayerTag.hasChatColors>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns 'true' if the player left chat colors enabled in their settings.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasChatColors", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().hasChatColors())).setAsyncSafe();

        /* @doc tag
         *
         * @Name clientListing
         * @RawName <PlayerTag.clientListing>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns 'true' if the player allows other players to see them in the tab list.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "clientListing", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().isClientListingAllowed())).setAsyncSafe();

        /* @doc tag
         *
         * @Name textFiltering
         * @RawName <PlayerTag.textFiltering>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns 'true' if the client has profanity filtering turned on, which is the default for
         * accounts under parental controls.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "textFiltering", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().isTextFilteringEnabled())).setAsyncSafe();

        /* @doc tag
         *
         * @Name particleStatus
         * @RawName <PlayerTag.particleStatus>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns the player's particle setting: ALL, DECREASED or MINIMAL.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "particleStatus", (attribute, object) ->
                new ElementTag(object.player.getPlayerSettings().getParticleStatus().name())).setAsyncSafe();

        /* @doc tag
         *
         * @Name skinParts
         * @RawName <PlayerTag.skinParts>
         * @Object PlayerTag
         * @ReturnType MapTag
         * @NoArg
         * @Async
         * @Warning
         * Reports a protocol default until the client sends its settings packet, see
         * <@link tag PlayerTag.hasSentOptions>.
         * @Description
         * Returns which parts of their skin the player has switched on, as a map of booleans with
         * the keys 'cape', 'jacket', 'leftSleeve', 'rightSleeve', 'leftPants', 'rightPants' and
         * 'hat'.
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "skinParts", (attribute, object) -> {
            SkinParts parts = object.player.getPlayerSettings().getSkinParts();
            MapTag map = new MapTag();
            map.putObject("cape", new ElementTag(parts.hasCape()));
            map.putObject("jacket", new ElementTag(parts.hasJacket()));
            map.putObject("leftSleeve", new ElementTag(parts.hasLeftSleeve()));
            map.putObject("rightSleeve", new ElementTag(parts.hasRightSleeve()));
            map.putObject("leftPants", new ElementTag(parts.hasLeftPants()));
            map.putObject("rightPants", new ElementTag(parts.hasRightPants()));
            map.putObject("hat", new ElementTag(parts.hasHat()));
            return map;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name skinBlob
         * @RawName <PlayerTag.skinBlob>
         * @Object PlayerTag
         * @ReturnType MapTag
         * @NoArg
         * @Async
         * @Description
         * Returns the player's skin as a map with the keys 'value' and 'signature', both base64
         * strings handed out by Mojang. You need both to put this skin on anything else, a client
         * rejects the texture if the signature is missing or does not match.
         * Returns null for players with no texture on their profile, which is normal for offline
         * mode accounts.
         *
         * @Usage
         * // Save a player's skin to a server flag so it can be reused later.
         * - flag server skins.<player.name>:<player.skinBlob>
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "skinBlob", (attribute, object) -> {
            for (GameProfile.Property property : object.player.getGameProfileProperties()) {
                if (!property.getName().equals("textures")) continue;
                MapTag map = new MapTag();
                map.putObject("value", new ElementTag(property.getValue()));
                if (property.getSignature() != null) {
                    map.putObject("signature", new ElementTag(property.getSignature()));
                }
                return map;
            }
            return null;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name modInfo
         * @RawName <PlayerTag.modInfo>
         * @Object PlayerTag
         * @ReturnType MapTag
         * @NoArg
         * @Async
         * @Description
         * Returns the mod list a Forge client sends during the handshake, as a map with 'type'
         * (the Forge handshake flavour, such as 'FML2') and 'mods' (a map of mod id to version).
         * Returns null for vanilla clients and for Fabric, which does not announce its mods.
         *
         * @Usage
         * // Turn away clients carrying a known cheat mod.
         * - if <player.modInfo.get[mods].contains[xray]>:
         *   - kick <player> "Remove your x-ray mod."
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "modInfo", (attribute, object) ->
                object.player.getModInfo().map(info -> {
                    MapTag map = new MapTag();
                    map.putObject("type", new ElementTag(info.getType()));
                    MapTag mods = new MapTag();
                    for (ModInfo.Mod mod : info.getMods()) {
                        mods.putObject(mod.getId(), new ElementTag(mod.getVersion()));
                    }
                    map.putObject("mods", mods);
                    return map;
                }).orElse(null)).setAsyncSafe();

        /* @doc tag
         *
         * @Name resourcePacks
         * @RawName <PlayerTag.resourcePacks>
         * @Object PlayerTag
         * @ReturnType MapTag
         * @NoArg
         * @Async
         * @Description
         * Returns the player's resource packs as a map with 'pending' and 'applied', each a list of
         * pack URLs. Pending ones have been offered and not answered yet, applied ones the client
         * has loaded. Both lists can be empty.
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "resourcePacks", (attribute, object) -> {
            ListTag pending = new ListTag();
            for (ResourcePackInfo info : object.player.getPendingResourcePacks()) {
                pending.addString(info.getUrl());
            }
            ListTag applied = new ListTag();
            for (ResourcePackInfo info : object.player.getAppliedResourcePacks()) {
                applied.addString(info.getUrl());
            }
            MapTag map = new MapTag();
            map.putObject("pending", pending);
            map.putObject("applied", applied);
            return map;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name tabListInfo
         * @RawName <PlayerTag.tabListInfo>
         * @Object PlayerTag
         * @ReturnType ListTag
         * @NoArg
         * @Async
         * @Description
         * Returns the header and footer currently shown around the player's tab list, as a list of
         * two entries in that order. Set them back with the mechanism of the same name.
         * A backend server usually replaces both when the player switches to it, so anything the
         * proxy sets has to be set again after the switch.
         *
         * @Usage
         * // Copy one player's tab list header and footer onto another.
         * - adjust <player[Steve]> tabListInfo:<player[Alex].tabListInfo>
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "tabListInfo", (attribute, object) -> {
            ListTag list = new ListTag();
            list.addObject(new ElementTag(object.player.getPlayerListHeader()));
            list.addObject(new ElementTag(object.player.getPlayerListFooter()));
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name tabEntries
         * @RawName <PlayerTag.tabEntries>
         * @Object PlayerTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the players currently listed in this player's tab list. The proxy tracks these
         * because it has to rewrite them whenever the player moves between backends, so this is
         * what the client is really showing, not what any one backend thinks.
         *
         * Entries that are not real connected players are left out: a plugin can push decorative
         * rows into a tab list, and those have a profile but nobody behind it.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "tabEntries", (attribute, object) -> {
            ListTag list = new ListTag();
            for (TabListEntry entry : object.player.getTabList().getEntries()) {
                PlayerTag listed = new PlayerTag(entry.getProfile().getId());
                if (listed.isOnline()) list.addObject(listed);
            }
            return list;
        }).setAsyncSafe();

        /* @doc mechanism
         *
         * @Name server
         * @Object PlayerTag
         * @Input ServerTag
         * @Description
         * Sends the player to the given backend server. Nothing happens if the server is not
         * registered on the proxy. The switch is fired off and not waited on, so the next command
         * runs immediately, before the player has actually landed.
         *
         * @Usage
         * // Send the player back to the lobby.
         * - adjust <player> server:<server[lobby]>
         */
        MECHANISM_PROCESSOR.registerMechanism("server", (object, value) -> {
            if (object.player == null) return object;
            ServerTag target = value instanceof ServerTag tag ? tag : new ServerTag(value.identify());
            if (target.isRegistered()) {
                object.player.createConnectionRequest(target.getServer()).fireAndForget();
            }
            return object;
        });

        /* @doc mechanism
         *
         * @Name transferToHost
         * @Object PlayerTag
         * @Input ElementTag
         * @Description
         * Hands the player over to another proxy or server entirely, given as 'host:port', or as just
         * the host for the default 25565. The client does the reconnecting itself, so this works
         * across networks, unlike <@link mechanism PlayerTag.server> which only moves a player
         * between this proxy's own backends.
         *
         * Needs a 1.20.5 or newer client. On anything older nothing happens and the script gets an
         * error, so check <@link tag PlayerTag.protocolVersion> first if your network lets old
         * clients in.
         *
         * The player leaves this proxy the moment the packet lands. Anything the script wanted to
         * tell them has to be said before this, not after.
         *
         * @Usage
         * // Move someone to the event network.
         * - adjust <player> transferToHost:events.example.com
         */
        MECHANISM_PROCESSOR.registerMechanism("transferToHost", (object, value) -> {
            if (object.player == null || !(value instanceof ElementTag element)) return object;

            InetSocketAddress target = ServerTag.parseAddress(element.asString());
            if (target == null) {
                Debugger.error("transferToHost: '" + element.asString() + "' is not a usable address, write it as host or host:port.");
                return object;
            }

            if (object.player.getProtocolVersion().getProtocol() < ProtocolVersion.MINECRAFT_1_20_5.getProtocol()) {
                Debugger.error("transferToHost: " + object.player.getUsername()
                        + " is on " + object.player.getProtocolVersion().getVersionIntroducedIn()
                        + ", transfers need a 1.20.5 or newer client.");
                return object;
            }

            object.player.transferToHost(target);
            return object;
        });

        /* @doc mechanism
         *
         * @Name serverLinks
         * @Object PlayerTag
         * @Input MapTag
         * @Description
         * Sets the links the client lists in its pause menu, as a map of label to URL. The order of
         * the map is the order of the menu.
         *
         * A key matching one of the ten built in names is sent as that type, which means the client
         * writes the label itself in the player's own language: 'BUG_REPORT', 'COMMUNITY_GUIDELINES',
         * 'SUPPORT', 'STATUS', 'FEEDBACK', 'COMMUNITY', 'WEBSITE', 'FORUMS', 'NEWS',
         * 'ANNOUNCEMENTS'. Prefer 'BUG_REPORT' where it fits, it is the only one Minecraft also shows
         * on the disconnect screen after a kick.
         *
         * Any other key is sent as a custom label and goes through the same component path as
         * narrate, so colours and gradients work. The match is case sensitive, so 'WEBSITE'
         * is the built in type while 'Website' is a custom label reading Website.
         *
         * URLs must be http or https. One bad entry fails the whole map and nothing is sent, because
         * half a menu is harder to notice than a menu that never changed.
         * An empty map clears the links.
         *
         * Needs a 1.21 or newer client, the proxy refuses to send them to anything older, and the
         * script gets an error saying so.
         * Links do not survive a disconnect, so they have to be set again on every join.
         *
         * A backend can send its own links afterwards and they win, so on a network running Corex
         * on both sides, decide on one place to set them.
         *
         * @Usage
         * // Two built in entries and one of your own.
         * - adjust <player> serverLinks:<map[WEBSITE=https://example.com;BUG_REPORT=https://example.com/bugs;<#5865F2>Discord=https://dsc.gg/corexinc]>
         *
         * @Usage
         * // Take them away again.
         * - adjust <player> serverLinks:<map[]>
         */
        MECHANISM_PROCESSOR.registerMechanism("serverLinks", (object, value) -> {
            if (object.player == null) return object;

            List<ServerLink> links;
            try {
                links = ServerLinkHelper.parse(value);
            }
            catch (IllegalArgumentException exception) {
                Debugger.error("serverLinks: " + exception.getMessage());
                return object;
            }

            if (!ServerLinkHelper.isSupported(object.player)) {
                Debugger.error("serverLinks: " + object.player.getUsername()
                        + " is on " + object.player.getProtocolVersion().getVersionIntroducedIn()
                        + ", server links need a 1.21 or newer client.");
                return object;
            }

            object.player.setServerLinks(links);
            return object;
        });

        /* @doc mechanism
         *
         * @Name tabListInfo
         * @Object PlayerTag
         * @Input ListTag
         * @Description
         * Sets the header and footer around the player's tab list from a list of two entries.
         * A list with one entry sets the header and empties the footer. An empty list clears both.
         *
         * @Usage
         * // Put a two line banner on the tab list.
         * - adjust <player> tabListInfo:<list[<&c>Example Network|<&7>You are on <player.server.name>]>
         *
         * @Usage
         * // Clear it again.
         * - adjust <player> tabListInfo:<list[]>
         */
        MECHANISM_PROCESSOR.registerMechanism("tabListInfo", (object, value) -> {
            if (object.player == null) return object;
            ListTag list = value instanceof ListTag tag ? tag : new ListTag(value.identify());
            if (list.isEmpty()) {
                object.player.clearPlayerListHeaderAndFooter();
                return object;
            }
            List<AbstractTag> entries = list.getList();
            Component header = entries.get(0).asComponent();
            Component footer = entries.size() > 1 ? entries.get(1).asComponent() : Component.empty();
            object.player.sendPlayerListHeaderAndFooter(header, footer);
            return object;
        });

        /* @doc mechanism
         *
         * @Name tabCompletions
         * @Object PlayerTag
         * @Input ListTag
         * @Description
         * Replaces the player's custom chat completions, the words their client offers on tab
         * alongside player names while typing in chat. An empty list removes them all.
         * This is chat only, it does not touch command suggestions.
         *
         * @Usage
         * // Offer the warp names while the player types.
         * - adjust <player> tabCompletions:<list[spawn|shop|arena]>
         */
        MECHANISM_PROCESSOR.registerMechanism("tabCompletions", (object, value) -> {
            if (object.player == null) return object;
            ListTag list = value instanceof ListTag tag ? tag : new ListTag(value.identify());
            List<String> completions = new ArrayList<>(list.size());
            for (AbstractTag entry : list.getList()) {
                completions.add(entry.identify());
            }
            object.player.setCustomChatCompletions(completions);
            return object;
        });

        /* @doc tag
         *
         * @Name player
         * @RawName <QueueTag.player>
         * @Object QueueTag
         * @ReturnType PlayerTag
         * @NoArg
         * @Async
         * @Modules VELOCITY
         * @Description
         * Returns a linked player of the queue, as a proxy PlayerTag.
         * The Paper plugin registers its own version of this tag, returning a server PlayerTag.
         */
        QueueTag.TAG_PROCESSOR.registerTag(PlayerTag.class, "player", (attribute, object) ->
                        ((PlayerTag) object.getQueue().getPlayer()))
                .ignoreTest().setAsyncSafe().setAvailableFor(Modules.VELOCITY);
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public boolean isOnline() {
        return player != null && player.isActive();
    }

    @Override
    public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + player.getUniqueId();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<PlayerTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NotNull Adjustable duplicate() { return this; }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NonNull MechanismProcessor<? extends AbstractTag> getMechanismProcessor() { return MECHANISM_PROCESSOR; }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        if (player == null) return null;
        File dbFile = new File(CorexVelocity.getInstance().getDataFolder().toFile(), "playerFlags.db");
        return new SqlFlagTracker(dbFile, player.getUniqueId().toString());
    }

    @Override
    public @NonNull String getTestValue() { return "p@465876c1-2a15-4fc0-9f0b-97de13aa46f1"; }
}