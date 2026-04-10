package dev.corexinc.corex.environment.tags.player;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.UUID;

/* @doc object
 *
 * @Name PlayerTag
 * @Prefix p
 * @Format
 * The identity format for players is the UUID of the relevant player.
 *
 * @Description
 * A PlayerTag represents a player in the game.
 */
public class PlayerTag implements AbstractTag {

    private static final String prefix = "p";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final OfflinePlayer offlinePlayer;

    public static final TagProcessor<PlayerTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<PlayerTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("player", (attribute) -> {
            if (attribute.hasParam()) {
                return new PlayerTag(attribute.getParam());
            }
            if (attribute.getQueue() != null && attribute.getQueue().getPlayer() != null) {
                return attribute.getQueue().getPlayer();
            }
            return null;
        });

        ObjectFetcher.registerFetcher(prefix, (uuidStr) -> new PlayerTag(UUID.fromString(uuidStr)));

        /* @doc tag
         *
         * @Name name
         * @RawName <PlayerTag.name>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the name of the player.
         *
         * @Implements EntityTag.name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
            String name = object.offlinePlayer.getName();
            return new ElementTag(name != null ? name : "Unknown");
        });

        /* @doc tag
         *
         * @Name isOnline
         * @RawName <PlayerTag.isOnline>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the player is currently online.
         * Works with offline players (returns false in that case).
         *
         * @Implements PlayerTag.is_online
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnline", (attribute, object) ->
                new ElementTag(String.valueOf(object.offlinePlayer.isOnline())));

        /* @doc tag
         *
         * @Name uuid
         * @RawName <PlayerTag.uuid>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the permanent unique ID of the player.
         *
         * @Implements EntityTag.uuid
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) ->
                new ElementTag(object.offlinePlayer.getUniqueId().toString()));

        /* @doc tag
         *
         * @Name location
         * @RawName <PlayerTag.location>
         * @Object PlayerTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * For players, this is at the center of their feet.
         *
         * @Implements EntityTag.location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "location", (attribute, object) ->
                new LocationTag(object.getPlayer().getLocation()));

        /* @doc tag
         *
         * @Name health
         * @RawName <PlayerTag.health>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.health
         * @NoArg
         * @Description
         * Returns the current health of the entity.
         *
         * @Implements EntityTag.health
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "health", ((attribute, object) ->
                new ElementTag(object.getPlayer().getHealth())));

        /* @doc tag
         *
         * @Name food
         * @RawName <PlayerTag.food>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @Mechanism PlayerTag.food
         * @NoArg
         * @Description
         * Returns the current food level (aka hunger) of the player.
         *
         * @Implements PlayerTag.food_level
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "food", ((attribute, object) ->
                new ElementTag(object.getPlayer().getFoodLevel())));

        /* @doc tag
         *
         * @Name target
         * @RawName <PlayerTag.target[(<matcher>)]>
         * @Object PlayerTag
         * @ReturnType EntityTag, PlayerTag
         * @NoArg
         * @Description
         * Returns the entity that the player is looking at, within a maximum range of 50 blocks,
         * or null if the player is not looking at an entity.
         * Optionally, specify an entity type matcher to only count matches as possible targets.
         *
         * @Implements PlayerTag.target
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "target", ((attribute, object) -> {
            Entity entity = object.getPlayer().getTargetEntity(50, false);
            if (entity == null) return null;
            return new EntityTag(entity);
        })).ignoreTest();

        /* @doc tag
         *
         * @Name itemInHand
         * @RawName <PlayerTag.itemInHand>
         * @Object PlayerTag
         * @ReturnType ItemTag
         * @NoArg
         * @Description
         * Returns the item the player is holding, or air if none.
         *
         * @Implements EntityTag.item_in_hand
         */
        TAG_PROCESSOR.registerTag(ItemTag.class, "itemInHand", ((attribute, object) ->
                new ItemTag(object.getPlayer().getInventory().getItemInMainHand())));

        /* @doc tag
         *
         * @Name maxHealth
         * @RawName <PlayerTag.maxHealth>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.maxHealth
         * @NoArg
         * @Description
         * Returns the maximum health of the entity.
         *
         * @Implements EntityTag.health_max
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxHealth", ((attribute, object) ->
                new ElementTag(Objects.requireNonNull(object.getPlayer()
                        .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()))).ignoreTest();

        /* @doc tag
         *
         * @Name isBanned
         * @RawName <PlayerTag.isBanned>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether this player's profile is banned from the server.
         * Works with offline players.
         *
         * @Implements PlayerTag.is_banned
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isBanned", (attribute, object) ->
                new ElementTag(object.offlinePlayer.isBanned()));

        /* @doc tag
         *
         * @Name isWhitelisted
         * @RawName <PlayerTag.isWhitelisted>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @Mechanism PlayerTag.whitelisted
         * @NoArg
         * @Description
         * Returns whether the player is on the server whitelist.
         * Works with offline players.
         *
         * @Implements PlayerTag.is_whitelisted
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isWhitelisted", (attribute, object) ->
                new ElementTag(object.offlinePlayer.isWhitelisted()));

        /* @doc tag
         *
         * @Name isOp
         * @RawName <PlayerTag.isOp>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @Mechanism PlayerTag.op
         * @NoArg
         * @Description
         * Returns whether this player has operator (OP) status on the server.
         * Works with offline players.
         *
         * @Implements PlayerTag.is_op
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isOp", (attribute, object) ->
                new ElementTag(object.offlinePlayer.isOp()));

        /* @doc tag
         *
         * @Name hasPlayedBefore
         * @RawName <PlayerTag.hasPlayedBefore>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether this player has ever joined this server before.
         * Works with offline players.
         *
         * @Implements PlayerTag.has_played_before
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "hasPlayedBefore", (attribute, object) ->
                new ElementTag(object.offlinePlayer.hasPlayedBefore()));

        /* @doc tag
         *
         * @Name firstPlayed
         * @RawName <PlayerTag.firstPlayed>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the Unix timestamp (in milliseconds) of the first time this player
         * joined the server, or 0 if they have never played.
         * Works with offline players.
         *
         * @Implements PlayerTag.first_played
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "firstPlayed", (attribute, object) ->
                new ElementTag(object.offlinePlayer.getFirstPlayed()));

        /* @doc tag
         *
         * @Name lastLogin
         * @RawName <PlayerTag.lastLogin>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the Unix timestamp (in milliseconds) of the last time this player
         * logged into the server, or 0 if they have never played.
         * Works with offline players.
         *
         * @Implements PlayerTag.last_login
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "lastLogin", (attribute, object) ->
                new ElementTag(object.offlinePlayer.getLastLogin()));

        /* @doc tag
         *
         * @Name lastSeen
         * @RawName <PlayerTag.lastSeen>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the Unix timestamp (in milliseconds) of the last time this player
         * was seen on the server. If the player is currently online, returns the
         * current time. Returns 0 if they have never played.
         * Works with offline players.
         *
         * @Implements PlayerTag.last_seen
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "lastSeen", (attribute, object) ->
                new ElementTag(object.offlinePlayer.getLastSeen()));

        /* @doc tag
         *
         * @Name lastDeathLocation
         * @RawName <PlayerTag.lastDeathLocation>
         * @Object PlayerTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the location where this player last died, or null if unavailable.
         * Works with offline players.
         *
         * @Implements PlayerTag.last_death_location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "lastDeathLocation", (attribute, object) -> {
            try {
                org.bukkit.Location loc = object.offlinePlayer.getLastDeathLocation();
                if (loc == null || loc.getWorld() == null) return null;
                return new LocationTag(loc);
            } catch (Exception e) {
                return null;
            }
        }).ignoreTest();

        /* @doc tag
         *
         * @Name respawnLocation
         * @RawName <PlayerTag.respawnLocation>
         * @Object PlayerTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the location where this player will respawn (bed or respawn anchor),
         * or null if they have no valid respawn point.
         * Works with offline players.
         *
         * @Implements PlayerTag.respawn_location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "respawnLocation", (attribute, object) -> {
            org.bukkit.Location loc = object.offlinePlayer.getRespawnLocation();
            return loc != null ? new LocationTag(loc) : null;
        }).ignoreTest();

        /* @doc tag
         *
         * @Name statistic[<name>]
         * @RawName <PlayerTag.statistic[<name>]>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the value of the given statistic for this player.
         * Input must be a valid Bukkit Statistic enum name, e.g. PLAY_ONE_MINUTE, DEATHS, JUMP.
         * Works with offline players.
         *
         * @Implements PlayerTag.statistic
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "statistic", (attribute, object) -> {
            if (!attribute.hasParam()) return null;
            try {
                org.bukkit.Statistic stat = org.bukkit.Statistic.valueOf(attribute.getParam().toUpperCase());
                return new ElementTag(object.offlinePlayer.getStatistic(stat));
            } catch (IllegalArgumentException e) {
                Debugger.error("Invalid statistic name: '" + attribute.getParam() + "'");
                return null;
            }
        }).ignoreTest();

        /* @doc tag
         *
         * @Name gameMode
         * @RawName <PlayerTag.gameMode>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @Mechanism PlayerTag.gameMode
         * @NoArg
         * @Description
         * Returns the current game mode of the player.
         * Possible values: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR.
         *
         * @Implements PlayerTag.gamemode
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "gameMode", (attribute, object) ->
                new ElementTag(object.getPlayer().getGameMode().name()));

        /* @doc tag
         *
         * @Name previousGameMode
         * @RawName <PlayerTag.previousGameMode>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the previous game mode of the player, or null if there is none.
         * Possible values: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR.
         *
         * @Implements PlayerTag.previous_gamemode
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "previousGameMode", (attribute, object) -> {
            GameMode prev = object.getPlayer().getPreviousGameMode();
            return prev != null ? new ElementTag(prev.name()) : null;
        });

        /* @doc tag
         *
         * @Name level
         * @RawName <PlayerTag.level>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Mechanism PlayerTag.level
         * @Description
         * Returns the current XP level of the player.
         *
         * @Implements PlayerTag.xp_level
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "level", (attribute, object) ->
                new ElementTag(object.getPlayer().getLevel()));

        /* @doc tag
         *
         * @Name exp
         * @RawName <PlayerTag.exp>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Mechanism PlayerTag.exp
         * @Description
         * Returns the player's progress towards the next XP level as a decimal between 0.0 and 1.0.
         *
         * @Implements PlayerTag.xp_to_next_level
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "exp", (attribute, object) ->
                new ElementTag(object.getPlayer().getExp()));

        /* @doc tag
         *
         * @Name totalExperience
         * @RawName <PlayerTag.totalExperience>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @Mechanism PlayerTag.totalExperience
         * @NoArg
         * @Description
         * Returns the total amount of experience points this player has collected.
         *
         * @Implements PlayerTag.total_xp
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "totalExperience", (attribute, object) ->
                new ElementTag(object.getPlayer().getTotalExperience()));

        /* @doc tag
         *
         * @Name isFlying
         * @RawName <PlayerTag.isFlying>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @Mechanism PlayerTag.flying
         * @NoArg
         * @Description
         * Returns whether the player is currently flying.
         *
         * @Implements PlayerTag.is_flying
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isFlying", (attribute, object) ->
                new ElementTag(object.getPlayer().isFlying()));

        /* @doc tag
         *
         * @Name isSneaking
         * @RawName <PlayerTag.isSneaking>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the player is currently sneaking (holding shift).
         *
         * @Implements PlayerTag.is_sneaking
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isSneaking", (attribute, object) ->
                new ElementTag(object.getPlayer().isSneaking()));

        /* @doc tag
         *
         * @Name isSprinting
         * @RawName <PlayerTag.isSprinting>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the player is currently sprinting.
         *
         * @Implements PlayerTag.is_sprinting
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isSprinting", (attribute, object) ->
                new ElementTag(object.getPlayer().isSprinting()));

        /* @doc tag
         *
         * @Name allowFlight
         * @RawName <PlayerTag.allowFlight>
         * @Object PlayerTag
         * @ReturnType ElementTag(Boolean)
         * @Mechanism PlayerTag.allowFlight
         * @NoArg
         * @Description
         * Returns whether this player is allowed to fly (like in creative mode).
         *
         * @Implements PlayerTag.can_fly
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "allowFlight", (attribute, object) ->
                new ElementTag(object.getPlayer().getAllowFlight()));

        /* @doc tag
         *
         * @Name flySpeed
         * @RawName <PlayerTag.flySpeed>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.flySpeed
         * @NoArg
         * @Description
         * Returns the current fly speed of the player as a decimal.
         * The default value is 0.1, and the range is -1.0 to 1.0.
         *
         * @Implements PlayerTag.fly_speed
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "flySpeed", (attribute, object) ->
                new ElementTag(object.getPlayer().getFlySpeed())).ignoreTest();

        /* @doc tag
         *
         * @Name walkSpeed
         * @RawName <PlayerTag.walkSpeed>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.walkSpeed
         * @NoArg
         * @Description
         * Returns the current walk speed of the player as a decimal.
         * The default value is 0.2, and the range is -1.0 to 1.0.
         *
         * @Implements PlayerTag.walk_speed
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "walkSpeed", (attribute, object) ->
                new ElementTag(object.getPlayer().getWalkSpeed()));

        /* @doc tag
         *
         * @Name ping
         * @RawName <PlayerTag.ping>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the player's estimated ping in milliseconds.
         *
         * @Implements PlayerTag.ping
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ping", (attribute, object) ->
                new ElementTag(object.getPlayer().getPing()));

        /* @doc tag
         *
         * @Name displayName
         * @RawName <PlayerTag.displayName>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @Mechanism PlayerTag.displayName
         * @NoArg
         * @Description
         * Returns the display name of the player as a MiniMessage string.
         * This is the "friendly" name shown in chat, which may include formatting.
         *
         * @Implements PlayerTag.display_name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "displayName", (attribute, object) ->
                new ElementTag(MINI_MESSAGE.serialize(object.getPlayer().displayName())));

        /* @doc tag
         *
         * @Name saturation
         * @RawName <PlayerTag.saturation>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.saturation
         * @NoArg
         * @Description
         * Returns the current food saturation of the player.
         * Saturation acts as a buffer for food level — it drains before the food bar does.
         *
         * @Implements PlayerTag.saturation
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "saturation", (attribute, object) ->
                new ElementTag(object.getPlayer().getSaturation()));

        /* @doc tag
         *
         * @Name exhaustion
         * @RawName <PlayerTag.exhaustion>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.exhaustion
         * @NoArg
         * @Description
         * Returns the current exhaustion level of the player.
         * Exhaustion controls how quickly the saturation and food bars deplete.
         *
         * @Implements PlayerTag.exhaustion
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "exhaustion", (attribute, object) ->
                new ElementTag(object.getPlayer().getExhaustion())).ignoreTest();

        /* @doc tag
         *
         * @Name locale
         * @RawName <PlayerTag.locale>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the locale (language) set in the player's client settings, e.g. "en_us" or "ru_ru".
         *
         * @Implements PlayerTag.locale
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "locale", (attribute, object) ->
                new ElementTag(object.getPlayer().locale().toString()));

        /* @doc tag
         *
         * @Name compassTarget
         * @RawName <PlayerTag.compassTarget>
         * @Object PlayerTag
         * @ReturnType LocationTag
         * @Mechanism PlayerTag.compassTarget
         * @NoArg
         * @Description
         * Returns the location the player's compass is currently pointing at.
         *
         * @Implements PlayerTag.compass_target
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "compassTarget", (attribute, object) ->
                new LocationTag(object.getPlayer().getCompassTarget()));

        /* @doc tag
         *
         * @Name wardenWarningLevel
         * @RawName <PlayerTag.wardenWarningLevel>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @Mechanism PlayerTag.wardenWarningLevel
         * @NoArg
         * @Description
         * Returns the player's current Warden warning level (0–4).
         * Reaching level 4 causes the Warden to spawn.
         *
         * @Implements PlayerTag.warden_warning_level
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "wardenWarningLevel", (attribute, object) ->
                new ElementTag(object.getPlayer().getWardenWarningLevel())).ignoreTest();

        /* @doc tag
         *
         * @Name wardenWarningCooldown
         * @RawName <PlayerTag.wardenWarningCooldown>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the player's cooldown in ticks until the next Warden warning can be triggered.
         *
         * @Implements PlayerTag.warden_warning_cooldown
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "wardenWarningCooldown", (attribute, object) ->
                new ElementTag(object.getPlayer().getWardenWarningCooldown())).ignoreTest();

        /* @doc tag
         *
         * @Name wardenTimeSinceLastWarning
         * @RawName <PlayerTag.wardenTimeSinceLastWarning>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the time in ticks since the player last triggered a Warden warning.
         *
         * @Implements PlayerTag.warden_time_since_last_warning
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "wardenTimeSinceLastWarning", (attribute, object) ->
                new ElementTag(object.getPlayer().getWardenTimeSinceLastWarning())).ignoreTest();

        /* @doc tag
         *
         * @Name attackCooldown
         * @RawName <PlayerTag.attackCooldown>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Description
         * Returns the percentage of the player's attack cooldown that has recharged, from 0.0 (empty) to 1.0 (full).
         *
         * @Implements PlayerTag.attack_cooldown
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "attackCooldown", (attribute, object) ->
                new ElementTag(object.getPlayer().getCooledAttackStrength(0f))).ignoreTest();

        /* @doc tag
         *
         * @Name itemOnCursor
         * @RawName <PlayerTag.itemOnCursor>
         * @Object PlayerTag
         * @ReturnType ItemTag
         * @NoArg
         * @Description
         * Returns the item the player is currently holding on their cursor (e.g. dragging in inventory), or air if none.
         *
         * @Implements PlayerTag.item_on_cursor
         */
        TAG_PROCESSOR.registerTag(ItemTag.class, "itemOnCursor", (attribute, object) ->
                new ItemTag(object.getPlayer().getItemOnCursor()));

        /* @doc tag
         *
         * @Name itemInOffHand
         * @RawName <PlayerTag.itemInOffHand>
         * @Object PlayerTag
         * @ReturnType ItemTag
         * @NoArg
         * @Description
         * Returns the item the player is holding in their off hand, or air if none.
         *
         * @Implements EntityTag.item_in_offhand
         */
        TAG_PROCESSOR.registerTag(ItemTag.class, "itemInOffHand", (attribute, object) ->
                new ItemTag(object.getPlayer().getInventory().getItemInOffHand()));






        /* @doc mechanism
         *
         * @Name health
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the amount of health the player has.
         *
         * @Implements EntityTag.health
         */
        MECHANISM_PROCESSOR.registerMechanism("health", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isDouble()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    double maxHp = Objects.requireNonNull(
                            player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue();
                    double clamped = Math.max(0, Math.min(el.asDouble(), maxHp));
                    SchedulerAdapter.runEntity(player, () -> player.setHealth(clamped));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name maxHealth
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the maximum health the player may have.
         *
         * @Implements EntityTag.max_health
         */
        MECHANISM_PROCESSOR.registerMechanism("maxHealth", (playerTag, value) -> {
            Player player = playerTag.getPlayer();
            if (player == null) return playerTag;

            var attribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (attribute == null) return playerTag;

            double targetMax;
            if (value instanceof ElementTag el && el.isDouble()) {
                targetMax = el.asDouble();
            } else {
                targetMax = attribute.getDefaultValue();
            }
            double finalMax = Math.max(0.1, targetMax);
            SchedulerAdapter.runEntity(player, () -> attribute.setBaseValue(finalMax));

            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name food
         * @Input ElementTag(Number)
         * @Object PlayerTag
         * @Description
         * Modifies the current food level of the player.
         * A value of '20' typically represents a full hunger bar.
         *
         * @Implements PlayerTag.food_level
         */
        MECHANISM_PROCESSOR.registerMechanism("food", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isInt()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    int clamped = Math.max(0, Math.min(el.asInt(), 20));
                    SchedulerAdapter.runEntity(player, () -> player.setFoodLevel(clamped));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name whitelisted
         * @Object PlayerTag
         * @Input ElementTag(Boolean)
         * @Description
         * Sets whether this player is on the server whitelist.
         * Works with offline players.
         *
         * @Implements PlayerTag.whitelisted
         */
        MECHANISM_PROCESSOR.registerMechanism("whitelisted", (playerTag, value) -> {
            if (value instanceof ElementTag el) {
                // OfflinePlayer operation — safe to call directly on any thread
                playerTag.offlinePlayer.setWhitelisted(el.asBoolean());
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name op
         * @Object PlayerTag
         * @Input ElementTag(Boolean)
         * @Description
         * Sets whether this player has operator (OP) status on the server.
         * Works with offline players.
         *
         * @Implements PlayerTag.is_op
         */
        MECHANISM_PROCESSOR.registerMechanism("op", (playerTag, value) -> {
            if (value instanceof ElementTag el) {
                playerTag.offlinePlayer.setOp(el.asBoolean());
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name statistic
         * @Object PlayerTag
         * @Input MapTag
         * @Description
         * Sets one or more statistics for this player.
         * Input is a MapTag where each key is a Bukkit Statistic enum name and each value is the new integer value.
         * Works with offline players.
         * @Usage
         * - adjust <player> statistic:<map[DEATHS=0;JUMP=500]>
         *
         * @Implements PlayerTag.statistic
         */
        MECHANISM_PROCESSOR.registerMechanism("statistic", (playerTag, value) -> {
            if (!(value instanceof MapTag mapTag)) return playerTag;
            for (String key : mapTag.keySet()) {
                AbstractTag val = mapTag.getObject(key);
                if (!(val instanceof ElementTag el) || !el.isInt()) continue;
                try {
                    org.bukkit.Statistic stat = org.bukkit.Statistic.valueOf(key.toUpperCase());
                    int clamped = Math.max(0, el.asInt());
                    playerTag.offlinePlayer.setStatistic(stat, clamped);
                } catch (IllegalArgumentException ignored) {}
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name gameMode
         * @Object PlayerTag
         * @Input ElementTag
         * @Description
         * Sets the game mode of the player.
         * Valid values: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR.
         *
         * @Implements PlayerTag.gamemode
         */
        MECHANISM_PROCESSOR.registerMechanism("gameMode", (playerTag, value) -> {
            if (!(value instanceof ElementTag el)) return playerTag;
            Player player = playerTag.getPlayer();
            if (player == null) return playerTag;
            try {
                GameMode mode = GameMode.valueOf(el.asString().toUpperCase());
                SchedulerAdapter.runEntity(player, () -> player.setGameMode(mode));
            } catch (IllegalArgumentException e) {
                Debugger.error("Invalid game mode: '" + el.asString() + "'");
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name level
         * @Object PlayerTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the XP level of the player.
         *
         * @Implements PlayerTag.xp_level
         */
        MECHANISM_PROCESSOR.registerMechanism("level", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isInt()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    int lvl = Math.max(0, el.asInt());
                    SchedulerAdapter.runEntity(player, () -> player.setLevel(lvl));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name exp
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the player's progress towards the next XP level.
         * Input is a decimal between 0.0 and 1.0.
         *
         * @Implements PlayerTag.xp_to_next_level
         */
        MECHANISM_PROCESSOR.registerMechanism("exp", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isDouble()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    float clamped = (float) Math.max(0.0, Math.min(1.0, el.asDouble()));
                    SchedulerAdapter.runEntity(player, () -> player.setExp(clamped));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name totalXp
         * @Object PlayerTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the total experience points of the player.
         *
         * @Implements PlayerTag.total_xp
         */
        MECHANISM_PROCESSOR.registerMechanism("totalXp", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isInt()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    int total = Math.max(0, el.asInt());
                    SchedulerAdapter.runEntity(player, () -> player.setTotalExperience(total));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name flying
         * @Object PlayerTag
         * @Input ElementTag(Boolean)
         * @Description
         * Sets whether the player is currently flying.
         * Note: the player must have flight allowed (see PlayerTag.allowFlight) for this to take effect.
         *
         * @Implements PlayerTag.is_flying
         */
        MECHANISM_PROCESSOR.registerMechanism("flying", (playerTag, value) -> {
            if (value instanceof ElementTag el) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    boolean fly = el.asBoolean();
                    SchedulerAdapter.runEntity(player, () -> player.setFlying(fly));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name allowFlight
         * @Object PlayerTag
         * @Input ElementTag(Boolean)
         * @Description
         * Sets whether this player is allowed to fly.
         *
         * @Implements PlayerTag.can_fly
         */
        MECHANISM_PROCESSOR.registerMechanism("allowFlight", (playerTag, value) -> {
            if (value instanceof ElementTag el) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    boolean allow = el.asBoolean();
                    SchedulerAdapter.runEntity(player, () -> player.setAllowFlight(allow));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name flySpeed
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the fly speed of the player.
         * Valid range is -1.0 to 1.0. The default is 0.1.
         *
         * @Implements PlayerTag.fly_speed
         */
        MECHANISM_PROCESSOR.registerMechanism("flySpeed", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isDouble()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    float clamped = (float) Math.max(-1.0, Math.min(1.0, el.asDouble()));
                    SchedulerAdapter.runEntity(player, () -> player.setFlySpeed(clamped));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name walkSpeed
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the walk speed of the player.
         * Valid range is -1.0 to 1.0. The default is 0.2.
         *
         * @Implements PlayerTag.walk_speed
         */
        MECHANISM_PROCESSOR.registerMechanism("walkSpeed", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isDouble()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    float clamped = (float) Math.max(-1.0, Math.min(1.0, el.asDouble()));
                    SchedulerAdapter.runEntity(player, () -> player.setWalkSpeed(clamped));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name saturation
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the food saturation of the player.
         * Saturation acts as a buffer for the food bar — it drains before the food bar does.
         *
         * @Implements PlayerTag.saturation
         */
        MECHANISM_PROCESSOR.registerMechanism("saturation", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isDouble()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    float sat = (float) Math.max(0.0, el.asDouble());
                    SchedulerAdapter.runEntity(player, () -> player.setSaturation(sat));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name exhaustion
         * @Object PlayerTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the exhaustion level of the player.
         * Exhaustion controls how quickly saturation and food bars deplete.
         *
         * @Implements PlayerTag.exhaustion
         */
        MECHANISM_PROCESSOR.registerMechanism("exhaustion", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isDouble()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    float exh = (float) Math.max(0.0, el.asDouble());
                    SchedulerAdapter.runEntity(player, () -> player.setExhaustion(exh));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name displayName
         * @Object PlayerTag
         * @Input ElementTag
         * @Description
         * Sets the display name of the player using MiniMessage formatting.
         * This is the "friendly" name shown in chat and other contexts.
         *
         * @Implements PlayerTag.display_name
         */
        MECHANISM_PROCESSOR.registerMechanism("displayName", (playerTag, value) -> {
            if (value instanceof ElementTag el) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    var component = MINI_MESSAGE.deserialize(el.asString());
                    SchedulerAdapter.runEntity(player, () -> player.displayName(component));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name compassTarget
         * @Object PlayerTag
         * @Input LocationTag
         * @Description
         * Sets the location the player's compass points at.
         *
         * @Implements PlayerTag.compass_target
         */
        MECHANISM_PROCESSOR.registerMechanism("compassTarget", (playerTag, value) -> {
            if (value instanceof LocationTag locTag) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    Location loc = locTag.getLocation();
                    SchedulerAdapter.runEntity(player, () -> player.setCompassTarget(loc));
                }
            }
            return playerTag;
        });

        /* @doc mechanism
         *
         * @Name wardenWarningLevel
         * @Object PlayerTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the player's Warden warning level (0–4).
         *
         * @Implements PlayerTag.warden_warning_level
         */
        MECHANISM_PROCESSOR.registerMechanism("wardenWarningLevel", (playerTag, value) -> {
            if (value instanceof ElementTag el && el.isInt()) {
                Player player = playerTag.getPlayer();
                if (player != null) {
                    int level = Math.max(0, Math.min(4, el.asInt()));
                    SchedulerAdapter.runEntity(player, () -> player.setWardenWarningLevel(level));
                }
            }
            return playerTag;
        });
    }

    public PlayerTag(UUID uuid) {
        this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
    }

    public PlayerTag(Player player) {
        this.offlinePlayer = player;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public Player getPlayer() {
        return offlinePlayer.getPlayer();
    }

    public PlayerTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.offlinePlayer = null;
        } else {
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(2) : raw;

            OfflinePlayer tempPlayer;
            try {
                tempPlayer = Bukkit.getOfflinePlayer(UUID.fromString(cleanRaw));
            } catch (Exception e) {
                tempPlayer = Bukkit.getOfflinePlayer(cleanRaw);
            }
            this.offlinePlayer = tempPlayer;
        }
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + offlinePlayer.getUniqueId().toString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<PlayerTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @Nullable MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "p@465876c1-2a15-4fc0-9f0b-97de13aa46f1";
    }
}