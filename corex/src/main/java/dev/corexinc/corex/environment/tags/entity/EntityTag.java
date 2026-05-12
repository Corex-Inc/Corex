package dev.corexinc.corex.environment.tags.entity;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.PdcFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/* @doc object
 *
 * @Name EntityTag
 * @Prefix e
 * @Format
 * The identity format for entities is a spawned entity's UUID, or an entity type.
 * For example, 'e@abc123' or 'e@zombie'.
 *
 * @Description
 * An EntityTag represents a spawned entity, or a generic entity type.
 */
public class EntityTag implements AbstractTag, Flaggable {

    private static final String prefix = "e";
    private final Entity entity;

    public static final TagProcessor<EntityTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("entity", (attribute) -> {
            if (!attribute.hasParam()) return null;
            return new EntityTag(attribute.getParam());
        });

        ObjectFetcher.registerFetcher(prefix, (uuidStr) -> new EntityTag(UUID.fromString(uuidStr)));

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
        TAG_PROCESSOR.registerTag(ElementTag.class, "type", (attribute, object) -> new ElementTag(object.entity.getType().name()));

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
    }

    public EntityTag(UUID uuid) {
        this.entity = Bukkit.getEntity(uuid);
    }

    public EntityTag(Entity entity) {
        this.entity = entity;
    }

    public EntityTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.entity = null;
        } else {
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(prefix.length() + 1) : raw;

            Entity tempEntity;
            try {
                tempEntity = Bukkit.getEntity(UUID.fromString(cleanRaw));
            } catch (Exception e) {
                tempEntity = null;
            }
            this.entity = tempEntity;
        }
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

    @Override
    public @NotNull String identify() {
        return prefix + "@" + entity.getUniqueId();
    }

    @Override
    public @NotNull String getPrefix() {
        return prefix;
    }

    @Override
    public @Nullable AbstractTag getAttribute(@NotNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
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