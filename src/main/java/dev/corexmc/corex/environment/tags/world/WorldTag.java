package dev.corexmc.corex.environment.tags.world;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.environment.tags.core.DurationTag;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class WorldTag implements AbstractTag {

    private static final String prefix = "w";
    private final World world;

    public static final TagProcessor<WorldTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("world", attr -> new WorldTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, WorldTag::new);

        PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getName() : ""));

        PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getUID().toString() : ""));

        PROCESSOR.registerTag(ElementTag.class, "environment", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getEnvironment().name() : ""));

        PROCESSOR.registerTag(ElementTag.class, "time", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getTime() : 0));

        PROCESSOR.registerTag(DurationTag.class, "fullTime", (attr, obj) ->
                obj.world != null ? new DurationTag(obj.world.getFullTime()) : null);

        PROCESSOR.registerTag(ElementTag.class, "players", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getPlayers().size() : 0));

        PROCESSOR.registerTag(ElementTag.class, "seed", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getSeed() : 0));

        PROCESSOR.registerTag(ElementTag.class, "difficulty", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getDifficulty().name() : ""));

        PROCESSOR.registerTag(ElementTag.class, "isStorming", (attr, obj) ->
                new ElementTag(obj.world != null && obj.world.hasStorm()));

        PROCESSOR.registerTag(ElementTag.class, "isThundering", (attr, obj) ->
                new ElementTag(obj.world != null && obj.world.isThundering()));

        PROCESSOR.registerTag(LocationTag.class, "spawn", (attr, obj) ->
                obj.world != null ? new LocationTag(obj.world.getSpawnLocation()) : null);
    }

    public WorldTag(World world) {
        this.world = world;
    }

    public WorldTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.world = null;
            return;
        }

        String clean = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(2) : raw;

        World resolved;
        try {
            resolved = Bukkit.getWorld(UUID.fromString(clean));
        } catch (Exception e) {
            resolved = Bukkit.getWorld(clean);
        }
        this.world = resolved;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + (world != null ? world.getName() : "");
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<WorldTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "w@world";
    }
}