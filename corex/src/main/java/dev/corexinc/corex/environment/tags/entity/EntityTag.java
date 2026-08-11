package dev.corexinc.corex.environment.tags.entity;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.properties.PropertyRegistrar;
import dev.corexinc.corex.api.properties.PropertyType;
import dev.corexinc.corex.api.properties.PropertyTypes;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.PdcFlagTracker;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.adapters.EntityAdapter;
import dev.corexinc.corex.environment.utils.entities.BukkitEntityView;
import dev.corexinc.corex.environment.utils.entities.FakeEntityRegistry;
import dev.corexinc.corex.environment.utils.entities.FakeEntityView;
import dev.corexinc.corex.environment.utils.entities.LiveEntityView;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
 *
 * @Implements EntityTag
 */
public class EntityTag implements AbstractTag, Adjustable, Flaggable {

    private static final String prefix = "e";

    private final Entity entity;
    private WrapperEntity fakeEntity = null;
    private LiveEntityView liveView = null;
    private final EntityType type;
    private final Map<String, AbstractTag> mechanisms;

    public static final TagProcessor<EntityTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<EntityTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private static final EntityAdapter nms = NMSHandler.get().get(EntityAdapter.class);

    private record NbtMechanism(String mechanism, Function<AbstractTag, AbstractTag> transform) {}

    private static final Map<String, NbtMechanism> NBT_MECHANISMS = new LinkedHashMap<>();

    static final PropertyRegistrar<EntityTag, LiveEntityView> PROPERTIES =
            new PropertyRegistrar<EntityTag, LiveEntityView>("EntityTag", TAG_PROCESSOR, MECHANISM_PROCESSOR,
                    EntityTag::bindProperty)
                    .withRecordedValues((object, property) -> object.mechanisms.get(property));

    static <E, V> void property(String name, Class<E> entityClass, PropertyType<V> type,
                                Function<E, V> reader, BiConsumer<LiveEntityView, V> writer) {
        PROPERTIES.property(name, type)
                .read(object -> entityClass.isInstance(object.entity) ? reader.apply(entityClass.cast(object.entity)) : null)
                .write(writer)
                .register();
    }

    static Dispatch dispatch(String name) {
        return new Dispatch(name);
    }

    static final class Dispatch {

        private record Branch(Class<?> entityClass,
                              Function<Entity, AbstractTag> reader,
                              BiConsumer<LiveEntityView, AbstractTag> writer) {}

        <E extends Entity, V> Dispatch onClearable(Class<E> entityClass, PropertyType<V> codec,
                                                   Function<E, V> reader, BiConsumer<LiveEntityView, V> writer) {
            return on(entityClass, codec, reader, writer, true);
        }

        private final String name;
        private final List<Branch> branches = new ArrayList<>();

        private Dispatch(String name) {
            this.name = name;
        }

        <E extends Entity, V> Dispatch on(Class<E> entityClass, PropertyType<V> codec,
                                          Function<E, V> reader, BiConsumer<LiveEntityView, V> writer) {
            return on(entityClass, codec, reader, writer, false);
        }

        private <E extends Entity, V> Dispatch on(Class<E> entityClass, PropertyType<V> codec,
                                                  Function<E, V> reader, BiConsumer<LiveEntityView, V> writer,
                                                  boolean clearable) {
            branches.add(new Branch(entityClass,
                    entity -> {
                        V value = reader.apply(entityClass.cast(entity));
                        return value != null ? codec.write(value) : null;
                    },
                    (view, raw) -> {
                        V value = resolve(codec, raw, clearable, entityClass);
                        if (value != null || (clearable && PropertyTypes.isClearInput(raw))) {
                            writer.accept(view, value);
                        }
                    }));
            return this;
        }

        private <V> V resolve(PropertyType<V> codec, AbstractTag raw, boolean clearable, Class<?> entityClass) {
            if (clearable && PropertyTypes.isClearInput(raw)) return null;
            V value = codec.parse(raw);
            if (value == null) {
                Debugger.echoError(null, "Invalid input '" + raw.identify() + "' for mechanism 'EntityTag."
                        + name + "' on " + entityClass.getSimpleName()
                        + " - expected " + codec.describeInput() + ".");
            }
            return value;
        }

        void register() {
            PROPERTIES.property(name, PropertyTypes.ANY)
                    .read(object -> {
                        if (object.entity == null) return null;
                        for (Branch branch : branches) {
                            if (branch.entityClass().isInstance(object.entity)) {
                                return branch.reader().apply(object.entity);
                            }
                        }
                        return null;
                    })
                    .write((view, raw) -> {
                        Class<?> species = view.bukkitType().getEntityClass();
                        if (species == null) return;
                        for (Branch branch : branches) {
                            if (branch.entityClass().isAssignableFrom(species)) {
                                branch.writer().accept(view, raw);
                                return;
                            }
                        }
                    })
                    .register();
        }
    }

    static <E, V> void clearableProperty(String name, Class<E> entityClass, PropertyType<V> type,
                                         Function<E, V> reader, BiConsumer<LiveEntityView, V> writer) {
        PROPERTIES.property(name, type)
                .read(object -> entityClass.isInstance(object.entity) ? reader.apply(entityClass.cast(object.entity)) : null)
                .write(writer)
                .clearable()
                .register();
    }

    static <E, V> void readOnlyProperty(String name, Class<E> entityClass, PropertyType<V> type,
                                        Function<E, V> reader) {
        PROPERTIES.property(name, type)
                .read(object -> entityClass.isInstance(object.entity) ? reader.apply(entityClass.cast(object.entity)) : null)
                .register();
    }

    static <E, V> void livingProperty(String name, Class<E> entityClass, PropertyType<V> type,
                                      Function<E, V> reader, BiConsumer<LiveEntityView, V> writer) {
        property(name, entityClass, type, reader, writer);
    }

    static <K extends Keyed> PropertyType<K> registryOf(Supplier<Registry<K>> registrySupplier,
                                                                   String description) {
        return new PropertyType<>() {

            private Registry<K> registry;

            private Registry<K> registry() {
                if (registry == null) registry = registrySupplier.get();
                return registry;
            }

            @Override
            public K parse(@NotNull AbstractTag input) {
                NamespacedKey key = NamespacedKey.fromString(input.identify().toLowerCase());
                if (key == null) return null;
                Registry<K> resolved = registry();
                return resolved != null ? resolved.get(key) : null;
            }

            @Override
            public @NotNull AbstractTag write(@NotNull K value) {
                return new ElementTag(value.getKey().getKey());
            }

            @Override
            public @NotNull Class<? extends AbstractTag> tagClass() {
                return ElementTag.class;
            }

            @Override
            public @NotNull String describeInput() {
                return description;
            }
        };
    }

    private static void registerMechanism(String name, BiConsumer<LiveEntityView, AbstractTag> applier) {
        registerMechanism(name, null, null, applier);
    }

    private static void registerMechanism(String name, String nbtKey, Function<AbstractTag,
            AbstractTag> nbtTransform, BiConsumer<LiveEntityView, AbstractTag> applier) {

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
         * @Async
         * @Description
         * Returns the permanent unique ID of the entity.
         *
         * @Implements EntityTag.uuid
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) -> {
            if (object.entity != null) return new ElementTag(object.entity.getUniqueId().toString());
            if (object.fakeEntity != null) return new ElementTag("fake-" + object.fakeEntity.getEntityId());
            return null;
        }).setAsyncSafe();

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
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
            if (object.entity != null) return new ElementTag(object.entity.getName());
            AbstractTag recorded = object.mechanisms.get("name");
            if (recorded != null) return new ElementTag(recorded.identify());
            return object.type != null ? new ElementTag(object.type.getKey().getKey()) : null;
        });

        /* @doc tag
         *
         * @Name type
         * @RawName <EntityTag.type>
         * @Object EntityTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the type of the entity.
         *
         * @Implements EntityTag.type
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "type", (attribute, object) -> new ElementTag(object.getEntityType().name())).setAsyncSafe();

        /* @doc tag
         *
         * @Name isSpawned
         * @RawName <EntityTag.isSpawned>
         * @Object EntityTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' if this EntityTag points to a spawned entity, or 'false' if it is an unspawned blueprint.
         *
         * @Implements EntityTag.is_spawned
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isSpawned", (attribute, object) -> new ElementTag(object.entity != null || object.fakeEntity != null)).setAsyncSafe();

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
        TAG_PROCESSOR.registerTag(ElementTag.class, "isAlive", (attribute, object) -> {
            if (object.entity != null) return new ElementTag(String.valueOf(!object.entity.isDead()));
            if (object.fakeEntity != null) return new ElementTag("true");
            return null;
        });

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
        TAG_PROCESSOR.registerTag(LocationTag.class, "location", (attribute, object) ->
                object.entity != null ? new LocationTag(object.entity.getLocation()) : null);

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
         *
         * @Implements EntityTag.all_raw_nbt
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
         * For example a spawned 'e@<uuid>' becomes 'e@zombie[maxHealth=100;name=My zombiiieeeee]'.
         * The blueprint can be passed to the Spawn command to create fresh copies.
         */
        TAG_PROCESSOR.registerTag(EntityTag.class, "blueprint", (attribute, object) ->
                new EntityTag(null, object.getEntityType(), mechanismsOf(object.describe())));

        registerProperties();

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
        registerMechanism("name", (target, val) -> target.setCustomName(val.asComponent()));

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
        registerMechanism("nbt", (target, val) -> { if (val instanceof MapTag map) target.setNbt(map); });

        /* @doc mechanism
         *
         * @Name forceNoPersist
         * @Object EntityTag
         * @Input ElementTag(Boolean)
         * @Description
         * Forces whether the entity is allowed to persist (survive a chunk unload or server restart, and be saved to disk).
         * Setting this to 'true' calls setPersistent(false) on the entity, forcing it to NOT persist.
         * Setting this to 'false' calls setPersistent(true), allowing it to persist normally.
         *
         * @Implements EntityTag.force_no_persist
         */
        registerMechanism("forceNoPersist", (target, val) -> target.setPersistent(!asBoolean(val)));

        /* @doc mechanism
         *
         * @Name brightness
         * @Object EntityTag
         * @Input MapTag
         * @Description
         * Sets the brightness override of a Display entity, bypassing the world's natural lighting.
         * Input is a MapTag with 'sky' and 'block' keys, each 0-15, for example 'map[sky=15;block=15]'.
         * Only applies to Display entities.
         *
         * @Implements EntityTag.brightness
         */
        registerMechanism("brightness", (target, val) -> {
            if (val instanceof MapTag map) {
                target.setBrightness(
                        map.getObject("block") != null ? asInt(map.getObject("block")) : 0,
                        map.getObject("sky") != null ? asInt(map.getObject("sky")) : 0
                );
            }
        });

    }

    private static void registerProperties() {
        EntityProperties.register();

        NBT_MECHANISMS.put("CustomNameVisible", new NbtMechanism("customNameVisible", EntityTag::nbtByteToBool));
        NBT_MECHANISMS.put("Health", new NbtMechanism("health", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("Glowing", new NbtMechanism("glowing", EntityTag::nbtByteToBool));
        NBT_MECHANISMS.put("NoGravity", new NbtMechanism("gravity", EntityTag::nbtInvertedByteToBool));
        NBT_MECHANISMS.put("Invulnerable", new NbtMechanism("invulnerable", EntityTag::nbtByteToBool));
        NBT_MECHANISMS.put("Silent", new NbtMechanism("silent", EntityTag::nbtByteToBool));
        NBT_MECHANISMS.put("NoAI", new NbtMechanism("ai", EntityTag::nbtInvertedByteToBool));
        NBT_MECHANISMS.put("Fire", new NbtMechanism("fireTicks", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("TicksFrozen", new NbtMechanism("freezeTicks", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("Air", new NbtMechanism("air", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("FallDistance", new NbtMechanism("fallDistance", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("Motion", new NbtMechanism("velocity", EntityTag::nbtMotion));
        NBT_MECHANISMS.put("Rotation", new NbtMechanism("rotation", EntityTag::nbtRotation));
        NBT_MECHANISMS.put("width", new NbtMechanism("width", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("height", new NbtMechanism("height", EntityTag::nbtNumber));
        NBT_MECHANISMS.put("view_range", new NbtMechanism("viewRange", EntityTag::nbtNumber));
    }

    private EntityTag(Entity entity, WrapperEntity fakeEntity, EntityType type, Map<String, AbstractTag> mechanisms) {
        this.entity = entity;
        this.fakeEntity = fakeEntity;
        this.type = type;
        this.mechanisms = mechanisms;
        this.liveView = entity != null ? new BukkitEntityView(entity)
                : fakeEntity != null ? new FakeEntityView(fakeEntity)
                  : null;
    }

    private EntityTag(Entity entity, EntityType type, Map<String, AbstractTag> mechanisms) {
        this(entity, null, type, mechanisms);
    }

    private static Map<String, AbstractTag> mechanismsOf(MapTag source) {
        Map<String, AbstractTag> result = new LinkedHashMap<>();
        for (String key : source.keySet()) {
            AbstractTag value = source.getObject(key);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    public EntityTag(UUID uuid) {
        this(Bukkit.getEntity(uuid), null, null, new LinkedHashMap<>());
    }

    public EntityTag(Entity entity) {
        this(entity, null, null, new LinkedHashMap<>());
    }

    public EntityTag(String raw) {
        Entity parsedEntity = null;
        WrapperEntity parsedFake = null;
        EntityType parsedType = null;
        Map<String, AbstractTag> parsedMechanisms = new LinkedHashMap<>();

        if (raw != null && !raw.isEmpty()) {
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(prefix.length() + 1) : raw;
            int bracketStart = cleanRaw.indexOf('[');
            String basePart = cleanRaw;

            if (bracketStart > 0 && cleanRaw.endsWith("]")) {
                basePart = cleanRaw.substring(0, bracketStart);
                parsedMechanisms = mechanismsOf(new MapTag(cleanRaw.substring(bracketStart + 1, cleanRaw.length() - 1)));
            }

            if (basePart.startsWith("fake-")) {
                try {
                    int id = Integer.parseInt(basePart.substring(5));
                    parsedFake = FakeEntityRegistry.getById(id);
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    parsedEntity = Bukkit.getEntity(UUID.fromString(basePart));
                } catch (IllegalArgumentException ignored) {
                    parsedType = matchEntityType(basePart);
                }
            }
        }

        this.entity = parsedEntity;
        this.fakeEntity = parsedFake;
        this.type = parsedType;
        this.mechanisms = parsedMechanisms;

        this.liveView = entity != null ? new BukkitEntityView(entity)
                : fakeEntity != null ? new FakeEntityView(fakeEntity)
                  : null;
    }

    private LiveEntityView bindProperty(String name, AbstractTag value) {
        if (entity == null) {
            mechanisms.put(name, value);
        }
        return liveView;
    }

    private AbstractTag adjust(String name, AbstractTag value, BiConsumer<LiveEntityView, AbstractTag> liveApplier) {
        if (liveView != null) {
            liveApplier.accept(liveView, value);
        }

        if (entity == null) {
            if (value == null) mechanisms.remove(name);
            else mechanisms.put(name, value);
        }
        return this;
    }

    public EntityTag fakeSpawn(Location location, List<UUID> targets, int durationTicks) {
        if (type == null) return null;

        com.github.retrooper.packetevents.protocol.entity.type.EntityType peType = SpigotConversionUtil.fromBukkitEntityType(type);

        WrapperEntity wrapperEntity = new WrapperEntity(peType);
        wrapperEntity.spawn(SpigotConversionUtil.fromBukkitLocation(location));

        EntityTag fake = new EntityTag(null, wrapperEntity, type, new LinkedHashMap<>());

        EntityMeta meta = wrapperEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        try {
            for (Map.Entry<String, AbstractTag> entry : mechanisms.entrySet()) {
                fake.applyMechanism(entry.getKey(), entry.getValue());
            }
        }
        finally {
            meta.setNotifyAboutChanges(true);
        }

        for (UUID target : targets) {
            wrapperEntity.addViewer(target);
        }

        FakeEntityRegistry.register(wrapperEntity, durationTicks);
        return fake;
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
            for (Map.Entry<String, AbstractTag> entry : mechanisms.entrySet()) {
                blueprint.applyMechanism(entry.getKey(), entry.getValue());
            }
        }

        if (persistent) spawned.setPersistent(true);
        return new EntityTag(spawned);
    }

    public MapTag readNbt() {
        if (entity == null || nms == null) return new MapTag();
        return nms.readNbt(entity);
    }

    private static int resolveTicks(AbstractTag val) {
        DurationTag asDuration = DurationTag.tryParse(val);
        return asDuration != null ? (int) Math.round(asDuration.getTicks()) : asInt(val);
    }

    public MapTag describe() {
        if (entity == null) {
            MapTag copy = new MapTag();
            for (Map.Entry<String, AbstractTag> entry : mechanisms.entrySet()) copy.putObject(entry.getKey(), entry.getValue());
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

    public static List<EntityTag> resolveBlueprints(AbstractTag argument) {
        List<EntityTag> blueprints = new ArrayList<>();
        switch (argument) {
            case EntityTag entity -> blueprints.add(entity);
            case ListTag list -> list.getList().forEach(tag ->
                    blueprints.add(tag instanceof EntityTag entity ? entity : new EntityTag(tag.identify())));
            case null -> {}
            default -> blueprints.add(new EntityTag(argument.identify()));
        }
        return blueprints;
    }

    public static ListTag resolveBlueprintList(CompiledArgument rawArg, ScriptQueue queue) {
        ListTag result = new ListTag();
        if (rawArg == null) return result;
        String raw = rawArg.getRaw();

        for (String piece : ObjectFetcher.splitIgnoringBrackets(raw, '|')) {
            String stripped = piece.strip();
            int bracketStart = stripped.indexOf('[');

            if (bracketStart > 0 && stripped.endsWith("]") && stripped.charAt(0) != '<') {
                result.addObject(new EntityTag(stripped, queue));
            } else {
                AbstractTag evaluated = ScriptCompiler.parseArg(stripped).evaluate(queue);
                for (EntityTag blueprint : resolveBlueprints(evaluated)) {
                    result.addObject(blueprint);
                }
            }
        }

        return result;
    }

    public EntityTag(String raw, ScriptQueue queue) {
        Entity parsedEntity = null;
        WrapperEntity parsedFake = null;
        EntityType parsedType = null;
        Map<String, AbstractTag> parsedMechanisms = new LinkedHashMap<>();

        if (raw != null && !raw.isEmpty()) {
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(prefix.length() + 1) : raw;
            int bracketStart = cleanRaw.indexOf('[');
            String basePart = cleanRaw;

            if (bracketStart > 0 && cleanRaw.endsWith("]")) {
                basePart = cleanRaw.substring(0, bracketStart);
                String bracketContent = cleanRaw.substring(bracketStart + 1, cleanRaw.length() - 1);

                for (String pair : ObjectFetcher.splitIgnoringBrackets(bracketContent, ';')) {
                    int eq = pair.indexOf('=');
                    if (eq <= 0) continue;

                    String key = pair.substring(0, eq).strip();
                    String valueRaw = pair.substring(eq + 1).strip();

                    AbstractTag value = ScriptCompiler.parseArg(valueRaw).evaluate(queue);
                    if (value != null) parsedMechanisms.put(key, value);
                }
            }

            String resolvedBase = ScriptCompiler.parseArg(basePart).evaluate(queue).identify();

            if (resolvedBase.startsWith("fake-")) {
                try {
                    int id = Integer.parseInt(resolvedBase.substring(5));
                    parsedFake = FakeEntityRegistry.getById(id);
                } catch (NumberFormatException ignored) {}
            } else {
                try {
                    parsedEntity = Bukkit.getEntity(UUID.fromString(resolvedBase));
                } catch (IllegalArgumentException ignored) {
                    parsedType = matchEntityType(resolvedBase);
                }
            }
        }

        this.entity = parsedEntity;
        this.fakeEntity = parsedFake;
        this.type = parsedType;
        this.mechanisms = parsedMechanisms;

        this.liveView = entity != null ? new BukkitEntityView(entity)
                : fakeEntity != null ? new FakeEntityView(fakeEntity)
                  : null;
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
        if (fakeEntity != null) return prefix + "@fake-" + fakeEntity.getEntityId();

        StringBuilder builder = new StringBuilder(prefix + "@");
        builder.append(type != null ? type.getKey().getKey() : "unknown");

        if (!mechanisms.isEmpty()) {
            List<String> pairs = new ArrayList<>();
            for (Map.Entry<String, AbstractTag> entry : mechanisms.entrySet()) {
                pairs.add(entry.getKey() + "=" + entry.getValue().identify());
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
        return new EntityTag(entity, type, new LinkedHashMap<>(mechanisms));
    }

    @Override
    public @NotNull AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NotNull AbstractTag applyMechanisms(@NotNull Map<String, AbstractTag> mechanisms) {
        if (fakeEntity == null) {
            return Adjustable.super.applyMechanisms(mechanisms);
        }

        EntityMeta meta = fakeEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        try {
            return Adjustable.super.applyMechanisms(mechanisms);
        }
        finally {
            meta.setNotifyAboutChanges(true);
        }
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
