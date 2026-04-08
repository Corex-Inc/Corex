package dev.corexinc.corex.environment.tags.player;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
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
         * @Description
         * Returns whether the player is currently online.
         * Works with offline players (returns false in that case).
         *
         * @Implements PlayerTag.is_online
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnline", (attribute, object) -> new ElementTag(String.valueOf(object.offlinePlayer.isOnline())));

        /* @doc tag
         *
         * @Name uuid
         * @RawName <PlayerTag.uuid>
         * @Object PlayerTag
         * @ReturnType ElementTag
         * @Description
         * Returns the permanent unique ID of the player.
         *
         * @Implements EntityTag.uuid
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) -> new ElementTag(object.offlinePlayer.getUniqueId().toString()));

        /* @doc tag
         *
         * @Name location
         * @RawName <PlayerTag.location>
         * @Object PlayerTag
         * @ReturnType LocationTag
         * @Description
         * For players, this is at the center of their feet.
         *
         * @Implements EntityTag.location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "location", (attribute, object) -> new LocationTag(object.getPlayer().getLocation()));

        /* @doc tag
         *
         * @Name health
         * @RawName <PlayerTag.health>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.health
         * @Description
         * Returns the current health of the entity.
         *
         * @Implements EntityTag.health
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "health", ((attribute, object) -> new ElementTag(object.getPlayer().getHealth())));

        /* @doc tag
         *
         * @Name food
         * @RawName <PlayerTag.food>
         * @Object PlayerTag
         * @ReturnType ElementTag(Number)
         * @Mechanism PlayerTag.food
         * @Description
         * Returns the current food level (aka hunger) of the player.
         *
         * @Implements PlayerTag.food_level
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "food", ((attribute, object) -> new ElementTag(object.getPlayer().getFoodLevel())));

        /* @doc tag
         *
         * @Name target
         * @RawName <PlayerTag.target[(<matcher>)]>
         * @Object PlayerTag
         * @ReturnType EntityTag
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
         * @Description
         * Returns the item the player is holding, or air if none.
         *
         * @Implements EntityTag.item_in_hand
         */
        TAG_PROCESSOR.registerTag(ItemTag.class, "itemInHand", ((attribute, object) -> new ItemTag(object.getPlayer().getInventory().getItemInMainHand())));

        /* @doc tag
         *
         * @Name maxHealth
         * @RawName <PlayerTag.maxHealth>
         * @Object PlayerTag
         * @ReturnType ElementTag(Decimal)
         * @Mechanism PlayerTag.maxHealth
         * @Description
         * Returns the maximum health of the entity.
         *
         * @Implements EntityTag.health_max
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxHealth", ((attribute, object) ->
                new ElementTag(Objects.requireNonNull(object.getPlayer().getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()))).ignoreTest();


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
                    double maxHp = Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue();
                    player.setHealth(Math.max(0, Math.min(el.asDouble(), maxHp)));
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
            if (targetMax < 0.1) targetMax = 0.1;
            attribute.setBaseValue(targetMax);

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
                if (player != null) player.setFoodLevel(Math.max(0, Math.min(el.asInt(), 20)));
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