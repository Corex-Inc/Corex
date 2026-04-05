package dev.corexinc.corex.environment.tags.entity;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class EntityTag implements AbstractTag {

    private static String prefix = "e";
    private final Entity entity;

    public static final TagProcessor<EntityTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("entity", (attribute) -> new EntityTag(attribute.getParam()));

        ObjectFetcher.registerFetcher(prefix, (uuidStr) -> new EntityTag(UUID.fromString(uuidStr)));

        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) ->
                new ElementTag(object.entity.getUniqueId().toString()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) ->
                new ElementTag(object.entity.getName()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "type", (attribute, object) ->
                new ElementTag(object.entity.getType().name()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isAlive", (attribute, object) ->
                new ElementTag(String.valueOf(!object.entity.isDead())));

        TAG_PROCESSOR.registerTag(LocationTag.class, "location", (attribute, object) ->
                new LocationTag(object.entity.getLocation()));
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
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(2) : raw;

            Entity tempEntity;
            try {
                tempEntity = Bukkit.getEntity(UUID.fromString(cleanRaw));
            } catch (Exception e) {
                tempEntity = null;
            }
            this.entity = tempEntity;
        }
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public @NotNull String identify() {
        return prefix + "@" + entity.getUniqueId().toString();
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
    public @NonNull String getTestValue() {
        return "e@cf5d1e35-fb92-476e-9c96-bc932ca0b0cb";
    }

    @Override
    public @NonNull TagProcessor<EntityTag> getProcessor() {
        return TAG_PROCESSOR;
    }
}