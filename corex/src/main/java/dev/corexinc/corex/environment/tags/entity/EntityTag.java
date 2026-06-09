package dev.corexinc.corex.environment.tags.entity;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.PdcFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.adapters.EntityAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/* @doc object
 *
 * @Name EntityTag
 * @Prefix e
 * @Format
 * The identity format for entities is either a spawned entity's UUID, or an entity type blueprint.
 * A spawned entity is 'e@<uuid>', for example 'e@cf5d1e35-fb92-476e-9c96-bc932ca0b0cb'.
 * A blueprint is 'e@<type>' with optional mechanisms, for example 'e@item_display' or 'e@zombie[maxHealth=100;name=okak]'.
 *
 * @Description
 * An EntityTag represents either a spawned entity, or an unspawned entity blueprint.
 * A blueprint holds an entity type and a set of mechanisms that are applied once the entity is spawned.
 * Blueprints are created without summoning anything, and are turned into real entities only by the Spawn command.
 *
 * Note that applying a mechanism to a spawned entity (via .with or the Adjust command) mutates the live entity in the world immediately.
 * Applying a mechanism to a blueprint only records it, to be applied by the next Spawn.
 */
public class EntityTag implements AbstractTag, Adjustable, Flaggable {

    private static final String prefix = "e";

    private final Entity entity;
    private final EntityType type;
    private final MapTag mechanisms;

    public static final TagProcessor<EntityTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<EntityTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private static final EntityAdapter nms = NMSHandler.get().get(EntityAdapter.class);

    private record NbtMechanism(String mechanism, Function<AbstractTag, AbstractTag> transform) {}

    private static final Map<String, NbtMechanism> NBT_MECHANISMS = new LinkedHashMap<>();

    private static void mechanism(String name, BiConsumer<Entity, AbstractTag> applier) {
        mechanism(name, null, null, applier);
    }

    private static void mechanism(String name, String nbtKey, Function<AbstractTag, AbstractTag> nbtTransform, BiConsumer<Entity, AbstractTag> applier) {
        MECHANISM_PROCESSOR.registerMechanism(name, (object, value) -> object.adjust(name, value, applier));
        if (nbtKey != null) NBT_MECHANISMS.put(nbtKey, new NbtMechanism(name, nbtTransform));
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("entity", (attribute) -> {
            if (!attribute.hasParam()) return null;
            return new EntityTag(attribute.getParam());
        });

        ObjectFetcher.registerFetcher(prefix, EntityTag::new);

        /* @doc tag
         *
         * @Name uuid
         * @RawName <EntityTag.uuid>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the permanent unique ID of the entity.
         *
         * @Implements EntityTag.uuid
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) -> new  ElementTag(object.entity.getUniqueId().toString()));

        /* @doc tag
         *
         * @Name name
         * @RawName <EntityTag.name>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the name of the entity.
         * This can be a custom_name or the entity type.
         *
         * @Implements EntityTag.name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> new ElementTag(object.entity.getName()));

        /* @doc tag
         *
         * @Name type
         * @RawName <EntityTag.type>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the type of the entity.
         *
         * @Implements EntityTag.type
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "type", (attribute, object) -> new ElementTag(object.getEntityType().name()));

        /* @doc tag
         *
         * @Name isSpawned
         * @RawName <EntityTag.isSpawned>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns 'true' if this EntityTag points to a spawned entity, or 'false' if it is an unspawned blueprint.
         *
         * @Implements EntityTag.is_spawned
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isSpawned", (attribute, object) -> new ElementTag(object.entity != null));

        /* @doc tag
         *
         * @Name isAlive
         * @RawName <EntityTag.isAlive>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns 'true' whether the entity is alive.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isAlive", (attribute, object) -> new ElementTag(String.valueOf(!object.entity.isDead())));

        /* @doc tag
         *
         * @Name location
         * @RawName <EntityTag.location>
         * @Object EntityTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * For living entities, this is at the center of their feet.
         *
         * @Implements EntityTag.location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "location", (attribute, object) -> new LocationTag(object.entity.getLocation()));

        /* @doc tag
         *
         * @Name describe
         * @RawName <EntityTag.describe>
         * @Object EntityTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a MapTag of the entity's mechanisms and their current values.
         * For a spawned entity this is read from the live entity, for a blueprint it is the recorded mechanisms.
         * The result can be fed back into <@link tag EntityTag.with> or the Adjust command.
         *
         * @Implements EntityTag.describe
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "describe", (attribute, object) -> object.describe());

        /* @doc tag
         *
         * @Name nbt
         * @RawName <EntityTag.nbt>
         * @Object EntityTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns the full raw NBT of a spawned entity as a MapTag of NBT key to SNBT value, for example 'map@[NoGravity=1b;Health=20.0f]'.
         * Each value keeps its exact NBT type, so the map round-trips losslessly through the nbt mechanism.
         * Returns an empty map for an unspawned blueprint, or when no NMS adapter is available for the server version.
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "nbt", (attribute, object) -> object.readNbt()).ignoreTest();

        /* @doc tag
         *
         * @Name blueprint
         * @RawName <EntityTag.blueprint>
         * @Object EntityTag
         * @ReturnType EntityTag
         * @NoArg
         * @Description
         * Returns an unspawned blueprint of the entity - its type plus a snapshot of its mechanisms.
         * For example a spawned 'e@<uuid>' becomes 'e@zombie[maxHealth=100;name=okak]'.
         * The blueprint can be passed to the Spawn command to create fresh copies.
         */
        TAG_PROCESSOR.registerTag(EntityTag.class, "blueprint", (attribute, object) ->
                new EntityTag(null, object.getEntityType(), object.describe()));

        /* @doc mechanism
         *
         * @Name name
         * @Object EntityTag
         * @Input ElementTag
         * @Description
         * Sets the entity's custom name.
         *
         * @Implements EntityTag.custom_name
         */
        mechanism("name", (target, val) -> target.customName(val.asComponent()));

        /* @doc mechanism
         *
         * @Name customNameVisible
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity's custom name is always visible.
         *
         * @Implements EntityTag.custom_name_visible
         */
        mechanism("customNameVisible", "CustomNameVisible", EntityTag::nbtByteToBool, (target, val) -> target.setCustomNameVisible(asBoolean(val)));

        /* @doc mechanism
         *
         * @Name maxHealth
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the maximum health of a living entity.
         *
         * @Implements EntityTag.max_health
         */
        mechanism("maxHealth", (target, val) -> {
            if (target instanceof LivingEntity living) {
                AttributeInstance attribute = living.getAttribute(Attribute.MAX_HEALTH);
                if (attribute != null) attribute.setBaseValue(asDouble(val));
            }
        });

        /* @doc mechanism
         *
         * @Name health
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the current health of a living entity, clamped to its maximum health.
         *
         * @Implements EntityTag.health
         */
        mechanism("health", "Health", EntityTag::nbtNumber, (target, val) -> {
            if (target instanceof LivingEntity living) {
                AttributeInstance attribute = living.getAttribute(Attribute.MAX_HEALTH);
                double max = attribute != null ? attribute.getValue() : asDouble(val);
                living.setHealth(Math.max(0.0, Math.min(asDouble(val), max)));
            }
        });

        /* @doc mechanism
         *
         * @Name glowing
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity has the glowing outline effect.
         *
         * @Implements EntityTag.glowing
         */
        mechanism("glowing", "Glowing", EntityTag::nbtByteToBool, (target, val) -> target.setGlowing(asBoolean(val)));

        /* @doc mechanism
         *
         * @Name gravity
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity is affected by gravity.
         *
         * @Implements EntityTag.gravity
         */
        mechanism("gravity", "NoGravity", EntityTag::nbtInvertedByteToBool, (target, val) -> target.setGravity(asBoolean(val)));

        /* @doc mechanism
         *
         * @Name invulnerable
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity is immune to all damage sources except void and /kill.
         *
         * @Implements EntityTag.invulnerable
         */
        mechanism("invulnerable", "Invulnerable", EntityTag::nbtByteToBool, (target, val) -> target.setInvulnerable(asBoolean(val)));

        /* @doc mechanism
         *
         * @Name silent
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether the entity produces sounds.
         *
         * @Implements EntityTag.silent
         */
        mechanism("silent", "Silent", EntityTag::nbtByteToBool, (target, val) -> target.setSilent(asBoolean(val)));

        /* @doc mechanism
         *
         * @Name ai
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Controls whether a living entity runs its AI (movement, targeting, goals).
         *
         * @Implements EntityTag.has_ai
         */
        mechanism("ai", "NoAI", EntityTag::nbtInvertedByteToBool, (target, val) -> {
            if (target instanceof LivingEntity living) living.setAI(asBoolean(val));
        });

        /* @doc mechanism
         *
         * @Name fireTicks
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many ticks the entity stays on fire for.
         *
         * @Implements EntityTag.fire_time
         */
        mechanism("fireTicks", "Fire", EntityTag::nbtNumber, (target, val) -> target.setFireTicks(asInt(val)));

        /* @doc mechanism
         *
         * @Name freezeTicks
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets how many ticks of powder snow freezing the entity has accumulated.
         *
         * @Implements EntityTag.freeze_duration
         */
        mechanism("freezeTicks", "TicksFrozen", EntityTag::nbtNumber, (target, val) -> target.setFreezeTicks(asInt(val)));

        /* @doc mechanism
         *
         * @Name air
         * @Object EntityTag
         * @Input ElementTag(Number)
         * @Description
         * Sets the remaining air (in ticks) of a living entity.
         *
         * @Implements EntityTag.oxygen
         */
        mechanism("air", "Air", EntityTag::nbtNumber, (target, val) -> {
            if (target instanceof LivingEntity living) living.setRemainingAir(asInt(val));
        });

        /* @doc mechanism
         *
         * @Name fallDistance
         * @Object EntityTag
         * @Input ElementTag(Decimal)
         * @Description
         * Sets the distance the entity has fallen, used to calculate fall damage.
         *
         * @Implements EntityTag.fall_distance
         */
        mechanism("fallDistance", "FallDistance", EntityTag::nbtNumber, (target, val) -> target.setFallDistance((float) asDouble(val)));

        /* @doc mechanism
         *
         * @Name velocity
         * @Object EntityTag
         * @Input LocationTag
         * @Description
         * Sets the entity's velocity to the vector of the given LocationTag.
         *
         * @Implements EntityTag.velocity
         */
        mechanism("velocity", "Motion", EntityTag::nbtMotion, (target, val) -> target.setVelocity(new LocationTag(val.identify()).getLocation().toVector()));

        /* @doc mechanism
         *
         * @Name rotation
         * @Object EntityTag
         * @Input LocationTag
         * @Description
         * Sets the entity's body rotation to the yaw and pitch of the given LocationTag.
         *
         * @Implements EntityTag.rotate
         */
        mechanism("rotation", "Rotation", EntityTag::nbtRotation, (target, val) -> {
            Location loc = new LocationTag(val.identify()).getLocation();
            target.setRotation(loc.getYaw(), loc.getPitch());
        });

        /* @doc mechanism
         *
         * @Name nbt
         * @Object EntityTag
         * @Input MapTag
         * @Description
         * Merges the given MapTag of raw NBT into the entity. Keys are vanilla NBT keys, values are SNBT (see <@link tag EntityTag.nbt>).
         * SNBT keeps the exact type, for example 'map@[NoGravity=1b;Health=20.0f]'. Pairs nicely with the output of the nbt tag.
         * This is a low-level mechanism, prefer a dedicated mechanism where one exists.
         *
         * @Implements EntityTag.nbt
         */
        mechanism("nbt", (target, val) -> {
            if (nms != null && val instanceof MapTag map) nms.applyNbt(target, map);
        });
    }

    private EntityTag(Entity entity, EntityType type, MapTag mechanisms) {
        this.entity = entity;
        this.type = type;
        this.mechanisms = mechanisms;
    }

    public EntityTag(UUID uuid) {
        this(Bukkit.getEntity(uuid), null, new MapTag());
    }

    public EntityTag(Entity entity) {
        this(entity, null, new MapTag());
    }

    public EntityTag(String raw) {
        Entity parsedEntity = null;
        EntityType parsedType = null;
        MapTag parsedMechanisms = new MapTag();

        if (raw != null && !raw.isEmpty()) {
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(prefix.length() + 1) : raw;
            int bracketStart = cleanRaw.indexOf('[');
            String basePart = cleanRaw;

            if (bracketStart > 0 && cleanRaw.endsWith("]")) {
                basePart = cleanRaw.substring(0, bracketStart);
                parsedMechanisms = new MapTag(cleanRaw.substring(bracketStart + 1, cleanRaw.length() - 1));
            }

            try {
                parsedEntity = Bukkit.getEntity(UUID.fromString(basePart));
            } catch (IllegalArgumentException ignored) {
                parsedType = matchEntityType(basePart);
            }
        }

        this.entity = parsedEntity;
        this.type = parsedType;
        this.mechanisms = parsedMechanisms;
    }

    private AbstractTag adjust(String name, AbstractTag value, BiConsumer<Entity, AbstractTag> liveApplier) {
        if (entity != null) {
            liveApplier.accept(entity, value);
        } else {
            mechanisms.putObject(name, value);
        }
        return this;
    }

    @SuppressWarnings("UnstableApiUsage")
    public EntityTag spawn(Location location, CreatureSpawnEvent.SpawnReason reason, boolean persistent) {
        World world = location.getWorld();
        if (world == null) return null;

        Entity spawned;
        if (entity != null) {
            spawned = entity.copy(location);
        } else {
            if (type == null) return null;
            spawned = world.spawnEntity(location, type, reason);
            EntityTag blueprint = new EntityTag(spawned);
            for (String key : mechanisms.keySet()) {
                blueprint.applyMechanism(key, mechanisms.getObject(key));
            }
        }

        if (persistent) spawned.setPersistent(true);
        return new EntityTag(spawned);
    }

    public MapTag readNbt() {
        if (entity == null || nms == null) return new MapTag();
        return nms.readNbt(entity);
    }

    public MapTag describe() {
        if (entity == null) {
            MapTag copy = new MapTag();
            for (String key : mechanisms.keySet()) copy.putObject(key, mechanisms.getObject(key));
            return copy;
        }

        MapTag nbt = readNbt();
        MapTag data = new MapTag();

        for (Map.Entry<String, NbtMechanism> entry : NBT_MECHANISMS.entrySet()) {
            AbstractTag raw = nbt.getObject(entry.getKey());
            if (raw == null) continue;
            AbstractTag value = entry.getValue().transform().apply(raw);
            if (value != null) data.putObject(entry.getValue().mechanism(), value);
        }

        if (entity.customName() != null) {
            data.putObject("name", new ElementTag(CorexSerializer.LEGACY.serialize(entity.customName())));
        }
        if (entity instanceof LivingEntity living) {
            AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) data.putObject("maxHealth", new ElementTag(maxHealth.getBaseValue()));
        }

        return data;
    }

    private static String nbtNumeric(String snbt) {
        if (!snbt.isEmpty() && "bslfdBSLFD".indexOf(snbt.charAt(snbt.length() - 1)) >= 0) {
            String body = snbt.substring(0, snbt.length() - 1);
            try {
                Double.parseDouble(body);
                return body;
            } catch (NumberFormatException ignored) {}
        }
        return snbt;
    }

    private static double[] nbtNumberList(String snbt, int expected) {
        String body = snbt.startsWith("[") && snbt.endsWith("]") ? snbt.substring(1, snbt.length() - 1) : snbt;
        int semicolon = body.indexOf(';');
        if (semicolon >= 0 && semicolon <= 2) body = body.substring(semicolon + 1);

        String[] parts = body.split(",");
        if (parts.length < expected) return null;

        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            try {
                values[i] = Double.parseDouble(nbtNumeric(parts[i].trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return values;
    }

    private static AbstractTag nbtNumber(AbstractTag value) {
        return new ElementTag(nbtNumeric(value.identify()));
    }

    private static AbstractTag nbtByteToBool(AbstractTag value) {
        String numeric = nbtNumeric(value.identify());
        return new ElementTag(!numeric.equals("0") && !numeric.equalsIgnoreCase("false"));
    }

    private static AbstractTag nbtInvertedByteToBool(AbstractTag value) {
        String numeric = nbtNumeric(value.identify());
        return new ElementTag(numeric.equals("0") || numeric.equalsIgnoreCase("false"));
    }

    private static AbstractTag nbtMotion(AbstractTag value) {
        double[] motion = nbtNumberList(value.identify(), 3);
        return motion != null ? new LocationTag(motion[0], motion[1], motion[2], 0, 0) : value;
    }

    private static AbstractTag nbtRotation(AbstractTag value) {
        double[] rotation = nbtNumberList(value.identify(), 2);
        return rotation != null ? new LocationTag(0, 0, 0, (float) rotation[0], (float) rotation[1]) : value;
    }

    private static EntityType matchEntityType(String name) {
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
        return key != null ? Registry.ENTITY_TYPE.get(key) : null;
    }

    private static double asDouble(AbstractTag value) {
        return value instanceof ElementTag element ? element.asDouble() : new ElementTag(value.identify()).asDouble();
    }

    private static int asInt(AbstractTag value) {
        return value instanceof ElementTag element ? element.asInt() : new ElementTag(value.identify()).asInt();
    }

    private static boolean asBoolean(AbstractTag value) {
        return value instanceof ElementTag element ? element.asBoolean() : new ElementTag(value.identify()).asBoolean();
    }

    public boolean tryAdvancedMatcher(String matcher) {
        if (entity == null) return false;
        if (matcher == null || matcher.isEmpty() || matcher.equals("*") || matcher.equalsIgnoreCase("any")) {
            return true;
        }

        String pattern = matcher.toLowerCase();

        if (pattern.equals(entity.getUniqueId().toString().toLowerCase())) {
            return true;
        }

        String typeName = entity.getType().name().toLowerCase();
        if (pattern.equals(typeName)) {
            return true;
        }

        if (pattern.contains("*")) {
            return typeName.matches(pattern.replace("*", ".*"));
        }

        return entity.customName() != null && entity.customName().toString().toLowerCase().contains(pattern);
    }

    public Entity getEntity() {
        return entity;
    }

    public EntityType getEntityType() {
        return entity != null ? entity.getType() : type;
    }

    @Override
    public @NotNull String identify() {
        if (entity != null) return prefix + "@" + entity.getUniqueId();

        StringBuilder builder = new StringBuilder(prefix + "@");
        builder.append(type != null ? type.getKey().getKey() : "unknown");

        if (!mechanisms.isEmpty()) {
            List<String> pairs = new ArrayList<>();
            for (String key : mechanisms.keySet()) {
                pairs.add(key + "=" + mechanisms.getObject(key).identify());
            }
            builder.append("[").append(String.join(";", pairs)).append("]");
        }
        return builder.toString();
    }

    @Override
    public @NotNull String getPrefix() {
        return prefix;
    }

    @Override
    public @Nullable AbstractTag getAttribute(@NotNull dev.corexinc.corex.api.tags.Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull Adjustable duplicate() {
        MapTag copy = new MapTag();
        for (String key : mechanisms.keySet()) copy.putObject(key, mechanisms.getObject(key));
        return new EntityTag(entity, type, copy);
    }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NonNull MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        if (entity == null) return null;
        return new PdcFlagTracker(entity, identify());
    }

    @Override
    public @NonNull String getTestValue() {
        return "e@cf5d1e35-fb92-476e-9c96-bc932ca0b0cb";
    }

    @Override
    public @NonNull TagProcessor<EntityTag> getProcessor() {
        return TAG_PROCESSOR;
    }
}
