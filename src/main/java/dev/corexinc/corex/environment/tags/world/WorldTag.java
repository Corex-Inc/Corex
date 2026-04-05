package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class WorldTag implements AbstractTag {

    private static final String prefix = "w";
    private final World world;

    public static final TagProcessor<WorldTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("world", attr -> new WorldTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, WorldTag::new);

        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getName() : ""));

        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getUID().toString() : ""));

        TAG_PROCESSOR.registerTag(ElementTag.class, "environment", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getEnvironment().name() : ""));

        TAG_PROCESSOR.registerTag(ElementTag.class, "time", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getTime() : 0));

        TAG_PROCESSOR.registerTag(DurationTag.class, "fullTime", (attr, obj) ->
                obj.world != null ? new DurationTag(obj.world.getFullTime()) : null);

        TAG_PROCESSOR.registerTag(ElementTag.class, "players", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getPlayers().size() : 0));

        TAG_PROCESSOR.registerTag(ElementTag.class, "seed", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getSeed() : 0));

        TAG_PROCESSOR.registerTag(ElementTag.class, "difficulty", (attr, obj) ->
                new ElementTag(obj.world != null ? obj.world.getDifficulty().name() : ""));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isStorming", (attr, obj) ->
                new ElementTag(obj.world != null && obj.world.hasStorm()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isThundering", (attr, obj) ->
                new ElementTag(obj.world != null && obj.world.isThundering()));

        TAG_PROCESSOR.registerTag(LocationTag.class, "spawn", (attr, obj) ->
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
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<WorldTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "w@world";
    }
}